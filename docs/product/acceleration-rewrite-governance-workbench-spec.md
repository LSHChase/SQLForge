# 加速与改写治理工作台方案

## Summary

本文件落地 `HARN-127` 对加速、改写、SQL 解析、SQL 历史、周期比对与告警闭环的复盘设计。本文是目标方案与任务拆分，不代表当前代码已经全部实现。

当前代码事实必须继续以 `docs/plans/document-truth-baseline.md` 为准：仓库已经具备 SQL 优化任务、解析记录、推荐中心、加速计划生命周期接口、查询执行侧已批准绑定应用，以及 SQL 历史的基础查询面；尚未具备完整的物理加速编排、深度改写规则体系、SQL diff 语义视图、改写记录持久化、周期性结果比对和自动差异告警闭环。

## Incorporated Requirements

本轮方案已并入以下历史与新增要求：

- 加速功能应独立于路由与改写，作为可选增强能力，来源可以是联机 SQL、解析工作台解析结果、批量解析、报表解析、慢 SQL、P99 超阈值或扫描量过大。
- SQL 解析不应被隐藏在加速流程里；解析驱动与查询驱动保留两条入口，后续加速候选、计划审批、应用验证在同一页面承接。
- 页面必须有字段标识来源是 `PARSE` 还是 `QUERY`，并展示 `sourceId`、`parseHistoryId` 或 `historyId`、`sqlFingerprint` 等追溯键。
- 模拟测试页面设计应复用已实现页面功能；已有能力用跳转，不重复造入口；仅在尚无页面或接口操作面时，在本页提供轻量真实接口测试按钮。
- 流程图只放在模拟/治理工作台页，不把已有解析、推荐、SQL 历史页面改成流程图页面。
- 推荐 SQL 逻辑不能停留在简单替换，必须分层深化，输出规则、前置条件、风险、收益、验证方式和是否允许自动应用。
- 推荐前后的 SQL 必须在页面上可对比差异，不能只展示两个代码块。
- 改写记录必须能在 SQL 历史查询页面看到。
- 改写前后的 SQL 结果必须定期比对；发现结果差异时要暂停自动应用、写历史记录并触发监控告警。

## Current Implementation Review

### Implemented Facts

- `sql-optimization` 已提供异步优化任务，包含 `PARSE`、`REWRITE`、`ACCELERATION_SUGGESTION` 类型。
- `sql-optimization` 已提供 `acceleration_plan` 生命周期接口，覆盖提交、审批、应用、验证和回滚。
- `query-execution` 已能按 SQL 指纹和 `PREFER_ACCELERATED` 执行偏好应用已批准加速绑定。
- 前端已存在解析工作台、批量解析中心、解析历史、推荐中心、SQL 查询和 SQL 历史等页面。
- 推荐中心已有原 SQL / 推荐 SQL 的基础展示，SQL 历史已有 `rewriteApplied` 与关联 refs 的基础展示。

### Pending Gaps

- 当前加速计划更接近“治理闭环 + 运行时绑定开关”，不能写成已完成物化视图、分区、分桶、拆分执行或生产装数。
- 当前推荐 SQL 规则偏保守，深度结构改写不足；多数复杂反模式只能回退为人工复核或推荐 SQL 等于原 SQL。
- 当前执行拦截依赖显式执行偏好和运行时绑定，不等于所有后续同指纹 SQL 自动应用。
- 当前没有统一的 `sourceType/sourceId` 加速候选对象，解析驱动与查询驱动之间缺少同一套后续治理对象。
- 当前没有面向 operator 的加速候选、计划审批、应用验证、监控告警同页工作台。
- 当前没有 SQL diff、规则级 diff、AST 摘要差异或风险高亮。
- 当前没有独立 `sql_rewrite_record` / `rewrite_validation_run` 查询面，也没有 SQL 历史详情里的改写记录 tab。
- 当前没有周期性原 SQL / 推荐 SQL 结果比对，也没有 `SQL_REWRITE_RESULT_DIVERGENCE` 告警事件闭环。

## Boundary Decisions

