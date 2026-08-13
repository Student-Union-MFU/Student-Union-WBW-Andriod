#!/usr/bin/env bash
# One-command dev run, tuned for this 15.8GB machine so it does NOT freeze:
#   build the APK FIRST (emulator off) -> free Gradle daemons -> boot emulator lean
#   -> install with `adb` (NOT `gradlew installDebug`, which would run a 2GB Gradle
#   daemon alongside the emulator and exhaust RAM) -> launch.
# Usage: ./run.sh
set -euo pipefail

export ANDROID_HOME="${ANDROID_HOME:-/home/yion/Android/Sdk}"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk}"

ADB="$ANDROID_HOME/platform-tools/adb"
EMULATOR="$ANDROID_HOME/emulator/emulator"
AVD="Pixel_7"
APP="th.ac.mfu.su.wbw"
ACTIVITY="$APP/.MainActivity"
APK="app/build/outputs/apk/debug/app-debug.apk"

cd "$(dirname "$0")"

# 1. Build the APK while NO emulator is running (Gradle + emulator together exhaust
#    RAM on this box). assembleDebug needs no device.
echo "==> Building APK (emulator not started yet)…"
./gradlew :app:assembleDebug

# 2. Stop the Gradle/Kotlin daemons so they don't compete with the emulator for RAM.
echo "==> Freeing Gradle daemons…"
./gradlew --stop >/dev/null 2>&1 || true

# 3. Boot the emulator only if none is already connected.
if ! "$ADB" devices | grep -qw "device"; then
  echo "==> Booting emulator ($AVD)…"
  nohup "$EMULATOR" -avd "$AVD" -gpu host -no-snapshot -no-boot-anim >/tmp/wbw-emulator.log 2>&1 &
  "$ADB" wait-for-device
  echo "==> Waiting for boot to complete…"
  until [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
  echo "==> Emulator ready."
else
  echo "==> Emulator already running."
fi

# 4. Install with adb (NOT gradlew installDebug) — negligible memory, no daemon.
echo "==> Installing APK via adb…"
"$ADB" install -r "$APK"

# 5. Launch.
echo "==> Launching $APP…"
"$ADB" shell am start -n "$ACTIVITY" >/dev/null
echo "==> Done. App is on screen."
