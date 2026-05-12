#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
RUN_BROWSER_SMOKE=true

usage() {
  cat <<'EOF'
Usage: bash scripts/run-acceleration-rewrite-governance-smoke.sh [--skip-browser]

Runs the repo-closed acceleration/rewrite governance smoke bundle for HARN-142.

Options:
  --skip-browser   Run static closeout and contract checks only.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-browser)
      RUN_BROWSER_SMOKE=false
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

run_step() {
  local label="$1"
  shift
  printf '[acceleration-governance-smoke] %s\n' "${label}"
  "$@"
}

cd "${REPO_ROOT}"

run_step "closeout contract" node scripts/check-acceleration-rewrite-governance-closeout.mjs
run_step "workbench contract" node scripts/check-acceleration-workbench-contract.mjs
run_step "recommendation contract" node scripts/check-recommendation-page-contract.mjs
run_step "history contract" node scripts/check-history-page-contract.mjs
run_step "alert contract" node scripts/check-alert-page-contract.mjs
run_step "frontend page governance" npm run test:frontend-page-governance

if [[ "${RUN_BROWSER_SMOKE}" == "true" ]]; then
  run_step "workbench browser smoke" npm run smoke:acceleration-workbench
  run_step "production rewrite closed-loop browser smoke" npm run smoke:production-rewrite-closed-loop
fi

printf '[acceleration-governance-smoke] completed\n'
