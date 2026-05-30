# SQLForge Task Ledger

本文件是当前待办与执行中任务的唯一轻量运行台账。

- 状态只允许：`todo`、`in_progress`、`in_review`、`blocked`
- `done` 任务必须移入 `tasks-done.md`
- `blocked` 任务必须包含：`Next action:`、`Escalation:`、`Human decision:`、`INBOX ref:`
- `in_review` 任务必须包含：`Review reason:`、`Human decision:`、`INBOX ref:`
- `todo` / `in_progress` 任务不得保留未决人工判断、升级链字段或显式等待人类处理的标记
- 任务台账不替代 `docs/plans/master-execution-plan.md`

## Todo

_No tasks._


## In Progress

### USER-CN-BACKEND-LARGE-CLASS-SPLIT-20260528: 后端超 200 行类拆分

- Status: in_progress
- Priority: 1
- Depends on: N/A
- Scope: 将后端 src/main/java 中所有非空非注释代码行超过 200 行的类按职责拆分，保持 public API、持久化映射、接口契约和运行行为不变，并补充可重复统计与验证证据。
- Validation:
  - `python3 scripts/foreman.py validate USER-CN-BACKEND-LARGE-CLASS-SPLIT-20260528`
- Progress log:
  - 2026-05-29: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-29: 建立非空非注释 Java 类行数统计口径，补充后端大类拆分策略文档，并完成 SQL catalog qualifier、组合解析结论、分发事件装配、L2 CommonSubgraph、L2 StarAgg、L2 ParameterizedAgg 等低耦合功能组拆分；对应聚焦测试通过。
  - 2026-05-29: 当前局部门禁已确认 `L2CommonSubgraphMvCandidateGenerator`、`CommonSubgraph*`、`L2StarAggMvCandidateGenerator`、`StarAgg*`、`L2ParameterizedAggMvCandidateGenerator`、`ParameterizedAgg*` 均不超过 200 行；全量后端仍有剩余超限类，继续按模块拆分。
  - 2026-05-29: 继续拆分 L2 Rollup 与 L2 Prejoin 物化视图候选生成器，抽出计划解析、结构阻断策略、维度/列映射、SQL 构造、rewrite 替换与候选证据等 package-private 协作者；`L2*` 门面和 `CandidateSql` 调用契约保持不变，Rollup/Prejoin 拆分文件组局部门禁均不超过 200 行。
  - 2026-05-29: 继续拆分 `L2MaterializedViewValidationSqlBuilder` 与 `L2GrainMeasureDeriver`，保留原嵌套输入/结果与推导结果 API，抽出校验 SQL 组装、CTE/列/指标渲染、grain/measure 推导、聚合解析和 coverage 构造协作者；对应文件组局部门禁均不超过 200 行，物化视图 41 个回归用例通过。
  - 2026-05-29: 继续拆分 `L2MaterializedViewRewriteCoverageValidator`，保留原阻断码、`ValidationInput` 与 `ValidationResult` 调用契约，抽出只读 SQL 安全、FROM/JOIN 关系扫描、字段覆盖、投影/分组/指标/谓词覆盖和 profile 值处理协作者；对应文件组局部门禁均不超过 200 行，物化视图 44 个回归用例通过。
  - 2026-05-29: 继续拆分 `L2AccelerationArtifactBuilder`，保留 `RULE_PRECOMPUTE_MV`、`AccelerationRecommendationInput` 与 `CommonSubgraphPeerSql` 调用契约，抽出候选生成编排、rewrite 覆盖校验、artifact map 装配、前置阻断、coverage proof、MV 字段覆盖和通用值处理协作者；对应文件组局部门禁均不超过 200 行，MV 聚焦 49 个用例与 pipeline/large-SQL/diff 55 个用例通过。
  - 2026-05-29: 继续拆分 `L2PredicateClassifier`，保留分类常量与 `PredicateClassificationResult` 调用契约，抽出分类引擎、谓词信号函数匹配、字段族匹配和 profile 值处理协作者；对应文件组局部门禁均不超过 200 行，分类聚焦 5 个用例与 MV 聚焦 49 个用例通过。
  - 2026-05-29: 继续拆分 `MaterializedViewRecommendationPlanner`、`L2AccelerationArtifactApplicationService`、`MaterializedViewCreateApplicationService`、worker delay 处理、治理 trace VO、加速方案 trace 支撑、优化任务 JSON codec 与结果摘要 runtime delta；新增协作者均通过局部门禁，受影响聚焦测试通过，当前全量后端超限类快照降至 107 个，任务继续推进。
  - 2026-05-29: 继续拆分 governance 查询历史/trace/config/metadata VO 与 datasource/metadata 领域数据对象、shared 加密 envelope 与 runtime SQL rewrite scanner、结构解析/查询执行 metadata VO、`RuntimeRewriteBinding` 值清洗与 copy helper；公开 API、构造签名、Jackson getter 与 builder 契约保持不变，受影响 shared/governance/query/sql-optimization 聚焦测试通过，当前全量后端超限类快照降至 94 个，任务继续推进。
  - 2026-05-29: 继续拆分 `SqlForgeJdbcAgent`、`SqlSurfaceObjectRefExtractor` 与 `SqlFingerprintUtils`，抽出 JDBC agent 观测/模式执行/审计上报/改写决策协作者，以及 surface object 词法支撑与 fingerprint 注释剥离、字面量参数化、归一化、哈希协作者；保留原 public facade 与调用契约不变，受影响 shared 聚焦 25 个用例通过，shared 拆分文件组局部门禁无超限项。
  - 2026-05-29: 继续拆分 `RuntimeSqlRewriteTemplateEngine`，抽出 program 组装、模板渲染、literal replay、nested residual replay、where segment / predicate 匹配与 SQL shape 解析协作者；保留原 public API 与 template replay 语义不变，受影响 shared 聚焦 32 个用例通过，rewrite/shared 拆分文件组局部门禁无超限项。
  - 2026-05-29: 继续拆分 `QueryExecutionResultDigestService`，抽出只读结果摘要组装、执行证据装配与 schema/row/key hash 协作者；保留现有请求/响应契约、只读守卫语义与 digest 字段结构不变，受影响 query-execution 聚焦 2 个用例通过，digest 文件组局部门禁无超限项。
  - 2026-05-29: 继续拆分 `HetuRouteCalibrationService`，抽出 mode adapter 索引、snapshot 装配与 readiness/route-parameter/summary 规则协作者；保留现有路由校准快照契约与 failure classification 语义不变，受影响 query-execution 聚焦 4 个用例通过，calibration 文件组局部门禁无超限项。
  - 2026-05-29: 继续拆分 `GovernanceDatasourceAccessApplicationService`，抽出 datasource scope runtime store、访问决策解析和审计请求工厂协作者；保留租户/数据源访问范围契约、系统租户变更语义与审计写入行为不变，受影响 governance 聚焦 8 个用例通过，datasource-access 文件组局部门禁无超限项。
  - 2026-05-29: 继续拆分 `GovernanceAlertApplicationService`，抽出告警分页/详情 VO 装配与 ack 审计/领域-记录转换协作者；保留分页、详情查询、ack 语义与审计落库行为不变，受影响 governance 聚焦 3 个用例通过，alert 文件组局部门禁无超限项。
  - 2026-05-29: 继续拆分 `QueryExecutionMaterializedViewCreateService`，抽出请求上下文/响应与审计装配、治理 JDBC 数据源解析以及 MV JDBC executor/datasource/evidence 协作者；保留内部物化视图创建/refresh 契约、部分成功语义、审计落库和失败异常行为不变，受影响 query-execution 聚焦 4 个用例通过，mv-create 文件组局部门禁无超限项。
  - 2026-05-29: 继续拆分 `QueryExecutionMetricsRecorder`，抽出统一 tag 装配、attempted-mode 解析和 cache-governance 指标记录协作者，并补充 focused metrics recorder 测试；保留请求/异常指标名、timeout/fallback/route-unavailable 判定和 cache governance 观测语义不变，受影响 query-execution 聚焦 31 个用例通过，metrics 文件组局部门禁无超限项。
  - 2026-05-29: 继续拆分 `QueryExecutionCacheGovernanceRuntimeService`，抽出配置/键值与 backend evidence 支撑、binding/metadata/eviction 状态对象、metadata state store、eviction ledger、entry selection、response factory、resolve/finalize orchestration 协作者；保留 apply/verify/invalidate/resolve facade、schema-version 失配失效、TTL/容量驱逐、backend unavailable bypass 与 cache governance evidence 语义不变，受影响 query-execution 聚焦 41 个用例通过，cache-governance 文件组局部门禁无超限项。
  - 2026-05-29: 继续拆分 `GovernanceProtectedPersistenceService`，抽出 traceability 引用校验、execution/query-history surface 投影、JSON accessor 支撑和敏感字段保护协作者；保留 save/update facade、引用一致性校验、surface 回填字段和敏感字段加密/脱敏语义不变，受影响 governance 聚焦 4 个用例通过，protected-persistence 文件组局部门禁无超限项。
  - 2026-05-29: 继续拆分 `DatasourceConfigApplicationService`，抽出 datasource 通用校验/VO 装配支撑、upsert/driver-binding/credential-envelope 处理、healthcheck 和 JDBC datasource/route resolve 协作者；保留 create/update/list/find facade、连接模式与驱动绑定校验、健康检查/JDBC resolve 响应契约和凭据加密/解密语义不变，受影响 governance 聚焦 4 个用例通过，datasource-config 文件组局部门禁无超限项。


