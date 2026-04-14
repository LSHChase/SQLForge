# SQLForge Agent Map

This repository follows an agent-first layout inspired by Harness engineering.

Read this file as a map, not an encyclopedia.

## Start Here

- [ARCHITECTURE.md](/models/project/codex/SQLForge/ARCHITECTURE.md): top-level code and dependency map
- [README.md](/models/project/codex/SQLForge/README.md): how to run the project
- [docs/architecture.md](/models/project/codex/SQLForge/docs/architecture.md): domain analysis and initialization rationale
- [docs/references/task-start-requirements.md](/models/project/codex/SQLForge/docs/references/task-start-requirements.md): mandatory Harness engineering reading before every requirement task

## Mandatory Before Every Requirement Task

- Read [docs/references/task-start-requirements.md](/models/project/codex/SQLForge/docs/references/task-start-requirements.md).
- Read the relevant architecture, product-spec, and execution-plan docs before implementation.
- Follow the documented Harness engineering rules instead of relying on prompt-only assumptions.

## Repository Knowledge Is The Source Of Truth

Use repository-local docs before making assumptions.

- [DESIGN.md](/models/project/codex/SQLForge/DESIGN.md): design intent and UX posture for the platform
- [PRODUCT_SENSE.md](/models/project/codex/SQLForge/PRODUCT_SENSE.md): product priorities and non-goals
- [PLANS.md](/models/project/codex/SQLForge/PLANS.md): where to find active/completed execution plans
- [QUALITY_SCORE.md](/models/project/codex/SQLForge/QUALITY_SCORE.md): quality grades and current gaps
- [RELIABILITY.md](/models/project/codex/SQLForge/RELIABILITY.md): runtime and validation expectations
- [SECURITY.md](/models/project/codex/SQLForge/SECURITY.md): security boundaries for current and future integrations

## Docs Layout

- [docs/design-docs/index.md](/models/project/codex/SQLForge/docs/design-docs/index.md): design doc index
- [docs/product-specs/index.md](/models/project/codex/SQLForge/docs/product-specs/index.md): product spec index
- [docs/exec-plans/tech-debt-tracker.md](/models/project/codex/SQLForge/docs/exec-plans/tech-debt-tracker.md): tracked technical debt
- [docs/generated/db-schema.md](/models/project/codex/SQLForge/docs/generated/db-schema.md): generated artifact placeholder
- [docs/references/repository-conventions.md](/models/project/codex/SQLForge/docs/references/repository-conventions.md): repo conventions for future agents
- [docs/references/task-start-requirements.md](/models/project/codex/SQLForge/docs/references/task-start-requirements.md): mandatory task-start checklist and compliance rules

## Code Navigation

- `src/bootstrap/`: composition root
- `src/app/`: HTTP entry layer
- `src/modules/*/domain/`: domain logic and heuristics
- `src/modules/*/application/`: use-case orchestration
- `src/infrastructure/`: repository and connector implementations
- `src/shared/`: small shared utilities

## Architectural Rules

- Keep business logic in `src/modules`.
- Treat `src/bootstrap` as the only composition root.
- Prefer adding repo-local docs over burying intent in prompts.
- New cross-domain behavior should enter through application-layer use cases.
- Domain code should stay legible, deterministic, and dependency-light.
- If a new rule matters repeatedly, encode it in tests or tooling.

## Validation

Before concluding a substantial change:

- run `npm test`
- verify docs reflect the current code paths
- keep AGENTS short and move durable knowledge into `docs/`
