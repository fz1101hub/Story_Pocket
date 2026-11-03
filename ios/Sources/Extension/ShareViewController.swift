import UIKit
import UniformTypeIdentifiers
import Photos

/// Shares a user-selected photo/video to Photos, or copies a validated social URL.
/// No remote media fetching or access to other apps' private sessions.
final class ShareViewController: UIViewController {
    private let message = UILabel()
    private let action = UIButton(type: .system)
    private var pendingURL: URL?
    private var pendingFile: URL?

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        message.numberOfLines = 0
        message.text = NSLocalizedString("share.loading", comment: "")
        action.setTitle(NSLocalizedString("share.save", comment: ""), for: .normal)
        action.isEnabled = false
        action.addTarget(self, action: #selector(performAction), for: .touchUpInside)
        let done = UIButton(type: .system)
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
                self?.receiveLink(item as? String ?? "")
            }
        } else {
            message.text = NSLocalizedString("share.unsupported", comment: "")
        }
    }

    private func receiveLink(_ text: String) {
        let url = SocialURL.extract(text)
        DispatchQueue.main.async {
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
                DispatchQueue.main.async {
                    self?.message.text = NSLocalizedString("status.import_failed", comment: "")
                }
                return
            }
            let ext = temporaryURL.pathExtension.isEmpty ? (type.preferredFilenameExtension ?? "") : temporaryURL.pathExtension
            let destination = FileManager.default.temporaryDirectory
                .appendingPathComponent(UUID().uuidString).appendingPathExtension(ext)
            do {
                try FileManager.default.copyItem(at: temporaryURL, to: destination)
                DispatchQueue.main.async {
                    self?.pendingFile = destination
                    self?.message.text = NSLocalizedString("share.ready_file", comment: "")
                    self?.action.isEnabled = true
                }
            } catch {
                DispatchQueue.main.async {
                    self?.message.text = NSLocalizedString("status.import_failed", comment: "")
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
        message.text = NSLocalizedString("share.saving", comment: "")
        PhotoSaver.save(file) { [weak self] success in
            DispatchQueue.main.async {
                try? FileManager.default.removeItem(at: file)
                self?.pendingFile = nil
                self?.message.text = NSLocalizedString(success ? "status.saved" : "status.save_failed", comment: "")
            }
        }
    }

    @objc private func close() {
        if let pendingFile { try? FileManager.default.removeItem(at: pendingFile) }
        extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
    }
}
