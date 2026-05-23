#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/smoke-lib.sh"

QUERY_EXECUTION_BASE_URL="${QUERY_EXECUTION_BASE_URL:-http://localhost:8081}"
QUERY_EXECUTION_HEALTH_URL="${QUERY_EXECUTION_HEALTH_URL:-${QUERY_EXECUTION_BASE_URL%/}/actuator/health}"
QUERY_EXECUTION_HETU_CALIBRATION_URL="${QUERY_EXECUTION_HETU_CALIBRATION_URL:-${QUERY_EXECUTION_BASE_URL%/}/api/query-execution/internal/hetu/route-calibration}"
REQUEST_TENANT_ID="${REQUEST_TENANT_ID:-tenant-a}"
REQUEST_USER_ID="${REQUEST_USER_ID:-hetu-smoke-bot}"
REQUEST_AUTH_SOURCE="${REQUEST_AUTH_SOURCE:-header}"
EXPECTED_QUERY_EXECUTION_MODE="${EXPECTED_QUERY_EXECUTION_MODE:-REAL}"
EXPECTED_HETU_ROUTE_PROFILE="${EXPECTED_HETU_ROUTE_PROFILE:-REPO_CLOSED_BASELINE}"
EXPECTED_HETU_ROUTE_ORDER="${EXPECTED_HETU_ROUTE_ORDER:-JDBC,REST,CLIENT}"
EXPECTED_HETU_EVIDENCE_SOURCE="${EXPECTED_HETU_EVIDENCE_SOURCE:-REPO_CLOSED_CONFIGURATION}"
EXPECTED_HETU_LIVE_VERIFICATION_STATUS="${EXPECTED_HETU_LIVE_VERIFICATION_STATUS:-PENDING_ENV_WINDOW}"
HETU_ENV_EVIDENCE_OUTPUT_PATH="${HETU_ENV_EVIDENCE_OUTPUT_PATH:-}"

usage() {
  cat <<'EOF'
Usage: ./scripts/run-hetu-env-smoke.sh

Environment variables:
  QUERY_EXECUTION_BASE_URL        Base URL for query-execution.
  QUERY_EXECUTION_HEALTH_URL      Optional explicit health URL override.
  REQUEST_TENANT_ID               Tenant header value.
  REQUEST_USER_ID                 User header value.
  REQUEST_AUTH_SOURCE             Auth source header value.
  EXPECTED_QUERY_EXECUTION_MODE   REAL, JDBC, REST, or CLIENT. REAL means any non-simulated Hetu mode.
  EXPECTED_HETU_ROUTE_PROFILE     Expected route-calibration profile.
  EXPECTED_HETU_ROUTE_ORDER       Expected effective route order, comma-separated.
  EXPECTED_HETU_EVIDENCE_SOURCE   Expected calibration evidence source.
  EXPECTED_HETU_LIVE_VERIFICATION_STATUS
                                  Expected live verification status.
  HETU_ENV_EVIDENCE_OUTPUT_PATH   Optional JSON file path for writing a structured evidence bundle.
EOF
}

assert_execution_mode() {
  local response="$1"
  if [[ "${EXPECTED_QUERY_EXECUTION_MODE}" == "REAL" ]]; then
    json_assert "${response}" 'payload["metadata"]["executionMode"] in ["JDBC", "REST", "CLIENT"]'
    return
  fi
  json_assert "${response}" "payload['metadata']['executionMode'] == '${EXPECTED_QUERY_EXECUTION_MODE}'"
}

assert_route_order() {
  local response="$1"
  local actual_order
  actual_order="$(json_extract "${response}" "','.join(payload['effectiveRouteOrder'])")"
  if [[ "${actual_order}" != "${EXPECTED_HETU_ROUTE_ORDER}" ]]; then
    echo "Unexpected Hetu route order: expected=${EXPECTED_HETU_ROUTE_ORDER} actual=${actual_order}" >&2
    exit 1
  fi
}

