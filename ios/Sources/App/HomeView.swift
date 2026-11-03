import SwiftUI
import UIKit
import UniformTypeIdentifiers
import Photos

struct HomeView: View {
    @State private var link = ""
    @State private var selectedURL: URL?
    @State private var importing = false
    @State private var isSaving = false
    @State private var status = ""

    private let gradient = LinearGradient(
        colors: [.purple, .pink, .orange],
        startPoint: .topLeading, endPoint: .bottomTrailing
    )

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("app.title").font(.largeTitle.bold())
                        Text("home.subtitle")
                    }
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(24)
                    .background(gradient, in: RoundedRectangle(cornerRadius: 22))

                    Text("home.add_link").font(.headline)
                    TextField("home.link_placeholder", text: $link, axis: .vertical)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.URL)
                        .padding(14)
                        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12))

                    HStack {
                        Button("home.paste") { link = UIPasteboard.general.string ?? "" }
                        Spacer()
                        Button("home.open") {
                            if let url = SocialURL.extract(link) {
                                selectedURL = url
                            } else {
                                status = NSLocalizedString("status.invalid_link", comment: "")
                            }
                        }
                        .buttonStyle(.borderedProminent)
                    }

                    Divider()
                    Text("home.import_title").font(.headline)
                    Text("home.import_hint").foregroundStyle(.secondary)
                    Button("home.import_button") { importing = true }
                        .buttonStyle(.borderedProminent)
                        .disabled(isSaving)

                    Text("home.limitations").font(.footnote).foregroundStyle(.secondary)
                    if !status.isEmpty { Text(status).font(.callout) }
                }
                .padding()
            }
            .navigationTitle("app.title")
            .onOpenURL { incoming in
                if let social = SocialURL.incoming(incoming) {
                    link = social.absoluteString
                    status = ""
                    selectedURL = social
                } else {
                    status = NSLocalizedString("status.invalid_link", comment: "")
                }
            }
            .sheet(item: $selectedURL) { url in
                NavigationStack {
                    BrowserView(url: url)
                        .navigationTitle("app.title")
                        .toolbar {
                            Button("common.done") { selectedURL = nil }
                        }
                }
            }
            .fileImporter(isPresented: $importing, allowedContentTypes: [.image, .movie]) { result in
                switch result {
                case .failure(let error):
                    // Dismissing the document picker is not an import failure.
                    let nsError = error as NSError
                    if nsError.domain != NSCocoaErrorDomain || nsError.code != NSUserCancelledError {
                        status = NSLocalizedString("status.import_failed", comment: "")
                    }
                case .success(let url):
                    let access = url.startAccessingSecurityScopedResource()
                    let ext = url.pathExtension
                    let temp = FileManager.default.temporaryDirectory
                        .appendingPathComponent(UUID().uuidString).appendingPathExtension(ext)
                    do {
                        try FileManager.default.copyItem(at: url, to: temp)
                        if access { url.stopAccessingSecurityScopedResource() }
                        isSaving = true
                        status = NSLocalizedString("share.saving", comment: "")
                        PhotoSaver.save(temp) { success in
                            DispatchQueue.main.async {
                                try? FileManager.default.removeItem(at: temp)
                                isSaving = false
                                status = NSLocalizedString(success ? "status.saved" : "status.save_failed", comment: "")
                            }
                        }
                    } catch {
                        if access { url.stopAccessingSecurityScopedResource() }
                        // A failed copy can leave a partial temporary file behind.
                        try? FileManager.default.removeItem(at: temp)
                        status = NSLocalizedString("status.import_failed", comment: "")
                    }
                }
            }
        }
    }
}

extension URL: @retroactive Identifiable {
    public var id: String { absoluteString }
}

enum PhotoSaver {
    static func save(_ file: URL, completion: @escaping (Bool) -> Void) {
        // Validate and decode before requesting permission or creating an asset.
        // Otherwise PhotoKit can report success after a no-op change block.
        // Never ask for Photos permission for a missing or empty attachment.
        guard let attributes = try? FileManager.default.attributesOfItem(atPath: file.path),
              let bytes = attributes[.size] as? NSNumber, bytes.int64Value > 0 else {
            completion(false)
            return
        }
        let isVideo = ["mov", "mp4", "m4v"].contains(file.pathExtension.lowercased())
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
            }) { saved, _ in completion(saved) }
        }
    }
}
