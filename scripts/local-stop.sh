#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

COMPOSE_BIN=()

setup_compose() {
  if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    COMPOSE_BIN=(docker compose)
    return
  fi

  if command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_BIN=(docker-compose)
    return
  fi

  echo "Docker Compose is not installed." >&2
  exit 1
}

compose() {
  "${COMPOSE_BIN[@]}" "$@"
}

main() {
  cd "${REPO_ROOT}"
  setup_compose

  if [[ "${1:-}" == "-v" ]]; then
    compose down -v
    exit 0
  fi

  compose down
}

main "$@"
