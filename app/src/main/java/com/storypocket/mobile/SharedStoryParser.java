package com.storypocket.mobile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Interpreta il testo ricevuto dal menu Condividi senza accettare host esterni. */
public final class SharedStoryParser {
    private static final Pattern IG_URL = Pattern.compile(
        "(?i)(?<![A-Za-z0-9@._/-])(?:https://)?(?:(?:www\\.|m\\.)?instagram\\.com|(?:www\\.)?instagr\\.am)/[^\\s<>\\\"'\\u200b]+"
    );
    private SharedStoryParser() { }
    public static String extract(String message) {
        if (message == null || message.isEmpty()) return null;
        String text = message.replace("&amp;", "&").replace("\\u200b", "");
        Matcher m = IG_URL.matcher(text);
        String fallback = null;
        while (m.find()) {
            String url = trimPunctuation(m.group());
            try {
                String normalized = TargetParser.normalize(url);
                if (TargetParser.canAutoDownload(normalized)) return normalized;
                if (fallback == null) fallback = normalized;
            } catch (IllegalArgumentException ignored) { }
        }
        if (fallback == null && text.trim().matches("@?[A-Za-z0-9._]{1,30}")) {
            try { fallback = TargetParser.normalize(text.trim()); }
            catch (IllegalArgumentException ignored) { }
        }
        return fallback;
    }
    private static String trimPunctuation(String url) {
        while (!url.isEmpty()) {
            char c = url.charAt(url.length() - 1);
            if (c == '¶»§q«^