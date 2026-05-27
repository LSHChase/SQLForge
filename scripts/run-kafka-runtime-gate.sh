#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

COMPOSE_BIN=()
RUN_CONFIG_CHECK=true
RUN_RUNTIME_SMOKE=true
REUSE_RUNNING_STACK=false
KEEP_STACK=false
STACK_STARTED_BY_SCRIPT=false
GOVERNANCE_PID=""
GOVERNANCE_LOG_DIR="${KAFKA_RUNTIME_GATE_LOG_DIR:-/tmp/sqlforge-kafka-runtime-gate}"
GOVERNANCE_LOG_FILE="${GOVERNANCE_LOG_DIR}/governance-kafka.log"
DEV_CRYPTO_KEY_BASE64="${SQLFORGE_PROD_CRYPTO_KEY_BASE64:-MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-sqlforge-mysql}"
MYSQL_DATABASE="${MYSQL_DATABASE:-sqlforge}"
MYSQL_USER="${MYSQL_USER:-sqlforge}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-sqlforge}"
KAFKA_CONTAINER="${KAFKA_CONTAINER:-sqlforge-kafka}"
KAFKA_BOOTSTRAP="${SQLFORGE_PROD_KAFKA_BOOTSTRAP:-localhost:9092}"
KAFKA_SECURITY_PROTOCOL="${SQLFORGE_PROD_KAFKA_SECURITY_PROTOCOL:-PLAINTEXT}"
KAFKA_SASL_MECHANISM="${SQLFORGE_PROD_KAFKA_SASL_MECHANISM:-}"
KAFKA_SASL_JAAS_CONFIG="${SQLFORGE_PROD_KAFKA_SASL_JAAS_CONFIG:-}"
KAFKA_SSL_TRUSTSTORE_LOCATION="${SQLFORGE_PROD_KAFKA_SSL_TRUSTSTORE_LOCATION:-}"
KAFKA_SSL_TRUSTSTORE_PASSWORD="${SQLFORGE_PROD_KAFKA_SSL_TRUSTSTORE_PASSWORD:-}"
SPRING_BOOT_PLUGIN_VERSION=""

usage() {
  cat <<'EOF'
Usage: ./scripts/run-kafka-runtime-gate.sh [options]

Options:
  --config-check        Only run Kafka bootstrap/security parameter validation.
  --runtime-smoke       Only run real Kafka runtime smoke.
  --reuse-running-stack Reuse an already running local stack.
  --keep-stack          Leave local infrastructure running after the script exits.
EOF
}

parse_args() {
  local explicit_mode=false

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --config-check)
        if [[ "${explicit_mode}" == "false" ]]; then
          RUN_CONFIG_CHECK=false
          RUN_RUNTIME_SMOKE=false
          explicit_mode=true
        fi
        RUN_CONFIG_CHECK=true
        shift
        ;;
      --runtime-smoke)
        if [[ "${explicit_mode}" == "false" ]]; then
          RUN_CONFIG_CHECK=false
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

check_tcp() {
  local host="$1"
  local port="$2"
  (echo >/dev/tcp/"${host}"/"${port}") >/dev/null 2>&1
}

