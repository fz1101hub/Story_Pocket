import Foundation

/// Shared checks for media explicitly selected by the user.
/// No remote downloads, decoding or Photos access occurs here.
enum MediaFilePolicy {
    private static let images: Set<String> = ["jpg", "jpeg", "png", "heic", "heif", "gif"]
    private static let videos: Set<String> = ["mov", "mp4", "m4v"]

    static func isVideo(_ file: URL) -> Bool {
        videos.contains(file.pathExtension.lowercased())
    }

    static func isSupported(_ file: URL) -> Bool {
        let ext = file.pathExtension.lowercased()
        return images.contains(ext) || videos.contains(ext)
    }

    static func hasContent(_ file: URL) -> Bool {
        guard file.isFileURL, isSupported(file),
              let attributes = try? FileManager.default.attributesOfItem(atPath: file.path),
              let kind = attributes[.type] as? FileAttributeType,
              kind == .typeRegular,
              let bytes = attributes[.size] as? NSNumber,
              bytes.int64Value > 0 else { return false }
        return true
    }
}
