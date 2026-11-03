#!/usr/bin/env python3
"""Controlli statici pre-build; non sostituiscono compilazione/emulatore."""
from pathlib import Path
import xml.etree.ElementTree as ET
import re

root = Path(__file__).resolve().parent.parent
manifest_path = root/'app/src/main/AndroidManifest.xml'
java_path = root/'app/src/main/java/com/storypocket/mobile/MainActivity.java'
gradle = (root/'app/build.gradle').read_text()
root_gradle = (root/'build.gradle').read_text()
flow = (root/'.github/workflows/build-apk.yml').read_text()
main = java_path.read_text()
manifest = ET.parse(manifest_path).getroot()
android = '{http://schemas.android.com/apk/res/android}'
count = 0

def test(v, msg):
    global count
    assert v, msg
    count+=1

permissions = [p.get(android+'name') for p in manifest.findall('uses-permission')]
app = manifest.find('application')
activity = app.find('activity')
filters = activity.findall('intent-filter')
actions = [[v.get(android+'name') for v in el.findall('action')] for el in filters]
cats = [[v.get(android+'name') for v in el.findall('category')] for el in filters]

test(permissions == ['android.permission.INTERNET'], 'Permessi non minimali')
test(app.get(android+'allowBackup') == 'false', 'Backup delle sessioni abilitato')
test(app.get(android+'usesCleartextTraffic') == 'false', 'HTTP non cifrato consentito')
test(activity.get(android+'exported') == 'true', 'Activity non esportata per SHARE')
test(activity.get(android+'launchMode') == 'singleTask', 'Nuove condivisioni devono riuti¶»§q«^