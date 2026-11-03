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
              components.fragment == nil,
              let host = components.host?.lowercased(),
              hosts.contains(where: { host == $0 || host.hasSuffix("." + $0) }),
              let url = components.url else { return nil }
        return url
    }

    /// Accept a supported HTTPS URL or an explicit StoryPocket deep link.
    /// Never forward arbitrary URL schemes or hosts to the embedded browser.
    static func incoming(_ url: URL) -> URL? {
        if let social = normalize(url.absoluteString) { return social }
        guard let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
              components.scheme?.lowercased() == "storypocket",
              components.host?.lowercased() == "open",
              (components.path.isEmpty || components.path == "/"),
              components.user == nil, components.password == nil,
              components.port == nil, components.fragment == nil,
              let items = components.queryItems,
              items.count == 1,
              items[0].name == "url",
              let value = items[0].value else { return nil }
        return normalize(value)
    }

    static func extract(_ text: String) -> URL? {
        if let url = normalize(text) { return url }
        let pattern = #"https://[^\s<>"']+"#
        guard let regex = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive]) else {
            return nil
        }
        let matches = regex.matches(in: text, range: NSRange(text.startIndex..., in: text))
        for match in matches {
            guard let range = Range(match.range, in: text) else { continue }
            let candidate = String(text[range])
                .trimmingCharacters(in: CharacterSet(charactersIn: ".,;)]}"))
            if let url = normalize(candidate) { return url }
        }
        return nil
    }
}
