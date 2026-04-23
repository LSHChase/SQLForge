#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/smoke-lib.sh"

SQL_OPTIMIZATION_API_BASE_URL="${SQL_OPTIMIZATION_API_BASE_URL:-http://localhost:8082}"
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
FAILURE_SUBMIT_REQUEST_ID=""
FAILURE_STATUS_REQUEST_ID=""
COMPENSATION_REQUEST_ID=""
COMPENSATION_TRACE_ID=""

usage() {
  cat <<'EOF'
Usage: ./scripts/manual-sql-optimization-governance-smoke.sh [--cleanup]

Options:
  --cleanup   Delete smoke-created optimization_task, audit_log and kafka_message_queue rows after verification.
EOF
}

mysql_exec() {
  local sql="$1"
  docker exec "${MYSQL_CONTAINER}" sh -lc \
    "mysql -N -B -u${MYSQL_USER} -p${MYSQL_PASSWORD} ${MYSQL_DATABASE} -e \"$sql\""
}

poll_terminal_status() {
  local task_id="$1"
  local expected_status="$2"
  shift 2
  local response=""
  local current_status=""

  for _ in $(seq 1 30); do
    response="$(assert_get_json "${SQL_OPTIMIZATION_API_BASE_URL}/api/sql-optimization/tasks/${task_id}" "$@")"
    current_status="$(json_extract "${response}" 'payload["status"]')"
    if [[ "${current_status}" == "${expected_status}" ]]; then
      echo "${response}"
      return 0
    fi
    sleep 1
  done

  echo "Optimization task ${task_id} did not reach ${expected_status}" >&2
  echo "${response}" >&2
  exit 1
}

cleanup_rows() {
  if [[ "${CLEANUP}" != "true" ]]; then
    return
  fi

  print_step "Cleaning up smoke optimization tasks"
  mysql_exec "DELETE FROM optimization_task WHERE task_id IN ('${success_task_id:-}','${failure_task_id:-}');"

  print_step "Cleaning up smoke queue rows"
  mysql_exec "DELETE FROM kafka_message_queue WHERE topic = 'governance.audit.event' AND (message_body LIKE '%\\\"requestId\\\":\\\"${COMPENSATION_REQUEST_ID}\\\"%' OR message_body LIKE '%\\\"requestId\\\":\\\"${SUCCESS_SUBMIT_REQUEST_ID}\\\"%' OR message_body LIKE '%\\\"requestId\\\":\\\"${SUCCESS_STATUS_REQUEST_ID}\\\"%' OR message_body LIKE '%\\\"requestId\\\":\\\"${FAILURE_SUBMIT_REQUEST_ID}\\\"%' OR message_body LIKE '%\\\"requestId\\\":\\\"${FAILURE_STATUS_REQUEST_ID}\\\"%');"

  print_step "Cleaning up smoke audit rows"
  mysql_exec "DELETE FROM audit_log WHERE request_id IN ('${SUCCESS_SUBMIT_REQUEST_ID}','${SUCCESS_STATUS_REQUEST_ID}','${FAILURE_SUBMIT_REQUEST_ID}','${FAILURE_STATUS_REQUEST_ID}','${COMPENSATION_REQUEST_ID}');"
}

