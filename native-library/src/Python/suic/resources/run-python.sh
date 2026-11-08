#!/bin/bash
set -u
ENV_FILE=/opt/sui-code/python.env
if [ -r "$ENV_FILE" ]; then
    unset SUI_PYTHON
    . "$ENV_FILE" || { echo '[QuickRun environment error] Cannot load python environment.' >&2; exit 78; }
    TOOL="${SUI_PYTHON:-}"
else
    TOOL=/usr/bin/python3
    echo '[QuickRun] Using the existing Ubuntu /usr/bin/python3; no managed override is configured.'
fi
[ -n "$TOOL" ] && [ -x "$TOOL" ] || { echo '[QuickRun environment error] Install python through Toolchains; no automatic installation or fallback.' >&2; exit 78; }
"$TOOL" --version || { echo '[QuickRun environment error] The selected tool cannot run.' >&2; exit 78; }
cd -- "$(dirname -- "$1")" || exit 66
BUILD=$(mktemp -d /tmp/sui-quickrun-XXXXXX) || exit 73
trap 'rm -rf -- "$BUILD"' EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
SOURCE="$1"
cp -- "$SOURCE" "$BUILD/input" || { echo '[QuickRun file error] Cannot snapshot the saved file.' >&2; exit 66; }
export PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
if ! "$TOOL" -c 'import sys,tokenize; compile(tokenize.open(sys.argv[1]).read(), sys.argv[2], "exec")' "$BUILD/input" "$SOURCE"; then
    echo '[QuickRun compilation failed] Python syntax error' >&2; exit 65;
fi
echo '[QuickRun] Running Python; terminal input is connected'
"$TOOL" -u -c 'import sys,tokenize,os; original=sys.argv[2]; source=tokenize.open(sys.argv[1]).read(); sys.argv=[original]; sys.path[0]=os.path.dirname(original); namespace=sys.modules["__main__"].__dict__; namespace.update(__file__=original, __package__=None, __cached__=None, __spec__=None); exec(compile(source, original, "exec"), namespace)' "$BUILD/input" "$SOURCE"
RESULT=$?
if [ "$RESULT" -ne 0 ]; then echo "[QuickRun runtime failed] exit $RESULT" >&2; exit 70; fi
echo '[QuickRun completed] exit 0'
