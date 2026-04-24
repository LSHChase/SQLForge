#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
node "$SCRIPT_DIR/portable-server.mjs" --config "$SCRIPT_DIR/portable-config.json"
