# Piano di test e checklist release

## Automatici
`bash tests/run_preflight.sh`, build Android Gradle 8.10.2 su Java 17 e Android SDK 35, verifica firma APK. I test in CI sono simulazioni e non sostituiscono il test con le piattaforme vere.

## Test Android da ripetere prima di un rilascio
- [ ] Installazione/aggiornamento su Android 10 o successivo.
- [ ] Condivisione consecutiva di almeno 3 link, verificando **una sola istanza**.
- [ ] Apertura del browser interno senza passare all'app Instagram esterna.
- [ ] Download Story foto e Story video corretto; evitare la copertina se si chiede un video.
- [ ] Post carosello e Reel: non confondere il contenuto con suggerimenti vicini.
- [ ] Reddit: controllo audio nel file video.
- [ ] Bluesky/Mastodon: media appartenente al post corretto.
- [ ] TikTok, Facebook, Pinterest, X, Threads: apertura dei link senza pretendere download universale.
- [ ] Allegati ricevuti da Android via `content://`.
- [ ] Annullamento del download e rimozione di file parziali.
- [ ] Interfaccia in verticale e orizzontale, contrasto e leggibilità.
- [ ] Download visibile nella cartella prevista.

Per bug: annotare versione app, Android, piattaforma, tipo media, messaggio esatto e percorso di condivisione, **senza allegare cookie o dati privati**.
