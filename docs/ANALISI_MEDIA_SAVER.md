# Architettura di riferimento: Media Saver

Durante lo sviluppo l'utente ha fornito l'XAPK di **Media Saver 2.11.1** per mostrare il comportamento desiderato. È stato analizzato staticamente come riferimento architetturale: **il codice proprietario e l'XAPK non vengono pubblicati**.

## Osservazioni utili
- Diversi resolver con fallback e timeout invece di dipendere esclusivamente dal browser.
- Distinzione tra Story, Post, Reel e contenuti multipli.
- Parser indipendenti dal DOM e possibili servizi remoti.
- Validazione dei risultati per evitare di scaricare media non corrispondenti.

## Applicazione indipendente a StoryPocket
- v1.6–1.8: analisi delle risorse del player e JSON visibili.
- v1.9: resolver Post/Reel nei dati Instagram.
- v2.1: resolver dedicato alle Stories per ID.
- v2.3: resolver pubblici separati per Bluesky, Reddit e Mastodon.

Non copiare script, API private, chiavi o proprietà intellettuale dell'app di confronto. Evitare di spedire cookie o password a servizi esterni.
