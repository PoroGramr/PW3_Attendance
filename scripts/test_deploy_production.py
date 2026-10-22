"""Offline deploy-script tests; Docker/Git/network calls are stateful stubs."""
import os
import subprocess
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("deploy-production.sh")
OLD = "sha256:" + "a" * 64
NEW = "sha256:" + "b" * 64

DOCKER = '''#!/bin/sh
printf 'docker %s\\n' "$*" >> "$TRACE"
case "$*" in
  "inspect -f "*"com.docker.compose.project"*) echo pw3-production; exit 0 ;;
  "inspect -f "*"com.docker.compose.service"*) echo app; exit 0 ;;
esac
if [ "$1" = inspect ] && [ "$2" = -f ]; then cat "$RUNNING_ID"; exit 0; fi
if [ "$1" = image ] && [ "$2" = inspect ]; then
  if [ "$3" = -f ]; then cat "$TAG_ID"; fi
  exit 0
fi
if [ "$1" = image ] && [ "$2" = tag ]; then
  if [ "$4" = pw3-production-app:latest ]; then cat "$BACKUP_ID" > "$TAG_ID";
  else cat "$RUNNING_ID" > "$BACKUP_ID"; fi
  exit 0
fi
case "$*" in
  *" build app") printf '%s' "$NEW_ID" > "$TAG_ID" ;;
  *" up "*) n=$(cat "$COUNTER"); echo $((n+1)) > "$COUNTER"
    cat "$TAG_ID" > "$RUNNING_ID" ;;
esac
exit 0
'''
CURL = '''#!/bin/sh
if printf '%s' "$*" | grep -q actuator/health; then
  n=$(cat "$COUNTER")
  if { [ "$CASE" = fail ] && [ "$n" -eq 1 ]; } || [ "$CASE" = rollback-fail ]; then
    printf '{"status":"DOWN"}'
  else printf '{"status":"UP"}'; fi
fi
exit 0
'''
GIT = '''#!/bin/sh
if [ "$1" = rev-parse ]; then echo abc123; fi
if [ "$1" = reset ]; then printf 'candidate-compose\\n' > compose.production.yml; fi
exit 0
'''
GRADLE = '''#!/bin/sh
mkdir -p build/libs
printf 'new-jar' > build/libs/test.jar
exit 0
'''


