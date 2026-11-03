# Architettura e flusso di salvataggio

## Componenti
- `MainActivity.java`: Activity unica, WebView, UI, condivisione, download e gestione errori.
- `TargetParser`, `SharedStoryParser`, `SocialLinks`: identificazione e normalizzazione dei contenuti.
- `NavigationPolicy`, `MediaContextPolicy`: navigazione controllata e media effettivamente visibile.
- `InstagramFirstPartyResolver`: ricerca metadati Post/Reel.
- `InstagramStoryResolver`: ricerca del file associato all'ID della Story.
- `PublicSocialResolver`: resolver pubblici per Reddit, Bluesky e Mastodon.
- `MediaTypePolicy`, `VideoUrlPolicy`: selezione e verifica della sorgente per evitare errori di formato.

## Flusso

```text
Condividi un link o un file Android
            |
    Parser e controllo URL
            |
     Piattaforma rilevata
       /               \
   File media         Link HTTPS
   content://            |
       |         WebView + resolver mirati
       |                 |
       |          File accessibile?
       |            sì / no
       |            /     \
       +----- verifica     Errore chiaro
                |
          MediaStore/Download
                |
        Download completato
```

## Principi
1. Non passare la sessione Instagram alle API di altri social.
2. Non avviare app esterne tramite schemi `intent://`, `instagram://`, ecc.
3. Associare sempre l'URL del file al post o alla Story richiesta.
4. Controllare HTTPS, host, MIME, primi byte, lunghezza e redirect.
5. Salvare il file solo a trasferimento ultimato; rimuovere i parziali.
6. Se video e audio arrivano in segmenti diversi, non dichiarare un MP4 completo.
7. Una nuova condivisione aggiorna l'Activity esistente.
