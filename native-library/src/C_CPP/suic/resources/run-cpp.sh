#!/bin/bash
set -u
ENV_FILE=/opt/sui-code/cpp.env
if [ -r "$ENV_FILE" ]; then
    unset SUI_CPP
    . "$ENV_FILE" || { echo '[QuickRun environment error] Cannot load cpp environment.' >&2; exit 78; }
    TOOL="${SUI_CPP:-}"
else
    TOOL=/usr/bin/g++
    echo '[QuickRun] Using the existing Ubuntu /usr/bin/g++; no managed override is configured.'
fi
[ -n "$TOOL" ] && [ -x "$TOOL" ] || { echo '[QuickRun environment error] Install cpp through Toolchains; no automatic installation or fallback.' >&2; exit 78; }
"$TOOL" --version || { echo '[QuickRun environment error] The selected tool cannot run.' >&2; exit 78; }
cd -- "$(dirname -- "$1")" || exit 66
BUILD=$(mktemp -d "${TMPDIR:-/tmp}/sui-quickrun-XXXXXX") || exit 73
trap 'rm -rf -- "$BUILD"' EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
SOURCE="$1"
cp -- "$SOURCE" "$BUILD/input" || { echo '[QuickRun file error] Cannot snapshot the saved file.' >&2; exit 66; }
echo '[QuickRun] Compiling cpp into a temporary directory'
if ! "$TOOL" -std=c++20 -Wall -Wextra "-ffile-prefix-map=$BUILD/input=$SOURCE" -iquote "$PWD" -x c++ "$BUILD/input" -o "$BUILD/program"; then
    echo '[QuickRun compilation failed]' >&2; exit 65;
fi
echo '[QuickRun] Running cpp; terminal input is connected'
"$BUILD/program"
RESULT=$?
if [ "$RESULT" -ne 0 ]; then echo "[QuickRun runtime failed] exit $RESULT" >&2; exit 70; fi
echo '[QuickRun completed] exit 0'
