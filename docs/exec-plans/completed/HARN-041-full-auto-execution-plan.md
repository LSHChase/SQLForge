## Candidate Execution Plan for `HARN-041`

### Story Placement
- Place under `A-STORY-007`.
- Scope this as a governed runtime hardening follow-up for no-task shaping, reservation lifecycle semantics, and demand re-entry after candidate pause/archive/abandon decisions.

### Intended Dependency Shape
- Treat `HARN-028`, `HARN-036`, and `HARN-038` as the effective upstream governance baseline because they define governed intake, execution preview, healthcheck/runtime cleanup, and post-closeout recovery semantics.
- Do not continue implementation until the formal task is materialized into the master plan, matrices, and ledger through the standard chain.

### Execution Sequence
1. Materialize and instantiate the formal task through `task_materialize.sh` and `foreman.py`; do not bypass the ledger chain.
2. Define a single reservation lifecycle model that distinguishes active candidate work from explicitly paused, archived-template, abandoned, released-dry-run, and materialized states.
3. Update shared reservation helpers so lifecycle transitions are auditable and downstream scripts can reason over the same terminal/non-terminal status rules.
4. Harden `governed_healthcheck.py` so explicitly paused/archived/abandoned candidates no longer trigger `reservation_conflict`, while unresolved active reservations still block confirm-run.
5. Harden `governed_runtime_dashboard.py` so cleanup/status actions preserve shaping evidence instead of encouraging manual JSON deletion, and so HARN-040 can be paused without remaining confirm-run eligible.
6. Document when a paused candidate may resume in place, when it must be re-shaped into a new candidate package, and how old dry-run evidence is preserved without being treated as a formal task.
7. Use the governed runtime tooling to archive the stale HARN-029/HARN-030 dry-run reservations, pause HARN-040, and then prove a new requirement can re-enter governed intake/shaping.
8. Run validation, task audit, and closeout through standard foreman entry points only.

### Delivery Boundaries
- Do not implement HARN-029 or HARN-030 as formal tasks.
- Do not continue confirm-run for HARN-040.
- Do not hand-delete runtime JSON to hide governance defects.
- Do not weaken tracked-dirty, closeout, or ledger governance gates.
