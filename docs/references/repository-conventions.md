# Repository Conventions

## Code

- prefer explicit modules over deeply shared helpers
- keep domain logic in `src/modules/*/domain`
- keep orchestration in `src/modules/*/application`
- compose dependencies only in `src/bootstrap`
- use UTF-8 for text files
- use Unix/LF line endings
- place production frontend code in `frontend/`
- place production backend code in `backend/`

## Docs

- if a decision matters later, write it down in-repo
- keep indexes current
- use execution plans for multi-step work

## Delivery

- follow harness engineering: docs-first, architecture-explicit, traceable checkpoints
- when a complete feature family lands, create a completion record in `docs/exec-plans/completed/`
- after that record exists, create one git commit for the feature family
- create one git tag checkpoint for the same commit
- write the checkpoint tag into the completion record so the repository can trace implementation to git history

## Enforcement

- architectural rules belong in tests or tooling
- docs structure should be validated, not assumed
