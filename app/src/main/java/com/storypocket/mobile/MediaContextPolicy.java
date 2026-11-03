package com.storypocket.mobile;
public final class MediaContextPolicy {
    private MediaContextPolicy() {}
    public static boolean isValid(String browserPage, String mediaPage) {
        return TargetParser.isInstagramUrl(browserPage) && TargetParser.isMediaPage(mediaPage);
    }
    public static String resolve(String browserPage, String jsCandidate) {
        if (!TargetParser.isInstagramUrl(browserPage) || !TargetParser.isMediaPage(jsCandidate)) return null;
        try { return TargetParser.normalize(jsCandidate); }
        catch (RuntimeException ignored) { return null; }
    }
}
