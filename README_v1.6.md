# StoryPocket Android 1.6 Beta — video originale

Questa versione sperimentale amplia la ricerca del **file video originale** sulle pagine di Instagram a cui l'utente può accedere. Usa, in ordine, gli URL del video visibile nel DOM, le risorse di rete osservate dal browser e le richieste CDN del WebView. Non registra lo schermo e non tenta di aggirare account privati o autorizzazioni.

## Limiti

Il WebView non fornisce necessariamente un file MP4 originale. Se Instagram usa stream a segmenti (DASH/HLS), blob, API protette o file firmati temporanei non accessibili, StoryPocket segnala il problema e non salva una copertina al posto del video. Non assembla segmenti né ricostruisce file audio/video. Una richiesta HTTP 206 viene rifiutata per evitare file video incompleti. Supporto per video pubblico/autorizzato, senza garanzia di successo.

Il recupero originale non richiede backend, né Python/Termux sul telefono. È una build beta di test, non una release pubblica. I download avvengono in `Download/StoryPocket` solo in caso di trasferimento completo.

## Come testare

1. Condividi un Reel o una Story video dall'app Instagram a StoryPocket, oppure copia il link e aprilo nell'app.
2. Accedi a Instagram nel WebView, se necessario. Riproduci il video fino all'avvio del player.
3. Premi **Recupera VIDEO originale**. L'app cerca un URL HTTPS video completo e valido, prova a scaricarlo e verifica MIME, firma MP4 e dimensione.
4. Se compare l'errore relativo a `blob:`, streaming o segmenti, il contenuto non viene salvato. La funzione non è in grado di convertire uno stream protetto in un MP4 completo.

## Verifiche

`bash tests/run_preflight.sh` esegue test dei collegamenti, validazione del video, JavaScript multimediale, sicurezza e controlli di regressione. La compilazione Android usa `gradle assembleDebug` con Java 17, Gradle 8.10.2 e Android SDK 35.