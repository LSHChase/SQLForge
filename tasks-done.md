# SQLForge Completed Tasks

本文件只记录已完成、已验证、已归档的任务。

## Done

### E-TASK-008: 清理潜在越界逻辑

- Status: done
- Completed at: 2026-04-22
- Commit subject: `refactor(frontend): move protected headers to dev proxy`
- Priority: 1
- Depends on: `E-TASK-007`
- Scope: 清理前端中的权威业务判断 Tech: `VUE-FE`,`JAVA-BE`. Layer: `frontend/router/views/styles`,`application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-E / Story `E-STORY-003` 前后端分离持续治理
- Human confirmation point: 若将权威逻辑重新放回前端需人工确认
- Data impact: 前端状态和判断逻辑
- Rollback / recovery: 恢复后端权威边界并移除越界逻辑
- Validation:
  - 边界抽查、build、lint
  - `python3 scripts/foreman.py validate E-TASK-008`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Removed frontend-owned protected authentication header construction from src/services/runtimeGateApi.js, replaced it with dev-only proxy hint headers, taught vite.config.js to inject protected request context at the Vite proxy boundary for local integration, updated the separation baseline document, and advanced the master execution plan active wave to E-TASK-008 while preserving the Phase-E boundary-governance sequence.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-008 --include-task-audit --extra-command "node scripts/check-frontend-backend-separation.js" --extra-command "npm run lint" --extra-command "npm run build" --extra-command "rg -n \"X-SQLForge-Dev-|createProtectedApiProxy|X-Tenant-Id|X-User-Id|X-Role-Codes|X-Request-Id|X-Trace-Id|X-Auth-Source|X-Issued-At|X-Expires-At\" src/services/runtimeGateApi.js vite.config.js docs/quality/frontend-backend-separation-baseline.md docs/plans/master-execution-plan.md"
  - Residual risk: Protected request-context synthesis is no longer in frontend business code, but development still depends on Vite proxy injection for local auth simulation; the next reconciliation batch must align partially consumed E-TASK-004~006 and F-TASK-001~003 with repository truth without re-implementing already landed capabilities.
  - Next step: Instantiate the first reconciliation task for the partially consumed E-TASK-004~006 and F-TASK-001~003 batch, starting with the highest-value Phase-E archive/truth alignment item before proceeding through the remaining repository-truth closeout tasks.

### E-TASK-007: 扩展分离检查清单

- Status: done
- Completed at: 2026-04-22
- Commit subject: `chore(ci): strengthen frontend backend separation checks`
- Priority: 1
- Depends on: `Phase-C`
- Scope: 强化前后端分离校验 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Matrix context: Phase-E / Story `E-STORY-003` 前后端分离持续治理
- Human confirmation point: 分离检查口径放宽需人工确认
- Data impact: 脚本规则，无业务数据
- Rollback / recovery: 恢复严格检查项
- Validation:
  - 分离检查脚本通过
  - `python3 scripts/foreman.py validate E-TASK-007`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Expanded the frontend/backend separation gate to auto-discover every Maven backend module, added runtime and authority-boundary detections for protected frontend header construction and hard-coded backend hosts, tightened noisy false-positive heuristics, updated the separation baseline document, and switched the master execution plan active wave to the Phase-E boundary-governance mainline with E-TASK-007 as the current work item.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-007 --include-task-audit --extra-command "node scripts/check-frontend-backend-separation.js" --extra-command "rg -n \"discoverBackendRoots|X-Tenant-Id|sql-optimization|benchmark-engine|Phase-E / E-STORY-003|E-TASK-007\" scripts/check-frontend-backend-separation.js docs/quality/frontend-backend-separation-baseline.md docs/plans/master-execution-plan.md tasks.md"
  - Residual risk: The separation gate now surfaces the highest-value boundary drift, but src/services/runtimeGateApi.js still constructs protected request headers as a temporary frontend-owned smoke helper; that warning remains intentional until E-TASK-008 removes or rehomes the logic.
  - Next step: Instantiate E-TASK-008 next and remove the frontend-owned protected request header logic flagged by the strengthened separation gate, then continue to reconciliation tasks for partially consumed E-TASK-004~006 and F-TASK-001~003.

### A-TASK-012: 抽取 shared 认证与治理客户端支撑

- Status: done
- Completed at: 2026-04-22
- Commit subject: `refactor(shared): extract auth and governance client support`
- Priority: 1
- Depends on: A-TASK-011
- Scope: 把 query-execution、sql-optimization、benchmark-engine 重复的认证请求元数据与治理内部客户端支撑下沉到 sqlforge-shared，消除跨服务漂移并补 R-126 验证。
- Validation:
  - `python3 scripts/foreman.py validate A-TASK-012`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Extracted shared request-metadata context, header-auth establishment, and protected governance request helpers into sqlforge-shared; rewired query-execution, sql-optimization, and benchmark-engine interceptors plus governance clients to consume the shared support; removed duplicated per-service RequestMetadataContext implementations; updated governance regression tests; and aligned the master plan active wave plus task matrices for A-TASK-012.
  - Validation evidence: python3 scripts/foreman.py validate A-TASK-012 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "mvn -B -pl query-execution,sql-optimization,benchmark-engine -am test -DskipITs"
  - Residual risk: The shared extraction currently covers header-based auth context and governance internal client support only; wider shared cleanup across other duplicated service contracts remains pending, and the next mainline still needs frontend/backend separation enforcement plus repository-truth reconciliation for partially consumed E/F tasks.
  - Next step: Resume the recommended mainline by instantiating E-TASK-007 to strengthen frontend/backend separation checks, then E-TASK-008 and the remaining reconciliation tasks for partially consumed E-TASK-004~006 and F-TASK-001~003.

### A-TASK-011: 主计划剩余任务对齐并修复跨服务鉴权审计缺口

- Status: done
- Completed at: 2026-04-22
- Commit subject: `fix(governance): align remaining plan and close auth audit gaps`
- Priority: 1
- Depends on: A-TASK-010
- Scope: 对齐 active wave、剩余任务与事实完成度；同时修复 query-execution/sql-optimization/benchmark-engine 当前已确认的鉴权、租户归一化与审计元数据高优先缺口，并同步验证与文档真值。
- Validation:
  - `python3 scripts/foreman.py validate A-TASK-011`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Aligned the master plan current wave and A-task matrix with repository truth, added A-TASK-011 governance authority entries, enabled prod auth in sql-optimization and benchmark-engine, normalized query-execution tenantId to authenticated context, and propagated real request metadata into optimization/benchmark governance audit writes with regression coverage.
  - Validation evidence: python3 scripts/foreman.py validate A-TASK-011 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "mvn -B -pl query-execution,sql-optimization,benchmark-engine -am test -DskipITs" --extra-command "grep -n \"enabled: true\" sql-optimization/src/main/resources/application-prod.yml benchmark-engine/src/main/resources/application-prod.yml"
  - Residual risk: Shared auth/governance client code is still duplicated across three services, Phase-D still lacks a dedicated phase-exit R-117/R-118 proof batch, and optimization/benchmark persisted carriers still run placeholder executors rather than production-grade engines.
  - Next step: Instantiate the next highest-value task to extract shared auth/governance client support into sqlforge-shared, then resume the Phase-E boundary-governance mainline with E-TASK-007 and E-TASK-008 before handling Phase-F residual hardening.

### E-TASK-003: 落地合规中心与规则库板块

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(frontend): add dashboard compliance and rulebook boards`
- Priority: 1
- Depends on: `E-TASK-002`
- Scope: 只读展示规则与合规信息 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-001` 研发驾驶舱
- Human confirmation point: 若移除规则/合规展示需人工确认
- Data impact: 前端展示数据，无后端持久化
- Rollback / recovery: 恢复只读展示页面
- Validation:
  - 可见性、只读性、构建
  - `python3 scripts/foreman.py validate E-TASK-003`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Expanded the /dashboard cockpit with explicit compliance-center and Codex-rulebook sections; added bilingual summaries for R-111 through R-115, surfaced append-only rulebook clusters, and derived rule-count summary cards from the repository truth in docs/security/compliance.md and docs/rules/codex-rules.md.
  - Validation evidence: Validated with python3 scripts/foreman.py validate E-TASK-003 --include-task-audit --extra-command "npm run build" --extra-command "rg -n \"dashboard\\.compliance|dashboard\\.rulebook|codexRulesMarkdown|complianceMarkdown|cardsSummary\" src/views/dashboard/DashboardView.vue src/locales/zh-CN.js src/locales/en-US.js"; npm run build passed and the rulebook summary now reads directly from the repository rule and compliance markdown sources.
  - Residual risk: The compliance and rulebook boards are still summary-only and depend on later work to connect more live operational evidence, such as audit-log query surfaces, backup-drill records, and any future machine-generated rule-growth telemetry.
  - Next step: Move to E-TASK-004 and keep the remaining Phase-E mainline on the business-route shells so the cockpit summary can continue routing into complete, independent workflow pages.

### E-TASK-002: 落地项目全景/架构设计/进度管理板块

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(frontend): add dashboard architecture and progress boards`
- Priority: 1
- Depends on: `E-TASK-001`
- Scope: 驾驶舱展示项目与计划信息 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-001` 研发驾驶舱
- Human confirmation point: 若隐藏已承诺信息板块需人工确认
- Data impact: 前端展示数据，无后端持久化
- Rollback / recovery: 恢复板块与原信息架构
- Validation:
  - `页面结构与 IA 对照`
  - `python3 scripts/foreman.py validate E-TASK-002`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Expanded the /dashboard cockpit with explicit project-panorama, architecture-design, and progress-management sections; added bilingual IA copy for vision, roadmap, glossary, rule index, architecture summaries, and wired the progress board to the authoritative delivery snapshot derived from tasks.md, tasks-done.md, validation-log, and the master execution plan.
  - Validation evidence: Validated with python3 scripts/foreman.py validate E-TASK-002 --include-task-audit --extra-command "npm run build" --extra-command "rg -n \"dashboard\\.panorama|dashboard\\.architecture|dashboard\\.progress|createDeliveryProgressSnapshot|deliveryProgressAvailability\" src/views/dashboard/DashboardView.vue src/locales/zh-CN.js src/locales/en-US.js"; npm run build passed and the dashboard progress block is now backed by the same repository-truth snapshot used by the delivery-progress page.
  - Residual risk: The new dashboard sections are intentionally summary-only and still depend on later Phase-E tasks to land the remaining compliance/rule-library boards and to keep business metrics aligned with future backend-delivered live data rather than curated static copy.
  - Next step: Instantiate E-TASK-003 and land the compliance-center plus rule-library sections so the cockpit completes the remaining 10.1 information architecture promised for Phase-E.

### E-TASK-001: 建立驾驶舱路由与导航骨架

- Status: done
- Completed at: 2026-04-22
- Commit subject: `docs(frontend): archive dashboard route shell baseline`
- Priority: 1
- Depends on: `Phase-C`
- Scope: 建立 dashboard 主路径与导航壳层 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-001` 研发驾驶舱
- Human confirmation point: 主路由改名或删减需人工确认
- Data impact: 前端路由结构
- Rollback / recovery: 恢复原导航与入口映射
- Validation:
  - build、路由可达、i18n
  - `python3 scripts/foreman.py validate E-TASK-001`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Verified that the dashboard route shell baseline was already present in the frontend carrier, including the /dashboard primary route, grouped navigation shell, route metadata wiring, and zh-CN/en-US i18n labels across the app shell.
  - Validation evidence: Validated with python3 scripts/foreman.py validate E-TASK-001 --include-task-audit --extra-command "npm run build" --extra-command "rg -n \"path: ROUTE_PATHS.dashboard|redirect: ROUTE_PATHS.dashboard|common.navGroups|dashboard.title|dashboard.summary\" src/router/index.js src/App.vue src/locales/zh-CN.js src/locales/en-US.js", covering build, route registration, navigation grouping, and i18n keys.
  - Residual risk: The dashboard shell baseline is in place, but the content sections still need later Phase-E tasks to align with the full information architecture and keep summary data consistent with backend-delivered capabilities.
  - Next step: Instantiate E-TASK-002 and fill the dashboard with project-overview, architecture-design, and progress-management sections on top of the validated route shell.

