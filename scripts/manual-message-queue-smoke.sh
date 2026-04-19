#!/usr/bin/env bash

set -euo pipefail

API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-sqlforge-mysql}"
MYSQL_DATABASE="${MYSQL_DATABASE:-sqlforge}"
MYSQL_USER="${MYSQL_USER:-sqlforge}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-sqlforge}"

MESSAGE_ID=""
MESSAGE_TOPIC="manual.smoke"
PARTITION_KEY="tenant-manual"
MESSAGE_BODY_TEMPLATE='{"event":"manual-smoke","traceId":"%s"}'
MESSAGE_HEADERS_TEMPLATE='{"source":"manual-message-queue-smoke","traceId":"%s"}'
CLEANUP=false

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

usage() {
  cat <<'EOF'
Usage: ./scripts/manual-message-queue-smoke.sh [--cleanup]

Options:
  --cleanup   Delete the inserted smoke-test message after verification.
EOF
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

assert_http_ok() {
  local url="$1"
  local status_code

  status_code="$(curl -s -o /tmp/sqlforge-manual-smoke-response.out -w '%{http_code}' "${url}")"
  if [[ "${status_code}" != "200" ]]; then
    echo "HTTP request failed for ${url}, status=${status_code}" >&2
    [[ -f /tmp/sqlforge-manual-smoke-response.out ]] && cat /tmp/sqlforge-manual-smoke-response.out >&2
    exit 1
  fi
  cat /tmp/sqlforge-manual-smoke-response.out
}

assert_post_ok() {
  local url="$1"
  local status_code

  status_code="$(curl -s -o /tmp/sqlforge-manual-smoke-response.out -w '%{http_code}' -X POST "${url}")"
  if [[ "${status_code}" != "200" ]]; then
    echo "HTTP POST failed for ${url}, status=${status_code}" >&2
    [[ -f /tmp/sqlforge-manual-smoke-response.out ]] && cat /tmp/sqlforge-manual-smoke-response.out >&2
    exit 1
  fi
  cat /tmp/sqlforge-manual-smoke-response.out
}

cleanup_message() {
  if [[ -z "${MESSAGE_ID}" ]]; then
    return
  fi

  print_step "Cleaning up smoke-test message ${MESSAGE_ID}"
  mysql_exec "DELETE FROM kafka_message_queue WHERE id = ${MESSAGE_ID};"
  echo "Deleted kafka_message_queue.id=${MESSAGE_ID}"
}

main() {
  local trace_id message_body message_headers stats_before stats_after retry_response row_after

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

  require_command docker
  require_command curl

  trace_id="manual-smoke-$(date +%Y%m%d%H%M%S)"
  printf -v message_body "${MESSAGE_BODY_TEMPLATE}" "${trace_id}"
  printf -v message_headers "${MESSAGE_HEADERS_TEMPLATE}" "${trace_id}"

  print_step "Checking governance health"
  assert_http_ok "${API_BASE_URL}/api/governance/health"

  print_step "Reading queue stats before insert"
  stats_before="$(assert_http_ok "${API_BASE_URL}/admin/messages/stats")"
  echo "${stats_before}"

  print_step "Inserting a FAILED smoke-test message into kafka_message_queue"
  MESSAGE_ID="$(mysql_exec "INSERT INTO kafka_message_queue (topic, partition_key, message_body, headers, status, retry_count, error_log) VALUES ('${MESSAGE_TOPIC}', '${PARTITION_KEY}', '${message_body}', '${message_headers}', 'FAILED', 3, 'manual smoke setup'); SELECT LAST_INSERT_ID();")"
  MESSAGE_ID="$(echo "${MESSAGE_ID}" | tail -n 1 | tr -d '\r')"
  echo "Inserted kafka_message_queue.id=${MESSAGE_ID}"

  print_step "Verifying inserted row"
  mysql_exec "SELECT id, topic, status, retry_count, error_log FROM kafka_message_queue WHERE id = ${MESSAGE_ID};"

  print_step "Calling retry endpoint"
  retry_response="$(assert_post_ok "${API_BASE_URL}/admin/messages/retry")"
  echo "${retry_response}"

  print_step "Validating row after retry"
  row_after="$(mysql_exec "SELECT id, topic, status, retry_count, IFNULL(error_log,'NULL') FROM kafka_message_queue WHERE id = ${MESSAGE_ID};")"
  echo "${row_after}"
  if [[ "${row_after}" != *$'\tPENDING\t0\tNULL' ]]; then
    echo "Smoke test failed: message ${MESSAGE_ID} was not reset to PENDING/0/NULL" >&2
    exit 1
  fi

  print_step "Reading queue stats after retry"
  stats_after="$(assert_http_ok "${API_BASE_URL}/admin/messages/stats")"
  echo "${stats_after}"

  if [[ "${CLEANUP}" == "true" ]]; then
    cleanup_message
  else
    echo "Leaving smoke-test message in kafka_message_queue.id=${MESSAGE_ID}. Re-run with --cleanup to delete it."
  fi

  print_step "Manual message queue smoke test completed"
}

main "$@"
