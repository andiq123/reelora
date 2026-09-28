#!/usr/bin/env python3
"""Remote navigation regression check. Run with Reelora's movie Home enabled."""
import argparse
import subprocess
import time
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('serial')
args = parser.parse_args()


def adb(*command):
    return subprocess.check_output(['adb', '-s', args.serial, *command], text=True)


def press(*keys):
    for key in keys:
        adb('shell', 'input', 'keyevent', 'KEYCODE_' + key)
        time.sleep(.12)


def tree():
    adb('shell', 'uiautomator', 'dump', '/sdcard/reelora-navigation.xml')
    return ET.fromstring(adb('shell', 'cat', '/sdcard/reelora-navigation.xml'))


def focused():
    root = tree()
    node = next(n for n in root.iter('node') if n.get('focused') == 'true')
    return tuple(dict.fromkeys(n.get('text') or n.get('content-desc') for n in node.iter('node')
                              if n.get('text') or n.get('content-desc')))


adb('shell', 'am', 'start', '-n', 'tv.reelora.app/.LauncherEntryActivity')
for _ in range(10):
    if any(n.get('text') == 'Explore' for n in tree().iter('node')):
        break
    time.sleep(.5)
else:
    raise RuntimeError('Open movie Home with an available hero before running this check')
press('BACK')
press(*(['DPAD_RIGHT'] * 40))
assert 'Settings' in focused(), 'The rightmost dock action was not reached'
for offset in (0, 3):  # Last action, then last installed app.
    press(*(['DPAD_LEFT'] * offset))
    dock = focused()
    for _ in range(3):
        press('DPAD_UP')
        assert 'Explore' in focused(), 'Dock Up did not reach the hero'
        press('DPAD_DOWN')
        assert focused() == dock, 'Hero Down lost the dock return target'
    print('Dock/hero round trips passed:', dock, flush=True)
press('DPAD_DOWN')
press(*(['DPAD_RIGHT'] * 40))
movie = focused()
assert any(' · ' in label for label in movie), 'Movie row was not reached; focus stayed outside a media card'
for _ in range(3):
    press('DPAD_UP', 'DPAD_UP')
    assert 'Explore' in focused(), 'Movie/dock Up did not reach the hero'
    press('DPAD_DOWN')
    assert focused() == dock, 'Movie round trip lost the dock target'
    press('DPAD_DOWN')
    assert focused() == movie, 'Movie round trip lost the last movie'
print('Far-right movie/hero round trips passed:', movie, flush=True)
press('BACK')
adb('shell', 'rm', '/sdcard/reelora-navigation.xml')
