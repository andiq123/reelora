#!/usr/bin/env python3
"""Measure Android launch dispatch and activity reuse; not a visible-frame benchmark."""
import argparse
import json
import re
import subprocess
import time

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('serial')
parser.add_argument('other_component', help='Installed app activity to return from, e.g. Netflix')
parser.add_argument('--samples', type=int, default=3)
args = parser.parse_args()
if not 1 <= args.samples <= 20:
    parser.error('--samples must be between 1 and 20')


def adb(*command):
    return subprocess.check_output(['adb', '-s', args.serial, 'shell', *command], text=True)


def launch(category):
    component = adb('cmd', 'package', 'resolve-activity', '--brief', '-a', 'android.intent.action.MAIN',
                    '-c', category, '-p', 'tv.reelora.app').strip().splitlines()[-1]
    if '/' not in component:
        raise RuntimeError('No Reelora entry point: ' + component)
    output = adb('am', 'start', '-W', '-a', 'android.intent.action.MAIN', '-c', category,
                 '-n', component)
    if 'Status: ok' not in output:
        raise RuntimeError(output)
    state = adb('dumpsys', 'activity', 'activities')
    records = sorted(set(re.findall(r'Hist #\d+: ActivityRecord\{(\w+) u\d+ tv\.reelora\.app/\.MainActivity', state)))
    return {'launch': output.strip(), 'activity_records': records}


samples = []
for _ in range(args.samples):
    adb('am', 'force-stop', 'tv.reelora.app')
    cold = launch('android.intent.category.LEANBACK_LAUNCHER')
    time.sleep(2)
    adb('am', 'start', '-W', '-n', args.other_component)
    time.sleep(1)
    returned = launch('android.intent.category.HOME')
    samples.append({'cold': cold, 'return': returned})
    time.sleep(1)
print(json.dumps({'serial': args.serial, 'other_component': args.other_component,
                  'note': 'am start -W timings exclude full content readiness and are not Netflix playback benchmarks.',
                  'samples': samples}, indent=2))
