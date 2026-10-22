#!/usr/bin/env bash
set -Eeuo pipefail

DEPLOY_DIR="${PW3_DEPLOY_DIR:-/home/ppp9177/pw3-production}"
HOST=pw3.porogramr.com
compose=(docker compose -f compose.production.yml)
image=pw3-production-app:latest
backup_tag=""
promotion_started=false
health_attempts="${PW3_HEALTH_ATTEMPTS:-90}"
health_sleep="${PW3_HEALTH_SLEEP:-2}"
[[ "$health_attempts" =~ ^[0-9]+$ ]] && (( health_attempts >= 1 && health_attempts <= 90 )) || exit 2
[[ "$health_sleep" =~ ^[0-9]+$ ]] && (( health_sleep <= 5 )) || exit 2

check_health() {
  local attempt
  for ((attempt=1; attempt<=health_attempts; attempt++)); do
    if curl --fail --silent --max-time 3 --resolve "$HOST:443:127.0.0.1" \
      "https://$HOST/actuator/health" | grep -q '"status":"UP"'; then
      if curl --fail --silent --max-time 3 --resolve "$HOST:443:127.0.0.1" \
        "https://$HOST/qr-display.html" -o /dev/null; then
        return 0
      fi
    fi
    sleep "$health_sleep"
  done
  return 1
}

rollback() {
  local failed=0
  docker image tag "$backup_tag" "$image" || failed=1
  cp -p .env.previous .env || failed=1
  cp -p app.jar.previous app.jar || failed=1
  if [ "$promotion_started" = true ]; then
    "${compose[@]}" up -d --no-build --no-deps --force-recreate app || failed=1
    check_health || failed=1
  fi
  if [ "$failed" -eq 0 ]; then
    echo 'PW3 previous image, env and artifact restored; health verified if promotion started'
  else
    echo 'PW3 ROLLBACK FAILED; operator intervention required' >&2
  fi
  return "$failed"
}

on_exit() {
  local status=$?
  trap - EXIT
  set +e
  rm -f .env.next
  if [ "$status" -ne 0 ] && [ -n "$backup_tag" ]; then
    if rollback; then
      rm -f .env.previous app.jar.previous
    else
      echo 'Retaining .env.previous and app.jar.previous for manual recovery' >&2
      status=1
    fi
  else
    rm -f .env.previous app.jar.previous
  fi
  exit "$status"
}

exec 9>"$DEPLOY_DIR/.deploy.lock"
flock -n 9 || { echo 'Another PW3 deployment is in progress'; exit 1; }
cd "$DEPLOY_DIR"
# Actions production secret only; never replace it with the legacy local DB.
: "${PRODUCTION_ENV_FILE:?Missing GitHub Actions PRODUCTION_ENV_FILE secret}"
umask 077
trap on_exit EXIT
printf '%s\n' "$PRODUCTION_ENV_FILE" > .env.next
unset PRODUCTION_ENV_FILE
for key in SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD; do
  grep -q "^${key}=." .env.next || { echo "Missing production variable: $key"; exit 1; }
done
if grep -Eq '^SPRING_DATASOURCE_URL=jdbc:mysql://(db|localhost|127\.0\.0\.1|attendance_db)(:|/)' .env.next; then
  echo 'Refusing to connect to the legacy local MySQL database'
  exit 1
fi
# Refuse deployment without a known-good image, artifact and env to restore.
test -f .env && test -f app.jar
[ "$(docker inspect -f '{{ index .Config.Labels "com.docker.compose.project" }}' pw3-attendance)" = "pw3-production" ]
[ "$(docker inspect -f '{{ index .Config.Labels "com.docker.compose.service" }}' pw3-attendance)" = "app" ]
docker image inspect "$image" >/dev/null 2>&1
cp -p .env .env.previous
cp -p app.jar app.jar.previous
backup_tag="pw3-production-app:rollback-${GITHUB_RUN_ID:-manual-$$}-${GITHUB_RUN_ATTEMPT:-1}"
docker image tag "$image" "$backup_tag"
mv .env.next .env

git fetch --prune origin main
git reset --hard origin/main
./gradlew --no-daemon bootJar
jar=$(find build/libs -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' -print -quit)
test -n "$jar"
cp "$jar" app.jar
"${compose[@]}" config --quiet
"${compose[@]}" build app
promotion_started=true
"${compose[@]}" up -d --remove-orphans
if ! check_health; then
  echo 'PW3 candidate failed Actuator/page checks; rolling back' >&2
  exit 1
fi
printf 'PW3 production healthy at https://%s (commit %s)\n' "$HOST" "$(git rev-parse --short HEAD)"
