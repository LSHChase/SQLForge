## Candidate Execution Plan for `HARN-034`

### Story Placement
- Place `HARN-034` under `A-STORY-008` (`Codex MCP 治理接入`).
- Treat the work as Phase-A governance/tooling scope, not product feature delivery.

### Execution Shape
1. Materialize `HARN-034` through the standard `foreman` path so the task enters the existing ledger and audit chain.
2. Add `docs/security/connectors.md` as the formal MCP connector/security boundary anchor without creating a second source of truth.
3. Extend repository rules and validation rules so MCP constraints become append-only governed policy.
4. Extend `python3 scripts/foreman.py compile-governance` and `python3 scripts/validate_codex_runtime.py` so MCP governance signals are compiled and checked automatically.
5. Add Codex-facing MCP local-use guidance and operational entry instructions that explicitly preserve `foreman` / `task_audit` / `closeout`.
6. Validate through the standard SQLForge validation chain and keep all MCP scope limited to approved read-only categories.

### Planned Work Packages
- Governance anchor package: add connector/MCP boundary documentation under `docs/security/connectors.md`.
- Policy package: append MCP-specific rules and matching validation rules.
- Automation package: compile MCP policy artifacts and runtime validation checks through existing governance entrypoints.
- Operator guidance package: add a Codex MCP playbook and local development guidance for governed use.
- Boundary package: explicitly freeze first-batch MCP scope to read-only categories and exclude multi-agent `mcp_profile`.

### Exit Conditions
- MCP governance baseline is anchored in formal docs.
- First-batch MCP scope is explicitly limited to approved read-only categories only.
- Rules, validation rules, compiled governance artifacts, and runtime validation are aligned.
- Codex MCP usage guidance exists and points back to the governed workflow.
- Task remains inside the current SQLForge audit chain and does not add a parallel truth model.
