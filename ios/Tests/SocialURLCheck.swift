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
        expect("A link (https://www.instagram.com/p/abc).", "https://www.instagram.com/p/abc")
        func expectIncoming(_ input: String, _ expected: String?) {
            guard let url = URL(string: input) else { fatalError("Invalid test URL: \\(input)") }
            let actual = SocialURL.incoming(url)?.absoluteString
            guard actual == expected else {
                fatalError("Unexpected incoming URL for \\(input): \\(actual ?? "nil")")
            }
        }
        expectIncoming("https://www.instagram.com/reel/abc", "https://www.instagram.com/reel/abc")
        expectIncoming("storypocket://open?url=https%3A%2F%2Fwww.instagram.com%2Freel%2Fabc",
                       "https://www.instagram.com/reel/abc")
        expectIncoming("storypocket://open?url=https%3A%2F%2Fexample.org%2Fpost", nil)
        expectIncoming("storypocket://open?url=http%3A%2F%2Finstagram.com%2Fp%2Fabc", nil)
        expectIncoming("storypocket://open?url=https%3A%2F%2Finstagram.com.evil.test%2Fp", nil)
        expectIncoming("storypocket://open?url=https%3A%2F%2Finstagram.com%2Fp&url=https%3A%2F%2Fx.com", nil)
        expectIncoming("storypocket://evil?url=https%3A%2F%2Finstagram.com%2Fp", nil)
        expectIncoming("storypocket://open/other?url=https%3A%2F%2Finstagram.com%2Fp", nil)
                print("SocialURL checks passed")
    }
}
