#!/usr/bin/env bash
# Builds GameFlow Assistant using the JDK's javac. No external package manager
# is required: the only dependency (sqlite-jdbc) is vendored in ./lib.
set -euo pipefail
cd "$(dirname "$0")"

OUT="out/classes"
JAR="out/GameFlowAssistant.jar"
LIBS="lib/sqlite-jdbc-3.45.1.0.jar:lib/slf4j-api-2.0.13.jar"

echo "[build] cleaning $OUT"
rm -rf "$OUT"
mkdir -p "$OUT"

echo "[build] compiling sources"
find src/main/java -name '*.java' > "$OUT/sources.txt"
javac -encoding UTF-8 -cp "$LIBS" -d "$OUT" @"$OUT/sources.txt"

echo "[build] copying resources"
cp -r src/main/resources/. "$OUT/"

echo "[build] packaging $JAR"
mkdir -p out
jar --create --file "$JAR" --main-class GameFlow.App -C "$OUT" .
echo "[build] done: $JAR"