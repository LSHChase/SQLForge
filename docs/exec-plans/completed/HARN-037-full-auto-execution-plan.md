## Candidate Execution Plan for `HARN-037`

### Scope Positioning
Shape a governed standard task under `A-STORY-008` to make the existing read-only MCP baseline easier to onboard, diagnose, and use safely, while preserving the established boundary: read-only evidence enhancement only, no writable control plane, no remote automation, no secret or live-inventory persistence.

### Execution Sequence
1. **Baseline confirmation and dependency bind**
   - Anchor the candidate task to the existing read-only MCP baseline established by `HARN-034` and `HARN-035`.
   - Confirm the task narrative is framed as an operationalization layer over existing governance, not a capability expansion.
   - Carry forward strict-mode constraints, single source of truth expectations, and `Main Foreman` authority for write-back / validate / closeout.

2. **Boundary and positioning alignment**
   - Align the canonical wording for MCP usage across the relevant governance/doc/runtime surfaces.
   - Ensure both single-agent local MCP usage and multi-agent `mcp_profile` usage describe the same read-only boundary.
   - Explicitly state that only governed read-only evidence enhancement is supported, and that writable MCP, remote automation, SSH/Kubernetes/database-execution style operations are outside the allowed model.

3. **Category-specific onboarding shaping**
   - Define onboarding expectations for each already-allowed MCP category without redefining the category taxonomy itself.
   - Require local access/setup guidance, prerequisite clarity, safe usage notes, and evidence-handling instructions for each category.
   - Ensure onboarding guidance avoids persisting secrets, tokens, endpoints, or live inventory.

4. **Doctor / healthcheck capability shaping**
   - Extend or introduce governed `doctor` / `healthcheck` behavior aimed at MCP readiness and governance completeness.
   - Cover checks for category declaration completeness, local prerequisite clarity, evidence write-back target clarity, and boundary compliance.
   - Include explicit detection expectations for prohibited writable or execution-oriented integrations, while keeping doctor/healthcheck itself non-operational and non-remote.

5. **Validation automation shaping**
   - Require automated validation for the new or extended doctor/healthcheck behavior.
   - Ensure validation proves both positive coverage for governed read-only MCP usage and negative coverage for prohibited server/integration types.
   - Keep validation within the existing instantiate -> validate -> closeout governance chain.

6. **Evidence and governance write-back shaping**
   - Define where MCP-related evidence should be written back under the existing governance system.
   - Preserve `Main Foreman` as the only write-back / validate / closeout authority.
   - Prevent any repo-tracked persistence of secrets, tokens, endpoints, or live server inventory.

7. **Task-pack handoff constraints**
   - Task-shaper should split work so documentation alignment, doctor/healthcheck implementation, and automation validation are all represented, but still remain a single governed standard task unless downstream shaping proves decomposition is necessary.
   - Any unresolved ambiguity about current category inventory or runtime entry-point conflicts should be captured as notes/open questions rather than treated as confirmed design facts.

### Governance Fit
- Story remains inside `A-STORY-008` because the work concerns governed MCP access and policy enforcement.
- Dependency order must respect prior MCP baseline tasks before operationalization work begins.
- Downstream execution must still use SQLForge standard actions: instantiate, validate, closeout.
