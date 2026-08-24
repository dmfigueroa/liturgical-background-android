#!/usr/bin/env bash
set -euo pipefail
"$(dirname "$0")/box.sh" adb devices -l
