# Contribuire a StoryPocket

Repository pubblico per lo sviluppo Android di StoryPocket.

Per proporre una modifica:
1. Apri una Issue con un caso verificabile, indicando versione dell'app,
   versione Android e messaggio di errore **senza credenziali o link privati**.
2. Esegui `bash tests/run_preflight.sh` e compila l'APK con Java 17,
   Gradle 8.10.2 e Android SDK 35.
3. Non includere keystore, password, cookie, dati di accesso o XAPK
   proprietari di software terzo.
4. Non dichiarare come «supportato» un downloader che riconosce soltanto
   gli URL senza recuperare media.
5. Conserva il task unico Android e il flusso di condivisione funzionante.
6. Non incorporare servizi remoti di risoluzione senza una valutazione
   esplicita di privacy e condizioni d'uso.

Il repository è pubblico ma non è ancora stata scelta una licenza esplicita.