ensure_port_free() {
  local service_name="$1"
  local port="$2"

  if check_tcp 127.0.0.1 "${port}"; then
    echo "Port ${port} is already in use before starting ${service_name}." >&2
    exit 1
  fi
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

wait_for_kafka() {
  local waited=0

  print_step "Waiting for Kafka connectivity"
  until docker exec "${KAFKA_CONTAINER}" sh -lc \
    "kafka-topics --bootstrap-server localhost:9092 --list >/dev/null 2>&1"; do
    if (( waited >= 120 )); then
      echo "Timed out waiting for Kafka container ${KAFKA_CONTAINER}." >&2
      exit 1
    fi
    sleep 3
    waited=$((waited + 3))
  done
}

wait_for_log_text() {
  local expected_text="$1"
  local timeout_seconds="${2:-40}"
  local waited=0

  until [[ -f "${GOVERNANCE_LOG_FILE}" ]] && grep -F "${expected_text}" "${GOVERNANCE_LOG_FILE}" >/dev/null 2>&1; do
    if (( waited >= timeout_seconds )); then
      echo "Timed out waiting for governance log to contain: ${expected_text}" >&2
      exit 1
    fi
    sleep 2
    waited=$((waited + 2))
  done
}

mysql_exec() {
  local sql="$1"
  docker exec "${MYSQL_CONTAINER}" sh -lc \
    "mysql --init-command=SET\ time_zone=\'+08:00\' -N -B -u${MYSQL_USER} -p${MYSQL_PASSWORD} ${MYSQL_DATABASE} -e \"$sql\""
}

run_sql_file() {
  local sql_file="$1"
  compose exec -T mysql mysql --init-command=SET\ time_zone=\'+08:00\' -usqlforge -psqlforge sqlforge < "${REPO_ROOT}/${sql_file}"
}

cleanup() {
  local exit_code=$?
  trap - EXIT

  if [[ -n "${GOVERNANCE_PID}" ]] && kill -0 "${GOVERNANCE_PID}" >/dev/null 2>&1; then
    print_step "Stopping governance Kafka runtime process ${GOVERNANCE_PID}"
    kill "${GOVERNANCE_PID}" >/dev/null 2>&1 || true
    wait "${GOVERNANCE_PID}" >/dev/null 2>&1 || true
  fi

  if [[ "${STACK_STARTED_BY_SCRIPT}" == "true" && "${KEEP_STACK}" != "true" ]]; then
    print_step "Stopping local infrastructure"
    bash "${REPO_ROOT}/scripts/local-stop.sh" >/dev/null 2>&1 || true
  fi

  if [[ "${exit_code}" -ne 0 && -f "${GOVERNANCE_LOG_FILE}" ]]; then
    print_step "Tail governance Kafka runtime log"
    tail -n 160 "${GOVERNANCE_LOG_FILE}" >&2 || true
  fi

  exit "${exit_code}"
}

start_stack() {
  if [[ "${REUSE_RUNNING_STACK}" == "true" ]]; then
    print_step "Reusing existing local infrastructure"
  else
    print_step "Starting local infrastructure"
    bash "${REPO_ROOT}/scripts/local-start.sh"
    STACK_STARTED_BY_SCRIPT=true
  fi

  print_step "Starting optional Kafka broker"
  compose --profile optional up -d kafka
  wait_for_kafka
}

ensure_runtime_schema() {
  print_step "Applying runtime schema baseline"
  run_sql_file "sql/init-schema.sql"
  run_sql_file "sql/init-data.sql"
}

install_governance_dependencies() {
  print_step "Installing governance runtime dependencies"
  mvn -B -pl governance -am install -DskipTests >/dev/null
}

start_governance_kafka_runtime() {
  local group_id="sqlforge-kafka-smoke-$(date +%s)"

  mkdir -p "${GOVERNANCE_LOG_DIR}"
  ensure_port_free governance 8080

  print_step "Starting governance in KAFKA mode"
  : > "${GOVERNANCE_LOG_FILE}"
  (
    cd "${REPO_ROOT}"
    export SPRING_PROFILES_ACTIVE=prod
    export SQLFORGE_PROD_DB_URL="jdbc:mysql://localhost:3306/sqlforge?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
    export SQLFORGE_PROD_DB_USER="${MYSQL_USER}"
    export SQLFORGE_PROD_DB_PASSWORD="${MYSQL_PASSWORD}"
    export SQLFORGE_PROD_REDIS_NODES="localhost:6379"
    export SQLFORGE_PROD_CRYPTO_KEY_ID="${SQLFORGE_PROD_CRYPTO_KEY_ID:-governance-prod-smoke-key}"
    export SQLFORGE_PROD_CRYPTO_KEY_BASE64="${DEV_CRYPTO_KEY_BASE64}"
    export SQLFORGE_PROD_KAFKA_BOOTSTRAP="${KAFKA_BOOTSTRAP}"
    export SQLFORGE_PROD_KAFKA_SECURITY_PROTOCOL="${KAFKA_SECURITY_PROTOCOL}"
    export SQLFORGE_PROD_KAFKA_SASL_MECHANISM="${KAFKA_SASL_MECHANISM}"
    export SQLFORGE_PROD_KAFKA_SASL_JAAS_CONFIG="${KAFKA_SASL_JAAS_CONFIG}"
    export SQLFORGE_PROD_KAFKA_SSL_TRUSTSTORE_LOCATION="${KAFKA_SSL_TRUSTSTORE_LOCATION}"
    export SQLFORGE_PROD_KAFKA_SSL_TRUSTSTORE_PASSWORD="${KAFKA_SSL_TRUSTSTORE_PASSWORD}"
    export SQLFORGE_PROD_KAFKA_GROUP_ID="${group_id}"
    export SQLFORGE_PROD_KAFKA_PRODUCER_REQUEST_TIMEOUT_MS="${SQLFORGE_PROD_KAFKA_PRODUCER_REQUEST_TIMEOUT_MS:-3000}"
    export SQLFORGE_PROD_KAFKA_PRODUCER_DELIVERY_TIMEOUT_MS="${SQLFORGE_PROD_KAFKA_PRODUCER_DELIVERY_TIMEOUT_MS:-5000}"
    export SQLFORGE_PROD_KAFKA_PRODUCER_MAX_BLOCK_MS="${SQLFORGE_PROD_KAFKA_PRODUCER_MAX_BLOCK_MS:-3000}"
    exec nohup mvn -B -f governance/pom.xml \
      "org.springframework.boot:spring-boot-maven-plugin:$(spring_boot_version):run" \
      > "${GOVERNANCE_LOG_FILE}" 2>&1
  ) &
  GOVERNANCE_PID=$!
  wait_for_http governance "http://localhost:8080/api/governance/health" "${GOVERNANCE_PID}" 120
}

build_protected_headers() {
  local request_id="$1"
  local trace_id="$2"
  local now_ms
  local expires_at

  now_ms="$(date +%s000)"
  expires_at="$((now_ms + 600000))"
  printf '%s\n' \
    "-H" "X-Tenant-Id: system" \
    "-H" "X-User-Id: kafka-runtime-user-001" \
    "-H" "X-Request-Id: ${request_id}" \
    "-H" "X-Trace-Id: ${trace_id}" \
    "-H" "X-Auth-Source: gateway" \
    "-H" "X-Issued-At: ${now_ms}" \
    "-H" "X-Expires-At: ${expires_at}" \
    "-H" "Content-Type: application/json"
}

assert_http_ok() {
  local url="$1"
  local response_file="$2"
  shift 2
  local status_code

  status_code="$(curl -sS -o "${response_file}" -w '%{http_code}' "$@" "${url}")"
  if [[ "${status_code}" != "200" ]]; then
    echo "HTTP request failed for ${url}, status=${status_code}" >&2
    cat "${response_file}" >&2 || true
    exit 1
  fi
}

publish_audit_event() {
  local trace_id="$1"
  local request_id="$2"
  local resource_id="$3"
  local response_file="$4"
  local payload_file="${GOVERNANCE_LOG_DIR}/audit-${trace_id}.json"
  local -a protected_headers

  printf '{"serviceCode":"QUERY_EXECUTION","operationCode":"AUDIT_QUERY","resourceType":"QUERY","resourceId":"%s","resultStatus":"SUCCESS","elapsedMs":12,"sourceIp":"127.0.0.1","userAgent":"phase-f-kafka-runtime-gate","requestParams":"{\\"traceId\\":\\"%s\\"}","responseSummary":"Kafka runtime gate smoke"}' \
    "${resource_id}" "${trace_id}" > "${payload_file}"
  mapfile -t protected_headers < <(build_protected_headers "${request_id}" "${trace_id}")
  assert_http_ok "http://localhost:8080/api/governance/internal/audit/write" "${response_file}" \
    -X POST "${protected_headers[@]}" --data-binary "@${payload_file}"
}

run_config_check() {
  print_step "Running Kafka bootstrap/security parameter gate"
  python3 "${REPO_ROOT}/scripts/verify_kafka_runtime_config.py" \
    --bootstrap-servers "${KAFKA_BOOTSTRAP}" \
    --security-protocol "${KAFKA_SECURITY_PROTOCOL}" \
    --sasl-mechanism "${KAFKA_SASL_MECHANISM}" \
    --sasl-jaas-config "${KAFKA_SASL_JAAS_CONFIG}" \
    --ssl-truststore-location "${KAFKA_SSL_TRUSTSTORE_LOCATION}" \
    --ssl-truststore-password "${KAFKA_SSL_TRUSTSTORE_PASSWORD}"

  print_step "Verifying Kafka security gate rejects incomplete secure config"
  if python3 "${REPO_ROOT}/scripts/verify_kafka_runtime_config.py" \
    --bootstrap-servers "${KAFKA_BOOTSTRAP}" \
    --security-protocol "SASL_SSL" >/dev/null 2>&1; then
    echo "Kafka security parameter gate did not reject incomplete SASL_SSL configuration." >&2
    exit 1
  fi
}

run_runtime_smoke() {
  local schedule_response="${GOVERNANCE_LOG_DIR}/schedule.json"
  local success_response="${GOVERNANCE_LOG_DIR}/audit-success-response.json"
  local fallback_response="${GOVERNANCE_LOG_DIR}/audit-fallback-response.json"
  local success_trace_id="kafka-success-$(date +%s)"
  local fallback_trace_id="kafka-fallback-$(date +%s)"

  start_stack
  ensure_runtime_schema
  install_governance_dependencies
  start_governance_kafka_runtime

  print_step "Checking Kafka topic connectivity"
  docker exec "${KAFKA_CONTAINER}" sh -lc \
    "kafka-topics --bootstrap-server localhost:9092 --create --if-not-exists --topic governance.audit.event --partitions 1 --replication-factor 1 >/dev/null 2>&1"

  print_step "Checking governance schedule extension state in KAFKA mode"
  assert_http_ok "http://localhost:8080/api/governance/internal/schedule/extensions" "${schedule_response}" \
    -H "X-Tenant-Id: system" \
    -H "X-User-Id: kafka-runtime-user-001" \
    -H "X-Request-Id: kafka-schedule-001" \
    -H "X-Trace-Id: kafka-schedule-001" \
    -H "X-Auth-Source: gateway" \
    -H "X-Issued-At: $(date +%s000)" \
    -H "X-Expires-At: $(( $(date +%s000) + 600000 ))"
  grep -F '"currentMode":"KAFKA"' "${schedule_response}" >/dev/null 2>&1
  grep -F '"status":"EXTERNALIZED"' "${schedule_response}" >/dev/null 2>&1

  print_step "Publishing audit event through real Kafka success path"
  publish_audit_event "${success_trace_id}" "kafka-success-request-001" "query-kafka-success" "${success_response}"
  grep -F '"deliveryMode":"KAFKA"' "${success_response}" >/dev/null 2>&1
  wait_for_log_text "${success_trace_id}" 40
  local success_fallback_count
  success_fallback_count="$(mysql_exec "SELECT COUNT(*) FROM kafka_message_queue WHERE topic = 'governance.audit.event' AND message_body LIKE '%${success_trace_id}%';")"
  if [[ "${success_fallback_count}" != "0" ]]; then
    echo "Expected no fallback queue rows for successful Kafka delivery, got ${success_fallback_count}." >&2
    exit 1
  fi

  print_step "Stopping Kafka broker to force fallback recovery"
  docker stop "${KAFKA_CONTAINER}" >/dev/null

  print_step "Publishing audit event through fallback recovery path"
  publish_audit_event "${fallback_trace_id}" "kafka-fallback-request-001" "query-kafka-fallback" "${fallback_response}"
  grep -F '"deliveryMode":"KAFKA"' "${fallback_response}" >/dev/null 2>&1
  wait_for_log_text "queued fallback message" 40
  local fallback_count
  fallback_count="$(mysql_exec "SELECT COUNT(*) FROM kafka_message_queue WHERE topic = 'governance.audit.event' AND message_body LIKE '%${fallback_trace_id}%';")"
  if [[ -z "${fallback_count}" || "${fallback_count}" -lt 1 ]]; then
    echo "Expected fallback queue rows for failed Kafka delivery, got ${fallback_count:-0}." >&2
    exit 1
  fi
}

main() {
  parse_args "$@"
  setup_compose
  require_command curl
  require_command python3
  trap cleanup EXIT

  cd "${REPO_ROOT}"

  if [[ "${RUN_CONFIG_CHECK}" == "true" ]]; then
    run_config_check
  fi

  if [[ "${RUN_RUNTIME_SMOKE}" == "true" ]]; then
    run_runtime_smoke
  fi

  print_step "Kafka runtime gate completed"
}

main "$@"
