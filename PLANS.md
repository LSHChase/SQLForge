# Plans

Execution plans are first-class repository artifacts.

## Locations

- Active plans: [docs/exec-plans/active/README.md](/models/project/codex/SQLForge/docs/exec-plans/active/README.md)
- Completed plans: [docs/exec-plans/completed/2026-04-13-initial-scaffold.md](/models/project/codex/SQLForge/docs/exec-plans/completed/2026-04-13-initial-scaffold.md)
- Technical debt: [docs/exec-plans/tech-debt-tracker.md](/models/project/codex/SQLForge/docs/exec-plans/tech-debt-tracker.md)

## Working Rule

- short-lived work can live in the agent session
- anything architectural, cross-cutting, or multi-step should be checked into `docs/exec-plans/`
- when a large module reaches a coherent delivery slice, record it under `docs/exec-plans/completed/`
- git commit and git tag checkpoints now happen per large module delivery, not per small feature slice
