#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/smoke-lib.sh"

GOVERNANCE_API_BASE_URL="${GOVERNANCE_API_BASE_URL:-http://localhost:8080}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-sqlforge-mysql}"
MYSQL_DATABASE="${MYSQL_DATABASE:-sqlforge}"
MYSQL_USER="${MYSQL_USER:-sqlforge}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-sqlforge}"
REQUEST_TENANT_ID="${REQUEST_TENANT_ID:-tenant-a}"
REQUEST_USER_ID="${REQUEST_USER_ID:-datasource-access-user-001}"
REQUEST_AUTH_SOURCE="${REQUEST_AUTH_SOURCE:-header}"
CLEANUP=false
DATASOURCE_REVOKED=false

SUCCESS_REQUEST_ID=""
DENY_REQUEST_ID=""
CROSS_TENANT_REQUEST_ID=""
REVOKE_CHANGE_REQUEST_ID=""
REVOKED_ACCESS_REQUEST_ID=""
RESTORE_CHANGE_REQUEST_ID=""

usage() {
  cat <<'EOF'
Usage: ./scripts/manual-governance-authorization-smoke.sh [--cleanup]

Options:
  --cleanup   Delete smoke-created governance audit_log rows after verification.
EOF
}

mysql_exec() {
  local sql="$1"
  docker exec "${MYSQL_CONTAINER}" sh -lc \
    "mysql -N -B -u${MYSQL_USER} -p${MYSQL_PASSWORD} ${MYSQL_DATABASE} -e \"$sql\""
}

restore_datasource_policy() {
  if [[ "${DATASOURCE_REVOKED}" != "true" ]]; then
    return
  fi

  local issued_at expires_at
  local original_tenant_id
  local -a restore_headers
  issued_at="$(date +%s000)"
  expires_at="$((issued_at + 600000))"
  original_tenant_id="${REQUEST_TENANT_ID}"
  REQUEST_TENANT_ID="system"
  mapfile -t restore_headers < <(build_protected_headers "${RESTORE_CHANGE_REQUEST_ID}" "${RESTORE_CHANGE_REQUEST_ID}" "${issued_at}" "${expires_at}")
  REQUEST_TENANT_ID="${original_tenant_id}"

  set +e
  curl -sS -X POST "${GOVERNANCE_API_BASE_URL}/api/governance/internal/datasource-access/scope/change" \
    "${restore_headers[@]}" \
    --data '{"tenantId":"tenant-a","datasourceId":"query-hetu","state":"ACTIVE","actions":["USE"],"changeReason":"runtime smoke restore"}' \
    >/dev/null
  set -e
  DATASOURCE_REVOKED=false
}

cleanup_rows() {
  if [[ "${CLEANUP}" != "true" ]]; then
    return
  fi

  print_step "清理 governance 数据源访问 smoke 审计行"
  mysql_exec "DELETE FROM audit_log WHERE request_id IN ('${SUCCESS_REQUEST_ID}','${DENY_REQUEST_ID}','${CROSS_TENANT_REQUEST_ID}','${REVOKE_CHANGE_REQUEST_ID}','${REVOKED_ACCESS_REQUEST_ID}','${RESTORE_CHANGE_REQUEST_ID}');"
}

cleanup_on_exit() {
  restore_datasource_policy
  cleanup_rows
}

