#!/usr/bin/env python3
"""Check five-language Android/iOS resource parity without external dependencies."""
from collections import Counter
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
LANGUAGES = ("it", "en", "fr", "de", "es")
ANDROID_BASE = ROOT / "app/src/main/res"
IOS_BASE = ROOT / "ios/Resources"
ANDROID_PLACEHOLDER = re.compile(r"%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z]")
IOS_PLACEHOLDER = re.compile(r"%(?:\d+\$)?(?:@|d|i|u|f|ld|lld|lu|llu)")
IOS_LINE = re.compile(r'^\s*"((?:[^"\\]|\\.)+)"\s*=\s*"((?:[^"\\]|\\.)*)"\s*;\s*$')

def android_strings(path):
    root = ET.parse(path).getroot()
    if root.tag != "resources":
        raise ValueError(f"{path}: root must be resources")
    entries = {}
    for element in root.findall("string"):
        key = element.attrib["name"]
        if key in entries:
            raise ValueError(f"{path}: duplicate key {key}")
        entries[key] = "".join(element.itertext())
    return entries

def ios_strings(path):
    entries = {}
    for line_number, line in enumerate(path.read_text(encoding="utf-8-sig").splitlines(), 1):
        line = line.strip()
        if not line or line.startswith("//") or line.startswith("/*") or line.startswith("*"):
            continue
        match = IOS_LINE.fullmatch(line)
        if not match:
            raise ValueError(f"{path}:{line_number}: malformed localization entry")
        key, value = match.groups()
        if key in entries:
            raise ValueError(f"{path}:{line_number}: duplicate key {key}")
        entries[key] = value
    return entries

def placeholders(value, pattern):
    # Ignore escaped percent signs, but retain argument positions and types.
    return Counter(pattern.findall(value.replace("%%", "")))

def check(platform, entries, pattern):
    reference = entries["it"]
    errors = []
    for language, strings in entries.items():
        missing = sorted(reference.keys() - strings.keys())
        extra = sorted(strings.keys() - reference.keys())
        if missing:
            errors.append(f"{platform}/{language}: missing {missing}")
        if extra:
            errors.append(f"{platform}/{language}: extra {extra}")
        for key in reference.keys() & strings.keys():
            if not strings[key].strip():
                errors.append(f"{platform}/{language}/{key}: empty translation")
            expected = placeholders(reference[key], pattern)
            actual = placeholders(strings[key], pattern)
            if expected != actual:
                errors.append(f"{platform}/{language}/{key}: placeholders {dict(actual)} != {dict(expected)}")
    if errors:
        raise ValueError("\n".join(errors))
    print(f"{platform}: {len(reference)} matching keys across {len(entries)} languages")

def main():
    android = {}
    ios = {}
    for language in LANGUAGES:
        android_folder = "values" if language == "it" else f"values-{language}"
        android[language] = android_strings(ANDROID_BASE / android_folder / "strings.xml")
        ios[language] = ios_strings(IOS_BASE / f"{language}.lproj" / "Localizable.strings")
    check("Android", android, ANDROID_PLACEHOLDER)
    # Detect Android Java references that have no resource in the default locale.
    activity = (ROOT / "app/src/main/java/com/storypocket/mobile/MainActivity.java").read_text(encoding="utf-8")
    referenced = set(re.findall(r"\bR\.string\.([A-Za-z_][A-Za-z_0-9]*)", activity))
    undefined = sorted(referenced - android["it"].keys())
    if undefined:
        raise ValueError(f"Android/MainActivity.java: undefined string resources {undefined}")
    print(f"Android: {len(referenced)} MainActivity string references resolved")
    # Prevent regressions to literal Italian messages in the two main feedback helpers.
    hardcoded_feedback = re.findall(r'\b(?:status|toast)\s*\(\s*"', activity)
    if hardcoded_feedback:
        raise ValueError(f"Android/MainActivity.java: {len(hardcoded_feedback)} hardcoded status/toast messages")
    print("Android: no literal status/toast messages")
    # Automatic scan failures are also shown to the user and must be localized.
    hardcoded_scan = re.findall(r'\bautoScanFailed\s*\([^;]*"[^"]+"', activity)
    if hardcoded_scan:
        raise ValueError(f"Android/MainActivity.java: {len(hardcoded_scan)} hardcoded automatic scan failures")
    print("Android: no literal automatic scan failure messages")
    hardcoded_exceptions = re.findall(r'new\s+(?:IOException|Exception)\s*\(\s*"', activity)
    if hardcoded_exceptions:
        raise ValueError(f"Android/MainActivity.java: {len(hardcoded_exceptions)} untranslated exception messages")
    print("Android: no literal transfer exception messages")
    if re.search(r'ClipData\.newPlainText\s*\(\s*"', activity):
        raise ValueError("Android/MainActivity.java: clipboard preview label is not localized")
    print("Android: clipboard preview label is localized")
    check("iOS", ios, IOS_PLACEHOLDER)
    # System permission prompts live in InfoPlist.strings, not Localizable.strings.
    ios_privacy = {
        language: ios_strings(IOS_BASE / f"{language}.lproj" / "InfoPlist.strings")
        for language in LANGUAGES
    }
    check("iOS InfoPlist", ios_privacy, IOS_PLACEHOLDER)
    for language, strings in ios_privacy.items():
        if "NSPhotoLibraryAddUsageDescription" not in strings:
            raise ValueError(f"iOS InfoPlist/{language}: missing Photos add-only permission prompt")
    # Verify explicit NSLocalizedString keys in the app and share extension.
    swift_sources = [
        ROOT / "ios/Sources/App/HomeView.swift",
        ROOT / "ios/Sources/Extension/ShareViewController.swift",
    ]
    swift_keys = set()
    for source in swift_sources:
        swift = source.read_text(encoding="utf-8")
        swift_keys.update(re.findall(r'NSLocalizedString\s*\(\s*"([^"]+)"', swift))
    # SwiftUI uses LocalizedStringKey implicitly in these literal UI initializers.
    # Check them as well; NSLocalizedString-only scans miss missing UI translations.
    implicit_pattern = re.compile(
        r'\b(?:Text|TextField|Button|Label|navigationTitle)\s*\(\s*"([^"]+)"'
    )
    # Guard the scanner itself: an over-escaped regex silently misses SwiftUI keys.
    if implicit_pattern.findall('Text("home.subtitle") Button("home.open")') != [
        "home.subtitle", "home.open"
    ]:
        raise ValueError("iOS: SwiftUI implicit localization scanner is broken")
    for source in swift_sources:
        swift = source.read_text(encoding="utf-8")
        swift_keys.update(implicit_pattern.findall(swift))
    missing_ios = sorted(swift_keys - ios["it"].keys())
    if missing_ios:
        raise ValueError(f"iOS: undefined NSLocalizedString keys {missing_ios}")
    print(f"iOS: {len(swift_keys)} explicit NSLocalizedString keys resolved")
    print("Localization parity checks passed.")

if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, ET.ParseError) as error:
        print(f"Localization validation failed: {error}", file=sys.stderr)
        sys.exit(1)
