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
            default: throw new IllegalArgumentException("Il server non ha fornito un'immagine o un video supportato.");
        }
    }

    /** Alcune CDN servono MP4 originali come octet-stream: serve la firma ftyp. */
    public static MediaFormat classifyResponse(String contentType, String expectedType, byte[] header) {
        try { return classify(contentType); }
        catch (IllegalArgumentException unsupported) {
            String mime = contentType == null ? "" : contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            if ("video".equals(expectedType) && (mime.equals("application/octet-stream") ||
                    mime.equals("binary/octet-stream")) && asciiAt(header, 4, "ftyp")) {
                return new MediaFormat("video/mp4", "mp4");
            }
            throw unsupported;
        }
    }

    public static boolean isVideo(MediaFormat format) {
        return format != null && format.mime.startsWith("video/");
    }
    public static boolean matchesExpectedType(String expectedType, MediaFormat format) {
        if (format == null) return false;
        if ("video".equals(expectedType)) return isVideo(format);
        if ("image".equals(expectedType)) return format.mime.startsWith("image/");
        return false;
    }

    private static boolean has(byte[] data, int index, int... bytes) {
        if (data == null || data.length < index + bytes.length) return false;
        for (int i = 0; i < bytes.length; i++) if ((data[index+i] & 0xff) != bytes[i]) return false;
        return true;
    }

    private static boolean asciiAt(byte[] data, int offset, String target) {
        if (data == null || data.length < offset + target.length()) return false;
        for (int i = 0; i < target.length(); i++) if ((char)(data[offset+i]&0xff) != target.charAt(i)) return false;
        return true;
    }

    public static boolean hasValidSignature(MediaFormat fmt, byte[] first) {
        if (fmt == null || first == null) return false;
        switch (fmt.mime) {
            case "image/jpeg": return has(first, 0, 0xff, 0xd8, 0xff);
            case "image/png": return has(first, 0, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a);
            case "image/webp": return asciiAt(first, 0, "RIFF") && asciiAt(first, 8, "WEBP");
            case "image/gif": return asciiAt(first, 0, "GIF87a") || asciiAt(first, 0, "GIF89a");
            case "video/webm": return has(first, 0, 0x1a, 0x45, 0xdf, 0xa3);
            // "styp" indica spesso un frammento DASH: non e' un MP4 standalone.
            case "video/mp4": return asciiAt(first, 4, "ftyp");
            case "video/quicktime": return asciiAt(first, 4, "ftyp") || asciiAt(first, 4, "moov");
            case "image/avif": return asciiAt(first, 4, "ftyp") &&
                (asciiAt(first, 8, "avif") || asciiAt(first, 8, "avis")
                 || asciiAt(first, 16, "avif") || asciiAt(first, 16, "avis"));
            case "image/heic": return asciiAt(first, 4, "ftyp") &&
                (asciiAt(first, 8, "heic") || asciiAt(first, 8, "heix")
                 || asciiAt(first, 8, "hevc") || asciiAt(first, 8, "mif1"));
            default: return false;
        }
    }
}
