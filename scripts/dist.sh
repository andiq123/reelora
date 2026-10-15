#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$PROJECT_DIR"
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"

./gradlew :app:assembleRelease
VERSION=$(sed -n 's/^appVersionName=//p' gradle.properties)
VERSION_CODE=$(sed -n 's/^appVersionCode=//p' gradle.properties)
APK="dist/ReeloraTV-${VERSION}.apk"
mkdir -p dist
cp app/build/outputs/apk/release/app-release.apk "$APK"

# Inspect the built artifact, rather than relying on its filename or signing label.
python3 - "$APK" "$ANDROID_HOME" "$VERSION" "$VERSION_CODE" <<'PY'
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile

apk, sdk, version, code = sys.argv[1:]
sdk = Path(sdk)
manifest = ET.fromstring(subprocess.check_output([
    str(sdk / 'cmdline-tools/latest/bin/apkanalyzer'), 'manifest', 'print', apk]))
android = '{http://schemas.android.com/apk/res/android}'
def require(condition, message):
    if not condition:
        raise SystemExit(message)
require(manifest.get('package') == 'tv.reelora.app', 'Unexpected package')
require(manifest.get(android + 'versionName') == version, 'Stale version name')
require(manifest.get(android + 'versionCode') == code, 'Stale version code')
application = manifest.find('application')
require(application.get(android + 'debuggable', 'false') == 'false', 'Debug APK rejected')
require(application.get(android + 'testOnly', 'false') == 'false', 'Test APK rejected')
require(all('WidgetPreviewActivity' not in node.get(android + 'name', '') for node in application), 'Preview activity in release')
require(Path('app/build/outputs/mapping/release/mapping.txt').stat().st_size > 0, 'Missing R8 mapping')
with zipfile.ZipFile(apk) as archive:
    require('assets/dexopt/baseline.prof' in archive.namelist(), 'Missing ART profile')
tools = sorted((sdk / 'build-tools').iterdir(), key=lambda path: [int(n) for n in re.findall(r'\d+', path.name)])
subprocess.run([str(tools[-1] / 'apksigner'), 'verify', apk], check=True)
print(f'Verified release {version} ({code}): debugging disabled, R8 mapping and ART profile present, valid signature')
PY
shasum -a 256 "$APK" > "$APK.sha256"
gzip -c app/build/outputs/mapping/release/mapping.txt > "${APK%.apk}.mapping.txt.gz"

if [ "${1:-}" ]; then
    "$ANDROID_HOME/platform-tools/adb" -s "$1" install -r "$APK"
    REELORA_DEVICE_APK=$("$ANDROID_HOME/platform-tools/adb" -s "$1" shell pm path tv.reelora.app | tr -d '\r' | sed -n 's/^package://p' | head -n 1)
    test -n "$REELORA_DEVICE_APK"
    REELORA_INSTALLED_APK=$(mktemp "${TMPDIR:-/tmp}/reelora-installed.XXXXXX")
    trap 'rm -f "$REELORA_INSTALLED_APK"' EXIT HUP INT TERM
    "$ANDROID_HOME/platform-tools/adb" -s "$1" pull "$REELORA_DEVICE_APK" "$REELORA_INSTALLED_APK" >/dev/null
    cmp "$APK" "$REELORA_INSTALLED_APK"
    printf '%s\n' 'Installed APK matches the verified release byte for byte'
    "$ANDROID_HOME/platform-tools/adb" -s "$1" shell am start -n tv.reelora.app/.LauncherEntryActivity
fi

printf '%s\n' "$APK"
