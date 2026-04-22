#!/usr/bin/env bash

set -euo pipefail

QUERY_API_BASE_URL="${QUERY_API_BASE_URL:-http://localhost:8081}"
GOVERNANCE_API_BASE_URL="${GOVERNANCE_API_BASE_URL:-http://localhost:8080}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-sqlforge-mysql}"
MYSQL_DATABASE="${MYSQL_DATABASE:-sqlforge}"
MYSQL_USER="${MYSQL_USER:-sqlforge}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-sqlforge}"
REQUEST_TENANT_ID="${REQUEST_TENANT_ID:-tenant-a}"
REQUEST_USER_ID="${REQUEST_USER_ID:-analyst-001}"
REQUEST_ROLE_CODES="${REQUEST_ROLE_CODES:-TENANT_ADMIN,ANALYST}"
REQUEST_AUTH_SOURCE="${REQUEST_AUTH_SOURCE:-header}"
COMPENSATION_TRACE_PREFIX="${SQLFORGE_GOVERNANCE_SMOKE_FORCE_TRACE_PREFIX:-SMOKE-FORCE-AUDIT-FALLBACK}"
CLEANUP=false

SUCCESS_REQUEST_ID=""
SUCCESS_TRACE_ID=""
COMPENSATION_REQUEST_ID=""
COMPENSATION_TRACE_ID=""
COMPENSATION_QUEUE_IDS=""

