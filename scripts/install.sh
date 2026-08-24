#!/usr/bin/env bash
set -euo pipefail
"$(dirname "$0")/build.sh"
"$(dirname "$0")/box.sh" adb install -r app/build/outputs/apk/debug/app-debug.apk
