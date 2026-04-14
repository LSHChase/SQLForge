# 2026-04-14 Workflow Baseline History

## Goal

Expose persisted BI workflow baselines through a dedicated backend query path and a frontend workflow workspace panel so operators can review recent baseline history without rerunning the workflow.

## Delivered

1. Extended the workflow baseline repository contract with list support for persisted baseline records.
2. Added BI workflow service and controller support for `GET /api/v1/workflows/bi-release/baselines`.
3. Added backend test coverage for baseline listing in both repository and service layers.
4. Added a bilingual frontend baseline-history panel with manual refresh and automatic refresh after BI workflow execution.
5. Updated README, product specs, quality notes, and checkpoints to reflect the new baseline-history surface.

## Checkpoint

- tag: `checkpoint/2026-04-14-workflow-baseline-history`
