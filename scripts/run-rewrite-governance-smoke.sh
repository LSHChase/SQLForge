#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
RUN_BROWSER_SMOKE=true

usage() {
  cat <<'EOF'
Usage: bash scripts/run-rewrite-governance-smoke.sh [--skip-browser]

运行仓库闭环的改写治理 smoke，不包含已移除的旧流程模拟参考页。

Options:
  --skip-browser   只运行静态 closeout 与契约检查。
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-browser)
      RUN_BROWSER_SMOKE=false
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "未知参数: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

run_step() {
  local label="$1"
  shift
  printf '[rewrite-governance-smoke] %s\n' "${label}"
  "$@"
}

cd "${REPO_ROOT}"

run_step "closeout 契约" node scripts/check-rewrite-governance-closeout.mjs
run_step "推荐页面契约" node scripts/check-recommendation-page-contract.mjs
run_step "历史页面契约" node scripts/check-history-page-contract.mjs
run_step "告警页面契约" node scripts/check-alert-page-contract.mjs
run_step "前端页面治理" npm run test:frontend-page-governance

if [[ "${RUN_BROWSER_SMOKE}" == "true" ]]; then
  run_step "生产改写闭环浏览器 smoke" npm run smoke:production-rewrite-closed-loop
fi

printf '[rewrite-governance-smoke] 完成\n'
