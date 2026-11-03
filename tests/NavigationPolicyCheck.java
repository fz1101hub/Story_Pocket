import com.storypocket.mobile.NavigationPolicy;
import static com.storypocket.mobile.NavigationPolicy.Decision;

public final class NavigationPolicyCheck {
    private static int tests;
    private static void expect(String url, boolean main, Decision expected) {
        Decision result = NavigationPolicy.decide(url, main);
        if (result != expected) throw new AssertionError(url + " expected " + expected + ", found " + result);
        tests++;
    }
    public static void main(String[] args) {
        expect("https://www.instagram.com/reel/DdrO2oPoISO/", true, Decision.IN_WEBVIEW);
        expect("https://m.instagram.com/p/ABC123/", true, Decision.IN_WEBVIEW);
        expect("https://instagram.com/stories/test/123/", true, Decision.IN_WEBVIEW);
        expect("https://www.instagr.am/p/ABC123/", true, Decision.IN_WEBVIEW);
        expect("https://instagram.com/reel/ABC123/?igsh=abc", true, Decision.IN_WEBVIEW);
        expect("intent://instagram.com/reel/foo/#Intent;scheme=instagram;package=com.instagram.android;end", true, Decision.BLOCK_APP_LINK);
        expect("instagram://media?id=123", true, Decision.BLOCK_APP_LINK);
        expect("fb://profile/1", true, Decision.BLOCK_APP_LINK);
        expect("market://details?id=com.instagram.android", true, Decision.BLOCK_APP_LINK);
        expect("https://www.facebook.com/login", true, Decision.BLOCK_EXTERNAL_WEB);
        expect("https://evil.com/", true, Decision.BLOCK_EXTERNAL_WEB);
        expect("https://instagram.com.ev¶»§q«^