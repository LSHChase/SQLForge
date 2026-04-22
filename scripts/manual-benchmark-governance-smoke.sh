#!/usr/bin/env bash

set -euo pipefail

BENCHMARK_ENGINE_API_BASE_URL="${BENCHMARK_ENGINE_API_BASE_URL:-http://localhost:8083}"
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

SUCCESS_SUBMIT_REQUEST_ID=""
SUCCESS_STATUS_REQUEST_ID=""
SUCCESS_REPORT_REQUEST_ID=""
FAILURE_SUBMIT_REQUEST_ID=""
FAILURE_STATUS_REQUEST_ID=""
COMPENSATION_REQUEST_ID=""
COMPENSATION_TRACE_ID=""

usage() {
  cat <<'EOF'
Usage: ./scripts/manual-benchmark-governance-smoke.sh [--cleanup]

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
  printf '\n[%s] %s\n' "$(date '+%H:%M:%S')" "$1"
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

  status_code="$(curl -s -o /tmp/sqlforge-benchmark-smoke-response.out -w '%{http_code}' \
    -X POST "$@" --data "${body}" "${url}")"
  if [[ "${status_code}" != "200" ]]; then
    echo "HTTP POST failed for ${url}, status=${status_code}" >&2
    [[ -f /tmp/sqlforge-benchmark-smoke-response.out ]] && cat /tmp/sqlforge-benchmark-smoke-response.out >&2
    exit 1
  fi
  cat /tmp/sqlforge-benchmark-smoke-response.out
}

