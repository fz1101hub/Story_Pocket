#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/test
javac -encoding UTF-8 -d build/test \
  app/src/main/java/com/storypocket/mobile/TargetParser.java \
  app/src/main/java/com/storypocket/mobile/SharedStoryParser.java \
  app/src/main/java/com/storypocket/mobile/MediaTypePolicy.java \
  app/src/main/java/com/storypocket/mobile/VideoUrlPolicy.java \
  app/src/main/java/com/storypocket/mobile/NavigationPolicy.java \
  tests/TargetParserCheck.java tests/SharedStoryParserCheck.java tests/PreflightCheck.java tests/JavaSyntaxCheck.java tests/OriginalVideoCheck.java tests/NavigationPolicyCheck.java
java -cp build/test TargetParserCheck
java -cp build/test SharedStoryParserCheck
java -cp build/test PreflightCheck
java -cp build/test JavaSyntaxCheck
java -cp build/test OriginalVideoCheck
java -cp build/test NavigationPolicyCheck
node tests/MediaProbeCheck.js
python3 tests/ProjectPreflight.py
python3 tests/TimeoutRegressionCheck.py
printf '\nTutti i controlli pre-compilazione disponibili in questo ambiente sono passati.\n'
