#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_BIN=()
KEEP_STACK=false

usage() {
  cat <<'EOF'
Usage: ./scripts/verify-db-scripts.sh [--keep-stack]

Verifies database script executability by:
1. applying init-schema.sql + init-data.sql to a clean MySQL database
2. recreating MySQL and applying the migration chain to a legacy bootstrap baseline
EOF
}

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
  echo "Docker Compose is not installed." >&2
  exit 1
}

compose() {
  "${COMPOSE_BIN[@]}" "$@"
}

print_step() {
  printf '\n[%s] %s\n' "$(date '+%H:%M:%S')" "$1"
}

wait_for_mysql() {
  local waited=0

  until compose exec -T mysql mysqladmin ping -h 127.0.0.1 -uroot -psqlforge --silent >/dev/null 2>&1; do
    if (( waited >= 60 )); then
      echo "MySQL container did not become ready within 60s." >&2
      exit 1
    fi
    sleep 2
    waited=$((waited + 2))
  done
}

recreate_mysql() {
  print_step "Recreating clean MySQL instance for DB script gate"
  compose down -v >/dev/null 2>&1 || true
  compose up -d mysql >/dev/null
  wait_for_mysql
}

mysql_apply_file() {
  local sql_file="$1"
  compose exec -T mysql mysql -usqlforge -psqlforge sqlforge < "${REPO_ROOT}/${sql_file}"
}

mysql_exec() {
  local sql="$1"
  compose exec -T mysql mysql -N -B -usqlforge -psqlforge sqlforge -e "$sql"
}

apply_legacy_bootstrap() {
  compose exec -T mysql mysql -usqlforge -psqlforge sqlforge <<'SQL'
CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tenant_id VARCHAR(64) NOT NULL,
  operator_id VARCHAR(64) DEFAULT NULL,
  operation_type VARCHAR(64) NOT NULL,
  target_type VARCHAR(64) NOT NULL,
  target_id VARCHAR(128) NOT NULL,
  request_params TEXT DEFAULT NULL,
  response_summary TEXT DEFAULT NULL,
  status VARCHAR(16) NOT NULL,
  cost_ms BIGINT DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS system_config (
  id BIGINT NOT NULL AUTO_INCREMENT,
  config_key VARCHAR(128) NOT NULL,
  config_value VARCHAR(512) DEFAULT NULL,
  description VARCHAR(255) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_system_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SQL
}

verify_init_scripts() {
  print_step "Applying init schema and init data to clean database"
  mysql_apply_file "sql/init-schema.sql"
  mysql_apply_file "sql/init-data.sql"
  mysql_exec "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='sqlforge' AND table_name='audit_log';" | grep -Fx '1' >/dev/null
  mysql_exec "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='sqlforge' AND table_name='governance_history_lookup_index';" | grep -Fx '1' >/dev/null
  mysql_exec "SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema='sqlforge' AND trigger_name='trg_audit_log_lookup_index_ai';" | grep -Fx '1' >/dev/null
}

verify_migrations() {
  local migration

  print_step "Applying legacy bootstrap before migration chain"
  apply_legacy_bootstrap

  for migration in "${REPO_ROOT}"/sql/migrations/*.sql; do
    print_step "Applying migration $(basename "${migration}")"
    compose exec -T mysql mysql -usqlforge -psqlforge sqlforge < "${migration}"
  done

  mysql_exec "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='sqlforge' AND table_name='optimization_task';" | grep -Fx '1' >/dev/null
  mysql_exec "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='sqlforge' AND table_name='benchmark_task_report';" | grep -Fx '1' >/dev/null
  mysql_exec "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='sqlforge' AND table_name='governance_history_lookup_index';" | grep -Fx '1' >/dev/null
}

cleanup() {
  local exit_code=$?
  trap - EXIT

  if [[ "${KEEP_STACK}" != "true" ]]; then
    compose down -v >/dev/null 2>&1 || true
  fi

  exit "${exit_code}"
}

main() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --keep-stack)
        KEEP_STACK=true
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

  cd "${REPO_ROOT}"
  setup_compose
  trap cleanup EXIT

  recreate_mysql
  verify_init_scripts

  recreate_mysql
  verify_migrations

  print_step "Database script executability verification passed"
}

main "$@"
