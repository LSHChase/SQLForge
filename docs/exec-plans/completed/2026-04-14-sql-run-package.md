# 2026-04-14 SQL Run Package

## Goal

Assemble the staged SQL stress-preparation artifacts into a single run package that can be handed to future runners or operators.

## Delivered

1. Added a Java backend run-package API derived from campaign schedules.
2. Added artifact pointers, recommended filenames, export sections, and handoff checklist output.
3. Added a frontend batch view for run-package review.
4. Kept the package deterministic and pure-structure derived from prior SQL planning artifacts.
5. Updated docs so the SQL stress-preparation path now reaches a unified handoff package.

## Checkpoint

- tag: `checkpoint/2026-04-14-sql-run-package`
