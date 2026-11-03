package com.storypocket.mobile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An independent, first-party-only resolver for Instagram Post/Reel metadata.
 *
 * Not affiliated with Instagram. The metadata endpoints are undocumented and
 * may reject all requests or change without notice. Never sends session cookies
 * to anyone other than www.instagram.com. Returns only verified Instagram/Meta
 * CDN links; file signatures are checked later by the existing downloader.
 */
public final class InstagramFirstPartyResolver {
    private static final int MAX_JSON = 2 * 1024 * 1024;
    private static final Pattern META_VIDEO = Pattern.compile(
        "<meta\\s+[^>]*?(?:property|name)\\s*=\\s*['\\\"]og:video(?::url)?['\\\"][^>]*?content\\s*=\\s*['\\\"]([^'\\\"]+)['\\\"]",
        Pattern.CASE_INSENSITIVE);
    private InstagramFirstPartyResolver() { }

    public static final class Result {
        public final String mediaUrl;
        public final String mediaType;
        public final String method;
        public final int itemCount;
        Result(String url, String type, String method, int itemCount) {
            this.mediaUrl = url; this.¶»§q«^