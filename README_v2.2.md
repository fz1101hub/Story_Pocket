# StoryPocket v2.2 Beta — una sola istanza Android

La MainActivity adesso usa `android:launchMode="singleTask"` e `android:documentLaunchMode="never"`. Le condivisioni Instagram (ACTION_SEND) e i link (ACTION_VIEW) ricevuti mentre StoryPocket è aperta arrivano a `onNewIntent()` e vengono gestiti dalla **stessa Activity** e dallo **stesso WebView**. La navigazione si aggiorna al nuovo contenuto, senza aggiungere una nuova scheda StoryPocket in Recenti.

Si conserva la sessione WebView; la ricerca automatica o il trasferimento precedenti vengono annullati all'arrivo del nuovo contenuto. La modifica non cambia il meccanismo sperimentale di download di Stories, Post o Reel.

Test: `bash tests/run_preflight.sh`, build `assembleDebug`, verifica firma APK Android.