#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

COMPOSE_BIN=()
RUN_COMPOSE_CHECK=true
RUN_RUNTIME_SMOKE=true
REUSE_RUNNING_STACK=false
KEEP_STACK=false
STACK_STARTED_BY_SCRIPT=false
SERVICE_NAMES=()
SERVICE_PIDS=()
SERVICE_LOGS=()

RUNTIME_SMOKE_LOG_DIR="${RUNTIME_SMOKE_LOG_DIR:-/tmp/sqlforge-runtime-smoke}"
DEV_CRYPTO_KEY_BASE64="${SQLFORGE_DEV_CRYPTO_KEY_BASE64:-MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=}"
SPRING_BOOT_PLUGIN_VERSION=""
GOVERNANCE_SMOKE_TRACE_PREFIX="${SQLFORGE_GOVERNANCE_SMOKE_FORCE_TRACE_PREFIX:-SMOKE-FORCE-AUDIT-FALLBACK}"

usage() {
  cat <<'EOF'
Usage: ./scripts/run-runtime-smoke.sh [options]

Options:
  --compose-check        Only run compose syntax validation.
  --runtime-smoke        Only run runtime smoke flow.
  --reuse-running-stack  Skip local-start/local-stop and reuse existing containers.
  --keep-stack           Do not stop containers after the script finishes.
EOF
}

parse_args() {
  local explicit_mode=false

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --compose-check)
        if [[ "${explicit_mode}" == "false" ]]; then
          RUN_COMPOSE_CHECK=false
          RUN_RUNTIME_SMOKE=false
          explicit_mode=true
        fi
        RUN_COMPOSE_CHECK=true
        shift
        ;;
      --runtime-smoke)
        if [[ "${explicit_mode}" == "false" ]]; then
          RUN_COMPOSE_CHECK=false
          RUN_RUNTIME_SMOKE=false
          explicit_mode=true
        fi
        RUN_RUNTIME_SMOKE=true
        shift
        ;;
      --reuse-running-stack)
        REUSE_RUNNING_STACK=true
        shift
        ;;
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

check_tcp() {
  local host="$1"
  local port="$2"
  (echo >/dev/tcp/"${host}"/"${port}") >/dev/null 2>&1
}

ensure_port_free() {
  local service_name="$1"
  local port="$2"

  if check_tcp 127.0.0.1 "${port}"; then
    echo "Port ${port} is already in use before starting ${service_name}. Stop the existing process or rerun in a clean environment." >&2
    exit 1
  fi
}

spring_boot_version() {
  if [[ -n "${SPRING_BOOT_PLUGIN_VERSION}" ]]; then
    printf '%s\n' "${SPRING_BOOT_PLUGIN_VERSION}"
    return
  fi

  SPRING_BOOT_PLUGIN_VERSION="$(sed -n 's:.*<spring.boot.version>\(.*\)</spring.boot.version>.*:\1:p' "${REPO_ROOT}/pom.xml" | head -n 1)"
  if [[ -z "${SPRING_BOOT_PLUGIN_VERSION}" ]]; then
    echo "Failed to resolve spring.boot.version from pom.xml" >&2
    exit 1
  fi
  printf '%s\n' "${SPRING_BOOT_PLUGIN_VERSION}"
}

register_service() {
  local service_name="$1"
  local pid="$2"
  local log_file="$3"
  SERVICE_NAMES+=("${service_name}")
  SERVICE_PIDS+=("${pid}")
  SERVICE_LOGS+=("${log_file}")
}

