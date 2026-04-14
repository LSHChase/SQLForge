# Quality Score

Current grade is intentionally blunt so future runs can improve it.

## Domain Grades

| Area | Grade | Notes |
|:---|:---|:---|
| Access | B | SQL intent analysis now uses a parser adapter and typed AST contract, but the active parser is still heuristic |
| Data Construction | C+ | Sampling plan is modeled, no Hudi/HMS integration yet |
| Execution | B- | Benchmark matrix now covers more dimensions and exposes executor contracts, but only dry-run execution is available |
| Intelligence | B | Core models, recommendations, and plan stability analysis exist; heuristics still need calibration |
| Reporting | B | Reports and workflows exist with stable shapes and action outputs; BI baselines now persist locally and are visible through a dedicated history API/workspace panel |
| Tenancy | C+ | Tenant profiles now flow through a shared provider boundary, but the backing data is still static |
| Repo Knowledge | B | Harness-style docs added, now backed by a dedicated repository knowledge lint |
| Mechanical Enforcement | B | Structural tests exist and repository knowledge now has dedicated lint tooling |

## Highest Priority Gaps

1. Replace the heuristic SQL parser adapter implementation with a real Trino parser.
2. Extend local file persistence into a shared persistence layer for baselines and reports.
3. Add connector contracts for Trino, Hudi, HMS, and K8s.
4. Extend the repository knowledge lint into CI and more content-aware checks.