main() {
  local issued_at expires_at
  local success_response deny_response cross_tenant_response revoke_response revoked_response
  local success_row deny_row cross_tenant_row revoke_row revoked_row
  local -a admin_headers readonly_headers cross_tenant_headers revoke_headers revoked_headers
  local original_tenant_id

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

  trap cleanup_on_exit EXIT

  require_command curl
  require_command docker
  require_command python3

  issued_at="$(date +%s000)"
  expires_at="$((issued_at + 600000))"
  SUCCESS_REQUEST_ID="gov-datasource-success-$(date +%Y%m%d%H%M%S)"
  DENY_REQUEST_ID="gov-datasource-deny-$(date +%Y%m%d%H%M%S)"
  CROSS_TENANT_REQUEST_ID="gov-datasource-cross-tenant-$(date +%Y%m%d%H%M%S)"
  REVOKE_CHANGE_REQUEST_ID="gov-datasource-revoke-$(date +%Y%m%d%H%M%S)"
  REVOKED_ACCESS_REQUEST_ID="gov-datasource-revoked-access-$(date +%Y%m%d%H%M%S)"
  RESTORE_CHANGE_REQUEST_ID="gov-datasource-restore-$(date +%Y%m%d%H%M%S)"

  mapfile -t admin_headers < <(build_protected_headers "${SUCCESS_REQUEST_ID}" "${SUCCESS_REQUEST_ID}" "${issued_at}" "${expires_at}")
  mapfile -t readonly_headers < <(build_protected_headers "${DENY_REQUEST_ID}" "${DENY_REQUEST_ID}" "${issued_at}" "${expires_at}")
  mapfile -t cross_tenant_headers < <(build_protected_headers "${CROSS_TENANT_REQUEST_ID}" "${CROSS_TENANT_REQUEST_ID}" "${issued_at}" "${expires_at}")
  original_tenant_id="${REQUEST_TENANT_ID}"
  REQUEST_TENANT_ID="system"
  mapfile -t revoke_headers < <(build_protected_headers "${REVOKE_CHANGE_REQUEST_ID}" "${REVOKE_CHANGE_REQUEST_ID}" "${issued_at}" "${expires_at}")
  REQUEST_TENANT_ID="${original_tenant_id}"
  mapfile -t revoked_headers < <(build_protected_headers "${REVOKED_ACCESS_REQUEST_ID}" "${REVOKED_ACCESS_REQUEST_ID}" "${issued_at}" "${expires_at}")

  print_step "验证 governance 数据源访问放行路径"
  success_response="$(assert_post_json "${GOVERNANCE_API_BASE_URL}/api/governance/internal/datasource-access/check" \
    '{"serviceCode":"QUERY_EXECUTION","tenantId":"tenant-a","resourceType":"QUERY_EXECUTION_QUERY","resourceId":"query-fingerprint-001","operationCode":"QUERY_EXECUTE_SYNC","datasourceId":"query-hetu","action":"USE"}' \
    "${admin_headers[@]}")"
  echo "${success_response}"
  json_assert "${success_response}" 'payload["allowed"] is True'
  json_assert "${success_response}" 'payload["reason"] == "ALLOWED"'

  success_row="$(mysql_exec "SELECT status, target_id FROM audit_log WHERE request_id = '${SUCCESS_REQUEST_ID}' AND service_code = 'GOVERNANCE' AND operation_type = 'DATASOURCE_ACCESS_CHECK' ORDER BY id DESC LIMIT 1;")"
  echo "${success_row}"
  if [[ "${success_row}" != SUCCESS$'\t'query-fingerprint-001 ]]; then
    echo "未找到 ${SUCCESS_REQUEST_ID} 对应的 SUCCESS 数据源访问审计行" >&2
    exit 1
  fi

  print_step "验证 governance 数据源动作拒绝路径"
  deny_response="$(assert_post_json "${GOVERNANCE_API_BASE_URL}/api/governance/internal/datasource-access/check" \
    '{"serviceCode":"QUERY_EXECUTION","tenantId":"tenant-a","resourceType":"QUERY_EXECUTION_QUERY","resourceId":"query-fingerprint-002","operationCode":"QUERY_EXECUTE_SYNC","datasourceId":"query-hetu","action":"EXPORT"}' \
    "${readonly_headers[@]}")"
  echo "${deny_response}"
  json_assert "${deny_response}" 'payload["allowed"] is False'
  json_assert "${deny_response}" 'payload["reason"] == "DATASOURCE_ACTION_DENIED"'
  json_assert "${deny_response}" 'payload["errorCode"] == 20002'

  deny_row="$(mysql_exec "SELECT status, target_id FROM audit_log WHERE request_id = '${DENY_REQUEST_ID}' AND service_code = 'GOVERNANCE' AND operation_type = 'DATASOURCE_ACCESS_CHECK' ORDER BY id DESC LIMIT 1;")"
  echo "${deny_row}"
  if [[ "${deny_row}" != FAILED$'\t'query-fingerprint-002 ]]; then
    echo "未找到 ${DENY_REQUEST_ID} 对应的 FAILED 数据源访问审计行" >&2
    exit 1
  fi

  print_step "验证跨租户数据源访问拒绝路径"
  cross_tenant_response="$(assert_post_json "${GOVERNANCE_API_BASE_URL}/api/governance/internal/datasource-access/check" \
    '{"serviceCode":"QUERY_EXECUTION","tenantId":"tenant-b","resourceType":"QUERY_EXECUTION_QUERY","resourceId":"query-fingerprint-003","operationCode":"QUERY_EXECUTE_SYNC","datasourceId":"query-hetu","action":"USE"}' \
    "${cross_tenant_headers[@]}")"
  echo "${cross_tenant_response}"
  json_assert "${cross_tenant_response}" 'payload["allowed"] is False'
  json_assert "${cross_tenant_response}" 'payload["reason"] == "CALLER_TENANT_MISMATCH"'
  json_assert "${cross_tenant_response}" 'payload["errorCode"] == 20001'

  cross_tenant_row="$(mysql_exec "SELECT status, target_id FROM audit_log WHERE request_id = '${CROSS_TENANT_REQUEST_ID}' AND service_code = 'GOVERNANCE' AND operation_type = 'DATASOURCE_ACCESS_CHECK' ORDER BY id DESC LIMIT 1;")"
  echo "${cross_tenant_row}"
  if [[ "${cross_tenant_row}" != FAILED$'\t'query-fingerprint-003 ]]; then
    echo "未找到 ${CROSS_TENANT_REQUEST_ID} 对应的 FAILED 跨租户数据源访问审计行" >&2
    exit 1
  fi

  print_step "通过 governance 变更接口撤销数据源访问范围"
  revoke_response="$(assert_post_json "${GOVERNANCE_API_BASE_URL}/api/governance/internal/datasource-access/scope/change" \
    '{"tenantId":"tenant-a","datasourceId":"query-hetu","state":"REVOKED","changeReason":"runtime smoke revoke"}' \
    "${revoke_headers[@]}")"
  DATASOURCE_REVOKED=true
  echo "${revoke_response}"
  json_assert "${revoke_response}" 'payload["status"] == "UPDATED"'
  json_assert "${revoke_response}" 'payload["state"] == "REVOKED"'

  revoke_row="$(mysql_exec "SELECT status, target_id, request_params FROM audit_log WHERE request_id = '${REVOKE_CHANGE_REQUEST_ID}' AND service_code = 'GOVERNANCE' AND operation_type = 'DATASOURCE_ACCESS_SCOPE_CHANGE' ORDER BY id DESC LIMIT 1;")"
  echo "${revoke_row}"
  if [[ "${revoke_row}" != SUCCESS$'\t'tenant-a:query-hetu$'\t'* ]]; then
    echo "未找到 ${REVOKE_CHANGE_REQUEST_ID} 对应的 SUCCESS 数据源范围变更审计行" >&2
    exit 1
  fi
  python3 - "${revoke_row}" <<'PY'
import json
import sys

row = sys.argv[1].split("\t", 2)
payload = json.loads(row[2])
if payload.get("state") != "REVOKED":
    raise SystemExit(1)
PY

  print_step "验证已撤销数据源会拒绝后续访问"
  revoked_response="$(assert_post_json "${GOVERNANCE_API_BASE_URL}/api/governance/internal/datasource-access/check" \
    '{"serviceCode":"QUERY_EXECUTION","tenantId":"tenant-a","resourceType":"QUERY_EXECUTION_QUERY","resourceId":"query-fingerprint-004","operationCode":"QUERY_EXECUTE_SYNC","datasourceId":"query-hetu","action":"USE"}' \
    "${revoked_headers[@]}")"
  echo "${revoked_response}"
  json_assert "${revoked_response}" 'payload["allowed"] is False'
  json_assert "${revoked_response}" 'payload["reason"] == "DATASOURCE_ACCESS_REVOKED"'
  json_assert "${revoked_response}" 'payload["errorCode"] == 20002'

  revoked_row="$(mysql_exec "SELECT status, target_id FROM audit_log WHERE request_id = '${REVOKED_ACCESS_REQUEST_ID}' AND service_code = 'GOVERNANCE' AND operation_type = 'DATASOURCE_ACCESS_CHECK' ORDER BY id DESC LIMIT 1;")"
  echo "${revoked_row}"
  if [[ "${revoked_row}" != FAILED$'\t'query-fingerprint-004 ]]; then
    echo "未找到 ${REVOKED_ACCESS_REQUEST_ID} 对应的 FAILED 已撤销数据源访问审计行" >&2
    exit 1
  fi

  restore_datasource_policy
  print_step "Governance 数据源访问 smoke 已完成"
}

main "$@"
