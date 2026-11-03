# StoryPocket Android 1.5 Beta — video e copertine

Questa versione distingue esplicitamente il **video originale** dall'**immagine di copertina** che Instagram potrebbe mostrare prima della riproduzione. Se il media visibile è un Reel, un video del player o una pagina con metadati video, la ricerca non usa una foto come ripiego. Lo stesso vale quando scegli **↓ Solo VIDEO (MP4)** su una Story o un Post.

L'app cerca indirizzi HTTPS del video nel player visibile o nei metadati della pagina e li scarica solo quando Instagram restituisce un vero file video riconosciuto. Non salva file a partire da `blob:` locali, non aggira privacy o restrizioni e può quindi mostrare un errore quando il filmato non è esposto tramite un link diretto scaricabile.

**Test offline:** 19 scenari del selettore HTML simulato, 48 controlli Java di formato, link e media, 32 controlli statici, 10 test di regressione sui timeout. La compilazione e la firma APK vengono verificate separatamente. **I filmati Instagram reali non sono stati collaudati su un telefono**.

Installa e apri StoryPocket, condividi un Reel con **Instagram → Condividi → StoryPocket** e verifica se salva un `.mp4`. Se viene mostrato «Il video non espone un URL scaricabile», non significa che sia un guasto del download: il player non espone un indirizzo video standard all'app. Puoi anche aprire una Story e premere «Solo VIDEO (MP4)». Il download avviene nella cartella **Download/StoryPocket**.