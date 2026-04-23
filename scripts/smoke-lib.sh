#!/usr/bin/env bash

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

print_step() {
  printf '\n[%s] %s\n' "$(date '+%H:%M:%S')" "$1"
}

build_protected_headers() {
  local request_id="$1"
  local trace_id="$2"
  local issued_at="$3"
  local expires_at="$4"

  printf '%s\n' \
    "-H" "X-Tenant-Id: ${REQUEST_TENANT_ID}" \
    "-H" "X-User-Id: ${REQUEST_USER_ID}" \
    "-H" "X-Role-Codes: ${REQUEST_ROLE_CODES}" \
    "-H" "X-Request-Id: ${request_id}" \
    "-H" "X-Trace-Id: ${trace_id}" \
    "-H" "X-Auth-Source: ${REQUEST_AUTH_SOURCE}" \
    "-H" "X-Issued-At: ${issued_at}" \
    "-H" "X-Expires-At: ${expires_at}" \
    "-H" "Content-Type: application/json"
}

smoke_response_summary() {
  local response_file="$1"
  python3 - "$response_file" <<'PY'
import json
import pathlib
import sys

path = pathlib.Path(sys.argv[1])
if not path.exists():
    print("<missing body>")
    raise SystemExit(0)

raw = path.read_text(encoding="utf-8", errors="replace").strip()
if not raw:
    print("<empty body>")
    raise SystemExit(0)

try:
    payload = json.loads(raw)
except Exception:
    normalized = " ".join(raw.split())
    print(normalized[:400])
else:
    compact = json.dumps(payload, ensure_ascii=True, separators=(",", ":"))
    print(compact[:400])
PY
}

status_matches() {
  local expected_statuses="$1"
  local actual_status="$2"
  local expected_status=""
  IFS=',' read -r -a status_list <<<"${expected_statuses}"
  for expected_status in "${status_list[@]}"; do
    if [[ "${actual_status}" == "${expected_status}" ]]; then
      return 0
    fi
  done
  return 1
}

smoke_assert_request() {
  local method="$1"
  local url="$2"
  local expected_statuses="$3"
  local body="$4"
  shift 4

  local response_file
  local status_code
  response_file="$(mktemp /tmp/sqlforge-smoke-response.XXXXXX)"

  if [[ -n "${body}" ]]; then
    status_code="$(curl -sS -o "${response_file}" -w '%{http_code}' -X "${method}" "$@" --data "${body}" "${url}" || true)"
  else
    status_code="$(curl -sS -o "${response_file}" -w '%{http_code}' -X "${method}" "$@" "${url}" || true)"
  fi

  if ! status_matches "${expected_statuses}" "${status_code}"; then
    echo "Smoke HTTP request failed: method=${method} url=${url} expected=${expected_statuses} actual=${status_code}" >&2
    echo "Response summary: $(smoke_response_summary "${response_file}")" >&2
    rm -f "${response_file}"
    exit 1
  fi

  cat "${response_file}"
  rm -f "${response_file}"
}

assert_post_json() {
  local url="$1"
  local body="$2"
  shift 2
  smoke_assert_request "POST" "${url}" "200" "${body}" "$@"
}

assert_get_json() {
  local url="$1"
  shift
  smoke_assert_request "GET" "${url}" "200" "" "$@"
}

assert_http_status() {
  local method="$1"
  local url="$2"
  local expected_statuses="$3"
  shift 3
  smoke_assert_request "${method}" "${url}" "${expected_statuses}" "" "$@"
}

json_assert() {
  local json_payload="$1"
  local python_expr="$2"
  JSON_PAYLOAD="${json_payload}" PYTHON_EXPR="${python_expr}" python3 - <<'PY'
import json
import os

payload = json.loads(os.environ["JSON_PAYLOAD"])
expression = os.environ["PYTHON_EXPR"]
if not eval(expression, {"payload": payload}):
    raise SystemExit(1)
PY
}

json_extract() {
  local json_payload="$1"
  local python_expr="$2"
  JSON_PAYLOAD="${json_payload}" PYTHON_EXPR="${python_expr}" python3 - <<'PY'
import json
import os

payload = json.loads(os.environ["JSON_PAYLOAD"])
expression = os.environ["PYTHON_EXPR"]
value = eval(expression, {"payload": payload})
print("" if value is None else value)
PY
}
