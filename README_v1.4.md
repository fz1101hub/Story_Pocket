# StoryPocket 1.4 beta – correzione download bloccato

- Corregge il ciclo infinito quando il player WebView non risponde o una CDN invia dati indefinitamente.
- Ricerca automatica: massimo 5 tentativi e massimo 25 secondi complessivi.
- Ricerca manuale: scadenza dopo 6 secondi se lo script non risponde.
- Download: massimo 45 secondi complessivi, timeout di connessione/lettura 8 secondi.
- Pulsante **Annulla operazione** per interrompere i tentativi e la connessione attiva.
- Mostra i KB ricevuti durante il download; pulisce i file parziali.
- Mantiene il supporto a Stories, Post, Reel e contenuti `content://` condivisi da Android.

Questi limiti sono pensati per evitare schermate bloccate, NON per superare le restrizioni di Instagram. L'accesso ai media resta sperimentale. L'APK di debug può richiedere disinstallazione della versione precedente se la chiave di firma cambia. Il test su Instagram reale non è stato eseguito.