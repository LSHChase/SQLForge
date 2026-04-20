# SQLForge Completed Tasks

本文件只记录已完成、已验证、已归档的任务。

## Done

### D-TASK-001: 固化查询执行服务边界

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `feat(query-execution): D-TASK-001 solidify query execution service boundary`
- Scope: 建立 `query-execution-service` 独立模块骨架，固化路由、执行控制、轻量解析、轻量改写和已批准加速配置应用的服务边界，不把治理、异步优化或压测主流程重新混入本服务。
- Validation:
  - 服务边界与 ADR 一致性检查
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-001` under explicit human direction after the final `Phase-C` audit cleanup commit `68f7bb1`.
  - 2026-04-20: `Phase-C` exit gate remains blocked by coverage threshold and missing Sonar environment configuration; this task proceeded by explicit human direction and did not rewrite that gate state.
  - 2026-04-20: added `query-execution-service` as an independent Maven module with application/domain/infrastructure/config skeleton, immutable boundary definition, multi-profile configuration, and a boundary application service plus unit test.
  - 2026-04-20: synced `service-capability-map.md`, `c4-overview.md`, `document-truth-baseline.md`, `document-gap-matrix.md`, `master-execution-plan.md`, `frontend-backend-separation-baseline.md`, `repo-map.md`, `README.md`, and `docs/README.md` so the new service carrier is treated as current fact instead of a pure target.
  - 2026-04-20: validation passed with compile, test, static-check, frontend-backend separation check, knowledge lint, and pre-closeout task audit, then the task was archived for single-task git closeout.

### C-TASK-008: 落实租户配置与访问占位能力

- Status: done
- Priority: 2
- Depends on: C-TASK-007
- Completed at: 2026-04-20
- Commit subject: `feat(governance): C-TASK-008 tighten tenant access placeholder policy`
- Scope: 保持 phase 0 最小租户校验闭环，继续把 `governance-service` 收敛为公共管理服务基线，不扩散到其他目标微服务职责。
- Validation:
  - `mvn clean compile`
  - `mvn test`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` after governance-service baseline hardening.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; `TenantAccessLogic`、`TenantConfigApplicationService` 和治理内部租户/数据源检查接口已经存在并通过当前测试链验证，但数据源授权仍是 placeholder，完整角色与资源矩阵仍待后续实现，见 `IMP-005`。
  - 2026-04-20: replaced the old non-empty datasource placeholder with a governance-local explicit placeholder policy under `governance.access-control.placeholder`, added tenant-config role gating and platform-admin override in `TenantConfigApplicationService`, and kept all logic inside `governance-service`.
  - 2026-04-20: validation passed with `mvn clean compile`, `mvn test`, `node scripts/lint-repository-knowledge.js`, and `python3 scripts/task_audit.py --check`, then the task was archived for single-task git closeout.

### C-TASK-009: 规划审计、数据源、调度扩展点

