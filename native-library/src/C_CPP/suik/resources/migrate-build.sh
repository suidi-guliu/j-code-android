#!/bin/bash
# Migrate stale build caches created by v0.4's temporary /sui-quickrun bind.
# This script is sourced with BUILD and PROJECT. Preserve the complete old directory for recovery.
if [ -r "$BUILD/CMakeCache.txt" ]; then
    OLD=$(sed -n 's/^CMAKE_HOME_DIRECTORY:INTERNAL=//p' "$BUILD/CMakeCache.txt")
    if [ -n "$OLD" ] && [ "$OLD" != "$PROJECT" ]; then
        BACKUP="$BUILD.pre-v05-$(date +%s)-$$"
        printf 'Build path changed (%s → %s). Retaining old cache at %s\n' "$OLD" "$PROJECT" "$BACKUP" >&2
        mv -- "$BUILD" "$BACKUP"
    fi
fi