class DeploymentTest(unittest.TestCase):
    def run_case(self, scenario):
        with tempfile.TemporaryDirectory(dir=os.environ.get("TMPDIR")) as tmp:
            root = Path(tmp)
            binary = root / "bin"
            binary.mkdir()
            for name, data in {"docker": DOCKER, "curl": CURL, "git": GIT,
                               "sleep": "#!/bin/sh\nexit 0\n"}.items():
                path = binary / name
                path.write_text(data)
                path.chmod(0o755)
            gradle = root / "gradlew"
            gradle.write_text(GRADLE if scenario != "build-fail" else "#!/bin/sh\nexit 1\n")
            gradle.chmod(0o755)
            (root / ".env").write_text("PREVIOUS_ENV=true\n")
            (root / "app.jar").write_text("old-jar")
            (root / "compose.production.yml").write_text("previous-compose\n")
            maintenance = root / "maintenance"
            maintenance.mkdir()
            marker = maintenance / "pw3-attendance"
            if scenario == "preexisting-marker":
                marker.write_text("operator-hold\n")
            if scenario == "preexisting-backup":
                (root / ".env.previous").write_text("last-resort-backup\n")
            trace = root / "trace"
            trace.write_text("")
            count = root / "counter"
            count.write_text("0")
            tag_id = root / "tag-id"
            tag_id.write_text(NEW if scenario == "tag-mismatch" else OLD)
            running_id = root / "running-id"
            running_id.write_text(OLD)
            backup_id = root / "backup-id"
            backup_id.write_text("")
            env = {**os.environ, "PATH": f"{binary}:{os.environ['PATH']}",
                   "PW3_DEPLOY_DIR": str(root), "PW3_HEALOPS_MAINTENANCE_DIR": str(maintenance),
                   "TRACE": str(trace), "COUNTER": str(count),
                   "TAG_ID": str(tag_id), "RUNNING_ID": str(running_id),
                   "BACKUP_ID": str(backup_id), "NEW_ID": NEW,
                   "CASE": scenario, "GITHUB_RUN_ID": "fixture", "PW3_HEALTH_ATTEMPTS": "1",
                   "PW3_HEALTH_SLEEP": "0",
                   "PRODUCTION_ENV_FILE": "SPRING_DATASOURCE_URL=jdbc:mysql://safe.invalid:3306/prod\n"
                   "SPRING_DATASOURCE_USERNAME=fixture\nSPRING_DATASOURCE_PASSWORD=fixture"}
            result = subprocess.run(["bash", str(SCRIPT)], env=env,
                                    capture_output=True, text=True, timeout=20)
            return {"result": result, "env": (root / ".env").read_text(),
                    "jar": (root / "app.jar").read_text(),
                    "compose": (root / "compose.production.yml").read_text(),
                    "trace": trace.read_text(), "tag": tag_id.read_text(),
                    "running": running_id.read_text(),
                    "retained": (root / ".env.previous").exists(),
                    "marker": marker.read_text() if marker.exists() else None}

    def test_success_promotes_candidate(self):
        s = self.run_case("success")
        self.assertEqual(s["result"].returncode, 0, s["result"].stderr)
        self.assertIn("safe.invalid", s["env"])
        self.assertEqual(s["jar"], "new-jar")
        self.assertEqual(s["tag"], NEW)
        self.assertEqual(s["running"], NEW)
        self.assertFalse(s["retained"])
        self.assertIsNone(s["marker"])

    def test_health_failure_restores_previous_image_env_jar_and_compose(self):
        s = self.run_case("fail")
        self.assertNotEqual(s["result"].returncode, 0)
        self.assertIn("restored", s["result"].stdout)
        self.assertEqual(s["env"], "PREVIOUS_ENV=true\n")
        self.assertEqual(s["jar"], "old-jar")
        self.assertEqual(s["compose"], "previous-compose\n")
        self.assertEqual(s["tag"], OLD)
        self.assertEqual(s["running"], OLD)
        self.assertFalse(s["retained"])
        self.assertIsNone(s["marker"])

    def test_build_failure_never_recreates_container(self):
        s = self.run_case("build-fail")
        self.assertNotEqual(s["result"].returncode, 0)
        self.assertEqual(s["tag"], OLD)
        self.assertEqual(s["env"], "PREVIOUS_ENV=true\n")
        self.assertEqual(s["compose"], "previous-compose\n")
        self.assertNotIn(" up ", s["trace"])
        self.assertIsNone(s["marker"])

    def test_failed_rollback_retains_local_backups(self):
        s = self.run_case("rollback-fail")
        self.assertNotEqual(s["result"].returncode, 0)
        self.assertIn("ROLLBACK FAILED", s["result"].stderr)
        self.assertTrue(s["retained"])
        self.assertIsNone(s["marker"])

    def test_preexisting_operator_marker_is_preserved(self):
        s = self.run_case("preexisting-marker")
        self.assertNotEqual(s["result"].returncode, 0)
        self.assertEqual(s["marker"], "operator-hold\n")
        self.assertNotIn("image tag", s["trace"])

    def test_preexisting_backup_is_never_overwritten_or_deleted(self):
        s = self.run_case("preexisting-backup")
        self.assertNotEqual(s["result"].returncode, 0)
        self.assertTrue(s["retained"])
        self.assertNotIn("image tag", s["trace"])
        self.assertEqual(s["env"], "PREVIOUS_ENV=true\n")

    def test_mismatched_image_tag_fails_closed(self):
        s = self.run_case("tag-mismatch")
        self.assertNotEqual(s["result"].returncode, 0)
        self.assertIn("differs", s["result"].stderr)
        self.assertNotIn("image tag", s["trace"])
        self.assertEqual(s["tag"], NEW)
        self.assertEqual(s["running"], OLD)


if __name__ == "__main__":
    unittest.main()
