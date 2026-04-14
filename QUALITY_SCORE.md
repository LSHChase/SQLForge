# Quality Score

Current grade is intentionally blunt so future runs can improve it.

## Domain Grades

| Area | Grade | Notes |
|:---|:---|:---|
| Access | B | SQL heuristic engine now covers more anti-pattern validations, no real AST parser yet |
| Data Construction | C+ | Sampling plan is modeled, no Hudi/HMS integration yet |
| Execution | B- | Benchmark matrix now covers more dimensions, no real executor yet |
| Intelligence | B | Core models, recommendations, and plan stability analysis exist; heuristics still need calibration |
| Reporting | B | Reports and workflows exist with stable shapes and action outputs; BI baselines now persist locally |
| Tenancy | C | Tenant profiles are static in-memory |
| Repo Knowledge | B | Harness-style docs added, now backed by a dedicated repository knowledge lint |
| Mechanical Enforcement | B | Structural tests exist and repository knowledge now has dedicated lint tooling |

## Highest Priority Gaps

1. Replace heuristic SQL parsing with real Trino parsing.
2. Extend local file persistence into a shared persistence layer for baselines and reports.
3. Add connector contracts for Trino, Hudi, HMS, and K8s.
4. Extend the repository knowledge lint into CI and more content-aware checks.
