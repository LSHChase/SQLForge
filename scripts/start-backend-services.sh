#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

REUSE_RUNNING_STACK=false
SKIP_BUILD=false
WAIT_TIMEOUT_SECONDS=120
BACKEND_RUNTIME_LOG_DIR="${BACKEND_RUNTIME_LOG_DIR:-/tmp/sqlforge-backend-runtime}"
DEV_CRYPTO_KEY_BASE64="${SQLFORGE_DEV_CRYPTO_KEY_BASE64:-MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=}"
SPRING_BOOT_PLUGIN_VERSION=""
STACK_STARTED_BY_SCRIPT=false
STARTED_SERVICE_PIDS=()
STARTED_SERVICE_NAMES=()

usage() {
  cat <<'EOF'
Usage: ./scripts/start-backend-services.sh [options]

Options:
  --reuse-running-stack  Skip scripts/local-start.sh and reuse the current MySQL/Redis/MinIO stack.
  --skip-build           Skip the Maven install step before starting backend services.
  --log-dir DIR          Override the runtime log and pid directory. Default: /tmp/sqlforge-backend-runtime
  --wait-timeout SEC     Seconds to wait for each health endpoint. Default: 120
EOF
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --reuse-running-stack)
        REUSE_RUNNING_STACK=true
        shift
        ;;
      --skip-build)
        SKIP_BUILD=true
        shift
        ;;
      --log-dir)
        [[ $# -lt 2 ]] && {
          echo "--log-dir requires a value." >&2
          exit 1
        }
        BACKEND_RUNTIME_LOG_DIR="$2"
        shift 2
        ;;
      --wait-timeout)
        [[ $# -lt 2 ]] && {
          echo "--wait-timeout requires a value." >&2
          exit 1
        }
        WAIT_TIMEOUT_SECONDS="$2"
        shift 2
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

require_background_launcher() {
  if command -v setsid >/dev/null 2>&1; then
    return
  fi
  echo "Missing required command: setsid" >&2
  exit 1
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
    echo "Port ${port} is already in use before starting ${service_name}. Stop the existing process or choose a clean environment." >&2
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

prepare_runtime_dir() {
  mkdir -p "${BACKEND_RUNTIME_LOG_DIR}"
}

pid_file_path() {
  local service_name="$1"
  printf '%s/%s.pid\n' "${BACKEND_RUNTIME_LOG_DIR}" "${service_name}"
}

log_file_path() {
  local service_name="$1"
  printf '%s/%s.log\n' "${BACKEND_RUNTIME_LOG_DIR}" "${service_name}"
}

ensure_pid_file_available() {
  local service_name="$1"
  local pid_file
  local existing_pid

  pid_file="$(pid_file_path "${service_name}")"
  if [[ ! -f "${pid_file}" ]]; then
    return
  fi

  existing_pid="$(<"${pid_file}")"
  if [[ -n "${existing_pid}" ]] && kill -0 "${existing_pid}" >/dev/null 2>&1; then
    echo "${service_name} already appears to be running with pid ${existing_pid}. Remove ${pid_file} after stopping it, or use a different --log-dir." >&2
    exit 1
  fi

  rm -f "${pid_file}"
}

register_service() {
  local service_name="$1"
  local pid="$2"

  STARTED_SERVICE_NAMES+=("${service_name}")
  STARTED_SERVICE_PIDS+=("${pid}")
  printf '%s\n' "${pid}" > "$(pid_file_path "${service_name}")"
}

tail_failure_logs() {
  local index
  for ((index = 0; index < ${#STARTED_SERVICE_NAMES[@]}; index++)); do
    local service_name="${STARTED_SERVICE_NAMES[${index}]}"
    local log_file
    log_file="$(log_file_path "${service_name}")"
    if [[ -f "${log_file}" ]]; then
      print_step "Tail ${service_name} runtime log"
      tail -n 80 "${log_file}" >&2 || true
    fi
  done
}

cleanup_on_failure() {
  local exit_code=$?
  local index

  trap - EXIT

  if [[ "${exit_code}" -eq 0 ]]; then
    exit 0
  fi

  for ((index = ${#STARTED_SERVICE_PIDS[@]} - 1; index >= 0; index--)); do
    local pid="${STARTED_SERVICE_PIDS[${index}]}"
    local service_name="${STARTED_SERVICE_NAMES[${index}]}"
    if kill -0 "${pid}" >/dev/null 2>&1; then
      print_step "Stopping ${service_name} runtime process ${pid}"
      kill "${pid}" >/dev/null 2>&1 || true
      wait "${pid}" >/dev/null 2>&1 || true
    fi
    rm -f "$(pid_file_path "${service_name}")"
  done

  if [[ "${STACK_STARTED_BY_SCRIPT}" == "true" ]]; then
    print_step "Stopping local infrastructure after startup failure"
    bash "${REPO_ROOT}/scripts/local-stop.sh" >/dev/null 2>&1 || true
  fi

  tail_failure_logs
  exit "${exit_code}"
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
  if [[ "${SKIP_BUILD}" == "true" ]]; then
    print_step "Skipping backend dependency install"
    return
  fi

  print_step "Installing backend runtime dependencies"
  mvn -B -pl governance,query-execution,sql-optimization,benchmark-engine -am install -DskipTests >/dev/null
}

wait_for_http() {
  local service_name="$1"
  local url="$2"
  local pid="$3"
  local waited=0

  print_step "Waiting for ${service_name} health endpoint"
  until curl -fsS "${url}" >/dev/null 2>&1; do
    if ! kill -0 "${pid}" >/dev/null 2>&1; then
      echo "${service_name} process exited before ${url} became ready." >&2
      exit 1
    fi
    if (( waited >= WAIT_TIMEOUT_SECONDS )); then
      echo "Timed out waiting for ${service_name} at ${url} after ${WAIT_TIMEOUT_SECONDS}s." >&2
      exit 1
    fi
    sleep 2
    waited=$((waited + 2))
  done
}

launch_service() {
  local service_name="$1"
  local health_url="$2"
  local startup_command="$3"
  local port="$4"
  local log_file
  local pid=""

  ensure_port_free "${service_name}" "${port}"
  ensure_pid_file_available "${service_name}"
  log_file="$(log_file_path "${service_name}")"

  print_step "Starting ${service_name}"
  : > "${log_file}"
  setsid bash -lc "${startup_command}" </dev/null > "${log_file}" 2>&1 &
  pid=$!
  register_service "${service_name}" "${pid}"
  wait_for_http "${service_name}" "${health_url}" "${pid}"
}

start_governance() {
  launch_service \
    "governance" \
    "http://localhost:8080/api/governance/health" \
    "cd ${REPO_ROOT} && export SQLFORGE_DEV_CRYPTO_KEY_BASE64=${DEV_CRYPTO_KEY_BASE64} && export SQLFORGE_GOVERNANCE_SMOKE_FORCE_AUDIT_DELIVERY_FAILURE=\${SQLFORGE_GOVERNANCE_SMOKE_FORCE_AUDIT_DELIVERY_FAILURE:-true} && export SQLFORGE_GOVERNANCE_SMOKE_FORCE_TRACE_PREFIX=\${SQLFORGE_GOVERNANCE_SMOKE_FORCE_TRACE_PREFIX:-SMOKE-FORCE-AUDIT-FALLBACK} && exec mvn -B -f governance/pom.xml org.springframework.boot:spring-boot-maven-plugin:$(spring_boot_version):run" \
    8080
}

start_query_execution() {
  launch_service \
    "query-execution" \
    "http://localhost:8081/actuator/health" \
    "cd ${REPO_ROOT} && export SQLFORGE_SECURITY_CRYPTO_KEY_ID=\${SQLFORGE_SECURITY_CRYPTO_KEY_ID:-query-execution-dev-key} && export SQLFORGE_SECURITY_CRYPTO_BASE64_KEY=\${SQLFORGE_SECURITY_CRYPTO_BASE64_KEY:-${DEV_CRYPTO_KEY_BASE64}} && export SQLFORGE_SECURITY_CRYPTO_BASE64KEY=\${SQLFORGE_SECURITY_CRYPTO_BASE64KEY:-${DEV_CRYPTO_KEY_BASE64}} && exec mvn -B -f query-execution/pom.xml org.springframework.boot:spring-boot-maven-plugin:$(spring_boot_version):run" \
    8081
}

start_sql_optimization() {
  launch_service \
    "sql-optimization" \
    "http://localhost:8082/actuator/health" \
    "cd ${REPO_ROOT} && export SQLFORGE_SQL_OPTIMIZATION_DEV_CRYPTO_KEY_BASE64=${DEV_CRYPTO_KEY_BASE64} && exec mvn -B -f sql-optimization/pom.xml org.springframework.boot:spring-boot-maven-plugin:$(spring_boot_version):run" \
    8082
}

start_benchmark_engine() {
  launch_service \
    "benchmark-engine" \
    "http://localhost:8083/actuator/health" \
    "cd ${REPO_ROOT} && export SQLFORGE_BENCHMARK_ENGINE_DEV_CRYPTO_KEY_BASE64=${DEV_CRYPTO_KEY_BASE64} && exec mvn -B -f benchmark-engine/pom.xml org.springframework.boot:spring-boot-maven-plugin:$(spring_boot_version):run" \
    8083
}

print_summary() {
  cat <<EOF

Backend runtime is ready:
- governance:        http://localhost:8080/api/governance/health
- query-execution:   http://localhost:8081/actuator/health
- sql-optimization:  http://localhost:8082/actuator/health
- benchmark-engine:  http://localhost:8083/actuator/health
- logs and pid files: ${BACKEND_RUNTIME_LOG_DIR}

Stop the backend services with:
  kill \$(cat ${BACKEND_RUNTIME_LOG_DIR}/*.pid)
  rm -f ${BACKEND_RUNTIME_LOG_DIR}/*.pid

If you also started local infrastructure here, stop it separately with:
  bash scripts/local-stop.sh
EOF
}

main() {
  parse_args "$@"
  require_command bash
  require_command curl
  require_command mvn
  require_background_launcher
  prepare_runtime_dir
  trap cleanup_on_failure EXIT

  cd "${REPO_ROOT}"
  start_stack
  install_backend_runtime_dependencies
  start_governance
  start_query_execution
  start_sql_optimization
  start_benchmark_engine
  trap - EXIT
  print_summary
}

main "$@"
