# SQLForge Completed Tasks

本文件只记录已完成、已验证、已归档的任务。

## Done

### F-TASK-009: 阶段交付回写闭环

- Status: done
- Completed at: 2026-04-21
- Commit subject: `docs(delivery): F-TASK-009 close phase-f delivery loop`
- Priority: 1
- Depends on: `F-TASK-008`
- Scope: 完成记录、commit、tag、回写闭环 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Matrix context: Phase-F / Story `F-STORY-003` 运维、审计与恢复
- Human confirmation point: 交付闭环若省略 commit/tag 回写需人工确认
- Data impact: 交付记录、git 元数据、验证日志
- Rollback / recovery: 补写交付记录和标签/提交元数据
- Validation:
  - `交付记录与 git 元数据一致`
  - `python3 scripts/foreman.py validate F-TASK-009`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 docs/deliveries/phase-f-story-003-ops-closeout.md，统一记录 Phase-F Story-003 下 F-TASK-007/008 的交付基线、F-TASK-009 的 delivery closeout 清单与 write-back 模板，并同步更新 docs 入口与文档覆盖矩阵，形成阶段交付闭环的文档落点。
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-009 --include-task-audit；node scripts/lint-repository-knowledge.js；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前 foreman 的 delivery-closeout 只负责打 tag 和追加 write-back，实际 write-back 仍会在仓库内留下后续元数据改动；此外 F-STORY-003 的交付闭环仍依赖人工维护 tag 命名策略和交付记录选择。
  - Next step: 执行 delivery-closeout，为本次 Phase-F Story-003 交付主提交打 tag，并把 tag/write-back 元数据回填到 docs/deliveries/phase-f-story-003-ops-closeout.md。

### F-TASK-008: 补齐备份恢复策略与演练记录模板

- Status: done
- Completed at: 2026-04-21
- Commit subject: `docs(deploy): F-TASK-008 add backup recovery baseline`
- Priority: 1
- Depends on: `F-TASK-007`
- Scope: 定义 RPO/RTO/演练记录模板 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Matrix context: Phase-F / Story `F-STORY-003` 运维、审计与恢复
- Human confirmation point: 备份恢复目标或演练频率变更需人工确认
- Data impact: 备份元数据、演练记录
- Rollback / recovery: 恢复旧模板并补录演练
- Validation:
  - 备份恢复模板可追溯
  - `python3 scripts/foreman.py validate F-TASK-008`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 docs/deployments/backup-recovery-baseline.md，把 MySQL、audit_log、export_record、kafka_message_queue、system_config 密文与密钥边界的备份对象、RPO/RTO、责任角色和恢复演练模板统一收口，并同步更新 docs 入口、华为云部署引用、文档真值基线与覆盖矩阵。
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-008 --include-task-audit；node scripts/lint-repository-knowledge.js；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前仓库仍未提供 MySQL/OBS 备份自动化脚本、密钥托管与轮换校验工具、以及恢复后自动 smoke 脚本；query-execution、sql-optimization、benchmark-engine 仍无独立持久化，因此服务级恢复更多依赖健康检查与治理元数据验收。
  - Next step: 进入 F-TASK-009 时，基于本备份恢复基线把演练记录、commit/tag、交付回写和 write-back 模板一起收口，形成阶段交付闭环。

### F-TASK-007: 补齐监控与日志规范落地清单

- Status: done
- Completed at: 2026-04-21
- Commit subject: `docs(deploy): F-TASK-007 add observability baseline checklist`
- Priority: 1
- Depends on: `Phase-D`
- Scope: 输出 logs/metrics/alerts 落地清单 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Matrix context: Phase-F / Story `F-STORY-003` 运维、审计与恢复
- Human confirmation point: 监控/日志采样策略显著削弱需人工确认
- Data impact: 日志、指标、告警配置
- Rollback / recovery: 恢复原监控规则
- Validation:
  - 清单完整、映射一致
  - `python3 scripts/foreman.py validate F-TASK-007`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-21: audited current observability facts across 4 backend modules, confirming shared `logback-spring.xml` baselines, shared actuator exposure (`health/info/metrics/prometheus`), governance audit fallback logs, and async task flow logs already exist in code/config.
  - 2026-04-21: created `docs/deployments/observability-baseline.md` to distinguish current implemented logs/metrics signals from still-missing business metrics, alert rules, and tracing/platform pipeline integrations, then wired the new authority doc into docs entrypoints and coverage tracking.
- Context closeout:
  - Completed scope: 新增 `docs/deployments/observability-baseline.md`，把 4 个后端服务当前已落地的日志、Actuator 暴露、审计/异步状态流信号统一收口为 logs/metrics/alerts 运维清单，并同步更新文档入口、华为云部署引用、文档真值基线与覆盖矩阵。
  - Validation evidence: `python3 scripts/foreman.py validate F-TASK-007 --include-task-audit`；`node scripts/lint-repository-knowledge.js`；`python3 scripts/task_audit.py --check --phase pre-closeout`；`python3 scripts/task_audit.py --check --phase post-closeout`。
  - Residual risk: 当前仓库仍未实现业务级 Micrometer 指标、仓库内 Prometheus/Alertmanager/Grafana 规则文件和统一 tracing/log pipeline 配置，业务告警仍需外部平台按本文档补位。
  - Next step: 进入 `F-TASK-008` 时，直接复用本 observability 基线中的审计、消息队列、导出与敏感数据观测项，补齐备份恢复策略和演练模板。

### D-TASK-014: 收口异步服务鉴权、占位执行与审计兜底

- Status: done
- Completed at: 2026-04-21
- Commit subject: `feat(runtime): D-TASK-014 secure async placeholders and audit fallback`
- Priority: 1
- Depends on: D-TASK-013
- Scope: 在 sql-optimization 与 benchmark-engine 落实 header-based 鉴权、租户隔离与异步占位执行，补 benchmark 原始报告数据查询，并为 governance 审计消息增加数据库队列兜底，同时收紧共享加密配置到显式密钥基线。
- Validation:
  - `python3 scripts/foreman.py validate D-TASK-014`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 在 `sql-optimization` 与 `benchmark-engine` 落实了 header-based 请求鉴权、租户隔离和异步占位执行器，新增 benchmark 原始报告数据查询接口，并为 `governance` 审计消息主路由失败补上数据库队列兜底，同时把共享敏感加密配置改为显式密钥基线。
  - Validation evidence: `python3 scripts/foreman.py validate D-TASK-014 --include-task-audit --extra-command "mvn -B -pl benchmark-engine -am test" --extra-command "mvn -B -pl sql-optimization -am test" --extra-command "mvn -B -pl governance -am test"`；`python3 scripts/task_audit.py --check --phase pre-closeout`；`python3 scripts/task_audit.py --check --phase post-closeout`。
  - Residual risk: benchmark/sql-optimization 仍然是本地占位执行链，尚未接入真实 worker、持久化队列与跨服务主动审计上报。
  - Next step: 后续若把这批能力固化进长期计划，需要把 D-TASK-014 补回主计划与任务矩阵，并继续把异步任务接到真实执行/回调链。

### HARN-009: Repair closeout archive boundaries and done-ledger structure checks

- Status: done
- Completed at: 2026-04-21
- Commit subject: `fix(codex): HARN-009 repair closeout boundary auditing`
- Priority: 1
- Depends on: HARN-008
- Scope: 只修两个治理缺口：`foreman closeout` 归档任务块时必须限制在当前台账 section 内，不得把后续 section heading 一并搬入 `tasks-done.md`；同时为 `task_audit` 增加 `tasks-done.md` 结构校验，明确禁止 `## In Review`、`## Blocked` 等 heading 混入完成台账正文。本批次允许同步修正当前已被污染的 `tasks-done.md` 结构，但不触碰任何业务模块脏改动。
- Validation:
  - `python3 -m py_compile scripts/foreman.py scripts/task_audit.py`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `python3 scripts/task_audit.py --check --phase post-closeout`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/foreman.py compile-governance --check`
- Progress log:
  - 2026-04-21: instantiated as a minimal governance repair batch after post-HARN-008 review found that `foreman closeout` could archive trailing section content into `tasks-done.md`, and `task_audit` failed to detect the resulting structural corruption.
  - 2026-04-21: restricted `extract_task_blocks_with_spans()` so closeout now stops at the next task header or the next section heading, which prevents `tasks.md` trailing section bodies from being archived into `tasks-done.md`.
  - 2026-04-21: added explicit `tasks-done.md` structural checks to `task_audit.py` for unexpected `##` headings and stray non-task content inside the done section, then repaired the already polluted `HARN-008` done block back to the intended shape.
- Context closeout:
  - Completed scope: Restricted foreman task-block archiving to the current ledger section, added explicit tasks-done structure validation for unexpected headings and stray non-task content, repaired the previously polluted HARN-008 done block, and wired exec-plan move plus coverage-matrix/policy recompilation so closeout stays structurally consistent.
  - Validation evidence: python3 -m py_compile scripts/foreman.py scripts/task_audit.py; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/task_audit.py --check --phase post-closeout; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance --check; python3 scripts/foreman.py validate HARN-009 --include-task-audit; targeted synthetic done-ledger corruption probe now fails with unexpected-heading and stray-content errors
  - Residual risk: The repair now blocks the concrete closeout-ledger corruption path and detects section-level contamination, but future ledger format expansions still need to stay aligned across foreman, task_audit, and document indexes.
  - Next step: Use the repaired closeout path as the default governance closeout flow, and if later extending ledger formats, update both archiving boundaries and structural audits in the same batch.

