# 2026-04-14 Workflow Workspace Delivery

## Goal

Bring the existing Java workflow APIs into the new multi-menu frontend so operators can run release, capacity, and plan-stability analysis directly from the browser.

## Delivered

1. Added a dedicated frontend workflow workspace to the bilingual console.
2. Wired BI release evaluation inputs and outputs to `POST /api/v1/workflows/bi-release`.
3. Wired capacity planning inputs and outputs to `POST /api/v1/workflows/capacity-plan`.
4. Wired plan-stability inputs and outputs to `POST /api/v1/workflows/plan-stability`.
5. Updated README, product specs, and checkpoint records so the new workspace is visible in repo-local documentation.

## Checkpoint

- tag: `checkpoint/2026-04-14-workflow-workspace-delivery`
