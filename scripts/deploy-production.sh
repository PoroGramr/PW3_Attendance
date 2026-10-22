#!/usr/bin/env bash
set -Eeuo pipefail

DEPLOY_DIR="${PW3_DEPLOY_DIR:-/home/ppp9177/pw3-production}"
HOST=pw3.porogramr.com
exec 9>"$DEPLOY_DIR/.deploy.lock"
flock -n 9 || { echo 'Another PW3 deployment is in progress'; exit 1; }
cd "$DEPLOY_DIR"
test -f .env || { echo 'Missing production .env'; exit 1; }
for key in SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD JWT_SECRET MINIO_ENDPOINT MINIO_ACCESS_KEY MINIO_SECRET_KEY MAIL_HOST MAIL_USERNAME MAIL_PASSWORD; do
  grep -q "^${key}=." .env || { echo "Missing production variable: $key"; exit 1; }
done

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
