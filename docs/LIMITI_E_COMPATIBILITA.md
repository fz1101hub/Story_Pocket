# Limiti e compatibilità v2.3

| Servizio | Riconoscimento link | Download da URL |
|---|---|---|
| Instagram | Stories, Post e Reel | Diversi tentativi di recupero; non sempre disponibili |
| Bluesky | Post | Immagini e dati pubblici compatibili |
| Mastodon | Post | Allegati pubblici tramite API dell'istanza |
| Reddit | Post | Alcuni MP4 e immagini pubbliche; audio non sempre incluso |
| TikTok, Facebook, Pinterest, Threads, X | Sì | Nessun download universale garantito |
| YouTube, Snapchat, Tumblr, Telegram | Sì | Apertura; eventuali allegati condivisi direttamente |
| VK, LinkedIn, Vimeo, Dailymotion, Twitch, Pixelfed, Imgur | Sì | Supporto variabile, solo se un media è esposto |

**Riconosciuto** significa che l'app identifica la piattaforma, non che possa scaricare ogni file. **Resolver pubblico** significa che si tenta il recupero di un URL accessibile senza credenziali aggiuntive.

Limitazioni note: pagine con autenticazione, permessi privati, video `blob:`, stream HLS/DASH, audio/video separati, rate limiting e modifiche delle piattaforme. Non aggirare DRM, blocchi d'accesso o diritti d'autore.

I file foto/video condivisi direttamente con Android tramite `content://` possono essere salvati indipendentemente dall'origine, se il dispositivo concede l'accesso.
