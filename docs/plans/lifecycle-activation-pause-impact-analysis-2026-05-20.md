# Lifecycle Activate / Pause Simplification Impact Analysis

Task: `USER-CN-SIMPLIFY-LIFECYCLE-ACTIVATE-PAUSE-20260520`

Status: implemented; closeout gates in progress.

Raw requirement: `docs/references/raw-requirements/USER-CN-SIMPLIFY-LIFECYCLE-ACTIVATE-PAUSE-20260520.md`

## Objective

Collapse operator-facing approval, publish, revoke, unpublish, apply, verify, and rollback lifecycle actions into two product actions: activate and pause. Keep safety evidence, validation history, runtime binding evidence, and repository governance terminology intact where those terms describe engineering process rather than product runtime lifecycle.

## Human Decision

The human-selected path is Option C, a full breaking state-machine migration:

- Approval is not a mandatory gate behind activate.
- SQL rewrite `unpublish` fully folds into pause.
- Acceleration plan `apply`, `verify`, and `rollback` fold into activate and pause.
- Existing runtime proof is preserved as `activationEvidence` and `pauseEvidence`.
- MV artifact `rollbackSql` remains artifact evidence for external recovery, not an operator lifecycle action.

## Impact Resolution

| Surface | Previous model | Implemented model | Primary artifacts |
|:---|:---|:---|:---|
| SQL rewrite record API | `/review`, `/publish`, `/pause`, `/unpublish`, publish eligibility, `publishStatus` | `/activation-eligibility`, `/activate`, `/pause`, `activationStatus` | `SqlRewriteRecordController`, `SqlRewriteRecordApplicationService`, `RewriteActivationEligibilityPolicy` |
| SQL rewrite runtime binding | publish / unpublish naming, `UNPUBLISHED` state | activate / pause naming, `ACTIVE` / `PAUSED` runtime truth | `QueryExecutionInternalController`, `QueryExecutionRuntimeRewriteBindingService`, `RuntimeRewriteBindingStatus` |
| Acceleration plan API | approval, apply, verify, rollback | activate, pause, `READY/ACTIVE/PAUSED/ACTIVATE_FAILED/PAUSE_FAILED` | `AccelerationPlanController`, `AccelerationPlan`, `QueryExecutionAccelerationRuntimeService` |
| Query execution acceleration binding | approved acceleration binding | activated acceleration binding | `ActivatedAccelerationBinding`, `QueryExecutionBoundaryDefinition` |
| DB schema and migrations | approval / publish / rollback columns and status names | activation / pause columns and status names | `sql/init-schema.sql`, `V20260425_002__acceleration_plan_governance.sql`, `V20260511_001__sql_rewrite_record_review_publish_runtime_fields.sql` |
| Frontend recommendation workflow | approve/reject/publish/pause/unpublish UI | activate/pause UI with activation eligibility evidence | `RecommendationCenterView.vue`, `runtimeGateApi.js`, locale files |
| Browser and contract checks | publish/pause/unpublish expectations | activation/pause expectations | `check-recommendation-page-contract.mjs`, `check-production-rewrite-closed-loop-browser-smoke.mjs`, `check-dev-frontend.mjs` |
| Governance history | publish status snapshot wording | activation status snapshot wording | governance history services, mapper, schema, frontend SQL history copy |

## Prompt-To-Artifact Checklist

| Requirement from prompt | Evidence artifact | Status |
|:---|:---|:---|
| Preserve the human requirement | `docs/references/raw-requirements/USER-CN-SIMPLIFY-LIFECYCLE-ACTIVATE-PAUSE-20260520.md` | Done |
| Establish governed context | `python3 scripts/foreman.py preflight`, `python3 scripts/foreman.py preflight --task USER-CN-SIMPLIFY-LIFECYCLE-ACTIVATE-PAUSE-20260520`, `tasks.md` | Done |
| Deeply analyze docs, rules, requirements, code, scripts, and validation | This file, authority docs read, repo-wide grep inventory, task progress log | Done |
| If core functionality is affected, list issues and solutions, then wait for human decision | Original inventory and Option C decision recorded above and in `tasks.md` | Done |
| Replace approval/publish/revoke/rollback product actions with activate/pause | Backend controllers/services/domain, frontend API/view/copy, smoke scripts, SQL schema, architecture/product docs | Done |
| Do not corrupt repository governance terms | Foreman, validation, release, dispatch, connector, and artifact recovery terminology retained where non-product lifecycle | Done |
| Preserve evidence | `activationEvidence`, `pauseEvidence`, `lastActivationStatusTrace`, validation runs, runtime binding ids/rule versions | Done |
| Verify implementation | Java, frontend, smoke, and repository gates listed below | In progress until closeout gates finish |

## Verification Evidence

Completed validation:

- `mvn -q -pl sql-optimization,governance,query-execution,sqlforge-shared -am test`
- `mvn -q clean test`
- `node scripts/check-recommendation-page-contract.mjs`
- `node scripts/check-production-rewrite-closed-loop-browser-smoke.mjs`
- `node scripts/check-dev-frontend.mjs`
- `npm run lint`
- `npm run build`
- `node scripts/lint-repository-knowledge.js`
- `git diff --check`
- `python3 scripts/foreman.py validate USER-CN-SIMPLIFY-LIFECYCLE-ACTIVATE-PAUSE-20260520`
- `python3 scripts/task_audit.py --check --phase pre-closeout`

Pending closeout validation:

- `python3 scripts/foreman.py closeout USER-CN-SIMPLIFY-LIFECYCLE-ACTIVATE-PAUSE-20260520`
- `python3 scripts/task_audit.py --check --phase post-closeout`
