import Foundation

enum SocialURL {
    private static let hosts: Set<String> = [
        "instagram.com", "tiktok.com", "facebook.com", "fb.watch",
        "pinterest.com", "pin.it", "reddit.com", "redd.it", "bsky.app",
        "mastodon.social", "mastodon.online", "mastodon.world",
        "threads.net", "threads.com", "x.com", "twitter.com", "t.co",
        "youtube.com", "youtu.be", "snapchat.com", "tumblr.com", "vk.com",
        "linkedin.com", "vimeo.com", "dailymotion.com", "twitch.tv",
        "telegram.me", "t.me", "pixelfed.social", "imgur.com"
    ]

    static func normalize(_ input: String) -> URL? {
        guard let components = URLComponents(string: input.trimmingCharacters(in: .whitespacesAndNewlines)),
              components.scheme?.lowercased() == "https",
              components.user == nil, components.password == nil,
              (components.port == nil || components.port == 443),
              let host = components.host?.lowercased(),
              hosts.contains(where: { host == $0 || host.hasSuffix("." + $0) }),
              let url = components.url else { return nil }
        return url
    }

    static func extract(_ text: String) -> URL? {
        if let url = normalize(text) { return url }
        guard let range = text.range(of: #"https://[^\s<>"']+"#, options: .regularExpression) else { return nil }
        let candidate = String(text[range]).trimmingCharacters(in: CharacterSet(charactersIn: ".,;)"))
        return normalize(candidate)
    }
}
