# HARN-009 Closeout Boundary Repair Plan

## Summary

- Goal: repair the closeout archive boundary logic and add explicit done-ledger structure checks so `tasks-done.md` cannot silently absorb non-`Done` headings.
- Owner: Foreman / Codex strict-mode governance repair batch
- Task id: `HARN-009`
- Scope boundary: `scripts/foreman.py`, `scripts/task_audit.py`, `tasks.md`, `tasks-done.md`, `docs/quality/validation-log.md`, and this exec-plan artifact only.
- Explicit non-scope: all dirty business-module changes under `benchmark-engine/`, `governance/`, `sql-optimization/`, and `sqlforge-shared/`.

## Closure Targets

1. `foreman closeout` only archives the task block that belongs to the current ledger section.
2. `task_audit` explicitly rejects `tasks-done.md` when non-`Done` headings or stray section markers are embedded in the done ledger body.
3. The currently polluted `tasks-done.md` structure is repaired back to the intended shape.

## Validation

- `python3 -m py_compile scripts/foreman.py scripts/task_audit.py`
- `python3 scripts/task_audit.py --check --phase pre-closeout`
- `python3 scripts/task_audit.py --check --phase post-closeout`
- `node scripts/lint-repository-knowledge.js`
- `python3 scripts/foreman.py compile-governance --check`
