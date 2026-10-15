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

def clock_anchor():
    import re
    for node in tree().iter('node'):
        if re.fullmatch(r'\d{1,2}:\d{2}(?: [AP]M)?', node.get('text', '')):
            bounds = tuple(map(int, re.findall(r'\d+', node.get('bounds', ''))))
            return bounds[2], bounds[1]
    raise AssertionError('The anchored clock disappeared')


adb('shell', 'am', 'start', '-n', 'tv.reelora.app/.LauncherEntryActivity')
time.sleep(.5)
press('BACK')
for _ in range(10):
    if any(n.get('text') == 'Explore' for n in tree().iter('node')):
        break
    time.sleep(.5)
else:
    raise RuntimeError('Open movie Home with an available hero before running this check')
press('BACK')
press(*(['DPAD_LEFT'] * 40))
first_app = focused()
assert any(n.get('focused') == 'true' and n.get('long-clickable') == 'true' for n in tree().iter('node')), 'Dock Left escaped to the hero'
press('DPAD_LEFT', 'DPAD_LEFT')
assert focused() == first_app, 'Dock Left did not stop at its first app'
press(*(['DPAD_RIGHT'] * 40))
assert 'Settings' in focused(), 'The rightmost dock action was not reached'
press('DPAD_RIGHT', 'DPAD_RIGHT')
assert 'Settings' in focused(), 'Dock Right escaped from its last action'
for offset in (0, 3):  # Last action, then last installed app.
    press(*(['DPAD_LEFT'] * offset))
    dock = focused()
    for _ in range(3):
        press('DPAD_UP')
        assert 'Explore' in focused(), 'Dock Up did not reach the hero'
        press('DPAD_DOWN')
        actual = focused()
        assert actual == dock, f'Hero Down lost the dock return target: {dock} -> {actual}'
    print('Dock/hero round trips passed:', dock, flush=True)
stage_clock = clock_anchor()
press('DPAD_DOWN')
assert clock_anchor() == stage_clock, 'Entering discovery moved the Home clock'
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
# Rows keep independent viewports. Move the first row fully left, then scroll
# the second fully right; Up must select a visible card without moving row one.
press(*(['DPAD_LEFT'] * 40))

def visible_movie_cards():
    import re
    result = []
    for node in tree().iter('node'):
        if node.get('focusable') != 'true':
            continue
        if any(child is not node and child.get('focusable') == 'true' for child in node.iter('node')):
            continue  # The mounted navigation container isn't a media card.
        labels = tuple(dict.fromkeys(n.get('text') or n.get('content-desc') for n in node.iter('node')
                       if n.get('text') or n.get('content-desc')))
        if not any(' · ' in label for label in labels):
            continue
        bounds = tuple(map(int, re.findall(r'\d+', node.get('bounds', ''))))
        if len(bounds) != 4 or bounds[0] >= bounds[2] or bounds[1] >= bounds[3]:
            continue
        result.append((labels, bounds))
    return result

before = visible_movie_cards()
assert before, 'The first category did not mount'
first_y = min((bounds[1] + bounds[3]) / 2 for _, bounds in before)
before = [(labels, bounds) for labels, bounds in before
          if abs((bounds[1] + bounds[3]) / 2 - first_y) < 10]

def dock_bounds():
    import re
    for node in tree().iter('node'):
        labels = tuple(dict.fromkeys(n.get('text') or n.get('content-desc') for n in node.iter('node')
                       if n.get('text') or n.get('content-desc')))
        if node.get('focusable') == 'true' and labels == dock:
            return tuple(map(int, re.findall(r'\d+', node.get('bounds', ''))))
    raise AssertionError('The anchored dock disappeared while browsing')

anchored_dock = dock_bounds()
press('DPAD_DOWN')
after_down = visible_movie_cards()
assert after_down and all(abs((bounds[1] + bounds[3]) / 2 - first_y) < 10 for _, bounds in after_down), \
    'Down did not replace one row at the same vertical anchor'
assert not set(labels for labels, _ in before) & set(labels for labels, _ in after_down), \
    'The previous category remained in the fixed row viewport'
assert dock_bounds() == anchored_dock, 'Changing categories moved the dock'
press(*(['DPAD_RIGHT'] * 40))
press('DPAD_UP')
after = visible_movie_cards()
assert all(any(labels == other and abs((bounds[0] + bounds[2]) - (position[0] + position[2])) <= 4
               for other, position in after) for labels, bounds in before), \
    f'Up changed the destination horizontal viewport: {before} -> {after}'
assert focused() in [labels for labels, _ in before], 'Up skipped the visible destination cards'
for _ in range(3):
    adb('shell', 'input', 'keyevent', '20', '20', '19', '19')
    assert any(' · ' in text for text in focused()), 'Rapid reversals lost movie focus'
for _ in range(10):
    press('DPAD_DOWN')
    assert any(' · ' in text for text in focused()), 'Down escaped the movie rows'
print('Anchored dock, single-row viewport, boundaries and rapid reversals passed', flush=True)
press('BACK')
adb('shell', 'rm', '/sdcard/reelora-navigation.xml')
