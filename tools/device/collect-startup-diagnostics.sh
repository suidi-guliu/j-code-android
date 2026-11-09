#!/usr/bin/env bash
# Read-only collection; no clear-data, uninstall, force-stop, logcat clear or rootfs changes.
set -euo pipefail
output=${1:-sui-startup-diagnostics}
mkdir -p "$output"
adb devices -l > "$output/devices.txt"
adb shell dumpsys package dev.suicode.ide.debug > "$output/package.txt"
adb logcat -d -v threadtime -s AndroidRuntime SuiStartup SuiStartupTest SuiPerformance > "$output/logcat.txt"
for name in startup.log startup.previous.log; do
  adb exec-out run-as dev.suicode.ide.debug cat "files/sui-code/diagnostics/$name" > "$output/$name" 2> "$output/$name.error" || true
done
printf 'Saved startup diagnostics to %s\n' "$output"
