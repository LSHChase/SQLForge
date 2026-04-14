# 2026-04-14 SQL Intent Batch Intake

## Goal

Extend the Java SQL intent module so operators can submit daily SQL samples in bulk for pure structural analysis.

## Delivered

1. Added a raw-text daily batch SQL intent API on the Java backend.
2. Added statement splitting by semicolon and blank-line fallback for daily SQL pastes.
3. Reused pure-structure intent analysis for each parsed statement and exposed batch summary signals.
4. Added frontend single/batch mode switching for manual daily SQL analysis.
5. Included local dev CORS patterns so browser-based testing remains stable across Vite ports.

## Checkpoint

- tag: `checkpoint/2026-04-14-sql-intent-batch-intake`
