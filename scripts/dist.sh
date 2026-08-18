#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$PROJECT_DIR"
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"

./gradlew :app:assembleRelease
VERSION=$(sed -n 's/^appVersionName=//p' gradle.properties)
APK="dist/ReeloraTV-${VERSION}.apk"
cp app/build/outputs/apk/release/app-release.apk "$APK"
shasum -a 256 "$APK" > "$APK.sha256"

if [ "${1:-}" ]; then
    "$ANDROID_HOME/platform-tools/adb" -s "$1" install -r "$APK"
    "$ANDROID_HOME/platform-tools/adb" -s "$1" shell am start -n tv.reelora.app/.MainActivity
fi

printf '%s\n' "$APK"
