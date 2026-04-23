#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/smoke-lib.sh"

GOVERNANCE_BASE_URL="${GOVERNANCE_BASE_URL:-http://localhost:8080}"
QUERY_EXECUTION_BASE_URL="${QUERY_EXECUTION_BASE_URL:-http://localhost:8081}"
SQL_OPTIMIZATION_BASE_URL="${SQL_OPTIMIZATION_BASE_URL:-http://localhost:8082}"
BENCHMARK_ENGINE_BASE_URL="${BENCHMARK_ENGINE_BASE_URL:-http://localhost:8083}"
FRONTEND_BASE_URL="${FRONTEND_BASE_URL:-http://localhost:3000}"

GOVERNANCE_HEALTH_URL="${GOVERNANCE_HEALTH_URL:-${GOVERNANCE_BASE_URL%/}/api/governance/health}"
QUERY_EXECUTION_HEALTH_URL="${QUERY_EXECUTION_HEALTH_URL:-${QUERY_EXECUTION_BASE_URL%/}/actuator/health}"
SQL_OPTIMIZATION_HEALTH_URL="${SQL_OPTIMIZATION_HEALTH_URL:-${SQL_OPTIMIZATION_BASE_URL%/}/actuator/health}"
BENCHMARK_ENGINE_HEALTH_URL="${BENCHMARK_ENGINE_HEALTH_URL:-${BENCHMARK_ENGINE_BASE_URL%/}/actuator/health}"
GOVERNANCE_PROTECTED_CHECK_URL="${GOVERNANCE_PROTECTED_CHECK_URL:-${GOVERNANCE_BASE_URL%/}/api/governance/admin/messages/stats}"

REQUEST_TENANT_ID="${REQUEST_TENANT_ID:-tenant-a}"
REQUEST_USER_ID="${REQUEST_USER_ID:-analyst-001}"
REQUEST_ROLE_CODES="${REQUEST_ROLE_CODES:-TENANT_ADMIN,ANALYST}"
REQUEST_AUTH_SOURCE="${REQUEST_AUTH_SOURCE:-header}"
POLL_ATTEMPTS="${ENV_SMOKE_POLL_ATTEMPTS:-30}"
POLL_INTERVAL_SECONDS="${ENV_SMOKE_POLL_INTERVAL_SECONDS:-1}"

CHECK_CONFIG_ONLY=false
SKIP_FRONTEND=false

usage() {
  cat <<'EOF'
Usage: ./scripts/run-env-smoke.sh [options]

Environment variables:
  GOVERNANCE_BASE_URL           Base URL for governance service.
  QUERY_EXECUTION_BASE_URL      Base URL for query-execution service.
  SQL_OPTIMIZATION_BASE_URL     Base URL for sql-optimization service.
  BENCHMARK_ENGINE_BASE_URL     Base URL for benchmark-engine service.
  FRONTEND_BASE_URL             Reachability URL for the frontend.
  GOVERNANCE_HEALTH_URL         Optional explicit governance health URL override.
  QUERY_EXECUTION_HEALTH_URL    Optional explicit query-execution health URL override.
  SQL_OPTIMIZATION_HEALTH_URL   Optional explicit sql-optimization health URL override.
  BENCHMARK_ENGINE_HEALTH_URL   Optional explicit benchmark-engine health URL override.
  GOVERNANCE_PROTECTED_CHECK_URL
                               Optional explicit protected governance URL override.
  REQUEST_TENANT_ID             Tenant header value for protected calls.
  REQUEST_USER_ID               User header value for protected calls.
  REQUEST_ROLE_CODES            Comma-separated role codes for protected calls.
  REQUEST_AUTH_SOURCE           Auth source header value.
  ENV_SMOKE_POLL_ATTEMPTS       Poll attempts for async task status checks.
  ENV_SMOKE_POLL_INTERVAL_SECONDS
                               Poll interval for async task status checks.

Options:
  --check-config  Validate and print the effective configuration without calling endpoints.
  --skip-frontend Skip the frontend reachability check.
  -h, --help      Show this help message.
EOF
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --check-config)
        CHECK_CONFIG_ONLY=true
        shift
        ;;
      --skip-frontend)
        SKIP_FRONTEND=true
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

