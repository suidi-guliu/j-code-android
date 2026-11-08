#!/bin/bash
set -euo pipefail
ACTION=$1
PROJECT=$2
TOOL=/usr/bin/python3
if [ -r /opt/sui-code/python.env ]; then . /opt/sui-code/python.env; TOOL=$SUI_PYTHON; fi
[ -x "$TOOL" ] || { echo 'Python environment unavailable' >&2; exit 78; }
cd -- "$PROJECT"
export PYTHONPYCACHEPREFIX="$PROJECT/.sui-code/pycache"
case "$ACTION" in
    build) "$TOOL" -m compileall -q -x '/\.sui-code/|/\.venv/' . ;;
    test) "$TOOL" -m unittest discover -s tests -v ;;
    run) exec "$TOOL" "$PROJECT/main.py" ;;
    *) exit 64 ;;
esac
