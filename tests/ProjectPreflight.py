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
test(activity.get(android+'launchMode') == 'singleTask', 'Nuove condivisioni devono riutilizzare il task esistente')
test(activity.get(android+'documentLaunchMode') == 'never', 'Nessuna istanza per documenti multipli')
test(any('android.intent.action.MAIN' in a for a in actions), 'Manca launcher')
test(any('android.intent.action.SEND' in a for a in actions), 'Manca ricezione ACTION_SEND')
test(any('android.intent.action.VIEW' in a for a in actions), 'Manca deep link VIEW')
test(all('android.intent.category.DEFAULT' in c for i,c in enumerate(cats) if 'android.intent.action.SEND' in actions[i]), 'Categoria DEFAULT mancante')
test(any(any(d.get(android+'mimeType','') in ['text/*','text/plain'] for d in f.findall('data')) for f,a in zip(filters,actions) if 'android.intent.action.SEND' in a), 'MIME del link non riconosciuto')
test(all(d.get(android+'scheme')=='https' for f,a in zip(filters,actions) if 'android.intent.action.VIEW' in a for d in f.findall('data')), 'Deep link non HTTPS')
test(all(d.get(android+'host') in ['instagram.com','www.instagram.com','www.reddit.com','bsky.app','mastodon.social','www.pinterest.com','www.tiktok.com'] for f,a in zip(filters,actions) if 'android.intent.action.VIEW' in a for d in f.findall('data')), 'Host inatteso nel deep link')
test('setAllowFileAccess(false)' in main, 'Accesso ai file WebView abilitato')
test('setAllowContentAccess(false)' in main, 'Accesso a content provider WebView abilitato')
test('MIXED_CONTENT_NEVER_ALLOW' in main, 'Mixed content ammesso')
test('addJavascriptInterface(' not in main, 'Interfaccia JS->Android abilitata')
test('onNewIntent(Intent intent)' in main, 'Manca gestore condivisioni successive')
test('Intent.EXTRA_TEXT' in main, 'Manca estrazione di testo condiviso')
test('MediaTypePolicy.hasValidSignature' in main, 'Manca verifica contenuto del file')
test('MAX_FILE_BYTES' in main, 'Manca limite dimensione file')
test('setInstanceFollowRedirects(false)' in main, 'Redirect automatici non filtrati')
test('TargetParser.isTrustedMediaUrl(currentUrl)' in main, 'Redirect host non filtrati')
test('MediaStore.Downloads.EXTERNAL_CONTENT_URI' in main, 'Non usa MediaStore per Download')
test('MediaStore.MediaColumns.IS_PENDING' in main, 'File parziali visibili durante scaricamento')
test('getContentResolver().delete(pending' in main, 'Manca pulizia download parziali')
test("minSdk 29" in gradle and "targetSdk 35" in gradle, 'Versione Android incoerente')
test('JavaVersion.VERSION_17' in gradle, 'Java di progetto non 17')
test("version '8.7.3'" in root_gradle, 'AGP non prevista')
test('assembleDebug' in flow, 'Pipeline non compila APK')
test('actions/upload-artifact' in flow, 'Pipeline non pubblica APK')
test('app/build/outputs/apk/debug/app-debug.apk' in flow, 'Percorso APK errato')
test((root/'app/src/main/res/drawable/ic_storypocket.xml').exists(), 'Icona mancante')
test('setSupportMultipleWindows(true)' in main, 'Pop-up di Instagram non bloccati')
test('NavigationPolicy.decide(url' in main, 'Filtro navigazione non applicato')
test('startActivity(new Intent(Intent.ACTION_VIEW' not in main, 'Reindirizzamento app Instagram non bloccato')
test('footerScroll' in main and 'landscape' in main, 'Correzione layout orizzontale mancante')
test('onReceivedError(WebView view' in main, 'Gestione errori WebView mancante')
print(f'OK: {count} controlli statici su manifest, sicurezza e pipeline Android.')
