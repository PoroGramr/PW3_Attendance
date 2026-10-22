#!/usr/bin/env bash
set -Eeuo pipefail

DEPLOY_DIR="${PW3_DEPLOY_DIR:-/home/ppp9177/pw3-production}"
HOST=pw3.porogramr.com
exec 9>"$DEPLOY_DIR/.deploy.lock"
flock -n 9 || { echo 'Another PW3 deployment is in progress'; exit 1; }
cd "$DEPLOY_DIR"
# Reuse the existing Actions production secret, never the legacy host DB env.
: "${PRODUCTION_ENV_FILE:?Missing GitHub Actions PRODUCTION_ENV_FILE secret}"
umask 077
trap 'rm -f "$DEPLOY_DIR/.env.next"' EXIT
printf '%s\n' "$PRODUCTION_ENV_FILE" > .env.next
unset PRODUCTION_ENV_FILE
for key in SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD; do
  grep -q "^${key}=." .env.next || { echo "Missing production variable: $key"; exit 1; }
done
if grep -Eq '^SPRING_DATASOURCE_URL=jdbc:mysql://(db|localhost|127\.0\.0\.1|attendance_db)(:|/)' .env.next; then
  echo 'Refusing to connect to the legacy local MySQL database'
  exit 1
fi
mv .env.next .env

git fetch --prune origin main
git reset --hard origin/main
./gradlew --no-daemon bootJar
jar=$(find build/libs -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' -print -quit)
test -n "$jar"
cp "$jar" app.jar
compose=(docker compose -f compose.production.yml)
"${compose[@]}" config --quiet
"${compose[@]}" build app
"${compose[@]}" up -d --remove-orphans

ready=false
for attempt in $(seq 1 90); do
  if curl --fail --silent --max-time 3 --resolve "$HOST:443:127.0.0.1" "https://$HOST/actuator/health" | grep -q '"status":"UP"'; then
    ready=true
    break
  fi
  sleep 2
done
if [ "$ready" != true ]; then
  "${compose[@]}" ps
  "${compose[@]}" logs --tail=100 app
  exit 1
fi
curl --fail --silent --show-error --resolve "$HOST:443:127.0.0.1" "https://$HOST/qr-display.html" -o /dev/null
printf 'PW3 production healthy at https://%s (commit %s)\n' "$HOST" "$(git rev-parse --short HEAD)"