require_integer() {
  local name="$1"
  local value="$2"
  if ! [[ "${value}" =~ ^[0-9]+$ ]] || (( value <= 0 )); then
    echo "Invalid ${name}: ${value}" >&2
    exit 1
  fi
}

require_http_url() {
  local name="$1"
  local value="$2"
  if ! [[ "${value}" =~ ^https?://.+$ ]]; then
    echo "Invalid ${name}: ${value}" >&2
    exit 1
  fi
}

print_effective_config() {
  cat <<EOF
Effective env smoke configuration:
  governance: ${GOVERNANCE_BASE_URL}
  query-execution: ${QUERY_EXECUTION_BASE_URL}
  sql-optimization: ${SQL_OPTIMIZATION_BASE_URL}
  benchmark-engine: ${BENCHMARK_ENGINE_BASE_URL}
  frontend: ${FRONTEND_BASE_URL}
  governance health: ${GOVERNANCE_HEALTH_URL}
  query-execution health: ${QUERY_EXECUTION_HEALTH_URL}
  sql-optimization health: ${SQL_OPTIMIZATION_HEALTH_URL}
  benchmark-engine health: ${BENCHMARK_ENGINE_HEALTH_URL}
  protected governance URL: ${GOVERNANCE_PROTECTED_CHECK_URL}
  request tenant: ${REQUEST_TENANT_ID}
  request user: ${REQUEST_USER_ID}
  request roles: ${REQUEST_ROLE_CODES}
  auth source: ${REQUEST_AUTH_SOURCE}
  poll attempts: ${POLL_ATTEMPTS}
  poll interval seconds: ${POLL_INTERVAL_SECONDS}
EOF
}

validate_config() {
  require_command curl
  require_command python3

  require_http_url "GOVERNANCE_BASE_URL" "${GOVERNANCE_BASE_URL}"
  require_http_url "QUERY_EXECUTION_BASE_URL" "${QUERY_EXECUTION_BASE_URL}"
  require_http_url "SQL_OPTIMIZATION_BASE_URL" "${SQL_OPTIMIZATION_BASE_URL}"
  require_http_url "BENCHMARK_ENGINE_BASE_URL" "${BENCHMARK_ENGINE_BASE_URL}"
  require_http_url "FRONTEND_BASE_URL" "${FRONTEND_BASE_URL}"
  require_http_url "GOVERNANCE_HEALTH_URL" "${GOVERNANCE_HEALTH_URL}"
  require_http_url "QUERY_EXECUTION_HEALTH_URL" "${QUERY_EXECUTION_HEALTH_URL}"
  require_http_url "SQL_OPTIMIZATION_HEALTH_URL" "${SQL_OPTIMIZATION_HEALTH_URL}"
  require_http_url "BENCHMARK_ENGINE_HEALTH_URL" "${BENCHMARK_ENGINE_HEALTH_URL}"
  require_http_url "GOVERNANCE_PROTECTED_CHECK_URL" "${GOVERNANCE_PROTECTED_CHECK_URL}"

  if [[ -z "${REQUEST_TENANT_ID}" || -z "${REQUEST_USER_ID}" || -z "${REQUEST_ROLE_CODES}" || -z "${REQUEST_AUTH_SOURCE}" ]]; then
    echo "Protected request context must define tenant, user, role codes, and auth source." >&2
    exit 1
  fi

  require_integer "ENV_SMOKE_POLL_ATTEMPTS" "${POLL_ATTEMPTS}"
  require_integer "ENV_SMOKE_POLL_INTERVAL_SECONDS" "${POLL_INTERVAL_SECONDS}"
}

create_context_headers() {
  local prefix="$1"
  local issued_at expires_at
  issued_at="$(date +%s000)"
  expires_at="$((issued_at + 600000))"
  build_protected_headers "${prefix}-request" "${prefix}-trace" "${issued_at}" "${expires_at}"
}

assert_health() {
  local service_name="$1"
  local url="$2"
  local response
  print_step "Health check: ${service_name}"
  response="$(assert_get_json "${url}")"
  echo "${response}"
  json_assert "${response}" 'payload["status"] == "UP"'
}

assert_frontend() {
  local response
  print_step "Frontend reachability"
  response="$(assert_http_status "GET" "${FRONTEND_BASE_URL}" "200")"
  if [[ -z "${response}" ]]; then
    echo "Frontend response body is empty for ${FRONTEND_BASE_URL}" >&2
    exit 1
  fi
  echo "${response}" | head -c 200
  printf '\n'
}

assert_protected_context() {
  local protected_response anonymous_response
  local -a protected_headers

  mapfile -t protected_headers < <(create_context_headers "env-smoke-governance")

  print_step "Protected governance request with headers"
  protected_response="$(assert_get_json "${GOVERNANCE_PROTECTED_CHECK_URL}" "${protected_headers[@]}")"
  echo "${protected_response}"
  json_assert "${protected_response}" '"pending" in payload and "failed" in payload'

  print_step "Protected governance request rejects missing headers"
  anonymous_response="$(assert_http_status "GET" "${GOVERNANCE_PROTECTED_CHECK_URL}" "401")"
  echo "${anonymous_response}"
}

poll_task_status() {
  local label="$1"
  local url="$2"
  local expected_status="$3"
  shift 3

  local attempt response current_status
  for attempt in $(seq 1 "${POLL_ATTEMPTS}"); do
    response="$(assert_get_json "${url}" "$@")"
    current_status="$(json_extract "${response}" 'payload.get("status")')"
    if [[ "${current_status}" == "${expected_status}" ]]; then
      echo "${response}"
      return 0
    fi
    sleep "${POLL_INTERVAL_SECONDS}"
  done

  echo "${label} did not reach ${expected_status} within ${POLL_ATTEMPTS} attempts." >&2
  echo "${response}" >&2
  exit 1
}

run_query_smoke() {
  local response target_engine query_body
  local -a headers
  mapfile -t headers < <(create_context_headers "env-smoke-query")
  query_body="$(cat <<EOF
{"sqlText":"SELECT * FROM orders","tenantId":"${REQUEST_TENANT_ID}","datasourceType":"HETU","accelerationPreference":"PREFER_ACCELERATED","faultToleranceStrategy":"FAIL_FAST"}
EOF
)"

  print_step "query-execution -> governance minimal smoke"
  response="$(assert_post_json "${QUERY_EXECUTION_BASE_URL%/}/api/query-execution/queries/execute" \
    "${query_body}" \
    "${headers[@]}")"
  echo "${response}"
  json_assert "${response}" 'payload["status"] == "SUCCESS"'
  target_engine="$(json_extract "${response}" 'payload["metadata"].get("targetEngine")')"
  if [[ -z "${target_engine}" ]]; then
    echo "Query smoke did not return metadata.targetEngine." >&2
    exit 1
  fi
}

run_sql_optimization_smoke() {
  local submit_response status_response task_id submit_body
  local -a submit_headers status_headers
  mapfile -t submit_headers < <(create_context_headers "env-smoke-sql-opt-submit")
  mapfile -t status_headers < <(create_context_headers "env-smoke-sql-opt-status")
  submit_body="$(cat <<EOF
{"tenantId":"${REQUEST_TENANT_ID}","taskType":"REWRITE","sqlText":"SELECT * FROM orders","datasourceType":"HETU","taskContext":{"parseDepth":"DEEP"}}
EOF
)"

  print_step "sql-optimization -> governance minimal smoke"
  submit_response="$(assert_post_json "${SQL_OPTIMIZATION_BASE_URL%/}/api/sql-optimization/tasks" \
    "${submit_body}" \
    "${submit_headers[@]}")"
  echo "${submit_response}"
  json_assert "${submit_response}" 'payload["status"] == "QUEUED"'
  task_id="$(json_extract "${submit_response}" 'payload.get("taskId")')"
  if [[ -z "${task_id}" ]]; then
    echo "SQL optimization smoke did not return taskId." >&2
    exit 1
  fi

  status_response="$(poll_task_status "sql-optimization task ${task_id}" \
    "${SQL_OPTIMIZATION_BASE_URL%/}/api/sql-optimization/tasks/${task_id}" "SUCCEEDED" "${status_headers[@]}")"
  echo "${status_response}"
  json_assert "${status_response}" 'payload["status"] == "SUCCEEDED"'
}

