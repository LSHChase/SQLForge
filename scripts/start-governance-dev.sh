#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SKIP_BUILD=false
DEV_CRYPTO_KEY_BASE64="${SQLFORGE_DEV_CRYPTO_KEY_BASE64:-MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=}"

usage() {
  cat <<'EOF'
Usage: ./scripts/start-governance-dev.sh [options]

Options:
  --skip-build   Skip the sqlforge-shared + governance install step.
EOF
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --skip-build)
        SKIP_BUILD=true
        shift
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        echo "Unknown argument: $1" >&2
        usage >&2
        exit 1
        ;;
    esac
  done
}

spring_boot_version() {
  sed -n 's:.*<spring.boot.version>\(.*\)</spring.boot.version>.*:\1:p' "${REPO_ROOT}/pom.xml" | head -n 1
}

parse_args "$@"

cd "${REPO_ROOT}"

if [[ "${SKIP_BUILD}" != "true" ]]; then
  mvn -B -pl sqlforge-shared,governance -am install -DskipTests >/dev/null
fi

export SQLFORGE_DEV_CRYPTO_KEY_BASE64="${DEV_CRYPTO_KEY_BASE64}"
exec mvn -B -f governance/pom.xml org.springframework.boot:spring-boot-maven-plugin:"$(spring_boot_version)":run
