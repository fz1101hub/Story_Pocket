# StoryPocket v1.8 Beta

- Aggiunge la ricerca di `video_versions` nei dati JSON già presenti nei tag `script` della pagina Instagram caricata.
- Verifica il codice identificativo del Reel/Post per non scaricare altri contenuti suggeriti.
- Conserva i controlli di sicurezza su host HTTPS, file MP4, dimensione, timer e firma del media.
- Nessuna richiesta a nuove API, nessuna esportazione di cookie, sessioni o credenziali.
- I video distribuiti soltanto in segmenti o `blob:` senza progressive URL possono ancora non essere recuperabili.
- Verifiche offline: test Java, policy, MediaProbeCheck.js con casi JSON validi/errati, verifiche manifest e timeout.