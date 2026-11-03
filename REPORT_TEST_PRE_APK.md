# StoryPocket Android 1.2 — Rapporto test pre-compilazione

Data: 1 novembre 2026. Verifica effettuata sul codice sorgente senza Android SDK, emulatore o account Instagram.

## Risultati: PASS

- Test originali del parser profili/permalink e dei testi condivisi da Android: superati.
- 43 verifiche aggiuntive Java: percorsi abusivi, domini falsi, redirect/CDN autorizzati, URL locali, formati e firme dei file: superate.
- Parsing sintattico Java 17 di 4 sorgenti, inclusa `MainActivity.java`: superato (non equivale alla compilazione Android).
- 13 scenari simulati del selettore multimediale JavaScript: foto, video, immagini decorative, nascosti, offscreen, CDN non autorizzati: superati.
- 32 controlli statici su AndroidManifest, impostazioni WebView, permessi, MediaStore, pulizia file incompleti, intent di condivisione e pipeline: superati.
- Workflow GitHub Actions: struttura YAML letta senza errori.

Eseguire nuovamente in una cartella contenente il progetto:

```bash
bash tests/run_preflight.sh
```

## Correzioni introdotte durante i test

1. I link Story con percorsi aggiuntivi o ambigui sono rifiutati anziché essere troncati e accettati.
2. Un file scaricato viene classificato dal Content-Type e controllato tramite la firma iniziale: pagine HTML/JSON non vengono salvate come foto o video.
3. Il selettore DOM ignora media con host non appartenenti alla allowlist Instagram/Meta.
4. Il comando Salva disabilita i pulsanti immediatamente sul thread UI per ridurre i doppi tocchi.
5. Il salvataggio manuale viene interrotto se la pagina cambia prima del risultato del selettore.

## Test NON ancora eseguiti

- Build Android Gradle e installazione APK.
- Comparsa di StoryPocket nel menu Condividi dell'app Instagram in una specifica versione e su un dato telefono.
- Login e sessione Instagram dentro WebView (Instagram può bloccarli).
- Download reale di Stories di altri profili: accesso consentito, selezione del media corretto, audio/video, blocchi e CDN dinamici.
- Salvataggio effettivo con MediaStore, screenshot PixelCopy su dispositivi diversi, grandi file e timeout di rete.
- Comportamento con rotazione, app in background, chiusura durante lo scaricamento, riavvio e rete assente.

**Conclusione:** lo stato è «test statici e simulati superati, funzionalità reale non verificata». Non confondere questo pacchetto ZIP con un APK installabile o collaudato.