assert_get_json() {
  local url="$1"
  shift
  local status_code

  status_code="$(curl -s -o /tmp/sqlforge-benchmark-smoke-response.out -w '%{http_code}' "$@" "${url}")"
  if [[ "${status_code}" != "200" ]]; then
    echo "HTTP GET failed for ${url}, status=${status_code}" >&2
    [[ -f /tmp/sqlforge-benchmark-smoke-response.out ]] && cat /tmp/sqlforge-benchmark-smoke-response.out >&2
    exit 1
  fi
  cat /tmp/sqlforge-benchmark-smoke-response.out
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

poll_terminal_status() {
  local task_id="$1"
  local expected_status="$2"
  shift 2
  local response=""
  local current_status=""

  for _ in $(seq 1 30); do
    response="$(assert_get_json "${BENCHMARK_ENGINE_API_BASE_URL}/api/benchmark-engine/tasks/${task_id}" "$@")"
    current_status="$(json_extract "${response}" 'payload["status"]')"
    if [[ "${current_status}" == "${expected_status}" ]]; then
      echo "${response}"
      return 0
    fi
    sleep 1
  done

  echo "Benchmark task ${task_id} did not reach ${expected_status}" >&2
  echo "${response}" >&2
  exit 1
}

cleanup_rows() {
  if [[ "${CLEANUP}" != "true" ]]; then
    return
  fi

  print_step "Cleaning up smoke queue rows"
  mysql_exec "DELETE FROM kafka_message_queue WHERE topic = 'governance.audit.event' AND (message_body LIKE '%\\\"requestId\\\":\\\"${SUCCESS_SUBMIT_REQUEST_ID}\\\"%' OR message_body LIKE '%\\\"requestId\\\":\\\"${SUCCESS_STATUS_REQUEST_ID}\\\"%' OR message_body LIKE '%\\\"requestId\\\":\\\"${SUCCESS_REPORT_REQUEST_ID}\\\"%' OR message_body LIKE '%\\\"requestId\\\":\\\"${FAILURE_SUBMIT_REQUEST_ID}\\\"%' OR message_body LIKE '%\\\"requestId\\\":\\\"${FAILURE_STATUS_REQUEST_ID}\\\"%' OR message_body LIKE '%\\\"requestId\\\":\\\"${COMPENSATION_REQUEST_ID}\\\"%');"

  print_step "Cleaning up smoke audit rows"
  mysql_exec "DELETE FROM audit_log WHERE request_id IN ('${SUCCESS_SUBMIT_REQUEST_ID}','${SUCCESS_STATUS_REQUEST_ID}','${SUCCESS_REPORT_REQUEST_ID}','${FAILURE_SUBMIT_REQUEST_ID}','${FAILURE_STATUS_REQUEST_ID}','${COMPENSATION_REQUEST_ID}');"
}

main() {
  local issued_at expires_at stats_before stats_after pending_before pending_after
  local success_submit success_status success_report failure_submit failure_status compensation_status
  local success_task_id success_report_id failure_task_id submit_row status_row report_row failed_row queue_row
  local -a success_submit_headers success_status_headers success_report_headers failure_submit_headers failure_status_headers compensation_headers stats_headers

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

  SUCCESS_SUBMIT_REQUEST_ID="benchmark-submit-success-$(date +%Y%m%d%H%M%S)"
  SUCCESS_STATUS_REQUEST_ID="benchmark-status-success-$(date +%Y%m%d%H%M%S)"
  SUCCESS_REPORT_REQUEST_ID="benchmark-report-success-$(date +%Y%m%d%H%M%S)"
  FAILURE_SUBMIT_REQUEST_ID="benchmark-submit-failure-$(date +%Y%m%d%H%M%S)"
  FAILURE_STATUS_REQUEST_ID="benchmark-status-failure-$(date +%Y%m%d%H%M%S)"
  COMPENSATION_REQUEST_ID="benchmark-status-comp-$(date +%Y%m%d%H%M%S)"
  COMPENSATION_TRACE_ID="${COMPENSATION_TRACE_PREFIX}-benchmark-$(date +%Y%m%d%H%M%S)"

  mapfile -t success_submit_headers < <(build_protected_headers "${SUCCESS_SUBMIT_REQUEST_ID}" "${SUCCESS_SUBMIT_REQUEST_ID}" "${issued_at}" "${expires_at}")
  mapfile -t success_status_headers < <(build_protected_headers "${SUCCESS_STATUS_REQUEST_ID}" "${SUCCESS_STATUS_REQUEST_ID}" "${issued_at}" "${expires_at}")
  mapfile -t success_report_headers < <(build_protected_headers "${SUCCESS_REPORT_REQUEST_ID}" "${SUCCESS_REPORT_REQUEST_ID}" "${issued_at}" "${expires_at}")
  mapfile -t failure_submit_headers < <(build_protected_headers "${FAILURE_SUBMIT_REQUEST_ID}" "${FAILURE_SUBMIT_REQUEST_ID}" "${issued_at}" "${expires_at}")
  mapfile -t failure_status_headers < <(build_protected_headers "${FAILURE_STATUS_REQUEST_ID}" "${FAILURE_STATUS_REQUEST_ID}" "${issued_at}" "${expires_at}")
  mapfile -t compensation_headers < <(build_protected_headers "${COMPENSATION_REQUEST_ID}" "${COMPENSATION_TRACE_ID}" "${issued_at}" "${expires_at}")
  mapfile -t stats_headers < <(build_protected_headers "benchmark-stats-$(date +%Y%m%d%H%M%S)" "benchmark-stats-$(date +%Y%m%d%H%M%S)" "${issued_at}" "${expires_at}")

  print_step "Reading queue stats before compensation scenario"
  stats_before="$(assert_get_json "${GOVERNANCE_API_BASE_URL}/api/governance/admin/messages/stats" "${stats_headers[@]}")"
  pending_before="$(json_extract "${stats_before}" 'payload["pending"]')"
  echo "${stats_before}"

  print_step "Submitting benchmark success task"
  success_submit="$(assert_post_json "${BENCHMARK_ENGINE_API_BASE_URL}/api/benchmark-engine/tasks" \
    '{"tenantId":"tenant-a","taskType":"COMPARISON","sqlText":"SELECT * FROM orders","taskContext":{"targetEngines":["HETU","HIVE"],"readonlyRequired":true,"shadowEnvironmentMode":"REQUIRED"}}' \
    "${success_submit_headers[@]}")"
  echo "${success_submit}"
  json_assert "${success_submit}" 'payload["status"] == "QUEUED"'
  success_task_id="$(json_extract "${success_submit}" 'payload["taskId"]')"

  print_step "Polling benchmark success task to SUCCEEDED"
  success_status="$(poll_terminal_status "${success_task_id}" "SUCCEEDED" "${success_status_headers[@]}")"
  echo "${success_status}"
  success_report_id="$(json_extract "${success_status}" 'payload["reportId"]')"

  print_step "Reading benchmark report"
  success_report="$(assert_get_json "${BENCHMARK_ENGINE_API_BASE_URL}/api/benchmark-engine/reports/${success_report_id}" "${success_report_headers[@]}")"
  echo "${success_report}"
  json_assert "${success_report}" 'payload["reportId"] == "'"${success_report_id}"'"'
  json_assert "${success_report}" 'payload["requestedFormat"] == "JSON"'
  json_assert "${success_report}" '"HETU" in payload["targetEngines"] and "HIVE" in payload["targetEngines"]'

  print_step "Verifying benchmark success audit rows"
  submit_row="$(mysql_exec "SELECT status, target_id FROM audit_log WHERE request_id = '${SUCCESS_SUBMIT_REQUEST_ID}' AND service_code = 'BENCHMARK_ENGINE' AND operation_type = 'BENCHMARK_TASK_SUBMIT' ORDER BY id DESC LIMIT 1;")"
  status_row="$(mysql_exec "SELECT status, target_id FROM audit_log WHERE request_id = '${SUCCESS_STATUS_REQUEST_ID}' AND service_code = 'BENCHMARK_ENGINE' AND operation_type = 'BENCHMARK_TASK_STATUS_QUERY' ORDER BY id DESC LIMIT 1;")"
  report_row="$(mysql_exec "SELECT status, target_id FROM audit_log WHERE request_id = '${SUCCESS_REPORT_REQUEST_ID}' AND service_code = 'BENCHMARK_ENGINE' AND operation_type = 'BENCHMARK_REPORT_QUERY' ORDER BY id DESC LIMIT 1;")"
  echo "${submit_row}"
  echo "${status_row}"
  echo "${report_row}"
  if [[ "${submit_row}" != QUEUED$'\t'"${success_task_id}" ]]; then
    echo "Expected QUEUED submit audit row for ${SUCCESS_SUBMIT_REQUEST_ID}" >&2
    exit 1
  fi
  if [[ "${status_row}" != SUCCEEDED$'\t'"${success_task_id}" ]]; then
    echo "Expected SUCCEEDED status audit row for ${SUCCESS_STATUS_REQUEST_ID}" >&2
    exit 1
  fi
  if [[ "${report_row}" != SUCCESS$'\t'"${success_report_id}" ]]; then
    echo "Expected SUCCESS report audit row for ${SUCCESS_REPORT_REQUEST_ID}" >&2
    exit 1
  fi

  print_step "Submitting benchmark failure task"
  failure_submit="$(assert_post_json "${BENCHMARK_ENGINE_API_BASE_URL}/api/benchmark-engine/tasks" \
    '{"tenantId":"tenant-a","taskType":"BASELINE","sqlText":"SELECT * FROM orders /*FAIL_BENCHMARK*/"}' \
    "${failure_submit_headers[@]}")"
  echo "${failure_submit}"
  json_assert "${failure_submit}" 'payload["status"] == "QUEUED"'
  failure_task_id="$(json_extract "${failure_submit}" 'payload["taskId"]')"

  print_step "Polling benchmark failure task to FAILED"
  failure_status="$(poll_terminal_status "${failure_task_id}" "FAILED" "${failure_status_headers[@]}")"
  echo "${failure_status}"
  json_assert "${failure_status}" 'payload["error"]["code"] == 14000'

  print_step "Triggering compensated failed benchmark status query"
  compensation_status="$(assert_get_json "${BENCHMARK_ENGINE_API_BASE_URL}/api/benchmark-engine/tasks/${failure_task_id}" "${compensation_headers[@]}")"
  echo "${compensation_status}"
  json_assert "${compensation_status}" 'payload["status"] == "FAILED"'

  print_step "Verifying compensated failed audit row"
  failed_row="$(mysql_exec "SELECT status, target_id FROM audit_log WHERE request_id = '${COMPENSATION_REQUEST_ID}' AND service_code = 'BENCHMARK_ENGINE' AND operation_type = 'BENCHMARK_TASK_STATUS_QUERY' ORDER BY id DESC LIMIT 1;")"
  echo "${failed_row}"
  if [[ "${failed_row}" != FAILED$'\t'"${failure_task_id}" ]]; then
    echo "Expected FAILED benchmark status audit row for ${COMPENSATION_REQUEST_ID}" >&2
    exit 1
  fi

  print_step "Verifying queued audit compensation message"
  queue_row="$(mysql_exec "SELECT COUNT(*) FROM kafka_message_queue WHERE topic = 'governance.audit.event' AND status = 'PENDING' AND message_body LIKE '%\\\"serviceCode\\\":\\\"BENCHMARK_ENGINE\\\"%' AND message_body LIKE '%\\\"requestId\\\":\\\"${COMPENSATION_REQUEST_ID}\\\"%' AND message_body LIKE '%\\\"traceId\\\":\\\"${COMPENSATION_TRACE_ID}\\\"%';")"
  echo "${queue_row}"
  if (( queue_row < 1 )); then
    echo "Expected at least one pending compensation message for ${COMPENSATION_REQUEST_ID}" >&2
    exit 1
  fi

  print_step "Reading queue stats after compensation scenario"
  stats_after="$(assert_get_json "${GOVERNANCE_API_BASE_URL}/api/governance/admin/messages/stats" "${stats_headers[@]}")"
  pending_after="$(json_extract "${stats_after}" 'payload["pending"]')"
  echo "${stats_after}"
  if (( pending_after < pending_before + 1 )); then
    echo "Expected queue pending count to increase after benchmark audit compensation" >&2
    exit 1
  fi

  cleanup_rows
  print_step "Benchmark-engine to governance smoke test completed"
}

main "$@"