### HARN-008: Close the 6 remaining Codex governance runtime gaps

- Status: done
- Completed at: 2026-04-21
- Commit subject: `feat(codex): HARN-008 close the remaining governance runtime gaps`
- Priority: 1
- Depends on: HARN-007
- Scope: 只修复 HARN-007 严格复核遗留的 6 个治理/runtime 闭口点：hooks 输出协议与 hooks.json 组织、preflight 真实消费文档真值、instantiate 真实消费任务矩阵、closeout 与 delivery-closeout 真正落地、运行态 closeout 后清理、以及可信本地验证证据；不触碰当前业务模块脏改动。
- Validation:
  - `python3 -m py_compile scripts/foreman.py .codex/hooks/*.py`
  - `python3 scripts/validate_codex_runtime.py`
  - `python3 scripts/foreman.py compile-governance --check`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-21: instantiated as the strict-mode follow-up batch to close the 6 unresolved governance/runtime gaps left after HARN-007, with write scope restricted to docs, ledgers, `.codex/`, and repository governance scripts only.
  - 2026-04-21: replaced the hard-coded foreman scaffolding with document-aware preflight, matrix-aware instantiate, explicit-stage closeout, delivery-closeout tag/write-back semantics, dynamic governance compilation, and idle-state cleanup after closeout.
  - 2026-04-21: aligned `.codex/hooks.json` and repository-local hook handlers with the official Codex hook output contracts, including `hookSpecificOutput.additionalContext` for `UserPromptSubmit` and event-specific permission decisions for `PreToolUse` / `PermissionRequest`.
  - 2026-04-21: fixed `.codex/config.toml` so project-doc settings no longer sit under the `[features]` table, which had been breaking real `codex exec` startup.
  - 2026-04-21: added `scripts/validate_codex_runtime.py` to deterministically validate preflight, matrix instantiate, delivery-closeout dry-run, hook output shapes, stop gating, and a trusted local `codex exec --json` path.
  - 2026-04-21: reran `python3 scripts/foreman.py validate HARN-008 --include-task-audit --extra-command 'codex exec --json --sandbox read-only --skip-git-repo-check "Reply with OK only."'`; governance validation, runtime simulation, task audit, repository knowledge lint, compile-governance drift check, and real Codex CLI execution all passed.
- Context closeout:
  - Completed scope: Aligned repository-local Codex hooks with official event contracts, replaced the hard-coded foreman scaffolding with document-aware preflight and matrix-aware instantiate, implemented explicit-stage closeout and delivery-closeout semantics, fixed project-scoped Codex config loading, added deterministic runtime validation plus a real codex exec verification path, and cleaned the HARN exec-plan/archive wiring without touching business-module dirty changes.
  - Validation evidence: python3 scripts/foreman.py validate HARN-008 --include-task-audit --extra-command "codex exec --json --sandbox read-only --skip-git-repo-check \"Reply with OK only.\""; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance --check
  - Residual risk: Repository-local hooks still depend on trusted-project loading in the users Codex environment, so AGENTS.md plus foreman/task_audit/lint remain the mandatory fallback enforcement chain.
  - Next step: Use the hardened foreman closeout and delivery-closeout path for later governance and F-task work, and keep task-matrix / blueprint / hook contracts in sync as the repository evolves.

### HARN-007: Integrate Codex runtime with repository truth and closeout flow

- Status: done
- Priority: 1
- Depends on: HARN-006
- Completed at: 2026-04-21
- Commit subject: `feat(codex): HARN-007 wire runtime governance scaffolding`
- Scope: 在不丢失现有 `docs/`、任务台账、验证日志、closeout、Git 审计链和 F-task 语义的前提下，把 SQLForge 的文档真值体系接入 Codex 的执行前、执行中、执行后生命周期；本轮只改治理与接线层，不触碰当前业务模块中的脏工作树改动。
- Validation:
  - `python3 -m py_compile scripts/foreman.py .codex/hooks/*.py`
  - `python3 scripts/foreman.py compile-governance`
  - `python3 scripts/foreman.py compile-governance --check`
  - `python3 scripts/foreman.py preflight --task HARN-007 --task-class standard --prompt "Integrate Codex runtime with repository truth and closeout flow under strict mode."`
  - `python3 scripts/foreman.py validate HARN-007 --include-task-audit`
  - `python3 scripts/foreman.py sync-context --done-ready`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-21: instantiated `HARN-007` as the strict-mode governance task for Codex-docs runtime integration after confirming the root cause was “docs truth exists but Codex execution path is not directly wired to it”.
  - 2026-04-21: current repository contains unrelated dirty business-module edits under `benchmark-engine/`, `governance/`, `sql-optimization/`, and `sqlforge-shared/`; this task therefore stayed inside governance docs, Codex integration scaffolding, scripts, and repository knowledge checks.
  - 2026-04-21: wrote the active exec plan and the formal `codex-governance-integration-blueprint.md`, then replaced the root navigation-only `AGENTS.md` with a hard repository contract that points Codex back to the authority chain and standard action entrypoints.
  - 2026-04-21: added project-scoped `.codex/config.toml`, `.codex/hooks.json`, repository-local hook handlers, runtime state examples, and a tracked current-task schema so Codex can consume repo policy without promoting `.codex/` into a second source of truth.
  - 2026-04-21: implemented `scripts/foreman.py` as the unified governance entrypoint for `preflight`, `instantiate`, `sync-context`, `validate`, `audit`, `compile-governance`, `closeout`, and `delivery-closeout`; the closeout commands are intentionally bootstrap-gated pending a later hardening batch that can safely auto-scope staged files.
  - 2026-04-21: updated `docs/README.md`, `docs/plans/README.md`, `docs/operations/README.md`, `docs/operations/local-development.md`, `README.md`, and `docs/plans/document-coverage-matrix.md` so the new Codex integration artifacts are visible to both humans and machine checks.
  - 2026-04-21: expanded `scripts/lint-repository-knowledge.js` to require the new blueprint, `.codex` scaffolding, and `scripts/foreman.py`, fixed the resulting README / coverage-matrix gaps, recompiled `.codex/policy/*.json`, and reran governance checks until both knowledge lint and compile-governance drift checks passed.
- Context closeout:
  - Completed scope: Codex runtime governance scaffolding is now wired into the repository through a formal blueprint, hardened root `AGENTS.md`, project-scoped `.codex` config/hooks/state/schema, a unified `scripts/foreman.py` CLI, generated governance manifests, and updated documentation/knowledge-lint indexes.
  - Validation evidence: `python3 -m py_compile scripts/foreman.py .codex/hooks/*.py`; `python3 scripts/foreman.py compile-governance`; `python3 scripts/foreman.py compile-governance --check`; `python3 scripts/foreman.py preflight --task HARN-007 --task-class standard --prompt "Integrate Codex runtime with repository truth and closeout flow under strict mode."`; `python3 scripts/foreman.py validate HARN-007 --include-task-audit`; `python3 scripts/foreman.py sync-context --done-ready`; `python3 scripts/task_audit.py --check --phase pre-closeout`; `node scripts/lint-repository-knowledge.js`
  - Residual risk: repository-local `.codex` hooks still need live validation inside a trusted Codex project, and `closeout` / `delivery-closeout` remain intentionally bootstrap-gated until a later batch can safely automate archival + commit scoping without catching unrelated dirty files.
  - Next step: validate the new `.codex` layer inside a trusted Codex runtime, then implement file-scope-aware automatic closeout and delivery-closeout semantics as the next governance hardening batch.

### HARN-006: Restore escalation failure-disposition wording

- Status: done
- Priority: 1
- Depends on: HARN-005
- Completed at: 2026-04-21
- Commit subject: `docs(harness): HARN-006 harden escalation and inbox audit chain`
- Scope: 在已恢复人工决策升级链“失败处置”显式语义的基础上，继续收口本轮严格模式复核发现的 4 个剩余问题：修正 `HARN-006` 的状态/证据口径、补强机器可检查的人类决策升级约束、补齐 `INBOX.md` 双向追溯格式与校验，并把验证日志从“closeout 证据”改回准确的工作树审计快照口径。
- Validation:
  - `python3 -m py_compile scripts/task_audit.py`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-21: review confirmed the former `R-158-A` failure-disposition wording no longer existed as explicit text after merge, and the current `R-156` / `R-160` wording had weakened to broad task-ledger or Git repair language.
  - 2026-04-21: restored explicit escalation-chain failure disposition wording in `docs/quality/validation-rules.md`, `docs/rules/codex-rules.md`, and `docs/operations/human-collaboration.md`.
  - 2026-04-21: user instructed to directly repair all 4 strict-mode review findings, which resolved the earlier closeout-strategy decision point and returned `HARN-006` to active execution.
  - 2026-04-21: current task remains active, so its validation evidence must be recorded as working-tree audit snapshots rather than closeout evidence until archival and single-task commit are complete.
  - 2026-04-21: updated `tasks.md`, `INBOX.md`, `docs/rules/codex-rules.md`, `docs/quality/validation-rules.md`, `docs/operations/human-collaboration.md`, `docs/operations/foreman-workflow.md`, and `scripts/task_audit.py` so unresolved human-decision markers are forbidden in `todo` / `in_progress`, `blocked` / `in_review` require `INBOX ref:`, and `INBOX` now has an explicit dual-traceability format.
  - 2026-04-21: reran `python3 -m py_compile scripts/task_audit.py`, `python3 scripts/task_audit.py --check --phase pre-closeout`, and `node scripts/lint-repository-knowledge.js`; current evidence is recorded as working-tree validation snapshots, not closeout evidence.
  - 2026-04-21: refined `task_audit.py` so `todo` / `in_progress` only fail on structured unresolved-human-decision fields, not on narrative text that merely mentions field names; reran py-compile, task audit, and repository knowledge lint after the fix.
  - 2026-04-21: tightened `task_audit.py` again so `blocked` / `in_review` now require structured field lines instead of loose substring matches, and `INBOX.md` entries are machine-checked for `Status:` / `Needed decision:` plus `Task refs:` / `Plan refs:` presence and task-id back references when a task uses `INBOX ref:`.
  - 2026-04-21: aligned `INBOX.md`, `docs/rules/codex-rules.md`, `docs/quality/validation-rules.md`, and `docs/operations/human-collaboration.md` so `Plan refs:` is allowed for plan-only items, but any task-linked `INBOX` item must list the task in `Task refs:`; reran py-compile, task audit, and repository knowledge lint after the alignment.
