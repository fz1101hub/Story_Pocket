# Sicurezza e privacy

## Garanzie progettuali
- Controllo dello schema HTTPS, hostname e redirect; rifiuto di host falsi come `instagram.com.evil.example`.
- Nessun invio di cookie o password Instagram ai resolver di social diversi.
- WebView con accesso ai file e contenuto locale disabilitato.
- Verifica dei tipi dei file, firma iniziale, dimensione e salvataggio completo.
- Gestione dei timeout e cancellazione dei download incompleti.
- Nessuna promessa di aggirare DRM, account privati o accessi limitati.

## Cosa non pubblicare in un repository pubblico
- Chiavi di firma `*.jks`, `*.keystore`, `*.pem` e password.
- Token GitHub/Instagram, cookie, sessioni web, secret di build.
- `local.properties`, output Gradle, log con dati sensibili, screenshot personali.
- L'XAPK di Media Saver o altri binari proprietari analizzati.
- La chat integrale, messaggi privati o dettagli identificativi non necessari.

**Pubblico non equivale necessariamente a open-source.** Il proprietario potrà scegliere successivamente una licenza (ad esempio MIT o Apache-2.0); nessuna licenza viene presunta.

Usare la funzione di download solo per media che si è autorizzati a conservare e in conformità ai termini delle piattaforme.
