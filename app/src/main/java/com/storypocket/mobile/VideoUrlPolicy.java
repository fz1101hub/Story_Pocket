package com.storypocket.mobile;

import java.net.URI;
import java.util.Locale;

/**
 * Seleziona solo URL plausibilmente di video originali. Non considera immagini,
 * manifest DASH/HLS o frammenti di streaming come video completi.
 * Non tenta mai di convertire blob: in URL HTTPS.
 */
public final class VideoUrlPolicy {
    private VideoUrlPolicy() { }

    public static int score(String address) {
        if (address == null || address.length() > 8192 || !TargetParser.isTrustedMediaUrl(address)) return 0;
        try {
            URI uri = URI.create(address);
            String path = uri.getRawPath() == null ? "" : uri.getRawPath().toLowerCase(Locale.ROOT);
            String query = uri.getRawQuery() == null ? "" : uri.getRawQuery().toLowerCase(Locale.ROOT);
            // Non salvare manifest, segmenti indipendenti, risposte range o audio isolato.
            if (path.matches(".*\\.(?:m3u8|mpd|m4s|ts|m4a|aac|jpg|jpeg|png|webp|gif|avif)(?:$|/.*)")) return 0;
            if (query.matches(".*(?:^|[&])(?:bytestart|byteend|start|end|range)=.*")) return 0;
            if (query.contains("dst-jpg") || query.contains("dst-webp") || query.contains("mime_type=image")) return 0;
            if (path.matches(".*\\.mp4$")) return 130;
            if (path.matches(".*\\.(?:mov|webm)$")) return 100;
            if (query.contains("mime_type=video") || query.contains("mimetype=video") || query.contains("dst-mp4")) return 105;
            if (path.contains("/o1/v/t16/") || path.contains("/v/t16/")) return 72;
            if (path.contains("/video/") || path.contains("/videos/")) return 52;
            return 0;
        } catch (RuntimeException ignored) { return 0; }
    }
}
