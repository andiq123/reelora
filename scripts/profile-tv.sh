#!/bin/sh
set -eu

TV_SERIAL=${1:?Usage: scripts/profile-tv.sh TV_SERIAL [OUTPUT_DIRECTORY]}
TV_OUTPUT=${2:-work/profile-tv}
SCRIPT_DIRECTORY=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

# Keep one measurement implementation; report tails instead of a median-only gate.
exec python3 "$SCRIPT_DIRECTORY/profile-interactions.py" "$TV_SERIAL" "$TV_OUTPUT" --flow shelves --samples 3
