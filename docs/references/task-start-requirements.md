# Task Start Requirements

## Purpose

This document is mandatory reading before every new requirement task.

SQLForge follows Harness engineering discipline:

- docs-first
- architecture-explicit
- execution-plan driven
- repository knowledge over prompt-only assumptions
- traceable delivery through completion records, commits, and tags

## Required Read Order

Before implementation starts, read these repository documents in order:

1. [AGENTS.md](/models/project/codex/SQLForge/AGENTS.md)
2. [ARCHITECTURE.md](/models/project/codex/SQLForge/ARCHITECTURE.md)
3. [README.md](/models/project/codex/SQLForge/README.md)
4. [docs/architecture.md](/models/project/codex/SQLForge/docs/architecture.md)
5. Relevant docs from [docs/product-specs/index.md](/models/project/codex/SQLForge/docs/product-specs/index.md)
6. Relevant docs from [docs/design-docs/index.md](/models/project/codex/SQLForge/docs/design-docs/index.md)
7. [PLANS.md](/models/project/codex/SQLForge/PLANS.md)
8. Related active or completed execution-plan records under `docs/exec-plans/`
9. [docs/references/repository-conventions.md](/models/project/codex/SQLForge/docs/references/repository-conventions.md)

## Mandatory Rules

1. Do not start implementation before reading the required documents.
2. Do not invent requirements when repository docs already define them.
3. If a requirement changes architecture, update docs in the same delivery slice.
4. If work spans multiple steps or domains, create or update an execution plan first.
5. When a feature family is complete, add a completion record, create a git commit, create a git tag, and write the tag into the record.
6. Keep AGENTS short; move durable instructions into `docs/`.
7. Encode repeatable rules in tests or tooling whenever possible.

## Compliance Gate

A requirement task is not considered properly started unless:

- the mandatory documents were read
- the relevant product and design scope was identified
- the plan and checkpoint obligations were acknowledged
- implementation matches the documented architecture boundaries
