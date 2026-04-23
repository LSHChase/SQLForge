#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/smoke-lib.sh"

QUERY_EXECUTION_BASE_URL="${QUERY_EXECUTION_BASE_URL:-http://localhost:8081}"
QUERY_EXECUTION_HEALTH_URL="${QUERY_EXECUTION_HEALTH_URL:-${QUERY_EXECUTION_BASE_URL%/}/actuator/health}"
REQUEST_TENANT_ID="${REQUEST_TENANT_ID:-tenant-a}"
REQUEST_USER_ID="${REQUEST_USER_ID:-hetu-smoke-bot}"
REQUEST_ROLE_CODES="${REQUEST_ROLE_CODES:-TENANT_ADMIN,ANALYST}"
REQUEST_AUTH_SOURCE="${REQUEST_AUTH_SOURCE:-header}"
EXPECTED_QUERY_EXECUTION_MODE="${EXPECTED_QUERY_EXECUTION_MODE:-REAL}"

usage() {
  cat <<'EOF'
Usage: ./scripts/run-hetu-env-smoke.sh

Environment variables:
  QUERY_EXECUTION_BASE_URL        Base URL for query-execution.
  QUERY_EXECUTION_HEALTH_URL      Optional explicit health URL override.
  REQUEST_TENANT_ID               Tenant header value.
  REQUEST_USER_ID                 User header value.
  REQUEST_ROLE_CODES              Comma-separated role codes.
  REQUEST_AUTH_SOURCE             Auth source header value.
  EXPECTED_QUERY_EXECUTION_MODE   REAL, JDBC, REST, or CLIENT. REAL means any non-simulated Hetu mode.
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

main() {
  local issued_at expires_at response
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

  print_step "Calling query-execution Hetu real-mode smoke"
  response="$(assert_post_json "${QUERY_EXECUTION_BASE_URL%/}/api/query-execution/queries/execute" \
    "{\"sqlText\":\"SELECT * FROM orders\",\"tenantId\":\"${REQUEST_TENANT_ID}\",\"datasourceType\":\"HETU\",\"accelerationPreference\":\"PREFER_ACCELERATED\",\"faultToleranceStrategy\":\"FAIL_FAST\"}" \
    "${headers[@]}")"
  echo "${response}"
  json_assert "${response}" 'payload["status"] == "SUCCESS"'
  json_assert "${response}" 'payload["metadata"]["targetEngine"] == "HETU"'
  json_assert "${response}" 'payload["implementationStage"] == "HETU_REAL_INTEGRATION"'
  assert_execution_mode "${response}"

  print_step "Hetu environment smoke completed"
}

main "$@"