usage() {
  cat <<'EOF'
Usage: ./scripts/manual-query-governance-smoke.sh [--cleanup]

Options:
  --cleanup   Delete smoke-created audit_log and kafka_message_queue rows after verification.
EOF
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

mysql_exec() {
  local sql="$1"
  docker exec "${MYSQL_CONTAINER}" sh -lc \
    "mysql -N -B -u${MYSQL_USER} -p${MYSQL_PASSWORD} ${MYSQL_DATABASE} -e \"$sql\""
}

print_step() {
  local title="$1"
  printf '\n[%s] %s\n' "$(date '+%H:%M:%S')" "${title}"
}

build_protected_headers() {
  local request_id="$1"
  local trace_id="$2"
  local issued_at="$3"
  local expires_at="$4"

  printf '%s\n' \
    "-H" "X-Tenant-Id: ${REQUEST_TENANT_ID}" \
    "-H" "X-User-Id: ${REQUEST_USER_ID}" \
    "-H" "X-Role-Codes: ${REQUEST_ROLE_CODES}" \
    "-H" "X-Request-Id: ${request_id}" \
    "-H" "X-Trace-Id: ${trace_id}" \
    "-H" "X-Auth-Source: ${REQUEST_AUTH_SOURCE}" \
    "-H" "X-Issued-At: ${issued_at}" \
    "-H" "X-Expires-At: ${expires_at}" \
    "-H" "Content-Type: application/json"
}

assert_post_json() {
  local url="$1"
  local body="$2"
  shift 2
  local status_code

  status_code="$(curl -s -o /tmp/sqlforge-query-governance-smoke-response.out -w '%{http_code}' \
    -X POST "$@" --data "${body}" "${url}")"
  if [[ "${status_code}" != "200" ]]; then
    echo "HTTP POST failed for ${url}, status=${status_code}" >&2
    [[ -f /tmp/sqlforge-query-governance-smoke-response.out ]] && cat /tmp/sqlforge-query-governance-smoke-response.out >&2
    exit 1
  fi
  cat /tmp/sqlforge-query-governance-smoke-response.out
}

assert_get_json() {
  local url="$1"
  shift
  local status_code

  status_code="$(curl -s -o /tmp/sqlforge-query-governance-smoke-response.out -w '%{http_code}' "$@" "${url}")"
  if [[ "${status_code}" != "200" ]]; then
    echo "HTTP GET failed for ${url}, status=${status_code}" >&2
    [[ -f /tmp/sqlforge-query-governance-smoke-response.out ]] && cat /tmp/sqlforge-query-governance-smoke-response.out >&2
    exit 1
  fi
  cat /tmp/sqlforge-query-governance-smoke-response.out
}

json_assert() {
  local json_payload="$1"
  local python_expr="$2"
  JSON_PAYLOAD="${json_payload}" PYTHON_EXPR="${python_expr}" python3 - <<'PY'
import json
import os

payload = json.loads(os.environ["JSON_PAYLOAD"])
expression = os.environ["PYTHON_EXPR"]
if not eval(expression, {"payload": payload}):
    raise SystemExit(1)
PY
}

json_extract() {
  local json_payload="$1"
  local python_expr="$2"
  JSON_PAYLOAD="${json_payload}" PYTHON_EXPR="${python_expr}" python3 - <<'PY'
import json
import os

payload = json.loads(os.environ["JSON_PAYLOAD"])
expression = os.environ["PYTHON_EXPR"]
value = eval(expression, {"payload": payload})
print("" if value is None else value)
PY
}

cleanup_rows() {
  if [[ "${CLEANUP}" != "true" ]]; then
    return
  fi

  if [[ -n "${SUCCESS_REQUEST_ID}" || -n "${COMPENSATION_REQUEST_ID}" ]]; then
    print_step "Cleaning up smoke queue rows"
    mysql_exec "DELETE FROM kafka_message_queue WHERE topic = 'governance.audit.event' AND (message_body LIKE '%\\\"requestId\\\":\\\"${SUCCESS_REQUEST_ID}\\\"%' OR message_body LIKE '%\\\"requestId\\\":\\\"${COMPENSATION_REQUEST_ID}\\\"%');"
  fi

  if [[ -n "${SUCCESS_REQUEST_ID}" || -n "${COMPENSATION_REQUEST_ID}" ]]; then
    print_step "Cleaning up smoke audit rows"
    mysql_exec "DELETE FROM audit_log WHERE request_id IN ('${SUCCESS_REQUEST_ID}','${COMPENSATION_REQUEST_ID}');"
  fi
}

main() {
  local issued_at expires_at stats_before stats_after
  local success_response compensation_response success_row compensation_row queue_row queue_ids_row pending_before pending_after
  local -a success_headers stats_headers compensation_headers

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --cleanup)
        CLEANUP=true
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

  require_command curl
  require_command docker
  require_command python3

  issued_at="$(date +%s000)"
  expires_at="$((issued_at + 600000))"

  SUCCESS_REQUEST_ID="query-governance-success-$(date +%Y%m%d%H%M%S)"
  SUCCESS_TRACE_ID="query-governance-success-$(date +%Y%m%d%H%M%S)"
  COMPENSATION_REQUEST_ID="query-governance-comp-$(date +%Y%m%d%H%M%S)"
  COMPENSATION_TRACE_ID="${COMPENSATION_TRACE_PREFIX}-$(date +%Y%m%d%H%M%S)"

  mapfile -t success_headers < <(build_protected_headers "${SUCCESS_REQUEST_ID}" "${SUCCESS_TRACE_ID}" "${issued_at}" "${expires_at}")
  mapfile -t compensation_headers < <(build_protected_headers "${COMPENSATION_REQUEST_ID}" "${COMPENSATION_TRACE_ID}" "${issued_at}" "${expires_at}")
  mapfile -t stats_headers < <(build_protected_headers "query-governance-stats-$(date +%Y%m%d%H%M%S)" "query-governance-stats-$(date +%Y%m%d%H%M%S)" "${issued_at}" "${expires_at}")

  print_step "Reading queue stats before compensation scenario"
  stats_before="$(assert_get_json "${GOVERNANCE_API_BASE_URL}/api/governance/admin/messages/stats" "${stats_headers[@]}")"
  echo "${stats_before}"
  pending_before="$(json_extract "${stats_before}" 'payload["pending"]')"

  print_step "Calling query-execution success path"
  success_response="$(assert_post_json "${QUERY_API_BASE_URL}/api/query-execution/queries/execute" \
    '{"sqlText":"SELECT * FROM orders","tenantId":"tenant-a","datasourceType":"HETU","accelerationPreference":"PREFER_ACCELERATED","faultToleranceStrategy":"FAIL_FAST"}' \
    "${success_headers[@]}")"
  echo "${success_response}"
  json_assert "${success_response}" 'payload["status"] == "SUCCESS"'
  json_assert "${success_response}" 'payload["metadata"]["targetEngine"] == "HETU"'

  print_step "Verifying success audit row"
  success_row="$(mysql_exec "SELECT status, target_id FROM audit_log WHERE request_id = '${SUCCESS_REQUEST_ID}' AND service_code = 'QUERY_EXECUTION' AND operation_type = 'QUERY_EXECUTE_SYNC' ORDER BY id DESC LIMIT 1;")"
  echo "${success_row}"
  if [[ "${success_row}" != SUCCESS$'\t'* ]]; then
    echo "Expected SUCCESS audit row for ${SUCCESS_REQUEST_ID}" >&2
    exit 1
  fi

  print_step "Calling query-execution degraded fallback path with audit compensation trigger"
  compensation_response="$(assert_post_json "${QUERY_API_BASE_URL}/api/query-execution/queries/execute" \
    '{"sqlText":"SELECT * FROM orders","tenantId":"tenant-a","datasourceType":"HETU","faultToleranceStrategy":"RETRY_THEN_FALLBACK","queryContext":{"timeoutMs":30}}' \
    "${compensation_headers[@]}")"
  echo "${compensation_response}"
  json_assert "${compensation_response}" 'payload["status"] == "PARTIAL"'
  json_assert "${compensation_response}" 'payload["degraded"] is True'
  json_assert "${compensation_response}" 'payload["metadata"]["targetEngine"] == "HIVE"'
  json_assert "${compensation_response}" 'len(payload["retryPath"]) == 2'

  print_step "Verifying degraded audit row"
  compensation_row="$(mysql_exec "SELECT status, target_id FROM audit_log WHERE request_id = '${COMPENSATION_REQUEST_ID}' AND service_code = 'QUERY_EXECUTION' AND operation_type = 'QUERY_EXECUTE_SYNC' ORDER BY id DESC LIMIT 1;")"
  echo "${compensation_row}"
  if [[ "${compensation_row}" != PARTIAL$'\t'* ]]; then
    echo "Expected PARTIAL audit row for ${COMPENSATION_REQUEST_ID}" >&2
    exit 1
  fi

  print_step "Verifying queued audit compensation message"
  queue_row="$(mysql_exec "SELECT COUNT(*), IFNULL(GROUP_CONCAT(id), '') FROM kafka_message_queue WHERE topic = 'governance.audit.event' AND status = 'PENDING' AND message_body LIKE '%\\\"serviceCode\\\":\\\"QUERY_EXECUTION\\\"%' AND message_body LIKE '%\\\"requestId\\\":\\\"${COMPENSATION_REQUEST_ID}\\\"%' AND message_body LIKE '%\\\"traceId\\\":\\\"${COMPENSATION_TRACE_ID}\\\"%';")"
  echo "${queue_row}"
  if [[ "${queue_row}" != $'1\t'* && "${queue_row}" != $'2\t'* && "${queue_row}" != $'3\t'* && "${queue_row}" != $'4\t'* && "${queue_row}" != $'5\t'* ]]; then
    echo "Expected at least one pending compensation message for ${COMPENSATION_REQUEST_ID}" >&2
    exit 1
  fi
  queue_ids_row="$(mysql_exec "SELECT IFNULL(GROUP_CONCAT(id), '') FROM kafka_message_queue WHERE topic = 'governance.audit.event' AND status = 'PENDING' AND message_body LIKE '%\\\"requestId\\\":\\\"${COMPENSATION_REQUEST_ID}\\\"%' AND message_body LIKE '%\\\"traceId\\\":\\\"${COMPENSATION_TRACE_ID}\\\"%';")"
  COMPENSATION_QUEUE_IDS="$(echo "${queue_ids_row}" | tail -n 1 | tr -d '\r')"

  print_step "Reading queue stats after compensation scenario"
  stats_after="$(assert_get_json "${GOVERNANCE_API_BASE_URL}/api/governance/admin/messages/stats" "${stats_headers[@]}")"
  echo "${stats_after}"
  pending_after="$(json_extract "${stats_after}" 'payload["pending"]')"
  if (( pending_after < pending_before + 1 )); then
    echo "Expected queue pending count to increase after audit compensation" >&2
    exit 1
  fi

  cleanup_rows
  print_step "Query-execution to governance smoke test completed"
}

main "$@"