main() {
  local issued_at expires_at stats_before stats_after pending_before pending_after
  local success_submit success_status failure_submit failure_status compensation_status
  local success_task_id failure_task_id submit_row status_row failed_row queue_row
  local -a success_submit_headers success_status_headers failure_submit_headers failure_status_headers compensation_headers stats_headers

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

  SUCCESS_SUBMIT_REQUEST_ID="sql-opt-submit-success-$(date +%Y%m%d%H%M%S)"
  SUCCESS_STATUS_REQUEST_ID="sql-opt-status-success-$(date +%Y%m%d%H%M%S)"
  FAILURE_SUBMIT_REQUEST_ID="sql-opt-submit-failure-$(date +%Y%m%d%H%M%S)"
  FAILURE_STATUS_REQUEST_ID="sql-opt-status-failure-$(date +%Y%m%d%H%M%S)"
  COMPENSATION_REQUEST_ID="sql-opt-status-comp-$(date +%Y%m%d%H%M%S)"
  COMPENSATION_TRACE_ID="${COMPENSATION_TRACE_PREFIX}-sql-opt-$(date +%Y%m%d%H%M%S)"

  mapfile -t success_submit_headers < <(build_protected_headers "${SUCCESS_SUBMIT_REQUEST_ID}" "${SUCCESS_SUBMIT_REQUEST_ID}" "${issued_at}" "${expires_at}")
  mapfile -t success_status_headers < <(build_protected_headers "${SUCCESS_STATUS_REQUEST_ID}" "${SUCCESS_STATUS_REQUEST_ID}" "${issued_at}" "${expires_at}")
  mapfile -t failure_submit_headers < <(build_protected_headers "${FAILURE_SUBMIT_REQUEST_ID}" "${FAILURE_SUBMIT_REQUEST_ID}" "${issued_at}" "${expires_at}")
  mapfile -t failure_status_headers < <(build_protected_headers "${FAILURE_STATUS_REQUEST_ID}" "${FAILURE_STATUS_REQUEST_ID}" "${issued_at}" "${expires_at}")
  mapfile -t compensation_headers < <(build_protected_headers "${COMPENSATION_REQUEST_ID}" "${COMPENSATION_TRACE_ID}" "${issued_at}" "${expires_at}")
  mapfile -t stats_headers < <(build_protected_headers "sql-opt-stats-$(date +%Y%m%d%H%M%S)" "sql-opt-stats-$(date +%Y%m%d%H%M%S)" "${issued_at}" "${expires_at}")

  print_step "Reading queue stats before compensation scenario"
  stats_before="$(assert_get_json "${GOVERNANCE_API_BASE_URL}/api/governance/admin/messages/stats" "${stats_headers[@]}")"
  pending_before="$(json_extract "${stats_before}" 'payload["pending"]')"
  echo "${stats_before}"

  print_step "Submitting sql-optimization success task"
  success_submit="$(assert_post_json "${SQL_OPTIMIZATION_API_BASE_URL}/api/sql-optimization/tasks" \
    '{"tenantId":"tenant-a","taskType":"REWRITE","sqlText":"SELECT * FROM orders","datasourceType":"HETU","taskContext":{"parseDepth":"DEEP"}}' \
    "${success_submit_headers[@]}")"
  echo "${success_submit}"
  json_assert "${success_submit}" 'payload["status"] == "QUEUED"'
  success_task_id="$(json_extract "${success_submit}" 'payload["taskId"]')"

  print_step "Polling sql-optimization success task to SUCCEEDED"
  success_status="$(poll_terminal_status "${success_task_id}" "SUCCEEDED" "${success_status_headers[@]}")"
  echo "${success_status}"
  json_assert "${success_status}" 'payload["suggestion"]["summary"] is not None'

  print_step "Verifying sql-optimization success audit rows"
  submit_row="$(mysql_exec "SELECT status, target_id FROM audit_log WHERE request_id = '${SUCCESS_SUBMIT_REQUEST_ID}' AND service_code = 'SQL_OPTIMIZATION' AND operation_type = 'OPTIMIZATION_TASK_SUBMIT' ORDER BY id DESC LIMIT 1;")"
  status_row="$(mysql_exec "SELECT status, target_id FROM audit_log WHERE request_id = '${SUCCESS_STATUS_REQUEST_ID}' AND service_code = 'SQL_OPTIMIZATION' AND operation_type = 'OPTIMIZATION_TASK_STATUS_QUERY' ORDER BY id DESC LIMIT 1;")"
  echo "${submit_row}"
  echo "${status_row}"
  if [[ "${submit_row}" != QUEUED$'\t'"${success_task_id}" ]]; then
    echo "Expected QUEUED submit audit row for ${SUCCESS_SUBMIT_REQUEST_ID}" >&2
    exit 1
  fi
  if [[ "${status_row}" != SUCCEEDED$'\t'"${success_task_id}" ]]; then
    echo "Expected SUCCEEDED status audit row for ${SUCCESS_STATUS_REQUEST_ID}" >&2
    exit 1
  fi

  print_step "Submitting sql-optimization failure task"
  failure_submit="$(assert_post_json "${SQL_OPTIMIZATION_API_BASE_URL}/api/sql-optimization/tasks" \
    '{"tenantId":"tenant-a","taskType":"ACCELERATION_SUGGESTION","sqlText":"SELECT * FROM orders /*FAIL_OPTIMIZATION*/","datasourceType":"HETU"}' \
    "${failure_submit_headers[@]}")"
  echo "${failure_submit}"
  json_assert "${failure_submit}" 'payload["status"] == "QUEUED"'
  failure_task_id="$(json_extract "${failure_submit}" 'payload["taskId"]')"

  print_step "Polling sql-optimization failure task to FAILED"
  failure_status="$(poll_terminal_status "${failure_task_id}" "FAILED" "${failure_status_headers[@]}")"
  echo "${failure_status}"
  json_assert "${failure_status}" 'payload["failure"]["code"] == 13000'

  print_step "Triggering compensated failed status query"
  compensation_status="$(assert_get_json "${SQL_OPTIMIZATION_API_BASE_URL}/api/sql-optimization/tasks/${failure_task_id}" "${compensation_headers[@]}")"
  echo "${compensation_status}"
  json_assert "${compensation_status}" 'payload["status"] == "FAILED"'

  print_step "Verifying compensated failed audit row"
  failed_row="$(mysql_exec "SELECT status, target_id FROM audit_log WHERE request_id = '${COMPENSATION_REQUEST_ID}' AND service_code = 'SQL_OPTIMIZATION' AND operation_type = 'OPTIMIZATION_TASK_STATUS_QUERY' ORDER BY id DESC LIMIT 1;")"
  echo "${failed_row}"
  if [[ "${failed_row}" != FAILED$'\t'"${failure_task_id}" ]]; then
    echo "Expected FAILED status audit row for ${COMPENSATION_REQUEST_ID}" >&2
    exit 1
  fi

  print_step "Verifying queued audit compensation message"
  queue_row="$(mysql_exec "SELECT COUNT(*) FROM kafka_message_queue WHERE topic = 'governance.audit.event' AND status = 'PENDING' AND message_body LIKE '%\\\"serviceCode\\\":\\\"SQL_OPTIMIZATION\\\"%' AND message_body LIKE '%\\\"requestId\\\":\\\"${COMPENSATION_REQUEST_ID}\\\"%' AND message_body LIKE '%\\\"traceId\\\":\\\"${COMPENSATION_TRACE_ID}\\\"%';")"
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
    echo "Expected queue pending count to increase after sql-optimization audit compensation" >&2
    exit 1
  fi

  cleanup_rows
  print_step "SQL optimization to governance smoke test completed"
}

main "$@"