- Context closeout:
  - Completed scope: escalation failure-disposition wording, task-state upgrade rules, INBOX dual-traceability format, and machine-checkable escalation/inbox validation are now aligned across rule ledger, validation rules, operations docs, task ledger guidance, inbox guidance, and `task_audit.py`.
  - Validation evidence: `python3 -m py_compile scripts/task_audit.py`; `python3 scripts/task_audit.py --check --phase pre-closeout`; `node scripts/lint-repository-knowledge.js`
  - Residual risk: semantic judgment about whether a specific narrative truly requires human escalation still cannot be fully automated; current checks enforce structured markers and known waiting phrases rather than full natural-language intent.
  - Next step: run immediate post-closeout audit after this single-task commit and confirm the archived `HARN-006` commit subject is visible in Git history.

### HARN-005: Close remaining strict-mode review gaps

- Status: done
- Priority: 1
- Depends on: HARN-004
- Completed at: 2026-04-21
- Commit subject: `docs(harness): HARN-005 close strict-mode review gaps`
- Scope: 收口严格模式复核剩余 4 条缺口：为 `task_audit` 增加 pre/post closeout phase、补 `HARN-004` 的 post-closeout 证据、把 `tasks-done.md` 的“最新归档在上”排序规则显式写入规则与流程并形成校验、以及为 `blocked` / `in_review` 增加人类决策升级链的机器可检查字段要求。
- Validation:
  - `python3 -m py_compile scripts/task_audit.py`
  - `python3 scripts/task_audit.py --check --phase post-closeout`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-21: strict-mode full review confirmed 4 remaining gaps after `HARN-004`: missing `HARN-004` post-closeout evidence, no phase-specific `task_audit` entrypoint, implicit `tasks-done.md` ordering dependency, and no machine-checkable metadata for “must ask human” task states.
  - 2026-04-21: expanded `scripts/task_audit.py` with explicit `--phase pre-closeout|post-closeout`, newest-first done-ledger checks, and required `Human decision:` / `Review reason:` metadata for `blocked` and `in_review` tasks.
  - 2026-04-21: aligned `docs/rules/codex-rules.md`, `docs/operations/foreman-workflow.md`, `docs/operations/git-and-task-closeout.md`, `docs/operations/human-collaboration.md`, `docs/quality/validation-rules.md`, `tasks.md`, and `docs/quality/validation-log.md`, and repaired the legacy `HARN-002` / `HARN-001` archive ordering drift so the new checks consume a consistent ledger.
- Context closeout:
  - Completed scope: phase-aware closeout auditing, archived-task ordering governance, and human-decision escalation metadata are now formalized in rules, operations docs, task ledger guidance, and machine checks; `HARN-004` also gained formal post-closeout evidence backfill.
  - Validation evidence: `python3 -m py_compile scripts/task_audit.py`; `python3 scripts/task_audit.py --check --phase post-closeout`; `python3 scripts/task_audit.py --check --phase pre-closeout`; `node scripts/lint-repository-knowledge.js`
  - Residual risk: `R-165` now has stronger machine-checkable task-state enforcement, but semantic judgment about whether a specific ambiguity truly required escalation still cannot be fully automated.
  - Next step: continue future strict-mode automation only if higher-fidelity ambiguity detection can be added without creating noisy false positives.

### HARN-004: Tighten closeout audit boundary and post-closeout traceability

- Status: done
- Priority: 1
- Depends on: HARN-003
- Completed at: 2026-04-21
- Commit subject: `docs(harness): HARN-004 tighten closeout audit chain`
- Scope: 修复严格模式复核识别出的两个 `P0` 缺口与一个 `P1` 证据链缺口：收紧 `task_audit` 对 pending commit 的豁免边界，重排 closeout 流程使 `R-168` 在 pre-commit 审计前即可检查到当前归档任务，并补齐 post-closeout 可追溯要求的正式文档口径。
- Validation:
  - `python3 -m py_compile scripts/task_audit.py`
  - `python3 scripts/task_audit.py --check`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-21: strict-mode re-review confirmed that the previous pending-commit exception in `scripts/task_audit.py` was too broad because any same-day archived task could slip through when the worktree was dirty, which weakened `R-157` / `R-160`.
  - 2026-04-21: tightened `scripts/task_audit.py` so pre-commit closeout now allows at most one newest same-day archived task to be pending Git history, while all other `tasks-done.md` entries must remain fully traceable.
  - 2026-04-21: aligned `docs/operations/foreman-workflow.md`, `docs/operations/git-and-task-closeout.md`, and `docs/quality/validation-rules.md` so the current closeout task must already be archived into `tasks-done.md` with `Context closeout` before pre-commit audit, and post-commit recheck is now an explicit required step.
- Context closeout:
  - Completed scope: closeout workflow, validation-rule wording, and task-audit enforcement were tightened so `R-168` now covers the current archived task during pre-commit audit, and the pending-commit exception is limited to the single current closeout task.
  - Validation evidence: `python3 -m py_compile scripts/task_audit.py`; `python3 scripts/task_audit.py --check`; `node scripts/lint-repository-knowledge.js`
  - Residual risk: strict mode still cannot fully automate semantic judgment for all “must ask human” scenarios; the strengthened audit now covers closeout shape and Git traceability, not every ambiguity class.
  - Next step: run immediate post-commit recheck for this task and confirm the archived `HARN-004` commit subject is now visible in Git history.

### HARN-003: Close strict-mode and context-closeout governance loop

- Status: done
- Priority: 1
- Depends on: DOC-GOV-005
- Completed at: 2026-04-21
- Commit subject: `docs(harness): HARN-003 close strict-mode governance loop`
- Scope: 将严格模式与任务收尾的上下文收缩/清理要求从单点规则扩展为完整治理链，补齐规则入口、operations 流程、验证规则、验证日志、历史约束、任务归档与机器审计的一致口径，并明确 `/contract`、`/clear` 仅为环境可选手段而非唯一工程要求。
- Validation:
  - `python3 -m py_compile scripts/task_audit.py`
  - `python3 scripts/task_audit.py --check`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-21: aligned `R-168` consumption across `docs/README.md`, `docs/architecture/init.md`, `docs/operations/README.md`, `docs/operations/foreman-workflow.md`, `docs/operations/git-and-task-closeout.md`, `docs/operations/human-collaboration.md`, and `docs/plans/implementation-readiness.md` so strict mode and task closeout now use one consistent Harness Engineering wording.
  - 2026-04-21: updated `docs/quality/validation-rules.md`, `scripts/task_audit.py`, and `scripts/lint-repository-knowledge.js` so repository checks now cover `R-168` closeout markers and validation index continuity, then repaired `docs/quality/validation-log.md` to keep the new governance evidence append-only.
  - 2026-04-21: appended the human decision trail in `docs/references/human-constraint-history.md`, backfilled `Context closeout` sections for applicable completed tasks, and archived this governance batch for single-task git closeout.
- Context closeout:
  - Completed scope: strict-mode and context-closeout governance is now closed across rule definitions, operations guidance, validation linkage, machine audit, history ledger, and append-only evidence.
  - Validation evidence: `python3 -m py_compile scripts/task_audit.py`; `python3 scripts/task_audit.py --check`; `node scripts/lint-repository-knowledge.js`
  - Residual risk: `R-165` default strict mode仍主要依赖文档治理与人工执行纪律，仓库目前只对其关键 closeout 结果通过 `R-168` 做机器校验，而未对所有“需人工决策”场景做全自动语义判定。
  - Next step: 后续若要继续提高严格模式自动化强度，应在不引入伪阳性的前提下，为 `INBOX.md` / 任务日志中的人工确认链增加更细粒度的静态校验。

### D-TASK-013: 落实敏感字段加密

