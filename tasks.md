# SQLForge Task Ledger

本文件是当前待办与执行中任务的唯一轻量运行台账。

- 状态只允许：`todo`、`in_progress`、`in_review`、`blocked`
- `done` 任务必须移入 `tasks-done.md`
- `blocked` 任务必须包含：`Next action:`、`Escalation:`、`Human decision:`、`INBOX ref:`
- `in_review` 任务必须包含：`Review reason:`、`Human decision:`、`INBOX ref:`
- `todo` / `in_progress` 任务不得保留未决人工判断、升级链字段或显式等待人类处理的标记
- 任务台账不替代 `docs/plans/master-execution-plan.md`

## Todo

_No tasks._


## In Progress

### E-TASK-034: 重构SQL查询与解析中心交互工作流

- Status: in_progress
- Priority: 1
- Depends on: E-TASK-033,E-TASK-022
- Scope: 重构 SQL 查询、解析工作台、批量解析中心与解析结果中心，改为查询条件+结果区+必要弹窗/抽屉模式，补齐单条/多条输入和 drill-through 交互。 Tech: VUE-FE. Layer: frontend/router/views/styles.
- Validation:
  - `python3 scripts/foreman.py validate E-TASK-034`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.

### E-TASK-035: 收口治理管理与开放接入页面体验

- Status: in_progress
- Priority: 1
- Depends on: E-TASK-034,E-TASK-032
- Scope: 重构 Dashboard、告警中心、路由治理、推荐中心、压测中心、系统管理与开放接入页面，去除无关信息与卡片堆叠，改为概览+列表/表格+抽屉/弹窗的治理工作台模式。 Tech: VUE-FE. Layer: frontend/router/views/styles.
- Validation:
  - `python3 scripts/foreman.py validate E-TASK-035`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.


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
- Next action: When the Win10 test environment is ready, deploy the yml-based governance/query-execution configuration from the runbook, run `bash scripts/run-hetu-env-smoke.sh` for one of `JDBC` / `REST` / `CLIENT`, and archive the returned log/response proof outside the repository.
- Escalation: If the external environment remains unavailable or credentials/connectivity are still uncertain after the deployment window opens, keep repository implementation moving and ask the environment owner to provide the executable window, reachable Hetu/MRS endpoint, and evidence retention location.
- Human decision: Confirm the deployment window, final Hetu mode, target datasource credentials, and who will archive the live smoke evidence in the real test environment.
- INBOX ref: INBOX-002
