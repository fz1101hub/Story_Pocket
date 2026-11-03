# StoryPocket Android v1.7 Beta — gestione navigazione Instagram

Questa versione corregge i comportamenti del browser incorporato emersi durante la prova su telefono Android.

## Modifiche
- Blocca **automaticamente** i redirect `intent://`, `instagram://`, `fb://`, `market://`, evitando di riportare l'utente nell'app Instagram durante il tentativo di visualizzare un Reel.
- Consente i link HTTPS di Instagram in WebView. I collegamenti esterni non vengono aperti senza un'azione esplicita dell'utente.
- Disattiva popup e `target=_blank` configurando più finestre senza gestire `onCreateWindow` (secondo la guida Android).
- Include il pulsante **Copia** per trasferire manualmente il collegamento nel browser di sistema.
- Migliora l'interfaccia in orizzontale: header compatto e barra comandi scorrevole, più spazio per il video.
- Gestisce errori di rete e HTTP bloccanti (401/403/429/5xx) nel browser interno e distingue un Reel privo del player da un MP4 non esposto.
- Aggiunti 21 test unitari sulla navigazione e ulteriori controlli statici. Versione Android **1.7.0-beta (versionCode 8)**.

## Limiti reali
- Correggere i redirect **non significa** che il player di Instagram venga reso disponibile nel browser WebView.
- Instagram può non consentire login/riproduzione tramite browser incorporati e può utilizzare streaming a segmenti, DRM o API non pubbliche. In questi casi il download MP4 originale **non è garantito**.
- Il login alla WebView non viene trasferito dalla app ufficiale Instagram, e neppure da Chrome.
- APK `assembleDebug` firmato con chiave di debug, adatto a test personali, non distribuzione Play Store.

## Test e build
```
bash tests/run_preflight.sh
gradle --no-daemon assembleDebug
```

L'APK per test è stato creato su Android SDK 35, AGP 8.7.3, Gradle 8.10.2 e JDK 17. I test unitari e statici **non** sostituiscono un test funzionale su un vero telefono con Instagram.