import Foundation

@main
struct SocialURLCheck {
    static func main() {
        func expect(_ input: String, _ expected: String?) {
            let actual = SocialURL.extract(input)?.absoluteString
            guard actual == expected else {
                fatalError("Unexpected URL for \(input): \(actual ?? "nil")")
            }
        }
        expect("https://www.instagram.com/reel/abc", "https://www.instagram.com/reel/abc")
        expect("Shared: https://example.org/post then https://bsky.app/profile/test/post/1",
               "https://bsky.app/profile/test/post/1")
        expect("HTTPS://WWW.REDDIT.COM/r/test", "HTTPS://WWW.REDDIT.COM/r/test")
        expect("https://instagram.com.evil.example/post", nil)
        expect("http://instagram.com/post", nil)
        expect("https://user:password@instagram.com/post", nil)
        expect("https://instagram.com:8080/post", nil)
        expect("https://instagram.com/post#fragment", nil)
        // Extraction intentionally accepts a URL token before surrounding prose.
        precondition(SocialURL.normalize("https://instagram.com/a b") == nil)
        precondition(SocialURL.normalize("https://instagram.com/a\npath") == nil)
        expect("https://instagram.com/a\npath", "https://instagram.com/a")
        expect("  https://instagram.com/p/abc  ", "https://instagram.com/p/abc")
        expect("https://instagram.com:443/post", "https://instagram.com:443/post")
        expect("https://www.instagram.com/p/abc?utm_source=share", "https://www.instagram.com/p/abc?utm_source=share")
        expect("A link (https://www.instagram.com/p/abc).", "https://www.instagram.com/p/abc")
        expect("Watch this! https://www.instagram.com/reel/abc!", "https://www.instagram.com/reel/abc")
        expect("Link: https://bsky.app/profile/test/post/1?", "https://bsky.app/profile/test/post/1")
        expect("Video https://youtu.be/abc:", "https://youtu.be/abc")
        func expectIncoming(_ input: String, _ expected: String?) {
            guard let url = URL(string: input) else { fatalError("Invalid test URL: \(input)") }
            let actual = SocialURL.incoming(url)?.absoluteString
            guard actual == expected else {
                fatalError("Unexpected incoming URL for \(input): \(actual ?? "nil")")
            }
        }
        expectIncoming("https://www.instagram.com/reel/abc", "https://www.instagram.com/reel/abc")
        expectIncoming("storypocket://open?url=https%3A%2F%2Fwww.instagram.com%2Freel%2Fabc",
                       "https://www.instagram.com/reel/abc")
        expectIncoming("storypocket://open?url=https%3A%2F%2Fexample.org%2Fpost", nil)
        expectIncoming("storypocket://open?url=http%3A%2F%2Finstagram.com%2Fp%2Fabc", nil)
        expectIncoming("storypocket://open?url=https%3A%2F%2Finstagram.com.evil.test%2Fp", nil)
        expectIncoming("storypocket://open?url=https%3A%2F%2Finstagram.com%2Fp&url=https%3A%2F%2Fx.com", nil)
        expectIncoming("storypocket://open?url=https%3A%2F%2Finstagram.com%2Fp&unused=1", nil)
        expectIncoming("storypocket://open?url=https%3A%2F%2Finstagram.com%2Fp#fragment", nil)
        expectIncoming("storypocket://open?URL=https%3A%2F%2Finstagram.com%2Fp", nil)
        expectIncoming("storypocket://evil?url=https%3A%2F%2Finstagram.com%2Fp", nil)
        expectIncoming("storypocket://open/other?url=https%3A%2F%2Finstagram.com%2Fp", nil)
        func expectBrowser(_ input: String, _ allowed: Bool) {
            guard let url = URL(string: input) else { fatalError("Invalid browser test URL") }
            precondition(SocialURL.allowsBrowserNavigation(url) == allowed)
        }
        expectBrowser("https://instagram.com/p/abc", true)
        expectBrowser("https://cdn.example.org/video.mp4", true)
        expectBrowser("https://example.org:443/path", true)
        expectBrowser("https://example.org:8443/path", false)
        expectBrowser("https://user:pass@example.org/path", false)
        expectBrowser("http://instagram.com/p/abc", false)
        expectBrowser("javascript:alert(1)", false)
        expectBrowser("file:///private/tmp/test.jpg", false)
        expectBrowser("about:blank", true)
        expectBrowser("about:srcdoc", false)
        expectBrowser("data:text/html,hello", false)
        expectBrowser("blob:https://instagram.com/id", false)
        expectBrowser("https://example.org:444/", false)
        expectBrowser("https://example.org:443/", true)
        expectBrowser("https://user@example.org/", false)
        expectBrowser("https://example.org/#fragment", true)
        print("SocialURL checks passed")
    }
}
