#!/usr/bin/env python3
"""Capture real Compose widget fixtures on a local emulator (debug APK required)."""
import argparse
from pathlib import Path
import subprocess
import time

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('serial')
parser.add_argument('--output', type=Path, default=Path('/tmp/reelora-widget-previews'))
args = parser.parse_args()


def adb(*command):
    return subprocess.check_output(['adb', '-s', args.serial, *command], text=True)


assert adb('shell', 'getprop', 'ro.kernel.qemu').strip() == '1', 'Use a local emulator, not a physical TV'
args.output.mkdir(parents=True, exist_ok=True)
original_scale = adb('shell', 'settings', 'get', 'system', 'font_scale').strip()
try:
    for scenario in ['ready', 'photo', 'live', 'long', 'loading', 'offline', 'stale', 'night', 'rain', 'snow', 'bright', 'large-romanian']:
        large = scenario == 'large-romanian'
        adb('shell', 'settings', 'put', 'system', 'font_scale', '1.3' if large else '1.0')
        adb('shell', 'am', 'start', '-S', '-W', '-n', 'tv.reelora.app/.WidgetPreviewActivity',
            '--es', 'scenario', 'long' if large else scenario, '--ez', 'romanian', str(large).lower())
        time.sleep(.7)
        with (args.output / f'{scenario}.png').open('wb') as image:
            subprocess.run(['adb', '-s', args.serial, 'exec-out', 'screencap', '-p'], stdout=image, check=True)
        print(f'Captured {scenario}', flush=True)
finally:
    if original_scale == 'null':
        adb('shell', 'settings', 'delete', 'system', 'font_scale')
    else:
        adb('shell', 'settings', 'put', 'system', 'font_scale', original_scale)
