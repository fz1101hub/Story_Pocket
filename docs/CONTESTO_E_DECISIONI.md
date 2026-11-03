# Contesto e decisioni di prodotto

Questo documento seleziona le informazioni tecniche e progettuali utili emerse durante lo sviluppo in chat. Non contiene la trascrizione integrale né dati personali o segreti.

## Requisiti confermati
- **Nome confermato:** StoryPocket, senza cambiare l'identità dell'app.
- **Piattaforma:** Android 10+; uso autonomo da telefono, senza PC né Termux.
- **Flusso principale:** Instagram o altro social → Condividi → StoryPocket → salva il media quando possibile.
- **Browser interno:** i contenuti condivisi sono visualizzati nell'app; non forzare apertura esterna tramite `intent://`.
- **Una sola istanza:** nuove condivisioni devono aggiornare la schermata esistente; confermato funzionante dall'utente con la v2.2.
- **Priorità:** stabilità e download originali, nessuna copertina immagine spacciata per video.
- **UI:** professionale, pulita, ma più vivace; gradiente viola–fucsia–arancione e sfondi chiari.
- **Supporto:** riconoscimento di quanti più social possibile, senza confondere riconoscimento URL con download garantito.
- **Distribuzione:** APK installabile direttamente e alternativa ZIP; repository GitHub pubblico con build GitHub Actions.

## Decisioni tecniche
- `singleTask`, `documentLaunchMode="never"` e `onNewIntent` per una sola Activity.
- Identificazione del media realmente aperto, non solo dell'URL iniziale WebView.
- Parser/risolutori separati per Instagram Story, Post/Reel e API pubbliche di altri social.
- Per le Stories considerare l'ID esatto; vietato scegliere Story suggerite o copertine video.
- Separazione dei cookie di ogni piattaforma. Niente passaggio di password a terze parti.
- HTTP/HTTPS, MIME, firma dei file, dimensione, file parziali e timeout verificati prima del successo.

## Risultati verificati e limiti
La modalità istanza unica della v2.2 è stata confermata funzionante nelle prove dell'utente. Le build fino alla v2.3 sono state compilate, firmate e testate offline. Le capacità di scaricamento da Instagram e altri social dipendono da ciò che le piattaforme rendono effettivamente accessibile; non esiste una garanzia universale.
