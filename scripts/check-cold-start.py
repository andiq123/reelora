#!/usr/bin/env python3
"""Check cold process starts with installed visible apps (does not reboot or clear data)."""
import argparse
import re
import subprocess
import time
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('serial')
args = parser.parse_args()


def adb(*command):
    return subprocess.check_output(['adb', '-s', args.serial, *command], text=True)


try:
    for _ in range(3):
        adb('shell', 'am', 'force-stop', 'tv.reelora.app')
        adb('shell', 'am', 'start', '-W', '-n', 'tv.reelora.app/.LauncherEntryActivity')
        deadline = time.monotonic() + 20
        while time.monotonic() < deadline:
            adb('shell', 'uiautomator', 'dump', '/sdcard/reelora-startup-check.xml')
            root = ET.fromstring(adb('shell', 'cat', '/sdcard/reelora-startup-check.xml'))
            focused = next((n for n in root.iter('node') if n.get('focused') == 'true'), None)
            if focused is not None:
                labels = {n.get('text') or n.get('content-desc') for n in focused.iter('node')}
                labels.discard('')
                assert not labels.intersection({'Search', 'Hidden', 'Settings', 'Explore'}), labels
                left = int(re.findall(r'\d+', focused.get('bounds'))[0])
                width = int(re.findall(r'\d+', root[0].get('bounds'))[2])
                assert left < width * .15, f'Dock started away from left edge: {left}/{width}'
                print('Cold-start focus:', labels, flush=True)
                break
            time.sleep(.25)
        else:
            raise AssertionError('Dock never received startup focus')
finally:
    adb('shell', 'rm', '-f', '/sdcard/reelora-startup-check.xml')
