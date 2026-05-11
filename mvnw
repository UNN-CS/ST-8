#!/usr/bin/env sh
set -eu

MAVEN_VERSION="3.9.9"
BASE_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
TOOLS_DIR="$BASE_DIR/.tools"
MAVEN_DIR="$TOOLS_DIR/apache-maven-$MAVEN_VERSION"
MAVEN_BIN="$MAVEN_DIR/bin/mvn"
MAVEN_TAR="$TOOLS_DIR/apache-maven-$MAVEN_VERSION-bin.tar.gz"
MAVEN_URL="https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz"

if [ ! -x "$MAVEN_BIN" ]; then
  echo "Maven not found locally. Downloading Apache Maven $MAVEN_VERSION..."
  mkdir -p "$TOOLS_DIR"
  curl -fsSL "$MAVEN_URL" -o "$MAVEN_TAR"
  tar -xzf "$MAVEN_TAR" -C "$TOOLS_DIR"
fi

exec "$MAVEN_BIN" "$@"
