# Quality Score

Current grade is intentionally blunt so future runs can improve it.

## Domain Grades

| Area | Grade | Notes |
|:---|:---|:---|
| Access | B | SQL heuristic engine now covers more anti-pattern validations, no real AST parser yet |
| Data Construction | C+ | Sampling plan is modeled, no Hudi/HMS integration yet |
| Execution | B- | Benchmark matrix now covers more dimensions, no real executor yet |
| Intelligence | B | Core models, recommendations, and plan stability analysis exist; heuristics still need calibration |
| Reporting | B | Reports and workflows exist with stable shapes and action outputs |
| Tenancy | C | Tenant profiles are static in-memory |
| Repo Knowledge | B | Harness-style docs added, needs continuous gardening |
| Mechanical Enforcement | B- | Structural tests exist, no custom lint yet |

## Highest Priority Gaps

1. Replace heuristic SQL parsing with real Trino parsing.
2. Add a real persistence layer for baselines and reports.
3. Add connector contracts for Trino, Hudi, HMS, and K8s.
4. Expand structural enforcement from tests to dedicated lint tooling.
