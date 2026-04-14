# 2026-04-14 Workflow Baseline Persistence

## Goal

Replace the in-memory BI workflow baseline path with durable local persistence so repeated release evaluations can compare against previous results even after backend restarts.

## Delivered

1. Added a `WorkflowBaselineRepository` abstraction for BI workflow baseline storage.
2. Added a file-backed repository implementation with Spring Boot wiring and configurable storage path.
3. Kept the in-memory implementation available for focused unit tests.
4. Added repository coverage for save-and-reload behavior.
5. Updated README, product specs, quality notes, tech debt tracking, and checkpoint records to reflect the new durable baseline path.

## Checkpoint

- tag: `checkpoint/2026-04-14-workflow-baseline-persistence`