### D-TASK-015: 补完 `query-execution` 真实执行适配与结果聚合基线

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(query-execution): add hetu mode-chain execution baseline`
- Priority: 1
- Depends on: `D-TASK-014`
- Scope: 落实 Hetu 多模式执行适配、模式选择、结果聚合与审计证据 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若真实执行适配放宽只读边界或引入破坏式执行契约需人工确认
- Data impact: 查询执行适配配置、执行结果聚合、审计记录
- Rollback / recovery: 切回最小同步基线并保留兼容执行模式/错误映射
- Validation:
  - `模块测试、跨模式适配测试、runtime smoke`
  - `python3 scripts/foreman.py validate D-TASK-015`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-22: completed the feature-flagged Hetu mode chain baseline in `query-execution`, including `JDBC/REST/CLIENT` mode adapters, routing, metadata/result aggregation, controller/service test updates, and contract/truth/capability/init doc sync.
  - 2026-04-22: `mvn -B -pl query-execution -am test -DskipITs` passed with new routing and adapter coverage.
  - 2026-04-22: the earlier Java 8 runtime misunderstanding was traced to a hand-run command that incorrectly hardcoded `spring-boot-maven-plugin:3.2.5:run`; repository POMs and runtime scripts remained aligned to Java 8 + Spring Boot `2.7.18`.
  - 2026-04-22: after rerunning the official foreman validation entrypoint with the standard runtime smoke, `python3 scripts/foreman.py validate D-TASK-015 --include-task-audit --extra-command 'python3 scripts/foreman.py compile-governance --check' --extra-command 'mvn -B -pl query-execution -am test -DskipITs' --extra-command 'bash scripts/run-runtime-smoke.sh --runtime-smoke'` passed.
- Context closeout:
  - Completed scope: Implemented the feature-flagged Hetu mode-chain baseline in query-execution, including JDBC/REST/CLIENT adapters, mode routing, execution metadata/result aggregation, and Spring Boot 2.7 / Java 8 compatible test coverage and contract-doc updates.
  - Validation evidence: Validated with python3 scripts/foreman.py validate D-TASK-015 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "mvn -B -pl query-execution -am test -DskipITs" --extra-command "bash scripts/run-runtime-smoke.sh --runtime-smoke", which passed on the repository-standard Java 8 + Spring Boot 2.7.18 toolchain.
  - Residual risk: Real Hetu execution remains feature-flagged off by default and still depends on external JDBC/REST/CLIENT connectivity, runtime credentials, and production parameter calibration before enabling outside controlled environments.
  - Next step: Proceed to the next Phase-D mainline task after confirming whether a follow-up task should add production-grade Hetu cluster evidence and tighter execution telemetry.

### A-TASK-010: 主计划与运行台账对齐

- Status: done
- Completed at: 2026-04-22
- Commit subject: `docs(governance): align master plan with task ledgers`
- Priority: 1
- Depends on: N/A
- Scope: 对齐 docs/plans/master-execution-plan.md、task-spec/task-governance 矩阵、tasks-done.md、INBOX.md 与 validation-log 的真实完成度，补回已执行但未入主计划的任务，并明确当前活跃波次与下一条可执行主线。
- Validation:
  - `python3 scripts/foreman.py validate A-TASK-010`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-22: reconciled `master-execution-plan.md`, `task-spec-matrix.md`, `task-governance-extension-matrix.md`, and plan index docs so `D-TASK-014`, `F-TASK-010`~`F-TASK-028`, new `A-TASK-010`, and new `D-TASK-015` are all visible to the same governance chain; resolved the stale active-wave pointer to `Phase-D / D-TASK-015`, cleared `INBOX-001`, and recompiled `.codex/policy/authority-map.json` after the authority-map drift check failed as expected on the first validation pass.
- Context closeout:
  - Completed scope: Reconciled the master execution plan, task spec matrix, governance extension matrix, and plan index documents with the actual execution record; backfilled D-TASK-014 and F-TASK-010 through F-TASK-028 into the planning corpus; introduced A-TASK-010 and D-TASK-015 as governed tasks; restored the current active wave to Phase-D with D-TASK-015 as the next executable mainline; and synced the derived authority map after governance compilation drift surfaced.
  - Validation evidence: Validated with python3 scripts/foreman.py validate A-TASK-010 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check", including repository knowledge lint, pre-closeout task audit, and a clean compile-governance drift check after recompiling .codex/policy/authority-map.json.
  - Residual risk: The governance source of truth is now aligned, but the newly restored mainline task D-TASK-015 is still not implemented; query-execution remains on the minimal synchronous baseline until that task closes.
  - Next step: Instantiate D-TASK-015 and implement the query-execution real execution adapter and result aggregation baseline on top of the repaired planning/ledger chain.

### F-TASK-027: 收口 Phase-F 退出门禁缺口

- Status: done
- Completed at: 2026-04-22
- Commit subject: `ci(governance): F-TASK-027 close phase-f exit gates`
- Priority: 1
- Depends on: F-TASK-026
- Scope: Close the remaining Phase-F exit-gate blockers by wiring dedicated database-script executability checks, coverage-threshold enforcement, Sonar-required delivery mode, and stronger R-118 compliance evidence into the phase-gate workflow and closeout path.
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-027`
- Context closeout:
  - Completed scope: Added a dedicated database-script executability gate into both local phase-gate execution and default CI, tightened the Phase Gate workflow so delivery/full runs require Sonar and compliance/full can invoke the real Kafka runtime gate, and strengthened the compliance baseline checks plus deployment baselines to treat observability, recovery, Kafka runtime, and DB script evidence as first-class Phase-F exit criteria.
  - Validation evidence: Validated with python3 scripts/foreman.py validate F-TASK-027 --include-task-audit --extra-command 'bash scripts/verify-db-scripts.sh' --extra-command 'python3 scripts/verify_compliance_baseline.py'; additionally confirmed blocking semantics with bash scripts/run-phase-gates.sh --gate delivery --coverage-phase phase0 --require-sonar failing at the enforced coverage threshold (73.0005% < 80%) and bash scripts/run-sonar.sh --require-config failing fast when SONAR_HOST_URL/SONAR_TOKEN are absent.
  - Residual risk: Phase-F exit gates are now real blockers, but the repository still needs higher aggregate coverage, provisioned Sonar secrets, and automation beyond workflow_dispatch before the full delivery/compliance path can act as a zero-touch release gate.
  - Next step: Phase-F governance implementation is closed through F-TASK-027; the remaining follow-up is operational hardening: raise coverage to threshold, provision Sonar in CI, and bind phase-gate execution to real release metadata instead of manual dispatch only.

### F-TASK-026: 接入真实 Kafka 运行验证与环境安全参数门禁

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(governance): F-TASK-026 gate real kafka runtime`
- Priority: 1
- Depends on: N/A
- Scope: Add Phase-F runtime verification for real Kafka mode, including bootstrap/security parameter validation, connectivity checks, failure-recovery smoke, and documented runtime-gate evidence so messaging is not only proven in DATABASE mode.
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-026`
- Context closeout:
  - Completed scope: Added explicit Kafka bootstrap and security configuration validation for governance, centralized Kafka client property assembly for producer and consumer paths, updated dev/prod messaging config and local compose wiring for a runnable KRaft-backed broker, and delivered both a dedicated Kafka runtime gate workflow and local scripts that prove success delivery plus broker-stop fallback recovery in real KAFKA mode.
  - Validation evidence: Validated with python3 scripts/foreman.py validate F-TASK-026 --include-task-audit --extra-command 'mvn -B -pl governance -Dtest=MessagingConfigTest,KafkaMessageProducerTest,KafkaMessageConsumerTest test' --extra-command 'bash scripts/run-phase-gates.sh --gate compliance --run-real-kafka-gate', including a full governance startup in KAFKA mode, bootstrap/security parameter checks, topic connectivity, and fallback recovery after broker interruption.
  - Residual risk: The real Kafka gate now exists and passes locally, but it still depends on Docker-capable runners, an available broker port, and environment-specific secrets/certs for secure modes beyond the plaintext smoke configuration used in the local baseline.
  - Next step: Close F-TASK-027 to finish the remaining Phase-F exit-gate closure around DB script executability, stronger compliance evidence, and delivery gate blocking semantics.

### F-TASK-025: 补齐治理归档历史窗口与深分页链路

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(governance): F-TASK-025 add long-window history lookup`
- Priority: 1
- Depends on: F-TASK-024
- Scope: Extend governance historical diagnostics beyond current indexed table lookups by adding archival-window query support, stronger deep-pagination strategy, and stable drill-through for older trace/task/report evidence across audit/query/export history so large-tenant and older-data forensics do not remain bounded by the current hot-window indexes.
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-025`
- Context closeout:
  - Completed scope: Added indexed long-window governance history lookup support with a dedicated lookup-index mapper and migration, extended the governance history API/service to accept windowStart/windowEnd filters, carried those parameters through parse-record, audit-forensics, repair-evidence, and audit-troubleshooting drill-through flows, and hardened the browser runtime smoke to prove long-window pagination and cross-page query preservation on the canonical governance history routes.
  - Validation evidence: Validated with python3 scripts/foreman.py validate F-TASK-025 --include-task-audit --extra-command 'mvn -B -pl governance -Dtest=GovernanceHistoryApplicationServiceTest,AuthWebMvcTest test' --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'bash scripts/run-runtime-smoke.sh --runtime-smoke', including a full startup + business + browser smoke pass.
  - Residual risk: The long-window lookup now avoids recent-scan behavior, but it still relies on current governance audit/query/export indexes rather than a separate archival/materialized history store, so very large tenants and colder data windows may still require a deeper history model.
  - Next step: Close F-TASK-026 to harden real Kafka runtime verification and security parameter gating on top of the updated governance runtime baseline.

