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
        if (!text.contains("://") && text.matches("(?i)(?:www\\.|m\\.)?(?:instagram\\.com|instagr\\.am)/.+")) {
            text = "https://" + text;
        }
        try {
            URI uri = URI.create(text);
            if (!safeHttps(uri) || !instagramHost(hostOf(uri))) throw new IllegalArgumentException();
            String path = uri.getRawPath() == null ? "" : uri.getRawPath();
            if (path.contains("//") || path.contains("%") || path.contains("\\\\")) throw new IllegalArgumentException();
            String[] p = path.split("/", -1);
            if (p.length < 2 || p[1].isEmpty()) throw new IllegalArgumentException();
            int n = p.length;
            if (n > 2 && p[n-1].isEmpty()) n--;
            String section = p[1].toLowerCase(Locale.ROOT);
            if (section.equals("stories")) {
                if ((n != 3 && n != 4) || !USER.matcher(p[2]).matches() ||
                    (n == 4 && !STORY_ID.matcher(p[3]).matches())) throw new IllegalArgumentException();
                return "https://www.instagram.com/stories/" + p[2] + "/" + (n == 4 ? p[3] + "/" : "");
            }
            if (section.equals("p") || section.equals("reel") || section.equals("reels") || section.equals("tv")) {
                if (n != 3 || !CODE.matcher(p[2]).matches()) throw new IllegalArgumentException();
                return "https://www.instagram.com/" + (section.equals("reels") ? "reel" : section) + "/" + p[2] + "/";
            }
            if (section.equals("share")) {
                if (n != 4 || !CODE.matcher(p[3]).matches() ||
                    !(p[2].equalsIgnoreCase("p") || p[2].equalsIgnoreCase("reel") ||
                      p[2].equalsIgnoreCase("story"))) throw new IllegalArgumentException();
                return "https://www.instagram.com/share/" + p[2].toLowerCase(Locale.ROOT) + "/" + p[3] + "/";
            }
            if (n != 2 || !USER.matcher(p[1]).matches() || reserved(p[1])) throw new IllegalArgumentException();
            return "https://www.instagram.com/" + p[1] + "/";
        } catch (RuntimeException ignored) {
            throw new IllegalArgumentException("Usa un link Instagram a Story, Post, Reel o profilo.");
        }
    }

    private static boolean reserved(String value) {
        String s = value.toLowerCase(Locale.ROOT);
        return s.equals("stories") || s.equals("p") || s.equals("reel") || s.equals("reels") ||
            s.equals("tv") || s.equals("share") || s.equals("accounts") || s.equals("explore");
    }

    public static boolean isInstagramUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = hostOf(uri);
            return safeHttps(uri) && (host.equals("instagram.com") || host.endsWith(".instagram.com") ||
                host.equals("instagr.am") || host.equals("www.instagr.am"));
        } catch (RuntimeException ignored) { return false; }
    }
    public static boolean isStoryUrl(String url) {
        try {
            String path = URI.create(normalize(url)).getPath();
            return path.matches("/stories/[A-Za-z0-9._]{1,30}/([0-9]{1,30}/)?");
        } catch (RuntimeException ignored) { return false; }
    }
    public static boolean isDirectStoryUrl(String url) {
        try { return URI.create(normalize(url)).getPath().matches("/stories/[A-Za-z0-9._]{1,30}/[0-9]{1,30}/"); }
        catch (RuntimeException ignored) { return false; }
    }
    public static boolean isShareUrl(String url) {
        try { return URI.create(normalize(url)).getPath().matches("/share/(p|reel|story)/[A-Za-z0-9_-]{3,80}/"); }
        catch (RuntimeException ignored) { return false; }
    }
    public static boolean isMediaPage(String url) {
        try {
            String path = URI.create(normalize(url)).getPath();
            return path.matches("/(p|reel|tv)/[A-Za-z0-9_-]{3,80}/") ||
                path.matches("/stories/[A-Za-z0-9._]{1,30}/[0-9]{1,30}/");
        } catch (RuntimeException ignored) { return false; }
    }
    public static boolean canAutoDownload(String url) { return isMediaPage(url) || isShareUrl(url); }
    public static boolean matchesSharedContent(String currentUrl, String expected) {
        try {
            String cur = normalize(currentUrl), exp = normalize(expected);
            if (cur.equals(exp) && isMediaPage(cur)) return true;
            if (!isShareUrl(exp) || !isMediaPage(cur)) return false;
            String section = URI.create(exp).getPath().split("/")[2];
            String target = URI.create(cur).getPath();
            if (section.equals("p")) return target.startsWith("/p/");
            if (section.equals("reel")) return target.startsWith("/reel/") || target.startsWith("/tv/");
            return target.startsWith("/stories/");
        } catch (RuntimeException ignored) { return false; }
    }
    public static String fileTag(String url) {
        try {
            String[] s = URI.create(normalize(url)).getPath().split("/");
            if (s.length >= 4 && s[1].equals("stories")) return "story_" + s[2];
            if (s.length >= 3 && (s[1].equals("reel") || s[1].equals("p") || s[1].equals("tv")))
                return (s[1].equals("p") ? "post" : "reel") + "_" + s[2];
        } catch (RuntimeException ignored) { }
        return "instagram";
    }
    public static String storyUsername(String url) {
        try {
            String[] p = URI.create(normalize(url)).getPath().split("/");
            if (p.length >= 3 && p[1].equals("stories")) return p[2];
        } catch (RuntimeException ignored) { }
        return "instagram";
    }
    public static boolean isTrustedMediaUrl(String url) {
        try {
            URI uri = URI.create(url);
            String h = hostOf(uri);
            if (!safeHttps(uri)) return false;
            return h.equals("instagram.com") || h.endsWith(".instagram.com")
                || h.equals("cdninstagram.com") || h.endsWith(".cdninstagram.com")
                || h.equals("fbcdn.net") || h.endsWith(".fbcdn.net")
                || h.equals("instagram.net") || h.endsWith(".instagram.net");
        } catch (RuntimeException ignored) { return false; }
    }
    public static boolean useInstagramCookies(String url) {
        try {
            String h = hostOf(URI.create(url));
            return h.equals("instagram.com") || h.endsWith(".instagram.com");
        } catch (RuntimeException ignored) { return false; }
    }
}
