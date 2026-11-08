#!/usr/bin/env bash
# Read-only capture: deliberately does not clear logcat, force-stop, reinstall, or change app data.
set -euo pipefail
command -v adb >/dev/null || { echo 'Android platform-tools adb is required.' >&2; exit 1; }
adb get-state >/dev/null
output=${1:-sui-code-v05-logcat.txt}
echo "Capturing to $output. Open Languages cold/warm, test keyboard/terminal and press Ctrl+C to finish."
adb logcat -v threadtime -T 1 SuiPerformance:V SuiKeyboard:V JCodeLSP:V JCodeDAP-adapter:V AndroidRuntime:E ActivityManager:I '*:S' | tee "$output"
