package com.storypocket.mobile;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

/** URL allowlist; no external hosts, arbitrary schemes or credentials. */
public final class TargetParser {
    private static final Pattern USER = Pattern.compile("[A-Za-z0-9._]{1,30}");
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9_-]{3,80}");
    private static final Pattern STORY_ID = Pattern.compile("[0-9]{1,30}");
    private TargetParser() { }

    private static String hostOf(URI uri) {
        return uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
    }
    private static boolean safeHttps(URI uri) {
        return "https".equalsIgnoreCase(uri.getScheme()) && uri.getUserInfo() == null &&
            (uri.getPort() == -1 || uri.getPort() == 443);
    }
    private static boolean instagramHost(String host) {
        return host.equals("instagram.com") || host.equals("www.instagram.com") ||
            host.equals("m.instagram.com") || host.equals("instagr.am") || host.equals("www.instagr.am");
    }

    public static String normalize(String raw) {
        if (raw == null) throw new IllegalArgumentException("Inserisci un nome utente o un link Instagram.");
        String text = raw.trim();
        if (text.startsWith("@")) text = text.substring(1);
        if (USER.matcher(text).matches() && !reserved(text)) {
            return "https://www.instagram.com/" + text + "/";
        }
        if (!text.contains("://") &¶»§q«^