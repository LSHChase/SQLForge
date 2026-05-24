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

### USER-CN-SYSTEM-MANAGEMENT-UX-REDESIGN-20260524: System management UX redesign

- Status: in_progress
- Priority: 1
- Depends on: N/A
- Scope: Frontend-only redesign of /system management workspace: preserve existing tabs, APIs, payload contracts, and data-testid coverage while improving hierarchy, toolbars, summaries, table states, and form validation.
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-SYSTEM-MANAGEMENT-UX-REDESIGN-20260524`
- Progress log:
  - 2026-05-24: instantiated from foreman CLI using repository truth and task matrices.

### USER-CN-CREATE-REAL-MV-20260524: 页面创建真实 MV

- Status: in_progress
- Priority: 1
- Depends on: USER-CN-REWRITE-VALIDATION-ACTIVATION-UX-20260521
- Scope: sql-optimization,query-execution,src/services/runtimeGateApi.js,src/views/recommendation-center/RecommendationCenterView.vue
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-CREATE-REAL-MV-20260524`
- Progress log:
  - 2026-05-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-24: 已补推荐详情页真实 MV 创建入口、确认动作、执行结果展示与 query-execution 创建链路；`python3 scripts/foreman.py validate USER-CN-CREATE-REAL-MV-20260524` 通过。

### USER-CN-TENANT-CONFIG-FRONTEND-UI-20260524: 实现租户与默认备用引擎前端统一维护入口

- Status: in_progress
- Priority: 1
- Depends on: USER-CN-TENANT-CONFIG-BACKEND-API-20260524
- Scope: src/App.vue,src/stores,src/services,src/locales
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-TENANT-CONFIG-FRONTEND-UI-20260524`
- Progress log:
  - 2026-05-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-24: 已在 App 顶部补租户、默认引擎、备用引擎统一维护控件；runtime API 与 Pinia tenant store 已接入 options/get/put 后端契约。

### USER-CN-TENANT-CONFIG-BACKEND-API-20260524: 实现租户引擎配置后端写接口与候选列表

- Status: in_progress
- Priority: 1
- Depends on: N/A
- Scope: governance
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-TENANT-CONFIG-BACKEND-API-20260524`
- Progress log:
  - 2026-05-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-24: 后端已新增租户配置候选列表与默认/备用引擎更新接口；目标 governance 测试通过。
  - 2026-05-24: foreman validate 受既有前端 `src/views/query/SqlQueryView.vue` 未使用 import 阻断全量 lint；未修改前端代码。


## In Review

_No tasks._


## Blocked

### USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-EXTERNAL-ARTIFACTS-20260518: 等待真实生产规模压测外部证据

- Status: blocked
- Priority: 1
- Depends on: USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-INGEST-20260518
- Scope: 等待外部生产或准生产环境 owner 提供真实压测 evidence directory 与通过校验后的 `verification-result.json`，覆盖来源 `provenance.json`、10000 并发、千万级日查询、30PB 数据布局、24 小时 replay、P95/P99、扫描字节、CPU、队列等待、成本账单和 `evidenceFileDigests`；在 artifacts 到位前不得把 repo-side verifier、runbook 或测试 fixtures 视为生产规模目标完成。
- Validation:
  - `python3 scripts/verify-benchmark-production-evidence.py --evidence-dir <external-evidence-dir> --output <external-evidence-dir>/verification-result.json`
  - `python3 scripts/audit-rewrite-production-readiness.py --verification-result <external-evidence-dir>/verification-result.json --evidence-dir <external-evidence-dir>`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-05-18: 仓库侧 `scaleTarget` 边界、生产证据 bundle verifier、证据目录 CLI 和 runbook 已提交；完成度审计仍未在仓库内发现真实外部生产 artifacts。
  - 2026-05-18: repo-side verifier/runbook 已补入 `daily-query-volume.json`，外部 artifacts 需要同时覆盖千万级日查询证明。
  - 2026-05-18: repo-side verifier/readiness audit 已要求顶层与 `scaleTargetEvidenceManifest` 同时保留 `evidenceFileDigests`，外部 artifacts 归档后必须可复算 SHA-256。
  - 2026-05-18: repo-side readiness audit 已要求同时传入原始 `--evidence-dir` 并复算必需证据文件 SHA-256/sizeBytes；仅提交 JSON 不足以完成目标。
  - 2026-05-18: repo-side verifier/readiness audit 与 benchmark manifest 已要求 `provenance.json` 和 `environmentId/environmentType/evidenceOwner/artifactArchiveRef/verifierOperator`；没有来源元数据时不得声明生产规模完成。
- Next action: 外部环境 owner 按 `docs/deployments/benchmark-production-evidence-runbook.md` 收集 evidence directory，运行 `python3 scripts/verify-benchmark-production-evidence.py --evidence-dir <external-evidence-dir> --output <external-evidence-dir>/verification-result.json` 和 `python3 scripts/audit-rewrite-production-readiness.py --verification-result <external-evidence-dir>/verification-result.json --evidence-dir <external-evidence-dir>`，并归档原始 artifacts、`provenance.json`、可复算 SHA-256 的 evidence directory、通过校验的 JSON 输出和含来源元数据与摘要的 `scaleTargetEvidenceManifest`。
- Escalation: 如果生产或准生产窗口、来源元数据、数据布局证明、长期 replay、指标导出、账单导出或原始文件摘要归档无法在目标周期提供，保持目标未完成并要求 owner 明确可执行窗口、证据归档位置和负责验收的人。
- Human decision: 确认可用于留证的生产或准生产环境、执行窗口、证据目录归档位置、成本账单来源、谁运行 verifier、谁复核 `provenance.json` 与 `evidenceFileDigests`，以及谁把通过后的含来源元数据和摘要的 `scaleTargetEvidenceManifest` 提交到 benchmark 任务。
- INBOX ref: INBOX-005

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
