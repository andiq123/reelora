#!/bin/sh
set -eu

TV_SERIAL=${1:?Usage: scripts/profile-tv.sh FIRE_TV_SERIAL}
APP_ID=tv.reelora.app

adb -s "$TV_SERIAL" shell am force-stop "$APP_ID"
adb -s "$TV_SERIAL" shell monkey -p "$APP_ID" -c android.intent.category.LEANBACK_LAUNCHER 1 >/dev/null
sleep 5
adb -s "$TV_SERIAL" shell input keyevent KEYCODE_DPAD_DOWN KEYCODE_DPAD_DOWN
sleep 2
adb -s "$TV_SERIAL" shell dumpsys gfxinfo "$APP_ID" reset >/dev/null
adb -s "$TV_SERIAL" shell 'for key in RIGHT RIGHT RIGHT RIGHT RIGHT LEFT LEFT LEFT LEFT LEFT; do input keyevent KEYCODE_DPAD_$key; sleep 0.25; done'
sleep 2

STATS=$(adb -s "$TV_SERIAL" shell dumpsys gfxinfo "$APP_ID" framestats)
printf '%s\n' "$STATS" | sed -n '/Total frames rendered:/,/Number Frame deadline missed:/p' | head -n 12
MEDIAN=$(printf '%s\n' "$STATS" | sed -n 's/50th percentile: \([0-9][0-9]*\)ms/\1/p' | head -n 1)
[ "$MEDIAN" -le 25 ] || { printf 'Median frame time regressed to %sms\n' "$MEDIAN" >&2; exit 1; }
