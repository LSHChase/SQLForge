# Checkpoints

| Date | Feature Family | Tag | Notes |
|:---|:---|:---|:---|
| 2026-04-13 | Vue Spring foundation | `checkpoint/2026-04-13-vue-spring-foundation` | Split frontend/backend scaffold, UTF-8 + LF rules, engine metadata API |
| 2026-04-13 | Harness task-start governance | `checkpoint/2026-04-13-harness-task-start-governance` | Mandatory task-start reading order, AGENTS entry point, repository enforcement tests |
| 2026-04-13 | Engine connection foundation | `checkpoint/2026-04-13-engine-connection-foundation` | Backend validation and registration APIs, frontend connection management UI |
| 2026-04-13 | Connection persistence and secret hygiene | `checkpoint/2026-04-13-connection-persistence-and-secret-hygiene` | File-backed connection metadata persistence, no raw password persistence, backend repository tests |
| 2026-04-13 | Connection probe foundation | `checkpoint/2026-04-13-connection-probe-foundation` | Real TCP connectivity probe API, JDBC URL diagnostics, frontend probe result panel |
| 2026-04-13 | Engine profiles and ARM delivery | `checkpoint/2026-04-13-engine-profiles-and-arm-delivery` | Rich engine connection profiles, frontend defaults, Dockerfiles, compose-based split deployment |
| 2026-04-14 | Connection execution module | `checkpoint/2026-04-14-connection-execution-module` | Saved-connection lifecycle, JDBC-aware probe, SQL preview, activity history, module overview |
| 2026-04-14 | Java workflow migration | `checkpoint/2026-04-14-java-workflow-migration` | Spring Boot mainline for BI release, capacity planning, and plan stability workflow APIs |
| 2026-04-14 | SQL intent analysis foundation | `checkpoint/2026-04-14-sql-intent-analysis-foundation` | Pure-structure SQL intent API, pressure classification, and manual analysis panel |
| 2026-04-14 | SQL intent batch intake | `checkpoint/2026-04-14-sql-intent-batch-intake` | Daily SQL raw-text intake, batch splitting, batch summary, and browser-safe local testing |
| 2026-04-14 | SQL pressure plan orchestration | `checkpoint/2026-04-14-sql-pressure-plan-orchestration` | Pressure cohorts, candidate test sets, concurrency ladder, and sampling rules for daily SQL batches |
| 2026-04-14 | SQL scenario blueprint | `checkpoint/2026-04-14-sql-scenario-blueprint` | Staged stress-test blueprint, workload mix guidance, and operator checklist for daily SQL batches |
| 2026-04-14 | SQL execution manifest | `checkpoint/2026-04-14-sql-execution-manifest` | Machine-readable execution manifest with stage criteria, metric focus, and guardrails |
| 2026-04-14 | SQL campaign schedule | `checkpoint/2026-04-14-sql-campaign-schedule` | Timed stage windows, promotion gates, fallback actions, and handoff notes |
| 2026-04-14 | SQL run package | `checkpoint/2026-04-14-sql-run-package` | Unified handoff package with artifact pointers, recommended files, export sections, and checklist |
| 2026-04-14 | SQL briefing report | `checkpoint/2026-04-14-sql-briefing-report` | Operator-readable final review report with summary, risks, actions, and agenda |
| 2026-04-14 | SQL readiness gate and bilingual workspace | `checkpoint/2026-04-14-sql-readiness-gate-bilingual-workspace` | Structure-based readiness decision API plus multi-menu zh/en frontend workspace for connections, analysis, planning, and handoff |
| 2026-04-14 | Workflow workspace delivery | `checkpoint/2026-04-14-workflow-workspace-delivery` | Frontend Java workflow workspace for BI release evaluation, capacity planning, and plan-stability analysis with zh/en navigation |
| 2026-04-14 | Workflow baseline persistence | `checkpoint/2026-04-14-workflow-baseline-persistence` | File-backed BI workflow baseline repository so previous-baseline comparisons survive backend restarts |
| 2026-04-14 | Repository knowledge lint | `checkpoint/2026-04-14-repository-knowledge-lint` | Dedicated repository knowledge lint script plus test integration for Harness-style doc structure enforcement |
| 2026-04-14 | SQL parser adapter | `checkpoint/2026-04-14-sql-parser-adapter` | Typed SQL structure AST plus heuristic parser adapter abstraction for intent-analysis and downstream workflow reuse |
| 2026-04-14 | Tenant profile provider | `checkpoint/2026-04-14-tenant-profile-provider` | Shared tenant profile provider boundary for SQL assessment and BI workflow services, replacing duplicated hard-coded tenant maps |