- Status: done
- Priority: 1
- Depends on: D-TASK-011
- Completed at: 2026-04-21
- Commit subject: `feat(governance): D-TASK-013 implement sensitive data encryption baseline`
- Scope: 在 Phase-D 已稳定的 `config/result/history/export/audit` 追溯链基础上，补共享 AES-256 敏感字段保护能力，并把 `governance` 内的 config snapshot、execution result、query history、export record、audit log 与 `system_config` 接到统一受保护持久化入口；优先收口密码、token、key 的密文存储，以及审计/导出相关字段的脱敏落库基线，不提前展开完整配置中心 UI、外部 KMS 或其他服务的真实上报改造。
- Validation:
  - 库、日志、导出无明文
  - `mvn -B -pl governance -am clean compile`
  - `mvn -B -pl governance -am test`
  - `mvn -B -pl governance -am validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-21: instantiated from `Phase-D / D-STORY-004` after D-TASK-012 stabilized the real audit write path, so the traceability chain could move from “schema exists” to “sensitive leaves are actually protected before persistence”.
  - 2026-04-21: added shared AES-256 GCM crypto and sensitive-key classification utilities under `sqlforge-shared`, so governance and later services can reuse one envelope format instead of ad hoc masking rules.
  - 2026-04-21: introduced `GovernanceProtectedPersistenceService` to protect `config_snapshot.snapshot_payload`, `execution_result.result_payload`, `query_history.query_context`, `export_record.export_options`, `audit_log.request_params/response_summary`, and raw SQL ciphertext writes before mapper persistence.
  - 2026-04-21: extended `system_config` with `sensitive_flag/value_ciphertext/value_mask/encryption_algorithm/encryption_key_id` and added `SystemConfigMapper`, so password/token/key style settings now have a stable ciphertext storage baseline instead of competing for the plain `config_value` column.
  - 2026-04-21: switched the existing governance audit write path onto the protected persistence service, added tests covering encrypted traceability leaves, masked audit/export fields, ciphertext SQL history payloads, and sensitive system-config storage, then synced persistence, security, capability, truth, init, repo-map, validation log, and task ledger documents.
- Context closeout:
  - Completed scope: shared AES-256 sensitive-field protection entered `sqlforge-shared`, governance traceability persistence paths now write ciphertext or masked payloads, and related authority docs were synchronized.
  - Validation evidence: `mvn -B -pl governance -am clean compile`; `mvn -B -pl governance -am test`; `mvn -B -pl governance -am validate pmd:pmd checkstyle:check`; `node scripts/check-frontend-backend-separation.js`; `node scripts/lint-repository-knowledge.js`; `python3 scripts/task_audit.py --check`
  - Residual risk: external KMS, other service consumers, and UI/admin management paths still remain follow-up work outside this task scope.
  - Next step: continue Phase-D traceability and security follow-up tasks on the now-protected persistence baseline instead of reopening plaintext storage semantics.

### D-TASK-012: 落实审计日志链路

- Status: done
- Priority: 1
- Depends on: D-TASK-011
- Completed at: 2026-04-21
- Commit subject: `feat(governance): D-TASK-012 implement audit persistence chain`
- Scope: 基于 D-TASK-011 已固化的 `config/result/history/export/audit` 追溯链，把 `governance` 内部 `POST /api/governance/internal/audit/write` 从“只发消息”升级为“真实写入 `audit_log` + 保留审计事件扩散”，同时把当前 header-based stateless auth 的鉴权建立/释放接入 `LOGIN` / `LOGOUT` 审计落库，并优先覆盖 SQL 操作、登录登出、权限变更三类事件的真实写入路径与校验。
- Validation:
  - `R-113` 场景测试
  - `mvn -B -pl governance -am clean compile`
  - `mvn -B -pl governance -am test`
  - `mvn -B -pl governance -am validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-21: instantiated from `Phase-D / D-STORY-004` after D-TASK-011 stabilized the core traceability tables and mapper baseline, so audit no longer had to stay as a message-only placeholder.
  - 2026-04-21: added `GovernanceAuditTrailService` and upgraded `/api/governance/internal/audit/write` to synchronously insert into `audit_log`, validate optional `configSnapshotId/resultId/historyId/exportId` references, and keep `governance.audit.event` fan-out semantics.
  - 2026-04-21: extended `AuditWriteRequest` / `AuditWriteResponse` so callers can attach `sagaId`, traceability foreign keys, and desensitized request/response summaries while receiving a stable `auditId` back from the real persistence path.
  - 2026-04-21: connected `AuthInterceptor` to real audit writes so the current header-based stateless auth baseline now records `LOGIN` / `LOGOUT` audit events and failed pre-auth attempts in `audit_log`.
  - 2026-04-21: added dedicated audit-chain tests covering SQL operation writes, permission-change writes, login/logout/failure auth writes, and missing traceability reference rejection, then synced interface baseline, capability map, persistence baseline, access-control spec, truth baseline, init summary, repo map, validation log, and task ledger.
- Context closeout:
  - Completed scope: governance audit write path now persists real `audit_log` records, auth events are written through the same chain, and the audit contract/document truth was synchronized.
  - Validation evidence: `R-113` scenarios; `mvn -B -pl governance -am clean compile`; `mvn -B -pl governance -am test`; `mvn -B -pl governance -am validate pmd:pmd checkstyle:check`; `node scripts/check-frontend-backend-separation.js`; `node scripts/lint-repository-knowledge.js`; `python3 scripts/task_audit.py --check`
  - Residual risk: cross-service callers are still limited to the current transitional skeleton, and broader audit retention/reporting tooling remains future work.
  - Next step: build on the real audit persistence chain for subsequent traceability, compliance, and governance-service hardening tasks.

### D-TASK-011: 补齐核心表与关联键设计

- Status: done
- Priority: 1
- Depends on: Phase-C
- Completed at: 2026-04-21
- Commit subject: `feat(governance): D-TASK-011 define core traceability schema and keys`
- Scope: 在 `governance` 侧先固化 Phase-D 的事务型元数据底座，补齐 `config_snapshot`、`execution_result`、`query_history`、`export_record` 和扩展 `audit_log` 的主外键与追溯链；同步提供 `init-schema.sql`、增量 migration、Entity / Mapper XML、持久化权威文档与最小映射检查测试，不提前接入真实业务写入链、队列消费或导出引擎。
- Validation:
  - schema/sql/entity 映射检查
  - `mvn -B -pl governance -am clean compile`
  - `mvn -B -pl governance -am test`
  - `mvn -B -pl governance -am validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-21: instantiated from `Phase-D / D-STORY-004` after query-execution、sql-optimization 和 benchmark-engine 的接口与模型骨架稳定，开始把 config/result/history/export/audit 的关系型底座一次固化。
  - 2026-04-21: extended `sql/init-schema.sql` with `config_snapshot`、`execution_result`、`query_history`、`export_record` and trace-aware `audit_log`, then added `sql/migrations/V20260421_011__core_traceability_chain.sql` as the compatible incremental DDL for published environments.
  - 2026-04-21: added governance traceability entities and MyBatis XML mappers so table names, key columns, and FK chain are now represented in Java persistence skeletons instead of living only in SQL comments.
  - 2026-04-21: created `docs/architecture/persistence.md` as the current authority for MySQL persistence, shared trace keys, migration policy, and schema-to-entity mapping, then synced README, truth baseline, coverage matrix, init summary, service capability map, and repo map.
  - 2026-04-21: added `TraceabilitySchemaMappingTest` to keep `init-schema.sql`, migration script, and mapper XML aligned before later tasks connect real repository writes, audit ingestion, export generation, and cross-service persistence flows.
- Context closeout:
  - Completed scope: Phase-D traceability schema, incremental migration, entities, XML mappers, mapping test, and persistence authority document were all established as the current relational baseline.
  - Validation evidence: schema/entity mapping checks; `mvn -B -pl governance -am clean compile`; `mvn -B -pl governance -am test`; `mvn -B -pl governance -am validate pmd:pmd checkstyle:check`; `node scripts/check-frontend-backend-separation.js`; `node scripts/lint-repository-knowledge.js`; `python3 scripts/task_audit.py --check`
  - Residual risk: real repository writes, export generation, and downstream cross-service persistence usage are still follow-up work on top of this schema baseline.
  - Next step: connect actual persistence chains and audit/export flows to the stabilized traceability tables instead of revisiting the schema contract.

### D-TASK-010: 实现压测报告查询接口

- Status: done
- Priority: 1
- Depends on: D-TASK-009
- Completed at: 2026-04-21
- Commit subject: `feat(benchmark-engine): D-TASK-010 add report query api skeleton`
- Scope: 在 `benchmark-engine` 中补 `GET /api/benchmark-engine/reports/{reportId}`，基于已落库的 placeholder `BenchmarkReport` 打通 JSON / PDF / HTML 三种查询形态；同步固化趋势图表、格式元数据、查询路径和原始数据下载占位路径，不提前接入真实导出引擎、文件存储或真实执行链路。
- Validation:
  - 报告 JSON/PDF/HTML/404/非法格式测试
  - `mvn -B -pl benchmark-engine -am clean compile`
  - `mvn -B -pl benchmark-engine -am test`
  - `mvn -B -pl benchmark-engine -am validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-21: instantiated from `Phase-D / D-STORY-003` after `D-TASK-009` stabilized placeholder task submit/poll flow and report persistence.
  - 2026-04-21: added `BenchmarkReportController` and `BenchmarkReportApplicationService` so `benchmark-engine` now exposes `GET /api/benchmark-engine/reports/{reportId}` with `format=JSON|PDF|HTML`.
  - 2026-04-21: upgraded `BenchmarkReportResponse` from model-only payload to report-query baseline by adding target engines, trend charts, available formats, report query path, and raw-data download placeholder path.
  - 2026-04-21: kept JSON as the structured default response, and rendered PDF / HTML as placeholder export bodies with stable content type and `Content-Disposition` metadata so later real templates can replace the renderer without reshaping the HTTP contract.
  - 2026-04-21: extended controller and service tests to cover JSON success, PDF success, HTML success, missing report 404, and invalid format 400, then synced README, interface baseline, capability map, truth baseline, C4, init summary, repo map, and validation log to `REPORT_QUERY_API_SKELETON`.
