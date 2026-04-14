# BI Release Evaluation

## Problem

BI teams need to know whether a new SQL can be released without blowing up Trino latency or cluster cost.

## Inputs

- tenant ID
- SQL text
- target SLA
- target concurrency
- data profile

## Output

- risk level
- whether benchmark is required
- benchmark plan
- benchmark analysis
- final decision: `approved`, `needs-optimization`, or `blocked`

## Success Criteria

- same SQL should produce a stable fingerprint
- decision should be traceable through report fields
- high-risk queries should not bypass benchmarking

## Initial API Shape

- `POST /api/v1/workflows/bi-release`
- `GET /api/v1/workflows/bi-release/baselines`

## Current Delivery

- Java Spring Boot backend exposes the BI release workflow as the mainline implementation.
- Output includes SQL assessment, benchmark plan, benchmark analysis, previous baseline comparison, and release decision.
- Frontend workspace exposes the BI release workflow with editable request inputs and JSON result review.
- Previous baseline summaries are now persisted to local backend storage so second-run comparisons survive process restarts.
- Persisted BI baseline records are now queryable through a dedicated history API and visible in the workflow workspace as a refreshable baseline-history panel.
- Tenant profile resolution now flows through a shared provider boundary instead of duplicate hard-coded maps inside workflow services.
- BI release output now includes executor provider contract metadata so the platform can evolve from dry-run planning to real benchmark execution.
