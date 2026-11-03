import Foundation

@main
struct MediaFilePolicyCheck {
    static func main() throws {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("storypocket-media-policy-" + UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }

        for ext in ["jpg", "jpeg", "png", "heic", "heif", "gif", "mov", "mp4", "m4v", "MP4"] {
            let file = directory.appendingPathComponent("sample." + ext)
            precondition(MediaFilePolicy.isSupported(file), "Unsupported media extension: " + ext)
            precondition(!MediaFilePolicy.hasContent(file), "Missing file accepted: " + ext)
            try Data().write(to: file)
            precondition(!MediaFilePolicy.hasContent(file), "Empty file accepted: " + ext)
            try Data([0x01]).write(to: file)
            precondition(MediaFilePolicy.hasContent(file), "Nonempty media rejected: " + ext)
        }

        for ext in ["txt", "html", "svg", "exe", "pdf", "zip"] {
            let file = directory.appendingPathComponent("sample." + ext)
            try Data([0x01]).write(to: file)
            precondition(!MediaFilePolicy.isSupported(file), "Unexpected extension: " + ext)
            precondition(!MediaFilePolicy.hasContent(file), "Unsupported media accepted: " + ext)
        }

        let fakeImageDirectory = directory.appendingPathComponent("folder.jpg")
        try FileManager.default.createDirectory(at: fakeImageDirectory, withIntermediateDirectories: false)
        precondition(!MediaFilePolicy.hasContent(fakeImageDirectory), "Directory accepted as media")

        let oversized = directory.appendingPathComponent("oversized.mp4")
        try Data([0x01]).write(to: oversized)
        let handle = try FileHandle(forWritingTo: oversized)
        try handle.truncate(atOffset: UInt64(MediaFilePolicy.maximumFileBytes + 1))
        try handle.close()
        precondition(!MediaFilePolicy.hasAcceptableSize(oversized), "Oversized file accepted")
        precondition(MediaFilePolicy.isTooLarge(oversized), "Oversized file not detected")
        let boundary = directory.appendingPathComponent("boundary.mp4")
        try Data([0x01]).write(to: boundary)
        let boundaryHandle = try FileHandle(forWritingTo: boundary)
        try boundaryHandle.truncate(atOffset: UInt64(MediaFilePolicy.maximumFileBytes))
        try boundaryHandle.close()
        precondition(MediaFilePolicy.hasContent(boundary), "Maximum allowed size rejected")
        precondition(!MediaFilePolicy.isTooLarge(boundary), "Allowed file flagged oversized")
        precondition(!MediaFilePolicy.hasContent(oversized), "Oversized media accepted")
        precondition(MediaFilePolicy.hasAcceptableSize(directory.appendingPathComponent("sample.mp4")))

        precondition(MediaFilePolicy.isVideo(directory.appendingPathComponent("sample.MP4")))
        precondition(!MediaFilePolicy.isVideo(directory.appendingPathComponent("sample.jpg")))
        precondition(!MediaFilePolicy.hasContent(URL(string: "https://example.org/video.mp4")!))
        print("MediaFilePolicy checks passed")
    }
}
