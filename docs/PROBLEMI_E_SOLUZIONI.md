# Diagnosi: problemi realmente osservati

## Il browser apre di nuovo Instagram
**Sintomo:** la schermata torna all'app Instagram o Android non riesce ad aprire il link. **Intervento v1.7:** bloccare `intent://` e schemi non HTTPS, mantenendo WebView.

## «Il video non espone un URL scaricabile»
La pagina può usare `blob:`, DASH/HLS o segmenti video/audio. Le versioni 1.6–1.9 cercano media nella rete WebView, nel DOM, nei JSON embedded e nei metadati. Se non esiste un file MP4 progressivo accessibile, il video può comunque essere riprodotto senza poter essere scaricato integralmente.

## «Apri una Story, Post o Reel nell'app»
**Intervento v2.0:** rilevare il media effettivamente visibile, non affidarsi soltanto all'URL registrato dalla WebView, che può restare sul feed.

## «Instagram non ha fornito un media scaricabile» con una Story
Il resolver Post/Reel non è adatto alle Stories. **Intervento v2.1:** associare il resolver all'ID esatto della Story, distinguere foto, video e copertina.

## Più StoryPocket aperti per ogni link
**Intervento v2.2:** manifest `singleTask`, `documentLaunchMode=never` e `onNewIntent`; comportamento confermato funzionante nelle prove utente.

## APK da GitHub Actions non si installa sopra il precedente
Le build debug su runner effimeri possono usare un diverso certificato rispetto alle build precedenti. Per aggiornare senza disinstallare servono stessa firma e versionCode crescente. Evitare di pubblicare il keystore in Git.

## Come segnalare un bug
Indicare versione app/Android, servizio, tipo di post, percorso di condivisione, errore esatto e se il player funzionava. Non allegare cookie, token, link privati o screenshot sensibili.