- Context closeout:
  - Completed scope: benchmark-engine now exposes stable report query skeletons for JSON/PDF/HTML and the report response contract includes format/query/download metadata for later real exporters.
  - Validation evidence: JSON/PDF/HTML/404/illegal-format tests; `mvn -B -pl benchmark-engine -am clean compile`; `mvn -B -pl benchmark-engine -am test`; `mvn -B -pl benchmark-engine -am validate pmd:pmd checkstyle:check`; `node scripts/check-frontend-backend-separation.js`; `node scripts/lint-repository-knowledge.js`; `python3 scripts/task_audit.py --check`
  - Residual risk: export rendering, file storage, and real benchmark execution/report generation are still placeholder follow-up work.
  - Next step: keep later benchmark tasks on the stable report-query HTTP contract without reshaping the current DTO/VO surface.

### D-TASK-009: 实现压测任务提交流程骨架

- Status: done
- Priority: 1
- Depends on: D-TASK-008
- Completed at: 2026-04-21
- Commit subject: `feat(benchmark-engine): D-TASK-009 add task submit and status api skeleton`
- Scope: 在 `benchmark-engine` 中补 `POST /api/benchmark-engine/tasks` 与 `GET /api/benchmark-engine/tasks/{taskId}`，用 in-memory placeholder repository 串通提交、轮询、失败路径和流程日志，同时把成功路径的占位报告落库，为后续 `D-TASK-010` 的报告查询接口保留稳定承载点，不提前接入真实调度、隔离执行或导出链路。
- Validation:
  - 提交/轮询/失败路径测试
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-21: instantiated from `Phase-D / D-STORY-003` after `D-TASK-008` stabilized the benchmark task/report model and contract baseline.
  - 2026-04-21: added `BenchmarkTaskController`, `BenchmarkTaskApplicationService`, repository contract, and `InMemoryBenchmarkTaskRepository` so `benchmark-engine` now exposes submit and polling HTTP skeletons on an independent carrier.
  - 2026-04-21: kept async semantics by returning a queued snapshot from `POST /api/benchmark-engine/tasks`, then running a deterministic placeholder lifecycle that can reach success or failure for stable poll-path tests.
  - 2026-04-21: enforced current isolation guardrails in the skeleton by rejecting `readonlyRequired=false` and `shadowEnvironmentMode=DISABLED`, while still preserving placeholder failure coverage via the explicit `FAIL_BENCHMARK` marker.
  - 2026-04-21: stored placeholder reports on successful runs so `D-TASK-010` can add report query HTTP contracts without reshaping the current task flow, then synced interface baseline, capability map, truth baseline, C4, init summary, README, repo map, and validation log to `ASYNC_TASK_API_SKELETON`.
- Context closeout:
  - Completed scope: benchmark-engine task submit/poll skeleton, placeholder lifecycle, isolation guardrails, and success-path report persistence baseline were all established on an independent carrier.
  - Validation evidence: submit/poll/failure-path tests; `mvn -B clean compile`; `mvn -B test`; `mvn -B validate pmd:pmd checkstyle:check`; `node scripts/check-frontend-backend-separation.js`; `node scripts/lint-repository-knowledge.js`; `python3 scripts/task_audit.py --check`
  - Residual risk: real scheduler, isolated execution workers, persistent storage, and export/report backends are still deferred follow-up work.
  - Next step: continue benchmark-engine delivery on the stabilized task submit/status contract and placeholder report carrier.

### D-TASK-008: 定义压测任务与报告模型

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `feat(benchmark-engine): D-TASK-008 define benchmark task and report model`
- Scope: 建立 `benchmark-engine` 独立模块骨架，固化 `BASELINE` / `COMPARISON` / `REGRESSION_GUARD` 三类压测任务实体、状态与阶段流转、阈值评估、影子环境/只读/脱敏约束，以及后续提交/查询接口复用的基础 DTO / VO 和报告模型，不提前接入真实调度、执行、持久化或导出链路。
- Validation:
  - 模型与架构文档一致
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-003` to establish the third independent backend carrier instead of leaving benchmark semantics only in target architecture documents.
  - 2026-04-20: added `benchmark-engine` as a standalone Maven module with Spring Boot bootstrap, multi-profile configuration, logging baseline, and `application` / `domain` / `infrastructure` / `config` package skeleton.
  - 2026-04-20: solidified benchmark task types, lifecycle statuses, task-type-specific phase flow, threshold model, engine metric snapshot model, report aggregate, and explicit isolation fields for readonly, shadow environment, and desensitization.
  - 2026-04-20: added baseline submit/status/report DTO / VO objects plus `BenchmarkTaskModelApplicationService` so later `D-TASK-009` / `D-TASK-010` can reuse stable model contracts without reshaping the domain again.
  - 2026-04-20: synced `service-interface-contract-baseline.md`, `service-capability-map.md`, `c4-overview.md`, `document-truth-baseline.md`, `document-gap-matrix.md`, `init.md`, `repo-map.md`, and `README.md` so the repository now treats `benchmark-engine` as current fact while keeping task submit flow, execution chain, and report query deferred.

### D-TASK-007: 输出优化建议结构

- Status: done
- Priority: 1
- Depends on: D-TASK-006
- Completed at: 2026-04-20
- Commit subject: `feat(sql-optimization): D-TASK-007 add structured optimization suggestion vo`
- Scope: 在 `sql-optimization` 中把 `OptimizationTaskStatusResponse` 的占位 `summary/error` 升级为正式的 `suggestion / failure` 结构，固化收益、成本、风险、工件和失败阶段输出，并让 `PARSE` / `REWRITE` / `ACCELERATION_SUGGESTION` 三类任务一次性对齐到统一响应模型。
- Validation:
  - 输出字段与文档一致
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-002` after `D-TASK-006` stabilized the async submit/poll API skeleton.
  - 2026-04-20: replaced the flat `summary/error` polling payload with structured `suggestion / failure` objects plus dedicated benefit, cost, risk, artifact, and failure VOs under the SQL optimization application contract.
  - 2026-04-20: kept the task aggregate and repository unchanged, and concentrated the behavioral change in `OptimizationTaskModelApplicationService` so each task type now emits distinct placeholder recommendation content while preserving the existing lifecycle semantics.
  - 2026-04-20: extended tests to assert rewrite success payload shape, failed placeholder payload shape, parse success suggestion structure, and failure risk metadata, then synced the interface baseline, capability map, truth baseline, and init summary to the new response contract.
  - 2026-04-20: reran compile, test, static-check, separation, knowledge-lint, and pre-closeout task audit, then archived the task for single-task git closeout.

### D-TASK-006: 实现任务提交与状态查询骨架

- Status: done
- Priority: 1
- Depends on: D-TASK-005
- Completed at: 2026-04-20
- Commit subject: `feat(sql-optimization): D-TASK-006 add task submit and status api skeleton`
- Scope: 在 `sql-optimization` 中补 `POST /api/sql-optimization/tasks` 与 `GET /api/sql-optimization/tasks/{taskId}`，用 in-memory placeholder repository 串通提交、轮询、失败路径和流程日志，同时保持当前实现停留在过渡骨架，不提前接入 MySQL、队列和真实回调。
- Validation:
  - 提交/轮询/失败路径测试
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-002` after `D-TASK-005` established the SQL optimization task model and contract baseline.
  - 2026-04-20: added `OptimizationTaskController`, `OptimizationTaskApplicationService`, repository contract, and `InMemoryOptimizationTaskRepository` so `sql-optimization` now exposes submit and polling HTTP skeletons on an independent carrier.
  - 2026-04-20: kept async semantics by returning a queued snapshot from `POST /api/sql-optimization/tasks`, then running a deterministic placeholder lifecycle that can reach success or failure for stable poll-path tests.
  - 2026-04-20: added integration tests for submit success, failed placeholder polling, not-found polling, invalid callback rejection, plus log-sampling tests for entry/state-change/end/exception traces.
  - 2026-04-20: synced interface baseline, capability map, truth baseline, C4, init summary, README, repo map, validation log, and task ledgers to the new `ASYNC_TASK_API_SKELETON` current fact.

### D-TASK-005: 固化异步优化任务模型

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `feat(sql-optimization): D-TASK-005 solidify async optimization task model`
- Scope: 建立 `sql-optimization` 独立模块骨架，固化 `parse` / `rewrite` / `acceleration suggestion` 三类异步优化任务实体、生命周期状态、类型感知阶段流转，以及后续提交/轮询接口复用的基础 DTO/VO 和错误码基线，不提前接入真实 HTTP 入口、持久化、队列或审批链。
- Validation:
  - `R-121` 四项检查通过
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-002` to start the SQL optimization service on an independent carrier instead of leaking async optimization concerns back into `governance` or `query-execution`.
  - 2026-04-20: added `sql-optimization` as a standalone Maven module with Spring Boot bootstrap, multi-profile configuration, logging baseline, and `application` / `domain` / `infrastructure` / `config` package skeleton.
  - 2026-04-20: solidified async task types, lifecycle statuses, task-type-specific phase flow, submission normalization, status-history records, and reusable submit/status contract objects plus shared SQL-optimization service code and error-code ownership.
  - 2026-04-20: synced `service-interface-contract-baseline.md`, `service-capability-map.md`, `c4-overview.md`, `document-truth-baseline.md`, `document-gap-matrix.md`, `init.md`, `repo-map.md`, and `README.md` so the repository now treats `sql-optimization` as current fact while still deferring HTTP entrypoints, persistence, callbacks, and suggestion payload details to follow-up tasks.
  - 2026-04-20: reran compile, test, static-check, separation, knowledge-lint, and pre-closeout task audit, then archived the task for single-task git closeout.

