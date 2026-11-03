package com.storypocket.mobile;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Public, unauthenticated API attempts for selected social networks; never forwards cookies or credentials. */
public final class PublicSocialResolver {
    public static final class Result {
        public final String mediaUrl, mediaType, platform;
        public Result(String url, String type, String platform) {
            this.mediaUrl = url; this.mediaType = type; this.platform = platform;
        }
    }
    private PublicSocialResolver() { }
    private static String encoded(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    private static JSONObject getJson(String source) throws Exception {
        // All API endpoints are derived from fixed trusted hosts or a recognized Mastodon instance.
        if (!(SocialLinks.isSupported(source) || source.startsWith("https://public.api.bsky.app/xrpc/")))
            throw new IllegalArgumentException("Host API non autorizzato");
        HttpURLConnection c = (HttpURLConnection) new URL(source).openConnection();
        c.setInstanceFollowRedirects(false);
        c.setConnectTimeout(6500); c.setReadTimeout(6500);
        c.setRequestProperty("Accept", "application/json");
        c.se¶»§q«^