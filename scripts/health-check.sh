#!/usr/bin/env bash

set -euo pipefail

FAIL_ON_ERROR=false
CHECK_FRONTEND=true
FAILURES=0

usage() {
  cat <<'EOF'
Usage: ./scripts/health-check.sh [--fail-on-error] [--skip-frontend]

Options:
  --fail-on-error  Return non-zero when any mandatory check fails.
  --skip-frontend  Do not probe the frontend at http://localhost:3000.
EOF
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --fail-on-error)
        FAIL_ON_ERROR=true
        shift
        ;;
      --skip-frontend)
        CHECK_FRONTEND=false
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
}

check_tcp() {
  local host="$1"
  local port="$2"
  (echo >/dev/tcp/"${host}"/"${port}") >/dev/null 2>&1
}

print_status() {
  local service="$1"
  local status="$2"
  local detail="$3"
  printf '%-20s %-8s %s\n' "${service}" "${status}" "${detail}"
}

record_failure() {
  FAILURES=$((FAILURES + 1))
}

print_result() {
  local service="$1"
  local ok="$2"
  local detail="$3"

  if [[ "${ok}" == "true" ]]; then
    print_status "${service}" "OK" "${detail}"
    return
  fi

  print_status "${service}" "FAIL" "${detail}"
  record_failure
}

check_http_ok() {
  local url="$1"
  local expected="${2:-}"
  local body=""

  if ! command -v curl >/dev/null 2>&1; then
    return 1
  fi

  if [[ -z "${expected}" ]]; then
    curl -fsS -o /dev/null "${url}" >/dev/null 2>&1
    return $?
  fi

  body="$(curl -fsS "${url}" 2>/dev/null || true)"
  if [[ -z "${body}" ]]; then
    return 1
  fi

  grep -q "${expected}" <<<"${body}"
}

main() {
  local pending_count=""
  parse_args "$@"

  echo "Service              Status   Detail"
  echo "---------------------------------------------"

  if check_tcp 127.0.0.1 3306; then
    print_status "MySQL" "OK" "tcp://localhost:3306"
  else
    print_status "MySQL" "FAIL" "tcp://localhost:3306"
    record_failure
  fi

  if docker exec sqlforge-redis redis-cli ping 2>/dev/null | grep -q '^PONG$'; then
    print_status "Redis" "OK" "redis-cli ping"
  elif check_tcp 127.0.0.1 6379; then
    print_status "Redis" "OK" "tcp://localhost:6379"
  else
    print_status "Redis" "FAIL" "tcp://localhost:6379"
    record_failure
  fi

  pending_count="$(docker exec sqlforge-mysql mysql --init-command=SET\ time_zone=\'+08:00\' -N -B -usqlforge -psqlforge sqlforge -e "SELECT COUNT(*) FROM kafka_message_queue WHERE status='PENDING';" 2>/dev/null || true)"
  if [[ "${pending_count}" =~ ^[0-9]+$ ]]; then
    print_status "MessageQueue" "OK" "消息队列（数据库模拟）：${pending_count}条待处理"
  else
    print_status "MessageQueue" "FAIL" "消息队列（数据库模拟）：kafka_message_queue unavailable"
    record_failure
  fi

  if check_http_ok "http://localhost:9000/minio/health/live"; then
    print_status "MinIO" "OK" "http://localhost:9000/minio/health/live"
  else
    print_status "MinIO" "FAIL" "http://localhost:9000/minio/health/live"
    record_failure
  fi

  if check_http_ok "http://localhost:8080/api/governance/health" '"status":"UP"'; then
    print_status "governance" "OK" "http://localhost:8080/api/governance/health"
  else
    print_status "governance" "FAIL" "http://localhost:8080/api/governance/health"
    record_failure
  fi

  if check_http_ok "http://localhost:8081/actuator/health" '"status":"UP"'; then
    print_status "query-execution" "OK" "http://localhost:8081/actuator/health"
  else
    print_status "query-execution" "FAIL" "http://localhost:8081/actuator/health"
    record_failure
  fi

  if check_http_ok "http://localhost:8082/actuator/health" '"status":"UP"'; then
    print_status "sql-optimization" "OK" "http://localhost:8082/actuator/health"
  else
    print_status "sql-optimization" "FAIL" "http://localhost:8082/actuator/health"
    record_failure
  fi

  if check_http_ok "http://localhost:8083/actuator/health" '"status":"UP"'; then
    print_status "benchmark-engine" "OK" "http://localhost:8083/actuator/health"
  else
    print_status "benchmark-engine" "FAIL" "http://localhost:8083/actuator/health"
    record_failure
  fi

  if [[ "${CHECK_FRONTEND}" != "true" ]]; then
    print_status "frontend" "SKIP" "frontend probe disabled"
  elif check_http_ok "http://localhost:3000"; then
    print_status "frontend" "OK" "http://localhost:3000"
  else
    print_status "frontend" "FAIL" "http://localhost:3000"
    record_failure
  fi

  if [[ "${FAIL_ON_ERROR}" == "true" && "${FAILURES}" -gt 0 ]]; then
    echo "Health check failed: ${FAILURES} mandatory probe(s) are not healthy." >&2
    exit 1
  fi
}

main "$@"