- SQLForge 负责发现候选、生成推荐、审批、绑定、验证、结果比对、监控告警和审计追溯。
- SQLForge 不直接执行生产改表、建物化视图、装数或修改底层存储；真实物理加速继续遵守 `PULL_ONLY` 外部协同边界。
- 静态解析证据必须标注 `staticOnly=true`，不得伪装成真实扫描量、真实耗时或真实收益。
- 高风险改写默认 `MANUAL_REVIEW_REQUIRED`；没有元数据、唯一键、排序语义或结果验证证据时，不允许自动应用。
- 周期比对必须走后端只读执行与治理审计，不允许前端拉取全量结果自行判定。

## Unified Flow

### Entry A: Parse-Driven

解析工作台、批量解析、报表解析或解析历史产生：

- `sourceType=PARSE`
- `sourceId=parseHistoryId | parseTaskId | batchId`
- `parseHistoryId`
- `parseTaskId`
- `batchId`
- `reportCode`
- `sqlFingerprint`
- `datasourceCode`
- `stage`
- `issueScenes`
- `staticOnly`

随后进入统一流程：

1. 证据归一
2. 加速 / 改写候选
3. 推荐 SQL 与加速建议生成
4. SQL diff 与风险展示
5. 计划审批
6. 应用验证
7. 运行时绑定或推荐分发
8. 效果监控与周期比对
9. 失效、暂停、回滚或废弃

### Entry B: Query-Driven

SQL 历史、慢 SQL、P99 超阈值、高扫描量、压测回归或人工输入产生：

- `sourceType=QUERY`
- `sourceId=historyId | executionId | benchmarkTaskId`
- `historyId`
- `executionId`
- `benchmarkTaskId`
- `sqlFingerprint`
- `datasourceCode`
- `stage`
- `elapsedMs`
- `p99Ms`
- `scannedRows`
- `scannedBytes`
- `runtimeEvidence`

若缺少结构解析证据，则工作台提供“先解析再生成建议”的显式动作，不把解析伪装成已经完成。

## Product State Mapping

| Product state | Backend status mapping | Meaning |
|:---|:---|:---|
| 草稿 | `DRAFT`, `PENDING_APPROVAL`, recommendation `RECOMMENDED` | 建议已生成，尚未确认或未形成可应用计划 |
| 待生效 | `APPROVED`, `APPLYING` | 已审批，等待应用、外部协同或运行时绑定 |
| 生效 | `APPLIED`, `VERIFIED`, `ACTIVE` | 已应用并进入监控窗口 |
| 待复核 | `VERIFY_FAILED`, validation `DIVERGED`, monitor `REGRESSED` | 验证失败、收益衰减或结果差异，需要人工处理 |
| 失效 | `INVALID`, `EXPIRED` | schema、模型、artifact、新鲜度或外部依赖失效 |
| 废弃 | `ROLLED_BACK`, `DEPRECATED`, `CANCELLED` | 用户取消、系统回滚或长期无收益 |

## Workbench Page Design

页面建议命名为 `加速治理工作台`，定位为真实接口 smoke 与正式治理流的统一 operator 页面。它不替代解析工作台、推荐中心或 SQL 历史页面。

### Layout

- 顶部紧凑 header：页面标题、当前租户、数据源、环境、`sourceType`、`sourceId`、`sqlFingerprint`。
- 双入口 segmented control：`解析驱动` / `查询驱动`。
- 入口表单：
  - `sourceType`
  - `parseHistoryId` 或 `historyId`
  - `sqlText`
  - `datasourceCode`
  - `schemaName`
  - `stage`
  - `reportCode`
  - `enableHetuExplain`
- 流程图：只在本页展示，节点为 `入口证据 -> 候选建议 -> SQL 差异 -> 计划审批 -> 应用验证 -> 监控告警 -> 回滚/废弃`。
- 操作区 tabs：
  - `候选建议`
  - `SQL 差异`
  - `计划审批`
  - `应用验证`
  - `监控与告警`
  - `接口证据`
- 右侧或底部证据抽屉：展示请求 JSON、响应 JSON、trace id、history id、config snapshot id、result id。

### Existing Page Reuse

已实现页面只做跳转，不在工作台重复完整功能：

- 解析详情：跳转到解析历史。
- 查询详情：跳转到 SQL 历史。
- 推荐详情：跳转到推荐中心。
- SQL 执行：跳转到 SQL 查询。
- 告警详情：跳转到告警中心。

