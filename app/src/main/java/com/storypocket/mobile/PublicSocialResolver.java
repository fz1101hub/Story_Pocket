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
        c.setRequestProperty("User-Agent", "StoryPocket/2.3 (Android public media viewer)");
        try {
            if (c.getResponseCode() != 200) return null;
            if (c.getContentLengthLong() > 1100000) return null;
            try (InputStream in=c.getInputStream(); ByteArrayOutputStream buffer=new ByteArrayOutputStream()) {
                byte[] buf=new byte[4096];int n;
                while ((n=in.read(buf))!=-1) { buffer.write(buf,0,n); if(buffer.size()>1100000)return null; }
                return new JSONObject(buffer.toString(StandardCharsets.UTF_8));
            }
        } finally { c.disconnect(); }
    }
    private static Result candidate(String url, String type, String p, boolean videoOnly) {
        if (url==null || !SocialLinks.isTrustedMedia(url) || (videoOnly && !"video".equals(type))) return null;
        if (url.contains(".m3u8") || url.contains(".mpd") || url.contains(".m4s")) return null;
        return new Result(url,type,p);
    }
    public static Result fromBlueskyJson(JSONObject root, boolean videoOnly) {
        if(root==null)return null;
        JSONObject thread=root.optJSONObject("thread");
        JSONObject post=thread==null?null:thread.optJSONObject("post");
        JSONObject embed=post==null?null:post.optJSONObject("embed");
        if(embed==null)return null;
        JSONArray images=embed.optJSONArray("images");
        if(images!=null&&!videoOnly){
            for(int i=0;i<images.length();i++){
                JSONObject im=images.optJSONObject(i);
                Result result=candidate(im==null?null:im.optString("fullsize",null),"image","Bluesky",false);
                if(result!=null)return result;
            }
        }
        // Bluesky video post embeds currently expose HLS streams; don't label a playlist as MP4.
        return null;
    }
    public static Result fromMastodonJson(JSONObject post, boolean videoOnly) {
        if(post==null)return null;
        JSONArray media=post.optJSONArray("media_attachments");
        if(media==null)return null;
        for(int i=0;i<media.length();i++) {
            JSONObject m=media.optJSONObject(i);if(m==null)continue;
            String kind=m.optString("type", "").toLowerCase(Locale.ROOT);
            String type=(kind.equals("video")||kind.equals("gifv"))?"video":(kind.equals("image")?"image":null);
            if(type==null)continue;
            Result r=candidate(m.optString("url",null),type,"Mastodon",videoOnly);
            if(r!=null)return r;
        }
        return null;
    }
    public static Result fromRedditJson(String raw, boolean videoOnly) {
        if(raw==null)return null;
        try {
            JSONArray pages=new JSONArray(raw);
            JSONObject listing=pages.optJSONObject(0);
            JSONObject data=listing==null?null:listing.optJSONObject("data");
            JSONArray children=data==null?null:data.optJSONArray("children");
            JSONObject first=children==null?null:children.optJSONObject(0);
            JSONObject post=first==null?null:first.optJSONObject("data");
            if(post==null)return null;
            // A post's video fallback may have silent audio; never join unrelated streams.
            JSONObject secure=post.optJSONObject("secure_media");
            JSONObject video=secure==null?null:secure.optJSONObject("reddit_video");
            if(video==null){
                JSONObject media=post.optJSONObject("media");video=media==null?null:media.optJSONObject("reddit_video");
            }
            if(video!=null){
                Result r=candidate(video.optString("fallback_url",null),"video","Reddit",videoOnly);
                if(r!=null)return r;
            }
            if(!videoOnly){
                Result image=candidate(post.optString("url_overridden_by_dest",null),"image","Reddit",false);
                if(image!=null && image.mediaUrl.matches("(?i).*\\.(jpe?g|png|gif|webp)(\\?.*)?$")) return image;
                JSONObject metadata=post.optJSONObject("media_metadata");
                JSONObject gallery=post.optJSONObject("gallery_data");
                JSONArray items=gallery==null?null:gallery.optJSONArray("items");
                if(items!=null&&metadata!=null){
                    for(int i=0;i<items.length();i++){
                        JSONObject item=items.optJSONObject(i);
                        JSONObject imageData=item==null?null:metadata.optJSONObject(item.optString("media_id"));
                        JSONObject s=imageData==null?null:imageData.optJSONObject("s");
                        Result r=candidate(s==null?null:s.optString("u",null),"image","Reddit",false);
                        if(r!=null)return r;
                    }
                }
            }
        } catch(Exception ignored) { }
        return null;
    }
    public static Result resolve(String normalized, boolean onlyVideo) throws Exception {
        String p=SocialLinks.platform(normalized);
        if(p==null)return null;
        URI url=URI.create(SocialLinks.normalize(normalized));
        String path=url.getPath();
        if("Bluesky".equals(p)) {
            String[] s=path.split("/");
            if(s.length!=5 || !s[1].equals("profile") || !s[3].equals("post") ||
               !s[2].matches("[A-Za-z0-9._:-]{2,100}") || !s[4].matches("[A-Za-z0-9]{2,60}"))return null;
            JSONObject profile=getJson("https://public.api.bsky.app/xrpc/app.bsky.actor.getProfile?actor="+encoded(s[2]));
            String did=profile==null?null:profile.optString("did",null);
            if(did==null || !did.matches("did:[a-z0-9]+:[A-Za-z0-9._:%-]{3,180}"))return null;
            String at="at://"+did+"/app.bsky.feed.post/"+s[4];
            return fromBlueskyJson(getJson("https://public.api.bsky.app/xrpc/app.bsky.feed.getPostThread?uri="+encoded(at)),onlyVideo);
        }
        if("Mastodon".equals(p)) {
            // Matches /@account/id and /users/account/statuses/id on known Mastodon hosts.
            String id=null;
            String[] s=path.split("/");
            if(s.length==3 && s[1].startsWith("@") && s[2].matches("[0-9]{3,30}")) id=s[2];
            if(s.length==5 && s[1].equals("users") && s[3].equals("statuses") && s[4].matches("[0-9]{3,30}")) id=s[4];
            if(id==null)return null;
            return fromMastodonJson(getJson("https://"+url.getHost()+"/api/v1/statuses/"+id),onlyVideo);
        }
        if("Reddit".equals(p)){
            if(!path.matches("(?i)^/r/[A-Za-z0-9_]+/comments/[A-Za-z0-9]+(?:/[^?#]*)?/?$"))return null;
            String endpoint="https://www.reddit.com"+path.replaceAll("/+$","")+".json?raw_json=1";
            // Read a JSON array via a bounded request rather than treating it as a JSONObject.
            HttpURLConnection c=(HttpURLConnection)new URL(endpoint).openConnection();
            c.setInstanceFollowRedirects(false);c.setConnectTimeout(6500);c.setReadTimeout(6500);
            c.setRequestProperty("User-Agent","StoryPocket/2.3 (Android public media viewer)");
            try{
                if(c.getResponseCode()!=200 || c.getContentLengthLong()>1100000)return null;
                try(InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>1100000)return null;}
                    return fromRedditJson(out.toString(StandardCharsets.UTF_8),onlyVideo);
                }
            }finally{c.disconnect();}
        }
        return null;
    }
}
