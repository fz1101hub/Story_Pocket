package com.storypocket.mobile;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small, reviewable social URL allowlist. It never accepts arbitrary web hosts. */
public final class SocialLinks {
    private static final Pattern URL = Pattern.compile("(?i)https?://[^\\s<>\\\"'\\u200b]+", Pattern.CASE_INSENSITIVE);
    private static final String[] SOCIAL_HOSTS = {
        "tiktok.com", "vm.tiktok.com", "vt.tiktok.com", "facebook.com", "fb.watch",
        "pinterest.com", "pin.it", "reddit.com", "redd.it", "bsky.app",
        "threads.net", "threads.com", "x.com", "twitter.com", "t.co",
        "youtube.com", "youtu.be", "snapchat.com", "tumblr.com", "vk.com",
        "linkedin.com", "vimeo.com", "dailymotion.com", "twitch.tv",
        "telegram.me", "t.me", "pixelfed.social", "mastodon.social",
        "mastodon.online", "mastodon.world", "mstdn.social", "fosstodon.org",
        "mas.to", "hachyderm.io", "infosec.exchange", "imgur.com"
    };
    private static final String[] MEDIA_HOSTS = {
        "i.redd.it", "preview.redd.it", "v.redd.it", "redditmedia.com",
        "cdn.bsky.app", "video.bsky.app", "bsky.app",
        "files.mastodon.social", "media.mastodon.social", "mastodon.social",
        "files.mastodon.online", "mastodon.online", "media.mstdn.social",
        "mstdn.social", "media.mas.to", "mas.to", "files.mastodon.world",
        "mastodon.world", "i.pinimg.com", "media.tumblr.com", "64.media.tumblr.com"
    };
    private SocialLinks() { }

    private static String host(String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null ||
                (uri.getPort() != -1 && uri.getPort() != 443)) return null;
            String h = uri.getHost();
            return h == null ? null : h.toLowerCase(Locale.ROOT);
        } catch (RuntimeException ignored) { return null; }
    }
    private static boolean matches(String host, String expected) {
        // Never let untrusted lookalikes (e.g. reddit.com.evil.tld) through.
        return host != null && (host.equals(expected) || host.endsWith("." + expected));
    }
    public static boolean isInstagram(String value) { return TargetParser.isInstagramUrl(value); }
    public static boolean isSupported(String value) {
        if (isInstagram(value)) return true;
        String h = host(value);
        if (h == null) return false;
        for (String accepted : SOCIAL_HOSTS) if (matches(h, accepted)) return true;
        return false;
    }
    public static boolean isTrustedMedia(String value) {
        String h = host(value);
        if (h == null) return false;
        for (String accepted : MEDIA_HOSTS) if (matches(h, accepted)) return true;
        return false;
    }
    public static String platform(String value) {
        if (isInstagram(value)) return "Instagram";
        String h = host(value);
        if (h == null) return null;
        if (matches(h,"tiktok.com")) return "TikTok";
        if (matches(h,"facebook.com") || matches(h,"fb.watch")) return "Facebook";
        if (matches(h,"pinterest.com") || matches(h,"pin.it")) return "Pinterest";
        if (matches(h,"reddit.com") || matches(h,"redd.it")) return "Reddit";
        if (matches(h,"bsky.app")) return "Bluesky";
        if (matches(h,"threads.net") || matches(h,"threads.com")) return "Threads";
        if (matches(h,"x.com") || matches(h,"twitter.com") || matches(h,"t.co")) return "X";
        if (matches(h,"youtube.com") || matches(h,"youtu.be")) return "YouTube";
        if (matches(h,"snapchat.com")) return "Snapchat";
        if (matches(h,"tumblr.com")) return "Tumblr";
        if (matches(h,"vk.com")) return "VK";
        if (matches(h,"linkedin.com")) return "LinkedIn";
        if (matches(h,"vimeo.com")) return "Vimeo";
        if (matches(h,"dailymotion.com")) return "Dailymotion";
        if (matches(h,"twitch.tv")) return "Twitch";
        if (matches(h,"telegram.me") || matches(h,"t.me")) return "Telegram";
        if (matches(h,"imgur.com")) return "Imgur";
        if (matches(h,"pixelfed.social")) return "Pixelfed";
        for (String instance : new String[]{"mastodon.social", "mastodon.online", "mastodon.world", "mstdn.social", "fosstodon.org", "mas.to", "hachyderm.io", "infosec.exchange"}) {
            if (matches(h, instance)) return "Mastodon";
        }
        return null;
    }
    public static boolean supportsPublicResolver(String value) {
        String p = platform(value);
        return "Bluesky".equals(p) || "Mastodon".equals(p) || "Reddit".equals(p);
    }
    public static String normalize(String raw) {
        if (raw == null || raw.length() > 8192) throw new IllegalArgumentException("Link troppo lungo o non valido.");
        String value = raw.trim();
        if (value.startsWith("@") || value.matches("[A-Za-z0-9._]{1,30}")) return TargetParser.normalize(value);
        if (!value.contains("://") && value.matches("(?i)(?:www\\.|m\\.|vm\\.|vt\\.)?[a-z0-9.-]+\\.[a-z]{2,}/.+"))
            value = "https://" + value;
        if (TargetParser.isInstagramUrl(value)) return TargetParser.normalize(value);
        try {
            URI uri = URI.create(value);
            if (!isSupported(value) || uri.getHost() == null) throw new IllegalArgumentException();
            String path = uri.getRawPath();
            if (path == null || path.isEmpty()) path = "/";
            if (path.contains("\\") || path.contains("//")) throw new IllegalArgumentException();
            // Keep essential IDs and query params, but never a fragment or credentials.
            URI clean = new URI("https", null, uri.getHost().toLowerCase(Locale.ROOT), -1,
                uri.getPath() == null || uri.getPath().isEmpty() ? "/" : uri.getPath(), uri.getQuery(), null);
            return clean.toASCIIString();
        } catch (Exception ignored) { throw new IllegalArgumentException("Inserisci un link di uno dei social supportati."); }
    }
    public static String extract(String message) {
        if (message == null) return null;
        String text = message.replace("&amp;", "&").replace("\u200b", "");
        Matcher m = URL.matcher(text);
        while (m.find()) {
            String raw = m.group();
            while (!raw.isEmpty() && ".,;!)]}>".indexOf(raw.charAt(raw.length()-1)) >= 0)
                raw = raw.substring(0, raw.length()-1);
            try { return normalize(raw); } catch (IllegalArgumentException ignored) { }
        }
        try { return normalize(text); } catch (IllegalArgumentException ignored) { return null; }
    }
    public static String fileTag(String value) {
        String p = platform(value);
        return p == null ? "social" : p.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
