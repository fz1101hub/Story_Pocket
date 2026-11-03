# iOS prototype — implementation status

Date: 2026-11-03.

The first iOS source prototype has been prepared separately: XcodeGen project specification, SwiftUI home, embedded WKWebView, HTTPS social URL parser, PhotoKit import, UIKit Share Extension and a macOS GitHub Actions simulator-build workflow.

Shared localization catalog: 5 languages (it, en, fr, de, es), 26 translation keys for each language, generating iOS Localizable.strings / InfoPlist.strings and starter Android resources. Local offline checks passed for translation parity and Swift URL allowlist tests. These changes are not yet all present in the GitHub tree.

Next steps: commit the prototype source files, validate macOS XcodeGen simulator build via Actions, fix compiler errors, integrate translated Android resources into actual Java UI strings and run Android CI. Follow with iOS App Groups/share-to-app improvements, authorized media saving and device testing.

Do not claim that a signed iOS IPA exists. The simulator app must build and be tested before release. Do not upload Apple signing keys, provisioning profiles, cookie data or any proprietary app binaries.