assert_execution_route_metadata() {
  local response="$1"
  local actual_order
  json_assert "${response}" "payload['metadata']['routeProfile'] == '${EXPECTED_HETU_ROUTE_PROFILE}'"
  json_assert "${response}" "payload['metadata']['routeEvidenceSource'] == '${EXPECTED_HETU_EVIDENCE_SOURCE}'"
  json_assert "${response}" "payload['metadata']['routeVerificationStatus'] == '${EXPECTED_HETU_LIVE_VERIFICATION_STATUS}'"
  actual_order="$(json_extract "${response}" "','.join(payload['metadata']['routeOrder'])")"
  if [[ "${actual_order}" != "${EXPECTED_HETU_ROUTE_ORDER}" ]]; then
    echo "Unexpected execution route order: expected=${EXPECTED_HETU_ROUTE_ORDER} actual=${actual_order}" >&2
    exit 1
  fi
}

write_evidence_bundle() {
  local calibration_response="$1"
  local execution_response="$2"
  if [[ -z "${HETU_ENV_EVIDENCE_OUTPUT_PATH}" ]]; then
    return
  fi

  ROUTE_CALIBRATION_JSON="${calibration_response}" \
  EXECUTION_RESPONSE_JSON="${execution_response}" \
  python3 - "${HETU_ENV_EVIDENCE_OUTPUT_PATH}" <<'PY'
import json
import os
import pathlib
import sys
from datetime import datetime, timezone

path = pathlib.Path(sys.argv[1])
path.parent.mkdir(parents=True, exist_ok=True)
bundle = {
    "generatedAt": datetime.now(timezone.utc).isoformat(),
    "routeCalibration": json.loads(os.environ["ROUTE_CALIBRATION_JSON"]),
    "executionResponse": json.loads(os.environ["EXECUTION_RESPONSE_JSON"]),
}
path.write_text(json.dumps(bundle, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
PY
}

main() {
  local issued_at expires_at calibration_response response
  local -a headers

  while [[ $# -gt 0 ]]; do
    case "$1" in
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

  require_command curl
  require_command python3

  issued_at="$(date +%s000)"
  expires_at="$((issued_at + 600000))"
  mapfile -t headers < <(build_protected_headers "hetu-env-smoke-$(date +%Y%m%d%H%M%S)" "hetu-env-smoke-$(date +%Y%m%d%H%M%S)" "${issued_at}" "${expires_at}")

  print_step "Health check: query-execution"
  response="$(assert_get_json "${QUERY_EXECUTION_HEALTH_URL}")"
  echo "${response}"
  json_assert "${response}" 'payload["status"] == "UP"'

  print_step "Fetching Hetu route calibration snapshot"
  calibration_response="$(assert_get_json "${QUERY_EXECUTION_HETU_CALIBRATION_URL}" "${headers[@]}")"
  echo "${calibration_response}"
  json_assert "${calibration_response}" 'payload["hetuEnabled"] == True'
  json_assert "${calibration_response}" "payload['routeProfile'] == '${EXPECTED_HETU_ROUTE_PROFILE}'"
  json_assert "${calibration_response}" "payload['clusterEvidence']['evidenceSource'] == '${EXPECTED_HETU_EVIDENCE_SOURCE}'"
  json_assert "${calibration_response}" "payload['clusterEvidence']['liveVerificationStatus'] == '${EXPECTED_HETU_LIVE_VERIFICATION_STATUS}'"
  json_assert "${calibration_response}" 'payload["implementationStage"] == "HETU_ROUTE_CALIBRATION_BASELINE"'
  assert_route_order "${calibration_response}"

  print_step "Calling query-execution Hetu real-mode smoke"
  response="$(assert_post_json "${QUERY_EXECUTION_BASE_URL%/}/api/query-execution/queries/execute" \
    "{\"sqlText\":\"SELECT * FROM orders\",\"tenantId\":\"${REQUEST_TENANT_ID}\",\"datasourceType\":\"HETU\",\"accelerationPreference\":\"PREFER_ACCELERATED\",\"faultToleranceStrategy\":\"FAIL_FAST\"}" \
    "${headers[@]}")"
  echo "${response}"
  json_assert "${response}" 'payload["status"] == "SUCCESS"'
  json_assert "${response}" 'payload["metadata"]["targetEngine"] == "HETU"'
  json_assert "${response}" 'payload["implementationStage"] == "HETU_REAL_INTEGRATION"'
  assert_execution_mode "${response}"
  assert_execution_route_metadata "${response}"
  write_evidence_bundle "${calibration_response}" "${response}"

  print_step "Hetu environment smoke completed"
}

main "$@"
