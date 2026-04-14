# Tech Debt Tracker

## Open

| Area | Debt | Impact | Next Step |
|:---|:---|:---|:---|
| SQL Parsing | Heuristic parsing instead of real Trino AST | Risk scoring can be inaccurate | Add parser adapter and typed AST contract |
| Persistence | File-backed baselines exist, but no database-backed repository yet | Durable local state exists but no shared production store | Add storage interface extensions and PostgreSQL implementation |
| Execution | No real benchmark executor | Platform cannot run live tests yet | Add executor provider contracts |
| Docs Enforcement | Dedicated repository knowledge lint exists, but it is still local-only | Drift is reduced locally but not yet enforced in CI or pre-commit hooks | Wire the lint into CI and optional pre-commit tooling |
| Tenancy | Static tenant profiles | No real quota/control-plane integration | Add tenant config provider |

## Closed

- Initial project scaffold completed on 2026-04-13
- Requirement decomposition, recommendation engine, and plan stability workflow completed on 2026-04-13
- Dedicated repository knowledge lint completed on 2026-04-14
