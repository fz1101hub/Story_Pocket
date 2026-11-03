#!/usr/bin/env python3
"""Static iOS project checks. These do not replace xcodebuild or device tests."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
project = (ROOT / "ios/project.yml").read_text(encoding="utf-8")
app = (ROOT / "ios/Sources/App/HomeView.swift").read_text(encoding="utf-8")
browser = (ROOT / "ios/Sources/App/BrowserView.swift").read_text(encoding="utf-8")
share = (ROOT / "ios/Sources/Extension/ShareViewController.swift").read_text(encoding="utf-8")
social = (ROOT / "ios/Sources/Shared/SocialURL.swift").read_text(encoding="utf-8")
workflow = (ROOT / ".github/workflows/build-ios.yml").read_text(encoding="utf-8")

def require(condition, description):
    if not condition:
        raise SystemExit(f"iOS preflight failed: {description}")
    print(f"PASS: {description}")

require("  StoryPocket:\n" in project and "  StoryPocketShare:\n" in project,
        "main app and share extension targets exist")
app_target, extension_target = project.split("  StoryPocketShare:\n", 1)
require("CFBundleURLSchemes:" in app_target and "- storypocket" in app_target,
        "StoryPocket URL scheme belongs to the main app")
require("CFBundleURLSchemes:" not in extension_target,
        "share extension does not register the app URL scheme")
require("NSPhotoLibraryAddUsageDescription:" in app_target
        and "NSPhotoLibraryAddUsageDescription:" in extension_target,
        "both targets declare Photos add-only purpose")
require("APPLICATION_EXTENSION_API_ONLY: YES" in extension_target,
        "share extension uses extension-safe APIs")
require(".nonPersistent()" in browser,
        "embedded browser does not persist website sessions")
require("SocialURL.allowsBrowserNavigation" in browser,
        "browser validates navigation destinations")
require("navigationAction.navigationType == .linkActivated" in browser,
        "new-window navigation requires a user-activated link")
require("SocialURL.normalize(url.absoluteString)" in browser,
        "browser validates initial social URL")
require("SocialURL.incoming(incoming)" in app,
        "main app validates incoming deep links")
require("SocialURL.extract(link)" in app,
        "main app validates pasted social links")
require("SocialURL.extract(text)" in share,
        "share extension validates shared text links")
require("requestAuthorization(for: .addOnly)" in app
        and "requestAuthorization(for: .addOnly)" in share,
        "both Photos save paths request add-only permission")
require("NSItemProvider" in share and "loadFileRepresentation" in share,
        "share extension receives user-selected file attachments")
require("UIApplication.shared" not in share,
        "share extension does not use unavailable host-app APIs")
require("URLSession.shared" not in share,
        "share extension does not fetch remote media")
require("swiftc ios/Sources/Shared/SocialURL.swift ios/Tests/SocialURLCheck.swift" in workflow,
        "CI compiles and runs standalone Swift URL tests")
require("CODE_SIGNING_ALLOWED=NO build" in workflow,
        "CI builds unsigned iOS simulator app")
print("iOS project preflight passed.")