## In Review

_No tasks._


## Blocked

### USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-EXTERNAL-ARTIFACTS-20260518: 等待真实生产规模压测外部证据

- Status: blocked
- Priority: 1
- Depends on: USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-INGEST-20260518
- Scope: 等待外部生产或准生产环境 owner 提供真实压测 evidence directory 与通过校验后的 `verification-result.json`，覆盖来源 `provenance.json`、10000 并发、千万级日查询、30PB 数据布局、24 小时 replay、P95/P99、扫描字节、CPU、队列等待、成本账单和 `evidenceFileDigests`；在 artifacts 到位前不得把 repo-side verifier、runbook 或测试 fixtures 视为生产规模目标完成。
- Validation:
  - `python3 scripts/verify-benchmark-production-evidence.py --evidence-dir <external-evidence-dir> --output <external-evidence-dir>/verification-result.json`
  - `python3 scripts/audit-rewrite-production-readiness.py --verification-result <external-evidence-dir>/verification-result.json --evidence-dir <external-evidence-dir>`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-05-18: 仓库侧 `scaleTarget` 边界、生产证据 bundle verifier、证据目录 CLI 和 runbook 已提交；完成度审计仍未在仓库内发现真实外部生产 artifacts。
  - 2026-05-18: repo-side verifier/runbook 已补入 `daily-query-volume.json`，外部 artifacts 需要同时覆盖千万级日查询证明。
  - 2026-05-18: repo-side verifier/readiness audit 已要求顶层与 `scaleTargetEvidenceManifest` 同时保留 `evidenceFileDigests`，外部 artifacts 归档后必须可复算 SHA-256。
  - 2026-05-18: repo-side readiness audit 已要求同时传入原始 `--evidence-dir` 并复算必需证据文件 SHA-256/sizeBytes；仅提交 JSON 不足以完成目标。
  - 2026-05-18: repo-side verifier/readiness audit 与 benchmark manifest 已要求 `provenance.json` 和 `environmentId/environmentType/evidenceOwner/artifactArchiveRef/verifierOperator`；没有来源元数据时不得声明生产规模完成。
