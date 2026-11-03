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

    private static boolean sameId(String raw, String id) {
        return id != null && raw != null && (raw.equals(id) || raw.startsWith(id + "_"));
    }

    private static final class Media {
        String url, type;
        long quality;
        Media(String u, String t, long q) { url=u;type=t;quality=q; }
    }

    private static Media bestVersions(JSONArray arr, boolean video) {
        if (arr == null) return null;
        Media best = null;
        for (int i=0; i<Math.min(arr.length(),60); i++) {
            JSONObject obj = arr.optJSONObject(i);
            if (obj == null) continue;
            String u=obj.optString("url", "");
            if (!TargetParser.isTrustedMediaUrl(u)) continue;
            if (video && VideoUrlPolicy.score(u) <= 0) continue;
            long w=Math.max(1,obj.optInt("width",1)),h=Math.max(1,obj.optInt("height",1));
            long q=Math.min(100000000L,w*h);
            if (best == null || q > best.quality) best=new Media(u,video?"video":"image",q);
        }
        return best;
    }

    private static Media parseMatched(JSONObject obj, boolean videoOnly) {
        JSONArray videos=obj.optJSONArray("video_versions");
        boolean declaredVideo=obj.optInt("media_type",0)==2 || obj.optBoolean("is_video",false)
            || videos!=null || obj.has("video_url");
        if (declaredVideo) {
            Media best=bestVersions(videos,true);
            if (best != null) return best;
            String raw=obj.optString("video_url","");
            if (TargetParser.isTrustedMediaUrl(raw) && VideoUrlPolicy.score(raw)>0)
                return new Media(raw,"video",1);
            return null; // Never use image_versions2 of a video as a 'saved video'.
        }
        if (videoOnly) return null;
        JSONObject versions=obj.optJSONObject("image_versions2");
        if (versions != null) {
            Media img=bestVersions(versions.optJSONArray("candidates"),false);
            if (img != null) return img;
        }
        String direct=obj.optString("display_url","");
        if (TargetParser.isTrustedMediaUrl(direct)) return new Media(direct,"image",1);
        return null;
    }

    /** Bounded traversal: match an item id/pk exactly, never just nearby text. */
    private static Media visit(Object node, String wanted, boolean videoOnly, int depth, int[] budget) {
        if (node == null || depth>24 || --budget[0]<0) return null;
        if (node instanceof JSONObject) {
            JSONObject obj=(JSONObject) node;
            if (sameId(obj.optString("pk",""),wanted) || sameId(obj.optString("id",""),wanted)) {
                return parseMatched(obj,videoOnly);
            }
            JSONArray names=obj.names();
            if (names == null) return null;
            for (int i=0; i<Math.min(names.length(),240); i++) {
                Object child=obj.opt(names.optString(i));
                if (child instanceof JSONObject || child instanceof JSONArray) {
                    Media m=visit(child,wanted,videoOnly,depth+1,budget);
                    if (m!=null) return m;
                }
            }
        } else if (node instanceof JSONArray) {
            JSONArray list=(JSONArray) node;
            for (int i=0;i<Math.min(list.length(),250);i++) {
                Object child=list.opt(i);
                if (child instanceof JSONObject || child instanceof JSONArray) {
                    Media m=visit(child,wanted,videoOnly,depth+1,budget);
                    if (m!=null) return m;
                }
            }
        }
        return null;
    }

    public static Result parseMetadata(String json,String id,boolean videoOnly) {
        if (json==null || id==null || !id.matches("[0-9]{1,30}") || json.length()>MAX_BODY_BYTES)
            return null;
        try {
            Media m=visit(new JSONObject(json),id,videoOnly,0,new int[]{30000});
            return m==null?null:new Result(m.url,m.type,"Instagram: dati della Story");
        } catch(Exception ignored) { return null; }
    }

    private static String request(String id,String cookie,String userAgent) throws Exception {
        String address="https://www.instagram.com/api/v1/media/"+id+"/info/";
        HttpURLConnection conn=null;
        try {
            URI u=URI.create(address);
            if (!"https".equals(u.getScheme()) || !"www.instagram.com".equals(u.getHost()))
                throw new IllegalArgumentException("Host non attendibile");
            conn=(HttpURLConnection)new URL(address).openConnection();
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(7000);
            conn.setRequestProperty("Accept","application/json");
            conn.setRequestProperty("Referer","https://www.instagram.com/");
            conn.setRequestProperty("User-Agent",userAgent==null?"Mozilla/5.0":userAgent);
            conn.setRequestProperty("X-IG-App-ID","936619743392459");
            // Session cookie is sent ONLY to the Instagram-owned host above.
            if(cookie!=null&&!cookie.isEmpty())conn.setRequestProperty("Cookie",cookie);
            if(conn.getResponseCode()!=200)return null;
            String type=conn.getContentType();
            if(type==null||!type.toLowerCase(Locale.ROOT).contains("json") ||
                conn.getContentLengthLong()>MAX_BODY_BYTES)return null;
            try(InputStream in=conn.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] b=new byte[8192];int n;
                while((n=in.read(b))!=-1) {
                    if(out.size()+n>MAX_BODY_BYTES)return null;
                    out.write(b,0,n);
                }
                return out.toString(StandardCharsets.UTF_8.name());
            }
        }finally { if(conn!=null)conn.disconnect(); }
    }

    public static Result resolve(String page,String cookie,String userAgent,boolean videoOnly) throws Exception {
        String id=storyId(page);
        if(id==null)return null;
        return parseMetadata(request(id,cookie,userAgent),id,videoOnly);
    }
}
