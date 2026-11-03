# StoryPocket iOS — prototype

SwiftUI app for iOS 16+, with a social HTTPS link validator, in-app WKWebView, file importer and explicit PhotoKit save to Photos.

## Build

Run the **Build StoryPocket iOS Simulator** GitHub Actions workflow. It generates an Xcode project with XcodeGen and builds an unsigned simulator app. A simulator artifact is not an installable iPhone IPA.

## Languages

Italian, English, French, German and Spanish in `Resources/*.lproj/Localizable.strings`. The device locale determines which strings are used.

## Scope and safety

The current iOS prototype does **not** automatically download social media, bypass authentication, or access private posts. The Share Extension already accepts user-selected files and social links; it does not fetch remote media. Device distribution requires signing and provisioning; never commit credentials.