- Next action: 外部环境 owner 按 `docs/deployments/benchmark-production-evidence-runbook.md` 收集 evidence directory，运行 `python3 scripts/verify-benchmark-production-evidence.py --evidence-dir <external-evidence-dir> --output <external-evidence-dir>/verification-result.json` 和 `python3 scripts/audit-rewrite-production-readiness.py --verification-result <external-evidence-dir>/verification-result.json --evidence-dir <external-evidence-dir>`，并归档原始 artifacts、`provenance.json`、可复算 SHA-256 的 evidence directory、通过校验的 JSON 输出和含来源元数据与摘要的 `scaleTargetEvidenceManifest`。
- Escalation: 如果生产或准生产窗口、来源元数据、数据布局证明、长期 replay、指标导出、账单导出或原始文件摘要归档无法在目标周期提供，保持目标未完成并要求 owner 明确可执行窗口、证据归档位置和负责验收的人。
- Human decision: 确认可用于留证的生产或准生产环境、执行窗口、证据目录归档位置、成本账单来源、谁运行 verifier、谁复核 `provenance.json` 与 `evidenceFileDigests`，以及谁把通过后的含来源元数据和摘要的 `scaleTargetEvidenceManifest` 提交到 benchmark 任务。
- INBOX ref: INBOX-005

### HARN-016: Track deferred external Hetu/MRS validation

- Status: blocked
- Priority: 1
- Depends on: `D-TASK-017`, `D-TASK-018`, `HARN-013`, `HARN-014`
- Scope: Record that external Win10 test-environment Hetu/MRS validation is deferred while repository-side implementation proceeds, and wire the pending follow-up into tasks/INBOX/plan audit chain without changing business code.
- Validation:
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: converted the environment-backed Hetu/MRS verification wait into an explicit blocked governance follow-up so repository-side implementation can continue without treating external test-environment latency as an active coding blocker.
  - 2026-05-08: HARN-088 added repository-side Hetu EXPLAIN plan-analysis support; live JDBC evidence for real Hetu/MRS credentials remains part of this blocked environment-backed validation chain.
- Next action: When the Win10 test environment is ready, deploy the yml-based governance/query-execution configuration from the runbook, run `bash scripts/run-hetu-env-smoke.sh` for one of `JDBC` / `REST` / `CLIENT`, and archive the returned log/response proof outside the repository.
- Escalation: If the external environment remains unavailable or credentials/connectivity are still uncertain after the deployment window opens, keep repository implementation moving and ask the environment owner to provide the executable window, reachable Hetu/MRS endpoint, and evidence retention location.
- Human decision: Confirm the deployment window, final Hetu mode, target datasource credentials, and who will archive the live smoke evidence in the real test environment.
- INBOX ref: INBOX-002
