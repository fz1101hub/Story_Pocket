# StoryPocket v2.1 Beta — Stories

Ripara il percorso di errore che riportava «Instagram non ha fornito un media scaricabile» senza tentare un resolver dedicato alle Stories. Aggiunge un parser che cerca l'ID **esatto** della Story nella pagina e nei dati Instagram, non confondendola con Story vicine. Un video non può essere sostituito da una copertina JPEG. La ricerca dei metadati è limitata a `www.instagram.com` e alla sessione esistente; non sono inviati cookie a servizi terzi.

**Limitazione importante:** l'endpoint Instagram usato non è documentato e può rifiutare le richieste. Le Stories private devono essere effettivamente visibili all'account autorizzato. Nessuna garanzia di download; evitare di eseguire tentativi ripetuti in loop.

La build Android richiede Java 17, Gradle 8.10.2 e Android SDK 35. Test offline in `tests` (incluso `StoryResolverCheck.java`).