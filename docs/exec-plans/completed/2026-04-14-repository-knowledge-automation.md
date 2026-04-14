# 2026-04-14 Repository Knowledge Automation

## Goal

Move repository knowledge enforcement out of local-only usage by wiring the existing lint and primary verification chain into CI and optional pre-commit tooling.

## Delivered

1. Added root verification scripts for backend tests, frontend build, and a combined all-up validation path.
2. Added a GitHub Actions workflow that runs repository tests, backend tests, and frontend production build.
3. Added an optional `.githooks/pre-commit` hook that runs the repository knowledge lint before commit.
4. Added a small installer script so local clones can enable the managed hooks path with one command.
5. Updated README, quality notes, tech debt tracking, active plan index, and checkpoints to reflect the new enforcement path.

## Checkpoint

- tag: `checkpoint/2026-04-14-repository-knowledge-automation`
