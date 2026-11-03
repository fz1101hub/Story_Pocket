import com.storypocket.mobile.NavigationPolicy;
import static com.storypocket.mobile.NavigationPolicy.Decision;
public final class NavigationPolicyCheck {
  private static int n;
  private static void ok(String url, boolean main, Decision expected) {
    Decision got=NavigationPolicy.decide(url,main);
    if(got!=expected) throw new AssertionError(url+" expected "+expected+" got "+got);
    n++;
  }
  public static void main(String[] args) {
    ok("https://www.instagram.com/reel/ABC123/",true,Decision.IN_WEBVIEW);
    ok("https://instagram.com/stories/user/123456/",true,Decision.IN_WEBVIEW);
    ok("https://www.facebook.com/login",true,Decision.IN_WEBVIEW);
    ok("https://www.reddit.com/r/cats/comments/abc/",true,Decision.IN_WEBVIEW);
    ok("https://bsky.app/profile/a/post/123",true,Decision.IN_WEBVIEW);
    ok("https://vm.tiktok.com/123",true,Decision.IN_WEBVIEW);
    ok("https://www.pinterest.com/pin/123",true,Decision.IN_WEBVIEW);
    ok("intent://instagram.com/reel/x/#Intent;scheme=instagram;end",true,Decision.BLOCK_APP_LINK);
    ok("instagram://media?id=123",true,Decision.BLOCK_APP_LINK);
    ok("fb://profile/123",true,Decision.BLOCK_APP_LINK);
    ok("market://details?id=a.b",true,Decision.BLOCK_APP_LINK);
    ok("https://evil.example/",true,Decision.BLOCK_EXTERNAL_WEB);
    ok("https://instagram.com.evil.example/reel/ABC123/",true,Decision.BLOCK_EXTERNAL_WEB);
    ok("http://instagram.com/reel/ABC123/",true,Decision.BLOCK_EXTERNAL_WEB);
    ok("https://user@instagram.com/reel/ABC123/",true,Decision.BLOCK_EXTERNAL_WEB);
    ok("javascript:alert(1)",true,Decision.BLOCK_UNSAFE_SCHEME);
    ok("file:///etc/passwd",true,Decision.BLOCK_UNSAFE_SCHEME);
    ok("content://com.example/data",true,Decision.BLOCK_UNSAFE_SCHEME);
    ok("about:blank",true,Decision.IN_WEBVIEW);
    ok(null,true,Decision.BLOCK_UNSAFE_SCHEME);
    System.out.println("PASS: "+n+" test navigazione multisocial");
  }
}
