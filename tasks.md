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

### D-TASK-066: 扩展 JDBC Agent `Governed Execute`

- Status: in_progress
- Priority: 1
- Depends on: `D-TASK-065`
- Scope: 通过平台 API 执行 SQL，并保留 fallback 语义 Tech: `JAVA-BE`,`OPS`. Layer: `common`,`deployments/ci/scripts`.
- Matrix context: Phase-D / Story `D-STORY-012` 开放接入与 JDBC Agent
- Human confirmation point: 若 JDBC Agent `Governed Execute` 会在平台不可用时无回退策略、或默认强制所有 SQL 走平台，需人工确认
- Data impact: Agent 执行模式、fallback 策略、平台调用链
- Rollback / recovery: 恢复租户/数据源级可切换边界和 fallback 语义
- Validation:
  - `governed-execute 测试`
  - `python3 scripts/foreman.py validate D-TASK-066`
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