- Status: done
- Priority: 2
- Depends on: C-TASK-008
- Completed at: 2026-04-20
- Commit subject: `feat(governance): C-TASK-009 close governance extension contracts`
- Scope: 只补治理扩展契约和骨架，不提前塞入完整业务实现；保持审计、数据源和调度扩展点的接口、错误码和文档一致。
- Validation:
  - 接口文档和错误码一致
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` as the governance extension-contract follow-up task.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; internal governance contract endpoints for `tenant-scope`、`datasource-access`、`audit/write`、`schedule/extensions` 已存在且测试通过，但仍需把扩展点从当前骨架进一步收口到完整契约，见 `IMP-007`。
  - 2026-04-20: hardened `datasource-access/check`、`audit/write`、`schedule/extensions` contracts in `governance-service`, added explicit contract-stage / implementation-stage metadata, fixed audit required fields (`serviceCode`,`elapsedMs`,`sourceIp`,`userAgent`), and synced `service-interface-contract-baseline.md` plus `access-control-spec.md`.
  - 2026-04-20: validation passed with `mvn clean compile`, `mvn test`, `mvn validate pmd:pmd checkstyle:check`, and `node scripts/lint-repository-knowledge.js`; task remained `in_review` until the message-abstraction closeout under `C-TASK-005` was committed.
  - 2026-04-20: reran compile, test, static-check, knowledge-lint, and pre-closeout task audit after `C-TASK-005`, then archived the governance extension-contract task for single-task git closeout.

### E-TASK-009: 建立临时 AI 交付进度页路由与展示骨架

- Status: done
- Priority: 2
- Depends on: E-TASK-001, E-TASK-002, Phase-C
- Completed at: 2026-04-20
- Commit subject: `feat(frontend): E-TASK-009 complete temporary delivery progress page`
- Scope: 建立 `/delivery-progress` 临时只读页面，展示 AI 编码任务进度且与 `/dashboard` 分离；展示真值只来自 `tasks.md`、`tasks-done.md`、验证日志和执行计划派生快照。
- Validation:
  - `npm run build`
  - `npm run build -- --mode development --outDir dist-dev`
  - `npm run lint`
  - `python3 scripts/task_audit.py --check`
  - `node scripts/lint-repository-knowledge.js`
  - 非生产路由可达
  - 生产默认隐藏
- Progress log:
  - 2026-04-20: instantiated from `Phase-E / E-STORY-004` as the temporary AI delivery progress page task.
  - 2026-04-20: started early by explicit human direction while the repository active wave remains `Phase-C`; execution must preserve `R-166` boundaries and keep `/dashboard` as the official business homepage.
  - 2026-04-20: extended the temporary page with runtime-flag semantics, temporary/non-production navigation badges, and ledger-derived sections for recent changes, pending blockers, and dependency chains without introducing a parallel state source.
  - 2026-04-20: completed current implementation and validation scope for `E-STORY-004`, appended validation evidence, and archived the task after single-task git closeout.

### C-TASK-001: 盘点应抽取的公共能力

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `docs(governance): DOC-GOV-001 establish document truth baseline`
- Scope: 对照 `sqlforge-common`、`governance-service` 与文档边界，产出当前应收敛到 common 的能力清单，明确哪些能力仍留在业务模块。
- Validation:
  - `mvn clean compile`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` as the first executable shared-foundation task.
  - 2026-04-20: `docs/plans/implementation-readiness.md` 与 `docs/architecture/service-capability-map.md` 已把 `sqlforge-common` 应承载的公共能力和禁入边界显式盘点完成，并写入主计划和真值文档。
  - 2026-04-20: strict ledger reconciliation revalidated repository compile and knowledge lint, then archived the inventory task because its implementation and git-history evidence are both present.

### C-TASK-002: 建立 common 包结构

- Status: done
- Priority: 1
- Depends on: C-TASK-001
- Completed at: 2026-04-20
- Commit subject: `refactor(common): C-TASK-002 close shared package structure baseline`
- Scope: 按“领域目录 + 分层子目录”与公共层边界建立 `sqlforge-common` 的目标包结构，不引入服务专属逻辑。
- Validation:
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` after shared-capability inventory.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; `sqlforge-common` 已形成 `async`、`audit`、`config`、`constants`、`context`、`exception`、`log`、`utils` 包结构，且当前验证链 `mvn clean compile`、`mvn test`、`mvn validate pmd:pmd checkstyle:check`、`node scripts/lint-repository-knowledge.js` 已通过，但单任务 git closeout 仍缺失，暂不归档。
  - 2026-04-20: synced `document-truth-baseline.md` so the shared module no longer appears as a placeholder-only module, then archived the package-structure task for single-task git closeout.

### C-TASK-003: 迁移重复或散落能力

- Status: done
- Priority: 1
- Depends on: C-TASK-002
- Completed at: 2026-04-20
- Commit subject: `refactor(common): C-TASK-003 migrate scattered shared capabilities`
- Scope: 把共性能力迁移到 common，仅迁移共性能力，不破坏服务边界。
- Validation:
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` after common package structure settled.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; 旧 `com.company.common` 与治理服务内部重复 common 能力已被当前工作树迁移到 `com.company.sqlforge.common` 并由 `governance-service` 消费，当前验证链 `mvn clean compile`、`mvn test`、`mvn validate pmd:pmd checkstyle:check`、`node scripts/lint-repository-knowledge.js` 已通过，但单任务 git closeout 仍缺失，暂不归档。
  - 2026-04-20: removed the last tracked `com.company.common` sources from `sqlforge-common` and updated `document-gap-matrix.md` so the shared-layer implementation no longer remains as an open gap.

### C-TASK-004: 对齐所有 application-*.yml 职责

