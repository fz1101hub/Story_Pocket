# StoryPocket v2.3 Beta — Multisocial + UI più colorata

## Cosa cambia

- UI leggera e leggibile con gradiente viola/fucsia/arancione nell'intestazione e nel pulsante Salva.
- Icona ridisegnata con colori più vivi.
- Accetta link HTTPS condivisi da Instagram, TikTok, Facebook, Pinterest, Reddit, Bluesky, Mastodon, Threads, X/Twitter, YouTube, Snapchat, Tumblr, VK, LinkedIn, Vimeo, Dailymotion, Twitch, Telegram, Pixelfed, Imgur.
- Mantiene la modalità `singleTask`: una sola istanza dell'app per tutti i link.
- Mantiene i metodi Instagram per Stories, Post e Reel.
- Bluesky: tenta file immagine originali esposti dall'API pubblica; i video HLS non diventano finti MP4.
- Mastodon: tenta immagini e video dei post pubblici tramite API pubblica dell'istanza riconosciuta.
- Reddit: tenta immagini e MP4 progressivi pubblici; i MP4 fallback possono essere senza audio; API e streaming possono impedire il recupero.
- Gli altri social: riconoscimento, apertura nella WebView per i domini consentiti, screenshot su azione dell'utente e salvataggio dei file multimediali che vengono condivisi direttamente come allegati Android. Non viene promesso un downloader per social che non espongono il file o non autorizzano il download.
- Nessun cookie/password inviato a resolver esterni. Host API e CDN soggetti ad allowlist HTTPS; contenuti verificati tramite magic bytes.

## Limiti e test

Questa è una versione sperimentale: la compilazione non implica che ogni social consenta il download. Non include un servizio di terze parti o aggiramenti di login/DRM. Condividere solo contenuti che è lecito conservare.

Test locali: `bash tests/run_preflight.sh` inclusi 84 nuovi test URL, test regressione navigazione e istanza unica.

## Build Android

JDK 17+, Android SDK API 35, Android Gradle Plugin 8.7.3, Gradle 8.10.2:

```bash
./gradlew assembleDebug
```

Se il wrapper non è incluso usare Gradle 8.10.2. `applicationId`: `com.storypocket.mobile`, `versionCode=14`, `versionName=2.3.0-beta`.