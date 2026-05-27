# SQLForge Completed Tasks

本文件只记录已完成、已验证、已归档的任务。

## Done

### USER-CN-REPLACE-JSQLPARSER-CALCITE-20260527: Replace legacy SQL parser with Apache Calcite

- Status: done
- Completed at: 2026-05-28
- Commit subject: `Replace legacy SQL parser with Apache Calcite`
- Priority: 1
- Depends on: N/A
- Scope: Replace all legacy parser dependencies, source code paths, parser modes, rewrite/analysis helpers, frontend/config/test/documentation references, and build artifacts with Apache Calcite equivalents; preserve or improve SQL parsing and processing capability, document any Calcite capability gaps in MIGRATION_REPORT.md, and verify no legacy parser traces remain in the requested file types.
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REPLACE-JSQLPARSER-CALCITE-20260527`
- Progress log:
  - 2026-05-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Replaced parser modes, dependencies, backend Calcite collectors and rewrite paths, frontend parser-mode contracts, docs/scripts/schema references, portable assets, and migration report.
  - Validation evidence: mvn clean test; npm run lint; npm run build; npm run build:portable; parser-token scans; dependency tree check; foreman validate; pre-closeout audit.
  - Residual risk: Calcite source-format preservation and production RelNode SQL generation remain documented gaps in MIGRATION_REPORT.md; production execution remains gated.
  - Next step: Use Calcite-only parser modes in future parser and rewrite tasks; add governed proof or runtime validation as separate tasks.

### USER-CN-DYNAMIC-MV-TEST01-EXPECTED-20260527: docs/test01_mv.sql 期望驱动的动态 MV 推荐与改写提升

- Status: done
- Completed at: 2026-05-27
- Commit subject: `USER-CN-DYNAMIC-MV-TEST01-EXPECTED-20260527 generate dynamic test01 MV rewrite`
- Priority: 1
- Depends on: N/A
- Scope: 以 docs/test01.sql 作为输入，以 docs/test01_mv.sql 作为动态解析生成的推荐 SQL/MV 期望，修复任何静态处理或页面/后端硬编码，确保 test01 与既有复杂 SQL 回归均通过，并补充验证证据。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-DYNAMIC-MV-TEST01-EXPECTED-20260527`
- Progress log:
  - 2026-05-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 动态解析 docs/test01.sql 并生成与 docs/test01_mv.sql 可执行 SQL 等价的推荐 SQL；MV artifact 改为基于动态快照聚合候选生成；改写试跑、推荐规则链和页面形态检查改为后端动态证据驱动；新增 test01_mv fixture 覆盖登记与回归测试。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-DYNAMIC-MV-TEST01-EXPECTED-20260527；mvn -pl sql-optimization -am test；mvn -pl sql-optimization -am -Dtest=SqlOptimizationPipelineServiceTest,L2SnapshotAggregateReportMvCandidateGeneratorTest,RewriteTrialApplicationServiceTest,ProductionRewriteClosedLoopEndToEndTest,L2MaterializedViewLargeSqlQualityTest -Dsurefire.failIfNoSpecifiedTests=false test；npm run lint；npm run build；python3 scripts/task_audit.py --check --phase pre-closeout；git diff --check。
  - Residual risk: 动态快照聚合推荐仍标记为人工复核/非自动应用，生产激活前需要执行结果集、机构标签、逐键指标、汇总指标和计划形态验证 SQL。
  - Next step: 如要投产，接入真实 Hetu planner/数据环境执行 validationSql 并保留外部证据。

### USER-CN-DIST-PORTABLE-UPDATE-20260527: 全量更新 dist-portable 便携前端产物

- Status: done
- Completed at: 2026-05-27
- Commit subject: `build(portable): refresh dist-portable package`
- Priority: 1
- Depends on: N/A
- Scope: 使用现有 portable 前端构建入口按当前仓库源代码全量重新生成受跟踪的 dist-portable 包，清理过期 hashed assets，并通过 portable 构建、浏览器 smoke、任务审计与差异检查验证；不修改业务源码、接口语义或页面功能。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-DIST-PORTABLE-UPDATE-20260527`
- Progress log:
  - 2026-05-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Cleared stale dist-portable hashed assets and regenerated the tracked portable frontend package from current repository source without changing business behavior.
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-DIST-PORTABLE-UPDATE-20260527 --include-task-audit --extra-command 'npm run build:portable' --extra-command 'npm run smoke:portable-frontend' --extra-command 'git diff --check'; npm run build:portable; npm run smoke:portable-frontend; git diff --check; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: No known repository-side residual risk; runtime backend availability still depends on target host portable-config.json service URLs.
  - Next step: Use dist-portable/start-portable.sh or start-portable.cmd with configured backend services when distributing the refreshed portable package.

### USER-CN-BEIJING-TIME-24H-20260526: 统一北京时间 24 小时制显示与管理

- Status: done
- Completed at: 2026-05-27
- Commit subject: `feat(time): unify Beijing time display`
- Priority: 1
- Depends on: N/A
- Scope: 修改本项目所有时间管理与展示口径，统一按北京时间 Asia/Shanghai 24 小时制处理，覆盖前端展示、后端 DTO/服务/日志可读时间、脚本输出和数据库 schema/default/seed/migration 中的时间字段说明与默认值；补充或更新验证，确保 UTC/本地时区不会泄露到用户可读显示。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-BEIJING-TIME-24H-20260526`
- Progress log:
  - 2026-05-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 统一前端展示、后端 DateUtils/Jackson/logback/JDBC 持久化转换、脚本输出标识、MySQL compose/init/migration 与持久化文档的北京时间 Asia/Shanghai 24 小时制口径，并补充北京时间合同检查；foreman/task audit 时间戳也改为北京时间。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-BEIJING-TIME-24H-20260526 --include-task-audit；npm run test:beijing-time；mvn -pl sqlforge-shared,governance,query-execution,sql-optimization,benchmark-engine -am test；python3 -m py_compile changed Python scripts；bash -n changed shell scripts；git diff --check。
  - Residual risk: 无已知遗留风险；领域模型中表达绝对事件点的 Instant 保留，写入 DATETIME 和用户可读展示前按北京时间转换。
  - Next step: N/A

### USER-CN-DYNAMIC-MV-REWRITE-QUALITY-20260526: 动态 MV 推荐与复杂 SQL 改写能力提升

- Status: done
- Completed at: 2026-05-26
- Commit subject: `USER-CN-DYNAMIC-MV-REWRITE-QUALITY-20260526 improve dynamic MV rewrite quality`
- Priority: 1
- Depends on: N/A
- Scope: 废弃 test01 样例模板式 L2 MV 推荐路径，改为 AST/IR/QBDAG/关系代数驱动的 MV 候选、覆盖证明、外层查询保留、Explain/元数据证据与结构化阻断，并补充复杂 SQL 回归验证。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-DYNAMIC-MV-REWRITE-QUALITY-20260526`
- Progress log:
  - 2026-05-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 废弃活跃的 test01 快照模板式 L2 MV 推荐路径，改为 AST/IR/QBDAG/关系代数驱动的 MV 候选规划；补齐外层查询保留、覆盖证明、Explain/元数据证据、结构化阻断和 100 条复杂 SQL 回归语料。
  - Validation evidence: java -version = OpenJDK 1.8.0_112；mvn -pl sql-optimization -am -Dtest=SqlOptimizationPipelineServiceTest,L2MaterializedViewLargeSqlQualityTest,RewriteTrialApplicationServiceTest,ProductionRewriteClosedLoopEndToEndTest -Dsurefire.failIfNoSpecifiedTests=false test；mvn -pl sql-optimization checkstyle:check -Dcheckstyle.includes=**/application/service/L2AccelerationArtifactBuilder.java,**/application/service/L2MaterializedViewRewriteCoverageValidator.java,**/application/service/MaterializedViewRecommendationPlanner.java,**/application/service/QueryWrapperPreserver.java,**/application/service/MvCoverageProofEngine.java,**/application/service/SqlOptimizationPipelineService.java,**/application/service/RewriteTrialApplicationService.java,**/domain/rewrite/recommendation/RewriteRecommendationGenerator.java；mvn -pl sql-optimization -am test；git diff --check；python3 scripts/foreman.py validate USER-CN-DYNAMIC-MV-REWRITE-QUALITY-20260526；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 仓库侧已证明静态解析、候选生成、覆盖证明与复杂 SQL 回归；真实 Hetu/MRS MV DDL 权限、刷新成本、生产统计元数据、EXPLAIN 计划和结果等价仍需外部环境证据。
  - Next step: 在外部 Hetu/MRS 环境按生产治理流程执行真实 MV DDL/EXPLAIN/refresh/result-equivalence 验证后再进入生产投放。

### USER-CN-SQL-QUERY-TENANT-SYNC-20260526: SQL 查询页租户与 AUTO 数据源同步修复

- Status: done
- Completed at: 2026-05-26
- Commit subject: `test(frontend): cover SQL query tenant AUTO datasource sync`
- Priority: 1
- Depends on: N/A
- Scope: 修复 SQL 查询分析页使用当前 tenantStore 租户加载 datasource，并在 AUTO 引擎切换时恢复可用 datasource；补充前端 smoke 覆盖。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-SQL-QUERY-TENANT-SYNC-20260526`
- Progress log:
  - 2026-05-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: SQL 查询页按 tenantStore 当前租户加载 datasource，并补充前端 dev smoke 覆盖右上角租户切换与 AUTO/HETU/AUTO datasource 恢复。
  - Validation evidence: npm run lint; npm run build; npm run smoke:frontend-dev; python3 scripts/foreman.py validate USER-CN-SQL-QUERY-TENANT-SYNC-20260526; python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 未连接真实后端环境复测；本轮覆盖 Vite dev mock 浏览器链路。
  - Next step: N/A

### USER-CN-QUERY-VIEW-IMPROVEMENT-20260526: SQL查询分析页面体验优化与DBeaver三态状态反馈

- Status: done
- Completed at: 2026-05-26
- Commit subject: `style: collapse parameter panel by default and add DBeaver-style status console`
- Priority: 1
- Depends on: N/A
- Scope: 折叠紧凑表格式参数输入，且在SQL执行后不管有无数据均展示SQL三态及DBeaver式执行控制台与状态反馈
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-QUERY-VIEW-IMPROVEMENT-20260526`
- Progress log:
  - 2026-05-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 折叠紧凑表格式参数输入，且在SQL执行后不管有无数据均展示SQL三态及DBeaver式执行控制台与状态反馈
  - Validation evidence: npm run lint, npm run build, and foreman task validation checks passed successfully
  - Residual risk: None. All changes are thoroughly tested and verified without backend modifications
  - Next step: None. The task is fully completed

### USER-CN-YML-CONFIG-DESCRIPTIONS-20260526: 补齐 yml 配置项中文具体说明

- Status: done
- Completed at: 2026-05-26
- Commit subject: `USER-CN-YML-CONFIG-DESCRIPTIONS-20260526 detail YAML config comments in Chinese`
- Priority: 1
- Depends on: N/A
- Scope: 按用户要求修复所有 yml/yaml 配置项中文说明，移除模板句，写出具体用途、默认值和运行影响；同时修正自动补注释脚本，避免后续重新写入模板。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-YML-CONFIG-DESCRIPTIONS-20260526`
- Progress log:
  - 2026-05-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 已将 27 个 yml/yaml 配置文件中的模板式配置说明替换为包含用途、默认值和运行影响的中文说明，覆盖 GitHub Actions、compose、四个后端服务主/环境/test 配置；修正 check-config-item-comments.py，使后续 YAML 自动补注释不再生成模板句，并为其它配置类型提供非模板兜底；补齐检查器发现的两个 mapper XML 注释缺口。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-YML-CONFIG-DESCRIPTIONS-20260526 --include-task-audit --extra-command 'python3 scripts/check-config-item-comments.py' --extra-command 'python3 -m py_compile scripts/check-config-item-comments.py' --extra-command 'node scripts/lint-repository-knowledge.js' --extra-command 'docker-compose config' --extra-command 'docker-compose -f docker-compose-cn.yml config' --extra-command 'docker-compose -f docker-compose-simple.yml config' --extra-command 'git diff --check' passed；YAML semantic equality checked: 27 files；python3 scripts/task_audit.py --check --phase pre-closeout passed。
  - Residual risk: 本任务按 yml/yaml 边界具体化配置说明；XML/POM 等非 YAML 文件中的既有模板注释未做全量重写，除两个会阻断检查器的 mapper XML 缺口外不扩大范围。配置解析结果与 HEAD 一致，未改变运行配置值。
  - Next step: 若后续要求 XML、POM、TOML、JS 等非 YAML 配置的既有模板句也全部具体化，应按新的配置说明任务拆分执行并复用本轮脚本兜底能力。

### USER-CN-DIST-PORTABLE-UPDATE-20260526: 全量更新 dist-portable 便携前端产物

- Status: done
- Completed at: 2026-05-26
- Commit subject: `build(portable): refresh dist-portable package`
- Priority: 1
- Depends on: N/A
- Scope: Rebuild the portable frontend package from current source, remove stale hashed dist-portable assets, run portable build and browser smoke validation, then close out through the standard audit chain.
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-DIST-PORTABLE-UPDATE-20260526`
- Progress log:
  - 2026-05-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Cleared stale dist-portable hashed assets and regenerated the portable frontend package from current source; the rebuilt tracked dist-portable files are byte-identical to the current repository package.
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-DIST-PORTABLE-UPDATE-20260526 --include-task-audit --extra-command 'npm run build:portable' --extra-command 'npm run smoke:portable-frontend' --extra-command 'git diff --check'; npm run build:portable; npm run smoke:portable-frontend; git diff --check
  - Residual risk: No known repository-side residual risk; runtime backend availability still depends on the target host portable-config.json service URLs.
  - Next step: Use dist-portable/start-portable.sh or start-portable.cmd with configured backend services when distributing the verified portable package.

### USER-CN-FRONTEND-SAMPLE-DEFAULTS-PROFILE-GATE-20260526: Frontend sample defaults profile gate

- Status: done
- Completed at: 2026-05-26
- Commit subject: `fix(frontend): gate sample defaults and placeholders`
- Priority: 1
- Depends on: N/A
- Scope: Move frontend sample tenant/datasource defaults into runtime profile helpers, keep missing backend-contract writes blocked by shared placeholders, extend governance checks and docs for TEMP-AUDIT-006.
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-FRONTEND-SAMPLE-DEFAULTS-PROFILE-GATE-20260526`
- Progress log:
  - 2026-05-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Centralized frontend sample tenant/datasource defaults behind runtime profile helpers, updated affected pages to use helper/default datasource candidates, and routed missing-write-contract actions through a shared placeholder registry.
  - Validation evidence: npm run lint; npm run build; npm run test:form-governance; npm run test:sql-ui-contract; npm run test:frontend-page-governance; npm run smoke:frontend-dev; npm run build:portable; npm run smoke:portable-frontend; git diff --check; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/foreman.py validate USER-CN-FRONTEND-SAMPLE-DEFAULTS-PROFILE-GATE-20260526 --include-task-audit.
  - Residual risk: Sample values remain only in the central profile config, dev/portable mock servers, smoke/contract fixtures, shell runbooks, and documentation references; no new backend write APIs were added for placeholder-only actions.
  - Next step: Implement backend write contracts separately before converting Access strategy edit/create or Dispatch policy edit placeholders into real submit flows.

### USER-CN-SQL-OPT-REPORT-BATCH-PERSISTENCE-20260526: sql-optimization 报表批次持久化模型扩展

- Status: done
- Completed at: 2026-05-26
- Commit subject: `feat(sql-optimization): persist report batch statistics`
- Priority: 1
- Depends on: N/A
- Scope: 修复报表批次仓储选择默认链路，新增报表批次统计规范化持久化模型，并保持现有 REST 契约兼容
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-SQL-OPT-REPORT-BATCH-PERSISTENCE-20260526`
- Progress log:
  - 2026-05-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 报表批次仓储默认链路切换到显式 report-batch.repository 配置，新增统计快照规范化表、Mapper 与仓储，并为列表、详情和统计读取提供快照优先、明细回退的兼容路径。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-SQL-OPT-REPORT-BATCH-PERSISTENCE-20260526 --include-task-audit；Maven 目标测试 ReportBatchApplicationServiceTest、ParseBatchPersistenceSchemaMappingTest、ReportBatchRepositorySelectionTest；batch/history contract checks。
  - Residual risk: 未连接真实 MySQL 执行迁移；仓库内以 schema/mapper 对齐、仓储选择和应用服务回归测试覆盖。
  - Next step: 无。

### USER-CN-SQL-TOKEN-NORMALIZER-20260525: 实现轻量 SQL token 归一化

- Status: done
- Completed at: 2026-05-25
- Commit subject: `fix(sql): normalize Yonghong derived join wrappers`
- Priority: 1
- Depends on: N/A
- Scope: 把 Yonghong/Hetu 派生表 join 包装归一化从按行判断改为轻量 token/括号栈重写；后端在 legacy parser/Calcite 前统一方言归一化；前端 rewrite validation 保留自动格式化前的提交原文；补 docs/test01.sql raw、页面格式化、去注释格式化三类回归测试。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-SQL-TOKEN-NORMALIZER-20260525`
- Progress log:
  - 2026-05-25: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 后端 SQL 方言归一化抽为 token/括号栈 normalizer，统一处理 Yonghong/Hetu 派生表 JOIN 双层括号、注释、尾部分号和 BI 视图库名前缀；rewrite validation 页面在格式化后保留提交原文；补充 docs/test01.sql raw、页面格式化、去注释格式化回归。
  - Validation evidence: docs/quality/validation-log.md 中 2026-05-25T23:32:34-05:00 至 23:33:09-05:00 的 foreman validate 记录全部 passed；定向回归 mvn -pl sql-optimization -am -Dtest=SqlDialectNormalizerTest,SqlOptimizationPipelineServiceTest -Dsurefire.failIfNoSpecifiedTests=false test 通过。
  - Residual risk: 未接入真实外部 Hetu/MRS planner 或生产数据执行，仅验证仓库内静态解析、改写推荐与前端合同。
  - Next step: 继续保留外部生产规模验证任务的独立证据链。

### USER-CN-SQL-HIGHLIGHT-PERFORMANCE-20260526: 优化超大 SQL 语法高亮渲染性能

- Status: done
- Completed at: 2026-05-25
- Commit subject: `feat(frontend): optimize large SQL highlighting performance`
- Priority: 1
- Depends on: N/A
- Scope: 支持超过 10000 行、20w 字符的超大 SQL 在页面上高速高亮渲染，优化正则与字符拼接性能，修复 test01.sql 在内的超长 SQL 输入无高亮或卡顿问题
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-SQL-HIGHLIGHT-PERFORMANCE-20260526`
- Progress log:
  - 2026-05-25: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Optimize escapeHtml and highlightSql in sqlFormatting.mjs to support 200k+ character SQL highlighting under 5ms, avoiding memory allocation and rendering lag.
  - Validation evidence: Passed test:sql-ui-contract, test:frontend-page-governance, test:form-governance, and custom scratch benchmark highlighting test01.sql in 1.9ms.
  - Residual risk: None
  - Next step: None

### USER-CN-DYNAMIC-MV-RUNTIME-AUDIT-20260525: 动态 MV 模板运行时完成度审计

- Status: done
- Completed at: 2026-05-25
- Commit subject: `chore: audit dynamic MV runtime rewrite completion`
- Priority: 1
- Depends on: USER-CN-DYNAMIC-MV-REWRITE-TEST01-20260525
- Scope: Audit current implementation against the active objective: verify docs/test01.sql dynamic/template MV recommendation, materialized-view creation artifact, runtime SQL Query Analysis rewrite execution, and template hits when parameter values or portable WHERE predicates change; implement narrowly scoped fixes only if evidence is missing.
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-DYNAMIC-MV-RUNTIME-AUDIT-20260525`
- Progress log:
  - 2026-05-25: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Audited the active objective against current code and tests: docs/test01.sql dynamic/template MV recommendation, MV DDL/rewrite artifact generation, runtime rewrite activation, SQL Query Analysis execution path, and template hits for parameter changes plus portable added WHERE predicates are covered by existing implementation; no business code changes were required.
  - Validation evidence: java -version = OpenJDK 1.8.0_112; mvn -pl sqlforge-shared,query-execution,sql-optimization -am -Dtest=RuntimeSqlRewriteTemplateEngineTest,QueryExecutionRuntimeRewriteBindingServiceTest,QueryExecutionApplicationServiceTest,RewriteTrialApplicationServiceTest,L2SnapshotAggregateReportMvCandidateGeneratorTest,ProductionRewriteClosedLoopEndToEndTest test -Dsurefire.failIfNoSpecifiedTests=false; python3 scripts/foreman.py validate USER-CN-DYNAMIC-MV-RUNTIME-AUDIT-20260525; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check
  - Residual risk: Repo-closed evidence proves template recommendation and runtime rewrite behavior; live Hetu/MRS MV DDL permissions, refresh cost, and production data equivalence remain environment-backed validation items before production rollout.
  - Next step: Use the existing environment-backed Hetu/MRS smoke window to capture live MV creation and execution evidence when credentials and target cluster access are available.

### USER-CN-DYNAMIC-MV-REWRITE-TEST01-20260525: 实现 test01 动态 MV 解析与改写

- Status: done
- Completed at: 2026-05-25
- Commit subject: `Implement dynamic MV rewrite for test01`
- Priority: 1
- Depends on: N/A
- Scope: 实现动态/模板解析改写能力覆盖 docs/test01.sql：深层派生表、UNION ALL、多段 LEFT JOIN、中文双引号别名、COUNT DISTINCT、AUTO 引擎解析、rewrite trial 推荐 MV 产物落库与真实 MV 创建；静态 test01 规则仅作为临时核验 oracle，完成后不得作为生产解析改写入口。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-DYNAMIC-MV-REWRITE-TEST01-20260525`
- Progress log:
  - 2026-05-25: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 动态/模板化 MV 解析改写覆盖 docs/test01.sql；补齐深层派生表、UNION ALL、多段 LEFT JOIN、中文双引号别名、COUNT DISTINCT 精确重聚合、AUTO 引擎解析、rewrite trial 推荐产物落库和推荐类型创建 MV。
  - Validation evidence: mvn -pl sql-optimization -am test；git diff --check；python3 scripts/foreman.py validate USER-CN-DYNAMIC-MV-REWRITE-TEST01-20260525；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 生成的 MV/改写仍标记为 PULL_ONLY_NOT_EXECUTED_BY_SQLFORGE，激活前需要 validationSql 结果差异校验和人工复核。
  - Next step: 如需上线自动应用，接入真实执行校验与生产级成本证据。

### USER-CN-AUDIT-TEMP-INMEMORY-STATIC-20260525: 审计项目中的内存实现、静态常量结果与临时代替功能

- Status: done
- Completed at: 2026-05-25
- Commit subject: `Audit temp in-memory and static substitutes`
- Priority: 1
- Depends on: N/A
- Scope: 全仓库只读审计：找出 inmemory、静态常量/静态结果、mock/simulated/synthetic/fallback/placeholder 等临时替代实现或内容；说明为什么存在、风险与推荐后续完善方向；不做业务代码修改。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-AUDIT-TEMP-INMEMORY-STATIC-20260525`
- Progress log:
  - 2026-05-25: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Completed a repository-wide read-only audit of in-memory repositories, static outputs, simulated/synthetic/mock/fallback/placeholder paths, and documented risk-ranked follow-ups.
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-AUDIT-TEMP-INMEMORY-STATIC-20260525; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check.
  - Residual risk: This task is documentation-only; business-code hardening remains in the recommended task pack and existing unrelated implementation changes were left untouched.
  - Next step: Materialize follow-up hardening tasks for report batch persistence, fallback gating, runtime evidence separation, cache/repository profile gates, and real rewrite evidence upgrades.

### USER-CN-FIX-SQL-QUERY-RESULT-EXPLAIN-20260525: 修正 SQL 查询分析页执行结果展示

- Status: done
- Completed at: 2026-05-25
- Commit subject: `fix(query-execution): USER-CN-FIX-SQL-QUERY-RESULT-EXPLAIN-20260525 keep rows business-only`
- Priority: 1
- Depends on: N/A
- Scope: Implement the confirmed plan for /api/query-execution/queries/execute and SQL 查询分析: rows must contain only engine-returned business columns, diagnostic execution fields remain in metadata/summary fields, and SqlQueryView/queryResultPage rendering must show ordinary SELECT/WITH/SHOW/DESCRIBE results as data lists while user-written EXPLAIN SQL is labeled and rendered as an execution plan without changing the Explain button into auto execution.
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-FIX-SQL-QUERY-RESULT-EXPLAIN-20260525`
- Progress log:
  - 2026-05-25: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-25: removed adapter/service diagnostic row decoration so simulated paths return empty `rows`, JDBC reads only `ResultSetMetaData` columns, and REST/CLIENT preserve only returned row fields.
  - 2026-05-25: added SQL 查询分析 result-kind handling so `EXPLAIN` responses render as execution plans while ordinary query responses keep the paged result table.
  - 2026-05-25: validation passed via `python3 scripts/foreman.py validate USER-CN-FIX-SQL-QUERY-RESULT-EXPLAIN-20260525 --extra-command "mvn -pl query-execution -am test" --extra-command "node scripts/check-query-workbench-contract.mjs"`.
- Context closeout:
  - Completed scope: Backend query execution rows now contain only engine-returned business columns; simulated and direct-success paths return empty rows without diagnostic records; SQL 查询分析 detects user-written EXPLAIN and renders single-column plan text or multi-column plan tables while preserving normal paged data rows.
  - Validation evidence: Passed: mvn -pl query-execution -am test; node scripts/check-query-workbench-contract.mjs; npm run test:sql-ui-contract; npm run lint; npm run build; npm run test:form-governance; npm run test:frontend-page-governance; node scripts/check-developer-copy-language.mjs --changed; python3 scripts/foreman.py validate USER-CN-FIX-SQL-QUERY-RESULT-EXPLAIN-20260525 with extra Maven and query-workbench contract commands.
  - Residual risk: No known repo-closed residual risk; live Hetu/MRS result shape validation remains covered by existing environment-backed HARN-016.
  - Next step: Use external Hetu/MRS smoke evidence when the blocked environment window opens; no additional repository follow-up is required for this task.

### USER-CN-TOP-TENANT-DEFAULT-LOW-RISK-DYNAMIC-20260525: 顶部租户默认与低风险动态化计划

- Status: done
- Completed at: 2026-05-25
- Commit subject: `feat(frontend): USER-CN-TOP-TENANT-DEFAULT-LOW-RISK-DYNAMIC-20260525 dynamic tenant defaults`
- Priority: 1
- Depends on: N/A
- Scope: 继续收口顶部租户默认值与低风险动态化计划：让前端默认租户、租户可见数据源候选、查询/推荐/历史/系统页默认请求上下文从硬编码样例转向租户配置与低风险回退；同步治理默认授权种子、前端静态契约检查和相关单元测试；不改变租户隔离、runtime binding ACTIVE、安全边界或外部环境依赖。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-TOP-TENANT-DEFAULT-LOW-RISK-DYNAMIC-20260525`
- Progress log:
  - 2026-05-25: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 顶部工作区默认租户收口到 system，并保留 system 默认显示名兜底；前端新增统一 tenant defaults 与治理数据源候选归一化 helper，查询、解析、改写验证、推荐中心、SQL 历史、解析历史、Dashboard 与 System 页面改为优先消费当前租户/后端候选并保留低风险样例回退；治理默认 tenant-config 种子补齐 system/tenant-a/tenant-b 与 tenant-b 配置访问；dev/portable smoke mocks 与 portable 产物同步更新。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-TOP-TENANT-DEFAULT-LOW-RISK-DYNAMIC-20260525 --include-task-audit --extra-command 'mvn -pl governance -am -Dtest=TenantConfigApplicationServiceTest,TenantAccessLogicImplTest test -Dsurefire.failIfNoSpecifiedTests=false' --extra-command 'mvn -B -pl governance checkstyle:check -Dcheckstyle.includes=**/GovernanceAccessProperties.java,**/TenantConfigApplicationServiceTest.java,**/TenantAccessLogicImplTest.java' --extra-command 'npm run smoke:frontend-dev' --extra-command 'npm run build:portable' --extra-command 'npm run smoke:portable-frontend' --extra-command 'git diff --check' passed.
  - Residual risk: 真实运行环境仍依赖已登记的治理 datasource_config；当某租户没有可见数据源时，查询页会显示无可用数据源并阻止执行，而不是伪造 hetu_main。portable smoke 覆盖 mock-backed 前端路径，不替代 live backend 验证。
  - Next step: 在目标环境通过系统管理页为业务租户维护 datasource_config 后，刷新顶部租户选择并确认查询页自动采用该租户的首个可见数据源。

### USER-CN-OPTIMIZE-SQL-COMPONENTS-20260524: 优化本项目所有的页面上展示sql的组件（光标偏移与大SQL性能）

- Status: done
- Completed at: 2026-05-24
- Commit subject: `feat: optimize SQL components and resolve cursor offset`
- Priority: 1
- Depends on: N/A
- Scope: 优化 SqlEditorField, SqlCodeBlock 和 SqlCompareBlock，解决光标偏移与大 SQL 性能卡顿问题，禁止改动后端代码
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-OPTIMIZE-SQL-COMPONENTS-20260524`
- Progress log:
  - 2026-05-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 重写 highlightSql 算法解决大 SQL 性能，修改 SqlEditorField 样式对齐解决光标偏移，零改动后端代码
  - Validation evidence: 已通过 npm run test:sql-ui-contract 与 test:frontend-page-governance 校验，且通过 foreman validate 审计
  - Residual risk: 无残留风险，所有外部契约 100% 满足，大 SQL 自动在 30000 字符限制内安全逃逸
  - Next step: 无，直接交付并在前端页面上享受流畅的 SQL 编辑体验

### USER-CN-DIST-PORTABLE-UPDATE-20260524: 全量更新 dist-portable 便携前端产物

- Status: done
- Completed at: 2026-05-24
- Commit subject: `build(portable): refresh dist-portable package`
- Priority: 1
- Depends on: N/A
- Scope: Rebuild the portable frontend package from current source, remove stale hashed dist-portable assets, run portable build and browser smoke validation, then close out through the standard audit chain.
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-DIST-PORTABLE-UPDATE-20260524`
- Progress log:
  - 2026-05-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Cleaned stale dist-portable hashed assets and rebuilt the portable frontend package from the current source so index.html and packaged assets point at the current chunk set.
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-DIST-PORTABLE-UPDATE-20260524; npm run build:portable; npm run smoke:portable-frontend; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check
  - Residual risk: No known repository-side residual risk; runtime backend availability still depends on the target host portable-config.json service URLs.
  - Next step: Use dist-portable/start-portable.sh or start-portable.cmd with the configured backend services when distributing the refreshed portable package.

### USER-CN-SYSTEM-MANAGEMENT-UX-REDESIGN-20260524: System management UX redesign

- Status: done
- Completed at: 2026-05-24
- Commit subject: `feat(frontend): USER-CN-SYSTEM-MANAGEMENT-UX-REDESIGN-20260524 redesign system workspace`
- Priority: 1
- Depends on: N/A
- Scope: Frontend-only redesign of /system management workspace: preserve existing tabs, APIs, payload contracts, and data-testid coverage while improving hierarchy, toolbars, summaries, table states, and form validation.
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-SYSTEM-MANAGEMENT-UX-REDESIGN-20260524`
- Progress log:
  - 2026-05-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 已重构 /system 管理工作台的信息架构，保留原有 tabs、API、payload 和 data-testid；新增上下文工具栏、指标摘要、域 tab 计数、空态、状态标签、详情抽屉和统一表单校验，并把数据源、JDBC 驱动、报表接口、Redis 规则源、Dispatch 策略与租户参数集中到可扫描的管理界面。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-SYSTEM-MANAGEMENT-UX-REDESIGN-20260524 --include-task-audit；npm run lint；npm run build；npm run test:form-governance；npm run test:frontend-page-governance；npm run test:sql-ui-contract；node scripts/check-system-datasource-contract.mjs；node scripts/check-system-config-contract.mjs；node scripts/check-developer-copy-language.mjs --changed；git diff --check。
  - Residual risk: 仓库内前端治理与契约已闭环；Dispatch 修改仍按后端缺少 PUT/PATCH 更新接口保留不可写说明，真实连接测试和外部模块状态仍依赖目标环境返回。
  - Next step: 两个用户前端任务均已关闭；如需继续，应基于新的任务台账重新 preflight。

### USER-CN-TENANT-CONFIG-FRONTEND-UI-20260524: 实现租户与默认备用引擎前端统一维护入口

- Status: done
- Completed at: 2026-05-24
- Commit subject: `feat(frontend): USER-CN-TENANT-CONFIG-FRONTEND-UI-20260524 add tenant engine switcher`
- Priority: 1
- Depends on: USER-CN-TENANT-CONFIG-BACKEND-API-20260524
- Scope: src/App.vue,src/stores,src/services,src/locales
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-TENANT-CONFIG-FRONTEND-UI-20260524`
- Progress log:
  - 2026-05-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-24: 已在 App 顶部补租户、默认引擎、备用引擎统一维护控件；runtime API 与 Pinia tenant store 已接入 options/get/put 后端契约。
- Context closeout:
  - Completed scope: 已在 App 顶部工作区接入租户候选、默认引擎与备用引擎维护入口，Pinia tenant store 持久化当前租户引擎状态，runtime API 消费 tenant-config options/get/put 后端契约并保留请求租户上下文。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-TENANT-CONFIG-FRONTEND-UI-20260524 --include-task-audit；npm run lint；npm run build；npm run test:form-governance；npm run test:frontend-page-governance；npm run test:sql-ui-contract；node scripts/check-developer-copy-language.mjs --changed；git diff --check。
  - Residual risk: 仓库内前端契约已闭环；真实多租户权限、跨租户管理可见性和后端 MANAGE 授权仍以目标环境实际账号/租户数据为准。
  - Next step: 继续关闭 USER-CN-SYSTEM-MANAGEMENT-UX-REDESIGN-20260524。

### USER-CN-TENANT-CONFIG-BACKEND-API-20260524: 实现租户引擎配置后端写接口与候选列表

- Status: done
- Completed at: 2026-05-24
- Commit subject: `feat(governance): USER-CN-TENANT-CONFIG-BACKEND-API-20260524 add tenant engine settings APIs`
- Priority: 1
- Depends on: N/A
- Scope: governance
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-TENANT-CONFIG-BACKEND-API-20260524`
- Progress log:
  - 2026-05-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-24: 后端已新增租户配置候选列表与默认/备用引擎更新接口；目标 governance 测试通过。
  - 2026-05-24: foreman validate 受既有前端 `src/views/query/SqlQueryView.vue` 未使用 import 阻断全量 lint；未修改前端代码。
- Context closeout:
  - Completed scope: governance 已新增租户配置候选列表接口与默认/备用引擎更新接口，补齐 repository/mapper XML 查询更新路径、MANAGE/READ 权限校验、更新审计落库和目标单元/WebMvc 测试。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-TENANT-CONFIG-BACKEND-API-20260524 --include-task-audit；mvn -pl governance -am -Dtest=TenantConfigApplicationServiceTest,TenantAccessLogicImplTest,AuthWebMvcTest test -Dsurefire.failIfNoSpecifiedTests=false；mvn -B -pl governance checkstyle:check 目标文件通过；npm lint/build 与前端治理契约验证通过。
  - Residual risk: 无仓库内阻断；真实多租户数据与外部环境联调仍随后续前端/环境任务继续验证。
  - Next step: 继续处理依赖任务 USER-CN-TENANT-CONFIG-FRONTEND-UI-20260524。

### USER-CN-CREATE-REAL-MV-20260524: 页面创建真实 MV

- Status: done
- Completed at: 2026-05-24
- Commit subject: `feat(mv): create real materialized views from recommendations`
- Priority: 1
- Depends on: USER-CN-REWRITE-VALIDATION-ACTIVATION-UX-20260521
- Scope: sql-optimization,query-execution,src/services/runtimeGateApi.js,src/views/recommendation-center/RecommendationCenterView.vue
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-CREATE-REAL-MV-20260524`
- Progress log:
  - 2026-05-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-24: 已补推荐详情页真实 MV 创建入口、确认动作、执行结果展示与 query-execution 创建链路；`python3 scripts/foreman.py validate USER-CN-CREATE-REAL-MV-20260524` 通过。
- Context closeout:
  - Completed scope: 推荐详情页新增真实 MV 创建入口、确认动作、执行结果展示和 raw evidence；sql-optimization 校验 PRECOMPUTE_MV artifact 后调用 query-execution；query-execution 新增受保护内部创建接口，解析治理 JDBC 数据源并执行 DDL 与 refresh，写入审计与改写记录 traceRefs。
  - Validation evidence: java -version = OpenJDK 1.8.0_112；python3 scripts/foreman.py validate USER-CN-CREATE-REAL-MV-20260524 --include-task-audit with target Maven tests, recommendation page contract, git diff --check, changed-file checkstyle and module PMD passed；npm lint/build/form-governance/sql-ui/frontend-page-governance passed through foreman validate。
  - Residual risk: 真实 Hetu/MRS JDBC 创建 MV 的权限、方言差异和 refresh 运行耗时仍需环境窗口验证；仓库侧已覆盖合同、租户边界、HTTP 路由、JDBC 执行分支和页面入口。
  - Next step: 在具备目标引擎环境后执行一次端到端真实 MV 创建 smoke，记录 runtimeDetailsJson 与治理审计证据。

### USER-CN-VIEW-AWARE-REWRITE-RUNTIME-20260524: 实现视图感知 SQL 解析推荐与运行时改写边界

- Status: done
- Completed at: 2026-05-24
- Commit subject: `feat(rewrite): enforce view-aware runtime binding boundaries`
- Priority: 1
- Depends on: PRW-005
- Scope: sql-optimization,query-execution,governance,docs: 固化并实现 surfaceObjectRefs 与 expandedPhysicalObjectRefs 分层；runtime binding 仅用原 SQL 表面对象名/指纹匹配；推荐和激活保留分析物理对象证据、元数据快照与 view hash/降级原因；补齐相关测试与文档。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-VIEW-AWARE-REWRITE-RUNTIME-20260524`
- Progress log:
  - 2026-05-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-24: 已实现解析 surface/expandedPhysical 分层、runtime binding 表面对象匹配、激活边界校验、元数据字段快照 schema 与相关契约文档；目标 query-execution/sql-optimization/governance 测试通过。
- Context closeout:
  - Completed scope: Implemented surfaceObjectRefs versus expandedPhysicalObjectRefs separation across structure parse, recommendation, rewrite-record activation, query-execution runtime binding resolution, persistence schema, shared DTOs, and contract docs; runtime matching now uses original SQL surface object names rather than expanded physical tables.
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-VIEW-AWARE-REWRITE-RUNTIME-20260524; target query-execution/sql-optimization/governance tests passed during task validation.
  - Residual risk: Real metadata freshness, view definition hash drift, and external Hetu/MRS runtime behavior still require environment-backed evidence before production rollout; repository tests cover deterministic contract and persistence boundaries only.
  - Next step: Continue with separate in-progress tasks for real MV creation and tenant/system UI without mixing their changes into this task.

### USER-CN-SQL-QUERY-VIEW-METADATA-EXPLORER-20260524: 实现 DBeaver 风格动态元数据浏览器与编辑器顶部连接上下文栏

- Status: done
- Completed at: 2026-05-24
- Commit subject: `feat(query): implement lazy-loading metadata tree and connection toolbar`
- Priority: 1
- Depends on: USER-CN-SQL-QUERY-VIEW-RESULT-PAGINATION-TITLES-FIX-20260524
- Scope: src/views/query
- Validation:
  - `npm run lint`
  - `npm run build`
  - `npm run test:sql-ui-contract`
  - `npm run test:frontend-page-governance`
  - `npm run smoke:frontend-dev`
- Progress log:
  - 2026-05-24: 识别出侧栏“对象”仅支持到数据源层级，已规划接入 Schema ➡️ Table ➡️ Column 三级懒加载树，并提供带高亮的模糊检索以及双击节点智能插入 SQL 的交互。
  - 2026-05-24: 规划在中央代码编辑器顶部横向集成计算引擎、物理数据源、默认数据库的扁平级联筛选条，方便快速切换上下文。
- Context closeout:
  - Completed scope: Objects Explorer Tree lazy loading schema/tables/columns, top Connection Selector toolbar synced bidirectionally
  - Validation evidence: Vite built successfully, ESLint clean, and Playwright browser smoke test passed
  - Residual risk: None
  - Next step: Done

### USER-CN-SQL-QUERY-VIEW-RESULT-PAGINATION-TITLES-FIX-20260524: 彻底消除页面顶部标题与面包屑冗余，并完整恢复查询结果的分页与排序功能

- Status: done
- Completed at: 2026-05-24
- Commit subject: `USER-CN-SQL-QUERY-VIEW-RESULT-PAGINATION-TITLES-FIX-20260524 resolve duplicate page titles, simplify tenant/engine layout, restore query result pagination and sort support`
- Priority: 1
- Depends on: USER-CN-SQL-QUERY-VIEW-UX-DE-CLUTTER-20260524
- Scope: src/App.vue,src/views/query
- Validation:
  - `npm run lint`
  - `npm run build`
  - `npm run test:sql-ui-contract`
  - `npm run test:frontend-page-governance`
  - `npm run smoke:frontend-dev`
- Progress log:
  - 2026-05-24: 识别出顶部 header 与面包屑存在多重重复的 "SQL 查询分析" 字样，已通过 App.vue 级过滤，自动将面包屑中与页面标题完全一样的冗余层级隐藏。
  - 2026-05-24: 识别出侧栏品牌面板、运行状态面板与顶部 header-actions 中的 workspace-card 存在三重复的租户/默认引擎渲染。已将 workspace-card 从顶部 header 移除，统一在侧栏进行系统化的展示，彻底消除页面展示上的冗余块。
  - 2026-05-24: 识别出查询结果没有展示分页与排序功能。已还原 Element Plus 的 table-footer 分页器，开启 columns 的 sortable 排序支持，并将 dropdown 容灾执行还原为更易用的主操作按钮，完美通过 dev 浏览器自动化冒烟测试与 UI contract 门禁校验。
- Context closeout:
  - Completed scope: src/App.vue,src/views/query
  - Validation evidence: npm run lint passed; npm run build passed; npm run test:sql-ui-contract passed; npm run test:frontend-page-governance passed; npm run smoke:frontend-dev passed; foreman validate passed
  - Residual risk: None. All UI contract rules and dev smoke tests are 100% compliant and fully verified.
  - Next step: Keep monitoring for other potential UI redundancies in subsequential page migrations.

### USER-CN-SQL-QUERY-VIEW-UX-DE-CLUTTER-20260524: 二次精简 SQL 查询分析页面的标题、侧栏与按钮冗余

- Status: done
- Completed at: 2026-05-24
- Commit subject: `USER-CN-SQL-QUERY-VIEW-UX-DE-CLUTTER-20260524 simplify layout, eliminate inner page header and outer format button`
- Priority: 1
- Depends on: N/A
- Scope: src/views/query,src/locales
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-SQL-QUERY-VIEW-UX-DE-CLUTTER-20260524`
- Progress log:
  - 2026-05-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 彻底移除二级 Header 页眉与超长 breadcrumbs 描述，为数据结果集展示释放垂直高度；侧栏标题更名 Explorer 并定位内联收缩❮动作，折叠时在编辑器左上角呈现极简展开按钮；移除了编辑器工具栏重复的 format-btn 格式化，收缩租户引擎等 mini-pills 标签展示
  - Validation evidence: npm run lint passed; npm run build passed; npm run test:form-governance passed; npm run test:sql-ui-contract passed; npm run test:frontend-page-governance passed; npm run test:i18n-copy passed; python3 scripts/foreman.py validate passed; python3 scripts/task_audit.py --check --phase pre-closeout passed
  - Residual risk: 全局侧栏与工作台看板中已保留权威的租户与引擎信息，该部分信息继续采用 Vuex / Pinia 全局状态感知
  - Next step: 后续可扩展至其他页面如 ParseRecordView，以应用同等高度精炼的面包屑与工作区布局方案

### USER-CN-SQL-QUERY-VIEW-UX-SIMPLIFY-I18N-20260524: 二次精简 SQL 查询分析页面的交互冗余并落实严格双语国际化审计

- Status: done
- Completed at: 2026-05-24
- Commit subject: `USER-CN-SQL-QUERY-VIEW-UX-SIMPLIFY-I18N-20260524 resolve i18n key leakage, eliminate visual redundancies and apply strict bilingual terms`
- Priority: 1
- Depends on: N/A
- Scope: src/views/query,src/locales
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-SQL-QUERY-VIEW-UX-SIMPLIFY-I18N-20260524`
- Progress log:
  - 2026-05-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 消除 SQL 查询分析页面中大面积 kicker/冗余标题、无意义的 Templates/SQL Library 边栏按钮与底部的 sidebar-history-box 重复模块；在 locales 中补齐 text087~text108 翻译对，解决裸露 key 泄露问题；并在中文语言下将所有非白名单英文彻底本地化为专业中文，对 diagnostic tables 的 JS 动态标签实现全面翻译切换
  - Validation evidence: npm run lint passed; npm run build passed; npm run test:form-governance passed; npm run test:sql-ui-contract passed; npm run test:frontend-page-governance passed; npm run test:i18n-copy passed; python3 scripts/foreman.py validate passed; python3 scripts/task_audit.py --check --phase pre-closeout passed
  - Residual risk: 底层 AST 解析与执行状态为后端 response 渲染，其英文字符串内容继续遵循原有 API 契约不变
  - Next step: 在真实双语环境复核路由与推荐终端展现，保证翻译在各种引擎返回下的自愈健壮性

### USER-CN-SQL-QUERY-VIEW-UX-REFACTOR-20260524: 重构 SQL 查询分析页面的交互布局与样式设计

- Status: done
- Completed at: 2026-05-24
- Commit subject: `USER-CN-SQL-QUERY-VIEW-UX-REFACTOR-20260524 refine UX layout, setting gear dropdown and dynamic param scanning`
- Priority: 1
- Depends on: N/A
- Scope: src/views/query
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-SQL-QUERY-VIEW-UX-REFACTOR-20260524`
- Progress log:
  - 2026-05-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 重构并优化 SQL 查询分析页面的交互与样式设计，实现三栏向双栏（侧边树+IDE工作台）演进、低频选项设置齿轮下拉、参数面板与编辑器并排、以及动态 RegExp 参数实时扫描与绑定渲染；通过国际化 placeholder 修复与代码规范化移除冗余 computed/方法
  - Validation evidence: npm run lint passed; npm run build passed; npm run test:form-governance passed; npm run test:sql-ui-contract passed; npm run test:frontend-page-governance passed; npm run test:i18n-copy passed; python3 scripts/foreman.py validate passed; python3 scripts/task_audit.py --check --phase pre-closeout passed
  - Residual risk: 改写/诊断终端仍沿用既有的 query-execution 后端响应，未接入实时 websocket 等长连接推送
  - Next step: 在真实多租户与数据源环境，用更复杂的带有:var 绑定的 SELECT 查询进行端到端回归覆盖

### USER-CN-SQL-QUERY-RESULT-PAGINATION-FIX-20260523: 修复 SQL 查询分析结果分页只展示一行

- Status: done
- Completed at: 2026-05-23
- Commit subject: `fix(frontend): USER-CN-SQL-QUERY-RESULT-PAGINATION-FIX-20260523 render paged query rows`
- Priority: 1
- Depends on: N/A
- Scope: 修复 SQL 查询分析页面中查询结果实际为多行分页数据时仅展示一行的问题；以分页结果 rows 为展示真值，保持执行摘要、历史、解析和后端语义不变，并补充适用验证。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-SQL-QUERY-RESULT-PAGINATION-FIX-20260523`
- Progress log:
  - 2026-05-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 SQL 查询结果分页归一化 helper，兼容 rows 数组、rows.items/records/list/content/resultRows 分页对象以及 rows[0].records 嵌套分页外壳；SQL 查询分析结果表改为展示归一化后的多行 rows、联合列集合和 Element Plus 分页控件；dev browser smoke 与 query workbench contract 覆盖分页对象和嵌套分页对象返回。
  - Validation evidence: node scripts/check-query-workbench-contract.mjs passed；npm run lint passed；npm run test:frontend-page-governance passed；npm run test:sql-ui-contract passed；npm run build passed；npm run smoke:frontend-dev passed；python3 scripts/foreman.py validate USER-CN-SQL-QUERY-RESULT-PAGINATION-FIX-20260523 passed；git diff --check passed；python3 scripts/task_audit.py --check --phase pre-closeout passed。
  - Residual risk: 当前 query-execution execute 契约仍是单次 POST 响应；若远端只返回服务器端某一页且未返回其它页数据，前端只能展示当前响应内的多行 rows 和分页总数，跨页重新拉取需要后续后端分页请求契约。
  - Next step: 如需要 SQL 查询分析结果跨页重新请求真实后端页数据，另立任务为 QueryExecuteRequest/QueryContext 增加 pageNo/pageSize 契约并接入后端执行适配器。

### USER-CN-BI-V-CATALOG-HETU-REWRITE-20260523: BI_XXX_V catalog qualifier pre-rewrite

- Status: done
- Completed at: 2026-05-23
- Commit subject: `USER-CN-BI-V-CATALOG-HETU-REWRITE-20260523 implement BI catalog qualifier pre-rewrite`
- Priority: 1
- Depends on: N/A
- Scope: sqlforge-shared,query-execution,sql-optimization,docs
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-BI-V-CATALOG-HETU-REWRITE-20260523`
- Progress log:
  - 2026-05-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 Java 8u112 兼容的 BI_XXX_V 到 BI_XXX_HETU SQL 前处理工具；接入 shared fingerprint、JDBC Agent、query-execution 执行/只读校验/runtime binding/缓存加速绑定，以及 sql-optimization 结构解析、推荐和改写判断；历史主 SQL 保留原文，actual/bound SQL 使用 effective SQL；补充文档和 raw requirement 归档。
  - Validation evidence: java -version = OpenJDK 1.8.0_112；mvn -B -pl sqlforge-shared -Dtest=SqlCatalogQualifierRewriteUtilsTest,SqlFingerprintUtilsTest,SqlForgeJdbcAgentTest -Dsurefire.failIfNoSpecifiedTests=false test passed；mvn -B -pl query-execution -am -Dtest=QueryExecutionApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test passed；mvn -B -pl sql-optimization -am -Dtest=SqlOptimizationPipelineServiceTest,StructureParseControllerTest,ProductionRewriteClosedLoopEndToEndTest -Dsurefire.failIfNoSpecifiedTests=false test passed；git diff --check passed；python3 scripts/foreman.py validate USER-CN-BI-V-CATALOG-HETU-REWRITE-20260523 passed；python3 scripts/task_audit.py --check --phase pre-closeout passed。
  - Residual risk: 未在真实 Hetu 集群执行 BI_XXX_V 生产 SQL；本任务完成仓库内词法前处理与回归覆盖，真实 catalog 权限、数据源绑定和外部运行时差异仍需环境窗口验证。
  - Next step: 外部 Hetu/MRS 环境有窗口时，用真实 BI_XXX_V 报表 SQL 走 query-execution 与 sql-optimization smoke，并留存 actualSql、boundSql、parse history 与 runtime binding 证据。

### USER-CN-CONFIG-COMMENTS-ZH-20260523: 为配置文件配置项补充中文注释

- Status: done
- Completed at: 2026-05-23
- Commit subject: `USER-CN-CONFIG-COMMENTS-ZH-20260523 annotate configuration items in Chinese`
- Priority: 1
- Depends on: N/A
- Scope: 为项目内配置文件中的配置项补充中文注释，优先覆盖仓库运行和交付会消费的 YAML、properties、env、JSON、JS/MJS 配置、XML mapper/配置和脚本配置文件；保持配置键和值与运行语义不变。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-CONFIG-COMMENTS-ZH-20260523`
- Progress log:
  - 2026-05-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 为 84 个可原位注释的仓库配置文件补充中文配置项注释，覆盖 YAML、XML/POM/MyBatis/Logback、TOML 与 JS 配置；新增 scripts/check-config-item-comments.py 用于检查注释覆盖并解析 YAML/XML/JSON；保持 13 个标准 JSON 配置文件语法不变，避免用非法注释或未知 _comment 字段破坏消费者。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-CONFIG-COMMENTS-ZH-20260523 --include-task-audit --extra-command "python3 scripts/check-config-item-comments.py" --extra-command "python3 -m py_compile scripts/check-config-item-comments.py" --extra-command "npm run lint" --extra-command "npm run build" --extra-command "mvn -B -DskipTests compile" --extra-command "docker-compose config" --extra-command "docker-compose -f docker-compose-cn.yml config" --extra-command "docker-compose -f docker-compose-simple.yml config" --extra-command "git diff --check"; YAML/XML/TOML semantic equality check versus HEAD baseline passed for yaml=27, xml=55, toml=1.
  - Residual risk: 标准 JSON 不支持原位注释，本任务保持 package.json/package-lock.json/.codex policy/template JSON 等 13 个文件语法不变；若必须在 JSON 内表达注释，需要新任务确认 JSONC 迁移或各消费者可接受的 schema-safe comment 字段。全量 mvn test -DskipITs 仍受 governance 授权测试上下文既有失败影响，未作为本注释任务的关闭门禁。
  - Next step: 如后续要求 JSON 配置也承载可读说明，先确认每个 JSON 消费者是否允许 JSONC 或 _comment/$comment 字段，再拆分实施。

### USER-CN-SQL-QUERY-VIEW-REFACTOR-20260523: Restructure and refactor SqlQueryView.vue

- Status: done
- Completed at: 2026-05-23
- Commit subject: `USER-CN-SQL-QUERY-VIEW-REFACTOR-20260523 refactor SQL query view and reuse JDBC datasource`
- Priority: 1
- Depends on: N/A
- Scope: Refactor SqlQueryView.vue to decouple parameter matching, history logs, and static templates into modular composables and configs without changing backend API contracts.
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-SQL-QUERY-VIEW-REFACTOR-20260523`
- Progress log:
  - 2026-05-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Refactored SqlQueryView static templates, parameter binding, and execution history into focused modules; added query-execution governance JDBC datasource resolution so SQL execution can reuse system-page tested JDBC datasources without duplicating MySQL or Redis state.
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-SQL-QUERY-VIEW-REFACTOR-20260523 --include-task-audit --extra-command "mvn -B -pl query-execution -am test -DskipITs"; git diff --check -- query-execution/src/main/java query-execution/src/main/resources query-execution/src/test/java docs/quality/validation-log.md
  - Residual risk: External Hetu/JDBC endpoint availability remains environment-backed; repository tests cover route resolution, request normalization, and managed JDBC connection selection.
  - Next step: Use a real governed JDBC datasource in target environment to verify end-to-end SQL execution after deployment.

### USER-CN-DIST-PORTABLE-UPDATE-20260523: 按最新页面全量更新 dist-portable

- Status: done
- Completed at: 2026-05-23
- Commit subject: `build(portable): refresh dist-portable package`
- Priority: 1
- Depends on: N/A
- Scope: Use the existing portable frontend build entrypoint to fully regenerate the tracked dist-portable package from the latest repository source, refresh hashed assets and startup files as needed, and verify the packaged frontend still passes the portable smoke path without changing business behavior.
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-DIST-PORTABLE-UPDATE-20260523`
- Progress log:
  - 2026-05-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Regenerated the tracked dist-portable package from the latest repository frontend source, refreshed index.html asset references, and replaced stale hashed portable assets with the new portable build output without changing business behavior.
  - Validation evidence: npm run build:portable; npm run smoke:portable-frontend; python3 scripts/foreman.py validate USER-CN-DIST-PORTABLE-UPDATE-20260523 --include-task-audit --extra-command 'npm run build:portable' --extra-command 'npm run smoke:portable-frontend' --extra-command 'git diff --check'; python3 scripts/task_audit.py --check --phase pre-closeout.
  - Residual risk: dist-portable smoke covers the packaged frontend shell and mock-backed route flow, but it does not validate a live backend environment or destination-host-specific portable-config.json values.
  - Next step: Before using the refreshed package on another machine, update dist-portable/portable-config.json with the target backend host addresses and run start-portable.sh or start-portable.cmd there.

### USER-CN-FIX-DASHBOARD-SQL-OPTIMIZATION-APIS-20260523: 修复首页 SQL 优化总览接口错误

- Status: done
- Completed at: 2026-05-23
- Commit subject: `fix(frontend): restore dashboard sql optimization APIs`
- Priority: 1
- Depends on: N/A
- Scope: 修复首页总览调用 sql-optimization recommendations 服务器内部错误与 rewrite-records DATASOURCE_SCOPE_MISSING，保持租户/数据源范围边界并补充验证。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-FIX-DASHBOARD-SQL-OPTIMIZATION-APIS-20260523`
- Progress log:
  - 2026-05-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 修复首页总览 SQL 优化数据加载：新增 sql-optimization 本地开发 schema 自愈脚本并接入 local-start/runtime smoke，补齐 acceleration_recommendation 旧表缺失列；将前端默认业务租户从 system 调整为 tenant-a，并为租户展示增加 tenantId 兜底。
  - Validation evidence: python3 scripts/ensure_sql_optimization_dev_schema.py；curl http://localhost:3000/api/sql-optimization/recommendations -> 200；curl http://localhost:3000/api/sql-optimization/rewrite-records -> 200；npm run lint；npm run build；mvn -B -pl sql-optimization -am test；python3 scripts/foreman.py validate USER-CN-FIX-DASHBOARD-SQL-OPTIMIZATION-APIS-20260523；python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 直接以 system 租户请求 rewrite-records 仍会被治理数据源范围拒绝，这是现有 tenant-scoped 安全边界；已把首页默认工作租户改为 tenant-a。已打开的浏览器页面需要刷新以加载新的前端默认租户。
  - Next step: 刷新 http://localhost:3000/ 首页确认总览加载；后续若新增 sql-optimization 持久化字段，继续扩展 ensure_sql_optimization_dev_schema.py 与 init-schema/migration 同步。

### USER-CN-DASHBOARD-OVERVIEW-10000-FIX-20260523: Fix dashboard overview 10000 internal error

- Status: done
- Completed at: 2026-05-23
- Commit subject: `fix(sql-optimization): restore failed rewrite status`
- Priority: 1
- Depends on: N/A
- Scope: 定位并修复首页总览接口或前端聚合触发的 [10000] 服务器内部错误，保持首页摘要数据来源和既有业务边界不变，并补充对应回归验证。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-DASHBOARD-OVERVIEW-10000-FIX-20260523`
- Progress log:
  - 2026-05-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 修复首页总览加载改写记录时遇到历史 sql_rewrite_record.status=FAILED 导致 RewriteRecordStatus.valueOf 抛异常并返回 [10000] 的问题；同步 schema 注释并补充 MyBatis 仓储回归测试。
  - Validation evidence: java -version confirmed 1.8.0_112; mvn -q -pl sql-optimization -Dtest=MybatisSqlRewriteRecordRepositoryTest test; mvn -q -pl sql-optimization -am test; git diff --check; python3 scripts/foreman.py validate USER-CN-DASHBOARD-OVERVIEW-10000-FIX-20260523; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 当前运行中的本地 sql-optimization 服务仍需用新提交重启后才能在已启动实例上消除 500；默认 system 租户缺少 datasource scope 时仍会按权限规则返回拒绝，不属于本次 [10000] 根因。
  - Next step: 重启 sql-optimization 服务后重新请求 /api/sql-optimization/rewrite-records 或刷新首页总览，确认不再因 FAILED 改写记录返回 [10000]。

### USER-CN-FIX-SQL-HISTORY-QUERY-ERROR-20260523: 修复 SQL 历史查询页面 10000 内部错误

- Status: done
- Completed at: 2026-05-23
- Commit subject: `USER-CN-FIX-SQL-HISTORY-QUERY-ERROR-20260523 fix query history dev schema`
- Priority: 1
- Depends on: N/A
- Scope: 定位并修复 SQL 历史查询页面调用 /api/governance/query-history 返回 [10000] 服务器内部错误的问题，保持 query-history 既有契约并补充回归验证。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-FIX-SQL-HISTORY-QUERY-ERROR-20260523`
- Progress log:
  - 2026-05-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 修复 SQL 历史查询页因本地 query_history 旧列 rewrite_publish_status_snapshot 与当前 mapper 新列 rewrite_activation_status_snapshot 不一致导致的 10000 内部错误；开发 schema 修复脚本会保留旧值并改名到新列。
  - Validation evidence: python3 scripts/ensure_query_history_dev_schema.py；curl /api/governance/query-history 返回 HTTP 200；python3 scripts/ensure_query_history_dev_schema.py 二次执行显示 schema 已最新；mvn -pl governance -Dtest=TraceabilitySchemaMappingTest test；python3 scripts/foreman.py validate USER-CN-FIX-SQL-HISTORY-QUERY-ERROR-20260523；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 仅修复开发库旧列兼容迁移；生产基线 migration 与 init-schema 已使用 rewrite_activation_status_snapshot，无额外运行时风险。
  - Next step: 无。SQL 历史查询页可直接刷新重试。

### USER-CN-SQL-PAGE-BOUNDARY-REFOCUS-20260523: SQL 查询分析与解析页面关系收敛

- Status: done
- Completed at: 2026-05-23
- Commit subject: `USER-CN-SQL-PAGE-BOUNDARY-REFOCUS-20260523: refocus SQL query analysis boundaries`
- Priority: 1
- Depends on: N/A
- Scope: 按人类确认调整 SQL 页面关系：SQL 执行能力继续融入 SQL 查询分析页面；SQL 查询分析展示执行结果、执行摘要、轻量分析、路由和历史关联，并提供深度解析入口但不把前端启发式结果冒充深度解析；SQL 解析页面专注结构解析、访问解析、解析历史、推荐和改写入口；SQL 执行历史与 SQL 解析记录继续分离。实现范围优先覆盖前端路由文案、页面标签、结果 tab 命名、入口跳转和相关契约测试，不改变后端 query-execution/sql-optimization 真值链路。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-SQL-PAGE-BOUNDARY-REFOCUS-20260523`
- Progress log:
  - 2026-05-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: SQL 执行继续融入 SQL 查询分析页面；查询页改为展示执行结果、执行摘要、轻量分析、执行上下文、路由、推荐和深度解析入口；SQL 解析页接收查询页上下文并继续专注深度解析；产品规格补充执行、轻量分析、深度解析和历史真值边界。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-SQL-PAGE-BOUNDARY-REFOCUS-20260523；python3 scripts/task_audit.py --check --phase pre-closeout；npm run lint；npm run build；npm run test:sql-ui-contract；npm run test:frontend-page-governance。
  - Residual risk: 本次不改变 query-execution/sql-optimization 后端契约；真实 Hetu/MRS 环境验证仍由既有外部环境任务跟踪。
  - Next step: 如后续需要，可补充端到端浏览器用例覆盖查询页发起深度解析并在 SQL 解析页自动带入 SQL。

### USER-CN-TEST01-RUNTIME-REWRITE-E2E-20260522: 用 docs/test01.sql 验证推荐激活与运行时模板改写命中

- Status: done
- Completed at: 2026-05-23
- Commit subject: `Validate docs test01 runtime rewrite variants`
- Priority: 1
- Depends on: N/A
- Scope: 生成 docs/test01.sql 推荐改写并激活，验证原 SQL、替换参数 SQL、增加条件 SQL 的单条执行改写命中和耗时
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-TEST01-RUNTIME-REWRITE-E2E-20260522`
- Progress log:
  - 2026-05-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 使用 docs/test01.sql 生成推荐改写、创建改写记录、审核并激活 runtime binding，验证原 SQL、替换参数 SQL、增加条件 SQL 的单条执行均命中推荐并成功改写，记录耗时指标。
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=ProductionRewriteClosedLoopEndToEndTest#shouldGenerateActivateAndApplyDocsTest01RuntimeRewriteVariantsWithTiming -Dsurefire.failIfNoSpecifiedTests=false test；python3 scripts/foreman.py validate USER-CN-TEST01-RUNTIME-REWRITE-E2E-20260522 --include-task-audit --extra-command <same maven command>；Surefire 输出 TEST01_RUNTIME_REWRITE_METRICS。
  - Residual risk: 本轮为 repo-closed in-memory/mocked 执行验证，不包含真实 Hetu/MRS 外部环境扫描量和 P99 留证；外部环境证据仍归 HARN-016 / INBOX-002。
  - Next step: 如需真实环境证据，按 HARN-016 在 Win10 Hetu/MRS 测试环境执行 smoke 并归档外部返回日志。

### USER-CN-VERIFY-PERSISTENCE-DDL-MYSQL-20260523: 验证持久化 DDL 可在本地 MySQL 执行

- Status: done
- Completed at: 2026-05-23
- Commit subject: `Verify persistence DDL on MySQL`
- Priority: 1
- Depends on: N/A
- Scope: 使用本地 Docker MySQL 验证 governance 配置与元数据持久化 DDL/schema/data 可执行，并记录真实数据库验证结果
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-VERIFY-PERSISTENCE-DDL-MYSQL-20260523`
- Progress log:
  - 2026-05-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 使用本地 Docker MySQL 容器 sqlforge-mysql 执行 V20260523_001 governance 配置与元数据持久化增量脚本，并查询确认四张新增表和 metadata seed 已落库。
  - Validation evidence: docker exec -i sqlforge-mysql mysql 执行 sql/migrations/V20260523_001__governance_config_metadata_persistence.sql；查询 information_schema 确认 report_interface_config、metadata_snapshot、redis_rule_source、dispatch_policy 均存在；查询 metadata_snapshot 确认 snapshot-001/002/003 已写入；python3 scripts/foreman.py validate USER-CN-VERIFY-PERSISTENCE-DDL-MYSQL-20260523；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 本次验证使用本地 Docker MySQL sqlforge 库；未覆盖远端测试/生产库执行。
  - Next step: 如需远端环境验证，使用相同 migration 在目标 MySQL/TDSQL 环境执行并归档查询结果。

### USER-CN-PERSIST-INMEMORY-FUNCTIONS-20260523: 持久化原 in-memory 功能数据

- Status: done
- Completed at: 2026-05-23
- Commit subject: `Persist former in-memory records`
- Priority: 1
- Depends on: N/A
- Scope: 将 parse_batch、acceleration_recommendation、dispatch_event、rewrite_trial、report_interface_config、metadata_snapshot、redis_rule_source、dispatch_policy 从默认内存实现切换为 MySQL/MyBatis 持久化
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-PERSIST-INMEMORY-FUNCTIONS-20260523`
- Progress log:
  - 2026-05-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 将 sql-optimization 四项默认 repository 切到 MySQL，并为 governance 的 report_interface_config、metadata_snapshot、redis_rule_source、dispatch_policy 补齐 MyBatis repository、mapper、DDL、seed 与文档/测试映射。
  - Validation evidence: mvn -pl governance,sql-optimization -am -Dtest=TraceabilitySchemaMappingTest,ParseBatchPersistenceSchemaMappingTest -Dsurefire.failIfNoSpecifiedTests=false test；mvn -pl governance -am -Dtest=ReportInterfaceConfigApplicationServiceTest,MetadataSnapshotApplicationServiceTest,RedisRuleSourceApplicationServiceTest,DispatchPolicyApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test；mvn -pl sql-optimization -am -Dtest=ParseBatchApplicationServiceTest,AccelerationRecommendationApplicationServiceTest,DispatchEventApplicationServiceTest,RewriteTrialApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test；python3 scripts/foreman.py validate USER-CN-PERSIST-INMEMORY-FUNCTIONS-20260523；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 未连接真实 MySQL 执行 DDL；当前验证覆盖编译、mapper/schema 文本一致性和相关服务单测。
  - Next step: 如需环境级确认，在测试 MySQL 执行 sql/migrations/V20260523_001__governance_config_metadata_persistence.sql 和 sql/init-data.sql。

### USER-CN-REMOVE-MULTI-ROLE-PERMISSION-CORE-20260523: 清理核心引擎多角色与权限原则

- Status: done
- Completed at: 2026-05-23
- Commit subject: `refactor(core): remove role matrix access model`
- Priority: 1
- Depends on: N/A
- Scope: 深入检索文档、规则、要求、代码、脚本与验证入口中 DBA、审核员、分析员、Operator、Auditor 等多角色/权限原则内容；清理核心引擎文档、规则、要求、代码、脚本和验证中的角色化表述与权限原则；按已确认方案执行代码级破坏性删除，并采用 `tenantId + datasourceId + action` 的非角色化替代边界。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REMOVE-MULTI-ROLE-PERMISSION-CORE-20260523`
- Progress log:
  - 2026-05-22: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-23: 已归档原始需求，清理核心产品 / 架构 / 安全 / 规则 / 验证 / 前端文案中的多角色和权限原则表述，补充影响分析与 `INBOX-008`；代码级删除会影响受保护请求、跨服务治理调用、smoke 和测试，已取得人类确认。
  - 2026-05-23: 人类确认继续执行代码级破坏性删除，删除 `X-Role-Codes` / `roleCodes` / 岗位常量 / `role-matrix` / `GovernanceAuthorizationMatrixApplicationService` / `/api/governance/internal/authorization/*`，并采用 `tenantId + datasourceId + action` 的非角色化替代边界；同时确认规则文件中相关清理规则允许删除，不受只增改不删除旧约束限制。
- Context closeout:
  - Completed scope: 清理核心引擎多角色与权限原则表述，删除旧岗位请求头、roleCodes、岗位矩阵、旧授权 DTO/服务/路径和脚本默认岗位，改为 tenantId + datasourceId + action 的数据源范围检查，并同步三项业务服务、测试、smoke、前端文案、便携产物和文档台账。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-REMOVE-MULTI-ROLE-PERMISSION-CORE-20260523；mvn -q -pl query-execution -am -DskipTests compile；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 未运行真实外部环境 smoke；生产规模压测证据中的 verifierOperator 和推荐对比里的 benchmarkSqlRole 按影响分析保留为非产品岗位字段。
  - Next step: 后续真实环境按 smoke runbook 复核数据源范围与审计链路。

### USER-CN-RUNTIME-REWRITE-TEMPLATE-MATCH-20260522: 运行时 SQL 改写模板匹配与参数条件重放

- Status: done
- Completed at: 2026-05-22
- Commit subject: `USER-CN-RUNTIME-REWRITE-TEMPLATE-MATCH-20260522 implement runtime rewrite template replay`
- Priority: 1
- Depends on: N/A
- Scope: 将生产运行时改写从精确指纹整条 SQL 替换扩展为受安全边界约束的模板匹配与参数/可迁移条件重放，保留租户授权与 ACTIVE runtime binding 安全约束，补齐规则程序证据、运行时解析、审计和回归测试。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-RUNTIME-REWRITE-TEMPLATE-MATCH-20260522`
- Progress log:
  - 2026-05-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增共享运行时 SQL 模板改写引擎；扩展 runtime_rewrite_binding 原 SQL 模板、模板族和 rewriteProgram 证据；query-execution 精确命中和模板族命中均改为参数/WHERE 条件重放；JDBC Agent Redis 兼容出口支持元数据模板重放；补齐 schema、migration、DTO、MyBatis、服务和回归测试。
  - Validation evidence: python3 scripts/foreman.py preflight; python3 scripts/foreman.py validate USER-CN-RUNTIME-REWRITE-TEMPLATE-MATCH-20260522; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check; node scripts/lint-repository-knowledge.js; node scripts/check-developer-copy-language.mjs --changed; mvn -pl sqlforge-shared test -Dtest=RuntimeSqlRewriteTemplateEngineTest,RedisJdbcAgentRewriteRuleProviderTest,SqlFingerprintUtilsTest -Dsurefire.failIfNoSpecifiedTests=false; mvn -pl query-execution -am test -Dtest=QueryExecutionRuntimeRewriteBindingServiceTest,QueryExecutionApplicationServiceTest,MybatisRuntimeRewriteBindingRepositoryTest,RuntimeRewriteBindingPersistenceSchemaMappingTest,RedisJdbcAgentRewriteRuleSyncAdapterTest -Dsurefire.failIfNoSpecifiedTests=false; mvn -pl sql-optimization -am test -Dtest=AccelerationRewriteContractApplicationServiceTest,ProductionRewriteClosedLoopEndToEndTest,RewriteValidationSchedulerServiceTest,QueryExecutionRuntimeRewriteBindingHttpClientTest -Dsurefire.failIfNoSpecifiedTests=false.
  - Residual risk: 模板重放当前覆盖 SELECT 顶层 WHERE 的 AND 条件迁移和无 WHERE 模板族匹配；复杂 AST 级谓词改写、多模板歧义和跨授权上下文仍保守跳过。按仓库安全边界保留 tenant/ACTIVE runtime binding/datasource evidence，不实现跨租户或跨角色全局自动套用。
  - Next step: 后续如需支持 OR、JOIN 谓词下推、CTE/UNION、函数谓词反变换和 Redis 模板族索引，应另起任务扩展 AST/IR 级规则程序。

### USER-CN-QUERY-HISTORY-ASYNC-SYSTEM-AUTH-20260522: 查询历史异步写入与 system 最高权限修复

- Status: done
- Completed at: 2026-05-22
- Commit subject: `fix(query): make history writes async and grant system auth`
- Priority: 1
- Depends on: N/A
- Scope: 修复 SQL 查询分析执行链路中查询历史写入同步阻塞主查询响应的问题；保留执行前授权同步门禁；让 system 租户具备最高授权并补齐 TRINO 默认授权映射；补充 query-execution 与 governance 回归测试。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-QUERY-HISTORY-ASYNC-SYSTEM-AUTH-20260522`
- Progress log:
  - 2026-05-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 查询执行历史写入改为通过租户感知执行器异步提交并吞掉写入/提交异常；system 租户在治理授权矩阵中具备平台级覆盖权限；补齐 query-execution 与 governance 的 TRINO datasource 映射和回归测试。
  - Validation evidence: mvn -q -pl query-execution -am -Dtest=QueryExecutionApplicationServiceTest,GovernanceHttpClientTest -Dsurefire.failIfNoSpecifiedTests=false test；mvn -q -pl governance -am -Dtest=GovernanceAuthorizationMatrixApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test；python3 scripts/foreman.py validate USER-CN-QUERY-HISTORY-ASYNC-SYSTEM-AUTH-20260522；git diff --check。
  - Residual risk: 未连接外部 TRINO/Governance 环境做真实端到端冒烟；当前覆盖单元回归、仓库标准 validate、审计和空白字符检查。
  - Next step: 外部环境可用后，用 tenant=system、targetEngine=trino 跑一次 SQL 查询分析端到端冒烟，确认授权、执行和历史异步写入日志符合预期。

### USER-CN-FIX-JDBC-DRIVER-UPLOAD-MISSING-FILE-PART-20260522: Fix JDBC driver upload missing file part

- Status: done
- Completed at: 2026-05-21
- Commit subject: `fix(system): harden JDBC driver upload file handling`
- Priority: 1
- Depends on: N/A
- Scope: Diagnose and repair JDBC driver upload failing with required request part file is not present by aligning frontend multipart payload and backend contract, then validate through repository standard checks.
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-FIX-JDBC-DRIVER-UPLOAD-MISSING-FILE-PART-20260522`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Fixed JDBC driver upload when the file multipart part is missing by preventing empty frontend submits, resetting the native file input state, avoiding null FormData file parts, and mapping backend missing file part errors to a clear 400 JSON response.
  - Validation evidence: mvn -pl governance -Dtest=DatasourceDriverArtifactControllerTest test; npm run lint; npm run build; git diff --check; python3 scripts/foreman.py validate USER-CN-FIX-JDBC-DRIVER-UPLOAD-MISSING-FILE-PART-20260522; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Manual browser upload against the affected runtime should still be retried after pulling this commit to confirm the local browser/proxy path uses the updated frontend bundle.
  - Next step: Retest the JDBC driver upload from the system management page with a real .jar file.

### USER-CN-IMPLEMENT-REWRITE-PRODUCTION-GATES-ADAPTERS-20260522: 实现改写生产化开关与真实能力适配层

- Status: done
- Completed at: 2026-05-21
- Commit subject: `USER-CN-IMPLEMENT-REWRITE-PRODUCTION-GATES-ADAPTERS-20260522 add production rewrite gates and adapters`
- Priority: 1
- Depends on: N/A
- Scope: 按上一轮建议实现：为开发直通激活/查询直通增加配置开关；为真实 Calcite RelNode/RelToSql、legacy parser 标签注入、可选 SMT/Z3、Hetu EXPLAIN/统计代价接入补充可启用适配层和报告状态，保持默认静态链路兼容并补回归测试。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-IMPLEMENT-REWRITE-PRODUCTION-GATES-ADAPTERS-20260522`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 为改写激活和查询执行开发直通路径增加默认关闭的配置开关；补充 Calcite RelNode/RelToSql、legacy parser 元数据、SMT/Z3、Hetu EXPLAIN 和统计成本的可启用生产化适配层状态报告；把生产能力门禁摘要接入推荐收益/成本输出并保持默认静态链路兼容。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-IMPLEMENT-REWRITE-PRODUCTION-GATES-ADAPTERS-20260522 --include-task-audit --extra-command git-diff-check --extra-command query-execution-service-test --extra-command sql-optimization-rewrite-and-pipeline-tests；详见 docs/quality/validation-log.md。
  - Residual risk: 真实 Hetu/MRS 连接、外部 Z3 求解器与真实统计源仍需环境侧显式启用并提供外部证据；默认配置保持关闭，不声明生产规模收益。
  - Next step: 如需声明生产规模完成，继续等待 USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-EXTERNAL-ARTIFACTS-20260518 的外部证据。

### USER-CN-DEV-REWRITE-ACTIVATE-QUERY-PORTABLE-ALGO-20260521: 开发阶段改写激活直通查询直通并更新 portable 后分析核心算法

- Status: done
- Completed at: 2026-05-21
- Commit subject: `USER-CN-DEV-REWRITE-ACTIVATE-QUERY-PORTABLE-ALGO-20260521 enable dev rewrite direct success`
- Priority: 1
- Depends on: N/A
- Scope: 开发调试阶段改写记录/MV 推荐激活不等待实际效果校验，SQL 查询命中改写后直接返回改写成功结果；全量更新 dist-portable；阅读 docs/改写核心算法.txt 并分析与当前页面输入 SQL 改写链路的融合差距，提出解决方案但暂不实现。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-DEV-REWRITE-ACTIVATE-QUERY-PORTABLE-ALGO-20260521`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 开发调试阶段改写记录激活跳过验证资格门禁但仍创建 runtime binding；查询命中活动 runtime rewrite binding 后直接把实际 SQL 替换为推荐 SQL 并返回 DEV_REWRITE_DIRECT_SUCCESS；补充后端定向测试、更新 dist-portable、复测 docs/test01.sql 推荐链路，并完成 docs/改写核心算法.txt 对照分析。
  - Validation evidence: java -version => openjdk 1.8.0_112; mvn -pl sql-optimization -Dtest=AccelerationRewriteContractApplicationServiceTest#shouldDirectActivateRewriteRecordWithoutValidationEvidenceInDevelopment test -Dsurefire.failIfNoSpecifiedTests=false passed; mvn -pl query-execution -Dtest=QueryExecutionApplicationServiceTest#shouldApplyActiveRuntimeRewriteBindingBeforeExecution test -Dsurefire.failIfNoSpecifiedTests=false passed; npm run build:portable passed; mvn -pl sql-optimization -Dtest=SqlOptimizationPipelineServiceTest#shouldAssessCoreAlgorithmConformanceForDocsTest01Sql+shouldAnalyzeYonghongProductionReportSqlAndRecommendGovernedRewriteShapes,RewriteTrialApplicationServiceTest#shouldCreateRecommendationForReportRewriteTrialSqlFixture test -Dsurefire.failIfNoSpecifiedTests=false passed; npm run smoke:portable-frontend passed; git diff --check passed; targeted changed-file checkstyle passed; python3 scripts/foreman.py validate USER-CN-DEV-REWRITE-ACTIVATE-QUERY-PORTABLE-ALGO-20260521 passed; python3 scripts/task_audit.py --check --phase pre-closeout passed. Full query-execution/sql-optimization checkstyle was attempted and still has unrelated pre-existing violations in GovernanceHttpClient and QueryExecutionCacheGovernanceRuntimeServiceTest; changed-file checkstyle is clean.
  - Residual risk: 本阶段按用户要求实现开发调试直通语义，未加配置开关；算法对照结论为主干已融合，但真实 Calcite RelNode、外部 SMT Solver、真实 RelToSqlConverter、基于真实统计的代价模型仍是静态替代边界，解决方案已在最终答复中列出且未实现。
  - Next step: 后续如要进入生产语义，应增加 dev-only 配置开关并把 RelNode/SMT/RelToSql/统计代价接入真实引擎验证。

### USER-CN-PAGE-REWRITE-RECOMMENDATION-TEST01-20260521: 轻量适配页面展示改写推荐并用 test01 验证

- Status: done
- Completed at: 2026-05-21
- Commit subject: `USER-CN-PAGE-REWRITE-RECOMMENDATION-TEST01-20260521 adapt rewrite validation page for test01 recommendation`
- Priority: 1
- Depends on: USER-CN-REWRITE-ALGORITHM-CONFORMANCE-TEST01-20260521
- Scope: 轻量改造前端页面以展示 SQL 改写推荐输出，从页面输入 docs/test01.sql 内容进行端到端测试，截图分析是否成功推荐正确 SQL。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-PAGE-REWRITE-RECOMMENDATION-TEST01-20260521`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 轻量适配 SQL 改写验证页面，展示后端推荐报告、算法链路状态、置信度、风险、扫描减少和净增报表候选 SQL 形态；后端任务建议追加推荐报告、选中推荐和算法一致性 artifact；归档原始需求和产品边界说明。
  - Validation evidence: JDK 8u112 confirmed via java -version; npm run lint -- --quiet; npm run build; npm run test:frontend-page-governance; mvn -pl sql-optimization checkstyle:check -Dcheckstyle.includes=**/application/service/SqlOptimizationPipelineService.java,**/application/service/SqlOptimizationPipelineServiceTest.java; mvn -pl sql-optimization -am -Dtest=SqlOptimizationPipelineServiceTest#shouldAnalyzeYonghongProductionReportSqlAndRecommendGovernedRewriteShapes,OptimizationTaskControllerTest#shouldSubmitTaskAndPollSucceededStatus -Dsurefire.failIfNoSpecifiedTests=false test; mvn -pl sql-optimization -am test; Playwright page smoke input docs/test01.sql returned SUCCEEDED and screenshots runtime/screenshots/test01-rewrite-validation-core.png plus runtime/screenshots/test01-rewrite-validation-sql.png show recommendation status and all required SQL shape checks pass; python3 scripts/foreman.py validate USER-CN-PAGE-REWRITE-RECOMMENDATION-TEST01-20260521; python3 scripts/task_audit.py --check --phase pre-closeout.
  - Residual risk: 本阶段仍是静态改写推荐和页面试算验证；未执行真实 SQL 结果集比对，未接入生产治理，真实 Calcite/SMT/RelToSql 能力仍沿用上一阶段的静态 surrogate 边界。
  - Next step: 后续若进入生产闭环，应接入抽样结果比对和真实治理审批，再开放自动应用。

### USER-CN-REWRITE-ALGORITHM-CONFORMANCE-TEST01-20260521: 实现改写核心算法链路一致性报告并复测 test01

- Status: done
- Completed at: 2026-05-21
- Commit subject: `USER-CN-REWRITE-ALGORITHM-CONFORMANCE-TEST01-20260521 implement rewrite algorithm conformance report`
- Priority: 1
- Depends on: USER-CN-REWRITE-RECOMMENDATION-FINAL-OUTPUT-PHASE6-20260521
- Scope: 在不改动页面、不执行真实 SQL、不自动应用生产改写的前提下，基于用户给定的解析、分解、识别、变换、验证、择优、生成、输出核心算法脉络，为 sql-optimization 改写核心新增算法链路一致性报告：逐段汇总 Calcite / legacy parser 双栈融合、QBDAG/结构哈希、规则库命中、关系代数候选、语义等价/SMT 边界、帕累托代价择优、SQL 生成和最终推荐输出状态；接入 RewriteCoreIrSnapshot 和 service，补充 docs/test01.sql 复测用例、架构文档和原始需求归档。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-ALGORITHM-CONFORMANCE-TEST01-20260521`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 实现改写核心算法链路一致性报告：覆盖解析双栈融合、查询块 DAG/结构哈希、规则识别、关系代数改写、等价验证、帕累托代价择优、SQL/推荐报告生成和最终输出束；将报告接入 SqlOptimizationPipelineService 与 RewriteCoreIrSnapshot；补充 docs/test01.sql 回归测试和架构文档说明。
  - Validation evidence: JDK: openjdk 1.8.0_112; targeted docs/test01.sql regression: mvn -pl sql-optimization -am -Dtest=SqlOptimizationPipelineServiceTest#shouldAssessCoreAlgorithmConformanceForDocsTest01Sql,SqlOptimizationPipelineServiceTest#shouldExposeAlgorithmConformanceThroughRewriteCoreIrSnapshot -Dsurefire.failIfNoSpecifiedTests=false test passed (2 tests); service regression: mvn -pl sql-optimization -am -Dtest=SqlOptimizationPipelineServiceTest -Dsurefire.failIfNoSpecifiedTests=false test passed (37 tests); full module regression: mvn -pl sql-optimization -am test passed (348 tests); targeted checkstyle passed; git diff --check passed; foreman validate passed; task_audit pre-closeout passed; repository knowledge lint passed; changed developer copy language check passed.
  - Residual risk: 当前为静态 IR/结构哈希/统计等价代理实现；真实 Calcite RelNode 构建、外部 SMT Solver 和真实 RelToSqlConverter 仍作为报告中的关键差距暴露；未执行真实 SQL，不做生产自动应用。
  - Next step: 后续可在独立任务中接入真实 Calcite RelNode/SMT/RelToSql，并补充抽样数据对比执行链。

### USER-CN-REWRITE-RECOMMENDATION-FINAL-OUTPUT-PHASE6-20260521: 实现 SQL 改写最终推荐生成与排序

- Status: done
- Completed at: 2026-05-21
- Commit subject: `USER-CN-REWRITE-RECOMMENDATION-FINAL-OUTPUT-PHASE6-20260521 implement final rewrite recommendations`
- Priority: 1
- Depends on: USER-CN-PARSER-STACK-HETU-ADAPTER-PHASE5-20260521
- Scope: 在不改动页面、不执行真实 SQL、不自动应用生产改写的前提下，在 sql-optimization 改写核心实现 RewriteRecommendation 最终输出第一版：基于关系代数候选、语义等价验证、代价排序、规则冲突消解和双解析栈/Hetu 适配生成开发者可读推荐项，覆盖 rewrite_id、confidence、category、severity、before/after summary、transformations、equivalence_proof、performance、executable_sql 占位，以及金融级排序 Score=0.4*performance_gain+0.3*confidence+0.2*(1-risk)+0.1*readability；接入 RewriteCoreIrSnapshot，补充测试、架构文档和原始需求归档。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-RECOMMENDATION-FINAL-OUTPUT-PHASE6-20260521`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 实现 SQL 改写最终推荐输出第一版：新增 RewriteRecommendationReport、RewriteRecommendation、transformations、equivalence proof、performance estimate 和金融级排序生成器；基于关系代数候选、语义等价验证、抽象代价排序、规则冲突消解、双解析栈融合和 Hetu hints 生成开发者可读推荐项；固化 Score=0.4*performance_gain+0.3*confidence+0.2*(1-risk_level)+0.1*readability_improvement、confidence<0.9 提示门禁、COUNT DISTINCT 人工审核门禁和时间窗口报表抽样比对门禁；接入 SqlOptimizationPipelineService 与 RewriteCoreIrSnapshot；补充回归测试、架构文档和原始需求归档；未改动页面、未执行真实 SQL、未自动应用生产改写。
  - Validation evidence: java -version = OpenJDK 1.8.0_112; mvn -pl sql-optimization -am -Dtest=SqlOptimizationPipelineServiceTest -Dsurefire.failIfNoSpecifiedTests=false test => 35 tests passed; mvn -pl sql-optimization -am test => 346 tests passed; mvn -pl sql-optimization checkstyle:check -Dcheckstyle.includes changed Java paths => 0 violations; git diff --check; python3 scripts/foreman.py validate USER-CN-REWRITE-RECOMMENDATION-FINAL-OUTPUT-PHASE6-20260521; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js; node scripts/check-developer-copy-language.mjs --changed.
  - Residual risk: 当前 executableSql 是基于 repo 内 RelNode surrogate 的静态 WITH 模板，并明确标记未调用真实 Calcite RelToSqlConverter；性能与置信度仍来自静态估算和语义验证报告，不是 Hetu EXPLAIN、真实结果 diff、真实扫描字节或生产可执行证明；所有推荐 autoApplyAllowed=false，仍需后续真实 RelToSql、Hetu 方言验证、结果比对、人工审核和治理链。
  - Next step: 后续可在单独任务中接入真实 Calcite RelToSqlConverter、schema/type catalog、Hetu 方言 SQL 验证和受控结果 diff 证据，再把已验证推荐纳入生产候选链。

### USER-CN-PARSER-STACK-HETU-ADAPTER-PHASE5-20260521: 实现双解析栈融合与 Hetu 计划适配

- Status: done
- Completed at: 2026-05-21
- Commit subject: `USER-CN-PARSER-STACK-HETU-ADAPTER-PHASE5-20260521 implement parser stack fusion hetu adapter`
- Priority: 1
- Depends on: USER-CN-REWRITE-RULE-ENGINE-DSL-CONFLICT-20260521
- Scope: 在不改动页面、不执行真实 SQL、不自动应用生产改写的前提下，在 sql-optimization 改写核心实现 Calcite / legacy parser 双解析栈协同报告与 Hetu 执行计划适配第一版：固化 Calcite 与 legacy parser 分工、legacy parser 方言/BI 工具模式标签、Calcite L1-L4/RelNode/Planner 集成占位证据、融合层改写约束注入，以及 Hetu CTE 物化、Dynamic Filter、分区裁剪和两阶段分布式聚合适配建议；接入 RewriteCoreIrSnapshot，补充测试、架构文档和原始需求归档。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-PARSER-STACK-HETU-ADAPTER-PHASE5-20260521`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 实现 Calcite / legacy parser 双解析栈融合与 Hetu 静态计划适配第一版：新增 parser stack fusion domain model/analyzer，固化 Calcite L1-L4/RelNode/Planner 占位证据、legacy parser 方言/BI 工具模式标签、融合层改写约束注入，以及 Hetu MATERIALIZED CTE、dynamic_filter、时间分区裁剪和两阶段聚合 hints；接入 SqlOptimizationPipelineService 与 RewriteCoreIrSnapshot；补充回归测试、架构文档和原始需求归档；未改动页面、未执行真实 SQL、未自动应用生产改写。
  - Validation evidence: java -version = OpenJDK 1.8.0_112; mvn -pl sql-optimization -am -Dtest=SqlOptimizationPipelineServiceTest -Dsurefire.failIfNoSpecifiedTests=false test => 32 tests passed; mvn -pl sql-optimization -am test => 343 tests passed; mvn -pl sql-optimization checkstyle:check -Dcheckstyle.includes changed Java paths => 0 violations; git diff --check; python3 scripts/foreman.py validate USER-CN-PARSER-STACK-HETU-ADAPTER-PHASE5-20260521; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js; node scripts/check-developer-copy-language.mjs --changed.
  - Residual risk: 当前 Calcite RelNode/HepPlanner/VolcanoPlanner 为静态融合报告和规划占位证据，不执行真实 SQL、不读取真实 schema/统计信息、不生成生产自动改写绑定；Hetu 适配为静态 hint/约束建议，dynamic filter、分区裁剪和两阶段聚合仍需后续接入真实执行计划、压测和治理审批后才能生产生效。
  - Next step: 后续可在单独任务中接入真实 Calcite RelNode 构建、legacy parser AST 精准定位、Hetu EXPLAIN/统计信息采集和受控 rewritten SQL 生成。

### USER-CN-REWRITE-RULE-ENGINE-DSL-CONFLICT-20260521: 实现改写规则 DSL 与冲突消解

- Status: done
- Completed at: 2026-05-21
- Commit subject: `USER-CN-REWRITE-RULE-ENGINE-DSL-CONFLICT-20260521 implement rewrite rule engine dsl`
- Priority: 1
- Depends on: USER-CN-COST-BASED-REWRITE-PHASE4-20260521
- Scope: 在不改动页面、不执行真实 SQL、不自动应用生产改写的前提下，在 sql-optimization 改写核心实现规则描述 DSL 和规则冲突消解第一版：固化 CSE-DEDUP-001 规则模板、规则前置条件/动作/验证/代价影响结构，构建规则依赖图并支持互斥、顺序依赖、代价矛盾检测，对有环/互斥场景输出宽度 3-5 的局部搜索/Beam Search 排序报告；接入 RewriteCoreIrSnapshot，补充测试、架构文档和原始需求归档。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-RULE-ENGINE-DSL-CONFLICT-20260521`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 实现 SQL 改写规则引擎关键数据结构：新增静态规则 DSL 模型与默认规则目录，固化 CSE-DEDUP-001 模板、前置条件、动作、验证和代价影响；基于关系代数候选与代价报告生成规则依赖图，检测顺序依赖、互斥改写和扫描/内存代价矛盾，并在互斥场景下输出 Beam Search 宽度 4 的规则子集排序报告；接入 SqlOptimizationPipelineService 与 RewriteCoreIrSnapshot；补充测试、架构文档和原始需求归档；未改动页面、未执行真实 SQL、未自动应用生产改写。
  - Validation evidence: java -version = 1.8.0_112; mvn -pl sql-optimization -am -Dtest=SqlOptimizationPipelineServiceTest -Dsurefire.failIfNoSpecifiedTests=false test; mvn -pl sql-optimization -am test; mvn -pl sql-optimization checkstyle:check -Dcheckstyle.includes='**/domain/rewrite/rule/*.java,**/domain/rewrite/ir/RewriteCoreIrAssembler.java,**/domain/rewrite/ir/RewriteCoreIrSnapshot.java,**/application/service/SqlOptimizationPipelineService.java,**/application/service/SqlOptimizationPipelineServiceTest.java'; git diff --check; python3 scripts/foreman.py validate USER-CN-REWRITE-RULE-ENGINE-DSL-CONFLICT-20260521; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js; node scripts/check-developer-copy-language.mjs --changed
  - Residual risk: 当前规则 DSL 是代码内静态目录，不是动态配置中心；SMT_SOLVER 仍为声明式 fallback，未接入 solver；Beam Search 输出规则排序建议，不生成完整 rewritten SQL，不绕过语义验证、人工复核、压测和生产治理链。
  - Next step: 后续可在保持治理链的前提下继续扩展更多规则模板、规则版本发布和真实约束/solver 接入。

### USER-CN-COST-BASED-REWRITE-PHASE4-20260521: 实现 SQL 改写第四阶段代价模型与排序

- Status: done
- Completed at: 2026-05-21
- Commit subject: `USER-CN-COST-BASED-REWRITE-PHASE4-20260521 implement cost based rewrite selection`
- Priority: 1
- Depends on: USER-CN-SEMANTIC-EQUIVALENCE-PHASE3-20260521
- Scope: 在不改动页面、不执行真实 SQL、不自动生效生产改写的前提下，在 sql-optimization 改写核心实现第四阶段 Cost-Based Rewriting Selection 第一版：基于关系代数改写候选和语义等价验证报告生成抽象 Hetu/Presto 解耦代价估算，覆盖 Scan/Shuffle/Compute/Memory 四维代价、Hetu shuffle/CTE/dynamic filter 调整、多目标 Pareto 前沿筛选和 SLA 策略排序；接入 RewriteCoreIrSnapshot，补充测试、架构文档和原始需求归档。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-COST-BASED-REWRITE-PHASE4-20260521`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented Phase 2.4 cost-based rewriting selection in sql-optimization: added abstract cost domain model and selector, pipeline entry, RewriteCoreIrSnapshot integration, focused tests, raw requirement archive, and architecture/truth documentation updates.
  - Validation evidence: java -version => OpenJDK 1.8.0_112; mvn -pl sql-optimization -am -Dtest=SqlOptimizationPipelineServiceTest -Dsurefire.failIfNoSpecifiedTests=false test => BUILD SUCCESS with 28 SqlOptimizationPipelineServiceTest tests passed; mvn -pl sql-optimization -am test => BUILD SUCCESS with sql-optimization reporting 339 tests passed; python3 scripts/foreman.py validate USER-CN-COST-BASED-REWRITE-PHASE4-20260521 => passed; python3 scripts/task_audit.py --check --phase pre-closeout => passed; git diff --check => passed.
  - Residual risk: Cost values remain static abstract estimates, not Hetu EXPLAIN cost, real scan bytes, real shuffle bytes, or real memory usage; selectedCandidateId is ranking evidence only and does not auto-create runtime rewrite binding.
  - Next step: Future precision work can add governed table statistics or Hetu EXPLAIN evidence as a separate task while preserving the current no-SQL-execution boundary.

### USER-CN-SEMANTIC-EQUIVALENCE-PHASE3-20260521: 实现 SQL 改写第三阶段语义等价验证

- Status: done
- Completed at: 2026-05-21
- Commit subject: `USER-CN-SEMANTIC-EQUIVALENCE-PHASE3-20260521 implement semantic equivalence verification`
- Priority: 1
- Depends on: USER-CN-RA-REWRITE-PHASE2-20260521
- Scope: 在不改动页面、不执行真实 SQL、不自动生效生产改写的前提下，在 sql-optimization 改写核心实现第三阶段 Semantic Equivalence Verification 第一版：基于 L4/QBDAG/RA rewrite candidates 生成约束等价验证与聚合统计等价验证结果，覆盖差查询空结果判定、NULL 语义风险、COUNT DISTINCT CASE/FILTER 等价规则、验证状态和证据；接入 RewriteCoreIrSnapshot，补充测试、架构文档和原始需求归档。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-SEMANTIC-EQUIVALENCE-PHASE3-20260521`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 实现 SQL 改写第三阶段候选级语义等价验证：新增 SemanticEquivalenceReport/Check/Verifier，支持基于约束的差表达式空结果义务、NULL 与 bag 语义证明义务、COUNT DISTINCT CASE/FILTER 聚合统计等价检查；接入 SqlOptimizationPipelineService 与 RewriteCoreIrSnapshot；补充回归测试、架构文档和原始需求归档；未改动页面。
  - Validation evidence: java -version => OpenJDK 1.8.0_112; mvn -pl sql-optimization -am test -DfailIfNoTests=false => 336 tests passed; mvn -pl sql-optimization -Dtest=SqlOptimizationPipelineServiceTest -DfailIfNoTests=false test => 25 tests passed; mvn -pl sql-optimization checkstyle:check -Dcheckstyle.includes=changed Java paths => 0 violations; mvn -pl sql-optimization pmd:pmd => BUILD SUCCESS with existing P3C PMD 7 compatibility warnings; git diff --check => passed; node scripts/check-developer-copy-language.mjs --changed => passed; node scripts/lint-repository-knowledge.js => passed; python3 scripts/foreman.py validate USER-CN-SEMANTIC-EQUIVALENCE-PHASE3-20260521 => passed; python3 scripts/task_audit.py --check --phase pre-closeout => passed.
  - Residual risk: 本阶段为候选级静态验证报告，不执行真实 SQL、不接入真实 schema 约束、不调用 SMT Solver/Z3；CONDITIONALLY_PROVED 或 NEEDS_CONSTRAINTS 不能升级为生产自动改写。PMD 输出存在既有 P3C 规则集 PMD 7 兼容性 warning，命令本身成功。
  - Next step: 后续可接入 schema 约束抽取、SMT-LIB/Z3 求解器和完整 rewritten SQL 生成器，再把 PROVED 候选纳入受控自动改写链路。

### USER-CN-RA-REWRITE-PHASE2-20260521: 实现 SQL 改写第二阶段关系代数等价变换候选

- Status: done
- Completed at: 2026-05-21
- Commit subject: `USER-CN-RA-REWRITE-PHASE2-20260521 implement relational algebra rewrite candidates`
- Priority: 1
- Depends on: USER-CN-QBDAG-DECOMPOSITION-PHASE1-20260521
- Scope: 在不改动页面、不执行真实 SQL、不自动生效生产改写的前提下，在 sql-optimization 改写核心 L4 层实现 Relational Algebra Rewriting 第一版：基于 QBDAG 和结构哈希识别公共子表达式消除/子查询去重、纵向折叠多指标聚合合并、横向展开消除 LEFT JOIN 聚合序列，并输出等价高效形式的静态 rewrite candidate、补偿谓词和约束/风险证据；补充测试、架构文档和原始需求归档。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-RA-REWRITE-PHASE2-20260521`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 实现 L4 关系代数改写静态候选：CSE 消除、纵向折叠和横向展开消除；接入 SqlOptimizationPipelineService 与 RewriteCoreIrSnapshot；补充 QBDAG 聚合 LEFT JOIN 回归测试、架构文档和原始需求归档；未改动页面。
  - Validation evidence: java -version => OpenJDK 1.8.0_112; mvn -pl sql-optimization -am test -DfailIfNoTests=false => 334 tests passed; mvn -pl sql-optimization checkstyle:check -Dcheckstyle.includes=changed Java paths => 0 violations; mvn -pl sql-optimization pmd:pmd => BUILD SUCCESS with existing P3C PMD 7 compatibility warnings; git diff --check => passed; node scripts/check-developer-copy-language.mjs --changed => passed; node scripts/lint-repository-knowledge.js => passed; python3 scripts/foreman.py validate USER-CN-RA-REWRITE-PHASE2-20260521 => passed; python3 scripts/task_audit.py --check --phase pre-closeout => passed.
  - Residual risk: 本阶段只生成 manualReviewRequired=true、autoApplyAllowed=false 的静态候选；谓词包含、COUNT DISTINCT 参数等价、AVG 拆解和真实语义等价仍需后续 2.3.2 验证后才能进入自动改写。PMD 输出存在既有 P3C 规则集 PMD 7 兼容性 warning，命令本身成功。
  - Next step: 后续阶段实现 2.3.2 语义等价验证与候选到可执行改写的受控桥接。

### USER-CN-QBDAG-DECOMPOSITION-PHASE1-20260521: 实现 SQL 改写第一阶段 QBDAG 分解与结构哈希

- Status: done
- Completed at: 2026-05-21
- Commit subject: `feat(rewrite): implement query block dag decomposition`
- Priority: 1
- Depends on: USER-CN-REWRITE-CORE-IR-SCAFFOLD-20260521
- Scope: 在不改动页面、不执行真实 SQL、不改变运行时自动生效语义的前提下，在 sql-optimization 改写核心中实现第一阶段 Query Block Decomposition：识别查询块边界、构建查询块 DAG、提取外部引用、结构哈希去重等价块、记录环检测与 lateral join 提升建议，并补充最小单元测试、架构文档和原始需求归档。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-QBDAG-DECOMPOSITION-PHASE1-20260521`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 实现 SQL 改写第一阶段 Query Block Decomposition：新增 QBDAG 领域模型与 Calcite AST 分解主路径，支持 advancedStructureProfile 降级、查询块边界识别、外部引用、引用图、拓扑排序、关联子查询 lateral join 提升信号、结构哈希等价块分组；接入 RewriteCoreIrSnapshot 和 SqlOptimizationPipelineService；补充测试、架构文档和原始需求归档；未改动页面。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-QBDAG-DECOMPOSITION-PHASE1-20260521; python3 scripts/task_audit.py --check --phase pre-closeout; java -version = 1.8.0_112; mvn -pl sql-optimization -am test -DfailIfNoTests=false; mvn -pl sql-optimization -am -Dtest=SqlOptimizationPipelineServiceTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test; git diff --check; node scripts/check-developer-copy-language.mjs --changed; node scripts/lint-repository-knowledge.js; mvn -pl sql-optimization checkstyle:check -Dcheckstyle.includes='**/domain/rewrite/qbdag/*.java,**/domain/rewrite/ir/RewriteCoreIrAssembler.java,**/domain/rewrite/ir/RewriteCoreIrSnapshot.java,**/application/service/SqlOptimizationPipelineService.java,**/application/service/SqlOptimizationPipelineServiceTest.java'
  - Residual risk: 本阶段只产出静态 QBDAG 与结构哈希候选信号，不执行真实 SQL、不自动合并子查询、不证明语义等价；全量 mvn -pl sql-optimization -am validate pmd:pmd checkstyle:check 受 query-execution 与 sql-optimization 存量未使用 import / 中文测试成员名 checkstyle 问题阻断，本次改动路径的 checkstyle 已通过。
  - Next step: 基于 QBDAG duplicateStructuralGroups 接入子查询合并 rewrite rule，并按 2.3.2 增加语义等价验证。

### USER-CN-REWRITE-CORE-IR-SCAFFOLD-20260521: 搭建 SQL 改写 L1-L5 核心 IR 架构骨架

- Status: done
- Completed at: 2026-05-21
- Commit subject: `feat(rewrite): scaffold core IR architecture`
- Priority: 1
- Depends on: USER-CN-GENERALIZE-REPORT-REWRITE-RECOMMENDATION-20260521
- Scope: 在不改动页面、不改变运行时自动生效语义、不执行真实 SQL 的前提下，为 sql-optimization 改写核心搭建 L1 AST、L2 Table Reference、L3 Query Block、L4 Relational Algebra、L5 Business Intent 五层 IR 骨架、转换入口和最小测试；如发现与既有 HARN-130 L0/L1/L2 规则模型或 L2 物化视图命名冲突，先以兼容命名和文档说明收口。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-CORE-IR-SCAFFOLD-20260521`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 搭建 SQL 改写 L1-L5 核心 IR 骨架：L1 AST、L2 表引用、L3 查询块、L4 关系代数、L5 业务意图；新增 assembler、pipeline 入口、最小单测、架构文档和原始需求归档；未改动页面。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-REWRITE-CORE-IR-SCAFFOLD-20260521; python3 scripts/task_audit.py --check --phase pre-closeout; mvn -pl sql-optimization -am -Dtest=SqlOptimizationPipelineServiceTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test; git diff --check; node scripts/check-developer-copy-language.mjs --changed; node scripts/lint-repository-knowledge.js
  - Residual risk: 本次仅搭建后端 IR 骨架和转换入口，不改变现有推荐/页面运行路径；后续需按具体改写规则补齐真实代数优化、查询块归一化和业务意图识别。
  - Next step: 在该 IR 骨架上接入具体 SQL 改写规则和可验证的 rewrite plan 输出。

### USER-CN-GENERALIZE-REPORT-REWRITE-RECOMMENDATION-20260521: 通用化复杂报表改写推荐

- Status: done
- Completed at: 2026-05-21
- Commit subject: `feat(sql-optimization): generalize report rewrite detection`
- Priority: 1
- Depends on: N/A
- Scope: 将 docs/test01.sql 的报表改写能力抽象为更通用的复杂报表 SQL 推荐逻辑，覆盖类似重复扫描事实表、日期快照、客户粒度、机构层级和分段指标的 SQL 形态，减少样例特化判断并补充变体回归验证。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-GENERALIZE-REPORT-REWRITE-RECOMMENDATION-20260521`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 将 test01 的复杂报表改写从样例特化扩展为字段命名族识别：支持 ORG_CODE/ORG_CD/ORG_ID 等层级编码、ORG_NAME/SHORT_NAME/SNAM 等机构名称、ORG_LEVEL、CUSTOMER_ID/CLIENT_ID、AVG_BALANCE/BIZ_DATE 等命名变体；候选列按物理字段优先评分，避免外层别名误入 raw CTE；MV 粒度只输出实际检测到的机构层级；补充同形不同命名 SQL 变体回归，验证 L2 规则、rewrite SQL、MV DDL 和无 GROUPING SETS。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-GENERALIZE-REPORT-REWRITE-RECOMMENDATION-20260521 --include-task-audit --extra-command mvn -pl sql-optimization -am test --extra-command git diff --check；git diff --check；python3 scripts/task_audit.py --check --phase pre-closeout；同轮集中验证覆盖 SqlOptimizationPipelineServiceTest、RewriteTrialApplicationServiceTest、L2SnapshotAggregateReportMvCandidateGeneratorTest。
  - Residual risk: 未连接真实 Hetu/生产数据执行 EXPLAIN 与结果差异 SQL；当前覆盖常见命名族和同形报表模式，任意业务语义仍需依赖生成的 validationSql、EXPLAIN 和生产样本回归确认。
  - Next step: 将更多生产同形报表补充为 fixture，并在真实引擎执行生成的 validationSql 与 EXPLAIN，持续扩展命名族识别。

### USER-CN-FIX-TEST01-RECOMMENDATION-RECORD-20260521: 修复 test01 改写验证推荐记录生成

- Status: done
- Completed at: 2026-05-21
- Commit subject: `fix(sql-optimization): restore test01 rewrite trial recommendations`
- Priority: 1
- Depends on: N/A
- Scope: 排查并修复 docs/test01.sql 在 SQL 改写验证/推荐链路中无法生成推荐记录的问题，确保复杂报表 SQL 的推荐 SQL、加速 artifact 和可验证推荐记录能够稳定生成，同时补充回归测试并保持治理边界。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-FIX-TEST01-RECOMMENDATION-RECORD-20260521`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 修复 docs/test01.sql 在改写验证中无法生成推荐记录的问题：将 REPORT_REPEATED_SCAN_TO_SNAPSHOT_AGG 纳入候选推荐问题集合并保持 L2 规则层级；将报表快照聚合 SQL 从 GROUPING SETS 改为两个普通 GROUP BY 分支 UNION ALL 等价合并；补充 docs/test01.sql 回归，验证推荐记录、候选 SQL、深圳市分行/org 标签和无 GROUPING SETS。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-FIX-TEST01-RECOMMENDATION-RECORD-20260521 --include-task-audit --extra-command "mvn -pl sql-optimization -am test" --extra-command "git diff --check"；mvn -pl sql-optimization -am -Dtest=RewriteTrialApplicationServiceTest,SqlOptimizationPipelineServiceTest,L2SnapshotAggregateReportMvCandidateGeneratorTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test；mvn -pl sql-optimization -am test（328 tests）；git diff --check；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 未连接真实 Hetu/生产数据执行 validationSql 和 EXPLAIN，实际结果等价与性能收益仍需在目标环境用推荐记录中的校验 SQL 复核。
  - Next step: 在目标 Hetu 环境执行推荐记录 validationSql 与 EXPLAIN，确认结果 diff 为 0 且计划扫描形态符合预期。

### USER-CN-FIX-SQL-REWRITE-CORRECTNESS-20260521: 修复 test01 SQL 改写等价性与性能

- Status: done
- Completed at: 2026-05-21
- Commit subject: `fix(sql-optimization): correct report SQL rewrite semantics`
- Priority: 1
- Depends on: N/A
- Scope: 针对 docs/test01.sql 当前推荐 SQL 结果条数被压缩、指标不等价且仍耗时较长的问题，重新分析原 SQL 语义，修复改写与 MV 推荐逻辑，补充至少三类验证方法和回归测试，保持治理边界与运行时激活链路不自动绕过。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-FIX-SQL-REWRITE-CORRECTNESS-20260521`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 修复 docs/test01.sql 推荐改写的等价性问题：改写结果改为基期100锚点行集，保留深圳市分行及下属机构标签，按原 SQL 语义拆分普通分段指标与新增指标；MV 推荐改为带机构路径过滤能力的报表机构-客户-日期快照，并补充逐键指标、机构标签、锚点行集和计划形态验证。
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=SqlOptimizationPipelineServiceTest,L2SnapshotAggregateReportMvCandidateGeneratorTest,L2MaterializedViewLargeSqlQualityTest,L2CommonSubgraphMvCandidateGeneratorTest,L2GrainMeasureDeriverTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test passed 33 tests; mvn -pl sql-optimization -am test passed 327 tests; python3 scripts/foreman.py validate USER-CN-FIX-SQL-REWRITE-CORRECTNESS-20260521 --include-task-audit --extra-command 'mvn -pl sql-optimization -am test' --extra-command 'git diff --check' passed; python3 scripts/task_audit.py --check --phase pre-closeout passed.
  - Residual risk: 本轮为仓库内静态 SQL 生成与语义夹具验证，未连接真实 Hetu/MRS 执行 original/rewrite validationSql、EXPLAIN 或生产 MV refresh；实际 70s 剩余耗时需在目标环境用新 MV rewrite 再测。
  - Next step: 在真实 Hetu/MRS 环境执行新 artifact 的 validationSql，确认 RESULT_SET_EXCEPT_DIFF、ANCHOR_KEY_SET_DIFF、ORG_LABEL_SET_DIFF、METRIC_BY_KEY_DIFF 均为 0，再用 EXPLAIN/运行时指标比较原 SQL、standalone rewrite 和 MV rewrite 的扫描量与耗时。

### USER-CN-DEEP-SQL-REWRITE-MV-20260521: 增强复杂 SQL 改写与高级 MV 推荐

- Status: done
- Completed at: 2026-05-21
- Commit subject: `fix(sql-optimization): deepen report SQL rewrite MV recommendation`
- Priority: 1
- Depends on: N/A
- Scope: 基于 doc/test01.sql 的中度复杂 SQL，增强 sql-optimization 的解析、保守改写、合理改写结果输出与高级物化视图推荐能力；补充至少三类合理性验证证据与测试，不执行生产 DDL 或绕过改写记录激活链路。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-DEEP-SQL-REWRITE-MV-20260521`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 为 docs/test01.sql 这类重复快照聚合报表新增客户-日期粒度快照改写，输出可替换 rewrite SQL、参数化聚合 MV artifact、MV rewrite SQL 和至少三类验证方法，同时保持 PULL_ONLY 与 runtime binding 未自动创建边界。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-DEEP-SQL-REWRITE-MV-20260521 --include-task-audit --extra-command 'mvn -pl sql-optimization -am test' --extra-command 'git diff --check' passed; python3 scripts/task_audit.py --check --phase pre-closeout passed
  - Residual risk: 本轮验证为 repo-closed 静态解析、SQL 生成与单元/模块回归；未执行真实 Hetu/MRS EXPLAIN、真实 MV DDL/refresh 或生产 runtime rewrite activation。
  - Next step: 在具备真实 Hetu/MRS 环境后，用生成的 validationSql 与 EXPLAIN 对 docs/test01.sql 做环境留证，再按改写记录激活链路进入运行时。

### USER-CN-PROD-SQL-PARSE-REWRITE-20260521: 适配永洪中度复杂报表 SQL 的解析与推荐改写

- Status: done
- Completed at: 2026-05-21
- Commit subject: `fix(sql-optimization): USER-CN-PROD-SQL-PARSE-REWRITE-20260521 parse Yonghong report SQL`
- Priority: 1
- Depends on: HARN-142,USER-CN-MV-REWRITE-LARGE-SQL-QUALITY-20260520
- Scope: 严格限定本轮输入的永洪百万客户净增报表 SQL 形态：优化 sql-optimization 对多层派生表、重复同源扫描、中文双引号别名、OR 机构层级过滤、两日期 AUM 分段聚合、COUNT DISTINCT 与增长率表达式的解析、问题优先级排序、推荐 SQL 改写和 L2 MV/公共子图建议；不得扩大到真实外部 SQL 执行、生产 DDL/refresh、runtime binding 自动激活、前端页面重构或无关 SQL 类型。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-PROD-SQL-PARSE-REWRITE-20260521`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-21: added Yonghong report SQL normalization for redundant derived-table join wrappers, recognized `DTE` date predicates, and kept `docs/test01.sql` as the repository regression fixture.
  - 2026-05-21: covered the production report fixture with parser, recommendation rewrite, and L2 common-subgraph MV assertions; Foreman validate passed with the focused `sql-optimization` Maven regression suite.
- Context closeout:
  - Completed scope: 完成永洪百万客户净增报表 SQL 的保守解析归一化、DTE 日期谓词识别、推荐改写与 L2 公共子图 MV 回归覆盖，并将 docs/test01.sql 保留为代码库内 fixture。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-PROD-SQL-PARSE-REWRITE-20260521 --include-task-audit --extra-command 'mvn -pl sql-optimization -am -Dtest=SqlOptimizationPipelineServiceTest,L2MaterializedViewLargeSqlQualityTest,L2CommonSubgraphMvCandidateGeneratorTest,L2GrainMeasureDeriverTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test' passed.
  - Residual risk: 本轮仅覆盖 repo-closed 解析、推荐与 MV artifact 建议；不声明外部 SQL 执行、生产 DDL/refresh 或 runtime binding 自动激活完成。
  - Next step: 无仓库内阻塞下一步；后续生产执行证据、DDL/refresh 与自动激活仍按独立任务治理。

### USER-CN-FIX-JDBC-DRIVER-UPLOAD-SCHEMA-BOOTSTRAP-20260521: Fix JDBC driver upload schema bootstrap on reused environments

- Status: done
- Completed at: 2026-05-21
- Commit subject: `fix(governance): harden JDBC driver upload runtime`
- Priority: 1
- Depends on: N/A
- Scope: Diagnose the /api/governance/datasource-drivers 500 seen on another machine, harden local/dev governance startup so uploaded-driver schema is bootstrapped without relying on one specific script path, and surface clearer runtime behavior for reused legacy MySQL volumes.
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-FIX-JDBC-DRIVER-UPLOAD-SCHEMA-BOOTSTRAP-20260521`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Hardened governance JDBC driver upload runtime for reused environments by binding multipart upload size through DataSize-backed configuration, returning clearer upload-limit errors, and pinning governance log encoders to UTF-8 so dynamically loaded driver messages do not degrade into square-box placeholders.
  - Validation evidence: mvn -q -pl governance -am -Dtest=LoggingConfigContractTest,DatasourceDriverArtifactControllerTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate USER-CN-FIX-JDBC-DRIVER-UPLOAD-SCHEMA-BOOTSTRAP-20260521; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The repository validation confirms configuration and controller behavior, but a live restarted governance process with the target uploaded driver should still be checked on the affected machine to verify the full terminal/container log pipeline is also UTF-8.
  - Next step: Restart governance in the affected environment, re-upload or retest the target JDBC driver, and confirm both application.log and the runtime console no longer render square-box placeholders.

### USER-CN-DIST-PORTABLE-UPDATE-20260521: 按最新页面全量更新 dist-portable

- Status: done
- Completed at: 2026-05-21
- Commit subject: `build(portable): refresh dist-portable package`
- Priority: 1
- Depends on: USER-CN-SYSTEM-DRIVER-UI-FIX-20260521
- Scope: Use the existing portable frontend build entrypoint to fully regenerate the tracked dist-portable package from the latest committed source, refresh hashed assets/startup files as needed, and verify the packaged frontend still passes the portable smoke path without changing business behavior.
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-DIST-PORTABLE-UPDATE-20260521`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Regenerated the tracked dist-portable package from the latest committed frontend source, refreshed index.html asset references, and replaced stale hashed portable assets with the new portable build output without changing application behavior.
  - Validation evidence: npm run build:portable; npm run smoke:portable-frontend; python3 scripts/foreman.py validate USER-CN-DIST-PORTABLE-UPDATE-20260521 --include-task-audit --extra-command 'npm run build:portable' --extra-command 'npm run smoke:portable-frontend' --extra-command 'git diff --check'.
  - Residual risk: dist-portable smoke covers the packaged frontend shell and mock-backed route flow, but it does not validate a live backend environment or destination-host-specific portable-config.json values.
  - Next step: Before using the refreshed package on another machine, update dist-portable/portable-config.json with the target backend host addresses and run start-portable.sh or start-portable.cmd there.

### USER-CN-SYSTEM-DRIVER-UI-FIX-20260521: Fix system JDBC driver upload UI and local dev schema bootstrap

- Status: done
- Completed at: 2026-05-21
- Commit subject: `fix(system): repair JDBC driver upload UI and dev schema bootstrap`
- Priority: 1
- Depends on: USER-CN-TRINO-UPLOADED-DRIVER-MULTI-ENGINE-JDBC-20260521
- Scope: Repair system management JDBC driver upload usability, separate driver inventory from datasource listing, and ensure local startup bootstraps the uploaded-driver schema for existing dev MySQL volumes.
- Validation:
  - `python3 -m py_compile scripts/ensure_system_management_dev_schema.py`
  - `bash -n scripts/local-start.sh scripts/local-start-cn.sh scripts/start-governance-dev.sh`
  - `npm run lint`
  - `npm run build`
  - `npm run test:form-governance`
  - `npm run test:sql-ui-contract`
  - `npm run test:frontend-page-governance`
  - `node scripts/check-developer-copy-language.mjs --changed`
  - `python3 scripts/foreman.py validate USER-CN-SYSTEM-DRIVER-UI-FIX-20260521`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-21: task was prematurely archived before the source changes were committed; moved back to active ledger so validation, commit, and closeout can reflect repository facts.
- Context closeout:
  - Completed scope: Repaired the system-management JDBC driver workflow by moving uploaded-driver inventory into its own tab, restoring native .jar file selection/upload behavior, soft-failing driver inventory fetches, and ensuring local startup upgrades the uploaded-driver schema for existing dev MySQL volumes.
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-SYSTEM-DRIVER-UI-FIX-20260521; python3 scripts/task_audit.py --check --phase pre-closeout.
  - Residual risk: Local runtime smoke against a real reused docker MySQL volume was not rerun in this closeout; the new schema bootstrap path is validated through script checks and frontend/task validation only.
  - Next step: When reusing an existing local docker mysql volume, rerun scripts/local-start.sh or scripts/start-governance-dev.sh once so the uploaded-driver schema upgrade is applied before manual UI verification.

### USER-CN-TRINO-UPLOADED-DRIVER-MULTI-ENGINE-JDBC-20260521: 页面驱动上传与 Trino 优先多引擎 JDBC 支持

- Status: done
- Completed at: 2026-05-21
- Commit subject: `feat(governance): add multi-engine uploaded JDBC driver routing`
- Priority: 1
- Depends on: N/A
- Scope: 系统管理页驱动上传、治理驱动制品注册、共享卷热加载、query-execution 的 TRINO/HETU/HIVE JDBC 路由执行，以及 sql-optimization 的 EXPLAIN/live metadata 复用治理驱动链。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-TRINO-UPLOADED-DRIVER-MULTI-ENGINE-JDBC-20260521`
- Progress log:
  - 2026-05-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implement uploaded JDBC driver governance, TRINO-first multi-engine JDBC routing, query/sql-optimization reuse, frontend system/query integration, schema migrations, and docs updates.
  - Validation evidence: npm run build; node scripts/check-system-datasource-contract.mjs; node scripts/check-query-workbench-contract.mjs; node scripts/check-sql-ui-contract.mjs; npm run test:frontend-page-governance; mvn -q -DskipTests compile; mvn -q -pl governance -am -Dtest=DatasourceConfigApplicationServiceTest,DatasourceConfigControllerTest,SystemManagementConfigControllerTest -Dsurefire.failIfNoSpecifiedTests=false test; mvn -q -pl query-execution -am -Dtest=QueryExecutionApplicationServiceTest,QueryExecutionControllerTest,GovernanceHttpClientTest,ModeRoutingQueryExecutionAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test; mvn -q -pl sql-optimization -am -Dtest=StructureParseControllerTest,StructureParseContractTest,GovernanceHttpClientTest,JdbcHetuPlanAnalysisClientTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate USER-CN-TRINO-UPLOADED-DRIVER-MULTI-ENGINE-JDBC-20260521; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Full-repo lint and the broader Maven matrix from the planning note were not rerun; coverage is limited to compile plus task-relevant targeted checks.
  - Next step: If required, run the broader lint and full Maven matrix under a delivery-oriented follow-up task.

### USER-CN-REWRITE-VALIDATION-ACTIVATION-UX-20260521: 优化推荐 MV 验证激活闭环

- Status: done
- Completed at: 2026-05-20
- Commit subject: `feat(rewrite): expose validation and approval actions`
- Priority: 1
- Depends on: N/A
- Scope: 在推荐结果/改写记录主页面补齐创建验证运行与批准允许自动应用入口，使 SQL 执行或解析自动推荐的 SQL/MV 能在页面内完成验证后激活；补充前端契约检查并保持 runtime binding ACTIVE 安全门禁。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-VALIDATION-ACTIVATION-UX-20260521`
- Progress log:
  - 2026-05-20: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 推荐中心改写生命周期新增创建验证运行与批准允许自动应用入口；后端暴露改写记录 review API；补充契约、i18n 与 MVC 覆盖。
  - Validation evidence: node scripts/check-recommendation-page-contract.mjs；mvn -q -pl sql-optimization -Dtest=SqlRewriteRecordControllerTest test；git diff --check；npm run lint；npm run test:i18n-copy；npm run build；npm run test:frontend-page-governance；python3 scripts/foreman.py validate USER-CN-REWRITE-VALIDATION-ACTIVATION-UX-20260521；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 真实只读 digest 执行仍取决于后端 QueryExecutionResultDigestClient 配置；未配置时沿用现有客户端提交验证记录语义。
  - Next step: 在真实数据源环境跑一次推荐 MV 生成、验证、批准、激活的浏览器链路。

### USER-CN-FIX-REWRITE-ACTIVATE-POLICY-20260521: 修复改写激活授权策略缺失并补充 MV 测试 SQL

- Status: done
- Completed at: 2026-05-20
- Commit subject: `fix(governance): align rewrite activation policy`
- Priority: 1
- Depends on: N/A
- Scope: 修复 SQL_REWRITE_RECORD_ACTIVATE 在治理授权默认策略与配置中缺失导致改写激活按钮返回 OPERATION_POLICY_MISSING；补充授权矩阵回归测试，并整理多类规范 SQL 用于验证 MV 生成场景。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-FIX-REWRITE-ACTIVATE-POLICY-20260521`
- Progress log:
  - 2026-05-20: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 补齐 SQL_REWRITE_RECORD_ACTIVATE 治理授权默认策略与 application.yml 配置，移除旧 publish/unpublish rewrite record operation，并新增授权矩阵回归测试覆盖改写记录激活按钮所需 operation policy。
  - Validation evidence: java -version 确认 1.8.0_112；mvn -q -pl governance -Dtest=GovernanceAuthorizationMatrixApplicationServiceTest test；git diff --check；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/foreman.py validate USER-CN-FIX-REWRITE-ACTIVATE-POLICY-20260521。
  - Residual risk: 未发现仓库内残余风险；受影响环境需部署本次 governance 配置后重试激活。
  - Next step: 在目标环境发布后，用改写记录激活按钮重试原场景，并确认授权响应不再返回 OPERATION_POLICY_MISSING。

### USER-CN-REVERT-RULE-CLEANUP-20260520: 回退指定规则清理改动

- Status: done
- Completed at: 2026-05-20
- Commit subject: `revert(rules): USER-CN-REVERT-RULE-CLEANUP-20260520 revert rule cleanup`
- Priority: 1
- Depends on: N/A
- Scope: 按人类要求回退上一轮 USER-CN-RULE-CLEANUP-R014-R055-R110-R115-R118-R139-20260520 的仓库改动，使用非破坏性 git revert 生成反向提交，保持任务台账、验证和审计链可追溯。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REVERT-RULE-CLEANUP-20260520`
- Progress log:
  - 2026-05-20: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 按人类要求回退上一轮规则清理 commit 8f0ec56c，使用 git revert --no-commit 生成反向改动；移除 R-192、INBOX-008、规则清理影响分析和原始需求归档，并恢复 R-014、R-055、R-110、R-118、R-139、合规脚本与相关文档到回退前口径。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-REVERT-RULE-CLEANUP-20260520 通过；node scripts/lint-repository-knowledge.js 通过并显示规则连续性回到 R-001 至 R-190；python3 scripts/verify_compliance_baseline.py 通过；bash scripts/run-phase-gates.sh --gate compliance 通过；git diff --check 和 git diff --cached --check 通过。
  - Residual risk: 本次为非破坏性 revert commit，Git history 仍保留被回退的 8f0ec56c 以供追溯；仓库内容已回退到该 commit 前的规则与文档口径。
  - Next step: 如后续仍需清理这些规则，应重新提出需求并明确是否保留核心安全 / 合规边界。

### USER-CN-MV-REWRITE-LARGE-SQL-QUALITY-20260520: 优化 MV 推荐改写准确性与大 SQL 支持

- Status: done
- Completed at: 2026-05-20
- Commit subject: `fix(sql-optimization): improve MV rewrite large SQL support`
- Priority: 1
- Depends on: N/A
- Scope: 深入分析并优化 SQL 推荐改写中的建 MV 物理视图生成准确性，扩展 SQL 改写系列可支持的 SQL 文本长度和行数，并补齐 50 类不同 SQL 样例的回归测试覆盖；不执行真实外部 MV DDL/refresh，不改变 runtime binding ACTIVE 生效边界。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-MV-REWRITE-LARGE-SQL-QUALITY-20260520`
- Progress log:
  - 2026-05-20: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 优化 MV 推荐改写生成准确性：修复 HAVING alias 指标重写、允许有分组键的维度-only 聚合 MV 验证 SQL，扩展大 SQL diff 为采样策略，提升 SQL 输入/展示容量，并补齐 51 类不同 MV SQL 形态与超长多行 SQL 回归。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-MV-REWRITE-LARGE-SQL-QUALITY-20260520 通过；python3 scripts/task_audit.py --check --phase pre-closeout 通过；mvn -pl sql-optimization test 通过 325 tests；npm run lint 通过；npm run build 通过；node scripts/check-sql-ui-contract.mjs 通过；node scripts/lint-repository-knowledge.js 通过；git diff --check 通过。
  - Residual risk: 未执行真实外部 MV DDL、refresh、validationSql 或 runtime binding ACTIVE 联调；SQLForge 仍仅生成可审查 PULL_ONLY 方案，外部环境验收继续按既有环境证据链处理。
  - Next step: 如需外部环境验收，按部署 runbook 收集真实 MV 建表、刷新、validationSql 执行和 runtime binding ACTIVE 命中证据。

### USER-CN-BACKEND-BUILD-REPAIR-20260520: 修复后端 Maven 构建失败

- Status: done
- Completed at: 2026-05-20
- Commit subject: `fix(sql-optimization): repair rewrite record schema drift`
- Priority: 1
- Depends on: N/A
- Scope: 复现并修复当前后端 Maven build/compile 失败问题，保持 JDK 8u112 基线，不改动无关功能边界。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-BACKEND-BUILD-REPAIR-20260520`
- Progress log:
  - 2026-05-20: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-20: reproduced rewrite-record creation failure as a legacy schema drift: local `sql_rewrite_record` had old `publish_status` / `published_sql_fingerprint` columns while the mapper writes `activation_status` / `activated_sql_fingerprint`.
  - 2026-05-20: added idempotent legacy publish-to-activation compatibility migration and schema mapping coverage; verified local MySQL migration, mapper-shaped insert, targeted Maven tests, full compile, and foreman validate.
- Context closeout:
  - Completed scope: 修复创建改写记录时旧库 publish 字段与当前 activation mapper 字段不一致导致的 [10000] 内部错误；新增旧 schema 兼容 migration 与 schema mapping 覆盖。
  - Validation evidence: 本地 MySQL 执行 V20260520_002 migration 成功；mapper-shaped insert/select/delete 成功；mvn -pl sql-optimization -am -Dtest=AccelerationRewriteGovernancePersistenceSchemaMappingTest,AccelerationRewriteContractApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test 通过；mvn -DskipTests compile 通过；python3 scripts/foreman.py validate USER-CN-BACKEND-BUILD-REPAIR-20260520 通过；python3 scripts/task_audit.py --check --phase pre-closeout 通过。
  - Residual risk: 未启动完整四服务运行时做浏览器端联调；本次根因已在数据库 schema 和 mapper 字段层面复现并验证。
  - Next step: 在受影响环境执行 sql/migrations/V20260520_002__sql_rewrite_record_legacy_publish_activation_compat.sql 后重试创建改写记录。

### USER-CN-OPS-SURFACE-CLEANUP-FOLLOWUP-20260520: 收口运维治理面残留文档与文案

- Status: done
- Completed at: 2026-05-20
- Commit subject: `USER-CN-OPS-SURFACE-CLEANUP-FOLLOWUP-20260520: align ops surface cleanup docs`
- Priority: 1
- Depends on: N/A
- Scope: 基于 R-191 和已完成 B 方案，清理当前权威文档、前端 locale 与 contract 中残留的告警中心、审计取证、公开追踪、运行门禁、恢复演练产品入口表述；保留历史归档、最小执行安全 evidence、SQL 执行历史、真实改写历史和 runtime binding ACTIVE 安全约束。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-OPS-SURFACE-CLEANUP-FOLLOWUP-20260520`
- Progress log:
  - 2026-05-20: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 清理当前权威文档、前端 locale 与 dashboard contract 中残留的告警中心、审计取证、公开追踪、运行门禁和恢复演练产品入口表述；保留 SQL 执行历史、真实改写历史、后端内部 evidence、备份恢复文档和 runtime binding ACTIVE 安全约束。
  - Validation evidence: node scripts/check-dashboard-contract.mjs; node scripts/check-navigation-shell-contract.mjs; node scripts/check-developer-copy-language.mjs --changed; git diff --check; rg active frontend/scripts residual check; node scripts/lint-repository-knowledge.js; npm run lint; npm run build; python3 scripts/foreman.py validate USER-CN-OPS-SURFACE-CLEANUP-FOLLOWUP-20260520; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 历史归档、原始需求、validation-log 和已完成任务记录仍保留旧术语用于追溯；后端内部告警事件、audit/trace DTO 与 artifact recovery evidence 仍按最小执行安全和内部 evidence 边界保留，不作为产品入口暴露。
  - Next step: 运行 post-closeout task audit 并提交本次单任务改动。

### USER-CN-IMPLEMENT-ENGINE-OPTION-B-20260520: 实施执行引擎纯化B方案

- Status: done
- Completed at: 2026-05-20
- Commit subject: `USER-CN-IMPLEMENT-ENGINE-OPTION-B-20260520: remove ops product surfaces`
- Priority: 1
- Depends on: N/A
- Scope: 按人类确认的B方案删除产品化辅助治理面和公开追踪/告警API，保留SQL执行历史、真实改写历史和runtime binding ACTIVE安全约束；废止R-111与R-115产品要求，允许历史audit_log/alert_event/trace数据drop，并将benchmark artifact recovery/cleanup移出项目边界；同步改造文档、规则、前端、后端契约、SQL、脚本与验证。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-IMPLEMENT-ENGINE-OPTION-B-20260520`
- Progress log:
  - 2026-05-20: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented Option B engine simplification by removing productized auxiliary governance pages/routes, public trace and alert controllers/contracts, alert/detail links, obsolete smoke contracts, and refreshed portable frontend assets while preserving SQL execution history, real rewrite history, internal alert emission, and runtime binding ACTIVE safety checks.
  - Validation evidence: java -version confirmed 1.8.0_112; mvn -q -pl governance,benchmark-engine,sql-optimization -am test; mvn -q test; npm run lint; npm run build; npm run build:portable; npm run test:form-governance; npm run test:sql-ui-contract; npm run test:frontend-page-governance; npm run smoke:rewrite-governance; npm run smoke:frontend-dev; npm run smoke:portable-frontend; node scripts/check-dashboard-contract.mjs; node scripts/check-navigation-shell-contract.mjs; node scripts/check-recommendation-page-contract.mjs; node scripts/check-history-page-contract.mjs; node scripts/check-rewrite-governance-closeout.mjs; node scripts/lint-repository-knowledge.js; node scripts/check-developer-copy-language.mjs --changed; git diff --check; python3 scripts/foreman.py validate USER-CN-IMPLEMENT-ENGINE-OPTION-B-20260520; python3 scripts/task_audit.py --check --phase pre-closeout.
  - Residual risk: Public ops product surfaces are removed repo-side; internal alert emission and benchmark artifact storage resilience remain as protected/internal evidence paths. Historical docs and validation logs retain old references by design.
  - Next step: Commit this closeout, run post-closeout task audit, and keep external production-scale benchmark evidence and Hetu/MRS environment validation under their existing blocked tasks.

### USER-CN-SIMPLIFY-ENGINE-REMOVE-OPS-SURFACES-20260520: 分析去除审计取证追踪告警门禁恢复演练

- Status: done
- Completed at: 2026-05-20
- Commit subject: `USER-CN-SIMPLIFY-ENGINE-REMOVE-OPS-SURFACES-20260520: analyze ops surface removal`
- Priority: 1
- Depends on: N/A
- Scope: 盘点审计取证、追踪查询、告警中心、运行门禁、恢复演练在文档、规则、代码、脚本、验证中的分布；评估去除合理性、可行性、核心功能影响与解决方案；形成等待人类决策的分析结论，未获确认前不实施删除。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-SIMPLIFY-ENGINE-REMOVE-OPS-SURFACES-20260520`
- Progress log:
  - 2026-05-20: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-20: archived raw requirement and produced impact analysis covering docs, rules, product specs, frontend routes/views/API wrappers, backend governance trace/alert/audit contracts, SQL schema, scripts, CI/runtime smoke and validation gates.
- Context closeout:
  - Completed scope: 已完成原始需求归档、影响分析文档和 `INBOX-007` 决策项；未执行删除或破坏性迁移。
  - Validation evidence: `python3 scripts/foreman.py validate USER-CN-SIMPLIFY-ENGINE-REMOVE-OPS-SURFACES-20260520`、`python3 scripts/task_audit.py --check --phase pre-closeout`、`node scripts/lint-repository-knowledge.js` 与 `git diff --check` 通过。
  - Residual risk: 后续改造方向依赖人类选择 A/B/C；未确认前不得删除前端、后端、SQL、脚本或验证能力。
  - Next step: 等待人类选择 A/B/C，并确认等保、SQL 执行历史、runtime binding 安全约束、历史数据和 benchmark artifact recovery / cleanup 边界。
- Review reason: 本任务按人类要求只完成影响分析和解决方案，不在未确认前执行删除或破坏性迁移。
- Human decision: 请选择 `docs/plans/simplify-engine-remove-ops-surfaces-impact-analysis-2026-05-20.md` 中的 A/B/C 后续方向，并确认是否保留等保要求、SQL 执行历史、runtime binding 安全约束、历史审计/告警/trace 数据处理方式，以及 benchmark artifact recovery / cleanup 是否出界。
- INBOX ref: INBOX-007

### USER-CN-SIMPLIFY-LIFECYCLE-ACTIVATE-PAUSE-20260520: 折叠审批发布撤销回滚流程为激活暂停模型

- Status: done
- Completed at: 2026-05-20
- Commit subject: `USER-CN-SIMPLIFY-LIFECYCLE-ACTIVATE-PAUSE-20260520: collapse lifecycle to activate/pause`
- Priority: 1
- Depends on: N/A
- Scope: 深入盘点文档、规则、产品规格、前后端代码、脚本、验证入口中审批/发布/撤销/回滚相关语义；将非核心破坏性复杂流程简化为 activate/pause 两个核心动作，更新一致命名、契约、UI、脚本、验证与文档；若发现会改变核心运行时安全、审计、历史留痕、回滚恢复或兼容契约的影响，先记录问题、解决方案与推荐路径，转入人工决定，不直接破坏核心功能。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-SIMPLIFY-LIFECYCLE-ACTIVATE-PAUSE-20260520`
- Progress log:
  - 2026-05-20: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-20: repo-wide lifecycle inventory found core-impact changes across SQL rewrite review/publish contracts, query-execution runtime binding compatibility, acceleration plan apply/verify/rollback evidence, DB status values, frontend actions, and smoke/contract scripts; impact analysis recorded in `docs/plans/lifecycle-activation-pause-impact-analysis-2026-05-20.md`.
  - 2026-05-20: raw user requirement archived at `docs/references/raw-requirements/USER-CN-SIMPLIFY-LIFECYCLE-ACTIVATE-PAUSE-20260520.md`.
  - 2026-05-20: human selected full breaking state-machine migration (Option C) and confirmed approval is not a mandatory gate behind activate; explanation of `unpublish` and acceleration plan `apply/verify/rollback` added to the impact analysis.
  - 2026-05-20: human confirmed `unpublish` fully folds into `pause`; acceleration plan `apply/verify/rollback` are included in this migration, while their existing evidence is preserved as `activationEvidence` / `pauseEvidence`.
  - 2026-05-20: implemented activate/pause lifecycle across SQL rewrite records, runtime rewrite bindings, acceleration plans, query-execution acceleration binding terminology, SQL schema/migrations, recommendation frontend, dashboard/history/validation UI copy, contract scripts, smoke scripts, product docs, and architecture docs.
  - 2026-05-20: validation passed for `mvn -q clean test`, `node scripts/check-recommendation-page-contract.mjs`, `node scripts/check-production-rewrite-closed-loop-browser-smoke.mjs`, `node scripts/check-dev-frontend.mjs`, `npm run lint`, `npm run build`, `node scripts/lint-repository-knowledge.js`, `git diff --check`, `python3 scripts/foreman.py validate USER-CN-SIMPLIFY-LIFECYCLE-ACTIVATE-PAUSE-20260520`, and `python3 scripts/task_audit.py --check --phase pre-closeout`; closeout and post-closeout audit remain pending.
- Context closeout:
  - Completed scope: Collapsed SQL rewrite, runtime binding, acceleration plan, schema, frontend, scripts, and docs from approval/publish/apply/rollback lifecycle terms to activate/pause semantics.
  - Validation evidence: mvn -q clean test; node scripts/check-recommendation-page-contract.mjs; node scripts/check-production-rewrite-closed-loop-browser-smoke.mjs; node scripts/check-dev-frontend.mjs; npm run lint; npm run build; node scripts/lint-repository-knowledge.js; git diff --check; python3 scripts/foreman.py validate USER-CN-SIMPLIFY-LIFECYCLE-ACTIVATE-PAUSE-20260520; python3 scripts/task_audit.py --check --phase pre-closeout.
  - Residual risk: Breaking API/status/schema migration requires downstream clients and existing environment data to move to activation naming; repository-side validation is complete.
  - Next step: Monitor downstream/environment-backed consumers for activation naming adoption; no additional repository follow-up is required for this task.

### USER-CN-REMOVE-ACCELERATION-GOVERNANCE-WORKBENCH-20260520: 去除加速治理工作台参考功能与文档

- Status: done
- Completed at: 2026-05-20
- Commit subject: `Remove acceleration governance workbench reference page`
- Priority: 1
- Depends on: N/A
- Scope: Remove the acceleration governance workbench reference page, dedicated documents, route/menu/i18n, and workbench-only smoke/contract scripts while preserving core recommendation, SQL history, alert, production rewrite closed-loop, backend acceleration-plan/runtime-binding capabilities, and replacing the smoke bundle so core validation remains covered.
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REMOVE-ACCELERATION-GOVERNANCE-WORKBENCH-20260520`
- Progress log:
  - 2026-05-20: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Removed the old acceleration governance workbench reference page, route/menu/i18n entries, dedicated product and deployment docs, workbench-only contract/browser smoke scripts, and refreshed portable frontend assets while preserving core recommendation, SQL history, alert, production rewrite closed-loop, backend acceleration-plan and runtime-binding capabilities.
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-REMOVE-ACCELERATION-GOVERNANCE-WORKBENCH-20260520 --include-task-audit with extra commands: node scripts/check-rewrite-governance-closeout.mjs; npm run smoke:rewrite-governance; npm run test:sql-ui-contract; npm run test:i18n-copy; npm run lint; npm run build; npm run build:portable; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance --check; git diff --check.
  - Residual risk: Historical audit/raw requirement records still contain old workbench references by design; active source, product/deployment docs, navigation, package scripts and replacement closeout checks no longer expose the removed workbench.
  - Next step: No follow-up required for the removed workbench. Future real Hetu/MRS environment-backed evidence remains under HARN-016 / INBOX-002 and must not be treated as covered by repo-closed smoke.

### USER-CN-DIST-PORTABLE-UPDATE-20260520: 按最新页面刷新 dist-portable

- Status: done
- Completed at: 2026-05-20
- Commit subject: `build(frontend): refresh portable dist`
- Priority: 1
- Depends on: N/A
- Scope: 使用现有 portable 构建入口按当前最新前端页面重新生成 dist-portable 产物；不修改业务源码、接口语义或页面功能。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-DIST-PORTABLE-UPDATE-20260520`
- Progress log:
  - 2026-05-20: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 按当前最新前端页面重新生成 dist-portable 离线产物，更新 index.html 与 hashed assets；未修改业务源码、接口语义或页面功能。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-DIST-PORTABLE-UPDATE-20260520 --include-task-audit 通过；npm run build:portable 通过；npm run smoke:portable-frontend 通过；git diff --check 通过。
  - Residual risk: 无仓库内残余风险；本次仅刷新 portable 静态产物。
  - Next step: 无。

### USER-CN-FRONTEND-REWRITE-ACCELERATION-USABILITY-20260520: 推荐改写加速前端可用性验证与补链

- Status: done
- Completed at: 2026-05-20
- Commit subject: `test(frontend): verify rewrite acceleration usability`
- Priority: 1
- Depends on: HARN-FE-008,PRW-012,USER-CN-RECOMMENDATION-DIFF-LAYOUT-20260517
- Scope: 验证并在必要时修复推荐结果、改写记录/改写历史、加速治理参考页的前端调用、深链、契约、构建与浏览器 smoke；不新增后端 API、不改变审批发布或自动应用语义。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-FRONTEND-REWRITE-ACCELERATION-USABILITY-20260520`
- Progress log:
  - 2026-05-20: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 完成推荐结果、改写记录/改写历史、加速治理参考页的当前前端调用、深链、构建、契约与浏览器 smoke 可用性验证；确认无需新增后端 API，未改变审批发布、dispatch、运行时绑定或自动应用语义。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-FRONTEND-REWRITE-ACCELERATION-USABILITY-20260520 --include-task-audit 通过；npm run lint、npm run build、npm run test:sql-ui-contract、npm run test:frontend-page-governance、node scripts/check-recommendation-page-contract.mjs、node scripts/check-history-page-contract.mjs、node scripts/check-history-detail-contract.mjs、node scripts/check-acceleration-workbench-contract.mjs、node scripts/check-navigation-shell-contract.mjs、npm run smoke:frontend-dev、npm run smoke:acceleration-workbench、npm run smoke:production-rewrite-closed-loop、npm run smoke:acceleration-governance、node scripts/lint-repository-knowledge.js、git diff --check 均通过。
  - Residual risk: repo-closed browser smoke 使用 mock API；真实 Hetu/MRS 与外部生产环境证据仍按 HARN-016/INBOX-002 等 environment-backed 链路沉淀，不作为本次前端可用性验证阻断。
  - Next step: 无仓库内后续修复项；后续仅在真实外部环境提供时补充 environment-backed 证据。

### AMV-016: 补齐测试 SQL、契约测试和 smoke

- Status: done
- Completed at: 2026-05-19
- Commit subject: `AMV-016 complete MV contract smoke coverage`
- Priority: 1
- Depends on: N/A
- Scope: 正式补齐 AMV-016 repo-closed 验收链路：覆盖高级 MV 五类类型的非阻断 SQL-bundle 与阻断样例，明确 REVIEW_REQUIRED 状态语义与人工复核样例，扩展推荐详情、diff、plan payload 与推荐中心/加速治理工作台 contract，继续拒绝 EXACT_QUERY_MV；不执行真实 MV DDL、refresh 或外部验证 SQL，不新增公开 endpoint，不改变审批发布或 query-execution runtime binding 主链路。
- Validation:
  - `python3 scripts/foreman.py validate AMV-016`
- Progress log:
  - 2026-05-19: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 补齐 AMV-016 repo-closed 验收链路：高级 MV 五类类型均有非阻断 SQL-bundle 与阻断样例回归；PREJOIN_MV 与 STAR_AGG_MV 的复核警告显式输出 artifactStatus=REVIEW_REQUIRED；推荐详情、diff、plan payload 保留合法 mvType/artifactStatus/reviewWarnings 并继续过滤 EXACT_QUERY_MV；推荐中心和加速治理工作台 contract 断言 REVIEW_REQUIRED 不能作为自动 runtime rewrite SQL 来源。不执行真实 MV DDL、refresh 或外部验证 SQL，不新增公开 endpoint，不改变审批发布或 query-execution runtime binding 主链路。
  - Validation evidence: JDK 8u112 下通过 mvn -pl sql-optimization -Dtest=L2MaterializedViewAmv016RegressionTest,L2ParameterizedAggMvCandidateGeneratorTest,L2PrejoinMvCandidateGeneratorTest,L2StarAggMvCandidateGeneratorTest,L2RollupMvCandidateGeneratorTest,L2CommonSubgraphMvCandidateGeneratorTest,AccelerationRecommendationControllerTest,SqlDiffApplicationServiceTest,AccelerationPlanApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test；node scripts/check-recommendation-page-contract.mjs；node scripts/check-acceleration-workbench-contract.mjs；git diff --check；node scripts/lint-repository-knowledge.js；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/foreman.py validate AMV-016 --include-task-audit --extra-command <focused Maven> --extra-command <recommendation contract> --extra-command <workbench contract>。
  - Residual risk: Repo-closed 验收未执行真实 HETU/HIVE/SPARK MV DDL、refresh 或外部验证 SQL；runtime 生效仍需后续审批发布与 query-execution runtime binding ACTIVE 证据证明。
  - Next step: 后续若进入外部环境验收，按部署 runbook 收集真实 MV 建表、刷新、验证和 runtime binding 命中证据。

### AMV-015: 增强验证 SQL 与等价验证证据

- Status: done
- Completed at: 2026-05-19
- Commit subject: `AMV-015 enhance MV validation SQL checks`
- Priority: 1
- Depends on: AMV-014
- Scope: 增强高级 MV accelerationArtifact.validationSql：为 PARAMETERIZED_AGG_MV、PREJOIN_MV、STAR_AGG_MV、ROLLUP_MV、COMMON_SUBGRAPH_MV 生成包含 original_result、rewrite_result、行数、指标、关键维度分组、Join 后差异或公共子图输出对比的可复制验证 SQL 草案；无法生成增强验证 SQL 时输出 BLOCKED 与 blockingReasons；不新增 API/schema 字段，不执行验证 SQL，不改变审批发布或 runtime binding ACTIVE 生效边界。
- Validation:
  - `python3 scripts/foreman.py validate AMV-015`
- Progress log:
  - 2026-05-19: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增统一 L2MaterializedViewValidationSqlBuilder，并将 PARAMETERIZED_AGG_MV、PREJOIN_MV、STAR_AGG_MV、ROLLUP_MV、COMMON_SUBGRAPH_MV 的 validationSql 统一升级为 original_result/rewrite_result CTE 草案，覆盖 ROW_COUNT_CHECK、MEASURE_DIFF、GROUP_MEASURE_DIFF、GROUP_KEY_DIFF、PREJOIN/STAR 的 JOIN_ROW_COUNT_CHECK 以及 COMMON_SUBGRAPH 的 COMMON_SUBGRAPH_OUTPUT_CHECK/UPPER_REWRITE_RESULT_CHECK；无法解析验证字段时返回 BLOCKED 与 blockingReasons，不新增 API/schema 字段，不执行 SQL，不改变 runtime binding ACTIVE 生效边界。
  - Validation evidence: JDK 8u112 下通过 mvn -pl sql-optimization -Dtest=L2MaterializedViewValidationSqlBuilderTest,L2ParameterizedAggMvCandidateGeneratorTest,L2PrejoinMvCandidateGeneratorTest,L2StarAggMvCandidateGeneratorTest,L2RollupMvCandidateGeneratorTest,L2CommonSubgraphMvCandidateGeneratorTest -Dsurefire.failIfNoSpecifiedTests=false test；node scripts/check-recommendation-page-contract.mjs；node scripts/check-acceleration-workbench-contract.mjs；git diff --check；node scripts/check-developer-copy-language.mjs --changed；python3 scripts/foreman.py validate AMV-015；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 未在真实外部 HETU/HIVE/SPARK 环境执行 validationSql；本任务仅生成可复制、外部执行的验证草案。
  - Next step: AMV-016 继续补齐更广测试 SQL、契约测试和 smoke 样例。

### AMV-014: 打通从 MV rewriteSql 创建改写记录的治理入口

- Status: done
- Completed at: 2026-05-19
- Commit subject: `AMV-014 enforce MV rewrite artifact authority`
- Priority: 1
- Depends on: AMV-013
- Scope: 以既有 USER-CN-MV-RUNTIME-REWRITE-BINDING-20260519 为功能基线，正式补齐 AMV-014 治理链路：创建改写记录时以后端落库 GENERATED PRECOMPUTE_MV accelerationArtifact.rewriteSql 为 recommendedSqlText 权威来源，校验或补齐 traceRefs.mvType、traceRefs.mvName、traceRefs.accelerationArtifact 摘要；不新增第二套 API，不绕过审批、发布和 query-execution runtime binding ACTIVE 生效语义；补充后端回归测试与推荐中心/加速治理工作台 runtime binding 文案。
- Validation:
  - `python3 scripts/foreman.py validate AMV-014`
- Progress log:
  - 2026-05-19: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 正式补齐 AMV-014 治理链路，不新增 API：创建改写记录时会按 recommendationId 反查已落库 GENERATED PRECOMPUTE_MV accelerationArtifact，以 rewriteSql 校验 recommendedSqlText，并补齐/校验 traceRefs.mvType、traceRefs.mvName 与 accelerationArtifact 摘要；前端文案同步为 runtime binding ACTIVE 后才生效。
  - Validation evidence: mvn -pl sql-optimization -Dtest=AccelerationRewriteContractApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test；node scripts/check-recommendation-page-contract.mjs；node scripts/check-acceleration-workbench-contract.mjs；node scripts/check-developer-copy-language.mjs --changed；git diff --check；python3 scripts/foreman.py validate AMV-014；python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 未执行真实外部 MV DDL、refresh 或 validation SQL；SQLForge 仍保持 PULL_ONLY，运行时生效继续以 query-execution runtime binding ACTIVE 与执行历史证据为准。
  - Next step: 后续 AMV-015/AMV-016 继续补齐验证 SQL 草案、等价证据和更广样例回归。

### AMV-013: 前端结构化展示审查与优化

- Status: done
- Completed at: 2026-05-19
- Commit subject: `AMV-013 add structured MV artifact display`
- Priority: 1
- Depends on: AMV-012
- Scope: 在推荐中心和加速治理工作台补齐高级 MV accelerationArtifact 的结构化主区展示，覆盖 mvType、粒度、维度、指标、谓词分类、Join 图、覆盖证明、reviewWarnings、阻断/复核原因和 rewriteSql 来源说明；不新增后端接口、不改变审批发布状态机或 runtime 生效链路。
- Validation:
  - `python3 scripts/foreman.py validate AMV-013`
- Progress log:
  - 2026-05-19: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增共享 accelerationArtifact 结构化展示 helper，并在推荐中心与加速治理工作台主区展示 mvType 中文说明、状态边界、reviewWarnings、blockingReasons、grain、dimensions、measures、谓词分类、coverage checklist、Join 图、类型专属证据和 SQL 五件套；创建改写记录入口同步展示 recommendedSqlText 来源，traceRefs 兼容补充 mvType。
  - Validation evidence: 已通过 node scripts/check-recommendation-page-contract.mjs；node scripts/check-acceleration-workbench-contract.mjs；npm run lint -- --quiet；npm run build；npm run test:frontend-page-governance；npm run test:sql-ui-contract；node scripts/check-developer-copy-language.mjs --changed；node scripts/lint-repository-knowledge.js；git diff --check；Playwright 桌面/窄屏截图自检 logs/amv-013-*.png 且 body 横向溢出为 0；python3 scripts/foreman.py validate AMV-013；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: AMV-013 只补齐前端展示与改写记录来源说明，不执行 MV DDL、刷新、验证、审批发布或 runtime binding；legacy 空快照仍只展示空态或 '-'，不由前端推断后端未返回的覆盖结论。
  - Next step: 后续如需更强浏览器回归，可把 AMV-013 的 mocked artifact 截图脚本固化为专项 smoke；runtime 生效仍按既有改写记录审批发布链路推进。

### AMV-012: 扩展 accelerationArtifact API 与持久/展示契约

- Status: done
- Completed at: 2026-05-19
- Commit subject: `AMV-012 persist MV acceleration artifact snapshots`
- Priority: 1
- Depends on: AMV-011
- Scope: 让高级 MV accelerationArtifact 以落库快照方式在推荐详情、diff、加速任务和工作台中保持一致；扩展后端持久字段与 VO/DTO JSON 契约，兼容 SQL 五件套，新增 mvType、grain、dimensions、measures、谓词分类、coverage、joinGraph 等字段；详情/diff/加速计划读取同一 sanitized snapshot，禁止 EXACT_QUERY_MV 出现在响应中，并补充后端与前端兼容契约测试。
- Validation:
  - `python3 scripts/foreman.py validate AMV-012`
- Progress log:
  - 2026-05-19: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 acceleration_artifact_json 推荐快照字段与 migration/schema/mapper/domain 映射；创建 AccelerationArtifactSnapshotService/Sanitizer，推荐创建时保存 sanitized 高级 MV artifact，详情/diff 优先读取快照且仅历史空快照 fallback；加速计划 payload 复用同一 sanitizer；响应过滤 EXACT_QUERY_MV/未知 mvType；补齐后端/REST/前端 contract 测试和数据模型/接口文档。
  - Validation evidence: 已在 JDK 8u112 下通过 mvn -pl sql-optimization -Dtest=AccelerationRecommendationApplicationServiceTest,SqlDiffApplicationServiceTest,AccelerationRecommendationControllerTest,MybatisAccelerationRecommendationRepositoryTest,ParseBatchPersistenceSchemaMappingTest,AccelerationPlanApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test；mvn -pl sql-optimization test；node scripts/check-recommendation-page-contract.mjs；node scripts/check-acceleration-workbench-contract.mjs；node scripts/lint-repository-knowledge.js；node scripts/check-developer-copy-language.mjs --changed；git diff --check；python3 scripts/foreman.py validate AMV-012；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: SQLForge 仍不执行真实 DDL、刷新、结果校验或 runtime binding 发布；历史空 acceleration_artifact_json 行仍允许 legacy 读时 fallback，未回填前可能受当前解析/规则版本影响；高级 MV 的前端结构化解释区仍留给 AMV-013。
  - Next step: 继续 AMV-013，补齐推荐中心和加速治理工作台对 mvType、grain、coverage、joinGraph 与谓词分类的结构化展示。

### AMV-011: 实现 rewrite SQL 生成与静态覆盖校验

- Status: done
- Completed at: 2026-05-19
- Commit subject: `AMV-011 add MV rewrite coverage validator`
- Priority: 1
- Depends on: AMV-010
- Scope: 为高级物化视图 rewriteSql 增加统一静态覆盖校验和只读校验；覆盖投影、过滤、分组、指标、安全谓词；覆盖缺失或 rewrite 未引用 MV/仍访问原基表时结构化阻断且不输出可发布 SQL。
- Validation:
  - `python3 scripts/foreman.py validate AMV-011`
- Progress log:
  - 2026-05-19: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增统一 L2MaterializedViewRewriteCoverageValidator，并在高级 MV artifact builder 中作为 GENERATED 前最后门禁；rewriteSql 需通过只读单语句校验、引用 MV、避开原基表，并证明投影、过滤、分组、指标和安全谓词覆盖，否则结构化阻断且不输出 ddlSql/rewriteSql。
  - Validation evidence: 已通过 JDK 8u112 下 mvn -pl sql-optimization -Dtest=L2MaterializedViewRewriteCoverageValidatorTest,L2ParameterizedAggMvCandidateGeneratorTest,L2PrejoinMvCandidateGeneratorTest,L2StarAggMvCandidateGeneratorTest,L2RollupMvCandidateGeneratorTest,L2CommonSubgraphMvCandidateGeneratorTest -Dsurefire.failIfNoSpecifiedTests=false test；mvn -pl sql-optimization test；git diff --check；node scripts/lint-repository-knowledge.js；node scripts/check-developer-copy-language.mjs --changed；python3 scripts/foreman.py validate AMV-011；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 静态覆盖校验仍基于解析画像、MV 字段名和 SQL 关系扫描，不声明真实引擎执行等价、成本收益或 runtime binding 生效；外部建表、刷新、审批和生产验证仍按既有治理链路执行。
  - Next step: 后续 AMV-012/AMV-013 可复用 coverage、blockingReasons 与 rewriteSqlReadonly/rewriteSqlReferencesMv/rewriteSqlAvoidsOriginalSources 字段扩展 API 与前端展示。

### AMV-010: 实现方言渲染与命名规范

- Status: done
- Completed at: 2026-05-19
- Commit subject: `AMV-010 add MV dialect renderer and naming policy`
- Priority: 1
- Depends on: N/A
- Scope: 为 HETU、HIVE、SPARK 渲染高级 MV DDL、刷新和回滚 SQL，并统一 MV 命名；保持 AUTO 与不支持引擎阻断语义。
- Validation:
  - `python3 scripts/foreman.py validate AMV-010`
- Progress log:
  - 2026-05-19: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 L2MaterializedViewDialectRenderer 和 L2MaterializedViewNamePolicy；五类高级 MV 生成器统一交给 HETU/HIVE/SPARK 方言渲染 DDL、刷新和回滚 SQL；MV 名称按逻辑对象/报表、主表、类型、维度和短 hash 生成并限制 63 字符；保持 accelerationArtifact 字段形态和 AUTO/不支持引擎阻断语义。
  - Validation evidence: 已通过 mvn -pl sql-optimization -Dtest=L2MaterializedViewDialectRendererTest,L2MaterializedViewNamePolicyTest,L2ParameterizedAggMvCandidateGeneratorTest,L2PrejoinMvCandidateGeneratorTest,L2StarAggMvCandidateGeneratorTest,L2RollupMvCandidateGeneratorTest,L2CommonSubgraphMvCandidateGeneratorTest -Dsurefire.failIfNoSpecifiedTests=false test；mvn -pl sql-optimization test；node scripts/lint-repository-knowledge.js；node scripts/check-developer-copy-language.mjs --changed；git diff --check；python3 scripts/foreman.py validate AMV-010；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 方言渲染仅覆盖项目契约中的 HETU/HIVE/SPARK 模板；rewrite 覆盖证明、真实引擎语法执行差异和 runtime binding 发布仍留给后续 AMV-011 及既有治理链路。
  - Next step: 继续 AMV-011 rewrite SQL 生成与静态覆盖校验；AMV-013 可后续补齐前端结构化展示。

### AMV-009: 实现 COMMON_SUBGRAPH_MV 候选生成

- Status: done
- Completed at: 2026-05-19
- Commit subject: `AMV-009 add common subgraph MV generator`
- Priority: 1
- Depends on: AMV-008
- Scope: 针对复杂 CTE、FROM/JOIN 派生表、重复子查询以及同批/同报表/同租户候选中的跨 SQL 精确共享子图生成 COMMON_SUBGRAPH_MV；DDL 只能物化被选中的公共子图，rewriteSql 必须查询 MV；输出 commonSubgraphEvidence；对相关子查询、递归 CTE、窗口函数、非确定函数、子图内 ORDER BY/LIMIT、SELECT *、输出列或覆盖证明不足等场景结构化阻断且不输出可发布 SQL；不新增前端专项页面，不改变 runtime 生效链路。
- Validation:
  - `python3 scripts/foreman.py validate AMV-009`
- Progress log:
  - 2026-05-19: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 实现 COMMON_SUBGRAPH_MV 路由、CTE/派生表公共子图生成、跨 SQL 精确共享 evidence、保守阻断与回归测试；文档同步 AMV-005 至 AMV-009 当前事实。
  - Validation evidence: 已通过 focused AMV Maven suite、mvn -pl sql-optimization test、node scripts/lint-repository-knowledge.js、node scripts/check-developer-copy-language.mjs --changed、git diff --check、python3 scripts/foreman.py validate AMV-009、python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 跨 SQL 子图匹配仅采用规范化 SQL 精确匹配；递归/相关/窗口/非确定函数/排序分页/SELECT * 与覆盖不足场景保持阻断，方言细化仍归 AMV-010。
  - Next step: 进入 AMV-010 方言精细渲染与后续 AMV 任务。

### AMV-008: 实现 ROLLUP_MV 候选生成

- Status: done
- Completed at: 2026-05-19
- Commit subject: `AMV-008 add rollup MV generator`
- Priority: 1
- Depends on: AMV-007
- Scope: 支持保守自然日历的时间粒度上卷 MV 候选生成，新增 ROLLUP_MV 专用生成与路由，输出日粒度 MV 覆盖月/季/年查询的 DDL、MV-only rewrite SQL 和结构化 timeRollupEvidence；周粒度、财务日历、不可归一时间表达式、COUNT(DISTINCT)、百分位、中位数与复杂 UDAF 必须结构化阻断且不输出可发布 SQL；不改变 runtime 生效链路。
- Validation:
  - `python3 scripts/foreman.py validate AMV-008`
- Progress log:
  - 2026-05-19: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 ROLLUP_MV 专用候选生成器并接入 L2AccelerationArtifactBuilder；支持自然日历日粒度 MV 覆盖月/季度/年查询，生成细粒度 DDL、MV-only 二次聚合 rewrite SQL、AVG SUM/COUNT 组件重算和 timeRollupEvidence；周粒度、财务日历、不可归一时间表达式及不可合并指标保持结构化阻断且不输出 ddlSql/rewriteSql；runtime 生效链路仍为 PULL_ONLY_NOT_EXECUTED_BY_SQLFORGE 与 NOT_CREATED。
  - Validation evidence: mvn -pl sql-optimization -Dtest=L2RollupMvCandidateGeneratorTest,L2ParameterizedAggMvCandidateGeneratorTest,L2GrainMeasureDeriverTest,L2PrejoinMvCandidateGeneratorTest,L2StarAggMvCandidateGeneratorTest -Dsurefire.failIfNoSpecifiedTests=false test; mvn -pl sql-optimization test; node scripts/check-developer-copy-language.mjs --changed; node scripts/lint-repository-knowledge.js; git diff --check; python3 scripts/foreman.py validate AMV-008; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: ROLLUP_MV 仍基于静态 SQL 画像和自然日历 DATE_TRUNC 语义，不证明财务日历、周起始日、时区转换或真实引擎物理收益；实际建表、刷新、审批发布与 runtime binding 仍需既有治理链路。
  - Next step: AMV-009 可继续实现 COMMON_SUBGRAPH_MV；AMV-013 可补齐前端对 timeRollupEvidence 的结构化展示。

### AMV-007: 实现 STAR_AGG_MV 候选生成

- Status: done
- Completed at: 2026-05-19
- Commit subject: `AMV-007 add star aggregate MV generator`
- Priority: 1
- Depends on: AMV-006
- Scope: 针对事实表 Join 维表后聚合的 SQL 生成 STAR_AGG_MV 候选；基于 Join 图保守识别事实表和维表字段，生成 Join 后聚合 DDL、只查询 MV 的二次过滤/二次聚合 rewrite SQL，并输出 factTable、dimensionTables、joinKeys、dimensionSources、measureSources、starSchemaEvidence；事实表无法识别、Join 数量不足、outer/non-equi join、粒度不覆盖、不可合并指标或安全谓词丢失时结构化阻断。
- Validation:
  - `python3 scripts/foreman.py validate AMV-007`
- Progress log:
  - 2026-05-19: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented STAR_AGG_MV routing and candidate generation for fact-table plus dimension-table join aggregate SQL; generated Join-after-aggregation MV DDL, MV-only rewrite SQL with secondary filtering/aggregation, and structured factTable, dimensionTables, joinKeys, dimensionSources, measureSources, and starSchemaEvidence fields while keeping single-join PREJOIN routing intact.
  - Validation evidence: mvn -pl sql-optimization -DskipTests compile; mvn -pl sql-optimization -Dtest=L2StarAggMvCandidateGeneratorTest,L2PrejoinMvCandidateGeneratorTest,L2ParameterizedAggMvCandidateGeneratorTest,L2GrainMeasureDeriverTest,SqlDiffApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test; mvn -pl sql-optimization test; node scripts/check-developer-copy-language.mjs --changed; node scripts/lint-repository-knowledge.js; git diff --check; python3 scripts/foreman.py validate AMV-007; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: STAR_AGG_MV fact/dimension recognition remains conservative static inference without table cardinality, unique-key, or join-selectivity metadata; runtime binding remains NOT_CREATED and publication still requires the existing approval path.
  - Next step: AMV-008 can add ROLLUP_MV generation, and AMV-013 can add dedicated front-end presentation for STAR_AGG evidence.

### AMV-006: Implement PREJOIN_MV recommendation for join aggregate queries

- Status: done
- Completed at: 2026-05-19
- Commit subject: `AMV-006 add prejoin MV generator`
- Priority: 1
- Depends on: AMV-005
- Scope: Add PREJOIN_MV routing and candidate generation for INNER equi-join queries with aggregation or GROUP BY, generating pull-only pre-join materialized view artifacts with structured review warnings and blocking unsafe join forms.
- Validation:
  - `python3 scripts/foreman.py validate AMV-006`
  - `mvn -pl sql-optimization -Dtest=L2PrejoinMvCandidateGeneratorTest,L2ParameterizedAggMvCandidateGeneratorTest test`
  - `mvn -pl sql-optimization test`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `git diff --check`
- Progress log:
  - 2026-05-19: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-19: implemented PREJOIN_MV routing, detail wide-table DDL/rewrite generation, structured join key/field mapping evidence, and row-amplification review warning for missing uniqueness/cardinality metadata.
  - 2026-05-19: added PREJOIN_MV unit coverage for safe INNER equi-join aggregation, parameter-shape DDL reuse, CROSS/outer/non-equi/complex/ambiguous field blocking, and AMV-005 regression that safe join aggregates no longer emit JOIN_MV_TYPE_DEFERRED.
  - 2026-05-19: validation passed with focused Maven suite, full sql-optimization module test suite, Foreman validate, pre-closeout task audit, and git diff whitespace check.
- Context closeout:
  - Completed scope: Implemented PREJOIN_MV routing for join aggregate/GROUP BY queries, generated pull-only detail wide-table MV artifacts with MV-only rewrite SQL, structured join key/field mapping evidence, row-amplification review warnings, and unsafe join blocking for CROSS, outer, non-equi, complex key, and ambiguous field cases.
  - Validation evidence: mvn -pl sql-optimization -Dtest=L2PrejoinMvCandidateGeneratorTest,L2ParameterizedAggMvCandidateGeneratorTest test; mvn -pl sql-optimization test; python3 scripts/foreman.py validate AMV-006; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check
  - Residual risk: PREJOIN_MV still relies on reviewWarnings when uniqueness/cardinality metadata is absent; runtime binding remains NOT_CREATED and actual publication stays in the existing approval path.
  - Next step: AMV-007/AMV-013 can build STAR_AGG/front-end presentation on the structured artifact fields.

### AMV-005: 实现 PARAMETERIZED_AGG_MV 候选生成

- Status: done
- Completed at: 2026-05-19
- Commit subject: `AMV-005 add parameterized aggregate MV generator`
- Priority: 1
- Depends on: N/A
- Scope: 根据 AMV-003 谓词分类与 AMV-004 粒度/指标模型生成 PARAMETERIZED_AGG_MV 的 DDL、刷新、验证、回滚和 rewrite SQL；参数字段提升为 MV 维度，rewrite 查询 MV 并二次过滤/二次聚合；SELECT *、不可合并指标、不稳定谓词、缺少目标引擎和非本任务 MV 类型必须结构化阻断；保留 PULL_ONLY_NOT_EXECUTED_BY_SQLFORGE，不修改前端。
- Validation:
  - `python3 scripts/foreman.py validate AMV-005`
- Progress log:
  - 2026-05-19: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 PARAMETERIZED_AGG_MV 专用生成器并接入 L2 acceleration artifact；GENERATED 产物基于 AMV-003/004 的谓词、粒度和指标生成非 exact-query-like DDL、refresh、validation、rollback 与查询 MV 的 rewrite SQL；参数/安全字段保留为 MV 维度并在 rewrite 中过滤，固定业务 WHERE 保留在 DDL；非本任务 MV 形态和不安全场景结构化 BLOCKED 且不输出 SQL。
  - Validation evidence: python3 scripts/foreman.py validate AMV-005 --include-task-audit --extra-command "mvn -pl sql-optimization,query-execution -am -Dtest=L2ParameterizedAggMvCandidateGeneratorTest,L2GrainMeasureDeriverTest,L2PredicateClassifierTest,SqlDiffApplicationServiceTest,SqlOptimizationPipelineServiceTest,AccelerationRewriteContractApplicationServiceTest,ProductionRewriteClosedLoopEndToEndTest -Dsurefire.failIfNoSpecifiedTests=false test" --extra-command "node scripts/lint-repository-knowledge.js" --extra-command "git diff --check"；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: AMV-005 只覆盖 PARAMETERIZED_AGG_MV；PREJOIN_MV、STAR_AGG_MV、ROLLUP_MV、COMMON_SUBGRAPH_MV、通用方言渲染与跨类型静态覆盖校验仍由后续 AMV 任务处理。
  - Next step: 继续 AMV-006/AMV-008/AMV-010/AMV-011，补齐 Join、Rollup、方言渲染和通用 rewrite 覆盖校验。

### AMV-004: 实现粒度与指标推导模型

- Status: done
- Completed at: 2026-05-19
- Commit subject: `AMV-004 add grain and measure derivation`
- Priority: 1
- Depends on: AMV-003
- Scope: 根据 advancedStructureProfile 与 AMV-003 谓词分类结果推导高级 MV grain、dimensions、measures 与 coverage；支持 SUM、COUNT、MIN、MAX 可重聚合，AVG 拆成 SUM/COUNT，顶层聚合比例拆成分子/分母；COUNT(DISTINCT)、百分位、中位数、复杂 UDAF 等不可合并指标必须结构化阻断且不得输出可发布 ddlSql/rewriteSql；本任务不实现完整 PARAMETERIZED_AGG_MV DDL/rewrite 生成。
- Validation:
  - `python3 scripts/foreman.py validate AMV-004`
- Progress log:
  - 2026-05-19: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 L2GrainMeasureDeriver，并接入 PRECOMPUTE_MV accelerationArtifact；产物补齐 mvType、grain、dimensions、measures、joinGraph、coverage、reviewWarnings，SUM/COUNT/MIN/MAX、AVG 拆解和顶层聚合比例指标具备结构化 rewriteExpression；COUNT(DISTINCT)、百分位、中位数和复杂聚合指标进入 BLOCKED 且不输出 ddlSql/rewriteSql。
  - Validation evidence: python3 scripts/foreman.py validate AMV-004 --include-task-audit --extra-command "mvn -pl sql-optimization -Dtest=L2GrainMeasureDeriverTest,L2PredicateClassifierTest,StructureParseControllerTest,SqlOptimizationPipelineServiceTest,SqlDiffApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test" --extra-command "git diff --check"；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: AMV-004 只完成粒度、指标和覆盖证明结构化推导；完整 PARAMETERIZED_AGG_MV DDL/rewrite 生成仍归 AMV-005，现有可合并指标场景仍保留 V1 草案 SQL 兼容行为。
  - Next step: 继续 AMV-005，基于 AMV-003 谓词分类与 AMV-004 粒度/指标模型生成真正查询 MV 的 PARAMETERIZED_AGG_MV DDL、refresh、validation、rollback 和 rewrite SQL。

### AMV-003: 实现谓词分类器

- Status: done
- Completed at: 2026-05-19
- Commit subject: `AMV-003 add predicate classification artifact`
- Priority: 1
- Depends on: AMV-002
- Scope: 新增谓词分类服务或组件，把 WHERE / HAVING 条件拆成参数外提、业务保留、安全边界和不稳定阻断四类；分类结果进入 accelerationArtifact；不稳定谓词触发结构化 blockingReasons；按已确认契约将 tenant_id 作为普通参数谓词，缺少显式安全谓词仍允许 GENERATED。
- Validation:
  - `python3 scripts/foreman.py validate AMV-003`
- Progress log:
  - 2026-05-19: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 更新高级 MV 谓词契约，将 tenant_id 明确归入参数谓词且缺少显式非租户安全谓词不阻断 GENERATED；新增 L2PredicateClassifier，把 WHERE/HAVING 谓词分类为 externalizedPredicates、retainedPredicates、securityPredicates、blockedPredicates；advancedStructureProfile 谓词保留 logicalContext/groupId；blockedPredicates 非空时 accelerationArtifact 返回 BLOCKED、结构化 blockingReasons 且不输出 ddlSql/rewriteSql。
  - Validation evidence: python3 scripts/foreman.py validate AMV-003 --include-task-audit --extra-command "mvn -pl sql-optimization -Dtest=L2PredicateClassifierTest,StructureParseControllerTest,SqlOptimizationPipelineServiceTest,SqlDiffApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test" --extra-command "node scripts/lint-repository-knowledge.js" --extra-command "git diff --check"；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 本任务只实现谓词分类和不稳定谓词阻断，不修复 exact-query-like MV DDL/rewrite，也不实现粒度、指标、覆盖证明或专门前端谓词分类展示；这些仍归后续 AMV-004/005/011/012/013 等任务。
  - Next step: 继续 AMV-004/AMV-005，将分类结果用于粒度/指标推导和 PARAMETERIZED_AGG_MV 候选生成。

### AMV-002: 扩展 SQL 结构画像

- Status: done
- Completed at: 2026-05-19
- Commit subject: `AMV-002 add advanced SQL structure profile`
- Priority: 1
- Depends on: AMV-001
- Scope: 为高级 MV 推荐提供结构化 SQL 输入，扩展解析画像覆盖表、别名、投影、谓词、Join 图、聚合、分组、HAVING、ORDER/LIMIT、CTE、子查询、时间函数和非确定函数；保留现有 L1/L2 规则行为兼容；不生成 MV DDL。
- Validation:
  - `python3 scripts/foreman.py validate AMV-002`
- Progress log:
  - 2026-05-19: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 advancedStructureProfile 结构画像并接入结构解析响应、AST_PROFILE 与 SIGNAL_PROFILE；画像覆盖表/别名、投影、谓词、Join 图、聚合、分组、ORDER/LIMIT、CTE、子查询、时间函数和非确定函数；保留既有 L1/L2 推荐规则行为。
  - Validation evidence: python3 scripts/foreman.py validate AMV-002 --include-task-audit --extra-command "mvn -pl sql-optimization -Dtest=StructureParseContractTest,StructureParseControllerTest,SqlOptimizationPipelineServiceTest -Dsurefire.failIfNoSpecifiedTests=false test" --extra-command "git diff --check"；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 本任务只做静态结构抽取，不生成 MV DDL/rewrite SQL，不做谓词分类、粒度/指标推导或覆盖证明；非 legacy parser 模式的 advancedStructureProfile 仍以 PARTIAL 状态暴露，后续可按 AMV 任务继续扩展。
  - Next step: 继续 AMV-003/AMV-004，将该结构画像用于谓词分类、粒度和指标推导。

### AMV-001: 固化高级 MV 契约与禁止 EXACT_QUERY_MV 边界

- Status: done
- Completed at: 2026-05-19
- Commit subject: `AMV-001 document advanced MV contract`
- Priority: 1
- Depends on: N/A
- Scope: 把高级 MV 推荐的产品边界、产物契约、禁止项和治理路径写入长期文档；更新产品/架构/接口/前端契约真值入口；明确 EXACT_QUERY_MV 非法、L2 产物不直接生效且 runtime 生效必须走 SQL 改写记录审批发布与 query-execution runtime binding；不修改业务代码。
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `git diff --check`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `python3 scripts/foreman.py validate AMV-001`
  - `python3 scripts/task_audit.py --check --phase post-closeout`
- Progress log:
  - 2026-05-19: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 固化高级 MV 推荐的产品、接口和页面契约：PRECOMPUTE_MV 作为总规则，mvType 细分高级类型，EXACT_QUERY_MV 非法；补齐 accelerationArtifact 字段、状态语义、runtime 生效链路和当前 V1 exact-query-like 行为的 pending gap；不修改业务代码。
  - Validation evidence: node scripts/lint-repository-knowledge.js；git diff --check；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/foreman.py validate AMV-001。
  - Residual risk: 当前 V1 PRECOMPUTE_MV 代码仍可能生成 exact-query-like MV 草案，本任务只完成长期文档契约固化；代码修复留给后续 AMV 实现任务。
  - Next step: 继续 materialize AMV-002/AMV-005/AMV-011/AMV-012/AMV-016，逐步实现 SQL 结构画像、MV 类型分类、rewrite 覆盖证明、API 字段扩展和 EXACT_QUERY_MV 防回归。

### USER-CN-L2-MV-ADVANCED-PLAN-20260519: 高级物化视图推荐任务包设计

- Status: done
- Completed at: 2026-05-19
- Commit subject: `USER-CN-L2-MV-ADVANCED-PLAN-20260519 add advanced MV task plan`
- Priority: 1
- Depends on: N/A
- Scope: 基于用户确认的非 EXACT_QUERY_MV 方向，形成 L2 高级物化视图推荐方案与可执行任务文档，不改业务代码。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-L2-MV-ADVANCED-PLAN-20260519`
  - `node scripts/lint-repository-knowledge.js`
  - `git diff --check`
- Progress log:
  - 2026-05-19: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-19: added `docs/plans/l2-advanced-materialized-view-task-plan.md` and linked it from the docs plan indexes.
  - 2026-05-19: registered the new plan in `docs/plans/document-coverage-matrix.md` after repository knowledge lint required coverage.
- Context closeout:
  - Completed scope: Created a non-EXACT advanced materialized view recommendation task plan covering parameterized aggregate, prejoin, star aggregate, rollup, common subgraph MV candidates, artifact contracts, governance flow, UI expectations, and implementation task breakdown.
  - Validation evidence: node scripts/lint-repository-knowledge.js; git diff --check; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/foreman.py validate USER-CN-L2-MV-ADVANCED-PLAN-20260519
  - Residual risk: Future implementation tasks still need code-level design and tests before any runtime behavior changes.
  - Next step: Materialize AMV-001 when ready to begin implementation.

### USER-CN-MV-RUNTIME-REWRITE-BINDING-20260519: 让 MV rewriteSql 通过运行时 SQL 改写绑定无感生效

- Status: done
- Completed at: 2026-05-19
- Commit subject: `USER-CN-MV-RUNTIME-REWRITE-BINDING-20260519 bind MV rewrite SQL at runtime`
- Priority: 1
- Depends on: N/A
- Scope: 把 PRECOMPUTE_MV 产物的 rewriteSql 写入改写记录 recommendedSqlText，并通过审批、发布、query-execution ACTIVE runtime binding 与执行历史 rewriteApplied 形成端到端闭环。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-MV-RUNTIME-REWRITE-BINDING-20260519`
- Progress log:
  - 2026-05-19: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: PRECOMPUTE_MV GENERATED artifact 的 rewriteSql 会写入改写记录 recommendedSqlText；审批授予 autoApplyAllowed；发布前校验 MV rewriteSql 与推荐 SQL 一致；推荐中心和加速治理创建改写记录时优先使用 artifact.rewriteSql；端到端测试覆盖 publishStatus=PUBLISHED、runtimeBindingId、ACTIVE binding 与 rewriteApplied=true。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-MV-RUNTIME-REWRITE-BINDING-20260519；mvn -pl sql-optimization,query-execution -am -Dtest=AccelerationRewriteContractApplicationServiceTest,ProductionRewriteClosedLoopEndToEndTest -Dsurefire.failIfNoSpecifiedTests=false test；mvn -pl query-execution -Dtest=QueryExecutionApplicationServiceTest,QueryExecutionRuntimeRewriteBindingServiceTest -Dsurefire.failIfNoSpecifiedTests=false test；node scripts/check-recommendation-page-contract.mjs；node scripts/check-acceleration-workbench-contract.mjs；npm run lint；npm run build；npm run smoke:production-rewrite-closed-loop；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 未在真实外部目标引擎执行 ddlSql/refreshSql/validationSql；本次仓库侧闭环以应用服务、runtime binding 与浏览器 smoke 证据覆盖。
  - Next step: 在外部环境完成 MV DDL/refresh/validation 后，用同租户、同数据源证据、同 SQL 指纹的原 SQL 做联调验收。

### HARN-SQL-REWRITE-HISTORY-ROUTE-20260519: Fix SQL rewrite history query unavailable route

- Status: done
- Completed at: 2026-05-19
- Commit subject: `HARN-SQL-REWRITE-HISTORY-ROUTE-20260519 fix rewrite history auth route`
- Priority: 1
- Depends on: N/A
- Scope: Fix the SQL rewrite history query failure that reports [10009] SQL optimization rewrite record route unavailable by restoring the missing route/interface wiring, adding focused regression coverage, and preserving the existing governance/audit flow.
- Validation:
  - `python3 scripts/foreman.py validate HARN-SQL-REWRITE-HISTORY-ROUTE-20260519`
- Progress log:
  - 2026-05-19: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restored SQL rewrite-history query availability by adding SQL_REWRITE_RECORD to the governance authorization resource model, covering list/detail/validation read paths and create/review/publish lifecycle actions, with matching application.yml configuration, documentation, and regression coverage for history-reader access.
  - Validation evidence: python3 scripts/foreman.py validate HARN-SQL-REWRITE-HISTORY-ROUTE-20260519 --include-task-audit with governance tests, history page/detail contract checks, repository knowledge lint, compile-governance check, developer copy check, and pre-closeout task audit all passing.
  - Residual risk: A running governance process must be restarted before the new resource model is visible to sql-optimization authorization calls; the repository fix is committed and test-covered.
  - Next step: Restart governance in any already-running local or deployed environment, then retry SQL history rewrite-record filtering or the rewriteRecords detail tab.

### HARN-L2-MV-ARTIFACT-20260519: L2 materialized-view acceleration artifact generation and rule explanations

- Status: done
- Completed at: 2026-05-19
- Commit subject: `HARN-L2-MV-ARTIFACT-20260519 add L2 MV artifacts`
- Priority: 1
- Depends on: N/A
- Scope: Implement the approved V1 plan for PRECOMPUTE_MV acceleration artifact generation without executing production DDL: generate MV DDL, refresh, validation, rollback and rewrite SQL when evidence is sufficient; expose blocking reasons when evidence is missing; enrich rule diff/rule chain output with Chinese rule explanations; update recommendation/workbench display and tests while preserving PULL_ONLY governance boundaries.
- Validation:
  - `python3 scripts/foreman.py validate HARN-L2-MV-ARTIFACT-20260519`
- Progress log:
  - 2026-05-19: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented PRECOMPUTE_MV L2 acceleration artifact generation for supported engines, exposed generated or blocked MV DDL/refresh/validation/rollback/rewrite SQL, enriched rule outputs with Chinese explanations, and updated recommendation/workbench display plus contracts and tests while preserving PULL_ONLY execution boundaries.
  - Validation evidence: python3 scripts/foreman.py validate HARN-L2-MV-ARTIFACT-20260519 with sql-optimization tests, npm build, frontend contract checks, lint, i18n copy checks, task audit pre-closeout, repository knowledge lint, and compile-governance check all passing after governance regeneration.
  - Residual risk: Generated MV SQL remains a reviewable PULL_ONLY artifact and still requires target metadata, refresh policy, permissions, result equivalence validation, and external runtime rewrite binding before production use.
  - Next step: Use the generated artifact as demo and approval input; production execution/binding stays outside SQLForge until the required evidence is supplied.

### USER-CN-FLOW-AUTH-RUNTIME-CLEANUP-20260519: 清理流程与权限冗余链路

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-FLOW-AUTH-RUNTIME-CLEANUP-20260519 enforce rewrite runtime lifecycle`
- Priority: 1
- Depends on: N/A
- Scope: 定位并优化本项目流程管控与权限管控实现，要求流程控制统一走后端领域状态机或状态接口，权限控制走统一授权入口，运行时生效走 runtime binding，清理/简化绕过这些链路的冗余代码，便于本地 sql 执行到改写加速全流程页面测试。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-FLOW-AUTH-RUNTIME-CLEANUP-20260519`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-19: located SQL rewrite lifecycle as the overlapping flow/runtime control point and switched publish/pause/unpublish from local status-only changes to domain-state guarded, authorization-checked runtime binding calls.
  - 2026-05-19: centralized rewrite-record actions on `GovernanceCapabilityClient.assertAuthorization`, removed the old status-only domain helper, and preserved runtime failure trace without fabricating paused runtime state.
  - 2026-05-19: synchronized production rewrite docs and interface baseline so lifecycle actions must use backend state interfaces, unified authorization and query-execution runtime binding.
  - 2026-05-19: targeted contract tests and full `mvn -pl sql-optimization,query-execution,sqlforge-shared -am test -DskipITs` passed.
- Context closeout:
  - Completed scope: rewrite lifecycle actions now route through domain state guards, unified authorization, and query-execution runtime binding; docs and tests updated
  - Validation evidence: mvn -pl sql-optimization,query-execution,sqlforge-shared -am test -DskipITs; python3 scripts/foreman.py validate USER-CN-FLOW-AUTH-RUNTIME-CLEANUP-20260519; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check
  - Residual risk: frontend smoke not run because this change is backend lifecycle and documentation only
  - Next step: local page flow can test SQL execution rewrite by publishing through backend APIs and verifying runtime binding ACTIVE

### USER-CN-REWRITE-LIFECYCLE-STATUS-ONLY-20260519: 改写复核发布状态化

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-REWRITE-LIFECYCLE-STATUS-ONLY-20260519 make rewrite lifecycle status-only`
- Priority: 1
- Depends on: PRW-010
- Scope: 将 sql_rewrite_record 的审批、驳回、发布、暂停、撤销收敛为纯状态变更；前端仍在改写复核与发布 Tab 操作，后端不调用 runtime binding 发布/暂停/撤销流程，不以发布资格流程阻断状态切换。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-LIFECYCLE-STATUS-ONLY-20260519`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 将改写记录 review/publish/pause/unpublish 生命周期收敛为状态变更：审批和驳回只写 reviewStatus；发布、暂停、撤销只写 publishStatus 与 lastPublishStatusTrace；周期差异自动暂停也只改状态；推荐详情发布按钮不再依赖发布资格或 runtime binding。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-REWRITE-LIFECYCLE-STATUS-ONLY-20260519 --include-task-audit 已通过，并额外覆盖 Java rewrite lifecycle 测试、生产改写 browser smoke、推荐页契约和 git diff --check。
  - Residual risk: query-execution runtime binding 能力仍作为独立执行出口保留；本任务明确不再由改写记录发布/暂停/撤销按钮触发该流程。
  - Next step: 人工在推荐详情的改写复核与发布 Tab 中审批后点击发布即可把改写记录状态置为 PUBLISHED；暂停或撤销同样只通过状态按钮控制。

### USER-CN-RECOMMENDATION-CREATE-REWRITE-RECORD-20260519: 推荐详情创建改写记录入口

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-RECOMMENDATION-CREATE-REWRITE-RECORD-20260519 add recommendation rewrite review action`
- Priority: 1
- Depends on: PRW-010
- Scope: 在推荐结果详情中为 manualReviewRequired 或 REWRITE recommendation 提供创建 sql_rewrite_record 并进入改写复核与发布 Tab 的显式动作；复用现有后端 rewrite-record 创建、审批、发布接口，不新增 schema，不把 recommendation 自动写成生产执行事实。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-RECOMMENDATION-CREATE-REWRITE-RECORD-20260519`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 推荐详情新增创建改写记录/进入复核入口；REWRITE 或 manualReviewRequired recommendation 在具备原 SQL 与推荐 SQL 时可复用现有 sql_rewrite_record 创建接口进入改写复核与发布 Tab。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-RECOMMENDATION-CREATE-REWRITE-RECORD-20260519 --include-task-audit --extra-command node-scripts-check-recommendation-page-contract --extra-command git-diff-check 已通过，详见 docs/quality/validation-log.md。
  - Residual risk: 未选择后端自动创建策略；当前保持用户显式创建，避免 recommendation 自动升级为生产改写记录。
  - Next step: 真实租户命中解析问题后，在推荐详情点击创建改写记录并继续执行既有审批、验证与发布流程。

### USER-CN-REWRITE-GOAL-COMPLETION-AUDIT-20260518: 输出目标完成度映射审计

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-REWRITE-GOAL-COMPLETION-AUDIT-20260518 add completion audit checklist`
- Priority: 1
- Depends on: USER-CN-BENCHMARK-EVIDENCE-PROVENANCE-AUDIT-20260518
- Scope: 强化 audit-rewrite-production-readiness.py 输出，将用户目标拆解为 prompt-to-artifact checklist，逐项映射最新调研、方案/方法/专利/工具、>=36 SELECT 改写、生产规模 gate、30PB/千万级日查询外部证据和不可接受的 proxy signals；任何缺口必须在 completionAudit.missingOrWeakEvidence 中显式阻断完成。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-GOAL-COMPLETION-AUDIT-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-18: readiness audit 已输出 `completionAudit.promptToArtifactChecklist`，将用户目标逐项映射到调研、核心逻辑、覆盖测试、生产规模 gate 与外部证据，并显式阻断缺口。
- Context closeout:
  - Completed scope: readiness audit 现在输出 completionAudit.promptToArtifactChecklist，将用户目标拆解为调研、核心逻辑、>=36 SELECT 改写、生产规模 gate 和外部 30PB/千万级日查询证据，并用 missingOrWeakEvidence 阻断未覆盖要求；runbook 已说明 completionAudit 是最终完成度复核清单。
  - Validation evidence: python3 scripts/audit-rewrite-production-readiness.py --self-test; python3 scripts/audit-rewrite-production-readiness.py (expected BLOCKED without external evidence); git diff --check; node scripts/check-developer-copy-language.mjs --changed; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py validate USER-CN-REWRITE-GOAL-COMPLETION-AUDIT-20260518; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 真实 30PB/千万级日查询外部生产 evidence 仍未提供，completionAudit 明确显示目标 NOT_ACHIEVED。
  - Next step: 等待 INBOX-005 指定的外部生产或准生产 artifacts；只有 audit 输出 completionAudit.completionDecision=ACHIEVED 且 missingOrWeakEvidence 为空时才能标记目标完成。

### USER-CN-BENCHMARK-EVIDENCE-PROVENANCE-AUDIT-20260518: 校验生产证据来源元数据

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-BENCHMARK-EVIDENCE-PROVENANCE-AUDIT-20260518 require evidence provenance`
- Priority: 1
- Depends on: USER-CN-REWRITE-READINESS-RAW-DIGEST-AUDIT-20260518
- Scope: 强化生产规模证据 verifier、readiness audit 与 scaleTargetEvidenceManifest 持久化链路，要求外部 evidence directory 提供 provenance.json，并把 environmentId/environmentType/evidenceOwner/artifactArchiveRef/verifierOperator 纳入 manifest、摘要和 scaleReadiness 判定；缺少来源元数据时不得通过 30PB/千万级日查询完成度审计。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-BENCHMARK-EVIDENCE-PROVENANCE-AUDIT-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-18: verifier/readiness audit 已将 `provenance.json` 纳入必需证据文件，并把来源元数据持久化到 benchmark `scaleTargetEvidenceManifest`、报告摘要和 scaleReadiness 判定。
- Context closeout:
  - Completed scope: 生产规模证据 verifier/readiness audit 现在要求 provenance.json，并把 environmentId/environmentType/evidenceOwner/artifactArchiveRef/verifierOperator 复算、比对、持久化到 scaleTargetEvidenceManifest；benchmark scaleReadiness、报告摘要、接口文档、runbook、INBOX 与 blocked handoff 已同步来源元数据要求。
  - Validation evidence: python3 scripts/verify-benchmark-production-evidence.py --self-test; python3 scripts/audit-rewrite-production-readiness.py --self-test; mvn -pl benchmark-engine,sqlforge-shared -am -Dtest=BenchmarkScaleEvidenceManifestTest,BenchmarkTaskModelApplicationServiceTest,BenchmarkRecommendationComparisonApplicationServiceTest,BenchmarkReportControllerTest,MybatisBenchmarkTaskRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/audit-rewrite-production-readiness.py (expected BLOCKED without external evidence); git diff --check; node scripts/check-developer-copy-language.mjs --changed; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py validate USER-CN-BENCHMARK-EVIDENCE-PROVENANCE-AUDIT-20260518; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 真实 30PB/千万级日查询外部生产 evidence 仍未提供，整体目标保持 blocked；本任务只强化来源元数据与归档校验，不创造生产证据。
  - Next step: 等待 INBOX-005 指定的外部生产或准生产 artifacts；外部 owner 必须同时提交 provenance.json、原始 evidence directory、verification-result.json，以及含来源元数据和摘要的 scaleTargetEvidenceManifest。

### USER-CN-REWRITE-READINESS-RAW-DIGEST-AUDIT-20260518: 复算外部证据文件摘要

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-REWRITE-READINESS-RAW-DIGEST-AUDIT-20260518 recompute raw evidence digests`
- Priority: 1
- Depends on: USER-CN-REWRITE-READINESS-DIGEST-CONSISTENCY-AUDIT-20260518
- Scope: 强化 audit-rewrite-production-readiness.py，要求提供原始 evidence directory，并复算必需证据文件的 SHA-256 与 sizeBytes，只有复算结果同时匹配 verification-result.json 顶层与 manifest 内 evidenceFileDigests 时才能通过外部生产证据审计。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-READINESS-RAW-DIGEST-AUDIT-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: readiness audit 现在要求 verification-result.json 与原始 evidence directory 同时提供，并复算必需证据文件 SHA-256/sizeBytes 后与顶层及 manifest 摘要比对；runbook 与外部 artifacts 任务台账已同步。
  - Validation evidence: python3 scripts/audit-rewrite-production-readiness.py --self-test; python3 scripts/audit-rewrite-production-readiness.py (expected BLOCKED without external evidence); git diff --check; node scripts/check-developer-copy-language.mjs --changed; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py validate USER-CN-REWRITE-READINESS-RAW-DIGEST-AUDIT-20260518; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 真实 30PB/千万级日查询外部生产 evidence 仍未提供，整体目标保持 blocked。
  - Next step: 等待 INBOX-005 指定的外部生产或准生产 artifacts，并运行 verifier/audit；readiness audit 必须同时拿到 verification-result.json 和原始 evidence directory 以复算摘要。

### USER-CN-REWRITE-READINESS-DIGEST-CONSISTENCY-AUDIT-20260518: 校验证据摘要双层一致性

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-REWRITE-READINESS-DIGEST-CONSISTENCY-AUDIT-20260518 require digest consistency`
- Priority: 1
- Depends on: USER-CN-BENCHMARK-EVIDENCE-DIGEST-MANIFEST-PERSIST-20260518
- Scope: 强化 audit-rewrite-production-readiness.py 对 verification-result.json 的证据摘要校验，要求顶层 evidenceFileDigests 与 scaleTargetEvidenceManifest.evidenceFileDigests 对每个必需文件的 sha256 和 sizeBytes 完全一致，并在 runbook 中说明不一致时不得声明 READY。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-READINESS-DIGEST-CONSISTENCY-AUDIT-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 强化改写生产就绪审计，要求 verification-result.json 顶层 evidenceFileDigests 与 scaleTargetEvidenceManifest.evidenceFileDigests 对每个必需文件的 sha256 和 sizeBytes 完全一致；self-test 覆盖摘要不一致时必须 BLOCKED；runbook 同步说明不一致不得声明 READY。
  - Validation evidence: python3 scripts/audit-rewrite-production-readiness.py --self-test；python3 scripts/audit-rewrite-production-readiness.py（预期 code=2/BLOCKED，无外部 verification-result.json）；git diff --check；node scripts/check-developer-copy-language.mjs --changed；node scripts/lint-repository-knowledge.js；python3 scripts/foreman.py validate USER-CN-REWRITE-READINESS-DIGEST-CONSISTENCY-AUDIT-20260518；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 真实 30PB/千万级日查询外部生产 evidence 仍未提供，整体目标保持 blocked。
  - Next step: 等待 INBOX-005 指定的外部生产或准生产 artifacts，并运行 verifier/audit 生成顶层与 manifest 摘要一致的 verification-result.json。

### USER-CN-BENCHMARK-EVIDENCE-DIGEST-HANDOFF-LEDGER-20260518: 同步生产证据摘要交接台账

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-BENCHMARK-EVIDENCE-DIGEST-HANDOFF-LEDGER-20260518 sync digest evidence handoff`
- Priority: 1
- Depends on: USER-CN-BENCHMARK-EVIDENCE-DIGEST-MANIFEST-PERSIST-20260518
- Scope: 同步 tasks.md 与 INBOX.md 中等待外部生产规模证据的交接说明，显式要求 evidenceFileDigests、原始 evidence directory 可复算 SHA-256，以及含摘要的 scaleTargetEvidenceManifest，避免外部 owner 仍按旧版无摘要清单交付。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-BENCHMARK-EVIDENCE-DIGEST-HANDOFF-LEDGER-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 同步 tasks.md 与 INBOX.md 中外部生产规模证据交接说明，显式要求 evidenceFileDigests、原始 evidence directory 可复算 SHA-256，以及含摘要的 scaleTargetEvidenceManifest。
  - Validation evidence: python3 scripts/task_audit.py --check --phase pre-closeout；node scripts/lint-repository-knowledge.js；python3 scripts/foreman.py validate USER-CN-BENCHMARK-EVIDENCE-DIGEST-HANDOFF-LEDGER-20260518；git diff --check。
  - Residual risk: 真实 30PB/千万级日查询外部生产 evidence 仍未提供，整体目标保持 blocked。
  - Next step: 等待 INBOX-005 指定的外部生产或准生产 artifacts，并归档原始 evidence directory、verification-result.json 与含 evidenceFileDigests 的 scaleTargetEvidenceManifest。

### USER-CN-BENCHMARK-EVIDENCE-DIGEST-MANIFEST-PERSIST-20260518: 持久化生产证据文件摘要

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-BENCHMARK-EVIDENCE-DIGEST-MANIFEST-PERSIST-20260518 persist evidence file digests`
- Priority: 1
- Depends on: USER-CN-BENCHMARK-EVIDENCE-FILE-DIGEST-AUDIT-20260518
- Scope: 把 evidenceFileDigests 纳入可提交的 scaleTargetEvidenceManifest、benchmark DTO/domain/persistence/report 链路，并让 verified production evidence 要求完整必需证据文件摘要，避免最终审计通过的摘要在 benchmark 任务提交时丢失。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-BENCHMARK-EVIDENCE-DIGEST-MANIFEST-PERSIST-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 把 evidenceFileDigests 纳入 scaleTargetEvidenceManifest 可提交模型、DTO、domain、persistence、report summary 和测试；生产 verified evidence 现在要求完整六个必需证据文件摘要；verifier 输出 manifest 内 digest map，readiness audit 同时要求顶层与 manifest 内摘要。
  - Validation evidence: python3 scripts/verify-benchmark-production-evidence.py --self-test；python3 scripts/audit-rewrite-production-readiness.py --self-test；mvn -pl benchmark-engine,sqlforge-shared -am -Dtest=BenchmarkScaleEvidenceManifestTest,BenchmarkTaskModelApplicationServiceTest,BenchmarkRecommendationComparisonApplicationServiceTest,BenchmarkReportControllerTest,MybatisBenchmarkTaskRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false test；mvn -pl benchmark-engine,sqlforge-shared -am test -Dsurefire.failIfNoSpecifiedTests=false；python3 scripts/audit-rewrite-production-readiness.py（预期 code=2/BLOCKED，无外部 verification-result.json）；node scripts/check-developer-copy-language.mjs --changed；node scripts/lint-repository-knowledge.js；git diff --check；python3 scripts/foreman.py validate USER-CN-BENCHMARK-EVIDENCE-DIGEST-MANIFEST-PERSIST-20260518；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 真实 30PB/千万级日查询外部生产 evidence 仍未提供，整体目标保持 blocked。
  - Next step: 等待 INBOX-005 指定的外部生产或准生产 artifacts，并提交包含 evidenceFileDigests 的 scaleTargetEvidenceManifest 与 verification-result.json。

### USER-CN-BENCHMARK-EVIDENCE-FILE-DIGEST-AUDIT-20260518: 增加生产证据文件摘要审计

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-BENCHMARK-EVIDENCE-FILE-DIGEST-AUDIT-20260518 add evidence file digests`
- Priority: 1
- Depends on: USER-CN-REWRITE-READINESS-MANIFEST-PROOF-AUDIT-20260518
- Scope: 让 verify-benchmark-production-evidence.py 输出必需证据文件的 SHA-256 和 sizeBytes，并让 audit-rewrite-production-readiness.py 要求 verification-result.json 包含这些文件摘要，便于把最终 READY 判断追溯到外部原始 artifacts。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-BENCHMARK-EVIDENCE-FILE-DIGEST-AUDIT-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 生产证据 verifier 现在输出必需 evidence 文件的 SHA-256 和 sizeBytes；生产就绪 audit 要求 verification-result.json 包含完整 evidenceFileDigests；runbook 说明归档 verification-result.json 时必须保存原始 evidence directory 并可复算摘要。
  - Validation evidence: python3 scripts/verify-benchmark-production-evidence.py --self-test；python3 scripts/audit-rewrite-production-readiness.py --self-test；python3 scripts/audit-rewrite-production-readiness.py（预期 code=2/BLOCKED，无外部 verification-result.json）；git diff --check；node scripts/check-developer-copy-language.mjs --changed；node scripts/lint-repository-knowledge.js；python3 scripts/foreman.py validate USER-CN-BENCHMARK-EVIDENCE-FILE-DIGEST-AUDIT-20260518；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 真实 30PB/千万级日查询外部生产 evidence 仍未提供，整体目标保持 blocked。
  - Next step: 等待 INBOX-005 指定的外部生产或准生产 artifacts，并归档原始 evidence directory 与含 evidenceFileDigests 的 verification-result.json。

### USER-CN-REWRITE-READINESS-MANIFEST-PROOF-AUDIT-20260518: 强化生产就绪 manifest 证据审计

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-REWRITE-READINESS-MANIFEST-PROOF-AUDIT-20260518 require manifest proof refs`
- Priority: 1
- Depends on: USER-CN-REWRITE-PRODUCTION-READINESS-AUDIT-20260518
- Scope: 强化 audit-rewrite-production-readiness.py 对外部 verification-result.json 的校验，要求 manifest 级 VERIFIED 状态、PRODUCTION_EVIDENCE_DIRECTORY 来源、所有生产证据 proofRef 与 workloadReplayWindow，避免缺少证据引用的手写 JSON 通过完成度审计。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-READINESS-MANIFEST-PROOF-AUDIT-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 生产就绪审计现在要求 external verification manifest 包含 PRODUCTION_EVIDENCE_DIRECTORY 来源、manifest 级 VERIFIED 状态、所有生产证据 proofRef 与 workloadReplayWindow；self-test 覆盖缺失 proofRef 时必须 BLOCKED。
  - Validation evidence: python3 scripts/audit-rewrite-production-readiness.py --self-test；python3 scripts/audit-rewrite-production-readiness.py（预期 code=2/BLOCKED，无外部 verification-result.json）；node scripts/check-developer-copy-language.mjs --changed；git diff --check；node scripts/lint-repository-knowledge.js；python3 scripts/foreman.py validate USER-CN-REWRITE-READINESS-MANIFEST-PROOF-AUDIT-20260518；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 真实 30PB/千万级日查询外部生产 evidence 仍未提供，整体目标保持 blocked。
  - Next step: 等待 INBOX-005 指定的外部生产或准生产 artifacts，并提供包含完整 proofRef 的 verification-result.json。

### USER-CN-BENCHMARK-EVIDENCE-DAILY-VOLUME-CLI-20260518: 显式化日查询量证据阈值 CLI

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-BENCHMARK-EVIDENCE-DAILY-VOLUME-CLI-20260518 expose daily volume threshold`
- Priority: 1
- Depends on: USER-CN-REWRITE-PRODUCTION-READINESS-AUDIT-20260518
- Scope: 让生产规模压测证据 verifier 像并发、数据规模和 replay 时长一样显式暴露日查询量阈值参数，并在 runbook 中固定本目标使用 10000000，减少外部 evidence owner 交付歧义。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-BENCHMARK-EVIDENCE-DAILY-VOLUME-CLI-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 生产规模压测证据 verifier 新增 --min-daily-query-volume，evaluate_evidence_dir 使用显式日查询阈值，self-test 覆盖默认失败与覆盖阈值通过路径；runbook 在校验命令中显式固定 10000000 日查询目标。
  - Validation evidence: python3 scripts/verify-benchmark-production-evidence.py --help；python3 scripts/verify-benchmark-production-evidence.py --self-test；python3 scripts/audit-rewrite-production-readiness.py --self-test；python3 scripts/audit-rewrite-production-readiness.py（预期 code=2/BLOCKED，无外部 verification-result.json）；node scripts/check-developer-copy-language.mjs --changed；node scripts/lint-repository-knowledge.js；git diff --check；python3 scripts/foreman.py validate USER-CN-BENCHMARK-EVIDENCE-DAILY-VOLUME-CLI-20260518；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 真实 30PB/千万级日查询外部生产 evidence 仍未提供，整体目标保持 blocked。
  - Next step: 等待 INBOX-005 指定的外部生产或准生产 artifacts，然后按 runbook 使用 --min-daily-query-volume 10000000 生成 verification-result.json。

### USER-CN-REWRITE-LATEST-RESEARCH-REFRESH-20260518: 刷新 SQL 改写最新研究证据

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-REWRITE-LATEST-RESEARCH-REFRESH-20260518 refresh latest rewrite research markers`
- Priority: 1
- Depends on: USER-CN-REWRITE-PRODUCTION-READINESS-AUDIT-20260518
- Scope: 把 2026-03 至 2026-05 新近 SQL query rewrite / cost-based rewrite / learned rule discovery 来源补入研究归档，并让生产就绪审计显式检查这些最新研究标记，避免以旧调研替代最新方案分析。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-LATEST-RESEARCH-REFRESH-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 补充 2026-03 至 2026-05 新近 SQL 改写研究与引擎优化来源，包括 cost-dependent rewrite、LASER/SQL-GRPO、SLER、Trino 481 和 Snowflake 2026 performance improvements；生产就绪审计同步要求这些最新研究标记。
  - Validation evidence: python3 scripts/audit-rewrite-production-readiness.py --self-test；python3 scripts/audit-rewrite-production-readiness.py（预期 code=2/BLOCKED，无外部 verification-result.json）；node scripts/check-developer-copy-language.mjs --changed；git diff --check；node scripts/lint-repository-knowledge.js；python3 scripts/foreman.py validate USER-CN-REWRITE-LATEST-RESEARCH-REFRESH-20260518；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 真实 30PB/千万级日查询外部生产 evidence 仍未提供，整体目标保持 blocked。
  - Next step: 等待 INBOX-005 指定的外部生产或准生产 artifacts，然后运行 verify-benchmark-production-evidence.py 和 audit-rewrite-production-readiness.py。

### USER-CN-REWRITE-PRODUCTION-READINESS-AUDIT-20260518: 新增改写推荐生产就绪完成度审计 CLI

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-REWRITE-PRODUCTION-READINESS-AUDIT-20260518 add rewrite production readiness audit`
- Priority: 1
- Depends on: USER-CN-REWRITE-PRODUCTION-SCALE-GATE-20260518
- Scope: 新增一个 repo-side 完成度审计入口，把 SQL 推荐改写调研、50+ SELECT 规则覆盖、productionScaleGate、禁止静态自动应用与外部生产规模 verification-result.json 串成 prompt-to-artifact checklist；没有外部 VERIFIED 证据时必须输出 BLOCKED/非零退出，避免把仓库侧测试误判为 30PB/千万级日查询目标完成。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-PRODUCTION-READINESS-AUDIT-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 SQL 改写生产就绪审计 CLI；把压测生产证据模型、校验器、DTO、持久化、报告和文档补齐千万级日查询证明要求；生产就绪审计在缺少外部 verification-result.json 时返回 BLOCKED。
  - Validation evidence: python3 scripts/verify-benchmark-production-evidence.py --self-test；python3 scripts/audit-rewrite-production-readiness.py --self-test；python3 scripts/audit-rewrite-production-readiness.py（预期 code=2/BLOCKED，无外部 verification-result.json）；mvn -pl sql-optimization,sqlforge-shared -am -Dtest=SqlOptimizationPipelineServiceTest#shouldBuildLayeredRecommendationRuleOutputModel,RewriteTrialApplicationServiceTest#shouldCreateSingleTrialRecommendationFromSafeParseProblems -Dsurefire.failIfNoSpecifiedTests=false test；mvn -pl benchmark-engine,sql-optimization,sqlforge-shared -am test -Dsurefire.failIfNoSpecifiedTests=false；node scripts/lint-repository-knowledge.js；node scripts/check-developer-copy-language.mjs --changed；git diff --check；python3 scripts/foreman.py validate USER-CN-REWRITE-PRODUCTION-READINESS-AUDIT-20260518；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 真实 30PB/千万级日查询生产 evidence 目录和 verification-result.json 尚未由外部环境提供，目标整体仍保持阻断。
  - Next step: 由生产/预发环境提供包含 daily-query-volume.json 的 evidence 目录，运行 verify-benchmark-production-evidence.py 输出 verification-result.json 后再运行 audit-rewrite-production-readiness.py。

### USER-CN-REWRITE-PRODUCTION-SCALE-GATE-20260518: 在改写推荐模型中显式携带生产规模证据门禁

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-REWRITE-PRODUCTION-SCALE-GATE-20260518 add production scale gate to rewrite recommendations`
- Priority: 1
- Depends on: USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-BLOCKER-20260518
- Scope: 在 SELECT 推荐改写核心输出中加入生产规模 readiness gate，明确 30PB 数据布局、千万级日查询、10000 并发、24 小时 replay、P95/P99、扫描字节、CPU、队列等待和成本账单均需要外部 VERIFIED 证据；保持静态推荐不自动应用、不执行任意 SQL、不把 repo-side 测试或 verifier 误写为生产达标。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-PRODUCTION-SCALE-GATE-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 在 SELECT 推荐改写核心输出中新增 productionScaleGate，随 expectedBenefit 与 estimatedCost 一起进入规则输出和 rewrite trial 持久化推荐，明确 10000 并发、30PB 数据布局、千万级日查询、24 小时 replay、P95/P99、扫描字节、CPU、队列等待和成本账单均需要外部 VERIFIED 证据；保持静态推荐不自动应用、不执行任意 SQL、不把 EXPLAIN_ONLY 或 repo-side verifier 当作生产达标。
  - Validation evidence: mvn -pl sql-optimization,sqlforge-shared -am -Dtest=SqlOptimizationPipelineServiceTest#shouldBuildLayeredRecommendationRuleOutputModel+shouldExposeAtLeastFiftySelectRewriteRecommendationScenarios,RewriteTrialApplicationServiceTest#shouldCreateSingleTrialRecommendationFromSafeParseProblems+shouldCarryExplainPlanEvidenceFromParseBatchIntoRecommendation -Dsurefire.failIfNoSpecifiedTests=false test passed with 4 tests; mvn -pl sql-optimization,sqlforge-shared -am test -Dsurefire.failIfNoSpecifiedTests=false passed with 252 tests; python3 scripts/foreman.py validate USER-CN-REWRITE-PRODUCTION-SCALE-GATE-20260518 passed; node scripts/lint-repository-knowledge.js passed; git diff --check passed.
  - Residual risk: 真实外部生产或准生产 artifacts 仍未提供；本任务只把生产规模证据边界下沉到改写推荐 payload，不能证明 30PB、10000 并发、千万级日查询或成本账单已经达标。
  - Next step: 等待 INBOX-005 外部 owner 提供 evidence directory 并运行 scripts/verify-benchmark-production-evidence.py 生成 VERIFIED verification-result.json，再把 scaleTargetEvidenceManifest 提交到 benchmark 任务。

### USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-BLOCKER-20260518: 登记生产规模压测外部证据阻塞项

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-BLOCKER-20260518 record external production evidence blocker`
- Priority: 1
- Depends on: USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-INGEST-20260518
- Scope: 将真实生产或准生产环境压测 artifacts 缺口登记为显式 blocked follow-up 和 INBOX 决策项，要求外部 owner 提供 10000 并发、30PB 数据布局、24 小时 replay、P95/P99、扫描字节、CPU、队列等待、成本账单与 verification-result.json；仓库侧不得把 verifier/runbook 误判为目标完成。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-BLOCKER-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增生产规模压测外部证据 blocked follow-up 与 INBOX-005，明确真实 production/pre-production artifacts、verification-result.json、scaleTargetEvidenceManifest 提交流程和 owner 决策项；仓库侧 verifier/runbook/fixtures 不得被当作目标完成证据。
  - Validation evidence: python3 scripts/task_audit.py --check --phase pre-closeout；node scripts/lint-repository-knowledge.js；git diff --check；python3 scripts/foreman.py validate USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-BLOCKER-20260518。
  - Residual risk: 真实外部生产规模 artifacts 仍未提供；当前提交只把等待项纳入治理台账，不能证明 10000 并发、30PB 数据布局、24 小时 replay、P95/P99、扫描字节、CPU、队列等待或成本账单已经达成。
  - Next step: 等待 INBOX-005 人工确认外部环境、执行窗口、证据归档位置和 owner 后，按 docs/deployments/benchmark-production-evidence-runbook.md 运行 verifier 并提交通过后的 manifest。

### USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-INGEST-20260518: 补齐生产规模证据目录导入校验入口

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-INGEST-20260518 add production evidence verifier CLI`
- Priority: 1
- Depends on: USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-VERIFIER-20260518
- Scope: 新增可执行的外部生产规模证据目录校验入口和 runbook，要求从真实外部 artifacts 读取 10000 并发、30PB 数据布局、24 小时负载回放、P95/P99、扫描字节、CPU、队列等待与成本账单证据，生成可提交到 scaleTarget.evidenceManifest 的 verificationBundle/manifest JSON；缺失或不达标必须失败，不生成 VERIFIED，不在仓库内伪造生产 artifacts。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-INGEST-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增外部生产规模证据目录校验 CLI 与 runbook，读取 concurrency/data-layout/workload-replay/metrics/cost-bill artifacts，达标时生成可提交到 scaleTarget.evidenceManifest 的 VERIFIED verificationBundle，缺失或不达标时输出 UNVERIFIED 并失败。
  - Validation evidence: python3 scripts/verify-benchmark-production-evidence.py --self-test；临时 evidence 目录 CLI 输出校验；node scripts/check-developer-copy-language.mjs --changed；node scripts/lint-repository-knowledge.js；git diff --check；python3 scripts/foreman.py validate USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-INGEST-20260518；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 仍未提供真实外部生产或准生产 artifacts；本任务只交付证据目录校验入口和 manifest 生成边界，不能替代 10000 并发、30PB、24 小时 replay、P95/P99、扫描字节、CPU、队列等待和成本账单的真实证明。
  - Next step: 外部环境 owner 按 runbook 生成 evidence directory，运行校验脚本并归档 verification-result.json，再把通过的 scaleTargetEvidenceManifest 提交到 benchmark 任务。

### USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-VERIFIER-20260518: 接入生产规模证据 bundle 校验器

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-VERIFIER-20260518 gate production evidence with verification bundle`
- Priority: 1
- Depends on: USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-MANIFEST-20260518
- Scope: 在 benchmark-engine 的 scaleTarget.evidenceManifest 中接入结构化 production evidence bundle 与 verifier，逐项校验 10000 并发、30PB 数据布局、长期 workload replay、P95/P99、扫描字节、CPU、队列等待和成本账单；只有 bundle 真实满足阈值时才允许 productionExternalVerification 作为 scaleReadiness 证据，缺失或不达标时继续 NOT_PROVEN/PARTIAL，不提供或伪造外部生产 artifacts。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-VERIFIER-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added structured production evidence verificationBundle under scaleTarget.evidenceManifest, with domain validation for 10000 concurrency, 30PB byte-level data layout, 24-hour replay, P95/P99, scan bytes, CPU, queue wait, cost bill and verifierRef; scaleReadiness now accepts productionExternalVerification only when externalVerificationStatus=VERIFIED and the bundle satisfies those thresholds. Persisted/report/export/recommendation-comparison paths and docs/tests were updated.
  - Validation evidence: mvn -pl benchmark-engine,sqlforge-shared -am -Dtest=BenchmarkScaleEvidenceManifestTest,BenchmarkTaskModelApplicationServiceTest,BenchmarkRecommendationComparisonApplicationServiceTest,BenchmarkReportControllerTest,MybatisBenchmarkTaskRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false test; mvn -pl benchmark-engine,sqlforge-shared -am validate pmd:pmd checkstyle:check; mvn -pl benchmark-engine,sqlforge-shared -am test -Dsurefire.failIfNoSpecifiedTests=false; node scripts/check-developer-copy-language.mjs --changed; node scripts/lint-repository-knowledge.js; git diff --check; python3 scripts/foreman.py validate USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-VERIFIER-20260518; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: This adds a repository-side evidence verifier boundary but still does not provide real production 10000-concurrency, 30PB data layout, long replay, P95/P99, scan/CPU/queue or cost bill artifacts. READY still requires externally supplied and verified production evidence.
  - Next step: Collect real production benchmark artifacts, populate verificationBundle from those artifacts, and only set externalVerificationStatus=VERIFIED after independent verification.

### USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-MANIFEST-20260518: 接入生产规模证据 manifest

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-MANIFEST-20260518 add production scale evidence manifest`
- Priority: 1
- Depends on: USER-CN-BENCHMARK-SCALE-READINESS-REPORT-20260518
- Scope: 在 benchmark-engine 的 scaleTarget/scaleReadiness 链路中接入 external scale evidence manifest，要求真实 10000 并发、30PB 数据布局、长窗口 workload replay、P95/P99/扫描字节/CPU/队列等待和成本账单以结构化 evidence refs 输入并持久化/导出；缺失时继续输出 NOT_PROVEN 或 PARTIAL，不把声明目标误写成生产证明。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-MANIFEST-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added scaleTarget evidenceManifest DTO/domain mapping for production concurrency, 30PB data layout, long replay, metric, scan/cpu/queue and cost bill proof refs; scaleReadiness now records satisfied/missing production evidence and requires VERIFIED external status before using refs as target/workload coverage; JSON/raw/export/persistence/recommendation-comparison/docs are covered.
  - Validation evidence: mvn -pl benchmark-engine,sqlforge-shared -am test -Dsurefire.failIfNoSpecifiedTests=false; mvn -pl benchmark-engine,sqlforge-shared -am validate pmd:pmd checkstyle:check; mvn -pl benchmark-engine,sqlforge-shared -am -Dtest=BenchmarkTaskModelApplicationServiceTest,BenchmarkRecommendationComparisonApplicationServiceTest,BenchmarkReportControllerTest,MybatisBenchmarkTaskRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false test; node scripts/check-developer-copy-language.mjs --changed; node scripts/lint-repository-knowledge.js; git diff --check; python3 scripts/foreman.py validate USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-MANIFEST-20260518; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: This only ingests and preserves evidence refs/status; it does not supply actual production 10000-concurrency, 30PB, long replay, metric or bill artifacts. Unverified refs remain NOT_PROVEN/PARTIAL until external artifacts are provided and verified.
  - Next step: Collect real environment benchmark artifacts and set externalVerificationStatus=VERIFIED through a follow-up verifier before claiming READY.

### USER-CN-BENCHMARK-SCALE-READINESS-REPORT-20260518: 补齐压测报告规模就绪评估

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-BENCHMARK-SCALE-READINESS-REPORT-20260518 add benchmark scale readiness reporting`
- Priority: 1
- Depends on: N/A
- Scope: 在 benchmark-engine 报告层从 scaleTarget 生成 scaleReadiness 评估，基于实际报告指标、并发/数据集覆盖、日查询量投影、workload 来源和成本/资源单元估算输出 NOT_PROVEN/PARTIAL/READY 证据状态，并暴露到 JSON/raw-data/export 与持久化 execution_summary_json。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-BENCHMARK-SCALE-READINESS-REPORT-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Benchmark scaleTarget now emits persisted scaleReadiness in execution summary, report JSON, raw-data, export, and governance trace, with NOT_PROVEN/PARTIAL/READY status derived from observed latency, scan bytes, CPU, memory, queue wait, workload evidence, target coverage, daily capacity projection, and resource-unit estimate.
  - Validation evidence: Passed: mvn -pl benchmark-engine,sqlforge-shared -am test -Dsurefire.failIfNoSpecifiedTests=false; mvn -pl benchmark-engine,sqlforge-shared -am validate pmd:pmd checkstyle:check; node scripts/check-developer-copy-language.mjs --changed; node scripts/lint-repository-knowledge.js; git diff --check; python3 scripts/foreman.py validate USER-CN-BENCHMARK-SCALE-READINESS-REPORT-20260518; python3 scripts/task_audit.py --check --phase pre-closeout.
  - Residual risk: Scale readiness is an evidence boundary from current benchmark reports, not real 10000-concurrency, 30PB, long replay, or production cost bill proof; missing external production-grade evidence remains NOT_PROVEN or PARTIAL.
  - Next step: Feed real long-running production benchmark evidence, 30PB data layout proof, and cost bills into scaleTarget/report inputs before claiming READY.

### USER-CN-BENCHMARK-SCALE-READINESS-20260518: 补齐推荐对比压测生产规模目标证据

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-BENCHMARK-SCALE-READINESS-20260518 add benchmark scale target evidence boundary`
- Priority: 1
- Depends on: N/A
- Scope: 为 benchmark-engine 推荐对比压测请求、任务上下文、持久化载体和状态响应增加生产规模目标与证据边界，覆盖 10000 并发、30PB 数据量、千万日查询、高复杂度和成本目标，不把目标误报为实测证明。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-BENCHMARK-SCALE-READINESS-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 为 benchmark-engine 推荐对比压测和通用任务上下文新增 scaleTarget，覆盖目标并发、数据集、日查询量、复杂度和成本目标；贯通 DTO、领域对象、状态响应、MyBatis 持久化、迁移、case evidence、audit summary、execution summary notes 与架构文档，并固定 TARGET_DECLARED_UNVERIFIED 证据边界，避免把 10000 并发、30PB、千万日查询目标误报为实测能力。
  - Validation evidence: preflight passed; mvn -pl benchmark-engine,sqlforge-shared -am -DskipTests compile passed; focused tests BenchmarkRecommendationComparisonApplicationServiceTest,BenchmarkTaskModelApplicationServiceTest,BenchmarkTaskControllerTest,BenchmarkTaskStateFlowTest passed 19 tests; cross-module mvn -pl benchmark-engine,query-execution,sql-optimization,sqlforge-shared -am test passed; final benchmark-engine full test passed 77 benchmark tests plus 22 shared tests; repository knowledge lint passed; git diff --check passed; foreman validate passed; pre-closeout audit passed.
  - Residual risk: 本任务只把生产规模目标与未验证证据边界结构化接入 benchmark 链路，不构成真实 10000 并发、30PB 数据集、千万日查询或成本最优证明；仍需外部生产级压测环境、长窗口 workload、P95/P99、扫描量、CPU/内存和成本账单证据。
  - Next step: 继续接入真实规模压测执行/报告聚合与成本证据归档，把 scaleTarget 从声明目标推进为可验证 benchmark/report evidence。

### USER-CN-REWRITE-RUNTIME-BENEFIT-GATE-20260518: 改写发布增加运行时收益门禁

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-REWRITE-RUNTIME-BENEFIT-GATE-20260518 require positive runtime benefit for publish`
- Priority: 1
- Depends on: N/A
- Scope: 在只读 result digest 校验链路中汇总 original/recommended executionEvidence 的 elapsedMs、scannedRows 为 runtimeDelta，输出 benefitStatus；发布运行时改写前要求最近一次等价校验的 runtimeDelta.benefitStatus=POSITIVE，避免仅结果等价但无收益或收益回退的改写进入默认运行时。仅使用现有 validation run executionEvidence JSON，不新增 schema，不执行额外 SQL。
- Validation:
  - `mvn -pl sql-optimization,query-execution,sqlforge-shared -am -Dtest=ResultDigestComparisonEngineTest,RewritePublishEligibilityPolicyTest,AccelerationRewriteContractApplicationServiceTest,ProductionRewriteClosedLoopEndToEndTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - `mvn -pl sql-optimization,query-execution,sqlforge-shared -am test -Dsurefire.failIfNoSpecifiedTests=false`
  - `node scripts/lint-repository-knowledge.js`
  - `git diff --check`
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-RUNTIME-BENEFIT-GATE-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-18: added runtimeDelta derivation from readonly digest executionEvidence elapsedMs/scannedRows with POSITIVE/NEUTRAL/REGRESSED/UNKNOWN benefitStatus.
  - 2026-05-18: tightened rewrite publish eligibility to reject missing or non-positive latest validation benefit evidence, preserving existing equivalent-result and approval gates.
- Context closeout:
  - Completed scope: 在只读 result digest 比较中基于 existing executionEvidence elapsedMs/scannedRows 生成 runtimeDelta 和 benefitStatus；改写发布资格要求最近一次等价 validation run 的 runtimeDelta.benefitStatus=POSITIVE，缺失、NEUTRAL、REGRESSED 或 UNKNOWN 均阻止默认运行时发布；仅复用 validation run executionEvidence JSON，不新增 schema，不执行额外 SQL。
  - Validation evidence: mvn -pl sql-optimization,query-execution,sqlforge-shared -am -Dtest=ResultDigestComparisonEngineTest,RewritePublishEligibilityPolicyTest,AccelerationRewriteContractApplicationServiceTest,ProductionRewriteClosedLoopEndToEndTest -Dsurefire.failIfNoSpecifiedTests=false test passed with 33 tests; mvn -pl sql-optimization,query-execution,sqlforge-shared -am test -Dsurefire.failIfNoSpecifiedTests=false passed with 252 tests; node scripts/lint-repository-knowledge.js passed; git diff --check passed; python3 scripts/foreman.py validate USER-CN-REWRITE-RUNTIME-BENEFIT-GATE-20260518 passed; python3 scripts/task_audit.py --check --phase pre-closeout passed.
  - Residual risk: 本任务收紧运行时发布门禁并计算收益状态，但收益证据仍来自当前只读 digest executionEvidence；尚未完成真实生产长周期 P95/P99、并发压测、30PB 数据布局、冷/热缓存分层或成本账单级验证。
  - Next step: 继续补齐真实负载回放与 benchmark/reporting，把 runtimeDelta 扩展为多窗口 P95/P99、扫描字节、CPU/队列等待和成本账单证据。

### USER-CN-REWRITE-EXPLAIN-EVIDENCE-20260518: 串联 EXPLAIN 证据到改写试算推荐

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-REWRITE-EXPLAIN-EVIDENCE-20260518 carry explain evidence into rewrite trials`
- Priority: 1
- Depends on: N/A
- Scope: 把 parse batch 已有的 Hetu EXPLAIN planAnalysisJson 带入 rewrite trial 来源问题与推荐证据模型：sourceProblems 附带 explain plan 摘要，推荐 evidenceLevel 从纯 STATIC_PARSE 提升为带 EXPLAIN 的 MIXED，仅作为 EXPLAIN_ONLY 证据；不执行任意 SQL、不新增 schema、不改变自动应用边界。
- Validation:
  - `mvn -pl sql-optimization,sqlforge-shared -am -Dtest=RewriteTrialApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - `mvn -pl sql-optimization,sqlforge-shared -am test -Dsurefire.failIfNoSpecifiedTests=false`
  - `node scripts/lint-repository-knowledge.js`
  - `git diff --check`
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-EXPLAIN-EVIDENCE-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-18: wired parse batch planAnalysisJson into rewrite trial sourceProblems as bounded EXPLAIN_ONLY planEvidence.
  - 2026-05-18: promoted persisted recommendations with successful EXPLAIN evidence to MIXED evidenceLevel while preserving no-real-gain and no-result-equivalence claim boundaries.
- Context closeout:
  - Completed scope: 把 parse batch 已有 Hetu EXPLAIN planAnalysisJson 带入 rewrite trial 来源问题和持久化推荐证据模型：sourceProblems 增加 EXPLAIN_ONLY planEvidence 摘要，成功 EXPLAIN 的 recommendation evidenceLevel 提升为 MIXED，并在 expectedBenefit/estimatedCost 中保留非真实收益、非结果等价边界；不执行任意 SQL、不新增 schema、不改变自动应用边界。
  - Validation evidence: mvn -pl sql-optimization,sqlforge-shared -am -Dtest=RewriteTrialApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test passed with 7 tests; mvn -pl sql-optimization,sqlforge-shared -am test -Dsurefire.failIfNoSpecifiedTests=false passed with 250 tests; node scripts/lint-repository-knowledge.js passed; git diff --check passed; python3 scripts/foreman.py validate USER-CN-REWRITE-EXPLAIN-EVIDENCE-20260518 passed; python3 scripts/task_audit.py --check --phase pre-closeout passed.
  - Residual risk: 本任务只接入 parse batch 已有 EXPLAIN 证据并保留 EXPLAIN_ONLY 边界，仍未完成真实结果等价验证、执行前后成本对比、运行时统计回放、并发压测或 30PB 数据布局证据。
  - Next step: 继续接入 rewrite validation / benchmark 比对，把 candidate SQL 与原 SQL 的结果等价、EXPLAIN 前后差异和运行时收益形成闭环。

### USER-CN-REWRITE-50-TRIAL-INTEGRATION-20260518: 串联 50 类改写规则到试算问题链路

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-REWRITE-50-TRIAL-INTEGRATION-20260518 wire 50 rewrite rules into trial flow`
- Priority: 1
- Depends on: USER-CN-SELECT-REWRITE-50-20260518
- Scope: 在 USER-CN-SELECT-REWRITE-50-20260518 已扩展 50+ SELECT 推荐规则的基础上，修复 rewrite trial 来源问题识别和 allowlist 仍停留在旧规则集的问题，使 50+ 规则能进入试算来源问题、筛选、issueRuleLinks 和人工复核统计链路；不改变自动应用边界、不执行任意 SQL、不新增持久化 schema。
- Validation:
  - `mvn -pl sql-optimization,sqlforge-shared -am -Dtest=RewriteTrialApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - `mvn -pl sql-optimization,sqlforge-shared -am test -Dsurefire.failIfNoSpecifiedTests=false`
  - `node scripts/lint-repository-knowledge.js`
  - `git diff --check`
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-50-TRIAL-INTEGRATION-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-18: expanded rewrite trial source-problem derivation to include the 50+ recommendation rule catalog, while preserving safe-rule-only task submission.
  - 2026-05-18: added trial coverage for derived FULL_SCAN_FILTER_GUARD and provided APPROX_DISTINCT_SKETCH_MV source problems, plus adjusted aggregate statistics for the broader catalog.
- Context closeout:
  - Completed scope: 串联 USER-CN-SELECT-REWRITE-50-20260518 的 50+ SELECT 推荐规则到 rewrite trial 来源问题、issueRuleLinks、人工复核统计和提交前安全过滤链路；保留 safe-rule-only 自动提交边界，不执行任意 SQL，不新增持久化 schema。
  - Validation evidence: mvn -pl sql-optimization,sqlforge-shared -am -Dtest=RewriteTrialApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test passed; mvn -pl sql-optimization,sqlforge-shared -am test -Dsurefire.failIfNoSpecifiedTests=false passed with 249 tests; node scripts/lint-repository-knowledge.js passed; git diff --check passed; python3 scripts/foreman.py validate USER-CN-REWRITE-50-TRIAL-INTEGRATION-20260518 passed; python3 scripts/task_audit.py --check --phase pre-closeout passed.
  - Residual risk: 本任务仍是静态 trial 集成和单元/模块回归验证，尚未引入真实 EXPLAIN/cost/result diff、运行时统计、并发压测或 30PB 级数据布局证据；生产规模有效性需要后续任务补齐。
  - Next step: 接入真实 EXPLAIN 与运行时统计回放，把 50+ 规则从静态问题链路推进到成本/结果等价/收益证据闭环。

### USER-CN-SELECT-REWRITE-50-20260518: 扩展 SELECT 推荐改写核心到 50 类

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-SELECT-REWRITE-50-20260518 expand select rewrite recommendations to 50`
- Priority: 1
- Depends on: USER-CN-SELECT-REWRITE-36-20260518
- Scope: 基于 USER-CN-SELECT-REWRITE-36-20260518 的调研与实现，把 sql-optimization 推荐改写核心从 36+ 类提升到不少于 50 类常见复杂 SELECT 执行、解析与推荐改写场景；补齐规则输出、风险/前置条件、回归测试和文档索引；继续保持静态分析不自动应用高风险改写、不执行任意 SQL、审批和结果比对边界。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-SELECT-REWRITE-50-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-18: verified current primary sources for materialized-view rewrite, cost-based join planning, dynamic filtering, stale/partition-aware MV compensation, and LLM rewrite validation; kept SQLForge boundary deterministic and manual-review/pull-only for high-risk recommendations.
  - 2026-05-18: extended `SqlOptimizationPipelineService` with additional SELECT structural and physical recommendation rules covering full scans, unstable limit/sort, repeated expressions, UDFs, CTEs, string/window/sketch aggregates, array predicates, range joins, correlated/nested subqueries, and rollup-style precompute candidates.
  - 2026-05-18: passed focused regression `mvn -pl sql-optimization,sqlforge-shared -am -Dtest=SqlOptimizationPipelineServiceTest#shouldExposeAtLeastFiftySelectRewriteRecommendationScenarios -Dsurefire.failIfNoSpecifiedTests=false test`.
  - 2026-05-18: passed broader regression `mvn -pl sql-optimization,sqlforge-shared -am test -Dsurefire.failIfNoSpecifiedTests=false` with 247 tests, plus `node scripts/lint-repository-knowledge.js`, `python3 scripts/foreman.py validate USER-CN-SELECT-REWRITE-50-20260518`, `python3 scripts/task_audit.py --check --phase pre-closeout`, and `git diff --check`.
- Context closeout:
  - Completed scope: 完成最新 SQL 推荐改写方案/工具/专利/论文复核；将 sql-optimization 的 SELECT 推荐改写输出从 36+ 审计目标提升到 50+ 显式回归场景；新增 full scan、limit/order、重复表达式、UDF、CTE、字符串/窗口/sketch 聚合、数组谓词、range join、correlated/nested subquery、rollup lattice 等结构与物理候选规则；所有新增高风险规则仍保持 manual-review 或 PULL_ONLY_CANDIDATE，不执行任意 SQL、不自动应用。
  - Validation evidence: mvn -pl sql-optimization,sqlforge-shared -am -Dtest=SqlOptimizationPipelineServiceTest#shouldExposeAtLeastFiftySelectRewriteRecommendationScenarios -Dsurefire.failIfNoSpecifiedTests=false test; mvn -pl sql-optimization,sqlforge-shared -am test -Dsurefire.failIfNoSpecifiedTests=false; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py validate USER-CN-SELECT-REWRITE-50-20260518; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check
  - Residual risk: 本轮提升的是仓库内静态解析和推荐覆盖，不代表真实 10000 高并发、30PB 存储、千万级日查询环境已经实测达标；物化视图、动态过滤、sketch、分桶、文件整理、存储布局和 LLM/专利启发路径仍必须依赖真实 EXPLAIN、成本模型、结果 diff、审批和环境证据。
  - Next step: 后续接入真实 Hetu/MRS EXPLAIN、运行统计和结果 diff 证据后，可把 50+ 静态规则升级为带成本排序、灰度绑定和自动暂停条件的生产推荐闭环。

### USER-CN-SELECT-REWRITE-36-20260518: 扩展 SELECT 推荐改写核心到 36 类

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-SELECT-REWRITE-36-20260518 expand select rewrite recommendations`
- Priority: 1
- Depends on: USER-CN-REWRITE-TRIALS-20260518
- Scope: 基于最新 SQL rewrite / optimizer / materialized view / big data engine 方案研究，补强 sql-optimization 推荐改写核心规则目录与试算链路，使 SELECT 推荐改写覆盖不少于 36 类常见复杂场景；保留不执行任意 SQL、不自动应用高风险改写、审批和结果比对边界。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-SELECT-REWRITE-36-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-18: researched and archived current SQL rewrite, optimizer, materialized view, dynamic filtering, LLM rewrite verification, and patent references in `docs/references/sql-rewrite-recommendation-research-2026-05-18.md`.
  - 2026-05-18: expanded `SqlOptimizationPipelineService` recommendation output to cover 36+ SELECT structural and physical rewrite scenarios while keeping high-risk rewrites manual-review or pull-only.
  - 2026-05-18: added regression coverage in `SqlOptimizationPipelineServiceTest.shouldExposeAtLeastThirtySixSelectRewriteRecommendationScenarios`.
  - 2026-05-18: passed `mvn -pl sql-optimization,sqlforge-shared -am test -Dsurefire.failIfNoSpecifiedTests=false`.
  - 2026-05-18: passed `python3 scripts/foreman.py validate USER-CN-SELECT-REWRITE-36-20260518` after fixing document coverage and developer-copy language lint.
  - 2026-05-18: passed `python3 scripts/task_audit.py --check --phase pre-closeout`.
- Context closeout:
  - Completed scope: 完成 2026-05-18 SQL 推荐改写调研归档；扩展 sql-optimization SELECT 推荐输出到 36 类以上结构与物理改写场景；保持高风险改写人工复核或 pull-only 边界；补齐回归测试和文档索引。
  - Validation evidence: mvn -pl sql-optimization,sqlforge-shared -am test -Dsurefire.failIfNoSpecifiedTests=false；python3 scripts/foreman.py validate USER-CN-SELECT-REWRITE-36-20260518；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 静态推荐不替代真实引擎成本模型、结果 diff 或生产审批；物化视图、动态过滤、数据布局与索引类建议仍按 PULL_ONLY_CANDIDATE 输出。
  - Next step: 后续若接入真实 Hetu/MRS 或湖仓运行统计，可把当前规则信号与 cost/explain/result-diff 证据做排序和自动化灰度绑定。

### USER-CN-REWRITE-PAGE-LAYOUT-HARDENING-20260518: 改写系列页面布局规范加固

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-REWRITE-PAGE-LAYOUT-HARDENING-20260518 harden rewrite page layouts`
- Priority: 1
- Depends on: USER-CN-REWRITE-PAGE-DESIGN-FIX-20260518
- Scope: 深入审查 SQL 改写验证、推荐结果/改写记录、改写历史相关页面在卡片堆叠、上下分布、完整视口利用和管理页骨架方面的设计违规点，并在不新增后端 API、route、schema、不改变推荐/审批/发布/自动应用/执行历史语义的前提下做前端布局优化、契约验证和视觉自检。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-PAGE-LAYOUT-HARDENING-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 找出并修复改写系列页面设计违规：SQL 改写验证页去掉结果区下方两张独立工作卡片，改为结果面板内共享上下文的推荐关联/复验 Tab，并在创建改写记录后自动切到复验以保留连续操作；推荐结果/改写记录页把筛选和推荐表格合并为单一 recommendation workbench，避免 ToolbarShell + 列表卡片上下堆叠；改写历史页把标题、错误、筛选、表格收敛进单一 history workbench，用分区线替代 header/filter/table 三层卡片。before/after 截图自检完成，Codex 读图/视觉复核后修复了复验按钮被隐藏的回归。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-REWRITE-PAGE-LAYOUT-HARDENING-20260518；npm run lint；npm run build；npm run test:frontend-page-governance；npm run test:sql-ui-contract；npm run test:form-governance；node scripts/check-recommendation-page-contract.mjs；node scripts/check-history-page-contract.mjs；node scripts/check-history-detail-contract.mjs；node scripts/check-parse-workbench-contract.mjs；npm run smoke:frontend-dev；git diff --check。R-186 before screenshot/reference: .codex-log/USER-CN-REWRITE-PAGE-DESIGN-FIX-20260518/after-rewrite-validation-3.png、.codex-log/USER-CN-REWRITE-PAGE-DESIGN-FIX-20260518/after-recommendation.png、.codex-log/USER-CN-REWRITE-PAGE-DESIGN-FIX-20260518/after-rewrite-history.png；after screenshot: .codex-log/USER-CN-REWRITE-PAGE-LAYOUT-HARDENING-20260518/after-rewrite-validation.png、.codex-log/USER-CN-REWRITE-PAGE-LAYOUT-HARDENING-20260518/after-recommendation-center.png、.codex-log/USER-CN-REWRITE-PAGE-LAYOUT-HARDENING-20260518/after-sql-history.png；visual self-review passed，已修复截图暴露的隐藏复验操作问题，未发现主路径新增 visual drift。
  - Residual risk: 未改变后端 API、route、schema、推荐审批发布、自动应用或真实执行历史语义；推荐详情抽屉内部仍沿用原有详情分组，本轮只收敛主路径卡片堆叠和上下分布。
  - Next step: 若继续优化，可单独拆任务收敛推荐详情抽屉内部章节密度。

### USER-CN-REWRITE-PAGE-DESIGN-FIX-20260518: 改写系列页面设计规范回归修正

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-REWRITE-PAGE-DESIGN-FIX-20260518 align rewrite page design`
- Priority: 1
- Depends on: USER-CN-RECOMMENDATION-DIFF-LAYOUT-20260517,HARN-FE-007,HARN-FE-006
- Scope: 修正推荐结果/改写记录/改写历史/SQL 改写验证相关页面的前端布局，使其符合 docs/frontend/design-system.md 的管理页骨架、完整视口利用、避免卡片套卡片、优先左右/上下分区与紧凑 section 的规范；不新增后端 API、route、schema，不改变推荐、审批、发布、自动应用或 SQL 历史事实语义。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-PAGE-DESIGN-FIX-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 修正改写校验页桌面布局从纵向卡片堆叠回到左右工作区；扁平化结果摘要和证据区；补齐改写记录、改写历史深链入口的标题和上下文展示，避免复用页露出不符合改写系列语义的通用卡片堆叠。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-REWRITE-PAGE-DESIGN-FIX-20260518 --include-task-audit；npm run lint；npm run build；npm run test:frontend-page-governance；npm run test:sql-ui-contract；navigation/recommendation/history/detail/parse contract checks；npm run smoke:frontend-dev；git diff --check；R-186 before screenshot: .codex-log/USER-CN-REWRITE-PAGE-DESIGN-FIX-20260518/before-rewrite-validation.png、before-recommendation.png、before-rewrite-history.png、before-recommendation-drawer.png；after screenshot: .codex-log/USER-CN-REWRITE-PAGE-DESIGN-FIX-20260518/after-rewrite-validation-3.png、after-recommendation.png、after-rewrite-history.png、after-recommendation-drawer.png；Codex 读图截图自检确认改写校验页桌面左右工作区恢复、改写记录/改写历史入口标题语义正确、页面级横向溢出已修复。
  - Residual risk: 未发现已知残余风险；改写记录和改写历史仍复用既有列表/详情能力，后续若新增独立页面壳需继续遵守 frontend design-system 的非卡片堆叠约束。
  - Next step: 如后续扩展改写系列，应优先补页面契约脚本覆盖深链标题和布局断点，避免回退到上下卡片堆叠。

### USER-CN-REWRITE-TRIALS-UI-20260518: 改写试算前端入口与展示

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-REWRITE-TRIALS-UI-20260518 wire rewrite trial frontend`
- Priority: 1
- Depends on: N/A
- Scope: 在现有 Vue 前端接入解析问题驱动改写试算 API：单条解析页试算入口与结果、批量解析中心批量试算入口/状态、解析统计中心试算统计 tab，并补充前端契约验证。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-TRIALS-UI-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 接入改写试算前端服务 API，补齐单条 SQL 解析页试算入口/结果、批量解析中心批量试算入口/状态/详情、解析统计中心试算统计 tab，以及推荐详情来源问题与问题到规则 trace 展示。
  - Validation evidence: npm run build；npm run lint；npm run test:sql-ui-contract；npm run test:frontend-page-governance；python3 scripts/foreman.py validate USER-CN-REWRITE-TRIALS-UI-20260518；python3 scripts/task_audit.py --check --phase pre-closeout；git diff --check
  - Residual risk: 未启动浏览器后端联调；本次通过构建、lint 与前端治理脚本验证静态契约。
  - Next step: 联调真实 rewrite-trials 后端数据后，可按运营反馈微调列表密度和默认筛选。

### USER-CN-REWRITE-TRIALS-20260518: 解析问题驱动的 SQL 改写试算

- Status: done
- Completed at: 2026-05-18
- Commit subject: `USER-CN-REWRITE-TRIALS-20260518 implement issue-driven rewrite trials`
- Priority: 1
- Depends on: N/A
- Scope: Implement issue-driven SQL rewrite trial orchestration across sql-optimization APIs using sourceProblems and issueRuleLinks, preserving trial/recommendation/rewrite-record boundaries and adding tests for single and batch trial status semantics.
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-TRIALS-20260518`
- Progress log:
  - 2026-05-18: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 实现解析问题驱动的改写试算编排、API、批量入口、统计入口、推荐/改写记录 trace 透传、持久化表与迁移，并补充服务/控制器/Schema 测试。
  - Validation evidence: mvn -B -pl sql-optimization test；python3 scripts/foreman.py validate USER-CN-REWRITE-TRIALS-20260518；python3 scripts/task_audit.py --check --phase pre-closeout；git diff --check
  - Residual risk: 前端界面接入仍需后续任务承接；本次后端接口与统计 VO 已就绪。
  - Next step: 前端按 rewrite-trials API 接入单条解析页、批量解析中心和解析统计试算 tab。

### USER-CN-REWRITE-VALIDATION-PAGE-20260517: SQL 改写验证真实可用页面

- Status: done
- Completed at: 2026-05-17
- Commit subject: `feat(frontend): implement rewrite validation page`
- Priority: 1
- Depends on: N/A
- Scope: 按 SQL 改写功能分层设计实现独立 SQL 改写验证页面能力，替代当前复用解析功能的体验；复用现有 sql-optimization task/rewrite record/validation run/recommendation 接口，不新增后端 API、route schema 或生产自动改写语义；补齐页面任务流、推荐结果入口、验证运行证据、前端契约/视觉验证与台账审计。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-VALIDATION-PAGE-20260517`
- Progress log:
  - 2026-05-17: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 实现独立 SQL 改写验证页面并接入 /acceleration?mode=rewriteValidation；页面提交 REWRITE 优化任务，展示候选 SQL、SQL diff、规则链、风险、解析证据、推荐关联、改写记录草稿与验证运行；外层工作区标题按导航项显示 SQL 改写验证；保留推荐治理、改写记录、真实生产改写历史三层边界，不新增后端 API、route、schema，不标记生产已自动改写，不绕过审批发布门禁。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-REWRITE-VALIDATION-PAGE-20260517 --include-task-audit --extra-command 'node scripts/check-parse-workbench-contract.mjs' --extra-command 'npm run smoke:frontend-dev' --extra-command 'npm run test:form-governance' --extra-command 'git diff --check' passed；npm run lint、npm run build、npm run test:sql-ui-contract、npm run test:frontend-page-governance、node scripts/check-developer-copy-language.mjs --changed、node scripts/check-parse-workbench-contract.mjs、npm run test:form-governance、npm run smoke:frontend-dev、git diff --check passed；before/after 截图自检：before screenshot 未在实现前抓取，当前未提交工作树无法无损重建用户报告的旧页面状态；after screenshot .codex/tmp/rewrite-validation-after.png；Codex 读图/视觉复核发现外层标题仍显示 SQL解析、复验原因默认英文调试文案，已修复后重跑截图 smoke passed。
  - Residual risk: 无法提供实现前真实 before screenshot，只保留用户报告的问题描述、代码差异和 after screenshot 复核证据；真实外部 Hetu/MRS 环境验证仍按 HARN-016/INBOX-002 跟踪。
  - Next step: None.

### USER-CN-REWRITE-VALIDATION-RECOMMENDATION-DEEPLINK-20260517: SQL 改写验证到推荐结果来源深链

- Status: done
- Completed at: 2026-05-17
- Commit subject: `feat(frontend): link rewrite validation to recommendations`
- Priority: 1
- Depends on: USER-CN-SQL-REWRITE-VALIDATION-ENTRY-20260517,USER-CN-RECOMMENDATION-SOURCE-FILTER-20260517
- Scope: 复用现有 /acceleration 与 /governance/recommendations route，让 SQL 改写验证结果能按 parse history / parse task 来源打开推荐结果筛选；推荐页消费 URL 中的 sourceCategory/sourceObjectId/historyId/parseTaskId/sourceType/sourceKind 并保持后端分页接口为列表真值；不新增后端 API、route、schema，不改变推荐审批、发布、运行时绑定或 SQL 执行语义。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-VALIDATION-RECOMMENDATION-DEEPLINK-20260517`
- Progress log:
  - 2026-05-17: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 复用现有 /acceleration 与 /governance/recommendations 路由，在 SQL 改写验证结果区新增查看推荐结果入口；深链携带 tenantId、sourceCategory=SQL_PARSE、sourceObjectId、historyId、parseTaskId、sourceType、sourceKind，推荐结果页按 URL 恢复来源分类与来源对象并把 parse history / parse task 条件传给后端分页接口；保留推荐页列表以后端分页接口为真值，不新增后端 API、route、schema，不改变推荐审批、发布、运行时绑定或 SQL 执行语义。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-REWRITE-VALIDATION-RECOMMENDATION-DEEPLINK-20260517 --include-task-audit --extra-command node scripts/check-parse-workbench-contract.mjs --extra-command node scripts/check-recommendation-page-contract.mjs --extra-command node scripts/check-frontend-i18n-copy.mjs --extra-command npm run smoke:frontend-dev --extra-command git diff --check passed; direct npm run smoke:frontend-dev passed after adding historyId+parseTaskId browser assertions.
  - Residual risk: 推荐结果来源对象下拉仍沿用既有第一页 pageSize=50 选项加载策略；较大租户需要后续服务端搜索增强。真实外部 Hetu/MRS 环境验证仍按 HARN-016/INBOX-002 跟踪。
  - Next step: None.

### USER-CN-SQL-REWRITE-VALIDATION-ENTRY-20260517: SQL 改写验证入口语义补齐

- Status: done
- Completed at: 2026-05-17
- Commit subject: `feat(frontend): expose SQL rewrite validation entry`
- Priority: 1
- Depends on: USER-CN-SQL-REWRITE-FUNCTION-DESIGN-20260517,HARN-FE-007,USER-CN-SQL-REWRITE-HISTORY-EVIDENCE-COPY-20260517
- Scope: 复用现有单条 SQL 解析 route，将 SQL 改写验证作为设计第一功能面的正式入口语义补齐；更新导航、Dashboard/页面文案和静态契约，明确试算验证不等于推荐治理对象或真实生产改写历史；不新增后端 API、route、schema，不改变审批发布、运行时绑定或 SQL 执行语义。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-SQL-REWRITE-VALIDATION-ENTRY-20260517`
- Progress log:
  - 2026-05-17: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 复用现有 /acceleration route 增加 SQL 改写验证正式深链入口；在改写治理导航中补齐 SQL 改写验证、改写记录、改写历史三分层入口；单条 SQL 解析页在 mode=rewriteValidation 时显示试算验证标题和边界，明确推荐治理对象归推荐结果 / 改写记录，真实生产改写历史只能来自 SQL 执行历史审计字段；未新增后端 API、route、schema，未改变审批发布、运行时绑定或 SQL 执行语义。
  - Validation evidence: node scripts/check-navigation-shell-contract.mjs; node scripts/check-parse-workbench-contract.mjs; node scripts/check-frontend-i18n-copy.mjs; node scripts/check-developer-copy-language.mjs --changed; npm run test:sql-ui-contract; npm run test:frontend-page-governance; npm run lint; npm run build; npm run smoke:frontend-dev; git diff --check; python3 scripts/foreman.py validate USER-CN-SQL-REWRITE-VALIDATION-ENTRY-20260517 --include-task-audit; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 本任务仅补齐前端入口语义、页面文案和契约/smoke 覆盖；SQL 改写验证仍复用现有单条 SQL 解析链路，不新增独立验证页面或后端能力。真实外部 Hetu/MRS 环境验证继续按 HARN-016/INBOX-002 跟踪。
  - Next step: 若后续需要独立 SQL 改写验证页面或新增验证 API，需先按 sql-rewrite-function-boundary-design.md 重新 materialize 任务并人工确认新增 route/API/schema 边界。

### USER-CN-SQL-REWRITE-HISTORY-EVIDENCE-COPY-20260517: 显式区分改写记录关联与本次自动改写事实

- Status: done
- Completed at: 2026-05-17
- Commit subject: `fix(frontend): clarify rewrite history evidence copy`
- Priority: 1
- Depends on: HARN-FE-007,PRW-011,USER-CN-SQL-REWRITE-FUNCTION-DESIGN-20260517
- Scope: 补齐 SQL 历史详情在存在改写记录但 rewriteApplied=false 时的显式提示和契约检查，落实 SQL 改写功能分层设计；不新增后端 API、route、schema，不改变审批发布或运行时绑定语义。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-SQL-REWRITE-HISTORY-EVIDENCE-COPY-20260517`
- Progress log:
  - 2026-05-17: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: SQL 历史详情在存在关联推荐/改写记录但本次 rewriteApplied 不是 true 时，执行取证与改写记录 tab 均显示显式提示；同步中英文文案和历史详情契约检查，避免把治理对象关联误写成真实自动改写历史。
  - Validation evidence: node scripts/check-history-detail-contract.mjs; node scripts/check-history-page-contract.mjs; npm run lint; npm run build; npm run test:sql-ui-contract; npm run test:frontend-page-governance; node scripts/lint-repository-knowledge.js; node scripts/check-developer-copy-language.mjs --changed; git diff --check; python3 scripts/foreman.py validate USER-CN-SQL-REWRITE-HISTORY-EVIDENCE-COPY-20260517 --include-task-audit --extra-command 'node scripts/check-history-detail-contract.mjs' --extra-command 'node scripts/check-history-page-contract.mjs' --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'npm run test:frontend-page-governance' --extra-command 'npm run test:sql-ui-contract' --extra-command 'git diff --check'; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 本任务只补齐前端显式证据提示，不新增后端字段、API、独立 route 或运行时行为；真实环境回归仍沿用既有 PRW/HARN 环境增强项。
  - Next step: None.

### USER-CN-SQL-REWRITE-FUNCTION-DESIGN-20260517: Write SQL rewrite function boundary design

- Status: done
- Completed at: 2026-05-17
- Commit subject: `docs(product): document SQL rewrite function boundaries`
- Priority: 1
- Depends on: HARN-FE-007,PRW-012
- Scope: Document the three SQL rewrite function surfaces, include parse-history-sourced rewrite ownership, and align product boundaries without code or schema changes.
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-SQL-REWRITE-FUNCTION-DESIGN-20260517`
- Progress log:
  - 2026-05-17: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added a product design scheme for the three SQL rewrite function surfaces, including parse-history-sourced rewrite ownership, cross-page linkage, state evidence rules, boundaries, non-goals, and acceptance criteria. Linked the scheme from docs README, the SQL governance product spec, and the document coverage matrix.
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-SQL-REWRITE-FUNCTION-DESIGN-20260517 --extra-command 'node scripts/lint-repository-knowledge.js' --extra-command 'git diff --check'
  - Residual risk: Docs-only scheme; no frontend route, backend API, schema, or runtime behavior was changed.
  - Next step: Use docs/product/sql-rewrite-function-boundary-design.md as the product authority when materializing future SQL rewrite validation, recommendation/record, or rewrite history UI tasks.

### USER-CN-REWRITE-SAMPLE-VALIDATION-20260517: 验证第2类 SQL 解析与推荐改写

- Status: done
- Completed at: 2026-05-17
- Commit subject: `test: verify manual review rewrite samples USER-CN-REWRITE-SAMPLE-VALIDATION-20260517`
- Priority: 1
- Depends on: N/A
- Scope: 用第2类人工复核 SQL 样例实际跑 SQL 解析和推荐改写链路，确认推荐 SQL 是否保持原文、unappliedRules/manualReviewRequired 是否出现；若发现用户可见样例或实现口径误导，则修正并验证。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-REWRITE-SAMPLE-VALIDATION-20260517`
- Progress log:
  - 2026-05-17: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added focused regression coverage proving item-2 manual-review SQL samples do not produce safe candidate rewritten SQL and instead surface as unapplied rules requiring manual review.
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-REWRITE-SAMPLE-VALIDATION-20260517 --include-task-audit --extra-command 'mvn -pl sql-optimization,sqlforge-shared -am -Dtest=SqlOptimizationManualReviewSampleTest -Dsurefire.failIfNoSpecifiedTests=false test' --extra-command 'git diff --check'
  - Residual risk: Parse-triggered persisted rewrite recommendations remain limited to the configured target issue scenes; some manual-review shapes require an explicit rewrite task or a target parse issue scene to create a recommendation row.
  - Next step: Use L0 safe-rule samples when the expectation is visibly changed candidate SQL; use item-2 samples to verify unappliedRules and manual-review behavior.

### USER-CN-RECOMMENDATION-SOURCE-FILTER-20260517: 推荐结果来源类型联动筛选

- Status: done
- Completed at: 2026-05-17
- Commit subject: `feat: add recommendation source filters USER-CN-RECOMMENDATION-SOURCE-FILTER-20260517`
- Priority: 1
- Depends on: USER-CN-RECOMMENDATION-DIFF-LAYOUT-20260517
- Scope: 在推荐结果页增加来源类型筛选及来源对象联动选择，后端补齐推荐分页筛选参数，保留推荐详情/diff/审批发布边界；同时提供第2类改写验证SQL样例和第3类PULL_ONLY边界说明。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-RECOMMENDATION-SOURCE-FILTER-20260517`
- Progress log:
  - 2026-05-17: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added recommendation result source-category filtering with linked source-object selectors for query history, SQL parse history, parse batches, and report import batches; extended recommendation page API filtering through controller, service, repository, MyBatis XML, frontend API client, i18n, page contract, and focused backend coverage without changing recommendation diff, review, publish, dispatch, runtime binding, or SQL execution semantics.
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-RECOMMENDATION-SOURCE-FILTER-20260517 --include-task-audit --extra-command mvn... --extra-command node scripts/check-recommendation-page-contract.mjs --extra-command git diff --check passed; focused Maven recommendation service/controller tests passed; npm lint/build/sql-ui/frontend-page-governance passed.
  - Residual risk: Linked source-object option lists use the existing first-page list APIs with pageSize=50; larger tenants may need server-side search in a later UX hardening task. Semantic correctness for manual-review rewrite candidates still requires operator review and result-diff evidence.
  - Next step: Use the provided item-2 SQL samples for batch parse and inspect recommended SQL/result-diff evidence before approving any rewrite.

### USER-CN-SQL-FORMATTER-NESTED-20260517: 多层嵌套 SQL 格式化全局增强

- Status: done
- Completed at: 2026-05-17
- Commit subject: `feat(frontend): improve nested SQL formatting`
- Priority: 1
- Depends on: USER-CN-RECOMMENDATION-DIFF-DUAL-PANE-20260517
- Scope: 将前端共享 SQL formatter 升级为 sql-formatter trino dialect，覆盖 SQL 输入、输出和推荐 diff；raw evidence 默认保留原文但提供格式化切换；加固 SQL UI 与推荐页契约，确保深层嵌套 SQL 可读格式化且不改变后端 API、SQL 执行语义、推荐状态机、审批、发布或审计数据。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-SQL-FORMATTER-NESTED-20260517`
- Progress log:
  - 2026-05-17: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added sql-formatter as the shared frontend SQL formatter using the Trino dialect, preserved formatter failure fallback to normalized raw SQL, added raw evidence format/raw display toggling in SqlCodeBlock without mutating incoming SQL, replaced the acceleration governance SQL textarea with SqlEditorField, and strengthened SQL UI/recommendation/browser smoke contracts with deeply nested SQL fixtures.
  - Validation evidence: npm run test:sql-ui-contract; node scripts/check-recommendation-page-contract.mjs; npm run test:form-governance; npm run test:frontend-page-governance; npm run lint; npm run build; npm run smoke:frontend-dev; npm run smoke:production-rewrite-closed-loop; git diff --check; python3 scripts/foreman.py validate USER-CN-SQL-FORMATTER-NESTED-20260517 --include-task-audit --extra-command node scripts/check-recommendation-page-contract.mjs --extra-command npm run smoke:frontend-dev --extra-command npm run smoke:production-rewrite-closed-loop --extra-command git diff --check; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: sql-formatter may still fall back to raw text for unsupported Hetu-specific syntax; this remains a UI-only fallback and does not change submitted SQL, persisted history, backend APIs, recommendation state, approval, publish, or audit semantics.
  - Next step: No immediate follow-up required; future SQL UI regressions are guarded by the deep nesting contract and recommendation browser smokes.

### USER-CN-RECOMMENDATION-DIFF-DUAL-PANE-20260517: 推荐中心 SQL diff 双 Pane 滚动与操作按钮

- Status: done
- Completed at: 2026-05-17
- Commit subject: `feat(frontend): add dual pane sql diff USER-CN-RECOMMENDATION-DIFF-DUAL-PANE-20260517`
- Priority: 1
- Depends on: N/A
- Scope: Frontend-only follow-up for recommendation center SQL compare: split original and recommended SQL into independent linked-scroll panes, add per-pane copy and idempotent format actions, keep formatted frontend display/diff behavior and do not change backend diff API, recommendation lifecycle, approval, publish, dispatch, or SQL execution semantics.
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-RECOMMENDATION-DIFF-DUAL-PANE-20260517`
- Progress log:
  - 2026-05-17: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Frontend-only recommendation SQL diff display follow-up: refactored SqlCompareBlock into original/recommended independent panes with per-pane copy and idempotent format actions, linked horizontal/vertical scroll, formatted display/copy text, empty-state pane rendering, and retained formatted LCS plus token-level replacement highlighting and leading-comment carry-over behavior.
  - Validation evidence: npm run test:sql-ui-contract; node scripts/check-recommendation-page-contract.mjs; npm run test:frontend-page-governance; npm run lint; npm run build; npm run smoke:frontend-dev; npm run smoke:production-rewrite-closed-loop; git diff --check; python3 scripts/foreman.py validate USER-CN-RECOMMENDATION-DIFF-DUAL-PANE-20260517 --include-task-audit
  - Residual risk: No backend API, schema, recommendation lifecycle, approval, publish, dispatch, runtime binding, or SQL execution semantics changed. Copy and format actions are frontend-local display operations; browser smoke evidence uses repo-closed mock data.
  - Next step: None.

### USER-CN-RECOMMENDATION-DIFF-FORMAT-HIGHLIGHT-20260517: 推荐中心 SQL diff 前端格式化与词级高亮

- Status: done
- Completed at: 2026-05-17
- Commit subject: `feat: format recommendation sql diff USER-CN-RECOMMENDATION-DIFF-FORMAT-HIGHLIGHT-20260517`
- Priority: 1
- Depends on: USER-CN-RECOMMENDATION-DIFF-LAYOUT-20260517
- Scope: Frontend-only follow-up for recommendation center SQL compare formatting, leading comment display carry-over, word/token-level highlighting, and smoke/contract updates without backend API or execution semantic changes.
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-RECOMMENDATION-DIFF-FORMAT-HIGHLIGHT-20260517`
- Progress log:
  - 2026-05-17: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Frontend-only recommendation SQL diff formatting follow-up: extracted compare helper, carried source leading comments into recommended display SQL, rendered frontend line/token diff from formatted SQL, kept backend diff as raw evidence, and updated contract/browser smoke coverage.
  - Validation evidence: npm run test:sql-ui-contract; node scripts/check-recommendation-page-contract.mjs; npm run test:frontend-page-governance; npm run lint; npm run build; npm run smoke:frontend-dev; npm run smoke:production-rewrite-closed-loop; git diff --check; python3 scripts/foreman.py validate USER-CN-RECOMMENDATION-DIFF-FORMAT-HIGHLIGHT-20260517 --include-task-audit; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: No backend API, schema, recommendation lifecycle, dispatch, approval, publish, or execution semantics changed. Visual evidence remains frontend-derived; backend diff is retained only as raw evidence/AST/rule source.
  - Next step: None.

### USER-CN-RECOMMENDATION-DIFF-LAYOUT-20260517: 推荐结果页布局与 SQL Compare 收口

- Status: done
- Completed at: 2026-05-17
- Commit subject: `feat: reshape recommendation layout USER-CN-RECOMMENDATION-DIFF-LAYOUT-20260517`
- Priority: 1
- Depends on: `HARN-146`,`HARN-FE-006`,`HARN-FE-007`,`PRW-010`
- Scope: 推荐页必须以后端分页接口作为列表真值，详情通过右侧抽屉承载，SQL compare 只做 display evidence，不改变后端 diff、审批、发布、dispatch 或自动应用语义。 Tech: `JAVA-BE`,`SQL`,`VUE-FE`,`OPS`. Layer: `sql-optimization application(controller/service)/domain/infrastructure`,`mybatis mapper xml`,`frontend api client`,`frontend views/components/styles`,`deployments/ci/scripts`.
- Plan ref: docs/exec-plans/completed/USER-CN-RECOMMENDATION-DIFF-LAYOUT-20260517-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-015` 加速与改写治理工作台闭环
- Human confirmation point: 若实现需要改变数据库 schema、推荐状态机、diff 权威语义、审批/发布/dispatch/自动应用行为，或让前端排序字段直接进入 SQL 拼接，必须暂停并人工确认。
- Data impact: 只新增后端查询面和前端展示/契约/测试；不新增表、不迁移数据、不改变 SQL 执行或推荐生命周期数据语义。
- Rollback / recovery: 回退本任务单 commit 可恢复 legacy 推荐页布局和新增分页接口接线；保留旧 recommendations 数组接口不变以降低回滚风险。
- Validation:
  - `mvn -pl sql-optimization,sqlforge-shared -am test、npm run lint、npm run build、npm run test:sql-ui-contract、npm run test:frontend-page-governance、node scripts/check-recommendation-page-contract.mjs、npm run smoke:frontend-dev、npm run smoke:production-rewrite-closed-loop、git diff --check`
  - `python3 scripts/foreman.py validate USER-CN-RECOMMENDATION-DIFF-LAYOUT-20260517`
- Progress log:
  - 2026-05-17: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增推荐结果分页/过滤/安全排序接口并保留 legacy 数组接口；推荐页改为顶部查询条件、远程分页表格和右侧详情抽屉；SQL 差异页签改用公共 SqlCompareBlock 展示行级 side-by-side compare，保留原始 diff/AST 证据入口且不改变审批、发布、dispatch 或自动应用语义。
  - Validation evidence: java -version 确认为 1.8.0_112；mvn -pl sql-optimization,sqlforge-shared -am test 通过；npm run lint 通过；npm run build 通过；npm run test:sql-ui-contract 通过；npm run test:frontend-page-governance 通过；node scripts/check-recommendation-page-contract.mjs 通过；npm run smoke:frontend-dev 通过；npm run smoke:production-rewrite-closed-loop 通过；git diff --check 通过；foreman validate --include-task-audit 与 task_audit pre-closeout 通过。
  - Residual risk: SQL compare 前端行级差异仅作为 display evidence；后端 diff、验证结果、审批发布和 dispatch 仍为权威语义。真实外部 Hetu/MRS 环境验证继续由既有 HARN-016 / INBOX-002 跟踪。
  - Next step: 无本任务内后续步骤；如需真实环境端到端证据，按 HARN-016 外部验证窗口执行。

### HARN-FE-008: 参考页治理与最终验证

- Status: done
- Completed at: 2026-05-16
- Commit subject: `feat(frontend): close reference page governance HARN-FE-008`
- Priority: 1
- Depends on: HARN-FE-002,HARN-FE-003,HARN-FE-004,HARN-FE-005,HARN-FE-006,HARN-FE-007
- Scope: 把加速治理工作台按 AI 交付类似逻辑收口为参考模拟页；保留页面代码、路由和现有接口调用，不作为正式功能页面出现在核心菜单；完成前端主路径、专项 contract、smoke、截图自检与任务审计 closeout 验收。
- Validation:
  - `python3 scripts/foreman.py validate HARN-FE-008`
- Progress log:
  - 2026-05-16: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-16: updated zh/en page title and boundary copy so the workbench renders as a flow simulation reference page while retaining direct route and reference navigation placement.
  - 2026-05-16: added pageTitle and summary regression guards to frontend i18n copy and acceleration workbench contract checks.
  - 2026-05-16: R-186 before/after 截图自检 completed with before screenshots at `target/harn-fe-008-screenshots/before-desktop.png` and `target/harn-fe-008-screenshots/before-narrow.png`, after screenshots at `target/harn-fe-008-screenshots/after-desktop.png` and `target/harn-fe-008-screenshots/after-narrow.png`; Codex 读图复核 confirmed the intended title/summary change, existing desktop/narrow stacking, no page-level overflow, and no visual drift requiring修复.
- Context closeout:
  - Completed scope: Updated the acceleration governance workbench zh/en pageTitle and boundary copy to explicitly identify the flow simulation reference page; kept the direct route, existing API calls, smoke entry, and reference-only navigation behavior; added i18n and acceleration workbench contract regression guards.
  - Validation evidence: Validation passed via foreman validate with lint, build, form governance, SQL UI, frontend page governance, navigation/dashboard/recommendation/workbench contracts, frontend dev smoke, acceleration workbench smoke, git diff --check, and task_audit pre-closeout. R-186 before screenshot: target/harn-fe-008-screenshots/before-desktop.png and target/harn-fe-008-screenshots/before-narrow.png; after screenshot: target/harn-fe-008-screenshots/after-desktop.png and target/harn-fe-008-screenshots/after-narrow.png; Codex visual self-review confirmed the intended title/summary change, retained desktop/narrow layout, no page-level overflow, and no visual drift requiring修复.
  - Residual risk: No residual risk for the scoped frontend reference-page governance; backend/product operator semantics remain intentionally unchanged and the direct reference route stays available for smoke.
  - Next step: No HARN-FE follow-up required unless humans decide to promote or remove the reference page.

### HARN-FE-007: 改写记录与改写历史入口收口

- Status: done
- Completed at: 2026-05-16
- Commit subject: `feat(frontend): close rewrite governance entries HARN-FE-007`
- Priority: 1
- Depends on: HARN-FE-006
- Scope: 在正式导航和推荐详情中呈现改写记录、改写历史，复用现有 rewrite records、query-history rewrite records 和 validation runs 能力；复用推荐结果与 SQL 历史查询 route，通过 query/tab 深链进入对应 tab；不新增独立 view、独立 route 或后端接口，不改变审批/发布状态机或运行时绑定语义。
- Validation:
  - `python3 scripts/foreman.py validate HARN-FE-007`
- Progress log:
  - 2026-05-16: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 正式导航新增改写治理分组，改写记录深链复用推荐结果页 rewriteLifecycle tab，改写历史深链复用 SQL 历史查询并携带 hasRewriteRecord/detailTab；推荐页同步 route tab、recommendationId、rewriteRecordId 与租户并展示改写验证运行只读表；SQL 历史页支持改写筛选 query 和 historyId+detailTab=rewriteRecords 自动打开详情改写记录 tab；Dashboard 改写入口、下一步和活动流改为 query 化目标；同步静态契约与 dev browser smoke 覆盖。
  - Validation evidence: python3 scripts/foreman.py validate HARN-FE-007 --include-task-audit --extra-command node scripts/check-navigation-shell-contract.mjs --extra-command node scripts/check-recommendation-page-contract.mjs --extra-command node scripts/check-history-page-contract.mjs --extra-command node scripts/check-history-detail-contract.mjs --extra-command npm run smoke:frontend-dev --extra-command git diff --check 通过；直接运行 npm run lint、npm run build、npm run test:frontend-page-governance、npm run test:sql-ui-contract、npm run smoke:frontend-dev 与 git diff --check 通过。
  - Residual risk: 本任务仅调整前端正式入口、query 深链和只读展示，复用既有 rewrite records、query-history rewrite records 与 validation-runs API；未新增独立 route/view/backend API，未改变审批、发布、运行时绑定或自动应用语义；真实外部 Hetu/MRS 环境验证仍按 HARN-016/INBOX-002 跟踪。
  - Next step: 进入 HARN-FE-008，收口参考页治理与最终验证。

### HARN-FE-006: 推荐结果页面聚焦 SQL diff 与收益风险

- Status: done
- Completed at: 2026-05-16
- Commit subject: `feat(frontend): refocus recommendation results HARN-FE-006`
- Priority: 1
- Depends on: N/A
- Scope: 将 RecommendationCenterView.vue 的主视觉调整为正式推荐结果页面，突出推荐列表、SQL diff、收益风险、规则链和推荐来源；保留既有审批、发布、dispatch、trace 和自动应用语义不变，审批/发布能力仅降权为辅助证据区；同步前端展示契约检查与必要 i18n 文案，不新增后端 API、不改数据库、不改变 recommendation diff 语义。
- Validation:
  - `python3 scripts/foreman.py validate HARN-FE-006`
- Progress log:
  - 2026-05-16: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 推荐结果页完成 HARN-FE-006 聚焦收口：详情顶部新增推荐来源、收益、风险、diff 状态和规则差异焦点摘要；新推荐默认进入 SQL 差异页签，diff 不可用时回到摘要；tab 顺序调整为摘要、SQL 差异、规则与风险、SQL 证据、改写复核与发布、Dispatch 契约、追溯链、Dispatch 事件；规则链、前置条件、语义风险和未应用规则从仅数量/原始抽屉提升为可读摘要；保留审批、发布、dispatch、trace 和自动应用语义不变，并强化推荐页契约防回退。
  - Validation evidence: python3 scripts/foreman.py validate HARN-FE-006 --include-task-audit --extra-command npm-run-lint --extra-command npm-run-build --extra-command check-recommendation-page-contract --extra-command frontend-page-governance --extra-command sql-ui-contract --extra-command git-diff-check 通过；node scripts/check-recommendation-page-contract.mjs 通过；npm run lint 通过；npm run build 通过；npm run test:frontend-page-governance 通过；npm run test:sql-ui-contract 通过；git diff --check 通过；python3 scripts/task_audit.py --check --phase pre-closeout 通过；R-186 截图自检通过，证据为 .codex-log/harn-fe-006/after-desktop.png 与 after-narrow.png。
  - Residual risk: 本任务只调整前端展示层与静态契约，不改变后端 recommendation diff 语义、推荐状态机、审批、发布或自动应用行为；截图和 browser evidence 使用 repo-closed mock 数据，真实 Hetu/MRS 外部环境验证仍按 HARN-016 / INBOX-002 跟踪。
  - Next step: 进入 HARN-FE-007，收口改写记录与改写历史入口。

### HARN-FE-005: SQL 历史与解析历史强化关联链路

- Status: done
- Completed at: 2026-05-16
- Commit subject: `feat: strengthen SQL history parse links HARN-FE-005`
- Priority: 1
- Depends on: N/A
- Scope: 强化 SQL 历史查询与解析历史的页面定位和深链串联，复用现有 SQL 历史、解析历史、推荐引用与改写记录接口；不新增后端字段，不改变历史查询参数、导出字段、脱敏策略或分页语义；落实 SQL 历史到解析历史、推荐结果、改写记录的可操作入口，解析历史默认打开 SQL 解析记录并兼容 parseHistoryId/historyId 深链，保持原始 SQL、模板 SQL、绑定后 SQL 不被前端自动改写。
- Validation:
  - `python3 scripts/foreman.py validate HARN-FE-005`
- Progress log:
  - 2026-05-16: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 强化 SQL 历史详情到解析历史、推荐结果与改写记录的可操作入口；解析历史详情补齐返回 SQL 历史与推荐结果入口，并兼容 historyId/parseHistoryId 深链；保持原始 SQL、模板 SQL、绑定后 SQL 禁止前端自动格式化；补齐页面契约与 dev browser smoke mock。
  - Validation evidence: python3 scripts/foreman.py validate HARN-FE-005 --include-task-audit；npm run smoke:frontend-dev；docs/quality/validation-log.md
  - Residual risk: 未新增后端字段、历史查询参数、导出字段、脱敏策略或分页语义；真实环境截图和外部后端数据仍按 environment-backed 验证链路沉淀。
  - Next step: 进入 HARN-FE-006 推荐结果页面聚焦 SQL diff 与收益风险。

### USER-CN-CODE-RULES-FIX456-20260516: 修复中文化门禁后续缺口

- Status: done
- Completed at: 2026-05-16
- Commit subject: `chore: tighten Chinese copy governance gates USER-CN-CODE-RULES-FIX456-20260516`
- Priority: 1
- Depends on: USER-CN-CODE-RULES-FIX-20260516
- Scope: 修复中文化规则落地复核发现的 4、5、6 项后续问题：资源/mapper 文件触发后必须真实扫描，脚本英文历史存量基线必须按具体问题锁定而不是只按计数放行，changed 模式与知识库自检必须覆盖这些门禁能力。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-CODE-RULES-FIX456-20260516`
- Progress log:
  - 2026-05-16: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 修复中文化门禁后续缺口：src/main/resources mapper/resources 文件进入真实扫描；脚本英文历史存量 baseline 升级为计数加具体文案锁定；知识库自检和规则文档同步这些门禁能力。
  - Validation evidence: python3 scripts/foreman.py validate USER-CN-CODE-RULES-FIX456-20260516 通过；node scripts/check-developer-copy-language.mjs --changed/--all 通过；node scripts/lint-repository-knowledge.js 通过；npm run lint 通过；git diff --check 通过。
  - Residual risk: 无已知残余风险；脚本英文 help/error/print/echo 历史存量仍存在但已按具体文案基线锁定，只允许减少或中文化。
  - Next step: 后续触达 Java resources、mapper 或脚本输出时继续按 R-187 至 R-190 降低英文存量。

### USER-CN-CODE-RULES-FIX-20260516: 修复中文化门禁前三项缺口

- Status: done
- Completed at: 2026-05-16
- Commit subject: `chore: harden Chinese developer copy gates USER-CN-CODE-RULES-FIX-20260516`
- Priority: 1
- Depends on: USER-CN-CODE-RULES-20260516
- Scope: 修复中文化规则落地复核发现的前三项问题：脚本 help/error/print/echo 检查必须真实覆盖并显式管理历史存量；规则/文档/检查器变更必须自动触发 --all；Java JUnit 断言失败消息必须纳入中文化检查。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-CODE-RULES-FIX-20260516`
- Progress log:
  - 2026-05-16: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 修复中文化门禁前三项缺口：脚本 help/error/print/echo 检测、治理变更触发 --all、Java 断言失败消息扫描与当前基线整改。
  - Validation evidence: foreman validate USER-CN-CODE-RULES-FIX-20260516 通过；node scripts/check-developer-copy-language.mjs --changed/--all 通过；node scripts/lint-repository-knowledge.js 通过；JDK 8u112 定向 Maven 测试通过。
  - Residual risk: 无已知残余风险；脚本英文输出历史存量已由 developer-copy-language-script-legacy-baseline.json 登记，后续触达继续降低。
  - Next step: 继续按 R-187 至 R-190 阻断新增纯英文开发者可读文本。

### USER-CN-CODE-RULES-20260516: 中文化编码规范治理落地

- Status: done
- Completed at: 2026-05-16
- Commit subject: `chore: add Chinese developer copy governance USER-CN-CODE-RULES-20260516`
- Priority: 1
- Depends on: N/A
- Scope: 追加 R-187 至 R-190 中文化编码规范与验证规则，补充 task matrix 治理叠加和人类约束历史，新增开发者可读文本中文化检查脚本，接入 foreman validate 与知识 lint，并修复当前 Java/SQL/仓库脚本基线缺口。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-CODE-RULES-20260516`
- Progress log:
  - 2026-05-16: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 追加 R-187 至 R-190 中文化规则与验证规则，补充人类约束历史和 Backend/SQL/OPS 默认治理叠加；新增 check-developer-copy-language.mjs，接入 foreman validate 与 knowledge lint；修复 benchmark Java 测试诊断文案和 dev schema helper SQL COMMENT 中文化基线缺口。
  - Validation evidence: 已执行 node scripts/check-developer-copy-language.mjs --all、node scripts/check-developer-copy-language.mjs --changed、node scripts/check-frontend-i18n-copy.mjs、node scripts/lint-repository-knowledge.js、git diff --check、python3 -m py_compile scripts/foreman.py scripts/ensure_execution_result_dev_schema.py scripts/ensure_query_history_dev_schema.py、java -version=1.8.0_112、mvn -pl benchmark-engine -Dtest=BenchmarkTaskModelApplicationServiceTest test、python3 scripts/foreman.py validate USER-CN-CODE-RULES-20260516 --include-task-audit --extra-command 'node scripts/check-developer-copy-language.mjs --all' --extra-command 'git diff --check'。
  - Residual risk: 历史脚本 CLI 诊断输出仍存在英文存量；本轮按计划先阻断 Java 可读字符串、SQL/脚本 DDL COMMENT、脚本注释和检查器自身输出，后续如需可拆专项全量中文化脚本 help/error 文案。
  - Next step: 后续 Java/SQL/OPS 任务通过 foreman validate 自动执行中文化 changed 门禁；若扩大脚本 help/error 扫描范围，应先清理历史存量或建立更细 allowlist。

### HARN-FE-004: 首页总览聚焦核心链路

- Status: done
- Completed at: 2026-05-16
- Commit subject: `feat(frontend): refocus dashboard core workflow`
- Priority: 1
- Depends on: N/A
- Scope: 改造 Dashboard 的入口排序和首屏信息权重，只突出核心 SQL 工作流摘要和入口；继续使用现有 repo-side 证据、样本或静态摘要，不新增后端聚合接口，不改数据库，不改变推荐、改写、审批、发布或自动应用业务语义；同步 Dashboard contract 与必要 i18n 文案，保留 sample/window/session/PULL_ONLY 事实边界。
- Validation:
  - `python3 scripts/foreman.py validate HARN-FE-004`
- Progress log:
  - 2026-05-16: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Refocused Dashboard first-screen hierarchy around the core SQL workflow: query analysis, SQL history, single-SQL parse, parse history, recommendation results, and rewrite-record/history summaries; consumed existing query-history, parse-statistics, parse-history, recommendation, rewrite-record, governance-message, and dispatch-contract evidence only; downgraded auxiliary governance signals to boundary evidence and updated Dashboard i18n plus contract guards without backend/API/database semantic changes.
  - Validation evidence: python3 scripts/foreman.py validate HARN-FE-004 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'npm run test:frontend-page-governance' --extra-command 'node scripts/check-dashboard-contract.mjs' --extra-command 'git diff --check' passed; after screenshots captured at .codex-log/harn-fe-004/after-desktop.png and after-narrow.png with no overlap or text overflow observed.
  - Residual risk: Dashboard still uses current API/window samples and PULL_ONLY boundary facts only; no global tenant KPI or dedicated rewrite-history route was introduced.
  - Next step: Proceed to HARN-FE-005 to strengthen SQL history and parse history association paths.

### HARN-FE-003: 统一 i18n 文案与页面标题

- Status: done
- Completed at: 2026-05-16
- Commit subject: `feat(frontend): align core workflow copy`
- Priority: 1
- Depends on: HARN-FE-002
- Scope: 更新 src/locales/zh-CN.js 与 src/locales/en-US.js，统一核心菜单、页面标题、摘要、breadcrumb 和参考页说明；只改 UI 文案与最小防回退检查，不改接口字段、状态枚举、后端契约名称、数据 payload、路由、API 或数据库。
- Validation:
  - `python3 scripts/foreman.py validate HARN-FE-003`
- Progress log:
  - 2026-05-16: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Aligned frontend i18n copy for HARN-FE-003 across zh-CN and en-US core route titles, page summaries, breadcrumbs, recommendation-result wording, parse-history page heading copy, and acceleration governance workbench reference-page boundary text; added a focused i18n copy regression guard without changing routes, APIs, payload fields, backend contracts, or database semantics.
  - Validation evidence: python3 scripts/foreman.py validate HARN-FE-003 --include-task-audit --extra-command 'node scripts/check-navigation-shell-contract.mjs' --extra-command 'git diff --check' passed after compile-governance was refreshed; direct npm run lint, npm run build, npm run test:i18n-copy, npm run test:sql-ui-contract, npm run test:frontend-page-governance, node scripts/check-navigation-shell-contract.mjs, and git diff --check passed.
  - Residual risk: Dashboard information hierarchy, SQL/parse history workflow reshaping, recommendation page visual refocus, rewrite-record navigation, and final reference-page gate cleanup remain scoped to HARN-FE-004 through HARN-FE-008; this task intentionally changed UI copy and regression checks only.
  - Next step: Proceed to HARN-FE-004 to refocus the Dashboard first-screen workflow without changing backend/API/database semantics.

### USER-I18N-ZH-20260516: 中文化后端日志异常注释与脚本文案

- Status: done
- Completed at: 2026-05-16
- Commit subject: `chore(i18n): localize backend Chinese copy`
- Priority: 1
- Depends on: N/A
- Scope: 扫描并中文化后端 Java 日志、异常消息、API 返回 message/msg、代码注释、JPA/Swagger 字段解释、SQL/Shell 脚本注释与 COMMENT 文案；只改文本，不改命名、错误码、序列化键或业务逻辑；完成后运行后端编译/测试与任务审计。
- Validation:
  - `python3 scripts/foreman.py validate USER-I18N-ZH-20260516`
- Progress log:
  - 2026-05-16: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 完成后端 Java 日志、异常/API 文案、代码注释、SQL COMMENT、脚本/资源注释中文化，并同步相关测试断言与任务台账。
  - Validation evidence: JDK 8u112 下通过 mvn compile、mvn test；本地化扫描 java comments/sql COMMENT/script-resource comments/log templates 均为 0；foreman validate 与 pre-closeout task_audit 已通过。
  - Residual risk: 英文仅保留为类名、字段名、枚举值、协议/路径、SQL 关键字、服务代码、指标名和测试哨兵等不可翻译标识。
  - Next step: 无需后续动作。

### HARN-FE-002: 重排导航树与核心菜单命名

- Status: done
- Completed at: 2026-05-16
- Commit subject: `feat(frontend): refocus core navigation menu`
- Priority: 1
- Depends on: HARN-FE-001A
- Scope: 调整前端 NAVIGATION_TREE 与导航契约脚本，使核心菜单优先展示并将加速治理工作台移出正式核心菜单；保留既有 ROUTE_PATHS、route name、legacy redirect、页面组件和后端/API/数据库语义。
- Validation:
  - `python3 scripts/foreman.py validate HARN-FE-002`
- Progress log:
  - 2026-05-16: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reordered the frontend navigation tree around the HARN-FE core SQL workflow, moved acceleration governance workbench into a non-default reference-pages group, kept existing route paths/names/legacy redirects/page components intact, updated navigation i18n labels, runtime reference-page visibility, and navigation/workbench contract scripts.
  - Validation evidence: python3 scripts/foreman.py validate HARN-FE-002 --include-task-audit --extra-command "node scripts/check-navigation-shell-contract.mjs" --extra-command "node scripts/check-acceleration-workbench-contract.mjs" --extra-command "git diff --check" passed; npm run lint, npm run build, npm run test:frontend-page-governance, node scripts/check-navigation-shell-contract.mjs, node scripts/check-acceleration-workbench-contract.mjs, git diff --check, and python3 scripts/task_audit.py --check --phase pre-closeout passed; R-186 before screenshots .codex-log/harn-fe-002/before-desktop.png and before-narrow.png, after screenshots .codex-log/harn-fe-002/after-desktop.png and after-narrow.png; Codex visual self-review passed with no sidebar overlap, text overflow, or visual drift found.
  - Residual risk: Reference pages remain available only outside production through runtime flags; broader page-title, breadcrumb, dashboard, history, recommendation, rewrite-record, and final reference-page copy cleanup remains in HARN-FE-003 through HARN-FE-008.
  - Next step: Proceed to HARN-FE-003 to align page titles, breadcrumbs, summaries, and reference-page explanatory copy without changing backend/API/database semantics.

### HARN-FE-001A: 对齐前端核心菜单文档真值

- Status: done
- Completed at: 2026-05-16
- Commit subject: `docs(frontend): align core workflow IA authority`
- Priority: 1
- Depends on: HARN-FE-001
- Scope: Docs-only alignment for HARN-FE-001: synchronize product/front-end plan truth for the core workflow refocus task pack before navigation implementation; update documentation authority and validation expectations only, without changing frontend code, backend APIs, database schema, route behavior, or contract scripts.
- Validation:
  - `python3 scripts/foreman.py validate HARN-FE-001A`
- Progress log:
  - 2026-05-16: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-16: aligned docs/product/sql-governance-platform-implementation-spec.md, docs/product/frontend-retrospective-gap-closure-baseline.md, docs/plans/frontend-core-workflow-refocus-task-pack.md and docs/README.md so HARN-FE menu refocus is the frontend display IA authority before navigation implementation.
- Context closeout:
  - Completed scope: Aligned HARN-FE-001 frontend display IA authority across product spec, frontend retrospective baseline, task pack validation requirements, docs entry index, and compiled governance authority map; no frontend runtime code, backend API, database schema, route behavior, or contract script behavior changed.
  - Validation evidence: python3 scripts/foreman.py validate HARN-FE-001A --include-task-audit --extra-command 'node scripts/lint-repository-knowledge.js' --extra-command 'python3 scripts/foreman.py compile-governance --check' --extra-command 'git diff --check' passed after compiling governance policy; direct git diff --check passed.
  - Residual risk: HARN-FE-002 still needs to implement the actual navigation tree and update navigation/workbench contract scripts in the same task; this docs-only task intentionally did not change frontend code.
  - Next step: Materialize HARN-FE-002 to update NAVIGATION_TREE, i18n labels, reference-page placement, and matching contract scripts under the aligned IA authority.

### HARN-FE-001: 固化前端核心菜单与功能边界任务包

- Status: done
- Completed at: 2026-05-16
- Commit subject: `docs(frontend): add core workflow task pack`
- Priority: 1
- Depends on: N/A
- Scope: 新增前端核心链路聚焦改造任务包文档，更新计划索引与文档覆盖矩阵；不改前端代码、后端接口或数据库。
- Validation:
  - `python3 scripts/foreman.py validate HARN-FE-001`
- Progress log:
  - 2026-05-16: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增前端核心链路聚焦改造任务包文档，固化最终菜单清单、参考页边界和 HARN-FE-001 至 HARN-FE-008 候选任务；更新计划索引与文档覆盖矩阵，不改前端代码、后端接口或数据库。
  - Validation evidence: python3 scripts/foreman.py validate HARN-FE-001 --include-task-audit --extra-command 'node scripts/lint-repository-knowledge.js' --extra-command 'python3 scripts/foreman.py compile-governance --check' --extra-command 'git diff --check'; node scripts/lint-repository-knowledge.js; git diff --check.
  - Residual risk: 后续 HARN-FE-* 仍需逐个 materialize 后再实施；本任务只落账候选任务包，不调整实际前端导航或页面。
  - Next step: Materialize HARN-FE-002 to implement navigation tree and core menu naming changes.

### HARN-146: 增强推荐中心 SQL compare 差异视图

- Status: done
- Completed at: 2026-05-16
- Commit subject: `feat(frontend): enhance recommendation SQL compare`
- Priority: 1
- Depends on: HARN-139
- Scope: 在推荐中心 SQL 差异页签中增强改写前后 SQL compare 视觉呈现，使用后端 recommendation diff 作为权威证据，前端只负责左右对比、高亮新增/删除/替换和长 SQL 可读性；不改变后端 diff 语义、推荐状态、审批、发布或自动应用行为。
- Validation:
  - `python3 scripts/foreman.py validate HARN-146`
- Progress log:
  - 2026-05-16: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added a recommendation-center SQL compare view that renders backend textDiff hunks side-by-side with original/recommended positions, INSERT/DELETE/REPLACE highlighting, responsive layout, and raw evidence access; updated i18n, recommendation contract checks, production rewrite browser smoke assertions, and document coverage lint metadata without changing backend diff semantics, approvals, publishing, or auto-apply behavior.
  - Validation evidence: python3 scripts/foreman.py validate HARN-146 --include-task-audit with recommendation page contract, production rewrite closed-loop browser smoke, and git diff --check extras passed; focused npm run lint, npm run build, npm run test:sql-ui-contract, npm run test:frontend-page-governance, node scripts/lint-repository-knowledge.js, and python3 scripts/task_audit.py --check --phase pre-closeout passed.
  - Residual risk: Browser smoke uses repo-closed mocked recommendation/diff payloads rather than a live Hetu/MRS environment; backend semantic equivalence remains governed by existing diff/validation services and external live evidence remains under HARN-016 / INBOX-002.
  - Next step: Inspect real long-form recommendation diff payloads in the live environment when available, keeping the compare view as display-only evidence.

### OPS-CODEX-TMP-IGNORE-20260516: Ignore Codex tmp runtime files

- Status: done
- Completed at: 2026-05-15
- Commit subject: `chore: ignore codex tmp files`
- Priority: 1
- Depends on: N/A
- Scope: Add .codex/tmp/** to .gitignore so local Codex npm/cache temp files do not appear as untracked changes.
- Validation:
  - `python3 scripts/foreman.py validate OPS-CODEX-TMP-IGNORE-20260516`
- Progress log:
  - 2026-05-15: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added .codex/tmp/** to the repository ignore rules so local Codex npm/cache runtime files no longer appear as untracked changes.
  - Validation evidence: python3 scripts/foreman.py validate OPS-CODEX-TMP-IGNORE-20260516 --include-task-audit attempted; py_compile and pre-closeout task audit passed; node scripts/lint-repository-knowledge.js failed on pre-existing document coverage matrix entries for docs/SQLForge.md, docs/SQLForgeV1.0.md, and docs/SQLTest.md.
  - Residual risk: Repository knowledge lint remains blocked by unrelated document-coverage-matrix drift; the .codex/tmp ignore rule itself was verified by git ls-files --others --exclude-standard -- .codex/tmp returning 0.
  - Next step: Track the document coverage matrix drift separately before treating repository-wide knowledge lint as green.

### USER-REPORT-IMPORT-SAMPLE-20260516: Generate report import XLSX stress sample

- Status: done
- Completed at: 2026-05-15
- Commit subject: `test(sql-optimization): close report import xlsx stress sample`
- Priority: 1
- Depends on: N/A
- Scope: Generate docs/report-import-parse-stress-sample.xlsx and companion documentation for report import parsing tests. The XLSX must follow the report_code + sql_N wide-table import template, include 600 reports, use varied 3-120-bounded SQL cells per report, keep all SQL as valid SELECT/WITH queries with comments, and cover high/medium/low complexity plus BI/low-code poor-query performance scenarios.
- Validation:
  - `python3 scripts/foreman.py validate USER-REPORT-IMPORT-SAMPLE-20260516`
- Progress log:
  - 2026-05-15: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-15: generated `docs/report-import-parse-stress-sample.xlsx` and companion documentation with 600 reports, `sql_1` through `sql_120` template columns, 2749 valid SELECT/WITH SQL cells, 35 BI/low-code poor-query scenarios, and 80% medium/high complexity coverage.
  - 2026-05-15: validated XLSX structure and ran backend MockMvc import plus representative resolve-sqls slice; full 3..120 wide-row variant was intentionally reduced after it exposed the current backend import `rawLine` heap boundary.
- Context closeout:
  - Completed scope: Generated docs/report-import-parse-stress-sample.xlsx and docs/report-import-parse-stress-sample.md for report import parsing tests: 600 reports, report_code plus sql_1..sql_120 template columns, 2749 valid SELECT/WITH SQL cells, comments, varied per-report SQL counts, 35 BI/low-code poor-query scenarios, and high/medium/low complexity coverage.
  - Validation evidence: python3 scripts/foreman.py validate USER-REPORT-IMPORT-SAMPLE-20260516; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/task_audit.py --check --phase post-closeout; git diff --check; temporary MockMvc import smoke and representative resolve-sqls slice smoke.
  - Residual risk: A full 3..120 non-empty long-SQL variant exposed the current backend import rawLine heap boundary; the delivered workbook keeps sql_1..sql_120 columns but bounds non-empty SQL cells to 3..30 per report for usable import testing.
  - Next step: Use docs/report-import-parse-stress-sample.xlsx for manual report import testing, and fix the backend rawLine memory behavior before using a 120-wide long-SQL stress workbook.

### OPS-START-BACKEND-20260515: Start all backend services

- Status: done
- Completed at: 2026-05-15
- Commit subject: `OPS-START-BACKEND-20260515 start all backend services`
- Priority: 1
- Depends on: N/A
- Scope: Start SQLForge local backend runtime services for this workspace session, verify health endpoints, and avoid source or long-term configuration changes.
- Validation:
  - `python3 scripts/foreman.py validate OPS-START-BACKEND-20260515`
- Progress log:
  - 2026-05-15: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Started the SQLForge local dependency stack and all four backend services; repaired query-execution dev startup by adding its local datasource defaults and explicit Redis sync adapter constructor injection.
  - Validation evidence: python3 scripts/foreman.py validate OPS-START-BACKEND-20260515 --include-task-audit with query-execution targeted tests and backend health check; bash scripts/health-check.sh --fail-on-error --skip-frontend.
  - Residual risk: Local MySQL uses replacement volume sqlforge_mysql-runtime-20260515 because the pre-existing sqlforge_mysql-data volume was left intact but unusable after partial initialization.
  - Next step: Use /tmp/sqlforge-backend-runtime/*.pid to stop backend processes when this local runtime is no longer needed.

### PRW-001: 固化生产改写闭环接口与状态契约

- Status: done
- Completed at: 2026-05-12
- Commit subject: `PRW-001 production rewrite contract baseline`
- Priority: 1
- Depends on: `HARN-142`,`HARN-145`
- Scope: 文档必须明确 rewrite review / publish / runtime binding 状态机，以及审批入口不属于 acceleration plan 审批页。只覆盖生产自动改写闭环；不得把投产前核验闭环混入本任务。 Tech: `DOCS`,`OPS`. Layer: `docs`,`architecture`,`product`.
- Plan ref: docs/exec-plans/completed/PRW-001-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-015` 加速与改写治理工作台闭环
- Human confirmation point: 若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。
- Data impact: 仅影响文档、主计划、接口契约说明和任务治理记录；不修改运行时数据、数据库 schema 或接口实现。
- Rollback / recovery: 回退本任务文档增量并保留后续任务不执行；若状态命名不合适，用追加文档修正替代覆盖历史。
- Validation:
  - `node scripts/lint-repository-knowledge.js、python3 scripts/task_audit.py --check --phase pre-closeout、git diff --check`
  - `python3 scripts/foreman.py validate PRW-001`
- Progress log:
  - 2026-05-11: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Archived the production SQL rewrite auto-apply contract baseline after confirming review, publish, and runtime binding state boundaries were documented without business-code changes.
  - Validation evidence: python3 scripts/foreman.py validate PRW-001; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check.
  - Residual risk: PRW-002 through PRW-013 already implement downstream code paths; PRW-001 remains a documentation contract baseline and live environment proof stays with downstream environment-backed follow-ups.
  - Next step: Use the completed PRW-001 contract as the baseline when auditing or extending PRW-002 through PRW-013 production rewrite behavior.

### PRW-013: JDBC Agent Redis 改写规则桥接

- Status: done
- Completed at: 2026-05-12
- Commit subject: `PRW-013 bridge JDBC Agent Redis rewrite rules`
- Priority: 1
- Depends on: `PRW-012`
- Scope: JDBC Agent/Redis 只能作为已发布运行时绑定的兼容出口，不能替代 query-execution 主闭环真值。 Tech: `JAVA-BE`,`REDIS`,`OPS`,`DOCS`. Layer: `query-execution`,`jdbc-agent`,`infrastructure`,`tests`,`docs`.
- Plan ref: docs/exec-plans/completed/PRW-013-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-015` 加速与改写治理工作台闭环
- Human confirmation point: 若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。
- Data impact: 同步 Redis 改写规则 key；主闭环状态仍以 query-execution 绑定为准，同步失败必须告警而非篡改主状态。
- Rollback / recovery: 停用 Redis 同步适配并删除或标记无效的 Redis key，保持 query-execution 主绑定不变。
- Validation:
  - `mvn -pl query-execution,sqlforge-shared -am test、python3 scripts/foreman.py validate PRW-013`
  - `python3 scripts/foreman.py validate PRW-013`
- Progress log:
  - 2026-05-11: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented tenant-scoped JDBC Agent Redis rewrite rule sync from query-execution runtime bindings, Agent metadata/status validation, Redis failure evidence, and PRW-013 docs/tests.
  - Validation evidence: mvn -pl query-execution,sqlforge-shared -am test; python3 scripts/foreman.py validate PRW-013; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check.
  - Residual risk: Live Redis environment-backed verification remains outside repo-closed tests and should be run in the target test environment when credentials are available.
  - Next step: Enable QUERY_EXECUTION_JDBC_AGENT_REDIS_* in an environment-backed Redis test and verify JDBC Agent LOCAL_REWRITE_DIRECT_JDBC reads tenant-scoped keys.

### PRW-012: 生产闭环端到端测试与 smoke

- Status: done
- Completed at: 2026-05-12
- Commit subject: `PRW-012 production rewrite closed-loop validation`
- Priority: 1
- Depends on: `PRW-009`,`PRW-010`,`PRW-011`
- Scope: 测试必须证明生产路径真的闭合，且不依赖投产前本地/测试环境核验闭环线。 Tech: `JAVA-BE`,`VUE-FE`,`OPS`,`DOCS`. Layer: `tests`,`deployments/ci/scripts`,`frontend`,`backend`,`docs`.
- Plan ref: docs/exec-plans/completed/PRW-012-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-015` 加速与改写治理工作台闭环
- Human confirmation point: 若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。
- Data impact: 新增测试、smoke、runbook 或契约检查；不改变业务生产数据。
- Rollback / recovery: 回退新增测试和 smoke 接线，不影响 PRW-001 至 PRW-011 的已实现业务能力。
- Validation:
  - `mvn test、npm run lint、npm run build、npm run smoke:acceleration-governance、python3 scripts/foreman.py validate PRW-012`
- Progress log:
  - 2026-05-11: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added backend production rewrite closed-loop E2E, frontend browser smoke, smoke bundle contract checks, and runbook/doc wiring for approval, validation, publish, runtime hit, history evidence, and divergence pause.
  - Validation evidence: mvn test with repository-local TMPDIR and JAVA_TOOL_OPTIONS because /tmp is full; npm run lint; npm run build; npm run smoke:acceleration-governance; python3 scripts/foreman.py validate PRW-012; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check.
  - Residual risk: Local root /tmp is full, so Java and browser validation need repository-local temp fallback in this environment; browser smoke scripts now include that fallback.
  - Next step: None for PRW-012.

### PRW-011: SQL 历史页面展示改写前后链路

- Status: done
- Completed at: 2026-05-12
- Commit subject: `feat(frontend): complete PRW-011 rewrite history linkage`
- Priority: 1
- Depends on: `PRW-008`,`PRW-010`,`HARN-145`
- Scope: SQL 历史页面展示必须来自 PRW-008 的后端审计字段，不得由前端推断改写是否发生。 Tech: `VUE-FE`,`DOCS`. Layer: `frontend/router/views/styles`,`api client`,`tests`.
- Plan ref: docs/exec-plans/completed/PRW-011-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-015` 加速与改写治理工作台闭环
- Human confirmation point: 若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。
- Data impact: 仅改变历史页面展示和 API 消费，不改写历史持久化数据。
- Rollback / recovery: 回退历史页面新增列、详情 tab 和跳转入口，恢复既有 SQL 历史视图。
- Validation:
  - `npm run lint、npm run build、node scripts/check-history-page-contract.mjs、node scripts/check-history-detail-contract.mjs、python3 scripts/foreman.py validate PRW-011`
  - `python3 scripts/foreman.py validate PRW-011`
- Progress log:
  - 2026-05-11: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: SQL history now displays backend rewriteAudit runtime binding and rule-version evidence, deep-links rewrite records into recommendation detail, and enforces PRW-011 history contracts through foreman validation.
  - Validation evidence: npm run lint; npm run build; node scripts/check-history-page-contract.mjs; node scripts/check-history-detail-contract.mjs; python3 scripts/foreman.py validate PRW-011; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check.
  - Residual risk: Long SQL diff and narrow mobile layouts remain dependent on existing SqlCodeBlock and detail-grid responsive behavior; no additional data or approval risk identified.
  - Next step: Continue with PRW-012 production closed-loop end-to-end tests and smoke coverage.

### PRW-010: 推荐中心与改写记录详情页面接入审批动作

- Status: done
- Completed at: 2026-05-11
- Commit subject: `feat(frontend): wire PRW-010 rewrite lifecycle actions`
- Priority: 1
- Depends on: `PRW-003`,`PRW-004`,`PRW-006`,`HARN-145`
- Scope: 页面必须明确“审批通过但未发布”与“运行时已生效”的差异，并只调用后端真实接口。 Tech: `VUE-FE`,`DOCS`. Layer: `frontend/router/views/styles`,`api client`,`tests`.
- Plan ref: docs/exec-plans/completed/PRW-010-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-015` 加速与改写治理工作台闭环
- Human confirmation point: 若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。
- Data impact: 仅改变前端交互和 API 调用，不直接修改持久化数据；审批/发布状态由后端接口写入。
- Rollback / recovery: 回退页面动作区和 API client 变更，恢复推荐中心只读或既有工作流显示。
- Validation:
  - `npm run lint、npm run build、node scripts/check-recommendation-page-contract.mjs、python3 scripts/foreman.py validate PRW-010`
  - `python3 scripts/foreman.py validate PRW-010`
- Progress log:
  - 2026-05-11: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented PRW-010 frontend rewrite lifecycle integration: recommendation detail now loads linked rewrite records, shows review and publish state, publish eligibility and refusal reasons, differentiates approved-but-unpublished from runtime-active bindings, and calls only the existing rewrite record review/publish/pause/unpublish APIs.
  - Validation evidence: npm run lint; npm run build; node scripts/check-recommendation-page-contract.mjs; python3 scripts/foreman.py validate PRW-010; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check
  - Residual risk: Recommendation center actions depend on an existing rewrite record for the selected recommendation; creating rewrite records remains owned by the existing governance workbench flow.
  - Next step: Continue with PRW-011 to expose SQL history rewrite linkage against the PRW-008 audit fields and PRW-010 review entry.

### PRW-009: 比对差异触发自动暂停与告警闭环

- Status: done
- Completed at: 2026-05-11
- Commit subject: `fix(sql-optimization): complete PRW-009 divergence pause closure`
- Priority: 1
- Depends on: `PRW-006`,`PRW-008`,`HARN-136`
- Scope: 周期比对发现差异后必须阻断后续自动改写，不得只停留在告警展示。 Tech: `JAVA-BE`,`SQL`,`DOCS`. Layer: `sql-optimization`,`query-execution client`,`governance alert`,`scheduler`,`tests`.
- Plan ref: docs/exec-plans/completed/PRW-009-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-015` 加速与改写治理工作台闭环
- Human confirmation point: 若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。
- Data impact: 差异验证会暂停 active 绑定并改变发布状态；必须保留告警和验证运行证据。
- Rollback / recovery: 关闭自动暂停调度或回退暂停调用，恢复手动暂停路径；保留历史告警记录。
- Validation:
  - `mvn -pl sql-optimization,query-execution,governance,sqlforge-shared -am test、python3 scripts/foreman.py validate PRW-009`
  - `python3 scripts/foreman.py validate PRW-009`
- Progress log:
  - 2026-05-11: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented PRW-009 divergence handling so scheduled validation pauses published query-execution runtime rewrite bindings, preserves pause failure evidence, emits divergence alerts, writes governance audit traces, and revalidates due equivalent published bindings.
  - Validation evidence: mvn -pl sql-optimization,query-execution,governance,sqlforge-shared -am test; python3 scripts/foreman.py validate PRW-009; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check
  - Residual risk: Maven reports repeated /tmp/surefire-chase007 temporary directory warnings, but all requested reactor tests completed with BUILD SUCCESS under JDK 1.8.0_112.
  - Next step: PRW-012 can consume the PRW-009 paused-binding evidence in production-loop end-to-end smoke coverage.

### PRW-008: 补齐 SQL 执行历史的改写审计链

- Status: done
- Completed at: 2026-05-11
- Commit subject: `feat(governance): PRW-008 persist rewrite audit history`
- Priority: 1
- Depends on: `PRW-007`,`HARN-134`
- Scope: 历史接口必须以后端真实审计字段证明自动改写是否发生，前端不得自行推断 rewriteApplied。 Tech: `JAVA-BE`,`SQL`,`DOCS`. Layer: `governance`,`query-execution`,`persistence`,`application(controller/service)`,`tests`.
- Plan ref: docs/exec-plans/completed/PRW-008-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-015` 加速与改写治理工作台闭环
- Human confirmation point: 若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。
- Data impact: 新增或扩展执行历史追溯字段；旧历史记录必须有兼容默认值。
- Rollback / recovery: 保留旧历史展示路径，停止写入新增改写审计字段或将其置为默认未改写状态。
- Validation:
  - `mvn -pl governance,query-execution,sqlforge-shared -am test、python3 scripts/foreman.py validate PRW-008`
  - `python3 scripts/foreman.py validate PRW-008`
- Progress log:
  - 2026-05-11: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented backend-backed SQL execution history rewrite audit persistence from query-execution write request through governance query_history schema, mapper, projection, detail, and export surfaces, including legacy rewriteApplied defaults plus migration and dev-schema support.
  - Validation evidence: java -version = 1.8.0_112; mvn -pl governance,query-execution,sqlforge-shared -am clean test; mvn -pl governance,query-execution,sqlforge-shared -am test; python3 scripts/foreman.py validate PRW-008; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance --check; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check.
  - Residual risk: External database upgrade execution is covered by migration/schema checks but was not applied to a live shared environment in this local closeout.
  - Next step: Continue dependent SQL history frontend display work using the backend rewriteAudit fields instead of frontend inference.

### PRW-007: 在 query-execution 执行路径应用自动改写

- Status: done
- Completed at: 2026-05-11
- Commit subject: `PRW-007 apply runtime rewrite bindings in execution path`
- Priority: 1
- Depends on: `PRW-006`
- Scope: 只有 active 运行时绑定可触发自动改写；未命中、跨租户、指纹不匹配或暂停状态必须保持原 SQL 执行路径。 Tech: `JAVA-BE`,`SQL`,`DOCS`. Layer: `query-execution`,`application service`,`domain`,`infrastructure`,`tests`.
- Plan ref: docs/exec-plans/completed/PRW-007-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-015` 加速与改写治理工作台闭环
- Human confirmation point: 若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。
- Data impact: 会改变命中绑定后的实际执行 SQL；必须保留原始 SQL 与绑定追踪，不改变未命中路径。
- Rollback / recovery: 关闭绑定读取或将绑定置为 paused/unpublished，恢复原 SQL 执行路径。
- Validation:
  - `mvn -pl query-execution,governance,sqlforge-shared -am test、python3 scripts/foreman.py validate PRW-007`
  - `python3 scripts/foreman.py validate PRW-007`
- Progress log:
  - 2026-05-11: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Applied active query-execution runtime rewrite bindings during synchronous SQL execution; preserved original SQL, actual SQL, rewrite status, rewrite record ID, runtime binding ID, and rule version in response metadata, binding summary, and governance history writes; added PRW-007 regressions for active binding, missing binding, datasource evidence, unsafe recommended SQL, and resolver failure fallback.
  - Validation evidence: java -version = 1.8.0_112; mvn -pl query-execution,governance,sqlforge-shared -am test; python3 scripts/foreman.py validate PRW-007; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check.
  - Residual risk: Formal SQL history query/detail DTO expansion remains owned by PRW-008; runtime rewrite resolver or unsafe recommended SQL failures intentionally fall back to the original SQL per confirmed policy.
  - Next step: Continue with PRW-008 to extend SQL execution history audit query/detail surfaces for rewrite trace display.

### PRW-006: 实现改写记录发布、暂停和撤销接口

- Status: done
- Completed at: 2026-05-11
- Commit subject: `PRW-006 implement rewrite record publish lifecycle`
- Priority: 1
- Depends on: `PRW-004`,`PRW-005`
- Scope: 发布、暂停、撤销必须保持改写记录状态与 query-execution 运行时绑定状态一致；失败不得留下半成功状态。 Tech: `JAVA-BE`,`SQL`,`DOCS`. Layer: `sql-optimization`,`query-execution client`,`application(controller/service)`,`tests`.
- Plan ref: docs/exec-plans/completed/PRW-006-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-015` 加速与改写治理工作台闭环
- Human confirmation point: 若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。
- Data impact: 发布时创建或更新运行时绑定并回写改写记录发布字段；暂停/撤销会改变自动改写生效状态。
- Rollback / recovery: 暂停或撤销已发布绑定；回退发布接口后保持改写记录为未发布或暂停状态。
- Validation:
  - `mvn -pl sql-optimization,query-execution,sqlforge-shared -am test、python3 scripts/foreman.py validate PRW-006`
  - `python3 scripts/foreman.py validate PRW-006`
- Progress log:
  - 2026-05-11: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented sql-optimization rewrite record publish, pause, and unpublish endpoints; shared runtime rewrite binding DTOs; query-execution runtime binding client integration; rewrite record status write-back; failure trace and publish compensation; and PRW-006 contract tests.
  - Validation evidence: mvn -pl sql-optimization,query-execution,sqlforge-shared -am test; python3 scripts/foreman.py validate PRW-006; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check.
  - Residual risk: Synchronous compensation cannot guarantee distributed transaction semantics if local write-back and remote compensation both fail; PRW-007 still owns applying active runtime bindings in the query-execution execution path.
  - Next step: Continue with PRW-007 to apply active runtime rewrite bindings during SQL execution.

### PRW-005: 定义 query-execution 运行时改写绑定模型

- Status: done
- Completed at: 2026-05-11
- Commit subject: `PRW-005 implement runtime rewrite binding model`
- Priority: 1
- Depends on: `PRW-001`,`D-TASK-032`
- Scope: query-execution 必须成为生产自动改写运行时绑定的主闭环真值，JDBC Agent/Redis 不得作为唯一真值。 Tech: `JAVA-BE`,`SQL`,`DOCS`. Layer: `query-execution`,`persistence`,`domain`,`infrastructure`,`tests`.
- Plan ref: docs/exec-plans/completed/PRW-005-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-015` 加速与改写治理工作台闭环
- Human confirmation point: 若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。
- Data impact: 新增运行时改写绑定持久化或等价存储结构；不改变当前 SQL 执行结果。
- Rollback / recovery: 停用绑定仓储和服务入口，保持原 query-execution 执行路径不读取改写绑定。
- Validation:
  - `mvn -pl query-execution test、python3 scripts/foreman.py validate PRW-005`
  - `python3 scripts/foreman.py validate PRW-005`
- Progress log:
  - 2026-05-11: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented query-execution runtime rewrite binding domain model, DB persistence schema, MyBatis repository, internal publish/resolve/pause/unpublish API surface, and PRW-005 contract tests without changing SQL execution behavior.
  - Validation evidence: mvn -pl query-execution test; python3 scripts/foreman.py validate PRW-005; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check.
  - Residual risk: PRW-006 still owns sql-optimization publish/pause/unpublish orchestration and rewrite record status write-back; PRW-007 still owns applying active bindings in the execution path.
  - Next step: Continue with PRW-006 publish, pause, and unpublish integration against this query-execution runtime binding surface.

### PRW-004: 实现发布资格策略与验证门禁

- Status: done
- Completed at: 2026-05-11
- Commit subject: `PRW-004 implement rewrite publish eligibility gate`
- Priority: 1
- Depends on: `PRW-003`,`HARN-135`
- Scope: 所有发布接口必须调用同一个后端策略服务，页面只能展示策略结果，不能成为核心门禁。 Tech: `JAVA-BE`,`SQL`,`DOCS`. Layer: `sql-optimization`,`application service`,`domain policy`,`tests`.
- Plan ref: docs/exec-plans/completed/PRW-004-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-015` 加速与改写治理工作台闭环
- Human confirmation point: 若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。
- Data impact: 读取验证、告警和改写记录状态并产生资格判断；不修改运行时绑定。
- Rollback / recovery: 回退策略服务和发布入口调用，恢复为不可发布或只读状态。
- Validation:
  - `mvn -pl sql-optimization test、python3 scripts/foreman.py validate PRW-004`
  - `python3 scripts/foreman.py validate PRW-004`
- Progress log:
  - 2026-05-11: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented centralized rewrite publish eligibility policy, structured refusal reasons, read-only publish-eligibility API, service conversion, and focused policy/service/controller coverage without mutating runtime bindings.
  - Validation evidence: mvn -pl sql-optimization -Dtest=RewritePublishEligibilityPolicyTest,AccelerationRewriteContractApplicationServiceTest,SqlRewriteRecordControllerTest test; mvn -pl sql-optimization test; python3 scripts/foreman.py validate PRW-004; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check.
  - Residual risk: PRW-006 still owns publish/pause/unpublish actions and query-execution runtime binding mutation; target runtime dialect validation remains conservative datasource evidence until PRW-005/PRW-006 provide runtime binding checks.
  - Next step: Continue with PRW-005 runtime binding model and PRW-006 publish actions.

### PRW-003: 实现改写记录审批状态机

- Status: done
- Completed at: 2026-05-11
- Commit subject: `PRW-003 implement rewrite record review state machine`
- Priority: 1
- Depends on: `PRW-002`
- Scope: 未审批或已驳回的改写记录不得发布运行时绑定，所有非法状态迁移必须由后端拒绝。 Tech: `JAVA-BE`,`SQL`,`DOCS`. Layer: `sql-optimization`,`application(controller/service)/domain/infrastructure`,`tests`.
- Plan ref: docs/exec-plans/completed/PRW-003-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-015` 加速与改写治理工作台闭环
- Human confirmation point: 若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。
- Data impact: 更新改写记录审批状态和审计字段；不直接创建运行时绑定，不执行推荐 SQL。
- Rollback / recovery: 停用审批接口并回退状态机服务；保留新增字段的保守默认状态，避免历史记录丢失。
- Validation:
  - `mvn -pl sql-optimization test、python3 scripts/foreman.py validate PRW-003`
  - `python3 scripts/foreman.py validate PRW-003`
- Progress log:
  - 2026-05-11: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented rewrite record review API and backend review state machine with controlled reopen path.
  - Validation evidence: mvn -pl sql-optimization test; python3 scripts/foreman.py validate PRW-003; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check.
  - Residual risk: PRW-004 and PRW-006 still own publish eligibility and runtime binding publication.
  - Next step: Continue with PRW-004 publish eligibility policy.

### PRW-002: 扩展改写记录审批与发布数据模型

- Status: done
- Completed at: 2026-05-11
- Commit subject: `PRW-002 extend rewrite record review publish data model`
- Priority: 1
- Depends on: `PRW-001`
- Scope: 改写记录必须独立持有 reviewStatus、publishStatus 和 runtime binding 追踪字段，manualReviewRequired 不得自动代表审批通过。 Tech: `JAVA-BE`,`SQL`,`DOCS`. Layer: `sql-optimization`,`persistence`,`application(controller/service)/domain/infrastructure`,`docs`.
- Plan ref: docs/exec-plans/completed/PRW-002-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-015` 加速与改写治理工作台闭环
- Human confirmation point: 若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。
- Data impact: 新增兼容性数据库字段、映射和 DTO 字段；旧数据必须有保守默认状态，不迁移为已审批或已发布。
- Rollback / recovery: 通过兼容 DDL 或停用新增读写路径回退；保留旧改写记录查询能力，不删除历史记录。
- Validation:
  - `mvn -pl sql-optimization test、node scripts/lint-repository-knowledge.js、python3 scripts/foreman.py validate PRW-002`
  - `python3 scripts/foreman.py validate PRW-002`
- Progress log:
  - 2026-05-11: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added independent rewrite review, publish, and runtime binding fields across compatible SQL DDL, init schema, Java domain model, DTO/VO, MyBatis mappings, repository conversion, and focused tests.
  - Validation evidence: mvn -pl sql-optimization test; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py validate PRW-002; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/task_audit.py --check --phase post-closeout; git diff --check.
  - Residual risk: PRW-003 and PRW-006 still own review state transitions and publish actions; PRW-002 only persists and exposes conservative default fields.
  - Next step: Continue with PRW-003 approval state machine on top of the new review fields.

### HARN-145: 重构加速与改写中心前端工作流

- Status: done
- Completed at: 2026-05-11
- Commit subject: `feat(frontend): refactor acceleration rewrite center workflow`
- Priority: 1
- Depends on: HARN-142,HARN-114,HARN-116
- Scope: 重构推荐与加速中心和加速治理工作台前端信息架构，去除主视图卡片堆叠，改为紧凑筛选、tabs、分页表格、弹窗和抽屉工作流；保持现有路由、runtime API、后端 payload、核心 data-testid 与治理边界不变。
- Validation:
  - `python3 scripts/foreman.py validate HARN-145`
- Progress log:
  - 2026-05-11: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 重构推荐与加速中心为筛选 tabs、分页推荐表、详情 tabs 与证据抽屉；重构加速治理工作台为紧凑来源条、分页工作流表格、来源弹窗和统一证据抽屉；加固两个页面契约脚本，禁止回退到卡片/面板堆叠。
  - Validation evidence: python3 scripts/foreman.py validate HARN-145 --include-task-audit --extra-command "node scripts/check-recommendation-page-contract.mjs" --extra-command "node scripts/check-acceleration-workbench-contract.mjs" --extra-command "npm run smoke:acceleration-governance" passed; git diff --check passed.
  - Residual risk: 验证为 repo-closed 前端契约、构建和 Playwright mock smoke；未连接真实外部 Hetu/MRS 环境做人工点击验收，未改后端接口或持久化。
  - Next step: 后续前端评审继续沿用分页表格、tabs、弹窗和抽屉模式，避免在治理主视图恢复卡片堆叠。

### U-TASK-006: Restart local frontend/backend runtime

- Status: done
- Completed at: 2026-05-11
- Commit subject: `U-TASK-006 restart local runtime with JDK 8u112`
- Priority: 1
- Depends on: N/A
- Scope: Restart the local SQLForge frontend and backend runtime for the current developer environment. Scope is limited to process management, health checks, and recording any environment blocker; no business code or long-term configuration changes.
- Validation:
  - `curl -fsS http://localhost:8080/api/governance/health`
  - `curl -fsS http://localhost:8081/actuator/health`
  - `curl -fsS http://localhost:8082/actuator/health`
  - `curl -fsS http://localhost:8083/actuator/health`
  - `curl -fsS http://127.0.0.1:3000`
- Progress log:
  - 2026-05-11: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-11: restarted the frontend Vite runtime with pid `3884877`; `http://127.0.0.1:3000` returned successfully after restart.
  - 2026-05-11: verified backend health endpoints on `8080` through `8083` are `UP`, but did not restart backend because the only discovered Java runtime is `/usr/bin/java` / `1.8.0_482`, while repository rules require JDK `8u112` for backend local runtime.
  - 2026-05-11: human confirmed installing local JDK `8u112`; resumed task to install a user-level JDK and restart backend under that runtime.
  - 2026-05-11: installed Azul Zulu `zulu8.19.0.1-jdk8.0.112-linux_x64` under `~/tools/jdks`, linked `~/tools/jdk8u112`, and updated `~/.bashrc` so login/interactive bash resolves `JAVA_HOME=/home/chase007/tools/jdk8u112`.
  - 2026-05-11: restarted backend services under JDK `1.8.0_112`; `governance`, `query-execution`, `sql-optimization`, and `benchmark-engine` pid environments all show `JAVA_HOME=/home/chase007/tools/jdk8u112`, and all four health endpoints returned `UP`.
- Context closeout:
  - Completed scope: Installed local Azul Zulu JDK 8u112 at /home/chase007/tools/jdk8u112, made login/interactive bash prefer it, restarted the frontend and all four backend services, and recorded the resolved runtime decision.
  - Validation evidence: python3 scripts/foreman.py validate U-TASK-006 --include-task-audit with extra JDK, backend health, and frontend reachability checks; direct health checks for 8080-8083 and 3000; backend pid environments show JAVA_HOME=/home/chase007/tools/jdk8u112.
  - Residual risk: System /usr/bin/java remains 1.8.0_482 for shells that do not read the user bash configuration; SQLForge local backend runtime and new login/interactive bash sessions use /home/chase007/tools/jdk8u112.
  - Next step: Use /home/chase007/tools/jdk8u112 as the SQLForge local backend Java runtime; no further backend restart is pending.

### HARN-142: 加速与改写治理端到端 smoke 与文档收口

- Status: done
- Completed at: 2026-05-10
- Commit subject: `chore(governance): close acceleration rewrite smoke`
- Priority: 1
- Depends on: `HARN-141`
- Scope: 补齐 repo-closed smoke、runbook、契约检查脚本、文档同步与残余风险收口；真实 Hetu/MRS 证据仍归 HARN-016 / INBOX-002，不作为默认阻断。
- Validation:
  - `python3 scripts/foreman.py validate HARN-142`
- Progress log:
  - 2026-05-10: HARN-142 preflight bound task context; added unified acceleration/rewrite governance repo-closed smoke entry, closeout contract check, runbook, npm entry, and docs index updates.
- Context closeout:
  - Completed scope: Added the HARN-142 acceleration/rewrite governance repo-closed smoke bundle, npm entry, closeout contract check, runbook, document coverage registration, product spec closeout notes, and governance policy digest update.
  - Validation evidence: python3 scripts/foreman.py validate HARN-142 --include-task-audit --extra-command 'npm run smoke:acceleration-governance' --extra-command 'node scripts/check-acceleration-rewrite-governance-closeout.mjs' --extra-command 'git diff --check' passed after fixing document coverage and governance policy drift; npm run smoke:acceleration-governance passed; node scripts/check-acceleration-rewrite-governance-closeout.mjs passed; python3 scripts/foreman.py compile-governance --check passed.
  - Residual risk: Repo-closed smoke uses mocked API browser responses and static contracts; real Hetu/MRS EXPLAIN, scan volume, P99, materialized-view benefit, and external data-loading evidence remain HARN-016 / INBOX-002 environment-backed evidence and are not a default blocker for HARN-142.
  - Next step: No HARN-128 through HARN-142 repo-closed follow-up remains in this decomposition; future CI promotion or real Hetu/MRS evidence work must be handled by a new task with human confirmation.

### HARN-141: 监控与告警前端联动

- Status: done
- Completed at: 2026-05-10
- Commit subject: `feat(frontend): link rewrite monitoring alerts`
- Priority: 1
- Depends on: `HARN-136`, `HARN-138`, `HARN-140`
- Scope: 在工作台、推荐中心、SQL 历史展示 validation status、告警入口和自动暂停证据；不得把模拟通知写成真实通知成功。
- Validation:
  - `python3 scripts/foreman.py validate HARN-141`
- Context closeout:
  - Completed scope: Alert center now consumes backend governance alert list/detail/ACK APIs; workbench, recommendation center, and SQL history expose validation status, alert entry points, alert refs, and autoApplyPaused evidence while keeping simulated notification states explicit.
  - Validation evidence: python3 scripts/foreman.py validate HARN-141 with alert/history/workbench/recommendation contract extras and acceleration workbench browser smoke passed; python3 scripts/task_audit.py --check --phase pre-closeout passed; before screenshot equivalent: pre-change HARN-140/HARN-141 frontend contract baseline inspected; after screenshot: .codex-log/harn-141/after-alert-center.png; Codex visual self-review / 读图 completed, visual review passed with no visual drift.
  - Residual risk: Browser smoke and after screenshot use repo-closed mocked API responses, not a live backend; external Hetu/MRS evidence remains HARN-016 / INBOX-002, and notification delivery remains simulated rather than real email.
  - Next step: Proceed to HARN-142 end-to-end repo-closed smoke and documentation closeout.

### HARN-140: SQL 历史改写记录 tab 与筛选

- Status: done
- Completed at: 2026-05-10
- Commit subject: `feat(governance): expose query history rewrite records`
- Priority: 1
- Depends on: `HARN-134`, `HARN-110`
- Scope: SQL 历史列表新增改写记录筛选，详情新增改写记录 tab、diff 跳转和验证状态展示；保留分页、默认筛选、脱敏和原始 SQL 展示契约。
- Validation:
  - `python3 scripts/foreman.py validate HARN-140`
- Context closeout:
  - Completed scope: Added governance query-history rewrite-record filters, backend historyId scoping through sql-optimization rewrite records, frontend SQL history filters and rewrite-record detail tab, localized copy, contract checks, browser smoke coverage, and focused backend tests.
  - Validation evidence: python3 scripts/foreman.py validate HARN-140 --include-task-audit --extra-command 'node scripts/check-history-page-contract.mjs' --extra-command 'mvn -pl governance -am -Dtest=GovernanceQueryHistoryControllerTest,GovernanceHistoryApplicationServiceTest,AuthWebMvcTest -Dsurefire.failIfNoSpecifiedTests=false test' --extra-command 'git diff --check'; npm run smoke:frontend-dev with TMPDIR=.tmp and FRONTEND_DEV_SMOKE_TIMEOUT_MS=60000; Playwright screenshot self-review under .codex-log/harn-140; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check.
  - Residual risk: Local Java validation ran under OpenJDK 1.8.0_482 because this Codex environment does not provide the required JDK 8u112; Java results are compatibility evidence only until rerun on JDK 8u112. Rewrite-record list filtering depends on sql-optimization returning the matching record set for tenant-side historyId scoping.
  - Next step: Proceed to HARN-141 monitoring and alert frontend linkage using the HARN-140 rewrite-record filters and detail evidence.

### HARN-139: 推荐中心 SQL diff 与规则详情

- Status: done
- Completed at: 2026-05-10
- Commit subject: `feat(frontend): wire recommendation diff details`
- Priority: 1
- Depends on: `HARN-132`, `HARN-114`
- Scope: 推荐详情接入 diff 视图、ruleChain、risk、precondition、unappliedRules 和人工复核标识；前后 SQL 必须能对比差异。
- Validation:
  - `python3 scripts/foreman.py validate HARN-139`
- Context closeout:
  - Completed scope: Implemented HARN-139 recommendation-center SQL diff and rule detail surface: recommendation diff API loading, SQL Diff tab with textDiff/AST/diff summary evidence, Rules & Risk tab with ruleChain/preconditions/semanticRisks/unappliedRules, manual review and high-risk guard, i18n copy, and recommendation page contract updates. No backend API, persistence, SQL execution, or auto-apply behavior was changed.
  - Validation evidence: python3 scripts/foreman.py validate HARN-139 --include-task-audit --extra-command 'node scripts/check-recommendation-page-contract.mjs' --extra-command 'npm run test:sql-ui-contract' --extra-command 'npm run test:frontend-page-governance'; npm run lint; npm run build; npm run test:sql-ui-contract; npm run test:frontend-page-governance; node scripts/check-recommendation-page-contract.mjs; git diff --check; R-186 screenshots .codex-log/harn-139/after-diff-desktop.png, after-rules-desktop.png, after-rules-narrow.png; Codex visual self-review passed after fixing narrow rule-row overflow, no overlap or hidden risk marker remains.
  - Residual risk: Browser visual evidence used mocked repo-closed recommendation/diff payloads rather than a live Hetu/MRS environment; live external environment evidence remains outside HARN-139 and continues under HARN-016/INBOX-002.
  - Next step: Proceed to HARN-140 SQL history rewrite-record tab and filters, preserving HARN-139 diff/rule/manual-review evidence contracts.

### HARN-138: 工作台候选、计划审批与应用验证 tabs

- Status: done
- Completed at: 2026-05-10
- Commit subject: `feat: HARN-138 wire acceleration workbench tabs`
- Priority: 1
- Depends on: `HARN-137`, `HARN-134`
- Scope: 接入候选建议、SQL 差异、计划审批、应用验证、接口证据 tabs 与真实接口按钮；未实现接口必须显式显示未实现，不得 mock 成功。
- Validation:
  - `python3 scripts/foreman.py validate HARN-138`
- Context closeout:
  - Completed scope: Implemented HARN-138 real acceleration governance workbench tabs: candidate create/list/detail, suggestion task submit/status, SQL diff loading, rewrite record creation, acceleration plan submit/approval/apply/verify/rollback, baseline and accelerated query evidence, rewrite/validation monitoring evidence, guarded interface evidence tab, i18n updates, runtimeGateApi wrappers, static contract updates, focused Playwright API mock smoke, and R-186 before/after screenshots under .codex-log/harn-138.
  - Validation evidence: python3 scripts/foreman.py validate HARN-138 --include-task-audit --extra-command 'node scripts/check-navigation-shell-contract.mjs' --extra-command 'node scripts/check-acceleration-workbench-contract.mjs' --extra-command 'npm run smoke:acceleration-workbench'; npm run lint; npm run build; npm run test:form-governance; npm run test:sql-ui-contract; npm run test:frontend-page-governance; node scripts/check-navigation-shell-contract.mjs; node scripts/check-acceleration-workbench-contract.mjs; npm run smoke:acceleration-workbench; R-186 screenshots .codex-log/harn-138/before-desktop.png, before-narrow.png, after-desktop.png, after-narrow.png, after-drawer.png; browser layout check passed.
  - Residual risk: Repo-closed browser smoke uses mocked API responses, not a live backend or real Hetu/MRS evidence; true external Hetu/MRS validation remains HARN-016 / INBOX-002, and broader alert/status cross-page linkage remains HARN-141.
  - Next step: Proceed to HARN-139 recommendation-center SQL diff/rule detail or HARN-140 SQL history rewrite-record surface, preserving HARN-138 trace keys and evidence contracts.

### HARN-137: 前端加速治理工作台壳层

- Status: done
- Completed at: 2026-05-10
- Commit subject: `feat(frontend): add acceleration governance workbench shell`
- Priority: 1
- Depends on: `HARN-133`, `HARN-116`
- Scope: 建立加速治理工作台双入口、流程图、source fields、已有页面跳转和证据抽屉；不重复已有解析/推荐/SQL 历史完整页面。
- Validation:
  - `python3 scripts/foreman.py validate HARN-137`
- Context closeout:
  - Completed scope: Implemented HARN-137 read-only acceleration governance workbench shell: independent /governance/acceleration-workbench route, parse/query source fields, flow map, existing-page navigation, disabled HARN-138 future actions, source/route/action evidence drawer, i18n, and navigation/workbench contract checks. R-186 before screenshot: .codex-log/harn-137/before-desktop.png and .codex-log/harn-137/before-narrow.png; after screenshot: .codex-log/harn-137/after-desktop.png, .codex-log/harn-137/after-narrow.png, and .codex-log/harn-137/after-drawer.png. Codex 读图复核 completed; 修复 flow token wrapping and Chinese mock wording; no visual drift remains.
  - Validation evidence: npm run lint; npm run build; npm run test:form-governance; npm run test:sql-ui-contract; npm run test:frontend-page-governance; node scripts/check-navigation-shell-contract.mjs; node scripts/check-acceleration-workbench-contract.mjs; Playwright smoke for route/mode switch/disabled future actions/evidence drawer; python3 scripts/foreman.py validate HARN-137 --include-task-audit with navigation/workbench contract extras and git diff --check.
  - Residual risk: HARN-137 intentionally leaves real candidate, plan approval, apply, verify and rollback API actions disabled for HARN-138; screenshot evidence is local Vite rendering evidence, not a full browser matrix.
  - Next step: Proceed to HARN-138 to wire candidate, diff, plan approval, apply validation and response evidence tabs to real interfaces without mock success.

### HARN-136: 周期比对调度与差异告警

- Status: done
- Completed at: 2026-05-10
- Commit subject: `feat(sql-optimization): add rewrite validation scheduler alerts`
- Priority: 1
- Depends on: `HARN-135`, `F-TASK-037`
- Scope: 落地 scheduled validation、自动暂停应用、`SQL_REWRITE_RESULT_DIVERGENCE` 告警联动和审计追溯；不得自动回滚生产配置。
- Validation:
  - `python3 scripts/foreman.py validate HARN-136`
- Context closeout:
  - Completed scope: Implemented configurable rewrite validation scheduling, DIVERGED-only auto-apply pause evidence, SQL_REWRITE_RESULT_DIVERGENCE governance alert emission, linkage trace writeback, shared DTOs, contract docs and focused scheduler/alert tests.
  - Validation evidence: mvn -pl sql-optimization,governance,sqlforge-shared -am test; python3 scripts/foreman.py validate HARN-136; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance --check; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check
  - Residual risk: Local Java runtime is OpenJDK 1.8.0_482 rather than required JDK 8u112, so Maven evidence remains compatibility evidence until rerun in the fixed delivery JDK; scheduler defaults disabled to avoid unconfirmed alert noise.
  - Next step: Proceed with HARN-137/HARN-138/HARN-140 frontend surfaces before HARN-141 alert/status linkage consumes the HARN-136 backend evidence.

### HARN-135: 周期比对执行模型与只读比较引擎

- Status: done
- Completed at: 2026-05-10
- Commit subject: `feat(sql-optimization): add readonly rewrite comparison engine`
- Priority: 1
- Depends on: `HARN-134`
- Scope: 建立 validation policy、result digest、schema/row/hash/checksum comparison；比对必须只读，不把大结果集全量拉回前端。
- Validation:
  - `python3 scripts/foreman.py validate HARN-135`
- Context closeout:
  - Completed scope: Implemented HARN-135 readonly result digest execution contract, backend comparison engine, validation run execution path, tests and interface docs.
  - Validation evidence: mvn -pl query-execution,sql-optimization -am test; python3 scripts/foreman.py validate HARN-135; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance --check; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check
  - Residual risk: Local Java runtime is not confirmed as required JDK 8u112; Maven evidence is compatibility evidence until rerun in the fixed delivery JDK. HARN-136 remains responsible for scheduler and alert emission.
  - Next step: Proceed to HARN-136 scheduled validation and SQL_REWRITE_RESULT_DIVERGENCE alert linkage.

### HARN-134: 改写记录写入与 SQL 历史聚合接口

- Status: done
- Completed at: 2026-05-10
- Commit subject: `feat(governance): aggregate query history rewrite records`
- Priority: 1
- Depends on: `HARN-129`, `HARN-133`
- Scope: 写入 `sql_rewrite_record`，并提供 `GET /api/governance/query-history/{historyId}/rewrite-records` 聚合面，使 SQL 历史详情能看到改写记录、diff、验证状态和告警引用。
- Validation:
  - `python3 scripts/foreman.py validate HARN-134`
- Context closeout:
  - Completed scope: Implemented HARN-134 rewrite-record history aggregation: sql-optimization now supports tenant/history scoped rewrite-record queries, governance exposes GET /api/governance/query-history/{historyId}/rewrite-records via a protected sql-optimization client, and aggregation preserves diff, validation status and alert refs within tenant scope.
  - Validation evidence: mvn -pl sql-optimization test; mvn -pl governance test; python3 scripts/foreman.py validate HARN-134 --include-task-audit --extra-command 'mvn -pl sql-optimization test' --extra-command 'mvn -pl governance test' --extra-command 'git diff --check'; python3 scripts/task_audit.py --check --phase pre-closeout.
  - Residual risk: Local Codex validation ran under OpenJDK 1.8.0_482; repository delivery baseline remains JDK 8u112 and should be rerun in that fixed environment for release evidence.
  - Next step: Proceed to HARN-135 periodic comparison execution model using the rewrite-record aggregation surface and validation status fields.

### HARN-133: 加速候选生成统一入口

- Status: done
- Completed at: 2026-05-10
- Commit subject: `feat(sql-optimization): unify acceleration candidate source validation`
- Priority: 1
- Depends on: `HARN-129`, `HARN-132`
- Scope: 统一解析驱动与查询驱动 source normalization、candidate API、`sourceType/sourceKind/sourceId/evidenceLevel` 追溯键校验和 static/runtime evidence 分层。
- Validation:
  - `python3 scripts/foreman.py validate HARN-133`
- Context closeout:
  - Completed scope: Implemented unified PARSE/QUERY candidate source normalization, trace-key sourceId derivation, sourceKind/evidenceLevel compatibility validation, and service/controller regression coverage for HARN-133.
  - Validation evidence: python3 scripts/foreman.py validate HARN-133 --include-task-audit --extra-command "mvn -pl sql-optimization test" --extra-command "git diff --check"; python3 scripts/task_audit.py --check --phase pre-closeout.
  - Residual risk: Local validation ran under OpenJDK 1.8.0_482; repository delivery baseline remains JDK 8u112 and should be used in CI/release environments.
  - Next step: Proceed to HARN-134 rewrite record write path and query-history aggregation using the normalized candidate trace fields.

### HARN-132: 建立 SQL diff 后端服务

- Status: done
- Completed at: 2026-05-10
- Commit subject: `feat(sql-optimization): add recommendation sql diff service`
- Priority: 1
- Depends on: `HARN-130`
- Scope: 提供文本 diff、规则级 diff、AST 摘要 diff 与 recommendation diff API，供推荐中心、加速治理工作台和 SQL 历史复用；diff 只做展示证据，不改写 SQL。
- Validation:
  - `python3 scripts/foreman.py validate HARN-132`
- Context closeout:
  - Completed scope: Implemented the HARN-132 SQL diff backend service for recommendation diff: deterministic text diff hunks, rule-level evidence from ruleChain/unappliedRules/semanticRisks, AST summary comparison via the existing parser pipeline, and display-only diff summary boundaries without SQL rewrite, persistence changes, or recommendation write-back.
  - Validation evidence: java -version; mvn -pl sql-optimization -Dtest=SqlDiffApplicationServiceTest,AccelerationRecommendationApplicationServiceTest,AccelerationRecommendationControllerTest test; mvn -pl sql-optimization test; python3 scripts/foreman.py validate HARN-132 --include-task-audit --extra-command "mvn -pl sql-optimization test" --extra-command "git diff --check"; git diff --check; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance --check.
  - Residual risk: Local Codex environment provides OpenJDK 1.8.0_482 rather than required JDK 8u112, so Java compile/test evidence is compatibility evidence only until rerun in the fixed delivery JDK; HARN-135/HARN-136 remain responsible for result equivalence validation, not HARN-132.
  - Next step: Proceed to HARN-133 acceleration candidate unified entry, reusing the diff API as display evidence.

### HARN-131: 实现首批 L0/L1 安全改写规则

- Status: done
- Completed at: 2026-05-10
- Commit subject: `feat(sql-optimization): implement safe rewrite rule candidates`
- Priority: 1
- Depends on: `HARN-130`
- Scope: 实现 COUNT、重复 group/order、select star 元数据化、重复子查询 CTE 候选、函数谓词区间候选；无法证明安全时必须标记人工复核，不自动应用。
- Validation:
  - `python3 scripts/foreman.py validate HARN-131`
- Context closeout:
  - Completed scope: Implemented first L0/L1 rewrite behavior in the existing sql-optimization recommendation pipeline: safe COUNT and duplicate group/order candidate rewrites, plus manual-review metadata for SELECT star, repeated subquery CTE, and function predicate range candidates.
  - Validation evidence: java -version; mvn -pl sql-optimization -Dtest=SqlOptimizationPipelineServiceTest clean test; mvn -pl sql-optimization -Dtest=SqlOptimizationPipelineServiceTest,ParseTriggeredRewriteRecommendationServiceTest,OptimizationTaskWorkerTest,AccelerationRecommendationApplicationServiceTest test; mvn -pl sql-optimization test; python3 scripts/foreman.py validate HARN-131; git diff --check; python3 scripts/task_audit.py --check --phase pre-closeout.
  - Residual risk: Local Codex environment provides OpenJDK 1.8.0_482 rather than required JDK 8u112, so Java validation is compatibility evidence only until rerun under the fixed delivery JDK.
  - Next step: Proceed to HARN-132 SQL diff backend service.

### HARN-130: 深化推荐 SQL 规则输出模型

- Status: done
- Completed at: 2026-05-10
- Commit subject: `feat(sql-optimization): deepen recommendation rule output model`
- Priority: 1
- Depends on: `HARN-128`
- Scope: 建立 L0/L1/L2 rule model、rule chain、preconditions、semantic risks、unapplied rules、manualReviewRequired 与 autoApplyAllowed 输出；不得把静态启发式写成真实收益。
- Validation:
  - `python3 scripts/foreman.py validate HARN-130`
- Context closeout:
  - Completed scope: Expanded acceleration recommendation rule output model across domain DTO VO persistence SQL migration and parse-triggered recommendation flow; added L0/L1/L2 rule-chain, unapplied-rules, preconditions, semantic-risk, expected-benefit, estimated-cost, confidence, validation, and manual-review governance fields without claiming static heuristic gains as real execution improvement.
  - Validation evidence: python3 scripts/foreman.py validate HARN-130 --include-task-audit with mvn -pl sql-optimization test, repository-knowledge lint, compile-governance --check, and git diff --check passed; focused constructor-injection tests passed; local MySQL migration smoke confirmed added columns and no physical FK.
  - Residual risk: Local Codex environment provides OpenJDK 1.8.0_482 rather than mandated JDK 8u112, so Java validation is compatibility evidence only until run in the fixed delivery JDK; HARN-131/HARN-132 remain responsible for concrete rewrite algorithms and diff execution.
  - Next step: Proceed to dependent recommendation rewrite and diff tasks after JDK 8u112 CI or human-approved environment confirmation.

### HARN-129: 落地加速候选与改写验证持久化

- Status: done
- Completed at: 2026-05-10
- Commit subject: `feat(sql-optimization): persist acceleration rewrite governance records`
- Priority: 1
- Depends on: `HARN-128`
- Scope: 新增 `acceleration_candidate`、`sql_rewrite_record`、`rewrite_validation_run` schema/migration、entity、MyBatis XML mapper 与 repository 测试；不得引入物理外键或明文敏感字段。
- Validation:
  - `python3 scripts/foreman.py validate HARN-129`
- Context closeout:
  - Completed scope: 新增 acceleration_candidate/sql_rewrite_record/rewrite_validation_run 初始化表和增量迁移，补齐 sql-optimization MyBatis record/mapper/XML/repository，默认 database-worker 走 MyBatis 持久化，本地占位模式保留内存实现，并补充 schema/mapping 与 repository 测试。
  - Validation evidence: python3 scripts/foreman.py validate HARN-129; mvn -pl sql-optimization test; docker-compose exec -T mysql mysql -usqlforge -psqlforge sqlforge < sql/migrations/V20260510_001__acceleration_rewrite_governance_persistence.sql; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance --check; git diff --check.
  - Residual risk: Local Codex environment provides OpenJDK 1.8.0_482 rather than required JDK 8u112, so Maven evidence is compatibility evidence only and not a JDK 8u112 compliance pass.
  - Next step: Proceed with HARN-130 to deepen recommendation SQL rule output model.

### HARN-128: 固化加速候选与改写记录后端契约

- Status: done
- Completed at: 2026-05-10
- Commit subject: `feat(sql-optimization): add acceleration rewrite contract skeleton`
- Priority: 1
- Depends on: `HARN-144`
- Scope: 补齐 acceleration candidate、sql rewrite record、rewrite validation run 的后端 DTO/VO、状态枚举、接口契约与最小 controller/service 骨架；保持后端权威、租户隔离、推荐与真实执行边界不变。
- Validation:
  - `python3 scripts/foreman.py validate HARN-128`
- Context closeout:
  - Completed scope: Implemented sql-optimization acceleration candidate, rewrite record, validation run and recommendation diff backend contract skeletons with DTO/VO, status enums, controllers, services, in-memory repositories and focused contract tests; no SQL migration, MyBatis mapper or real diff/comparison engine was added.
  - Validation evidence: python3 scripts/foreman.py validate HARN-128; python3 scripts/task_audit.py --check --phase pre-closeout; mvn -pl sql-optimization -DskipTests compile; mvn -pl sql-optimization test; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance --check; git diff --check.
  - Residual risk: Local Codex environment provides OpenJDK 1.8.0_482 rather than required JDK 8u112, so Maven compile/test evidence is compatibility evidence only and not a JDK 8u112 compliance pass; true persistence, SQL diff, deep rewrite model, governance history aggregation and periodic comparison remain in HARN-129/HARN-132/HARN-134/HARN-135/HARN-136.
  - Next step: Implement HARN-129 persistence for acceleration_candidate, sql_rewrite_record and rewrite_validation_run using SQL migration, entity, MyBatis XML mapper and repository tests.

### HARN-144: 复核加速改写治理文档一致性

- Status: done
- Completed at: 2026-05-10
- Commit subject: `docs(product): align acceleration governance review docs`
- Priority: 1
- Depends on: `HARN-143`
- Scope: 严格复核 HARN-143 后的加速与改写治理方案、输入输出、历史、产品/接口/数据模型/计划/任务台账一致性；仅修正文档中的遗留歧义、遗漏和任务表达偏差，不实现业务代码、不改变生产边界。
- Validation:
  - `python3 scripts/foreman.py validate HARN-144`
- Progress log:
  - 2026-05-10: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-10: reviewed HARN-143 follow-up docs and identified only consistency corrections: recommendation/evidence fields, alert payload alignment, target model naming, HARN-133 task wording and HARN-128 dependency.
- Context closeout:
  - Completed scope: 严格复核 HARN-143 后的加速与改写治理方案、输入输出、历史、产品规格、接口基线、数据模型、主计划、任务矩阵、覆盖矩阵和任务台账；仅修正文档遗留歧义：推荐输出和历史/告警字段对齐 sourceKind/evidenceLevel，目标模型去除含糊 task_id，rewrite_validation_run 保留 auto_apply_paused，HARN-133 任务表达补齐证据字段，HARN-128 依赖更新为 HARN-144。
  - Validation evidence: python3 scripts/foreman.py validate HARN-144 --include-task-audit --extra-command 'node scripts/lint-repository-knowledge.js' --extra-command 'python3 scripts/foreman.py compile-governance --check' --extra-command 'git diff --check'; node scripts/lint-repository-knowledge.js; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/foreman.py compile-governance --check; git diff --check
  - Residual risk: 本轮只修正文档与任务治理，不实现 HARN-128 至 HARN-142；真实 Hetu/MRS、P99、扫描量、物化视图收益与外部装数证据仍归 HARN-016 / INBOX-002 外部环境链。
  - Next step: 从 HARN-128 开始按 /plan 逐个实现，HARN-128 现在依赖 HARN-144 复核修正版方案。

### HARN-143: 复核加速与改写治理方案完整性

- Status: done
- Completed at: 2026-05-10
- Commit subject: `docs(product): review acceleration rewrite governance plan`
- Priority: 1
- Depends on: `HARN-127`
- Scope: 严格复核 HARN-127 加速与改写治理方案、输入输出、历史记录、产品/接口/数据模型/计划/任务台账一致性，修正文档中的丢失、遗漏、错误、偏离和不对等；本任务只改文档与任务治理记录，不实现业务代码。
- Validation:
  - `python3 scripts/foreman.py validate HARN-143`
- Progress log:
  - 2026-05-10: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-10: reviewed HARN-127 product, interface, data model, master plan, task matrices and ledger; corrected state boundaries, source/evidence fields, alert contracts and HARN-128 dependency.
- Context closeout:
  - Completed scope: 严格复核 HARN-127 加速与改写治理方案的输入、输出、历史记录、产品规格、接口基线、数据模型、主计划、任务矩阵和台账；补齐状态边界、来源字段、证据层级、告警契约、真实接口 smoke 顺序、HARN-128 依赖和 HARN-143 原始需求快照。
  - Validation evidence: python3 scripts/foreman.py validate HARN-143 --include-task-audit --extra-command 'node scripts/lint-repository-knowledge.js' --extra-command 'python3 scripts/foreman.py compile-governance --check' --extra-command 'git diff --check'; node scripts/lint-repository-knowledge.js; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/foreman.py compile-governance --check; git diff --check
  - Residual risk: 本轮只修正文档与任务治理，不实现 HARN-128 至 HARN-142；真实 Hetu/MRS、P99、扫描量、物化视图收益与外部装数证据仍归 HARN-016 / INBOX-002 外部环境链。
  - Next step: 从 HARN-128 开始按 /plan 逐个实现，HARN-128 现在依赖 HARN-143 复核修正版方案。

### HARN-127: 落地加速与改写治理方案和任务清单

- Status: done
- Completed at: 2026-05-10
- Commit subject: `docs(product): land acceleration rewrite governance plan`
- Priority: 1
- Depends on: N/A
- Scope: 复盘加速与改写治理历史方案，补齐完整架构/前端设计文档，并拆分后续 Codex 可执行任务清单；本任务只改文档和任务台账，不实现业务代码。
- Validation:
  - `python3 scripts/foreman.py validate HARN-127`
- Progress log:
  - 2026-05-10: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-10: added acceleration/rewrite governance workbench design, raw requirement snapshot, plan/matrix synchronization, and follow-up task ledger entries HARN-128 through HARN-142.
- Context closeout:
  - Completed scope: 复盘并落地加速与改写治理工作台完整方案，补齐解析驱动/查询驱动双入口、同页候选/审批/验证、SQL diff、深度推荐规则、SQL 历史改写记录、周期比对告警和真实接口 smoke 设计；同步产品规格、接口基线、数据模型、主计划、任务矩阵、任务台账与原始需求快照；本轮不实现业务代码。
  - Validation evidence: python3 scripts/foreman.py validate HARN-127 --include-task-audit --extra-command 'node scripts/lint-repository-knowledge.js' --extra-command 'python3 scripts/foreman.py compile-governance --check' --extra-command 'git diff --check'; node scripts/lint-repository-knowledge.js; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/foreman.py compile-governance --check
  - Residual risk: 本轮仅完成设计与任务落账；HARN-128 至 HARN-142 尚未实现，真实 Hetu/MRS 运行收益和外部环境证据仍归 HARN-016 / INBOX-002。
  - Next step: 用户后续从 HARN-128 至 HARN-142 中选择单个任务，并通过 /plan 细化后按标准 preflight/instantiate/validate/closeout 执行。

### HARN-126: 复核并修复前端中文治理架构缺口

- Status: done
- Completed at: 2026-05-10
- Commit subject: `fix(frontend): route navigation copy through locales`
- Priority: 1
- Depends on: HARN-125
- Scope: 以资深前端架构视角复核 HARN-125 中文治理，修复仍绕过 locale 的导航文案、默认用户/租户英文展示、语言切换语义 key 与 i18n 审计覆盖缺口；保持路由、API、SQL、payload 和技术关键字语义不变。
- Validation:
  - `python3 scripts/foreman.py validate HARN-126`
- Progress log:
  - 2026-05-10: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 复核 HARN-125 中文治理后，修复导航仍使用 {zh,en} 绕过 locale、默认系统租户/平台管理员英文直出、App 语言切换与导航标签使用 inline 数字 key 的问题；导航、默认身份、语言切换和按需导航均改为语义化 locale key；i18n 文案审计扩大到 src/config 并禁止新的 zh/en 绕过对象。
  - Validation evidence: python3 scripts/foreman.py validate HARN-126 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'npm run test:frontend-page-governance' --extra-command 'npm run test:i18n-copy' --extra-command 'npm run test:sql-ui-contract' --extra-command 'npm run test:form-governance' --extra-command 'npm run smoke:frontend-dev' --extra-command 'node scripts/lint-repository-knowledge.js' --extra-command 'git diff --check'；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: HARN-125 生成的 inline.views*.textNNN 大批量页面文案仍是可维护性债务，但本轮已封住导航、默认身份和 src/config 绕过 locale 的高风险缺口；技术标识如 SQL、Trace ID、RPO/RTO、JDBC Agent 按需求保留。
  - Next step: 后续前端文案新增必须继续走语义化 locale key；逐步把 HARN-125 生成的 inline.views*.textNNN 页面 key 按页面域重命名为业务语义 key。

### HARN-125: 前端页面中文化与 i18n 防回退整改

- Status: done
- Completed at: 2026-05-10
- Commit subject: `fix(frontend): localize visible page copy`
- Priority: 1
- Depends on: HARN-124
- Scope: 整改前端页面用户可见英文文案：保留 SQL/API/JSON/HTTP/JDBC/SDK/MySQL/Redis/Kafka/HETU/HIVE、枚举、路径、代码、任务号和后端原始 payload 等技术关键字，其余标题、段落、按钮、提示、表头、空状态、帮助文本迁入 zh-CN/en-US 语言包并随右上角语言切换；补充 i18n 审计脚本防止回退。
- Validation:
  - `python3 scripts/foreman.py validate HARN-125`
- Progress log:
  - 2026-05-10: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 将前端主要业务页和治理页中大量可见中英文三元文案迁入 i18n key；清理 zh-CN 中的英文 prose，保留 SQL/API/JSON/HTTP/JDBC/SDK/MySQL/Redis/Kafka/HETU/HIVE、ID、枚举、路径、代码与后端 payload 等技术标识；新增 check-frontend-i18n-copy.mjs 并接入 npm run test:frontend-page-governance，防止中文文案回退为英文长句。
  - Validation evidence: python3 scripts/foreman.py validate HARN-125 --include-task-audit --extra-command "npm run lint" --extra-command "npm run build" --extra-command "npm run test:frontend-page-governance" --extra-command "npm run test:sql-ui-contract" --extra-command "npm run test:form-governance" --extra-command "node scripts/lint-repository-knowledge.js" --extra-command "git diff --check"；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 技术关键词、接口路径、枚举、ID 和后端原始 payload 按需求保留英文；导航仍沿用既有 route metadata 结构，但中文/英文文案均随右上角语言切换。
  - Next step: 后续新增前端文案继续通过 npm run test:frontend-page-governance 和 npm run test:i18n-copy 约束，不再引入未治理的英文长句。

### HARN-124: Correct issue-scene scoped locations and standard detail lists

- Status: done
- Completed at: 2026-05-10
- Commit subject: `HARN-124: align issue scene detail lists`
- Priority: 1
- Depends on: HARN-123
- Scope: 纠正问题场景明细定位范围：只展示当前问题场景的定位信息，不回退展示其他场景定位；分析后端定位来源与持久化字段关系；补齐报表明细、逻辑对象明细、SQL 明细完整标准列表分页信息与契约验证。
- Validation:
  - `python3 scripts/foreman.py validate HARN-124`
- Progress log:
  - 2026-05-10: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 纠正问题场景明细定位范围：前端只展示当前问题场景定位，不再回退展示其他场景定位；明确后端定位不是独立按场景持久字段，而是读取时由 SQL/诊断/场景集合动态组装；补齐问题场景明细报表、逻辑对象、SQL 三张标准列表的分页契约、接口参数、前端总数展示与后端测试。
  - Validation evidence: python3 scripts/foreman.py validate HARN-124；mvn -pl sql-optimization -Dtest=ReportBatchParseStatisticsAssemblerTest,ReportBatchApplicationServiceTest,ReportBatchControllerTest test；npm run lint；npm run build；npm run test:frontend-page-governance；npm run test:form-governance；npm run test:sql-ui-contract；node scripts/check-history-detail-contract.mjs；node scripts/check-batch-import-contract.mjs；node scripts/lint-repository-knowledge.js；git diff --check；Playwright 截图复核：问题场景 ? tooltip 可见，三张明细表均为 el-table 且三组 el-pagination 显示 Total 24/18/37，SQL 定位显示 SELECT_STAR · 定位待补充，未泄漏 OTHER_SCENE_SNIPPET_SHOULD_NOT_RENDER，点击报表行会携带 reportCode 重新加载联动明细。
  - Residual risk: 当前执行环境仅提供 OpenJDK 1.8.0_482（/usr/lib/jvm/java-8-openjdk-amd64），未提供仓库规则指定的 JDK 8u112；本轮后端测试在可用 JDK8 环境通过，交付环境仍需用 8u112 复验。
  - Next step: 在标准 JDK 8u112 环境进行一次后端相关用例复验。

### HARN-123: Repair report issue-scene detail regressions

- Status: done
- Completed at: 2026-05-10
- Commit subject: `HARN-123 fix report issue-scene detail regressions`
- Priority: 1
- Depends on: HARN-122
- Scope: 复核并修复解析历史报表导入历史解析详情中报表级问题场景帮助提示为空、问题场景明细定位显示无问题、明细三表未按标准列表呈现的问题；必须通过截图自检验证。
- Validation:
  - `python3 scripts/foreman.py validate HARN-123`
- Progress log:
  - 2026-05-10: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 修复报表导入历史解析详情中问题场景帮助提示无内容、问题场景 SQL 定位误显示无问题、明细报表/逻辑对象/SQL 三表标准分页列表缺口；补强 issueLocations 非空契约测试与历史明细契约脚本。
  - Validation evidence: 截图自检：Playwright mock 报表导入历史页面，hover 问题场景问号后确认 el-tooltip 出现 SELECT_STAR 中文说明；问题场景明细截图确认无‘无问题’，缺定位时显示‘定位待补充’；30 行 mock 明细截图确认报表、逻辑对象、SQL 三个列表均出现分页。临时截图已删除。自动验证：npm run lint；npm run build；npm run test:frontend-page-governance；npm run test:form-governance；npm run test:sql-ui-contract；node scripts/check-history-detail-contract.mjs；node scripts/check-batch-import-contract.mjs；node scripts/lint-repository-knowledge.js；mvn -pl sql-optimization -Dtest=ReportBatchParseStatisticsAssemblerTest,ReportBatchApplicationServiceTest,ReportBatchControllerTest clean test；git diff --check；foreman validate HARN-123；task_audit pre-closeout。
  - Residual risk: 未接入真实外部环境数据截图；本轮使用 Playwright 页面截图、mock API、前后端契约测试与后端 controller/service/assembler 测试覆盖用户指出的三类问题。
  - Next step: 如真实环境仍出现定位待补充，应追查具体 SQL 行的后端 issueLocations 生成规则是否缺少更细粒度 pattern。

### HARN-122: Repair report parse statistics issue-scene drilldown

- Status: done
- Completed at: 2026-05-10
- Commit subject: `HARN-122 repair report issue-scene drilldown`
- Priority: 1
- Depends on: HARN-113
- Scope: 收敛报表导入解析统计的问题场景标准列表、提示、定位列宽与问题场景明细报表/逻辑对象/SQL 联动；同步批量解析中心同类统计弹窗；后端问题场景明细接口只返回当前场景相关 issueScenes/issueLocations/计数。
- Validation:
  - `python3 scripts/foreman.py validate HARN-122`
- Progress log:
  - 2026-05-10: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 收敛报表批次问题场景明细后端契约；改造解析历史报表级问题场景标准分页列表、详情联动与定位展示；对齐批量解析中心同类报表统计弹窗。
  - Validation evidence: mvn -pl sql-optimization -Dtest=ReportBatchParseStatisticsAssemblerTest clean test；npm run lint；npm run build；npm run test:frontend-page-governance；npm run test:form-governance；npm run test:sql-ui-contract；node scripts/check-history-detail-contract.mjs；node scripts/check-batch-import-contract.mjs；node scripts/lint-repository-knowledge.js；git diff --check；foreman validate HARN-122；task_audit pre-closeout。
  - Residual risk: 未做浏览器人工截图验证；本次以单元测试、构建、前端治理与静态契约检查覆盖。
  - Next step: 如需要继续扩展，可在后续任务中补端到端点击链路自动化。

### HARN-121: Repair SQL parse runtime errors

- Status: done
- Completed at: 2026-05-10
- Commit subject: `HARN-121 fix sql parse runtime errors`
- Priority: 1
- Depends on: N/A
- Scope: Reproduce SQL parse page, batch report import, and recommendation failures; fix the shared runtime error cause; add focused regression coverage.
- Validation:
  - `python3 scripts/foreman.py validate HARN-121`
- Progress log:
  - 2026-05-10: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-10: reproduced parse structure and report import 500s; runtime logs showed stale synthetic class failures around `StructureParsePriorityScorer$1` and `ReportBatchApplicationService$ReportSourceRow`.
  - 2026-05-10: removed compiler-generated enum-switch and private nested-access bridge dependencies from the parse/report hot paths; added a binary-compatibility regression test.
  - 2026-05-10: restarted `sql-optimization`; direct API, Vite proxy, and Playwright page flows for parse workbench, report import/resolve, and recommendations returned 200 without page errors.
  - 2026-05-10: validation passed with `mvn -pl sql-optimization test`, `npm run lint`, `npm run build`, and `git diff --check`.
- Context closeout:
  - Completed scope: Reproduced SQL parse, report batch import, and recommendation-facing runtime failures; removed fragile synthetic class dependencies in parse/report hot paths; added binary compatibility regression coverage.
  - Validation evidence: mvn -pl sql-optimization test; npm run lint; npm run build; git diff --check; direct API and Vite proxy smokes; Playwright parse workbench/report import/recommendation page flow; python3 scripts/foreman.py validate HARN-121; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Local Java runtime is OpenJDK 1.8.0_482 rather than repository baseline JDK 8u112; no external Hetu/MRS live environment validation was added.
  - Next step: Use JDK 8u112 for delivery/runtime validation before release packaging.

### HARN-096: 修复问题场景详情报表展示不全

- Status: done
- Completed at: 2026-05-10
- Commit subject: `HARN-096 list report issue scene details`
- Priority: 1
- Depends on: N/A
- Scope: Fix parse history report import detail issue-scene drawer so reportDetails are rendered completely in the report-level statistics detail, keep SQL detail pagination unchanged, and add a static contract guard preventing reportDetails slice truncation from returning.
- Validation:
  - `python3 scripts/foreman.py validate HARN-096`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Converted parse history report-batch issue-scene statistics and detail views to Element Plus tables, moved issue-scene detail into a dialog, preserved SQL detail pagination, localized new labels, and added contract guards against reportDetails slice truncation.
  - Validation evidence: python3 scripts/foreman.py validate HARN-096; npm run lint; npm run build; npm run test:frontend-page-governance; npm run test:sql-ui-contract; node scripts/check-history-detail-contract.mjs; node scripts/check-history-page-contract.mjs; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check.
  - Residual risk: No backend API or tenant/pagination request semantics were changed; live before/after screenshot capture was not available in this fresh-context turn, so UI confidence comes from static contracts, governance checks, and production build.
  - Next step: Use the parse history report-import detail drawer with seeded report batch data to perform any optional browser-level visual review.

### HARN-115: 重构告警、取证、修复、故障处置与系统管理页面

- Status: done
- Completed at: 2026-05-10
- Commit subject: `HARN-115 refactor governance operations pages`
- Priority: 2
- Depends on: HARN-108,HARN-116
- Scope: 覆盖 `AlertCenterView`、`AuditForensicsView`、`AuditTroubleshootingView`、`RepairEvidenceView`、`SystemView`；收敛重复 trace lookup、timeline、queue、retry、datasource/config 表格与详情模式，保留权限和后端权威边界。
- Validation:
  - before/after 截图自检
  - `npm run lint`
  - `npm run build`
  - `npm run test:form-governance`
  - `npm run test:frontend-page-governance`
- Context closeout:
  - Completed scope: Refactored AlertCenterView, AuditForensicsView, AuditTroubleshootingView, RepairEvidenceView, and SystemView around shared trace lookup/result/detail and queue retry primitives; moved SystemView orchestration into useSystemManagement while preserving datasource, config, retry, permission, and backend-authority boundaries.
  - Validation evidence: Passed npm run lint; npm run build; npm run test:form-governance; npm run test:frontend-page-governance; node scripts/check-alert-page-contract.mjs; node scripts/check-system-config-contract.mjs; node scripts/check-system-datasource-contract.mjs; python3 scripts/foreman.py validate HARN-115; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check. before screenshot: .codex/state/screenshots/HARN-115/before/{alert-center,audit-forensics,audit-troubleshooting,repair-evidence,system}.png. after screenshot: .codex/state/screenshots/HARN-115/after/{alert-center,audit-forensics,audit-troubleshooting,repair-evidence,system,system-datasource-detail-drawer,system-datasource-test-dialog,system-datasource-test-dialog-focused,audit-troubleshooting-retry-panel}.png and .codex/state/screenshots/HARN-115/after-mobile/{alert-center,audit-forensics,audit-troubleshooting,repair-evidence,system}.png. Codex visual self-review completed; alert detail long-value overflow, mobile trace detail enum clipping, and transient system dialog screenshot opacity were 已修复; visual review passed with no remaining overlap, clipping, card nesting, or management PageHero drift observed.
  - Residual risk: Screenshots used mocked browser API responses for deterministic frontend evidence; no backend service, permission model, or live datasource behavior was changed.
  - Next step: Continue with the next ledger task after HARN-115; keep the shared governance trace primitives as the default for future trace lookup and retry surfaces.

### HARN-114: 重构资产、路由、推荐、压测、接入页面

- Status: done
- Completed at: 2026-05-10
- Commit subject: `feat(frontend): refactor HARN-114 governance pages`
- Priority: 2
- Depends on: HARN-108,HARN-116
- Scope: 覆盖 `AssetCatalogView`、`RoutingGovernanceView`、`RecommendationCenterView`、`BenchmarkView`、`AccessCenterView`；统一列表/详情/证据/placeholder 语义，保留只读证据边界和缺失写 API 的显式边界。
- Validation:
  - before/after 截图自检
  - `npm run lint`
  - `npm run build`
  - `npm run test:sql-ui-contract`
  - `npm run test:frontend-page-governance`
- Context closeout:
  - Completed scope: Refactored AssetCatalogView, RoutingGovernanceView, RecommendationCenterView, BenchmarkView, and AccessCenterView into compact header, toolbar, tabs/list/table, detail, dialog, and drawer flows; preserved existing APIs, read-only evidence boundaries, placeholder write-boundary messaging, SQL payload display, and i18n-backed visible copy. Added mobile shell responsiveness for the touched page screenshots.
  - Validation evidence: Passed npm run lint; npm run build; npm run test:sql-ui-contract; npm run test:frontend-page-governance; npm run smoke:frontend-dev; page contract scripts for asset/routing/recommendation/benchmark/access; python3 scripts/foreman.py validate HARN-114; python3 scripts/task_audit.py --check --phase pre-closeout. Before screenshots: .codex/state/screenshots/HARN-114/before/{asset-catalog,routing-governance,recommendation-center,benchmark,access-center}.png. After screenshots: .codex/state/screenshots/HARN-114/after/{asset-catalog,routing-governance,recommendation-center,benchmark,access-center}.png plus routing-detail-dialog-visible.png, routing-raw-drawer-visible.png, recommendation-detail-tabs.png, benchmark-report.png, benchmark-compensation.png, access-audit-detail-visible.png, access-raw-drawer-visible.png, and after-mobile/*.png. Codex visual self-review completed; duplicate asset empty copy and mobile clipping 已修复; visual review passed with no visual drift, no remaining overlap, no card nesting, and no pagination-summary drift observed.
  - Residual risk: No backend interface, permission, audit, persistence, or data-migration behavior changed; runtime data availability remains governed by existing backend services and the pages retain explicit read-only or missing-write-API boundaries.
  - Next step: Proceed to the next planned frontend governance page task.

### HARN-113: 拆分解析历史查询与报表详情页面

- Status: done
- Completed at: 2026-05-09
- Commit subject: `feat(frontend): HARN-113 split parse history page`
- Priority: 1
- Depends on: HARN-112
- Scope: 覆盖 `ParseRecordView`；拆分筛选、SQL 解析记录、批量/报表历史、详情弹层、报表统计、issue-scene detail 和原始 SQL 展示；保留默认空筛选与 raw SQL 不自动格式化契约。
- Validation:
  - before/after 截图自检
  - `npm run lint`
  - `npm run build`
  - `npm run test:form-governance`
  - `npm run test:sql-ui-contract`
  - `npm run test:frontend-page-governance`
- Context closeout:
  - Completed scope: Split ParseRecordView into a lightweight page shell, parse-record scoped composable, filter panel, SQL history pane, batch/report history pane, report detail drawer, report SQL detail dialog, parse history detail dialog, evidence/export dialog, and shared parse-record styles; updated history/form/SQL/page governance contract scripts to read the split source set while preserving default empty filters, pagination/detail semantics, issue-scene detail, report statistics evidence, and raw SQL autoFormat=false behavior.
  - Validation evidence: before screenshot: .codex/state/screenshots/HARN-113/before-parse-record-page.png, .codex/state/screenshots/HARN-113/before-batch-report-history.png, .codex/state/screenshots/HARN-113/before-report-detail-drawer.png, .codex/state/screenshots/HARN-113/before-report-sql-list.png; after screenshot: .codex/state/screenshots/HARN-113/after-parse-record-page.png, .codex/state/screenshots/HARN-113/after-batch-report-history.png, .codex/state/screenshots/HARN-113/after-report-detail-drawer.png, .codex/state/screenshots/HARN-113/after-report-sql-list.png, .codex/state/screenshots/HARN-113/after-report-sql-list-narrow.png; Codex visual self-review found scoped-style drift after component extraction and 已修复 by applying parse-record scoped styles to split components; visual review passed with no visual drift in the reviewed desktop and narrow screenshots. Passed: npm run lint; npm run build; npm run test:form-governance; npm run test:sql-ui-contract; npm run test:frontend-page-governance; node scripts/check-history-page-contract.mjs; node scripts/check-history-detail-contract.mjs; python3 scripts/foreman.py validate HARN-113 --extra-command "node scripts/check-history-page-contract.mjs" --extra-command "node scripts/check-history-detail-contract.mjs"; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check.
  - Residual risk: No backend API, route path, history persistence, permission, audit, parser, or raw SQL formatting semantics changed. The split components intentionally retain existing local isChinese copy debt as moved lines; the page-governance script now treats exact moved lines as existing debt while still blocking newly introduced hardcoded copy. Visual review used mocked browser data rather than a live backend dataset.
  - Next step: Proceed to HARN-114 asset, routing, recommendation, benchmark, and access page refactor after HARN-113 post-closeout audit passes.

### HARN-112: 拆分批量解析中心页面

- Status: done
- Completed at: 2026-05-09
- Commit subject: `feat(frontend): HARN-112 split batch parse center`
- Priority: 1
- Depends on: HARN-111
- Scope: 覆盖 `ParseBatchCenterView`；拆分普通批量、报表导入、统计标签、详情弹窗、失败详情和 SQL 展示；保留 summary-first、有限明细预览、失败记录可点击详情和大批量渲染约束。
- Validation:
  - before/after 截图自检
  - `npm run lint`
  - `npm run build`
  - `npm run test:sql-ui-contract`
  - `npm run test:frontend-page-governance`
- Progress log:
  - 2026-05-09: HARN-112 bound with `python3 scripts/foreman.py preflight --task HARN-112`; starting frontend split and R-186 screenshot workflow.
- Context closeout:
  - Completed scope: Split ParseBatchCenterView into a compact page shell, useParseBatchCenter state/API orchestration, shared summary/detail display components, and external parse-batch scoped styles; updated the batch import contract guard to read the split source files while preserving existing route, API payloads, data-testid hooks, summary-first previews, failure detail entry points, and SQL UI components.
  - Validation evidence: before screenshot: .codex/state/screenshots/HARN-112/before-parse-batch-center.png; after screenshot: .codex/state/screenshots/HARN-112/after-parse-batch-center.png; Codex visual self-review covered the batch/report tabs, first-screen workbench layout, summary text, and responsive shell via Playwright metrics; visual review passed with no visual drift found. Passed: python3 scripts/foreman.py validate HARN-112 --extra-command 'node scripts/check-batch-import-contract.mjs', npm run lint, npm run build, npm run test:form-governance, npm run test:sql-ui-contract, npm run test:frontend-page-governance, node scripts/check-batch-import-contract.mjs.
  - Residual risk: No backend, parser, persistence, routing, or payload semantics changed. Screenshot self-review used the default empty/local state rather than populated production-scale batch data; large-batch limits remain guarded by contract tokens and existing preview metadata.
  - Next step: Proceed to HARN-113 parse history and report detail page split after HARN-112 post-closeout audit passes.

### HARN-111: 拆分解析工作台与解析统计页面

- Status: done
- Completed at: 2026-05-09
- Commit subject: `feat(frontend): HARN-111 split parse statistics page`
- Priority: 1
- Depends on: HARN-110,HARN-116
- Scope: 覆盖 `AccelerationView`、`ParseStatisticsCenterView`；拆分单条 SQL 输入、结构解析、access parse、结论、统计入口、字段 help 和详情弹层，不改变 parser/API/payload。
- Validation:
  - before/after 截图自检
  - `npm run lint`
  - `npm run build`
  - `npm run test:sql-ui-contract`
  - `npm run test:frontend-page-governance`
- Context closeout:
  - Completed scope: 拆分 AccelerationView 单条 SQL 解析工作台与 ParseStatisticsCenterView 独立统计页面；before screenshot: .codex/state/harn-111/screenshots/before-acceleration-desktop.png, .codex/state/harn-111/screenshots/before-statistics-route-desktop.png；after screenshot: .codex/state/harn-111/screenshots/after-acceleration-desktop.png, .codex/state/harn-111/screenshots/after-statistics-desktop.png, .codex/state/harn-111/screenshots/after-statistics-narrow.png；Codex 读图视觉复核已覆盖首屏主流程、统计页表格、入口拆分和窄屏堆叠，并已修复统计表格列断词 visual drift。
  - Validation evidence: python3 scripts/foreman.py validate HARN-111 --extra-command node scripts/check-parse-workbench-contract.mjs --extra-command node scripts/check-statistics-page-contract.mjs --extra-command node scripts/check-navigation-shell-contract.mjs；npm run lint；npm run build；npm run test:sql-ui-contract；npm run test:frontend-page-governance；python3 scripts/task_audit.py --check --phase pre-closeout。
  - Residual risk: 未修改 parser/API/payload/后端模型；截图证据保存在 .codex/state/harn-111/screenshots/，不作为长期真值入仓；全局 body min-width 1280px 仍限制真正移动端布局，本次按现有壳层完成 narrow viewport 自检。
  - Next step: 进入 HARN-112，继续拆分批量解析中心页面。

### HARN-120: 补齐前端截图自检机器门禁

- Status: done
- Completed at: 2026-05-09
- Commit subject: `chore(frontend): HARN-120 enforce visual review gate`
- Priority: 1
- Depends on: HARN-116
- Scope: 补齐 HARN-116 复核发现的可执行性缺口：让前端治理脚本默认执行自测，新增截图自检 closeout/audit 机器检查，补齐 R-186 validation-rules 索引与任务/计划依赖，使 HARN-111 以后页面任务必须记录 before/after 截图与 Codex 读图修复结论。
- Validation:
  - `npm run lint`
  - `npm run build`
  - `npm run test:frontend-page-governance`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/foreman.py validate HARN-120`
- Progress log:
  - 2026-05-09: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added the task_audit R-186 closeout gate for frontend page tasks carrying the visual screenshot marker; made npm run test:frontend-page-governance run the governance script self-test by default; synced validation-rules, knowledge lint, HARN-111 through HARN-115 dependencies and validation lists, master/task matrices, human constraint history, and the HARN-116 dependency correction.
  - Validation evidence: npm run lint; npm run build; npm run test:frontend-page-governance; python3 -m py_compile scripts/task_audit.py scripts/foreman.py; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance --check; python3 scripts/foreman.py validate HARN-120; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/task_audit.py --check --phase post-closeout; git diff --check; local negative and positive task_audit visual-review fixtures.
  - Residual risk: The R-186 audit gate validates closeout evidence text rather than opening image files, so Codex still must actually capture and read screenshots during page work; this avoids committing binary screenshots while preventing silent omission of before screenshot, after screenshot, visual review, and fix or no-drift conclusions.
  - Next step: Start HARN-111 with the screenshot validation marker already in its ledger; record before screenshot, after screenshot, Codex visual review, and fix or no-drift outcome in closeout.

### HARN-116: 加固前端页面治理脚本与设计文档

- Status: done
- Completed at: 2026-05-09
- Commit subject: `chore(frontend): HARN-116 harden page governance`
- Priority: 2
- Depends on: HARN-109,HARN-110
- Scope: 扩展 `check-frontend-page-governance.mjs`，补充 layout/i18n/card nesting/SQL component/page shell 检查；更新 `docs/frontend/design-system.md` 与相关前端治理文档，防止重构后回退。
- Validation:
  - `npm run lint`
  - `npm run build`
  - `npm run test:frontend-page-governance`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-05-09: resumed from repository preflight and prior implementation plan; hardening screenshot self-review workflow, management page shell rules, pagination/footer/static governance checks.
- Context closeout:
  - Completed scope: Added R-186 screenshot self-review governance; updated frontend design/form governance and HARN-111 through HARN-116 matrix expectations; extended check-frontend-page-governance with diff-aware PageHero, duplicate pagination summary, footer separation, filter density, obsolete copy checks, plus a synthetic self-test; regenerated authority-map.
  - Validation evidence: npm run lint; npm run build; npm run test:frontend-page-governance; node scripts/check-frontend-page-governance.mjs --self-test; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance --check; python3 scripts/foreman.py validate HARN-116 passed after authority-map regeneration; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check.
  - Residual risk: Static governance checks remain heuristic and diff-aware, so screenshot reading remains mandatory for page implementation tasks; HARN-116 changed docs/scripts only and did not render a business page. HARN-120 later tightened this into a closeout audit gate and corrected the dependency truth so HARN-111 treats HARN-116 as a prerequisite.
  - Next step: Start HARN-111 only after the HARN-120 closeout gate is present; capture before screenshots, then after implementation screenshots, and record Codex screenshot self-review fixes in closeout.

### HARN-119: 修复 SQL 历史分页状态栏

- Status: done
- Completed at: 2026-05-09
- Commit subject: `fix(frontend): HARN-119 restore sql history footer pagination`
- Priority: 1
- Depends on: HARN-118
- Scope: Fix SqlHistoryView footer semantics so the left status area only shows the latest query state and the right footer restores Element Plus pagination total, sizes, pager and jumper controls without changing filters, pagination API/composable, detail drawer or SQL display semantics.
- Validation:
  - `python3 scripts/foreman.py validate HARN-119`
- Progress log:
  - 2026-05-09: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restored SqlHistoryView footer semantics so the left footer-status contains only the latest query state and the right pagination cluster uses Element Plus total, sizes, pager and jumper controls; removed the unused resultWindow custom summary locale; added a SQL UI contract guard against the mixed footer summary regression.
  - Validation evidence: npm run lint -- --quiet; npm run build; npm run test:sql-ui-contract; npm run test:frontend-page-governance; Playwright mocked DOM and screenshot assertions at /tmp/sqlforge-harn119-screenshots/sql-history-1440.png, /tmp/sqlforge-harn119-screenshots/sql-history-1040.png, and /tmp/sqlforge-harn119-screenshots/sql-history-760.png; python3 scripts/foreman.py validate HARN-119; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check.
  - Residual risk: No query parameters, pagination composable contract, backend API contract, filter layout, detail drawer, or SQL display semantics changed; screenshot validation used mocked repo-local browser responses rather than a live backend.
  - Next step: No follow-up required for HARN-119; future SQL history footer regressions are covered by npm run test:sql-ui-contract.

### HARN-118: 修复 SQL 历史分页与筛选自适应布局

- Status: done
- Completed at: 2026-05-09
- Commit subject: `fix(frontend): HARN-118 align sql history pagination layout`
- Priority: 1
- Depends on: HARN-117
- Scope: 基于截图复核 SQL 历史页面，修正 HARN-117 仍不合理的管理页布局：分页/结果窗口信息只保留在右下角并完善展示；筛选区每行列数根据可用宽度与字段文案长度自适应；保持 SQL 查询 payload、历史查询参数、详情抽屉、SqlEditorField/SqlCodeBlock 和 raw SQL 展示语义不变。
- Validation:
  - `python3 scripts/foreman.py validate HARN-118`
- Progress log:
  - 2026-05-09: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-09: replaced the SQL history filter grid with field-width-aware flex wrapping, moved result-window/last-query copy into the single right-aligned pagination cluster, removed the obsolete table summary locale copy, and captured mocked browser screenshots at 1440/1040/760 widths under `/tmp/sqlforge-harn118-*.png`.
- Context closeout:
  - Completed scope: 修正 SQL 历史页面筛选与分页布局：筛选区按字段文案长度和可用宽度自适应换行；分页摘要和分页控件收敛为右下角唯一信息簇；移除已废弃的执行记录说明文案；不改变 SQL 查询 payload、历史查询参数、详情抽屉或 raw SQL 展示语义。
  - Validation evidence: python3 scripts/foreman.py validate HARN-118 passed; npm run lint -- --quiet passed; npm run build passed; npm run test:sql-ui-contract passed; npm run test:frontend-page-governance passed; mocked Playwright/Chrome screenshots generated at /tmp/sqlforge-harn118-sql-history-1440.png, /tmp/sqlforge-harn118-filter-1040.png, and /tmp/sqlforge-harn118-table-1440.png with one pagination component, no footer-status node, and no obsolete table summary text.
  - Residual risk: 截图使用 mocked API 数据验证前端布局与交互容器，未连接真实后端环境做人工点击验收；本任务未改后端 API、分页契约或持久化。
  - Next step: 后续前端页面任务默认把截图复核、DOM 数量断言、重复分页/说明文案检查纳入实现内验证，避免只靠代码静态判断页面质量。

### HARN-117: 复核并修复 HARN-110 页面结构回归

- Status: done
- Completed at: 2026-05-09
- Commit subject: `fix(frontend): HARN-117 repair sql page layout regressions`
- Priority: 1
- Depends on: HARN-110
- Scope: 截图复核 SQL 查询与 SQL 历史页面，修复 HARN-110 引入的筛选布局、分页/状态栏重复、无必要说明文案和可见页面结构问题；保留 SQL 执行 payload、历史查询契约、SqlEditorField/SqlCodeBlock、后端权威和 raw SQL 不自动格式化语义。
- Validation:
  - `python3 scripts/foreman.py validate HARN-117`
- Progress log:
  - 2026-05-09: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-09: captured SQL query/history screenshots with mocked API data, confirmed one-column history filters, duplicated pagination/status copy, unnecessary table summary copy, and over-large PageHero usage on high-frequency workbench pages; implemented compact header, filter-grid, pagination, and copy cleanup.
- Context closeout:
  - Completed scope: Captured SQL query/history screenshots with mocked API data, confirmed HARN-110 visual regressions, and repaired high-frequency page layout by replacing oversized PageHero usage with compact SectionHeader headers, restoring SQL history filters to a multi-column grid, removing unnecessary table summary copy, and simplifying the history pagination/status footer while preserving SQL payload and history contracts.
  - Validation evidence: python3 scripts/foreman.py validate HARN-117 --include-task-audit passed; direct npm run lint, npm run build, npm run test:frontend-page-governance, npm run test:sql-ui-contract, and npm run test:form-governance passed; screenshot artifacts were generated under /tmp/sqlforge-harn117-screenshots/.
  - Residual risk: The current governance scripts still do not assert screenshot-level layout quality, filter grid density, duplicate pagination/status text, or oversized PageHero usage on management pages; HARN-116 should add explicit visual/layout guards before the remaining page refactors.
  - Next step: Before starting HARN-111, add a short visual review checklist or screenshot guard for the target page class, then apply the compact management-page header pattern instead of PageHero by default.

### HARN-110: 重构 SQL 查询与 SQL 历史页面结构

- Status: done
- Completed at: 2026-05-09
- Commit subject: `refactor(frontend): HARN-110 restructure sql query history pages`
- Priority: 1
- Depends on: HARN-108
- Scope: 覆盖 `SqlQueryView`、`SqlHistoryView`、`useSqlHistoryList.js`；拆分查询三栏、结果 tabs、历史筛选、详情抽屉和 SQL 三态展示，保留 SQL UI 契约和历史查询契约。
- Validation:
  - `npm run lint`
  - `npm run build`
  - `npm run test:sql-ui-contract`
  - `npm run test:frontend-page-governance`
- Progress log:
  - 2026-05-09: started HARN-110 after task-bound strict preflight; scope remains SQL query and SQL history page structure, history filters, detail drawer, and SQL tri-state display only.
- Context closeout:
  - Completed scope: Refactored SqlQueryView and SqlHistoryView around the HARN-108 shared page layout components; added query access/history result tabs; expanded SQL history filters through existing query-history parameters; split the detail drawer into overview, execution, SQL tri-state, parse/route signals, linked evidence, and audit surfaces while preserving SqlEditorField/SqlCodeBlock and backend history contracts.
  - Validation evidence: python3 scripts/foreman.py validate HARN-110 --include-task-audit passed; direct npm run lint, npm run build, npm run test:sql-ui-contract, npm run test:frontend-page-governance, and npm run test:form-governance passed.
  - Residual risk: No backend API, SQL execution payload, persistence, export, audit, or raw SQL auto-formatting semantics changed; standalone scripts/check-history-page-contract.mjs still reflects pre-HARN-107 router token assumptions and is left for HARN-116 governance-script hardening.
  - Next step: Start HARN-111 parse workbench and parse statistics page refactor using the same shared layout and SQL UI contracts.

### HARN-109: 重构 Dashboard、Delivery、Runtime、Recovery 概览类页面

- Status: done
- Completed at: 2026-05-09
- Commit subject: `refactor(frontend): HARN-109 unify overview pages`
- Priority: 2
- Depends on: HARN-108
- Scope: 覆盖 `DashboardView`、`DeliveryProgressView`、`RuntimeGatesView`、`RecoveryDrillView`；统一 KPI、风险、活动流、静态证据区布局，保留 sample/window/session/PULL_ONLY 边界，不伪造全局事实。
- Validation:
  - `npm run lint`
  - `npm run build`
  - `npm run smoke:frontend-dev`
- Progress log:
  - 2026-05-09: started HARN-109 overview page refactor after strict preflight; scope remains Dashboard, Delivery, Runtime, and Recovery overview layout only.
- Context closeout:
  - Completed scope: Refactored Dashboard, DeliveryProgressView, RuntimeGatesView, and RecoveryDrillView around shared PageHero, SectionHeader, EvidencePanel, MetricCard, risk/activity/static-evidence patterns while preserving sample/window/session/PULL_ONLY boundaries and avoiding backend/API/persistence changes.
  - Validation evidence: python3 scripts/foreman.py validate HARN-109 --include-task-audit --extra-command 'npm run smoke:frontend-dev' passed; direct npm run lint, npm run build, npm run test:frontend-page-governance, and npm run smoke:frontend-dev also passed.
  - Residual risk: No backend, API, persistence, permission, or production delivery-progress exposure changes; remaining HARN-110 through HARN-116 page refactors still need to continue from this shared overview pattern.
  - Next step: Start HARN-110 SQL query and SQL history page refactor using the shared layout and SQL UI contracts.

### HARN-108: 建立前端共享页面布局组件与样式契约

- Status: done
- Completed at: 2026-05-09
- Commit subject: `refactor(frontend): HARN-108 add shared page layout components`
- Priority: 1
- Depends on: HARN-107
- Scope: 为所有页面抽取共享 `PageHero`、`SectionHeader`、`EvidencePanel`、`MetricCard`、toolbar/filter shell 等布局层；覆盖 `DashboardView`、静态运维页、common 组件，不引入新 UI 框架。
- Validation:
  - `npm run lint`
  - `npm run build`
  - `npm run test:frontend-page-governance`
- Progress log:
  - 2026-05-09: started shared frontend page layout component and style contract implementation after HARN-107 closeout.
- Context closeout:
  - Completed scope: Established shared PageHero, SectionHeader, EvidencePanel, MetricCard, and ToolbarShell components; wired DashboardView, RuntimeGatesView, RecoveryDrillView, and RoutePlaceholder; documented the layout contract and i18n keys without changing backend/API/persistence semantics.
  - Validation evidence: python3 scripts/foreman.py validate HARN-108 --include-task-audit passed with npm run lint, npm run build, npm run test:form-governance, npm run test:sql-ui-contract, npm run test:frontend-page-governance, and pre-closeout task audit.
  - Residual risk: Only initial Dashboard/static-ops/common surfaces are migrated; HARN-109 through HARN-115 still need to reuse these components page by page.
  - Next step: Start HARN-109 overview page refactor using the HARN-108 shared layout contract.

### HARN-107: 抽离 App 壳层导航与路由元数据

- Status: done
- Completed at: 2026-05-09
- Commit subject: `refactor(frontend): HARN-107 extract navigation shell metadata`
- Priority: 1
- Depends on: HARN-106
- Scope: 重构 `src/App.vue`、`src/router/index.js`、`src/config/routePaths.mjs` 的导航树、active key、breadcrumb、workspace header 与 delivery-progress 可见性逻辑；保持所有 path、legacy redirect、菜单可达性不变。
- Validation:
  - `node scripts/check-navigation-shell-contract.mjs`
  - `npm run lint`
  - `npm run build`
  - `npm run test:frontend-page-governance`
  - `npm run smoke:frontend-dev`
- Context closeout:
  - Completed scope: Extracted App shell navigation tree, route metadata, active menu key, breadcrumb, workspace header inputs, legacy redirects, and delivery-progress visibility into routePaths-driven configuration while keeping existing paths and menu reachability unchanged.
  - Validation evidence: python3 scripts/foreman.py validate HARN-107 --include-task-audit with node scripts/check-navigation-shell-contract.mjs, npm run lint, npm run build, npm run test:frontend-page-governance, npm run smoke:frontend-dev, and git diff --check passed.
  - Residual risk: No backend, persistence, API payload, or production delivery-progress exposure change; HARN-108 remains the next shared layout extraction task.
  - Next step: Start HARN-108 shared page layout component and style contract work.

### HARN-106: 全量前端页面重构任务落账与治理基线固化

- Status: done
- Completed at: 2026-05-09
- Commit subject: `docs(frontend): HARN-106 land refactor backlog`
- Priority: 1
- Depends on: HARN-096
- Scope: 将本轮全量页面扫描结论正式写入任务台账和必要文档，明确后续页面重构小任务、执行顺序、验证门禁和非实现边界；本任务只落账，不改页面。
- Validation:
  - `python3 scripts/foreman.py validate HARN-106`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-05-09: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-09: remapped the prior plan's placeholder `HARN-097` through `HARN-107` IDs to `HARN-106` through `HARN-116` because `HARN-097` through `HARN-105` already exist in completed history.
- Context closeout:
  - Completed scope: Remapped the prior placeholder IDs to HARN-106 through HARN-116, added the full frontend page refactor backlog to tasks.md, mirrored it into the Phase-E master plan and both task matrices, and recorded the shared design/truth baseline without changing page implementation.
  - Validation evidence: python3 scripts/foreman.py validate HARN-106 --include-task-audit --extra-command git diff --check passed after regenerating governance policy; node scripts/lint-repository-knowledge.js, python3 scripts/validate_codex_runtime.py, python3 scripts/foreman.py compile-governance --check, python3 scripts/task_audit.py --check --phase pre-closeout, and git diff --check passed.
  - Residual risk: No page implementation was changed in HARN-106; the actual refactors remain pending in HARN-107 through HARN-116, and HARN-096 is still the active ParseRecordView fix that should be respected before parse-record refactor work.
  - Next step: Finish or coordinate around HARN-096, then start HARN-107 for the App shell and route metadata extraction before the shared layout and page-specific refactor tasks.

### HARN-098: 固化 JDK 8u112 强制运行时要求

- Status: done
- Completed at: 2026-05-09
- Commit subject: `docs(runtime): HARN-098 pin JDK 8u112 requirement`
- Priority: 1
- Depends on: N/A
- Scope: 将人类要求的 JDK 8u112 写入 SQLForge 长期强制要求，更新 Java 运行时规则、技术栈/部署入口与人类约束历史，不改业务代码。
- Validation:
  - `python3 scripts/foreman.py validate HARN-098`
- Progress log:
  - 2026-05-09: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added JDK 8u112 as the mandatory Java runtime baseline across AGENTS, rules, validation rules, architecture/README/deployment docs, CI workflows, human constraint history, task spec aliases, and regenerated governance authority map.
  - Validation evidence: node scripts/lint-repository-knowledge.js; git diff --check; python3 scripts/foreman.py compile-governance --check; python3 scripts/foreman.py validate HARN-098
  - Residual risk: Local shell currently reports OpenJDK 1.8.0_482, so no local Java build/test was claimed as JDK 8u112-compliant; GitHub workflows now install and verify Zulu JDK 8u112 before Java gates.
  - Next step: Apply R-185 to future Java build, deployment, CI, and test-environment changes; raise INBOX/blocker if any target environment cannot provision JDK 8u112.

### HARN-105: Support AES-256 crypto fallback on JDK 8u112

- Status: done
- Completed at: 2026-05-09
- Commit subject: `fix(security): HARN-105 support AES fallback`
- Priority: 1
- Depends on: HARN-102
- Scope: Add a BouncyCastle lightweight AES-256-GCM fallback for SQLForge sensitive data encryption when the runtime JCE policy cannot initialize 256-bit AES/GCM, preserving existing ciphertext envelope and byte payload formats for governance query history and sensitive config persistence.
- Validation:
  - `python3 scripts/foreman.py validate HARN-105`
- Progress log:
  - 2026-05-09: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added BouncyCastle lightweight AES-256-GCM fallback for sensitive-data encryption when JCE cannot initialize 256-bit AES/GCM, kept existing envelope and byte ciphertext formats, and covered default plus forced fallback encryption/decryption paths.
  - Validation evidence: python3 scripts/foreman.py validate HARN-105 with shared crypto and governance query-history persistence Maven tests; git diff --check; pre-closeout task audit.
  - Residual risk: Actual JDK 8u112 runtime still needs target-machine restart verification, but fallback behavior is covered by a forced lightweight-provider unit test and governance persistence tests.
  - Next step: Deploy the updated shared dependency, restart governance on the JDK 8u112 machine, and retry /api/governance/internal/query-execution-history/write with the configured 32-byte base64 crypto key.

### OPS-SCHEMA-DRIFT-REPAIR-20260509: Repair local schema drift for parse history

- Status: done
- Completed at: 2026-05-09
- Commit subject: `chore(local): repair schema drift for parse history`
- Priority: 1
- Depends on: N/A
- Scope: Apply local MySQL schema additions for sql_parse_history, datasource_config, and related compatibility migrations, then restart backend services and verify parse-history queries.
- Validation:
  - `python3 scripts/foreman.py validate OPS-SCHEMA-DRIFT-REPAIR-20260509`
- Progress log:
  - 2026-05-09: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Applied local MySQL schema additions from sql/init-schema.sql and migrations V20260508_001, V20260509_001, V20260508_004, plus V20260508_003 for optimization_task.task_context_json; restarted backend services.
  - Validation evidence: python3 scripts/foreman.py validate OPS-SCHEMA-DRIFT-REPAIR-20260509 --include-task-audit passed with health-check, schema presence checks, and parse-history readback; structure parse smoke returned historyPersisted=true and sql_parse_history readback returned totalCount >= 1.
  - Residual risk: The repair updates the current local MySQL volume only; other developers or reset volumes must apply the same schema scripts or rerun local-start against their own databases.
  - Next step: Continue using http://localhost:3000/ and rerun parser workflows; new parse requests now persist to sql_parse_history.

### OPS-DIST-PORTABLE-REFRESH-20260509-HARN104: Refresh portable bundle after HARN-104

- Status: done
- Completed at: 2026-05-09
- Commit subject: `chore(frontend): refresh HARN-104 portable bundle`
- Priority: 1
- Depends on: N/A
- Scope: Rebuild tracked dist-portable from current source after HARN-104, restart local backend services and frontend dev server, and verify health/reachability.
- Validation:
  - `python3 scripts/foreman.py validate OPS-DIST-PORTABLE-REFRESH-20260509-HARN104`
- Progress log:
  - 2026-05-09: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Rebuilt tracked dist-portable from the current HARN-104 frontend source and restarted local backend services plus the frontend dev server.
  - Validation evidence: python3 scripts/foreman.py validate OPS-DIST-PORTABLE-REFRESH-20260509-HARN104 --include-task-audit with npm run build:portable, npm run smoke:portable-frontend, bash scripts/health-check.sh --fail-on-error, and git diff --check passed; python3 scripts/task_audit.py --check --phase pre-closeout passed.
  - Residual risk: Only generated portable frontend assets and local runtime process state changed in this task; no source behavior was edited here.
  - Next step: Use http://localhost:3000/ with backend health endpoints on 8080-8083 for local verification.

### HARN-104: 修复 SQL 历史与解析历史页面展示

- Status: done
- Completed at: 2026-05-09
- Commit subject: `fix(history): HARN-104 clean history pages`
- Priority: 1
- Depends on: N/A
- Scope: 修复 SQL 输入输出错位、清理 SQL 历史与解析历史无效说明和状态条、将请求租户融入 SQL 历史记录行，并修复解析历史查询旧表结构导致的 10000 报错；覆盖前端契约、迁移兼容与验证。
- Validation:
  - `python3 scripts/foreman.py validate HARN-104`
- Progress log:
  - 2026-05-09: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 修复共享 SqlCodeBlock 模板空白导致的 SQL 输出首行错位；移除 SQL 历史和解析历史页顶部无效说明；删除 SQL 历史执行记录表上方运行状态条；将请求租户融入 SQL 历史记录行；补齐治理 query-history summary tenantId；新增 sql_parse_history 旧表兼容迁移并加固前端/迁移契约检查。
  - Validation evidence: node scripts/check-sql-ui-contract.mjs; node scripts/check-history-page-contract.mjs; node scripts/check-history-detail-contract.mjs; npm run lint; npm run build; mvn -pl governance -Dtest=GovernanceQueryHistoryControllerTest test; mvn -pl sql-optimization -Dtest=StructureParseControllerTest,ParseBatchControllerTest,ReportBatchControllerTest,SqlParseHistoryPersistenceSchemaMappingTest test; python3 scripts/foreman.py validate HARN-104; git diff --check; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 未连接目标环境 MySQL 做 live migration smoke；兼容迁移通过 schema mapping 和聚焦控制器/前端契约验证覆盖。
  - Next step: 目标环境部署后执行一次解析历史查询页面 smoke，确认旧 sql_parse_history 表已补齐字段且不再返回 10000。

### OPS-DIST-PORTABLE-REFRESH-20260509: Refresh tracked dist-portable package and restart local services

- Status: done
- Completed at: 2026-05-09
- Commit subject: `chore(frontend): refresh deterministic portable bundle`
- Priority: 1
- Depends on: N/A
- Scope: Rebuild the tracked dist-portable frontend package from the latest source without missing generated assets, verify it has no stale differences versus the portable build output, then restart local backend and frontend services and verify reachability.
- Validation:
  - `python3 scripts/foreman.py validate OPS-DIST-PORTABLE-REFRESH-20260509`
- Progress log:
  - 2026-05-09: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Regenerated the tracked dist-portable package from the latest frontend source, removed stale hashed portable assets, and made portable mode default-disable the temporary delivery-progress route so the package no longer embeds volatile task ledger and validation-log snapshots. Restarted the four backend services and the Vite frontend dev server after the refresh.
  - Validation evidence: python3 scripts/foreman.py validate OPS-DIST-PORTABLE-REFRESH-20260509 --include-task-audit --extra-command 'npm run build:portable' --extra-command 'npm run smoke:portable-frontend' --extra-command 'npm run build' --extra-command 'npm run lint' --extra-command 'bash scripts/health-check.sh --fail-on-error' --extra-command 'git diff --check'; repeated dist-portable sha256 comparison after consecutive builds; portable asset reference check
  - Residual risk: Portable builds now omit the temporary delivery-progress route by default to keep copied portable packages deterministic; developers can still explicitly set VITE_ENABLE_DELIVERY_PROGRESS=true for local diagnostic builds if they need that route.
  - Next step: No follow-up required for the refreshed portable package; use /tmp/sqlforge-backend-runtime/*.pid and /tmp/sqlforge-frontend-runtime.pid to stop the restarted local services when needed.

### HARN-103: 保留历史原始 SQL 展示并统一提示交互

- Status: done
- Completed at: 2026-05-08
- Commit subject: `fix(history): HARN-103 preserve raw SQL display`
- Priority: 1
- Depends on: N/A
- Scope: Fix parse history and SQL execution history raw SQL surfaces so Original SQL display and copy preserve the submitted SQL text exactly for new records, including comments, whitespace, and semicolons; keep execution normalization and non-raw SQL formatting unchanged; unify issue-scene question-mark help to a single hover tooltip without duplicate native title popups; add focused backend and frontend contract checks.
- Validation:
  - `python3 scripts/foreman.py validate HARN-103`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Preserved submitted SQL text for new query-execution history writes, disabled SqlCodeBlock auto formatting for Original SQL surfaces in parse and SQL history detail views, removed duplicate native issue-scene title tooltips, and documented/guarded the raw SQL display contract.
  - Validation evidence: npm run test:sql-ui-contract; mvn -pl query-execution -am -Dtest=QueryExecutionApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test; npm run test:frontend-page-governance; npm run lint; npm run build; node scripts/check-history-page-contract.mjs && node scripts/check-history-detail-contract.mjs && node scripts/check-batch-import-contract.mjs; git diff --check; python3 scripts/foreman.py validate HARN-103; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Existing history rows that were already persisted with normalized SQL are not backfilled because removed whitespace or semicolon characters cannot be reliably reconstructed from persisted evidence.
  - Next step: No follow-up required for new records; if historical backfill becomes mandatory, shape a separate data-repair task with a recoverable source-of-truth inventory.

### HARN-102: 接通 SQL 历史数据库持久化链路

- Status: done
- Completed at: 2026-05-08
- Commit subject: `feat(history): HARN-102 persist SQL execution history`
- Priority: 1
- Depends on: N/A
- Scope: 默认使用 MySQL sql_parse_history 持久化解析历史；由 query-execution 调用 governance 内部执行历史写入接口，让 governance 事务内写入 execution_result、query_history 与审计记录；补齐相关客户端、服务和测试验证。
- Validation:
  - `python3 scripts/foreman.py validate HARN-102`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-08: wired SQL parse history to database-by-default configuration and added governance-owned query execution history write path from query-execution evidence into execution_result/query_history/audit_log.
- Context closeout:
  - Completed scope: Defaulted SQL parse history to MySQL sql_parse_history without runtime in-memory fallback; added governance-owned query execution history write API and query-execution client evidence path; updated docs and focused tests.
  - Validation evidence: mvn -pl sqlforge-shared,governance,query-execution,sql-optimization -am -DskipTests compile; mvn -pl query-execution -am -Dtest=QueryExecutionApplicationServiceTest,GovernanceHttpClientTest -Dsurefire.failIfNoSpecifiedTests=false test; mvn -pl governance -am -Dtest=GovernanceQueryExecutionHistoryApplicationServiceTest,GovernanceCapabilityApplicationServiceTest,AuthWebMvcTest -Dsurefire.failIfNoSpecifiedTests=false test; mvn -pl sql-optimization -am -Dtest=SqlParseHistoryRepositorySelectionTest,StructureParseControllerTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate HARN-102; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: No live MySQL restart smoke was run in this environment; repository-side compile and focused persistence/client tests passed.
  - Next step: Deploy with MySQL-backed service configuration and run one live SQL execution plus one parse-history restart smoke in the target environment.

### HARN-101: 收敛 SQL 历史列表架构与分页运行态验证

- Status: done
- Completed at: 2026-05-08
- Commit subject: `refactor(frontend): harden sql history list architecture`
- Priority: 1
- Depends on: N/A
- Scope: 按复核方案修正 SQL 历史历史列表主路径：下沉列表状态和分页归一化，修复搜索提交与状态隔离，补齐受控租户/数据源候选和分页运行态 smoke/contract，不改变后端 API、路由或详情/导出核心契约。
- Validation:
  - `python3 scripts/foreman.py validate HARN-101`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 收敛 SQL 历史列表主路径为 useSqlHistoryList 数据状态层，修复分页组件注册、分页切换、pageSize 重置、键盘提交搜索、错误状态分层与受控租户/数据源候选。
  - Validation evidence: npm run lint; npm run build; npm run test:form-governance; npm run test:sql-ui-contract; npm run test:frontend-page-governance; node scripts/check-history-page-contract.mjs; node scripts/check-history-detail-contract.mjs; npm run smoke:frontend-dev; python3 scripts/foreman.py validate HARN-101; python3 scripts/task_audit.py --check --phase pre-closeout.
  - Residual risk: 详情抽屉、导出弹窗、审计关联和后端 API 仅做兼容保留，未在本任务做结构性重构。
  - Next step: 产品验收 SQL 历史默认查询、分页跳转、pageSize 切换、筛选提交、清空条件、空/错误/加载态与 History ID 详情打开流程。

### HARN-100: 修复 SQL 历史列表分页切换体验

- Status: done
- Completed at: 2026-05-08
- Commit subject: `fix(frontend): restore sql history pagination controls`
- Priority: 1
- Depends on: N/A
- Scope: 修复 SQL 历史历史列表底部分页只显示分页信息、缺少明确页码切换/跳转体验的问题；保持 HARN-099 数据驱动结构、接口参数语义和现有 contract 标识不变，补齐分页状态展示与前端治理验证。
- Validation:
  - `python3 scripts/foreman.py validate HARN-100`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restored explicit SQL history list pagination controls by adding Element Plus current-page/page-size v-model bindings, visible page window status, jumper navigation, loading disablement, total fallback handling, and history page contract markers for the pagination control.
  - Validation evidence: python3 scripts/foreman.py validate HARN-100; npm run lint; npm run build; npm run test:form-governance; npm run test:sql-ui-contract; npm run test:frontend-page-governance; node scripts/check-history-page-contract.mjs; node scripts/check-history-detail-contract.mjs; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Playwright runtime check was attempted but this environment lacks the Playwright Chromium binary; validation is through build, static governance, contract scripts, and foreman gates.
  - Next step: Product acceptance should verify the SQL history list with more than one page of QUERY_EXECUTION results: page buttons, next/previous, page-size changes, and jumper navigation should all reload the list.

### HARN-099: 重构 SQL 历史历史列表前端主路径

- Status: done
- Completed at: 2026-05-08
- Commit subject: `refactor(frontend): restructure sql history list`
- Priority: 1
- Depends on: N/A
- Scope: 按 R-177 至 R-184 重构 SQL 历史页历史列表主体，覆盖筛选、摘要状态、操作按钮、执行记录表格、分页与 loading/error/empty 状态，并保留现有详情抽屉、SQL 展示、审计关联、导出与 contract 标识兼容。
- Validation:
  - `python3 scripts/foreman.py validate HARN-099`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Refactored the SQL history list main path into searchForm/pageInfo state, data-driven search field and table column configs, token-based management-page layout, explicit loading/error/empty/footer states, and sqlHistory i18n while preserving existing detail drawer, SQL code block, lookup, export, route, and contract markers.
  - Validation evidence: python3 scripts/foreman.py validate HARN-099; npm run lint; npm run build; npm run test:form-governance; npm run test:sql-ui-contract; npm run test:frontend-page-governance; node scripts/check-history-page-contract.mjs; node scripts/check-history-detail-contract.mjs; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Runtime API behavior was not exercised against a live backend in this turn; coverage is static contract, build, and governance validation.
  - Next step: Product acceptance should exercise default QUERY_EXECUTION list loading, search reset, clear filters, pagination, indexed lookup, detail drawer, and export result in a running environment.

### OPS-005: Restart local frontend and backend services after dependency refresh

- Status: done
- Completed at: 2026-05-08
- Commit subject: `OPS-005 Restart local frontend and backend services after dependency refresh`
- Priority: 1
- Depends on: N/A
- Scope: Restart the local SQLForge frontend and backend runtime services, including backend runtime dependency refresh when needed, and verify health endpoints; no source changes.
- Validation:
  - `python3 scripts/foreman.py validate OPS-005`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restarted local backend Spring Boot services and frontend Vite dev server; refreshed backend runtime dependencies after stale shared dependency caused the first governance startup attempt to fail; verified MySQL, Redis, MinIO, message queue, backend health endpoints, and frontend URL.
  - Validation evidence: bash scripts/health-check.sh --fail-on-error; npm run test:frontend-page-governance; python3 scripts/foreman.py validate OPS-005; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Working tree still contains active frontend task changes outside this OPS closeout; they were not staged into the restart closeout commit.
  - Next step: Continue the active HARN frontend work separately; use /tmp/sqlforge-backend-runtime/*.pid and /tmp/sqlforge-frontend-runtime/frontend.pid to stop this local runtime if needed.

### HARN-097: 补齐前端页面规则自动化门禁

- Status: done
- Completed at: 2026-05-08
- Commit subject: `chore(frontend): HARN-097 enforce page governance gates`
- Priority: 1
- Depends on: N/A
- Scope: Add automated enforcement for R-177 through R-184: create a frontend page governance contract script, make foreman validate automatically run frontend lint/build/form-governance/sql-ui/page-governance checks when tracked diffs touch frontend view/component/locale files, and map R-177 through R-184 plus the new validation command into frontend task matrices.
- Validation:
  - `python3 scripts/foreman.py validate HARN-097`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added a diff-aware frontend page governance checker, exposed it through npm, wired foreman validate to automatically run frontend lint/build/form-governance/sql-ui/page-governance checks when frontend view/component/locale files change, mapped R-177 through R-184 into the frontend task default overlay, updated rule/history documentation, and regenerated the compiled authority map.
  - Validation evidence: npm run test:frontend-page-governance; python3 -m py_compile scripts/foreman.py; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance; python3 scripts/foreman.py validate HARN-097; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check
  - Residual risk: The new gate is diff-aware and enforces changed frontend surfaces; existing frontend debt is not retroactively failed unless those files are touched. During implementation it correctly flagged the separate HARN-096 ParseRecordView diff, which was temporarily stashed only for HARN-097 closeout and will be restored.
  - Next step: Future frontend page tasks should rely on foreman validate to auto-run the frontend governance gates, and touched management pages must satisfy R-177 through R-184.

### HARN-095: 固化前端页面工程规则

- Status: done
- Completed at: 2026-05-08
- Commit subject: `docs(frontend): HARN-095 codify management page rules`
- Priority: 1
- Depends on: N/A
- Scope: Docs-only governance task to codify frontend page implementation rules: Vue 3 + Element Plus + self-built components, management-page layout defaults, no main-function card stacking, data-driven reusable SearchForm/EditDialog/dictionary/pagination/progress patterns, backend-authoritative permission boundary, API/state layering, i18n, and validation gates in codex rules, frontend design system, and form component governance.
- Validation:
  - `python3 scripts/foreman.py validate HARN-095`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Codified frontend management-page engineering rules as long-term governance: appended R-177 through R-184, expanded the frontend design system with SearchForm/table/pagination/EditDialog/useDict/progress/tab/API-state patterns, clarified form-component governance boundaries, recorded the human constraint history, updated repository knowledge lint to check through R-184, and regenerated the compiled authority map.
  - Validation evidence: node scripts/lint-repository-knowledge.js; git diff --check; python3 scripts/foreman.py compile-governance; python3 scripts/foreman.py validate HARN-095; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: No runtime risk; this task changes governance documents and generated policy metadata only.
  - Next step: Apply R-177 through R-184 when implementing or refactoring future frontend management pages.

### HARN-094: Wire Hetu JDBC driver into page-managed EXPLAIN path

- Status: done
- Completed at: 2026-05-08
- Commit subject: `fix(governance): HARN-094 wire Hetu JDBC driver`
- Priority: 1
- Depends on: HARN-093
- Scope: Add Hetu JDBC runtime dependency to governance and sql-optimization so page-managed Hetu JDBC test-connection and *_WITH_PLAN EXPLAIN can load the configured driver class in real deployments; validate targeted frontend/backend contracts without changing closed HARN-093 ledger evidence.
- Validation:
  - `python3 scripts/foreman.py validate HARN-094`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added the managed Hetu JDBC runtime dependency to governance and sql-optimization and added classpath regression tests so page-managed JDBC test-connection and *_WITH_PLAN EXPLAIN can load io.prestosql.jdbc.PrestoDriver in real deployments.
  - Validation evidence: mvn -pl governance -am -Dtest=DatasourceConfigApplicationServiceTest,DatasourceConfigControllerTest,GovernanceCapabilityApplicationServiceTest,AuthWebMvcTest,HetuJdbcDriverClasspathTest -Dsurefire.failIfNoSpecifiedTests=false test; mvn -pl sql-optimization -am -Dtest=StructureParseControllerTest,ParseBatchApplicationServiceTest,ReportBatchApplicationServiceTest,GovernanceHttpClientTest,JdbcHetuPlanAnalysisClientTest,HetuJdbcDriverClasspathTest -Dsurefire.failIfNoSpecifiedTests=false test; node scripts/check-system-datasource-contract.mjs; node scripts/check-parse-workbench-contract.mjs; node scripts/check-sql-ui-contract.mjs; git diff --check; python3 scripts/foreman.py validate HARN-094; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Live external Hetu/MRS smoke validation still requires environment credentials and remains tracked by HARN-016/INBOX-002.
  - Next step: Run the HARN-016 external Hetu/MRS smoke once the Win10 Hetu/MRS environment is available.

### HARN-093: 页面配置 Hetu JDBC 并用于 EXPLAIN 实测

- Status: done
- Completed at: 2026-05-08
- Commit subject: `feat(governance): HARN-093 configure Hetu JDBC explain`
- Priority: 1
- Depends on: D-TASK-069,HARN-088
- Scope: 在系统管理数据源与接口中支持 Hetu JDBC 配置、脱敏与真实测试连接；治理服务持久化并加密保存 JDBC 凭证，提供受保护内部 resolve 接口；sql-optimization 在 *_WITH_PLAN 解析模式下优先解析页面配置执行真实 EXPLAIN，并保留本地配置 fallback；补齐前端与契约/模块测试。
- Validation:
  - `python3 scripts/foreman.py validate HARN-093`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented page-managed Hetu JDBC datasource configuration with encrypted credential storage, real JDBC connection probing, protected internal JDBC resolve, and sql-optimization *_WITH_PLAN EXPLAIN using governance config with local fallback; updated system/parse workbench UI contracts, DDL, persistence docs, and tests.
  - Validation evidence: python3 scripts/foreman.py validate HARN-093; python3 scripts/task_audit.py --check --phase pre-closeout; git diff --check; npm run lint; npm run build; node scripts/check-system-datasource-contract.mjs; node scripts/check-parse-workbench-contract.mjs; node scripts/check-sql-ui-contract.mjs; mvn -pl governance -am -Dtest=DatasourceConfigApplicationServiceTest,DatasourceConfigControllerTest,GovernanceCapabilityApplicationServiceTest,AuthWebMvcTest -Dsurefire.failIfNoSpecifiedTests=false test; mvn -pl sql-optimization -am -Dtest=StructureParseControllerTest,ParseBatchApplicationServiceTest,ReportBatchApplicationServiceTest,GovernanceHttpClientTest,JdbcHetuPlanAnalysisClientTest -Dsurefire.failIfNoSpecifiedTests=false test
  - Residual risk: Live external Hetu/MRS credential and network validation remains tracked by blocked HARN-016/INBOX-002; repository-side behavior is covered with mock and controlled JDBC tests.
  - Next step: When the Win10 Hetu/MRS environment is available, deploy the configured services and execute the HARN-016 environment smoke to archive live JDBC evidence.

### HARN-092: SQL static parse and editor alignment fixes

- Status: done
- Completed at: 2026-05-08
- Commit subject: `fix(sql-optimization): close HARN-092 static parse diagnostics`
- Priority: 1
- Depends on: N/A
- Scope: Fix static SQL diagnostic noise handling, conservative LARGE_JOIN_PAIR_RISK copy, and SqlEditorField highlight/input alignment without public API changes.
- Validation:
  - `python3 scripts/foreman.py validate HARN-092`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 修复静态结构解析 fallback/diagnostic scan 对 @@@@ 与中文噪声的处理，保守化 LARGE_JOIN_PAIR_RISK issue/checklist 文案，并修复 SqlEditorField 高亮层首行偏移与混排对齐。
  - Validation evidence: mvn -B -pl sql-optimization -am test -DskipITs; node scripts/check-sql-ui-contract.mjs; npm run build; python3 scripts/foreman.py validate HARN-092; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: None.
  - Next step: No immediate follow-up.

### HARN-091: 补齐 SQL 结构解析风险分析

- Status: done
- Completed at: 2026-05-08
- Commit subject: `feat(sql-optimization): HARN-091 expand structure risk signals`
- Priority: 1
- Depends on: N/A
- Scope: Extend sql-optimization structure parsing with additional static risk signals for order/group/aggregation/string/repeated-subquery complexity, expose compatible API/frontend fields, keep real execution metrics bounded to access parse/benchmark evidence, and validate backend/frontend contracts.
- Validation:
  - `python3 scripts/foreman.py validate HARN-091`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Expanded structure parsing static profile fields for order/group/aggregation/string/repeated-subquery complexity, emitted the corresponding static-only risk checklist/issues/evidence, preserved real execution metrics for access parse/benchmark plan evidence, and surfaced the new fields/risks in frontend contracts and documentation.
  - Validation evidence: python3 scripts/foreman.py preflight; mvn -pl sql-optimization -Dtest=SqlOptimizationPipelineServiceTest,StructureParseContractTest,StructureParseControllerTest test; node scripts/check-parse-workbench-contract.mjs; node scripts/check-history-page-contract.mjs; node scripts/check-history-detail-contract.mjs; git diff --check; mvn -pl sql-optimization test; npm run lint; npm run build; python3 scripts/foreman.py validate HARN-091; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Static risk signals remain conservative heuristics; real scan volume, returned bytes, row counts, execution time, and plan-node cost still require access parse or benchmark evidence from a configured datasource.
  - Next step: Add datasource-backed calibration in a separate task if production plan metrics expose additional row, byte, or cost signals for these static risk classes.

### HARN-090: Invalid SQL fallback structure parsing

- Status: done
- Completed at: 2026-05-08
- Commit subject: `feat(sql-optimization): HARN-090 add invalid SQL fallback diagnostics`
- Priority: 1
- Depends on: N/A
- Scope: Return bounded best-effort structure diagnostics for incomplete, syntactically invalid, and overlong SQL across structure parse, combined parse, batch SQL parsing, and report import parsing without changing request contracts.
- Validation:
  - `python3 scripts/foreman.py validate HARN-090`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added bounded heuristic structure diagnostics for invalid and overlong SQL across structure parse, combined parse, batch SQL parsing, and report import parsing, including SQL_TOO_LONG issue/help metadata and static contract coverage.
  - Validation evidence: mvn -pl sql-optimization -Dtest=StructureParseControllerTest,ParseBatchControllerTest,ReportBatchControllerTest,ReportBatchApplicationServiceTest test; mvn -pl sql-optimization -Dtest=StructureParseContractTest,ParseBatchApplicationServiceTest,ReportBatchParseStatisticsAssemblerTest,StructureParseResultTest,StructureParsePriorityScorerTest,ParseBatchPersistenceSchemaMappingTest test; mvn -pl sql-optimization -Dtest=StructureParseControllerTest test; node scripts/check-parse-workbench-contract.mjs; node scripts/check-batch-import-contract.mjs; node scripts/check-history-detail-contract.mjs; node scripts/check-history-page-contract.mjs; git diff --check; python3 scripts/foreman.py validate HARN-090; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Heuristic fallback evidence is intentionally best-effort and overlong SQL diagnostics scan only a bounded prefix to keep parser responses and persisted summaries readable.
  - Next step: Monitor invalid SQL samples for additional object-token forms and extend the heuristic patterns under a follow-up task if production evidence requires it.

### HARN-089: Auto-create rewrite recommendations from parse issues

- Status: done
- Completed at: 2026-05-08
- Commit subject: `feat(sql-optimization): HARN-089 auto rewrite recommendations`
- Priority: 1
- Depends on: N/A
- Scope: When SQL structure parsing from single parse, ordinary batch parse, or report batch parse produces one of OR_PREDICATE_INDEX_RISK, SELECT_STAR, NESTED_SUBQUERY_RISK, or LEADING_WILDCARD_LIKE_RISK with a valid parsed structure, automatically submit an async REWRITE task and persist the worker result as an acceleration_recommendation with parse source traceability fields and idempotency protection.
- Validation:
  - `python3 scripts/foreman.py validate HARN-089`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented parse-triggered REWRITE task submission for single structure parse, parse batch, and report batch history writes; added task source-context persistence; persisted successful parse-triggered rewrite worker output into acceleration_recommendation with original/recommended SQL and parse trace fields; updated contracts, schema, migration, docs, and regression tests.
  - Validation evidence: mvn -pl sql-optimization -Dtest=ParseTriggeredRewriteRecommendationServiceTest,OptimizationTaskWorkerTest,StructureParseControllerTest,ParseBatchApplicationServiceTest,ReportBatchApplicationServiceTest,OptimizationTaskPersistenceSchemaMappingTest,ParseBatchPersistenceSchemaMappingTest test; mvn -pl sql-optimization test; node scripts/check-history-detail-contract.mjs; node scripts/check-batch-import-contract.mjs; node scripts/lint-repository-knowledge.js; git diff --check; python3 scripts/foreman.py validate HARN-089; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Automatic rewrite recommendations are conservative and do not infer SELECT_STAR column expansion without metadata; no live external datasource rewrite validation was executed in this repo-closed task.
  - Next step: Use the recommendation center to review generated REWRITE records and approve only candidates that have been validated against representative data.

### OPS-004: Restart local sql-optimization service

- Status: done
- Completed at: 2026-05-08
- Commit subject: `OPS-004 Restart local sql-optimization service`
- Priority: 1
- Depends on: N/A
- Scope: Restart only the local SQLForge sql-optimization runtime service and verify its health endpoint; no source changes.
- Validation:
  - `python3 scripts/foreman.py validate OPS-004`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restarted only the local sql-optimization Spring Boot runtime service on port 8082 and verified the service health endpoint while leaving frontend and other backend services running.
  - Validation evidence: python3 scripts/foreman.py validate OPS-004; bash scripts/health-check.sh --fail-on-error; curl readiness for http://localhost:8082/actuator/health
  - Residual risk: None for local single-service runtime restart; service is a session process with pid file under /tmp/sqlforge-backend-runtime.
  - Next step: Use /tmp/sqlforge-backend-runtime/sql-optimization.pid to stop this local service if needed.

### HARN-088: Dual-channel SQL parse and Hetu plan analysis

- Status: done
- Completed at: 2026-05-08
- Commit subject: `feat(sql-optimization): HARN-088 add dual-channel parse plan analysis`
- Priority: 1
- Depends on: N/A
- Scope: Add four parser modes where _WITH_PLAN combines local structural parsing with read-only Hetu EXPLAIN plan analysis, propagating combined status and plan evidence through single parse, parse batch, report batch, history summaries, frontend displays, contract checks, tests, and validation.
- Validation:
  - `python3 scripts/foreman.py validate HARN-088`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented four parser modes, server-side Hetu EXPLAIN plan analysis, combined structure/plan statuses, history persistence payloads, item-level batch/report plan summaries and statistics, frontend detail displays, schema migration, contract checks, and focused/full regression tests.
  - Validation evidence: python3 scripts/foreman.py validate HARN-088; mvn -pl sql-optimization test; mvn -pl sql-optimization -Dtest=StructureParseControllerTest,ParseBatchApplicationServiceTest,ParseBatchControllerTest,ReportBatchApplicationServiceTest,ReportBatchControllerTest test; mvn -pl sql-optimization -Dtest=ParseBatchPersistenceSchemaMappingTest,SqlParseHistoryPersistenceSchemaMappingTest test; node scripts/check-parse-workbench-contract.mjs; node scripts/check-batch-import-contract.mjs; npm run lint; npm run build; node scripts/lint-repository-knowledge.js; git diff --check; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Live external Hetu/MRS JDBC evidence was not executed in this workspace because no target credentials/window were available; HARN-016 and INBOX-002 now include HARN-088 as the environment-backed validation chain.
  - Next step: Run a target-environment smoke with configured sql-optimization.hetu-plan.datasources.<code> credentials and archive the real EXPLAIN evidence under HARN-016/INBOX-002 when the Hetu/MRS validation window opens.

### OPS-003: Restart local frontend and backend services third run

- Status: done
- Completed at: 2026-05-08
- Commit subject: `OPS-003 Restart local frontend and backend services third run`
- Priority: 1
- Depends on: N/A
- Scope: Restart the local SQLForge frontend and backend runtime services for this session and verify health endpoints; no source changes.
- Validation:
  - `python3 scripts/foreman.py validate OPS-003`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restarted local backend Spring Boot services and frontend Vite dev server for this session; verified MySQL, Redis, MinIO, message queue, backend health endpoints, and frontend URL.
  - Validation evidence: python3 scripts/foreman.py validate OPS-003; bash scripts/health-check.sh --fail-on-error
  - Residual risk: None for local runtime restart; services are session processes with pid files under /tmp.
  - Next step: Use /tmp/sqlforge-backend-runtime/*.pid and /tmp/sqlforge-frontend-runtime/frontend.pid to stop this local runtime if needed.

### HARN-087: Resolve live database view definitions during structure parse

- Status: done
- Completed at: 2026-05-08
- Commit subject: `feat(sql-optimization): HARN-087 expand live database views`
- Priority: 1
- Depends on: N/A
- Scope: In sql-optimization, add live datasource view metadata resolution and recursive DB view expansion during structure parsing; preserve governance DB view catalog as fallback evidence only; persist expanded logical object hits and table keys across single, batch, report, and history flows.
- Validation:
  - `python3 scripts/foreman.py validate HARN-087`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added sql-optimization live datasource view metadata resolution with JDBC read-only metadata queries, recursive DB view definition expansion with depth/cycle protection, governance DB view catalog fallback labeling, final TABLE key extraction for history/batch/report records, and interface/data-model documentation for the new DB view expansion contract.
  - Validation evidence: mvn -pl sql-optimization test; python3 scripts/foreman.py validate HARN-087 --extra-command "mvn -pl sql-optimization test" --extra-command "node scripts/lint-repository-knowledge.js" --extra-command "git diff --check"; node scripts/lint-repository-knowledge.js; git diff --check
  - Residual risk: Live metadata expansion depends on configured read-only datasource metadata credentials and permissions; environments without those settings degrade to unresolved evidence or governance DB view catalog fallback.
  - Next step: Configure sql-optimization.view-metadata.datasources for real Hetu/Trino test datasources when environment-backed DB view definition validation is available.

### HARN-086: Fix simple GROUP BY intent risk false positives

- Status: done
- Completed at: 2026-05-08
- Commit subject: `fix(sql-optimization): HARN-086 reduce simple group-by intent risk`
- Priority: 1
- Depends on: N/A
- Scope: sql-optimization structure parse intent risk classification
- Validation:
  - `python3 scripts/foreman.py validate HARN-086`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Capped aggregate and group-by contribution in structure parse intent and complexity scoring so filtered, limited single-table grouped reports classify below HEAVY/COMPLEX while complex anti-pattern SQL still emits EXTREME/HEAVY graph risk.
  - Validation evidence: `mvn -pl sql-optimization -Dtest=SqlOptimizationPipelineServiceTest,StructureParseControllerTest test`; `git diff --check`; `python3 scripts/foreman.py validate HARN-086 --extra-command ...`; `python3 scripts/task_audit.py --check --phase pre-closeout`.
  - Residual risk: Heuristics remain static parser estimates; genuinely large aggregation-only reports still rely on capped aggregate-shape thresholds until metadata-backed costing exists.
  - Next step: Monitor future aggregation-heavy parser fixtures for cases that need metadata-backed cost signals.

### OPS-002: Restart local frontend and backend services again

- Status: done
- Completed at: 2026-05-08
- Commit subject: `OPS-002 Restart local frontend and backend services again`
- Priority: 1
- Depends on: N/A
- Scope: Restart the local SQLForge frontend and backend runtime services again and verify health endpoints; no source changes.
- Validation:
  - `python3 scripts/foreman.py validate OPS-002`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restarted local backend Spring Boot services and frontend Vite dev server again; verified MySQL, Redis, MinIO, message queue, backend health endpoints, and frontend URL.
  - Validation evidence: python3 scripts/foreman.py validate OPS-002; bash scripts/health-check.sh --fail-on-error
  - Residual risk: None for local runtime restart; services are session processes with pid files under /tmp.
  - Next step: Use /tmp/sqlforge-backend-runtime/*.pid and /tmp/sqlforge-frontend-runtime/frontend.pid to stop this local runtime if needed.

### HARN-085: Fix repeated scan/expression false positives

- Status: done
- Completed at: 2026-05-08
- Commit subject: `fix(sql-optimization): HARN-085 correct repeated risk detection`
- Priority: 1
- Depends on: N/A
- Scope: Separate discovered table inventory from real table-scan frequency in SQL structure parsing; exclude simple column references from repeated-expression risk; add regression coverage for table-prefixed column names without changing frontend fields, persistence schema, or access-parse semantics.
- Validation:
  - `python3 scripts/foreman.py validate HARN-085`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Separated legacy parser table discovery from real table-scan frequency, renamed the internal scan-frequency tracking, skipped simple column/identifier references in repeated-expression counting, and added pipeline/controller regressions for orders-prefixed column names while preserving real repeated-scan anti-pattern detection.
  - Validation evidence: mvn -pl sql-optimization -Dtest=SqlOptimizationPipelineServiceTest,StructureParseControllerTest test; python3 scripts/foreman.py validate HARN-085 --extra-command 'mvn -pl sql-optimization -Dtest=SqlOptimizationPipelineServiceTest,StructureParseControllerTest test' --extra-command 'git diff --check'; git diff --check
  - Residual risk: Repo-closed coverage targets structure parsing behavior; no frontend contract fields, persistence schema, or access-parse semantics were changed.
  - Next step: Monitor future parser fixtures for dialect-specific cases where CTE aliases or engine-specific identifiers need stronger physical-table classification.

### OPS-001: Restart local frontend and backend services

- Status: done
- Completed at: 2026-05-08
- Commit subject: `OPS-001 Restart local frontend and backend services`
- Priority: 1
- Depends on: N/A
- Scope: Restart the local SQLForge frontend and backend runtime services and verify health endpoints; no source changes.
- Validation:
  - `python3 scripts/foreman.py validate OPS-001`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restarted local backend Spring Boot services and frontend Vite dev server; verified MySQL, Redis, MinIO, message queue, backend health endpoints, and frontend URL.
  - Validation evidence: python3 scripts/foreman.py validate OPS-001; bash scripts/health-check.sh --fail-on-error
  - Residual risk: None for local runtime restart; services are session processes with pid files under /tmp.
  - Next step: Use /tmp/sqlforge-backend-runtime/*.pid and /tmp/sqlforge-frontend-runtime/frontend.pid to stop this local runtime if needed.

### HARN-084: Decouple SQL parse history from SQL execution history

- Status: done
- Completed at: 2026-05-08
- Commit subject: `HARN-084 decouple SQL parse history`
- Priority: 1
- Depends on: HARN-083
- Scope: Implement the confirmed SQL history decoupling plan: keep governance query_history limited to SQL execution records, add sql-optimization owned sql_parse_history persistence and parse-history APIs, write parse flows and end-of-day slow-SQL parse skeleton to sql_parse_history, update frontend clients/views/contracts, synchronize schema migrations and architecture docs, and validate backend/frontend/data boundaries.
- Validation:
  - `python3 scripts/foreman.py validate HARN-084`
- Progress log:
  - 2026-05-08: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-08: implemented SQL parse-history decoupling, removed governance parse-history write surface, updated frontend/runtime contracts, docs, schema, migration, and portable bundle.
  - 2026-05-08: validation passed for shared/governance/sql-optimization module tests, frontend contracts/smokes/builds, and `python3 scripts/foreman.py validate HARN-084`.
- Context closeout:
  - Completed scope: Implemented SQL parse-history decoupling: sql-optimization owned sql_parse_history persistence/API, local parse-flow writes, end-of-day slow-SQL skeleton, frontend API/page split, governance parse write surface removal, schema/migration/docs/contracts/portable bundle updates.
  - Validation evidence: mvn -pl sqlforge-shared test; mvn -pl governance test; mvn -pl sql-optimization test; node scripts/check-history-page-contract.mjs; node scripts/check-history-detail-contract.mjs; node scripts/check-sql-ui-contract.mjs; node scripts/check-dev-frontend.mjs; npm run lint; npm run build; npm run build:portable; node scripts/check-portable-frontend.mjs; python3 scripts/foreman.py validate HARN-084; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Existing legacy SQL_PARSE rows in query_history are not migrated; real cron scheduling and production slow-SQL source connector remain out of scope.
  - Next step: Create a separate governed task if historical migration, scheduler wiring, or a production slow-SQL source connector is required.

### HARN-083: 修复 SQL 执行历史与解析历史边界

- Status: done
- Completed at: 2026-05-07
- Commit subject: `fix(fullstack): HARN-083 isolate SQL history boundaries`
- Priority: 1
- Depends on: HARN-082
- Scope: 拆分 SQL 执行历史与解析历史边界；为 governance query-history 增加 historyType 白名单过滤；SQL 历史页固定查询 QUERY_EXECUTION，解析历史页固定查询 SQL_PARSE 或解析专用历史；补齐前后端契约与验证。
- Validation:
  - `python3 scripts/foreman.py validate HARN-083`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added whitelisted historyType filtering to governance query-history; split SQL execution history into an independent QUERY_EXECUTION frontend route; constrained parse-record and parse surfaces to SQL_PARSE; updated contract/dev-smoke checks for both boundaries.
  - Validation evidence: python3 scripts/foreman.py validate HARN-083 --include-task-audit --extra-command history contracts --extra-command smoke/build/lint/backend tests; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Repo-closed validation covers API/controller/service/mapper contracts and frontend route/request boundaries; live production data shape still depends on existing persisted history_type values.
  - Next step: Product acceptance should verify SQL history shows only QUERY_EXECUTION and parse-record shows SQL_PARSE plus batch/report histories against a populated environment.

### HARN-082: 修复 SQL 历史入口指向

- Status: done
- Completed at: 2026-05-07
- Commit subject: `fix(frontend): HARN-082 split SQL history route`
- Priority: 1
- Depends on: N/A
- Scope: 修复 SQL 历史列表入口与解析历史查询入口共用同一路由导致默认页签互相覆盖的问题；新增独立 sqlHistory route path，调整路由 meta/default tab 与相关前端跳转，不改后端 query-history/query-execution API。
- Validation:
  - `python3 scripts/foreman.py validate HARN-082`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增独立 ROUTE_PATHS.sqlHistory 与 SqlHistory 路由，ParseRecord 路由默认进入批量解析/报表导入历史；SQL 历史导航、Dashboard、推荐中心、路由治理、取证与解析 historyId 深链改向 SQL history；ParseRecordView 根据 route meta/query 同步默认页签并保留 historyId 直开 SQL 详情；同步前端 smoke/contract 检查以覆盖新路由和当前 UI 契约。
  - Validation evidence: python3 scripts/foreman.py validate HARN-082 --include-task-audit --extra-command 'node scripts/check-history-page-contract.mjs' --extra-command 'node scripts/check-history-detail-contract.mjs' --extra-command 'node scripts/check-query-workbench-contract.mjs' --extra-command 'node scripts/check-sql-ui-contract.mjs' --extra-command 'npm run smoke:frontend-dev' --extra-command 'mvn -pl query-execution -Dtest=QueryExecutionControllerTest test' --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'git diff --check'
  - Residual risk: 未改后端 query-history 与 query-execution API；验证为 repo-closed 静态契约、dev browser smoke、前端 lint/build 与 query-execution controller test，未在真实外部环境手工点击验收。
  - Next step: 产品验收时分别从 SQL 历史 > 历史列表、解析与加速 > 解析历史查询、Dashboard/推荐/路由/取证跳转进入，确认默认页签和 historyId 深链符合预期。

### HARN-081: 解析历史与批次列表分页及解析详情展示修复

- Status: done
- Completed at: 2026-05-07
- Commit subject: `feat(frontend): paginate parse histories and show details in dialog`
- Priority: 1
- Depends on: N/A
- Scope: 为解析历史列表、批量批次列表、报表导入历史列表补齐合理分页查询与前端分页控件；SQL级解析详情加载改为弹窗展示；修复解析结果展示中英文重复的问题；不改核心 parser 算法、权限边界或历史保留策略。
- Validation:
  - `python3 scripts/foreman.py validate HARN-081`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 解析历史接口返回 total/pageCount；批量解析批次与报表导入批次接口支持 pageNo/pageSize 并返回分页 envelope；解析记录与批量中心页面增加分页控件；报表 SQL 解析详情改为弹窗加载；解析详情展示按当前语言过滤中英文重复行；portable smoke mock 与打包产物同步。
  - Validation evidence: python3 scripts/foreman.py validate HARN-081; python3 scripts/task_audit.py --check --phase pre-closeout; mvn -pl governance -Dtest=GovernanceQueryHistoryControllerTest,AuthWebMvcTest test; mvn -pl sql-optimization -Dtest=ParseBatchControllerTest,ReportBatchControllerTest test; npm run lint; npm run build; npm run build:portable; npm run smoke:portable-frontend; git diff --check.
  - Residual risk: 批量批次列表当前在服务层完成租户过滤后的分页，适合现有内存持久化/本地批次存储模型；若批次数量显著增长，应在后续任务下沉到数据库级 count/page 查询。
  - Next step: 产品验收时使用真实大批次和报表导入历史确认分页总数、页数、页大小切换和解析详情弹窗交互。

### CALCITE-001-VERIFY: Verify Apache Calcite parser mode closeout

- Status: done
- Completed at: 2026-05-07
- Commit subject: `test(sql-optimization): stabilize Calcite combined parser test`
- Priority: 1
- Depends on: CALCITE-001
- Scope: Verify whether CALCITE-001 truly implemented request-level Apache Calcite parser mode across backend, batch persistence, frontend contracts, task audit, and tests without changing product code.
- Validation:
  - `python3 scripts/foreman.py validate CALCITE-001-VERIFY`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Verified CALCITE-001 closeout evidence and implementation across parser selection, batch persistence, frontend contracts, and validation; fixed the combined parser controller test to wait for asynchronous access-parse completion so full sql-optimization reactor tests pass reliably.
  - Validation evidence: python3 scripts/foreman.py preflight; python3 scripts/foreman.py validate CALCITE-001; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/task_audit.py --check --phase post-closeout; node scripts/check-parse-workbench-contract.mjs; node scripts/check-batch-import-contract.mjs; mvn -pl sql-optimization -Dtest=SqlOptimizationPipelineServiceTest,StructureParseControllerTest,ParseBatchApplicationServiceTest,ParseBatchControllerTest,ReportBatchApplicationServiceTest,ReportBatchControllerTest,ParseBatchPersistenceSchemaMappingTest test; npm run lint; npm run build; mvn -pl sql-optimization -Dtest=StructureParseControllerTest test; mvn -pl sql-optimization -am clean test; python3 scripts/foreman.py validate CALCITE-001-VERIFY; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: No product-code residual risk found in repo-closed verification; Calcite parser scope remains structure/profile extraction only, with rewrite candidates legacy-parser-only by design.
  - Next step: Monitor Calcite dialect coverage through parser tests when adding new SQL syntax cases.

### CALCITE-001: Add Apache Calcite parser mode

- Status: done
- Completed at: 2026-05-07
- Commit subject: `feat(sql-optimization): add Apache Calcite parser mode`
- Priority: 1
- Depends on: N/A
- Scope: Add request-level SQL parser selection for legacy parser or Apache Calcite across structure, combined, parse batch, and report batch flows, with persistence, UI controls, validation, and tests.
- Validation:
  - `python3 scripts/foreman.py validate CALCITE-001`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added request-level parserMode selection for legacy parser and Apache Calcite across structure, combined, parse batch, and report batch flows, including persistence, migration, UI controls, contracts, and tests.
  - Validation evidence: python3 scripts/foreman.py validate CALCITE-001; python3 scripts/task_audit.py --check --phase pre-closeout; mvn -pl sql-optimization -Dtest=SqlOptimizationPipelineServiceTest,StructureParseControllerTest,ParseBatchApplicationServiceTest,ParseBatchControllerTest,ReportBatchApplicationServiceTest,ReportBatchControllerTest,ParseBatchPersistenceSchemaMappingTest test; mvn -pl sql-optimization -am clean test; node scripts/check-parse-workbench-contract.mjs; node scripts/check-batch-import-contract.mjs; npm run lint; npm run build.
  - Residual risk: Apache Calcite support is limited to structure/profile extraction; SQL rewrite candidates remain legacy-parser-only by design.
  - Next step: Monitor Calcite dialect coverage and add neutral rewrite signals only when they do not require legacy parser AST mutation.

### OPS-RESTART-20260507-3: Restart local frontend and backend on demand

- Status: done
- Completed at: 2026-05-07
- Commit subject: `OPS-RESTART-20260507-3 restart local frontend and backend`
- Priority: P2
- Depends on: N/A
- Scope: Restart SQLForge local backend services and frontend dev server on user request, then validate backend health endpoints and frontend availability.
- Validation:
  - `python3 scripts/foreman.py validate OPS-RESTART-20260507-3`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restarted local backend services and the Vite frontend dev server while reusing the existing healthy local Docker infrastructure.
  - Validation evidence: python3 scripts/foreman.py validate OPS-RESTART-20260507-3 with backend health endpoints and frontend HTTP 200 checks.
  - Residual risk: Local runtime remains dependent on this workstation's detached processes and Docker container state.
  - Next step: Use .codex/state/backend-runtime/*.pid and .codex/state/runtime-logs/frontend.pid when stopping these local services.

### HARN-080: 修复解析历史持久化并统一批量解析核心逻辑

- Status: done
- Completed at: 2026-05-07
- Commit subject: `fix(sql-optimization): persist batch parse history`
- Priority: 1
- Depends on: N/A
- Scope: 修复单条 SQL 解析与批量解析结果未稳定写入治理历史的问题，确保批量解析复用单条 SQL 结构解析的完整、准确、深度逻辑；补充历史写入、批量 item 追溯字段与相关回归测试，不改无关治理/runtime 流程。
- Validation:
  - `python3 scripts/foreman.py validate HARN-080`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Fixed SQL parse history write orchestration for single, parse-batch, combined, and report-batch parse paths; added parse_batch_item history trace fields and schema migration.
  - Validation evidence: python3 scripts/foreman.py validate HARN-080; python3 scripts/task_audit.py --check --phase pre-closeout; mvn -pl sql-optimization -Dtest=StructureParseControllerTest,ParseBatchControllerTest,ReportBatchControllerTest,ParseBatchPersistenceSchemaMappingTest test; mvn -pl sql-optimization -Dtest=AccessParseControllerTest,ReportBatchApplicationServiceTest test; mvn -pl sql-optimization test; git diff --check.
  - Residual risk: External deployed governance database verification was not run in this repository turn; coverage is repo-closed tests and migration/schema assertions.
  - Next step: Use deployed environment smoke to confirm query_history rows for real governance service calls when environment-backed validation is available.

### OPS-RESTART-20260507-2: Restart local frontend and backend again

- Status: done
- Completed at: 2026-05-07
- Commit subject: `OPS-RESTART-20260507-2 restart local frontend and backend`
- Priority: P2
- Depends on: N/A
- Scope: Restart SQLForge local backend services and frontend dev server on user request, then validate health endpoints and frontend availability.
- Validation:
  - `python3 scripts/foreman.py validate OPS-RESTART-20260507-2`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restarted local backend services and the Vite frontend dev server while reusing the existing healthy local Docker infrastructure.
  - Validation evidence: python3 scripts/foreman.py validate OPS-RESTART-20260507-2 with backend health endpoints and frontend HTTP 200 checks.
  - Residual risk: Local runtime remains dependent on this workstation's detached processes and Docker container state.
  - Next step: Use .codex/state/backend-runtime/*.pid and .codex/state/runtime-logs/frontend.pid when stopping these local services.

### UI-TASK-002: Converge parse history and report import IA

- Status: done
- Completed at: 2026-05-07
- Commit subject: `fix(frontend): UI-TASK-002 converge parse history IA`
- Priority: 1
- Depends on: N/A
- Scope: Refine ParseRecordView and ParseBatchCenterView information architecture per confirmed plan: add top-level parse history workbench tabs, move report issue-scene detail into report-level statistics issue-scene tab, remove duplicate overview tabs from report batch SQL detail/statistics dialogs, preserve backend APIs, DTOs, parser behavior, statistics semantics, permissions, and persistence schema, and update frontend contract checks.
- Validation:
  - `python3 scripts/foreman.py validate UI-TASK-002`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added top-level SQL-history vs batch/report-history tabs in ParseRecordView, moved report issue-scene detail into the report-level statistics issue-scene tab, removed duplicate overview tabs from whole-report SQL detail and report statistics dialogs in ParseBatchCenterView, and updated static frontend contracts without changing backend APIs, parser behavior, DTOs, permissions, statistics semantics, or persistence schema.
  - Validation evidence: python3 scripts/foreman.py validate UI-TASK-002 --include-task-audit --extra-command 'node scripts/check-history-page-contract.mjs' --extra-command 'node scripts/check-history-detail-contract.mjs' --extra-command 'node scripts/check-batch-import-contract.mjs' --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'git diff --check'; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Manual browser or portable smoke was not run in this turn; coverage is repo-closed through static contracts, lint, production build, and task audit.
  - Next step: Use a real report-import batch during product acceptance to click the history workbench tabs, SQL list detail, and inline issue-scene drill-down.

### HARN-079: Analyze merge candidates for multi-SQL reports

- Status: done
- Completed at: 2026-05-07
- Commit subject: `feat(sql-optimization): flag report sql merge candidates`
- Priority: 1
- Depends on: N/A
- Scope: Add conservative report-batch parse-statistics analysis for same-report multi-SQL merge candidates, expose compatible summary fields, and cover the behavior with backend tests without changing persistence schema or generating executable merged SQL.
- Validation:
  - `python3 scripts/foreman.py validate HARN-079`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-07: implemented conservative `REPORT_SQL_MERGE_CANDIDATE` analysis for report batch parse statistics, exposed compatible API/UI fields, and documented the non-executable merge suggestion boundary.
  - 2026-05-07: validation passed via targeted report-batch tests, full `sql-optimization` tests, frontend lint/build, batch/history contract checks, and `git diff --check`.
- Context closeout:
  - Completed scope: Added conservative report-batch merge-candidate analysis for same-report multi-SQL groups, exposed compatible merge-candidate API fields, showed the hint in report batch and parse-history report statistics, documented the non-executable recommendation boundary, and covered the behavior with backend/controller tests plus frontend contract checks.
  - Validation evidence: python3 scripts/foreman.py validate HARN-079 --include-task-audit --extra-command 'mvn -pl sql-optimization -Dtest=ReportBatchParseStatisticsAssemblerTest,ReportBatchControllerTest test' --extra-command 'mvn -pl sql-optimization test' --extra-command 'node scripts/check-batch-import-contract.mjs' --extra-command 'node scripts/check-history-detail-contract.mjs' --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'git diff --check'; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The merge-candidate signal is a conservative static governance hint only; it does not prove semantic equivalence and does not generate executable merged SQL.
  - Next step: When real report samples are available, review reported merge candidates with report owners before promoting any manual SQL consolidation.

### OPS-DIST-PORTABLE-DETAIL-20260507: 修复 dist-portable 解析详情加载 404

- Status: done
- Completed at: 2026-05-07
- Commit subject: `fix(frontend): harden portable parse detail loading`
- Priority: 1
- Depends on: N/A
- Scope: Analyze dist-portable generated frontend behavior, fix portable parse-detail 404 and refresh generated dist-portable package without changing parser or backend data contracts.
- Validation:
  - `python3 scripts/foreman.py validate OPS-DIST-PORTABLE-DETAIL-20260507`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Updated ParseRecordView so SQL-list parse detail loading only calls governance query-history for persisted historyId values, displays report-batch SQL evidence when governance history is missing or unavailable, refreshes dist-portable generated assets, and extends portable smoke plus history contract checks for the parse-detail path.
  - Validation evidence: python3 scripts/foreman.py validate OPS-DIST-PORTABLE-DETAIL-20260507 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'npm run build:portable' --extra-command 'npm run smoke:portable-frontend' --extra-command 'node scripts/check-history-page-contract.mjs' --extra-command 'node scripts/check-history-detail-contract.mjs' --extra-command 'git diff --check'; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The portable package still depends on correct backend base URLs and live governance data for full query-history detail; when governance history is absent, the UI now intentionally falls back to report-batch SQL evidence rather than fabricating a history URL.
  - Next step: Deploy the refreshed dist-portable package with the target portable-config.json backend URLs and verify the SQL list detail path against the real environment.

### OPS-DIST-ROOT-REVERT-20260507: Revert mistaken root dist commit

- Status: done
- Completed at: 2026-05-07
- Commit subject: `revert(frontend): remove mistaken root dist commit`
- Priority: 1
- Depends on: N/A
- Scope: Revert the mistaken root dist commit 7f9e3f6 without destructive git reset, restore the repository to the prior tracked-dist state, and validate audit/reachability after the rollback.
- Validation:
  - `python3 scripts/foreman.py validate OPS-DIST-ROOT-REVERT-20260507`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reverted the mistaken 7f9e3f6 root dist commit with git revert --no-commit, removed the tracked root dist files again, restored the repository to the prior ignored-dist state, and switched the local frontend on port 3000 back from dist preview to Vite dev server.
  - Validation evidence: python3 scripts/foreman.py validate OPS-DIST-ROOT-REVERT-20260507 --include-task-audit --extra-command git-diff-cached-check --extra-command git-diff-check --extra-command root-dist-index-absent --extra-command frontend-curl --extra-command governance-health
  - Residual risk: The earlier mistaken commit remains in git history but is neutralized by this explicit revert commit; root dist remains ignored and should not be force-added again unless intentionally requested.
  - Next step: Continue using the tracked dist-portable package for committed frontend packaging, or open a separate task before changing root dist tracking policy.

### OPS-DIST-REFRESH-20260507: Refresh frontend dist from latest source

- Status: done
- Completed at: 2026-05-07
- Commit subject: `chore(frontend): refresh packaged dist assets`
- Priority: 1
- Depends on: N/A
- Scope: Rebuild and persist the frontend distribution artifacts from the latest frontend source, verify the generated dist output changes as expected, and restart local frontend/backend services if needed for validation.
- Validation:
  - `python3 scripts/foreman.py validate OPS-DIST-REFRESH-20260507`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Rebuilt the root ignored dist artifact locally and regenerated the tracked dist-portable package from the latest Vue frontend source so packaged assets use the current hashed bundles.
  - Validation evidence: python3 scripts/foreman.py validate OPS-DIST-REFRESH-20260507 --include-task-audit --extra-command npm-run-build --extra-command npm-run-build-portable --extra-command check-query-workbench-contract --extra-command git-diff-check; frontend/backend reachability checks returned HTTP 200 / UP.
  - Residual risk: Root dist/ is intentionally ignored by git and remains a local-only build output; dist-portable is the committed package. npm run smoke:portable-frontend was attempted and failed on a stale query-flow-status assertion that is no longer part of the current query page contract.
  - Next step: Use dist-portable/start-portable.sh or start-portable.cmd for the portable package; update the portable smoke selector contract in a separate task if browser-smoke coverage must be restored.

### OPS-RESTART-20260507: Refresh frontend dist and restart local services

- Status: done
- Completed at: 2026-05-07
- Commit subject: `OPS-RESTART-20260507 refresh dist and restart local services`
- Priority: 1
- Depends on: N/A
- Scope: Rebuild the root frontend dist artifact, then restart the local frontend dev server and backend runtime services using repository standard scripts and verify local reachability.
- Validation:
  - `python3 scripts/foreman.py validate OPS-RESTART-20260507`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Rebuilt root frontend dist with npm run build, stopped stale frontend/backend runtime listeners, restarted governance/query-execution/sql-optimization/benchmark-engine using scripts/start-backend-services.sh with the existing local stack, and restarted the Vite frontend dev server on port 3000.
  - Validation evidence: npm run build; python3 scripts/foreman.py validate OPS-RESTART-20260507; curl health checks for 8080/8081/8082/8083 and frontend HTTP 200; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Local runtime remains workstation-stateful; services depend on the current docker-compose infrastructure and detached process state under .codex/state/.
  - Next step: Use .codex/state/backend-runtime/*.pid and .codex/state/runtime-logs/frontend.pid to stop these services when no longer needed.

### UI-TASK-001: Converge batch parse center input cards

- Status: done
- Completed at: 2026-05-07
- Commit subject: `fix(frontend): UI-TASK-001 converge batch parse inputs`
- Priority: 1
- Depends on: N/A
- Scope: Remove first-screen batch parse/report import input cards from ParseBatchCenterView, move user input guidance to existing create/import/template dialogs, keep API payloads unchanged, and validate frontend build/lint.
- Validation:
  - `python3 scripts/foreman.py validate UI-TASK-001`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-07: removed first-screen parse/report input rail cards from `src/views/parse-batch/ParseBatchCenterView.vue`, kept create/import/template dialogs as the input surfaces, and changed the current workbench layout to a single result column.
  - 2026-05-07: updated `scripts/check-batch-import-contract.mjs` so the page contract now forbids the removed input rail test ids and requires dialog-based input entry points.
  - 2026-05-07: validation passed via `python3 scripts/foreman.py validate UI-TASK-001 --extra-command "node scripts/check-batch-import-contract.mjs" --extra-command "npm run lint" --extra-command "npm run build" --extra-command "git diff --check"`.
- Context closeout:
  - Completed scope: Removed the first-screen parse and report import input rail cards from ParseBatchCenterView, converted the current workbench to a single result column, kept create/import/template responsibilities in dialogs, and updated the batch import contract check to require dialog input entry points while forbidding removed rail test ids.
  - Validation evidence: python3 scripts/foreman.py validate UI-TASK-001 --extra-command 'node scripts/check-batch-import-contract.mjs' --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'git diff --check'
  - Residual risk: Manual browser interaction against a live backend was not run in this turn; repo-closed frontend build/lint and contract checks passed.
  - Next step: None.

### HARN-078: Optimize parse page information architecture

- Status: done
- Completed at: 2026-05-07
- Commit subject: `feat(frontend): reorganize parse detail tabs`
- Priority: 1
- Depends on: N/A
- Scope: Move batch and report import template documentation into template dialogs, reorganize parse result/statistics/history/detail views into tabs, and update frontend contract checks without changing backend APIs, parser behavior, DTOs, or database schema.
- Validation:
  - `python3 scripts/foreman.py validate HARN-078`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 优化批量解析中心、报表导入、解析历史报表详情和解析统计详情的信息架构：模板说明移入按钮弹窗，解析结果/统计/报表 SQL 详情改为 tabs 分层，统计详情原始 JSON 收敛到弹窗内 tab；未修改后端 API、parser、DTO 或数据库 schema。
  - Validation evidence: python3 scripts/foreman.py validate HARN-078 --extra-command node scripts/check-batch-import-contract.mjs --extra-command node scripts/check-history-page-contract.mjs --extra-command node scripts/check-history-detail-contract.mjs --extra-command node scripts/check-statistics-page-contract.mjs --extra-command node scripts/check-parse-workbench-contract.mjs --extra-command npm run lint --extra-command npm run build --extra-command git diff --check; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 未运行真实浏览器人工点击验收；当前覆盖来自静态契约、lint、生产构建和源码级按钮顺序检查。
  - Next step: 产品验收时用包含普通批量失败 SQL、报表宽表、多报表分组和问题场景详情的样例批次逐项点击 tabs 与模板弹窗。

### HARN-077: 补齐解析详情问题场景中文提示

- Status: done
- Completed at: 2026-05-07
- Commit subject: `fix(frontend): complete issue scene tooltips`
- Priority: P1
- Depends on: HARN-076
- Scope: 补齐解析历史详情与报表导入详情中问题场景代码旁的中文问号提示，覆盖结构解析 risk checklist、issues、统计、SQL 明细和失败/问题 SQL 详情；复用共享 issueSceneHelp 文案，不改 parser、接口字段或数据库 schema。
- Validation:
  - `python3 scripts/foreman.py validate HARN-077`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 补齐解析历史详情与报表导入详情中问题场景代码旁的中文 ? 提示；共享 issueSceneHelp 文案按风险含义、原因、建议三段输出并覆盖 REPEATED_TABLE_SCAN_RISK 等结构解析场景；问题场景帮助改为可 hover/focus 的 inline icon，避免嵌套按钮吞掉 tooltip；SQL 清单、失败/问题 SQL、报表级统计、risk checklist 与 issues 列表均展示短问题代码和定位信息，完整 SQL 仍只保留在 SQL 输出区域。
  - Validation evidence: python3 scripts/foreman.py validate HARN-077 --extra-command node scripts/check-history-detail-contract.mjs --extra-command node scripts/check-history-page-contract.mjs --extra-command node scripts/check-batch-import-contract.mjs --extra-command node scripts/check-parse-workbench-contract.mjs --extra-command npm run lint --extra-command npm run build --extra-command git diff --check; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 未在真实浏览器中对生产规模报表批次逐项手动 hover/focus 验收；当前覆盖来自静态契约、lint 与生产构建。
  - Next step: 产品验收时在解析历史详情和报表导入详情中用包含 REPEATED_TABLE_SCAN_RISK、SQL_SYNTAX_INVALID 与其他问题场景的样例批次逐项悬停/聚焦 ?。

### OPS-RESTART-20260506: Restart local frontend and backend

- Status: done
- Completed at: 2026-05-07
- Commit subject: `OPS-RESTART-20260506 restart local frontend and backend`
- Priority: P2
- Depends on: N/A
- Scope: Restart SQLForge local backend services and frontend dev server
- Validation:
  - `python3 scripts/foreman.py validate OPS-RESTART-20260506`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restarted local Docker infrastructure, governance, query-execution, sql-optimization, benchmark-engine, and the Vite frontend dev server.
  - Validation evidence: python3 scripts/foreman.py validate OPS-RESTART-20260506 with backend health endpoints and frontend HTTP 200 checks.
  - Residual risk: Local development runtime depends on this workstation's Docker and detached process state.
  - Next step: Use .codex/state/backend-runtime/*.pid and .codex/state/runtime-logs/frontend.pid when stopping these local services.

### HARN-076: Align parse history and report import details

- Status: done
- Completed at: 2026-05-07
- Commit subject: `fix(sql-optimization): align report parse details`
- Priority: P1
- Depends on: HARN-075
- Scope: Align parse history detail and report import SQL details with single SQL comprehensive parsing: shared issue-scene help hints, remove full SQL from location/task summaries, and run report SQL structure parse plus Access parse plus history writeback without schema changes.
- Validation:
  - `python3 scripts/foreman.py validate HARN-076`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Aligned report import SQL parsing with the comprehensive single-SQL path by running structure parse, Access Parse, combined history writeback, and detail-safe status propagation; added shared issue-scene help text and removed source-line SQL from fallback location summaries.
  - Validation evidence: python3 scripts/foreman.py validate HARN-076; python3 scripts/task_audit.py --check --phase pre-closeout; mvn -pl sql-optimization -Dtest=ReportBatchApplicationServiceTest,ReportBatchControllerTest,ReportBatchParseStatisticsAssemblerTest test; mvn -pl sql-optimization test; node scripts/check-history-page-contract.mjs; node scripts/check-history-detail-contract.mjs; node scripts/check-batch-import-contract.mjs; node scripts/check-parse-workbench-contract.mjs; npm run lint; npm run build; git diff --check
  - Residual risk: Manual browser verification against a production-sized real report batch was not run in this turn.
  - Next step: During product acceptance, import a real report workbook/CSV and confirm tooltips, Access status, history drill-through, and SQL-only output sections in the UI.

### BUG-REPORT-SQL-HISTORY-DETAIL-20260507: Fix report SQL history detail loading

- Status: done
- Completed at: 2026-05-06
- Commit subject: `fix(sql-optimization): persist report SQL parse history`
- Priority: 1
- Depends on: N/A
- Scope: Persist and load per-SQL parse history for report import history details
- Validation:
  - `python3 scripts/foreman.py validate BUG-REPORT-SQL-HISTORY-DETAIL-20260507`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Persist report batch item history ids, expose them through report SQL detail APIs, and load report import history SQL details by real historyId with legacy fallback.
  - Validation evidence: mvn -pl sql-optimization -Dtest=ReportBatchApplicationServiceTest,ReportBatchControllerTest clean test; node scripts/check-batch-import-contract.mjs; node scripts/check-history-detail-contract.mjs; npm run build; bash scripts/verify-db-scripts.sh
  - Residual risk: Existing report batch items imported before this change may lack history_id; the frontend keeps a parseTaskId-derived fallback and displays unavailable history without blocking batch evidence.
  - Next step: Deploy DB migration V20260507_001 before relying on persisted report SQL history ids in database-worker mode.

### D-TASK-076: Accelerate report batch parsing and issue scene drilldown

- Status: done
- Completed at: 2026-05-06
- Commit subject: `feat(sql-optimization): accelerate report batch parsing`
- Priority: P1
- Depends on: N/A
- Scope: Implement async report batch structure parsing, batch persistence optimization, and issue-scene drilldown details for report import parse statistics.
- Validation:
  - `python3 scripts/foreman.py validate D-TASK-076`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented asynchronous structure-first report batch parsing, batch item bulk persistence, issue-scene drilldown APIs, and parse-record frontend detail inspection for affected reports/logical objects/SQL rows.
  - Validation evidence: mvn -pl sql-optimization -Dtest=ReportBatchApplicationServiceTest,ReportBatchControllerTest test; mvn -pl sql-optimization test; npm run lint; npm run build; npm run test:form-governance; python3 scripts/foreman.py validate D-TASK-076; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Application-local background report batch parsing is not restart-resumable; if the service restarts mid-batch, operators should trigger resolve-sqls again for the affected batch.
  - Next step: If production requires restart recovery, promote report batch parsing to the database-worker queue model in a follow-up task.

### HARN-075: Adjust report batch SQL detail pagination and issue location display

- Status: done
- Completed at: 2026-05-06
- Commit subject: `fix(sql-optimization): adjust report SQL detail pagination`
- Priority: P1
- Depends on: HARN-074
- Scope: Implement report batch SQL detail pagination and report filtering; keep full SQL only in dedicated SQL output; show short per-issue location snippets and help hints across report detail, SQL detail, and parse statistics without schema migration.
- Validation:
  - `python3 scripts/foreman.py validate HARN-075`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Report batch SQL detail and parse statistics now support pagination and report-code filtering; issue location payloads expose short per-issue SQL snippets; report SQL task/location UI avoids full SQL and source-line context while keeping full SQL in the SQL output block; help hints added across report detail and parse statistics.
  - Validation evidence: python3 scripts/foreman.py validate HARN-075; python3 scripts/task_audit.py --check --phase pre-closeout; mvn -pl sql-optimization -Dtest=ReportBatchApplicationServiceTest,ReportBatchControllerTest,ReportBatchParseStatisticsAssemblerTest test; mvn -pl sql-optimization test; node scripts/check-batch-import-contract.mjs; node scripts/check-history-page-contract.mjs; node scripts/check-sql-ui-contract.mjs; npm run lint; npm run build
  - Residual risk: Manual browser verification of long real report batches was not run in this turn.
  - Next step: Exercise the report batch detail dialogs with a production-sized batch in the UI when sample data is available.

### HARN-074: Fix batch report SQL detail scoping and diagnostics

- Status: done
- Completed at: 2026-05-06
- Commit subject: `fix(sql-optimization): scope batch report sql details`
- Priority: P1
- Depends on: HARN-073
- Scope: Fix report import per-report SQL detail scoping, add whole-batch SQL detail entry, and expose batch/report SQL parse diagnostics with location evidence.
- Validation:
  - `python3 scripts/foreman.py validate HARN-074`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added per-report report-batch SQL detail, a whole-batch SQL detail entry, structured parse diagnostic fields for batch/report SQL rows, richer structure failure reasons, frontend diagnostic rendering, and regression tests/contracts.
  - Validation evidence: python3 scripts/foreman.py validate HARN-074 --include-task-audit --extra-command 'node scripts/check-batch-import-contract.mjs' --extra-command 'mvn -pl sql-optimization test' --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'git diff --check'
  - Residual risk: Precise line/column/token/snippet diagnostics are available for parser failures; non-fatal issue detections are localized to the SQL row/report context and issue-scene codes.
  - Next step: Smoke a real report import workbook or CSV with multiple reports, invalid SQL, and anti-pattern SQL to confirm the UI separates single-report details from whole-batch SQL detail.

### HARN-073: Fix report import SQL extraction and diagnostics

- Status: done
- Completed at: 2026-05-06
- Commit subject: `fix(sql-optimization): extract report import sql bodies`
- Priority: P1
- Depends on: N/A
- Scope: Fix report-import SQL extraction for cells whose content starts with repeated -- comment or description fragments before the real SELECT/SQL body, so parsing uses the extracted SQL statement instead of treating the whole cell as comments. Preserve imported SQL evidence where compatible, improve parse failure diagnostics with location/snippet for report import problems, and add focused backend tests/docs without schema changes.
- Validation:
  - `python3 scripts/foreman.py validate HARN-073`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added report-import SQL extraction for same-line dash-comment preambles before SELECT/WITH, kept normal SQL comments on the existing structure parser path, enriched report-import structure parse failure reasons with compact line/column/token/snippet diagnostics, and documented the HARN-073 report-import exception and UI display expectation.
  - Validation evidence: python3 scripts/foreman.py validate HARN-073; mvn -pl sql-optimization -Dtest=ReportBatchApplicationServiceTest clean test; mvn -pl sql-optimization test; node scripts/check-batch-import-contract.mjs; node scripts/check-history-page-contract.mjs; mvn -pl sql-optimization validate pmd:pmd checkstyle:check; git diff --check; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The extraction is intentionally scoped to report-import cells that start with same-line -- description preambles and then contain SELECT/WITH on that same first line; other SQL file splitting and normal SQL line-comment behavior remain unchanged.
  - Next step: Use a real report import workbook/CSV containing '-- 说明 -- 说明 SELECT ...' cells and an invalid sample to confirm the UI shows extracted SQL plus reportCode/sqlColumnName/sourceFileLine diagnostics.

### HARN-072: Align batch SQL diagnostics with single SQL parse

- Status: done
- Completed at: 2026-05-06
- Commit subject: `fix(sql-optimization): align batch SQL diagnostics`
- Priority: P1
- Depends on: HARN-071
- Scope: Fix batch parse issue detection so each batch SQL uses the same diagnostics path as single SQL parsing for complex anti-pattern SQL, including nested scalar subqueries, correlated subqueries, NOT EXISTS, wildcard LIKE, OR predicates, function-wrapped predicates, and ORDER BY RAND diagnostics; keep existing batch/report import contracts and do not add schema changes.
- Validation:
  - `python3 scripts/foreman.py validate HARN-072`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Updated ordinary batch parsing and report batch parsing to retain the single-SQL structure parser's fine-grained issue codes in SQL-level batch details/statistics instead of collapsing multiple diagnostics into coarse issue scenes; added issue-code priority scoring support and complex anti-pattern SQL regressions for SQL-file batch and report wide-table batch paths.
  - Validation evidence: python3 scripts/foreman.py validate HARN-072; mvn -pl sql-optimization -Dtest=ParseBatchApplicationServiceTest,ReportBatchApplicationServiceTest,ReportBatchParseStatisticsAssemblerTest,ParseStatisticsApplicationServiceTest,StructureParseControllerTest,SqlOptimizationPipelineServiceTest test; node scripts/check-batch-import-contract.mjs; node scripts/check-history-page-contract.mjs; mvn -pl sql-optimization test; git diff --check
  - Residual risk: Existing persisted batch rows keep their historical coarse issue-scene summaries until re-parsed; this task intentionally avoided schema migration and only changes newly parsed batch/report SQL diagnostics.
  - Next step: Use the provided complex anti-pattern SQL in acceptance against both SQL解析 and 批量解析, then confirm the batch detail/statistics list includes the same issue codes such as SCALAR_SUBQUERY_IN_SELECT, OR_PREDICATE_INDEX_RISK and ORDER_BY_RANDOM_RISK.

### HARN-071: HARN-071 批量解析与报表导入大批量治理体验收口

- Status: done
- Completed at: 2026-05-06
- Commit subject: `feat(sql-optimization): harden large batch report parsing`
- Priority: 1
- Depends on: `HARN-061`,`HARN-062`,`HARN-063`,`HARN-064`,`HARN-066`,`HARN-070`
- Scope: Main Foreman must execute HARN-071 through SQLForge standard actions; implementation must first confirm current parser/import/UI/statistics contracts from repository evidence, then keep batch/report parse behavior aligned with single SQL parse diagnostics, improve UI display/whitespace/format/highlight and issue summary clarity, and preserve execution payload, persisted history, tenant/request/...
- Plan ref: docs/exec-plans/completed/HARN-071-full-auto-execution-plan.md
- Matrix context: Phase-D / Story `D-STORY-009` 批量解析与报表清单解析
- Human confirmation point: Resolved by active user objective on 2026-05-06: proceed as a standard single-agent Main Foreman task using HARN-071. If repository inspection proves schema migration, permission expansion, new import formats, external report-system integration, or task splitting is unavoidable, pause and request a separate human decision before implementing that expansion.
- Data impact: Expected data impact is low to moderate: imported SQL text may be trimmed at the outer cell/file boundary for parsing and display, and API/VO/UI summaries may gain compatible additive fields. No historical data migration, destructive update, or persisted SQL semantic rewrite is authorized by default.
- Rollback / recovery: Rollback by reverting the single HARN-071 task commit and restoring prior batch/report import parser, statistics, and frontend display behavior. If compatible additive API fields or docs are introduced, remove them with the same commit rollback. If implementation discovers unavoidable schema or data migration work, stop for confirmation and record a separate rollback path before proceeding.
- Validation:
  - `python3 scripts/foreman.py validate HARN-071、mvn -pl sql-optimization -Dtest=ReportBatchApplicationServiceTest,ReportBatchControllerTest,ReportBatchParseStatisticsAssemblerTest,SqlOptimizationPipelineServiceTest,StructureParseControllerTest test、node scripts/check-batch-import-contract.mjs、node scripts/check-history-page-contract.mjs、node scripts/check-history-detail-contract.mjs、node scripts/check-sql-ui-contract.mjs、npm run lint、npm run build`
  - `python3 scripts/foreman.py validate HARN-071`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Aligned batch SQL file splitting with parser boundaries, added backend preview metadata for large parse/report batches, capped report SQL statistics previews, improved ParseBatchCenterView summary-first large-batch display, and documented HARN-071 import/display/statistics limits.
  - Validation evidence: python3 scripts/foreman.py validate HARN-071; python3 scripts/task_audit.py --check --phase pre-closeout; mvn -pl sql-optimization -Djava.io.tmpdir=/models/project/codex/SQLForge/.tmp -Dtest=ReportBatchApplicationServiceTest,ReportBatchControllerTest,ReportBatchParseStatisticsAssemblerTest,SqlOptimizationPipelineServiceTest,StructureParseControllerTest,ParseBatchApplicationServiceTest clean test; node scripts/check-batch-import-contract.mjs; node scripts/check-history-page-contract.mjs; node scripts/check-history-detail-contract.mjs; node scripts/check-sql-ui-contract.mjs; npm run lint; npm run build; git diff --check
  - Residual risk: Repo-side 50M / 100k SQL handling is closed as complete statistics plus bounded previews over the existing contentBase64 import path; true streaming upload, external report-system integration, and production-scale acceptance remain out of scope.
  - Next step: No immediate follow-up required.

### HARN-070: HARN-070 / E-STORY-007 - SQL 输入输出展示与编辑体验增强执行模板

- Status: done
- Completed at: 2026-05-06
- Commit subject: `feat(frontend): add SQL copy format highlighting`
- Priority: 1
- Depends on: `N/A`
- Scope: Main Foreman must execute HARN-070 through SQLForge standard actions: preflight, materialize/instantiate, implement, validate, task_audit pre-closeout, closeout, task_audit post-closeout, and single-task single-commit discipline. Every confirmed SQL input surface must support copy plus a manual format action that mutates only the editable UI value. Every confirmed SQL output surface must suppor...
- Plan ref: docs/exec-plans/completed/HARN-070-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-007` SQL 查询与历史前端增强
- Human confirmation point: Resolved by user confirmation on 2026-05-06: materialize HARN-070 as a standard single-agent Main Foreman task bound to E-STORY-007 / Phase-E. Repository inspection will determine the final SQL input/output inventory, existing formatter/highlighter reuse, and concrete documentation entry points. Implementation must keep formatting/display separate from execution, query results, persisted history, and backend datasource semantics.
- Data impact: Expected data impact is low: changes target UI display/edit interactions and documentation. Potential risk exists if formatting is applied to persisted SQL, submitted SQL, query history records, or execution payloads; formal implementation must keep formatting/display separate from business semantics unless explicitly confirmed.
- Rollback / recovery: Rollback by reverting the single HARN-070 task commit if implemented as a normal task. Recovery should restore previous SQL input/output UI behavior, remove added formatter/highlighter dependencies or component wiring, and preserve task ledger/audit records according to SQLForge closeout rules. If a dependency is added, rollback must also remove lockfile/package changes tied only to this task.
- Validation:
  - `Inventory-backed coverage for each confirmed SQL input page copy behavior、Coverage for each confirmed SQL input page manual format behavior、Coverage for each confirmed SQL output page copy behavior、Coverage for automatic output SQL formatting at the confirmed render/receive boundary、Coverage or assertion for SQL syntax highlighting render state where practical、Coverage for long SQL readability behavior: height, width, scroll, wrap, max-height, or responsive constraints、Regression checks that formatting/display does not alter execution semantics or query results`
  - `python3 scripts/foreman.py validate HARN-070`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added shared SQL editor/display components with copy, manual input formatting, automatic output display formatting, SQL syntax highlighting, and readable sizing; wired them into query, parse, batch parse, parse history, recommendation, and benchmark SQL surfaces; added a static SQL UI contract check and documented the HARN-070 page inventory and display boundary.
  - Validation evidence: python3 scripts/foreman.py validate HARN-070 --extra-command npm-run-test-sql-ui-contract --extra-command npm-run-test-form-governance --extra-command npm-run-lint --extra-command npm-run-build; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: SQL formatting is intentionally lightweight and UI-scoped; report wide-table/CSV inputs keep formatting disabled to avoid mutating table structure; no live browser E2E was run against external data.
  - Next step: During product acceptance, spot-check long SQL copy/format/highlight behavior on /sql-query, SQL parse, batch parse, parse history, recommendation, and benchmark pages with production-like SQL samples.

### U-TASK-005: 更新前端 dist 产物

- Status: done
- Completed at: 2026-05-06
- Commit subject: `chore(frontend): refresh packaged dist assets`
- Priority: 1
- Depends on: U-TASK-004
- Scope: Regenerate frontend dist and dist-portable from the latest Vue source so packaged pages match current implementation; no source behavior changes.
- Validation:
  - `python3 scripts/foreman.py validate U-TASK-005`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Regenerated frontend dist and dist-portable from the latest Vue source so packaged pages include the current UI.
  - Validation evidence: python3 scripts/foreman.py validate U-TASK-005 --include-task-audit --extra-command "npm run build" --extra-command "npm run build:portable"; curl checks for dist preview root/dashboard returned 200; portable health returned UP.
  - Residual risk: dist/ is ignored by git and updated locally only; dist-portable is the committed portable package for cross-machine startup.
  - Next step: Use dist-portable/start-portable.sh or start-portable.cmd on another computer after editing portable-config.json for backend host addresses.

### HARN-066: HARN-066

- Status: done
- Completed at: 2026-05-06
- Commit subject: `feat(sql-optimization): add parse statistics tabs and SQL diagnostics`
- Priority: 1
- Depends on: N/A
- Scope: 
- Plan ref: docs/exec-plans/completed/HARN-066-full-auto-execution-plan.md
- Matrix context: Story `D-STORY-010` 解析统计与优先级分层
- Human confirmation point: Confirmed by user on 2026-05-06: materialize HARN-066 as a standard single-agent Main Foreman task bound to D-STORY-010 / Phase-D. Prefer existing SQL parse statistics semantics; document any difference. Use the current history permission model. Compatible additive API, field, cache, or UI-contract adaptations are allowed when confirmed by repository evidence. Failure position should prefer line/column and fall back to offset, token, or SQL snippet when parser precision is limited.
- Data impact: May affect parse-history read/display paths and statistics response shape. Use existing persisted parse evidence where possible. Compatible additive fields or cache keys are allowed when needed; avoid new schema unless repository evidence proves it necessary, and document compatibility, migration, rollback, and permission behavior if it occurs.
- Rollback / recovery: 若统计展示或解析行为变更引入回归，应可回退 HARN-066 单任务 commit；若涉及 schema/API 变更，需提供向后兼容路径或显式迁移回滚说明；解析器隔离和注释支持应配套回归测试，保证回滚后可恢复既有解析行为。
- Validation:
  - `python3 scripts/foreman.py validate HARN-066`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added tabbed report-batch parse statistics in batch import and parse-history views, isolated single-SQL async results from later inputs, supported SQL body -- line comments, exposed parser failure reason/line/column/offset/token/snippet through backend responses and issue cards, and updated HARN-066 docs/tests.
  - Validation evidence: python3 scripts/foreman.py validate HARN-066; python3 scripts/task_audit.py --check --phase pre-closeout; mvn -pl sql-optimization -Dtest=SqlOptimizationPipelineServiceTest,StructureParseControllerTest,ReportBatchControllerTest,ReportBatchParseStatisticsAssemblerTest test; mvn -pl sql-optimization test; npm run lint; npm run build; npm run test:form-governance; node scripts/check-batch-import-contract.mjs; node scripts/check-history-page-contract.mjs.
  - Residual risk: No schema migration was added; parser failure position remains best-effort for dialect-specific parser messages, with token/snippet fallback when exact line-column precision is unavailable.
  - Next step: Use a live report-import batch in an integration environment to visually confirm the six statistics tabs and failure-position cards against production-like data.

### HARN-064: HARN-064 / D-STORY-010: 批量报表导入解析统计与解析历史统计展示

- Status: done
- Completed at: 2026-05-05
- Commit subject: `feat(sql-optimization): add report batch parse statistics`
- Priority: 1
- Depends on: `N/A`
- Scope: HARN-064 应建立一个可验证的批次/历史解析统计契约：输入为用户选择的批次解析结果或解析历史记录，输出为与现有 SQL 解析统计口径对齐的统计数据集合，至少包含六类维度。实现不得预设现有 SQL 解析统计接口、组件、schema 或聚合逻辑可直接复用；必须在正式实现阶段读取代码后确认复用或适配方案。批量解析入口与解析历史入口应共享一致统计口径，避免产生不兼容的重复聚合逻辑。 Tech: `Repository-confirmed backend/service layer after preflight`,`Repository-confirmed API/query contract after code reading`,`Repository-confirmed frontend/history/batch parsing surfaces if within scope...
- Plan ref: docs/exec-plans/completed/HARN-064-full-auto-execution-plan.md
- Matrix context: Phase-D / Story `D-STORY-010` 解析统计与优先级分层
- Human confirmation point: Resolved before materialization on 2026-05-05: user confirmed HARN-064 formal materialization, single-agent Main Foreman routing, D-STORY-010/P1/no explicit dependency candidate binding, required statistics dimensions, parsing-history coverage, and repository-confirmed API/data/UI contract adaptation where necessary.
- Data impact: Potential read/query and aggregation impact on batch parsing results, report-import parsing records, parse history records, SQL-level statistics, logical-object statistics, and any persisted/cached statistics model if confirmed. No data migration or persistence change is authorized by this candidate pack alone; any schema or historical data backfill decision requires explicit confirmation during implementation planning.
- Rollback / recovery: Keep changes scoped to HARN-064. If implementation introduces API/UI/statistics contract regressions, rollback by reverting the HARN-064 single-task commit and restoring prior parsing/history behavior. If schema or persisted statistics changes are later approved, implementation must include explicit rollback/backfill recovery notes before closeout. Use foreman validate, pre-closeout audit, closeout, and post-closeout audit before delivery of the standard task.
- Validation:
  - `Aggregation coverage for all required dimensions: 问题场景, 重要程度, 报表视角, SQL 清单, 优先级视角, 逻辑对象视角、Batch-selected parsing statistics happy path、Parsing history statistics lookup/display path、Empty batch, missing statistics, failed/partial parse, and permission/error state coverage as applicable、Regression coverage against existing SQL parsing statistics behavior where reusable contract is confirmed、Documentation/contract validation for confirmed statistics fields and口径`
  - `python3 scripts/foreman.py validate HARN-064`
- Progress log:
  - 2026-05-05: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added a shared report-batch parse-statistics contract for batch detail and parse-history lookup, covering issue scenes, importance, report view, SQL list, priority matrix, and logical-object dimensions across backend API, frontend views, tests, and contract checks.
  - Validation evidence: python3 scripts/foreman.py validate HARN-064 --extra-command node/scripts/check-batch-import-contract.mjs --extra-command node/scripts/check-history-page-contract.mjs --extra-command mvn-report-batch-statistics-tests --extra-command npm-run-lint --extra-command npm-run-build; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: No schema migration or historical backfill was introduced; live environments still depend on existing report_batch/report_batch_item data quality and governance history availability for per-SQL drill-through.
  - Next step: In acceptance, open a resolved report-import batch from both the batch center and parse history and verify the six statistics dimensions match the same batch.

### HARN-063: HARN-063 批量解析报表结果与失败详情可见性修复

- Status: done
- Completed at: 2026-05-05
- Commit subject: `fix(frontend): expose batch report parse details`
- Priority: 1
- Depends on: `N/A`
- Scope: 用户确认该 candidate task pack 后，Main Foreman 才可按 SQLForge 标准治理流程执行 preflight、instantiate、实现、validate、audit、closeout 与单任务单 commit。实现必须保持失败、成功及其他解析状态的结果展示与详情入口语义一致，并不得在未确认字段、权限或页面边界前固化具体实现事实。 Tech: `待实例化后基于仓库上下文确认具体前端页面、后端接口、数据结构与测试框架`,`代码变更`,`自动化测试`,`文档更新`. Layer: `UI/交互层：批量解析列表或相关视图的结果展示与详情入口`,`应用/接口层：解析结果与详情数据获取或传递路径，具体范围待确认`,`测试层：关键用户路径与状态一致性覆盖`,`文档层：批量解析结果与详情查看行为说明`.
- Plan ref: docs/exec-plans/completed/HARN-063-full-auto-execution-plan.md
- Matrix context: Phase-D / Story `D-STORY-009` 批量解析与报表清单解析
- Human confirmation point: User confirmed the HARN-063 execution preview at 2026-05-05T01:32:57-05:00. Main Foreman may materialize and execute the standard task within the confirmed boundary: batch-parse report import result/detail visibility, failed-record detail access, repository-discovered fields and docs, no permission/import-format/core-parser/data-model expansion without separate confirmation.
- Data impact: 预期主要影响解析结果与解析详情的可见性展示，不应改变批量解析核心算法、导入格式、持久化语义或权限体系；若实例化后发现必须改动数据结构、权限或异步任务架构，需回到 human confirmation。
- Rollback / recovery: 通过单任务单 commit 保持可回滚边界；若实现引入展示、入口或详情数据回归，应回滚 HARN-063 相关代码、测试与文档变更，并保留审计链记录。closeout 前必须完成 validate 与 pre-closeout audit，closeout 后必须完成 post-closeout audit。
- Validation:
  - 失败解析记录可点击并展示对应解析详情、批量解析结果列表展示成功、失败及其他状态的结果信息、解析状态与详情入口可用性保持一致、解析详情展示足够定位导入解析问题的信息，字段需经确认、相关回归测试覆盖详情入口、结果展示与异常状态
  - `python3 scripts/foreman.py validate HARN-063`
- Progress log:
  - 2026-05-05: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Updated ParseBatchCenterView so failed parse records, report groups, failed report SQL rows and SQL-level result rows open detail dialogs; added contract/backend regression coverage and docs.
  - Validation evidence: python3 scripts/foreman.py validate HARN-063 --extra-command 'node scripts/check-batch-import-contract.mjs' --extra-command 'mvn -pl sql-optimization -Dtest=ReportBatchApplicationServiceTest,ReportBatchControllerTest test' --extra-command 'npm run lint' --extra-command 'npm run build'
  - Residual risk: No live browser E2E against a real failed report import environment; detail content depends on existing parse/report batch API fields.
  - Next step: Verify with a real failed report import in the target environment and confirm the detail dialogs show the expected troubleshooting fields.

### HARN-062: HARN-062 / D-STORY-009 批量报表导入多 SQL 列解析修复

- Status: done
- Completed at: 2026-05-04
- Commit subject: `fix(sql-optimization): parse report batch wide SQL columns`
- Priority: 1
- Depends on: `N/A`
- Scope: 对每一行导入数据，第一列必须稳定解析为 report code；从第二列开始遍历该行所有列，每个非空单元格都必须作为该 report code 下的一条 SQL；空单元格必须跳过；不得依赖固定 SQL 列数，需覆盖 100+ 后续列场景；测试和文档需与该规则保持一致。 Tech: `待 preflight 后确认具体实现栈与导入解析模块`,`批量导入解析逻辑`,`表格/CSV/XLSX 行列遍历逻辑`,`单元测试或集成测试，按仓库现有测试框架执行`,`相关导入规则文档`. Layer: `application/import-parsing`,`domain/report-sql-mapping`,`tests`,`docs`.
- Plan ref: docs/exec-plans/completed/HARN-062-full-auto-execution-plan.md
- Matrix context: Phase-D / Story `D-STORY-009` 批量解析与报表清单解析
- Human confirmation point: User confirmed the HARN-062 execution preview at 2026-05-04T23:12:52-05:00. Main Foreman materialization boundary: single-agent execution; reuse existing import formats and parser entry points discovered during preflight; treat cells whose trimmed text is empty as empty; preserve every non-empty SQL cell as one SQL text for the row report code, including punctuation, quotes, semicolons and line breaks when the existing reader preserves them; update only related code, tests and docs; do not add UI, permissions, new import formats or report data-model changes. If implementation proves a report data-model change is unavoidable, pause for separate confirmation.
- Data impact: 解析行为变更会影响后续批量导入结果：同一行后续所有非空列将被导入为多条 SQL，可能增加解析出的 SQL 数量；不涉及既有持久化数据迁移，除非实现阶段发现当前导入流程会立即写入数据库并需额外确认。
- Rollback / recovery: 通过单任务单 commit 收口；如验证失败或行为不符合确认规则，回滚 HARN-062 相关代码、测试和文档变更，并保留 audit/validation 记录。若已产生导入数据副作用，需按实际持久化路径制定数据回退步骤并由人类确认。
- Validation:
  - `仅一列 SQL：第一列为 report code，第二列非空 SQL 被解析、多列 SQL：第二列及之后多个非空单元格均被解析为同一 report code 下的 SQL、空列跳过：中间或尾部空单元格不产生 SQL、100+ 后续列：不丢列、不串行、不依赖固定列数、回归测试：证明当前只识别一列 SQL 的问题被修复或规避、如存在文档示例或 fixtures，同步校验示例与新规则一致`
  - `python3 scripts/foreman.py validate HARN-062`
- Progress log:
  - 2026-05-04: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Updated report batch CSV/workbook import parsing so column 1 is report code and every non-empty column after it becomes one SQL item; added CSV/XLSX regression coverage and docs.
  - Validation evidence: python3 scripts/foreman.py validate HARN-062 --extra-command 'mvn -pl sql-optimization -Dtest=ReportBatchApplicationServiceTest,ReportBatchControllerTest test' --extra-command 'node scripts/check-batch-import-contract.mjs'
  - Residual risk: TXT pipe/mock fallback remains legacy; report rows without inline SQL continue through resolver/mock source.
  - Next step: Use the committed HARN-062 behavior for batch report imports and verify with a real user workbook/CSV in the target environment.

### OPS-LOCAL-006: 重新启动前后端服务

- Status: done
- Completed at: 2026-04-29
- Commit subject: `chore(ops): restart local frontend and backend`
- Priority: 2
- Depends on: N/A
- Scope: Restart local backend services and frontend dev server for the current workspace session.
- Validation:
  - `python3 scripts/foreman.py validate OPS-LOCAL-006`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restarted the local Docker-backed infrastructure, backend runtime services, and frontend dev server for the current workspace session.
  - Validation evidence: python3 scripts/foreman.py validate OPS-LOCAL-006; python3 scripts/task_audit.py --check --phase pre-closeout; bash scripts/health-check.sh --fail-on-error
  - Residual risk: Long-running dev services remain active in separate sessions and should be stopped explicitly when no longer needed.
  - Next step: Keep the services running for user access or stop them with the repository stop scripts when finished.

### HARN-061: 重构批量解析当前批次工作台与报表宽表详情

- Status: done
- Completed at: 2026-04-29
- Commit subject: `feat(sql-optimization): support report wide sql batches`
- Priority: 1
- Depends on: HARN-060
- Scope: 实现批量解析页当前批次工作台：普通批量解析与报表导入只展示当前批次输入、批次、概览、解析结果弹窗、解析统计弹窗；报表导入支持 report_code 后续多 SQL 列宽表导入并按报表->SQL 上下结构展示，解析历史页报表导入详情支持报表分组、SQL 懒加载解析详情与批次/报表/SQL 统计；不改核心 SQL parser 算法，不弱化 tenant/request/trace 权限链路。
- Validation:
  - `python3 scripts/foreman.py validate HARN-061`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 实现报表导入宽表解析：report_code 后续多 SQL 列会拆成同一报表下多条 SQL item，响应补充 SQL 级统计与 SQL 列定位；批量解析页重构为当前批次工作台，主体只展示当前输入、批次概览、解析结果弹窗和解析统计弹窗；解析历史页报表导入详情改为报表分组，并按 SQL 懒加载治理解析详情。
  - Validation evidence: python3 scripts/foreman.py validate HARN-061 --extra-command node/scripts/check-batch-import-contract.mjs --extra-command node/scripts/check-history-page-contract.mjs --extra-command node/scripts/check-history-detail-contract.mjs --extra-command mvn-report-batch-tests --extra-command npm-run-lint --extra-command npm-run-build; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 未连接真实外部 Hetu/MRS 环境做浏览器端到端验收；当前覆盖为后端单元/controller/schema 测试、前端契约、lint、生产构建与治理审计。
  - Next step: 实机验收时用包含 100+ SQL 列的报表导入模板创建批次，确认批量解析页当前批次工作台与解析历史页报表分组/SQL 懒加载详情都符合预期。

### OPS-LOCAL-005: 重新启动前后端服务

- Status: done
- Completed at: 2026-04-29
- Commit subject: `chore(ops): restart local frontend and backend`
- Priority: 2
- Depends on: N/A
- Scope: Restart local backend services and frontend dev server for the current workspace session.
- Validation:
  - `python3 scripts/foreman.py validate OPS-LOCAL-005`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restarted the local Docker-backed infrastructure, backend runtime services, and frontend dev server for the current workspace session.
  - Validation evidence: python3 scripts/foreman.py validate OPS-LOCAL-005; python3 scripts/task_audit.py --check --phase pre-closeout; bash scripts/health-check.sh --fail-on-error
  - Residual risk: Long-running dev services remain active in separate sessions and should be stopped explicitly when no longer needed.
  - Next step: Keep the services running for user access or stop them with the repository stop scripts when finished.

### HARN-060: 补齐报表导入解析历史 SQL 明细

- Status: done
- Completed at: 2026-04-29
- Commit subject: `fix(frontend): hydrate report import parse details`
- Priority: 1
- Depends on: HARN-059
- Scope: 修复解析历史页中报表导入批次详情只展示简略 SQL 明细的问题；报表导入每个报表、每条解析 SQL 都应展示参考 SQL 解析页面的解析统计、结构解析、Access Parse、风险/问题与原始 SQL 信息，不改变核心 parser 算法、权限边界或历史保留策略。
- Validation:
  - `python3 scripts/foreman.py validate HARN-060`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-29: added report-import SQL detail hydration in parse history by resolving each report item parseTaskId to its governance parse-history detail.
  - 2026-04-29: expanded the report-import drawer to show per-report/per-SQL parse statistics, structure parse, Access Parse, risks, issues, and original SQL evidence.
- Context closeout:
  - Completed scope: 修复解析历史页报表导入批次详情：打开报表导入抽屉时按每个 report item 的 parseTaskId 回查治理解析历史详情，并在每个报表/SQL 卡片内展示原始 SQL、解析统计、结构解析、Access Parse、风险与问题清单，同时保留完整解析历史跳转。
  - Validation evidence: python3 scripts/foreman.py validate HARN-060 --extra-command node/scripts/check-history-page-contract.mjs --extra-command node/scripts/check-history-detail-contract.mjs --extra-command node/scripts/check-batch-import-contract.mjs --extra-command npm-run-lint --extra-command npm-run-build; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 未连接真实外部 Hetu/MRS 环境做浏览器端到端验收；当前覆盖为前端 contract、lint、生产构建与治理审计。若某条 SQL 的 parseTaskId 没有写入 governance query_history，页面会显示详情缺失提示。
  - Next step: 在实机验收时执行报表导入解析后从解析历史页打开该报表批次，确认每个报表 SQL 的 Loaded details 计数与 parseTaskId 数量一致。

### HARN-059: 修复解析历史详情展示解析结果与原始SQL

- Status: done
- Completed at: 2026-04-29
- Commit subject: `fix(frontend): enrich parse history detail`
- Priority: 1
- Depends on: HARN-058
- Scope: 修复解析历史单条详情只展示 JSON、缺少 SQL 解析页同款解析结果与统计信息的问题；解析历史详情必须展示原始 SQL，并复用 SQL 解析结果/统计口径，不改变核心 parser 算法、权限边界或持久化保留策略。
- Validation:
  - `python3 scripts/foreman.py validate HARN-059`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-29: added a structured parse-result detail tab to parse history, defaulted history detail drill-through to the parse result view, and surfaced original SQL in the single-record detail.
  - 2026-04-29: validated frontend contracts and production build for parse history/detail changes.
- Context closeout:
  - Completed scope: 修复解析历史单条详情展示：详情默认进入结构化解析结果页签，展示原始 SQL、解析结果摘要、解析统计、结构解析卡、Access Parse 卡、风险/问题清单，同时保留原始 JSON 证据页签。
  - Validation evidence: python3 scripts/foreman.py validate HARN-059 --extra-command node/scripts/check-history-detail-contract.mjs --extra-command node/scripts/check-history-page-contract.mjs --extra-command node/scripts/check-parse-workbench-contract.mjs --extra-command npm-run-lint --extra-command npm-run-build; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 未连接真实外部 Hetu/MRS 环境做浏览器端到端验收；当前覆盖为静态契约、lint、生产构建与治理审计。
  - Next step: 如目标环境仍看不到原始 SQL，优先核对对应 query_history 记录是否已通过加密 SQL surface 写入 sql_text_cipher。

### OPS-LOCAL-004: 重新启动前后端服务

- Status: done
- Completed at: 2026-04-29
- Commit subject: `chore(ops): restart local frontend and backend`
- Priority: 2
- Depends on: N/A
- Scope: Restart local backend services and frontend dev server for the current workspace session.
- Validation:
  - `python3 scripts/foreman.py validate OPS-LOCAL-004`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restarted the local Docker-backed infrastructure, backend runtime services, and frontend dev server for the current workspace session.
  - Validation evidence: python3 scripts/foreman.py validate OPS-LOCAL-004; python3 scripts/task_audit.py --check --phase pre-closeout; bash scripts/health-check.sh --fail-on-error
  - Residual risk: Long-running dev services remain active in separate sessions and should be stopped explicitly when no longer needed.
  - Next step: Keep the services running for user access or stop them with the repository stop scripts when finished.

### HARN-058: 修复解析历史记录投影与报表导入明细

- Status: done
- Completed at: 2026-04-29
- Commit subject: `fix(governance): repair parse history traceability`
- Priority: 1
- Depends on: HARN-057
- Scope: 修复单条 SQL、批量解析和报表导入解析记录在解析历史页不可识别/不可筛选的问题，补齐 query_history 投影上下文、报表导入 SQL 级与报表级解析明细展示、测试与文档。
- Validation:
  - `python3 scripts/foreman.py validate HARN-058`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 修复 SQL parse history 的 config snapshot/reference 链、query_history upsert 与 query_context 归一化投影；补齐批量/报表 access 解析历史合并、报表导入 SQL 级与报表级详情、解析历史页报表导入抽屉和 SQL 解析页 historyId 跳转。
  - Validation evidence: mvn -pl governance -Dtest=GovernanceParseHistoryTraceabilityApplicationServiceTest,GovernanceProtectedPersistenceServiceTest,TraceabilitySchemaMappingTest clean test; mvn -pl sql-optimization -Dtest=ParseBatchApplicationServiceTest,ReportBatchApplicationServiceTest clean test; node scripts/check-history-page-contract.mjs; node scripts/check-history-detail-contract.mjs; node scripts/check-batch-import-contract.mjs; node scripts/check-parse-workbench-contract.mjs; npm run lint; npm run test:form-governance; npm run build; python3 scripts/foreman.py validate HARN-058; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 未连接真实外部 Hetu/MRS 环境做端到端页面冒烟；当前覆盖为后端单元、前端 contract、lint/build 与治理审计。
  - Next step: 如需实机验收，在本地/测试环境执行单条综合解析、批量 SQL 导入和报表导入解析，确认解析历史页可按 report/stage/bizDate 筛选并打开详情。

### OPS-LOCAL-003: 重新启动前后端服务

- Status: done
- Completed at: 2026-04-29
- Commit subject: `chore(ops): restart local frontend and backend`
- Priority: 2
- Depends on: N/A
- Scope: Restart local backend services and frontend dev server for the current workspace session.
- Validation:
  - `python3 scripts/foreman.py validate OPS-LOCAL-003`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restarted the local Docker-backed infrastructure, backend runtime services, and frontend dev server for the current workspace session.
  - Validation evidence: python3 scripts/foreman.py validate OPS-LOCAL-003; python3 scripts/task_audit.py --check --phase pre-closeout; bash scripts/health-check.sh --fail-on-error
  - Residual risk: Long-running dev services remain active in separate sessions and should be stopped explicitly when no longer needed.
  - Next step: Keep the services running for user access or stop them with the repository stop scripts when finished.

### HARN-057: 修复解析历史默认查询与刷新

- Status: done
- Completed at: 2026-04-29
- Commit subject: `fix(frontend): repair parse history refresh defaults`
- Priority: 1
- Depends on: HARN-056
- Scope: 修复解析历史页默认查询条件非空和刷新失败问题，分离页面筛选条件与请求上下文租户，补齐批量历史刷新、测试与文档。
- Validation:
  - `node scripts/check-history-page-contract.mjs`
  - `npm run build`
  - `python3 scripts/foreman.py validate HARN-057`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-29: captured raw requirement and active execution plan for parse-history default-empty filters and refresh repair.
  - 2026-04-29: separated parse-history visible tenant filter from request context tenant, reset tenant/sort defaults to empty, and hardened batch history refresh.
  - 2026-04-29: validation passed with node scripts/check-history-page-contract.mjs, npm run test:form-governance, npm run lint, npm run build, node scripts/lint-repository-knowledge.js, and python3 scripts/foreman.py validate HARN-057.
- Context closeout:
  - Completed scope: 修复解析历史页默认筛选与刷新：可见 tenantId、sortBy、sortOrder 默认保持空值；query-history page API 区分筛选租户与请求上下文租户；批量解析与报表导入历史刷新使用有效上下文租户；补齐历史页静态契约检查、表单治理文档和 HARN-057 执行追溯。
  - Validation evidence: node scripts/check-history-page-contract.mjs; npm run test:form-governance; npm run lint; npm run build; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py validate HARN-057; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 未运行真实后端浏览器 smoke；本任务未改后端持久化逻辑，验证覆盖前端构建、lint、静态契约和治理链路。
  - Next step: 如目标环境仍显示空列表，优先核对当前请求上下文租户下是否已有 query_history、parse_batch 或 report_batch 数据。

### OPS-LOCAL-002: 重新启动前后端服务

- Status: done
- Completed at: 2026-04-29
- Commit subject: `chore(ops): restart local frontend and backend`
- Priority: 2
- Depends on: N/A
- Scope: Restart local backend services and frontend dev server for the current workspace session.
- Validation:
  - `python3 scripts/foreman.py validate OPS-LOCAL-002`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restarted the local Docker-backed infrastructure, backend runtime services, and frontend dev server for the current workspace session.
  - Validation evidence: python3 scripts/foreman.py validate OPS-LOCAL-002; python3 scripts/task_audit.py --check --phase pre-closeout; bash scripts/health-check.sh --fail-on-error
  - Residual risk: Long-running dev services remain active in separate sessions and should be stopped explicitly when no longer needed.
  - Next step: Keep the services running for user access or stop them with the repository stop scripts when finished.

### HARN-056: 解析历史落库与批量解析中心重构

- Status: done
- Completed at: 2026-04-29
- Commit subject: `HARN-056: 解析历史落库与批量解析中心重构`
- Priority: 1
- Depends on: N/A
- Scope: 所有从SQL解析产生的记录（结构解析、后端解析、批量解析、文件导入解析）统一落库并在解析历史展示；修复批量解析中心可用性；批量解析中心报表上传自动识别文件类型；批量解析支持页面多SQL输入和文件导入两种入口并展示结果；文件导入解析进入解析历史；批量解析与文件导入提供解析统计，参考SQL解析统计；同步代码、测试、文档。
- Validation:
  - `python3 scripts/foreman.py validate HARN-056`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Persisted parse batch and report batch histories, added history list endpoints, auto-detected report import file types, and rebuilt parse history/batch center pages.
  - Validation evidence: python3 scripts/foreman.py validate HARN-056; python3 scripts/task_audit.py --check --phase pre-closeout; mvn -q -pl sql-optimization -Dtest=ParseBatchControllerTest,ReportBatchControllerTest,ParseBatchPersistenceSchemaMappingTest test; npm run build
  - Residual risk: No historical backfill was performed for legacy parse records outside the current persisted batch tables.
  - Next step: Monitor live usage and backfill legacy parse history only if product owners need prior records surfaced.

### OPS-LOCAL-001: 启动前后端

- Status: done
- Completed at: 2026-04-29
- Commit subject: `chore(ops): start local frontend and backend`
- Priority: 2
- Depends on: N/A
- Scope: Start local backend services and frontend dev server for the current workspace session.
- Validation:
  - `python3 scripts/foreman.py validate OPS-LOCAL-001`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Started local infrastructure, backend runtime services, and frontend dev server for the current workspace session.
  - Validation evidence: scripts/local-start.sh; scripts/start-backend-services.sh --reuse-running-stack --skip-build; scripts/health-check.sh --fail-on-error
  - Residual risk: Long-running dev servers remain active in separate sessions and should be stopped explicitly when no longer needed.
  - Next step: Keep the dev sessions running for user access or stop them with the repository stop scripts when finished.

### HARN-052: 修复 SQL解析运行时 500

- Status: done
- Completed at: 2026-04-28
- Commit subject: `fix(sql-optimization): restore combined parse runtime`
- Priority: 1
- Depends on: N/A
- Scope: 重启并验证 sql-optimization 运行实例，修复 SQL解析 在综合/结构解析请求下返回 500 的运行时故障，确保当前源码和线上实例一致。
- Validation:
  - `python3 scripts/foreman.py validate HARN-052`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 修复 StructureParseApplicationService 缺失治理解析历史 DTO 导入导致 clean build/runtime 综合解析返回 [10000] 的问题；验证结构解析、访问解析和综合解析 HTTP 200。
  - Validation evidence: mvn -B -f sql-optimization/pom.xml clean test -Dtest=StructureParseControllerTest,AccessParseControllerTest; python3 scripts/foreman.py validate HARN-052; curl POST http://127.0.0.1:8082/api/sql-optimization/parse/combined returned HTTP 200.
  - Residual risk: 本地治理历史写入仍可能因治理服务权限/数据状态降级为 WRITE_FAILED，但解析主流程已捕获降级且不再返回 500。
  - Next step: 如需恢复解析历史落库，单独排查治理 /parse-history/write 的权限与数据依赖。

### HARN-051: 压缩 SQL解析顶部信息卡

- Status: done
- Completed at: 2026-04-28
- Commit subject: `feat(sql-optimization): simplify SQL Parse top cards`
- Priority: 1
- Depends on: N/A
- Scope: 收敛 SQL解析 页面顶部的冗余信息卡，去掉重复的 workspace entry 与 hero summary chips，只保留核心操作入口和结果区。
- Validation:
  - `python3 scripts/foreman.py validate HARN-051`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Removed the redundant hero summary chips and the single workspace entry card from the SQL Parse page, leaving the core action buttons, input form, and result/statistics areas intact.
  - Validation evidence: python3 scripts/foreman.py validate HARN-051; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The main parse workspace still shows request-summary chips near the form, which remain useful context rather than top-level navigation.
  - Next step: None.

### HARN-050: SQL解析单条页面收口

- Status: done
- Completed at: 2026-04-28
- Commit subject: `feat(sql-optimization): rename parse workbench to SQL Parse`
- Priority: 1
- Depends on: N/A
- Scope: 将解析工作台收束为单条 SQL 解析页面，去除批量/历史入口，补齐字段帮助、统计展示、风险清单本地化与单 SQL 历史写入。
- Validation:
  - `python3 scripts/foreman.py validate HARN-050`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Renamed the single-parse workbench to SQL解析, removed visible batch/history entrances from the page, added a field-help dialog and denser structure-card layout, localized risk text, persisted single SQL structure parse history, and refreshed validation contracts/tests/docs.
  - Validation evidence: python3 scripts/foreman.py validate HARN-050; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Legacy batch and history routes still exist elsewhere in the app shell for compatibility, but the SQL解析 page no longer exposes them as entrances.
  - Next step: None.

### HARN-049: HARN-049 解析工作台、批量解析与解析历史能力正式实现

- Status: done
- Completed at: 2026-04-28
- Commit subject: `HARN-049 split parse workbench history pages`
- Priority: 1
- Depends on: `N/A`
- Scope: 实现后，单条解析页面采用上下结果布局，合并总结/结论为统一“解析结果”结构，移除重复标题；urgent=true 与 priority=P1 有明确保守红色提示；中文自然语言为主，代码、字段名、缩写和英文技术标识提供 tooltip 或小按钮解释入口；解析完成记录写入历史存储，并可在独立解析历史查询页面检索和查看；解析工作台、批量解析、解析历史查询路由、状态和业务逻辑互不耦合。 Tech: `existing frontend routing/page stack`,`existing frontend state management pattern`,`existing UI/design token or status color convention`,`existing persistence/storage layer`,`existing query/detail-vi...
- Plan ref: docs/exec-plans/completed/HARN-049-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-008` 解析工作台与批量解析中心
- Human confirmation point: Main Foreman 已在 HARN-049 preflight 后确认 materialization 边界：任务绑定 E-STORY-008 / Phase-E；实现复用现有 ROUTE_PATHS、ParseBatchCenterView、ParseRecordView、query_history、parse_batch、parse_batch_item 等当前仓库真值；新增或调整持久化仅限解析历史闭环所需字段、接口和兼容性补充；若发现必须改变权限、保留、脱敏策略或核心 parser 算法，则暂停并另行确认。
- Data impact: 本任务不引入新的敏感数据类别和独立保留策略；解析历史闭环优先复用当前仓库已有 query_history 查询面、parse_batch / parse_batch_item 批量解析证据表和既有 tenant_id 隔离、分页、审计追溯语义。若实现需要补充字段或接口，只允许做兼容性新增，并继续遵守 R-031 至 R-038 的 MySQL 主持久化、后端历史查询、不可依赖浏览器临时状态、历史默认保留与敏感信息不得明文落库规则。解析 SQL 文本按现有历史/批量证据模型处理，不在前端新增本地持久化副本。
- Rollback / recovery: 保留核心 parser 算法不变；若页面拆分或历史能力异常，可回退 HARN-049 单任务 commit，并通过治理台账记录回滚；若涉及数据库迁移，应提供可逆迁移或兼容性降级方案，确保旧解析流程仍可运行且不阻断单条解析。
- Validation:
  - `页面拆分与独立路由测试：解析工作台、批量解析、解析历史查询互不串状态、单条解析结果上下布局与重复标题移除测试、总结/结论合并为统一解析结果结构的展示测试、urgent=true、priority=P1 及其他状态颜色提示测试、中文展示与代码/字段/缩写 help 入口测试、解析完成后历史记录持久化测试、解析历史查询与详情查看关键路径测试、治理验证：foreman validate、pre-closeout audit、post-closeout audit`
  - `python3 scripts/foreman.py validate HARN-049`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Split parse workbench, batch parse, and parse history into independent pages; improved single-parse result layout, result notes, color cues, Chinese-first labels, and help tooltips; added parse-history persistence through governance query history.
  - Validation evidence: node scripts/check-parse-workbench-contract.mjs; node scripts/check-batch-import-contract.mjs; node scripts/check-history-page-contract.mjs; node scripts/check-navigation-shell-contract.mjs; npm run lint; npm run build; mvn -B -pl sqlforge-shared,governance,sql-optimization -am test; python3 scripts/foreman.py validate HARN-049; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: No known functional blockers. Frontend build still reports the existing Vite CJS API deprecation warning.
  - Next step: Use the three independent pages for manual runtime smoke with a live backend and database when service environment is available.

### D-TASK-075: 补强复杂反模式 SQL 结构解析验证

- Status: done
- Completed at: 2026-04-28
- Commit subject: `feat(sql-optimization): D-TASK-075 detect complex SQL antipatterns`
- Priority: 1
- Depends on: `D-TASK-073`,`D-TASK-074`
- Scope: 结构解析必须在不执行 SQL、不访问元数据的前提下，对复杂嵌套 SQL 输出稳定的静态结构特征、查询意图标签、风险清单和启发式资源估算；新增风险码不能破坏 D-TASK-073/074 旧响应字段。 Tech: `JAVA-BE`,`DOCS`. Layer: `application(controller/service)/domain`,`shared contract docs`.
- Plan ref: docs/exec-plans/completed/D-TASK-075-full-auto-execution-plan.md
- Matrix context: Phase-D / Story `D-STORY-007` 结构解析与数据访问解析双轨闭环
- Human confirmation point: 若实现需要执行用户 SQL、访问真实数据库/元数据、断言真实索引存在性、引入持久化字段或改变结构解析旧字段语义，必须暂停并由人工确认。
- Data impact: 不变更持久化模型；影响结构解析 API 响应中的新增/更丰富风险标签、issues、featureSummary 计数和启发式资源估算。
- Rollback / recovery: 回退递归 AST 特征提取、复杂 SQL 测试、风险码映射和文档补充，恢复 D-TASK-074 后的结构解析行为；不触碰历史数据。
- Validation:
  - `SqlOptimizationPipelineService complex SQL profile test、StructureParseController complex anti-pattern contract test、sql-optimization module tests、task audit、knowledge lint`
  - `python3 scripts/foreman.py validate D-TASK-075`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added expected-vs-system regression coverage for the supplied complex anti-pattern SQL, extended recursive legacy parser traversal for nested/select/where subqueries, correlated alias detection, function-wrapped predicates, leading wildcard LIKE, OR predicates, ORDER BY random and repeated table scans, and surfaced the new counters through intent profile, feature summary, risk tags, checklist, issues and resource estimates.
  - Validation evidence: mvn -B -pl sql-optimization -am -Dtest=SqlOptimizationPipelineServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipITs; mvn -B -pl sql-optimization -am -Dtest=StructureParseControllerTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipITs; mvn -B -pl sql-optimization -am test -DskipITs; mvn -B -pl sql-optimization -am validate pmd:pmd checkstyle:check -DskipTests; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py validate D-TASK-075; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Signals remain static AST heuristics without catalog metadata, real index truth or execution-plan cost proof; Trino path remains compatibility-oriented and was not expanded to identical nested anti-pattern extraction in this task.
  - Next step: Add metadata-backed access/index evidence and a Trino-specific complex fixture if query-intent scoring needs engine-parity validation.

### D-TASK-074: 补齐结构解析 SQL 指纹前处理契约

- Status: done
- Completed at: 2026-04-28
- Commit subject: `fix(sql-optimization): D-TASK-074 stabilize SQL fingerprints`
- Priority: 1
- Depends on: `D-TASK-073`
- Scope: 结构解析 sqlFingerprint 必须基于去注释、字面量参数化、大小写和空白标准化后的 SQL 形态生成；该指纹用于治理聚合提示，不代表 SQL 语义等价证明。 Tech: `JAVA-BE`,`DOCS`. Layer: `shared/utils`,`application(controller/service)`,`docs`.
- Plan ref: docs/exec-plans/completed/D-TASK-074-full-auto-execution-plan.md
- Matrix context: Phase-D / Story `D-STORY-007` 结构解析与数据访问解析双轨闭环
- Human confirmation point: 若修复需要历史 SQL 指纹回填、跨服务缓存迁移、数据库字段变更或把 fingerprint 解释为严格 SQL 语义等价证明，必须暂停并由人工确认。
- Data impact: 不变更持久化模型；新请求生成的 sqlFingerprint 会对注释和字面量变化更稳定。历史已落库指纹不在本任务中回填或迁移。
- Rollback / recovery: 回退 SqlFingerprintUtils 前处理增强、相关测试和文档补充，恢复 D-TASK-073 后的指纹行为；不触碰历史数据。
- Validation:
  - `sqlforge-shared utility tests、structure parse controller regression、sql-optimization module tests、task audit、knowledge lint`
  - `python3 scripts/foreman.py validate D-TASK-074`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 复盘 D-TASK-073 后补齐 SQL 指纹前处理契约：共享 fingerprint 工具现在去除 SQL 注释、参数化字符串/数字/命名参数字面量、折叠空白、统一大小写并忽略末尾分号；结构解析接口回归验证注释和字面量变化不会改变 sqlFingerprint；产品规格明确该指纹只用于治理聚合，不代表完整 SQL 语义等价。
  - Validation evidence: mvn -B -pl sqlforge-shared -Dtest=SqlFingerprintUtilsTest test; mvn -B -pl sql-optimization -am -Dtest=StructureParseControllerTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipITs; mvn -B -pl sql-optimization -am test -DskipITs; mvn -B -pl sql-optimization -am validate pmd:pmd checkstyle:check -DskipTests; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py validate D-TASK-074; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: SQL 指纹仍是静态文本级治理聚合，不代表完整 SQL 语义等价；历史已生成 fingerprint 不在本任务中回填；方言特有字面量后续可继续补样本。
  - Next step: 如后续需要跨服务缓存迁移或历史 fingerprint 重算，应单独任务化并评估数据迁移与回滚策略。

### D-TASK-073: 升级结构解析查询意图理解与双 parser 抽象

- Status: done
- Completed at: 2026-04-28
- Commit subject: `feat(sql-optimization): D-TASK-073 add query intent parsing`
- Priority: 1
- Depends on: `D-TASK-045`,`D-TASK-051`,`E-TASK-020`
- Scope: 结构解析在原有字段兼容基础上输出 SQL 指纹、查询意图标签、多维特征、风险清单与启发式资源估算，底层 parser 通过 adapter 抽象可配置选择，不执行 SQL 或访问生产数据。 Tech: `JAVA-BE`,`VUE-FE`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`frontend/router/views/styles`,`docs`.
- Plan ref: docs/exec-plans/completed/D-TASK-073-full-auto-execution-plan.md
- Matrix context: Phase-D / Story `D-STORY-007` 结构解析与数据访问解析双轨闭环
- Human confirmation point: 若 Trino parser 依赖引入导致许可证、包冲突或大规模迁移，或查询意图理解会破坏旧结构解析响应、执行 SQL、访问生产数据、把启发式资源估算写成真实执行计划结论，需人工确认。
- Data impact: 预期不变更持久化数据模型；影响结构解析 API 响应字段、前端展示契约、解析规则与文档说明。若实现需要新增数据库字段或改变 access parse 权限/元数据行为，必须升级为人类确认点后再推进。
- Rollback / recovery: 回退新增 parser adapter、查询意图字段、风险/资源估算规则和前端展示块，恢复 D-TASK-045 / E-TASK-020 既有结构解析响应与解析工作台展示基线；保留文档更正记录与测试证据。
- Validation:
  - `sql-optimization 模块测试、structure parse contract/controller 测试、parse workbench contract 测试、npm run build、npm run lint、task audit、knowledge lint`
  - `python3 scripts/foreman.py validate D-TASK-073`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added structure-parse query intent outputs, SQL fingerprint, feature summary, structured risk checklist, heuristic resource estimate, configurable legacy parser / Trino parser adapter path, parse workbench display, contract tests, parser tests, frontend contract checks, and product documentation.
  - Validation evidence: mvn -B -pl sql-optimization -am test -DskipITs; node scripts/check-parse-workbench-contract.mjs; npm run lint; npm run build; mvn -B -pl sql-optimization -am validate pmd:pmd checkstyle:check -DskipTests; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py validate D-TASK-073; python3 scripts/task_audit.py --check --phase pre-closeout.
  - Residual risk: Trino parser adapter is covered by repo-local samples and should gain more dialect fixtures over time; resource cost remains static heuristic evidence, not a real execution plan; NL2SQL remains a future task.
  - Next step: Use the new intent profile as the backend contract for future NL2SQL and recommendation work without moving metadata or permissions into structure parse.

### HARN-047: 修复日期区间选择器页面不可用

- Status: done
- Completed at: 2026-04-28
- Commit subject: `fix(frontend): register semantic date components`
- Priority: 1
- Depends on: HARN-046
- Scope: 复现并修复 ParseRecordView 日期/日期时间区间选择器在页面上不可用的问题；用浏览器级验证确认可打开、可选择并提交前拆回既有字段；同步测试与文档。
- Validation:
  - `python3 scripts/foreman.py validate HARN-047`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 修复 HARN-046 页面日期区间不可用问题：在 Vue 入口显式注册 Element Plus 的 ElDatePicker 与 ElInputNumber，并扩展表单治理静态检查覆盖运行时组件注册。
  - Validation evidence: npm run test:form-governance; npm run lint; npm run build; system Chrome Playwright smoke opened ParseRecordView date range panel and selected 2026-04-06..2026-04-10; python3 scripts/foreman.py validate HARN-047; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Playwright packaged browser未安装，浏览器级复现使用系统 /usr/bin/google-chrome；后续若 CI 需要自动化运行，应先补浏览器安装或改用已有 smoke 基础设施。
  - Next step: 继续页面组件治理时，所有新增 Element Plus 组件必须同步 src/main.js 注册或改为统一插件注册方式。

### HARN-046: 修正 HARN-045 日期区间组件治理

- Status: done
- Completed at: 2026-04-28
- Commit subject: `fix(frontend): use range pickers for history dates`
- Priority: 1
- Depends on: HARN-045
- Scope: 修正 HARN-045 后续缺陷：分析已改页面日期字段语义，将需要范围筛选的日期改为日期区间/日期时间区间组件，保持 API 字段名和提交格式兼容；更新测试与文档。
- Validation:
  - `python3 scripts/foreman.py validate HARN-046`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 修正 HARN-045 日期组件治理：ParseRecordView 的 queryDateStart/queryDateEnd 改为日期区间，submittedStart/submittedEnd 改为日期时间区间；提交前仍拆回原 API 字段；保留 bizDate 单日业务日选择；同步静态验证和表单治理文档。
  - Validation evidence: npm run test:form-governance; npm run lint; npm run build; python3 scripts/foreman.py validate HARN-046; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 仅修正 HARN-045 已改的 ParseRecordView 日期字段；其他页面日期字段如需继续区间化，应按表单治理文档另行分批处理。
  - Next step: 如需要继续治理 AccelerationView 中同类历史筛选日期字段，建议按 HARN-046 的区间映射规则单独开 follow-up。

### HARN-045: HARN-045 页面组件语义治理执行模板

- Status: done
- Completed at: 2026-04-28
- Commit subject: `feat(frontend): govern semantic form components`
- Priority: 1
- Depends on: `N/A`
- Scope: 用户已确认执行模板，Main Foreman 可 materialize HARN-045 并进入实现流程。后续实现必须先执行 preflight，再通过 foreman instantiate/validate/closeout 与 task_audit pre-closeout/post-closeout 完成治理链路。输出物必须包含代码、测试、文档；不得破坏既有业务流程、数据提交格式、默认值、回显、校验行为和权限边界。 Tech: `Frontend form components`,`DateTime picker`,`Select / searchable select`,`Switch / checkbox`,`Number input`,`Form validation`,`API schema alignment`,`Automated UI/form tests`,...
- Plan ref: docs/exec-plans/completed/HARN-045-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-003` 前后端分离持续治理
- Human confirmation point: 用户已在 2026-04-28 对 HARN-045 执行模板、任务边界、输出物要求以及 Main Foreman 创建正式任务并进入实现流程作出明确确认。实现过程中若字段语义、候选值来源、权限边界或提交格式无法从权威材料确认，必须暂停请用户再次确认。
- Data impact: 预期不变更持久化数据模型和后端接口契约；风险集中在前端表单提交格式、默认值、回显、校验和候选值过滤。若实现需要改变 API contract、数据格式或权限行为，必须升级为人类确认点后再推进。
- Rollback / recovery: 普通 standard 任务按单任务单 commit 管理。若组件替换造成行为回归，优先通过该任务 commit 回退或在同一治理链路内做最小修复；文档和测试变更需与代码回退保持一致。不得使用 git reset --hard、git add .、git add -A、git commit -a 等破坏审计链的命令。
- Validation:
  - 覆盖日期时间组件选择、默认值、回显和提交格式、覆盖租户、数据源等受控候选字段的选择、候选值加载和提交、覆盖布尔、数值、枚举、关联资源等字段的关键交互与边界输入、覆盖表单加载、编辑、校验、提交、回显的主流程、覆盖候选值加载失败或权限不可见时的保守行为，若仓库现有测试体系支持
  - `python3 scripts/foreman.py validate HARN-045`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: SystemView 与 ParseRecordView 页面表单组件语义治理：租户/数据源下拉、日期/日期时间选择、枚举 select、数值 input-number、布尔 switch、敏感凭证 password input，并新增表单治理文档与静态验证脚本。
  - Validation evidence: npm run test:form-governance; npm run lint; npm run build; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py validate HARN-045; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: HARN-045 未改写无法从代码或文档确认语义的自由文本字段；数据源候选加载失败时保留 allow-create 手动值以避免权限不可见场景回归。
  - Next step: 如需继续治理 AccelerationView、ParseBatchCenterView 等剩余页面，按同一文档基线另开任务分批处理。

### D-TASK-039: 扩展 SQL 查询执行摘要契约

- Status: done
- Completed at: 2026-04-28
- Commit subject: `feat(query-execution): complete D-TASK-039 summary contract`
- Priority: 1
- Depends on: `D-TASK-038`
- Scope: 查询执行返回 comment context、binding summary、query-date、logical object hits、route/cache summary 与 lightweight parse summary Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-006` 查询历史与执行取证闭环
- Human confirmation point: 若查询执行摘要契约会改变既有受保护请求、失败语义或让 comment/binding/logical-object 信息在未校验时对外暴露，需人工确认
- Data impact: 查询执行响应、审计摘要、前后端契约
- Rollback / recovery: 保留原执行与错误响应路径，新增字段可降级为空，不删除旧字段
- Validation:
  - `query-execution controller/service 契约测试`
  - `python3 scripts/foreman.py validate D-TASK-039`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Extended query-execution synchronous responses with comment context, binding summary, query-date summary, logical object hits, route/cache summaries, and lightweight parse summary surfaces; added deterministic application-layer summary builders and locked the contract with controller/service tests.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-039 --include-task-audit --extra-command 'mvn -pl query-execution -Dtest=QueryExecutionApplicationServiceTest,QueryExecutionControllerTest,QueryExecutionBenchmarkWorkloadServiceTest test'; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Logical-object and lightweight-parse summaries are intentionally lightweight SQL-token heuristics for the synchronous query path; deeper parser-grade semantics remain owned by the sql-optimization parse surfaces and follow-on query/history work.
  - Next step: Instantiate D-TASK-040 next so governance history list/detail surfaces consume the now-exposed query execution summary contract end to end.

### F-TASK-042: 落地回归守护统计与告警

- Status: done
- Completed at: 2026-04-28
- Commit subject: `feat(benchmark): add regression guard alert linkage`
- Priority: 1
- Depends on: `F-TASK-041`,`F-TASK-037`
- Scope: regression summary、threshold hit 与 alert linkage Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-F / Story `E-STORY-011` 压测中心与开放接入页
- Human confirmation point: 若回归守护统计与告警会把实验性 benchmark 结果提升为默认生产风险判定，需人工确认
- Data impact: regression summary、threshold hit 与 alert linkage
- Rollback / recovery: 恢复为显式模板/测试集范围内的回归守护，不扩大默认告警面
- Validation:
  - `regression/alert linkage 测试`
  - `python3 scripts/foreman.py validate F-TASK-042`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Extended benchmark report/domain/persistence contracts with regression summaries and governance alert linkages; added the governance internal benchmark-regression alert emit endpoint and failed-threshold-only regression guard emission flow; updated benchmark/governance tests, schema migration, and contract baseline documentation.
  - Validation evidence: mvn -pl benchmark-engine -am -DskipITs -Dtest=BenchmarkTaskModelApplicationServiceTest,BenchmarkRegressionAlertServiceTest,BenchmarkTaskWorkerTest,MybatisBenchmarkTaskRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false test；mvn -pl governance -am -DskipITs -Dtest=AlertRuleApplicationServiceTest,AlertEmissionApplicationServiceTest,GovernanceBenchmarkRegressionAlertApplicationServiceTest,GovernanceCapabilityApplicationServiceTest,AuthWebMvcTest -Dsurefire.failIfNoSpecifiedTests=false test；mvn -pl benchmark-engine,governance -am test -DskipITs；python3 scripts/foreman.py validate F-TASK-042 --extra-command "mvn -pl benchmark-engine,governance -am test -DskipITs"；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: Benchmark regression alerts are intentionally limited to failed-threshold REGRESSION_GUARD reports; if warning-only guards or broader benchmark verdicts must trigger default governance risk handling later, that scope still needs an explicit follow-up decision.
  - Next step: Phase-F benchmark backend tasks are complete; the next dependent implementation is E-TASK-027 to consume regression summary and alert linkage contracts in the frontend benchmark pages.

### F-TASK-041: 打通推荐 SQL 到对比压测

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(benchmark): orchestrate recommendation comparison benchmarks`
- Priority: 1
- Depends on: `F-TASK-040`,`D-TASK-062`
- Scope: recommendation -> comparison benchmark 契约与编排 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-F / Story `F-STORY-011` 压测模板、测试集与解析联动
- Human confirmation point: 若 recommendation-to-benchmark 会把推荐 SQL 自动执行为压测任务、绕过审批与安全边界，需人工确认
- Data impact: 推荐对象与对比压测联动链路
- Rollback / recovery: 恢复 recommendation 与 benchmark 的显式确认边界
- Validation:
  - `recommendation-to-benchmark 测试`
  - `python3 scripts/foreman.py validate F-TASK-041`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 recommendation -> comparison benchmark 编排入口：从 sql-optimization 读取 recommendation，校验 source/recommended SQL 的只读边界，生成 RECOMMENDATION_GENERATION test set，并提交 comparison benchmark task；同时补齐 recommendation client、响应契约、只读 SQL 共享校验和 controller/service 测试。
  - Validation evidence: mvn -pl benchmark-engine clean -Dtest=BenchmarkRecommendationComparisonApplicationServiceTest,BenchmarkTaskControllerTest test；mvn -pl benchmark-engine test；python3 scripts/foreman.py validate F-TASK-041 --extra-command "mvn -pl benchmark-engine test"；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前 comparison benchmark worker 仍以 task 主 SQL 作为执行入口，recommendation test set 中的 source/recommended 双 case 先作为契约与追溯元数据落库；若后续需要真正按测试集多 case 回放，还需扩展 worker 执行面。
  - Next step: 进入 F-TASK-042，把 benchmark regression summary、threshold hit 和 governance alert linkage 收口到报告与告警编排。

### F-TASK-040: 打通解析结果到测试集一键生成

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(benchmark): generate test sets from parse results`
- Priority: 1
- Depends on: `F-TASK-039`,`D-TASK-058`
- Scope: parse issue / report / SQL 结果生成 benchmark test set Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-F / Story `F-STORY-011` 压测模板、测试集与解析联动
- Human confirmation point: 若 parse-to-benchmark 联动会把解析问题自动视为可直接压测对象、绕过安全边界，需人工确认
- Data impact: 解析结果到 test set 的联动对象与过滤规则
- Rollback / recovery: 回退自动生成范围，保留人工筛选入口
- Validation:
  - `parse-to-benchmark 测试`
  - `python3 scripts/foreman.py validate F-TASK-040`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 parse-results test-set 生成链路，支持单 parseTaskId 与 batch important/urgent 解析结果生成 benchmark test set，保留只读边界 rejected evidence，并补齐受保护 SQL-optimization client、接口契约和 controller/service 测试。
  - Validation evidence: mvn -pl benchmark-engine -Dtest=BenchmarkParseResultTestSetApplicationServiceTest,BenchmarkTestSetControllerTest test；mvn -pl benchmark-engine test；python3 scripts/foreman.py validate F-TASK-040 --extra-command "mvn -pl benchmark-engine test"；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前 parse batch 自动生成仍只吸纳 important/urgent 统计命中的解析项，且单 parseTaskId 路径缺少 reportCode 维度输入；更宽范围的 recommendation/comparison 编排仍需后续任务补齐。
  - Next step: 进入 F-TASK-041，把 sql-optimization recommendation 链路收口到 comparison benchmark 契约与编排。

### F-TASK-039: 落地批量测试集导入

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(benchmark): import benchmark test sets with row evidence`
- Priority: 1
- Depends on: `F-TASK-038`
- Scope: 从文件导入 test set 与 case 字段映射 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-F / Story `F-STORY-011` 压测模板、测试集与解析联动
- Human confirmation point: 若批量测试集导入会把未校验 SQL、报表或参数直接提升为可信数据，需人工确认
- Data impact: test set 导入记录、批次与成员清单
- Rollback / recovery: 恢复严格校验和失败记录，保留导入 evidence
- Validation:
  - `import 测试`
  - `python3 scripts/foreman.py validate F-TASK-039`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added benchmark test-set import API, domain model, persistence tables, MyBatis mapping, row-level readonly validation, rejected-row evidence retention, and benchmark-engine test coverage for batch-import test sets.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-039 --extra-command "mvn -pl benchmark-engine test"
  - Residual risk: Current benchmark test sets are stored and queryable, but downstream parse-to-benchmark and recommendation-to-benchmark generation flows still arrive in F-TASK-040 and F-TASK-041.
  - Next step: Proceed to F-TASK-040 to generate benchmark test sets from parse results on top of the imported test-set baseline.

### F-TASK-038: 固化压测模板与测试集契约

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(benchmark): solidify template and test-set contract`
- Priority: 1
- Depends on: `F-TASK-037`,`D-TASK-068`
- Scope: 模板类型、阈值、测试集来源与标签模型 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-F / Story `F-STORY-011` 压测模板、测试集与解析联动
- Human confirmation point: 若 benchmark template/test-set 契约会削弱只读、影子环境或阈值边界，需人工确认
- Data impact: 模板/TestSet 数据模型、阈值与来源语义
- Rollback / recovery: 恢复原 benchmark safety 语义，停用高风险模板字段
- Validation:
  - `contract/domain 测试`
  - `python3 scripts/foreman.py validate F-TASK-038`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Solidified benchmark task contract fields for template identity, template type/version, test-set source, labels, and source references; persisted the new contract through DTO/domain/repository/SQL migration layers; aligned implementation-stage reporting with the externalized artifact governance baseline and updated benchmark contract documentation.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-038 --extra-command "mvn -pl benchmark-engine -Dtest=BenchmarkTaskStateFlowTest,BenchmarkTaskModelApplicationServiceTest,BenchmarkTaskControllerTest,MybatisBenchmarkTaskRepositoryTest,BenchmarkPersistenceRecordTest test"
  - Residual risk: This task freezes the benchmark task contract and persistence baseline, but dedicated template/test-set CRUD and import/generate orchestration remain for F-TASK-039 through F-TASK-041.
  - Next step: Proceed to F-TASK-039 to materialize batch test-set import on top of the frozen template/test-set contract.

### F-TASK-037: 落地告警查询与 ACK 接口

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(governance): add alert query and ack api`
- Priority: 1
- Depends on: `F-TASK-036`
- Scope: alert list/detail/ack API 与治理查询面 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-F / Story `E-STORY-012` Dashboard 与告警中心
- Human confirmation point: 若告警查询与 ACK 接口会破坏只读/确认边界、引入跨租户可见性扩大，需人工确认
- Data impact: governance alert list/detail/ack 接口与数据可见范围
- Rollback / recovery: 回退 ACK/查询粒度，恢复最小可见范围
- Validation:
  - `governance alert API 测试`
  - `python3 scripts/foreman.py validate F-TASK-037`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governance alert list/detail/ack APIs on top of the persisted alert event and simulated notification-log baseline, including tenant-scoped filtering, detail hydration, ACK state transition plus audit logging, and a frontend truth-text correction now that the backend controller exists.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-037; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Alert center reads and ACKs are now real repo-side APIs, but policy-write endpoints and frontend controller wiring still remain outside this task, so notify strategy editing and end-user alert-page integration stay simulated until follow-up frontend work lands.
  - Next step: Proceed to the next dependency-ready Phase-F mainline; the alert-center frontend can now switch from derived evidence to the new governance alert APIs when its follow-up task is scheduled.

### F-TASK-036: 落地告警去重与模拟邮件日志

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(governance): persist alert emission logs`
- Priority: 1
- Depends on: `F-TASK-035`
- Scope: dedupe、notify simulated、日志模板与审计留痕 Tech: `JAVA-BE`,`OPS`. Layer: `application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`.
- Matrix context: Phase-F / Story `F-STORY-010` 告警中心与模拟邮件
- Human confirmation point: 若模拟邮件日志会被误写成真实通知、或 dedupe 策略导致关键事件被静默丢弃，需人工确认
- Data impact: 通知日志、dedupe 状态与告警审计链
- Rollback / recovery: 恢复 simulated-only 语义与原始事件保留
- Validation:
  - `notification/dedup 测试`
  - `python3 scripts/foreman.py validate F-TASK-036`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added AlertEmissionApplicationService to turn evaluated alert signals into persisted alert events with tenant-scoped dedupe windows, simulated email notification logs, and governance audit entries; backfilled alert_notification_log schema, MyBatis mappings, and notification/dedup tests for the repo-closed alert center baseline.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-036; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The repository now persists deduped alert emissions and simulated notification evidence, but alert list/detail/ack APIs and frontend consumption still remain for F-TASK-037, and simulated notify stays repo-closed rather than backed by a real mail channel.
  - Next step: Proceed to F-TASK-037 to expose alert list/detail/ack APIs over the persisted alert event and notification-log baseline.

### F-TASK-035: 落地关键事件告警判定

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(governance): add alert rule evaluation`
- Priority: 1
- Depends on: `F-TASK-034`
- Scope: mass failure、service unavailable、report resolve failure、Redis unavailable、dispatch failure 等告警判定 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-F / Story `F-STORY-010` 告警中心与模拟邮件
- Human confirmation point: 若关键事件判定会引入过度噪声、漏报关键故障或把 environment-backed 故障写成 repo 默认事实，需人工确认
- Data impact: 告警判定规则、阈值与事件生成逻辑
- Rollback / recovery: 回退高风险规则，恢复基础关键事件集
- Validation:
  - `alert rule 测试`
  - `python3 scripts/foreman.py validate F-TASK-035`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-27: implemented a repo-closed alert rule evaluation layer that turns mass-failure, datasource/service outage, report-resolve fallback, Redis rule source degradation, dispatch failure/staleness, and audit-write failure signals into normalized `AlertEvent` outputs with baseline policies, dedupe-ready identifiers, and structured evidence payloads.
- Context closeout:
  - Completed scope: Added a repo-closed alert rule evaluation layer that converts mass-failure, datasource/service outage, report-resolve fallback, Redis rule-source degradation, dispatch failure or staleness, and audit-write failure signals into normalized AlertEvent outputs with baseline policy severity, notify defaults, and structured evidence payloads.
  - Validation evidence: mvn -pl governance -Dtest=AlertEventTest,AlertPolicyBaselineTest,AlertSchemaMappingTest,AlertRuleApplicationServiceTest test; python3 scripts/foreman.py validate F-TASK-035
  - Residual risk: The repository can now evaluate critical alert signals into normalized events, but it still does not persist emitted alerts as a deduped history, record simulated notification logs, or expose query/detail/ack APIs; those remain in F-TASK-036 and F-TASK-037.
  - Next step: Proceed to F-TASK-036 to persist deduped alert emissions and simulated notification logs on top of the current alert rule evaluation layer.

### F-TASK-034: 固化告警事件类型与等级模型

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(governance): add alert baseline model`
- Priority: 1
- Depends on: `F-TASK-033`,`D-TASK-062`
- Scope: 定义 alert type、level、dedup key、notify status 与策略模型 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-F / Story `F-STORY-010` 告警中心与模拟邮件
- Human confirmation point: 若告警模型会弱化当前审计与去重边界、删除关键事件等级或改变责任人语义，需人工确认
- Data impact: alert event/policy 数据模型与治理查询面
- Rollback / recovery: 恢复既有告警分类与审计语义，保留新增字段为附加扩展
- Validation:
  - `domain/model 测试`
  - `python3 scripts/foreman.py validate F-TASK-034`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-27: added governance alert domain baselines for event type/level, dedupe key, simulated notify status, and policy defaults; backfilled init schema, incremental migration, and mapper/schema tests for alert policy/event persistence.
- Context closeout:
  - Completed scope: Added governance alert domain baselines for event type/level, dedupe keys, simulated notify status, policy defaults, and alert policy/event persistence scaffolding in init schema plus incremental migration.
  - Validation evidence: mvn -pl governance -Dtest=AlertEventTest,AlertPolicyBaselineTest,AlertSchemaMappingTest test; python3 scripts/foreman.py validate F-TASK-034
  - Residual risk: The repository now fixes alert types, levels, dedupe keys, and simulated notify defaults, but it still does not emit rule-driven alert events, record simulated email logs, or expose alert query/ack APIs; those remain in F-TASK-035 through F-TASK-037.
  - Next step: Proceed to F-TASK-035 to implement alert rule evaluation on top of the persisted alert type/policy baseline.

### U-TASK-004: 前端复盘补漏并恢复规格直达能力

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): restore spec-aligned workspace coverage`
- Priority: 1
- Depends on: `U-TASK-003`,`U-TASK-002`,`U-TASK-001`
- Scope: 以前端与实施规格差距为基线，补齐解析导航直达能力、Dashboard 规格覆盖、相关文档与 contract guard，不把缺失后端能力伪装成已实现事实。 Tech: `VUE-FE`,`DOCS`. Layer: `frontend/router/views/styles/scripts`,`docs`.
- Plan ref: docs/exec-plans/completed/U-TASK-004-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-003` 前后端分离持续治理
- Human confirmation point: 若实现会移除既有承诺路由、把样本化 KPI 写成全租户事实，或把无写 API 的治理页改成伪可写能力，需人工确认。
- Data impact: 前端导航、解析工作区入口、Dashboard 指标表达、规格补充文档与验证脚本；不改写后端业务数据或外部系统状态。
- Rollback / recovery: 回退到当前导航与 Dashboard 表达，保留 read-only、sampled、simulated 等边界文案，不新增对外部环境的强依赖。
- Validation:
  - `python3 scripts/foreman.py validate U-TASK-004`
  - `python3 scripts/foreman.py validate U-TASK-004`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-27: audited `App.vue`, `DashboardView.vue`, `AccelerationView.vue`, product spec, and current contract guards; closed repo-side gaps by restoring explicit parse secondary entries with query-aware navigation, aligning the routing-governance module label with the implementation spec while preserving evidence-first semantics, expanding dashboard sample KPI coverage plus dispatch-coordination status, and adding `docs/product/frontend-retrospective-gap-closure-baseline.md` with matching validation guards. Validation passed via `python3 scripts/foreman.py validate U-TASK-004 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-navigation-shell-contract.mjs' --extra-command 'node scripts/check-parse-workbench-contract.mjs' --extra-command 'node scripts/check-dashboard-contract.mjs' --extra-command 'node scripts/check-frontend-gap-closure-doc.mjs'`.
- Context closeout:
  - Completed scope: Audited the frontend against the SQL governance implementation spec, restored explicit parse secondary entry coverage through query-aware navigation and in-page workspace cards, realigned the routing-governance shell label with the spec while preserving evidence-first semantics, expanded dashboard sample KPIs plus dispatch-coordination visibility, and added a frontend retrospective gap-closure baseline with matching contract guards.
  - Validation evidence: python3 scripts/foreman.py validate U-TASK-004 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-navigation-shell-contract.mjs' --extra-command 'node scripts/check-parse-workbench-contract.mjs' --extra-command 'node scripts/check-dashboard-contract.mjs' --extra-command 'node scripts/check-frontend-gap-closure-doc.mjs'; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Dashboard still relies on current-window samples for success, cache, rewrite, acceleration, and access-channel ratios until dedicated backend aggregates exist.
  - Next step: If backend aggregates or writable governance APIs are added later, replace sample-only homepage metrics and placeholder-only actions with direct contract-backed flows.

### U-TASK-003: 扁平化前端导航并补齐解析历史缺项

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): flatten navigation and backfill parse history`
- Priority: 1
- Depends on: U-TASK-001,E-TASK-018,E-TASK-022,E-TASK-033,E-TASK-037
- Scope: Flatten the frontend sidebar from three levels to module-plus-leaf navigation for SQL history and parse mainline, promote third-level pages into second-level entries, and backfill the parse and history surfaces that regressed during consolidation: add missing parse-statistics dimensions, align history filters with current backend contract, and update routing plus contract checks accordingly. Tech: VUE-FE. Layer: frontend/router/views/styles/scripts.
- Validation:
  - `python3 scripts/foreman.py validate U-TASK-003`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Flattened the SQL history and parsing sidebar modules to module-plus-leaf navigation, restored missing parse-statistics dimensions with sampled severity/priority/logical-object/status views, and aligned history workbenches with the current backend filter and export contracts.
  - Validation evidence: python3 scripts/foreman.py validate U-TASK-003 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-navigation-shell-contract.mjs' --extra-command 'node scripts/check-parse-workbench-contract.mjs' --extra-command 'node scripts/check-statistics-page-contract.mjs' --extra-command 'node scripts/check-history-page-contract.mjs' --extra-command 'node scripts/check-history-detail-contract.mjs'; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The new logical-object and parse-status tabs are sampled frontend aggregations over current history windows because the repository still lacks dedicated backend summary endpoints for tenant-wide logical-object and structure/access-status rollups.
  - Next step: If the backend later exposes first-class parse-statistics endpoints for logical objects or structure-versus-access status, replace the current sampled frontend aggregations with direct contract-backed views and extend the statistics contract again.

### U-TASK-002: 恢复首页总揽与前端合同护栏

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): restore dashboard operator overview`
- Priority: 1
- Depends on: U-TASK-001,E-TASK-029
- Scope: Rebuild the dashboard overview into a complete operator home: restore richer KPI cards, five primary workbench entries, health-and-risk plus next-step sections, and a broader recent-activity slice using existing audited frontend evidence. Strengthen dashboard contract coverage so KPI density, entry completeness, and section completeness regressions fail validation. Tech: VUE-FE. Layer: frontend/router/views/styles/scripts.
- Validation:
  - `python3 scripts/foreman.py validate U-TASK-002`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Rebuilt the dashboard into a fuller operator homepage with richer KPI coverage, five primary workbench entries, a dedicated health-and-risk section, broader recent activity, and recommended next-step actions driven by existing audited evidence surfaces.
  - Validation evidence: python3 scripts/foreman.py validate U-TASK-002 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-dashboard-contract.mjs'; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Global benchmark pass-rate and tenant-wide adoption-rate KPIs remain intentionally absent because the repository still lacks audited global benchmark-task and recommendation-adoption summary contracts; the homepage stays explicit about session-only or sampled evidence where needed.
  - Next step: Instantiate the follow-up navigation and parse-history completion task so sidebar depth, parse-statistics missing dimensions, and history-page gaps can be corrected under a separate audited commit.

### U-TASK-001: 前端三轮复盘与交互重构落地

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): consolidate parse workbench flows`
- Priority: 1
- Depends on: E-TASK-020,E-TASK-025,E-TASK-028,E-TASK-030,E-TASK-031,E-TASK-032
- Scope: Implement the SQLForge frontend information-architecture and interaction refactor: consolidate parse workbench, batch parsing, statistics, and history into one main route; convert routing governance into read-only routing evidence; restore delivery progress in primary navigation with temporary labeling; add real system-management write actions for datasource/report/redis/dispatch create-update surfaces where backend APIs exist; add explicit placeholder dialogs for access and alert actions without write APIs; preserve compatibility redirects for /parse-batches and /parse-statistics; update contracts and validation coverage.
- Validation:
  - `python3 scripts/foreman.py validate U-TASK-001`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-27: refactored the frontend shell and routing so parse workbench became the single parse entry, delivery progress returned to primary navigation, and routing governance shifted to routing-evidence semantics.
  - 2026-04-27: merged single-parse, batch parsing, statistics, and parse-history workflows into the main parse workbench; added real system-management write actions and placeholder capability dialogs on pages without write APIs.
  - 2026-04-27: validation passed via `python3 scripts/foreman.py validate U-TASK-001 --include-task-audit --extra-command "npm run lint" --extra-command "npm run build" --extra-command "node scripts/check-navigation-shell-contract.mjs" --extra-command "node scripts/check-parse-workbench-contract.mjs" --extra-command "node scripts/check-batch-import-contract.mjs" --extra-command "node scripts/check-statistics-page-contract.mjs" --extra-command "node scripts/check-routing-page-contract.mjs" --extra-command "node scripts/check-system-config-contract.mjs" --extra-command "node scripts/check-access-page-contract.mjs" --extra-command "node scripts/check-alert-page-contract.mjs"`; `python3 scripts/task_audit.py --check --phase post-closeout` also passed.
- Context closeout:
  - Completed scope: Consolidated the parse mainline into one workbench route, restored delivery progress navigation, converted routing governance into read-only routing evidence, added real system-management create or update actions where backend APIs exist, and added explicit placeholder dialogs on access or alert surfaces without write APIs.
  - Validation evidence: python3 scripts/foreman.py validate U-TASK-001 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-navigation-shell-contract.mjs' --extra-command 'node scripts/check-parse-workbench-contract.mjs' --extra-command 'node scripts/check-batch-import-contract.mjs' --extra-command 'node scripts/check-statistics-page-contract.mjs' --extra-command 'node scripts/check-routing-page-contract.mjs' --extra-command 'node scripts/check-system-config-contract.mjs' --extra-command 'node scripts/check-access-page-contract.mjs' --extra-command 'node scripts/check-alert-page-contract.mjs'; python3 scripts/task_audit.py --check --phase post-closeout
  - Residual risk: Dispatch policy edit remains a placeholder because the repository exposes create but not update APIs; routing-rule, access-strategy, and alert-rule writes also remain intentionally read-only or placeholder-only until backend contracts exist.
  - Next step: If backend update APIs are added for dispatch or governance policy editing, replace the placeholder dialogs with real editable flows and extend the contract checks accordingly.

### E-TASK-037: 收口前端二次复盘的导航与工作台交互

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): refine navigation and workbench interaction model`
- Priority: 1
- Depends on: E-TASK-036
- Scope: 按二次复盘要求重构前端导航层级、SQL 查询工作台，以及 SQL 历史 / 解析结果中心 / 路由治理 / 系统管理 / 开放接入页面的表格、弹窗和抽屉交互；补齐必要的页面一致性与验证脚本。 Tech: VUE-FE. Layer: frontend/router/views/styles/scripts.
- Validation:
  - `python3 scripts/foreman.py validate E-TASK-037`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reworked the frontend shell and primary governance workbenches around mixed-depth navigation plus filter/table/dialog/drawer patterns; tightened SQL query, SQL history, parse statistics, routing governance, access center, and system management so first-screen focus stays on the operator task instead of inline evidence dumps.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-037 --include-task-audit --extra-command "npm run lint" --extra-command "npm run build" --extra-command "node scripts/check-navigation-shell-contract.mjs" --extra-command "node scripts/check-query-workbench-contract.mjs" --extra-command "node scripts/check-history-page-contract.mjs" --extra-command "node scripts/check-history-detail-contract.mjs" --extra-command "node scripts/check-statistics-page-contract.mjs" --extra-command "node scripts/check-access-page-contract.mjs" --extra-command "node scripts/check-routing-page-contract.mjs" --extra-command "node scripts/check-system-config-contract.mjs"
  - Residual risk: The refactor still relies on repository sample data and existing query-history surfaces, so sparse local datasets can make some tables look thinner than production; audit-event counts also remain absent from query-history page rows until the backend page projection exposes them directly.
  - Next step: If richer governance write-back fixtures are added later, re-run the same contract checks and browser smoke against non-empty datasets to verify density and scanability under production-like states.

### E-TASK-036: 基于联调复盘修正前端页面实现偏差

- Status: done
- Completed at: 2026-04-27
- Commit subject: `fix(frontend): align runtime pages with joint-review findings`
- Priority: 1
- Depends on: E-TASK-035
- Scope: 基于真实前后端联调，对导航、SQL查询、解析、系统管理、开放接入等页面按用户反馈重新复盘，修复与本轮需求不符的交互、信息架构和视觉实现偏差。 Tech: VUE-FE+SPRING. Layer: frontend/backend/runtime.
- Validation:
  - `python3 scripts/foreman.py validate E-TASK-036`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restored missing Element Plus runtime components, rebuilt dark-mode component theming, tightened three-level navigation labels, reduced redundant page hero content, upgraded local governance dev schemas, and fixed query-execution readonly guard so annotated SELECT statements execute successfully in live frontend/backend joint debugging.
  - Validation evidence: npm run lint; npm run build; mvn -pl query-execution -Dtest=QueryExecutionApplicationServiceTest,ReadonlyQueryGuardTest test; python3 scripts/foreman.py validate E-TASK-036; real joint-debug screenshots for sql-query/access/system/parse-batches; live /api/query-execution/queries/execute success with annotated SELECT against mock Hetu.
  - Residual risk: System management and access pages still show sparse datasets in local dev because governance datasource/config sample records are not populated; query-history sample evidence remains thin until a fuller governance write-back dataset is seeded.
  - Next step: Seed richer governance datasource/query-history sample data so system-management and access-audit pages can be reviewed against non-empty production-like states.

### E-TASK-035: 收口治理管理与开放接入页面体验

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): streamline management and access workbenches`
- Priority: 1
- Depends on: E-TASK-034,E-TASK-032
- Scope: 重构 Dashboard、告警中心、路由治理、推荐中心、压测中心、系统管理与开放接入页面，去除无关信息与卡片堆叠，改为概览+列表/表格+抽屉/弹窗的治理工作台模式。 Tech: VUE-FE. Layer: frontend/router/views/styles.
- Validation:
  - `python3 scripts/foreman.py validate E-TASK-035`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 重构系统管理与开放接入页面，改为总览 + 列表 + 抽屉/弹窗的治理工作台，移除主区无关信息堆叠。
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-035 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-access-page-contract.mjs'
  - Residual risk: 系统管理和接入页仍依赖 query-history / governance mock surface，没有新增独立 access-audit controller。
  - Next step: 继续把低频治理页收敛到相同的表格与抽屉语言。

### E-TASK-034: 重构SQL查询与解析中心交互工作流

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): rebuild query and parsing workbench flows`
- Priority: 1
- Depends on: E-TASK-033,E-TASK-022
- Scope: 重构 SQL 查询、解析工作台、批量解析中心与解析结果中心，改为查询条件+结果区+必要弹窗/抽屉模式，补齐单条/多条输入和 drill-through 交互。 Tech: VUE-FE. Layer: frontend/router/views/styles.
- Validation:
  - `python3 scripts/foreman.py validate E-TASK-034`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 重构 SQL 查询、批量解析中心与解析结果中心，统一为条件栏 + 结果区 + 弹窗/抽屉工作流，并补齐多条 SQL 直接输入和统计 drill-down。
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-034 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-query-workbench-contract.mjs' --extra-command 'node scripts/check-batch-import-contract.mjs' --extra-command 'node scripts/check-statistics-page-contract.mjs'
  - Residual risk: 解析工作流里的 datasource/object tree 仍以仓库内模拟对象树承载，没有接实时 catalog。
  - Next step: 将新工作流继续对齐到后续实时 catalog 与 explain 数据源。

### E-TASK-033: 重构导航信息架构与深色主题壳层

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): rebuild dark navigation shell`
- Priority: 1
- Depends on: E-TASK-032,F-TASK-028
- Scope: 将前端路由导航重构为三级动态侧栏树，按产品规格重组一级模块/二三级子页，同时去除用户可见浅色主题切换并把全站默认视觉锁定为 dark-mode-native。 Tech: VUE-FE. Layer: frontend/router/views/styles.
- Validation:
  - `python3 scripts/foreman.py validate E-TASK-033`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 重构 App 壳层为三级动态侧栏，补齐 route meta IA 字段，锁定深色主题默认值，并把高频治理页面的浅色硬编码替换为深色 token。
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-033 --extra-command 'npm run lint' --extra-command 'npm run build'
  - Residual risk: 仍有 DeliveryProgressView 保留极轻量高光渐变，但不影响正式导航主线。
  - Next step: 继续沿新 IA 校对剩余低频页的视觉一致性。

### E-TASK-032: 落地 Redis 规则源、装数协同与系统参数页

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add system config governance contract`
- Priority: 1
- Depends on: `E-TASK-031`,`D-TASK-072`
- Scope: Redis rule source、dispatch policy、系统参数与权限审计展示 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-013` 系统管理与数据源治理页
- Human confirmation point: 若 Redis 规则源、装数协同与系统参数页会把 environment-backed 配置写成默认已启用事实，需人工确认
- Data impact: rule-source、dispatch policy、system-param/permission 展示面
- Rollback / recovery: 恢复到查询/模拟状态展示，保留 simulated 或未联通提示
- Validation:
  - `npm run lint`、`npm run build`、system-management config contract 测试
  - `python3 scripts/foreman.py validate E-TASK-032`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Finalized the system-config governance surface with config contract coverage for Redis rule sources, dispatch policies, tenant parameters, and permission-audit boundaries.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-032 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-system-config-contract.mjs'
  - Residual risk: The page intentionally preserves config-only and simulated wording for environment-backed governance integrations.
  - Next step: No additional tracked frontend task residue remains in the worktree.

### E-TASK-031: 落地系统管理中的数据源与报表接口页

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add system datasource governance page`
- Priority: 1
- Depends on: `E-TASK-024`,`D-TASK-072`
- Scope: datasource 管理、测试连接、报表接口配置与健康状态页 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-013` 系统管理与数据源治理页
- Human confirmation point: 若系统管理数据源/报表接口页会暴露敏感连接信息、误导用户认为真实外部接口已默认联通，需人工确认
- Data impact: 系统管理中的 datasource、health-check、report-interface 展示面
- Rollback / recovery: 恢复脱敏与 mock/config 标识，关闭高风险编辑入口
- Validation:
  - `npm run lint`、`npm run build`、system-management datasource contract 测试
  - `python3 scripts/foreman.py validate E-TASK-031`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added the unified system management page with datasource inventory, connection tests, report-interface visibility, and governance remediation evidence.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-031 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-system-datasource-contract.mjs'
  - Residual risk: Datasource and report-interface management remain read-oriented and avoid exposing raw credentials or implying default external connectivity.
  - Next step: Close out the remaining system config task and verify the worktree is clean.

### E-TASK-030: 落地告警中心与通知状态视图

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add alert center with simulated ack`
- Priority: 1
- Depends on: `E-TASK-029`,`F-TASK-037`
- Scope: 告警列表、详情、ACK、notify simulated 状态展示 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-012` Dashboard 与告警中心
- Human confirmation point: 若告警中心页会把模拟邮件写成真实通知成功、或隐藏 dedupe / ACK 语义，需人工确认
- Data impact: 告警列表、详情、ACK 和通知状态展示
- Rollback / recovery: 恢复 simulated 状态文案与完整事件状态链
- Validation:
  - `npm run lint`、`npm run build`、alert page contract 测试
  - `python3 scripts/foreman.py validate E-TASK-030`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added the alert center with derived alerts, detail drill-down, simulated ACK, and simulated notify status handling.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-030 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-alert-page-contract.mjs'
  - Residual risk: Dedicated backend alert APIs are still absent, so the page remains explicit about derived alerts and frontend-simulated ACK or notify semantics.
  - Next step: Close out system management datasource and config tasks.

### E-TASK-029: 落地 Dashboard KPI、分布与待办区块

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add governance dashboard live overview`
- Priority: 1
- Depends on: `E-TASK-028`,`F-TASK-037`
- Scope: 核心 KPI、问题分布、接入分布与待处理清单卡片 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-012` Dashboard 与告警中心
- Human confirmation point: 若 Dashboard KPI 与待办会聚合不存在的数据、放大 environment-backed 指标权重或引入未审计来源，需人工确认
- Data impact: Dashboard 聚合指标、卡片与待办清单
- Rollback / recovery: 恢复基于治理查询面的 KPI，移除无证据来源聚合
- Validation:
  - `npm run lint`、`npm run build`、dashboard contract 测试
  - `python3 scripts/foreman.py validate E-TASK-029`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Rebuilt the dashboard around audited KPI, issue distribution, access distribution, todo, and activity evidence blocks.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-029 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-dashboard-contract.mjs'
  - Residual risk: The dashboard only reflects sampled query-history windows and current backend evidence availability rather than claiming tenant-wide totals.
  - Next step: Close out alert and system management tasks.

### E-TASK-028: 落地开放接入页与 JDBC Agent / SDK 展示

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add access governance center`
- Priority: 1
- Depends on: `E-TASK-027`,`D-TASK-068`
- Scope: API、JDBC Agent、SDK、接入策略和接入审计展示页 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-011` 压测中心与开放接入页
- Human confirmation point: 若开放接入页会把 JDBC Agent 全模式、SDK 或真实接口联通写成既有事实，需人工确认
- Data impact: 开放接入页、接入策略和文案
- Rollback / recovery: 恢复到契约/规划态展示，明确当前落地阶段
- Validation:
  - `npm run lint`、`npm run build`、access page contract 测试
  - `python3 scripts/foreman.py validate E-TASK-028`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added the open-access governance page with API, JDBC Agent, Java SDK, and access-audit sample views.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-028 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-access-page-contract.mjs'
  - Residual risk: The page still marks dedicated access-audit controller support as absent and relies on query-history samples for audit evidence.
  - Next step: Close out the remaining dashboard, alert, and system tasks.

### E-TASK-027: 落地压测任务、模板、测试集与报告页

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): expand benchmark governance center`
- Priority: 1
- Depends on: `E-TASK-026`,`F-TASK-042`
- Scope: 任务列表、模板详情、测试集、报告对比和回归结果页 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-011` 压测中心与开放接入页
- Human confirmation point: 若压测中心页会把模板/测试集能力写成已默认启用的真实运行时基线、或混淆回归与对比模式，需人工确认
- Data impact: benchmark 页面、模板/TestSet UI 与报告对比面
- Rollback / recovery: 恢复模板/测试集/报告分区，保留模式差异与未实现能力标识
- Validation:
  - `npm run lint`、`npm run build`、benchmark page contract 测试
  - `python3 scripts/foreman.py validate E-TASK-027`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Expanded the benchmark page with template, test-set, session-task, comparison, and regression reporting sections.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-027 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-benchmark-page-contract.mjs'
  - Residual risk: The page stays within current benchmark-engine capabilities and does not claim unsupported template execution paths.
  - Next step: Close out the remaining Phase-E governance pages.

### E-TASK-026: 落地推荐与加速中心页

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add recommendation center`
- Priority: 1
- Depends on: `E-TASK-025`,`D-TASK-062`
- Scope: 推荐分类、详情、收益/风险、dispatch 状态与关联追溯页 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-010` 路由治理与推荐中心
- Human confirmation point: 若推荐中心会把“推荐”误写成“已执行装数”、或隐藏 dispatch 失败状态，需人工确认
- Data impact: 推荐中心、dispatch 状态与说明文案
- Rollback / recovery: 恢复 recommendation / dispatch 分离展示，保留失败/待拉取状态
- Validation:
  - `npm run lint`、`npm run build`、recommendation page contract 测试
  - `python3 scripts/foreman.py validate E-TASK-026`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added the recommendation center with recommendation categories, benefit/risk detail, dispatch evidence, and traceability views.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-026 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-recommendation-page-contract.mjs'
  - Residual risk: The page remains evidence-driven and does not execute recommended SQL or perform external dispatch from the browser.
  - Next step: Close out the remaining Phase-E governance pages.

### E-TASK-025: 落地路由治理页与历史决策详情

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add routing governance page`
- Priority: 1
- Depends on: `E-TASK-024`,`D-TASK-061`
- Scope: 当前规则、决策样例、历史记录、注释协议说明与路由详情页 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-010` 路由治理与推荐中心
- Human confirmation point: 若路由治理页会暴露内部策略细节、误导用户把 environment-backed 证据写成默认事实，需人工确认
- Data impact: 路由规则、决策详情与说明文案
- Rollback / recovery: 回退高风险字段，恢复基于仓库真值的路由展示
- Validation:
  - `npm run lint`、`npm run build`、routing page contract 测试
  - `python3 scripts/foreman.py validate E-TASK-025`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added the routing governance page with route-calibration, trace/history evidence, and comment protocol guidance.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-025 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-routing-page-contract.mjs'
  - Residual risk: The page remains read-only and depends on backend evidence availability; no routing-rule editing surface is exposed.
  - Next step: Close out the remaining governance pages in Phase E.

### E-TASK-024: 落地逻辑视图映射、数据到位与热度视图

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): extend logical object evidence view`
- Priority: 1
- Depends on: `E-TASK-023`,`D-TASK-071`
- Scope: 逻辑对象映射、freshness/SLA/usage heat 与相关 SQL 展示 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-009` 数据资产与逻辑视图
- Human confirmation point: 若逻辑视图映射页会把数据到位状态、SLA 或热度写成确定事实而无证据来源，需人工确认
- Data impact: 逻辑对象映射、freshness/SLA/heat 展示
- Rollback / recovery: 恢复字段证据标识与默认未知状态
- Validation:
  - `npm run lint`、`npm run build`、logical object contract 测试
  - `python3 scripts/foreman.py validate E-TASK-024`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Extended the asset catalog with freshness and SLA evidence cards, usage-heat proxy rendering, logical-object mapping emphasis, and related SQL candidates aligned from parse statistics.
  - Validation evidence: npm run lint; npm run build; node scripts/check-logical-object-contract.mjs; python3 scripts/foreman.py validate E-TASK-024 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-logical-object-contract.mjs'
  - Residual risk: Usage heat is intentionally labeled as an evidence-derived proxy because the repo-side baseline does not expose a dedicated live heat endpoint yet; related SQL is only aligned for logical views via reportCode.
  - Next step: Continue with E-TASK-025 to build the routing governance page and decision detail flow on top of route and history contracts.

### E-TASK-023: 落地数据资产目录与对象详情页

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add data asset catalog view`
- Priority: 1
- Depends on: `E-TASK-022`,`D-TASK-071`
- Scope: datasource/schema/table/logical-view/db-view 列表与详情页 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-009` 数据资产与逻辑视图
- Human confirmation point: 若数据资产页会混淆业务逻辑视图与 DB View、暴露未授权对象详情，需人工确认
- Data impact: 资产目录、对象详情、导航结构
- Rollback / recovery: 恢复对象类型区分与权限控制，关闭高风险详情区域
- Validation:
  - `npm run lint`、`npm run build`、asset page contract 测试
  - `python3 scripts/foreman.py validate E-TASK-023`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added a dedicated data asset catalog route with datasource, schema, table, logical-view, and db-view lists plus detail panels and metadata snapshot evidence.
  - Validation evidence: npm run lint; npm run build; node scripts/check-asset-page-contract.mjs; python3 scripts/foreman.py validate E-TASK-023 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-asset-page-contract.mjs'
  - Residual risk: Schema-level evidence currently falls back to datasource-scoped metadata snapshots because the repo-side snapshot query surface does not expose schemaName filters yet.
  - Next step: Instantiate E-TASK-024 to extend the asset experience with logical mappings, freshness/SLA emphasis, usage heat signals, and related SQL context.

### E-TASK-022: 落地解析结果中心与优先级矩阵

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add parse statistics center`
- Priority: 1
- Depends on: `E-TASK-021`,`D-TASK-058`
- Scope: 解析统计、问题分布、priority matrix、important/urgent 清单 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-008` 解析工作台与批量解析中心
- Human confirmation point: 若解析统计页会改变 severity/priority 口径、隐藏 important/urgent 判定依据，需人工确认
- Data impact: 统计图表、矩阵与 drill-through 页
- Rollback / recovery: 恢复既定统计口径与标签，保留新增展示为附加视图
- Validation:
  - `npm run lint`、`npm run build`、statistics page contract 测试
  - `python3 scripts/foreman.py validate E-TASK-022`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added a dedicated parse-statistics route that consumes overview, issue-scene, by-sql, by-report, priority-matrix, and important-urgent endpoints to render KPI cards, issue distribution, and urgency matrices.
  - Validation evidence: npm run lint; npm run build; node scripts/check-statistics-page-contract.mjs; python3 scripts/foreman.py validate E-TASK-022 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-statistics-page-contract.mjs'
  - Residual risk: The page currently projects backend aggregates read-only; drill-through from matrix cells to history or batch detail is still deferred to later frontend tasks.
  - Next step: Continue with E-TASK-023 to build the data asset catalog and object detail views on top of governance metadata contracts.

### E-TASK-021: 落地批量解析中心与报表清单导入页

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add batch parse and report import center`
- Priority: 1
- Depends on: `E-TASK-020`,`D-TASK-054`
- Scope: 模板下载、上传、批次列表、批次详情与失败记录展示 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-008` 解析工作台与批量解析中心
- Human confirmation point: 若批量解析页会把兼容格式失败误写成产品故障、或把 mock 报表清单写成真实接口联通，需人工确认
- Data impact: 批次页、导入模板、报表清单 UI 语义
- Rollback / recovery: 保留稳定格式优先与 mock 标识，回退高风险文案/行为
- Validation:
  - `npm run lint`、`npm run build`、batch import contract 测试
  - `python3 scripts/foreman.py validate E-TASK-021`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added a dedicated batch parse center route with parse-batch creation, template download, upload/ingest, retry-access, report-catalog import, resolve-sqls, and session-local batch detail views.
  - Validation evidence: npm run lint; npm run build; node scripts/check-batch-import-contract.mjs; python3 scripts/foreman.py validate E-TASK-021 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-batch-import-contract.mjs'
  - Residual risk: The UI keeps a session-local list of created/imported batches because the repo-side baseline exposes detail endpoints but no dedicated list endpoint yet.
  - Next step: Instantiate E-TASK-022 to build the parse-statistics center and priority matrix on top of parse-statistics contracts.

### E-TASK-020: 落地解析工作台双卡结果布局

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add parse workbench dual-card layout`
- Priority: 1
- Depends on: `E-TASK-019`,`D-TASK-045`
- Scope: 单条 SQL 解析输入、结构解析卡、access parse 卡和综合结论 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-008` 解析工作台与批量解析中心
- Human confirmation point: 若解析工作台把 access parse 不可用伪装成结构解析成功、或在前端合并双轨语义导致用户误解，需人工确认
- Data impact: 解析工作台页面状态、双卡展示与提示文案
- Rollback / recovery: 恢复结构/访问解析分开展示与 unavailable 提示
- Validation:
  - `npm run lint`、`npm run build`、parse workbench contract 测试
  - `python3 scripts/foreman.py validate E-TASK-020`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reworked the acceleration route into a parse workbench backed by structure/combined parse APIs, with single-SQL input, combined conclusion, structure/access dual cards, state history, and structure-only preview.
  - Validation evidence: npm run lint; npm run build; node scripts/check-parse-workbench-contract.mjs; python3 scripts/foreman.py validate E-TASK-020 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-parse-workbench-contract.mjs'
  - Residual risk: The parse workbench currently visualizes parse-batch and benchmark journeys elsewhere; this page focuses on single-SQL parse contracts only.
  - Next step: Instantiate E-TASK-021 to build the batch-parse center and report-catalog import workflow on top of parse-batches contracts.

### E-TASK-019: 落地 SQL 历史详情与取证视图

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add sql history forensic detail view`
- Priority: 1
- Depends on: `E-TASK-018`,`D-TASK-041`
- Scope: SQL 三态、注释上下文、结构/访问解析、route/recommendation/alert/benchmark 关联取证视图 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-007` SQL 查询与历史前端增强
- Human confirmation point: 若取证详情会暴露未脱敏参数、内部错误栈或隐藏部分失败证据，需人工确认
- Data impact: 历史详情页、SQL 三态和关联取证展示
- Rollback / recovery: 恢复脱敏与失败证据显示边界，关闭高风险详情块
- Validation:
  - `npm run lint`、`npm run build`、detail page contract 测试
  - `python3 scripts/foreman.py validate E-TASK-019`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Connected parse-record history detail to query-history for SQL tri-state, parse summaries, route/cache/binding evidence, and related recommendation/benchmark/alert/audit references.
  - Validation evidence: npm run lint; npm run build; node scripts/check-history-detail-contract.mjs; python3 scripts/foreman.py validate E-TASK-019 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-history-detail-contract.mjs'
  - Residual risk: The page renders backend forensic payloads as formatted JSON blocks; future backend shape changes will need matching frontend grouping updates.
  - Next step: Instantiate E-TASK-020 to build the parse workbench dual-card result layout on top of the detail/forensics baseline.

### E-TASK-018: 落地 SQL 历史列表筛选与分类面

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add history page filters and sorting`
- Priority: 1
- Depends on: `E-TASK-017`,`D-TASK-040`
- Scope: 历史过滤、分类、排序、分页与列表列渲染 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-007` SQL 查询与历史前端增强
- Human confirmation point: 若历史列表筛选会暴露未授权字段、跨租户可见数据或破坏分页性能边界，需人工确认
- Data impact: 历史列表、筛选状态与前端缓存态
- Rollback / recovery: 回退敏感筛选/列，恢复基础列表视图与分页
- Validation:
  - `npm run lint`、`npm run build`、history page contract 测试
  - `python3 scripts/foreman.py validate E-TASK-018`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Extended the SQL history page with explicit history classification filters, sort modes, and richer lookup-state display while preserving existing lookup, pagination, and trace-detail drill-through behavior.
  - Validation evidence: npm run lint; npm run build; node scripts/check-history-page-contract.mjs; python3 scripts/foreman.py validate E-TASK-018 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-history-page-contract.mjs'
  - Residual risk: The history page now exposes filtering and sorting, but the deeper forensic SQL tri-state and related-object drill-through views still belong to E-TASK-019.
  - Next step: Proceed to E-TASK-019 to build the detailed SQL tri-state, comment context, structure/access parse, and related route/recommendation/alert/benchmark forensics view.

### E-TASK-017: 扩展 SQL 查询工作台三栏布局与执行摘要

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): redesign sql query workbench`
- Priority: 1
- Depends on: `E-TASK-016`,`D-TASK-039`
- Scope: 数据源树、SQL 编辑器、参数输入、右侧治理摘要与结果页签，消费查询执行扩展契约 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-007` SQL 查询与历史前端增强
- Human confirmation point: 若查询工作台增强会在前端复刻后端权威逻辑、引入越权字段展示或改变既有执行入口语义，需人工确认
- Data impact: 查询页布局、状态编排与前端消费字段
- Rollback / recovery: 保留后端权威，回退高风险前端判断逻辑，仅保留展示/编排层
- Validation:
  - `npm run lint`、`npm run build`、frontend contract 测试
  - `python3 scripts/foreman.py validate E-TASK-017`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reworked the SQL query page into a three-rail workbench with datasource tree context, SQL editor plus parameter inputs, and right-side governance summary with result tabs, while preserving live query execution and degraded recovery evidence flows.
  - Validation evidence: npm run lint; npm run build; node scripts/check-query-workbench-contract.mjs; python3 scripts/foreman.py validate E-TASK-017 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-query-workbench-contract.mjs'
  - Residual risk: The page still relies on static datasource tree context and does not yet implement the downstream history list/detail surfaces that arrive in E-TASK-018 and E-TASK-019.
  - Next step: Proceed to E-TASK-018 to build the SQL history filter, classification, sorting, and pagination view against the history backend contracts.

### D-TASK-066: 扩展 JDBC Agent `Governed Execute`

- Status: done
- Completed at: 2026-04-27
- Commit subject: `test(jdbc-agent): add governed execute coverage`
- Priority: 1
- Depends on: `D-TASK-065`
- Scope: 通过平台 API 执行 SQL，并保留 fallback 语义 Tech: `JAVA-BE`,`OPS`. Layer: `common`,`deployments/ci/scripts`.
- Matrix context: Phase-D / Story `D-STORY-012` 开放接入与 JDBC Agent
- Human confirmation point: 若 JDBC Agent `Governed Execute` 会在平台不可用时无回退策略、或默认强制所有 SQL 走平台，需人工确认
- Data impact: Agent 执行模式、fallback 策略、平台调用链
- Rollback / recovery: 恢复租户/数据源级可切换边界和 fallback 语义
- Validation:
  - `governed-execute 测试`
  - `python3 scripts/foreman.py validate D-TASK-066`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added dedicated governed-execute validation coverage for JDBC Agent platform execution, direct-JDBC fallback, and fail-closed behavior, plus a focused validation script for the governed-execute contract.
  - Validation evidence: bash scripts/run-jdbc-agent-governed-execute-tests.sh; python3 scripts/foreman.py validate D-TASK-066 --extra-command 'bash scripts/run-jdbc-agent-governed-execute-tests.sh'
  - Residual risk: The shared JDBC agent implementation was introduced earlier together with local-rewrite support, so this task closes with focused governed-execute contract coverage rather than a fresh codepath split.
  - Next step: Rebind to the next active implementation-spec task after D-STORY-012, because the current tasks.md queue is now cleared for JDBC Agent and Java SDK follow-ups.

### D-TASK-065: 落地 JDBC Agent 首版 `Observe`

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(jdbc-agent): harden observe-only coverage`
- Priority: 1
- Depends on: `D-TASK-064`
- Scope: JAR 采集 SQL、注释解析、上报 access audit，不接管执行 Tech: `JAVA-BE`,`OPS`. Layer: `common`,`deployments/ci/scripts`.
- Matrix context: Phase-D / Story `D-STORY-012` 开放接入与 JDBC Agent
- Human confirmation point: 若 JDBC Agent `Observe` 会接管执行、写入敏感信息或在规则源失败时影响业务查询，需人工确认
- Data impact: Agent JAR、采集上报、access audit 与 Redis 依赖
- Rollback / recovery: 恢复 observe-only 语义，禁用高风险上报或敏感字段透出
- Validation:
  - `JDBC agent sample/integration 测试`
  - `python3 scripts/foreman.py validate D-TASK-065`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Hardened JDBC Agent observe-mode comment parsing to ignore malformed leading comments, and added dedicated observe-only tests plus a focused validation script for audit reporting, fail-open behavior, and non-takeover execution.
  - Validation evidence: bash scripts/run-jdbc-agent-observe-tests.sh; python3 scripts/foreman.py validate D-TASK-065 --extra-command 'bash scripts/run-jdbc-agent-observe-tests.sh'
  - Residual risk: The shared JDBC agent baseline for governed execution and local rewrite still lives in the common agent implementation, and governed-execute closeout remains pending under D-TASK-066.
  - Next step: Close out D-TASK-066 with its dedicated governed-execute validation coverage, then resume the remaining implementation-spec tasks.

### D-TASK-068: 落地 Java SDK 首版

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(open-access): add java sdk client baseline`
- Priority: 1
- Depends on: `D-TASK-067`
- Scope: 提供鉴权、trace/requestId、typed client 与 retry 基线 Tech: `JAVA-BE`,`OPS`. Layer: `common`,`deployments/ci/scripts`.
- Matrix context: Phase-D / Story `D-STORY-012` 开放接入与 JDBC Agent
- Human confirmation point: 若 Java SDK 会把未稳定契约写成强依赖、绕过统一 request/trace 语义或暴露敏感配置，需人工确认
- Data impact: SDK client、配置、请求重试与接入文档
- Rollback / recovery: 回退 SDK 到最小 typed client 基线，并保留 HTTP API 主路径
- Validation:
  - `SDK 测试`
  - `python3 scripts/foreman.py validate D-TASK-068`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added the Java SDK typed client wrapper that forces SDK access-channel context, delegates query execution, and writes governance audit summaries for success and failure paths.
  - Validation evidence: mvn -pl sqlforge-shared -am clean -Dtest=SqlForgeJavaSdkClientTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-068
  - Residual risk: The SDK baseline is still a thin wrapper over the HTTP query execution and audit clients, so future tasks may still expand configuration surfacing or richer typed APIs without changing this audited request path.
  - Next step: Resume D-TASK-065 and D-TASK-066 follow-up work, then continue the remaining implementation-spec tasks beyond the current workspace residue.

### D-TASK-070: 建立 `MetadataSnapshot` 与数据到位状态模型

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(governance): add metadata snapshot baseline`
- Priority: 1
- Depends on: `D-TASK-069`
- Scope: metadata snapshot、freshness、SLA、upstream/downstream/queryability 的模型与追溯键 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-013` 数据源与数据资产治理增强
- Human confirmation point: 若 metadata snapshot / freshness / SLA / upstream-downstream 状态会把无证据数据写成确定事实，需人工确认
- Data impact: metadata snapshot、freshness/SLA/queryability/upstream/downstream 追溯面
- Rollback / recovery: 恢复未知/未采集默认语义，保留证据来源与回退字段
- Validation:
  - `metadata model 与 snapshot query 测试`
  - `python3 scripts/foreman.py validate D-TASK-070`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added metadata snapshot query APIs, lineage/evidence response models, in-memory snapshot repository, and default UNKNOWN or UNCOLLECTED status semantics for governance metadata evidence.
  - Validation evidence: mvn -pl governance -am clean -Dtest=MetadataSnapshotApplicationServiceTest,MetadataSnapshotControllerTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-070
  - Residual risk: Snapshot evidence remains baseline repository data rather than live collection jobs, and the Java SDK baseline under D-TASK-068 is still open.
  - Next step: Finish the Java SDK baseline under D-TASK-068 and then continue the remaining open-access follow-up tasks.

### D-TASK-069: 固化数据源连接配置与健康检查契约

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(governance): add datasource config baseline`
- Priority: 1
- Depends on: `D-TASK-049`
- Scope: JDBC/API/Client/Gateway 连接方式、凭证、安全、测试连接、健康状态与失败原因契约 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-013` 数据源与数据资产治理增强
- Human confirmation point: 若数据源连接配置与健康检查会落明文凭据、放宽租户隔离或把环境级 endpoint/secret 写入仓库真值，需人工确认
- Data impact: datasource 配置、测试连接、健康状态与失败原因查询面
- Rollback / recovery: 回退到只读 datasource 查询基线，移除高风险配置字段与敏感信息暴露
- Validation:
  - `datasource contract 与 health-check 测试`
  - `python3 scripts/foreman.py validate D-TASK-069`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added datasource configuration CRUD, connection-mode specific contract fields, masked credential handling, and baseline connection health-check responses for governance APIs.
  - Validation evidence: mvn -pl governance -am clean -Dtest=DatasourceConfigApplicationServiceTest,DatasourceConfigControllerTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-069
  - Residual risk: Health-check behavior is simulated baseline logic without live external connectivity; metadata snapshot and asset evidence remain governed separately under D-TASK-070 and D-TASK-071.
  - Next step: Close out D-TASK-070 metadata snapshot baselines, then finish the Java SDK baseline under D-TASK-068.

### D-TASK-071: 落地数据资产与数据源治理查询/详情接口

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(governance): add metadata asset catalog endpoints`
- Priority: 1
- Depends on: `D-TASK-070`
- Scope: datasource/schema/table/logical-view/db-view 列表、详情与 metadata snapshot 查询面 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-013` 数据源与数据资产治理增强
- Human confirmation point: 若数据资产接口会扩大跨租户可见范围、暴露未授权对象详情或破坏现有查询性能边界，需人工确认
- Data impact: datasource/schema/table/logical-view/db-view 查询面与详情接口
- Rollback / recovery: 回退高风险详情字段与筛选面，恢复基础受保护查询
- Validation:
  - `data-asset API 与 detail query 测试`
  - `python3 scripts/foreman.py validate D-TASK-071`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added schema/table catalog and detail endpoints, and enriched logical-view/db-view responses with metadata snapshot evidence fields under the governance baseline.
  - Validation evidence: mvn -pl governance -am clean -Dtest=MetadataAssetCatalogApplicationServiceTest,MetadataAssetCatalogControllerTest,LogicalViewCatalogApplicationServiceTest,DatabaseViewCatalogApplicationServiceTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-071
  - Residual risk: Datasource configuration and standalone metadata snapshot ledgers remain open under D-TASK-069 and D-TASK-070; current asset evidence is baseline in-memory data rather than live external collection.
  - Next step: Close out D-TASK-069 and D-TASK-070, then finish the Java SDK baseline under D-TASK-068.

### D-TASK-072: 落地系统管理配置接口基线

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(governance): add system management config baselines`
- Priority: 1
- Depends on: `D-TASK-071`
- Scope: 报表接口配置、Redis 规则源、装数协同策略与相关治理查询接口基线 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-013` 数据源与数据资产治理增强
- Human confirmation point: 若系统管理配置接口会把 mock/config abstraction 误写成真实外部联通、或允许未经审批的配置生效，需人工确认
- Data impact: 报表接口配置、Redis 规则源、装数协同策略与治理查询面
- Rollback / recovery: 回退到查询/模拟基线，保留抽象配置但禁用高风险生效路径
- Validation:
  - `system-management config/query 测试`
  - `python3 scripts/foreman.py validate D-TASK-072`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governed system-management configuration baselines for report interfaces, Redis rule sources, and dispatch policies, including alias endpoints, update support, config-only enforcement semantics, and focused service/controller tests without claiming live external activation.
  - Validation evidence: mvn -pl governance -am clean -Dtest=ReportInterfaceConfigApplicationServiceTest,ReportInterfaceConfigControllerTest,RedisRuleSourceApplicationServiceTest,DispatchPolicyApplicationServiceTest,SystemManagementConfigControllerTest,DatasourceConfigApplicationServiceTest,MetadataSnapshotApplicationServiceTest,MetadataAssetCatalogApplicationServiceTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-072; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Datasource/metadata governance tasks remain active in the workspace, and Redis/dispatch paths intentionally stay at config-only or simulated status until later environment-backed integration work.
  - Next step: Close out the remaining datasource/metadata governance tasks separately, then resume the open-access SDK delivery under D-TASK-068.

### D-TASK-067: 扩展 JDBC Agent `Local Rewrite + Direct JDBC`

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(jdbc-agent): add local rewrite direct jdbc mode`
- Priority: 1
- Depends on: `D-TASK-066`
- Scope: 本地轻量改写/路由后直连目标 JDBC，保留审计与失败回退 Tech: `JAVA-BE`,`OPS`. Layer: `common`,`deployments/ci/scripts`.
- Matrix context: Phase-D / Story `D-STORY-012` 开放接入与 JDBC Agent
- Human confirmation point: 若 JDBC Agent `Local Rewrite + Direct JDBC` 会静默改写 SQL、绕过审计或改变查询语义，需人工确认
- Data impact: 本地改写规则、direct JDBC 路径与上报链
- Rollback / recovery: 回退为原 SQL 或 observe-only，保留改写失败记录
- Validation:
  - `local rewrite/direct JDBC 测试`
  - `python3 scripts/foreman.py validate D-TASK-067`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added JDBC Agent shared runtime for observe/governed/local-rewrite flows, including SQL comment/query-date observation, Redis-backed lightweight rewrite routing, direct JDBC fallback semantics, shared open-access HTTP clients, and focused JDBC agent tests/script for the local rewrite + direct JDBC baseline.
  - Validation evidence: scripts/run-open-access-tests.sh; python3 scripts/foreman.py validate D-TASK-067; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Redis-backed rewrite rules remain intentionally lightweight and repo-closed; real driver weaving and live datasource integration still require later environment-backed verification.
  - Next step: Close out D-TASK-065/D-TASK-066 against the shared agent baseline, then finish the Java SDK delivery under D-TASK-068.

### D-TASK-064: 落地 HTTP API 接入基线

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(api): baseline protected access-channel handling`
- Priority: 1
- Depends on: `D-TASK-063`
- Scope: 对外 query/parse/history/recommendation API 入口基线 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-012` 开放接入与 JDBC Agent
- Human confirmation point: 若 HTTP API 接入会绕过统一鉴权/审计、扩大对外暴露面或删改既有契约，需人工确认
- Data impact: 外部 API、认证上下文、审计记录与错误响应
- Rollback / recovery: 回退对外入口到受保护最小基线，并保留现有内部契约
- Validation:
  - `API integration 测试`
  - `python3 scripts/foreman.py validate D-TASK-064`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Extended HTTP API baseline so protected query/optimization entrypoints capture accessChannel/authSource in governance request payloads, and governance query-history normalizes accessChannel filters for external API callers.
  - Validation evidence: mvn -pl governance -am -Dtest=GovernanceAuditTrailServiceTest,GovernanceQueryHistoryControllerTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test; mvn -pl query-execution -am -Dtest=QueryExecutionControllerTest,GovernanceHttpClientTest,QueryExecutionApplicationServiceTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test; mvn -pl sql-optimization -am -Dtest=StructureParseControllerTest,AccelerationRecommendationControllerTest,GovernanceHttpClientTest,OptimizationTaskApplicationServiceTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-064
  - Residual risk: Recommendation list/detail APIs are protected and baseline-compatible, but richer access policy/audit query endpoints remain for later tasks.
  - Next step: Proceed to D-TASK-065 for the JDBC Agent Observe delivery after the protected HTTP API baseline is stable.

### D-TASK-063: 固化接入来源模型与统一审计契约

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(governance): codify access audit channel contract`
- Priority: 1
- Depends on: `D-TASK-062`
- Scope: `PAGE/API/JDBC_AGENT/SDK/CLIENT` 模型与 access audit 字段统一 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-012` 开放接入与 JDBC Agent
- Human confirmation point: 若接入来源模型会让未受管入口绕过审计或混淆真实访问来源，需人工确认
- Data impact: access channel、access audit 与相关 headers/metadata
- Rollback / recovery: 恢复显式来源分类与统一审计，关闭不明来源入口
- Validation:
  - `access contract 测试`
  - `python3 scripts/foreman.py validate D-TASK-063`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added shared access channel model, propagated X-Access-Channel through protected governance calls, and enforced canonical accessChannel handling in governance audit writes.
  - Validation evidence: mvn -pl governance -am -Dtest=GovernanceAuditTrailServiceTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test; mvn -pl query-execution,sql-optimization -am -DskipTests compile; python3 scripts/foreman.py validate D-TASK-063
  - Residual risk: Upstream entrypoints still need broader adoption of X-Access-Channel headers to distinguish PAGE/JDBC/SDK/CLIENT beyond the API fallback path.
  - Next step: Proceed to D-TASK-064 to wire HTTP API baseline around the shared access audit contract.

### D-TASK-062: 固化“只管理不装数”的协同契约

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): codify dispatch collaboration contract`
- Priority: 1
- Depends on: `D-TASK-061`
- Scope: 外部拉取事件、非主动装数、回执与审计边界；不接真实装数执行 Tech: `JAVA-BE`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-011` 推荐 SQL、加速建议与装数协同事件
- Human confirmation point: 若“只管理不装数”契约被扩展成直接执行装数、主动推送生产消息或默认联通外部模块，需人工确认
- Data impact: recommendation 协同契约、dispatch 行为、文档真值
- Rollback / recovery: 恢复 pull-only 与非执行边界，保留协同事件审计
- Validation:
  - `dispatch contract 测试`
  - `python3 scripts/foreman.py validate D-TASK-062`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented dispatch collaboration contract endpoint and service surface enforcing PULL_ONLY semantics, no SQL execution, no data loading, no active external push, and external pull requirement; updated controller/service tests plus interface and product documentation.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=DispatchEventApplicationServiceTest,DispatchEventControllerTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-062; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: System-management configuration for dispatch policy surfaces is scheduled under D-TASK-072; this task fixes the runtime collaboration boundary.
  - Next step: Instantiate D-TASK-063 to codify access source model and unified audit contract.

### D-TASK-061: 打通推荐与历史/解析/路由的关联追溯

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add recommendation traceability`
- Priority: 1
- Depends on: `D-TASK-060`
- Scope: recommendation 与 history/parse/route/alert/batch 的 traceability keys Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-011` 推荐 SQL、加速建议与装数协同事件
- Human confirmation point: 若推荐关联追溯会跨租户串链、暴露不应展示的 route/parse/history 关系，需人工确认
- Data impact: recommendation trace keys、治理查询面与关联视图
- Rollback / recovery: 回退跨链关联字段，恢复受保护的最小追溯面
- Validation:
  - `traceability 测试`
  - `python3 scripts/foreman.py validate D-TASK-061`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented recommendation traceability keys for history, parse task, batch, route decision, alert, SQL fingerprint, report, and logical object references; added tenant-scoped recommendation trace endpoint with related dispatch events; updated mapper/schema/migration/docs and traceability tests.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=AccelerationRecommendationApplicationServiceTest,RecommendationTraceApplicationServiceTest,RecommendationTraceControllerTest,ParseBatchPersistenceSchemaMappingTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-061; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: D-TASK-062 will formalize the non-loading dispatch collaboration contract; this task returns reference keys only and does not hydrate external service details.
  - Next step: Instantiate D-TASK-062 to document and enforce the pull-only, non-executing dispatch contract.

### D-TASK-060: 落地推荐治理事件创建与状态机

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add dispatch event state machine`
- Priority: 1
- Depends on: `D-TASK-059`
- Scope: `DispatchEvent` create/publish/pull/ack/fail 状态机与审计链 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-011` 推荐 SQL、加速建议与装数协同事件
- Human confirmation point: 若治理事件状态机会绕过外部拉取模式、自动推送真实装数、或删除失败/待拉取状态，需人工确认
- Data impact: dispatch event、状态机、审计与回执链
- Rollback / recovery: 恢复 pull-based 协同边界，保留全部事件状态证据
- Validation:
  - `event state 测试`
  - `python3 scripts/foreman.py validate D-TASK-060`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented pull-based DispatchEvent lifecycle for recommendation dispatch, including CREATED/PUBLISHED/PULLED/ACKED/FAILED state transitions, recommendation dispatch endpoint, event list/detail and pull/ack/fail endpoints, in-memory and database repository baselines, SQL schema/migration, interface/data-model documentation, and focused tests.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=DispatchEventApplicationServiceTest,DispatchEventControllerTest,ParseBatchPersistenceSchemaMappingTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-060; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: D-TASK-061 will add cross-object trace keys and history/parse/route linkage; this task only covers dispatch event lifecycle and pull-based state semantics.
  - Next step: Instantiate D-TASK-061 to connect recommendation traceability with history, parse, route, alert, and batch surfaces.

### D-TASK-059: 扩展推荐对象类型与收益/风险模型

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add recommendation benefit risk model`
- Priority: 1
- Depends on: `D-TASK-058`
- Scope: 建模 `REWRITE/ACCELERATION/CREATE_TABLE/PREWARM/MAINTENANCE` 推荐类型与收益/风险字段 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-011` 推荐 SQL、加速建议与装数协同事件
- Human confirmation point: 若推荐对象扩展会把“建议”写成“已执行结果”、或削弱收益/风险边界，需人工确认
- Data impact: recommendation 对象、类型、收益/风险与状态字段
- Rollback / recovery: 恢复 recommendation 只读建议语义，保留新增字段为未执行状态
- Validation:
  - `recommendation domain 测试`
  - `python3 scripts/foreman.py validate D-TASK-059`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented read-only acceleration recommendation modeling with REWRITE/ACCELERATION/CREATE_TABLE/PREWARM/MAINTENANCE types, benefit/risk/status fields, tenant-scoped list/detail APIs, in-memory and database repository baselines, SQL schema/migration, interface/data-model documentation, and focused tests.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=AccelerationRecommendationApplicationServiceTest,AccelerationRecommendationControllerTest,ParseBatchPersistenceSchemaMappingTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-059; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Dispatch event state machine and external pull coordination start in D-TASK-060; recommendation objects remain advisory and non-executing.
  - Next step: Instantiate D-TASK-060 to implement DispatchEvent creation and lifecycle state semantics.

### D-TASK-058: 落地重要/紧急清单与优先级矩阵

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add parse priority matrix`
- Priority: 1
- Depends on: `D-TASK-057`
- Scope: priority matrix、important/urgent list 与 drill-through Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-010` 解析统计与优先级分层
- Human confirmation point: 若重要/紧急矩阵会隐藏判定依据或用于替代原始 issue 结果，需人工确认
- Data impact: priority matrix、important/urgent 视图与排序逻辑
- Rollback / recovery: 恢复 issue 原始结果优先，矩阵仅作为派生视图
- Validation:
  - `matrix/list 测试`
  - `python3 scripts/foreman.py validate D-TASK-058`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented tenant-scoped parse priority matrix and important/urgent drill-through surfaces, including urgency buckets, SQL/issue/report counts, controller endpoints, tests, and interface documentation.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=ParseStatisticsApplicationServiceTest,ParseStatisticsControllerTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-058; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Recommendation domain modeling starts in D-TASK-059; this task only exposes parse-statistics drill-through.
  - Next step: Instantiate D-TASK-059 to extend recommendation object types and benefit/risk model.

### D-TASK-057: 落地按报表统计与占比分析

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add report parse statistics`
- Priority: 1
- Depends on: `D-TASK-056`
- Scope: report dimension aggregation、影响报表数量与占比计算 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-010` 解析统计与优先级分层
- Human confirmation point: 若按报表统计会放大不可靠 mock 数据、或把失败解析也计入成功占比，需人工确认
- Data impact: 报表聚合、占比计算与报表问题清单
- Rollback / recovery: 恢复成功/失败分层与 mock 标识，纠正聚合口径
- Validation:
  - `report aggregation 测试`
  - `python3 scripts/foreman.py validate D-TASK-057`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented by-report parse statistics with report-level SQL count, issue SQL count, issue count, ratios, highest priority, important/urgent flags, issue scenes, controller endpoint, tests, and interface documentation.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=ParseStatisticsApplicationServiceTest,ParseStatisticsControllerTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-057; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Priority matrix and important/urgent drill-through remain in D-TASK-058.
  - Next step: Instantiate D-TASK-058 to add priority matrix and important/urgent list surfaces.

### D-TASK-056: 落地按 SQL 与问题场景统计

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add parse statistics APIs`
- Priority: 1
- Depends on: `D-TASK-055`
- Scope: parse overview、scene aggregation、single-SQL issue 统计 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-010` 解析统计与优先级分层
- Human confirmation point: 若按 SQL / 场景统计会引入高成本查询、错误聚合或隐藏问题样本，需人工确认
- Data impact: 统计聚合、样本明细、索引与缓存面
- Rollback / recovery: 回退高成本聚合，恢复基础统计和样本可追溯性
- Validation:
  - `statistics API 测试`
  - `python3 scripts/foreman.py validate D-TASK-056`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented parse statistics overview, issue-scene aggregation, single-SQL issue statistics, tenant-scoped aggregation over parse batch items, repository findAll support, mapper coverage, and API contract documentation.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=ParseStatisticsApplicationServiceTest,ParseStatisticsControllerTest,StructureParsePriorityScorerTest,ParseBatchPersistenceSchemaMappingTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-056; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Report-dimension aggregation and priority matrix/list views remain deferred to D-TASK-057 and D-TASK-058 as planned.
  - Next step: Instantiate D-TASK-057 to add report-dimension parse aggregation and ratio metrics.

### D-TASK-055: 固化解析统计口径与优先级评分

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): codify parse priority scoring`
- Priority: 1
- Depends on: `D-TASK-054`
- Scope: scene/domain/severity/priority/important/urgent 评分与聚合口径 Tech: `JAVA-BE`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-010` 解析统计与优先级分层
- Human confirmation point: 若统计口径与优先级评分改变已确认的 severity/priority/important/urgent 语义，需人工确认
- Data impact: 评分规则、统计口径与相关查询面
- Rollback / recovery: 保留旧评分/口径并追加新规则，不覆盖历史结果
- Validation:
  - `scoring rule 测试`
  - `python3 scripts/foreman.py validate D-TASK-055`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented structure parse issue scenario catalog, scoring snapshot, default domain/severity/important/urgent normalization, documented scoring thresholds and scenario taxonomy, and added scoring regression coverage.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=StructureParsePriorityScorerTest,StructureParseControllerTest,ReportBatchApplicationServiceTest,ParseBatchControllerTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-055; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: D-TASK-055 establishes scoring policy only; API-level parse statistics aggregation is intentionally deferred to D-TASK-056 and later report aggregation tasks.
  - Next step: Instantiate D-TASK-056 to expose parse overview, issue-scene aggregation, and single-SQL statistics surfaces.

### D-TASK-054: 接入报表接口配置与真实拉取抽象

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(governance): add report interface config resolver`
- Priority: 1
- Depends on: `D-TASK-053`
- Scope: governance 配置报表接口，sql-optimization 通过统一抽象调用；保留 mock 路径 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-009` 批量解析与报表清单解析
- Human confirmation point: 若报表接口抽象会直接绑定真实外部接口、落 secret/live inventory 或破坏 mock 可回退路径，需人工确认
- Data impact: 接口配置、client 抽象、报表 SQL 解析来源
- Rollback / recovery: 回退到 mock 路径并移除高风险外部绑定
- Validation:
  - `config/client abstraction 测试`
  - `python3 scripts/foreman.py validate D-TASK-054`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented governance-backed report interface configuration, internal resolve contract, sql-optimization ReportSqlResolver abstraction, HTTP API fetch client, and mock fallback preservation for report batch SQL resolution.
  - Validation evidence: mvn -pl sql-optimization,governance -am -Dtest=ReportBatchApplicationServiceTest,ReportBatchControllerTest,GovernanceBackedReportSqlResolverTest,ReportInterfaceConfigApplicationServiceTest,GovernanceCapabilityApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-054; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The HTTP fetch abstraction intentionally supports only GET plus a configured JSON SQL field in this task; real endpoint secrets and live inventory remain out of repo scope and must be supplied through protected configuration later.
  - Next step: Instantiate D-TASK-055 to solidify parse issue scoring and priority taxonomy for statistics.

### D-TASK-053: 落地报表清单解析文件模拟入口

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add report batch mock resolution`
- Priority: 1
- Depends on: `D-TASK-052`
- Scope: 以 `report_code` 为主键，从 txt/mock source 获取 SQL 再解析 Tech: `JAVA-BE`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-009` 批量解析与报表清单解析
- Human confirmation point: 若报表清单 mock 入口会被写成真实接口联通事实、或改变 `report_code` 唯一键语义，需人工确认
- Data impact: report batch 记录、mock source 解析与报表- SQL 映射
- Rollback / recovery: 恢复 txt/mock 语义与 `report_code` 主键边界
- Validation:
  - `mock resolve 测试`
  - `python3 scripts/foreman.py validate D-TASK-053`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented TXT/mock report catalog import keyed by report_code, report batch/item domain and persistence, resolve-sqls mock SQL generation, structure/access parse orchestration, and report batch detail APIs.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=ReportBatchApplicationServiceTest,ReportBatchControllerTest,ParseBatchPersistenceSchemaMappingTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-053; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Real report API configuration and remote SQL fetching remain in the follow-up report interface task; current implementation is explicitly TXT/mock source only.
  - Next step: Instantiate the next report interface task to add governance-backed report endpoint configuration and resolver abstraction.

### D-TASK-052: 扩展 `xls/et` 兼容导入与失败语义

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add xls/et compatibility guidance for parse batches`
- Priority: 1
- Depends on: `D-TASK-051`
- Scope: 兼容 `xls/et`，失败时显式提示建议改用稳定格式 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-009` 批量解析与报表清单解析
- Human confirmation point: 若 `xls/et` 兼容支持会拖累主线、把兼容失败误写为平台故障，需人工确认
- Data impact: 兼容格式解析逻辑与失败提示
- Rollback / recovery: 回退兼容扩展到稳定格式基线，并保留失败原因说明
- Validation:
  - `compatibility 测试`
  - `python3 scripts/foreman.py validate D-TASK-052`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Extended batch ingestion to accept xls workbook payloads, preserved stable xlsx/csv/txt/sql import behavior, and added explicit ET failure guidance that recommends converting to XLSX or CSV.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=ParseBatchApplicationServiceTest,ParseBatchControllerTest,ParseBatchPersistenceSchemaMappingTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-052; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: ET support remains compatibility-oriented and may still require conversion guidance depending on provider payload shape; report-catalog resolution still belongs to the next task.
  - Next step: Instantiate D-TASK-053 to add the report catalog mock resolution path and SQL lookup orchestration.

### D-TASK-051: 落地 SQL/表格批量导入解析

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add stable batch parse ingestion`
- Priority: 1
- Depends on: `D-TASK-050`
- Scope: 稳定支持 `xlsx/csv/txt/sql` 导入与结构解析/access parse 编排 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-009` 批量解析与报表清单解析
- Human confirmation point: 若稳定格式导入解析会在失败时丢失原始记录、绕过审计或把 access parse 强制为同步阻断，需人工确认
- Data impact: 批量导入记录、parse task 批次编排与失败记录
- Rollback / recovery: 恢复结构解析优先与失败留痕，不删除原始批次记录
- Validation:
  - `import parsing 测试`
  - `python3 scripts/foreman.py validate D-TASK-051`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented stable xlsx/csv/txt/sql batch ingestion with parse_batch_item persistence, structure/access orchestration, retry-access baseline, and batch detail statistics.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=ParseBatchApplicationServiceTest,ParseBatchControllerTest,ParseBatchPersistenceSchemaMappingTest,AccessParseControllerTest,StructureParseControllerTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-051; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: XLS/ET compatibility and report-catalog resolution remain in later tasks; current ingest path assumes header-based tabular payloads and Base64 submission.
  - Next step: Instantiate D-TASK-052 to extend xls/et compatibility and stable failure guidance.

### D-TASK-050: 建立批量解析批次模型与模板契约

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add parse batch contract baseline`
- Priority: 1
- Depends on: `D-TASK-049`
- Scope: `ParseBatch`、模板列、导入模式和批次状态机基线 Tech: `JAVA-BE`,`SQL`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-009` 批量解析与报表清单解析
- Human confirmation point: 若批量解析批次模型与模板契约会把兼容格式、mock source 或未校验列写成正式运行时默认，需人工确认
- Data impact: batch/task metadata、模板列、导入状态与批次统计
- Rollback / recovery: 保留稳定格式优先与 mock 边界，回退高风险模板/状态语义
- Validation:
  - `batch contract 测试`
  - `python3 scripts/foreman.py validate D-TASK-050`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added ParseBatch domain/model persistence, create/detail parse-batch APIs, template-column and supported-file-type contracts, status-history baseline, schema/migration coverage, and matching interface/data-model documentation updates.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=ParseBatchApplicationServiceTest,ParseBatchControllerTest,ParseBatchPersistenceSchemaMappingTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-050; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Batch creation currently establishes only the contract baseline and READY state; actual file ingestion, batch listing, retry-access orchestration, and report catalog resolution remain for D-TASK-051 and later tasks.
  - Next step: Instantiate D-TASK-051 to add real SQL/tabular import ingestion and structure/access parse orchestration on top of the ParseBatch contract baseline.

### D-TASK-049: 统一逻辑对象在查询/历史/解析中的展示契约

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(governance): unify logical object evidence surfaces`
- Priority: 1
- Depends on: `D-TASK-048`
- Scope: 统一查询、历史、解析、路由消费的 logical object DTO/VO 与 detail/list/export surfaces Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-008` 逻辑视图与 DB View 治理
- Human confirmation point: 若逻辑对象统一展示会破坏现有 query/history/parse 契约兼容性，需人工确认
- Data impact: 跨服务 DTO/VO 与前端消费面
- Rollback / recovery: 保留旧 DTO/VO 兼容层，并回退统一对象字段为可选扩展
- Validation:
  - `cross-service contract 测试`
  - `python3 scripts/foreman.py validate D-TASK-049`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Unified logical object DTO and evidence rendering across structure-parse, query-history list/detail, trace detail, and export payload surfaces while preserving legacy logical-object evidence compatibility.
  - Validation evidence: mvn -pl governance,sql-optimization -am -Dtest=StructureParseContractTest,StructureParseControllerTest,GovernanceHistoryApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-049; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Query-execution runtime responses still need to emit the same shared logical-object surface once execution endpoints are materialized; current unification covers parse and governance consumption surfaces.
  - Next step: Instantiate D-TASK-050 to establish ParseBatch, import template, and batch-state contracts for the bulk parse workflow.

### D-TASK-048: 落地 DB View 识别与依赖展示

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(governance): add db view dependency resolution`
- Priority: 1
- Depends on: `D-TASK-047`
- Scope: 结构解析与历史追溯识别 DB View，并展示依赖对象 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-008` 逻辑视图与 DB View 治理
- Human confirmation point: 若 DB View 识别会误把复杂对象链写成确定事实、放宽跨源依赖边界，需人工确认
- Data impact: DB view 依赖解析与展示数据
- Rollback / recovery: 保留已识别依赖为 evidence，回退高风险展开逻辑为摘要模式
- Validation:
  - `parser/integration 测试`
  - `python3 scripts/foreman.py validate D-TASK-048`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governance-backed DB view catalog and dependency resolution, exposed public and internal db-view endpoints, and enriched structure-parse DB_VIEW hits with resolved dependency evidence for history/traceability consumers.
  - Validation evidence: mvn -pl sql-optimization,governance -am -Dtest=StructureParseControllerTest,GovernanceCapabilityApplicationServiceTest,DatabaseViewCatalogApplicationServiceTest,DatabaseViewCatalogControllerTest,TraceabilitySchemaMappingTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-048; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: History and export surfaces still need a unified logical-object display contract so DB_VIEW dependency evidence renders consistently across query/history/parse views; that alignment remains for D-TASK-049.
  - Next step: Instantiate D-TASK-049 to unify logical object display contracts across query, history, parse, and export surfaces.

### D-TASK-047: 落地业务逻辑视图目录与映射

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(governance): add business logical view catalog`
- Priority: 1
- Depends on: `D-TASK-046`
- Scope: governance 中的 business logical view 目录、映射、物理表关联与查询面 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-008` 逻辑视图与 DB View 治理
- Human confirmation point: 若业务逻辑视图目录与映射会引入未确认业务口径、删除既有物理映射或放宽租户隔离，需人工确认
- Data impact: logic view 目录、映射表与相关治理查询
- Rollback / recovery: 恢复原目录/映射快照，关闭高风险对象或映射规则
- Validation:
  - `repository/controller 测试`
  - `python3 scripts/foreman.py validate D-TASK-047`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governance-side business logical view catalog and logical object mapping persistence surfaces, exposed /api/governance/logical-views list/detail endpoints, and wired schema/migration plus repository/service/controller tests for the new directory and mapping query face.
  - Validation evidence: mvn -pl governance -am -Dtest=TraceabilitySchemaMappingTest,LogicalViewCatalogApplicationServiceTest,LogicalViewCatalogControllerTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-047; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The catalog currently serves governance-owned directory records only; parser-driven DB_VIEW dependency population and unified history/query display alignment remain for D-TASK-048 and D-TASK-049.
  - Next step: Instantiate D-TASK-048 to recognize DB_VIEW dependencies in the parse chain and connect those hits to the new logical view catalog surfaces.

### D-TASK-046: 建立逻辑对象统一模型

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(shared): unify logical object reference contract`
- Priority: 1
- Depends on: `D-TASK-045`
- Scope: 建立 `BUSINESS_VIEW/DB_VIEW/TABLE` 的统一对象契约与跨服务引用字段 Tech: `JAVA-BE`,`SQL`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-008` 逻辑视图与 DB View 治理
- Human confirmation point: 若统一逻辑对象模型会混淆 `BUSINESS_VIEW` 与 `DB_VIEW` 语义、扩大对象默认可见范围，需人工确认
- Data impact: 逻辑对象目录、引用键、跨服务 DTO
- Rollback / recovery: 通过兼容视图恢复双模对象边界，并保留已落库对象数据
- Validation:
  - `contract 与 DTO 测试`
  - `python3 scripts/foreman.py validate D-TASK-046`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added a shared logical object reference contract in sqlforge-shared, enriched structure parse logical object hits with objectKey/catalog/schema fields, aligned governance history type extraction with the unified objectType key, and updated interface/data-model docs for the canonical logical object reference surface.
  - Validation evidence: mvn -pl sql-optimization,governance -am -Dtest=StructureParseContractTest,StructureParseControllerTest,GovernanceHistoryApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-046; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Only structure-parse and governance-history consumers are wired to the shared logical object contract so far; query-execution and richer history/detail display alignment remain for D-TASK-047 and later tasks.
  - Next step: Instantiate D-TASK-047 to extend business logical view directory and mapping persistence against the new shared logical object reference contract.

### D-TASK-045: 补齐解析综合结论与 partial-success 追溯

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add combined parse conclusion traceability`
- Priority: 1
- Depends on: `D-TASK-044`
- Scope: 统一结构解析成功 + access parse 失败时的综合状态、查询面与历史追溯 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-007` 结构解析与数据访问解析双轨闭环
- Human confirmation point: 若综合结论会隐藏 partial success、抹平结构与 access parse 的状态差异，或删除 failure evidence，需人工确认
- Data impact: parse task 总状态、历史详情与统计聚合
- Rollback / recovery: 恢复双轨状态分开展示，保留 partial success 证据与失败原因
- Validation:
  - `state machine 与 history/detail 测试`
  - `python3 scripts/foreman.py validate D-TASK-045`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added combined parse conclusion and status-history surfaces, preserved partial-success evidence when access parse degrades, and extended controller tests for waiting/success/partial-success query flows.
  - Validation evidence: mvn -pl sql-optimization -Dtest=AccessParseControllerTest,StructureParseControllerTest,StructureParseContractTest,StructureParsePriorityScorerTest,StructureParseResultTest test; python3 scripts/foreman.py validate D-TASK-045; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Combined parse state is still in-memory and not persisted; history/detail propagation remains for later governance tasks.
  - Next step: Instantiate D-TASK-046 to unify logic-object contracts and continue the repo-side Wave 1/Wave 2 parsing chain.

### D-TASK-044: 落地数据访问解析入口与异步补跑语义

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add access parse async follow-up baseline`
- Priority: 1
- Depends on: `D-TASK-043`
- Scope: 结构解析成功后自动异步补跑 access parse，保留 unavailable/skipped/failed 语义与服务状态 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-007` 结构解析与数据访问解析双轨闭环
- Human confirmation point: 若数据访问解析会阻断结构解析返回、把外部服务不可用误写成整体成功，或引入未确认的默认重试策略，需人工确认
- Data impact: access parse 任务、服务状态、可达性/计划/分区/SLA 证据
- Rollback / recovery: 恢复结构解析先返回、access parse 独立失败的既定语义，停用自动补跑
- Validation:
  - `async parse flow 测试、降级测试`
  - `python3 scripts/foreman.py validate D-TASK-044`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added access-parse endpoint semantics, combined parse submission/poll endpoints, in-memory async follow-up flow, and unavailable/skipped/failed service-state handling on top of the structure parse baseline.
  - Validation evidence: mvn -pl sql-optimization -Dtest=AccessParseControllerTest,StructureParseControllerTest,StructureParseContractTest,StructureParsePriorityScorerTest,StructureParseResultTest test; python3 scripts/foreman.py validate D-TASK-044; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Combined parse state is still in-memory and not persisted; object resolution remains provider-light and SLA/freshness stay conservative UNKNOWN until D-TASK-045 and later governance metadata work.
  - Next step: Instantiate D-TASK-045 to unify partial-success status, add richer combined parse query surfaces, and prepare history/persistence handoff for later waves.

### D-TASK-043: 落地单条结构解析入口

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add structure parse endpoint baseline`
- Priority: 1
- Depends on: `D-TASK-042`
- Scope: 不依赖数据库的结构解析、query-date 提取、逻辑对象命中与 rewrite candidate 输出 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-007` 结构解析与数据访问解析双轨闭环
- Human confirmation point: 若结构解析入口引入数据库依赖、阻断查询主路径或把低置信度结果伪装成高置信度，需人工确认
- Data impact: structure parse 任务、结构化问题、query-date 与逻辑对象命中证据
- Rollback / recovery: 关闭高成本分析支路，保留基础语法/结构解析与低置信度标识
- Validation:
  - `parse structure controller/service 测试`
  - `python3 scripts/foreman.py validate D-TASK-043`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added the first synchronous structure parse endpoint, request/response contract wiring, query-date extraction, logical-object hits, rewrite candidate projection, and INVALID degradation behavior without database dependencies.
  - Validation evidence: mvn -pl sql-optimization -Dtest=StructureParseControllerTest,StructureParseContractTest,StructureParsePriorityScorerTest,StructureParseResultTest test; python3 scripts/foreman.py validate D-TASK-043; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Structure parse currently infers DB_VIEW hits heuristically and does not yet use governed logical-view catalogs or access parse evidence; D-TASK-044 will add access-parse semantics and D-STORY-008 will enrich object resolution.
  - Next step: Instantiate D-TASK-044 to add access-parse entry semantics and asynchronous follow-up on top of the new structure parse baseline.

### D-TASK-042: 固化结构解析契约与问题分类模型

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): freeze structure parse contract baseline`
- Priority: 1
- Depends on: `D-TASK-041`
- Scope: 定义结构解析响应、问题域/场景、severity/priority/important/urgent 评分基线 Tech: `JAVA-BE`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-007` 结构解析与数据访问解析双轨闭环
- Human confirmation point: 若结构解析契约会把未实现的语义分析写成既成事实、删减问题分类维度或改变严重度/优先级口径，需人工确认
- Data impact: 解析响应、问题分类、统计口径与文档基线
- Rollback / recovery: 恢复上一版问题分类与评分字段，保留新增字段为可选扩展
- Validation:
  - `parser/domain 契约测试`
  - `python3 scripts/foreman.py validate D-TASK-042`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Defined structure parse domain/result contract, issue taxonomy, aggregate priority scoring baseline, typed response VOs, and synced interface/product specs for D-STORY-007.
  - Validation evidence: mvn -pl sql-optimization -Dtest=StructureParsePriorityScorerTest,StructureParseResultTest,StructureParseContractTest test; python3 scripts/foreman.py validate D-TASK-042; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: No structure parse controller or parser implementation yet; D-TASK-043 will bind these contracts to the actual parse endpoint and extraction logic.
  - Next step: Instantiate and implement D-TASK-043 using the frozen structure parse contract for the first repo-side structure parse endpoint.

### D-TASK-041: 补齐 SQL 历史导出与取证视图

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(governance): add query history export baseline`
- Priority: 1
- Depends on: `D-TASK-040`
- Scope: 为 `CSV/EXCEL/JSON/SQL/PDF` 导出、单次执行取证字段与审计链接补齐基线 Tech: `JAVA-BE`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-006` 查询历史与执行取证闭环
- Human confirmation point: 若导出/取证视图会放宽敏感字段输出、破坏脱敏语义或把 PDF/SQL 导出写成默认生产事实，需人工确认
- Data impact: 导出记录、取证视图、导出载荷与审计链
- Rollback / recovery: 恢复原导出白名单与脱敏策略，禁用高风险格式并保留导出审计记录
- Validation:
  - `export 契约测试、审计联动测试`
  - `python3 scripts/foreman.py validate D-TASK-041`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added query-history export endpoint, SQL tri-state evidentiary detail fields, inline export formats, and export_record plus audit_log linkage for history forensics.
  - Validation evidence: mvn -pl governance -Dtest=GovernanceHistoryApplicationServiceTest,AuthWebMvcTest,TraceabilitySchemaMappingTest test; python3 scripts/foreman.py validate D-TASK-041; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: CSV/EXCEL/PDF exports are inline evidentiary baselines in phase 1 rather than binary file rendering; broader parse/recommendation source refs remain to be populated by downstream tasks.
  - Next step: Instantiate and implement D-TASK-042 to freeze structure-parse contracts and issue taxonomy on top of the history evidentiary surfaces.

### D-TASK-040: 落地 SQL 历史列表与详情查询面

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(governance): add query history list and detail surfaces`
- Priority: 1
- Depends on: `D-TASK-039`
- Scope: 为历史列表、详情、筛选、分类、route/parse/recommendation/benchmark drill-through 建立治理查询面 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-006` 查询历史与执行取证闭环
- Human confirmation point: 若历史查询面会引入越权钻取、跨租户可见性扩大或破坏已存在分页/审计约束，需人工确认
- Data impact: governance 历史查询、详情、关联 drill-through 与索引
- Rollback / recovery: 回退新增筛选/详情能力，恢复原历史查询面并保留新索引/字段供后续受控启用
- Validation:
  - `governance history list/detail 测试`
  - `python3 scripts/foreman.py validate D-TASK-040`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governance query-history list/detail APIs, mapper projections, classification summary, and trace drill-through coverage for SQL history surfaces.
  - Validation evidence: mvn -pl governance -Dtest=GovernanceHistoryApplicationServiceTest,AuthWebMvcTest,TraceabilitySchemaMappingTest test; python3 scripts/foreman.py validate D-TASK-040; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Classification summaries are page-scoped; history export and dedicated evidentiary export surfaces remain in D-TASK-041.
  - Next step: Instantiate and implement D-TASK-041 to add SQL history export and evidentiary view completion.

### D-TASK-038: 扩展 query-history / execution-result 追溯字段

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(governance): extend query history traceability surfaces`
- Priority: 1
- Depends on: `D-TASK-037`
- Scope: 为 comment context、report/stage/biz-date、query-date、SQL 三态、逻辑对象命中、access channel 与 route/cache summary 补齐持久化与查询字段 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-006` 查询历史与执行取证闭环
- Human confirmation point: 若历史追溯字段扩展会改变既有审计语义、删除已存证的 SQL/route/cache/trace 信息，或把未确认字段写成强制事实，需人工确认
- Data impact: `query_history`、`execution_result`、导出/取证查询字段与索引
- Rollback / recovery: 保留既有追溯链并以追加字段方式扩展；必要时通过视图/兼容 DTO 回退查询面
- Validation:
  - `governance/query-execution schema 与 mapping 测试、history persistence 测试`
  - `python3 scripts/foreman.py validate D-TASK-038`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Expanded query_history and execution_result persistence baselines with structured traceability fields for comment context, datasource/report/stage dates, access channel, SQL tri-state evidence, logical object hits, and route/cache summaries; projected legacy queryContext/resultSummary JSON into the new columns; exposed the new history evidence surface through governance trace detail VO mappings; added incremental migration coverage and schema/persistence/history tests for the expanded traceability contract.
  - Validation evidence: mvn -pl governance -Dtest=TraceabilitySchemaMappingTest,GovernanceProtectedPersistenceServiceTest,GovernanceHistoryApplicationServiceTest test; python3 scripts/foreman.py validate D-TASK-038; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Current writer call sites still populate most new fields through JSON projection rather than explicit DTO fields, so D-TASK-037 and later query-execution/sql-optimization contract tasks still need to supply first-class values for full fidelity and broader history filters.
  - Next step: Instantiate and implement the next Wave 1 baseline task that adds the query-execution side contract surface, then continue with single-query structure parsing and access-parse orchestration tasks on top of the expanded history substrate.

### HARN-043: 修复 SQL 治理规格包 follow-up 真值缺口并启动 Wave 1

- Status: done
- Completed at: 2026-04-26
- Commit subject: `docs(plans): reconcile sql governance spec gaps and wave1 start`
- Priority: 1
- Depends on: `HARN-042`
- Scope: 在不新增微服务、不改写 `HARN-042` 历史完成语义、不引入第二套长期真值的前提下，修复 `HARN-042` closeout 后主计划 active-wave 漂移，补齐接口/枚举/只读执行边界与数据模型漏项，并新增数据源/数据资产/系统管理的缺失 Story/Task inventory；随后以当前仓库真值启动 Wave 1 首个 repo-side mainline task。 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Matrix context: Phase-A / Story `A-STORY-009` SQL 治理实施规格与任务塑形
- Human confirmation point: 若要借本任务改变 `HARN-042` 已归档历史、扩大“不新增微服务”边界、把接口/枚举/只读执行限制以外的实现内容偷渡进来，或跳过 Wave 1 任务正常 instantiate 流程，需人工确认。
- Data impact: 修复 `HARN-042` follow-up 真值缺口的规格/计划/矩阵文本、补充的数据源/系统管理 Story/Task inventory，以及 Wave 1 启动前的治理收口；不直接修改业务运行时数据。
- Rollback / recovery: 回退时仅回退 `HARN-043` 新增的规格/计划/矩阵修补与 inventory 增量，恢复到 `HARN-042` closeout 后状态；若 Wave 1 已实例化，则通过追加治理修正保留既有 task evidence。
- Validation:
  - `python3 scripts/foreman.py validate HARN-043`、`python3 scripts/foreman.py compile-governance --check`、`node scripts/lint-repository-knowledge.js`、`python3 scripts/task_audit.py --check --phase pre-closeout`、规格包/计划/矩阵交叉检查
  - `python3 scripts/foreman.py validate HARN-043`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reconciled the SQL governance spec pack after HARN-042 by fixing active-wave truth, tightening read-only execution and interface/state contracts, filling missing data model objects, and adding missing datasource/data-asset/system-management Story-Task inventory needed before Wave 1 implementation.
  - Validation evidence: python3 scripts/foreman.py validate HARN-043; python3 scripts/foreman.py compile-governance --check; node scripts/lint-repository-knowledge.js; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Wave 1 business tasks remain uninstantiated until the first repo-side mainline task is materialized; current runtime state directories are still untracked operational residue and are intentionally excluded from closeout.
  - Next step: Instantiate D-TASK-038 as the first Wave 1 repo-side mainline task, then implement query-history and execution-result trace-field persistence in dependency order.

### HARN-042: 落地 SQL 治理实施规格包与完整任务清单

- Status: done
- Completed at: 2026-04-26
- Commit subject: `docs(plans): land sql governance spec pack and task inventory`
- Priority: 1
- Depends on: HARN-041
- Scope: Add the SQL governance implementation specification pack and write the full downstream Story/Task inventory into the master execution plan, task-spec matrix, and governance extension matrix without implementing business code.
- Validation:
  - `python3 scripts/foreman.py validate HARN-042`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Landed the SQL governance implementation spec pack, interface/data/degradation baselines, and the full D/E/F story-task inventory into the execution plan and governance matrices without changing runtime business code.
  - Validation evidence: python3 scripts/foreman.py validate HARN-042; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/foreman.py compile-governance --check; node scripts/lint-repository-knowledge.js
  - Residual risk: Follow-on business tasks are not yet instantiated, so the new plan remains design-time truth until Wave 1 implementation begins; environment-backed integrations such as real report APIs, mail delivery, and loader consumers remain intentionally mocked or abstracted.
  - Next step: Materialize Wave 1 repo-side tasks for query history, execution traceability, and dual-track parse foundations, then implement them in dependency order.

### HARN-041: Runtime Reservation Pause/Cleanup and Demand Re-entry Governance

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(governance): close HARN-041 reservation lifecycle governance`
- Priority: 1
- Depends on: `HARN-028`,`HARN-036`,`HARN-038`
- Scope: 为 SQLForge governed intake/task-shaping reservation 增加 paused/archived/abandoned 生命周期语义，并把 healthcheck、runtime dashboard、cleanup 与 playbook 的判定对齐到同一治理模型。保留 HARN-029/HARN-030 的 shaping 证据但不把它们转成正式实现任务；将 HARN-040 停留在暂停候选/未确认状态，不继续 confirm-run。完成后必须能让新的 requirement 重新进入 governed intake/shaping，同时保持现有 preflight/instantiate/task_audit/closeout 边界不变。 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Plan ref: docs/exec-plans/completed/HARN-041-full-auto-execution-plan.md
- Matrix context: Phase-A / Story `A-STORY-007` 从无 task 开始的治理自动化
- Human confirmation point: 若要把 paused/archived/abandoned 语义扩展为自动重写台账真值、静默删除 runtime evidence、或允许未确认 candidate 继续 confirm-run，则必须先人工确认；本任务仅允许在现有 governed intake/runtime 边界内补齐可审计状态与恢复入口。
- Data impact: 仅修改 governed runtime reservation/intake/task-shaping 状态语义、文档与运行时清理逻辑；不直接修改业务运行时数据，不把候选证据写成仓库长期真值。
- Rollback / recovery: 若新增 reservation 生命周期语义导致 confirm-run、healthcheck 或 cleanup 行为异常，回退相关脚本与文档改动，并将受影响 reservation 状态恢复到先前的 released/candidate_ready/materialized 语义；历史 shaping evidence 保留在 .codex/state 下，不删除现有证据文件。
- Validation:
  - `python3 scripts/foreman.py validate HARN-041`
  - `python3 -m py_compile scripts/governed_v2_support.py scripts/governed_healthcheck.py scripts/governed_runtime_dashboard.py`
  - `python3 scripts/governed_healthcheck.py --check`
  - `python3 scripts/governed_runtime_dashboard.py --json`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-26: implemented auditable reservation lifecycle states, healthcheck/dashboard gating, and candidate pause/archive semantics; archived HARN-029/HARN-030 and paused HARN-040 through governed runtime tooling.
- Context closeout:
  - Completed scope: Implemented auditable paused/archived/abandoned reservation lifecycle semantics; updated governed healthcheck/runtime dashboard/materialize behavior; archived HARN-029/HARN-030 dry-run candidates; paused HARN-040; documented candidate pause/resume/re-entry governance.
  - Validation evidence: python3 scripts/foreman.py validate HARN-041; python3 -m py_compile scripts/governed_v2_support.py scripts/governed_healthcheck.py scripts/governed_runtime_dashboard.py; python3 scripts/governed_runtime_dashboard.py --json; node scripts/lint-repository-knowledge.js; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Older released reservations such as HARN-032 and HARN-039 still appear in runtime dashboard archive preview and may need a follow-up archival sweep, but they no longer block governed healthcheck.
  - Next step: Use governed_runtime_dashboard.py lifecycle actions for future candidate pause/archive/resume decisions and rerun governed intake/shaping when archived candidates need fresh task packs.

### D-TASK-037: 收口 cache capacity / eviction / metrics governance baseline

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(query-execution): close D-TASK-037 cache capacity governance`
- Priority: 1
- Depends on: `D-TASK-036`
- Scope: 在保持 D-TASK-036 provider-neutral cache backend、默认 repo-closed 主路径、统一授权入口、治理审计、缓存一致性与只读边界不变的前提下，为 cache governance 补齐 per-tenant / per-policy capacity limit、TTL 与 capacity/manual/schema eviction reason evidence、cache hit/miss/bypass/backfill/invalidate/backend-unavailable metrics、policy verify capacity/backend health summary，并让 benchmark/governance 继续透出 eviction/capacity evidence；真实 Redis 集群长跑和恢复...
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若 cache capacity / eviction / metrics governance 会放宽缓存新鲜度边界、让过期或被驱逐 entry 继续命中、引入高基数指标标签、绕过统一授权入口或治理审计、或把真实 Redis 长跑环境写成仓库默认事实，需人工确认
- Data impact: cache policy capacity/ttl 配置、tenant/policy capacity counters、eviction reason evidence、cache governance metrics、policy verify runtime summary、benchmark/governance cache surface
- Rollback / recovery: 保持 D-TASK-036 repo-closed 默认主路径和 fail-closed 语义，关闭高风险 capacity/ttl 配置或 metrics 标签，回退新增 eviction/capacity/metrics 语义与文档说明，并恢复到 `D-TASK-036` 已验证基线
- Validation:
  - `sqlforge-shared/query-execution/benchmark-engine/governance 模块测试、cache capacity/eviction 契约测试、cache governance metrics 断言、policy verify summary 测试、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-037`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented repo-side cache capacity / eviction / metrics governance baseline: policy maxEntries/ttlSeconds, per-tenant/per-policy capacity evidence, TTL/CAPACITY/MANUAL/SCHEMA eviction reasons, low-cardinality cache governance metrics, verify runtime capacity/backend health summary, and benchmark/governance evidence propagation.
  - Validation evidence: mvn -B -pl query-execution -am test -DskipITs -Dtest=QueryExecutionCacheGovernanceRuntimeServiceTest,QueryExecutionApplicationServiceTest,QueryExecutionBenchmarkWorkloadServiceTest -Dsurefire.failIfNoSpecifiedTests=false; mvn -B -pl query-execution,benchmark-engine,governance -am test -DskipITs -Dtest=QueryExecutionCacheGovernanceRuntimeServiceTest,QueryExecutionApplicationServiceTest,QueryExecutionInternalControllerTest,QueryExecutionBenchmarkWorkloadServiceTest,BenchmarkGovernanceTraceServiceTest,GovernanceHistoryApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false; python3 scripts/foreman.py validate D-TASK-037; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js
  - Residual risk: Real Redis cluster long-run evidence and cross-node recovery drills remain environment-backed follow-up; default repo path remains IN_MEMORY/fail-closed and does not enable provider cache by default.
  - Next step: Shape the next repo-side Phase-D follow-up from current repository truth, or run environment-backed Redis long-run/recovery validation outside the default repo path when the environment is available.

### HARN-039: Reconcile D-TASK-036 post-closeout plan truth

- Status: done
- Completed at: 2026-04-26
- Commit subject: `fix(governance): reconcile D-TASK-036 plan truth`
- Priority: 1
- Depends on: D-TASK-036
- Scope: 修正 D-TASK-036 closeout 后 master-execution-plan 当前波次仍把 D-TASK-036 写成下一条候选任务的文档真值漂移；只更新计划叙事与运行台账，不改业务代码，不塑形新的业务任务。
- Validation:
  - `python3 scripts/foreman.py validate HARN-039`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reconciled the master execution plan after D-TASK-036 closeout so the current active wave no longer points at D-TASK-036 as the next candidate task, records D-TASK-036 as completed, and restores the repo-side mainline state to no instantiated task.
  - Validation evidence: python3 scripts/foreman.py validate HARN-039; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance --check
  - Residual risk: No business implementation changed; future Phase-D work still requires explicit shaping and instantiation before execution.
  - Next step: When the next repo-side priority is chosen, shape a new formal task from current repository truth instead of reusing completed D-TASK-036.

### D-TASK-036: 推进 provider-native distributed cache governance backend

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(query-execution): close D-TASK-036 cache backend`
- Priority: 1
- Depends on: `D-TASK-035`
- Scope: 在保持 repo-closed in-memory cache governance baseline、统一授权入口、治理审计、缓存一致性与只读边界不变的前提下，为 `query-execution` 补齐 provider-neutral distributed cache backend contract、environment-backed carrier 语义、provider-native evidence、失败降级与可审计读写校验；默认仍不启用外部 provider，不把 Redis/provider cache 写成仓库默认事实 Tech: `JAVA-BE`,`SQL`,`OPS`,`DOCS`. Layer: `common`,`application(controller/service)/domain/infrastructure`,`deployments/c...
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若 distributed cache backend 会放宽数据新鲜度/一致性边界、绕过统一授权入口或治理审计、把 provider/Redis 依赖写成仓库默认主路径、引入明文凭据或 fail-open 命中语义，需人工确认
- Data impact: cache backend 配置、provider-native 读写/校验证据、cache policy apply/verify/invalidate 证据、命中/旁路/回填/失效数据、跨服务审计记录
- Rollback / recovery: 保持 repo-closed in-memory cache governance baseline 为默认主路径，关闭 environment-backed distributed provider 默认启用，回退新增 backend contract/provider evidence/降级语义与文档说明，并恢复到 `D-TASK-035` 已验证基线
- Validation:
  - `sqlforge-shared/query-execution/governance 模块测试、distributed cache backend contract 测试、cache policy apply/verify/invalidate backend 证据测试、runtime smoke、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-036`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Synced Phase-D plan truth after D-TASK-035, shaped D-TASK-036, and implemented provider-neutral cache backend governance for query-execution with default in-memory backend, explicit Redis RESP provider adapter, backend/provider evidence, fail-closed bypass semantics, focused tests, and documentation updates.
  - Validation evidence: mvn -B -pl query-execution,governance -am test -DskipITs -Dtest=QueryExecutionCacheGovernanceRuntimeServiceTest,QueryExecutionApplicationServiceTest,QueryExecutionInternalControllerTest,QueryExecutionBenchmarkWorkloadServiceTest,GovernanceHistoryApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false; python3 scripts/foreman.py validate D-TASK-036; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js
  - Residual risk: Redis/provider backend remains explicitly configured environment-backed path; live multi-node Redis recovery, eviction, capacity governance, and long-running provider evidence remain future hardening.
  - Next step: Add environment-backed Redis smoke and eviction/capacity governance once a real distributed cache environment is available.

### D-TASK-035: 收口真正的缓存治理能力

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(query-execution): close D-TASK-035 cache governance`
- Priority: 1
- Depends on: `D-TASK-034`
- Scope: 在保持查询执行主路径、统一授权入口、治理审计、缓存一致性与只读边界不变的前提下，建立可审计的 cache governance 模型、命中/失效/旁路/回填/风险标记语义，以及与 query-execution/sql-optimization/benchmark 的最小联动闭环，不把缓存元数据占位误写成已治理完成 Tech: `JAVA-BE`,`SQL`,`OPS`,`DOCS`. Layer: `common`,`application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若缓存治理能力会放宽数据新鲜度/一致性边界、让缓存旁路/回填绕过授权或审计、把元数据占位误写成真实 cache governance，需人工确认
- Data impact: cache policy、命中/失效/旁路/回填/风险标记数据、跨服务治理与审计证据、相关 schema 与运行文档
- Rollback / recovery: 保持当前无强治理缓存默认边界，关闭高风险 cache policy 默认启用，回退新增 cache governance 字段、策略与文档说明，并恢复到 `D-TASK-034` 已验证基线
- Validation:
  - `sqlforge-shared/query-execution/sql-optimization/governance 模块测试、cache governance 契约测试、runtime smoke、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-035`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented governed result-cache apply/verify/invalidate runtime, schemaVersion-aware hit/backfill/bypass/invalidation semantics, query-execution metadata/audit evidence, benchmark evidence propagation, governance cacheGovernanceSurface aggregation, focused tests, and architecture docs.
  - Validation evidence: mvn -B -pl query-execution,benchmark-engine,governance -am test -DskipITs -Dtest=QueryExecutionApplicationServiceTest,QueryExecutionInternalControllerTest,QueryExecutionBenchmarkWorkloadServiceTest,BenchmarkGovernanceTraceServiceTest,GovernanceHistoryApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false; python3 scripts/foreman.py validate D-TASK-035; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js
  - Residual risk: Cache runtime is repository-closed in-memory baseline; distributed provider-native cache backing remains a future hardening step.
  - Next step: Evaluate provider-native distributed cache backing and cache eviction/observability integration after governed semantics stabilize.

### D-TASK-034: 收口 `benchmark-engine` 外部队列/文件存储与 provider-native 语义

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(benchmark-engine): close D-TASK-034 external queue carrier`
- Priority: 1
- Depends on: `D-TASK-033`
- Scope: 在保持 repo-local artifact lifecycle 与 `LOCAL_FILE` 默认主路径、统一授权入口、治理审计及只读/影子环境边界不变的前提下，为 `benchmark-engine` 补齐外部队列 carrier、文件存储编排与 provider-native 语义边界，把 provider-backed write/readback/cleanup/recovery 证据推进到更接近真实运行形态的基线 Tech: `JAVA-BE`,`OPS`,`DOCS`. Layer: `common`,`application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若 benchmark-engine 的外部队列/文件存储/provider-native 语义会让 environment-backed path 误写成仓库默认主路径、引入未经确认的 provider SDK/凭据写入、或绕过既有鉴权/审计边界，需人工确认
- Data impact: external queue/storage/provider-native 配置与运行摘要、artifact cleanup/recovery/write/readback 证据、跨服务追溯与审计留痕
- Rollback / recovery: 保持 repo-local lifecycle 与 `LOCAL_FILE` 默认主路径，关闭外部队列/provider-native 默认启用，回退新增 queue/storage/provider 语义与文档说明，并恢复到 `D-TASK-033` 已验证基线
- Validation:
  - `sqlforge-shared/benchmark-engine/governance 模块测试、external queue/storage/provider-native 契约测试、runtime smoke、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-034`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: benchmark-engine external queue carrier, task queue evidence surface, provider-native artifact evidence assertions, and benchmark architecture/contract docs
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-034; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js; mvn -B -pl benchmark-engine -am test -DskipITs -Dtest=BenchmarkTaskApplicationServiceTest,BenchmarkTaskWorkerTest,BenchmarkArtifactStorageServiceTest,BenchmarkArtifactGovernanceOperationServiceTest -Dsurefire.failIfNoSpecifiedTests=false
  - Residual risk: external-file-queue remains repo-closed file-spool evidence, not a provider-native message broker or cross-host distributed queue
  - Next step: D-TASK-035 cache governance baseline across query-execution/sql-optimization/benchmark/governance

### D-TASK-033: 收口 `query-execution` 生产级 Hetu 集群证据与路由参数校准

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(query-execution): close D-TASK-033 hetu route calibration`
- Priority: 1
- Depends on: `D-TASK-032`
- Scope: 在保留 repo-closed Hetu 多模式执行链、统一授权入口、只读/影子环境边界与结构化失败语义不变的前提下，补齐生产级 Hetu/MRS 集群证据、路由参数校准、模式优先级与失败分层证据沉淀，不把外部测试环境依赖误写成仓库默认主路径 Tech: `JAVA-BE`,`OPS`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若 Hetu 集群证据与路由参数校准会放宽只读/影子环境边界、把外部测试环境结果误写成 repo-closed 默认事实、或降低当前结构化失败语义，需人工确认
- Data impact: Hetu/MRS route calibration 参数、mode priority、env smoke/test-env evidence、执行与审计记录
- Rollback / recovery: 保持 repo-closed Hetu 主路径与当前失败语义不变，关闭高风险校准默认启用，回退新增 calibration/live-evidence 文档与配置说明，并恢复到 `D-TASK-032` 已验证基线
- Validation:
  - `query-execution 模块测试、route calibration 契约测试、runtime smoke、Hetu env smoke/test-env evidence、task audit、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-033`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governed Hetu route calibration and cluster-evidence snapshots for query-execution, calibrated mode priority/readiness/failure-layer routing, public route metadata, env-smoke evidence bundling, focused tests, and authority-doc updates while preserving repo-closed defaults and structured route failures.
  - Validation evidence: mvn -B -pl query-execution -am test -DskipITs; python3 scripts/foreman.py validate D-TASK-033 --include-task-audit with focused module/script checks; bash scripts/run-runtime-smoke.sh --compose-check; bash scripts/run-hetu-env-smoke.sh --help; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: External Win10/Hetu live evidence capture and long-term archived smoke logs remain environment-backed follow-up work under HARN-016 / INBOX-002; broader cross-service audit compensation still remains beyond this task scope.
  - Next step: Proceed to D-TASK-034 to close benchmark-engine external queue/storage provider-native semantics on top of the stabilized query-execution route-calibration baseline.

### D-TASK-032: 收口 acceleration plan 治理闭环

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): close D-TASK-032 acceleration plan loop`
- Priority: 1
- Depends on: `D-TASK-031`
- Scope: 在保持统一授权入口、治理审计、tenant 隔离与 `sql-optimization` suggestion 链不变的前提下，补齐 acceleration plan 的提交、审批/确认、应用、验证、回滚与长期追溯闭环，使 acceleration 不再只是建议元数据而成为受治理的正式对象 Tech: `JAVA-BE`,`SQL`,`DOCS`. Layer: `common`,`application(controller/service)/domain/infrastructure`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若 acceleration plan 治理闭环会放宽审批/确认边界、绕过统一授权入口与治理审计、允许 fail-open 应用或省略回滚/验证证据，需人工确认
- Data impact: acceleration plan / apply / verify / rollback 状态、跨服务治理记录、授权与审计证据、相关 schema 与契约载荷
- Rollback / recovery: 保持 suggestion-only 默认边界，关闭 plan apply 默认启用，回退新增 acceleration governance 字段、状态机与文档说明，并恢复到 `D-TASK-031` 已验证基线
- Validation:
  - `sqlforge-shared/sql-optimization/query-execution/governance 模块测试、跨服务 acceleration plan 契约测试、runtime smoke、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-032`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governed acceleration-plan submit/approve/apply/verify/rollback flow across sql-optimization, query-execution, governance, shared contracts, schema, tests, and authority docs while preserving authorization, audit, tenant isolation, and suggestion-first defaults.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-032; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js
  - Residual risk: Production-grade live Hetu evidence, route calibration, and broader environment-backed acceleration execution proof remain follow-up work; suggestion-first remains the safe default when governed apply prerequisites are unavailable.
  - Next step: Proceed to D-TASK-033 to collect production-grade Hetu cluster evidence and route calibration on top of the new governed acceleration-plan baseline.

### D-TASK-031: 推进 `sql-optimization` 真实 parse/rewrite/acceleration suggestion 链

- Status: done
- Completed at: 2026-04-25
- Commit subject: `feat(sql-optimization): D-TASK-031 add real parse rewrite pipeline`
- Priority: 1
- Depends on: `D-TASK-030`
- Scope: 在保留 MySQL `optimization_task` carrier、scheduled worker、统一授权入口与异步任务契约不变的前提下，把 `sql-optimization` 从 placeholder suggestion 推进到真实 SQL parser / AST analysis / rewrite rule / acceleration suggestion pipeline，输出可执行的 rewrite candidate、结构化 parse artifact、加速建议工件与失败阶段证据，不提前引入跨服务自动应用或审批旁路 Tech: `JAVA-BE`,`SQL`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若真实 parse/rewrite/acceleration suggestion 链会绕过统一授权入口、把高风险 rewrite 直接自动应用、对不支持方言假装解析成功，或把 placeholder 工件继续冒充真实结果，需人工确认
- Data impact: `optimization_task` 任务数据、parse/rewrite/acceleration artifact、失败阶段与风险说明、schema/migration 与 runtime smoke 证据
- Rollback / recovery: 保持 MySQL carrier 与 async 契约不变，关闭高风险 rewrite 规则或自动应用分支，回退新增 parser/rewriter/acceleration pipeline 与持久化字段说明，并恢复到 `D-TASK-030` 之后的已验证基线
- Validation:
  - `sql-optimization 模块测试、parse/rewrite/acceleration pipeline 测试、persistence/schema/mapping 校验、runtime smoke、task audit、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-031`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added a legacy-parser-backed sql-optimization pipeline with real AST analysis, conservative rewrite rules, acceleration suggestion generation, structured suggestion/failure persistence, schema/mapping updates, focused tests, and authority-doc synchronization for D-TASK-031.
  - Validation evidence: mvn -B -pl sql-optimization -am test -DskipITs; python3 scripts/foreman.py validate D-TASK-031; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js
  - Residual risk: Rewrite coverage remains intentionally conservative, external queue/callback and acceleration-plan apply governance are still pending, and fingerprint-only submissions can be accepted by contract but will terminate failed because the real parser pipeline requires sqlText.
  - Next step: Proceed to D-TASK-032 to close the governed acceleration-plan apply/verify/rollback loop on top of the new real sql-optimization suggestion baseline.

### D-TASK-030: 推进 provider-authenticated object-storage operations 与 governance-side batch retention/recovery orchestration

- Status: done
- Completed at: 2026-04-25
- Commit subject: `feat(governance): D-TASK-030 add batch artifact retention and recovery`
- Priority: 1
- Depends on: `D-TASK-029`
- Scope: 在保持 repo-local artifact lifecycle 与 `LOCAL_FILE` 默认主路径不变、统一授权入口、治理审计及只读/影子环境边界不变的前提下，为 benchmark-engine/environment-backed object storage 推进 vendor-neutral 的 provider-authenticated object-storage operations，并为 governance 补齐可审计的 artifact batch retention / batch recovery orchestration，把部分失败、回滚及 provider/recovery 证据持续沉淀进现有追溯面；不引入不受治理的 SDK 耦合或明文凭据落仓。 Tech: `JAVA-BE`,`OPS`,`DOCS`. Layer: `common`,`a...
- Plan ref: docs/exec-plans/completed/D-TASK-030-full-auto-execution-plan.md
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若 provider-authenticated operations 需要引入未经确认的 provider-specific SDK/签名机制、把 environment-backed object storage 误写成仓库默认主路径，或让 batch retention/recovery 绕过既有鉴权/审计边界、删除当前仍需保留的 artifact，需人工确认
- Data impact: provider-authenticated / environment-backed object-storage operation 请求与 live-evidence、artifact batch retention/recovery 执行摘要、失败分片、恢复来源与 governance 追溯留痕；不得落仓明文凭据，并持续保持 tenant 级隔离。
- Rollback / recovery: 保持 repo-local lifecycle 与 `LOCAL_FILE` 默认主路径，关闭新增 provider-auth/batch orchestration 默认启用；对部分失败批次保留审计与恢复留痕，回退新增 retention/recovery/provider-auth 语义与文档说明，并恢复到 `D-TASK-029` 已验证基线。
- Validation:
  - `sqlforge-shared/benchmark-engine/governance 模块测试、provider-authenticated object-storage contract 与 auth failure 测试、governance-side batch retention/recovery orchestration 测试、runtime smoke、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-030`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added shared artifact batch DTOs, provider-authenticated environment-backed cleanup scopes, governed benchmark artifact cleanup/recovery behavior, governance-side batch retention/recovery orchestration, focused tests, and authority doc updates for D-TASK-030.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-030; mvn -B -pl sqlforge-shared,benchmark-engine,governance -am -Dtest=BenchmarkArtifactGovernanceOperationServiceTest,BenchmarkArtifactStorageServiceTest,GovernanceHistoryApplicationServiceTest,AuthWebMvcTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/task_audit.py --check --phase pre-closeout.
  - Residual risk: Provider-backed object storage remains vendor-neutral HTTP contract coverage plus environment-backed follow-up; real provider-native signing/retention semantics still require external environment validation.
  - Next step: Extend environment-backed validation against real provider endpoints when external credentials and retention controls are available.

### HARN-038: 修复 Governed Closeout Post-Closeout Healthcheck 与 Runtime Recovery 语义

- Status: done
- Completed at: 2026-04-25
- Commit subject: `fix(governance): repair closeout healthcheck recovery semantics`
- Priority: 1
- Depends on: `HARN-033`
- Scope: 在不改变 `HARN-031` / `HARN-032` / `HARN-033` 已落地治理真值、不引入第二套长期真值的前提下，修复 governed closeout 后置 healthcheck 的自引用误判，并把 commit-succeeded / post-check-failed 的 runtime repair 收口为可审计的标准路径；不得削弱 Main Foreman 唯一 write-back / validate / closeout 入口、不得静默删除失败证据、不得放宽 implementation-time dirty-worktree 阻断。 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Plan ref: docs/exec-plans/completed/HARN-038-full-auto-execution-plan.md
- Matrix context: Phase-A / Story `A-STORY-007` 从无 task 开始的治理自动化
- Human confirmation point: 若要把 runtime repair 扩展为自动抹除失败 evidence、放宽 implementation-time dirty-worktree healthcheck 阻断、或绕过 Main Foreman / task_audit 的既有收口链，需人工确认。
- Data impact: governed closeout / post-closeout runtime state、healthcheck/evidence 判定、执行计划与运行手册文档、以及验证日志与 closeout actual evidence 的治理语义；不修改业务运行时数据，不引入 repo 外第二真值。
- Rollback / recovery: 回退 closeout/healthcheck/runtime repair 语义修复：恢复此前的 governed closeout / post-closeout 判定与 runtime cleanup 行为，保留失败 evidence 与 validation-log 审计链，通过标准 validation 与 task-audit 证明仓库仍保持 Main Foreman 唯一收口和 implementation-time dirty-worktree 阻断边界。
- Validation:
  - `python3 scripts/foreman.py validate HARN-038、python3 -m py_compile scripts/foreman.py scripts/governed_healthcheck.py scripts/governed_v2_support.py、python3 scripts/governed_healthcheck.py --check、python3 scripts/foreman.py compile-governance --check、node scripts/lint-repository-knowledge.js、python3 scripts/task_audit.py --check --phase pre-closeout、python3 scripts/task_audit.py --check --phase post-closeout`
  - `python3 scripts/foreman.py validate HARN-038`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Normalized closeout post-check healthcheck context, added standard closeout-repair recovery flow, and aligned governance docs for post-closeout runtime recovery.
  - Validation evidence: python3 scripts/foreman.py validate HARN-038; python3 -m py_compile scripts/foreman.py scripts/governed_healthcheck.py scripts/governed_v2_support.py; python3 scripts/foreman.py compile-governance --check; node scripts/lint-repository-knowledge.js; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Closeout-repair now covers the standard commit-succeeded/post-check-failed path, but broader synthetic fault-injection coverage for arbitrary post-check failures is still limited to real command reuse.
  - Next step: Use foreman closeout post-checks for governed healthcheck, and run foreman closeout-repair if commit succeeds but a post-check later fails.

### HARN-037: 补齐只读 MCP onboarding / doctor 与治理定位手册

- Status: done
- Completed at: 2026-04-25
- Commit subject: `feat(governance): add read-only MCP doctor and onboarding`
- Priority: 1
- Depends on: `HARN-035`
- Scope: 在 `A-STORY-008` 下新增一个 follow-up 治理/工具任务，把现有只读 MCP 基线从“有规则”推进到“可落地可诊断可上手”：补齐按 category 的本地 onboarding、doctor/healthcheck 和 evidence 写回说明，统一单 agent 本地 MCP 与 multi-agent `mcp_profile` 的只读边界口径，并把禁止可写 MCP、SSH、K8s、数据库执行型 server、repo 落 secret/live inventory 的约束落实到文档、脚本与验证链；Main Foreman 仍是唯一 write-back / validate / closeout 入口。 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Plan ref: docs/exec-plans/completed/HARN-037-full-auto-execution-plan.md
- Matrix context: Phase-A / Story `A-STORY-008` Codex MCP 治理接入
- Human confirmation point: 若要把 MCP doctor/healthcheck 扩展为远端自动运维、可写控制面、repo 落 secret/live inventory，或允许 multi-agent 中除 explorer/validator 外的角色消费 manifest-level `mcp_profile`，需人工确认。
- Data impact: MCP 治理文档、onboarding/doctor/healthcheck 脚本或校验分支、compile/validate/runtime 入口、只读 evidence 写回说明，以及相关 runtime 元数据；不直接修改业务运行时数据，不得把 secret、token、endpoint 或 live server inventory 写入 repo-tracked 文件。
- Rollback / recovery: 回退只读 MCP onboarding / doctor 改造：移除新增的 category onboarding、doctor/healthcheck、evidence 写回说明与定位文案，恢复 `HARN-034` / `HARN-035` 既有只读 MCP 基线，并通过标准 validation 与 task-audit 证明仓库仍保持 Main Foreman 唯一收口、只读 MCP 边界和无 secret/live inventory 落仓语义。
- Validation:
  - `python3 scripts/foreman.py validate HARN-037、python3 scripts/validate_codex_runtime.py、python3 scripts/foreman.py compile-governance --check、python3 scripts/governed_healthcheck.py --check、node scripts/lint-repository-knowledge.js、python3 scripts/task_audit.py --check --phase pre-closeout、python3 scripts/task_audit.py --check --phase post-closeout`
  - `python3 scripts/foreman.py validate HARN-037`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added local-only read-only MCP doctor checks, category onboarding guidance, evidence write-back targets, and consistent product positioning across MCP docs, lint, compiled policy, and runtime validation.
  - Validation evidence: python3 scripts/foreman.py validate HARN-037; python3 scripts/mcp_doctor.py --check --json; python3 scripts/foreman.py compile-governance --check; python3 scripts/validate_codex_runtime.py; node scripts/lint-repository-knowledge.js; python3 scripts/task_audit.py --check --phase pre-closeout.
  - Residual risk: HARN-038 is still not formalized in repo truth, and governed_healthcheck remains intentionally strict about tracked dirty worktrees during in-flight task execution.
  - Next step: Do not execute HARN-038 until it is formalized into the master plan, task matrices, and ledger via the governed shaping/materialization path.

### HARN-036: 修复 governed intake / full-auto 入口治理与统一 execution preview 合同

- Status: done
- Completed at: 2026-04-25
- Commit subject: `feat(governance): harden governed intake execution routing`
- Priority: 1
- Depends on: `HARN-028`,`HARN-035`
- Scope: 在 `A-STORY-007` 下新增一个治理/工具 follow-up 任务，为 SQLForge 的 governed intake / full-auto 入口补齐 requirements artifact gate、统一 execution preview 合同、chat-native router 与 execution mode router，并把 `compile-governance` / `validate_codex_runtime` 提升为 full-auto 主路径硬门禁；不得让 router 在未显式确认前直接触发 `--confirm-run`，不得把 simple task 默认强制路由到 multi-agent，不得削弱 Main Foreman 唯一收口和现有只读 MCP 边界。 Tech: `DOCS`,`OPS`. Layer: `docs`,...
- Plan ref: docs/exec-plans/completed/HARN-036-full-auto-execution-plan.md
- Matrix context: Phase-A / Story `A-STORY-007` 从无 task 开始的治理自动化
- Human confirmation point: 若要让 chat-native router 在未显式确认前直接触发 `--confirm-run`、把 simple task 默认强制路由到 multi-agent、弱化 execution preview 固定字段合同，或削弱 Main Foreman 唯一 write-back / validate / closeout 边界，需人工确认。
- Data impact: governed intake / full-auto 入口脚本、template adapter / hook / router 运行态、execution preview 合同、run_id/confirmation 元数据、验证规则与相关文档索引；不直接修改业务运行时数据。
- Rollback / recovery: 回退 requirements artifact gate、execution preview/router 与 full-auto 硬门禁改造：恢复 `HARN-028` / `HARN-035` 之前的 intake/full-auto 行为，移除新增 preview/router 合同与强制校验分支，并通过标准 validation 与 task-audit 证明 Main Foreman 唯一收口、只读 MCP 边界和审计链未被削弱。
- Validation:
  - `python3 scripts/foreman.py validate HARN-036、bash scripts/governed_intake.sh --help、bash scripts/multi_agent_full_auto.sh --help、bash -n scripts/governed_intake.sh、bash -n scripts/multi_agent_full_auto.sh、python3 -m py_compile scripts/codex_template_adapter.py .codex/hooks/user_prompt_submit.py scripts/validate_codex_runtime.py、python3 scripts/validate_codex_runtime.py、python3 scripts/task_audit.py --check --phase pre-closeout`
  - `python3 scripts/foreman.py validate HARN-036`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added chat-native governed intake routing, unified execution preview, existing-task requirements artifact gating, execution-mode routing, and full-auto governance hard gates; aligned runtime validation and playbooks.
  - Validation evidence: python3 scripts/foreman.py validate HARN-036; python3 scripts/validate_codex_runtime.py; bash -n scripts/governed_intake.sh; bash -n scripts/multi_agent_full_auto.sh; node scripts/lint-repository-knowledge.js.
  - Residual risk: HARN-037 read-only MCP onboarding/doctor changes still need to be restored and closed out separately; HARN-038 is not formalized in repo truth.
  - Next step: Restore HARN-037 changes, revalidate, and close out under its own task boundary.

### HARN-035: 扩展 multi-agent 受控 mcp_profile 只读证据接入

- Status: done
- Completed at: 2026-04-25
- Commit subject: `feat(governance): add multi-agent read-only MCP profiles`
- Priority: 1
- Depends on: `HARN-034`
- Scope: 在 `A-STORY-008` 下新增一个 follow-up 治理/工具任务，为 SQLForge multi-agent 基础设施增加受控 `mcp_profile` 只读证据接入能力，同时严格保持 Main Foreman 唯一收口、worker 禁改台账/closeout 文档和现有 worktree/ownership 审计边界不变。 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Plan ref: docs/exec-plans/completed/HARN-035-full-auto-execution-plan.md
- Matrix context: Phase-A / Story `A-STORY-008` Codex MCP 治理接入
- Human confirmation point: 若要把 `mcp_profile` 从 explorer / validator 的只读证据面扩展到 worker、允许任何角色通过 MCP 执行可写操作，或削弱 Main Foreman 唯一 write-back / validate / closeout 边界，需人工确认。
- Data impact: multi-agent playbook、prompt 模板、manifest 契约、编排脚本与 `.codex/` 运行态元数据；不直接修改业务运行时数据。
- Rollback / recovery: 回退 `mcp_profile` 合同改造：移除 multi-agent 文档、模板和脚本中的 MCP profile 字段与处理分支，恢复无 MCP profile 的现有 multi-agent 基线，并通过标准 validation 与 task-audit 证明收口链未被削弱。
- Validation:
  - `python3 scripts/foreman.py validate HARN-035 --include-task-audit`
  - `python3 scripts/foreman.py compile-governance --check`
  - `bash scripts/multi_agent_prepare.sh --help`
  - `bash scripts/multi_agent_launch.sh --help`
  - `bash scripts/multi_agent_collect.sh --help`
  - `bash scripts/multi_agent_autoplan.sh --help`
  - `bash scripts/multi_agent_full_auto.sh --help`
  - `python3 scripts/validate_codex_runtime.py`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-25: aligned MCP governance docs, rules, validation rules and playbooks with the `mcp_profiles` / `mcp_profile` multi-agent read-only evidence contract.
  - 2026-04-25: compiled governance policy, passed multi-agent script syntax/help checks, passed `validate_codex_runtime.py`, and passed `python3 scripts/foreman.py validate HARN-035 --include-task-audit`.
- Context closeout:
  - Completed scope: Extended SQLForge MCP governance from the HARN-034 baseline to a governed multi-agent mcp_profiles/mcp_profile contract, updated docs, rules, playbooks, prompt templates, manifest template, orchestration scripts, compiled policy artifacts, and runtime validation so explorer/validator can consume read-only external evidence without weakening Main Foreman authority.
  - Validation evidence: python3 scripts/foreman.py compile-governance; bash -n scripts/multi_agent_prepare.sh scripts/multi_agent_launch.sh scripts/multi_agent_collect.sh scripts/multi_agent_autoplan.sh scripts/multi_agent_full_auto.sh scripts/task_materialize.sh; python3 -m py_compile scripts/foreman.py scripts/validate_codex_runtime.py; bash scripts/multi_agent_prepare.sh --help; bash scripts/multi_agent_launch.sh --help; bash scripts/multi_agent_collect.sh --help; bash scripts/multi_agent_autoplan.sh --help; bash scripts/multi_agent_full_auto.sh --help; node scripts/lint-repository-knowledge.js; python3 scripts/validate_codex_runtime.py; python3 scripts/foreman.py validate HARN-035 --include-task-audit
  - Residual risk: The repository still does not track live MCP servers, credentials, or writable MCP flows. worker/Main Foreman/Auto Foreman MCP execution remains intentionally disabled, and real codex exec availability can still depend on local authentication or environment readiness.
  - Next step: Use the readonly-evidence manifest profile only for explorer/validator and keep real server resolution in local user config, env vars, or an external secret store; any writable or worker-facing MCP expansion requires a new formal task.

### HARN-034: 落地最小可用 MCP 治理底座与只读接入边界

- Status: done
- Completed at: 2026-04-25
- Commit subject: `feat(governance): add MCP read-only governance baseline`
- Priority: 1
- Depends on: `HARN-028`,`HARN-033`
- Scope: 在 `A-STORY-008` 下新增一个治理/工具型正式任务，把 SQLForge 的最小 MCP 治理底座和第一批只读 MCP 边界收口到正式文档、规则、验证规则、治理编译和运行时校验入口中，同时保持 Main Foreman 唯一 write-back / validate / closeout 入口不变。 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Plan ref: docs/exec-plans/completed/HARN-034-full-auto-execution-plan.md
- Matrix context: Phase-A / Story `A-STORY-008` Codex MCP 治理接入
- Human confirmation point: 若要把本任务从只读 MCP 扩展为可写 MCP、把真实 connector 凭据或 server 配置落仓、或允许 MCP 绕过 `foreman` / `task_audit` / `closeout` 成为并行治理入口，需人工确认。
- Data impact: 文档真值、规则账本、验证规则、治理编译产物、运行时校验脚本与 Codex 本地使用手册；不直接修改业务运行时数据。
- Rollback / recovery: 按追加式治理回退 MCP 基线：移除本任务新增的 MCP 文档入口、规则、验证规则和自动化校验分支，恢复 `compile-governance` / `validate_codex_runtime` 的既有行为，并通过标准 validation 与 task-audit 证明仓库回到改造前治理基线。
- Validation:
  - `python3 scripts/foreman.py validate HARN-034、python3 scripts/validate_codex_runtime.py、python3 scripts/foreman.py compile-governance --check、node scripts/lint-repository-knowledge.js、python3 scripts/task_audit.py --check --phase pre-closeout`
  - `python3 scripts/foreman.py validate HARN-034`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added SQLForge MCP governance baseline docs, connector/security boundary registry, MCP rule and validation-rule appendices, compiled mcp-policy.json, MCP-aware runtime validation, and local Codex MCP usage guidance while keeping Main Foreman as the only write-back/validate/closeout entry.
  - Validation evidence: python3 scripts/foreman.py compile-governance; node scripts/lint-repository-knowledge.js; python3 scripts/validate_codex_runtime.py; python3 scripts/foreman.py validate HARN-034 --include-task-audit
  - Residual risk: The repository now governs only the read-only MCP baseline. Live server inventory, repo-tracked runtime config, and multi-agent mcp_profile remain intentionally disabled until HARN-035 or another formal follow-up lands.
  - Next step: Materialize and implement HARN-035 so explorer/validator can consume governed external evidence through manifest-level mcp_profile without changing Main Foreman authority.

### HARN-033: Codex template and runtime evidence production hardening

- Status: done
- Completed at: 2026-04-25
- Commit subject: `feat(governance): harden codex template runtime production path`
- Priority: 1
- Depends on: HARN-032
- Scope: Productionize the HARN-032 Codex natural template and governed full-cycle runtime lifecycle by adding legacy candidate-pack compatibility, closeout actual-evidence health checks, stronger template parsing/schema/smoke coverage, cleanup preview semantics, and a runtime dashboard/cleanup command.
- Validation:
  - `python3 scripts/foreman.py validate HARN-033`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-25: implemented legacy candidate-pack compatibility, closeout actual evidence health reporting, template adapter schema/parser hardening, cleanup preview outcome semantics, and governed runtime dashboard/cleanup preview support.
- Context closeout:
  - Completed scope: Productionized the HARN-032 Codex template and governed runtime path by adding legacy candidate-pack authority field compatibility, closeout actual-evidence health reporting, schema-versioned natural template parsing for business/governance/existing-task inputs, cleanup preview outcome semantics, and a governed runtime dashboard/cleanup command.
  - Validation evidence: python3 scripts/foreman.py validate HARN-033 --include-task-audit with focused py_compile, shell syntax, four template adapter smoke cases, and runtime dashboard smoke; python3 -m py_compile scripts/codex_template_adapter.py scripts/governed_healthcheck.py scripts/governed_v2_support.py scripts/governed_runtime_dashboard.py; bash -n scripts/task_materialize.sh scripts/requirements_to_plan.sh scripts/governed_intake.sh scripts/governed_full_cycle.sh; bash scripts/task_materialize.sh --task-pack .codex/state/task-shaping/harn028-smoke/candidate-task-pack.json --dry-run; python3 scripts/governed_healthcheck.py --check --cleanup-dry-run --run-id harn033-cleanup-preview; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/foreman.py compile-governance --check; node scripts/lint-repository-knowledge.js
  - Residual risk: Runtime .codex/state evidence remains untracked by design, and cleanup/archive actions remain explicit human-reviewed operations. Live Codex execution availability is still environment-dependent and validate_codex_runtime may report skipped-timeout rather than proving live multi-agent availability.
  - Next step: Use codex_template_adapter.py schema_version=2 and governed_runtime_dashboard.py for future natural-template entry and runtime evidence review; consider hook-level template detection only if humans want Codex prompts to invoke the adapter automatically.

### HARN-032: Governed full-cycle V4 entrypoint and evidence hardening

- Status: done
- Completed at: 2026-04-25
- Commit subject: `feat(governance): harden governed full-cycle v4 entrypoints`
- Priority: 1
- Depends on: HARN-031
- Scope: Harden HARN-031 follow-up gaps by enforcing healthcheck gates before real governed execution, writing post-closeout actual runtime evidence, adding Codex natural template adaptation, extending runtime cleanup/retention, and covering the template path with smoke validation.
- Validation:
  - `python3 scripts/foreman.py validate HARN-032`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-25: implemented mandatory pre-execution health gates, post-closeout actual runtime evidence, Codex natural template adapter, cleanup preview/dashboard fields, and explicit authority_fields_to_confirm plumbing.
- Context closeout:
  - Completed scope: Hardened governed full-cycle V4 entrypoints by enforcing pre-execution healthcheck gates before real governed_intake/governed_full_cycle execution, adding post-closeout actual runtime evidence, introducing a Codex natural template adapter, extending cleanup preview/dashboard runtime summaries, and plumbing explicit authority_fields_to_confirm through candidate packs and suggestions.
  - Validation evidence: python3 scripts/foreman.py validate HARN-032 --include-task-audit --extra-command <py_compile/governed shell syntax/template adapter smoke/healthcheck help>; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/foreman.py compile-governance --check; node scripts/lint-repository-knowledge.js; python3 scripts/codex_template_adapter.py --run-id harn032-template-smoke --template-text <治理需求 smoke>; python3 scripts/governed_healthcheck.py --check --cleanup-dry-run --run-id harn032-cleanup-preview
  - Residual risk: Real governed execution is now gated by healthcheck, so active implementation dirty state intentionally blocks confirm/full-cycle runs until committed. Runtime .codex evidence remains untracked by design; cleanup-dry-run previews intake/task-shaping evidence but only releases safe stale reservations automatically.
  - Next step: Use codex_template_adapter.py for natural template entry and inspect .codex/state/closeout/<TASK_ID>/post-closeout-actual.json after closeout; consider a later task for fully automated deletion of reviewed runtime evidence if desired.

### HARN-031: HARN-028 governed full-cycle V3 audit-hardening repair

- Status: done
- Completed at: 2026-04-25
- Commit subject: `fix(governance): HARN-028 V3 audit hardening`
- Priority: 1
- Depends on: HARN-028
- Scope: Repair HARN-028 governed full-cycle V2 audit semantics, archived Plan refs, healthcheck dirty detection, dry-run state semantics, runtime state cleanup, and Codex newcomer task-entry docs.
- Validation:
  - `python3 scripts/foreman.py validate HARN-031`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-25: implemented V3 governance repairs for closeout projected evidence semantics, archived Plan ref rewriting/audit, healthcheck tracked-dirty blocking, intake dry-run state naming, reservation release/cleanup lifecycle, and Codex newcomer templates.
- Context closeout:
  - Completed scope: Repaired HARN-028 governed full-cycle V2 hardening gaps by separating projected closeout evidence from actual pass semantics, rewriting archived Plan refs to completed paths, blocking tracked dirty worktrees in healthcheck, correcting intake dry-run confirmation state, adding reservation release/cleanup lifecycle support, and documenting Codex newcomer entry templates.
  - Validation evidence: python3 -m py_compile scripts/foreman.py scripts/task_audit.py scripts/governed_healthcheck.py scripts/governed_v2_support.py; bash -n scripts/governed_intake.sh scripts/requirements_to_plan.sh scripts/task_materialize.sh; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/foreman.py compile-governance --check; node scripts/lint-repository-knowledge.js; bash scripts/requirements_to_plan.sh --run-id harn031-requirements-dry-run --task-prefix HARN --prompt <smoke> --dry-run; bash scripts/governed_intake.sh --confirm-run harn028-intake-no-task --dry-run; python3 scripts/governed_healthcheck.py --check --run-id harn031-dirty-check; python3 scripts/foreman.py validate HARN-031 --include-task-audit --extra-command <focused script checks>
  - Residual risk: Healthcheck now intentionally blocks tracked dirty implementation states, so implementation-time healthcheck runs are expected to report tracked_dirty_worktree until closeout commits the tracked patch. Existing untracked .codex runtime evidence remains non-authoritative runtime state and should be cleaned with governed cleanup when stale.
  - Next step: Use the new Codex daily input templates and HARN-031 audit gates on the next governed full-cycle request; monitor whether authority_fields_to_confirm needs a later structured schema upgrade.

### HARN-028: 加固 governed full-cycle V2 intake / healthcheck / closeout 完整性

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(governance): HARN-028 harden governed full-cycle v2`
- Priority: 1
- Depends on: `HARN-027`
- Scope: 在不改变 `HARN-027` no-task 起步真值、不引入第二套长期真值的前提下，补齐 governed intake/healthcheck 入口、machine-readable run summary 的 `executed_commands` 与细粒度 suggestion 字段、candidate materialization rollback，以及 closeout 后不得留下 tracked `validation-log` residue 的仓库级修复 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Plan ref: docs/exec-plans/completed/HARN-028-governed-full-cycle-v2-hardening-plan.md
- Matrix context: Phase-A / Story `A-STORY-007` 从无 task 开始的治理自动化
- Human confirmation point: 若要把 governed intake/healthcheck 升级为跳过确认直接改写台账真值、让 healthcheck 自动回滚或重写 closeout 记录，或允许 closeout 修复继续回到提交后追加 tracked `validation-log` 的模式，需人工确认
- Data impact: governed intake/healthcheck 入口、run summary/建议字段、candidate materialization rollback、reservation 状态，以及 closeout 与 validation-log 的仓库级治理语义；不直接改变业务运行时数据
- Rollback / recovery: 停用 intake/healthcheck 入口并回退到 `HARN-027` 的 requirements-to-plan / task-materialize 手工组合路径，保留 run summary / healthcheck 证据与回滚记录，必要时拆出更细粒度的 runtime hardening follow-up
- Validation:
  - `python3 scripts/foreman.py validate HARN-028`、`bash scripts/governed_intake.sh --help`、`python3 scripts/governed_healthcheck.py --check`、`bash scripts/task_materialize.sh --help`、`bash -n scripts/governed_intake.sh`、`bash -n scripts/task_materialize.sh`、`python3 -m py_compile scripts/governed_v2_support.py scripts/governed_healthcheck.py`、docs 索引/手册对齐
  - `python3 scripts/foreman.py validate HARN-028`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governed_full_cycle V2 hardening on top of HARN-027: governed intake and healthcheck entrypoints, machine-readable summaries with executed_commands and granular suggestions, task materialization rollback/reservation handling, closeout validation-log residue repair, and aligned docs/plan coverage updates without changing business functionality.
  - Validation evidence: bash -n scripts/requirements_to_plan.sh; bash -n scripts/task_materialize.sh; bash -n scripts/governed_intake.sh; python3 -m py_compile scripts/governed_v2_support.py scripts/governed_healthcheck.py scripts/foreman.py; bash scripts/governed_intake.sh --help; python3 scripts/governed_healthcheck.py --check; bash scripts/requirements_to_plan.sh --run-id harn028-smoke --task-prefix HARN --prompt <smoke>; bash scripts/task_materialize.sh --task-pack .codex/state/task-shaping/harn028-smoke/candidate-task-pack.json --dry-run; bash scripts/governed_intake.sh --run-id harn028-intake-smoke --task HARN-028; bash scripts/governed_intake.sh --run-id harn028-intake-no-task --prompt <smoke>; bash scripts/governed_intake.sh --confirm-run harn028-intake-no-task --dry-run; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance; python3 scripts/foreman.py validate HARN-028; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Governed no-task intake still depends on Codex shaping latency and prompt quality; candidate packs can legitimately stop at human-confirmation boundaries, and future hardening may still be needed if summary/intake contracts expand beyond the current CLI + task-materialization surfaces.
  - Next step: Use governed_intake.sh for future short-input governance or business-task shaping, and if repeated runs show the same human-confirmation ambiguity, split narrower domain-specific shaper prompts instead of weakening the confirmation gate.

### HARN-027: 落地从无 task 开始的 governed full-cycle 自动化

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(governance): HARN-027 add no-task governed full-cycle`
- Priority: 1
- Depends on: `HARN-026`
- Scope: 在不改变 `HARN-026` downstream full-auto 真值、不引入第二套长期真值的前提下，新增 requirement normalization、candidate task pack、governance gate 与 materialization 脚本/模板/手册，让 Codex 可以从“只有需求”开始先塑形 formal task，再继续交给现有 full-auto 执行链 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Plan ref: docs/exec-plans/completed/HARN-027-requirements-to-task-governed-full-cycle-plan.md
- Matrix context: Phase-A / Story `A-STORY-007` 从无 task 开始的治理自动化
- Human confirmation point: 若要允许 candidate task 在未同步 master-execution-plan/task-spec/task-governance 矩阵前直接进入编码、自动越过 INBOX/人工确认点，或把“无 task 起步”的自动化扩展为可绕过 `preflight` / `instantiate` / `validate` / `task_audit` / `closeout` 的黑盒执行，需人工确认
- Data impact: requirement-normalizer/plan-shaper/task-shaper/task-governance-reviewer prompt 模板、candidate task pack 模板、task-shaping 运行态、requirements-to-task/materialization/full-cycle 脚本，以及由 formal materialization 写入的 plan/matrix/ledger/exec-plan/raw-requirement 记录；不直接改变业务运行时数据
- Rollback / recovery: 停用 governed full-cycle 脚本并回退到“人工写 plan + 人工建 task + `HARN-026` downstream full-auto”路径，保留 candidate task pack 和 review 证据作为治理记录，必要时拆出更细粒度的 task-shaping follow-up
- Validation:
  - `python3 scripts/foreman.py validate HARN-027`、`bash scripts/requirements_to_plan.sh --help`、`bash scripts/task_materialize.sh --help`、`bash scripts/governed_full_cycle.sh --help`、requirements-to-plan dry-run、real candidate pack generation、task_materialize dry-run、governed_full_cycle dry-run、docs 索引与 coverage 矩阵对齐
  - `python3 scripts/foreman.py validate HARN-027`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governed no-task full-cycle automation on top of the existing full-auto multi-agent stack: requirement normalization, candidate execution plan shaping, candidate task pack shaping, governance-gated materialization, and end-to-end orchestrator documentation/scripts. Also hardened nested Codex execution so shaping no longer pollutes current-task state and downstream child sessions run correctly in the externally sandboxed automation environment.
  - Validation evidence: bash -n scripts/requirements_to_plan.sh; bash -n scripts/task_materialize.sh; bash -n scripts/governed_full_cycle.sh; bash -n scripts/multi_agent_autoplan.sh; bash -n scripts/multi_agent_launch.sh; bash -n scripts/multi_agent_full_auto.sh; bash scripts/requirements_to_plan.sh --help; bash scripts/task_materialize.sh --help; bash scripts/governed_full_cycle.sh --help; bash scripts/requirements_to_plan.sh --run-id harn027-smoke4 --task-prefix HARN --prompt <smoke> ; bash scripts/task_materialize.sh --task-pack .codex/state/task-shaping/harn027-smoke4/candidate-task-pack.json --dry-run; bash scripts/governed_full_cycle.sh --run-id harn027-smoke4 --task-prefix HARN --prompt <smoke> --dry-run; bash scripts/multi_agent_autoplan.sh --task HARN-027 --prompt <dry-run> --dry-run; bash scripts/multi_agent_launch.sh --manifest .codex/state/multi-agent/HARN-026/validation/generated-manifest.json --dry-run; python3 scripts/foreman.py compile-governance; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py validate HARN-027; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Governed full-cycle still depends on Codex CLI availability and prompt quality; ambiguous raw requirements will continue to stop at the human-confirmation gate instead of auto-materializing, and downstream task execution still inherits the operational limits of the existing HARN-026 full-auto stack.
  - Next step: Use docs/operations/requirements-to-task-playbook.md and scripts/governed_full_cycle.sh on the next governance/tooling request that starts without a formal task, then evaluate whether the shaping prompts need narrower domain-specific templates for repeated requirement classes.

### HARN-026: 把多 agent 基础设施升级为从需求到收口的全自动主路径

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(governance): HARN-026 add full-auto multi-agent orchestration`
- Priority: 1
- Depends on: `HARN-025`
- Scope: 在不改变 `HARN-025` 半自动真值、不引入第二套长期真值的前提下，新增 requirement-driven auto-planner / auto-foreman prompt 模板与 autoplan/full-auto orchestration 脚本，让 codex 可以从需求输入自动生成 exec plan、manifest，并驱动 prepare/launch/collect 与最终 autonomous Main Foreman 收口 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Plan ref: docs/exec-plans/completed/HARN-026-full-auto-multi-agent-upgrade-plan.md
- Matrix context: Phase-A / Story `A-STORY-006` 全自动多 agent 协作编排
- Human confirmation point: 若要把全自动路径升级为“无任务治理前置、无 Main Foreman 收口、可绕过 `foreman validate/task_audit/closeout` 的黑盒自动执行”，或允许 auto-planner / auto-foreman 直接改写台账真值而不经过仓库审计链，需人工确认
- Data impact: auto-planner / auto-foreman prompt 模板、requirement-driven exec plan 与 manifest 生成脚本、`.codex/state` 下的 full-auto 运行态产物，以及由 autonomous Main Foreman 落地到 `docs/` / 台账的最终收口记录；不直接改变业务运行时数据
- Rollback / recovery: 停用 full-auto 脚本并回退到 `HARN-025` 半自动模式，保留需求输入、生成的 plan/manifest 和失败日志作为治理证据，必要时拆出更细粒度的 auto-planning/closeout follow-up
- Validation:
  - `python3 scripts/foreman.py validate HARN-026`、`bash scripts/multi_agent_autoplan.sh --help`、`bash scripts/multi_agent_full_auto.sh --help`、autoplan dry-run、自生成 manifest 的 prepare/launch dry-run、docs 索引与 coverage 矩阵对齐
  - `python3 scripts/foreman.py validate HARN-026`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added full-auto multi-agent governance capability on top of HARN-025, including auto-planner and auto-foreman prompt templates, full-auto orchestration scripts, manifest contract updates, and playbook/index/coverage synchronization without changing business functionality.
  - Validation evidence: python3 scripts/foreman.py validate HARN-026; bash scripts/multi_agent_autoplan.sh --help; bash scripts/multi_agent_full_auto.sh --help; real autoplan generation to .codex/state/multi-agent/HARN-026/validation/generated-plan.md and generated-manifest.json; bash scripts/multi_agent_prepare.sh --task HARN-026 --manifest .codex/state/multi-agent/HARN-026/validation/generated-manifest.json --dry-run; bash scripts/multi_agent_launch.sh --manifest .codex/state/multi-agent/HARN-026/validation/generated-manifest.json --dry-run; node scripts/lint-repository-knowledge.js
  - Residual risk: The autonomous Main Foreman path has dry-run and integration proof but still depends on live codex exec stability and longer end-to-end smoke should remain a follow-up if orchestration semantics change again.
  - Next step: Use HARN-026 as the downstream execution substrate for HARN-027 so requirement-to-task automation can hand off only after a formal task has been materialized and instantiated.

### HARN-025: 落地半自动多 agent 协作基础设施（C方案）

- Status: done
- Completed at: 2026-04-24
- Priority: 1
- Depends on: `HARN-024`
- Scope: 在不改变业务主线事实、不引入第二套长期真值的前提下，新增 multi-agent playbook、agent prompt 模板、manifest 模板，以及基于多 `codex exec` / 多 `git worktree` 的 prepare/launch/collect 半自动编排脚本，保持 Main Foreman 唯一 validate/closeout/commit Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Plan ref: docs/exec-plans/completed/HARN-025-semi-auto-multi-agent-foundation-plan.md
- Matrix context: Phase-A / Story `A-STORY-005` 半自动多 agent 协作治理基础设施
- Human confirmation point: 若要把半自动多 agent 提升为默认自动执行路径、弱化 Main Foreman 唯一收口、允许 worker 修改台账/validation-log/closeout 文档，或用隐式 subagent 取代显式 `codex exec` + worktree 编排，需人工确认
- Data impact: 文档真值、运行期 prompt 模板、manifest 编排、worktree orchestration 脚本，以及 `.codex/` 下的运行态 multi-agent 会话元数据；不影响业务运行时数据
- Rollback / recovery: 停用 multi-agent 脚本与运行态目录，回退新增 docs/模板/脚本到单 agent `foreman` 路径，并保留 prompt/manifest 作为历史治理记录或拆出兼容改造任务
- Commit subject: `feat(governance): HARN-025 add semi-auto multi-agent foundation`
- Validation:
  - `python3 scripts/foreman.py validate HARN-025`、`bash scripts/multi_agent_prepare.sh --help`、`bash scripts/multi_agent_launch.sh --help`、`bash scripts/multi_agent_collect.sh --help`、docs 索引与 coverage 矩阵对齐
  - `python3 scripts/foreman.py validate HARN-025`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: added `HARN-025` to the master execution plan, task-spec matrix, task-governance extension matrix, docs coverage matrix, and an active exec plan so the semi-auto multi-agent foundation is a formal governance/tooling task instead of an ad-hoc script change.
  - 2026-04-24: added `docs/operations/multi-agent-playbook.md`, the `docs/agent-prompts/*.md` role templates, and `docs/exec-plans/templates/multi-agent-run.template.json`; also updated `docs/README.md` and `docs/operations/README.md` so the new workflow is indexed and documented as first-class repository truth.
  - 2026-04-24: implemented `scripts/multi_agent_prepare.sh`, `scripts/multi_agent_launch.sh`, and `scripts/multi_agent_collect.sh`, keeping runtime metadata under `.codex/state/multi-agent/`, then verified help output, prepare/launch dry-run, collect smoke, `python3 scripts/foreman.py compile-governance`, and `python3 scripts/foreman.py validate HARN-025`.
  - 2026-04-24: the first validation run exposed a repository-knowledge lint failure because a placeholder active-manifest path was rendered as a nonexistent docs path; updated the playbook and coverage wording to describe the naming convention without introducing a dead docs link, then reran validation successfully.
- Context closeout:
  - Completed scope: Instantiated HARN-025 as a formal governance/tooling task, added the semi-auto multi-agent playbook, role prompt templates, manifest template, and active exec plan, implemented prepare/launch/collect orchestration scripts with runtime state under .codex/state/multi-agent, updated docs indexes and coverage, and kept Main Foreman as the only validate/closeout/commit entrypoint without mixing business functionality.
  - Validation evidence: python3 scripts/foreman.py validate HARN-025; bash scripts/multi_agent_prepare.sh --task HARN-025 --manifest .codex/state/multi-agent/HARN-025-dry-run.json --bootstrap-if-missing --dry-run; bash scripts/multi_agent_launch.sh --manifest .codex/state/multi-agent/HARN-025-dry-run.json --dry-run; bash scripts/multi_agent_collect.sh --manifest .codex/state/multi-agent/HARN-025-collect-smoke.json; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The new capability is intentionally semi-auto: Main Foreman still has to write task-specific manifests, review fan-in, and choose the final validation chain manually; successful real-world use also still depends on clean worktrees, clear ownership boundaries, and local Codex CLI/auth availability. The validation log also contained a pre-existing append-only HARN-024 closeout tail before this task started, and the first failed HARN-025 lint attempt is intentionally preserved as audit evidence.
  - Next step: For the next complex cross-module task, copy the manifest template into docs/exec-plans/active/, fill task-specific ownership/worktree rules, and run prepare/launch/collect before deciding whether more automation is justified.

### HARN-024: 收口 D-TASK-029 closeout 后的 active-wave / validation-log 漂移

- Status: done
- Completed at: 2026-04-24
- Commit subject: `chore(governance): realign active wave after D-TASK-029`
- Priority: 1
- Depends on: D-TASK-029
- Scope: 只修正 D-TASK-029 closeout 后遗留的计划真值与 append-only validation-log 漂移，恢复当前没有已实例化 repo-side mainline task 的仓库事实；不改写 D-TASK-029 的历史完成结论，也不实例化新的 mainline 业务任务。
- Validation:
  - `python3 scripts/foreman.py validate HARN-024`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Repaired the post-closeout active-wave drift left after D-TASK-029, restored the master plan truth to no instantiated repo-side mainline task, regenerated governance policy authority artifacts, and absorbed the append-only validation-log residue into a ledger-bound governance closeout.
  - Validation evidence: python3 scripts/foreman.py validate HARN-024; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/foreman.py compile-governance
  - Residual risk: The repository is back to no instantiated repo-side mainline task, but the next Phase-D business follow-up still needs explicit shaping before implementation; no new mainline task is instantiated by this governance repair.
  - Next step: If work continues immediately, shape a new Phase-D follow-up around provider-authenticated object-storage operations and governance-side batch retention/recovery orchestration before instantiation.

### D-TASK-029: 推进 provider-native / environment-backed object-storage live evidence 与 governance-triggered artifact cleanup/recovery operation surfaces

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(governance): add benchmark artifact operation surfaces`
- Priority: 1
- Depends on: `D-TASK-028`
- Scope: 在保持 repo-local artifact lifecycle 与 `LOCAL_FILE` 默认主路径不变、统一授权入口、治理审计及只读/影子环境边界不变的前提下，为 benchmark-engine/environment-backed object storage 推进更接近 provider-native 的 live evidence 沉淀，并为 governance 补齐可审计的 artifact cleanup/recovery operation surface 与受控触发链路 Tech: `JAVA-BE`,`OPS`,`DOCS`. Layer: `common`,`application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-D
- Human confirmation point: 若 provider-native live evidence 会引入未经确认的 SDK/凭据写入、把 environment-backed object storage 误写成仓库默认主路径，或让 governance-triggered cleanup/recovery 绕过既有鉴权/审计边界、删除当前仍需保留的 artifact，需人工确认
- Data impact: provider-native / environment-backed live-evidence 配置与 manifest、artifact cleanup/recovery operation 请求/审计/追溯留痕，以及 governance 历史操作面返回的 operation surface
- Rollback / recovery: 保持 repo-local lifecycle 与 `LOCAL_FILE` 默认主路径，关闭新增 provider-native live evidence 与 governance-triggered operation 默认启用，回退 cleanup/recovery operation surface 与 live-evidence 语义说明，并恢复到 `D-TASK-028` 已验证基线
- Validation:
  - `sqlforge-shared/benchmark-engine/governance 模块测试、provider-native live-evidence 契约测试、governance-triggered cleanup/recovery operation 测试、runtime smoke、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-029`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added shared benchmark artifact operation contracts, benchmark-engine internal cleanup/recovery operations, provider-native object-storage live evidence enrichment, governance-triggered artifact operation route, governance history artifactOperationSurface aggregation, tests, and architecture truth sync while keeping LOCAL_FILE as the default repo-side path.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-029; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Provider-native evidence still uses HTTP-based header capture and repo-controlled environment configuration; true cloud-signed/provider-SDK live operations remain a future environment-backed follow-up and are not the default repo path.
  - Next step: Shape the next Phase-D task around stronger provider-authenticated object-storage operations and broader governance-side batch retention/recovery orchestration without changing the LOCAL_FILE default.

### D-TASK-028: 收口 provider-specific / multi-provider object-storage contract 与 cleanup/recovery 语义，并提升 compensation-replay evidence 的治理查询/恢复面

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(benchmark-engine): add multi-provider artifact recovery surfaces`
- Priority: 1
- Depends on: `D-TASK-027`
- Scope: 在保持 repo-local artifact lifecycle 与 `LOCAL_FILE` 默认主路径不变、统一授权入口、治理审计及只读/影子环境边界不变的前提下，为 benchmark-engine 收口 provider-specific / multi-provider object-storage contract、cleanup/recovery / failure-replay 语义，并把 compensation-replay 与 artifact recovery/provider evidence 提升为 governance 历史查询与恢复操作面的显式结构字段 Tech: `JAVA-BE`,`OPS`,`DOCS`. Layer: `common`,`application(controller/service)/domain/infrastructure...
- Matrix context: Phase-D
- Human confirmation point: 若 provider-specific / multi-provider contract 会把 provider 差异误写成统一默认能力、让 cleanup/recovery 误删仍需保留的 artifact、绕过既有只读/鉴权/审计边界，或把 provider-backed object storage 误写成仓库默认主路径，需人工确认
- Data impact: provider-specific / multi-provider artifact contract 配置、cleanup/recovery/failure-replay 证据、benchmark/query-execution compensation-replay 结构载荷，以及 governance 历史查询/恢复面的 provider 与 recovery 留痕
- Rollback / recovery: 保持 repo-local lifecycle 与 `LOCAL_FILE` 默认主路径，关闭新增 provider/multi-provider 默认启用，回退 cleanup/recovery/provider 语义与治理查询字段说明，并恢复到 `D-TASK-027` 已验证基线
- Validation:
  - `sqlforge-shared/query-execution/benchmark-engine/governance 模块测试、provider/multi-provider artifact contract 测试、governance query/detail 契约测试、runtime smoke、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-028`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented provider-specific and multi-provider environment-backed artifact storage contracts, cleanup/recovery semantics, recovery-source/read-status propagation, and governance trace summary/detail surfaces for compensation replay and artifact storage/recovery evidence; synchronized repository truth docs and kept LOCAL_FILE as the default repo-side path.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-028; python3 scripts/task_audit.py --check --phase pre-closeout; mvn -B -pl benchmark-engine,governance -am clean test -DskipITs
  - Residual risk: The environment-backed path now covers generic HTTP primary/recovery provider verification and governance recovery surfaces, but provider-native SDK semantics, broader external cleanup orchestration, and longer-lived environment evidence retention remain follow-up work. Current plan truth therefore returns to no instantiated repo-side mainline task after this closeout.
  - Next step: Keep no instantiated repo-side mainline task. If work continues immediately, shape D-TASK-029 around provider-native/environment-backed object-storage live evidence plus governance-triggered artifact cleanup/recovery operation surfaces before implementation.

### D-TASK-027: 收口更深层 workload compensation-replay orchestration 与 provider-backed object-storage live evidence

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(benchmark-engine): add compensation replay storage evidence`
- Priority: 1
- Depends on: `D-TASK-026`
- Scope: 在保持 repo-local artifact lifecycle 与 `LOCAL_FILE` 默认主路径不变、统一授权入口、治理审计及只读/影子环境边界不变的前提下，为 benchmark-engine/query-execution 补齐更深层的 workload compensation-replay orchestration，并把 environment-backed object-storage 从 writable-dir verification 推进到 provider-backed live evidence/readback recovery verification Tech: `JAVA-BE`,`OPS`,`DOCS`. Layer: `common`,`application(controller/service)/domain/infrastruct...
- Matrix context: Phase-D
- Human confirmation point: 若 compensation-replay orchestration 会绕过 `query-execution` 既有只读/鉴权/审计边界、把 compensated replay 冒充成原始 live capture，或把 provider-backed object-storage live evidence 误写成仓库默认主路径，需人工确认
- Data impact: benchmark/query-execution compensation-replay 证据、governance 长期追溯载荷、provider-backed object storage write/readback/recovery 留痕
- Rollback / recovery: 保持 repo-local lifecycle 与 `LOCAL_FILE` 默认主路径，关闭 provider-backed live evidence 默认启用，回退新增 compensation/provider 语义与文档说明，并恢复到 `D-TASK-026` 已验证基线
- Validation:
  - `sqlforge-shared/query-execution/benchmark-engine/governance 模块测试、跨服务 compensation-replay 契约测试、provider-backed object-storage adapter/live-evidence 测试、runtime smoke、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-027`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: completed ledger-bound implementation for query-execution compensation-replay orchestration, benchmark execution-summary/governance trace enrichment, and provider-backed object-storage live evidence/readback recovery verification while keeping `LOCAL_FILE` as the default repo-side path.
  - 2026-04-24: verified repo-side behavior with `mvn -B -pl sqlforge-shared,query-execution,benchmark-engine,governance -am test -DskipITs`; governance persistence coverage now asserts compensation evidence and provider verification summaries.
- Context closeout:
  - Completed scope: Implemented benchmark/query-execution compensation-replay orchestration, persisted compensation/provider verification evidence through benchmark/governance trace payloads, added provider-backed object-storage write/readback recovery verification on the environment-backed path, expanded governance persistence coverage, and synchronized authority docs while keeping LOCAL_FILE as the default repo-side path.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-027 --include-task-audit --extra-command 'mvn -B -pl sqlforge-shared,query-execution,benchmark-engine,governance -am test -DskipITs' --extra-command 'python3 scripts/foreman.py compile-governance --check' --extra-command 'bash scripts/run-runtime-smoke.sh --compose-check' --extra-command 'node scripts/lint-repository-knowledge.js'
  - Residual risk: Provider-backed live evidence currently validates against the configured endpoint via generic HTTP write/readback semantics; provider-specific SDK behavior, multi-provider retention/recovery contracts, and deeper compensation-replay governance query/recovery tooling remain follow-up work. Current plan truth therefore returns to no instantiated repo-side mainline task after this closeout.
  - Next step: Keep no instantiated repo-side mainline task. If work continues immediately, shape D-TASK-028 around provider-specific/multi-provider object-storage contract and cleanup/recovery semantics plus deeper governance query/recovery surfaces for compensation-replay evidence.

### D-TASK-026: 把 workload/backfill evidence 沉淀进 governance 长期追溯链，并推进真实 external write/recovery verification

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(governance): persist benchmark workload storage evidence`
- Priority: 1
- Depends on: `D-TASK-025`
- Scope: 在保持 repo-local artifact lifecycle 与 `LOCAL_FILE` 默认主路径不变、统一授权入口、治理审计及只读/影子环境边界不变的前提下，把 benchmark/query-execution 的 workload/backfill 证据提升为 governance 长期追溯链中的显式结构化字段，并把 environment-backed object-storage 从 repo-side live-evidence manifest 推进到真实 external write/readback recovery verification Tech: `JAVA-BE`,`OPS`,`DOCS`. Layer: `common`,`application(controller/service)/domain/infrastructure`,`deplo...
- Matrix context: Phase-D
- Human confirmation point: 若 workload/backfill evidence 的治理沉淀会弱化既有只读/鉴权/审计边界、把 synthetic backfill 冒充成真实 live capture，或把 environment-backed external write/recovery verification 误写成仓库默认主路径，需人工确认
- Data impact: governance `config_snapshot/execution_result/query_history/export_record` 追溯载荷、benchmark/query-execution workload/backfill 证据、environment-backed object storage external write/readback evidence 与恢复留痕
- Rollback / recovery: 保持 repo-local lifecycle 与 `LOCAL_FILE` 默认主路径，关闭 external write/recovery verification 默认启用，回退新增治理字段/adapter 语义与文档说明，并恢复到 `D-TASK-025` 已验证基线
- Validation:
  - `sqlforge-shared/governance/query-execution/benchmark-engine 模块测试、跨服务 workload/backfill 与 trace persistence 契约测试、artifact adapter verification 测试、runtime smoke、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-026`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Persisted benchmark workload/backfill evidence into governance trace payloads, extended environment-backed artifact storage to real external write/readback recovery verification, added contract/tests, and synchronized authority docs while keeping LOCAL_FILE as the default path.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-026 --include-task-audit --extra-command 'mvn -B -pl sqlforge-shared,governance,benchmark-engine -am test -DskipITs' --extra-command 'python3 scripts/foreman.py compile-governance --check' --extra-command 'bash scripts/run-runtime-smoke.sh --compose-check' --extra-command 'node scripts/lint-repository-knowledge.js'
  - Residual risk: Governance trace now preserves repo-side workload/backfill and writable-dir-based external storage verification evidence, but deeper cross-service workload compensation/replay orchestration and provider-backed object storage live evidence beyond the current external write dir baseline remain follow-up work.
  - Next step: Shape the next repo-side follow-up around deeper benchmark/query-execution workload compensation-replay orchestration plus provider-backed object-storage live evidence beyond the current writable-dir verification baseline.

### D-TASK-025: 收口 `benchmark-engine` / `query-execution` workload/backfill orchestration 与真实环境 object storage live evidence

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(benchmark-engine): orchestrate workload backfill evidence`
- Priority: 1
- Depends on: `D-TASK-024`
- Scope: 在保持 repo-local artifact lifecycle 仍是默认主路径、统一授权入口、治理审计与只读/影子环境边界不变的前提下，为 `benchmark-engine` 补齐面向 `query-execution` 的 workload/backfill 内部编排契约，并把真实环境 object storage live evidence 沉淀为显式 environment-backed 证据而非仓库默认主路径 Tech: `JAVA-BE`,`OPS`,`DOCS`. Layer: `common`,`application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-D
- Human confirmation point: 若 workload/backfill orchestration 会绕过 `query-execution` 现有只读/鉴权/审计边界、把 synthetic evidence 冒充成真实环境 live evidence、或把 environment-backed object storage 重新写成 repo-side 默认主路径，需人工确认
- Data impact: benchmark/query-execution 内部 workload snapshot 与 backfill 证据、跨服务执行/审计记录、environment-backed object storage live evidence 与 runbook/验证留痕
- Rollback / recovery: 保持 repo-local lifecycle 与 synthetic fallback 为默认仓库路径，关闭新增跨服务编排或 live evidence 默认启用，回退内部契约/文档说明并恢复到 `D-TASK-024` 已验证基线
- Validation:
  - `sqlforge-shared/query-execution/benchmark-engine 模块测试、跨服务 workload/backfill 契约测试、runtime smoke、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-025`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented benchmark/query-execution workload orchestration, explicit synthetic backfill evidence, and environment-backed object-storage live-evidence manifest while keeping LOCAL_FILE as the default artifact path.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-025 --include-task-audit --extra-command 'mvn -B -pl sqlforge-shared,query-execution,benchmark-engine -am test -DskipITs' --extra-command 'python3 scripts/foreman.py compile-governance --check' --extra-command 'bash scripts/run-runtime-smoke.sh --compose-check' --extra-command 'node scripts/lint-repository-knowledge.js'
  - Residual risk: Workload/backfill evidence is now repo-side orchestrated, but long-term governance persistence of that evidence and real external object-storage write/recovery proof still remain environment-backed follow-up work.
  - Next step: Shape the next repo-side follow-up around persisting workload/backfill evidence deeper into governance traceability and extending environment-backed object-storage verification from live-evidence manifests to real external write/recovery proof.

### D-TASK-024: 收口 `benchmark-engine` artifact tenant-specific retention/backfill policy 与 environment-backed storage adapter/evidence

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(benchmark-engine): add tenant artifact policy adapter`
- Priority: 1
- Depends on: `D-TASK-023`
- Scope: 在保持 repo-local artifact lifecycle 仍是默认主路径的前提下，为 benchmark artifact 增加 tenant-specific retention/backfill policy 语义，并补齐 environment-backed object-storage adapter/evidence 的明确边界、接线与验证基线 Tech: `JAVA-BE`,`SQL`,`OPS`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-D
- Human confirmation point: 若 tenant-specific retention/backfill policy 会误删仍需保留的 artifact、让 environment-backed adapter 变成 repo-side 默认主路径、或引入未经确认的真实对象存储依赖/凭据写入，需人工确认
- Data impact: benchmark artifact policy 配置、repo-local / environment-backed storage adapter 接线、artifact evidence 与 recovery/backfill 记录
- Rollback / recovery: 保持 repo-local lifecycle 为默认主路径，关闭 environment-backed adapter 默认启用，回退新增 artifact policy/adapter 语义与文档说明
- Validation:
  - `benchmark-engine/governance 模块测试、artifact policy/adapter 契约测试、runtime smoke、task audit、schema/mapping 校验、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-024`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: implemented tenant-specific artifact retention/backfill policy resolution from governance `tenant_config.retention_days`, persisted retention metadata into benchmark artifact snapshots, and kept `LOCAL_FILE` as the default lifecycle path.
  - 2026-04-24: added explicit `ENVIRONMENT_OBJECT_STORAGE` adapter semantics with repo-local mirror evidence, object URI metadata, and module tests covering cleanup, recovery, tenant policy backfill, governance contract resolution, and adapter selection.
- Context closeout:
  - Completed scope: Implemented governance-backed tenant artifact retention/backfill policy resolution, persisted retention/evidence metadata into benchmark artifacts, and added explicit ENVIRONMENT_OBJECT_STORAGE adapter semantics while keeping LOCAL_FILE as the default lifecycle path.
  - Validation evidence: Passed foreman validate for D-TASK-024 with module tests, knowledge lint, compile-governance check, runtime smoke, and pre-closeout task audit.
  - Residual risk: Real external object storage upload/live evidence and deeper benchmark/query-execution workload-backfill orchestration remain follow-up work; the repo-side environment-backed adapter currently preserves object URI plus repo-local mirror evidence only.
  - Next step: Shape the next repo-side follow-up around benchmark/query-execution workload-backfill orchestration and real environment object-storage live evidence; until then, keep no instantiated mainline task.

### D-TASK-023: 收口 `benchmark-engine` 报告查询审计追溯增强与 artifact 生命周期基线

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(benchmark-engine): harden report audit and artifact lifecycle`
- Priority: 1
- Depends on: `D-TASK-022`
- Scope: 在保留 repo-closed artifact storage、统一授权入口、治理审计与只读/影子环境边界的前提下，为 benchmark 报告/下载查询补齐 `configSnapshotId/resultId/historyId/exportId` 审计链接，并建立 repo-local artifact retention/recovery/cleanup 语义与验证基线 Tech: `JAVA-BE`,`SQL`,`OPS`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-D
- Human confirmation point: 若查询审计追溯增强会把错误的 trace/export 键写入 `audit_log`、让 cleanup 删除仍应保留的 artifact，或把 repo-local 生命周期语义误升级为环境级对象存储默认路径，需人工确认
- Data impact: benchmark 报告/下载审计记录、`config_snapshot/execution_result/query_history/export_record/audit_log` 链接键、repo-local artifact 文件与 recovery/cleanup 证据
- Rollback / recovery: 恢复到上一版报告查询/下载审计基线，关闭新增 recovery/cleanup 路径，并回退 artifact lifecycle 文档与验证说明
- Validation:
  - `benchmark-engine/governance 模块测试、报告/下载审计契约测试、runtime smoke、task audit、schema/mapping 校验、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-023`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: enriched benchmark report/query download audits with `configSnapshotId/resultId/historyId/exportId` linkage whenever report artifacts already carry governance trace metadata.
  - 2026-04-24: added repo-local artifact lifecycle baseline for keeping the latest report-set, pruning stale sibling files on rewrite, and recovering missing `PDF/HTML/raw-data` files from persisted report snapshots.
  - 2026-04-24: synchronized authority docs and local benchmark governance smoke semantics to the new D-TASK-023 repository truth before standard validation and closeout.
- Context closeout:
  - Completed scope: Implemented trace-linked benchmark report/download audits, repo-local artifact stale-file cleanup and snapshot recovery, updated local smoke semantics, and synchronized authority docs to the new repository truth.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-023 --include-task-audit; mvn -B -pl governance,benchmark-engine -am test -DskipITs; bash scripts/run-runtime-smoke.sh --compose-check; node scripts/lint-repository-knowledge.js; bash -n scripts/manual-benchmark-governance-smoke.sh; python3 scripts/foreman.py compile-governance --check
  - Residual risk: Benchmark artifact lifecycle is still repo-local and report-set scoped; tenant-specific retention/backfill policy plus environment-backed object-storage adapter/evidence remain outside the default repo-side path.
  - Next step: Shape D-TASK-024 to cover benchmark artifact tenant-specific retention/backfill policy and environment-backed object-storage evidence without replacing the repo-local default baseline.

### D-TASK-022: 推进 benchmark-engine 外部 artifact storage、raw-data download 与治理追溯编排

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(benchmark-engine): externalize artifacts and trace exports`
- Priority: 1
- Depends on: D-TASK-021
- Scope: 在保留 repo-closed 隔离执行、统一授权入口、治理审计与只读/影子环境边界的前提下，把 benchmark 报告导出与 raw-data snapshot 提升到外置 artifact storage 基线，并通过 governance 内部受保护编排把 config/result/history/export 追溯链接到 benchmark-engine 报告与下载路径。
- Matrix context: Phase-D
- Human confirmation point: 若外部 artifact storage 会泄露明文敏感数据、绕过 governance 追溯链/统一授权入口，或把环境级对象存储依赖误写成 repo-closed 默认主路径，需人工确认
- Data impact: benchmark-engine artifact storage 配置、raw-data 下载快照、governance `config_snapshot/execution_result/query_history/export_record/audit_log` 追溯链、跨服务 runtime smoke 证据
- Rollback / recovery: 关闭新增 artifact externalization / trace orchestration 路径，回退到当前 `benchmark_task_report` 持久化导出基线，并恢复上一版报告查询/下载契约与治理文档说明
- Validation:
  - `benchmark-engine/governance 模块测试、导出/下载契约测试、runtime smoke、task audit、schema/mapping 校验、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-022`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: implemented repo-local externalized artifact storage for benchmark `JSON/PDF/HTML` exports and raw-data snapshot download, while keeping repo-closed isolation as the default baseline.
  - 2026-04-24: added governance internal benchmark report trace orchestration so benchmark-engine registers `config_snapshot/execution_result/query_history/export_record` links instead of bypassing governance persistence.
  - 2026-04-24: synchronized authority docs and task ledgers to the new D-TASK-022 repository truth before standard validation and closeout.
- Context closeout:
  - Completed scope: Implemented repo-local benchmark artifact externalization, raw-data download, and governance trace/export orchestration; synchronized authority docs and task ledgers to the new repository truth.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-022 --include-task-audit; mvn -B -pl governance,benchmark-engine -am test -DskipITs; bash scripts/run-runtime-smoke.sh --compose-check; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance --check
  - Residual risk: Benchmark report/download query paths still need richer audit-link enrichment plus artifact retention/recovery/cleanup semantics; environment-backed object storage evidence remains outside the repo-closed default baseline.
  - Next step: Shape D-TASK-023 to enrich benchmark report/download audit linkage and artifact lifecycle semantics without promoting environment-backed storage to the default repo-side path.

### D-TASK-021: 推进 `benchmark-engine` 真实隔离执行与导出链路

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(benchmark-engine): add isolated execution export chain`
- Priority: 1
- Depends on: `D-TASK-020`
- Scope: 把 `benchmark-engine` 从 placeholder 执行/导出基线推进到真实隔离执行、可复现报告快照与导出产物链路，保持只读、影子环境优先、统一授权入口与治理审计契约不变 Tech: `JAVA-BE`,`SQL`,`OPS`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若真实压测执行链会放宽只读/影子环境隔离、绕过统一授权入口/治理审计、或把占位导出直接冒充为真实快照导出，需人工确认
- Data impact: benchmark-engine 执行配置、`benchmark_task` / `benchmark_task_report` 数据、报告快照/导出产物元数据、跨服务审计与 runtime smoke 证据
- Rollback / recovery: 关闭新增真实执行/导出路径，恢复到当前持久化 placeholder 基线，并回退到上一版报告查询契约、隔离约束与审计说明
- Validation:
  - `benchmark-engine 模块测试、导出/报告契约测试、runtime smoke、task audit、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-021`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: replaced the placeholder benchmark path with a repo-closed isolated execution chain, persisted `execution_summary_json` and `export_artifacts_json` on `benchmark_task_report`, and switched PDF/HTML report queries to serve the stored export bundle instead of in-method placeholder rendering.
- Context closeout:
  - Completed scope: Implemented repo-closed isolated benchmark execution, persisted execution summaries plus JSON/PDF/HTML export artifacts on benchmark_task_report, switched report rendering to replay stored artifacts, and synchronized benchmark-engine truth/docs/governance state.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-021 --include-task-audit --extra-command 'mvn -B -pl benchmark-engine -am test -DskipITs' --extra-command 'bash scripts/run-runtime-smoke.sh --compose-check' --extra-command 'node scripts/lint-repository-knowledge.js'; python3 scripts/foreman.py compile-governance --check
  - Residual risk: Benchmark-engine now has a repo-closed isolated execution/export baseline, but external file storage, raw data download, deeper cross-service orchestration, and environment-level execution evidence remain follow-up gaps; no new repo-side mainline task is instantiated yet.
  - Next step: If benchmark-engine follow-up continues, shape and instantiate a new Phase-D task for external artifact storage/raw-data download/cross-service orchestration before further implementation; otherwise keep the repository truth explicit that no repo-side mainline is instantiated.

### HARN-023: Reconcile E-TASK-016 post-closeout drift

- Status: done
- Completed at: 2026-04-24
- Commit subject: `fix(governance): reconcile e-task-016 active wave`
- Priority: 1
- Depends on: E-TASK-016
- Scope: Align the current active wave after E-TASK-016 closeout, remove the stale pointer to the completed frontend boundary-hardening task, and update repository truth so the next repo-side mainline is either explicitly shaped or stated as uninstantiated without changing frontend/runtime behavior or environment-backed follow-up semantics.
- Validation:
  - `python3 scripts/foreman.py validate HARN-023`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: repointed the master plan away from completed `E-TASK-016`, restored the repository truth that no repo-side mainline is currently instantiated, and shaped `D-TASK-021` as the next uninstantiated repo-side mainline around `benchmark-engine` real isolated execution/export follow-up without changing frontend/runtime or environment-backed follow-up semantics.
- Context closeout:
  - Completed scope: Reconciled the post-closeout drift left behind after E-TASK-016, repointed the master plan away from the completed frontend boundary-hardening task, restored the explicit truth that no repo-side mainline is currently instantiated, and shaped D-TASK-021 as the next formal but uninstantiated repo-side mainline around benchmark-engine real isolated execution/export follow-up without changing frontend/runtime or environment-backed follow-up semantics.
  - Validation evidence: python3 scripts/foreman.py validate HARN-023 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "node scripts/lint-repository-knowledge.js"; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The active-wave truth and task matrices are back in sync, but D-TASK-021 is only shaped, not instantiated, so benchmark-engine still remains on the current persisted placeholder execution/export baseline until the next repo-side mainline is explicitly started; HARN-016 and INBOX-001 remain separate environment-backed follow-ups.
  - Next step: Instantiate D-TASK-021 explicitly before implementation so benchmark-engine real isolated execution/export work proceeds on the now-shaped Phase-D mainline while preserving the local-only dev-smoke boundary and existing environment-backed follow-up semantics.

### E-TASK-016: 固化 dev browser smoke 的 local repo-closed 基线语义

- Status: done
- Completed at: 2026-04-24
- Commit subject: `fix(frontend): lock dev smoke to local baseline`
- Priority: 1
- Depends on: `E-TASK-015`
- Scope: 明确 Vite dev browser smoke 只作为本地 repo-closed 开发验证基线，不把它升级到更广的 CI/runtime gating，并同步后续计划/操作文档保持 full-stack runtime smoke 作为多服务主路径 Tech: `VUE-FE`,`OPS`,`DOCS`. Layer: `frontend/router/views/styles`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-E / Story `E-STORY-005` 前端构建链迁移与便携产物治理
- Human confirmation point: 若把 dev browser smoke 从当前 local repo-closed 基线升级为默认 CI/runtime gate、削弱现有 full-stack runtime smoke 主路径，或通过该任务改写 `E-TASK-015` 的历史完成结论，需人工确认
- Data impact: dev browser smoke 的边界定义、前端验证语义、CI/runtime gating 叙事与文档表述
- Rollback / recovery: 保留 `E-TASK-015` 已验证的 local dev smoke 基线，回退高风险边界/脚本/文档改动，并恢复 full-stack runtime smoke 作为多服务主路径的既有真值
- Validation:
  - `npm run lint`、`npm run build`、`npm run build:portable`、`node scripts/check-dev-frontend.mjs`、`node scripts/lint-repository-knowledge.js`、task audit
  - `python3 scripts/foreman.py validate E-TASK-016`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reinforced the frontend dev browser smoke boundary so it remains a local repo-closed baseline only, updated local-development and CI/phase-gate baseline docs to keep full-stack runtime smoke as the multi-service main path, and added repository knowledge-lint checks that fail if smoke:frontend-dev or check-dev-frontend.mjs is wired into default CI/runtime gate entrypoints.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-016 --include-task-audit --extra-command "npm run lint" --extra-command "npm run build" --extra-command "npm run build:portable" --extra-command "node scripts/check-dev-frontend.mjs" --extra-command "node scripts/lint-repository-knowledge.js"; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The local-only dev smoke boundary is now documented and lint-enforced, but the next business-facing frontend or CI task could still attempt to widen that boundary; HARN-016 and INBOX-001 remain unrelated environment-backed follow-ups, and any future promotion of dev smoke into broader gating still requires explicit human confirmation.
  - Next step: Before implementing the next business-facing task, instantiate it explicitly and verify its contract, authority docs, validation path, and task-matrix/governance fields are complete; if the next task touches CI/runtime semantics, preserve full-stack runtime smoke as the default main path unless a new confirmed task changes that boundary.

### HARN-022: Shape E-TASK-016 from E-TASK-015 residual risk

- Status: done
- Completed at: 2026-04-24
- Commit subject: `fix(governance): shape e-task-016 local dev smoke boundary`
- Priority: 1
- Depends on: HARN-021
- Scope: Align the active-wave truth after HARN-021, capture the confirmed decision that the Vite dev browser smoke remains a local repo-closed baseline only, and shape the resulting repo-side frontend follow-up as E-TASK-016 in the master plan and task matrices without changing frontend/runtime behavior or environment-backed follow-up semantics.
- Validation:
  - `python3 scripts/foreman.py validate HARN-022`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: captured the explicit human decision that Vite dev browser smoke remains a local repo-closed baseline only, updated the Phase-E plan/matrices to shape the resulting follow-up as `E-TASK-016`, and repointed the active-wave truth away from the completed `HARN-021` batch to the new uninstantiated repo-side frontend mainline without changing frontend/runtime or environment-backed follow-up semantics.
- Context closeout:
  - Completed scope: Instantiated HARN-022, updated the Phase-E active-wave truth to point at the newly shaped follow-up E-TASK-016, added E-TASK-016 to the master plan and both task matrices, and captured the explicit decision that Vite dev browser smoke remains a local repo-closed baseline only rather than a broader CI/runtime gate without changing frontend/runtime or environment-backed follow-up semantics.
  - Validation evidence: python3 scripts/foreman.py validate HARN-022 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "node scripts/lint-repository-knowledge.js"; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The new Phase-E follow-up is now shaped as E-TASK-016, but it is not yet instantiated; HARN-016 and INBOX-001 remain environment-backed follow-ups, and the dev browser smoke boundary now depends on future work continuing to preserve the local-only semantics instead of re-promoting it into broader CI/runtime gating.
  - Next step: If frontend follow-up continues, instantiate E-TASK-016 explicitly before implementation so the local repo-closed dev-smoke boundary can be hardened without altering the existing full-stack runtime smoke main path; otherwise keep HARN-016 blocked and INBOX-001 open as non-mainline follow-ups.

### HARN-021: Reconcile E-TASK-015 post-closeout drift

- Status: done
- Completed at: 2026-04-24
- Commit subject: `fix(governance): reconcile e-task-015 active wave`
- Priority: 1
- Depends on: E-TASK-015
- Scope: Align the current active wave after E-TASK-015 closeout, remove the stale pointer to the completed frontend task, and make the repository truth explicit about whether any repo-side mainline is currently instantiated without changing frontend/runtime behavior or environment-backed follow-up semantics.
- Validation:
  - `python3 scripts/foreman.py validate HARN-021`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: updated the master execution plan current active wave after `E-TASK-015` closeout so repository truth no longer points at a completed frontend task; current state now explicitly shows no instantiated repo-side mainline task while `HARN-021` remains the active governance reconciliation batch and `HARN-016` plus `INBOX-001` stay as non-mainline environment-backed follow-ups.
- Context closeout:
  - Completed scope: Instantiated HARN-021, updated the master execution plan current active wave so it no longer points at completed E-TASK-015, made the repository truth explicit that no repo-side mainline task is currently instantiated, and absorbed the append-only validation-log residue for E-TASK-015/HARN-021 into the governed batch without changing frontend/runtime or environment-backed follow-up semantics.
  - Validation evidence: python3 scripts/foreman.py validate HARN-021 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "node scripts/lint-repository-knowledge.js"; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The plan truth is back in sync, but the repository still has no newly instantiated repo-side mainline task; HARN-016 and INBOX-001 remain environment-backed follow-ups, and any next mainline must be explicitly shaped before reusing the active-wave block.
  - Next step: When the next repo-side priority is chosen, instantiate it explicitly before changing the active-wave block again; until then keep HARN-016 blocked and INBOX-001 open as non-mainline follow-ups.

### E-TASK-015: 补齐前端 dev browser smoke 并清理 SFC 恢复后的当前叙事

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(frontend): add Vite dev browser smoke`
- Priority: 1
- Depends on: `E-TASK-014`
- Scope: 为 Vite dev server 补齐轻量浏览器 smoke，并清理仍把 non-SFC 迁移表述成当前真值的计划/操作文档；保留现有 Vue SFC、portable 包与 full-stack runtime smoke 语义 Tech: `VUE-FE`,`OPS`,`DOCS`. Layer: `frontend/router/views/styles`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-E / Story `E-STORY-005` 前端构建链迁移与便携产物治理
- Human confirmation point: 若新增 dev browser smoke 会替代既有 full-stack runtime smoke、削弱现有 portable / 代理语义验证，或为清理叙事而改写历史任务完成记录，需人工确认
- Data impact: Vite dev server 浏览器 smoke 覆盖、当前前端治理叙事、路由/代理语义
- Rollback / recovery: 保留 `E-TASK-014` 已验证的 Vue SFC / portable 基线，回退高风险 dev smoke 或文档清理改动，并恢复到上一个已验证的前端交付真值
- Validation:
  - `npm run lint`、`npm run build`、`npm run build:portable`、toolchain/portable/dev browser smoke 检查、task audit
  - `python3 scripts/foreman.py validate E-TASK-015`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: added a lightweight Vite dev browser smoke entrypoint that boots the dev server, validates history-route rendering plus query success/recovery flows against browser-side `/api/*` mocks, and updated operations/separation docs so current frontend truth distinguishes Vue SFC dev smoke from the heavier full-stack runtime smoke.
- Context closeout:
  - Completed scope: Added a lightweight Vite dev browser smoke that boots the local dev server, validates history-mode route rendering plus query success/recovery flows against browser-side /api mocks, records dev request-header semantics, and updates operations/separation docs so the current frontend truth distinguishes the restored Vue SFC dev baseline from the heavier full-stack runtime smoke.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-015 --include-task-audit --extra-command "npm run lint" --extra-command "npm run build" --extra-command "npm run build:portable" --extra-command "node scripts/check-frontend-toolchain.mjs" --extra-command "node scripts/check-portable-frontend.mjs" --extra-command "node scripts/check-dev-frontend.mjs"; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The repository now has a lightweight Vite dev browser smoke and the current SFC/dev-baseline docs are aligned, but the new smoke is intentionally browser-mocked and does not replace the existing multi-service runtime smoke; historical non-SFC task narratives remain in tasks-done by design as immutable completion records, and HARN-016 plus INBOX-001 remain unchanged environment-backed follow-ups.
  - Next step: If frontend hardening continues, decide whether the lightweight dev browser smoke should stay as a local repo-closed baseline only or be promoted into broader CI/runtime gating alongside the existing full-stack smoke without duplicating coverage.

### HARN-020: Reconcile E-TASK-014 closeout drift and shape E-TASK-015

- Status: done
- Completed at: 2026-04-24
- Commit subject: `fix(governance): reconcile E-TASK-014 closeout drift`
- Priority: 1
- Depends on: E-TASK-014
- Scope: Align the current active wave and validation-log residue after E-TASK-014 closeout, then decide and shape the next repo-side frontend follow-up E-TASK-015 around dev browser smoke coverage and historical non-SFC narrative cleanup without changing frontend/runtime behavior or environment-backed follow-up semantics.
- Validation:
  - `python3 scripts/foreman.py validate HARN-020`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: reconciled the stale Phase-E active-wave pointer left behind after `E-TASK-014` closeout, kept the append-only validation-log residue inside the governed batch, and shaped `E-TASK-015` as the next repo-side frontend follow-up for lightweight Vite dev browser smoke plus current-state non-SFC narrative cleanup.
- Context closeout:
  - Completed scope: Aligned the Phase-E active wave away from the stale E-TASK-014 pointer, updated the Phase-E master plan plus both task matrices to shape E-TASK-015 as the next repo-side frontend follow-up, and absorbed the append-only validation-log residue into the governed batch without changing frontend/runtime behavior or environment-backed follow-up semantics.
  - Validation evidence: python3 scripts/foreman.py validate HARN-020 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "node scripts/lint-repository-knowledge.js"; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The governance chain is back in sync, but E-TASK-015 is not yet instantiated, so the repository still lacks the planned lightweight Vite dev browser smoke and the current-state non-SFC narrative cleanup beyond the newly aligned plan/matrix truth; HARN-016 and INBOX-001 remain unchanged environment-backed follow-ups.
  - Next step: Instantiate E-TASK-015 if frontend follow-up continues so the Vite dev server gains lightweight browser smoke coverage and current governance/operations docs are fully aligned with the restored Vue SFC baseline without rewriting historical task records.

### E-TASK-014: 恢复 Vue SFC 构建链并保留前端便携产物

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(frontend): restore Vue SFC toolchain`
- Priority: 1
- Depends on: E-TASK-013
- Scope: Restore the root frontend to Vue single-file components now that @vitejs/plugin-vue and @vue/compiler-sfc are allowed again, while preserving the current frontend behavior and the portable build/package workflow.
- Validation:
  - `python3 scripts/foreman.py validate E-TASK-014`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: human policy changed again to allow `@vitejs/plugin-vue` and `@vue/compiler-sfc`, so the task scope was executed as a structured restoration of the Vue SFC toolchain rather than another non-SFC hardening step.
  - 2026-04-24: restored the historical `.vue` root and route-view source files, rewired the app entry and lazy-loaded router imports back to SFC modules, reintroduced `@vitejs/plugin-vue` plus `@vue/compiler-sfc`, and removed the temporary local vue runtime shim plus generated `.js`/`.css` component artifacts.
  - 2026-04-24: preserved the later portable build, vendor chunk split, explicit Element Plus component registration, and browser smoke workflow, then updated the frontend toolchain check and governance truth to validate the restored SFC-based contract instead of the prior dependency-ban policy.
- Context closeout:
  - Completed scope: Restored the root frontend back to Vue single-file components, reintroduced @vitejs/plugin-vue plus @vue/compiler-sfc, removed the temporary vue runtime shim and generated JS/CSS view artifacts, and kept the portable package plus chunk-splitting/browser-smoke workflow intact.
  - Validation evidence: Validated with npm run lint, npm run build, npm run build:portable, node scripts/check-frontend-toolchain.mjs, node scripts/check-portable-frontend.mjs, and foreman validate including task_audit.
  - Residual risk: The repository now again depends on the Vue SFC toolchain and still assumes Node 18.20.8 on target environments, while some local dev pages can still surface backend-driven 404/500 responses when their backing services are not running.
  - Next step: If frontend follow-up continues, add a small browser smoke for the dev build itself and decide whether the now-restored SFC architecture should trigger any cleanup of historical non-SFC task narratives beyond the new governance truth sync.

### E-TASK-013: 补齐 portable 前端浏览器 smoke 并收口构建分包告警

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(frontend): harden portable browser smoke and chunk budget`
- Priority: 1
- Depends on: `E-TASK-012`
- Scope: 把 portable 包纳入关键路由浏览器 smoke，并收口当前 Vite 大 chunk 告警而不重引 SFC 依赖 Tech: `VUE-FE`,`OPS`. Layer: `frontend/router/views/styles`,`deployments/ci/scripts`.
- Matrix context: Phase-E / Story `E-STORY-005` 前端构建链迁移与便携产物治理
- Human confirmation point: 若 portable 验证被降级为 health-only 检查、分包方案改变路由/代理/缓存语义，或为压低 chunk 告警而牺牲关键页面可用性，需人工确认
- Data impact: portable 浏览器 smoke 覆盖、前端 chunk 输出、关键路由与代理语义
- Rollback / recovery: 保留当前 portable 包与关键路由语义，回退高风险分包策略，并恢复到现有可工作的构建输出
- Validation:
  - `npm run lint`、`npm run build`、`npm run build:portable`、portable browser smoke、chunk warning 检查
  - `python3 scripts/foreman.py validate E-TASK-013`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: replaced the root `ElementPlus` full-library install with explicit component registration, lazy-loaded the route views to reduce eager payload, added a repo-specific Vite chunk budget plus vendor chunk split, and upgraded `scripts/check-portable-frontend.mjs` from a health-only check to a browser-driven portable smoke with deep hash-route coverage and proxy-header assertions against a local mock backend.
- Context closeout:
  - Completed scope: Added a browser-driven portable frontend smoke that boots the packaged dist-portable server against a local mock backend, verifies deep hash-route rendering plus proxy-header forwarding, replaced the root full-library Element Plus install with explicit component registration, lazy-loaded route views, and tightened the Vite bundle layout with vendor chunking plus a repo-specific chunk budget so the previous default large-chunk warning no longer fires.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-013 --include-task-audit --extra-command "npm run lint" --extra-command "npm run build" --extra-command "npm run build:portable" --extra-command "node scripts/check-portable-frontend.mjs"; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Portable browser smoke now covers representative packaged routes and proxy semantics against a mock backend, but it still does not exercise a live backend stack, and dist-portable remains an environment-sensitive artifact that assumes a local Node runtime plus correct backend target URLs on the destination host; HARN-016 and INBOX-001 remain unchanged environment-backed follow-ups.
  - Next step: If frontend hardening continues, decide whether to extend the portable smoke from mock-backend verification to a live backend runtime path or keep the current repo-closed mock-backed smoke as the stable portable baseline.

### HARN-019: Reconcile E-TASK-011/E-TASK-012 post-closeout drift

- Status: done
- Completed at: 2026-04-24
- Commit subject: `fix(governance): reconcile frontend post-closeout drift`
- Priority: 1
- Depends on: E-TASK-012
- Scope: Align current active wave, task matrices, INBOX/ledger truth, and validation-log closeout residue after E-TASK-011 and E-TASK-012 completion; decide whether the remaining frontend residual risk becomes the next repo-side hardening task without changing business code or environment-backed follow-up semantics.
- Validation:
  - `python3 scripts/foreman.py validate HARN-019`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: aligned the current active wave away from the stale Phase-D pointer, backfilled `E-TASK-010` through `E-TASK-012` into the Phase-E plan/matrices, and shaped the remaining portable browser-smoke plus bundle-warning residual risk into the next repo-closed frontend hardening follow-up `E-TASK-013`.
- Context closeout:
  - Completed scope: Aligned the current active wave away from the stale Phase-D pointer, backfilled E-TASK-010 through E-TASK-012 into the Phase-E master plan and both task matrices, and converted the remaining portable browser-smoke plus bundle-warning residual risk into the next repo-closed frontend hardening follow-up E-TASK-013 while absorbing the append-only validation-log tail into the governed batch.
  - Validation evidence: python3 scripts/foreman.py validate HARN-019 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "node scripts/lint-repository-knowledge.js"; python3 scripts/task_audit.py --check --phase pre-closeout; npm run build (confirmed current Vite large-chunk warning remains as E-TASK-013 input)
  - Residual risk: The repository plan and matrices are now back in sync, but E-TASK-013 is not yet instantiated, so the portable package still lacks dedicated browser smoke coverage and the production build still emits Vite's large-chunk warning; HARN-016 and INBOX-001 also remain unchanged environment-backed follow-ups.
  - Next step: Instantiate E-TASK-013 if frontend hardening continues so the portable package gains browser-level smoke coverage and the current chunk warning is reduced without reintroducing the Vue SFC toolchain.

### E-TASK-012: 修复前端非 SFC 迁移后的布局回归

- Status: done
- Completed at: 2026-04-24
- Commit subject: `fix(frontend): restore Element Plus layout baseline`
- Priority: 1
- Depends on: E-TASK-011
- Scope: Diagnose and fix the frontend layout regressions introduced by the non-SFC migration so the app restores its intended sidebar, header, dashboard, and route-level page layouts without reintroducing @vitejs/plugin-vue or @vue/compiler-sfc.
- Validation:
  - `python3 scripts/foreman.py validate E-TASK-012`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: reproduced the layout regression in a browser session and confirmed the non-SFC migration had restored `app.use(ElementPlus)` but not the Element Plus base stylesheet, causing `el-container` / `el-aside` / `el-header` layout primitives to collapse into default block flow.
  - 2026-04-24: restored `element-plus/dist/index.css`, added the missing `--sqlforge-radius-xl` design token, and revalidated dashboard plus representative route layouts together with standard and portable frontend builds.
- Context closeout:
  - Completed scope: Reproduced the non-SFC layout regression, restored Element Plus base CSS so container primitives render with their intended flex layout again, added the missing radius token, and refreshed the portable frontend assets to match the fixed shell styling.
  - Validation evidence: Validated with browser-based layout checks across representative routes, npm run lint, npm run build, npm run build:portable, node scripts/check-portable-frontend.mjs, and foreman validate including task_audit.
  - Residual risk: Representative route layout is restored, but some pages still surface backend-driven 404/500 responses in the local dev environment and the production bundle remains larger than Vite's default chunk warning threshold.
  - Next step: If frontend hardening continues, capture a small automated browser smoke for key routes and consider bundle splitting to reduce the large-entry warning.

### E-TASK-011: 去除 Vue SFC 构建链并增加双产物便携前端包

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(frontend): remove SFC pipeline and add portable package`
- Priority: 1
- Depends on: N/A
- Scope: Migrate the root frontend away from Vue single-file components so the repository no longer depends on @vitejs/plugin-vue or @vue/compiler-sfc, align Node/NPM/Vite/Vue/vue-i18n versions to the requested baseline, and add both standard and portable frontend build outputs where the portable package can be copied to another host and started locally without recompilation while still proxying to real backend APIs.
- Validation:
  - `python3 scripts/foreman.py validate E-TASK-011`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: converted the root frontend away from `.vue` single-file components by generating plain `.js` component modules plus extracted `.css`, rewired the app entry and router imports, and removed the direct Vite SFC plugin / auto-import tooling path from the active build.
  - 2026-04-24: aligned the frontend toolchain contract to Node `18.20.8`, npm `10.8.2`, Vite `5.4.11`, Vue runtime `3.5.13`, and `vue-i18n` `10.0.8`, then replaced the root `vue` package with a local runtime shim so the repository no longer resolves `@vue/compiler-sfc`.
  - 2026-04-24: added a second portable frontend output in `dist-portable/` with relative assets, hash-history routing, a local proxy server, generated startup scripts, and a portable config file so the built package can be copied to another host and started without recompilation.
- Context closeout:
  - Completed scope: Migrated the root frontend away from Vue single-file components into plain JavaScript plus extracted CSS modules, rewired the app bootstrap and routes, and removed the active Vite SFC plugin/auto-import build path.
  - Validation evidence: Validated with npm lint/build/build:portable, frontend toolchain checks, portable package checks, and foreman validate including task_audit.
  - Residual risk: The portable bundle still requires a local Node 18.20.8 runtime plus correct backend target URLs, and no live browser smoke against a real backend stack was executed in this task.
  - Next step: Run a browser smoke against the portable package with a live backend target and consider bundle splitting if transfer size becomes a deployment concern.

### E-TASK-010: 收敛前端 Node/Vite/Vue 版本并核对 Vue SFC 构建约束

- Status: done
- Completed at: 2026-04-23
- Commit subject: `chore(frontend): align toolchain versions and capture SFC constraint`
- Priority: 1
- Depends on: N/A
- Scope: Pin the root frontend toolchain to Node 18.20.8, Vite 5.4.11, and Vue 3.5.13; inspect whether the current Vue single-file-component build can operate without @vitejs/plugin-vue and without a direct @vue/compiler-sfc dependency; apply only repository-truth-consistent changes and keep runtime smoke/build validation traceable.
- Validation:
  - `python3 scripts/foreman.py validate E-TASK-010`
  - `npm run build`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: pinned the root frontend toolchain declarations to Node `18.20.8`, Vite `5.4.11`, and Vue `3.5.13`; removed the direct `@vue/compiler-sfc` devDependency; refreshed the npm lockfile plus GitHub Actions Node version selectors; and verified `npm run build` still passes on `vite@5.4.11`.
  - 2026-04-23: confirmed the current frontend still imports `src/App.vue` and route/view `.vue` single-file components, `vite.config.js` still depends on `@vitejs/plugin-vue`, and `vue@3.5.13` still carries `@vue/compiler-sfc` transitively, so the requested "unsupported `@vitejs/plugin-vue` / unsupported `@vue/compiler-sfc`" state cannot be reached without a broader non-SFC frontend migration or a tooling-policy exception.
- Next action: After human confirmation, either keep the current Vue SFC architecture and accept `@vitejs/plugin-vue` plus Vue's transitive `@vue/compiler-sfc`, or open a dedicated refactor task to migrate `src/App.vue`, router views, and the Vite transform path away from `.vue` SFC usage.
- Escalation: If the environment policy truly bans `@vitejs/plugin-vue` or any transitive `@vue/compiler-sfc`, stop treating this as a version-only dependency change and escalate it as a scoped frontend architecture migration with explicit acceptance of rewrite cost and regression risk.
- Human decision: Decide whether repository truth should continue using Vue SFCs with `@vitejs/plugin-vue`, or whether to authorize a broader refactor that removes `.vue` SFC usage and accepts the required build/runtime rewiring.
- INBOX ref: INBOX-003
- Context closeout:
  - Completed scope: Pinned the root frontend toolchain declarations and CI workflows to Node 18.20.8, Vite 5.4.11, and Vue 3.5.13, removed the direct @vue/compiler-sfc devDependency, refreshed the npm lockfile, and recorded the remaining Vue single-file-component build constraint through the task and INBOX audit chain instead of pretending the current SFC frontend can run without its required plugin/tooling path.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-010 --include-task-audit --extra-command 'npm run build' --extra-command 'npm ls @vue/compiler-sfc'; npm ls @vitejs/plugin-vue @vue/compiler-sfc vue vite
  - Residual risk: The repository frontend still imports src/App.vue and route-level .vue files through vite.config.js with @vitejs/plugin-vue, and vue@3.5.13 still carries @vue/compiler-sfc transitively, so a strict ban on either package still requires a broader non-SFC migration or a tooling-policy exception.
  - Next step: Decide whether to keep the current Vue SFC architecture with its required plugin/transitive compiler path, or authorize a dedicated frontend migration task that removes .vue SFC usage before enforcing a stricter package ban.

### HARN-018: Reconcile D-TASK-020 post-closeout drift

- Status: done
- Completed at: 2026-04-23
- Commit subject: `fix(governance): HARN-018 reconcile d-task-020 drift`
- Priority: 1
- Depends on: D-TASK-020
- Scope: Align current active wave and plan truth after D-TASK-020 completion without inventing a new unapproved mainline task or changing environment-backed follow-up semantics.
- Validation:
  - `python3 scripts/foreman.py validate HARN-018`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: updated the master execution plan current active wave after D-TASK-020 closeout so repository truth no longer points at a completed repo-side mainline task; current state now explicitly shows no instantiated mainline task while HARN-016 and INBOX-001 remain environment-backed follow-ups only.
- Context closeout:
  - Completed scope: Aligned the Phase-D current active wave after D-TASK-020 closeout by clearing the stale pointer to a completed repo-side mainline task and updating the master plan to reflect the current repository truth: no active repo-side mainline is instantiated, while HARN-016 and INBOX-001 remain external or environment-backed follow-ups.
  - Validation evidence: python3 scripts/foreman.py validate HARN-018 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "node scripts/lint-repository-knowledge.js"
  - Residual risk: The repository mainline is now truthfully idle rather than stale, but the external Hetu/MRS evidence wait in HARN-016 and the Sonar restoration decision in INBOX-001 remain unresolved; validation-log tails continue as append-only audit residue outside the single-task stage scope.
  - Next step: When a new repo-side priority is chosen, instantiate it explicitly before changing the active-wave block again; until then keep HARN-016 blocked and INBOX-001 open as non-mainline follow-ups.

### D-TASK-020: 补齐异步服务执行遥测与业务指标基线

- Status: done
- Completed at: 2026-04-23
- Commit subject: `feat(observability): D-TASK-020 add async metrics baseline`
- Priority: 1
- Depends on: D-TASK-019
- Scope: Add minimal Micrometer business metrics for sql-optimization and benchmark-engine, keep low-cardinality tags, extend tests/runtime smoke, and sync observability/document-truth docs without changing external environment-backed workflows.
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若异步服务新增遥测暴露敏感信息、为便于排障引入 task id / tenant id 等高基数标签，或削弱既有日志/审计语义以换取指标简化，需人工确认
- Data impact: sql-optimization/benchmark-engine 指标、异步任务终态信号、worker/report 延迟可观测数据
- Rollback / recovery: 移除高风险 meter、恢复以日志/审计为主的既有语义，并回退到上一版稳定 tags 与文档说明
- Validation:
  - `sql-optimization/benchmark-engine 模块测试、prometheus 指标断言、runtime smoke、task audit、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-020`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: added `OptimizationMetricsRecorder` and `BenchmarkMetricsRecorder`, wired minimal Micrometer counters/timers into async submit/worker/report paths, extended targeted unit coverage with meter assertions, and updated observability/document-truth authority text from “async services mainly rely on logs” to the new four-service minimal metrics baseline.
- Context closeout:
  - Completed scope: Added OptimizationMetricsRecorder and BenchmarkMetricsRecorder, wired minimal Micrometer counters/timers into sql-optimization submit/worker paths and benchmark-engine submit/worker/report paths, extended targeted unit coverage with meter assertions, recompiled authority-map policy state, and synchronized observability/document-truth docs so all four backend services now have a repo-closed minimal business metrics baseline.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-020 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "mvn -B -pl sql-optimization,benchmark-engine -am test -DskipITs -Dtest=OptimizationTaskApplicationServiceTest,OptimizationTaskWorkerTest,BenchmarkTaskApplicationServiceTest,BenchmarkTaskWorkerTest,BenchmarkReportApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false" --extra-command "bash scripts/run-runtime-smoke.sh --runtime-smoke" --extra-command "node scripts/lint-repository-knowledge.js"
  - Residual risk: The repository now exposes minimal async-service metrics, but PrometheusRule/Alertmanager/Grafana assets, log-pipeline templates, tracing, and external-environment follow-ups such as HARN-016 Hetu/MRS evidence and INBOX-001 Sonar restoration remain outside this task scope; validation-log tails also continue as append-only audit residue outside the single-task stage scope.
  - Next step: If Phase-D observability hardening continues, reconcile the current active wave after this closeout and decide whether the next repo-side task should target broader cross-service tracing/aggregated observability or another governed follow-up.

### HARN-017: Reconcile D-TASK-019 post-closeout drift

- Status: done
- Completed at: 2026-04-23
- Commit subject: `fix(governance): HARN-017 reconcile d-task-019 drift`
- Priority: 1
- Depends on: D-TASK-019
- Scope: Align current active wave, ledgers, and plan truth after D-TASK-019 completion without changing business code or environment-backed follow-up semantics.
- Validation:
  - `python3 scripts/foreman.py validate HARN-017`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: updated the master execution plan current active wave from completed `D-TASK-019` to the new repo-closed follow-up `D-TASK-020`, and extended the task spec / governance matrices so the next async-service observability hardening step has formal scope, validation, and low-cardinality telemetry guardrails.
- Context closeout:
  - Completed scope: Aligned the Phase-D current active wave after D-TASK-019 closeout by moving the mainline from the completed query-execution/governance telemetry task to a new repo-closed follow-up D-TASK-020, and synchronized the master plan plus task spec/governance matrices so async-service observability hardening is now the formal next executable task without changing any business-code or environment-backed semantics.
  - Validation evidence: python3 scripts/foreman.py validate HARN-017 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "node scripts/lint-repository-knowledge.js"
  - Residual risk: The external Hetu/MRS evidence wait in HARN-016 and the Sonar environment-restoration follow-up in INBOX-001 remain unchanged environment-backed items; validation-log closeout tails also continue as append-only audit residue outside the single-task stage scope.
  - Next step: Instantiate D-TASK-020 and implement minimal Micrometer business metrics for sql-optimization and benchmark-engine so all four backend services share a repo-closed observability baseline.

### D-TASK-019: 补齐 query-execution 执行遥测与业务指标基线

- Status: done
- Completed at: 2026-04-23
- Commit subject: `feat(observability): add query and governance metrics baseline`
- Priority: 1
- Depends on: D-TASK-018
- Scope: 为 query-execution 与 governance 补最小业务级 Micrometer 指标与执行遥测，支撑后续 Hetu 参数调优与运行时排障；不依赖外部测试环境，不扩展业务功能。
- Validation:
  - `python3 scripts/foreman.py validate D-TASK-019`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: aligned Phase-D active-wave wording plus task/governance matrices so `D-TASK-019` becomes the repo-closed follow-up for query-execution telemetry hardening rather than external-environment tuning.
  - 2026-04-23: added `QueryExecutionMetricsRecorder` and `GovernanceMetricsRecorder`, wiring minimal Micrometer counters/timers/gauges for query results, mode hits/attempts, timeout/fallback/route-unavailable, audit fallback, retry count, and database queue backlog.
  - 2026-04-23: extended query-execution/governance unit coverage with prometheus metric assertions and updated observability/document-truth authority text from “no business metrics” to the new minimal implemented baseline.
- Context closeout:
  - Completed scope: Added minimal Micrometer business metrics for query-execution and governance, including query request/latency/mode-hit/mode-attempt/timeout/fallback/route-unavailable signals plus governance audit-fallback, message-retry, and database queue backlog meters; wired the recorders into the existing services, updated targeted unit coverage with metric assertions, and synchronized the Phase-D plan/matrix plus observability/document-truth authority docs to the new repo-closed telemetry baseline.
  - Validation evidence: Validated with python3 scripts/foreman.py validate D-TASK-019 --include-task-audit --extra-command 'python3 scripts/foreman.py compile-governance --check' --extra-command 'mvn -B -pl query-execution,governance -am test -DskipITs -Dtest=QueryExecutionApplicationServiceTest,GovernanceAuditTrailServiceTest,MessageAdminApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false' --extra-command 'bash scripts/run-runtime-smoke.sh --runtime-smoke' --extra-command 'node scripts/lint-repository-knowledge.js'; along the way runtime smoke exposed constructor-selection regressions in governance/query-execution startup, which were fixed and then verified by a full passing runtime smoke run.
  - Residual risk: The repository now exposes minimal tuning-oriented metrics for query-execution/governance, but external Hetu/MRS evidence, production parameter calibration, PrometheusRule/Alertmanager/Grafana assets, and broader telemetry coverage for sql-optimization/benchmark-engine still remain outside the repository-closed baseline.
  - Next step: Use the new query-execution/governance metrics as the default repo-closed tuning baseline, and if Phase-D continues telemetry hardening, extend the same Micrometer coverage to sql-optimization and benchmark-engine before reopening external-environment parameter tuning.

### D-TASK-018: Remove legacy foreign keys and enforce traceability integrity in application

- Status: done
- Completed at: 2026-04-23
- Commit subject: `feat(governance): remove traceability foreign keys`
- Priority: 1
- Depends on: `D-TASK-017`
- Scope: 移除 governance 核心追溯链在 MySQL/TDSQL 上的历史外键约束，补齐引用键索引与应用层完整性校验；同步更新 init-schema、migration、映射测试、主计划/任务矩阵，以及 Hetu/MRS 测试环境部署文档为 Win10+IDEA+yml 配置读取口径，并提供待确认清单与选项。
- Validation:
  - `python3 scripts/foreman.py validate D-TASK-018`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: aligned master plan, task spec matrix, governance extension matrix, and validation baseline so `D-TASK-018` becomes the active Phase-D mainline task for foreign-key removal and application-level integrity enforcement.
  - 2026-04-23: removed physical foreign-key constraints from `sql/init-schema.sql`, added `sql/migrations/V20260423_017__drop_traceability_foreign_keys.sql` for published environments, and updated persistence authority text from schema-level foreign keys to reference-key + application-integrity semantics.
  - 2026-04-23: extended `GovernanceProtectedPersistenceService` to validate referenced records, tenant consistency, and traceability chain coherence before persisting `execution_result`、`query_history`、`export_record`、`audit_log`; refreshed `TraceabilitySchemaMappingTest` and `GovernanceProtectedPersistenceServiceTest` accordingly.
  - 2026-04-23: rewrote the Hetu/MRS deployment runbook to the requested `Win10 + IDEA + yml` configuration flow, explicitly excluded Kafka validation for test env, and replaced the old smoke-first emphasis with a deployment confirmation checklist and operator options.
- Context closeout:
  - Completed scope: Removed physical foreign-key constraints from the governance traceability schema baseline in sql/init-schema.sql, added V20260423_017__drop_traceability_foreign_keys.sql for published environments, and shifted traceability integrity enforcement into GovernanceProtectedPersistenceService so execution_result/query_history/export_record/audit_log now validate referenced-record existence, tenant consistency, and chain coherence in application code. Also updated the Phase-D plan/matrices, persistence and validation docs, and rewrote the Hetu/MRS deployment runbook to the requested Win10 + IDEA + yml configuration flow with Kafka excluded and a confirmation-checklist-first operator path.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-018 --include-task-audit --extra-command 'mvn -B -pl governance -am clean test -Dtest=GovernanceProtectedPersistenceServiceTest,TraceabilitySchemaMappingTest,GovernanceAuditTrailServiceTest -Dsurefire.failIfNoSpecifiedTests=false' --extra-command 'python3 scripts/foreman.py compile-governance --check'; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js; rg -n 'CONSTRAINT fk_|FOREIGN KEY' sql/init-schema.sql sql/migrations/V20260423_017__drop_traceability_foreign_keys.sql governance/src/test/java/com/company/governance/infrastructure/persistence/TraceabilitySchemaMappingTest.java
  - Residual risk: The repository-side schema contract is now aligned with R-169, but already published databases still require the new drop-foreign-key migration to be executed, and the Win10 test-environment deployment still depends on human confirmation of Hetu mode, yml layout, auth path, and whether live evidence commands should be issued next.
  - Next step: Confirm the deployment checklist options from the updated Hetu/MRS runbook, apply the new migration in the target test database, start governance and query-execution from IDEA with the chosen Win10 yml profile, and then decide whether to request a second-step live evidence command set for JDBC/REST/CLIENT.

### HARN-015: Add no-foreign-key rule for MySQL/TDSQL

- Status: done
- Completed at: 2026-04-23
- Commit subject: `docs(rules): add no-foreign-key mysql policy`
- Priority: 1
- Depends on: N/A
- Scope: 将 MySQL/TDSQL 禁止外键约束 追加为正式仓库规则，并同步到相关持久化/实现文档与治理审计链。
- Validation:
  - `python3 scripts/foreman.py validate HARN-015`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: appended `R-169` to the rule ledger, synchronized the human constraint history, and updated the persistence baseline from “foreign-key relationships” to “reference-key relationships” for MySQL / TDSQL.
  - 2026-04-23: documented that legacy schema-level foreign keys remain a historical implementation drift to be removed by a dedicated future schema-governance task, while prohibiting any new foreign-key expansion immediately.
- Context closeout:
  - Completed scope: Added the new repository rule R-169 to prohibit physical foreign-key constraints on MySQL/TDSQL, synchronized the human constraint history, and updated the persistence baseline so relationship modeling now uses reference keys, indexes, and application-level integrity instead of foreign keys; also documented that existing schema-level foreign keys are legacy drift requiring a dedicated cleanup task rather than a pattern to continue.
  - Validation evidence: python3 scripts/foreman.py validate HARN-015 --include-task-audit; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance; python3 scripts/foreman.py compile-governance --check
  - Residual risk: The rule and authority documents are now aligned, but sql/init-schema.sql and historical migrations still contain legacy foreign-key constraints from before R-169. A dedicated schema-governance task is still required to remove those constraints from implemented DDL.
  - Next step: Open a focused schema-governance follow-up to remove legacy foreign-key constraints from sql/init-schema.sql and matching migrations, replacing them with indexes and application-level integrity checks while preserving traceability and audit semantics.

### HARN-014: Capture HARN-013 post-closeout governance tail

- Status: done
- Completed at: 2026-04-23
- Commit subject: `fix(governance): capture HARN-013 post-closeout tail`
- Priority: 1
- Depends on: N/A
- Scope: 把 HARN-013 closeout 后遗留的 .codex/policy/authority-map.json 同步件与 docs/quality/validation-log.md post-closeout 审计尾项纳入正常审计链，不扩展业务或部署文档范围。
- Validation:
  - `python3 scripts/foreman.py validate HARN-014`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Captured the HARN-013 post-closeout governance tail by staging the synced .codex policy authority map and append-only validation-log entries produced after the deployment-runbook closeout, without changing the completed deployment documentation scope or any business implementation.
  - Validation evidence: python3 scripts/foreman.py validate HARN-014 --include-task-audit --extra-command 'python3 scripts/foreman.py compile-governance --check'; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js
  - Residual risk: The repository audit chain is clean again, but real Hetu/MRS runtime success still depends on external environment deployment, credentials, reachable orders data, and environment-owned evidence retention.
  - Next step: Use the deployed runbook from HARN-013 to stand up governance and query-execution in the test environment, run bash scripts/run-hetu-env-smoke.sh for JDBC/REST/CLIENT evidence, and archive the returned log and response proof outside the repository.

### HARN-013: Document Hetu/MRS test-environment deployment runbook

- Status: done
- Completed at: 2026-04-23
- Commit subject: `docs(deploy): add hetu test-environment runbook`
- Priority: 1
- Depends on: N/A
- Scope: 新增真实 Hetu/MRS 测试环境部署与取证 runbook，覆盖 governance/query-execution 部署清单、配置项、启动顺序、scripts/run-hetu-env-smoke.sh 执行方法、JDBC/REST/CLIENT 证据留档要求，并同步现有部署入口文档。
- Validation:
  - `python3 scripts/foreman.py validate HARN-013`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: added `docs/deployments/hetu-test-environment-deployment-runbook.md` to document the minimal governance/query-execution deployment path, exact test-environment inputs, mode-specific Hetu configuration, smoke execution, and evidence retention workflow for real Hetu/MRS validation.
  - 2026-04-23: linked the new runbook from docs deployment entrypoints and registered it in `document-coverage-matrix.md` so repository knowledge lint and deployment truth stay aligned.
- Context closeout:
  - Completed scope: Added a dedicated Hetu/MRS test-environment deployment runbook for governance and query-execution, including prerequisites, build/package commands, database initialization, test-profile and prod-profile auth considerations, mode-specific JDBC/REST/CLIENT configuration, smoke execution, and evidence retention guidance; linked the runbook from deployment entrypoints and registered it in the document coverage matrix.
  - Validation evidence: python3 scripts/foreman.py validate HARN-013 --include-task-audit --extra-command 'bash scripts/run-hetu-env-smoke.sh --help'; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance --check
  - Residual risk: The repository now contains an operator-ready deployment/runbook path, but real JDBC/REST/CLIENT success evidence still depends on an external Hetu/MRS environment with reachable endpoints, a queryable orders dataset, valid credentials, and environment-specific auth/network tuning.
  - Next step: Deploy governance and query-execution to the target test environment with the documented profile and Hetu mode settings, run bash scripts/run-hetu-env-smoke.sh against the real endpoint, and archive the returned SUCCESS/HETU/HETU_REAL_INTEGRATION evidence under your environment-owned evidence store.

### HARN-012: Reconcile D-TASK-017 post-closeout drift

- Status: done
- Completed at: 2026-04-23
- Commit subject: `fix(governance): reconcile D-TASK-017 post-closeout drift`
- Priority: 1
- Depends on: N/A
- Scope: 修正 D-TASK-017 closeout 后遗留的两类治理漂移：把 docs/plans/master-execution-plan.md 的 active-wave 指针从已完成的 D-TASK-017 挪走，并将 docs/quality/validation-log.md 中未纳入提交的 post-closeout 尾项重新纳入正常审计链；不改写 D-TASK-017 的历史完成结论，也不扩展业务实现范围。
- Validation:
  - `python3 scripts/foreman.py validate HARN-012`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: confirmed the only dirty-worktree tail after D-TASK-017 closeout was append-only validation evidence in docs/quality/validation-log.md, while docs/plans/master-execution-plan.md still pointed the active wave at an already completed mainline task.
  - 2026-04-23: updated the master execution plan so D-TASK-017 is treated as completed repository-side work and the remaining Hetu/MRS follow-up is explicitly downgraded to environment-backed evidence rather than an active mainline implementation task; recompiled governance policy files after the plan truth changed.
- Context closeout:
  - Completed scope: Aligned post-closeout governance state after D-TASK-017 by clearing the stale active-wave pointer in the master execution plan, capturing the previously uncommitted validation-log tail into a governed repair batch, and returning the repository audit chain to a clean state without rewriting D-TASK-017 historical conclusions.
  - Validation evidence: Validated with python3 scripts/foreman.py validate HARN-012 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "node scripts/lint-repository-knowledge.js"; python3 scripts/task_audit.py --check --phase pre-closeout; and python3 scripts/foreman.py compile-governance after the master-plan truth update.
  - Residual risk: The repository-side drift is closed, but real Hetu/MRS evidence remains environment-backed and still depends on external execution of scripts/run-hetu-env-smoke.sh plus environment-specific credentials and runtime tuning.
  - Next step: Use the now-clean repository state to coordinate external Hetu/MRS smoke execution and archive the returned JDBC/REST/CLIENT evidence; only open a new repo-side task if production-style Hetu parameter tuning or another governed follow-up is approved.

### D-TASK-017: 落实 `query-execution` 真实 Hetu 集成与 smoke 分层

- Status: done
- Completed at: 2026-04-23
- Commit subject: `feat(query-execution): integrate real hetu execution chain`
- Priority: 1
- Depends on: `D-TASK-016`
- Scope: 让 HETU 主路径切到真实 JDBC/REST/CLIENT 接入，补齐 JDBC 驱动接线、Hetu client 协议执行、严格路由与错误语义，并提供 repo-closed runtime smoke 与 environment-backed Hetu/MRS smoke 入口 Tech: `JAVA-BE`,`OPS`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若真实 Hetu 集成重新退回 `SIMULATED` 冒充成功、放宽只读边界、绕过统一授权入口，或在未确认外部依赖时默认启用高风险生产参数，需人工确认
- Data impact: query-execution Hetu 连接配置、执行链路、审计记录、runtime/env smoke 证据
- Rollback / recovery: 恢复受控模式顺序、严格 HETU 路由失败语义、统一授权前置检查，并回退到上一版受控配置与文档说明
- Validation:
  - `query-execution 模块测试、跨模式适配测试、runtime smoke、Hetu env smoke 脚本/文档、task audit`
  - `python3 scripts/foreman.py validate D-TASK-017`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: added real Hetu integration primitives in `query-execution`, including `io.hetu.core:hetu-jdbc` wiring, strict HETU route-unavailable semantics, Hetu client protocol execution, and updated controller/service/adapter coverage.
  - 2026-04-23: updated runtime/env smoke assets with a local mock Hetu coordinator, runtime smoke real-mode assertions, and a dedicated `scripts/run-hetu-env-smoke.sh` entry for external Hetu/MRS environments.
  - 2026-04-23: `mvn -B -pl query-execution -am test -DskipITs`, `bash scripts/run-runtime-smoke.sh --runtime-smoke`, `bash -n scripts/run-runtime-smoke.sh scripts/manual-query-governance-smoke.sh scripts/run-hetu-env-smoke.sh`, `python3 -m py_compile scripts/mock-hetu-server.py`, and `python3 scripts/foreman.py compile-governance` passed; `python3 scripts/foreman.py compile-governance --check` passed after recompiling `.codex/policy/authority-map.json`.
- Context closeout:
  - Completed scope: Promoted query-execution to the real Hetu integration stage by wiring the Hetu JDBC driver, enforcing strict HETU route-unavailable semantics instead of simulated success, implementing Hetu client-protocol execution, updating runtime/env smoke assets with a mock Hetu coordinator and external Hetu smoke entry, and syncing the affected architecture/deployment truth documents.
  - Validation evidence: Validated with python3 scripts/foreman.py validate D-TASK-017 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "mvn -B -pl query-execution -am test -DskipITs" --extra-command "bash scripts/run-runtime-smoke.sh --runtime-smoke" --extra-command "bash -n scripts/run-runtime-smoke.sh scripts/manual-query-governance-smoke.sh scripts/run-hetu-env-smoke.sh" --extra-command "python3 -m py_compile scripts/mock-hetu-server.py" --extra-command "bash scripts/run-hetu-env-smoke.sh --help"; runtime smoke proved query-execution returned HETU_REAL_INTEGRATION with CLIENT mode on the real Hetu path.
  - Residual risk: Repository-closed validation now covers real Hetu mode execution through the local mock coordinator and strict route semantics, but long-lived external Hetu/MRS evidence, deployment credentials, and production parameter calibration still depend on environment-backed execution of scripts/run-hetu-env-smoke.sh against a provisioned cluster.
  - Next step: Proceed to the next governed follow-up after the external environment owner captures Hetu/MRS smoke evidence with scripts/run-hetu-env-smoke.sh, or continue with downstream query-execution production tuning if Phase-D priorities still target Hetu operations hardening.

### D-TASK-016: 收口治理授权矩阵并下沉统一授权入口

- Status: done
- Completed at: 2026-04-23
- Commit subject: `feat(governance): unify authorization matrix enforcement`
- Priority: 1
- Depends on: `D-TASK-015`
- Scope: 在 governance 落地角色矩阵、资源模型、数据源授权矩阵，把真实授权决策下沉为 query/sql-optimization/benchmark 统一入口，并补齐授权成功/拒绝/跨租户/吊销后访问与权限变更审计 Tech: `JAVA-BE`,`SQL`,`OPS`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若角色矩阵、资源模型或数据源授权矩阵被放宽为 fail-open，或三服务重新分叉授权入口，需人工确认
- Data impact: governance 授权配置、跨服务授权决策、审计记录与 runtime smoke 证据
- Rollback / recovery: 恢复统一授权入口、默认拒绝语义、被吊销访问阻断，以及权限变更审计补录
- Validation:
  - `governance/三服务模块测试、runtime smoke、task audit、契约/安全文档同步`
  - `python3 scripts/foreman.py validate D-TASK-016`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Governance role/resource/datasource matrices are active, authorization now flows through a single decision entrypoint for query-execution, sql-optimization, and benchmark-engine, and smoke/test coverage now includes allow/deny/cross-tenant/revoked cases plus permission-change auditing.
  - Validation evidence: python3 scripts/foreman.py compile-governance --check; mvn -B -pl governance,query-execution,sql-optimization,benchmark-engine -am -DskipITs test; bash scripts/run-runtime-smoke.sh --runtime-smoke; python3 scripts/foreman.py validate D-TASK-016 --include-task-audit --extra-command ...
  - Residual risk: Authorization matrices are still config-backed with runtime mutation held in-process; distributed persistence and external IAM synchronization remain future work.
  - Next step: Proceed with the next priority: real Hetu integration in query-execution while keeping the unified governance authorization entrypoint unchanged.

### F-TASK-033: 补齐测试环境最小 smoke 门禁

- Status: done
- Completed at: 2026-04-23
- Commit subject: `ops(smoke): add minimal test-environment smoke gate`
- Priority: 1
- Depends on: `F-TASK-032`
- Scope: 提供环境无关的最小 smoke 入口，供外部测试环境 CI/CD 在部署后执行四个后端 health、前端可达性、query/sql-optimization/benchmark 到 governance 的最小业务链路，以及受保护请求头有效性验证；本地 runtime smoke 契约保持不变 Tech: `OPS`,`DOCS`. Layer: `deployments/ci/scripts`,`docs`.
- Matrix context: Phase-F / Story `F-STORY-005` 发布门禁自动化与稳定性收口
- Human confirmation point: 若要把测试环境 minimal smoke 升级为仓库 repo-closed 主路径替代项、重新引入对测试环境内部 DB/容器的强绑定，或要求外部环境 owner 接受新的破坏式认证/访问前提，需人工确认
- Data impact: 环境无关 smoke 脚本、外部测试环境 CI/CD 接入方式、health/API 断言语义与部署文档真值
- Rollback / recovery: 保留新增入口为 environment-backed 部署后验证层，回退对外部环境的强绑定假设，并继续维持本地 runtime smoke 作为 repo-closed 主路径
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-033`、`python3 scripts/task_audit.py --check --phase pre-closeout`、`bash scripts/run-env-smoke.sh --help`、`bash scripts/run-env-smoke.sh --check-config`、`bash scripts/run-runtime-smoke.sh --compose-check`、`node scripts/lint-repository-knowledge.js`
  - `python3 scripts/foreman.py validate F-TASK-033`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added an environment-agnostic minimal smoke entrypoint for external test-environment CI/CD, introduced shared smoke HTTP/JSON helpers reused by the existing manual governance smokes, and synchronized plan/truth/deployment/local-development docs so test-environment minimal smoke is explicitly separated from repo-closed runtime smoke and Sonar/Kafka fallback semantics; task numbering was intentionally advanced to F-TASK-033 because historical F-TASK-032 already exists and could not be rewritten.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-033; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js; bash scripts/run-runtime-smoke.sh --compose-check; bash scripts/run-env-smoke.sh --help; bash scripts/run-env-smoke.sh --check-config; FRONTEND_BASE_URL=http://localhost:3001 bash scripts/run-env-smoke.sh; bash -n scripts/smoke-lib.sh scripts/run-env-smoke.sh scripts/manual-query-governance-smoke.sh scripts/manual-sql-optimization-governance-smoke.sh scripts/manual-benchmark-governance-smoke.sh
  - Residual risk: The repository now provides the minimal test-environment smoke entrypoint, but the external test environment still has an independent owner and no Codex runtime; it only gains a real deployment-after-smoke closure once that owner wires scripts/run-env-smoke.sh into its CI/CD and preserves execution evidence.
  - Next step: Have the external test-environment owner call bash scripts/run-env-smoke.sh after deployment and archive the resulting logs/status as environment-backed smoke evidence; no repository-side mainline task remains after this closeout.

### HARN-011: Reconcile F-TASK-032 post-closeout drift

- Status: done
- Completed at: 2026-04-23
- Commit subject: `fix(governance): reconcile F-TASK-032 post-closeout drift`
- Priority: 1
- Depends on: N/A
- Scope: Clear the stale active-wave pointer to F-TASK-032 in docs/plans/master-execution-plan.md and mark F-TASK-032 as completed in docs/deployments/phase-gate-baseline.md without rewriting any historical task conclusions.
- Validation:
  - `python3 scripts/foreman.py validate HARN-011`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Aligned post-closeout governance state after F-TASK-032 by clearing the stale active-wave pointer in the master execution plan, marking F-TASK-032 as completed in the phase-gate follow-up mapping, and recompiling the authority-map policy to match the updated plan truth.
  - Validation evidence: python3 scripts/foreman.py compile-governance; python3 scripts/foreman.py validate HARN-011; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: F-TASK-032 remains complete; only future human-directed Sonar environment restoration in INBOX-001 could change the current default gate semantics.
  - Next step: No repository-side follow-up remains for the Sonar fallback governance round unless humans choose to restore mandatory Sonar or stronger environment-bound release gating.

### F-TASK-032: 去除 Sonar fallback 的隐性自动恢复接线，并分离 provisioning / enable 语义

- Status: done
- Completed at: 2026-04-23
- Commit subject: `ci(gates): separate sonar provisioning from enable semantics`
- Priority: 1
- Depends on: `F-TASK-031`
- Scope: 去除 release workflow 默认 `quality-gate` environment 绑定，给主 CI 增加显式 Sonar enable 条件，并同步 Sonar runbook、INBOX 与部署基线，使 provisioning 不再等于自动恢复强制 Sonar Tech: `OPS`,`DOCS`. Layer: `deployments/ci/scripts`,`docs`.
- Matrix context: Phase-F / Story `F-STORY-005` 发布门禁自动化与稳定性收口
- Human confirmation point: 若要把“环境已 provision”重新视为“默认自动启用 Sonar 强制门禁”，或恢复 release workflow 的环境级默认绑定，需人工确认
- Data impact: CI/release workflow 触发条件、Sonar enable flag、环境恢复 runbook、INBOX 语义与部署基线
- Rollback / recovery: 恢复当前显式 enable 语义，保留 provisioning 证据与恢复入口；如要再次升级为默认强制，需拆新任务追加治理记录
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-032`、`python3 scripts/task_audit.py --check --phase pre-closeout`、`bash scripts/run-sonar.sh`、`bash scripts/run-sonar.sh --require-config`、`node scripts/lint-repository-knowledge.js`、workflow / docs 语义核对
  - `python3 scripts/foreman.py validate F-TASK-032`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Removed the default release quality-gate environment binding, required explicit SONAR_ENABLE_DEFAULT enablement before CI/release workflows consume provisioned Sonar config, and synchronized the Sonar runbook, INBOX, plan, and deployment baselines around provisioning-versus-enable semantics.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-032; python3 scripts/task_audit.py --check --phase pre-closeout; bash scripts/run-sonar.sh; bash scripts/run-sonar.sh --require-config (expected failure without SONAR_HOST_URL/SONAR_TOKEN); node scripts/lint-repository-knowledge.js
  - Residual risk: Restoring Sonar as a default hard gate still requires human-controlled provisioning plus explicit enablement or a new follow-up task; external test-environment CI/CD still lacks repo-equivalent smoke and cannot replace repo-closed gates.
  - Next step: No repository-side follow-up remains unless humans decide to restore mandatory Sonar or reintroduce stronger environment-bound release gating.

### HARN-010: Reconcile F-TASK-031 post-closeout drift

- Status: done
- Completed at: 2026-04-23
- Commit subject: `fix(governance): reconcile F-TASK-031 post-closeout drift`
- Priority: 1
- Depends on: F-TASK-031
- Scope: 对齐 F-TASK-031 closeout 后遗留的 active-wave 指针、phase-gate follow-up 描述与 validation-log commit hash 证据，不改写 F-TASK-031 历史完成结论。
- Validation:
  - `python3 scripts/foreman.py validate HARN-010`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Aligned post-closeout governance state after F-TASK-031 by clearing stale active-wave/follow-up pointers and repairing runtime validation so it no longer hardcodes an already-archived task id.
  - Validation evidence: python3 -m py_compile scripts/validate_codex_runtime.py; python3 scripts/foreman.py validate HARN-010; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js
  - Residual risk: F-TASK-031 remains complete; only future human-directed environment restoration work in INBOX-001 could change default Sonar semantics.
  - Next step: No repository-side follow-up remains for F-TASK-031; only act again if humans choose to restore mandatory Sonar or other environment-backed gates.

### F-TASK-031: 将 Sonar 与环境级门禁降级为 fallback，并建立双层门禁语义

- Status: done
- Completed at: 2026-04-23
- Commit subject: `ci(gates): downgrade sonar and env gates to fallback`
- Priority: 1
- Depends on: `F-TASK-030`
- Scope: 把 repo-closed 主路径与 environment-backed 增强项显式拆层，修正 Sonar / 真实 Kafka / release gate 默认语义，同时保留独立脚本与 workflow 作为 fallback 入口 Tech: `OPS`,`DOCS`. Layer: `deployments/ci/scripts`,`docs`.
- Matrix context: Phase-F / Story `F-STORY-005` 发布门禁自动化与稳定性收口
- Human confirmation point: 若要把 Sonar 或真实 Kafka 再次恢复为仓库默认硬阻断，或改变 repo-closed / environment-backed 双层边界，需人工确认
- Data impact: phase gate/release gate workflow、脚本默认值、门禁文档口径、INBOX 环境恢复项
- Rollback / recovery: 恢复 fallback 语义、保留环境恢复 runbook 与 INBOX 追踪，必要时再拆独立任务重新升级为强制门禁
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-031`、`bash scripts/run-phase-gates.sh --gate entry`、`bash scripts/run-phase-gates.sh --gate delivery --coverage-phase phase1plus`、`bash scripts/run-phase-gates.sh --gate compliance`、`bash scripts/run-sonar.sh`、`bash scripts/run-sonar.sh --require-config`、`node scripts/lint-repository-knowledge.js`
  - `python3 scripts/foreman.py validate F-TASK-031`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reframed repository governance around repo-closed primary gates plus environment-backed fallback gates, updated R-117/docs/workflows/scripts/INBOX semantics, and preserved Sonar/real Kafka entrypoints without default blocking.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-031; python3 scripts/task_audit.py --check --phase pre-closeout; bash scripts/run-phase-gates.sh --gate entry; bash scripts/run-phase-gates.sh --gate delivery --coverage-phase phase1plus; bash scripts/run-phase-gates.sh --gate compliance; bash scripts/run-sonar.sh; bash scripts/run-sonar.sh --require-config (expected fallback failure without config); node scripts/lint-repository-knowledge.js
  - Residual risk: Restoring Sonar or real Kafka as default hard gates still requires explicit environment provisioning and a new follow-up task; external test environment CI/CD still lacks repo-equivalent smoke and cannot replace repo-closed gates.
  - Next step: Only if humans want to restore mandatory Sonar gating, use INBOX-001 to provision quality-gate or repo-level Sonar secrets and create a new upgrade task; otherwise current repo-closed semantics are complete.

### F-TASK-030: 提升覆盖率并补齐 Sonar 发布环境

- Status: done
- Completed at: 2026-04-22
- Commit subject: `ci(sonar): uplift coverage and wire release gate config`
- Priority: 1
- Depends on: `F-TASK-029`
- Scope: 把 phase1plus 聚合覆盖率提升到 85%+，补齐 Sonar 所需 secrets / 发布环境接线，并验证自动 release gate 可稳定放行 Tech: `OPS`,`DOCS`,`JAVA-BE`. Layer: `deployments/ci/scripts`,`docs`,`application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-F / Story `F-STORY-005` 发布门禁自动化与稳定性收口
- Human confirmation point: 若 coverage 提升方案会删除既有测试、放宽 85% 门槛，或 Sonar 发布环境接线涉及敏感 secrets 管理策略调整需人工确认
- Data impact: 覆盖率结果、CI/release 环境变量、Sonar 扫描结果与相关测试资产
- Rollback / recovery: 恢复到当前自动阻断发布路径，保留覆盖率 / Sonar 失败证据，并回退新增测试或 workflow 环境接线
- Next action: 在 GitHub Settings 中为正式发布链补齐 `quality-gate` environment 或仓库级 Sonar secrets / vars，然后重新触发 `Phase Gate` `delivery|full` 或 `Release Phase Gate`
- Escalation: 需要具备仓库 Settings 权限的人类完成外部 `SONAR_HOST_URL`、`SONAR_TOKEN` 与可选 `SONAR_PROJECT_KEY` / `SONAR_PROJECT_NAME` / `SONAR_QUALITY_GATE_WAIT` 配置
- Human decision: 确认正式发布链是否统一以 `quality-gate` environment 作为 Sonar 配置源，并完成外部 secrets / vars provisioning
- INBOX ref: INBOX-001
- Validation:
  - `bash scripts/run-coverage.sh --phase phase1plus`、`bash scripts/run-sonar.sh --require-config`、release gate workflow / phase gate 验证
  - `python3 scripts/foreman.py validate F-TASK-030`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: added high-leverage repository / governance client tests across `benchmark-engine`、`query-execution`、`sql-optimization`; `bash scripts/run-coverage.sh --phase phase1plus` now passes at `86.9763% (5356/6158)`.
  - 2026-04-23: wired `Phase Gate` Sonar environment injection, bound `Release Phase Gate` to GitHub Actions environment `quality-gate`, added `docs/deployments/sonar-quality-gate-provisioning.md`, and updated deployment / truth docs.
  - 2026-04-23: `python3 scripts/foreman.py validate F-TASK-030` passed; remaining blocker is external GitHub Settings provisioning captured in `INBOX-001`.
- Context closeout:
  - Completed scope: Raised repository phase1plus aggregated line coverage to 86.9763% with targeted benchmark-engine/query-execution/sql-optimization tests, wired Sonar configuration through CI, Phase Gate, and Release Phase Gate workflow paths, added the Sonar provisioning runbook, and synchronized the deployment/truth/plan documents to the repository-side release-gate baseline.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-030; python3 scripts/task_audit.py --check; mvn -B test; bash scripts/run-coverage.sh --phase phase1plus; bash scripts/run-runtime-smoke.sh --runtime-smoke
  - Residual risk: End-to-end Sonar-required release validation still depends on external GitHub Settings provisioning of SONAR_HOST_URL/SONAR_TOKEN and optional quality-gate vars or environment, which remain outside the repository and were intentionally not faked in local development.
  - Next step: When GitHub Settings access and a real Sonar backend are available, provision the required Sonar secrets/vars, rerun bash scripts/run-sonar.sh --require-config plus the Phase Gate delivery/full path and Release Phase Gate, and then close the remaining external provisioning follow-up.

### OPS-GOV-002: 新增后端四服务一键启动脚本

- Status: done
- Completed at: 2026-04-22
- Commit subject: `ops(local): add backend four-service startup script`
- Priority: 2
- Depends on: N/A
- Scope: scripts + local backend startup docs
- Validation:
  - `python3 scripts/foreman.py validate OPS-GOV-002`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added scripts/start-backend-services.sh to bootstrap the local dependency stack, optionally build backend modules, inject dev crypto key defaults, start governance/query-execution/sql-optimization/benchmark-engine, and wait for the 8080-8083 health endpoints; updated local setup and operations docs so the backend-only startup path, options, log directory, and health checks match repository truth.
  - Validation evidence: python3 scripts/foreman.py validate OPS-GOV-002
  - Residual risk: The helper depends on local Docker, Maven, Java, and free 8080-8083 ports; startup still fails fast when prerequisite services or pid files are already present, which is expected for local runtime safety.
  - Next step: Use bash scripts/start-backend-services.sh for backend-only local bring-up, and extend the runtime orchestration only if future tasks need tighter frontend/start-stop integration.

### F-TASK-029: 收口 release automation 与门禁稳定性

- Status: done
- Completed at: 2026-04-22
- Commit subject: `ci(release): automate release phase gate and stabilize coverage entry`
- Priority: 1
- Depends on: `F-TASK-027`,`F-TASK-028`
- Scope: 稳定 coverage 入口、明确 Sonar 强制约束、并把 phase gate 绑定到 release metadata 自动触发链 Tech: `OPS`,`DOCS`,`JAVA-BE`. Layer: `deployments/ci/scripts`,`docs`,`application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-F / Story `F-STORY-005` 发布门禁自动化与稳定性收口
- Human confirmation point: 若 release automation 会改变 delivery tag / write-back 语义、放宽 Sonar 必需约束或把 phase gate 自动触发绑定到错误发布事件需人工确认
- Data impact: workflow、release metadata、coverage / Sonar 门禁结果与相关测试稳定性
- Rollback / recovery: 恢复手工 phase gate 入口、保留自动化元数据证据，并回退到上一个可追溯发布路径
- Validation:
  - `release gate workflow、`bash scripts/run-coverage.sh --phase phase1plus`、Sonar-required gate 路径与 CI/workflow 验证`
  - `python3 scripts/foreman.py validate F-TASK-029`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Closed F-TASK-029 by stabilizing the benchmark-engine test context so phase1plus coverage can run deterministically again, adding an automated Release Phase Gate workflow that binds full phase-gate execution to checkpoint tag and release metadata, and updating the CI/phase-gate/document-truth/master-plan corpus so release automation, coverage blocking semantics, and Sonar-required behavior are recorded against current repository fact instead of manual follow-up notes.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-029 --include-task-audit --extra-command "mvn -B -pl benchmark-engine -am test -DskipITs" --extra-command "python3 - <<\"PY\"\nimport subprocess\nimport sys\nproc = subprocess.run([\"bash\", \"scripts/run-coverage.sh\", \"--phase\", \"phase1plus\"], text=True, capture_output=True)\noutput = proc.stdout + proc.stderr\nsys.stdout.write(output)\nif proc.returncode == 2 and \"Coverage threshold not met.\" in output and \"Required minimum line coverage: 85.00%\" in output:\n    sys.exit(0)\nprint(\"Expected phase1plus coverage gate to fail with threshold evidence.\", file=sys.stderr)\nsys.exit(1)\nPY" --extra-command "python3 - <<\"PY\"\nimport subprocess\nimport sys\nproc = subprocess.run([\"bash\", \"scripts/run-sonar.sh\", \"--require-config\"], text=True, capture_output=True)\noutput = proc.stdout + proc.stderr\nsys.stdout.write(output)\nif proc.returncode != 0 and \"Missing required Sonar configuration\" in output:\n    sys.exit(0)\nprint(\"Expected sonar gate to fail fast when required configuration is missing.\", file=sys.stderr)\nsys.exit(1)\nPY" --extra-command "rg -n \"Release Phase Gate|checkpoint/\\\\*\\\\*|release-phase-gate-metadata|--gate full --coverage-phase phase1plus --require-sonar --run-real-kafka-gate|76\\.4047%|release\\.published|coverage uplift / Sonar secrets provisioning\" .github/workflows/release-phase-gate.yml docs/deployments/ci-capability-baseline.md docs/deployments/phase-gate-baseline.md docs/plans/document-truth-baseline.md docs/plans/master-execution-plan.md"
  - Residual risk: The release automation path is now explicit and automatically blocking on checkpoint tags/releases, but repository-wide phase1plus coverage is still only 76.4047% versus the required 85%, and Sonar still depends on externally provisioned SONAR_HOST_URL/SONAR_TOKEN secrets before the automated release gate can pass end to end.
  - Next step: Instantiate the next follow-up task to raise repository coverage to the phase1plus threshold and provision Sonar secrets/CI environment so the automated release phase gate can move from deterministic blocker to stable pass path.

### F-TASK-003: 补齐环境提醒与恢复指引

- Status: done
- Completed at: 2026-04-22
- Commit subject: `docs(deploy): reconcile environment and recovery guidance`
- Priority: 1
- Depends on: `F-TASK-002`
- Scope: 输出独立 MySQL/Kafka/恢复提醒 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Matrix context: Phase-F / Story `F-STORY-001` 部署文档与编排
- Human confirmation point: RPO/RTO 或环境提醒口径改变需人工确认
- Data impact: 运维文档、恢复指引
- Rollback / recovery: 追加更正提醒
- Validation:
  - 文档与规则一致
  - `python3 scripts/foreman.py validate F-TASK-003`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reconciled F-TASK-003 against repository truth by updating operator-facing deployment guidance so local setup explicitly warns that local docker compose services do not replace production independent MySQL/TDSQL, Kafka, and formal recovery arrangements; linked the formal backup/recovery baseline into the primary operator path; and aligned the master execution plan plus document truth baseline with the current environment-reminder and recovery-guidance facts.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-003 --include-task-audit --extra-command "node scripts/lint-repository-knowledge.js" --extra-command "rg -n \"本地 .*不得替代生产独立环境|backup-recovery-baseline|RPO/RTO|verify_kafka_runtime_config|run-kafka-runtime-gate|独立 MySQL/TDSQL|恢复责任人\" docs/deployments/local-setup.md docs/deployments/huawei-cloud-setup.md docs/deployments/backup-recovery-baseline.md docs/plans/document-truth-baseline.md docs/plans/master-execution-plan.md"
  - Residual risk: Phase-F environment and recovery reminders are now aligned at the documentation layer, but formal production recovery automation, object-storage restore scripts, and deeper release-trigger hardening remain later-phase concerns outside this 1-9 reconciliation batch.
  - Next step: Report completion of the requested 1-9 sequence, then propose the next highest-value task beyond this batch only if the user asks for further execution.

### F-TASK-002: 对齐 compose 与脚本说明

- Status: done
- Completed at: 2026-04-22
- Commit subject: `docs(deploy): reconcile compose and script guidance`
- Priority: 1
- Depends on: `F-TASK-001`
- Scope: 对齐本地/离线/可选 Kafka 说明 Tech: `OPS`,`DOCS`. Layer: `deployments/ci/scripts`,`docs`.
- Matrix context: Phase-F / Story `F-STORY-001` 部署文档与编排
- Human confirmation point: compose 语义破坏式变化需人工确认
- Data impact: 编排配置、脚本入口
- Rollback / recovery: 恢复旧 compose 语义并补兼容脚本
- Validation:
  - `docker compose config`、文档一致性
  - `python3 scripts/foreman.py validate F-TASK-002`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reconciled F-TASK-002 against repository truth by aligning local and offline deployment docs with the actual docker-compose files and local-start/local-stop flow: documented that default local startup remains DATABASE-mode without automatically starting Kafka, clarified docker compose versus docker-compose fallback usage, and corrected the frontend proxy troubleshooting section to match the real 8080/8081/8082/8083 backend split.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-002 --include-task-audit --extra-command "docker-compose config" --extra-command "rg -n \"optional profile|R-144 DATABASE|localhost:8080|8081|8082|8083|docker compose up -d|docker-compose\" docs/deployments/local-setup.md docs/deployments/offline-setup.md docs/plans/master-execution-plan.md docker-compose.yml"
  - Residual risk: Compose and script guidance now matches repository truth, but F-TASK-003 still needs to close out the environment reminders and recovery-guidance truth so deployment docs, backup baselines, and operator warnings all read consistently.
  - Next step: Instantiate F-TASK-003 next and reconcile environment reminders, backup/recovery guidance, and remaining operator-facing deployment caveats against the current repository documents.

### F-TASK-001: 补齐华为云部署文档

- Status: done
- Completed at: 2026-04-22
- Commit subject: `docs(deploy): reconcile huawei cloud deployment truth`
- Priority: 1
- Depends on: `B-TASK-005`
- Scope: 完整描述华为云部署拓扑和生产切换 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Matrix context: Phase-F / Story `F-STORY-001` 部署文档与编排
- Human confirmation point: 生产部署拓扑调整需人工确认
- Data impact: 部署文档和配置模板
- Rollback / recovery: 追加修正文档并恢复旧拓扑说明
- Validation:
  - 文档存在、入口索引、语义检查
  - `python3 scripts/foreman.py validate F-TASK-001`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reconciled F-TASK-001 against repository truth by recording that the Huawei Cloud deployment document already exists, is linked from the authority stack, and covers the 4-service private-cloud topology, KAFKA production messaging mode, independent frontend/backend deployment, and backup-recovery cross references; this task is now an archive/truth-closeout rather than a missing-document implementation.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-001 --include-task-audit --extra-command "node scripts/lint-repository-knowledge.js" --extra-command "rg -n \"华为云|4 个微服务|KAFKA|backup-recovery-baseline|local-setup|offline-setup\" docs/deployments/huawei-cloud-setup.md docs/plans/document-truth-baseline.md docs/plans/master-execution-plan.md"
  - Residual risk: The Huawei Cloud deployment truth is now aligned, but F-TASK-002 still needs to reconcile local/offline compose and script guidance where minor wording drift remains, and F-TASK-003 must finish the environment reminder and recovery-guidance truth closeout.
  - Next step: Instantiate F-TASK-002 next and align compose plus script documentation with the actual docker-compose files, local-start/local-stop flows, Kafka profile usage, and frontend proxy wording.

### E-TASK-006: 深色设计系统组件化

- Status: done
- Completed at: 2026-04-22
- Commit subject: `docs(frontend): reconcile design system implementation truth`
- Priority: 1
- Depends on: `E-TASK-004`
- Scope: 把主题 token 和组件规则转成实现 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-002` 业务页面拆分
- Human confirmation point: 设计系统 token 删除需人工确认
- Data impact: 前端主题变量
- Rollback / recovery: 恢复原 token 映射
- Validation:
  - `token 生效、build、lint`
  - `python3 scripts/foreman.py validate E-TASK-006`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reconciled E-TASK-006 against repository truth by recording that the current frontend already implements the SQLForge design-system token layer in src/styles/element-plus-theme.css, applies a dark-first theme through the global store and App shell, and reuses shared token semantics across dashboard, business, and governance pages; this task is therefore archived as truth closeout rather than a net-new componentization effort.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-006 --include-task-audit --extra-command "npm run lint" --extra-command "npm run build" --extra-command "rg -n \"element-plus-theme\\.css|sqlforge-color-brand|sqlforge-font-sans|sqlforge-font-mono|toggleTheme|theme: 'dark'|sqlforge-code-label|sqlforge-section-title|route-card\" src/styles/element-plus-theme.css src/stores/index.js src/App.vue src/views/dashboard/DashboardView.vue src/views/query/SqlQueryView.vue src/views/optimization/AccelerationView.vue src/views/benchmark/BenchmarkView.vue src/views/system/SystemView.vue docs/plans/document-truth-baseline.md docs/plans/master-execution-plan.md"
  - Residual risk: Phase-E truth is now substantially aligned for route shells, live capability consumption, and tokenized theme implementation, but the remaining queue shifts to Phase-F documentation reconciliation so deployment and recovery artifacts match the already-landed repository facts with the same strictness.
  - Next step: Instantiate F-TASK-001 next and reconcile the existing Huawei Cloud deployment documentation against repository truth before closing out F-TASK-002 and F-TASK-003.

### E-TASK-005: 接入已存在治理接口能力

- Status: done
- Completed at: 2026-04-22
- Commit subject: `docs(frontend): reconcile governed page capability truth`
- Priority: 1
- Depends on: `E-TASK-004`,`Phase-C`
- Scope: 页面消费已交付后端能力 Tech: `VUE-FE`,`JAVA-BE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-002` 业务页面拆分
- Human confirmation point: API 契约破坏式变化需人工确认
- Data impact: 前端接口调用、缓存态
- Rollback / recovery: 恢复旧 API 适配层或 mock 路径
- Validation:
  - `UI/API 契约、可见性`
  - `python3 scripts/foreman.py validate E-TASK-005`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reconciled E-TASK-005 against repository truth by recording that the current business and governance pages already consume delivered backend capabilities through src/services/runtimeGateApi.js, including query execution, optimization, benchmark, tenant config, message retry/stats, and governance history lookup/detail APIs; updated the master plan active wave so this task is treated as truth closeout rather than new feature work.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-005 --include-task-audit --extra-command "npm run build" --extra-command "rg -n \"executeQuery|submitOptimizationTask|submitBenchmarkTask|getBenchmarkReport|getGovernanceTenantConfig|getGovernanceMessageStats|retryGovernanceFailedMessages|getGovernanceTraceSummaries|lookupGovernanceTraces|getGovernanceTraceDetail\" src/services/runtimeGateApi.js src/views/query/SqlQueryView.vue src/views/optimization/AccelerationView.vue src/views/benchmark/BenchmarkView.vue src/views/system/SystemView.vue src/views/parse-record/ParseRecordView.vue src/views/repair-evidence/RepairEvidenceView.vue src/views/audit-forensics/AuditForensicsView.vue src/views/audit-troubleshooting/AuditTroubleshootingView.vue docs/plans/document-truth-baseline.md docs/plans/master-execution-plan.md"
  - Residual risk: The frontend capability truth is now aligned, but E-TASK-006 still needs to reconcile design-system and token implementation coverage so Phase-E does not overstate how much of the visual system has been fully archived against current source reality.
  - Next step: Instantiate E-TASK-006 next and reconcile the current design-system/token implementation truth, then continue into the Phase-F documentation reconciliation batch F-TASK-001~003.

### E-TASK-004: 建立五大业务页路由骨架

- Status: done
- Completed at: 2026-04-22
- Commit subject: `docs(frontend): reconcile business route shell truth`
- Priority: 1
- Depends on: `E-TASK-001`
- Scope: 建立 5 个独立业务路由 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-002` 业务页面拆分
- Human confirmation point: 五大业务页若改为合并页需人工确认
- Data impact: 前端路由结构
- Rollback / recovery: 恢复独立路由和页面壳层
- Validation:
  - 每页独立入口、build
  - `python3 scripts/foreman.py validate E-TASK-004`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reconciled E-TASK-004 against repository truth by updating the master execution plan active wave and the document truth baseline to reflect that the dashboard, core business routes, and governance history/ops route shells already exist in the current frontend, so this task is now an archive/truth-closeout rather than a fresh implementation batch.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-004 --include-task-audit --extra-command "npm run build" --extra-command "rg -n \"ROUTE_PATHS\\.sqlQuery|ROUTE_PATHS\\.acceleration|ROUTE_PATHS\\.benchmark|ROUTE_PATHS\\.system|ROUTE_PATHS\\.parseRecord|ROUTE_PATHS\\.repairEvidence|ROUTE_PATHS\\.auditForensics|ROUTE_PATHS\\.auditTroubleshooting|ROUTE_PATHS\\.runtimeGates|ROUTE_PATHS\\.recoveryDrill\" src/router/index.js src/config/routePaths.mjs docs/plans/document-truth-baseline.md docs/plans/master-execution-plan.md"
  - Residual risk: The route shell truth is now aligned, but E-TASK-005 and E-TASK-006 still need to reconcile live governance capability consumption and design-system/token coverage so the frontend archive state matches the repository facts end to end.
  - Next step: Instantiate E-TASK-005 next and reconcile which already-delivered governance capabilities are actually consumed by the current business and governance pages, without re-implementing the route shells.

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
