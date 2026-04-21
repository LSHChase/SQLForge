# SQLForge Repo Map

> Generated snapshot for AI navigation. Refresh when layout or module boundaries change.

Last refreshed: `2026-04-20`

## Root

- `AGENTS.md`: short operational pointer
- `README.md`: human-facing project entry
- `tasks.md`: active task ledger
- `tasks-done.md`: completed task archive
- `INBOX.md`: human-decision inbox
- `.agent/config.json`: machine-readable foreman defaults
- `package.json`, `vite.config.js`, `src/`: root Vue frontend
- `pom.xml`: Maven parent
- `sql/`: schema and seed SQL
- `scripts/`: repository automation and validation utilities

## Backend Modules

- `governance/`: current governance backend baseline
- `query-execution/`: query execution service boundary skeleton
- `sqlforge-shared/`: shared common layer baseline

## Docs

- `docs/architecture/`: architecture baseline and messaging abstraction
- `docs/architecture/service-capability-map.md`: target 4-service allocation mapped to current modules
- `docs/architecture/service-interface-contract-baseline.md`: interface-level baseline for context fields, error-code ownership, DTO/event boundaries, and audit payloads
- `docs/rules/`: append-only rule ledger
- `docs/quality/`: validation rules, quality baselines, validation log
- `docs/operations/`: foreman workflow, collaboration, local dev, closeout, best practices
- `docs/plans/`: master execution plan, historical phase plan, truth baseline, gap matrix, phase prerequisite matrix, readiness spec, coverage matrices, task governance extension, retrospective template, governance retrospectives
- `docs/exec-plans/active/`: approved active execution plans
- `docs/exec-plans/completed/`: completed or abandoned execution plans
- `docs/generated/`: generated navigation artifacts
- `docs/security/`: compliance and access-control specifications
- `docs/adr/`: architecture decision records
- `docs/references/`: raw requirements archive and human constraint history