run_benchmark_smoke() {
  local submit_response status_response report_response task_id report_id submit_body
  local -a submit_headers status_headers report_headers
  mapfile -t submit_headers < <(create_context_headers "env-smoke-benchmark-submit")
  mapfile -t status_headers < <(create_context_headers "env-smoke-benchmark-status")
  mapfile -t report_headers < <(create_context_headers "env-smoke-benchmark-report")
  submit_body="$(cat <<EOF
{"tenantId":"${REQUEST_TENANT_ID}","taskType":"COMPARISON","sqlText":"SELECT * FROM orders","taskContext":{"targetEngines":["HETU","HIVE"],"readonlyRequired":true,"shadowEnvironmentMode":"REQUIRED"}}
EOF
)"

  print_step "benchmark-engine -> governance minimal smoke"
  submit_response="$(assert_post_json "${BENCHMARK_ENGINE_BASE_URL%/}/api/benchmark-engine/tasks" \
    "${submit_body}" \
    "${submit_headers[@]}")"
  echo "${submit_response}"
  json_assert "${submit_response}" 'payload["status"] == "QUEUED"'
  task_id="$(json_extract "${submit_response}" 'payload.get("taskId")')"
  if [[ -z "${task_id}" ]]; then
    echo "Benchmark smoke did not return taskId." >&2
    exit 1
  fi

  status_response="$(poll_task_status "benchmark task ${task_id}" \
    "${BENCHMARK_ENGINE_BASE_URL%/}/api/benchmark-engine/tasks/${task_id}" "SUCCEEDED" "${status_headers[@]}")"
  echo "${status_response}"
  report_id="$(json_extract "${status_response}" 'payload.get("reportId")')"
  if [[ -z "${report_id}" ]]; then
    echo "Benchmark smoke did not return reportId." >&2
    exit 1
  fi

  report_response="$(assert_get_json "${BENCHMARK_ENGINE_BASE_URL%/}/api/benchmark-engine/reports/${report_id}" "${report_headers[@]}")"
  echo "${report_response}"
  json_assert "${report_response}" 'payload["reportId"] == "'"${report_id}"'"'
}

main() {
  parse_args "$@"
  validate_config
  print_effective_config

  if [[ "${CHECK_CONFIG_ONLY}" == "true" ]]; then
    exit 0
  fi

  assert_health "governance" "${GOVERNANCE_HEALTH_URL}"
  assert_health "query-execution" "${QUERY_EXECUTION_HEALTH_URL}"
  assert_health "sql-optimization" "${SQL_OPTIMIZATION_HEALTH_URL}"
  assert_health "benchmark-engine" "${BENCHMARK_ENGINE_HEALTH_URL}"

  if [[ "${SKIP_FRONTEND}" != "true" ]]; then
    assert_frontend
  fi

  assert_protected_context
  run_query_smoke
  run_sql_optimization_smoke
  run_benchmark_smoke

  print_step "Environment minimal smoke completed"
}

main "$@"
