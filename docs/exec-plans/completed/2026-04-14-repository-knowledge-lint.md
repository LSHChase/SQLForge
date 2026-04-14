# 2026-04-14 Repository Knowledge Lint

## Goal

Move repository knowledge enforcement beyond ad hoc tests by adding a dedicated lint entrypoint for the Harness-style documentation structure.

## Delivered

1. Added a standalone repository knowledge lint script that validates the required doc map and task-start rules.
2. Wired the lint into the root npm scripts as `npm run lint:docs`.
3. Reused the same lint logic from the repository knowledge test so enforcement stays consistent.
4. Updated README, quality notes, tech debt tracking, and checkpoints to reflect the new tooling path.

## Checkpoint

- tag: `checkpoint/2026-04-14-repository-knowledge-lint`
