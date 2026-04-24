#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/smoke-lib.sh"

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
SUCCESS_RAW_REQUEST_ID=""
FAILURE_SUBMIT_REQUEST_ID=""
FAILURE_STATUS_REQUEST_ID=""
COMPENSATION_REQUEST_ID=""
COMPENSATION_TRACE_ID=""
SUCCESS_TASK_ID=""
SUCCESS_REPORT_ID=""
FAILURE_TASK_ID=""

usage() {
  cat <<'EOF'
Usage: ./scripts/manual-benchmark-governance-smoke.sh [--cleanup]

Options:
  --cleanup   Delete smoke-created benchmark_task/benchmark_task_report/audit_log/kafka_message_queue rows after verification.
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

  print_step "Cleaning up persisted benchmark report rows"
  mysql_exec "DELETE FROM benchmark_task_report WHERE report_id IN ('${SUCCESS_REPORT_ID:-}','report-${FAILURE_TASK_ID:-}');"

  print_step "Cleaning up persisted benchmark task rows"
  mysql_exec "DELETE FROM benchmark_task WHERE task_id IN ('${SUCCESS_TASK_ID:-}','${FAILURE_TASK_ID:-}');"

  print_step "Cleaning up smoke queue rows"
  mysql_exec "DELETE FROM kafka_message_queue WHERE topic = 'governance.audit.event' AND (message_body LIKE '%\\\"requestId\\\":\\\"${SUCCESS_SUBMIT_REQUEST_ID}\\\"%' OR message_body LIKE '%\\\"requestId\\\":\\\"${SUCCESS_STATUS_REQUEST_ID}\\\"%' OR message_body LIKE '%\\\"requestId\\\":\\\"${SUCCESS_REPORT_REQUEST_ID}\\\"%' OR message_body LIKE '%\\\"requestId\\\":\\\"${SUCCESS_RAW_REQUEST_ID}\\\"%' OR message_body LIKE '%\\\"requestId\\\":\\\"${FAILURE_SUBMIT_REQUEST_ID}\\\"%' OR message_body LIKE '%\\\"requestId\\\":\\\"${FAILURE_STATUS_REQUEST_ID}\\\"%' OR message_body LIKE '%\\\"requestId\\\":\\\"${COMPENSATION_REQUEST_ID}\\\"%');"

  print_step "Cleaning up smoke audit rows"
  mysql_exec "DELETE FROM audit_log WHERE request_id IN ('${SUCCESS_SUBMIT_REQUEST_ID}','${SUCCESS_STATUS_REQUEST_ID}','${SUCCESS_REPORT_REQUEST_ID}','${SUCCESS_RAW_REQUEST_ID}','${FAILURE_SUBMIT_REQUEST_ID}','${FAILURE_STATUS_REQUEST_ID}','${COMPENSATION_REQUEST_ID}');"
}

