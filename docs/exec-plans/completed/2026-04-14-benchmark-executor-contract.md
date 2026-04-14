# 2026-04-14 Benchmark Executor Contract

## Goal

Introduce an execution-provider boundary for BI benchmark planning so the workflow can move from dry-run planning to real benchmark execution without reshaping its result contract.

## Delivered

1. Added benchmark executor provider and registry contracts.
2. Added a dry-run executor provider as the default implementation.
3. Updated the BI release workflow to emit `executorPlan` metadata in both workflow and report output.
4. Extended backend tests to assert the new executor contract surface.
5. Updated README, product specs, quality notes, tech debt tracking, and checkpoints to reflect the new execution boundary.

## Checkpoint

- tag: `checkpoint/2026-04-14-benchmark-executor-contract`
