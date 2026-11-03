# Storico delle versioni e dei problemi affrontati

| Versione | Lavoro svolto | Motivazione |
|---|---|---|
| 1.1–1.3 | Intent di condivisione Android, parser link Story/Post/Reel e allegati | Condividere direttamente da Instagram |
| 1.4 | Timeout, pulizia dei file incompleti e fallback controllati | Download sospesi o infiniti |
| 1.5 | Video originale distinto dalla miniatura | Evitare il JPG al posto dell'MP4 |
| 1.6 | Ispezione risorse e richieste WebView | Il player non espone un MP4 diretto |
| 1.7 | Blocco dei redirect ad app esterne; landscape | Instagram si riapriva da StoryPocket |
| 1.8 | Lettura JSON embedded con `video_versions` | Ricerca del video nel documento Web |
| 1.9 | Resolver Post/Reel indipendente dalla pagina | Più strategie senza copiare app terze |
| 2.0 | Riconoscimento del contenuto visibile | Falso «Apri una Story, Post o Reel nell'app» |
| 2.1 | Resolver dedicato alla singola Story | Errore «Instagram non ha fornito un media scaricabile» |
| 2.2 | Android `singleTask` + blocco istanze documento | Più finestre create a ogni condivisione |
| 2.3 | Parser multisocial e nuova palette/icona | Espansione oltre Instagram, UI più colorata |

Lo storico è una descrizione delle modifiche, non una garanzia di funzionamento con tutti i link. Per dettagli specifici consultare i `README_v*.md`.

**Versione più recente:** 2.3.0-beta, `versionCode=14`, `applicationId=com.storypocket.mobile`. Un APK compilato e verificato era disponibile sul sito statico StoryPocket; SHA-256 storico: `0eb497336e1432a89829128a13594e039e98b913435aed7ed8a17a04f6371098`. Le nuove build Actions possono avere hash e certificati diversi.
