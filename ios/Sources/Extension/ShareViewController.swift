import UIKit
import UniformTypeIdentifiers
import Photos

/// Shares a user-selected photo/video to Photos, or copies a validated social URL.
/// No remote media fetching or access to other apps' private sessions.
final class ShareViewController: UIViewController {
    private let message = UILabel()
    private let action = UIButton(type: .system)
    private let done = UIButton(type: .system)
    private var pendingURL: URL?
    private var pendingFile: URL?
    private var isSaving = false
    private var isClosing = false

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        message.numberOfLines = 0
        message.text = NSLocalizedString("share.loading", comment: "")
        action.setTitle(NSLocalizedString("share.save", comment: ""), for: .normal)
        action.isEnabled = false
        action.addTarget(self, action: #selector(performAction), for: .touchUpInside)
        done.setTitle(NSLocalizedString("common.done", comment: ""), for: .normal)
        done.addTarget(self, action: #selector(close), for: .touchUpInside)
        let stack = UIStackView(arrangedSubviews: [message, action, done])
        stack.axis = .vertical
        stack.spacing = 20
        stack.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor, constant: 24),
            stack.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor, constant: -24),
            stack.centerYAnchor.constraint(equalTo: view.safeAreaLayoutGuide.centerYAnchor)
        ])
        receive()
    }

    private func receive() {
        let providers = (extensionContext?.inputItems as? [NSExtensionItem] ?? [])
            .flatMap { $0.attachments ?? [] }
        if let provider = providers.first(where: { $0.hasItemConformingToTypeIdentifier(UTType.movie.identifier) }) {
            receiveFile(provider, type: .movie)
        } else if let provider = providers.first(where: { $0.hasItemConformingToTypeIdentifier(UTType.image.identifier) }) {
            receiveFile(provider, type: .image)
        } else if let provider = providers.first(where: { $0.hasItemConformingToTypeIdentifier(UTType.url.identifier) }) {
            provider.loadItem(forTypeIdentifier: UTType.url.identifier, options: nil) { [weak self] item, _ in
                self?.receiveLink((item as? URL)?.absoluteString ?? (item as? String) ?? "")
            }
        } else if let provider = providers.first(where: { $0.hasItemConformingToTypeIdentifier(UTType.plainText.identifier) }) {
            provider.loadItem(forTypeIdentifier: UTType.plainText.identifier, options: nil) { [weak self] item, _ in
                let text = (item as? String) ?? (item as? NSAttributedString)?.string ?? ""
                self?.receiveLink(text)
            }
        } else {
            message.text = NSLocalizedString("share.unsupported", comment: "")
        }
    }

    private func receiveLink(_ text: String) {
        let url = SocialURL.extract(text)
        DispatchQueue.main.async {
            guard !self.isClosing else { return }
            guard let url else {
                self.message.text = NSLocalizedString("status.invalid_link", comment: "")
                return
            }
            self.pendingURL = url
            self.message.text = NSLocalizedString("share.copy_hint", comment: "")
            self.action.setTitle(NSLocalizedString("share.copy", comment: ""), for: .normal)
            self.action.isEnabled = true
        }
    }

    private func receiveFile(_ provider: NSItemProvider, type: UTType) {
        provider.loadFileRepresentation(forTypeIdentifier: type.identifier) { [weak self] temporaryURL, _ in
            guard let temporaryURL else {
                DispatchQueue.main.async { [weak self] in
                    guard let self, !self.isClosing else { return }
                    self.message.text = NSLocalizedString("status.import_failed", comment: "")
                }
                return
            }
            let ext = temporaryURL.pathExtension.isEmpty ? (type.preferredFilenameExtension ?? "") : temporaryURL.pathExtension
            let destination = FileManager.default.temporaryDirectory
                .appendingPathComponent(UUID().uuidString).appendingPathExtension(ext)
            do {
                guard MediaFilePolicy.isSupported(destination) else {
                    DispatchQueue.main.async { [weak self] in
                        guard let self, !self.isClosing else { return }
                        self.message.text = NSLocalizedString("share.unsupported", comment: "")
                    }
                    return
                }
                try FileManager.default.copyItem(at: temporaryURL, to: destination)
                guard MediaFilePolicy.hasContent(destination) else {
                    throw NSError(domain: NSCocoaErrorDomain, code: NSFileReadCorruptFileError)
                }
                DispatchQueue.main.async { [weak self] in
                    guard let self, !self.isClosing else {
                        try? FileManager.default.removeItem(at: destination)
                        return
                    }
                    self.pendingFile = destination
                    self.message.text = NSLocalizedString("share.ready_file", comment: "")
                    self.action.isEnabled = true
                }
            } catch {
                // FileManager may leave a partial destination if copying fails.
                try? FileManager.default.removeItem(at: destination)
                DispatchQueue.main.async { [weak self] in
                    guard let self, !self.isClosing else { return }
                    self.message.text = NSLocalizedString("status.import_failed", comment: "")
                }
            }
        }
    }

    @objc private func performAction() {
        if let url = pendingURL {
            UIPasteboard.general.url = url
            message.text = NSLocalizedString("share.copied", comment: "")
            action.isEnabled = false
            return
        }
        guard let file = pendingFile else { return }
        action.isEnabled = false
        done.isEnabled = false
        isSaving = true
        message.text = NSLocalizedString("share.saving", comment: "")
        ExtensionPhotoSaver.save(file) { [weak self] success in
            DispatchQueue.main.async {
                guard let self else { return }
                self.isSaving = false
                self.done.isEnabled = true
                if success {
                    try? FileManager.default.removeItem(at: file)
                    self.pendingFile = nil
                } else {
                    // Keep the imported file so the user can retry after a PhotoKit failure.
                    self.action.isEnabled = true
                }
                self.message.text = NSLocalizedString(success ? "status.saved" : "status.save_failed", comment: "")
            }
        }
    }

    @objc private func close() {
        // PhotoKit may still be reading the temporary file: do not delete it mid-save.
        guard !isSaving, !isClosing else { return }
        isClosing = true
        if let pendingFile { try? FileManager.default.removeItem(at: pendingFile) }
        extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
    }
}

private enum ExtensionPhotoSaver {
    static func save(_ file: URL, completion: @escaping (Bool) -> Void) {
        // Validate supported nonempty media before requesting Photos access.
        guard MediaFilePolicy.hasContent(file) else {
            completion(false)
            return
        }
        let isVideo = MediaFilePolicy.isVideo(file)
        let image = isVideo ? nil : UIImage(contentsOfFile: file.path)
        guard isVideo || image != nil else {
            completion(false)
            return
        }
        PHPhotoLibrary.requestAuthorization(for: .addOnly) { status in
            guard status == .authorized || status == .limited else {
                completion(false)
                return
            }
            PHPhotoLibrary.shared().performChanges({
                if isVideo {
                    PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: file)
                } else if image != nil {
                    // Preserve the original file format and embedded metadata.
                    PHAssetChangeRequest.creationRequestForAssetFromImage(atFileURL: file)
                }
            }) { success, _ in completion(success) }
        }
    }
}
