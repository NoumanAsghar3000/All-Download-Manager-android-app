#!/bin/sh
set -e
if command -v gradle >/dev/null 2>&1; then exec gradle "$@"; fi
echo "Gradle is required. Open android/ in Android Studio or install Gradle 8.9+." >&2
exit 1