### Lightweight Real Interface Actions

对于现有页面尚未覆盖的操作，工作台提供最小按钮：

1. 生成建议：`POST /api/sql-optimization/tasks`
2. 查询建议任务：`GET /api/sql-optimization/tasks/{taskId}`
3. 创建计划：`POST /api/sql-optimization/acceleration-plans`
4. 审批计划：`POST /api/sql-optimization/acceleration-plans/{planId}/approval`
5. 应用计划：`POST /api/sql-optimization/acceleration-plans/{planId}/apply`
6. 验证计划：`POST /api/sql-optimization/acceleration-plans/{planId}/verify`
7. 执行加速查询：`POST /api/query-execution/queries/execute`
8. 回滚计划：`POST /api/sql-optimization/acceleration-plans/{planId}/rollback`

当目标接口尚未实现时，按钮必须显示 `未实现` 或 `需要后续任务`，不能提交 mock 成功。

### Visual Rules

- 页面采用工具型布局，不使用营销式 hero。
- 不使用卡片套卡片；流程图、tabs、表格和证据抽屉使用统一 section。
- JSON 只做下钻，主区展示可读摘要、状态、差异和风险。
- 对长 SQL 使用已有 SQL 组件；推荐 SQL 与原 SQL 对比必须支持 diff。
- 每个 tab 顶部固定展示 `sourceType` 与追溯键，避免来源丢失。

## Recommendation Logic

推荐 SQL 分三层输出，不同层级必须明确自动应用边界。

### L0 Safe Syntax Rewrites

| Rule | Trigger | Recommended action | Auto apply |
|:---|:---|:---|:---|
| `COUNT_ONE_TO_COUNT_STAR` | `COUNT(1)` | 改为 `COUNT(*)` | 可在验证通过后自动 |
| `DUPLICATE_GROUP_ORDER_KEY` | 重复 group/order key | 去重保持顺序 | 可在验证通过后自动 |
| `TRAILING_ORDER_IN_SUBQUERY` | 不影响最终语义的子查询排序 | 移除冗余排序 | 仅低风险场景 |
| `NORMALIZE_ALIAS` | 别名冲突或不可读 | 规范别名 | 默认人工确认 |

### L1 Structural Rewrites

| Rule | Trigger | Required evidence | Recommended action | Risk |
|:---|:---|:---|:---|:---|
| `SELECT_STAR_EXPANSION` | `SELECT *` | 元数据字段清单 | 展开必要列 | 投影变更风险 |
| `OR_TO_UNION_ALL` | 大量 OR 谓词 | 谓词互斥或去重策略 | 拆分为 `UNION ALL` 或 `UNION` | 行重复风险 |
| `FUNCTION_PREDICATE_TO_RANGE` | 分区/索引列被函数包裹 | 字段类型、时区规则 | 改为范围谓词 | 时间边界风险 |
| `SCALAR_SUBQUERY_TO_JOIN` | SELECT 标量子查询 | 唯一性证明 | 改为 JOIN/聚合 JOIN | 行膨胀风险 |
| `REPEATED_SUBQUERY_TO_CTE` | 重复子查询 | 子查询无副作用 | 抽取 CTE | 引擎优化差异 |
| `NOT_EXISTS_TO_ANTI_JOIN` | 反关联查询 | NULL 语义证明 | 改为 anti join | NULL 语义风险 |
| `LEADING_LIKE_REVIEW` | 前导 `%LIKE` | 字段索引/搜索能力 | 推荐搜索索引或改写方案 | 默认人工复核 |
| `ORDER_RANDOM_REVIEW` | 随机排序 | 采样目标 | 推荐采样策略 | 结果随机性风险 |

### L2 Engine / Physical Coordination

