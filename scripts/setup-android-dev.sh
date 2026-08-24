#!/usr/bin/env bash
set -euo pipefail

BOX_NAME=android-dev
FEDORA_IMAGE=registry.fedoraproject.org/fedora:44
SDK_DIR="$HOME/Android/Sdk"
JDK_DIR="$HOME/.local/share/android-dev/jdk17"
CMDLINE_VERSION=15859902
CMDLINE_SHA256=4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583
GRADLE_VERSION=8.13

if ! distrobox list 2>/dev/null | grep -Eq "[|[:space:]]${BOX_NAME}[[:space:]]*[|]"; then
  distrobox create --name "$BOX_NAME" --image "$FEDORA_IMAGE" --yes
fi

distrobox enter "$BOX_NAME" -- sudo dnf install -y git curl wget unzip

if [[ ! -x "$JDK_DIR/bin/javac" ]] || [[ "$("$JDK_DIR/bin/javac" -version 2>&1)" != javac\ 17.* ]]; then
  jdk_archive="/tmp/temurin17.tar.gz"
  curl -fL "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse" -o "$jdk_archive"
  jdk_temp="$(mktemp -d)"
  tar -xzf "$jdk_archive" -C "$jdk_temp"
  mkdir -p "$(dirname "$JDK_DIR")"
  rm -rf "$JDK_DIR"
  mv "$jdk_temp"/* "$JDK_DIR"
  rm -rf "$jdk_temp" "$jdk_archive"
fi

export JAVA_HOME="$JDK_DIR"
export PATH="$JAVA_HOME/bin:$PATH"

if [[ ! -x "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" ]]; then
  archive="/tmp/commandlinetools-linux-${CMDLINE_VERSION}_latest.zip"
  curl -fL "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_VERSION}_latest.zip" -o "$archive"
  echo "$CMDLINE_SHA256  $archive" | sha256sum --check
  mkdir -p "$SDK_DIR/cmdline-tools"
  temp_dir="$(mktemp -d)"
  unzip -q "$archive" -d "$temp_dir"
  mkdir -p "$SDK_DIR/cmdline-tools/latest"
  cp -a "$temp_dir/cmdline-tools/." "$SDK_DIR/cmdline-tools/latest/"
  rm -rf "$temp_dir" "$archive"
fi

export ANDROID_HOME="$SDK_DIR"
export ANDROID_SDK_ROOT="$SDK_DIR"
export PATH="$SDK_DIR/cmdline-tools/latest/bin:$SDK_DIR/platform-tools:$PATH"
yes | sdkmanager --licenses >/dev/null || true
sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ ! -f "$project_dir/gradle/wrapper/gradle-wrapper.jar" ]]; then
  gradle_zip="/tmp/gradle-${GRADLE_VERSION}-bin.zip"
  curl -fL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "$gradle_zip"
  temp_gradle="$(mktemp -d)"
  unzip -q "$gradle_zip" -d "$temp_gradle"
  "$temp_gradle/gradle-${GRADLE_VERSION}/bin/gradle" -p "$project_dir" wrapper --gradle-version "$GRADLE_VERSION" --distribution-type bin
  rm -rf "$temp_gradle" "$gradle_zip"
fi

printf 'Android CLI environment ready in %s.\n' "$BOX_NAME"
