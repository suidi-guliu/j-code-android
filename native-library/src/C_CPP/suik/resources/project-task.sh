#!/bin/bash
set -euo pipefail
ACTION=$1
PROJECT=$2
[ -f "$PROJECT/CMakeLists.txt" ] || { echo 'Create or open a CMake project before Build/Test. Single files run through C/C++ Basic Plugin.' >&2; exit 64; }
CC=/usr/bin/gcc
CXX=/usr/bin/g++
if [ -r /opt/sui-code/c.env ]; then . /opt/sui-code/c.env; CC=$SUI_C; fi
if [ -r /opt/sui-code/cpp.env ]; then . /opt/sui-code/cpp.env; CXX=$SUI_CPP; fi
[ -x "$CC" ] && [ -x "$CXX" ] || { echo 'Configured GCC/G++ cannot run' >&2; exit 78; }
BUILD="$PROJECT/.sui-code/build"
. "$(dirname "$0")/migrate-build.sh"
cmake -DCMAKE_EXPORT_COMPILE_COMMANDS=ON -DCMAKE_C_COMPILER="$CC" -DCMAKE_CXX_COMPILER="$CXX" -S "$PROJECT" -B "$PROJECT/.sui-code/build" -G Ninja -DCMAKE_BUILD_TYPE=Debug
cmake --build "$PROJECT/.sui-code/build"
if [ "$ACTION" = test ]; then ctest --test-dir "$PROJECT/.sui-code/build" --output-on-failure; fi

if [ "$ACTION" = run ]; then exec "$PROJECT/.sui-code/build/app"; fi
