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
GOVERNANCE_PID=""
GOVERNANCE_LOG="${RUNTIME_SMOKE_GOVERNANCE_LOG:-/tmp/sqlforge-governance-runtime.log}"
DEV_CRYPTO_KEY_BASE64="${SQLFORGE_DEV_CRYPTO_KEY_BASE64:-MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=}"

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

spring_boot_version() {
  sed -n 's:.*<spring.boot.version>\(.*\)</spring.boot.version>.*:\1:p' "${REPO_ROOT}/pom.xml" | head -n 1
}

cleanup() {
  local exit_code=$?
  trap - EXIT

  if [[ -n "${GOVERNANCE_PID}" ]] && kill -0 "${GOVERNANCE_PID}" >/dev/null 2>&1; then
    print_step "Stopping governance runtime process ${GOVERNANCE_PID}"
    kill "${GOVERNANCE_PID}" >/dev/null 2>&1 || true
    wait "${GOVERNANCE_PID}" >/dev/null 2>&1 || true
  fi

  if [[ "${STACK_STARTED_BY_SCRIPT}" == "true" && "${KEEP_STACK}" != "true" ]]; then
    print_step "Stopping local infrastructure"
    bash "${REPO_ROOT}/scripts/local-stop.sh" >/dev/null 2>&1 || true
  fi

  if [[ "${exit_code}" -ne 0 && -f "${GOVERNANCE_LOG}" ]]; then
    print_step "Tail governance runtime log"
    tail -n 200 "${GOVERNANCE_LOG}" >&2 || true
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

start_governance() {
  local spring_boot_plugin_version
  spring_boot_plugin_version="$(spring_boot_version)"
  if [[ -z "${spring_boot_plugin_version}" ]]; then
    echo "Failed to resolve spring.boot.version from pom.xml" >&2
    exit 1
  fi

  print_step "Installing governance runtime dependencies"
  mvn -B -pl governance -am install -DskipTests >/dev/null

  print_step "Starting governance runtime"
  : > "${GOVERNANCE_LOG}"
  (
    cd "${REPO_ROOT}"
    export SQLFORGE_DEV_CRYPTO_KEY_BASE64="${DEV_CRYPTO_KEY_BASE64}"
    exec nohup mvn -B -f governance/pom.xml \
      "org.springframework.boot:spring-boot-maven-plugin:${spring_boot_plugin_version}:run" \
      > "${GOVERNANCE_LOG}" 2>&1
  ) &
  GOVERNANCE_PID=$!
}

wait_for_governance() {
  local waited=0

  print_step "Waiting for governance health endpoint"
  until curl -fsS http://localhost:8080/api/governance/health >/dev/null 2>&1; do
    if [[ -n "${GOVERNANCE_PID}" ]] && ! kill -0 "${GOVERNANCE_PID}" >/dev/null 2>&1; then
      echo "Governance process exited before health endpoint became ready." >&2
      exit 1
    fi
    if (( waited >= 120 )); then
      echo "Timed out waiting for governance health endpoint after 120s." >&2
      exit 1
    fi
    sleep 2
    waited=$((waited + 2))
  done
}

run_runtime_smoke() {
  start_stack
  start_governance
  wait_for_governance

  print_step "Running health check gate"
  bash "${REPO_ROOT}/scripts/health-check.sh" --skip-frontend --fail-on-error

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
