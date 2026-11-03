# StoryPocket — Android

**StoryPocket** è un'app Android per ricevere contenuti condivisi dai social network,
aprire il collegamento in un browser interno e salvare i media **quando la piattaforma
rende disponibile un file accessibile e il salvataggio è autorizzato**.

**Versione attuale:** `2.3.0-beta` (`versionCode 14`) · **Android:** 10+ (API 29+) ·
**Lingua:** italiano · **Stato:** beta sperimentale.

> Non è un prodotto ufficiale di Instagram, Meta, TikTok, Reddit o altre piattaforme.
> Il riconoscimento di un link **non** garantisce il download. Usa StoryPocket solo
> per contenuti che hai il diritto di conservare.

## Funzionamento

1. Nel social scegli **Condividi → StoryPocket**, oppure copia il link e incollalo nell'app.
2. StoryPocket riconosce il servizio, apre il contenuto nella WebView interna e prova
   a identificarne la foto o il video.
3. Usa **Salva foto/video** oppure **Recupera VIDEO originale** (dove disponibile).
4. I download completati sono visibili in `Download/StoryPocket`.

Su Android viene usata **una sola istanza** di StoryPocket (`singleTask`): i nuovi
link aggiornano la stessa schermata e non creano nuove voci in Recenti.

## Piattaforme e funzionalità

| Piattaforma | Apertura e riconoscimento link | Recupero media da URL |
| --- | --- | --- |
| **Instagram** | Sì | Tentativi specifici per Stories, Post e Reel; non garantiti |
| **Reddit** | Sì | Media pubblici e alcuni video MP4, a volte senza audio |
| **Bluesky** | Sì | Immagini pubbliche mediante dati/API disponibili |
| **Mastodon** | Sì | Media dei post pubblici delle istanze compatibili |
| TikTok, Facebook, Pinterest, Threads, X | Sì | Nessuna garanzia di estrazione diretta |
| YouTube, Snapchat, Tumblr, Telegram | Sì | Riconoscimento e navigazione; niente downloader universale |
| VK, LinkedIn, Vimeo, Dailymotion, Twitch, Pixelfed, Imgur | Sì | Supporto variabile, solo quando un file è accessibile |

Sono supportati anche i **file foto/video inviati direttamente tramite la
condivisione Android**, indipendentemente dal social di origine.

## Sicurezza e limiti

- Non inserire credenziali in siti di recupero sconosciuti: StoryPocket non invia
  i cookie Instagram ad altri social o a servizi di terze parti.
- URL e download sono soggetti a controlli sugli host, HTTPS, tipo di contenuto
  e firma iniziale del file.
- Non ricostruisce necessariamente stream HLS/DASH, video con audio separato o
  contenuti `blob:`. Non aggira login, DRM o impostazioni private.
- Alcuni media non sono recuperabili e l'app deve indicarlo senza salvare
  una foto di copertina come se fosse il video.

Per le restrizioni dettagliate: [Limiti noti](docs/LIMITI_E_COMPATIBILITA.md) e
[Sicurezza e privacy](docs/SICUREZZA_E_PRIVACY.md).

## UI e branding

**Nome confermato:** StoryPocket. La direzione visiva approvata usa una
palette ispirata ai gradienti social (viola → fucsia → arancione), con
superfici luminose, card pulite e gerarchia chiara. La v2.3 ha introdotto
una prima implementazione colorata e una nuova icona; i mockup in
[`docs/design/`](docs/design/) sono **concept**, non screenshot dell'APK.

## Compilare su GitHub Actions (anche senza PC)

Apri la scheda **Actions** del repository pubblico, seleziona
**Build StoryPocket APK** e premi **Run workflow**. Al termine apri il run
e scarica l'artifact `StoryPocket-debug-APK`: contiene `app-debug.apk`.

Il workflow usa runner `ubuntu-latest`, Java 17, Android SDK 35 e Gradle
8.10.2; esegue test offline, compilazione e verifica della firma.
L'artifact viene distribuito da GitHub, non da un server privato.

**Attenzione alla firma:** una build debug di GitHub Actions può avere un
certificato diverso dall'APK già installato dal nostro sito di download.
Android potrebbe richiedere di disinstallare prima la versione precedente,
perdendo i dati locali. Per aggiornamenti stabili serve configurare una
**chiave di firma release privata tramite GitHub Secrets**: non va mai
pubblicata nel repository.

## Compilazione locale

Requisiti: Java 17, Android SDK API 35 / build-tools 35.0.0,
Gradle **8.10.2**, Android Gradle Plugin **8.7.3**.

```bash
bash tests/run_preflight.sh
gradle --no-daemon assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Questo progetto non include il Gradle wrapper: in CI viene installata la
versione fissata di Gradle.

## Documentazione del progetto

- [Contesto, requisiti e decisioni emersi dallo sviluppo](docs/CONTESTO_E_DECISIONI.md)
- [Storico delle versioni](docs/STORICO_VERSIONI.md)
- [Architettura e flusso download](docs/ARCHITETTURA.md)
- [Risoluzione dei problemi incontrati](docs/PROBLEMI_E_SOLUZIONI.md)
- [Limiti e piattaforme](docs/LIMITI_E_COMPATIBILITA.md)
- [Sicurezza e privacy](docs/SICUREZZA_E_PRIVACY.md)
- [Test su telefono e checklist release](docs/PIANO_TEST.md)
- [Roadmap](docs/ROADMAP.md)
- [Come pubblicare e gestire GitHub Actions](docs/GITHUB_ACTIONS_E_RILASCI.md)
- [Analisi funzionale di riferimento esterno](docs/ANALISI_MEDIA_SAVER.md)

Le vecchie note `README_v*.md` sono conservate come riferimento storico, ma
questo README descrive la versione corrente.

## Distribuzione esistente

Una build **v2.3 Beta** già compilata e verificata è disponibile su:
https://storypocket-apk-download.vercel.app/StoryPocket_v2.3_Beta.apk

Quell'APK e gli artifact Actions **non hanno necessariamente la stessa firma**.
Per informazioni su aggiornamenti e certificati, vedi la guida ai rilasci.

## Licenza

Il codice è pubblicabile come repository GitHub **pubblico**, ma la scelta di
renderlo pubblico non equivale automaticamente a concedere una licenza
open-source. **Non viene concessa una licenza d'uso o ridistribuzione esplicita
finché il proprietario del repository non ne sceglie una.**