#!/usr/bin/env bash
# GameFlow Assistant (Kotlin JVM build + run).
# Requires a JDK 17+ and the Kotlin standalone compiler (kotlinc) on PATH, or
# set $KOTLINC to the kotlinc binary. This is the verified path.
#
# Usage:
#   ./run.sh            launch the GUI
#   ./run.sh --demo     launch with the built-in simulation (no dialog)
#   ./run.sh --headless run the engine without a window (self-test)
set -e
cd "$(dirname "$0")"

# --- locate kotlinc ---------------------------------------------------------
KOTLINC_BIN="${KOTLINC:-$(command -v kotlinc || true)}"
if [ -z "$KOTLINC_BIN" ] || [ ! -x "$KOTLINC_BIN" ]; then
  echo "kotlinc not found. Install the Kotlin compiler or set KOTLINC=/path/to/kotlinc"
  exit 1
fi
# kotlinc dir -> ../lib holds the kotlin-stdlib jars
STDLIB_DIR="$(cd "$(dirname "$KOTLINC_BIN")/../lib" && pwd 2>/dev/null || echo '')"

JARS="lib/sqlite-jdbc-3.45.1.0.jar:lib/slf4j-api-2.0.13.jar"
if [ -n "$STDLIB_DIR" ]; then
  JARS="$STDLIB_DIR/kotlin-stdlib.jar:$STDLIB_DIR/kotlin-stdlib-jdk8.jar:$JARS"
fi

mode="${1:-gui}"

# --- compile (only when out/ is missing or runs with --headless/--demo) ----
if [ ! -d out/GameFlow ] || [ "$mode" != "gui" ]; then
  rm -rf out
  mkdir -p out
  echo "Compiling..."
  "$KOTLINC_BIN" -classpath "$JARS" -d out $(find src/main/kotlin -name '*.kt')
  if [ -d src/test ]; then
    mkdir -p out/test
    "$KOTLINC_BIN" -classpath "out:$JARS" -d out/test $(find src/test -name '*.kt')
  fi
  # Resources (template READMEs) onto the classpath.
  cp -r src/main/resources/* out/ 2>/dev/null || true
fi

case "$mode" in
  --demo)     exec java -cp "out:$JARS" GameFlow.App --demo ;;
  --headless) exec java -Djava.awt.headless=true -cp "out:$JARS" GameFlow.App --demo --headless ;;
  smoke)      exec java -cp "out/test:out:$JARS" GameFlow.SmokeTest ;;
  *)          exec java -cp "out:$JARS" GameFlow.App ;;
esac