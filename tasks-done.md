# SQLForge Completed Tasks

本文件只记录已完成、已验证、已归档的任务。

## Done

### HARN-001: Codify foreman workflow and task ledger

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-19
- Commit subject: `chore(harness): HARN-001 codify foreman workflow and task ledger`
- Scope: add SQLForge task ledger, inbox, agent config, operations docs, generated repo map, validation log, task audit script, and append-only harness governance rules.
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `node scripts/check-frontend-backend-separation.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-19: audited current repository knowledge, rule continuity, validation constraints, and root Git boundary before applying harness governance changes.
  - 2026-04-19: added task ledger files, operations docs, generated repo map, agent config, validation log, exec-plan directories, and task audit automation.
  - 2026-04-19: appended `R-156` to `R-161`, updated documentation entry points, and aligned repository knowledge lint with the new governance baseline.
  - 2026-04-19: validated repository knowledge lint, frontend-backend separation, and task audit, then archived the task for single-task commit closeout.

### HARN-002: Close remaining harness doc drift

- Status: done
- Priority: 1
- Depends on: HARN-001
- Completed at: 2026-04-20
- Commit subject: `docs(harness): HARN-002 close remaining harness doc drift`
- Scope: align the root and docs README summaries with the current harness governance baseline and confirmed service direction.
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: audited post-HARN-001 residual drift and isolated two remaining stale README summaries for targeted closeout.
  - 2026-04-20: updated `docs/README.md` to stop advertising stale rule and service counts, and updated `README.md` to describe the current repository baseline instead of the old initialization state.
  - 2026-04-20: validated repository knowledge lint and task audit, then archived the task for single-task commit closeout.

### DOC-GOV-001: Establish document truth baseline and readiness governance

- Status: done
- Priority: 1
- Depends on: HARN-002
- Completed at: 2026-04-20
- Commit subject: `docs(governance): DOC-GOV-001 establish document truth baseline`
- Scope: add the document truth baseline, implementation readiness spec, service capability map, governance retrospective template and baseline retrospective, then wire them into the docs entry points, master plan, coverage matrix, rule consumption notes, history ledger, repo map, and repository knowledge lint.
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `node scripts/check-frontend-backend-separation.js`
  - `mvn -B test`
  - `npm run build`
  - `npm run lint`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: audited the repository truth against the initialization architecture, rules, plans, ADR index, access-control spec, and deployment docs to isolate drift between current facts and confirmed targets.
  - 2026-04-20: added `document-truth-baseline.md`, `implementation-readiness.md`, `service-capability-map.md`, `retrospective-template.md`, and `document-governance-retrospective-2026-04-20.md`.
  - 2026-04-20: updated `docs/README.md`, `docs/plans/README.md`, `docs/plans/master-execution-plan.md`, `docs/plans/document-coverage-matrix.md`, `docs/architecture/init.md`, `docs/generated/repo-map.md`, `docs/rules/codex-rules.md`, `docs/references/human-constraint-history.md`, and `scripts/lint-repository-knowledge.js` to consume the new governance layer.
  - 2026-04-20: wrote baseline validation evidence into `docs/quality/validation-log.md`, confirmed documentation coverage completeness, and prepared the batch for git closeout.
