# GitHub Actions e rilasci APK

## Compilazione dal telefono
1. Apri la scheda **Actions** di `fz1101hub/Story_Pocket`.
2. Seleziona **Build StoryPocket APK**.
3. Premi **Run workflow** sulla branch `main` (oppure usa una build automatica del push).
4. Attendi il completamento del job Android e scarica l'artifact **StoryPocket-debug-APK**.
5. Estrai `app-debug.apk` e installalo sul telefono, verificando prima la compatibilità della firma.

Runner usato: `ubuntu-latest` con Java 17, Android SDK 35, build-tools 35 e Gradle 8.10.2. Le Actions su runner standard nei repository pubblici generalmente non consumano i minuti mensili previsti per i privati, ma restano quote, limiti di durata e concorrenza.

## Firma Android: attenzione
Il certificato dell'APK di debug creato da GitHub Actions può **essere diverso** da quello utilizzato nelle precedenti APK. Android può quindi impedire l'aggiornamento diretto sopra la versione installata. Non pubblicare mai `debug.keystore` nel repo per risolvere questo problema. Una firma release stabile va gestita mediante secrets e workflow dedicato.

## Provenienza build esistente
La precedente v2.3 Beta era scaricabile da:
https://storypocket-apk-download.vercel.app/StoryPocket_v2.3_Beta.apk

SHA-256 storico:
`0eb497336e1432a89829128a13594e039e98b913435aed7ed8a17a04f6371098`.
Il nuovo APK GitHub Actions non avrà necessariamente lo stesso hash.
