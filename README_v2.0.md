# StoryPocket v2.0 Beta — Fix selezione media dal browser interno

Quando Instagram usa la navigazione SPA, il link restituito dalla WebView puo rimanere sulla home/reels anche con il Reel attivo. Questa versione identifica il media nella pagina via JS limitato al DOM: prima l’URL effettivo, poi un unico player visibile con relativo link, oppure un’unica Story/Reel/Post in dialog. Nessuna scelta casuale di un Post suggerito.

I pulsanti Salva foto/video e Recupera VIDEO originale usano lo stesso contesto; quando manca un link univoco indicano di aprire il contenuto a schermo intero o incollare l’URL. Viene inoltre invalidato il timeout da 45 secondi dopo un download completato.

Il download Instagram resta sperimentale: non tutte le pagine contengono MP4 recuperabili.