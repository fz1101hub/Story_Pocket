# StoryPocket iOS — prototype

SwiftUI app for iOS 16+, with a social HTTPS link validator, in-app WKWebView, file importer and explicit PhotoKit save to Photos.

## Build

Run the **Build StoryPocket iOS Simulator** GitHub Actions workflow. It generates an Xcode project with XcodeGen and builds an unsigned simulator app. A simulator artifact is not an installable iPhone IPA.

## Languages

Italian, English, French, German and Spanish in `Resources/*.lproj/Localizable.strings`, plus localized Photos permission prompts in `Resources/*.lproj/InfoPlist.strings`. The device locale determines which strings are used.

## URL checks and browser privacy

The embedded browser uses an ephemeral WebKit data store. It accepts HTTPS navigation on standard TLS ports and opens supported new-window links in the same browser view. The initial social URL must pass the supported-host validator. Insecure and local URL schemes are blocked.

The Foundation-only parser tests can run without Xcode:

```sh
swiftc ios/Sources/Shared/SocialURL.swift ios/Tests/SocialURLCheck.swift -o /tmp/storypocket-social-url-check
/tmp/storypocket-social-url-check
```

## Local media import checks

Both the app and Share Extension accept explicitly selected JPEG, PNG, HEIC/HEIF,
GIF, MOV, MP4 and M4V files. A shared Foundation-only policy rejects missing,
empty, unsupported, oversized (over 120 MiB) and nonregular files before requesting Photos access.
Both import paths check the source before copying and recheck the copied file. The
iOS workflow compiles and executes both the URL tests and media policy tests:

```sh
swiftc ios/Sources/Shared/MediaFilePolicy.swift ios/Tests/MediaFilePolicyCheck.swift -o /tmp/storypocket-media-check
/tmp/storypocket-media-check
python3 tests/IOSProjectCheck.py
```

These checks do not prove that PhotoKit saving succeeds on an iPhone; device
permission and file-format testing are still required.

## Scope and safety

The current iOS prototype does **not** automatically download social media, bypass authentication, or access private posts. The Share Extension already accepts user-selected files and social links; it does not fetch remote media. Device distribution requires signing and provisioning; never commit credentials.
