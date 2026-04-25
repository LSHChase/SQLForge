## Candidate Execution Plan for `HARN-035`

### Story Placement
- Place `HARN-035` under `A-STORY-008` (`Codex MCP 治理接入`).
- Treat the work as a follow-on Phase-A governance/tooling task that depends on `HARN-034`.

### Execution Shape
1. Materialize `HARN-035` only after `HARN-034` has established the MCP governance baseline.
2. Extend multi-agent documentation and manifest contracts with a governed top-level `mcp_profiles` registry plus per-agent `mcp_profile`, both explicitly limited to read-only evidence.
3. Limit MCP-assisted evidence reading to `explorer` and `validator`, while keeping Main Foreman as the only write-back / validate / closeout authority.
4. Update `prepare` / `launch` / `collect` / optional `full-auto` flows so they can carry and inspect `mcp_profile` metadata without creating a second truth source.
5. Keep worker ownership and forbidden-path rules intact; MCP metadata must not weaken existing path, audit, or closeout boundaries.
6. Validate through the normal SQLForge validation chain and multi-agent dry-run paths.

### Planned Work Packages
- Playbook package: extend multi-agent documentation with MCP profile semantics and allowed roles.
- Prompt package: update relevant agent prompts so explorer/validator can consume governed read-only evidence.
- Manifest package: extend template/schema guidance with `mcp_profiles` and `mcp_profile`.
- Orchestration package: update prepare/launch/collect/full-auto scripts to pass and inspect MCP profile metadata.
- Boundary package: preserve Main Foreman as the only write-back / validate / closeout authority and keep worker MCP scope read-only or disabled.

### Exit Conditions
- Multi-agent manifest contracts support governed MCP profile metadata.
- Explorer/validator MCP usage is documented and constrained to read-only evidence.
- Main Foreman remains the only write-back / validate / closeout authority.
- Worker forbidden-path and ownership guarantees remain intact.
- Validation and dry-run coverage exists for the new multi-agent MCP contract.
