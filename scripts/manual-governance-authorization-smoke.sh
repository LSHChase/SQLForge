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
REQUEST_USER_ID="${REQUEST_USER_ID:-tenant-admin-001}"
REQUEST_ROLE_CODES="${REQUEST_ROLE_CODES:-TENANT_ADMIN}"
REQUEST_AUTH_SOURCE="${REQUEST_AUTH_SOURCE:-header}"
READONLY_ROLE_CODES="${READONLY_ROLE_CODES:-READONLY}"
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
  local -a restore_headers
  issued_at="$(date +%s000)"
  expires_at="$((issued_at + 600000))"
  REQUEST_ROLE_CODES="TENANT_ADMIN"
  mapfile -t restore_headers < <(build_protected_headers "${RESTORE_CHANGE_REQUEST_ID}" "${RESTORE_CHANGE_REQUEST_ID}" "${issued_at}" "${expires_at}")

  set +e
  curl -sS -X POST "${GOVERNANCE_API_BASE_URL}/api/governance/internal/authorization/datasource/change" \
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

  print_step "Cleaning up governance authorization audit rows"
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
  local original_role_codes

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
  SUCCESS_REQUEST_ID="gov-authz-success-$(date +%Y%m%d%H%M%S)"
  DENY_REQUEST_ID="gov-authz-deny-$(date +%Y%m%d%H%M%S)"
  CROSS_TENANT_REQUEST_ID="gov-authz-cross-tenant-$(date +%Y%m%d%H%M%S)"
  REVOKE_CHANGE_REQUEST_ID="gov-authz-revoke-$(date +%Y%m%d%H%M%S)"
  REVOKED_ACCESS_REQUEST_ID="gov-authz-revoked-access-$(date +%Y%m%d%H%M%S)"
  RESTORE_CHANGE_REQUEST_ID="gov-authz-restore-$(date +%Y%m%d%H%M%S)"

  original_role_codes="${REQUEST_ROLE_CODES}"
  REQUEST_ROLE_CODES="${original_role_codes}"
  mapfile -t admin_headers < <(build_protected_headers "${SUCCESS_REQUEST_ID}" "${SUCCESS_REQUEST_ID}" "${issued_at}" "${expires_at}")
  REQUEST_ROLE_CODES="${READONLY_ROLE_CODES}"
  mapfile -t readonly_headers < <(build_protected_headers "${DENY_REQUEST_ID}" "${DENY_REQUEST_ID}" "${issued_at}" "${expires_at}")
  REQUEST_ROLE_CODES="${original_role_codes}"
  mapfile -t cross_tenant_headers < <(build_protected_headers "${CROSS_TENANT_REQUEST_ID}" "${CROSS_TENANT_REQUEST_ID}" "${issued_at}" "${expires_at}")
  mapfile -t revoke_headers < <(build_protected_headers "${REVOKE_CHANGE_REQUEST_ID}" "${REVOKE_CHANGE_REQUEST_ID}" "${issued_at}" "${expires_at}")
  mapfile -t revoked_headers < <(build_protected_headers "${REVOKED_ACCESS_REQUEST_ID}" "${REVOKED_ACCESS_REQUEST_ID}" "${issued_at}" "${expires_at}")

  print_step "Verifying governance authorization success path"
  success_response="$(assert_post_json "${GOVERNANCE_API_BASE_URL}/api/governance/internal/authorization/decide" \
    '{"serviceCode":"QUERY_EXECUTION","tenantId":"tenant-a","resourceType":"QUERY_EXECUTION_QUERY","resourceId":"query-fingerprint-001","operationCode":"QUERY_EXECUTE_SYNC","datasourceId":"query-hetu"}' \
    "${admin_headers[@]}")"
  echo "${success_response}"
  json_assert "${success_response}" 'payload["allowed"] is True'
  json_assert "${success_response}" 'payload["reason"] == "ALLOWED"'

  success_row="$(mysql_exec "SELECT status, target_id FROM audit_log WHERE request_id = '${SUCCESS_REQUEST_ID}' AND service_code = 'GOVERNANCE' AND operation_type = 'AUTHORIZATION_DECISION' ORDER BY id DESC LIMIT 1;")"
  echo "${success_row}"
  if [[ "${success_row}" != SUCCESS$'\t'query-fingerprint-001 ]]; then
    echo "Expected SUCCESS authorization audit row for ${SUCCESS_REQUEST_ID}" >&2
    exit 1
  fi

  print_step "Verifying governance authorization denial path"
  deny_response="$(assert_post_json "${GOVERNANCE_API_BASE_URL}/api/governance/internal/authorization/decide" \
    '{"serviceCode":"QUERY_EXECUTION","tenantId":"tenant-a","resourceType":"QUERY_EXECUTION_QUERY","resourceId":"query-fingerprint-002","operationCode":"QUERY_EXECUTE_SYNC","datasourceId":"query-hetu"}' \
    "${readonly_headers[@]}")"
  echo "${deny_response}"
  json_assert "${deny_response}" 'payload["allowed"] is False'
  json_assert "${deny_response}" 'payload["reason"] == "ROLE_PERMISSION_DENIED"'
  json_assert "${deny_response}" 'payload["errorCode"] == 20000'

  deny_row="$(mysql_exec "SELECT status, target_id FROM audit_log WHERE request_id = '${DENY_REQUEST_ID}' AND service_code = 'GOVERNANCE' AND operation_type = 'AUTHORIZATION_DECISION' ORDER BY id DESC LIMIT 1;")"
  echo "${deny_row}"
  if [[ "${deny_row}" != FAILED$'\t'query-fingerprint-002 ]]; then
    echo "Expected FAILED authorization audit row for ${DENY_REQUEST_ID}" >&2
    exit 1
  fi

  print_step "Verifying cross-tenant authorization denial path"
  cross_tenant_response="$(assert_post_json "${GOVERNANCE_API_BASE_URL}/api/governance/internal/authorization/decide" \
    '{"serviceCode":"QUERY_EXECUTION","tenantId":"tenant-b","resourceType":"QUERY_EXECUTION_QUERY","resourceId":"query-fingerprint-003","operationCode":"QUERY_EXECUTE_SYNC","datasourceId":"query-hetu"}' \
    "${cross_tenant_headers[@]}")"
  echo "${cross_tenant_response}"
  json_assert "${cross_tenant_response}" 'payload["allowed"] is False'
  json_assert "${cross_tenant_response}" 'payload["reason"] == "CALLER_TENANT_MISMATCH"'
  json_assert "${cross_tenant_response}" 'payload["errorCode"] == 20001'

  cross_tenant_row="$(mysql_exec "SELECT status, target_id FROM audit_log WHERE request_id = '${CROSS_TENANT_REQUEST_ID}' AND service_code = 'GOVERNANCE' AND operation_type = 'AUTHORIZATION_DECISION' ORDER BY id DESC LIMIT 1;")"
  echo "${cross_tenant_row}"
  if [[ "${cross_tenant_row}" != FAILED$'\t'query-fingerprint-003 ]]; then
    echo "Expected FAILED cross-tenant authorization audit row for ${CROSS_TENANT_REQUEST_ID}" >&2
    exit 1
  fi

  print_step "Revoking datasource authorization through governance mutation endpoint"
  revoke_response="$(assert_post_json "${GOVERNANCE_API_BASE_URL}/api/governance/internal/authorization/datasource/change" \
    '{"tenantId":"tenant-a","datasourceId":"query-hetu","state":"REVOKED","changeReason":"runtime smoke revoke"}' \
    "${revoke_headers[@]}")"
  DATASOURCE_REVOKED=true
  echo "${revoke_response}"
  json_assert "${revoke_response}" 'payload["status"] == "UPDATED"'
  json_assert "${revoke_response}" 'payload["state"] == "REVOKED"'

  revoke_row="$(mysql_exec "SELECT status, target_id, request_params FROM audit_log WHERE request_id = '${REVOKE_CHANGE_REQUEST_ID}' AND service_code = 'GOVERNANCE' AND operation_type = 'PERMISSION_CHANGE' ORDER BY id DESC LIMIT 1;")"
  echo "${revoke_row}"
  if [[ "${revoke_row}" != SUCCESS$'\t'tenant-a:query-hetu$'\t'* ]]; then
    echo "Expected SUCCESS permission change audit row for ${REVOKE_CHANGE_REQUEST_ID}" >&2
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

  print_step "Verifying revoked datasource blocks subsequent authorization"
  revoked_response="$(assert_post_json "${GOVERNANCE_API_BASE_URL}/api/governance/internal/authorization/decide" \
    '{"serviceCode":"QUERY_EXECUTION","tenantId":"tenant-a","resourceType":"QUERY_EXECUTION_QUERY","resourceId":"query-fingerprint-004","operationCode":"QUERY_EXECUTE_SYNC","datasourceId":"query-hetu"}' \
    "${revoked_headers[@]}")"
  echo "${revoked_response}"
  json_assert "${revoked_response}" 'payload["allowed"] is False'
  json_assert "${revoked_response}" 'payload["reason"] == "DATASOURCE_ACCESS_REVOKED"'
  json_assert "${revoked_response}" 'payload["errorCode"] == 20002'

  revoked_row="$(mysql_exec "SELECT status, target_id FROM audit_log WHERE request_id = '${REVOKED_ACCESS_REQUEST_ID}' AND service_code = 'GOVERNANCE' AND operation_type = 'AUTHORIZATION_DECISION' ORDER BY id DESC LIMIT 1;")"
  echo "${revoked_row}"
  if [[ "${revoked_row}" != FAILED$'\t'query-fingerprint-004 ]]; then
    echo "Expected FAILED revoked authorization audit row for ${REVOKED_ACCESS_REQUEST_ID}" >&2
    exit 1
  fi

  restore_datasource_policy
  print_step "Governance authorization smoke test completed"
}

main "$@"
