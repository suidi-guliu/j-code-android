#!/usr/bin/env bash
set -euo pipefail
mode=${1:?upgrade or fresh}
case "$mode" in upgrade) port=5554;; fresh) port=5556;; *) exit 2;; esac
: "${RUNNER_TEMP:?GitHub owned runner required}" "${ANDROID_HOME:?SDK required}" "${ANDROID_AVD_HOME:?owned AVDs required}"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
evidence="$RUNNER_TEMP/v054-android-evidence"
mkdir -p "$evidence"
emulator -avd "sui-v054-$mode" -port "$port" -no-window -no-audio -no-snapshot -no-boot-anim -gpu swiftshader -memory 2048 -cores 2 -skin 1280x800 -camera-back none -camera-front none > "$evidence/emulator-$mode.log" 2>&1 &
emulator_pid=$!
trap 'kill "$emulator_pid" 2>/dev/null || true' EXIT
serial="emulator-$port"
ready=false
for attempt in $(seq 1 120); do
  if adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' | grep -q '^1$'; then ready=true; break; fi
  sleep 5
done
test "$ready" = true
python3 tools/ide_reliability/android_regression.py --serial "$serial" --mode "$mode" --inputs "$RUNNER_TEMP/v054-inputs" --apk .delivery/sui-code-v0.5.4/sui-code-v0.5.4-debug.apk --kit .delivery/sui-code-v0.5.4/sui-code-zh-hans-1.0.1.suik --evidence "$evidence/$mode"
adb -s "$serial" emu kill || true
wait "$emulator_pid" || true
trap - EXIT
