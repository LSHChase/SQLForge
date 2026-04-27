#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

COMPOSE_BIN=()

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

setup_compose() {
  require_command docker
  if docker compose version >/dev/null 2>&1; then
    COMPOSE_BIN=(docker compose)
    return
  fi
  if command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_BIN=(docker-compose)
    return
  fi

  echo "Docker Compose is not installed. Install Docker Desktop or docker-compose first." >&2
  exit 1
}

compose() {
  "${COMPOSE_BIN[@]}" "$@"
}

is_port_in_use() {
  local port="$1"

  if command -v lsof >/dev/null 2>&1; then
    lsof -nP -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1
    return
  fi

  if command -v ss >/dev/null 2>&1; then
    ss -ltn "( sport = :${port} )" | grep -q ":${port}"
    return
  fi

  if command -v netstat >/dev/null 2>&1; then
    netstat -an 2>/dev/null | grep -E "[\\.:]${port}[[:space:]].*LISTEN" >/dev/null
    return
  fi

  (echo >/dev/tcp/127.0.0.1/"${port}") >/dev/null 2>&1
}

wait_for_port() {
  local host="$1"
  local port="$2"
  local timeout_seconds="$3"
  local waited=0

  while ! (echo >/dev/tcp/"${host}"/"${port}") >/dev/null 2>&1; do
    if (( waited >= timeout_seconds )); then
      echo "Timed out waiting for ${host}:${port} after ${timeout_seconds}s." >&2
      exit 1
    fi
    sleep 2
    waited=$((waited + 2))
  done
}

wait_for_mysql() {
  local waited=0

  while ! compose exec -T mysql mysqladmin ping -h 127.0.0.1 -uroot -psqlforge --silent >/dev/null 2>&1; do
    if (( waited >= 60 )); then
      echo "MySQL container is reachable on 3306 but did not become ready within 60s." >&2
      exit 1
    fi
    sleep 2
    waited=$((waited + 2))
  done
}

run_sql_file() {
  local sql_file="$1"
  echo "Applying ${sql_file}..."
  compose exec -T mysql mysql -usqlforge -psqlforge sqlforge < "${REPO_ROOT}/${sql_file}"
}

check_message_queue_table() {
  local table_count

  table_count="$(compose exec -T mysql mysql -N -B -usqlforge -psqlforge sqlforge -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='sqlforge' AND table_name='kafka_message_queue';" 2>/dev/null || echo "")"

  if [[ "${table_count}" == "1" ]]; then
    echo "Verified kafka_message_queue table exists for R-144 DATABASE mode."
  else
    echo "Warning: kafka_message_queue table does not exist. Check sql/init-schema.sql and R-144 setup." >&2
  fi
}

main() {
  cd "${REPO_ROOT}"
  setup_compose

  for port in 3306 6379 9000; do
    if is_port_in_use "${port}"; then
      echo "Port ${port} is already in use. Release it before starting the local environment." >&2
      exit 1
    fi
  done

  echo "Starting local infrastructure with Docker Compose..."
  compose up -d

  echo "Waiting for MySQL port 3306..."
  wait_for_port 127.0.0.1 3306 60
  wait_for_mysql

  run_sql_file "sql/init-schema.sql"
  run_sql_file "sql/init-data.sql"
  python3 "${REPO_ROOT}/scripts/ensure_execution_result_dev_schema.py"
  python3 "${REPO_ROOT}/scripts/ensure_query_history_dev_schema.py"
  check_message_queue_table

  cat <<'EOF'
Local services are ready:
- MySQL: mysql://sqlforge:sqlforge@localhost:3306/sqlforge
- Redis: redis://localhost:6379
- MinIO API: http://localhost:9000
- MinIO Console: http://localhost:9001
消息队列使用数据库模拟模式（R-144），无需Kafka。
EOF
}

main "$@"
