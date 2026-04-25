## Candidate Execution Plan for `HARN-038`

### Story Placement
- Place under `A-STORY-007`.
- Scope this as a governed maintenance task for workflow hardening in closeout / post-closeout / healthcheck runtime behavior.

### Intended Dependency Shape
- Treat `HARN-033` as the immediate upstream dependency because it introduced post-closeout actual evidence health reporting and governed runtime cleanup preview semantics.
- Do not start implementation until the formal task is materialized into the master plan, matrices, and ledger.

### Execution Sequence
1. Confirm governance context and materialize/instantiate the formal task through the standard foreman path; no direct task materialization outside the ledger chain.
2. Baseline the defect boundary precisely: distinguish closeout success, post-closeout verification failure, evidence discovery scope, and runtime state mutation points.
3. Refine the healthcheck evidence inspection rule so post-closeout checks do not self-reference the just-archived task's own newly written actual evidence in a way that creates false failure.
4. Define strict but recoverable runtime cleanup semantics for the case where closeout commit succeeds but post-check fails.
5. Preserve strict implementation-time dirty-worktree blocking; any fix must be context-sensitive to post-closeout/runtime repair semantics rather than a weakening of enforcement.
6. Update governance/runtime documentation so `pre-confirm`, `post-closeout`, and `runtime repair` have explicit boundaries, recommended commands, and operator expectations.
7. Add automated validation that covers: false self-reference regression, successful cleanup after post-check failure, retention of failed evidence, and continued dirty-worktree blocking during implementation-time checks.
8. Run standard validation and audit steps through foreman/task-audit entry points only, then close out through the governed chain.

### Delivery Boundaries
- No bypass of Main Foreman for write-back, validate, or closeout.
- No second truth source.
- No silent evidence deletion.
- No business-code changes.
- No weakening of governance gates to make tests pass.