### OPS-NAME-001: 全仓模块与工程命名去 Service 化整改

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `refactor(repo): OPS-NAME-001 remove Service suffix from engineering names`
- Scope: 以严格模式对全仓执行模块/工程/运行时命名整改，统一把 `governance-service`、`query-execution-service`、`sqlforge-common` 及相关运行标签、脚本、规则、需求、流程、验证日志、历史台账收敛到无 `Service` 后缀的工程命名，同时保留后端 `application` 包域、`controller`/`service` 实际分层与 `*Service` 服务类型命名，不丢失既有章节/步骤/流程/历史内容，并建立旧名到新名的权威映射。
- Validation:
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated as the temporary highest-priority strict-mode naming remediation task; all feature work paused until repo-wide module, runtime, code, script, and document naming were synchronized.
  - 2026-04-20: renamed module directories, Maven modules, artifact ids, Spring application names, runtime labels, service-code constants, scripts, tests, rules, plans, validation logs, and historical ledgers from `*-service` / `sqlforge-common` engineering names to `governance` / `query-execution` / `sqlforge-shared`.
  - 2026-04-20: preserved backend layering semantics by keeping `application` as a package domain, `controller` and `service` as the actual inbound layers, and concrete service-layer types on `*Service` naming.
  - 2026-04-20: repaired the remaining wording tail by correcting the old-to-new mapping table and removing the last misleading `application service` layer phrasing, then reran repo-wide validation for single-task closeout.

### D-TASK-004: 增加异常回滚与运行日志

- Status: done
- Priority: 1
- Depends on: D-TASK-003
- Completed at: 2026-04-20
- Commit subject: `feat(query-execution): D-TASK-004 add rollback markers and flow logs`
- Scope: 给 `query-execution` 的最小同步执行闭环补入口/出口/异常/状态变更日志，并把 timeout/fallback 路径的本地回滚/补偿标记固化到当前响应模型与代码路径。
- Validation:
  - `R-123` 抽查通过
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-001` after `D-TASK-003` committed the minimal synchronous execution loop in commit `ea25220`.
  - 2026-04-20: this task stayed inside the existing synchronous skeleton and only added observability plus local recovery markers; it did not open real database execution, distributed rollback, or governance audit integration.
  - 2026-04-20: extended `QueryExecutionApplicationService` with entry/exit/exception/state-change logs keyed by SQL fingerprint, kept SQL text out of logs, and added timeout/fallback local recovery markers through `retryPath.resultStatus/localRecoveryMarker/localRecoveryAction`.
  - 2026-04-20: added regression tests covering success logging, timeout rollback marker, fallback compensation marker, and exception logging, then synced architecture/truth/interface docs to the new observable execution baseline.
  - 2026-04-20: reran compile, test, static-check, separation, knowledge-lint, and pre-closeout task audit, then archived the task for single-task git closeout.

### D-TASK-003: 实现最小同步执行闭环

- Status: done
- Priority: 1
- Depends on: D-TASK-002
- Completed at: 2026-04-20
- Commit subject: `feat(query-execution): D-TASK-003 implement minimal sync execution loop`
- Scope: 在 `query-execution` 中把 `/api/query-execution/queries/execute` 接到最小同步执行路径，保持只读优先和确定性输出，不放开任意 SQL 执行。
- Validation:
  - 正常/超时/失败/降级路径测试
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-001` after `D-TASK-002` committed the public DTO / VO / error-code contract baseline in commit `0624dd0`.
  - 2026-04-20: implementation stayed inside the query-execution boundary and introduced a deterministic synchronous skeleton with read-only SQL guard, controlled route/fallback decisions, and no arbitrary SQL execution capability.
  - 2026-04-20: replaced the pure contract placeholder service with `QueryExecutionApplicationService`, added read-only assessment/guard logic, a deterministic execution adapter, and route handling for success, timeout, rejected-risk, and fallback-degraded paths.
  - 2026-04-20: synced service-interface, truth-baseline, architecture overview, capability map, and init docs so the repository now treats minimal synchronous execution as current fact while keeping governance calls, real adapters, rollback, and runtime logs deferred to follow-up work.
  - 2026-04-20: reran compile, test, static-check, separation, knowledge-lint, and pre-closeout task audit, then archived the task for single-task git closeout.

### D-TASK-002: 定义联机查询接口 DTO/VO/错误码

- Status: done
- Priority: 1
- Depends on: D-TASK-001
- Completed at: 2026-04-20
- Commit subject: `feat(query-execution): D-TASK-002 define online query api contracts`
- Scope: 在 `query-execution` 中固化联机查询的请求/响应 DTO、错误码区间和最小 HTTP 契约入口，保持契约优先，不提前引入真实执行引擎闭环。
- Validation:
  - `R-121` 四项检查通过
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-001` after `D-TASK-001` established the independent `query-execution` module baseline.
  - 2026-04-20: this task remained contract-first and limited to DTO/VO, error-code ownership, controller/service contract shape, and baseline interface documentation; the synchronous execution loop remains reserved for `D-TASK-003`.
  - 2026-04-20: added `QueryExecutionController`, request/response DTO/VO models, query execution status and policy enums, a contract service skeleton in the `application` package domain, shared query-execution error codes/service code, and contract tests covering valid request, validation failure, and pipeline-not-ready fallback behavior.
  - 2026-04-20: synced `service-interface-contract-baseline.md`, reran compile, test, static-check, separation, knowledge-lint, and pre-closeout task audit, then archived the task for single-task git closeout.

### D-TASK-001: 固化查询执行服务边界

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `feat(query-execution): D-TASK-001 solidify query execution service boundary`
- Scope: 建立 `query-execution` 独立模块骨架，固化路由、执行控制、轻量解析、轻量改写和已批准加速配置应用的服务边界，不把治理、异步优化或压测主流程重新混入本服务。
- Validation:
  - 服务边界与 ADR 一致性检查
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-001` under explicit human direction after the final `Phase-C` audit cleanup commit `68f7bb1`.
  - 2026-04-20: `Phase-C` exit gate remains blocked by coverage threshold and missing Sonar environment configuration; this task proceeded by explicit human direction and did not rewrite that gate state.
  - 2026-04-20: added `query-execution` as an independent Maven module with `application` package-domain plus `domain`/`infrastructure`/`config` skeleton, immutable boundary definition, multi-profile configuration, and the boundary service class `QueryExecutionBoundaryApplicationService` plus unit test.
  - 2026-04-20: synced `service-capability-map.md`, `c4-overview.md`, `document-truth-baseline.md`, `document-gap-matrix.md`, `master-execution-plan.md`, `frontend-backend-separation-baseline.md`, `repo-map.md`, `README.md`, and `docs/README.md` so the new service carrier is treated as current fact instead of a pure target.
  - 2026-04-20: validation passed with compile, test, static-check, frontend-backend separation check, knowledge lint, and pre-closeout task audit, then the task was archived for single-task git closeout.

### C-TASK-008: 落实租户配置与访问占位能力

