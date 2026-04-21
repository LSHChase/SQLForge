# SQLForge Codex Governance Integration Blueprint

## 1. Purpose

本蓝图定义 SQLForge 如何把现有 `docs/` 文档真值体系、任务台账、验证规则、closeout 流程、Git 审计链与交付回写要求，完整接入 Codex 的执行前、执行中、执行后生命周期。

本蓝图的目标不是复制现有文档，而是把现有真值体系编译为 Codex 可以发现、注入、阻断和审计的运行层，同时保留原始文档与历史记录作为唯一长期权威来源。

## 2. Non-Negotiable Invariants

1. `docs/`、`tasks.md`、`tasks-done.md`、`INBOX.md`、`docs/quality/validation-log.md` 和 Git history 仍是唯一真值与审计链。
2. 不新增第二套长期真值；`.codex/` 中的配置、policy、state 与 prompts 只允许是运行时配置或编译产物。
3. 不允许绕过 `scripts/foreman.py` 直接跳过 preflight、validation、closeout 与 delivery closeout。
4. 非 trivial 任务必须先经过文档上下文建立，再进入实现。
5. `turn stop` 不等于 `task finish`；只有进入 `done_ready` 或显式 `closeout` 时才允许归档和提交。
6. 普通任务执行单任务单 commit；只有 delivery 类任务要求 tag / write-back。
7. 任何现有流程、规则、约束、F-task 特例、closeout 语义和审计链都不得因 Codex 集成而被删节、合并或静默弱化。

## 3. Codex Integration Scope

### 3.1 Execution before work

- Root `AGENTS.md` provides the hard repository contract.
- `UserPromptSubmit` hook injects authority-aware preflight context.
- `scripts/foreman.py preflight` builds a compact task/session context from repository truth.

### 3.2 Execution during work

- `PreToolUse` hook blocks destructive or audit-breaking Bash commands.
- `scripts/foreman.py` is the only standard action entrypoint for task instantiation, validation, context sync, closeout, and delivery closeout.
- `PermissionRequest` hook only governs outside-sandbox approval decisions.

### 3.3 Execution after work

- `Stop` hook distinguishes a paused turn from a finished task.
- If a task has entered `done_ready` or `closeout`, `Stop` continues Codex until the required closeout chain has completed.

### 3.4 Automation and CI

- `codex exec --json` remains the non-interactive automation entrypoint for repeatable governance jobs.
- CI and local quality gates replay the same audits (`task_audit`, repository knowledge lint, foreman governance compile check).

## 4. Truth Model

### 4.1 Primary truth sources

1. `docs/README.md`
2. `docs/plans/document-truth-baseline.md`
3. `docs/architecture/init.md`
4. `docs/rules/codex-rules.md`
5. `docs/quality/validation-rules.md`
6. 领域专项文档
7. `docs/plans/master-execution-plan.md`
8. `docs/plans/phase-prerequisite-matrix.md`
9. `docs/plans/task-spec-matrix.md`
10. `docs/plans/task-governance-extension-matrix.md`
11. `tasks.md` / `tasks-done.md` / `INBOX.md`

### 4.2 Compiled runtime artifacts

以下文件是编译产物或运行配置，不构成新的长期真值：

- `.codex/config.toml`
- `.codex/hooks.json`
- `.codex/policy/*.json`
- `.codex/state/*.json`
- root `AGENTS.md` 中的执行摘要

每个机器产物都必须可追溯回对应源文档和章节。

## 5. Runtime Layers

### 5.1 Truth layer

- Existing `docs/` and ledgers remain authoritative.

### 5.2 Instruction layer

- Root `AGENTS.md` contains only hard execution requirements, authority entry points, standard action entrypoints, and fallback rules.

### 5.3 Hook orchestration layer

- `.codex/hooks.json` orchestrates prompt-time, tool-time, approval-time, and stop-time control points.

### 5.4 Standard action layer

- `scripts/foreman.py` provides a single CLI for preflight, task instantiation, validation, audits, closeout, delivery closeout, and governance compilation.

### 5.5 Audit backstop layer

- `scripts/task_audit.py`
- `scripts/lint-repository-knowledge.js`
- Git history
- delivery write-back records

## 6. Root AGENTS Contract

Root `AGENTS.md` must remain concise and human-maintainable. It must include:

1. Hard repository requirements
2. Authority entry order
3. Standard action entrypoints
4. Audit chain
5. Fallback rule if `.codex` project config or hooks are not active

It must not:

- duplicate large sections of `docs/`
- replace rule text or ADR text
- become an alternate long-form architecture document

## 7. Hook Orchestration Contract

### 7.1 Hook set

First-class hooks:

- `UserPromptSubmit`
- `PreToolUse`
- `PermissionRequest`
- `Stop`

`PostToolUse` is optional and not part of the required enforcement boundary for the first integrated version.

### 7.2 Orchestration rule

Each hook event must use one repository-local orchestrator command, even though Codex may load matching hooks from multiple files. This keeps repository behavior deterministic and reduces race conditions from concurrent hook execution.

