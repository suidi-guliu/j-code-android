#!/usr/bin/env bash
set -euo pipefail
mode=${1:?upgrade or fresh}
case "$mode" in upgrade) port=5554;; fresh) port=5556;; *) exit 2;; esac
: "${RUNNER_TEMP:?Owned runner required}" "${ANDROID_HOME:?SDK required}" "${ANDROID_AVD_HOME:?Owned AVDs required}"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
export SUI_OWNED_ANDROID_CI=1
candidate=.delivery/sui-code-v0.5.5-candidate
evidence="$RUNNER_TEMP/v055-evidence/$mode"
mkdir -p "$evidence"
serial="emulator-$port"
fixture_pid=''
emulator -avd "sui-v055-$mode" -port "$port" -no-window -no-audio -no-snapshot -no-boot-anim -gpu swiftshader -memory 2048 -cores 2 -skin 1280x800 -camera-back none -camera-front none > "$evidence/emulator.log" 2>&1 &
emulator_pid=$!
cleanup() {
  if [[ -n "$fixture_pid" ]]; then kill -INT "$fixture_pid" 2>/dev/null || true; fi
  adb -s "$serial" emu kill >/dev/null 2>&1 || kill "$emulator_pid" 2>/dev/null || true
}
trap cleanup EXIT
ready=false
for attempt in $(seq 1 120); do
  if adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' | grep -q '^1$'; then ready=true; break; fi
  kill -0 "$emulator_pid"
  sleep 5
done
test "$ready" = true
/usr/bin/python3 tools/ide_reliability/android_debug_fixture.py --output "$evidence/debug-fixture" > "$evidence/fixture.log" 2>&1 &
fixture_pid=$!
metadata="$evidence/debug-fixture/fixture.json"
for attempt in $(seq 1 20); do
  if [[ -s "$metadata" ]]; then break; fi
  kill -0 "$fixture_pid"
  sleep 1
done
for fixture_port in $(jq -r '.controlPort,.ttyPort,.dapPort' "$metadata"); do
  adb -s "$serial" reverse "tcp:$fixture_port" "tcp:$fixture_port"
done
python3 tools/ide_reliability/android_regression_v055.py \
  --serial "$serial" --mode "$mode" --inputs "$RUNNER_TEMP/v055-inputs" \
  --apk "$candidate/sui-code-v0.5.5-debug.apk" \
  --android-test-apk "$candidate/sui-code-v0.5.5-androidTest.apk" \
  --kit localization/releases/sui-code-zh-hans-1.0.2.suik \
  --debug-fixture "$metadata" --evidence "$evidence"