- Status: done
- Priority: 2
- Depends on: C-TASK-007
- Completed at: 2026-04-20
- Commit subject: `feat(governance): C-TASK-008 tighten tenant access placeholder policy`
- Scope: 保持 phase 0 最小租户校验闭环，继续把 `governance` 收敛为公共管理服务基线，不扩散到其他目标微服务职责。
- Validation:
  - `mvn clean compile`
  - `mvn test`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` after governance baseline hardening.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; `TenantAccessLogic`、`TenantConfigApplicationService` 和治理内部租户/数据源检查接口已经存在并通过当前测试链验证，但数据源授权仍是 placeholder，完整角色与资源矩阵仍待后续实现，见 `IMP-005`。
  - 2026-04-20: replaced the old non-empty datasource placeholder with a governance-local explicit placeholder policy under `governance.access-control.placeholder`, added tenant-config role gating and platform-admin override in `TenantConfigApplicationService`, and kept all logic inside `governance`.
  - 2026-04-20: validation passed with `mvn clean compile`, `mvn test`, `node scripts/lint-repository-knowledge.js`, and `python3 scripts/task_audit.py --check`, then the task was archived for single-task git closeout.

### C-TASK-009: 规划审计、数据源、调度扩展点

- Status: done
- Priority: 2
- Depends on: C-TASK-008
- Completed at: 2026-04-20
- Commit subject: `feat(governance): C-TASK-009 close governance extension contracts`
- Scope: 只补治理扩展契约和骨架，不提前塞入完整业务实现；保持审计、数据源和调度扩展点的接口、错误码和文档一致。
- Validation:
  - 接口文档和错误码一致
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` as the governance extension-contract follow-up task.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; internal governance contract endpoints for `tenant-scope`、`datasource-access`、`audit/write`、`schedule/extensions` 已存在且测试通过，但仍需把扩展点从当前骨架进一步收口到完整契约，见 `IMP-007`。
  - 2026-04-20: hardened `datasource-access/check`、`audit/write`、`schedule/extensions` contracts in `governance`, added explicit contract-stage / implementation-stage metadata, fixed audit required fields (`serviceCode`,`elapsedMs`,`sourceIp`,`userAgent`), and synced `service-interface-contract-baseline.md` plus `access-control-spec.md`.
  - 2026-04-20: validation passed with `mvn clean compile`, `mvn test`, `mvn validate pmd:pmd checkstyle:check`, and `node scripts/lint-repository-knowledge.js`; task remained `in_review` until the message-abstraction closeout under `C-TASK-005` was committed.
  - 2026-04-20: reran compile, test, static-check, knowledge-lint, and pre-closeout task audit after `C-TASK-005`, then archived the governance extension-contract task for single-task git closeout.

### E-TASK-009: 建立临时 AI 交付进度页路由与展示骨架

- Status: done
- Priority: 2
- Depends on: E-TASK-001, E-TASK-002, Phase-C
- Completed at: 2026-04-20
- Commit subject: `feat(frontend): E-TASK-009 complete temporary delivery progress page`
- Scope: 建立 `/delivery-progress` 临时只读页面，展示 AI 编码任务进度且与 `/dashboard` 分离；展示真值只来自 `tasks.md`、`tasks-done.md`、验证日志和执行计划派生快照。
- Validation:
  - `npm run build`
  - `npm run build -- --mode development --outDir dist-dev`
  - `npm run lint`
  - `python3 scripts/task_audit.py --check`
  - `node scripts/lint-repository-knowledge.js`
  - 非生产路由可达
  - 生产默认隐藏
- Progress log:
  - 2026-04-20: instantiated from `Phase-E / E-STORY-004` as the temporary AI delivery progress page task.
  - 2026-04-20: started early by explicit human direction while the repository active wave remains `Phase-C`; execution must preserve `R-166` boundaries and keep `/dashboard` as the official business homepage.
  - 2026-04-20: extended the temporary page with runtime-flag semantics, temporary/non-production navigation badges, and ledger-derived sections for recent changes, pending blockers, and dependency chains without introducing a parallel state source.
  - 2026-04-20: completed current implementation and validation scope for `E-STORY-004`, appended validation evidence, and archived the task after single-task git closeout.

### C-TASK-001: 盘点应抽取的公共能力

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `docs(governance): DOC-GOV-001 establish document truth baseline`
- Scope: 对照 `sqlforge-shared`、`governance` 与文档边界，产出当前应收敛到 common 的能力清单，明确哪些能力仍留在业务模块。
- Validation:
  - `mvn clean compile`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` as the first executable shared-foundation task.
  - 2026-04-20: `docs/plans/implementation-readiness.md` 与 `docs/architecture/service-capability-map.md` 已把 `sqlforge-shared` 应承载的公共能力和禁入边界显式盘点完成，并写入主计划和真值文档。
  - 2026-04-20: strict ledger reconciliation revalidated repository compile and knowledge lint, then archived the inventory task because its implementation and git-history evidence are both present.

### C-TASK-002: 建立 common 包结构

- Status: done
- Priority: 1
- Depends on: C-TASK-001
- Completed at: 2026-04-20
- Commit subject: `refactor(common): C-TASK-002 close shared package structure baseline`
- Scope: 按“领域目录 + 分层子目录”与公共层边界建立 `sqlforge-shared` 的目标包结构，不引入服务专属逻辑。
- Validation:
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` after shared-capability inventory.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; `sqlforge-shared` 已形成 `async`、`audit`、`config`、`constants`、`context`、`exception`、`log`、`utils` 包结构，且当前验证链 `mvn clean compile`、`mvn test`、`mvn validate pmd:pmd checkstyle:check`、`node scripts/lint-repository-knowledge.js` 已通过，但单任务 git closeout 仍缺失，暂不归档。
  - 2026-04-20: synced `document-truth-baseline.md` so the shared module no longer appears as a placeholder-only module, then archived the package-structure task for single-task git closeout.

### C-TASK-003: 迁移重复或散落能力

- Status: done
- Priority: 1
- Depends on: C-TASK-002
- Completed at: 2026-04-20
- Commit subject: `refactor(common): C-TASK-003 migrate scattered shared capabilities`
- Scope: 把共性能力迁移到 common，仅迁移共性能力，不破坏服务边界。
- Validation:
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` after common package structure settled.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; 旧 `com.company.common` 与治理服务内部重复 common 能力已被当前工作树迁移到 `com.company.sqlforge.common` 并由 `governance` 消费，当前验证链 `mvn clean compile`、`mvn test`、`mvn validate pmd:pmd checkstyle:check`、`node scripts/lint-repository-knowledge.js` 已通过，但单任务 git closeout 仍缺失，暂不归档。
  - 2026-04-20: removed the last tracked `com.company.common` sources from `sqlforge-shared` and updated `document-gap-matrix.md` so the shared-layer implementation no longer remains as an open gap.

### C-TASK-004: 对齐所有 application-*.yml 职责

- Status: done
- Priority: 2
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `refactor(R-144): sync kafka abstraction to compose, scripts, config, sql, docs, lint`
- Scope: 明确 dev/test/prod 配置职责，补齐 coverage 和 Sonar 执行入口所需的环境说明，不改变生产默认安全语义。
- Validation:
  - `bash scripts/run-coverage.sh --phase report-only`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` to align configuration responsibilities before deeper service hardening.
  - 2026-04-20: dev / main-test / test-resource / prod 的 `messaging.mode` 职责已分别固定为 `DATABASE` / `DATABASE` / `MOCK` / `KAFKA`，并同步到了本地部署和消息抽象文档。
  - 2026-04-20: strict ledger reconciliation reran `bash scripts/run-coverage.sh --phase report-only`, confirmed report generation and profile responsibility consistency, then archived the task.

### C-TASK-005: 固化消息抽象接口实现路线

- Status: done
- Priority: 2
- Depends on: C-TASK-004
- Completed at: 2026-04-20
- Commit subject: `feat(messaging): C-TASK-005 solidify messaging abstraction route`
- Scope: 统一消息接口、配置和实现切换，保持 `DATABASE` / `MOCK` / `KAFKA` 三种模式的边界与契约清晰。
- Validation:
  - mock/database/kafka 契约测试
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/lint-repository-knowledge.js`
  - `bash scripts/run-coverage.sh --phase report-only`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` after profile responsibilities were aligned.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; `MessageProducer` / `MessageConsumer` 抽象、`MessagingConfig` 路由、`Database` / `Mock` / `Kafka` 实现及对应测试已在当前工作树落地，当前验证链 `mvn clean compile`、`mvn test`、`mvn validate pmd:pmd checkstyle:check`、`node scripts/lint-repository-knowledge.js` 和 `bash scripts/run-coverage.sh --phase report-only` 已通过，但当前 Kafka 客户端接入尚未形成单任务 git closeout，暂不归档。
  - 2026-04-20: reran compile, test, static-check, knowledge-lint, coverage, and pre-closeout task audit evidence, then archived the messaging abstraction task for single-task git closeout.

### C-TASK-006: 完成消息流管理接口验证

- Status: done
- Priority: 2
- Depends on: C-TASK-005
- Completed at: 2026-04-20
- Commit subject: `feat(R-144): add database queue admin endpoints and runtime verification`
- Scope: 验证 retry/stats/manual smoke 管理接口，确保消息表与治理管理面闭环可用。
- Validation:
  - `mvn test`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` after message abstraction routing was established.
  - 2026-04-20: `MessageAdminController`、`MessageAdminApplicationTest`、`scripts/manual-message-queue-smoke.sh`、`docs/deliveries/init-completion.md` 与 `docs/deployments/local-setup.md` 已形成管理接口、消息表和人工 smoke 的验证闭环。
  - 2026-04-20: strict ledger reconciliation revalidated the current test chain and knowledge lint, confirmed that runtime verification evidence is already documented, then archived the task.

### C-TASK-007: 对齐现有分层

- Status: done
- Priority: 2
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `refactor(governance): C-TASK-007 close layered governance baseline`
- Scope: 对齐 `governance` 当前 `application` 包域下的 `controller`/`service` 与 `domain`/`infrastructure` 分层，确保其继续向公共管理服务边界收敛。
- Validation:
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` as the first governance hardening task.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; 当前工作树已形成 `application` 包域下的 `controller` / `service` 与 `domain` / `infrastructure` 分层、严格请求上下文校验与治理内部契约基线，并通过 `mvn clean compile`、`mvn test`、`mvn validate pmd:pmd checkstyle:check`、`node scripts/lint-repository-knowledge.js` 验证，但当前分层调整尚未完成单任务 git closeout，暂不归档。
  - 2026-04-20: removed the last `governance.common` remnants, added regression coverage for `HealthStatusApplicationService` and `MessageRetryResultVO`, and synced local delivery/setup notes with the protected governance admin paths and current Phase-C baseline summary.

