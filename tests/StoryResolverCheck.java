import com.storypocket.mobile.InstagramStoryResolver;
public class StoryResolverCheck {
 static int count=0;
 static void check(boolean v,String name){if(!v)throw new AssertionError(name);count++;}
 public static void main(String[] a){
  String page="https://www.instagram.com/stories/alice.user/123456789012345678/";
  check("123456789012345678".equals(InstagramStoryResolver.storyId(page)),"valid story");
  check(InstagramStoryResolver.storyId("https://www.instagram.com/reel/ABCDE/")==null,"not reel");
  check(InstagramStoryResolver.storyId("https://www.instagram.com/stories/alice.user/")==null,"story ID required");
  String p="{\"reels_media\":[{\"items\":["+
   "{\"pk\":\"999999999\",\"media_type\":2,\"video_versions\":[{\"url\":\"https://a.cdninstagram.com/bad.mp4\"}]},"+
   "{\"pk\":\"123456789012345678\",\"media_type\":2,\"image_versions2\":{\"candidates\":[{\"url\":\"https://a.cdninstagram.com/cover.jpg\"}]},\"video_versions\":["+
   "{\"url\":\"https://a.cdninstagram.com/small.mp4\",\"width\":240,\"height\":360},"+
   "{\"url\":\"https://a.cdninstagram.com/full.mp4\",\"width\":1080,\"height\":1920}]}]}]}";
  var result=InstagramStoryResolver.parseMetadata(p,"123456789012345678",false);
  check(result!=null&&result.mediaType.equals("video")&&result.mediaUrl.endsWith("full.mp4"),"exact story best video");
  check(InstagramStoryResolver.parseMetadata(p,"555555555",false)==null,"adjacent story rejected");
  String onlyCover="{\"items\":[{\"id\":\"123456789012345678_4567\",\¶»§q«^