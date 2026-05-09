# SQLForge Task Ledger

本文件是当前待办与执行中任务的唯一轻量运行台账。

- 状态只允许：`todo`、`in_progress`、`in_review`、`blocked`
- `done` 任务必须移入 `tasks-done.md`
- `blocked` 任务必须包含：`Next action:`、`Escalation:`、`Human decision:`、`INBOX ref:`
- `in_review` 任务必须包含：`Review reason:`、`Human decision:`、`INBOX ref:`
- `todo` / `in_progress` 任务不得保留未决人工判断、升级链字段或显式等待人类处理的标记
- 任务台账不替代 `docs/plans/master-execution-plan.md`

## Todo

### HARN-109: 重构 Dashboard、Delivery、Runtime、Recovery 概览类页面

- Status: todo
- Priority: 2
- Depends on: HARN-108
- Scope: 覆盖 `DashboardView`、`DeliveryProgressView`、`RuntimeGatesView`、`RecoveryDrillView`；统一 KPI、风险、活动流、静态证据区布局，保留 sample/window/session/PULL_ONLY 边界，不伪造全局事实。
- Validation:
  - `npm run lint`
  - `npm run build`
  - `npm run smoke:frontend-dev`

### HARN-110: 重构 SQL 查询与 SQL 历史页面结构

- Status: todo
- Priority: 1
- Depends on: HARN-108
- Scope: 覆盖 `SqlQueryView`、`SqlHistoryView`、`useSqlHistoryList.js`；拆分查询三栏、结果 tabs、历史筛选、详情抽屉和 SQL 三态展示，保留 SQL UI 契约和历史查询契约。
- Validation:
  - `npm run lint`
  - `npm run build`
  - `npm run test:sql-ui-contract`
  - `npm run test:frontend-page-governance`

### HARN-111: 拆分解析工作台与解析统计页面

- Status: todo
- Priority: 1
- Depends on: HARN-110
- Scope: 覆盖 `AccelerationView`、`ParseStatisticsCenterView`；拆分单条 SQL 输入、结构解析、access parse、结论、统计入口、字段 help 和详情弹层，不改变 parser/API/payload。
- Validation:
  - `npm run lint`
  - `npm run build`
  - `npm run test:sql-ui-contract`
  - `npm run test:frontend-page-governance`

### HARN-112: 拆分批量解析中心页面

- Status: todo
- Priority: 1
- Depends on: HARN-111
- Scope: 覆盖 `ParseBatchCenterView`；拆分普通批量、报表导入、统计标签、详情弹窗、失败详情和 SQL 展示；保留 summary-first、有限明细预览、失败记录可点击详情和大批量渲染约束。
- Validation:
  - `npm run lint`
  - `npm run build`
  - `npm run test:sql-ui-contract`
  - `npm run test:frontend-page-governance`

### HARN-113: 拆分解析历史查询与报表详情页面

- Status: todo
- Priority: 1
- Depends on: HARN-112
- Scope: 覆盖 `ParseRecordView`；拆分筛选、SQL 解析记录、批量/报表历史、详情弹层、报表统计、issue-scene detail 和原始 SQL 展示；保留默认空筛选与 raw SQL 不自动格式化契约。
- Validation:
  - `npm run lint`
  - `npm run build`
  - `npm run test:form-governance`
  - `npm run test:sql-ui-contract`
  - `npm run test:frontend-page-governance`

### HARN-114: 重构资产、路由、推荐、压测、接入页面

- Status: todo
- Priority: 2
- Depends on: HARN-108
- Scope: 覆盖 `AssetCatalogView`、`RoutingGovernanceView`、`RecommendationCenterView`、`BenchmarkView`、`AccessCenterView`；统一列表/详情/证据/placeholder 语义，保留只读证据边界和缺失写 API 的显式边界。
- Validation:
  - `npm run lint`
  - `npm run build`
  - `npm run test:sql-ui-contract`
  - `npm run test:frontend-page-governance`

### HARN-115: 重构告警、取证、修复、故障处置与系统管理页面

- Status: todo
- Priority: 2
- Depends on: HARN-108
- Scope: 覆盖 `AlertCenterView`、`AuditForensicsView`、`AuditTroubleshootingView`、`RepairEvidenceView`、`SystemView`；收敛重复 trace lookup、timeline、queue、retry、datasource/config 表格与详情模式，保留权限和后端权威边界。
- Validation:
  - `npm run lint`
  - `npm run build`
  - `npm run test:form-governance`
  - `npm run test:frontend-page-governance`

### HARN-116: 加固前端页面治理脚本与设计文档

- Status: todo
- Priority: 2
- Depends on: HARN-109,HARN-110,HARN-111,HARN-112,HARN-113,HARN-114,HARN-115
- Scope: 扩展 `check-frontend-page-governance.mjs`，补充 layout/i18n/card nesting/SQL component/page shell 检查；更新 `docs/frontend/design-system.md` 与相关前端治理文档，防止重构后回退。
- Validation:
  - `npm run lint`
  - `npm run build`
  - `npm run test:frontend-page-governance`
  - `node scripts/lint-repository-knowledge.js`


## In Progress

### HARN-096: 修复问题场景详情报表展示不全

- Status: in_progress
- Priority: 1
- Depends on: N/A
- Scope: Fix parse history report import detail issue-scene drawer so reportDetails are rendered completely in the report-level statistics detail, keep SQL detail pagination unchanged, and add a static contract guard preventing reportDetails slice truncation from returning.
- Validation:
  - `python3 scripts/foreman.py validate HARN-096`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.


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
