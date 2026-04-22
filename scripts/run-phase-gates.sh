#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GATE_MODE="entry"
COVERAGE_PHASE="phase1plus"
REQUIRE_SONAR="false"

usage() {
  cat <<'EOF'
Usage: bash scripts/run-phase-gates.sh [--gate entry|delivery|compliance|full] [--coverage-phase report-only|phase0|phase1plus] [--require-sonar]

Options:
  --gate            Gate scope. Defaults to entry.
                    entry: repository governance and document drift checks
                    delivery: build/lint/test/coverage/sonar delivery gate
                    compliance: minimal machine-checkable R-118 baseline
                    full: run entry + delivery + compliance
  --coverage-phase  Coverage mode passed to scripts/run-coverage.sh during delivery/full. Defaults to phase1plus.
  --require-sonar   Require SonarQube configuration and fail if secrets are missing.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --gate)
      GATE_MODE="${2:-}"
      shift 2
      ;;
    --coverage-phase)
      COVERAGE_PHASE="${2:-}"
      shift 2
      ;;
    --require-sonar)
      REQUIRE_SONAR="true"
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

run_entry_gate() {
  echo "Running R-116 entry gate checks"
  python3 scripts/task_audit.py --check
  python3 scripts/foreman.py compile-governance --check
  node scripts/lint-repository-knowledge.js
}

run_delivery_gate() {
  echo "Running R-117 delivery gate checks"
  mvn -B clean install
  bash scripts/run-coverage.sh --phase "$COVERAGE_PHASE"
  if [[ "$REQUIRE_SONAR" == "true" ]]; then
    bash scripts/run-sonar.sh --require-config
  else
    bash scripts/run-sonar.sh
  fi
  npm run lint
  npm run build
  node scripts/lint-repository-knowledge.js
}

run_compliance_gate() {
  echo "Running R-118 compliance gate checks"
  python3 scripts/verify_compliance_baseline.py
}

cd "$ROOT_DIR"

case "$GATE_MODE" in
  entry)
    run_entry_gate
    ;;
  delivery)
    run_delivery_gate
    ;;
  compliance)
    run_compliance_gate
    ;;
  full)
    run_entry_gate
    run_delivery_gate
    run_compliance_gate
    ;;
  *)
    echo "Unsupported gate mode: $GATE_MODE" >&2
    exit 1
    ;;
esac
