#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PHASE_MODE="${COVERAGE_PHASE:-report-only}"
THRESHOLD=""

usage() {
  cat <<'EOF'
Usage: bash scripts/run-coverage.sh [--phase report-only|phase0|phase1plus] [--threshold RATE]

Options:
  --phase       Coverage mode. Defaults to report-only.
                report-only: generate reports without threshold enforcement
                phase0: enforce line coverage >= 0.80
                phase1plus: enforce line coverage >= 0.85
  --threshold   Override the line coverage threshold, e.g. 0.82

Environment:
  COVERAGE_PHASE  Default phase mode when --phase is not provided
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --phase)
      PHASE_MODE="${2:-}"
      shift 2
      ;;
    --threshold)
      THRESHOLD="${2:-}"
      shift 2
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

case "$PHASE_MODE" in
  report-only)
    DEFAULT_THRESHOLD=""
    ;;
  phase0)
    DEFAULT_THRESHOLD="0.80"
    ;;
  phase1plus)
    DEFAULT_THRESHOLD="0.85"
    ;;
  *)
    echo "Unsupported coverage phase: $PHASE_MODE" >&2
    exit 1
    ;;
esac

if [[ -z "$THRESHOLD" ]]; then
  THRESHOLD="$DEFAULT_THRESHOLD"
fi

cd "$ROOT_DIR"

echo "Running coverage generation for phase mode: $PHASE_MODE"
mvn -B clean org.jacoco:jacoco-maven-plugin:prepare-agent test org.jacoco:jacoco-maven-plugin:report

mapfile -t REPORTS < <(find "$ROOT_DIR" -path '*/target/site/jacoco/jacoco.xml' -type f | sort)
if [[ ${#REPORTS[@]} -eq 0 ]]; then
  echo "No JaCoCo XML reports were generated." >&2
  exit 1
fi

echo "Coverage reports:"
printf ' - %s\n' "${REPORTS[@]#"$ROOT_DIR"/}"

python3 - "$THRESHOLD" "${REPORTS[@]}" <<'PY'
import sys
import xml.etree.ElementTree as ET

threshold = sys.argv[1]
paths = sys.argv[2:]

missed = 0
covered = 0

for path in paths:
    root = ET.parse(path).getroot()
    counters = [c for c in root.findall("counter") if c.attrib.get("type") == "LINE"]
    if not counters:
        continue
    counter = counters[0]
    missed += int(counter.attrib["missed"])
    covered += int(counter.attrib["covered"])

total = missed + covered
if total == 0:
    print("No line coverage counters found in JaCoCo reports.", file=sys.stderr)
    sys.exit(1)

rate = covered / total
print(f"Aggregated line coverage: {rate:.4%} ({covered}/{total})")

if threshold:
    threshold_value = float(threshold)
    print(f"Required minimum line coverage: {threshold_value:.2%}")
    if rate + 1e-12 < threshold_value:
        print("Coverage threshold not met.", file=sys.stderr)
        sys.exit(2)
PY