main() {
  local issued_at expires_at stats_before stats_after pending_before pending_after
  local success_submit success_status success_report success_raw failure_submit failure_status compensation_status
  local submit_row status_row report_row raw_row failed_row queue_row
  local persisted_task_row persisted_report_row persisted_failed_task_row persisted_failed_report_count
  local -a success_submit_headers success_status_headers success_report_headers success_raw_headers failure_submit_headers failure_status_headers compensation_headers stats_headers

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
  SUCCESS_RAW_REQUEST_ID="benchmark-raw-success-$(date +%Y%m%d%H%M%S)"
  FAILURE_SUBMIT_REQUEST_ID="benchmark-submit-failure-$(date +%Y%m%d%H%M%S)"
  FAILURE_STATUS_REQUEST_ID="benchmark-status-failure-$(date +%Y%m%d%H%M%S)"
  COMPENSATION_REQUEST_ID="benchmark-status-comp-$(date +%Y%m%d%H%M%S)"
  COMPENSATION_TRACE_ID="${COMPENSATION_TRACE_PREFIX}-benchmark-$(date +%Y%m%d%H%M%S)"

  mapfile -t success_submit_headers < <(build_protected_headers "${SUCCESS_SUBMIT_REQUEST_ID}" "${SUCCESS_SUBMIT_REQUEST_ID}" "${issued_at}" "${expires_at}")
  mapfile -t success_status_headers < <(build_protected_headers "${SUCCESS_STATUS_REQUEST_ID}" "${SUCCESS_STATUS_REQUEST_ID}" "${issued_at}" "${expires_at}")
  mapfile -t success_report_headers < <(build_protected_headers "${SUCCESS_REPORT_REQUEST_ID}" "${SUCCESS_REPORT_REQUEST_ID}" "${issued_at}" "${expires_at}")
  mapfile -t success_raw_headers < <(build_protected_headers "${SUCCESS_RAW_REQUEST_ID}" "${SUCCESS_RAW_REQUEST_ID}" "${issued_at}" "${expires_at}")
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
  SUCCESS_TASK_ID="$(json_extract "${success_submit}" 'payload["taskId"]')"

  print_step "Polling benchmark success task to SUCCEEDED"
  success_status="$(poll_terminal_status "${SUCCESS_TASK_ID}" "SUCCEEDED" "${success_status_headers[@]}")"
  echo "${success_status}"
  SUCCESS_REPORT_ID="$(json_extract "${success_status}" 'payload["reportId"]')"

  print_step "Reading benchmark report"
  success_report="$(assert_get_json "${BENCHMARK_ENGINE_API_BASE_URL}/api/benchmark-engine/reports/${SUCCESS_REPORT_ID}" "${success_report_headers[@]}")"
  echo "${success_report}"
  json_assert "${success_report}" 'payload["reportId"] == "'"${SUCCESS_REPORT_ID}"'"'
  json_assert "${success_report}" 'payload["requestedFormat"] == "JSON"'
  json_assert "${success_report}" '"HETU" in payload["targetEngines"] and "HIVE" in payload["targetEngines"]'

  print_step "Downloading benchmark raw-data snapshot"
  success_raw="$(assert_get_json "${BENCHMARK_ENGINE_API_BASE_URL}/api/benchmark-engine/reports/${SUCCESS_REPORT_ID}/raw-data" "${success_raw_headers[@]}")"
  echo "${success_raw}"
  json_assert "${success_raw}" 'payload["reportId"] == "'"${SUCCESS_REPORT_ID}"'"'

  print_step "Verifying persisted benchmark task/report rows"
  persisted_task_row="$(mysql_exec "SELECT status, report_id FROM benchmark_task WHERE task_id = '${SUCCESS_TASK_ID}' LIMIT 1;")"
  persisted_report_row="$(mysql_exec "SELECT task_id FROM benchmark_task_report WHERE report_id = '${SUCCESS_REPORT_ID}' LIMIT 1;")"
  echo "${persisted_task_row}"
  echo "${persisted_report_row}"
  if [[ "${persisted_task_row}" != SUCCEEDED$'\t'"${SUCCESS_REPORT_ID}" ]]; then
    echo "Expected persisted SUCCEEDED benchmark_task row for ${SUCCESS_TASK_ID}" >&2
    exit 1
  fi
  if [[ "${persisted_report_row}" != "${SUCCESS_TASK_ID}" ]]; then
    echo "Expected persisted benchmark_task_report row for ${SUCCESS_REPORT_ID}" >&2
    exit 1
  fi

  print_step "Verifying benchmark success audit rows"
  submit_row="$(mysql_exec "SELECT status, target_id FROM audit_log WHERE request_id = '${SUCCESS_SUBMIT_REQUEST_ID}' AND service_code = 'BENCHMARK_ENGINE' AND operation_type = 'BENCHMARK_TASK_SUBMIT' ORDER BY id DESC LIMIT 1;")"
  status_row="$(mysql_exec "SELECT status, target_id FROM audit_log WHERE request_id = '${SUCCESS_STATUS_REQUEST_ID}' AND service_code = 'BENCHMARK_ENGINE' AND operation_type = 'BENCHMARK_TASK_STATUS_QUERY' ORDER BY id DESC LIMIT 1;")"
  report_row="$(mysql_exec "SELECT status, target_id, COALESCE(config_snapshot_id,''), COALESCE(result_id,''), COALESCE(history_id,''), COALESCE(export_id,'') FROM audit_log WHERE request_id = '${SUCCESS_REPORT_REQUEST_ID}' AND service_code = 'BENCHMARK_ENGINE' AND operation_type = 'BENCHMARK_REPORT_QUERY' ORDER BY id DESC LIMIT 1;")"
  raw_row="$(mysql_exec "SELECT status, target_id, COALESCE(config_snapshot_id,''), COALESCE(result_id,''), COALESCE(history_id,''), COALESCE(export_id,'') FROM audit_log WHERE request_id = '${SUCCESS_RAW_REQUEST_ID}' AND service_code = 'BENCHMARK_ENGINE' AND operation_type = 'BENCHMARK_REPORT_QUERY' ORDER BY id DESC LIMIT 1;")"
  echo "${submit_row}"
  echo "${status_row}"
  echo "${report_row}"
  echo "${raw_row}"
  if [[ "${submit_row}" != QUEUED$'\t'"${SUCCESS_TASK_ID}" ]]; then
    echo "Expected QUEUED submit audit row for ${SUCCESS_SUBMIT_REQUEST_ID}" >&2
    exit 1
  fi
  if [[ "${status_row}" != SUCCEEDED$'\t'"${SUCCESS_TASK_ID}" ]]; then
    echo "Expected SUCCEEDED status audit row for ${SUCCESS_STATUS_REQUEST_ID}" >&2
    exit 1
  fi
  if [[ "${report_row}" != SUCCESS$'\t'"${SUCCESS_REPORT_ID}"$'\t'"cfg-benchmark-${SUCCESS_REPORT_ID}"$'\t'"result-benchmark-${SUCCESS_REPORT_ID}"$'\t'"history-benchmark-${SUCCESS_REPORT_ID}"$'\t'"export-benchmark-${SUCCESS_REPORT_ID}-json-export" ]]; then
    echo "Expected SUCCESS report audit row with trace/export links for ${SUCCESS_REPORT_REQUEST_ID}" >&2
    exit 1
  fi
  if [[ "${raw_row}" != SUCCESS$'\t'"${SUCCESS_REPORT_ID}"$'\t'"cfg-benchmark-${SUCCESS_REPORT_ID}"$'\t'"result-benchmark-${SUCCESS_REPORT_ID}"$'\t'"history-benchmark-${SUCCESS_REPORT_ID}"$'\t'"export-benchmark-${SUCCESS_REPORT_ID}-raw-data" ]]; then
    echo "Expected SUCCESS raw-data audit row with trace/export links for ${SUCCESS_RAW_REQUEST_ID}" >&2
    exit 1
  fi

  print_step "Submitting benchmark failure task"
  failure_submit="$(assert_post_json "${BENCHMARK_ENGINE_API_BASE_URL}/api/benchmark-engine/tasks" \
    '{"tenantId":"tenant-a","taskType":"BASELINE","sqlText":"SELECT * FROM orders /*FAIL_BENCHMARK*/"}' \
    "${failure_submit_headers[@]}")"
  echo "${failure_submit}"
  json_assert "${failure_submit}" 'payload["status"] == "QUEUED"'
  FAILURE_TASK_ID="$(json_extract "${failure_submit}" 'payload["taskId"]')"

  print_step "Polling benchmark failure task to FAILED"
  failure_status="$(poll_terminal_status "${FAILURE_TASK_ID}" "FAILED" "${failure_status_headers[@]}")"
  echo "${failure_status}"
  json_assert "${failure_status}" 'payload["error"]["code"] == 14000'

  print_step "Verifying failed task persistence and missing report write-back"
  persisted_failed_task_row="$(mysql_exec "SELECT status FROM benchmark_task WHERE task_id = '${FAILURE_TASK_ID}' LIMIT 1;")"
  persisted_failed_report_count="$(mysql_exec "SELECT COUNT(*) FROM benchmark_task_report WHERE task_id = '${FAILURE_TASK_ID}';")"
  echo "${persisted_failed_task_row}"
  echo "${persisted_failed_report_count}"
  if [[ "${persisted_failed_task_row}" != "FAILED" ]]; then
    echo "Expected persisted FAILED benchmark_task row for ${FAILURE_TASK_ID}" >&2
    exit 1
  fi
  if (( persisted_failed_report_count != 0 )); then
    echo "Expected no benchmark_task_report row for failed task ${FAILURE_TASK_ID}" >&2
    exit 1
  fi

  print_step "Triggering compensated failed benchmark status query"
  compensation_status="$(assert_get_json "${BENCHMARK_ENGINE_API_BASE_URL}/api/benchmark-engine/tasks/${FAILURE_TASK_ID}" "${compensation_headers[@]}")"
  echo "${compensation_status}"
  json_assert "${compensation_status}" 'payload["status"] == "FAILED"'

  print_step "Verifying compensated failed audit row"
  failed_row="$(mysql_exec "SELECT status, target_id FROM audit_log WHERE request_id = '${COMPENSATION_REQUEST_ID}' AND service_code = 'BENCHMARK_ENGINE' AND operation_type = 'BENCHMARK_TASK_STATUS_QUERY' ORDER BY id DESC LIMIT 1;")"
  echo "${failed_row}"
  if [[ "${failed_row}" != FAILED$'\t'"${FAILURE_TASK_ID}" ]]; then
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
