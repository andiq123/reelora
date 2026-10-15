#!/usr/bin/env python3
"""Sample warm TV interactions with gfxinfo; requires movie Home and a loaded hero.

Leaves Reelora at Home, preserves preferences, and never pauses another app's media.
Run the same release, flow, samples, and playback conditions for comparisons.
"""
import argparse
import csv
import io
import json
import math
from pathlib import Path
import re
import subprocess
import time

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('serial')
parser.add_argument('output', type=Path)
parser.add_argument('--flow', choices=['shelves', 'dialogs', 'details'], required=True)
parser.add_argument('--samples', type=int, default=3)
args = parser.parse_args()
if not 1 <= args.samples <= 10:
    parser.error('--samples must be between 1 and 10')
args.output.mkdir(parents=True, exist_ok=True)


def adb(*command):
    return subprocess.check_output(['adb', '-s', args.serial, *command], text=True)


def key(*keys):
    for value in keys:
        adb('shell', 'input', 'keyevent', 'KEYCODE_' + value)
        time.sleep(.4)


def home():
    key('BACK', 'BACK')
    time.sleep(.4)


def open_details():
    key('DPAD_DOWN')
    adb('shell', 'input', 'keyevent', *(['KEYCODE_DPAD_LEFT'] * 40))
    time.sleep(.6)
    key('DPAD_CENTER')


def exercise():
    if args.flow == 'shelves':
        key('DPAD_DOWN', 'DPAD_RIGHT', 'DPAD_RIGHT', 'DPAD_DOWN', 'DPAD_RIGHT',
            'DPAD_DOWN', 'DPAD_UP', 'DPAD_UP', 'DPAD_LEFT', 'DPAD_UP')
    elif args.flow == 'dialogs':
        for _ in range(3):
            key('MENU', 'DPAD_DOWN', 'DPAD_DOWN', 'DPAD_UP', 'BACK')
    else:
        # Details is already open: top actions -> cast -> similar -> back to top.
        key('DPAD_DOWN', 'DPAD_RIGHT', 'DPAD_RIGHT', 'DPAD_DOWN', 'DPAD_RIGHT',
            'DPAD_UP', 'DPAD_LEFT', 'DPAD_UP')


def summarize(raw):
    frames = []
    for block in raw.split('---PROFILEDATA---')[1::2]:
        reader = csv.DictReader(io.StringIO(block.strip()))
        for row in reader:
            if row.get('Flags') != '0':
                continue
            start, end = int(row['IntendedVsync']), int(row['FrameCompleted'])
            if not 0 < start < end < 2**63 - 1:
                continue
            frames.append(((end - start) / 1e6,
                           (int(row['SyncQueued']) - int(row['HandleInputStart'])) / 1e6))
    def percentiles(values):
        values = sorted(values)
        return {str(p): round(values[min(len(values) - 1, math.ceil(len(values) * p / 100) - 1)], 2)
                for p in (50, 90, 95, 99)} if values else {}
    histogram = re.search(r'Total frames rendered: (\d+)\s+Janky frames: (\d+) \(([^)]+)\)', raw)
    process_ms = {str(p): int(re.search(rf'^{p}th percentile: (\d+)ms', raw, re.MULTILINE)[1])
                  for p in (50, 90, 95, 99)}
    return {'process_frame_ms': process_ms,
            'sampled_frames': len(frames), 'sampled_wall_frame_ms': percentiles([f[0] for f in frames]),
            'ui_work_ms': percentiles([f[1] for f in frames]),
            'over_16_67ms': sum(f[0] > 16.67 for f in frames),
            'over_33_33ms': sum(f[0] > 33.33 for f in frames),
            'gfxinfo_total_and_jank': histogram.groups() if histogram else None}


adb('shell', 'am', 'start', '-n', 'tv.reelora.app/.LauncherEntryActivity')
time.sleep(1)
home()
if args.flow == 'details':
    open_details()
    time.sleep(3)  # Warm metadata and artwork separately from interaction measurement.
exercise()
home()
results = []
try:
    for sample in range(args.samples):
        home()
        if args.flow == 'details':
            open_details()
            time.sleep(1)
        if sample == 0:
            image = subprocess.check_output(['adb', '-s', args.serial, 'exec-out', 'screencap', '-p'])
            (args.output / f'{args.flow}-target.png').write_bytes(image)
        adb('shell', 'dumpsys', 'gfxinfo', 'tv.reelora.app', 'reset')
        exercise()
        time.sleep(.5)
        raw = adb('shell', 'dumpsys', 'gfxinfo', 'tv.reelora.app', 'framestats')
        (args.output / f'{args.flow}-{sample + 1}.txt').write_text(raw)
        result = summarize(raw)
        results.append(result)
        print(json.dumps(result), flush=True)
finally:
    home()
report = {'serial': args.serial, 'flow': args.flow, 'samples': results,
          'note': 'Warm gfxinfo samples, not sustained FPS. Process percentiles cover all windows; '
                  'raw sampled wall times include scheduling, have a bounded ring buffer, '
                  'and exclude windows already closed. '
                  'GPU percentiles are omitted because vendor timestamps can be invalid. '
                  'Background playback and device load are not controlled.'}
(args.output / f'{args.flow}.json').write_text(json.dumps(report, indent=2) + '\n')
