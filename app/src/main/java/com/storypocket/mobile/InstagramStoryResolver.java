package com.storypocket.mobile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * First-party, Story-ID-matched fallback. Uses only www.instagram.com and its
 * existing WebView login. Does not call external downloader services or copy
 * media belonging to adjacent Stories in the author's sequence.
 * Instagram endpoints are undocumented; requests may be refused.
 */
public final class InstagramStoryResolver {
    private static final Pattern STORY_PATH = Pattern.compile("^/stories/[A-Za-z0-9._]{1,30}/([0-9]{1,30})/$");
    private static final int MAX_BODY_BYTES = 2 * 1024 * 1024;
    private InstagramStoryResolver() { }

    public static final class Result {
        public final String mediaUrl;
        public final String mediaType;
        public final String method;
        Result(String u, String t, String m) { mediaUrl = u; mediaType = t; method = m; }
    }

    public static String storyId(String page) {
        try {
            String path = URI.create(TargetParser.normalize(page)).getPath();
            Matcher m = STORY_PATH.matcher(path);
            return m.matches() ? m.group(1) : null;
        } catch (RuntimeException ignored) { return null; }
    }

    private static bo¶»§q«^