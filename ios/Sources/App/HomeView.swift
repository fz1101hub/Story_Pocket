import SwiftUI
import UIKit
import UniformTypeIdentifiers
import Photos

struct HomeView: View {
    @State private var link = ""
    @State private var selectedURL: URL?
    @State private var importing = false
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

                    Text("home.limitations").font(.footnote).foregroundStyle(.secondary)
                    if !status.isEmpty { Text(status).font(.callout) }
                }
                .padding()
            }
            .navigationTitle("app.title")
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
                case .failure:
                    status = NSLocalizedString("status.import_failed", comment: "")
                case .success(let url):
                    let access = url.startAccessingSecurityScopedResource()
                    let ext = url.pathExtension
                    let temp = FileManager.default.temporaryDirectory
                        .appendingPathComponent(UUID().uuidString).appendingPathExtension(ext)
                    do {
                        try FileManager.default.copyItem(at: url, to: temp)
                        if access { url.stopAccessingSecurityScopedResource() }
                        PhotoSaver.save(temp) { success in
                            DispatchQueue.main.async {
                                try? FileManager.default.removeItem(at: temp)
                                status = NSLocalizedString(success ? "status.saved" : "status.save_failed", comment: "")
                            }
                        }
                    } catch {
                        if access { url.stopAccessingSecurityScopedResource() }
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
        PHPhotoLibrary.requestAuthorization(for: .addOnly) { status in
            guard status == .authorized || status == .limited else {
                completion(false)
                return
            }
            PHPhotoLibrary.shared().performChanges({
                if ["mov", "mp4", "m4v"].contains(file.pathExtension.lowercased()) {
                    PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: file)
                } else if let image = UIImage(contentsOfFile: file.path) {
                    PHAssetChangeRequest.creationRequestForAsset(from: image)
                }
            }) { saved, _ in completion(saved) }
        }
    }
}
