#!/usr/bin/env bash
# Isolated real core mechanics and source-content checks; NOT a native game build or visual test.
set -euo pipefail
cd "$(dirname "$0")/.."
python3 scripts/test_journal_content_02518.py
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT
java -m jdk.compiler/com.sun.tools.javac.Main -d "$classes" \
  src/main/java/com/chaogarden/core/*.java \
  src/main/java/com/chaogarden/fruit/FruitCatalog.java \
  src/test/java/com/chaogarden/core/JournalMechanicsTest02518.java
java -ea -cp "$classes" com.chaogarden.core.JournalMechanicsTest02518
