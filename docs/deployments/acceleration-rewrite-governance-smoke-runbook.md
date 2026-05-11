# Acceleration Rewrite Governance Smoke Runbook

本文是 `HARN-142` 的 repo-closed smoke 与文档收口入口，覆盖加速治理工作台、推荐中心 SQL diff、SQL 历史改写记录和告警联动的仓库内可复跑验证。

## Scope

- 默认入口：`npm run smoke:acceleration-governance`
- 等价 shell 入口：`bash scripts/run-acceleration-rewrite-governance-smoke.sh`
- 静态快速入口：`bash scripts/run-acceleration-rewrite-governance-smoke.sh --skip-browser`
- 本 runbook 只证明 repo-closed 前端契约、页面治理和 mocked API browser smoke 闭环。
- 真实 Hetu / MRS EXPLAIN、扫描量、P99、物化视图收益和外部装数证据仍归 `HARN-016` / `INBOX-002`，属于 environment-backed evidence and not a default blocker for HARN-142。

## What The Smoke Runs

`npm run smoke:acceleration-governance` 串联以下检查：

- `node scripts/check-acceleration-rewrite-governance-closeout.mjs`
- `node scripts/check-acceleration-workbench-contract.mjs`
- `node scripts/check-recommendation-page-contract.mjs`
- `node scripts/check-history-page-contract.mjs`
- `node scripts/check-alert-page-contract.mjs`
- `npm run test:frontend-page-governance`
- `npm run smoke:acceleration-workbench`

浏览器 smoke 会临时启动 Vite dev server，并在 Playwright 中 mock `/api/*`。它会覆盖候选创建、推荐任务、SQL diff、改写记录、计划提交/审批/应用/验证/回滚、基线与加速查询、历史改写记录、validation run 和 `SQL_REWRITE_RESULT_DIVERGENCE` 自动暂停证据。

## Expected Evidence

- 命令输出包含 `acceleration rewrite governance closeout ok`
- 命令输出包含 `acceleration workbench contract ok`
- 命令输出包含 `recommendation page contract ok`
- 命令输出包含 `alert page contract ok`
- 命令输出包含 `Acceleration workbench browser smoke passed`
- `python3 scripts/foreman.py validate HARN-142` 会把验证记录写入 `docs/quality/validation-log.md`

## Failure Handling

- closeout contract 失败：先修正 smoke 入口、runbook 或文档索引，不要跳过该脚本。
- 页面契约失败：按报错 token 回到对应 Vue/API/route 文件修复，避免把缺失能力写成已验证。
- browser smoke 失败：优先检查 Playwright/Chrome 可用性、Vite dev server 启动和 mocked endpoint 路径漂移。
- 若失败原因是真实 Hetu / MRS 环境不可用，不应阻断 HARN-142；该证据继续保留在 `HARN-016` / `INBOX-002`。

## Boundary

本 runbook 不改变默认 CI、release gate 或 phase gate。若未来要把 `npm run smoke:acceleration-governance` 提升为默认 CI / release 阻断，必须通过新的任务和人工确认更新 `docs/deployments/ci-capability-baseline.md` 与阶段门禁文档。
