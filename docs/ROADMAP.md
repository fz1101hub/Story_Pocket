# Roadmap StoryPocket

## Priorità 0 — stabilità
- Preservare `singleTask` e le condivisioni ripetute da Instagram.
- Testare aggiornamenti Android senza perdere sessioni e dati.
- Configurare firma release stabile tramite GitHub Secrets, senza includere chiavi nel repository.
- Ridurre regressioni del flusso Instagram già usato dall'utente.

## Priorità 1 — affidabilità downloader
- Test reali per Story foto/video, Reel, Post carosello, media Reddit, immagini Bluesky e allegati Mastodon.
- Errori distinti per link errato, login, HTTP 403/429, media non disponibile, file incompleto e segmenti streaming.
- Miglior supporto a video e audio separati, dove permesso e tecnicamente fattibile.
- Verifica che il file salvato sia riproducibile e con audio.

## Priorità 2 — interfaccia
- Conservare il nome **StoryPocket**.
- Evolvere la palette viola/fucsia/arancione verso una UI più pulita e professionale.
- Migliorare icona adattiva, schermate dei download, cronologia e messaggi di stato.
- Aggiungere tema scuro solo dopo aver stabilizzato quello chiaro.

## Priorità 3 — espansione
- Aggiungere resolver soltanto quando verificabili e compatibili con privacy e policy.
- Documentare apertamente per ogni piattaforma differenza tra riconoscimento, anteprima e download.
- Distribuire APK tramite GitHub Actions e, successivamente, Release con firma stabile.
