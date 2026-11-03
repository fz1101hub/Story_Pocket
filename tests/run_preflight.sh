#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/test
javac -encoding UTF-8 -d build/test \
  app/src/main/java/com/storypocket/mobile/TargetParser.java \
  app/src/main/java/com/storypocket/mobile/SocialLinks.java \
  app/src/main/java/com/storypocket/mobile/SharedStoryParser.java \
  app/src/main/java/com/storypocket/mobile/MediaTypePolicy.java \
  app/src/main/java/com/storypocket/mobile/VideoUrlPolicy.java \
  app/src/main/java/com/storypocket/mobile/NavigationPolicy.java \
  app/src/main/java/com/storypocket/mobile/MediaContextPolicy.java \
  tests/NavigationPolicyCheck.java tests/OriginalVideoCheck.java
java -cp build/test NavigationPolicyCheck
java -cp build/test OriginalVideoCheck
node tests/MediaProbeCheck.js
node tests/StoryMediaProbeCheck.js
python3 tests/ProjectPreflight.py
python3 tests/LocalizationCheck.py
python3 tests/IOSProjectCheck.py
printf '\nPreflight v2.3 superato.\n'
