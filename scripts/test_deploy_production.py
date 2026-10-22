"""Offline deploy-script tests; every Docker/Git/network command is stubbed."""
import os
import subprocess
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("deploy-production.sh")

DOCKER = '''#!/bin/sh
printf 'docker %s\\n' "$*" >> "$TRACE"
case "$*" in
  "inspect -f "*"com.docker.compose.project"*) echo pw3-production; exit 0 ;;
  "inspect -f "*"com.docker.compose.service"*) echo app; exit 0 ;;
  "image inspect "*) exit 0 ;;
  "image tag "*) exit 0 ;;
  *" up "*) n=$(cat "$COUNTER"); echo $((n+1)) > "$COUNTER" ;;
esac
exit 0
'''
CURL = '''#!/bin/sh
if printf '%s' "$*" | grep -q actuator/health; then
  n=$(cat "$COUNTER")
  if { [ "$CASE" = fail ] && [ "$n" -eq 1 ]; } || [ "$CASE" = rollback-fail ]; then printf '{"status":"DOWN"}';
  else printf '{"status":"UP"}'; fi
fi
exit 0
'''
GIT = '''#!/bin/sh
if [ "$1" = rev-parse ]; then echo abc123; fi
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
            (root / "compose.production.yml").write_text("services: {}\n")
            trace = root / "trace"
            trace.write_text("")
            count = root / "counter"
            count.write_text("0")
            env = {**os.environ, "PATH": f"{binary}:{os.environ['PATH']}",
                   "PW3_DEPLOY_DIR": str(root), "TRACE": str(trace), "COUNTER": str(count),
                   "CASE": scenario, "GITHUB_RUN_ID": "fixture", "PW3_HEALTH_ATTEMPTS": "1",
                   "PW3_HEALTH_SLEEP": "0",
                   "PRODUCTION_ENV_FILE": "SPRING_DATASOURCE_URL=jdbc:mysql://safe.invalid:3306/prod\n"
                   "SPRING_DATASOURCE_USERNAME=fixture\nSPRING_DATASOURCE_PASSWORD=fixture"}
            result = subprocess.run(["bash", str(SCRIPT)], env=env,
                                    capture_output=True, text=True, timeout=20)
            return (result, (root / ".env").read_text(), (root / "app.jar").read_text(),
                    trace.read_text(), (root / ".env.previous").exists())

    def test_success_promotes_candidate(self):
        result, env, jar, trace, retained = self.run_case("success")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("safe.invalid", env)
        self.assertEqual(jar, "new-jar")
        self.assertEqual(trace.count("--remove-orphans"), 1)
        self.assertFalse(retained)

    def test_health_failure_restores_previous_image_env_and_jar(self):
        result, env, jar, trace, retained = self.run_case("fail")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("restored", result.stdout)
        self.assertEqual(env, "PREVIOUS_ENV=true\n")
        self.assertEqual(jar, "old-jar")
        self.assertIn("--no-build --no-deps --force-recreate app", trace)
        self.assertEqual(trace.count("image tag "), 2)
        self.assertFalse(retained)

    def test_build_failure_never_recreates_container(self):
        result, env, jar, trace, retained = self.run_case("build-fail")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(env, "PREVIOUS_ENV=true\n")
        self.assertEqual(jar, "old-jar")
        self.assertNotIn(" up ", trace)
        self.assertFalse(retained)

    def test_failed_rollback_retains_local_backups(self):
        result, _, _, _, retained = self.run_case("rollback-fail")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("ROLLBACK FAILED", result.stderr)
        self.assertTrue(retained)


if __name__ == "__main__":
    unittest.main()
