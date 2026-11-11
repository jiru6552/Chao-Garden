#!/usr/bin/env bash
# Isolated rules/mesh checks. This is NOT a Minecraft/NeoForge compilation or visual test.
set -euo pipefail
cd "$(dirname "$0")/.."
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT
java -m jdk.compiler/com.sun.tools.javac.Main -d "$classes" \
  src/main/java/com/chaogarden/toys/ToyInteractionRules.java \
  src/test/java/com/chaogarden/toys/ToyInteractionRulesTest.java
java -ea -cp "$classes" com.chaogarden.toys.ToyInteractionRulesTest
python scripts/test_coffin_geometry_02518.py

python scripts/test_mounted_toy_geometry_02518.py

python scripts/test_rigid_toy_geometry_02518.py
python scripts/test_stationary_toy_geometry_02518.py