| Rule | Trigger | Output | Boundary |
|:---|:---|:---|:---|
| `PRECOMPUTE_MV` | 高频聚合、报表复用 | 物化视图建议、刷新策略、预热 SQL | PULL_ONLY |
| `PARTITION_PRUNING` | 时间/业务键过滤高频 | 分区键建议、分区过滤改写 | PULL_ONLY |
| `BUCKET_JOIN` | 大表 join 且 join key 稳定 | 分桶建议、join key 证据 | PULL_ONLY |
| `SPLIT_SQL` | 大 SQL 可拆为独立子任务 | 拆分 SQL、聚合方式、并发边界 | 需验证 |
| `RESULT_CACHE` | 低变更、高复用查询 | cache policy、TTL、新鲜度条件 | 查询执行侧绑定 |
| `REPORT_SQL_MERGE` | 同报表多 SQL 高重叠 | 合并候选与人工复核说明 | 不自动生成执行 SQL |
| `STATISTICS_REFRESH` | 计划劣化疑似统计过期 | 统计刷新建议 | 外部执行 |
| `FILE_COMPACTION` | 小文件多或扫描碎片高 | compact 建议 | 外部执行 |

每条推荐必须输出：

- `recommendationId`
- `sourceType`
- `sourceId`
- `sqlFingerprint`
- `originalSql`
- `recommendedSql`
- `ruleChain[]`
- `unappliedRules[]`
- `preconditions[]`
- `semanticRisks[]`
- `expectedBenefit`
- `estimatedCost`
- `confidence`
- `validationMethod`
- `autoApplyAllowed`
- `manualReviewRequired`

## SQL Difference View

推荐中心、加速治理工作台和 SQL 历史改写记录详情都应复用同一 diff 视图。

能力要求：

- 左右对比：原 SQL / 推荐 SQL。
- 统一 diff：新增、删除、移动、替换高亮。
- 规则级 diff：点击 `ruleChain` 中某条规则，只高亮该规则带来的变化。
- AST 摘要差异：
  - 投影列变化
  - 表与别名变化
  - Join 类型变化
  - 谓词变化
  - Group / Order / Limit 变化
  - 聚合粒度变化
- 风险提示：
  - 是否改变返回列
  - 是否改变过滤条件
  - 是否改变 join 类型
  - 是否改变去重或聚合语义
  - 是否依赖时区、NULL 或 Decimal 精度规则

## SQL History Rewrite Records

SQL 历史详情必须新增 `改写记录` tab，列表页新增相关筛选。

### Detail Tab Fields

- `rewriteRecordId`
- `recommendationId`
- `optimizationTaskId`
- `sourceType`
- `sourceId`
- `historyId`
- `parseHistoryId`
- `sqlFingerprint`
- `originalSql`
- `recommendedSql`
- `executedSql`
- `ruleChain`
- `diffSummary`
- `validationStatus`
- `lastValidationRunId`
- `lastComparedAt`
- `alertStatus`
- `traceRefs`

### List Filters

- `hasRewriteRecord`
- `rewriteValidationStatus`
- `rewriteSourceType`
- `recommendationId`
- `rewriteApplied`

SQL 历史不得只依赖 `recommendationRefs` 中的弱引用展示改写。后端需要提供可查询、可分页、可审计的改写记录聚合面。

## Periodic Result Comparison

### Lifecycle

1. 生成改写建议时创建验证策略草稿。
2. 审批前至少执行一次影子比对。
3. 生效后按策略周期比对原 SQL 与推荐 SQL。
4. schema 变更、分区变更、统计信息刷新、数据刷新失败或加速 artifact 失效时触发额外比对。
5. 发现差异时：
   - 标记 `validationStatus=DIVERGED`
   - 暂停自动应用
   - 写入 SQL 历史改写记录
   - 写入推荐详情
   - 触发 `SQL_REWRITE_RESULT_DIVERGENCE` 告警

### Comparison Rules

- schema 一致性：列名、顺序、类型、精度。
- 行数一致性：总行数或抽样窗口行数。
- 主键集合一致性：存在主键或唯一键时按 key 比对。
- 排序敏感 SQL：按顺序逐行比较。
- 非排序 SQL：按集合 hash 或分桶 checksum 比较。
- 数值误差：Decimal、Double、聚合值使用可配置容差。
- 时间规则：统一时区、日期边界和格式化策略。
- NULL 规则：区分 NULL、空字符串、0 和缺失字段。
- 大结果集：使用抽样 + 分桶 checksum，不把全量结果拉回前端。

### Alert Event

建议告警类型：

