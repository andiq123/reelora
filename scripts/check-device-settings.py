#!/usr/bin/env python3
"""Check Device settings task isolation and Back on a TV with English Reelora UI.

Start on Reelora Home with its Settings dialog closed and SYSTEM not selected.
"""
import argparse
import subprocess
import time
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('serial')
args = parser.parse_args()


def adb(*args_):
    return subprocess.check_output(['adb', '-s', args.serial, *args_], text=True)


def press(*keys):
    for key in keys:
        adb('shell', 'input', 'keyevent', 'KEYCODE_' + key)
        time.sleep(.25)


def labels():
    adb('shell', 'uiautomator', 'dump', '/sdcard/reelora-settings-check.xml')
    root = ET.fromstring(adb('shell', 'cat', '/sdcard/reelora-settings-check.xml'))
    return {n.get('text') for n in root.iter('node')}


def resumed():
    return next(line for line in adb('shell', 'dumpsys', 'activity', 'activities').splitlines()
                if 'ResumedActivity:' in line)


try:
    press('MENU', 'DPAD_DOWN', 'DPAD_DOWN', 'DPAD_DOWN', 'DPAD_CENTER', 'DPAD_RIGHT')
    assert 'Device settings' in labels(), 'SYSTEM section was not reached'
    for _ in range(2):
        press('DPAD_CENTER')
        time.sleep(1)
        native = resumed()
        assert 'tv.reelora.app/' not in native, 'Device settings returned to Reelora'
        state = adb('shell', 'dumpsys', 'activity', 'activities')
        top = state.split('* Hist')[1]
        assert 'mActivityType=standard' in top, 'Settings joined the Home task'
        press('DPAD_DOWN', 'DPAD_UP')
        assert resumed() == native, 'Native settings lost focus during navigation'
        for _ in range(6):
            press('BACK')
            if 'tv.reelora.app/' in resumed():
                break
        assert 'tv.reelora.app/' in resumed(), 'Back did not return to Reelora'
        assert 'Device settings' in labels(), 'Reelora did not preserve its Settings screen'
    print('Native settings stayed in a separate task; navigation and Back passed twice.')
    press('BACK')
finally:
    adb('shell', 'rm', '-f', '/sdcard/reelora-settings-check.xml')
