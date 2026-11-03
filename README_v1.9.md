# StoryPocket 1.9 beta — esperimento con resolver multipli

## Obiettivo

Ispirazione architetturale tratta dall'analisi **statica** del pacchetto XAPK Media Saver 2.11.1 fornito dall'utente. Nessun suo codice, script, token o chiave è stato incorporato in StoryPocket.

## Cosa cambia rispetto alla 1.8

Quando la ricerca nel browser incorporato non trova un MP4 per un **Post o un Reel** e l'utente preme un pulsante di salvataggio:

1. StoryPocket conserva le strategie già presenti per DOM, dati incorporati e URL media osservati.
2. Esegue **un nuovo tentativo indipendente** leggendo i dati del Post/Reel dal dominio `www.instagram.com`, attraverso una richiesta nativa con timeout.
3. Prova in alternativa la pagina incorporata dello stesso contenuto, sempre sul dominio Instagram.
4. Valida che le URL dei media puntino alle CDN Meta/Instagram consentite. Il salvataggio controlla firma, MIME e dimensioni del file.

La sessione `CookieManager` è usata **solo** per richieste HTTPS a `www.instagram.com`. Non è trasferita ad alcun servizio di terze parti. Non viene creato un backend.

## Limitazioni importanti

- **Non è una replica completa di Media Saver**: quel pacchetto include diversi parser di servizi esterni e un resolver server remoto, mentre la v1.9 aggiunge per ora un metodo indipendente solo di prima parte.
- Le interfacce web di Instagram possono cambiare o rispondere con HTTP 401/403, bloccare il recupero diretto o restituire stream a segmenti non ricomponibili con questo metodo.
- L'integrazione nuova è per **Post e Reel** con shortcode. Le Stories mantengono i tentativi già presenti, senza un nuovo resolver specifico.
- Nessuna garanzia di accesso ai contenuti privati di altri account, né di recupero dei video riprodotti solo via stream protetti.
- Le prove automatizzate non equivalgono al collaudo su un telefono con Instagram reale.

## Prossimo sviluppo per equivalenza funzionale

Un sistema modulare di adapter `MediaResolver` con ordine dei metodi, timeout, diagnostica specifica per ciascun metodo e fallback esterno **solo per contenuti pubblici e previo consenso**. Nessun inoltro di cookie, password o token ad altri siti. Ogni adapter dovrà essere verificato su campioni reali accessibili legittimamente.

## APK

Applicazione Android 10+ con versione `1.9.0-beta`, package `com.storypocket.mobile`. L'APK di debug è per prove personali.