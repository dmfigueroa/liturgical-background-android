# Liturgical Wallpaper for Android

[![Android CI](https://github.com/dmfigueroa/liturgical-background-android/actions/workflows/android.yml/badge.svg)](https://github.com/dmfigueroa/liturgical-background-android/actions/workflows/android.yml)

A native Android 14+ client that applies user-selected wallpapers from the normalized Colombian liturgical calendar. It uses Kotlin, Jetpack Compose Material 3, Material You dynamic color, `WallpaperManager`, WorkManager, and inexact `AlarmManager` transitions. Android Studio and an emulator are not required.

The development application ID is `com.example.liturgicalwallpaper`; replace `applicationId` and `namespace` before publication.

## Bluefin command-line setup

Bluefin remains immutable. The setup script creates/reuses the Fedora 44 Distrobox `android-dev`, installs OpenJDK 17 and basic archive/network tools there, and installs the Android command-line SDK into the shared `$HOME/Android/Sdk`. It does not install Android Studio or create an AVD.

```bash
chmod +x scripts/*.sh
./scripts/setup-android-dev.sh
```

The script is idempotent. It installs only platform-tools, API 36, and Build Tools 36.0.0 and bootstraps the checked-in Gradle Wrapper. Build commands can remain on the Bluefin host:

```bash
./scripts/build.sh
./scripts/test.sh
./scripts/devices.sh
./scripts/install.sh
./scripts/logcat.sh
```

The equivalent direct device command is:

```bash
distrobox enter android-dev -- adb devices -l
```

No emulator is used. Enable USB debugging or Android wireless debugging on a physical Android 14+ device. If wireless pairing is needed, obtain the address and pairing code from the device, then run (do not guess the code):

```bash
./scripts/box.sh adb pair DEVICE_IP:PAIRING_PORT
./scripts/box.sh adb connect DEVICE_IP:DEBUG_PORT
```

## API configuration

The centralized default endpoint is `https://liturgical-color.dmfigueroa.com/v1/today`. Forward port 3000 when overriding it with a local development server:

```bash
./scripts/box.sh adb reverse tcp:3000 tcp:3000
```

Override the centralized URL at build time using either an environment variable or Gradle property:

```bash
LITURGICAL_API_URL=http://127.0.0.1:3000/v1/today ./scripts/build.sh
./scripts/box.sh ./gradlew assembleDebug -PLITURGICAL_API_URL=http://127.0.0.1:3000/v1/today
```

Release uses the production URL above. Any release override must use HTTPS:

```bash
LITURGICAL_API_URL=https://calendar.example.invalid/v1/today \
  ./scripts/box.sh ./gradlew assembleRelease
```

Cleartext traffic is enabled only in debug. The APK contains no API secret.

## Architecture

```text
normalized web service
        |
CalendarApiClient (GET /v1/today + ETag)
        |
CalendarRepository
        |
atomic private JSON cache
        |
LiturgicalStateCalculator (America/Bogota)
        |
TransitionScheduler
        |
WallpaperCoordinator / WallpaperService
        |
Android WallpaperManager
```

WorkManager refreshes the network calendar roughly every 24 hours while automatic mode is enabled. AlarmManager schedules only the next local First Vespers or midnight boundary with a 15-minute inexact window. Exact alarm permission is intentionally absent: timely correction is required, not second-level precision. Startup, reboot, clock changes, and periodic sync are additional recovery points.

The `/v1/today` today/tomorrow payload and wallpaper files are separate from DataStore preferences. Network failure never replaces a valid cache, and network access is not involved in effective-day calculation. Unknown colors map only to the configured Unknown wallpaper; a missing normal color does nothing and is reported in the UI.

## Testing

`./scripts/test.sh` runs JVM tests, Android lint, and creates the debug APK. Pure JVM coverage includes First Vespers boundaries, calendar date boundaries, custom transition time, every normalized color, JSON cache validation, failed/304/updated refreshes, ETag request behavior, wallpaper mapping, and next-alarm calculations.

A small instrumentation smoke test is included but is not run by CI and requires a physical device:

```bash
./scripts/box.sh ./gradlew connectedDebugAndroidTest
```

Debug builds expose controls for 17:59, 18:00, 18:01, midnight, and immediate transition handling. These controls are compiled out of release behavior. Manual `Apply now` intentionally changes the selected device wallpaper; automated tests do not.

The generated APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Releases

Every push and pull request runs the JVM tests, Android lint, and a debug build. Pushing a version tag such as `v0.1.0` additionally builds and verifies a signed release APK, then publishes it to GitHub Releases with generated release notes.

Release signing uses the `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD` secrets in the protected `release` GitHub environment. Tagged releases require maintainer approval before the job can access them. The signing key is never stored in this repository.
