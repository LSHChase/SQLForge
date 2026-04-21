# HARN-007 Codex Runtime Integration Plan

## Summary

- Goal: wire SQLForge's existing document-truth, ledger, validation, closeout, and delivery-governance system into Codex's native instruction discovery and lifecycle hooks without creating a second source of truth.
- Owner: Foreman / Codex strict-mode governance batch
- Task id: `HARN-007`
- Scope boundary: governance docs, Codex integration scaffolding, hook orchestration, foreman CLI, generated policy manifests, and repository knowledge enforcement only.
- Explicit non-scope for this batch: unresolved dirty business-module edits already present in the worktree.

## Preconditions

1. `docs/README.md`, `docs/plans/document-truth-baseline.md`, `docs/architecture/init.md`, `docs/rules/codex-rules.md`, and `docs/quality/validation-rules.md` remain the authority chain.
2. `tasks.md`, `tasks-done.md`, `INBOX.md`, `docs/quality/validation-log.md`, and Git history remain the audit chain.
3. Current unrelated dirty worktree changes are preserved and excluded from this task's write scope.

## Deliverables

1. Formal blueprint document:
   - `docs/plans/codex-governance-integration-blueprint.md`
2. Hardened Codex entry contract:
   - `AGENTS.md`
3. Project-scoped Codex configuration and hook orchestration:
   - `.codex/config.toml`
   - `.codex/hooks.json`
   - `.codex/hooks/*.py`
   - `.codex/policy/*.json`
   - `.codex/state/.gitkeep`
   - `.codex/state/current-task.example.json`
4. Unified governance action entrypoint:
   - `scripts/foreman.py`
5. Documentation and index alignment:
   - `docs/README.md`
   - `docs/plans/README.md`
   - `docs/operations/README.md`
   - `docs/operations/local-development.md`
6. Knowledge-lint enforcement updates:
   - `scripts/lint-repository-knowledge.js`

## Execution Steps

### Step 1: Formalize the blueprint

- Write the full Codex integration blueprint with:
  - truth model
  - AGENTS contract
  - hook orchestration contract
  - foreman CLI contract
  - runtime state contract
  - closeout vs delivery-closeout separation
  - fallback and drift strategy

### Step 2: Wire the instruction layer

- Replace the root navigation-only `AGENTS.md` with a hard-contract version.
- Keep it concise and point to authority entry docs instead of copying long source text.

### Step 3: Scaffold project-scoped Codex runtime integration

- Add `.codex/config.toml`
- Add `.codex/hooks.json`
- Add hook handlers for:
  - `UserPromptSubmit`
  - `PreToolUse`
  - `PermissionRequest`
  - `Stop`
- Add machine-readable policy/state files.

### Step 4: Implement unified foreman CLI

- Add `scripts/foreman.py` with subcommands:
  - `preflight`
  - `instantiate`
  - `sync-context`
  - `validate`
  - `audit`
  - `compile-governance`
  - `closeout`
  - `delivery-closeout`

### Step 5: Update discovery and enforcement docs

- Index the new blueprint and Codex integration entrypoints.
- Add local-development guidance for the new CLI and `.codex` layer.
- Update repository knowledge lint to require the new integration files.

### Step 6: Validate and close out

- Compile Python files
- Run governance compile check
- Run preflight against `HARN-007`
- Run task audit and knowledge lint
- Archive task with `Context closeout`
- Create a single-task commit
- Run post-closeout audit

## Risks

- Hooks are experimental and repo-local `.codex` layers require a trusted project; therefore the fallback path in `AGENTS.md` and `foreman.py` must remain mandatory.
- `PreToolUse` only guards simple Bash cases; it is useful but not a complete enforcement boundary.
- `Stop` must distinguish “turn pause” from “task finish” to avoid premature closeout.

## Validation

- `python3 -m py_compile scripts/foreman.py .codex/hooks/*.py`
- `python3 scripts/foreman.py compile-governance --check`
- `python3 scripts/foreman.py preflight --task HARN-007 --task-class standard`
- `python3 scripts/task_audit.py --check --phase pre-closeout`
- `node scripts/lint-repository-knowledge.js`
