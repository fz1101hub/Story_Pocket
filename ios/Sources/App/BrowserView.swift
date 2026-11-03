import SwiftUI
import WebKit

struct BrowserView: UIViewRepresentable {
    let url: URL
    func makeCoordinator() -> Coordinator { Coordinator() }
    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .nonPersistent()
        let web = WKWebView(frame: .zero, configuration: configuration)
        web.navigationDelegate = context.coordinator
        return web
    }
    func updateUIView(_ web: WKWebView, context: Context) {
        guard context.coordinator.lastURL != url else { return }
        context.coordinator.lastURL = url
        web.load(URLRequest(url: url))
    }
    final class Coordinator: NSObject, WKNavigationDelegate {
        var lastURL: URL?
        func webView(_ webView: WKWebView,
                     decidePolicyFor navigationAction: WKNavigationAction,
                     decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
            let allowed = navigationAction.request.url.map(SocialURL.allowsBrowserNavigation) ?? false
            decisionHandler(allowed ? .allow : .cancel)
        }
    }
}
