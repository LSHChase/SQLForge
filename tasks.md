# SQLForge Task Ledger

本文件是当前待办与执行中任务的唯一轻量运行台账。

- 状态只允许：`todo`、`in_progress`、`in_review`、`blocked`
- `done` 任务必须移入 `tasks-done.md`
- `blocked` 任务必须包含：`Next action:`、`Escalation:`、`Human decision:`、`INBOX ref:`
- `in_review` 任务必须包含：`Review reason:`、`Human decision:`、`INBOX ref:`
- `todo` / `in_progress` 任务不得保留未决人工判断、升级链字段或显式等待人类处理的标记
- 任务台账不替代 `docs/plans/master-execution-plan.md`

## Todo

### HARN-140: SQL 历史改写记录 tab 与筛选

- Status: todo
- Priority: 1
- Depends on: `HARN-134`, `HARN-110`
- Scope: SQL 历史列表新增改写记录筛选，详情新增改写记录 tab、diff 跳转和验证状态展示；保留分页、默认筛选、脱敏和原始 SQL 展示契约。
- Validation:
  - `python3 scripts/foreman.py validate HARN-140`

### HARN-141: 监控与告警前端联动

- Status: todo
- Priority: 1
- Depends on: `HARN-136`, `HARN-138`, `HARN-140`
- Scope: 在工作台、推荐中心、SQL 历史展示 validation status、告警入口和自动暂停证据；不得把模拟通知写成真实通知成功。
- Validation:
  - `python3 scripts/foreman.py validate HARN-141`

### HARN-142: 加速与改写治理端到端 smoke 与文档收口

- Status: todo
- Priority: 1
- Depends on: `HARN-141`
- Scope: 补齐 repo-closed smoke、runbook、契约检查脚本、文档同步与残余风险收口；真实 Hetu/MRS 证据仍归 HARN-016 / INBOX-002，不作为默认阻断。
- Validation:
  - `python3 scripts/foreman.py validate HARN-142`


## In Progress

_No tasks._


## Blocked

### HARN-016: Track deferred external Hetu/MRS validation

- Status: blocked
- Priority: 1
- Depends on: `D-TASK-017`, `D-TASK-018`, `HARN-013`, `HARN-014`
- Scope: Record that external Win10 test-environment Hetu/MRS validation is deferred while repository-side implementation proceeds, and wire the pending follow-up into tasks/INBOX/plan audit chain without changing business code.
- Validation:
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: converted the environment-backed Hetu/MRS verification wait into an explicit blocked governance follow-up so repository-side implementation can continue without treating external test-environment latency as an active coding blocker.
  - 2026-05-08: HARN-088 added repository-side Hetu EXPLAIN plan-analysis support; live JDBC evidence for real Hetu/MRS credentials remains part of this blocked environment-backed validation chain.
- Next action: When the Win10 test environment is ready, deploy the yml-based governance/query-execution configuration from the runbook, run `bash scripts/run-hetu-env-smoke.sh` for one of `JDBC` / `REST` / `CLIENT`, and archive the returned log/response proof outside the repository.
- Escalation: If the external environment remains unavailable or credentials/connectivity are still uncertain after the deployment window opens, keep repository implementation moving and ask the environment owner to provide the executable window, reachable Hetu/MRS endpoint, and evidence retention location.
- Human decision: Confirm the deployment window, final Hetu mode, target datasource credentials, and who will archive the live smoke evidence in the real test environment.
- INBOX ref: INBOX-002
