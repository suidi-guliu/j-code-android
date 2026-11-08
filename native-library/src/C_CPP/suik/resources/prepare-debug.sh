#!/bin/bash
set -eu
SOURCE=$1
PROJECT=$2
OUT="$PROJECT/.sui-code/debug"
mkdir -p "$OUT"
if [ -f "$PROJECT/CMakeLists.txt" ]; then
    cmake -S "$PROJECT" -B "$OUT" -G Ninja -DCMAKE_BUILD_TYPE=Debug >&2
    cmake --build "$OUT" >&2
    PROGRAM="$OUT/app"
    [ -x "$PROGRAM" ] || { echo 'Configure a CMake executable target named app for this Kit workflow.' >&2; exit 64; }
else
    case "$SOURCE" in
        *.c) TOOL=/usr/bin/gcc; FLAGS=-std=c17; [ ! -r /opt/sui-code/c.env ] || { . /opt/sui-code/c.env; TOOL=$SUI_C; } ;;
        *) TOOL=/usr/bin/g++; FLAGS=-std=c++20; [ ! -r /opt/sui-code/cpp.env ] || { . /opt/sui-code/cpp.env; TOOL=$SUI_CPP; } ;;
    esac
    PROGRAM="$OUT/program"
    "$TOOL" "$FLAGS" -g -O0 "$SOURCE" -o "$PROGRAM" >&2
fi
jq -cn --arg program "$PROGRAM" '{program:$program}'
