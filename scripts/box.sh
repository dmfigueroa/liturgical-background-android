#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
sdk_dir="$HOME/Android/Sdk"
jdk_dir="$HOME/.local/share/android-dev/jdk17"
exec distrobox enter android-dev -- bash -lc "export JAVA_HOME='$jdk_dir' ANDROID_HOME='$sdk_dir' ANDROID_SDK_ROOT='$sdk_dir'; export PATH='$jdk_dir/bin:$sdk_dir/cmdline-tools/latest/bin:$sdk_dir/platform-tools:/usr/local/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/sbin:/bin'; cd '$project_dir'; exec \"\$@\"" bash "$@"
