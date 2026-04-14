# SQL Intent Analysis

## Problem

Stress-testing preparation needs a pure structural way to classify incoming SQL before any live execution is available.

## Inputs

- one or more SQL statements
- optional statement ids or labels

## Output

- deterministic SQL fingerprint
- statement type and structural profile
- intent tags based on SQL shape only
- pressure-oriented load classification
- structural alerts that operators can review before deeper testing

## Success Criteria

- the same SQL produces the same fingerprint and intent summary
- analysis does not require database connectivity or query execution
- output is readable enough to group daily SQL samples into pressure buckets

## Initial API Shape

- `POST /api/v1/sql/intent-analysis`
- `POST /api/v1/sql/intent-analysis/daily-batch`
- `POST /api/v1/sql/intent-analysis/pressure-plan`
- `POST /api/v1/sql/intent-analysis/scenario-blueprint`
- `POST /api/v1/sql/intent-analysis/execution-manifest`
- `POST /api/v1/sql/intent-analysis/campaign-schedule`
- `POST /api/v1/sql/intent-analysis/run-package`
- `POST /api/v1/sql/intent-analysis/briefing-report`

## Current Delivery

- Java Spring Boot backend supports batch SQL intent analysis from input SQL text.
- Output includes structural profile, intent tags, pressure-oriented load class, and structural alerts.
- Frontend provides a minimal analysis panel for manual SQL inspection.
- Daily SQL samples can be pasted as raw text and split by semicolon or blank line before structural analysis.
- The backend can derive a pressure-preparation plan with candidate sets, concurrency ladder, and sampling rules from daily SQL batches.
- The backend can derive staged scenario blueprints with workload mix and operator checklist guidance from daily SQL batches.
- The backend can derive machine-readable execution manifests with stage criteria, metric focus, and global guardrails.
- The backend can derive campaign schedules with stage windows, promotion gates, fallback actions, and handoff notes.
- The backend can assemble a unified run package with artifact pointers, recommended files, and handoff checklist output.
- The backend can derive an operator-readable briefing report with summary, risks, next actions, and review agenda.
