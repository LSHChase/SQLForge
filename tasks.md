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

_No tasks._


## Blocked

### USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-EXTERNAL-ARTIFACTS-20260518: 等待真实生产规模压测外部证据

- Status: blocked
- Priority: 1
- Depends on: USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-INGEST-20260518
- Scope: 等待外部生产或准生产环境 owner 提供真实压测 evidence directory 与通过校验后的 `verification-result.json`，覆盖 10000 并发、30PB 数据布局、24 小时 replay、P95/P99、扫描字节、CPU、队列等待和成本账单；在 artifacts 到位前不得把 repo-side verifier、runbook 或测试 fixtures 视为生产规模目标完成。
- Validation:
  - `python3 scripts/verify-benchmark-production-evidence.py --evidence-dir <external-evidence-dir> --output <external-evidence-dir>/verification-result.json`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-05-18: 仓库侧 `scaleTarget` 边界、生产证据 bundle verifier、证据目录 CLI 和 runbook 已提交；完成度审计仍未在仓库内发现真实外部生产 artifacts。
- Next action: 外部环境 owner 按 `docs/deployments/benchmark-production-evidence-runbook.md` 收集 evidence directory，运行 `python3 scripts/verify-benchmark-production-evidence.py --evidence-dir <external-evidence-dir> --output <external-evidence-dir>/verification-result.json`，并归档原始 artifacts 与通过校验的 JSON 输出。
- Escalation: 如果生产或准生产窗口、数据布局证明、长期 replay、指标导出或账单导出无法在目标周期提供，保持目标未完成并要求 owner 明确可执行窗口、证据归档位置和负责验收的人。
- Human decision: 确认可用于留证的生产或准生产环境、执行窗口、证据目录归档位置、成本账单来源、谁运行 verifier，以及谁把通过后的 `scaleTargetEvidenceManifest` 提交到 benchmark 任务。
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