tail_failure_logs() {
  local index
  for ((index = 0; index < ${#SERVICE_NAMES[@]}; index++)); do
    if [[ -f "${SERVICE_LOGS[${index}]}" ]]; then
      print_step "Tail ${SERVICE_NAMES[${index}]} runtime log"
      tail -n 120 "${SERVICE_LOGS[${index}]}" >&2 || true
    fi
  done
}

cleanup() {
  local exit_code=$?
  local index

  trap - EXIT

  for ((index = ${#SERVICE_PIDS[@]} - 1; index >= 0; index--)); do
    if kill -0 "${SERVICE_PIDS[${index}]}" >/dev/null 2>&1; then
      print_step "Stopping ${SERVICE_NAMES[${index}]} runtime process ${SERVICE_PIDS[${index}]}"
      kill "${SERVICE_PIDS[${index}]}" >/dev/null 2>&1 || true
      wait "${SERVICE_PIDS[${index}]}" >/dev/null 2>&1 || true
    fi
  done

  if [[ "${STACK_STARTED_BY_SCRIPT}" == "true" && "${KEEP_STACK}" != "true" ]]; then
    print_step "Stopping local infrastructure"
    bash "${REPO_ROOT}/scripts/local-stop.sh" >/dev/null 2>&1 || true
  fi

  if [[ "${exit_code}" -ne 0 ]]; then
    tail_failure_logs
  fi

  exit "${exit_code}"
}

run_compose_check() {
  print_step "Validating compose syntax"
  compose config >/dev/null
}

start_stack() {
  if [[ "${REUSE_RUNNING_STACK}" == "true" ]]; then
    print_step "Reusing existing local infrastructure"
    return
  fi

  print_step "Starting local infrastructure"
  bash "${REPO_ROOT}/scripts/local-start.sh"
  STACK_STARTED_BY_SCRIPT=true
}

install_backend_runtime_dependencies() {
  print_step "Installing backend runtime dependencies"
  mvn -B -pl governance,query-execution,sql-optimization,benchmark-engine -am install -DskipTests >/dev/null
}

ensure_frontend_dependencies() {
  require_command npm

  if [[ -d "${REPO_ROOT}/node_modules" ]]; then
    return
  fi

  print_step "Installing frontend runtime dependencies"
  (
    cd "${REPO_ROOT}"
    npm install >/dev/null
  )
}

wait_for_http() {
  local service_name="$1"
  local url="$2"
  local pid="$3"
  local timeout_seconds="${4:-120}"
  local waited=0

  print_step "Waiting for ${service_name} health endpoint"
  until curl -fsS "${url}" >/dev/null 2>&1; do
    if ! kill -0 "${pid}" >/dev/null 2>&1; then
      echo "${service_name} process exited before ${url} became ready." >&2
      exit 1
    fi
    if (( waited >= timeout_seconds )); then
      echo "Timed out waiting for ${service_name} at ${url} after ${timeout_seconds}s." >&2
      exit 1
    fi
    sleep 2
    waited=$((waited + 2))
  done
}

start_governance() {
  local log_file="${RUNTIME_SMOKE_LOG_DIR}/governance.log"
  local pid=""

  ensure_port_free governance 8080

  print_step "Starting governance runtime"
  : > "${log_file}"
  (
    cd "${REPO_ROOT}"
    export SQLFORGE_DEV_CRYPTO_KEY_BASE64="${DEV_CRYPTO_KEY_BASE64}"
    export SQLFORGE_GOVERNANCE_SMOKE_FORCE_AUDIT_DELIVERY_FAILURE="${SQLFORGE_GOVERNANCE_SMOKE_FORCE_AUDIT_DELIVERY_FAILURE:-true}"
    export SQLFORGE_GOVERNANCE_SMOKE_FORCE_TRACE_PREFIX="${GOVERNANCE_SMOKE_TRACE_PREFIX}"
    exec nohup mvn -B -f governance/pom.xml \
      "org.springframework.boot:spring-boot-maven-plugin:$(spring_boot_version):run" \
      > "${log_file}" 2>&1
  ) &
  pid=$!
  register_service governance "${pid}" "${log_file}"
  wait_for_http governance "http://localhost:8080/api/governance/health" "${pid}" 120
}

start_query_execution() {
  local log_file="${RUNTIME_SMOKE_LOG_DIR}/query-execution.log"
  local pid=""

  ensure_port_free query-execution 8081

  print_step "Starting query-execution runtime"
  : > "${log_file}"
  (
    cd "${REPO_ROOT}"
    export SQLFORGE_SECURITY_CRYPTO_KEY_ID="${SQLFORGE_SECURITY_CRYPTO_KEY_ID:-query-execution-dev-key}"
    export SQLFORGE_SECURITY_CRYPTO_BASE64_KEY="${SQLFORGE_SECURITY_CRYPTO_BASE64_KEY:-${DEV_CRYPTO_KEY_BASE64}}"
    export SQLFORGE_SECURITY_CRYPTO_BASE64KEY="${SQLFORGE_SECURITY_CRYPTO_BASE64KEY:-${DEV_CRYPTO_KEY_BASE64}}"
    exec nohup mvn -B -f query-execution/pom.xml \
      "org.springframework.boot:spring-boot-maven-plugin:$(spring_boot_version):run" \
      > "${log_file}" 2>&1
  ) &
  pid=$!
  register_service query-execution "${pid}" "${log_file}"
  wait_for_http query-execution "http://localhost:8081/actuator/health" "${pid}" 120
}

start_sql_optimization() {
  local log_file="${RUNTIME_SMOKE_LOG_DIR}/sql-optimization.log"
  local pid=""

  ensure_port_free sql-optimization 8082

  print_step "Starting sql-optimization runtime"
  : > "${log_file}"
  (
    cd "${REPO_ROOT}"
    export SQLFORGE_SQL_OPTIMIZATION_DEV_CRYPTO_KEY_BASE64="${DEV_CRYPTO_KEY_BASE64}"
    exec nohup mvn -B -f sql-optimization/pom.xml \
      "org.springframework.boot:spring-boot-maven-plugin:$(spring_boot_version):run" \
      > "${log_file}" 2>&1
  ) &
  pid=$!
  register_service sql-optimization "${pid}" "${log_file}"
  wait_for_http sql-optimization "http://localhost:8082/actuator/health" "${pid}" 120
}

start_benchmark_engine() {
  local log_file="${RUNTIME_SMOKE_LOG_DIR}/benchmark-engine.log"
  local pid=""

  ensure_port_free benchmark-engine 8083

  print_step "Starting benchmark-engine runtime"
  : > "${log_file}"
  (
    cd "${REPO_ROOT}"
    export SQLFORGE_BENCHMARK_ENGINE_DEV_CRYPTO_KEY_BASE64="${DEV_CRYPTO_KEY_BASE64}"
    exec nohup mvn -B -f benchmark-engine/pom.xml \
      "org.springframework.boot:spring-boot-maven-plugin:$(spring_boot_version):run" \
      > "${log_file}" 2>&1
  ) &
  pid=$!
  register_service benchmark-engine "${pid}" "${log_file}"
  wait_for_http benchmark-engine "http://localhost:8083/actuator/health" "${pid}" 120
}

start_frontend() {
  local log_file="${RUNTIME_SMOKE_LOG_DIR}/frontend.log"
  local pid=""

  ensure_port_free frontend 3000
  ensure_frontend_dependencies

  print_step "Starting frontend runtime"
  : > "${log_file}"
  (
    cd "${REPO_ROOT}"
    exec nohup npm run dev > "${log_file}" 2>&1
  ) &
  pid=$!
  register_service frontend "${pid}" "${log_file}"
  wait_for_http frontend "http://localhost:3000" "${pid}" 90
}

run_runtime_smoke() {
  mkdir -p "${RUNTIME_SMOKE_LOG_DIR}"
  start_stack
  install_backend_runtime_dependencies
  start_governance
  start_query_execution
  start_sql_optimization
  start_benchmark_engine
  start_frontend

  print_step "Running health check gate"
  bash "${REPO_ROOT}/scripts/health-check.sh" --fail-on-error

  print_step "Running query-execution to governance business smoke"
  bash "${REPO_ROOT}/scripts/manual-query-governance-smoke.sh" --cleanup

  print_step "Running message queue smoke"
  bash "${REPO_ROOT}/scripts/manual-message-queue-smoke.sh" --cleanup
}

main() {
  parse_args "$@"
  setup_compose
  require_command curl
  trap cleanup EXIT

  cd "${REPO_ROOT}"

  if [[ "${RUN_COMPOSE_CHECK}" == "true" ]]; then
    run_compose_check
  fi

  if [[ "${RUN_RUNTIME_SMOKE}" == "true" ]]; then
    run_runtime_smoke
  fi

  print_step "Runtime smoke checks completed"
}

main "$@"