### 7.3 Event contracts

#### `UserPromptSubmit`

Responsibilities:

- classify request type (`advisory`, `trivial`, `standard`, `delivery`)
- decide whether task binding is required
- run or request `preflight`
- add compact authority-aware context via `additionalContext`

Allowed outputs:

- extra developer context
- prompt blocking for structurally invalid requests

#### `PreToolUse`

Responsibilities:

- guard against destructive or audit-breaking Bash commands
- deny known-bad commands such as:
  - `git add .`
  - `git add -A`
  - `git commit -a`
  - `git reset --hard`
- require an active bound task for non-trivial implementation work

Boundaries:

- only intercepts Bash
- useful guardrail, not a complete enforcement boundary

#### `PermissionRequest`

Responsibilities:

- auto-allow or auto-deny approval prompts for out-of-sandbox operations according to repository policy
- does not handle normal in-repo governance flow

#### `Stop`

Responsibilities:

- decide whether the turn can safely end
- if the task is still mid-flight, only ensure runtime state is clean
- if the task has reached `done_ready` or `closeout`, continue Codex until closeout steps complete

Stop must not treat every turn pause as task completion.

## 8. Foreman CLI Contract

All standard repository-governance actions must flow through `scripts/foreman.py`.

### 8.1 `preflight`

Purpose:

- build a compact, task-aware context from the repository truth sources

Outputs:

- `.codex/state/session-context.json`
- `.codex/state/current-task.json` updates

### 8.2 `instantiate`

Purpose:

- materialize a task from the plan matrix into `tasks.md`

Sources:

- `master-execution-plan.md`
- `task-spec-matrix.md`
- `task-governance-extension-matrix.md`

### 8.3 `sync-context`

Purpose:

- update runtime state without archiving the task

### 8.4 `validate`

Purpose:

- execute the configured validation chain for a task and write evidence references

### 8.5 `audit`

Purpose:

- wrap `task_audit.py` and related repository governance checks

### 8.6 `compile-governance`

Purpose:

- compile machine-readable Codex policy artifacts from authority sources
- detect drift between documents and generated policy files

### 8.7 `closeout`

Purpose:

- archive a completed task
- run pre-closeout audit
- enforce single-task commit expectations
- run post-closeout audit

### 8.8 `delivery-closeout`

Purpose:

- apply phase or delivery-specific tag / write-back semantics
- only valid for delivery-class work

## 9. Runtime State Contract

Runtime state is stored under `.codex/state/`. It is not long-term truth. It is session-local integration glue for hooks and `foreman.py`.

### 9.1 `current-task.json`

Minimum fields:

- `version`
- `task_id`
- `task_class`
- `phase`
- `status`
- `bound_to_ledger`
- `strict_mode`
- `requires_human_decision`
- `delivery_scope`
- `preflight`
- `validation`
- `closeout`
- `traceability`
- `updated_at`

### 9.2 Task classes

- `advisory`
- `trivial`
- `standard`
- `delivery`

### 9.3 Phases

- `idle`
- `preflight`
- `discovery`
- `implementation`
- `validation`
- `done_ready`
- `closeout`
- `delivery_closeout`
- `closed`
- `blocked`

### 9.4 State rules

1. `turn stop` during `implementation` or `validation` must not auto-commit.
2. `done_ready` is the earliest phase where `Stop` may escalate into closeout.
3. `delivery_closeout` is only legal for `delivery` tasks.
4. `blocked` requires the human-decision chain in the ledger and/or `INBOX.md`.

## 10. Closeout And Delivery Contract

### 10.1 Task closeout

Task closeout includes:

1. final validation
2. `Context closeout`
3. migration from `tasks.md` to `tasks-done.md`
4. pre-closeout audit
5. single-task commit
6. post-closeout audit

### 10.2 Delivery closeout

Delivery closeout includes everything in task closeout plus:

- tag policy
- delivery record write-back
- Git / tag / delivery metadata consistency

Only delivery-class tasks use this path.

## 11. Drift And Fallback Strategy

### 11.1 Drift

`compile-governance --check` must fail if compiled Codex policy artifacts diverge from the current document truth.

### 11.2 Fallback

If repo-local `.codex/` config or hooks are not active:

- root `AGENTS.md` remains the mandatory contract
- `scripts/foreman.py` remains the required action entrypoint
- `task_audit.py` and repository knowledge lint remain the enforcement backstop

### 11.3 Trusted-project constraint

Repo-local `.codex/config.toml` and `.codex/hooks.json` depend on the project running in a trusted Codex environment. This is an acceleration path, not a replacement for repository governance itself.

## 12. Acceptance Gates

The integration is considered valid only when all of the following are true:

1. non-trivial work cannot proceed without consuming repository truth through `preflight`
2. completed tasks cannot end without entering the closeout chain
3. ordinary tasks do not accidentally trigger delivery semantics
4. F-task and delivery requirements are machine-checkable
5. every Codex runtime artifact can trace back to the original source document or ledger entry
6. the repository still works even if repo-local `.codex/` loading is unavailable
