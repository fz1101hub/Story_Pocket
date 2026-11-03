package com.storypocket.mobile;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small, reviewable social URL allowlist. It never accepts arbitrary web hosts. */
public final class SocialLinks {
    private static final Pattern URL = Pattern.compile("(?i)https?://[^\\s<>\\\"'\\u200b]+", Pattern.CASE_INSENSITIVE);
    private static final String[] SOCIAL_HOSTS = {
        "tiktok.com", "vm.tiktok.com", "vt.tiktok.com", "facebook.com", "fb.watch",
        "pinterest.com", "pin.it", "reddit.com", "redd.it", "bsky.app",
        "threads.net", "threads.com", "x.com", "twitter.com", "t.co",
        "youtube.com", "youtu.be", "snapchat.com", "tumblr.com", "vk.com",
        "linkedin.com", "vimeo.com", "dailymotion.com", "twitch.tv",
        "telegram.me", "t.me", "pixelfed.social", "mastodon.social",
        "mastodon.online", "mastodon.world", "mstdn.social", "fosstodon.org",
        "mas.to", "hachyderm.io", "infosec.exchange", "imgur.com"
    };
    private static final String[] MEDIA_HOSTS = {
        "i.redd.it", "preview.redd.it", "v.redd.it", "redditmedia.com",
        "cdn.bsky.app", "video.bsky.app", "bsky.app",
        "files.mastodon.social", "media.mastodon.social", "mastodon.social",
        "files.mastodon.online", "mastodon.online", "media.mstdn.social",
        "mstdn.social", "media.mas.to", "mas.to", "files.mastodon.world",
        "mastodon.world", "i.pinimg.com", "media.tumblr.com", "64.media.tumb¶»§q«^