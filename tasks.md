# SQLForge Task Ledger

本文件是当前待办与执行中任务的唯一轻量运行台账。

- 状态只允许：`todo`、`in_progress`、`in_review`、`blocked`
- `done` 任务必须移入 `tasks-done.md`
- `blocked` 任务必须包含：`Next action:`、`Escalation:`、`Human decision:`、`INBOX ref:`
- `in_review` 任务必须包含：`Review reason:`、`Human decision:`、`INBOX ref:`
- `todo` / `in_progress` 任务不得保留未决人工判断、升级链字段或显式等待人类处理的标记
- 任务台账不替代 `docs/plans/master-execution-plan.md`

## Todo

### HARN-132: 建立 SQL diff 后端服务

- Status: todo
- Priority: 1
- Depends on: `HARN-130`
- Scope: 提供文本 diff、规则级 diff、AST 摘要 diff 与 recommendation diff API，供推荐中心、加速治理工作台和 SQL 历史复用；diff 只做展示证据，不改写 SQL。
- Validation:
  - `python3 scripts/foreman.py validate HARN-132`

### HARN-133: 加速候选生成统一入口

- Status: todo
- Priority: 1
- Depends on: `HARN-129`, `HARN-132`
- Scope: 统一解析驱动与查询驱动 source normalization、candidate API、`sourceType/sourceKind/sourceId/evidenceLevel` 追溯键校验和 static/runtime evidence 分层。
- Validation:
  - `python3 scripts/foreman.py validate HARN-133`

### HARN-134: 改写记录写入与 SQL 历史聚合接口

- Status: todo
- Priority: 1
- Depends on: `HARN-129`, `HARN-133`
- Scope: 写入 `sql_rewrite_record`，并提供 `GET /api/governance/query-history/{historyId}/rewrite-records` 聚合面，使 SQL 历史详情能看到改写记录、diff、验证状态和告警引用。
- Validation:
  - `python3 scripts/foreman.py validate HARN-134`

### HARN-135: 周期比对执行模型与只读比较引擎

- Status: todo
- Priority: 1
- Depends on: `HARN-134`
- Scope: 建立 validation policy、result digest、schema/row/hash/checksum comparison；比对必须只读，不把大结果集全量拉回前端。
- Validation:
  - `python3 scripts/foreman.py validate HARN-135`

### HARN-136: 周期比对调度与差异告警

- Status: todo
- Priority: 1
- Depends on: `HARN-135`, `F-TASK-037`
- Scope: 落地 scheduled validation、自动暂停应用、`SQL_REWRITE_RESULT_DIVERGENCE` 告警联动和审计追溯；不得自动回滚生产配置。
- Validation:
  - `python3 scripts/foreman.py validate HARN-136`

### HARN-137: 前端加速治理工作台壳层

- Status: todo
- Priority: 1
- Depends on: `HARN-133`, `HARN-116`
- Scope: 建立加速治理工作台双入口、流程图、source fields、已有页面跳转和证据抽屉；不重复已有解析/推荐/SQL 历史完整页面。
- Validation:
  - `python3 scripts/foreman.py validate HARN-137`

### HARN-138: 工作台候选、计划审批与应用验证 tabs

- Status: todo
- Priority: 1
- Depends on: `HARN-137`, `HARN-134`
- Scope: 接入候选建议、SQL 差异、计划审批、应用验证、接口证据 tabs 与真实接口按钮；未实现接口必须显式显示未实现，不得 mock 成功。
- Validation:
  - `python3 scripts/foreman.py validate HARN-138`

### HARN-139: 推荐中心 SQL diff 与规则详情

- Status: todo
- Priority: 1
- Depends on: `HARN-132`, `HARN-114`
- Scope: 推荐详情接入 diff 视图、ruleChain、risk、precondition、unappliedRules 和人工复核标识；前后 SQL 必须能对比差异。
- Validation:
  - `python3 scripts/foreman.py validate HARN-139`

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
