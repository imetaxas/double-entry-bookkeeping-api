#!/usr/bin/env bash
#
# Zips double-entry-bookkeeping-api source code for minimal storage/transfer.
# Excludes all build artifacts, caches, and generated files.
# The output is fully compilable with: mvn compile
#
# Usage: ./zip-source.sh [output-file]
#   Default output: <project-name>-source.zip in the project directory.
#   ZIP_SOURCE_DIR=/path/to/other-project ./zip-source.sh [output-file]

set -euo pipefail

PROJECT_DIR="$(cd "${ZIP_SOURCE_DIR:-$(dirname "$0")}" && pwd)"
PROJECT_NAME="$(basename "$PROJECT_DIR")"
OUTPUT="${1:-${PROJECT_NAME}-source.zip}"

cd "$PROJECT_DIR"

rm -f "$OUTPUT"

zip -r "$OUTPUT" . \
  -x '**/target/*' \
  -x 'target/*' \
  -x '.idea/*' \
  -x '*.iml' \
  -x '.vscode/*' \
  -x '.settings/*' \
  -x '.classpath' \
  -x '.project' \
  -x '.DS_Store' \
  -x 'Thumbs.db' \
  -x '*.log' \
  -x '.env' \
  -x 'dependency-reduced-pom.xml' \
  -x '*.class' \
  -x '*.jar' \
  -x '*.war' \
  -x '*.ear' \
  -x '*.zip' \
  -x '*.tar.gz' \
  -x '*-source.zip'

SIZE=$(du -h "$OUTPUT" | cut -f1)
echo ""
echo "Created: $OUTPUT ($SIZE)"
echo "To build: unzip $OUTPUT -d $PROJECT_NAME && cd $PROJECT_NAME && mvn compile"
