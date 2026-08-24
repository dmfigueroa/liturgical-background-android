#!/usr/bin/env bash
set -euo pipefail
"$(dirname "$0")/box.sh" adb logcat --pid="$("$(dirname "$0")/box.sh" adb shell pidof com.example.liturgicalwallpaper)"
