package com.storypocket.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * App Android autonoma: navigazione social con host allowlist e salvataggio locale delle
 * Stories e dei media disponibili. Nessun server di terzi, API private o bypass.
 * L'estrazione dei media dal DOM è una funzione sperimentale, non garantita da Instagram.
 */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(250, 247, 252);
    private static final int SURFACE = Color.WHITE;
    private static final int TEXT = Color.rgb(39, 29, 55);
    private static final int MUTED = Color.rgb(103, 93, 118);
    private static final int PURPLE = Color.rgb(126, 48, 224);
    private static final int PINK = Color.rgb(227, 39, 143);
    private static final int ORANGE = Color.rgb(255, 119, 54);
    private static final int MAX_FILE_BYTES = 120 * 1024 * 1024;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    // URL media gia' richiesti dal WebView per il SOLO contenuto attivo.
    // Non conserva corpi HTTP, password, cookie o risposte di Instagram.
    private final Object videoSourcesLock = new Object();
    private final LinkedHashMap<String, Long> observedVideoUrls = new LinkedHashMap<>();
    private static final long VIDEO_SOURCE_TTL_MS = 45000L;
    private WebView browser;
    private long lastNavigationNotice;
    private EditText profileInput;
    private TextView statusText;
    private TextView sourceNotice;
    private Button saveMediaButton;
    private Button saveImageButton;
    private Button saveVideoButton;
    private Button cancelButton;
    private boolean isSaving;
    // Il link ricevuto dal menu Condividi attiva una sola ricerca automatica
    // della Story corrispondente. Mai scaricare automaticamente altre pagine.
    private String pendingSharedContent;
    private int autoAttempts = 0;
    private int shareGeneration = 0;
    private boolean autoProbeInFlight = false;
    private long autoDeadline;
    private int probeNonce;
    private int manualProbeNonce;
    private static final int AUTO_MAX_ATTEMPTS = 5;
    private static final long AUTO_TOTAL_MS = 25000;
    private static final long AUTO_PROBE_MS = 6000;
    private static final long DOWNLOAD_TOTAL_MS = 45000;
    private volatile int transferGeneration = 0;
    private volatile HttpURLConnection activeConnection;
    private final Runnable autoAttempt = () -> attemptSharedDownload();
    private final Runnable autoTimeout = () -> {
        if (pendingSharedContent != null && SystemClock.elapsedRealtime() >= autoDeadline) {
            cancelAutoDownload();
            status(getString(R.string.auto_timeout));
            toast(getString(R.string.auto_unavailable));
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(95, 30, 189));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        getWindow().setNavigationBarColor(BG);
        CookieManager.getInstance().setAcceptCookie(true);
        createUi();
        configureBrowser();
        if (!handleSharedIntent(getIntent())) {
            browser.loadUrl("https://www.instagram.com/");
        }
    }

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private GradientDrawable rounded(int fill, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private GradientDrawable gradient(int radius) {
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.rgb(102, 43, 224), PINK, ORANGE});
        bg.setCornerRadius(dp(radius));
        return bg;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String label, int background, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTextColor(color);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(rounded(background, 14));
        b.setPadding(dp(9), 0, dp(9), 0);
        return b;
    }

    private void createUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(false);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            // Insets di sistema anche su Android 15 / modalità edge-to-edge.
            int top, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                top = bars.top;
                bottom = bars.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(0, top, 0, bottom);
            return insets;
        });

        final boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(16), dp(12), dp(16), dp(10));
        header.setBackground(gradient(0));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(top, new LinearLayout.LayoutParams(-1, dp(45)));
        TextView logo = text("↓", 25, Color.WHITE, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(rounded(0x44FFFFFF, 13));
        top.addView(logo, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout nameCol = new LinearLayout(this);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, -2, 1f);
        nameParams.leftMargin = dp(10);
        top.addView(nameCol, nameParams);
        nameCol.addView(text("StoryPocket", 21, Color.WHITE, true));
        nameCol.addView(text(getString(R.string.home_subtitle), 11, Color.WHITE, false));
        TextView info = text("ⓘ", 23, Color.WHITE, false);
        info.setGravity(Gravity.CENTER);
        info.setOnClickListener(v -> showInfo());
        top.addView(info, new LinearLayout.LayoutParams(dp(35), dp(42)));
        if (landscape) top.setVisibility(View.GONE); // Give the actual WebView space on phones in landscape.

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, dp(49));
        inputParams.topMargin = landscape ? 0 : dp(12);
        header.addView(inputRow, inputParams);
        profileInput = new EditText(this);
        profileInput.setSingleLine(true);
        profileInput.setTextColor(TEXT);
        profileInput.setHintTextColor(MUTED);
        profileInput.setTextSize(14);
        profileInput.setHint(getString(R.string.link_hint));
        profileInput.setPadding(dp(13), 0, dp(9), 0);
        profileInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        profileInput.setBackground(rounded(SURFACE, 14));
        inputRow.addView(profileInput, new LinearLayout.LayoutParams(0, -1, 1f));
        Button open = button(getString(R.string.open), 0xFF47188C, Color.WHITE);
        LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(dp(78), -1);
        openParams.leftMargin = dp(8);
        inputRow.addView(open, openParams);
        open.setOnClickListener(v -> openProfile());
        Button copy = button(getString(R.string.copy), 0xFFFFEDF8, 0xFF812C76);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(dp(70), -1);
        copyParams.leftMargin = dp(6);
        inputRow.addView(copy, copyParams);
        copy.setOnClickListener(v -> copyCurrentLink());

        sourceNotice = text(getString(R.string.social_platforms), 11, Color.WHITE, false);
        TextView notice = sourceNotice;
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, dp(29));
        noteParams.topMargin = dp(2);
        header.addView(notice, noteParams);
        if (landscape) notice.setVisibility(View.GONE);

        browser = new WebView(this);
        browser.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        root.addView(browser, webParams);

        LinearLayout footer = new LinearLayout(this);
        footer.setPadding(dp(14), dp(11), dp(14), dp(12));
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setBackgroundColor(Color.WHITE);
        ScrollView footerScroll = new ScrollView(this);
        // Otherwise the fixed footer consumes nearly all the screen in landscape.
        footerScroll.setFillViewport(false);
        footerScroll.setVerticalScrollBarEnabled(true);
        root.addView(footerScroll, new LinearLayout.LayoutParams(-1, landscape ? dp(116) : -2));
        footerScroll.addView(footer, new ScrollView.LayoutParams(-1, -2));
        statusText = text(getString(R.string.home_status), 12, MUTED, false);
        footer.addView(statusText, new LinearLayout.LayoutParams(-1, dp(landscape ? 38 : 46)));
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        footer.addView(buttons, new LinearLayout.LayoutParams(-1, dp(54)));
        saveMediaButton = button(getString(R.string.save_media), PURPLE, Color.WHITE);
        saveMediaButton.setBackground(gradient(14));
        buttons.addView(saveMediaButton, new LinearLayout.LayoutParams(0, -1, 1.3f));
        LinearLayout.LayoutParams scrParams = new LinearLayout.LayoutParams(0, -1, 1f);
        scrParams.leftMargin = dp(9);
        saveImageButton = button(getString(R.string.screenshot), 0xFFFFECF6, 0xFF963D89);
        buttons.addView(saveImageButton, scrParams);
        saveMediaButton.setOnClickListener(v -> { cancelAutoDownload(); saveCurrentMedia(false, -1); });
        saveImageButton.setOnClickListener(v -> saveScreenshot());
        saveVideoButton = button(getString(R.string.save_original_video), 0xFFF5E9FF, 0xFF7130B1);
        LinearLayout.LayoutParams videoParams = new LinearLayout.LayoutParams(-1, dp(40));
        videoParams.topMargin = dp(5);
        footer.addView(saveVideoButton, videoParams);
        saveVideoButton.setOnClickListener(v -> {
            cancelAutoDownload();
            saveCurrentMedia(false, -1, -1, true);
        });
        cancelButton = button(getString(R.string.cancel), 0xFFF2F0F5, MUTED);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(-1, dp(40));
        cancelParams.topMargin = dp(5);
        footer.addView(cancelButton, cancelParams);
        cancelButton.setOnClickListener(v -> {
            boolean wasRunning = pendingSharedContent != null || isSaving;
            cancelAutoDownload();
            cancelTransfer();
            if (wasRunning) status(getString(R.string.cancelled));
            else status(getString(R.string.nothing_running));
        });

        setContentView(root);
    }

    private void configureBrowser() {
        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setDomStorageEnabled(true);
        browser.getSettings().setAllowFileAccess(false);
        browser.getSettings().setAllowContentAccess(false);
        browser.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
        browser.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        browser.getSettings().setMediaPlaybackRequiresUserGesture(true);
        // With true and no onCreateWindow override, target=_blank/window.open
        // cannot replace the Instagram page or launch an external app.
        browser.getSettings().setSupportMultipleWindows(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(browser, false);
        if (Build.VERSION.SDK_INT >= 26) browser.getSettings().setSafeBrowsingEnabled(true);
        browser.setWebChromeClient(new WebChromeClient());
        browser.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // Eseguito dal WebView su un thread di I/O. Osserva solo richieste GET,
                // senza intercettare la risposta o cambiare il caricamento del player.
                if (request != null && !request.isForMainFrame() &&
                        "GET".equalsIgnoreCase(request.getMethod()) && request.getUrl() != null) {
                    rememberVideoUrl(request.getUrl().toString());
                }
                return null;
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                // Evita di riciclare video di Post o Reel precedenti.
                clearVideoSources();
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl() == null ? null : request.getUrl().toString();
                NavigationPolicy.Decision decision = NavigationPolicy.decide(url, request.isForMainFrame());
                if (decision == NavigationPolicy.Decision.IN_WEBVIEW) return false;
                // Important: NEVER dispatch an Instagram / intent:// app link automatically.
                // Returning true cancels the navigation, keeping StoryPocket in front.
                if (request.isForMainFrame()) {
                    if (decision == NavigationPolicy.Decision.BLOCK_APP_LINK) {
                        navigationNotice(getString(R.string.blocked_app_redirect), request.hasGesture());
                    } else if (decision == NavigationPolicy.Decision.BLOCK_EXTERNAL_WEB) {
                        navigationNotice(getString(R.string.blocked_external_link), request.hasGesture());
                    } else {
                        navigationNotice(getString(R.string.blocked_link_type), request.hasGesture());
                    }
                }
                return true;
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    cancelAutoDownload();
                    status(getString(R.string.page_load_failed));
                }
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request,
                    WebResourceResponse errorResponse) {
                if (request.isForMainFrame()) {
                    int code = errorResponse.getStatusCode();
                    if (code == 401 || code == 403 || code == 429 || code >= 500) {
                        cancelAutoDownload();
                        status(getString(R.string.social_http_unavailable, code));
                    }
                }
            }
            @Override public void onPageFinished(WebView view, String url) {
                if (pendingSharedContent != null) {
                    if (SystemClock.elapsedRealtime() >= autoDeadline) {
                        autoTimeout.run();
                    } else if (TargetParser.matchesSharedContent(url, pendingSharedContent)) {
                        status(getString(R.string.instagram_content_opened));
                        scheduleAutoDownload(1600);
                    } else if (url.contains("/accounts/login")) {
                        cancelAutoDownload();
                        status(getString(R.string.instagram_login_required));
                    } else {
                        status(getString(R.string.shared_content_waiting));
                    }
                } else if (TargetParser.isMediaPage(url)) {
                    status(getString(R.string.content_opened_save));
                } else if (SocialLinks.isSupported(url) && !SocialLinks.isInstagram(url)) {
                    status(getString(R.string.social_page_opened, SocialLinks.platform(url)));
                    setSourceNotice(url);
                } else {
                    status(getString(R.string.share_to_start));
                }
            }
        });
    }

    /** Legge i diversi campi di Android ACTION_SEND, senza presumere un solo formato MIME. */
    private boolean handleSharedIntent(Intent intent) {
        if (intent == null) return false;
        String action = intent.getAction();
        if (!Intent.ACTION_SEND.equals(action) && !Intent.ACTION_SEND_MULTIPLE.equals(action)
                && !Intent.ACTION_VIEW.equals(action)) return false;

        StringBuilder shared = new StringBuilder();
        appendSharedText(shared, intent.getCharSequenceExtra(Intent.EXTRA_TEXT));
        appendSharedText(shared, intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT));
        appendSharedText(shared, intent.getCharSequenceExtra(Intent.EXTRA_TITLE));
        if (intent.getData() != null) appendSharedText(shared, intent.getData().toString());
        ClipData clips = intent.getClipData();
        if (clips != null) {
            for (int i = 0; i < Math.min(clips.getItemCount(), 12); i++) {
                ClipData.Item item = clips.getItemAt(i);
                appendSharedText(shared, item.getText());
                if (item.getUri() != null) appendSharedText(shared, item.getUri().toString());
                // Non aprire content:// come URL di Instagram: servirebbe un permesso file.
            }
        }
        String target = SharedStoryParser.extract(shared.toString());
        if (target == null) target = SocialLinks.extract(shared.toString());
        clearVideoSources();
        cancelAutoDownload();
        if (isSaving) cancelTransfer();
        if (target == null) {
            Uri streamedMedia = null;
            if (Build.VERSION.SDK_INT >= 33) {
                streamedMedia = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri.class);
            } else {
                streamedMedia = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            }
            if (streamedMedia == null && clips != null) {
                for (int i = 0; i < clips.getItemCount(); i++) {
                    Uri item = clips.getItemAt(i).getUri();
                    if (item != null && "content".equals(item.getScheme())) { streamedMedia = item; break; }
                }
            }
            String mime = intent.getType();
            if (streamedMedia != null && "content".equals(streamedMedia.getScheme()) &&
                    mime != null && (mime.startsWith("image/") || mime.startsWith("video/"))) {
                Uri incoming = streamedMedia;
                status(getString(R.string.shared_media_saving));
                int token = startTransfer();
                io.execute(() -> copySharedMedia(incoming, mime, token));
                return true;
            }
            status(getString(R.string.shared_content_invalid));
            toast(getString(R.string.share_https_or_media));
            return true;
        }
        profileInput.setText(target);
        setSourceNotice(target);
        if (!SocialLinks.isInstagram(target)) {
            status(getString(R.string.social_link_received, SocialLinks.platform(target)));
            browser.loadUrl(target);
            if (SocialLinks.supportsPublicResolver(target)) {
                // Run a bounded public API lookup, never upload cookies or credentials.
                attemptSocialDownload(target, false);
            } else {
                status(getString(R.string.social_link_opened, SocialLinks.platform(target)));
            }
            return true;
        }
        if (TargetParser.canAutoDownload(target)) {
            beginAutoDownload(target);
            status(getString(R.string.auto_search_started));
            browser.loadUrl(target);
            scheduleAutoDownload(2200);
        } else {
            status(getString(R.string.profile_link_received));
            browser.loadUrl(target);
        }
        return true;
    }

    private static void appendSharedText(StringBuilder out, CharSequence text) {
        if (text == null || out.length() >= 16000) return;
        int length = Math.min(16000 - out.length(), text.length());
        out.append(text, 0, length).append('\n');
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleSharedIntent(intent);
    }

    private boolean sameSharedContent(String currentUrl, String expected) {
        return expected != null && currentUrl != null
                && TargetParser.matchesSharedContent(currentUrl, expected);
    }

    private void beginAutoDownload(String target) {
        cancelAutoDownload();
        pendingSharedContent = target;
        autoDeadline = SystemClock.elapsedRealtime() + AUTO_TOTAL_MS;
        main.postDelayed(autoTimeout, AUTO_TOTAL_MS + 50);
    }

    private void cancelAutoDownload() {
        shareGeneration++;
        probeNonce++;
        pendingSharedContent = null;
        autoProbeInFlight = false;
        autoAttempts = 0;
        autoDeadline = 0L;
        main.removeCallbacks(autoAttempt);
        main.removeCallbacks(autoTimeout);
    }

    private void scheduleAutoDownload(long delayMs) {
        if (pendingSharedContent == null || autoProbeInFlight || isFinishing()) return;
        if (SystemClock.elapsedRealtime() >= autoDeadline) {
            autoTimeout.run();
            return;
        }
        main.removeCallbacks(autoAttempt);
        main.postDelayed(autoAttempt, delayMs);
    }

    private void attemptSharedDownload() {
        if (pendingSharedContent == null || autoProbeInFlight) return;
        if (SystemClock.elapsedRealtime() >= autoDeadline) { autoTimeout.run(); return; }
        if (isSaving) {
            cancelAutoDownload();
            status(getString(R.string.auto_save_busy));
            return;
        }
        String page = browser.getUrl();
        if (!sameSharedContent(page, pendingSharedContent)) {
            // Login o redirect: non cercare video di profili diversi.
            cancelAutoDownload();
            status(getString(R.string.auto_page_mismatch));
            return;
        }
        autoAttempts++;
        autoProbeInFlight = true;
        int nonce = ++probeNonce;
        int generation = shareGeneration;
        status(getString(R.string.auto_search_progress, autoAttempts, AUTO_MAX_ATTEMPTS));
        main.postDelayed(() -> {
            if (generation == shareGeneration && nonce == probeNonce && autoProbeInFlight) {
                autoScanFailed(generation, getString(R.string.player_not_responding));
            }
        }, AUTO_PROBE_MS);
        saveCurrentMedia(true, generation, nonce);
    }

    private void autoScanFailed(int generation, String detail) {
        if (generation != shareGeneration || pendingSharedContent == null) return;
        autoProbeInFlight = false;
        probeNonce++; // Invalida eventuali risposte JS in ritardo.
        if (SystemClock.elapsedRealtime() >= autoDeadline || autoAttempts >= AUTO_MAX_ATTEMPTS) {
            String browserPage = browser.getUrl();
            String story = TargetParser.isDirectStoryUrl(browserPage) &&
                    sameSharedContent(browserPage, pendingSharedContent) ? browserPage : null;
            cancelAutoDownload();
            if (story != null) {
                // Stories have numeric IDs, not Post/Reel shortcodes. Try their own resolver.
                tryInstagramStoryMetadata(story, browserPage, false);
            } else {
                status(getString(R.string.auto_no_media));
                toast(getString(R.string.shared_media_not_found, detail));
            }
        } else {
            scheduleAutoDownload(1500);
        }
    }

    private void rememberVideoUrl(String url) {
        if (VideoUrlPolicy.score(url) <= 0) return;
        long now = SystemClock.elapsedRealtime();
        synchronized (videoSourcesLock) {
            observedVideoUrls.put(url, now);
            // Elimina richieste vecchie e mantieni il limite per memoria e privacy.
            observedVideoUrls.entrySet().removeIf(e -> now - e.getValue() > VIDEO_SOURCE_TTL_MS);
            while (observedVideoUrls.size() > 80) {
                String oldest = observedVideoUrls.keySet().iterator().next();
                observedVideoUrls.remove(oldest);
            }
        }
    }

    private void clearVideoSources() {
        synchronized (videoSourcesLock) { observedVideoUrls.clear(); }
    }

    private String bestObservedVideoUrl() {
        long now = SystemClock.elapsedRealtime();
        synchronized (videoSourcesLock) {
            String best = null;
            int rank = 0;
            for (Map.Entry<String, Long> entry : observedVideoUrls.entrySet()) {
                if (now - entry.getValue() > VIDEO_SOURCE_TTL_MS) continue;
                int points = VideoUrlPolicy.score(entry.getKey());
                if (points > rank || (points == rank && best != null)) {
                    best = entry.getKey();
                    rank = points;
                }
            }
            return best;
        }
    }

    private void openProfile() {
        clearVideoSources();
        cancelAutoDownload();
        try {
            String target = SocialLinks.normalize(profileInput.getText().toString());
            setSourceNotice(target);
            browser.loadUrl(target);
            status(getString(R.string.loading));
        } catch (IllegalArgumentException e) {
            toast(e.getMessage());
        }
    }

    private void navigationNotice(String text, boolean userGesture) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastNavigationNotice < 2500L) return;
        lastNavigationNotice = now;
        status(text);
        if (userGesture) toast(text);
    }

    private void copyCurrentLink() {
        try {
            String normalized = SocialLinks.normalize(profileInput.getText().toString());
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) throw new IllegalStateException("Appunti non disponibili");
            clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.clipboard_link_label), normalized));
            toast(getString(R.string.link_copied));
        } catch (Exception e) {
            toast(getString(R.string.invalid_link_first));
        }
    }

    private void setSourceNotice(String url) {
        String p = SocialLinks.platform(url);
        if (p != null && sourceNotice != null) sourceNotice.setText(getString(R.string.platform_detected, p));
    }

    private void status(String message) { main.post(() -> statusText.setText(message)); }
    private void toast(String message) { main.post(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show()); }
    private void saving(boolean value) {
        Runnable change = () -> {
            isSaving = value;
            saveMediaButton.setEnabled(!value);
            saveVideoButton.setEnabled(!value);
            saveImageButton.setEnabled(!value);
            saveMediaButton.setAlpha(value ? .5f : 1f);
            saveVideoButton.setAlpha(value ? .5f : 1f);
            saveImageButton.setAlpha(value ? .5f : 1f);
        };
        // Il primo tap deve disabilitare subito i bottoni: niente doppi download.
        if (Looper.myLooper() == Looper.getMainLooper()) change.run();
        else main.post(change);
    }

    private void showInfo() {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.info_title))
            .setMessage(getString(R.string.info_body))
            .setPositiveButton(getString(R.string.understood), (d, w) -> d.dismiss()).show();
    }

    /** One public metadata attempt for recognized platforms. Never uses Instagram credentials. */
    private void attemptSocialDownload(String socialPage, boolean videoOnly) {
        if (isSaving) return;
        String platform = SocialLinks.platform(socialPage);
        if (platform == null || SocialLinks.isInstagram(socialPage)) return;
        if (!SocialLinks.supportsPublicResolver(socialPage)) {
            status(getString(R.string.social_direct_unavailable, platform));
            toast(getString(R.string.social_auto_extract_unavailable, platform));
            return;
        }
        int token = startTransfer();
        String userAgent = browser.getSettings().getUserAgentString();
        status(getString(R.string.social_public_file_search, platform));
        io.execute(() -> {
            try {
                PublicSocialResolver.Result result = PublicSocialResolver.resolve(socialPage, videoOnly);
                if (token != transferGeneration) return;
                if (result == null) {
                    finishTransfer(token, getString(R.string.public_file_missing, platform),
                        getString(R.string.public_file_missing_reason));
                    return;
                }
                main.post(() -> { if(token==transferGeneration)
                    status(getString(R.string.social_media_type_found, platform, result.mediaType)); });
                fetchAndSave(result.mediaUrl, result.mediaType, SocialLinks.fileTag(socialPage), userAgent, token);
            } catch (Exception e) {
                finishTransfer(token, getString(R.string.public_recovery_failed, platform),
                    getString(R.string.public_content_unavailable, e.getMessage()));
            }
        });
    }

    private void saveCurrentMedia(boolean automatic, int generation) {
        saveCurrentMedia(automatic, generation, -1);
    }

    private void saveCurrentMedia(boolean automatic, int generation, int nonce) {
        saveCurrentMedia(automatic, generation, nonce, false);
    }

    private void saveCurrentMedia(boolean automatic, int generation, int nonce, boolean onlyVideo) {
        if (isSaving) {
            if (automatic) autoScanFailed(generation, getString(R.string.save_already_running));
            return;
        }
        String browserPage = browser.getUrl();
        if (SocialLinks.isSupported(browserPage) && !SocialLinks.isInstagram(browserPage)) {
            attemptSocialDownload(browserPage, onlyVideo);
            return;
        }
        if (TargetParser.isMediaPage(browserPage)) {
            scanMediaOnPage(automatic, generation, nonce, onlyVideo, browserPage, browserPage);
            return;
        }
        if (automatic) {
            autoScanFailed(generation, getString(R.string.shared_link_not_open));
        } else if (TargetParser.isInstagramUrl(browserPage)) {
            // Instagram navigates inside the page using pushState and modal viewers.
            // The native WebView URL may remain /, /reels/ or /explore/ while the
            // visible player is for an individual Post/Reel/Story.
            resolveVisibleMediaPage(browserPage, onlyVideo);
        } else {
            toast(getString(R.string.open_instagram_page));
        }
    }

    private static final String MEDIA_CONTEXT_SCRIPT =
        "(function () {\n" +
        " try {\n" +
        "   const href = location.href;\n" +
        "   function mediaUrl(raw) {\n" +
        "     try {\n" +
        "       const u = new URL(raw, href);\n" +
        "       if (u.protocol !== 'https:' || !['instagram.com','www.instagram.com','m.instagram.com'].includes(u.hostname.toLowerCase())) return null;\n" +
        "       if (!/^\\/(?:reel|reels|p|tv)\\/[A-Za-z0-9_-]{3,80}\\/?$/i.test(u.pathname) &&\n" +
        "           !/^\\/stories\\/[A-Za-z0-9._]{1,30}\\/[0-9]{1,30}\\/?$/i.test(u.pathname)) return null;\n" +
        "       return u.origin + u.pathname;\n" +
        "     } catch(e) { return null; }\n" +
        "   }\n" +
        "   if (mediaUrl(href)) return mediaUrl(href);\n" +
        "   const vw = innerWidth, vh = innerHeight;\n" +
        "   function area(el) {\n" +
        "     const r=el.getBoundingClientRect(), st=getComputedStyle(el);\n" +
        "     if (st.display==='none' || st.visibility==='hidden' || Number(st.opacity)<0.1) return 0;\n" +
        "     const w=Math.max(0,Math.min(vw,r.right)-Math.max(0,r.left));\n" +
        "     const h=Math.max(0,Math.min(vh,r.bottom)-Math.max(0,r.top));\n" +
        "     return w*h;\n" +
        "   }\n" +
        "   // Only identify the active large player, not suggested or background posts.\n" +
        "   const players=Array.from(document.querySelectorAll('video')).filter(v=>area(v)>=30000)\n" +
        "     .sort((a,b)=>area(b)-area(a));\n" +
        "   if (players.length && (players.length===1 || area(players[0])>area(players[1])*1.7)) {\n" +
        "     const player=players[0];\n" +
        "     let parent=player;\n" +
        "     for(let depth=0;parent && depth<9;depth++,parent=parent.parentElement) {\n" +
        "       const links=new Set();\n" +
        "       if (parent.matches && parent.matches('a[href]')) {\n" +
        "         const url=mediaUrl(parent.href); if(url) links.add(url);\n" +
        "       }\n" +
        "       if (parent.querySelectorAll) {\n" +
        "         for(const a of parent.querySelectorAll('a[href]')) {\n" +
        "           const url=mediaUrl(a.href);if(url && area(a)>0) links.add(url);\n" +
        "         }\n" +
        "       }\n" +
        "       if(links.size===1) return Array.from(links)[0];\n" +
        "       if(links.size>1) break; // Ambiguous suggestions are never downloaded.\n" +
        "     }\n" +
        "   }\n" +
        "   // In an expanded single-media dialog, exactly one media link is required.\n" +
        "   const dialogs=Array.from(document.querySelectorAll('[role=\"dialog\"]')).filter(d=>area(d)>=30000);\n" +
        "   if (dialogs.length===1) {\n" +
        "     const links=new Set();\n" +
        "     for(const a of dialogs[0].querySelectorAll('a[href]')) {\n" +
        "       const url=mediaUrl(a.href);\n" +
        "       if(url && area(a)>0) links.add(url);\n" +
        "     }\n" +
        "     if(links.size===1) return Array.from(links)[0];\n" +
        "   }\n" +
        "   // Do not guess using the input box or a random recommended video.\n" +
        "   return null;\n" +
        " } catch(e) { return null; }\n" +
        "})()";

    private void resolveVisibleMediaPage(String browserPage, boolean onlyVideo) {
        final int nonce = ++manualProbeNonce;
        saving(true);
        status(getString(R.string.identify_visible_media));
        main.postDelayed(() -> {
            if (nonce == manualProbeNonce && isSaving) {
                manualProbeNonce++;
                saving(false);
                status(getString(R.string.instagram_no_response));
            }
        }, AUTO_PROBE_MS);
        browser.evaluateJavascript(MEDIA_CONTEXT_SCRIPT, raw -> {
            if (nonce != manualProbeNonce || !browserPage.equals(browser.getUrl())) return;
            String candidate = null;
            try {
                if (raw != null && !"null".equals(raw)) {
                    candidate = new JSONArray("[" + raw + "]").getString(0);
                }
            } catch (Exception ignored) { }
            String page = MediaContextPolicy.resolve(browserPage, candidate);
            if (page == null) {
                manualProbeNonce++;
                saving(false);
                status(getString(R.string.instagram_media_not_identified));
                toast(getString(R.string.instagram_direct_link_missing));
                return;
            }
            manualProbeNonce++; // invalidate discovery watchdog
            status(getString(R.string.media_found_saving));
            scanMediaOnPage(false, -1, -1, onlyVideo, page, browserPage);
        });
    }

    /** page is the validated media identity; browserPage is the WebView navigation snapshot. */
    private void scanMediaOnPage(boolean automatic, int generation, int nonce, boolean onlyVideo,
                                 String page, String browserPage) {
        if (!MediaContextPolicy.isValid(browserPage, page)) {
            if (automatic) autoScanFailed(generation, getString(R.string.content_not_open));
            else { saving(false); status(getString(R.string.invalid_media_link)); }
            return;
        }
        final int manualNonce;
        if (!automatic) {
            saving(true);
            manualNonce = ++manualProbeNonce;
            status(getString(R.string.search_visible_media));
            main.postDelayed(() -> {
                if (manualNonce == manualProbeNonce && isSaving) {
                    manualProbeNonce++;
                    saving(false);
                    status(getString(R.string.player_timeout));
                }
            }, AUTO_PROBE_MS);
        } else {
            manualNonce = -1;
        }
        // JS solo su pagine HTTPS Instagram specifiche: Story, Post e Reel.
        // e NON installa interfacce JavaScript persistenti.
        String script =
            "(function(){\n" +
            "  const forceVideo = '__SP_FORCE_VIDEO__' === 'true';\n" +
            "  const contentPath = '__SP_MEDIA_PATH__';\n" +
            "  const path = /^\\/(?:reel|p|tv|stories)\\//i.test(contentPath) ? contentPath : ((typeof location !== 'undefined' && location.pathname) || '');\n" +
            "  const reel = /^\\/(?:reel|tv)\\//i.test(path);\n" +
            "  const vw = innerWidth, vh = innerHeight;\n" +
            "  let videoCandidate = null, imageCandidate = null;\n" +
            "  let visibleVideo = false, metaVideo = false;\n" +
            "  function trusted(url){\n" +
            "    if (!url || !/^https:\\/\\//i.test(url)) return false;\n" +
            "    try {\n" +
            "      const u = new URL(url), h = u.hostname.toLowerCase();\n" +
            "      return u.protocol === 'https:' && !u.username && !u.password &&\n" +
            "        (!u.port || u.port === '443') &&\n" +
            "        ['instagram.com','cdninstagram.com','fbcdn.net','instagram.net']\n" +
            "          .some(d => h === d || h.endsWith('.' + d));\n" +
            "    } catch(e){ return false; }\n" +
            "  }\n" +
            "  function visibility(el){\n" +
            "    const r = el.getBoundingClientRect(), s = getComputedStyle(el);\n" +
            "    if (s.display === 'none' || s.visibility === 'hidden' || Number(s.opacity) < 0.1) return 0;\n" +
            "    if (r.width < 140 || r.height < 170 || r.right <= 0 || r.bottom <= 0 || r.left >= vw || r.top >= vh) return 0;\n" +
            "    const width = Math.max(0, Math.min(r.right,vw)-Math.max(r.left,0));\n" +
            "    const height = Math.max(0,Math.min(r.bottom,vh)-Math.max(r.top,0));\n" +
            "    const area = width * height;\n" +
            "    if (area < 30000) return 0;\n" +
            "    return area - 0.2 * (Math.abs((r.left+r.right)/2-vw/2) + Math.abs((r.top+r.bottom)/2-vh/2));\n" +
            "  }\n" +
            "  function add(kind, url, score){\n" +
            "    if (!trusted(url)) return;\n" +
            "    const value = {type: kind, url, score};\n" +
            "    if (kind === 'video') {\n" +
            "      if (!videoCandidate || score > videoCandidate.score) videoCandidate = value;\n" +
            "    } else if (!imageCandidate || score > imageCandidate.score) imageCandidate = value;\n" +
            "  }\n" +
            "  document.querySelectorAll('video').forEach(v => {\n" +
            "    const score = visibility(v);\n" +
            "    if (!score) return;\n" +
            "    visibleVideo = true; // A player with a blob URL still means a VIDEO, not its poster image.\n" +
            "    const sources = [v.currentSrc, v.getAttribute && v.getAttribute('src'), v.src];\n" +
            "    if (v.querySelectorAll) v.querySelectorAll('source').forEach(src => {\n" +
            "      sources.push(src.src, src.getAttribute && src.getAttribute('src'));\n" +
            "    });\n" +
            "    sources.forEach(src => add('video',src,score));\n" +
            "  });\n" +
            "  // Le URL di rete sono utili quando video.currentSrc e' blob:.\n" +
            "  try { if (visibleVideo || reel) performance.getEntriesByType('resource').slice(-400).forEach(entry => {\n" +
            "    const u = entry.name || '';\n" +
            "    if (/\\.(?:mp4|webm|mov)(?:[?#]|$)|mime_type=video|mimetype=video|dst-mp4|\\/t16\\//i.test(u) &&\n" +
            "      !/[?&](?:bytestart|byteend|range)=|\\.(?:m4s|mpd|m3u8)(?:[?#]|$)/i.test(u))\n" +
            "      add('video',u,visibleVideo ? vw*vh*0.7 : vw*vh*0.3);\n" +
            "  }); } catch(e) {}\n" +
            "  document.querySelectorAll('meta').forEach(m => {\n" +
            "    const key = ((m.getAttribute && (m.getAttribute('property') || m.getAttribute('name'))) || '').toLowerCase();\n" +
            "    if (key === 'og:video' || key === 'og:video:url' || key === 'og:video:secure_url' || key === 'twitter:player:stream') {\n" +
            "      metaVideo = true;\n" +
            "      add('video',m.content || (m.getAttribute && m.getAttribute('content')),vw*vh/2);\n" +
            "    }\n" +
            "  });\n" +
            "  // Alcune pagine Reel contengono versioni MP4 progressive nei dati JSON iniziali.\n" +
            "  // Leggi soltanto le versioni associate esattamente al Reel/Post aperto.\n" +
            "  try {\n" +
            "    const codeMatch = /^\\/(?:reel|tv|p)\\/([A-Za-z0-9_-]{3,80})(?:\\/|$)/i.exec(path);\n" +
            "    const openedCode = codeMatch ? codeMatch[1] : null;\n" +
            "    if (openedCode && (reel || visibleVideo || metaVideo || forceVideo)) {\n" +
            "      const seen = new WeakSet();\n" +
            "      let visited = 0, totalScriptChars = 0;\n" +
            "      function visit(node, depth) {\n" +
            "        if (depth > 28 || visited++ > 35000 || node == null) return;\n" +
            "        if (typeof node === 'string') {\n" +
            "          if (node.length > 10 && node.length < 2000000 && node.includes('video_versions') &&\n" +
            "              node.includes(openedCode) && /^[\\s]*[\\[{]/.test(node)) {\n" +
            "            try { visit(JSON.parse(node), depth + 1); } catch(e) {}\n" +
            "          }\n" +
            "          return;\n" +
            "        }\n" +
            "        if (typeof node !== 'object' || seen.has(node)) return;\n" +
            "        seen.add(node);\n" +
            "        if (!Array.isArray(node)) {\n" +
            "          const id = node.code || node.shortcode || node.short_code;\n" +
            "          if (id === openedCode && Array.isArray(node.video_versions)) {\n" +
            "            node.video_versions.forEach(v => {\n" +
            "              if (v && typeof v.url === 'string') {\n" +
            "                const size = Math.max(0, Number(v.width)||0) * Math.max(0, Number(v.height)||0);\n" +
            "                add('video', v.url, 100000000 + Math.min(size, 10000000));\n" +
            "              }\n" +
            "            });\n" +
            "          }\n" +
            "        }\n" +
            "        const children = Array.isArray(node) ? node : Object.values(node);\n" +
            "        for (const child of children) {\n" +
            "          if (visited > 35000) break;\n" +
            "          visit(child, depth + 1);\n" +
            "        }\n" +
            "      }\n" +
            "      const scripts = Array.from(document.querySelectorAll('script')).slice(-70);\n" +
            "      for (const item of scripts) {\n" +
            "        const raw = item.textContent || '';\n" +
            "        if (raw.length > 4500000 || totalScriptChars + raw.length > 10000000) continue;\n" +
            "        totalScriptChars += raw.length;\n" +
            "        if (!raw.includes('video_versions') || !raw.includes(openedCode) || !/^[\\s]*[\\[{]/.test(raw)) continue;\n" +
            "        try { visit(JSON.parse(raw), 0); } catch(e) {}\n" +
            "        if (visited > 35000) break;\n" +
            "      }\n" +
            "    }\n" +
            "  } catch(e) {}\n" +
            "  // Stories are identified by a numeric media ID, not by a Reel shortcode.\n" +
            "  // Match exactly the opened Story; never download adjacent Stories or thumbnails.\n" +
            "  try {\n" +
            "    const storyMatch=/^\\/stories\\/[A-Za-z0-9._]{1,30}\\/([0-9]{1,30})(?:\\/|$)/i.exec(path);\n" +
            "    if (storyMatch) {\n" +
            "      const storyId=storyMatch[1],seenStory=new WeakSet();\n" +
            "      let visitedStory=0, totalStoryChars=0;\n" +
            "      function storyMatchId(id){return String(id||'')===storyId || String(id||'').startsWith(storyId+'_');}\n" +
            "      function visitStory(node, depth){\n" +
            "        if(node==null||depth>25||visitedStory++>26000)return;\n" +
            "        if(typeof node==='string'){\n" +
            "          if(node.length>10&&node.length<1500000&&node.includes(storyId)&&\n" +
            "             (node.includes('video_versions')||node.includes('image_versions2'))&&/^[\\s]*[\\[{]/.test(node)){\n" +
            "            try{visitStory(JSON.parse(node),depth+1);}catch(e){}\n" +
            "          }return;\n" +
            "        }\n" +
            "        if(typeof node!=='object'||seenStory.has(node))return;seenStory.add(node);\n" +
            "        if(!Array.isArray(node) && (storyMatchId(node.pk)||storyMatchId(node.id))){\n" +
            "          const isVideo=Number(node.media_type)===2 || Boolean(node.video_versions)||Boolean(node.video_url);\n" +
            "          if(isVideo){\n" +
            "            metaVideo=true; // Do not turn a Story video into a JPEG cover.\n" +
            "            const videos=Array.isArray(node.video_versions)?node.video_versions:[];\n" +
            "            videos.forEach(v=>{if(v&&typeof v.url==='string')add('video',v.url,\n" +
            "              100000000+Math.min(10000000,(Number(v.width)||1)*(Number(v.height)||1)));});\n" +
            "            if(typeof node.video_url==='string')add('video',node.video_url,100000000);\n" +
            "          }else if(Number(node.media_type)===1){\n" +
            "            const list=node.image_versions2&&node.image_versions2.candidates;\n" +
            "            if(Array.isArray(list))list.forEach(v=>{if(v&&typeof v.url==='string')\n" +
            "              add('image',v.url,100000000+Math.min(10000000,(Number(v.width)||1)*(Number(v.height)||1)));});\n" +
            "          }\n" +
            "        }\n" +
            "        const children=Array.isArray(node)?node:Object.values(node);\n" +
            "        for(const child of children){if(visitedStory>26000)break;visitStory(child,depth+1); }\n" +
            "      }\n" +
            "      for(const item of Array.from(document.querySelectorAll('script')).slice(-80)){\n" +
            "        const raw=item.textContent||'';\n" +
            "        if(raw.length>3500000||totalStoryChars+raw.length>9000000)continue;\n" +
            "        totalStoryChars+=raw.length;\n" +
            "        if(!raw.includes(storyId)||(!raw.includes('video_versions')&&!raw.includes('image_versions2')))continue;\n" +
            "        try{visitStory(JSON.parse(raw),0);}catch(e){}\n" +
            "      }\n" +
            "    }\n" +
            "  }catch(e){}\n" +
            "  document.querySelectorAll('img').forEach(i => {\n" +
            "    const score = visibility(i);\n" +
            "    if (score) add('image',i.currentSrc || i.src,score);\n" +
            "  });\n" +
            "  // Reel, active video players, or explicit video metadata never fall back to a still thumbnail.\n" +
            "  if (forceVideo || reel || visibleVideo || metaVideo) {\n" +
            "    return JSON.stringify(videoCandidate || {error: (!visibleVideo && !metaVideo) ? 'Instagram non mostra il player video dentro StoryPocket: la copertina non viene salvata. Impossibile recuperare il filmato originale da questa pagina.' : 'Il video nel player non espone un MP4 scaricabile. La copertina non viene salvata.'});\n" +
            "  }\n" +
            "  return JSON.stringify(imageCandidate || {error:'Nessun media visibile trovato'});\n" +
            "})()";
        script = script.replace("__SP_FORCE_VIDEO__", onlyVideo ? "true" : "false")
            .replace("__SP_MEDIA_PATH__", Uri.parse(page).getPath());
        browser.evaluateJavascript(script, raw -> {
            if (!automatic && (manualNonce != manualProbeNonce || !isSaving)) return;
            if (automatic && (generation != shareGeneration || nonce != probeNonce ||
                    !autoProbeInFlight || !sameSharedContent(browser.getUrl(), pendingSharedContent))) {
                return; // Utente passato a un'altra Story o nuova condivisione.
            }
            if (!automatic && !browserPage.equals(browser.getUrl())) {
                saving(false);
                status(getString(R.string.story_page_changed));
                return;
            }
            try {
                if (raw == null || "null".equals(raw)) throw new Exception(getString(R.string.player_no_result));
                String decoded = new JSONArray("[" + raw + "]").getString(0);
                JSONObject result = new JSONObject(decoded);
                if (result.has("error") &&
                        (onlyVideo || page.contains("/reel/") || page.contains("/tv/") ||
                         result.optString("error").contains("video"))) {
                    String networkUrl = bestObservedVideoUrl();
                    if (networkUrl != null) {
                        result = new JSONObject();
                        result.put("url", networkUrl);
                        result.put("type", "video");
                    }
                }
                if (result.has("error")) {
                    throw new Exception(result.getString("error") +
                        " La sorgente video completa non compare nelle richieste osservate. " +
                        "Potrebbe essere uno streaming a segmenti, non un file MP4 unico.");
                }
                String url = result.getString("url");
                String type = result.getString("type");
                if (automatic && "image".equals(type) && autoAttempts < 2) {
                    autoScanFailed(generation, getString(R.string.waiting_photo_or_video_cover));
                    return;
                }
                if ("video".equals(type) && VideoUrlPolicy.score(url) == 0) {
                    throw new Exception(getString(R.string.video_original_unavailable));
                }
                if (!TargetParser.isTrustedMediaUrl(url)) {
                    throw new Exception(getString(R.string.video_cdn_unsupported));
                }
                String filenameTag = TargetParser.fileTag(page);
                String userAgent = browser.getSettings().getUserAgentString();
                if (automatic) {
                    cancelAutoDownload(); // Un solo download per ogni condivisione.
                    status(getString(R.string.auto_download_started));
                } else {
                    manualProbeNonce++; // Invalida il watchdog della ricerca JS.
                }
                int token = startTransfer();
                io.execute(() -> fetchAndSave(url, type, filenameTag, userAgent, token));
            } catch (Exception e) {
                if (automatic) {
                    autoScanFailed(generation, e.getMessage());
                } else {
                    manualProbeNonce++;
                    // Media Saver uses several independent resolvers. Our first safe additional
                    // strategy talks only to Instagram and reuses this app's login, never a third-party.
                    if (InstagramFirstPartyResolver.shortcode(page) != null) {
                        tryInstagramMetadata(page, browserPage, onlyVideo || page.contains("/reel/"));
                    } else if (TargetParser.isDirectStoryUrl(page)) {
                        tryInstagramStoryMetadata(page, browserPage, onlyVideo);
                    } else {
                        saving(false);
                        status(getString(R.string.direct_download_unavailable));
                        toast(e.getMessage() + " " + getString(R.string.screenshot_photos_only_hint));
                    }
                }
            }
        });
    }

    /** A separate first-party strategy. Only HTTPS www.instagram.com receives cookies. */
    private void tryInstagramMetadata(String page, String browserPage, boolean videoOnly) {
        if (!browserPage.equals(browser.getUrl())) { saving(false); return; }
        String cookie = CookieManager.getInstance().getCookie("https://www.instagram.com/");
        String userAgent = browser.getSettings().getUserAgentString();
        status(getString(R.string.instagram_metadata_fallback));
        int token = startTransfer();
        io.execute(() -> {
            try {
                InstagramFirstPartyResolver.Result found =
                    InstagramFirstPartyResolver.resolve(page, cookie, userAgent, videoOnly);
                if (token != transferGeneration) return;
                if (found == null) {
                    finishTransfer(token, getString(R.string.instagram_original_unavailable),
                        getString(R.string.instagram_original_unavailable_reason));
                    return;
                }
                status(getString(R.string.media_found_by_method, found.method));
                fetchAndSave(found.mediaUrl, found.mediaType, TargetParser.fileTag(page), userAgent, token);
            } catch (Exception e) {
                finishTransfer(token, getString(R.string.instagram_service_failed),
                    getString(R.string.instagram_service_failed_reason, e.getMessage()));
            }
        });
    }

    /** Attempt the exact shared Story ID, not the author's full Story sequence. */
    private void tryInstagramStoryMetadata(String page, String browserPage, boolean videoOnly) {
        if (!browserPage.equals(browser.getUrl()) || !TargetParser.isDirectStoryUrl(page)) {
            saving(false);
            status(getString(R.string.story_changed));
            return;
        }
        String cookie = CookieManager.getInstance().getCookie("https://www.instagram.com/");
        String userAgent = browser.getSettings().getUserAgentString();
        status(getString(R.string.story_metadata_search));
        int token = startTransfer();
        io.execute(() -> {
            try {
                InstagramStoryResolver.Result found =
                    InstagramStoryResolver.resolve(page, cookie, userAgent, videoOnly);
                if (token != transferGeneration) return;
                if (found == null) {
                    finishTransfer(token, getString(R.string.story_unavailable),
                        getString(R.string.story_unavailable_reason));
                    return;
                }
                main.post(() -> { if (token == transferGeneration) status(
                    getString(R.string.story_original_found, found.mediaType)); });
                fetchAndSave(found.mediaUrl, found.mediaType, TargetParser.fileTag(page), userAgent, token);
            } catch (Exception e) {
                finishTransfer(token, getString(R.string.story_original_failed),
                    getString(R.string.story_original_failed_reason, e.getMessage()));
            }
        });
    }

    // Le timeout di socket sono limiti per singola lettura: qui serve anche un
    // timeout ASSOLUTO, per impedire stream che inviano pochi byte per sempre.
    private int startTransfer() {
        int token = ++transferGeneration;
        saving(true);
        main.postDelayed(() -> {
            if (token == transferGeneration) {
                cancelTransfer();
                status(getString(R.string.download_timeout));
                toast(getString(R.string.instagram_download_timeout));
            }
        }, DOWNLOAD_TOTAL_MS);
        return token;
    }

    private void cancelTransfer() {
        transferGeneration++;
        manualProbeNonce++;
        HttpURLConnection connection = activeConnection;
        if (connection != null) connection.disconnect();
        activeConnection = null;
        saving(false);
    }

    private void ensureActive(int token, int activeToken, long deadline) throws IOException {
        if (token != activeToken) throw new IOException(getString(R.string.download_cancelled));
        if (SystemClock.elapsedRealtime() > deadline) throw new IOException(getString(R.string.download_timeout));
    }

    private void finishTransfer(int token, String message, String detail) {
        main.post(() -> {
            if (token != transferGeneration) return; // Nessun messaggio da download vecchi.
            transferGeneration++; // invalidate the absolute 45-second watchdog after completion
            activeConnection = null;
            saving(false);
            status(message);
            if (detail != null) toast(detail);
        });
    }

    private void fetchAndSave(String mediaUrl, String type, String filenameTag, String userAgent, int token) {
        HttpURLConnection conn = null;
        Uri pending = null;
        long deadline = SystemClock.elapsedRealtime() + DOWNLOAD_TOTAL_MS;
        try {
            String currentUrl = mediaUrl;
            int redirects = 0;
            while (true) {
                ensureActive(token, transferGeneration, deadline);
                if (!TargetParser.isTrustedMediaUrl(currentUrl) && !SocialLinks.isTrustedMedia(currentUrl))
                    throw new IOException(getString(R.string.media_host_untrusted));
                conn = (HttpURLConnection) new URL(currentUrl).openConnection();
                activeConnection = conn;
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", userAgent);
                if (TargetParser.isTrustedMediaUrl(currentUrl))
                    conn.setRequestProperty("Referer", "https://www.instagram.com/");
                if (TargetParser.useInstagramCookies(currentUrl)) {
                    String cookie = CookieManager.getInstance().getCookie("https://www.instagram.com/");
                    if (cookie != null && !cookie.isEmpty()) conn.setRequestProperty("Cookie", cookie);
                }
                int code = conn.getResponseCode();
                ensureActive(token, transferGeneration, deadline);
                if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                    String location = conn.getHeaderField("Location");
                    if (++redirects > 3 || location == null) throw new IOException(getString(R.string.too_many_redirects));
                    currentUrl = new URL(new URL(currentUrl), location).toString();
                    conn.disconnect();
                    activeConnection = null;
                    conn = null;
                    continue;
                }
                if (code == 206) throw new IOException(getString(R.string.partial_media_response));
                if (code != 200) throw new IOException(getString(R.string.media_http_error, code));
                break;
            }
            long length = conn.getContentLengthLong();
            if (length > MAX_FILE_BYTES) throw new IOException(getString(R.string.media_over_limit));
            try (BufferedInputStream input = new BufferedInputStream(conn.getInputStream())) {
                input.mark(32);
                byte[] first = new byte[24];
                int count = 0;
                while (count < first.length) {
                    ensureActive(token, transferGeneration, deadline);
                    int n = input.read(first, count, first.length - count);
                    if (n == -1) break;
                    count += n;
                }
                input.reset();
                MediaTypePolicy.MediaFormat format;
                try { format = MediaTypePolicy.classifyResponse(conn.getContentType(), type, first); }
                catch (IllegalArgumentException e) { throw new IOException(e.getMessage()); }
                if (!MediaTypePolicy.matchesExpectedType(type, format)) {
                    throw new IOException(getString(R.string.download_wrong_media_type));
                }
                if (!MediaTypePolicy.hasValidSignature(format, first)) {
                    throw new IOException(getString(R.string.download_invalid_response));
                }
                ensureActive(token, transferGeneration, deadline);
                String name = filenameTag + "_" + System.currentTimeMillis() + "." + format.extension;
                pending = insertDownload(name, format.mime);
                if (pending == null) throw new IOException(getString(R.string.download_create_failed));
                long total = 0L, lastUpdate = 0L;
                try (OutputStream output = getContentResolver().openOutputStream(pending)) {
                    if (output == null) throw new IOException(getString(R.string.download_open_failed));
                    byte[] buffer = new byte[16384];
                    int n;
                    while (true) {
                        ensureActive(token, transferGeneration, deadline);
                        n = input.read(buffer);
                        if (n == -1) break;
                        total += n;
                        if (total > MAX_FILE_BYTES) throw new IOException(getString(R.string.media_stream_over_limit));
                        output.write(buffer, 0, n);
                        long now = SystemClock.elapsedRealtime();
                        if (now - lastUpdate > 1800) {
                            lastUpdate = now;
                            String progress = length > 0
                                ? getString(R.string.download_progress_total, total / 1024, length / 1024)
                                : getString(R.string.download_progress, total / 1024);
                            main.post(() -> { if (token == transferGeneration) statusText.setText(progress); });
                        }
                    }
                }
                if (total == 0) throw new IOException(getString(R.string.download_empty_media));
                if (length > 0 && total != length) throw new IOException(getString(R.string.download_incomplete));
            }
            ensureActive(token, transferGeneration, deadline);
            publishDownload(pending);
            pending = null;
            finishTransfer(token, getString(R.string.media_saved_location), getString(R.string.media_saved_success));
        } catch (Exception e) {
            finishTransfer(token, getString(R.string.media_save_unavailable), e.getMessage());
        } finally {
            if (pending != null) getContentResolver().delete(pending, null, null);
            if (conn != null) conn.disconnect();
            if (activeConnection == conn) activeConnection = null;
        }
    }

    /** Se Android condivide direttamente un file, salvalo senza richiedere alcun link. */
    private void copySharedMedia(Uri source, String declaredMime, int token) {
        long deadline = SystemClock.elapsedRealtime() + DOWNLOAD_TOTAL_MS;
        Uri pending = null;
        try {
            ensureActive(token, transferGeneration, deadline);
            String detected = getContentResolver().getType(source);
            MediaTypePolicy.MediaFormat fmt = MediaTypePolicy.classify(detected == null ? declaredMime : detected);
            try (BufferedInputStream in = new BufferedInputStream(getContentResolver().openInputStream(source))) {
                in.mark(32);
                byte[] first = new byte[24];
                int count = 0, n;
                while (count < first.length) {
                    ensureActive(token, transferGeneration, deadline);
                    n = in.read(first, count, first.length - count);
                    if (n <= 0) break;
                    count += n;
                }
                in.reset();
                if (!MediaTypePolicy.hasValidSignature(fmt, first)) throw new IOException(getString(R.string.shared_unsupported_media));
                pending = insertDownload("condiviso_" + System.currentTimeMillis() + "." + fmt.extension, fmt.mime);
                if (pending == null) throw new IOException(getString(R.string.download_create_failed));
                long total = 0;
                try (OutputStream out = getContentResolver().openOutputStream(pending)) {
                    if (out == null) throw new IOException(getString(R.string.shared_destination_failed));
                    byte[] buffer = new byte[16384];
                    while (true) {
                        ensureActive(token, transferGeneration, deadline);
                        n = in.read(buffer);
                        if (n == -1) break;
                        total += n;
                        if (total > MAX_FILE_BYTES) throw new IOException(getString(R.string.shared_file_too_large));
                        out.write(buffer, 0, n);
                    }
                }
                if (total == 0) throw new IOException(getString(R.string.shared_empty_file));
            }
            ensureActive(token, transferGeneration, deadline);
            publishDownload(pending);
            pending = null;
            finishTransfer(token, getString(R.string.shared_file_saved_location), getString(R.string.shared_file_saved_success));
        } catch (Exception error) {
            finishTransfer(token, getString(R.string.shared_file_save_failed), error.getMessage());
        } finally {
            if (pending != null) getContentResolver().delete(pending, null, null);
        }
    }

    private Uri insertDownload(String name, String mime) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/StoryPocket");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        return getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
    }

    private void publishDownload(Uri uri) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.IS_PENDING, 0);
        getContentResolver().update(uri, values, null, null);
    }

    private void saveScreenshot() {
        if (isSaving) return;
        if (!TargetParser.isMediaPage(browser.getUrl()) && !SocialLinks.isSupported(browser.getUrl())) {
            toast(getString(R.string.open_social_to_capture));
            return;
        }
        if (browser.getWidth() <= 0 || browser.getHeight() <= 0) {
            toast(getString(R.string.story_not_ready));
            return;
        }
        saving(true);
        status(getString(R.string.capturing_screen));
        int[] position = new int[2];
        browser.getLocationInWindow(position);
        int width = browser.getWidth();
        int height = browser.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Rect region = new Rect(position[0], position[1], position[0] + width, position[1] + height);
        PixelCopy.request(getWindow(), region, bitmap, result -> {
            if (result != PixelCopy.SUCCESS) {
                bitmap.recycle();
                saving(false);
                status(getString(R.string.screen_capture_failed));
                toast(getString(R.string.capture_not_allowed));
                return;
            }
            String username = TargetParser.isInstagramUrl(browser.getUrl()) ?
                TargetParser.fileTag(browser.getUrl()) : SocialLinks.fileTag(browser.getUrl());
            io.execute(() -> writeScreenshot(bitmap, username));
        }, main);
    }

    private void writeScreenshot(Bitmap bitmap, String username) {
        Uri pending = null;
        try {
            String name = "schermata_" + username + "_" + System.currentTimeMillis() + ".png";
            pending = insertDownload(name, "image/png");
            if (pending == null) throw new IOException(getString(R.string.screenshot_create_failed));
            try (OutputStream output = getContentResolver().openOutputStream(pending)) {
                if (output == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    throw new IOException(getString(R.string.screenshot_png_failed));
                }
            }
            publishDownload(pending);
            pending = null;
            status(getString(R.string.screenshot_saved));
            toast(getString(R.string.screenshot_png_saved));
        } catch (Exception e) {
            status(getString(R.string.screenshot_save_failed));
            toast(e.getMessage());
        } finally {
            if (pending != null) getContentResolver().delete(pending, null, null);
            bitmap.recycle();
            saving(false);
        }
    }

    @Override public void onBackPressed() {
        cancelAutoDownload();
        if (browser.canGoBack()) browser.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        clearVideoSources();
        cancelAutoDownload();
        cancelTransfer();
        if (browser != null) browser.destroy();
        io.shutdown();
        super.onDestroy();
    }
}