### F-TASK-028: 拆分主线业务与治理运维页面路径

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(frontend): F-TASK-028 split governance route namespaces`
- Priority: 1
- Depends on: F-TASK-024
- Scope: Split the main business routes and the F-series governance/operations routes so `/dashboard`, `/sql-query`, `/acceleration`, `/benchmark`, and `/system` remain the product path while long-chain history/forensics/remediation/runtime-gates/recovery-drill/delivery-progress views move under dedicated `/governance/history/*` and `/governance/ops/*` namespaces with secondary navigation, keeping drill-through intact without pushing deep troubleshooting flows back into the main business pages.
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-028`
- Context closeout:
  - Completed scope: Split the frontend navigation baseline into main workflow routes and dedicated governance history/ops namespaces, added canonical route constants plus legacy redirects, updated the app shell and dashboard navigation to surface governance history and ops as secondary paths, and delivered dedicated runtime-gates and recovery-drill pages under the new governance route tree without pushing long troubleshooting flows back into the main business pages.
  - Validation evidence: Validated with python3 scripts/foreman.py validate F-TASK-028 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build', plus the current browser/runtime smoke path now resolving the canonical governance namespaces used by the route tree.
  - Residual risk: Deep governance history pages still rely on follow-up tasks to carry every drill-through link and runtime assertion onto the canonical route tree; this task intentionally focused on the route namespace split, shell navigation, and governance ops landing pages.
  - Next step: Close F-TASK-025 to finish long-window governance history lookup and cross-page drill-through on top of the new governance namespaces.

### F-TASK-024: 新增审计故障处置与修复决策页

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(frontend): add audit remediation decision runtime gate`
- Priority: 1
- Depends on: N/A
- Scope: Add an audit troubleshooting/remediation decision page that correlates trace/task/report forensic evidence with failure type, compensation status, write-back status, and queue impact; expose real remediation actions including governance failed-message retry and drill-through into backlog/history/repair evidence; extend the browser runtime gate to validate the evidence -> decision -> remediation -> acceptance chain.
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-024`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added a dedicated /audit-troubleshooting remediation decision page that correlates trace/task/report forensic evidence with queue impact and acceptance signals, wired audit-forensics and repair-evidence drill-through into the new page, extended repair-evidence with a return path to remediation, expanded the browser runtime gate to verify evidence -> remediation -> acceptance including a real governance failed-message retry and post-repair state change, and queued follow-up F-series tasks for route splitting, archival history, real Kafka verification, and remaining Phase-F gate closure.
  - Validation evidence: Validated with npm run lint, npm run build, bash scripts/health-check.sh --fail-on-error, npm run smoke:frontend-runtime, python3 scripts/foreman.py validate F-TASK-024, and python3 scripts/task_audit.py --check --phase pre-closeout against the running multi-service stack.
  - Residual risk: The remediation decision page still uses the current governance indexed history and queue admin APIs rather than a dedicated archival/materialized history model or separated governance route tree, so very old data, large tenants, and main-vs-ops navigation separation remain follow-up work rather than part of this task.
  - Next step: Start F-TASK-028 to split main business routes from governance/ops routes, then continue with F-TASK-025 archival history/deep pagination, F-TASK-026 real Kafka runtime verification, and F-TASK-027 Phase-F exit-gate closure.

### F-TASK-023: 扩展历史诊断与审计取证分页链路

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(frontend): extend historical forensic runtime chain`
- Priority: 1
- Depends on: N/A
- Scope: Extend /parse-record with indexed long-window trace/task/report lookup, pagination, and drill-through to trace detail; add a dedicated audit-forensics page that stitches compensation and repair evidence across trace/task/report lookups; expand browser runtime gate to cover the new historical forensic chain.
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-023`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Extended /parse-record with indexed long-window trace/task/report lookup, pagination, and drill-through actions; added a dedicated /audit-forensics page to stitch compensation, repair, and write-back evidence across paged governance lookups; updated repair-evidence to accept drill-through query context; and expanded the default browser runtime smoke to cover parse-record -> audit-forensics -> repair-evidence.
  - Validation evidence: Validated with npm run lint, npm run build, bash scripts/health-check.sh --fail-on-error, python3 scripts/foreman.py validate F-TASK-023, and npm run smoke:frontend-runtime against the running multi-service stack.
  - Residual risk: The new forensic pages still depend on current governance audit/query/export history density and indexed lookup over existing tables rather than a dedicated archival history model, so very large or older tenant datasets may still require deeper materialization and pagination tuning.
  - Next step: Continue expanding governance historical diagnostics by adding the next audit-troubleshooting page that pivots from trace/task/report evidence into remediation decisions and older archival windows.

### F-TASK-022: 升级治理长期历史反查与分页追溯

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(governance): page indexed history lookups`
- Priority: 1
- Depends on: N/A
- Scope: replace recent-scan governance trace/task/report lookup with indexed history queries that support pagination over older audit, query-history, export, and task/report-linked evidence, then extend the frontend forensic pages to consume the paged API
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-022`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Replaced recent-scan governance trace/task/report reverse lookup with indexed paged history queries, added pagination cursor/page response support in governance, updated the /repair-evidence frontend to consume paged results with load-more, and extended browser runtime smoke to verify older task history pagination.
  - Validation evidence: Validated with mvn -pl governance -Dtest=GovernanceHistoryApplicationServiceTest,AuthWebMvcTest,TraceabilitySchemaMappingTest test, npm run lint, npm run build, bash scripts/health-check.sh --fail-on-error, python3 scripts/foreman.py validate F-TASK-022, and npm run smoke:frontend-runtime after restarting governance with the dev crypto key.
  - Residual risk: Long-window lookup now uses existing audit/query/export indexes rather than a dedicated archival history table, so very large tenants may still need follow-up work on deeper historical materialization and broader forensic page adoption.
  - Next step: Continue the browser runtime gate expansion into the next governance history or audit-forensics page, prioritizing views that can reverse-search compensation and repair evidence by trace/task/report on top of the new paged indexed lookup.

### F-TASK-021: 扩展治理历史修复追溯页 browser runtime gate

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat: add governance repair evidence browser gate`
- Priority: 1
- Depends on: N/A
- Scope: add a governance history repair-evidence page keyed by trace/task/report lookups and extend browser runtime smoke to assert compensation and repair evidence rendering
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-021`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governance history reverse-lookup APIs for trace/task/report evidence, delivered the /repair-evidence browser page, and extended the default frontend runtime smoke to assert compensation and repair evidence rendering across query, optimization, and benchmark traces.
  - Validation evidence: Validated with mvn -pl governance -Dtest=GovernanceHistoryApplicationServiceTest,AuthWebMvcTest test, npm run lint, npm run build, bash scripts/health-check.sh --fail-on-error, python3 scripts/foreman.py validate F-TASK-021, and npm run smoke:frontend-runtime after restarting governance and frontend against the updated code.
  - Residual risk: The new lookup endpoint still scans recent governance evidence rather than a dedicated indexed history table, so very old trace/task/report chains can age out of the reverse-lookup window until long-range archival queries are added.
  - Next step: Continue the browser runtime gate expansion to the next governance history or audit-forensics page, prioritizing views that expose older historical diagnosis and cross-trace repair evidence beyond the current recent-window lookup.

### F-TASK-020: 扩展治理历史页 browser runtime gate

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(governance): extend parse record history runtime gate`
- Priority: 1
- Depends on: N/A
- Scope: add governance history read APIs, replace /parse-record placeholder with a real traceability page, and extend browser runtime smoke to assert history/audit evidence rendering
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-020`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governance history summary/detail APIs, replaced /parse-record placeholder with a live historical diagnosis page, and extended the default browser runtime gate to assert audit traceability rendering.
  - Validation evidence: foreman validate passed with npm run lint, npm run build, governance targeted tests, bash scripts/health-check.sh --fail-on-error, and npm run smoke:frontend-runtime after restarting governance with the dev crypto key.
  - Residual risk: query_history/export_record remain empty in current dev smoke, so parse-record evidence is still audit-driven until later history/export writers are expanded.
  - Next step: Continue expanding browser runtime gate into additional governance history and audit troubleshooting views.

### F-TASK-019: 加固前端补偿信号稳定性

- Status: done
- Completed at: 2026-04-22
- Commit subject: `fix(frontend): stabilize compensation runtime evidence`
- Priority: 1
- Depends on: N/A
- Scope: 把前端 query/optimization/benchmark 页面中的补偿判定从 pending-only 调整为 pending 或 total 双信号，补齐 total delta 可视化，并收口 F-TASK-018 closeout 后遗留的验证日志与工作区变更。
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-019`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-22: captured the post-closeout residual view changes from F-TASK-018 into a dedicated follow-up task so the compensation-signal hardening and validation-log tail can re-enter the normal audit chain without amending history.
- Context closeout:
  - Completed scope: Captured the missed post-closeout frontend view changes from F-TASK-018, changed query/optimization/benchmark compensation detection to use pending-or-total queue growth, exposed total delta evidence in the UI, and recorded the remaining validation-log entries in the audit chain.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-019 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'npm run smoke:frontend-runtime'
  - Residual risk: The browser runtime gate is now stable against fast queue consumption for the current four pages, but frontend coverage still does not include parse-record or deeper audit/history visualization.
  - Next step: Continue extending the browser runtime gate to the next real business page, prioritizing parse-record or other governance history views now that compensation evidence has been stabilized.

### F-TASK-018: 扩展 system 治理管理页 browser runtime gate

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(frontend): add governance system runtime gate`
- Priority: 1
- Depends on: N/A
- Scope: 把前端 /system 从占位页替换为真实 governance 管理页，接入 tenant-config、message stats、retry failed messages，并把浏览器 runtime smoke 扩展到治理 backlog 与补偿修复动作，继续扩大默认 CI 的前端业务级门禁覆盖。
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-018`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-22: replaced `/system` placeholder with a live governance admin page that loads tenant-config, message stats, failed-message retry evidence, and extended browser smoke to seed a FAILED queue row and verify retry remediation from the UI.
- Context closeout:
  - Completed scope: Replaced the /system placeholder with a live governance admin runtime page, added tenant-config/message-stats/retry APIs, seeded FAILED queue remediation evidence in browser smoke, and expanded the default frontend runtime gate from core execution pages to governance repair actions.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-018 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'npm run smoke:frontend-runtime'
  - Residual risk: Default browser runtime gate now covers the core execution pages plus governance remediation, but parse-record and other business views still remain outside the default browser smoke and there is still no frontend visualization for deeper trace/audit history records.
  - Next step: Extend the browser runtime gate to the next real business page, prioritizing parse-record or other governance history views so frontend runtime coverage continues to grow beyond the current four pages.

### F-TASK-017: 扩展前端失败恢复与审计补偿 runtime gate

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(frontend): extend recovery runtime gate`
- Priority: 1
- Depends on: N/A
- Scope: 把前端浏览器 smoke 从成功链路扩展到失败恢复与审计补偿可视化，复用已完成的 sql-optimization / benchmark-engine 持久化后端能力，并接入默认 runtime gate / CI。
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-017`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-22: extended frontend runtime gate pages and Playwright smoke to cover query-execution degraded recovery, sql-optimization failed-task compensation, benchmark-engine failed-task compensation, and governance queue pending-delta evidence.
- Context closeout:
  - Completed scope: Extended the frontend runtime gate pages and Playwright smoke from success-only checks to query degraded recovery, sql-optimization failed-task compensation, benchmark-engine failed-task compensation, governance queue pending-delta visualization, and 64-character-safe correlation headers.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-017 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'npm run smoke:frontend-runtime'
  - Residual risk: Default browser runtime gate now covers the three key frontend flows end-to-end, but more business pages and richer audit-compensation remediation views are still outside the default smoke suite.
  - Next step: Extend the browser runtime gate to additional business pages and richer remediation/audit views now that the core three frontend chains are blocked by default CI.

### F-TASK-016: 推进 benchmark-engine 真实持久化与调度链路

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(benchmark-engine): persist benchmark task pipeline`
- Priority: 1
- Depends on: N/A
- Scope: 把 benchmark-engine 从 in-memory 占位执行器推进到 MySQL 持久化任务表、报告回写与 worker/scheduler 链路，并复用治理 smoke 门禁验证成功链路、失败恢复与审计补偿。
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-016`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented MySQL-backed benchmark task/report persistence, report write-back, scheduled worker execution, schema/migration updates, and governance smoke assertions for success, failure recovery, and audit compensation.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-016 --include-task-audit --extra-command "mvn -B -pl benchmark-engine -am test -DskipITs" --extra-command "bash scripts/run-runtime-smoke.sh --compose-check" --extra-command "bash scripts/manual-benchmark-governance-smoke.sh --cleanup"
  - Residual risk: Default full runtime smoke is still blocked in this workstation by an unrelated process already occupying port 3000, so frontend-included CI parity still depends on a clean runner; benchmark-engine also still uses placeholder execution logic behind the persisted carrier rather than a real isolated benchmark executor/export pipeline.
  - Next step: Extend the frontend browser runtime gate from success-only flows to failure recovery and audit-compensation visualization now that sql-optimization and benchmark-engine both have persisted backend carriers.

### F-TASK-015: 推进 sql-optimization 真实持久化与调度链路

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(sql-optimization): persist optimization tasks with scheduled worker`
- Priority: 1
- Depends on: N/A
- Scope: 把 sql-optimization 从 in-memory 占位执行器推进到 MySQL 持久化任务表、真实状态流转与 worker/scheduler 链路，并同步更新验证脚本与文档证据链。
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-015`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Replace sql-optimization placeholder carrier with MySQL-backed optimization_task persistence, scheduled worker execution, runtime schema bootstrap, and governance smoke coverage.
  - Validation evidence: mvn -B -pl sql-optimization -am test; bash scripts/manual-sql-optimization-governance-smoke.sh --cleanup; python3 scripts/foreman.py validate F-TASK-015
  - Residual risk: Full default runtime smoke remains blocked locally by an existing port 3000 frontend process; backend sql-optimization runtime path is validated directly.
  - Next step: Start the follow-up benchmark-engine persistence/scheduler task, then extend frontend smoke to failure recovery and audit compensation visualization.

### F-TASK-014: 扩展前端真实业务 runtime smoke 门禁

- Status: done
- Completed at: 2026-04-21
- Commit subject: `feat(frontend): F-TASK-014 gate browser runtime smoke`
- Priority: 1
- Depends on: N/A
- Scope: 把前端可见的真实业务路径接入默认 runtime smoke / CI，验证前端发起请求后 query-execution、sql-optimization、benchmark-engine 与 governance 的关键链路能从 UI 侧跑通，并补脚本与文档证据链。
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-014`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 为前端新增 sql-query、acceleration、benchmark 三个真实业务页与受保护 API client，按服务扩展 Vite proxy，新增浏览器驱动 smoke 并接入默认 runtime smoke / CI。
  - Validation evidence: npm run lint；npm run build；bash scripts/run-runtime-smoke.sh --compose-check；bash scripts/run-runtime-smoke.sh --runtime-smoke --reuse-running-stack --keep-stack；python3 scripts/foreman.py validate F-TASK-014 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'bash scripts/run-runtime-smoke.sh --compose-check' --extra-command 'bash scripts/run-runtime-smoke.sh --runtime-smoke --reuse-running-stack --keep-stack'；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 前端 runtime gate 当前覆盖 3 条成功业务链路，但失败恢复 UI、审计补偿可视化和更多业务页尚未并入浏览器 smoke；sql-optimization 和 benchmark-engine 仍是占位执行器链路。
  - Next step: 继续推进 sql-optimization / benchmark-engine 的真实持久化、调度与回调链路，并把对应失败恢复与前端可视化一并接入 runtime gate。

### F-TASK-013: 扩展优化与压测业务级 runtime smoke 门禁

- Status: done
- Completed at: 2026-04-21
- Commit subject: `feat(runtime): F-TASK-013 gate optimization-benchmark business smoke`
- Priority: 1
- Depends on: N/A
- Scope: 把 sql-optimization 和 benchmark-engine 的真实业务联调、失败恢复路径与治理审计补偿验证接入默认 runtime smoke / CI，沿用 F-TASK-012 的模式扩展关键链路门禁，并同步脚本与文档证据链。
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-013`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-21: wired `sql-optimization` and `benchmark-engine` to governance tenant/datasource checks plus audit write contracts, and added business smoke scripts for success, failure, and audit compensation paths.
  - 2026-04-21: verified module tests and default runtime smoke for query-execution/sql-optimization/benchmark-engine business gates before closeout.
- Context closeout:
  - Completed scope: 为 sql-optimization 和 benchmark-engine 增加治理内部 HTTP client、租户/数据源校验与 audit/write 上报；新增两条业务级 runtime smoke，验证提交/轮询/报告读取、失败恢复路径与审计补偿队列；同步扩展默认 runtime smoke、CI/本地文档真值与治理数据源映射。
  - Validation evidence: mvn -B -pl sql-optimization,benchmark-engine -am test；bash scripts/run-runtime-smoke.sh --compose-check；bash scripts/run-runtime-smoke.sh --runtime-smoke --keep-stack；python3 scripts/foreman.py validate F-TASK-013 --include-task-audit --extra-command 'mvn -B -pl sql-optimization,benchmark-engine -am test' --extra-command 'bash scripts/run-runtime-smoke.sh --compose-check' --extra-command 'bash scripts/run-runtime-smoke.sh --runtime-smoke --reuse-running-stack --keep-stack'；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前业务级 runtime gate 已覆盖 query-execution、sql-optimization、benchmark-engine 到 governance 的关键链路，但前端仍缺少更深层业务 smoke；benchmark/sql-optimization 仍是占位执行器链路，生产级真实持久化、回调、消息消费和 Kafka 安全参数验证尚未闭环。
  - Next step: 继续把前端真实业务链路与更深层恢复/积压演练纳入默认 runtime gate，并在后续任务中把 sql-optimization / benchmark-engine 从占位执行器推进到真实持久化和调度链路。

### F-TASK-012: 扩展跨服务业务级 runtime smoke 门禁

- Status: done
- Completed at: 2026-04-21
- Commit subject: `feat(runtime): F-TASK-012 gate query-governance business smoke`
- Priority: 1
- Depends on: N/A
- Scope: 把 query-execution -> governance 的真实业务联调、失败恢复路径与审计补偿验证接入默认 runtime smoke / CI，形成比启动探针更深一层的关键链路门禁，并同步脚本与文档证据链。
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-012`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-21: wired query-execution protected context, governance internal client, runtime business smoke, degraded fallback verification, and audit compensation queue checks into the default runtime gate.
- Context closeout:
  - Completed scope: 为 query-execution 增加受保护请求上下文、治理内部 HTTP client 与审计写入，使执行链路在真实运行时会调用 governance 的租户/数据源校验与 audit/write；为 governance 增加按 trace 前缀触发的定向审计路由失败注入；新增 scripts/manual-query-governance-smoke.sh 并把它接入默认 runtime smoke/CI，验证 query-execution -> governance 的成功链路、超时降级恢复与审计补偿队列兜底。
  - Validation evidence: mvn -B -pl query-execution -am test；mvn -B -pl governance -am test；bash scripts/run-runtime-smoke.sh --compose-check；bash scripts/run-runtime-smoke.sh --runtime-smoke --reuse-running-stack --keep-stack；python3 scripts/foreman.py validate F-TASK-012 --include-task-audit --extra-command 'mvn -B -pl query-execution -am test' --extra-command 'mvn -B -pl governance -am test' --extra-command 'bash scripts/run-runtime-smoke.sh --compose-check' --extra-command 'bash scripts/run-runtime-smoke.sh --runtime-smoke --reuse-running-stack --keep-stack'；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前默认业务级 runtime gate 只覆盖 query-execution -> governance；sql-optimization、benchmark-engine 与前端仍停留在启动级 smoke，治理侧的主消息路由失败注入也仅用于本地/CI 的 trace 前缀定向演练，不代表生产消息故障演练已闭环。
  - Next step: 继续把 sql-optimization / benchmark-engine 的真实业务链路、跨服务审计上报和失败恢复路径纳入默认 runtime gate，并补治理消息消费/积压恢复的更深层验证。

### F-TASK-011: 扩展多服务运行时 smoke 门禁

- Status: done
- Completed at: 2026-04-21
- Commit subject: `ci(runtime): F-TASK-011 gate multi-service smoke`
- Priority: 1
- Depends on: F-TASK-010
- Scope: 把 query-execution、sql-optimization、benchmark-engine 和前端的真实启动探针接入默认 CI，扩展 runtime smoke 为多服务门禁，并同步脚本与文档证据链。
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-011`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-21: extended runtime smoke plan to cover query-execution, sql-optimization, benchmark-engine, frontend startup probes, and CI/doc truth alignment.
- Context closeout:
  - Completed scope: 扩展 scripts/run-runtime-smoke.sh 为多服务编排，真实拉起 governance、query-execution、sql-optimization、benchmark-engine 与前端 dev server；增强 scripts/health-check.sh 为多服务强制探针；同步调整默认 CI 与文档真值，使 runtime smoke 从 governance 单点扩展为多服务门禁。
  - Validation evidence: bash scripts/run-runtime-smoke.sh --compose-check；bash scripts/run-runtime-smoke.sh --runtime-smoke --reuse-running-stack --keep-stack；python3 scripts/foreman.py validate F-TASK-011 --include-task-audit --extra-command 'bash scripts/run-runtime-smoke.sh --compose-check' --extra-command 'bash scripts/run-runtime-smoke.sh --runtime-smoke --reuse-running-stack --keep-stack'；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前默认 CI 已覆盖多服务启动与基础健康探针，但仍未覆盖更深层跨服务业务回归、真实 Kafka 运行验证与生产密钥托管链路；query-execution 仍依赖共享加密配置环境变量注入 dev key 才能完成 smoke。
  - Next step: 继续把更深层跨服务业务 smoke、失败恢复路径和交付级 phase gate 验证并入默认 CI，优先补查询执行与治理链路之间的业务级联调证据。

### F-TASK-010: 接入运行时 smoke 到默认 CI

- Status: done
- Completed at: 2026-04-21
- Commit subject: `ci(runtime): F-TASK-010 gate compose and smoke checks`
- Priority: 1
- Depends on: F-TASK-005,F-TASK-006
- Scope: 把 docker compose config、本地启动/健康探针、消息队列 smoke 从本地脚本接入默认 CI，补齐 runtime 门禁与文档证据链。
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-010`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 把 compose 校验、本地依赖启动、governance 健康探针与消息队列 smoke 接入默认 CI；新增 scripts/run-runtime-smoke.sh 串联 compose 校验、local-start、governance 启动、health-check 与 manual-message-queue-smoke，并增强 health-check.sh 的可阻断模式；同时修复 GovernanceAuditTrailService 的 Spring 构造器装配，使 dev 启动路径可被真实验证。
  - Validation evidence: mvn -B -pl governance -am test；bash scripts/run-runtime-smoke.sh --compose-check；bash scripts/run-runtime-smoke.sh --runtime-smoke --reuse-running-stack --keep-stack；python3 scripts/foreman.py validate F-TASK-010 --include-task-audit --extra-command 'bash scripts/run-runtime-smoke.sh --compose-check' --extra-command 'bash scripts/run-runtime-smoke.sh --runtime-smoke --reuse-running-stack --keep-stack'；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前默认 runtime smoke 仍只覆盖 governance 与数据库消息队列链路，query-execution、sql-optimization、benchmark-engine 与前端尚未纳入统一启动验证；脚本使用 dev 测试密钥满足敏感字段加密初始化，仅适用于本地/CI smoke，不代表生产密钥托管已闭环。
  - Next step: 继续把其余后端模块与前端的真实启动探针纳入 CI，逐步把 runtime smoke 从 governance 单点扩展为多服务启动门禁。

### F-TASK-006: 接入 Java 规范扫描

- Status: done
- Completed at: 2026-04-21
- Commit subject: `ci(java): F-TASK-006 trace scan artifacts`
- Priority: 1
- Depends on: `F-TASK-004`
- Scope: 让 pmd/checkstyle 进入 CI Tech: `OPS`,`JAVA-BE`. Layer: `deployments/ci/scripts`,`common`.
- Matrix context: Phase-F / Story `F-STORY-002` CI 与质量门禁
- Human confirmation point: 扫描阈值与工具变更需人工确认
- Data impact: CI 质量结果、构建流程
- Rollback / recovery: 回退扫描接入并保留报告
- Validation:
  - `mvn validate pmd:pmd checkstyle:check`
  - `python3 scripts/foreman.py validate F-TASK-006`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 把 .github/workflows/ci.yml 中的 Java 质量检查拆成 validate、PMD、Checkstyle report、Checkstyle gate 四个显式步骤，并新增 Java 报告校验与 artifact 上传；新增 scripts/verify_java_quality_reports.py 校验各模块 PMD / Checkstyle XML 与 HTML 报告是否产出；同步更新 CI 能力基线与阿里 Java 规范落地文档。
  - Validation evidence: mvn -B -DskipTests validate pmd:pmd checkstyle:checkstyle checkstyle:check；python3 -m py_compile scripts/verify_java_quality_reports.py；python3 scripts/verify_java_quality_reports.py；python3 scripts/foreman.py validate F-TASK-006 --include-task-audit --extra-command 'python3 -m py_compile scripts/verify_java_quality_reports.py' --extra-command 'python3 scripts/verify_java_quality_reports.py'；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前 Java 扫描已可追溯并保留 artifact，但 PMD 仍沿用现有 p3c 规则集与默认阈值，扫描本身未拆成独立 job，也未生成跨历史趋势报表；若后续要把 PMD 违规数纳入硬阻断或趋势治理，仍需单独任务确认。
  - Next step: 优先补齐 compose/runtime smoke 与本地启动健康检查入 CI，把当前仍停留在本地脚本的运行时验证收进默认门禁。

### F-TASK-005: 接入阶段门禁脚本化验证

- Status: done
- Completed at: 2026-04-21
- Commit subject: `ci(gates): F-TASK-005 script phase gate checks`
- Priority: 1
- Depends on: `F-TASK-004`
- Scope: 让阶段切换可阻断 Tech: `OPS`,`DOCS`. Layer: `deployments/ci/scripts`,`docs`.
- Matrix context: Phase-F / Story `F-STORY-002` CI 与质量门禁
- Human confirmation point: 门禁阻断规则放宽需人工确认
- Data impact: CI 阻断逻辑
- Rollback / recovery: 恢复原门禁规则
- Validation:
  - 门禁阻断验证
  - `python3 scripts/foreman.py validate F-TASK-005`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 scripts/run-phase-gates.sh 与 scripts/verify_compliance_baseline.py，把 R-116/R-117/R-118 阶段门禁脚本化；在 .github/workflows/ci.yml 增加 task_audit 与 foreman compile-governance --check 的默认阻断，并新增手动 phase-gate workflow 承载可执行的 entry/compliance/delivery 门禁；同步更新 CI 能力基线、阶段门禁基线、docs 入口与文档覆盖矩阵。
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-005 --include-task-audit；node scripts/lint-repository-knowledge.js；bash scripts/run-phase-gates.sh --gate entry；bash scripts/run-phase-gates.sh --gate compliance；bash scripts/run-phase-gates.sh --gate delivery --coverage-phase phase0；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前默认 CI 仍未把完整 delivery gate 设为每次提交必跑，phase1plus 85% 覆盖率与 Sonar 依赖仍通过手动 phase-gate workflow 承载；compose/runtime smoke 也尚未接入默认流水线。
  - Next step: 进入 F-TASK-006，把 Java 静态扫描结果在 CI 中拆分为可追踪的质量步骤与工件，补齐 R-151/R-152 的落地证据。

### F-TASK-004: 盘点现有 CI 能力

- Status: done
- Completed at: 2026-04-21
- Commit subject: `docs(ci): F-TASK-004 inventory current CI coverage`
- Priority: 1
- Depends on: `Phase-C`,`Phase-E`
- Scope: 盘点 CI 对 lint/build/test 的覆盖 Tech: `OPS`,`DOCS`. Layer: `deployments/ci/scripts`,`docs`.
- Matrix context: Phase-F / Story `F-STORY-002` CI 与质量门禁
- Human confirmation point: 无
- Data impact: CI 清单和流水线映射
- Rollback / recovery: 恢复原 CI 清单说明
- Validation:
  - `CI 清单完整性检查`
  - `python3 scripts/foreman.py validate F-TASK-004`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 docs/deployments/ci-capability-baseline.md，盘点 .github/workflows/ci.yml 当前对 lint/build/test/scan 的真实覆盖，明确本地可执行但尚未进入 CI 的 task_audit、foreman validate、phase gate coverage、compose/runtime smoke 等缺口，并同步更新 docs 入口、文档真值基线与覆盖矩阵。
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-004 --include-task-audit；node scripts/lint-repository-knowledge.js；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前 .github/workflows/ci.yml 仍未接入 task_audit、foreman compile-governance --check、phase gate coverage threshold、compose/runtime smoke，也未把 Sonar 固化为默认必经门禁；这些缺口需要后续 F-TASK-005/006 补齐。
  - Next step: 进入 F-TASK-005，把 task_audit、governance compile check 和阶段门禁脚本化接入 CI，先形成真正可阻断的 phase gate。

### F-TASK-009: 阶段交付回写闭环

- Status: done
- Completed at: 2026-04-21
- Commit subject: `docs(delivery): F-TASK-009 close phase-f delivery loop`
- Priority: 1
- Depends on: `F-TASK-008`
- Scope: 完成记录、commit、tag、回写闭环 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Matrix context: Phase-F / Story `F-STORY-003` 运维、审计与恢复
- Human confirmation point: 交付闭环若省略 commit/tag 回写需人工确认
- Data impact: 交付记录、git 元数据、验证日志
- Rollback / recovery: 补写交付记录和标签/提交元数据
- Validation:
  - `交付记录与 git 元数据一致`
  - `python3 scripts/foreman.py validate F-TASK-009`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 docs/deliveries/phase-f-story-003-ops-closeout.md，统一记录 Phase-F Story-003 下 F-TASK-007/008 的交付基线、F-TASK-009 的 delivery closeout 清单与 write-back 模板，并同步更新 docs 入口与文档覆盖矩阵，形成阶段交付闭环的文档落点。
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-009 --include-task-audit；node scripts/lint-repository-knowledge.js；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前 foreman 的 delivery-closeout 只负责打 tag 和追加 write-back，实际 write-back 仍会在仓库内留下后续元数据改动；此外 F-STORY-003 的交付闭环仍依赖人工维护 tag 命名策略和交付记录选择。
  - Next step: 执行 delivery-closeout，为本次 Phase-F Story-003 交付主提交打 tag，并把 tag/write-back 元数据回填到 docs/deliveries/phase-f-story-003-ops-closeout.md。

### F-TASK-008: 补齐备份恢复策略与演练记录模板

- Status: done
- Completed at: 2026-04-21
- Commit subject: `docs(deploy): F-TASK-008 add backup recovery baseline`
- Priority: 1
- Depends on: `F-TASK-007`
- Scope: 定义 RPO/RTO/演练记录模板 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Matrix context: Phase-F / Story `F-STORY-003` 运维、审计与恢复
- Human confirmation point: 备份恢复目标或演练频率变更需人工确认
- Data impact: 备份元数据、演练记录
- Rollback / recovery: 恢复旧模板并补录演练
- Validation:
  - 备份恢复模板可追溯
  - `python3 scripts/foreman.py validate F-TASK-008`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 docs/deployments/backup-recovery-baseline.md，把 MySQL、audit_log、export_record、kafka_message_queue、system_config 密文与密钥边界的备份对象、RPO/RTO、责任角色和恢复演练模板统一收口，并同步更新 docs 入口、华为云部署引用、文档真值基线与覆盖矩阵。
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-008 --include-task-audit；node scripts/lint-repository-knowledge.js；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前仓库仍未提供 MySQL/OBS 备份自动化脚本、密钥托管与轮换校验工具、以及恢复后自动 smoke 脚本；query-execution、sql-optimization、benchmark-engine 仍无独立持久化，因此服务级恢复更多依赖健康检查与治理元数据验收。
  - Next step: 进入 F-TASK-009 时，基于本备份恢复基线把演练记录、commit/tag、交付回写和 write-back 模板一起收口，形成阶段交付闭环。

### F-TASK-007: 补齐监控与日志规范落地清单

- Status: done
- Completed at: 2026-04-21
- Commit subject: `docs(deploy): F-TASK-007 add observability baseline checklist`
- Priority: 1
- Depends on: `Phase-D`
- Scope: 输出 logs/metrics/alerts 落地清单 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Matrix context: Phase-F / Story `F-STORY-003` 运维、审计与恢复
- Human confirmation point: 监控/日志采样策略显著削弱需人工确认
- Data impact: 日志、指标、告警配置
- Rollback / recovery: 恢复原监控规则
- Validation:
  - 清单完整、映射一致
  - `python3 scripts/foreman.py validate F-TASK-007`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-21: audited current observability facts across 4 backend modules, confirming shared `logback-spring.xml` baselines, shared actuator exposure (`health/info/metrics/prometheus`), governance audit fallback logs, and async task flow logs already exist in code/config.
  - 2026-04-21: created `docs/deployments/observability-baseline.md` to distinguish current implemented logs/metrics signals from still-missing business metrics, alert rules, and tracing/platform pipeline integrations, then wired the new authority doc into docs entrypoints and coverage tracking.
- Context closeout:
  - Completed scope: 新增 `docs/deployments/observability-baseline.md`，把 4 个后端服务当前已落地的日志、Actuator 暴露、审计/异步状态流信号统一收口为 logs/metrics/alerts 运维清单，并同步更新文档入口、华为云部署引用、文档真值基线与覆盖矩阵。
  - Validation evidence: `python3 scripts/foreman.py validate F-TASK-007 --include-task-audit`；`node scripts/lint-repository-knowledge.js`；`python3 scripts/task_audit.py --check --phase pre-closeout`；`python3 scripts/task_audit.py --check --phase post-closeout`。
  - Residual risk: 当前仓库仍未实现业务级 Micrometer 指标、仓库内 Prometheus/Alertmanager/Grafana 规则文件和统一 tracing/log pipeline 配置，业务告警仍需外部平台按本文档补位。
  - Next step: 进入 `F-TASK-008` 时，直接复用本 observability 基线中的审计、消息队列、导出与敏感数据观测项，补齐备份恢复策略和演练模板。

### D-TASK-014: 收口异步服务鉴权、占位执行与审计兜底

- Status: done
- Completed at: 2026-04-21
- Commit subject: `feat(runtime): D-TASK-014 secure async placeholders and audit fallback`
- Priority: 1
- Depends on: D-TASK-013
- Scope: 在 sql-optimization 与 benchmark-engine 落实 header-based 鉴权、租户隔离与异步占位执行，补 benchmark 原始报告数据查询，并为 governance 审计消息增加数据库队列兜底，同时收紧共享加密配置到显式密钥基线。
- Validation:
  - `python3 scripts/foreman.py validate D-TASK-014`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 在 `sql-optimization` 与 `benchmark-engine` 落实了 header-based 请求鉴权、租户隔离和异步占位执行器，新增 benchmark 原始报告数据查询接口，并为 `governance` 审计消息主路由失败补上数据库队列兜底，同时把共享敏感加密配置改为显式密钥基线。
  - Validation evidence: `python3 scripts/foreman.py validate D-TASK-014 --include-task-audit --extra-command "mvn -B -pl benchmark-engine -am test" --extra-command "mvn -B -pl sql-optimization -am test" --extra-command "mvn -B -pl governance -am test"`；`python3 scripts/task_audit.py --check --phase pre-closeout`；`python3 scripts/task_audit.py --check --phase post-closeout`。
  - Residual risk: benchmark/sql-optimization 仍然是本地占位执行链，尚未接入真实 worker、持久化队列与跨服务主动审计上报。
  - Next step: 后续若把这批能力固化进长期计划，需要把 D-TASK-014 补回主计划与任务矩阵，并继续把异步任务接到真实执行/回调链。

### HARN-009: Repair closeout archive boundaries and done-ledger structure checks

- Status: done
- Completed at: 2026-04-21
- Commit subject: `fix(codex): HARN-009 repair closeout boundary auditing`
- Priority: 1
- Depends on: HARN-008
- Scope: 只修两个治理缺口：`foreman closeout` 归档任务块时必须限制在当前台账 section 内，不得把后续 section heading 一并搬入 `tasks-done.md`；同时为 `task_audit` 增加 `tasks-done.md` 结构校验，明确禁止 `## In Review`、`## Blocked` 等 heading 混入完成台账正文。本批次允许同步修正当前已被污染的 `tasks-done.md` 结构，但不触碰任何业务模块脏改动。
- Validation:
  - `python3 -m py_compile scripts/foreman.py scripts/task_audit.py`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `python3 scripts/task_audit.py --check --phase post-closeout`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/foreman.py compile-governance --check`
- Progress log:
  - 2026-04-21: instantiated as a minimal governance repair batch after post-HARN-008 review found that `foreman closeout` could archive trailing section content into `tasks-done.md`, and `task_audit` failed to detect the resulting structural corruption.
  - 2026-04-21: restricted `extract_task_blocks_with_spans()` so closeout now stops at the next task header or the next section heading, which prevents `tasks.md` trailing section bodies from being archived into `tasks-done.md`.
  - 2026-04-21: added explicit `tasks-done.md` structural checks to `task_audit.py` for unexpected `##` headings and stray non-task content inside the done section, then repaired the already polluted `HARN-008` done block back to the intended shape.
- Context closeout:
  - Completed scope: Restricted foreman task-block archiving to the current ledger section, added explicit tasks-done structure validation for unexpected headings and stray non-task content, repaired the previously polluted HARN-008 done block, and wired exec-plan move plus coverage-matrix/policy recompilation so closeout stays structurally consistent.
  - Validation evidence: python3 -m py_compile scripts/foreman.py scripts/task_audit.py; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/task_audit.py --check --phase post-closeout; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance --check; python3 scripts/foreman.py validate HARN-009 --include-task-audit; targeted synthetic done-ledger corruption probe now fails with unexpected-heading and stray-content errors
  - Residual risk: The repair now blocks the concrete closeout-ledger corruption path and detects section-level contamination, but future ledger format expansions still need to stay aligned across foreman, task_audit, and document indexes.
  - Next step: Use the repaired closeout path as the default governance closeout flow, and if later extending ledger formats, update both archiving boundaries and structural audits in the same batch.

### HARN-008: Close the 6 remaining Codex governance runtime gaps

- Status: done
- Completed at: 2026-04-21
- Commit subject: `feat(codex): HARN-008 close the remaining governance runtime gaps`
- Priority: 1
- Depends on: HARN-007
- Scope: 只修复 HARN-007 严格复核遗留的 6 个治理/runtime 闭口点：hooks 输出协议与 hooks.json 组织、preflight 真实消费文档真值、instantiate 真实消费任务矩阵、closeout 与 delivery-closeout 真正落地、运行态 closeout 后清理、以及可信本地验证证据；不触碰当前业务模块脏改动。
- Validation:
  - `python3 -m py_compile scripts/foreman.py .codex/hooks/*.py`
  - `python3 scripts/validate_codex_runtime.py`
  - `python3 scripts/foreman.py compile-governance --check`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-21: instantiated as the strict-mode follow-up batch to close the 6 unresolved governance/runtime gaps left after HARN-007, with write scope restricted to docs, ledgers, `.codex/`, and repository governance scripts only.
  - 2026-04-21: replaced the hard-coded foreman scaffolding with document-aware preflight, matrix-aware instantiate, explicit-stage closeout, delivery-closeout tag/write-back semantics, dynamic governance compilation, and idle-state cleanup after closeout.
  - 2026-04-21: aligned `.codex/hooks.json` and repository-local hook handlers with the official Codex hook output contracts, including `hookSpecificOutput.additionalContext` for `UserPromptSubmit` and event-specific permission decisions for `PreToolUse` / `PermissionRequest`.
  - 2026-04-21: fixed `.codex/config.toml` so project-doc settings no longer sit under the `[features]` table, which had been breaking real `codex exec` startup.
  - 2026-04-21: added `scripts/validate_codex_runtime.py` to deterministically validate preflight, matrix instantiate, delivery-closeout dry-run, hook output shapes, stop gating, and a trusted local `codex exec --json` path.
  - 2026-04-21: reran `python3 scripts/foreman.py validate HARN-008 --include-task-audit --extra-command 'codex exec --json --sandbox read-only --skip-git-repo-check "Reply with OK only."'`; governance validation, runtime simulation, task audit, repository knowledge lint, compile-governance drift check, and real Codex CLI execution all passed.
- Context closeout:
  - Completed scope: Aligned repository-local Codex hooks with official event contracts, replaced the hard-coded foreman scaffolding with document-aware preflight and matrix-aware instantiate, implemented explicit-stage closeout and delivery-closeout semantics, fixed project-scoped Codex config loading, added deterministic runtime validation plus a real codex exec verification path, and cleaned the HARN exec-plan/archive wiring without touching business-module dirty changes.
  - Validation evidence: python3 scripts/foreman.py validate HARN-008 --include-task-audit --extra-command "codex exec --json --sandbox read-only --skip-git-repo-check \"Reply with OK only.\""; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance --check
  - Residual risk: Repository-local hooks still depend on trusted-project loading in the users Codex environment, so AGENTS.md plus foreman/task_audit/lint remain the mandatory fallback enforcement chain.
  - Next step: Use the hardened foreman closeout and delivery-closeout path for later governance and F-task work, and keep task-matrix / blueprint / hook contracts in sync as the repository evolves.

### HARN-007: Integrate Codex runtime with repository truth and closeout flow

- Status: done
- Priority: 1
- Depends on: HARN-006
- Completed at: 2026-04-21
- Commit subject: `feat(codex): HARN-007 wire runtime governance scaffolding`
- Scope: 在不丢失现有 `docs/`、任务台账、验证日志、closeout、Git 审计链和 F-task 语义的前提下，把 SQLForge 的文档真值体系接入 Codex 的执行前、执行中、执行后生命周期；本轮只改治理与接线层，不触碰当前业务模块中的脏工作树改动。
- Validation:
  - `python3 -m py_compile scripts/foreman.py .codex/hooks/*.py`
  - `python3 scripts/foreman.py compile-governance`
  - `python3 scripts/foreman.py compile-governance --check`
  - `python3 scripts/foreman.py preflight --task HARN-007 --task-class standard --prompt "Integrate Codex runtime with repository truth and closeout flow under strict mode."`
  - `python3 scripts/foreman.py validate HARN-007 --include-task-audit`
  - `python3 scripts/foreman.py sync-context --done-ready`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-21: instantiated `HARN-007` as the strict-mode governance task for Codex-docs runtime integration after confirming the root cause was “docs truth exists but Codex execution path is not directly wired to it”.
  - 2026-04-21: current repository contains unrelated dirty business-module edits under `benchmark-engine/`, `governance/`, `sql-optimization/`, and `sqlforge-shared/`; this task therefore stayed inside governance docs, Codex integration scaffolding, scripts, and repository knowledge checks.
  - 2026-04-21: wrote the active exec plan and the formal `codex-governance-integration-blueprint.md`, then replaced the root navigation-only `AGENTS.md` with a hard repository contract that points Codex back to the authority chain and standard action entrypoints.
  - 2026-04-21: added project-scoped `.codex/config.toml`, `.codex/hooks.json`, repository-local hook handlers, runtime state examples, and a tracked current-task schema so Codex can consume repo policy without promoting `.codex/` into a second source of truth.
  - 2026-04-21: implemented `scripts/foreman.py` as the unified governance entrypoint for `preflight`, `instantiate`, `sync-context`, `validate`, `audit`, `compile-governance`, `closeout`, and `delivery-closeout`; the closeout commands are intentionally bootstrap-gated pending a later hardening batch that can safely auto-scope staged files.
  - 2026-04-21: updated `docs/README.md`, `docs/plans/README.md`, `docs/operations/README.md`, `docs/operations/local-development.md`, `README.md`, and `docs/plans/document-coverage-matrix.md` so the new Codex integration artifacts are visible to both humans and machine checks.
  - 2026-04-21: expanded `scripts/lint-repository-knowledge.js` to require the new blueprint, `.codex` scaffolding, and `scripts/foreman.py`, fixed the resulting README / coverage-matrix gaps, recompiled `.codex/policy/*.json`, and reran governance checks until both knowledge lint and compile-governance drift checks passed.
- Context closeout:
  - Completed scope: Codex runtime governance scaffolding is now wired into the repository through a formal blueprint, hardened root `AGENTS.md`, project-scoped `.codex` config/hooks/state/schema, a unified `scripts/foreman.py` CLI, generated governance manifests, and updated documentation/knowledge-lint indexes.
  - Validation evidence: `python3 -m py_compile scripts/foreman.py .codex/hooks/*.py`; `python3 scripts/foreman.py compile-governance`; `python3 scripts/foreman.py compile-governance --check`; `python3 scripts/foreman.py preflight --task HARN-007 --task-class standard --prompt "Integrate Codex runtime with repository truth and closeout flow under strict mode."`; `python3 scripts/foreman.py validate HARN-007 --include-task-audit`; `python3 scripts/foreman.py sync-context --done-ready`; `python3 scripts/task_audit.py --check --phase pre-closeout`; `node scripts/lint-repository-knowledge.js`
  - Residual risk: repository-local `.codex` hooks still need live validation inside a trusted Codex project, and `closeout` / `delivery-closeout` remain intentionally bootstrap-gated until a later batch can safely automate archival + commit scoping without catching unrelated dirty files.
  - Next step: validate the new `.codex` layer inside a trusted Codex runtime, then implement file-scope-aware automatic closeout and delivery-closeout semantics as the next governance hardening batch.

### HARN-006: Restore escalation failure-disposition wording

- Status: done
- Priority: 1
- Depends on: HARN-005
- Completed at: 2026-04-21
- Commit subject: `docs(harness): HARN-006 harden escalation and inbox audit chain`
- Scope: 在已恢复人工决策升级链“失败处置”显式语义的基础上，继续收口本轮严格模式复核发现的 4 个剩余问题：修正 `HARN-006` 的状态/证据口径、补强机器可检查的人类决策升级约束、补齐 `INBOX.md` 双向追溯格式与校验，并把验证日志从“closeout 证据”改回准确的工作树审计快照口径。
- Validation:
  - `python3 -m py_compile scripts/task_audit.py`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-21: review confirmed the former `R-158-A` failure-disposition wording no longer existed as explicit text after merge, and the current `R-156` / `R-160` wording had weakened to broad task-ledger or Git repair language.
  - 2026-04-21: restored explicit escalation-chain failure disposition wording in `docs/quality/validation-rules.md`, `docs/rules/codex-rules.md`, and `docs/operations/human-collaboration.md`.
  - 2026-04-21: user instructed to directly repair all 4 strict-mode review findings, which resolved the earlier closeout-strategy decision point and returned `HARN-006` to active execution.
  - 2026-04-21: current task remains active, so its validation evidence must be recorded as working-tree audit snapshots rather than closeout evidence until archival and single-task commit are complete.
  - 2026-04-21: updated `tasks.md`, `INBOX.md`, `docs/rules/codex-rules.md`, `docs/quality/validation-rules.md`, `docs/operations/human-collaboration.md`, `docs/operations/foreman-workflow.md`, and `scripts/task_audit.py` so unresolved human-decision markers are forbidden in `todo` / `in_progress`, `blocked` / `in_review` require `INBOX ref:`, and `INBOX` now has an explicit dual-traceability format.
  - 2026-04-21: reran `python3 -m py_compile scripts/task_audit.py`, `python3 scripts/task_audit.py --check --phase pre-closeout`, and `node scripts/lint-repository-knowledge.js`; current evidence is recorded as working-tree validation snapshots, not closeout evidence.
  - 2026-04-21: refined `task_audit.py` so `todo` / `in_progress` only fail on structured unresolved-human-decision fields, not on narrative text that merely mentions field names; reran py-compile, task audit, and repository knowledge lint after the fix.
  - 2026-04-21: tightened `task_audit.py` again so `blocked` / `in_review` now require structured field lines instead of loose substring matches, and `INBOX.md` entries are machine-checked for `Status:` / `Needed decision:` plus `Task refs:` / `Plan refs:` presence and task-id back references when a task uses `INBOX ref:`.
  - 2026-04-21: aligned `INBOX.md`, `docs/rules/codex-rules.md`, `docs/quality/validation-rules.md`, and `docs/operations/human-collaboration.md` so `Plan refs:` is allowed for plan-only items, but any task-linked `INBOX` item must list the task in `Task refs:`; reran py-compile, task audit, and repository knowledge lint after the alignment.
- Context closeout:
  - Completed scope: escalation failure-disposition wording, task-state upgrade rules, INBOX dual-traceability format, and machine-checkable escalation/inbox validation are now aligned across rule ledger, validation rules, operations docs, task ledger guidance, inbox guidance, and `task_audit.py`.
  - Validation evidence: `python3 -m py_compile scripts/task_audit.py`; `python3 scripts/task_audit.py --check --phase pre-closeout`; `node scripts/lint-repository-knowledge.js`
  - Residual risk: semantic judgment about whether a specific narrative truly requires human escalation still cannot be fully automated; current checks enforce structured markers and known waiting phrases rather than full natural-language intent.
  - Next step: run immediate post-closeout audit after this single-task commit and confirm the archived `HARN-006` commit subject is visible in Git history.

### HARN-005: Close remaining strict-mode review gaps

- Status: done
- Priority: 1
- Depends on: HARN-004
- Completed at: 2026-04-21
- Commit subject: `docs(harness): HARN-005 close strict-mode review gaps`
- Scope: 收口严格模式复核剩余 4 条缺口：为 `task_audit` 增加 pre/post closeout phase、补 `HARN-004` 的 post-closeout 证据、把 `tasks-done.md` 的“最新归档在上”排序规则显式写入规则与流程并形成校验、以及为 `blocked` / `in_review` 增加人类决策升级链的机器可检查字段要求。
- Validation:
  - `python3 -m py_compile scripts/task_audit.py`
  - `python3 scripts/task_audit.py --check --phase post-closeout`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-21: strict-mode full review confirmed 4 remaining gaps after `HARN-004`: missing `HARN-004` post-closeout evidence, no phase-specific `task_audit` entrypoint, implicit `tasks-done.md` ordering dependency, and no machine-checkable metadata for “must ask human” task states.
  - 2026-04-21: expanded `scripts/task_audit.py` with explicit `--phase pre-closeout|post-closeout`, newest-first done-ledger checks, and required `Human decision:` / `Review reason:` metadata for `blocked` and `in_review` tasks.
  - 2026-04-21: aligned `docs/rules/codex-rules.md`, `docs/operations/foreman-workflow.md`, `docs/operations/git-and-task-closeout.md`, `docs/operations/human-collaboration.md`, `docs/quality/validation-rules.md`, `tasks.md`, and `docs/quality/validation-log.md`, and repaired the legacy `HARN-002` / `HARN-001` archive ordering drift so the new checks consume a consistent ledger.
- Context closeout:
  - Completed scope: phase-aware closeout auditing, archived-task ordering governance, and human-decision escalation metadata are now formalized in rules, operations docs, task ledger guidance, and machine checks; `HARN-004` also gained formal post-closeout evidence backfill.
  - Validation evidence: `python3 -m py_compile scripts/task_audit.py`; `python3 scripts/task_audit.py --check --phase post-closeout`; `python3 scripts/task_audit.py --check --phase pre-closeout`; `node scripts/lint-repository-knowledge.js`
  - Residual risk: `R-165` now has stronger machine-checkable task-state enforcement, but semantic judgment about whether a specific ambiguity truly required escalation still cannot be fully automated.
  - Next step: continue future strict-mode automation only if higher-fidelity ambiguity detection can be added without creating noisy false positives.

### HARN-004: Tighten closeout audit boundary and post-closeout traceability

- Status: done
- Priority: 1
- Depends on: HARN-003
- Completed at: 2026-04-21
- Commit subject: `docs(harness): HARN-004 tighten closeout audit chain`
- Scope: 修复严格模式复核识别出的两个 `P0` 缺口与一个 `P1` 证据链缺口：收紧 `task_audit` 对 pending commit 的豁免边界，重排 closeout 流程使 `R-168` 在 pre-commit 审计前即可检查到当前归档任务，并补齐 post-closeout 可追溯要求的正式文档口径。
- Validation:
  - `python3 -m py_compile scripts/task_audit.py`
  - `python3 scripts/task_audit.py --check`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-21: strict-mode re-review confirmed that the previous pending-commit exception in `scripts/task_audit.py` was too broad because any same-day archived task could slip through when the worktree was dirty, which weakened `R-157` / `R-160`.
  - 2026-04-21: tightened `scripts/task_audit.py` so pre-commit closeout now allows at most one newest same-day archived task to be pending Git history, while all other `tasks-done.md` entries must remain fully traceable.
  - 2026-04-21: aligned `docs/operations/foreman-workflow.md`, `docs/operations/git-and-task-closeout.md`, and `docs/quality/validation-rules.md` so the current closeout task must already be archived into `tasks-done.md` with `Context closeout` before pre-commit audit, and post-commit recheck is now an explicit required step.
- Context closeout:
  - Completed scope: closeout workflow, validation-rule wording, and task-audit enforcement were tightened so `R-168` now covers the current archived task during pre-commit audit, and the pending-commit exception is limited to the single current closeout task.
  - Validation evidence: `python3 -m py_compile scripts/task_audit.py`; `python3 scripts/task_audit.py --check`; `node scripts/lint-repository-knowledge.js`
  - Residual risk: strict mode still cannot fully automate semantic judgment for all “must ask human” scenarios; the strengthened audit now covers closeout shape and Git traceability, not every ambiguity class.
  - Next step: run immediate post-commit recheck for this task and confirm the archived `HARN-004` commit subject is now visible in Git history.

### HARN-003: Close strict-mode and context-closeout governance loop

- Status: done
- Priority: 1
- Depends on: DOC-GOV-005
- Completed at: 2026-04-21
- Commit subject: `docs(harness): HARN-003 close strict-mode governance loop`
- Scope: 将严格模式与任务收尾的上下文收缩/清理要求从单点规则扩展为完整治理链，补齐规则入口、operations 流程、验证规则、验证日志、历史约束、任务归档与机器审计的一致口径，并明确 `/contract`、`/clear` 仅为环境可选手段而非唯一工程要求。
- Validation:
  - `python3 -m py_compile scripts/task_audit.py`
  - `python3 scripts/task_audit.py --check`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-21: aligned `R-168` consumption across `docs/README.md`, `docs/architecture/init.md`, `docs/operations/README.md`, `docs/operations/foreman-workflow.md`, `docs/operations/git-and-task-closeout.md`, `docs/operations/human-collaboration.md`, and `docs/plans/implementation-readiness.md` so strict mode and task closeout now use one consistent Harness Engineering wording.
  - 2026-04-21: updated `docs/quality/validation-rules.md`, `scripts/task_audit.py`, and `scripts/lint-repository-knowledge.js` so repository checks now cover `R-168` closeout markers and validation index continuity, then repaired `docs/quality/validation-log.md` to keep the new governance evidence append-only.
  - 2026-04-21: appended the human decision trail in `docs/references/human-constraint-history.md`, backfilled `Context closeout` sections for applicable completed tasks, and archived this governance batch for single-task git closeout.
- Context closeout:
  - Completed scope: strict-mode and context-closeout governance is now closed across rule definitions, operations guidance, validation linkage, machine audit, history ledger, and append-only evidence.
  - Validation evidence: `python3 -m py_compile scripts/task_audit.py`; `python3 scripts/task_audit.py --check`; `node scripts/lint-repository-knowledge.js`
  - Residual risk: `R-165` default strict mode仍主要依赖文档治理与人工执行纪律，仓库目前只对其关键 closeout 结果通过 `R-168` 做机器校验，而未对所有“需人工决策”场景做全自动语义判定。
  - Next step: 后续若要继续提高严格模式自动化强度，应在不引入伪阳性的前提下，为 `INBOX.md` / 任务日志中的人工确认链增加更细粒度的静态校验。

### D-TASK-013: 落实敏感字段加密

- Status: done
- Priority: 1
- Depends on: D-TASK-011
- Completed at: 2026-04-21
- Commit subject: `feat(governance): D-TASK-013 implement sensitive data encryption baseline`
- Scope: 在 Phase-D 已稳定的 `config/result/history/export/audit` 追溯链基础上，补共享 AES-256 敏感字段保护能力，并把 `governance` 内的 config snapshot、execution result、query history、export record、audit log 与 `system_config` 接到统一受保护持久化入口；优先收口密码、token、key 的密文存储，以及审计/导出相关字段的脱敏落库基线，不提前展开完整配置中心 UI、外部 KMS 或其他服务的真实上报改造。
- Validation:
  - 库、日志、导出无明文
  - `mvn -B -pl governance -am clean compile`
  - `mvn -B -pl governance -am test`
  - `mvn -B -pl governance -am validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-21: instantiated from `Phase-D / D-STORY-004` after D-TASK-012 stabilized the real audit write path, so the traceability chain could move from “schema exists” to “sensitive leaves are actually protected before persistence”.
  - 2026-04-21: added shared AES-256 GCM crypto and sensitive-key classification utilities under `sqlforge-shared`, so governance and later services can reuse one envelope format instead of ad hoc masking rules.
  - 2026-04-21: introduced `GovernanceProtectedPersistenceService` to protect `config_snapshot.snapshot_payload`, `execution_result.result_payload`, `query_history.query_context`, `export_record.export_options`, `audit_log.request_params/response_summary`, and raw SQL ciphertext writes before mapper persistence.
  - 2026-04-21: extended `system_config` with `sensitive_flag/value_ciphertext/value_mask/encryption_algorithm/encryption_key_id` and added `SystemConfigMapper`, so password/token/key style settings now have a stable ciphertext storage baseline instead of competing for the plain `config_value` column.
  - 2026-04-21: switched the existing governance audit write path onto the protected persistence service, added tests covering encrypted traceability leaves, masked audit/export fields, ciphertext SQL history payloads, and sensitive system-config storage, then synced persistence, security, capability, truth, init, repo-map, validation log, and task ledger documents.
- Context closeout:
  - Completed scope: shared AES-256 sensitive-field protection entered `sqlforge-shared`, governance traceability persistence paths now write ciphertext or masked payloads, and related authority docs were synchronized.
  - Validation evidence: `mvn -B -pl governance -am clean compile`; `mvn -B -pl governance -am test`; `mvn -B -pl governance -am validate pmd:pmd checkstyle:check`; `node scripts/check-frontend-backend-separation.js`; `node scripts/lint-repository-knowledge.js`; `python3 scripts/task_audit.py --check`
  - Residual risk: external KMS, other service consumers, and UI/admin management paths still remain follow-up work outside this task scope.
  - Next step: continue Phase-D traceability and security follow-up tasks on the now-protected persistence baseline instead of reopening plaintext storage semantics.

### D-TASK-012: 落实审计日志链路

- Status: done
- Priority: 1
- Depends on: D-TASK-011
- Completed at: 2026-04-21
- Commit subject: `feat(governance): D-TASK-012 implement audit persistence chain`
- Scope: 基于 D-TASK-011 已固化的 `config/result/history/export/audit` 追溯链，把 `governance` 内部 `POST /api/governance/internal/audit/write` 从“只发消息”升级为“真实写入 `audit_log` + 保留审计事件扩散”，同时把当前 header-based stateless auth 的鉴权建立/释放接入 `LOGIN` / `LOGOUT` 审计落库，并优先覆盖 SQL 操作、登录登出、权限变更三类事件的真实写入路径与校验。
- Validation:
  - `R-113` 场景测试
  - `mvn -B -pl governance -am clean compile`
  - `mvn -B -pl governance -am test`
  - `mvn -B -pl governance -am validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-21: instantiated from `Phase-D / D-STORY-004` after D-TASK-011 stabilized the core traceability tables and mapper baseline, so audit no longer had to stay as a message-only placeholder.
  - 2026-04-21: added `GovernanceAuditTrailService` and upgraded `/api/governance/internal/audit/write` to synchronously insert into `audit_log`, validate optional `configSnapshotId/resultId/historyId/exportId` references, and keep `governance.audit.event` fan-out semantics.
  - 2026-04-21: extended `AuditWriteRequest` / `AuditWriteResponse` so callers can attach `sagaId`, traceability foreign keys, and desensitized request/response summaries while receiving a stable `auditId` back from the real persistence path.
  - 2026-04-21: connected `AuthInterceptor` to real audit writes so the current header-based stateless auth baseline now records `LOGIN` / `LOGOUT` audit events and failed pre-auth attempts in `audit_log`.
  - 2026-04-21: added dedicated audit-chain tests covering SQL operation writes, permission-change writes, login/logout/failure auth writes, and missing traceability reference rejection, then synced interface baseline, capability map, persistence baseline, access-control spec, truth baseline, init summary, repo map, validation log, and task ledger.
- Context closeout:
  - Completed scope: governance audit write path now persists real `audit_log` records, auth events are written through the same chain, and the audit contract/document truth was synchronized.
  - Validation evidence: `R-113` scenarios; `mvn -B -pl governance -am clean compile`; `mvn -B -pl governance -am test`; `mvn -B -pl governance -am validate pmd:pmd checkstyle:check`; `node scripts/check-frontend-backend-separation.js`; `node scripts/lint-repository-knowledge.js`; `python3 scripts/task_audit.py --check`
  - Residual risk: cross-service callers are still limited to the current transitional skeleton, and broader audit retention/reporting tooling remains future work.
  - Next step: build on the real audit persistence chain for subsequent traceability, compliance, and governance-service hardening tasks.

### D-TASK-011: 补齐核心表与关联键设计

- Status: done
- Priority: 1
- Depends on: Phase-C
- Completed at: 2026-04-21
- Commit subject: `feat(governance): D-TASK-011 define core traceability schema and keys`
- Scope: 在 `governance` 侧先固化 Phase-D 的事务型元数据底座，补齐 `config_snapshot`、`execution_result`、`query_history`、`export_record` 和扩展 `audit_log` 的主外键与追溯链；同步提供 `init-schema.sql`、增量 migration、Entity / Mapper XML、持久化权威文档与最小映射检查测试，不提前接入真实业务写入链、队列消费或导出引擎。
- Validation:
  - schema/sql/entity 映射检查
  - `mvn -B -pl governance -am clean compile`
  - `mvn -B -pl governance -am test`
  - `mvn -B -pl governance -am validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-21: instantiated from `Phase-D / D-STORY-004` after query-execution、sql-optimization 和 benchmark-engine 的接口与模型骨架稳定，开始把 config/result/history/export/audit 的关系型底座一次固化。
  - 2026-04-21: extended `sql/init-schema.sql` with `config_snapshot`、`execution_result`、`query_history`、`export_record` and trace-aware `audit_log`, then added `sql/migrations/V20260421_011__core_traceability_chain.sql` as the compatible incremental DDL for published environments.
  - 2026-04-21: added governance traceability entities and MyBatis XML mappers so table names, key columns, and FK chain are now represented in Java persistence skeletons instead of living only in SQL comments.
  - 2026-04-21: created `docs/architecture/persistence.md` as the current authority for MySQL persistence, shared trace keys, migration policy, and schema-to-entity mapping, then synced README, truth baseline, coverage matrix, init summary, service capability map, and repo map.
  - 2026-04-21: added `TraceabilitySchemaMappingTest` to keep `init-schema.sql`, migration script, and mapper XML aligned before later tasks connect real repository writes, audit ingestion, export generation, and cross-service persistence flows.
- Context closeout:
  - Completed scope: Phase-D traceability schema, incremental migration, entities, XML mappers, mapping test, and persistence authority document were all established as the current relational baseline.
  - Validation evidence: schema/entity mapping checks; `mvn -B -pl governance -am clean compile`; `mvn -B -pl governance -am test`; `mvn -B -pl governance -am validate pmd:pmd checkstyle:check`; `node scripts/check-frontend-backend-separation.js`; `node scripts/lint-repository-knowledge.js`; `python3 scripts/task_audit.py --check`
  - Residual risk: real repository writes, export generation, and downstream cross-service persistence usage are still follow-up work on top of this schema baseline.
  - Next step: connect actual persistence chains and audit/export flows to the stabilized traceability tables instead of revisiting the schema contract.

### D-TASK-010: 实现压测报告查询接口

- Status: done
- Priority: 1
- Depends on: D-TASK-009
- Completed at: 2026-04-21
- Commit subject: `feat(benchmark-engine): D-TASK-010 add report query api skeleton`
- Scope: 在 `benchmark-engine` 中补 `GET /api/benchmark-engine/reports/{reportId}`，基于已落库的 placeholder `BenchmarkReport` 打通 JSON / PDF / HTML 三种查询形态；同步固化趋势图表、格式元数据、查询路径和原始数据下载占位路径，不提前接入真实导出引擎、文件存储或真实执行链路。
- Validation:
  - 报告 JSON/PDF/HTML/404/非法格式测试
  - `mvn -B -pl benchmark-engine -am clean compile`
  - `mvn -B -pl benchmark-engine -am test`
  - `mvn -B -pl benchmark-engine -am validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-21: instantiated from `Phase-D / D-STORY-003` after `D-TASK-009` stabilized placeholder task submit/poll flow and report persistence.
  - 2026-04-21: added `BenchmarkReportController` and `BenchmarkReportApplicationService` so `benchmark-engine` now exposes `GET /api/benchmark-engine/reports/{reportId}` with `format=JSON|PDF|HTML`.
  - 2026-04-21: upgraded `BenchmarkReportResponse` from model-only payload to report-query baseline by adding target engines, trend charts, available formats, report query path, and raw-data download placeholder path.
  - 2026-04-21: kept JSON as the structured default response, and rendered PDF / HTML as placeholder export bodies with stable content type and `Content-Disposition` metadata so later real templates can replace the renderer without reshaping the HTTP contract.
  - 2026-04-21: extended controller and service tests to cover JSON success, PDF success, HTML success, missing report 404, and invalid format 400, then synced README, interface baseline, capability map, truth baseline, C4, init summary, repo map, and validation log to `REPORT_QUERY_API_SKELETON`.
- Context closeout:
  - Completed scope: benchmark-engine now exposes stable report query skeletons for JSON/PDF/HTML and the report response contract includes format/query/download metadata for later real exporters.
  - Validation evidence: JSON/PDF/HTML/404/illegal-format tests; `mvn -B -pl benchmark-engine -am clean compile`; `mvn -B -pl benchmark-engine -am test`; `mvn -B -pl benchmark-engine -am validate pmd:pmd checkstyle:check`; `node scripts/check-frontend-backend-separation.js`; `node scripts/lint-repository-knowledge.js`; `python3 scripts/task_audit.py --check`
  - Residual risk: export rendering, file storage, and real benchmark execution/report generation are still placeholder follow-up work.
  - Next step: keep later benchmark tasks on the stable report-query HTTP contract without reshaping the current DTO/VO surface.

### D-TASK-009: 实现压测任务提交流程骨架

- Status: done
- Priority: 1
- Depends on: D-TASK-008
- Completed at: 2026-04-21
- Commit subject: `feat(benchmark-engine): D-TASK-009 add task submit and status api skeleton`
- Scope: 在 `benchmark-engine` 中补 `POST /api/benchmark-engine/tasks` 与 `GET /api/benchmark-engine/tasks/{taskId}`，用 in-memory placeholder repository 串通提交、轮询、失败路径和流程日志，同时把成功路径的占位报告落库，为后续 `D-TASK-010` 的报告查询接口保留稳定承载点，不提前接入真实调度、隔离执行或导出链路。
- Validation:
  - 提交/轮询/失败路径测试
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-21: instantiated from `Phase-D / D-STORY-003` after `D-TASK-008` stabilized the benchmark task/report model and contract baseline.
  - 2026-04-21: added `BenchmarkTaskController`, `BenchmarkTaskApplicationService`, repository contract, and `InMemoryBenchmarkTaskRepository` so `benchmark-engine` now exposes submit and polling HTTP skeletons on an independent carrier.
  - 2026-04-21: kept async semantics by returning a queued snapshot from `POST /api/benchmark-engine/tasks`, then running a deterministic placeholder lifecycle that can reach success or failure for stable poll-path tests.
  - 2026-04-21: enforced current isolation guardrails in the skeleton by rejecting `readonlyRequired=false` and `shadowEnvironmentMode=DISABLED`, while still preserving placeholder failure coverage via the explicit `FAIL_BENCHMARK` marker.
  - 2026-04-21: stored placeholder reports on successful runs so `D-TASK-010` can add report query HTTP contracts without reshaping the current task flow, then synced interface baseline, capability map, truth baseline, C4, init summary, README, repo map, and validation log to `ASYNC_TASK_API_SKELETON`.
- Context closeout:
  - Completed scope: benchmark-engine task submit/poll skeleton, placeholder lifecycle, isolation guardrails, and success-path report persistence baseline were all established on an independent carrier.
  - Validation evidence: submit/poll/failure-path tests; `mvn -B clean compile`; `mvn -B test`; `mvn -B validate pmd:pmd checkstyle:check`; `node scripts/check-frontend-backend-separation.js`; `node scripts/lint-repository-knowledge.js`; `python3 scripts/task_audit.py --check`
  - Residual risk: real scheduler, isolated execution workers, persistent storage, and export/report backends are still deferred follow-up work.
  - Next step: continue benchmark-engine delivery on the stabilized task submit/status contract and placeholder report carrier.

### D-TASK-008: 定义压测任务与报告模型

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `feat(benchmark-engine): D-TASK-008 define benchmark task and report model`
- Scope: 建立 `benchmark-engine` 独立模块骨架，固化 `BASELINE` / `COMPARISON` / `REGRESSION_GUARD` 三类压测任务实体、状态与阶段流转、阈值评估、影子环境/只读/脱敏约束，以及后续提交/查询接口复用的基础 DTO / VO 和报告模型，不提前接入真实调度、执行、持久化或导出链路。
- Validation:
  - 模型与架构文档一致
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-003` to establish the third independent backend carrier instead of leaving benchmark semantics only in target architecture documents.
  - 2026-04-20: added `benchmark-engine` as a standalone Maven module with Spring Boot bootstrap, multi-profile configuration, logging baseline, and `application` / `domain` / `infrastructure` / `config` package skeleton.
  - 2026-04-20: solidified benchmark task types, lifecycle statuses, task-type-specific phase flow, threshold model, engine metric snapshot model, report aggregate, and explicit isolation fields for readonly, shadow environment, and desensitization.
  - 2026-04-20: added baseline submit/status/report DTO / VO objects plus `BenchmarkTaskModelApplicationService` so later `D-TASK-009` / `D-TASK-010` can reuse stable model contracts without reshaping the domain again.
  - 2026-04-20: synced `service-interface-contract-baseline.md`, `service-capability-map.md`, `c4-overview.md`, `document-truth-baseline.md`, `document-gap-matrix.md`, `init.md`, `repo-map.md`, and `README.md` so the repository now treats `benchmark-engine` as current fact while keeping task submit flow, execution chain, and report query deferred.

### D-TASK-007: 输出优化建议结构

- Status: done
- Priority: 1
- Depends on: D-TASK-006
- Completed at: 2026-04-20
- Commit subject: `feat(sql-optimization): D-TASK-007 add structured optimization suggestion vo`
- Scope: 在 `sql-optimization` 中把 `OptimizationTaskStatusResponse` 的占位 `summary/error` 升级为正式的 `suggestion / failure` 结构，固化收益、成本、风险、工件和失败阶段输出，并让 `PARSE` / `REWRITE` / `ACCELERATION_SUGGESTION` 三类任务一次性对齐到统一响应模型。
- Validation:
  - 输出字段与文档一致
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-002` after `D-TASK-006` stabilized the async submit/poll API skeleton.
  - 2026-04-20: replaced the flat `summary/error` polling payload with structured `suggestion / failure` objects plus dedicated benefit, cost, risk, artifact, and failure VOs under the SQL optimization application contract.
  - 2026-04-20: kept the task aggregate and repository unchanged, and concentrated the behavioral change in `OptimizationTaskModelApplicationService` so each task type now emits distinct placeholder recommendation content while preserving the existing lifecycle semantics.
  - 2026-04-20: extended tests to assert rewrite success payload shape, failed placeholder payload shape, parse success suggestion structure, and failure risk metadata, then synced the interface baseline, capability map, truth baseline, and init summary to the new response contract.
  - 2026-04-20: reran compile, test, static-check, separation, knowledge-lint, and pre-closeout task audit, then archived the task for single-task git closeout.

### D-TASK-006: 实现任务提交与状态查询骨架

- Status: done
- Priority: 1
- Depends on: D-TASK-005
- Completed at: 2026-04-20
- Commit subject: `feat(sql-optimization): D-TASK-006 add task submit and status api skeleton`
- Scope: 在 `sql-optimization` 中补 `POST /api/sql-optimization/tasks` 与 `GET /api/sql-optimization/tasks/{taskId}`，用 in-memory placeholder repository 串通提交、轮询、失败路径和流程日志，同时保持当前实现停留在过渡骨架，不提前接入 MySQL、队列和真实回调。
- Validation:
  - 提交/轮询/失败路径测试
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-002` after `D-TASK-005` established the SQL optimization task model and contract baseline.
  - 2026-04-20: added `OptimizationTaskController`, `OptimizationTaskApplicationService`, repository contract, and `InMemoryOptimizationTaskRepository` so `sql-optimization` now exposes submit and polling HTTP skeletons on an independent carrier.
  - 2026-04-20: kept async semantics by returning a queued snapshot from `POST /api/sql-optimization/tasks`, then running a deterministic placeholder lifecycle that can reach success or failure for stable poll-path tests.
  - 2026-04-20: added integration tests for submit success, failed placeholder polling, not-found polling, invalid callback rejection, plus log-sampling tests for entry/state-change/end/exception traces.
  - 2026-04-20: synced interface baseline, capability map, truth baseline, C4, init summary, README, repo map, validation log, and task ledgers to the new `ASYNC_TASK_API_SKELETON` current fact.

### D-TASK-005: 固化异步优化任务模型

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `feat(sql-optimization): D-TASK-005 solidify async optimization task model`
- Scope: 建立 `sql-optimization` 独立模块骨架，固化 `parse` / `rewrite` / `acceleration suggestion` 三类异步优化任务实体、生命周期状态、类型感知阶段流转，以及后续提交/轮询接口复用的基础 DTO/VO 和错误码基线，不提前接入真实 HTTP 入口、持久化、队列或审批链。
- Validation:
  - `R-121` 四项检查通过
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-002` to start the SQL optimization service on an independent carrier instead of leaking async optimization concerns back into `governance` or `query-execution`.
  - 2026-04-20: added `sql-optimization` as a standalone Maven module with Spring Boot bootstrap, multi-profile configuration, logging baseline, and `application` / `domain` / `infrastructure` / `config` package skeleton.
  - 2026-04-20: solidified async task types, lifecycle statuses, task-type-specific phase flow, submission normalization, status-history records, and reusable submit/status contract objects plus shared SQL-optimization service code and error-code ownership.
  - 2026-04-20: synced `service-interface-contract-baseline.md`, `service-capability-map.md`, `c4-overview.md`, `document-truth-baseline.md`, `document-gap-matrix.md`, `init.md`, `repo-map.md`, and `README.md` so the repository now treats `sql-optimization` as current fact while still deferring HTTP entrypoints, persistence, callbacks, and suggestion payload details to follow-up tasks.
  - 2026-04-20: reran compile, test, static-check, separation, knowledge-lint, and pre-closeout task audit, then archived the task for single-task git closeout.

### OPS-NAME-001: 全仓模块与工程命名去 Service 化整改

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `refactor(repo): OPS-NAME-001 remove Service suffix from engineering names`
- Scope: 以严格模式对全仓执行模块/工程/运行时命名整改，统一把 `governance-service`、`query-execution-service`、`sqlforge-common` 及相关运行标签、脚本、规则、需求、流程、验证日志、历史台账收敛到无 `Service` 后缀的工程命名，同时保留后端 `application` 包域、`controller`/`service` 实际分层与 `*Service` 服务类型命名，不丢失既有章节/步骤/流程/历史内容，并建立旧名到新名的权威映射。
- Validation:
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated as the temporary highest-priority strict-mode naming remediation task; all feature work paused until repo-wide module, runtime, code, script, and document naming were synchronized.
  - 2026-04-20: renamed module directories, Maven modules, artifact ids, Spring application names, runtime labels, service-code constants, scripts, tests, rules, plans, validation logs, and historical ledgers from `*-service` / `sqlforge-common` engineering names to `governance` / `query-execution` / `sqlforge-shared`.
  - 2026-04-20: preserved backend layering semantics by keeping `application` as a package domain, `controller` and `service` as the actual inbound layers, and concrete service-layer types on `*Service` naming.
  - 2026-04-20: repaired the remaining wording tail by correcting the old-to-new mapping table and removing the last misleading `application service` layer phrasing, then reran repo-wide validation for single-task closeout.

### D-TASK-004: 增加异常回滚与运行日志

- Status: done
- Priority: 1
- Depends on: D-TASK-003
- Completed at: 2026-04-20
- Commit subject: `feat(query-execution): D-TASK-004 add rollback markers and flow logs`
- Scope: 给 `query-execution` 的最小同步执行闭环补入口/出口/异常/状态变更日志，并把 timeout/fallback 路径的本地回滚/补偿标记固化到当前响应模型与代码路径。
- Validation:
  - `R-123` 抽查通过
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-001` after `D-TASK-003` committed the minimal synchronous execution loop in commit `ea25220`.
  - 2026-04-20: this task stayed inside the existing synchronous skeleton and only added observability plus local recovery markers; it did not open real database execution, distributed rollback, or governance audit integration.
  - 2026-04-20: extended `QueryExecutionApplicationService` with entry/exit/exception/state-change logs keyed by SQL fingerprint, kept SQL text out of logs, and added timeout/fallback local recovery markers through `retryPath.resultStatus/localRecoveryMarker/localRecoveryAction`.
  - 2026-04-20: added regression tests covering success logging, timeout rollback marker, fallback compensation marker, and exception logging, then synced architecture/truth/interface docs to the new observable execution baseline.
  - 2026-04-20: reran compile, test, static-check, separation, knowledge-lint, and pre-closeout task audit, then archived the task for single-task git closeout.

### D-TASK-003: 实现最小同步执行闭环

- Status: done
- Priority: 1
- Depends on: D-TASK-002
- Completed at: 2026-04-20
- Commit subject: `feat(query-execution): D-TASK-003 implement minimal sync execution loop`
- Scope: 在 `query-execution` 中把 `/api/query-execution/queries/execute` 接到最小同步执行路径，保持只读优先和确定性输出，不放开任意 SQL 执行。
- Validation:
  - 正常/超时/失败/降级路径测试
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-001` after `D-TASK-002` committed the public DTO / VO / error-code contract baseline in commit `0624dd0`.
  - 2026-04-20: implementation stayed inside the query-execution boundary and introduced a deterministic synchronous skeleton with read-only SQL guard, controlled route/fallback decisions, and no arbitrary SQL execution capability.
  - 2026-04-20: replaced the pure contract placeholder service with `QueryExecutionApplicationService`, added read-only assessment/guard logic, a deterministic execution adapter, and route handling for success, timeout, rejected-risk, and fallback-degraded paths.
  - 2026-04-20: synced service-interface, truth-baseline, architecture overview, capability map, and init docs so the repository now treats minimal synchronous execution as current fact while keeping governance calls, real adapters, rollback, and runtime logs deferred to follow-up work.
  - 2026-04-20: reran compile, test, static-check, separation, knowledge-lint, and pre-closeout task audit, then archived the task for single-task git closeout.

### D-TASK-002: 定义联机查询接口 DTO/VO/错误码

- Status: done
- Priority: 1
- Depends on: D-TASK-001
- Completed at: 2026-04-20
- Commit subject: `feat(query-execution): D-TASK-002 define online query api contracts`
- Scope: 在 `query-execution` 中固化联机查询的请求/响应 DTO、错误码区间和最小 HTTP 契约入口，保持契约优先，不提前引入真实执行引擎闭环。
- Validation:
  - `R-121` 四项检查通过
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-001` after `D-TASK-001` established the independent `query-execution` module baseline.
  - 2026-04-20: this task remained contract-first and limited to DTO/VO, error-code ownership, controller/service contract shape, and baseline interface documentation; the synchronous execution loop remains reserved for `D-TASK-003`.
  - 2026-04-20: added `QueryExecutionController`, request/response DTO/VO models, query execution status and policy enums, a contract service skeleton in the `application` package domain, shared query-execution error codes/service code, and contract tests covering valid request, validation failure, and pipeline-not-ready fallback behavior.
  - 2026-04-20: synced `service-interface-contract-baseline.md`, reran compile, test, static-check, separation, knowledge-lint, and pre-closeout task audit, then archived the task for single-task git closeout.

### D-TASK-001: 固化查询执行服务边界

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `feat(query-execution): D-TASK-001 solidify query execution service boundary`
- Scope: 建立 `query-execution` 独立模块骨架，固化路由、执行控制、轻量解析、轻量改写和已批准加速配置应用的服务边界，不把治理、异步优化或压测主流程重新混入本服务。
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
  - 2026-04-20: added `query-execution` as an independent Maven module with `application` package-domain plus `domain`/`infrastructure`/`config` skeleton, immutable boundary definition, multi-profile configuration, and the boundary service class `QueryExecutionBoundaryApplicationService` plus unit test.
  - 2026-04-20: synced `service-capability-map.md`, `c4-overview.md`, `document-truth-baseline.md`, `document-gap-matrix.md`, `master-execution-plan.md`, `frontend-backend-separation-baseline.md`, `repo-map.md`, `README.md`, and `docs/README.md` so the new service carrier is treated as current fact instead of a pure target.
  - 2026-04-20: validation passed with compile, test, static-check, frontend-backend separation check, knowledge lint, and pre-closeout task audit, then the task was archived for single-task git closeout.

### C-TASK-008: 落实租户配置与访问占位能力

- Status: done
- Priority: 2
- Depends on: C-TASK-007
- Completed at: 2026-04-20
- Commit subject: `feat(governance): C-TASK-008 tighten tenant access placeholder policy`
- Scope: 保持 phase 0 最小租户校验闭环，继续把 `governance` 收敛为公共管理服务基线，不扩散到其他目标微服务职责。
- Validation:
  - `mvn clean compile`
  - `mvn test`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` after governance baseline hardening.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; `TenantAccessLogic`、`TenantConfigApplicationService` 和治理内部租户/数据源检查接口已经存在并通过当前测试链验证，但数据源授权仍是 placeholder，完整角色与资源矩阵仍待后续实现，见 `IMP-005`。
  - 2026-04-20: replaced the old non-empty datasource placeholder with a governance-local explicit placeholder policy under `governance.access-control.placeholder`, added tenant-config role gating and platform-admin override in `TenantConfigApplicationService`, and kept all logic inside `governance`.
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
  - 2026-04-20: hardened `datasource-access/check`、`audit/write`、`schedule/extensions` contracts in `governance`, added explicit contract-stage / implementation-stage metadata, fixed audit required fields (`serviceCode`,`elapsedMs`,`sourceIp`,`userAgent`), and synced `service-interface-contract-baseline.md` plus `access-control-spec.md`.
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
- Scope: 对照 `sqlforge-shared`、`governance` 与文档边界，产出当前应收敛到 common 的能力清单，明确哪些能力仍留在业务模块。
- Validation:
  - `mvn clean compile`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` as the first executable shared-foundation task.
  - 2026-04-20: `docs/plans/implementation-readiness.md` 与 `docs/architecture/service-capability-map.md` 已把 `sqlforge-shared` 应承载的公共能力和禁入边界显式盘点完成，并写入主计划和真值文档。
  - 2026-04-20: strict ledger reconciliation revalidated repository compile and knowledge lint, then archived the inventory task because its implementation and git-history evidence are both present.

### C-TASK-002: 建立 common 包结构

- Status: done
- Priority: 1
- Depends on: C-TASK-001
- Completed at: 2026-04-20
- Commit subject: `refactor(common): C-TASK-002 close shared package structure baseline`
- Scope: 按“领域目录 + 分层子目录”与公共层边界建立 `sqlforge-shared` 的目标包结构，不引入服务专属逻辑。
- Validation:
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` after shared-capability inventory.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; `sqlforge-shared` 已形成 `async`、`audit`、`config`、`constants`、`context`、`exception`、`log`、`utils` 包结构，且当前验证链 `mvn clean compile`、`mvn test`、`mvn validate pmd:pmd checkstyle:check`、`node scripts/lint-repository-knowledge.js` 已通过，但单任务 git closeout 仍缺失，暂不归档。
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
  - 2026-04-20: strict ledger reconciliation marked the task as partial; 旧 `com.company.common` 与治理服务内部重复 common 能力已被当前工作树迁移到 `com.company.sqlforge.common` 并由 `governance` 消费，当前验证链 `mvn clean compile`、`mvn test`、`mvn validate pmd:pmd checkstyle:check`、`node scripts/lint-repository-knowledge.js` 已通过，但单任务 git closeout 仍缺失，暂不归档。
  - 2026-04-20: removed the last tracked `com.company.common` sources from `sqlforge-shared` and updated `document-gap-matrix.md` so the shared-layer implementation no longer remains as an open gap.

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
  - 2026-04-20: `MessageAdminController`、`MessageAdminApplicationTest`、`scripts/manual-message-queue-smoke.sh`、`docs/deliveries/init-completion.md` 与 `docs/deployments/local-setup.md` 已形成管理接口、消息表和人工 smoke 的验证闭环。
  - 2026-04-20: strict ledger reconciliation revalidated the current test chain and knowledge lint, confirmed that runtime verification evidence is already documented, then archived the task.

### C-TASK-007: 对齐现有分层

- Status: done
- Priority: 2
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `refactor(governance): C-TASK-007 close layered governance baseline`
- Scope: 对齐 `governance` 当前 `application` 包域下的 `controller`/`service` 与 `domain`/`infrastructure` 分层，确保其继续向公共管理服务边界收敛。
- Validation:
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` as the first governance hardening task.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; 当前工作树已形成 `application` 包域下的 `controller` / `service` 与 `domain` / `infrastructure` 分层、严格请求上下文校验与治理内部契约基线，并通过 `mvn clean compile`、`mvn test`、`mvn validate pmd:pmd checkstyle:check`、`node scripts/lint-repository-knowledge.js` 验证，但当前分层调整尚未完成单任务 git closeout，暂不归档。
  - 2026-04-20: removed the last `governance.common` remnants, added regression coverage for `HealthStatusApplicationService` and `MessageRetryResultVO`, and synced local delivery/setup notes with the protected governance admin paths and current Phase-C baseline summary.

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
  - 2026-04-20: cleanup classification isolated the remaining CI/quality files from Phase-C feature work because they add delivery gate entrypoints rather than shared-foundation or governance functionality.
  - 2026-04-20: added `scripts/run-sonar.sh`, wired coverage/Sonar helpers into `.github/workflows/ci.yml`, `Makefile`, and `docs/operations/local-development.md`, and kept the Sonar path no-op safe when environment variables are not configured.
  - 2026-04-20: reran the Sonar helper in no-config mode, reran coverage report generation, and prepared the quality-gate batch for single-task git closeout.

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