- Status: done
- Priority: 2
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `refactor(R-144): sync kafka abstraction to compose, scripts, config, sql, docs, lint`
- Scope: 明确 dev/test/prod 配置职责，补齐 coverage 和 Sonar 执行入口所需的环境说明，不改变生产默认安全语义。
- Validation:
  - `bash scripts/run-coverage.sh --phase report-only`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` to align configuration responsibilities before deeper service hardening.
  - 2026-04-20: dev / main-test / test-resource / prod 的 `messaging.mode` 职责已分别固定为 `DATABASE` / `DATABASE` / `MOCK` / `KAFKA`，并同步到了本地部署和消息抽象文档。
  - 2026-04-20: strict ledger reconciliation reran `bash scripts/run-coverage.sh --phase report-only`, confirmed report generation and profile responsibility consistency, then archived the task.

### C-TASK-005: 固化消息抽象接口实现路线

- Status: done
- Priority: 2
- Depends on: C-TASK-004
- Completed at: 2026-04-20
- Commit subject: `feat(messaging): C-TASK-005 solidify messaging abstraction route`
- Scope: 统一消息接口、配置和实现切换，保持 `DATABASE` / `MOCK` / `KAFKA` 三种模式的边界与契约清晰。
- Validation:
  - mock/database/kafka 契约测试
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/lint-repository-knowledge.js`
  - `bash scripts/run-coverage.sh --phase report-only`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` after profile responsibilities were aligned.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; `MessageProducer` / `MessageConsumer` 抽象、`MessagingConfig` 路由、`Database` / `Mock` / `Kafka` 实现及对应测试已在当前工作树落地，当前验证链 `mvn clean compile`、`mvn test`、`mvn validate pmd:pmd checkstyle:check`、`node scripts/lint-repository-knowledge.js` 和 `bash scripts/run-coverage.sh --phase report-only` 已通过，但当前 Kafka 客户端接入尚未形成单任务 git closeout，暂不归档。
  - 2026-04-20: reran compile, test, static-check, knowledge-lint, coverage, and pre-closeout task audit evidence, then archived the messaging abstraction task for single-task git closeout.

### C-TASK-006: 完成消息流管理接口验证

- Status: done
- Priority: 2
- Depends on: C-TASK-005
- Completed at: 2026-04-20
- Commit subject: `feat(R-144): add database queue admin endpoints and runtime verification`
- Scope: 验证 retry/stats/manual smoke 管理接口，确保消息表与治理管理面闭环可用。
- Validation:
  - `mvn test`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` after message abstraction routing was established.
  - 2026-04-20: `MessageAdminController`、`MessageAdminApplicationServiceTest`、`scripts/manual-message-queue-smoke.sh`、`docs/deliveries/init-completion.md` 与 `docs/deployments/local-setup.md` 已形成管理接口、消息表和人工 smoke 的验证闭环。
  - 2026-04-20: strict ledger reconciliation revalidated the current test chain and knowledge lint, confirmed that runtime verification evidence is already documented, then archived the task.

### C-TASK-007: 对齐现有分层

