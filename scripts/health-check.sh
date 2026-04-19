#!/usr/bin/env bash

set -euo pipefail

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

main() {
  local pending_count=""

  echo "Service              Status   Detail"
  echo "---------------------------------------------"

  if check_tcp 127.0.0.1 3306; then
    print_status "MySQL" "OK" "tcp://localhost:3306"
  else
    print_status "MySQL" "FAIL" "tcp://localhost:3306"
  fi

  if docker exec sqlforge-redis redis-cli ping 2>/dev/null | grep -q '^PONG$'; then
    print_status "Redis" "OK" "redis-cli ping"
  elif check_tcp 127.0.0.1 6379; then
    print_status "Redis" "OK" "tcp://localhost:6379"
  else
    print_status "Redis" "FAIL" "tcp://localhost:6379"
  fi

  pending_count="$(docker exec sqlforge-mysql mysql -N -B -uroot -psqlforge sqlforge -e "SELECT COUNT(*) FROM kafka_message_queue WHERE status='PENDING';" 2>/dev/null || true)"
  if [[ "${pending_count}" =~ ^[0-9]+$ ]]; then
    print_status "MessageQueue" "OK" "消息队列（数据库模拟）：${pending_count}条待处理"
  else
    print_status "MessageQueue" "FAIL" "消息队列（数据库模拟）：kafka_message_queue unavailable"
  fi

  if command -v curl >/dev/null 2>&1 && curl -fsS http://localhost:9000/minio/health/live >/dev/null 2>&1; then
    print_status "MinIO" "OK" "http://localhost:9000/minio/health/live"
  else
    print_status "MinIO" "FAIL" "http://localhost:9000/minio/health/live"
  fi

  if command -v curl >/dev/null 2>&1 && curl -fsS http://localhost:8080/api/governance/health | grep -q '"status":"UP"'; then
    print_status "governance-service" "OK" "http://localhost:8080/api/governance/health"
  else
    print_status "governance-service" "FAIL" "http://localhost:8080/api/governance/health"
  fi

  if command -v curl >/dev/null 2>&1 && curl -fsS http://localhost:3000 >/dev/null 2>&1; then
    print_status "frontend" "OK" "http://localhost:3000"
  else
    print_status "frontend" "SKIP" "http://localhost:3000 (not running)"
  fi
}

main "$@"
