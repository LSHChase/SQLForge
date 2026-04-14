# Tech Debt Tracker

## Open

| Area | Debt | Impact | Next Step |
|:---|:---|:---|:---|
| SQL Parsing | Parser adapter and typed AST contract exist, but parsing is still heuristic rather than real Trino AST | Swap-in path exists, but risk scoring can still be inaccurate | Add real Trino parser implementation behind the adapter |
| Persistence | File-backed baselines exist, but no database-backed repository yet | Durable local state exists but no shared production store | Add storage interface extensions and PostgreSQL implementation |
| Execution | No real benchmark executor | Platform cannot run live tests yet | Add executor provider contracts |
| Docs Enforcement | Dedicated repository knowledge lint exists, but it is still local-only | Drift is reduced locally but not yet enforced in CI or pre-commit hooks | Wire the lint into CI and optional pre-commit tooling |
| Tenancy | Shared tenant profile provider exists, but profiles are still static in-process | Duplication is removed, but there is no real quota or control-plane integration | Replace the static provider with config-backed or control-plane-backed tenant sources |

## Closed

- Initial project scaffold completed on 2026-04-13
- Requirement decomposition, recommendation engine, and plan stability workflow completed on 2026-04-13
- Dedicated repository knowledge lint completed on 2026-04-14
