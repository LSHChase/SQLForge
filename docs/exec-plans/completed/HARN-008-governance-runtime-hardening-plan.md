# HARN-008 Governance Runtime Hardening Plan

## Summary

- Goal: close the 6 unresolved governance/runtime gaps left after HARN-007 so Codex execution consumes repository truth, enforces closeout semantics, and leaves no stale runtime state.
- Owner: Foreman / Codex strict-mode governance batch
- Task id: `HARN-008`
- Scope boundary: `.codex/`, repository governance scripts, governance documents, task ledgers, validation evidence, and exec-plan artifacts only.
- Explicit non-scope for this batch: unrelated dirty business-module edits already present under `benchmark-engine/`, `governance/`, `sql-optimization/`, and `sqlforge-shared/`.

## Preconditions

1. `docs/README.md`, `docs/plans/document-truth-baseline.md`, `docs/architecture/init.md`, `docs/rules/codex-rules.md`, and `docs/quality/validation-rules.md` remain the authority chain.
2. `tasks.md`, `tasks-done.md`, `INBOX.md`, `docs/quality/validation-log.md`, and Git history remain the audit chain.
3. The closeout implementation must never scoop unrelated dirty files into a governance commit.

## Closure Targets

1. Hook protocol and orchestration:
   - align hook JSON output shape with official Codex hook contracts
   - tighten `.codex/hooks.json` matcher/command organization
2. Authority-aware preflight:
   - `scripts/foreman.py preflight` must digest current repository truth instead of emitting a fixed summary
3. Matrix-aware task instantiation:
   - `scripts/foreman.py instantiate` must consume `master-execution-plan.md`, `task-spec-matrix.md`, and `task-governance-extension-matrix.md`
4. Real closeout semantics:
   - `closeout` must archive tasks, run audits, create a single-task commit from explicit stage scopes, and clean runtime state
   - `delivery-closeout` must add tag/write-back semantics for delivery tasks
5. Runtime cleanup:
   - `.codex/state/current-task.json` and `.codex/state/session-context.json` must not remain pinned to an old active task after closeout
6. Trusted validation evidence:
   - add deterministic runtime/hook validation and attempt a real local Codex validation path where feasible

## Execution Steps

### Step 1: Re-anchor runtime metadata

- move the completed `HARN-007` exec plan into `docs/exec-plans/completed/`
- create this `HARN-008` active plan
- reopen the runtime state for the active hardening batch

### Step 2: Repair hook contracts

- update hook helpers to emit event-specific official Codex output shapes
- align `hooks.json` with the supported matcher semantics for each hook type
- add repeatable simulations for `UserPromptSubmit`, `PreToolUse`, `PermissionRequest`, and `Stop`

### Step 3: Make foreman consume document truth

- parse authority documents for preflight summaries and source digests
- parse task matrices and master-plan context for task instantiation
- compile policy artifacts from live repository truth instead of hard-coded `HARN-007` paths

### Step 4: Implement safe closeout

- require explicit stage scopes so closeout cannot capture unrelated dirty files
- move archived tasks into `tasks-done.md`
- run pre-closeout audit, commit, post-closeout audit, and runtime cleanup
- support delivery tag/write-back behavior without weakening standard-task semantics

### Step 5: Validate and close out

- py-compile `scripts/foreman.py` and `.codex/hooks/*.py`
- run hook/runtime simulations
- run governance compile drift check
- run pre-closeout audit and repository knowledge lint
- close out `HARN-008` with a single governance-only commit

## Risks

- Real repo-local hooks depend on Codex trusted-project loading, so deterministic local simulation must remain the enforcement backstop.
- Delivery semantics must stay opt-in and task-class-aware; ordinary governance tasks must not accidentally tag or write back delivery metadata.
- The runtime cleanup must preserve traceability in ledgers and Git while removing stale active-task state from `.codex/state/`.

## Validation

- `python3 -m py_compile scripts/foreman.py .codex/hooks/*.py`
- `python3 scripts/validate_codex_runtime.py`
- `python3 scripts/foreman.py compile-governance --check`
- `python3 scripts/task_audit.py --check --phase pre-closeout`
- `node scripts/lint-repository-knowledge.js`