### HARN-002: Close remaining harness doc drift

- Status: done
- Priority: 1
- Depends on: HARN-001
- Completed at: 2026-04-20
- Commit subject: `docs(harness): HARN-002 close remaining harness doc drift`
- Scope: align the root and docs README summaries with the current harness governance baseline and confirmed service direction.
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: audited post-HARN-001 residual drift and isolated two remaining stale README summaries for targeted closeout.
  - 2026-04-20: updated `docs/README.md` to stop advertising stale rule and service counts, and updated `README.md` to describe the current repository baseline instead of the old initialization state.
  - 2026-04-20: validated repository knowledge lint and task audit, then archived the task for single-task commit closeout.

### DOC-GOV-001: Establish document truth baseline and readiness governance

- Status: done
- Priority: 1
- Depends on: HARN-002
- Completed at: 2026-04-20
- Commit subject: `docs(governance): DOC-GOV-001 establish document truth baseline`
- Scope: add the document truth baseline, implementation readiness spec, service capability map, governance retrospective template and baseline retrospective, then wire them into the docs entry points, master plan, coverage matrix, rule consumption notes, history ledger, repo map, and repository knowledge lint.
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `node scripts/check-frontend-backend-separation.js`
  - `mvn -B test`
  - `npm run build`
  - `npm run lint`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: audited the repository truth against the initialization architecture, rules, plans, ADR index, access-control spec, and deployment docs to isolate drift between current facts and confirmed targets.
  - 2026-04-20: added `document-truth-baseline.md`, `implementation-readiness.md`, `service-capability-map.md`, `retrospective-template.md`, and `document-governance-retrospective-2026-04-20.md`.
  - 2026-04-20: updated `docs/README.md`, `docs/plans/README.md`, `docs/plans/master-execution-plan.md`, `docs/plans/document-coverage-matrix.md`, `docs/architecture/init.md`, `docs/generated/repo-map.md`, `docs/rules/codex-rules.md`, `docs/references/human-constraint-history.md`, and `scripts/lint-repository-knowledge.js` to consume the new governance layer.
  - 2026-04-20: wrote baseline validation evidence into `docs/quality/validation-log.md`, confirmed documentation coverage completeness, and prepared the batch for git closeout.

### DOC-GOV-002: Close strict governance audit gaps

- Status: done
- Priority: 1
- Depends on: DOC-GOV-001
- Completed at: 2026-04-20
- Commit subject: `docs(governance): DOC-GOV-002 close strict audit gaps`
- Scope: close the remaining 7 governance audit gaps by adding explicit gap and prerequisite matrices, an interface contract baseline, a task governance extension matrix, a repair retrospective, updated rules and indices, and final task/git closeout consistency.
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `node scripts/check-frontend-backend-separation.js`
  - `mvn -B test`
  - `npm run build`
  - `npm run lint`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: re-audited the original governance plan against repository truth and isolated 7 still-open closure gaps around matrices, interface contracts, task-extension fields, and batch consistency.
  - 2026-04-20: added `document-gap-matrix.md`, `phase-prerequisite-matrix.md`, `service-interface-contract-baseline.md`, `task-governance-extension-matrix.md`, and `document-governance-repair-retrospective-2026-04-20.md`.
  - 2026-04-20: updated `docs/README.md`, `docs/plans/README.md`, `docs/plans/master-execution-plan.md`, `docs/plans/document-coverage-matrix.md`, `docs/plans/task-spec-matrix.md`, `docs/generated/repo-map.md`, `docs/rules/codex-rules.md`, `docs/references/human-constraint-history.md`, and `scripts/lint-repository-knowledge.js` to consume the new closure layer.
  - 2026-04-20: completed the governance batch audit chain by aligning `tasks-done.md`, `docs/quality/validation-log.md`, repair retrospective, and git closeout records.

### DOC-GOV-003: Close final readiness and closeout consistency gaps

- Status: done
- Priority: 1
- Depends on: DOC-GOV-002
- Completed at: 2026-04-20
- Commit subject: `docs(governance): DOC-GOV-003 close final readiness gaps`
- Scope: close the remaining micro consistency gaps by aligning implementation-readiness and docs read order with the prerequisite and task-governance matrices, then append the missing `DOC-GOV-002` task-audit closeout evidence and archive the repair batch.
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: re-checked the 16-item governance audit list against repository truth and confirmed that only three residual consistency gaps remained after `DOC-GOV-002`.
  - 2026-04-20: updated `docs/plans/implementation-readiness.md` and `docs/README.md` so non-trivial execution order explicitly consumes `phase-prerequisite-matrix.md` and `task-governance-extension-matrix.md`.
  - 2026-04-20: appended follow-up closure notes to `docs/plans/document-governance-repair-retrospective-2026-04-20.md`, backfilled the missing `DOC-GOV-002 closeout task-audit` validation record, and prepared the batch for git closeout.

### DOC-GOV-004: Close docs authority wording drift

- Status: done
- Priority: 1
- Depends on: DOC-GOV-003
- Completed at: 2026-04-20
- Commit subject: `docs(governance): DOC-GOV-004 close docs authority drift`
- Scope: remove the last README-level authority wording drift by keeping `init.md` as the historical baseline entry while pointing current service-boundary and interface-contract execution authority to the explicit governance baseline documents.
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: strict re-audit found one remaining main-entry wording drift in `docs/README.md`, where current authority was still described as coming directly from `init.md`.
  - 2026-04-20: updated `docs/README.md` to distinguish historical initialization baseline from current authority, and appended the closeout rationale to `docs/plans/document-governance-repair-retrospective-2026-04-20.md`.

### DOC-GOV-005: Align C4, process audit, and governance authority follow-ups

- Status: done
- Priority: 1
- Depends on: DOC-GOV-004
- Completed at: 2026-04-20
- Commit subject: `docs(governance): DOC-GOV-005 align c4 and process authority follow-ups`
- Scope: add the C4 authority document and process-flow governance audit, then align docs entry points, rule consumption notes, execution-plan wording, and history records so the strict-mode and temporary delivery-page follow-ups have explicit authority anchors.
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: post-closeout drift review found a remaining governance bundle spanning C4 authority location, process-flow audit indexing, strict-mode (`R-165`) consumption notes, and the temporary delivery-page (`R-166`) authority trail.
  - 2026-04-20: added `docs/architecture/c4-overview.md` and `docs/plans/process-flow-and-governance-audit-2026-04-20.md`, then aligned `docs/README.md`, `docs/architecture/init.md`, `docs/plans/README.md`, `docs/plans/document-coverage-matrix.md`, `docs/plans/master-execution-plan.md`, `docs/plans/task-spec-matrix.md`, `docs/plans/task-governance-extension-matrix.md`, `docs/operations/human-collaboration.md`, `docs/quality/validation-rules.md`, `docs/references/human-constraint-history.md`, `docs/rules/codex-rules.md`, and `scripts/lint-repository-knowledge.js`.
  - 2026-04-20: reran repository knowledge lint and task audit, then archived the governance follow-up batch for single-task git closeout.

### OPS-GOV-001: Add local and CI quality-gate entrypoints

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `ops(ci): OPS-GOV-001 add quality gate entrypoints`
- Scope: add executable SonarQube and coverage entrypoints for local/CI use, wire them into repository helper commands, and let CI consume them when configuration is present.
- Validation:
  - `bash scripts/run-sonar.sh`
  - `bash scripts/run-coverage.sh --phase report-only`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: cleanup classification isolated the remaining CI/quality files from Phase-C feature work because they add delivery gate entrypoints rather than shared-foundation or governance functionality.
  - 2026-04-20: added `scripts/run-sonar.sh`, wired coverage/Sonar helpers into `.github/workflows/ci.yml`, `Makefile`, and `docs/operations/local-development.md`, and kept the Sonar path no-op safe when environment variables are not configured.
  - 2026-04-20: reran the Sonar helper in no-config mode, reran coverage report generation, and prepared the quality-gate batch for single-task git closeout.

### HARN-001: Codify foreman workflow and task ledger

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-19
- Commit subject: `chore(harness): HARN-001 codify foreman workflow and task ledger`
- Scope: add SQLForge task ledger, inbox, agent config, operations docs, generated repo map, validation log, task audit script, and append-only harness governance rules.
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `node scripts/check-frontend-backend-separation.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-19: audited current repository knowledge, rule continuity, validation constraints, and root Git boundary before applying harness governance changes.
  - 2026-04-19: added task ledger files, operations docs, generated repo map, agent config, validation log, exec-plan directories, and task audit automation.
  - 2026-04-19: appended `R-156` to `R-161`, updated documentation entry points, and aligned repository knowledge lint with the new governance baseline.
  - 2026-04-19: validated repository knowledge lint, frontend-backend separation, and task audit, then archived the task for single-task commit closeout.
