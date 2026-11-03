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
        web.uiDelegate = context.coordinator
        return web
    }
    func updateUIView(_ web: WKWebView, context: Context) {
        guard SocialURL.normalize(url.absoluteString) != nil else { return }
        guard context.coordinator.lastURL != url else { return }
        context.coordinator.lastURL = url
        web.load(URLRequest(url: url))
    }
    final class Coordinator: NSObject, WKNavigationDelegate, WKUIDelegate {
        var lastURL: URL?
        func webView(_ webView: WKWebView,
                     createWebViewWith configuration: WKWebViewConfiguration,
                     for navigationAction: WKNavigationAction,
                     windowFeatures: WKWindowFeatures) -> WKWebView? {
            // Handle target=_blank in the same ephemeral browser instead of opening a popup.
            guard navigationAction.targetFrame == nil,
                  navigationAction.navigationType == .linkActivated,
                  let destination = navigationAction.request.url,
                  SocialURL.allowsBrowserNavigation(destination) else { return nil }
            webView.load(navigationAction.request)
            return nil
        }
        func webView(_ webView: WKWebView,
                     decidePolicyFor navigationAction: WKNavigationAction,
                     decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
            let allowed = navigationAction.request.url.map(SocialURL.allowsBrowserNavigation) ?? false
            decisionHandler(allowed ? .allow : .cancel)
        }
    }
}
