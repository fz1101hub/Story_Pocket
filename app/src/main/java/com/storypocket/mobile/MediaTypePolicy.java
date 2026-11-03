package com.storypocket.mobile;

import java.util.Locale;

/**
 * Valida il Content-Type e la firma iniziale del media prima di creare un download.
 * Nessuna dipendenza Android: verificabile con test JVM.
 */
public final class MediaTypePolicy {
    private MediaTypePolicy() { }

    public static final class MediaFormat {
        public final String mime;
        public final String extension;
        MediaFormat(String mime, String extension) {
            this.mime = mime;
            this.extension = extension;
        }
    }

    public static MediaFormat classify(String contentType) {
        if (contentType == null) throw new IllegalArgumentException("Il server non ha indicato il tipo del media.");
        String mime = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        switch (mime) {
            case "image/jpeg": return new MediaFormat(mime, "jpg");
            case "image/png": return new MediaFormat(mime, "png");
            case "image/webp": return new MediaFormat(mime, "webp");
            case "image/avif": return new MediaFormat(mime, "avif");
            case "image/gif": return new MediaFormat(mime, "gif");
            case "image/heic": return new MediaFormat(mime, "heic");
            case "video/mp4": return new MediaFormat(mime, "mp4");
            case "video/webm": return new MediaFormat(mime, "webm");
            case "video/quicktime": return new MediaFormat(mime, "mov");
            default: throw new IllegalArgumentException("¶»§q«^