- `SQL_REWRITE_RESULT_DIVERGENCE`
- `SQL_REWRITE_VALIDATION_FAILED`
- `ACCELERATION_PLAN_REGRESSED`
- `ACCELERATION_ARTIFACT_INVALIDATED`

告警 payload 至少包含：

- `tenantId`
- `sourceType`
- `sourceId`
- `historyId`
- `parseHistoryId`
- `recommendationId`
- `rewriteRecordId`
- `validationRunId`
- `sqlFingerprint`
- `differenceType`
- `sampleEvidence`
- `autoApplyPaused`

## Target Data Objects

以下为目标模型，当前未全部实现。

### `acceleration_candidate`

- 归属：`sql-optimization`
- 主键：`candidate_id`
- 结构化字段：`tenant_id`, `source_type`, `source_id`, `history_id`, `parse_history_id`, `task_id`, `sql_fingerprint`, `datasource_code`, `stage`, `candidate_type`, `status`, `confidence`, `priority`, `created_by`, `created_at`, `updated_at`
- JSON 字段：`source_evidence_json`, `issue_evidence_json`, `runtime_evidence_json`, `benefit_estimate_json`, `cost_estimate_json`, `risk_json`

### `sql_rewrite_record`

- 归属：`sql-optimization`，由 `governance` 查询面聚合到 SQL 历史详情。
- 主键：`rewrite_record_id`
- 结构化字段：`tenant_id`, `recommendation_id`, `optimization_task_id`, `source_type`, `source_id`, `history_id`, `parse_history_id`, `sql_fingerprint`, `datasource_code`, `status`, `validation_status`, `auto_apply_allowed`, `manual_review_required`, `created_by`, `created_at`, `updated_at`
- 大文本字段：`original_sql_text`, `recommended_sql_text`, `executed_sql_text`
- JSON 字段：`rule_chain_json`, `diff_summary_json`, `risk_json`, `trace_refs_json`

### `rewrite_validation_run`

- 归属：`sql-optimization` 或 `benchmark-engine` 协同；只读执行通过 `query-execution`。
- 主键：`validation_run_id`
- 结构化字段：`tenant_id`, `rewrite_record_id`, `recommendation_id`, `history_id`, `sql_fingerprint`, `status`, `comparison_status`, `difference_type`, `started_at`, `finished_at`
- JSON 字段：`comparison_policy_json`, `original_result_digest_json`, `recommended_result_digest_json`, `difference_sample_json`, `execution_evidence_json`

## Target Interfaces

新增或扩展接口应保持后端权威，前端只负责编排。

- `POST /api/sql-optimization/acceleration-candidates`
- `GET /api/sql-optimization/acceleration-candidates/{candidateId}`
- `GET /api/sql-optimization/recommendations/{recommendationId}/diff`
- `POST /api/sql-optimization/rewrite-records`
- `GET /api/sql-optimization/rewrite-records`
- `GET /api/sql-optimization/rewrite-records/{rewriteRecordId}`
- `POST /api/sql-optimization/rewrite-records/{rewriteRecordId}/validation-runs`
- `GET /api/sql-optimization/rewrite-records/{rewriteRecordId}/validation-runs`
- `GET /api/governance/query-history/{historyId}/rewrite-records`

现有接口继续作为 repo-closed smoke 主路径：

- `POST /api/sql-optimization/tasks`
- `GET /api/sql-optimization/tasks/{taskId}`
- `POST /api/sql-optimization/acceleration-plans`
- `POST /api/sql-optimization/acceleration-plans/{planId}/approval`
- `POST /api/sql-optimization/acceleration-plans/{planId}/apply`
- `POST /api/sql-optimization/acceleration-plans/{planId}/verify`
- `POST /api/sql-optimization/acceleration-plans/{planId}/rollback`
- `POST /api/query-execution/queries/execute`

## Testability

### Repo-Closed Testable Now

- 提交 `ACCELERATION_SUGGESTION` 任务并轮询完成。
- 创建、审批、应用、验证、回滚 acceleration plan。
- 执行 `PREFER_ACCELERATED` 查询并检查 `metadata.accelerationApplied`。
- 打开解析、推荐、SQL 历史页面并验证追溯跳转。

### Requires Follow-Up Implementation

