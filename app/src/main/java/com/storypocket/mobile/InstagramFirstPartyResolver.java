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
            this.mediaUrl = url; this.mediaType = type; this.method = method; this.itemCount = itemCount;
        }
    }

    /** Can only work for content with a Post/Reel shortcode, not Story IDs. */
    public static String shortcode(String instagramUrl) {
        try {
            String path = URI.create(TargetParser.normalize(instagramUrl)).getPath();
            String[] pieces = path.split("/");
            if (pieces.length != 3 ||
                    !(pieces[1].equals("reel") || pieces[1].equals("p") || pieces[1].equals("tv"))) return null;
            return pieces[2];
        } catch (RuntimeException ignored) { return null; }
    }

    private static boolean usable(String url, boolean video) {
        if (!TargetParser.isTrustedMediaUrl(url)) return false;
        if (video) return VideoUrlPolicy.score(url) > 0;
        String lower = URI.create(url).getPath().toLowerCase(Locale.ROOT);
        return lower.matches(".*\\.(jpg|jpeg|png|webp|heic|avif)$") ||
            url.contains("dst-jpg") || url.contains("mime_type=image");
    }

    private static final class Candidate {
        String url; String type; int quality;
        Candidate(String url, String type, int quality) {
            this.url = url; this.type = type; this.quality = quality;
        }
    }

    private static Candidate bestFromVersions(JSONObject item, String listKey, boolean isVideo) {
        JSONArray arr = item.optJSONArray(listKey);
        if (arr == null && !isVideo) {
            JSONObject img = item.optJSONObject("image_versions2");
            if (img != null) arr = img.optJSONArray("candidates");
        }
        Candidate best = null;
        if (arr != null) {
            for (int i = 0; i < Math.min(arr.length(), 60); i++) {
                JSONObject entry = arr.optJSONObject(i);
                if (entry == null) continue;
                String url = entry.optString("url", "");
                if (!usable(url, isVideo)) continue;
                int quality = Math.max(1, entry.optInt("width", 1)) * Math.max(1, entry.optInt("height", 1));
                if (best == null || quality > best.quality) best = new Candidate(url, isVideo ? "video" : "image", quality);
            }
        }
        return best;
    }

    private static Candidate candidatesOf(JSONObject item, boolean preferVideo) {
        Candidate video = bestFromVersions(item, "video_versions", true);
        String directVideo = item.optString("video_url", "");
        if (video == null && usable(directVideo, true)) video = new Candidate(directVideo, "video", 1);
        if (video != null) return video;
        if (preferVideo) return null;
        Candidate img = bestFromVersions(item, "image_versions2", false);
        String directImage = item.optString("display_url", "");
        if (img == null && usable(directImage, false)) img = new Candidate(directImage, "image", 1);
        return img;
    }

    /** Parse only a shortcode-matched first-party media object. No related/suggested videos. */
    public static Result parseMetadata(String json, String code, boolean videoOnly) {
        if (json == null || json.length() > MAX_JSON) return null;
        try {
            JSONObject root = new JSONObject(json);
            JSONArray items = root.optJSONArray("items");
            if (items != null) {
                for (int i = 0; i < Math.min(items.length(), 8); i++) {
                    JSONObject obj = items.optJSONObject(i);
                    if (obj == null || !code.equals(obj.optString("code", ""))) continue;
                    return parseItem(obj, videoOnly, "Instagram: dati del contenuto");
                }
            }
            JSONObject data = root.optJSONObject("data");
            JSONObject match = data == null ? null : data.optJSONObject("xdt_shortcode_media");
            if (match == null && data != null) match = data.optJSONObject("shortcode_media");
            if (match == null) {
                JSONObject graphql = root.optJSONObject("graphql");
                if (graphql != null) match = graphql.optJSONObject("shortcode_media");
            }
            if (match != null && code.equals(match.optString("shortcode", "")))
                return parseItem(match, videoOnly, "Instagram: media del Post/Reel");
        } catch (Exception ignored) { }
        return null;
    }

    private static Result parseItem(JSONObject item, boolean videoOnly, String method) {
        JSONArray carousel = item.optJSONArray("carousel_media");
        if (carousel == null) {
            JSONObject edge = item.optJSONObject("edge_sidecar_to_children");
            if (edge != null) {
                JSONArray edges = edge.optJSONArray("edges");
                if (edges != null) {
                    carousel = new JSONArray();
                    for (int i = 0; i < Math.min(edges.length(), 40); i++) {
                        JSONObject e = edges.optJSONObject(i);
                        if (e != null && e.optJSONObject("node") != null) carousel.put(e.optJSONObject("node"));
                    }
                }
            }
        }
        if (carousel != null && carousel.length() > 0) {
            for (int i = 0; i < Math.min(carousel.length(), 40); i++) {
                JSONObject entry = carousel.optJSONObject(i);
                if (entry == null) continue;
                Candidate c = candidatesOf(entry, videoOnly);
                if (c != null) return new Result(c.url, c.type, method + " (elemento " + (i+1) + ")", carousel.length());
            }
        }
        Candidate c = candidatesOf(item, videoOnly);
        return c == null ? null : new Result(c.url, c.type, method, 1);
    }

    /** Minimal GET with strict HTTPS, no redirects, bounded response; only first-party host. */
    private static String fetchInstagram(String url, String cookie, String userAgent, boolean isJson) throws Exception {
        HttpURLConnection conn = null;
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !"www.instagram.com".equalsIgnoreCase(uri.getHost()) ||
                uri.getPort() != -1 || uri.getUserInfo() != null) throw new IllegalArgumentException("Host non valido");
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(7000);
            conn.setRequestProperty("User-Agent", userAgent == null ? "Mozilla/5.0" : userAgent);
            conn.setRequestProperty("Accept", isJson ? "application/json" : "text/html");
            conn.setRequestProperty("Referer", "https://www.instagram.com/");
            if (isJson) conn.setRequestProperty("X-IG-App-ID", "936619743392459");
            // Only the existing Instagram WebView cookie is allowed. Never sent to a CDN or helper service.
            if (cookie != null && !cookie.isEmpty()) conn.setRequestProperty("Cookie", cookie);
            if (conn.getResponseCode() != 200) return null;
            String ctype = conn.getContentType();
            if (ctype == null || !(ctype.toLowerCase(Locale.ROOT).contains(isJson ? "json" : "html"))) return null;
            if (conn.getContentLengthLong() > MAX_JSON) return null;
            try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; int read;
                while ((read = in.read(buffer)) != -1) {
                    if (out.size() + read > MAX_JSON) return null;
                    out.write(buffer, 0, read);
                }
                return out.toString(StandardCharsets.UTF_8.name());
            }
        } finally { if (conn != null) conn.disconnect(); }
    }

    public static Result resolve(String instagramUrl, String cookie, String userAgent, boolean videoOnly) throws Exception {
        String code = shortcode(instagramUrl);
        if (code == null) return null;
        String api = "https://www.instagram.com/api/v1/media/shortcode/" + code + "/info/";
        String json = fetchInstagram(api, cookie, userAgent, true);
        Result apiResult = parseMetadata(json, code, videoOnly);
        if (apiResult != null) return apiResult;
        // Separate public Instagram embed-page attempt, no external providers or cookie sharing.
        String base = URI.create(TargetParser.normalize(instagramUrl)).getPath();
        String html = fetchInstagram("https://www.instagram.com" + base + "embed/captioned/", cookie, userAgent, false);
        if (html == null) return null;
        Matcher m = META_VIDEO.matcher(html);
        while (m.find()) {
            String u = m.group(1).replace("&amp;", "&").replace("&#38;", "&");
            if (usable(u, true)) return new Result(u, "video", "Instagram: pagina incorporata", 1);
        }
        return null;
    }
}
