# StoryPocket 1.3 Beta — aggiornamento condivisione

Questa versione aggiunge il riconoscimento di link a Stories (`/stories/utente/ID/`), Post (`/p/CODICE/`), Reel (`/reel/CODICE/`), vecchi video `/tv/`, link brevi `instagram.com/share/p/...`, `instagram.com/share/reel/...`, `instagram.com/share/story/...`, `instagr.am/p/...`, profili e testo che contiene URL Instagram.

Dall'app Instagram premi Condividi e scegli StoryPocket. Se il menu Condividi invia solo una stringa interna senza un URL web, copia il link e incollalo nella casella dentro StoryPocket: questa limitazione viene da come la sorgente condivide i dati. Se Android invia direttamente un'immagine/video tramite `content://`, l'app tenta di salvarlo direttamente nella cartella Download/StoryPocket.

L'app apre Instagram in una WebView e tenta di salvare **un elemento multimediale visibile** nel player. I post con più foto non vengono scaricati tutti automaticamente. La procedura non garantisce il download perché Instagram può esporre URL non accessibili o video tramite blob/sorgenti indirette.

Sicurezza: URL navigabili limitati a domini Instagram, media CDN in allowlist HTTPS; MIME + magic bytes controllati, limite 120 MB. Non implementa bypass di profili privati o accessi non autorizzati. La sessione WebView non coincide con l'app Instagram; potrebbe servire accedere separatamente. Non affiliata a Meta.

## Compilazione

Java 17, Android SDK 35 e Gradle 8.10.2. Esegui `gradle assembleDebug` dalla radice del progetto. APK in `app/build/outputs/apk/debug/app-debug.apk`. La build debug usa una firma di debug, quindi è per test, non Play Store.

## Verifiche eseguite

`bash tests/run_preflight.sh`: parsing URL e condivisione, selettore media simulato, sintassi Java e controlli statici. Un APK che compila non garantisce che la condivisione di Instagram reale funzioni: è necessario provarla su un telefono Android.