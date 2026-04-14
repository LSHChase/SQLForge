# 2026-04-14 SQL Pressure Plan Orchestration

## Goal

Turn daily SQL intent batches into operator-readable pressure-preparation plans for stress-testing.

## Delivered

1. Added a Java backend pressure-plan API derived from pure structural SQL analysis.
2. Added cohorting by load class and complexity with candidate smoke, standard, and heavy sets.
3. Added concurrency ladder, data-scale guidance, and sampling rules for daily SQL batches.
4. Added frontend batch pressure-plan generation and result panels.
5. Updated docs so the SQL stress-preparation workflow is visible in the repository runbook and specs.

## Checkpoint

- tag: `checkpoint/2026-04-14-sql-pressure-plan-orchestration`