- Status: done
- Priority: 2
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `refactor(governance): C-TASK-007 close layered governance baseline`
- Scope: 对齐 `governance-service` 当前 controller/application/domain/infrastructure 分层，确保其继续向公共管理服务边界收敛。
- Validation:
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` as the first governance-service hardening task.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; 当前工作树已形成 `controller` / `application` / `domain` / `infrastructure` 分层、严格请求上下文校验与治理内部契约基线，并通过 `mvn clean compile`、`mvn test`、`mvn validate pmd:pmd checkstyle:check`、`node scripts/lint-repository-knowledge.js` 验证，但当前分层调整尚未完成单任务 git closeout，暂不归档。
  - 2026-04-20: removed the last `governance.common` remnants, added regression coverage for `HealthStatusApplicationService` and `MessageRetryResultVO`, and synced local delivery/setup notes with the protected governance admin paths and current Phase-C baseline summary.

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

### DOC-GOV-002: Close strict governance audit gaps

- Status: done
- Priority: 1
- Depends on: DOC-GOV-001
- Completed at: 2026-04-20
- Commit subject: `docs(governance): DOC-GOV-002 close strict audit gaps`
- Scope: close the remaining 7 governance audit gaps by adding explicit gap and prerequisite matrices, an interface contract baseline, a task governance extension matrix, a repair retrospective, updated rules and indices, and final task/git closeout consistency.
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `node scripts/check-frontend-backend-separation.js`
  - `mvn -B test`
  - `npm run build`
  - `npm run lint`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: re-audited the original governance plan against repository truth and isolated 7 still-open closure gaps around matrices, interface contracts, task-extension fields, and batch consistency.
  - 2026-04-20: added `document-gap-matrix.md`, `phase-prerequisite-matrix.md`, `service-interface-contract-baseline.md`, `task-governance-extension-matrix.md`, and `document-governance-repair-retrospective-2026-04-20.md`.
  - 2026-04-20: updated `docs/README.md`, `docs/plans/README.md`, `docs/plans/master-execution-plan.md`, `docs/plans/document-coverage-matrix.md`, `docs/plans/task-spec-matrix.md`, `docs/generated/repo-map.md`, `docs/rules/codex-rules.md`, `docs/references/human-constraint-history.md`, and `scripts/lint-repository-knowledge.js` to consume the new closure layer.
  - 2026-04-20: completed the governance batch audit chain by aligning `tasks-done.md`, `docs/quality/validation-log.md`, repair retrospective, and git closeout records.

### DOC-GOV-003: Close final readiness and closeout consistency gaps

- Status: done
- Priority: 1
- Depends on: DOC-GOV-002
- Completed at: 2026-04-20
- Commit subject: `docs(governance): DOC-GOV-003 close final readiness gaps`
- Scope: close the remaining micro consistency gaps by aligning implementation-readiness and docs read order with the prerequisite and task-governance matrices, then append the missing `DOC-GOV-002` task-audit closeout evidence and archive the repair batch.
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: re-checked the 16-item governance audit list against repository truth and confirmed that only three residual consistency gaps remained after `DOC-GOV-002`.
  - 2026-04-20: updated `docs/plans/implementation-readiness.md` and `docs/README.md` so non-trivial execution order explicitly consumes `phase-prerequisite-matrix.md` and `task-governance-extension-matrix.md`.
  - 2026-04-20: appended follow-up closure notes to `docs/plans/document-governance-repair-retrospective-2026-04-20.md`, backfilled the missing `DOC-GOV-002 closeout task-audit` validation record, and prepared the batch for git closeout.

### DOC-GOV-004: Close docs authority wording drift

- Status: done
- Priority: 1
- Depends on: DOC-GOV-003
- Completed at: 2026-04-20
- Commit subject: `docs(governance): DOC-GOV-004 close docs authority drift`
- Scope: remove the last README-level authority wording drift by keeping `init.md` as the historical baseline entry while pointing current service-boundary and interface-contract execution authority to the explicit governance baseline documents.
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: strict re-audit found one remaining main-entry wording drift in `docs/README.md`, where current authority was still described as coming directly from `init.md`.
  - 2026-04-20: updated `docs/README.md` to distinguish historical initialization baseline from current authority, and appended the closeout rationale to `docs/plans/document-governance-repair-retrospective-2026-04-20.md`.

### DOC-GOV-005: Align C4, process audit, and governance authority follow-ups

- Status: done
- Priority: 1
- Depends on: DOC-GOV-004
- Completed at: 2026-04-20
- Commit subject: `docs(governance): DOC-GOV-005 align c4 and process authority follow-ups`
- Scope: add the C4 authority document and process-flow governance audit, then align docs entry points, rule consumption notes, execution-plan wording, and history records so the strict-mode and temporary delivery-page follow-ups have explicit authority anchors.
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: post-closeout drift review found a remaining governance bundle spanning C4 authority location, process-flow audit indexing, strict-mode (`R-165`) consumption notes, and the temporary delivery-page (`R-166`) authority trail.
  - 2026-04-20: added `docs/architecture/c4-overview.md` and `docs/plans/process-flow-and-governance-audit-2026-04-20.md`, then aligned `docs/README.md`, `docs/architecture/init.md`, `docs/plans/README.md`, `docs/plans/document-coverage-matrix.md`, `docs/plans/master-execution-plan.md`, `docs/plans/task-spec-matrix.md`, `docs/plans/task-governance-extension-matrix.md`, `docs/operations/human-collaboration.md`, `docs/quality/validation-rules.md`, `docs/references/human-constraint-history.md`, `docs/rules/codex-rules.md`, and `scripts/lint-repository-knowledge.js`.
  - 2026-04-20: reran repository knowledge lint and task audit, then archived the governance follow-up batch for single-task git closeout.

### OPS-GOV-001: Add local and CI quality-gate entrypoints

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `ops(ci): OPS-GOV-001 add quality gate entrypoints`
- Scope: add executable SonarQube and coverage entrypoints for local/CI use, wire them into repository helper commands, and let CI consume them when configuration is present.
- Validation:
  - `bash scripts/run-sonar.sh`
  - `bash scripts/run-coverage.sh --phase report-only`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: cleanup classification isolated the remaining CI/quality files from Phase-C feature work because they add delivery gate entrypoints rather than shared-foundation or governance-service functionality.
  - 2026-04-20: added `scripts/run-sonar.sh`, wired coverage/Sonar helpers into `.github/workflows/ci.yml`, `Makefile`, and `docs/operations/local-development.md`, and kept the Sonar path no-op safe when environment variables are not configured.
  - 2026-04-20: reran the Sonar helper in no-config mode, reran coverage report generation, and prepared the quality-gate batch for single-task git closeout.