- 统一 candidate 对象。
- SQL diff API 与规则级 diff。
- 深度推荐规则。
- 改写记录持久化与 SQL 历史聚合。
- 周期性结果比对与差异告警。
- 加速治理工作台同页审批、验证、监控。

### Environment-Backed Only

- 真实 Hetu / MRS EXPLAIN、扫描量、P99、物化视图收益与生产级外部装数证据。
- 这些证据继续归入 `HARN-016` / `INBOX-002` 外部环境链，不阻塞 repo-closed 设计和实现任务。

## Codex Task Decomposition

以下任务按单任务单 commit 设计，后续每次由人类选择一个任务并通过 `/plan` 细化后实现。

| Task ID | Title | Primary output | Key validation |
|:---|:---|:---|:---|
| `HARN-128` | 固化加速候选与改写记录后端契约 | `sql-optimization` 目标 DTO/VO、状态枚举、接口契约文档和最小 controller/service 骨架 | `mvn -pl sql-optimization test`, contract tests |
| `HARN-129` | 落地 `acceleration_candidate` / `sql_rewrite_record` / `rewrite_validation_run` 持久化 | SQL migration、entity、mapper XML、repository tests | db-script check, mapper tests |
| `HARN-130` | 深化推荐 SQL 规则输出模型 | L0/L1/L2 rule model、rule chain、risk/precondition/unapplied rules 输出 | `SqlOptimizationPipelineServiceTest` |
| `HARN-131` | 实现首批 L0/L1 安全改写规则 | `COUNT(*)`、重复 group/order、select star 元数据化、重复子查询 CTE 候选、函数谓词区间候选 | parser/rewrite tests |
| `HARN-132` | 建立 SQL diff 后端服务 | text diff、rule-level diff、AST summary diff API | diff service tests |
| `HARN-133` | 加速候选生成统一入口 | parse-driven/query-driven source normalization 与 candidate API | service/controller tests |
| `HARN-134` | 改写记录写入与 SQL 历史聚合接口 | `query-history/{historyId}/rewrite-records` 聚合面 | governance + optimization contract tests |
| `HARN-135` | 周期比对执行模型与只读比较引擎 | validation policy、result digest、schema/row/hash comparison | comparison engine tests |
| `HARN-136` | 周期比对调度与差异告警 | scheduled validation、pause auto-apply、alert event linkage | scheduler/alert tests |
| `HARN-137` | 前端加速治理工作台壳层 | 双入口、流程图、source fields、已有页面跳转、证据抽屉 | lint/build/page governance/screenshots |
| `HARN-138` | 工作台候选、计划审批与应用验证 tabs | 真实接口按钮、状态映射、响应证据 | browser smoke + API mock contract |
| `HARN-139` | 推荐中心 SQL diff 与规则详情 | diff 视图、ruleChain、risk/precondition 展示 | recommendation page contract |
| `HARN-140` | SQL 历史改写记录 tab 与筛选 | list filters、history detail rewrite tab、diff 跳转 | history page/detail contract |
| `HARN-141` | 监控与告警前端联动 | validation status、告警入口、自动暂停证据展示 | alert/history/workbench contract |
| `HARN-142` | 端到端 smoke 与文档收口 | 加速治理工作台 repo-closed smoke、runbook、契约检查脚本 | `foreman validate`, frontend smoke, knowledge lint |

## Execution Order

推荐执行顺序：

1. `HARN-128` 至 `HARN-134`：后端契约、持久化、推荐、diff 与历史聚合。
2. `HARN-135` 至 `HARN-136`：周期比对和告警闭环。
3. `HARN-137` 至 `HARN-141`：前端工作台、推荐中心、SQL 历史和告警展示。
4. `HARN-142`：端到端 smoke、runbook 和文档收口。

任何任务若发现必须改变生产装数、真实物化视图执行、权限边界、历史保留策略或外部环境默认依赖，必须暂停并拆出人工确认项。

## Related Documents

- `docs/product/sql-governance-platform-implementation-spec.md`
- `docs/architecture/sql-governance-interface-extension-baseline.md`
- `docs/architecture/sql-governance-data-model-extension.md`
- `docs/adr/ADR-013-acceleration-service-and-materialized-view-strategy.md`
- `docs/frontend/design-system.md`
- `docs/plans/master-execution-plan.md`
