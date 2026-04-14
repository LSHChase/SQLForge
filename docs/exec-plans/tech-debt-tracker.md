# Tech Debt Tracker

## Open

| Area | Debt | Impact | Next Step |
|:---|:---|:---|:---|
| SQL Parsing | Heuristic parsing instead of real Trino AST | Risk scoring can be inaccurate | Add parser adapter and typed AST contract |
| Persistence | In-memory repository only | No durable baselines or history | Add storage interface and PostgreSQL implementation |
| Execution | No real benchmark executor | Platform cannot run live tests yet | Add executor provider contracts |
| Docs Enforcement | No dedicated doc linter yet | Docs can drift silently | Add repository knowledge structure checks |
| Tenancy | Static tenant profiles | No real quota/control-plane integration | Add tenant config provider |

## Closed

- Initial project scaffold completed on 2026-04-13
- Requirement decomposition, recommendation engine, and plan stability workflow completed on 2026-04-13
