# SQL Governance Interface Extension Baseline

## Summary

本文件在现有 [service-interface-contract-baseline.md](./service-interface-contract-baseline.md) 之上，为 SQL 治理产品线补齐查询、历史、结构解析、数据访问解析、批量解析、推荐治理事件、压测模板/测试集、开放接入与告警接口扩展基线。本文档定义的是目标契约，不代表当前代码已全部实现。

## 1. Shared Context Extensions

在统一受保护请求上下文基础上，SQL 治理产品线新增以下跨接口上下文字段：

| Field | Required | Description |
|:---|:---|:---|
| `accessChannel` | Yes | `PAGE/API/JDBC_AGENT/SDK/CLIENT` |
| `reportCode` | No | 从 SQL 注释或批量导入元数据提取 |
| `stage` | No | 报表阶段，如 `DEV/UAT/PROD` |
| `bizDate` | No | 执行日期 / 业务批次日期 |
| `datasourceCode` | No | 注释或显式选择的数据源 |
| `engineHint` | No | 注释或策略给出的目标引擎偏好 |
| `priority` | No | 查询、批量、推荐、压测的治理优先级 |

## 2. Query Execution Extension Contracts

### 2.1 Execute Query

- `POST /api/query-execution/queries/execute`

请求体：

- `sqlText`
- `sqlTemplateText`
- `bindParameters`
- `bindingMode`
  - `POSITIONAL`
  - `NAMED`
- `datasourceCode`
- `catalog`
- `schemaName`
- `logicalObjectRefs`
- `commentContext`
- `queryContext`
  - `timeoutMs`
  - `schemaVersion`
  - `accelerationPreference`
  - `faultToleranceStrategy`

响应体必须包含：

- `executionId`
- `status`
- `rowCount`
- `resultPreview`
- `commentContext`
- `queryDateSummary`
- `bindingSummary`
  - `bindingRenderStatus`
    - `SUCCESS`
    - `PARTIAL`
    - `FAILED`
    - `MASKED`
- `logicalObjectHits`
- `cacheSummary`
- `routeSummary`
- `lightweightParseSummary`
- `traceSummary`

### 2.2 Explain Query

- `POST /api/query-execution/queries/explain`

返回：

- `executionId`
- `explainMode`
- `planSummary`
- `warningSummary`
- `routeSummary`
- `logicalObjectHits`

### 2.3 Query Execution Detail

- `GET /api/query-execution/queries/{queryId}`

返回：

- 原始 SQL
- 模板 SQL
- 绑定参数
- 绑定后 SQL
- 注释上下文
- query date
- 执行链路
- 结果摘要
- 缓存命中
- 路由结果
- 轻量改写结果

## 3. Query History Contracts

查询历史契约只承载 SQL 执行历史和执行追溯语义。SQL 解析记录由 `sql-optimization` 的 parse-history 契约承载，不再通过 `historyType=SQL_PARSE` 或解析页请求 `/api/governance/query-history` 做逻辑隔离。

### 3.1 History List

- `GET /api/governance/query-history`

支持过滤：

- `timeRange`
- `reportCode`
- `tenantId`
- `datasourceCode`
- `stage`
- `bizDate`
- `queryDateStart`
- `queryDateEnd`
- `status`
- `cacheHit`
- `rewriteApplied`
- `accelerationApplied`
- `parameterizedSql`
- `logicalObjectType`
- `accessChannel`
- `engine`
- `historyType=QUERY_EXECUTION` 固定执行历史语义，不能用于解析记录查询

### 3.2 History Detail

- `GET /api/governance/query-history/{historyId}`

返回区块：

- `sqlText`
- `sqlTemplateText`
- `boundSqlText`
- `sqlState`
- `commentContext`
- `queryDateSummary`
- `logicalObjectHits`
- `executionSummary`
- `routeDecision`
- `recommendationRefs`
- `benchmarkRefs`
- `auditRefs`
- `alertRefs`

### 3.3 History Export

- `POST /api/governance/query-history/export`

请求：

- `historyId`
- `exportFormat`
- `includeTraceDetail`
- `exportReason`

导出格式：

- `CSV`
- `EXCEL`
- `JSON`
- `SQL_TEXT`
- `PDF_REPORT`

说明：

- 一期所有导出格式统一落到 `inline evidentiary export` 基线
- `PDF_REPORT` 一期返回 textual evidence payload，并同步写入 `export_record + audit_log`

## 4. Parse Contracts

### 4.1 Structure Parse

- `POST /api/sql-optimization/parse/structure`

请求：

- `sqlText`
- `sqlTemplateText`
- `bindParameters`
- `bindingMode`
- `datasourceCode`
- `commentContext`

响应：

- `parseTaskId`
- `parseType=STRUCTURE`
- `syntaxStatus`
- `complexityLevel`
- `sqlType`
- `queryDateSummary`
- `logicalObjectHits`
- `riskTags`
- `rewriteCandidates`
- `issues`
- `priorityScore`
- `priorityLevel`
- `important`
- `urgent`

`queryDateSummary` 子字段：

- `queryDateStart`
- `queryDateEnd`
- `queryDateFields`
- `queryDateStatus`

`queryDateStatus` 枚举：

- `RESOLVED`
- `PARTIAL`
- `UNRESOLVED`

`logicalObjectHits[]` 子字段：

- `objectType`
- `objectKey`
- `objectName`
- `catalogName`
- `schemaName`
- `matchSource`
- `resolved`
- `mappedPhysicalTargets`

`objectType` 枚举：

- `BUSINESS_VIEW`
- `DB_VIEW`
- `TABLE`

`objectKey` 规则：

- `TYPE:qualified_object_name_lowercase`
- 例如 `DB_VIEW:analytics.vw_sales_daily`
- 作为查询、历史、解析、路由之间的统一引用键

数据库 View 展开口径：

- 当结构解析请求携带 `datasourceCode` 时，SQL 优化服务应优先通过目标数据源只读元数据实时判断命中对象是否为底层数据库 View，并读取 View definition SQL。
- 实时命中 DB View 时，`logicalObjectHits` 保留该 View 的 `DB_VIEW` hit，`matchSource=LIVE_DB_VIEW_METADATA`；View definition 中递归解析出的最终叶子表追加为 `TABLE` hit，`matchSource=DB_VIEW_DEFINITION`。
- `mappedPhysicalTargets` 对 DB View 表示最终叶子 `TABLE:*` keys，而不是原 SQL 中的 View 名；`featureSummary.tableCount` 与解析历史 `logicalObjectKeys` 使用最终唯一 `TABLE:*` keys。
- 递归展开默认最大深度为 5，并按规范化对象 key 防循环；循环、无权限、无配置、元数据查询失败或 View definition 不可解析时，结构解析不得失败，应保留原对象 hit，并通过 `DB_VIEW_DEFINITION_UNRESOLVED` risk/issue 标识降级原因。
- governance 的 `database_view_ref/dependency` 目录只能作为实时元数据不可用时的缓存/降级证据；使用该目录时 `matchSource=DB_VIEW_CATALOG_FALLBACK`，不得写成实时 View definition 结果。

`issues[]` 子字段：

- `issueCode`
- `issueDomain`
- `issueScene`
- `severity`
- `summary`
- `detail`
- `suggestedAction`
- `important`
- `urgent`
- `affectedSqlCount`
- `affectedReportCount`
- `priorityScore`
- `priorityLevel`

结构解析问题域：

- `STRUCTURE`
- `PERFORMANCE`
- `DATA`
- `ROUTING`
- `COMPATIBILITY`
- `CONVENTION`
- `GOVERNANCE`

结构解析统计评分口径：

- `severity`: `INFO | LOW | MEDIUM | HIGH | CRITICAL`
- `priorityLevel`: `P1 | P2 | P3 | P4`
- `important` 表示影响治理决策或后续改写/加速判断
- `urgent` 表示可能影响在线执行、路由或大面积失败风险
- 评分基础来自 `severity`，再叠加场景权重、important、urgent、受影响 SQL 数和受影响报表数
- `P1`: 分数大于等于 90，或 important + urgent 且分数大于等于 80
- `P2`: 分数大于等于 70
- `P3`: 分数大于等于 40
- `P4`: 其余

首版问题场景目录：

- `PARSER_FAILURE`: 结构问题，高严重度，important
- `WIDE_PROJECTION`: 治理问题，中严重度，important
- `MISSING_FILTER`: 性能问题，高严重度，important + urgent
- `UNBOUNDED_SORT`: 性能问题，中严重度
- `MULTI_JOIN_COMPLEXITY`: 结构问题，中严重度，important
- `QUERY_DATE_UNRESOLVED`: 数据问题，中严重度，important
- `ROUTE_HINT_CONFLICT`: 路由问题，高严重度，important + urgent
- `DIALECT_INCOMPATIBLE`: 兼容问题，高严重度，important
- `PARAMETER_BINDING_RISK`: 结构问题，中严重度，important
- `GENERAL_WARNING`: 规范问题，低严重度

结构解析严重度：

- `INFO`
- `LOW`
- `MEDIUM`
- `HIGH`
- `CRITICAL`

结构解析优先级：

- `P1`
- `P2`
- `P3`
- `P4`

结构解析复杂度：

- `SIMPLE`
- `MODERATE`
- `COMPLEX`
- `EXTREME`

结构解析语法状态：

- `VALID`
- `INVALID`

说明：

- 结构解析入口为同步接口
- 语法不可解析时仍返回 `200 OK + syntaxStatus=INVALID`
- 不因 parser failure 阻断结构解析证据面返回

### 4.2 Access Parse

- `POST /api/sql-optimization/parse/access`

请求：

- 继承 structure parse 请求
- `connectionRequired=true`

响应：

- `parseTaskId`
- `parseType=ACCESS`
- `serviceStatus`
- `connectionStatus`
- `objectResolutionStatus`
- `planSummary`
- `partitionStatus`
- `dataFreshnessStatus`
- `slaStatus`
- `compatibilityStatus`
- `availabilityWarning`
- `degradeReason`

`serviceStatus` 枚举：

- `AVAILABLE`
- `UNAVAILABLE`
- `SKIPPED`

`connectionStatus` 枚举：

- `CONNECTED`
- `FAILED`
- `UNAVAILABLE`
- `SKIPPED`

`objectResolutionStatus` 枚举：

- `RESOLVED`
- `PARTIAL`
- `UNAVAILABLE`
- `SKIPPED`

### 4.3 Combined Parse Status

- `POST /api/sql-optimization/parse/combined`
- `GET /api/sql-optimization/parse/{parseTaskId}`

`POST /parse/combined` 必须表达：

- 先执行结构解析
- 若连接可用则自动触发 access parse
- 返回统一 `parseTaskId` 与初始结构解析结果
- 初始同步返回应保留 `ACCESS_PARSING` 过渡态

必须能表达：

- `CREATED`
- `STRUCTURE_PARSING`
- `STRUCTURE_SUCCEEDED`
- `ACCESS_PARSING`
- `ACCESS_SUCCEEDED`
- `PARTIAL_SUCCEEDED`
- `FAILED`

### 4.4 Parse History

- `GET /api/sql-optimization/parse-history`
- `GET /api/sql-optimization/parse-history/{parseHistoryId}`
- `POST /api/sql-optimization/parse-history/export`

列表支持过滤：

- `tenantId`
- `sourceType`
- `reportCode`
- `datasourceCode`
- `stage`
- `bizDate`
- `queryDateStart`
- `queryDateEnd`
- `status`
- `logicalObjectType`
- `accessChannel`
- `engine`
- `submittedBy`
- `traceId`
- `parseTaskId`
- `submittedStart`
- `submittedEnd`
- `sortBy`
- `sortOrder`
- `pageNo`
- `pageSize`

列表项返回：

- `parseHistoryId`
- `historyId`: 兼容前端旧字段时等同于 `parseHistoryId`
- `historyType=SQL_PARSE_RECORD`
- `tenantId`
- `sourceType`
- `sourceId`
- `batchKey`
- `parseTaskId`
- `sqlFingerprint`
- `datasourceCode`
- `datasourceType`
- `reportCode`
- `stageCode`
- `bizDate`
- `queryDateStart`
- `queryDateEnd`
- `queryDateStatus`
- `accessChannel`
- `parserMode`
- `bindingMode`
- `parameterizedSqlFlag`
- `resultStatus`
- `targetEngine`
- `traceId`
- `requestId`
- `sagaId`
- `submittedBy`
- `submittedAt`
- `createdAt`
- `updatedAt`

详情额外返回：

- `sqlText`
- `sqlTemplateText`
- `sqlState`
- `structureParseSummary`
- `accessParseSummary`
- `executionSummary`
- `queryContext`
- `commentContext`
- `bindingSummary`
- `logicalObjectHits`
- `issueScenes`
- `logicalObjectKeys`
- `recommendationRefs`
- `benchmarkRefs`
- `alertRefs`
- `auditRefs`

导出请求：

- `parseHistoryId`
- `exportFormat`: 当前支持 `JSON` / `CSV`
- `exportReason`
- `includeTraceDetail`

边界：

- `sql_parse_history` 是解析记录持久化真源，属于 `sql-optimization`。
- `query_history` 是 SQL 执行历史持久化真源，属于治理追溯链，不再新增解析记录。
- SQL 执行历史写入由 `query-execution` 调用 `POST /api/governance/internal/query-execution-history/write` 提交执行证据，`governance` 负责同一事务内落 `execution_result`、`query_history` 与审计引用。
- 结构解析、综合解析、批量解析、报表解析和日终慢 SQL 解析写入 `sql_parse_history`。
- 日终慢 SQL 解析只读慢 SQL 执行历史候选，按窗口、阈值和 `batchKey + sqlFingerprint` 幂等生成解析记录；真实 cron 调度不属于本契约当前实现。

## 5. Batch Parse Contracts

### 5.1 Create Parse Batch

- `POST /api/sql-optimization/parse-batches`

字段：

- `batchName`
- `importMode`
- `fileType`
- `templateVersion`
- `datasourceCode`
- `structureParseOnly`

`importMode` 枚举：

- `SQL_FILE`
- `TABULAR_FILE`
- `REPORT_CATALOG`

`fileType` 枚举：

- `XLS`
- `XLSX`
- `ET`
- `CSV`
- `TXT`
- `SQL`

返回基线：

- `batchId`
- `tenantId`
- `batchName`
- `importMode`
- `sourceType`
- `fileType`
- `templateVersion`
- `datasourceCode`
- `structureParseOnly`
- `status`
- `supportedFileTypes`
- `templateColumns`
- `statusHistory`
- `createdAt`
- `updatedAt`

### 5.2 Ingest Parse Batch Payload

- `POST /api/sql-optimization/parse-batches/{batchId}/ingest`

字段：

- `fileName`
- `contentBase64`
- `charset`

当前 repo-side 基线：

- 稳定支持 `xlsx/csv/txt/sql`
- 二级兼容支持 `xls`
- `et` 若无法按 workbook 兼容解析，必须返回“请改用 XLSX/CSV”的显式失败提示
- `contentBase64` 由接入方自行编码
- `SQL_FILE` 模式按 SQL 语句切分
- `TABULAR_FILE` 模式要求提供表头并至少包含 `sql_text`
- 结构解析先跑；当 `structureParseOnly=false` 时再编排 access parse

### 5.3 Parse Batch Detail

- `GET /api/sql-optimization/parse-batches/{batchId}`

返回：

- 批次摘要
- 导入记录
- 结构解析统计
- 数据访问解析统计
- 问题统计
- 报表统计
- 失败记录
- 失败记录与导入记录使用同一详情字段集合，至少保留 `itemId`、`sequenceNumber`、`reportCode`、`sqlText`、`parseTaskId`、结构解析状态、Access 状态、`failureReason`、问题场景、逻辑对象和时间戳，供批量解析页点击查看详情

当前 repo-side 基线至少冻结：

- `UPLOADED -> VALIDATING -> READY -> RUNNING_STRUCTURE -> RUNNING_ACCESS -> COMPLETED | PARTIAL_COMPLETED | FAILED` 状态链
- 模板列契约
- 支持文件类型矩阵

### 5.4 Retry Access Parse for Batch

- `POST /api/sql-optimization/parse-batches/{batchId}/retry-access`

字段：

- `failureFilter`
- `datasourceCode`
- `forceRecheckAvailability`

当前 repo-side 基线：

- 默认仅重试 `PARTIAL_SUCCESS` 的批次记录
- `failureFilter` 支持 `ALL | UNAVAILABLE | FAILED`
- `datasourceCode` 可覆盖原批次行上的 datasource 进行补跑

### 5.5 Report Batch Import

- `POST /api/sql-optimization/report-batches/import`

字段：

- `batchName`
- `fileType`
- `reportCodeField`
- `datasourceCode`
- `stage`
- `priority`
- `contentBase64`
- `charset`

repo-side 基线：

- `CSV` / `XLSX` / `XLS` / `ET` 报表批次载荷采用位置语义：第一列是报表代码，第二列及之后每个 trim 后非空的单元格都是该报表代码下的一条 SQL
- 表头可选；若存在表头，第一列表头可使用 `report_code`、`reportCode`、`报表代码` 或请求中的 `reportCodeField`，后续列名只作为 `sqlColumnName` 追溯标签
- 空单元格跳过，不产生 SQL；实现不得依赖固定 SQL 列数，需支持 100+ 后续 SQL 列
- 只有报表代码且没有内联 SQL 的记录保留 resolver / `TXT_MOCK_SOURCE` 回退语义

### 5.6 Resolve Report SQLs

- `POST /api/sql-optimization/report-batches/{batchId}/resolve-sqls`

repo-side 基线：

- `sql-optimization` 通过统一 `ReportSqlResolver` 获取报表 SQL
- 当 governance 配置不可用、未配置、禁用或远端拉取失败时，保留 txt/mock source 回退路径
- 回退不阻断结构解析和数据访问解析编排

### 5.7 Report Interface Config

- `POST /api/governance/report-interface-configs`
- `GET /api/governance/report-interface-configs?tenantId={tenantId}`
- `POST /api/governance/internal/report-interface-configs/resolve`

配置字段：

- `tenantId`
- `datasourceCode`
- `stage`
- `sourceType`
- `endpointCode`
- `endpointName`
- `baseUrl`
- `pathTemplate`
- `httpMethod`
- `reportCodeParamName`
- `sqlJsonPath`
- `authMode`
- `timeoutMs`
- `enabled`

解析返回：

- `resolverStatus`: `ACTIVE | DISABLED | MOCK_FALLBACK`
- `sourceType`: `HTTP_API | TXT_MOCK_SOURCE`
- `unavailableReason`
- `contractStage`
- `implementationStage`

约束：

- 首版真实拉取抽象只实现 `HTTP_API + GET + JSON sql field`
- 不在 repo 中绑定真实外部 endpoint、secret 或 live inventory
- 未命中配置时返回 `TXT_MOCK_SOURCE` / `MOCK_FALLBACK`

### 5.8 Report Batch Detail

- `GET /api/sql-optimization/report-batches/{batchId}`

返回：

- 批次摘要
- 导入记录
- 解析后的 SQL 列表
- 结构解析统计
- 数据访问解析统计
- 失败记录
- 报表 SQL 级详情字段至少包含 `itemId`、`sequenceNumber`、`reportCode`、`reportName`、`sourceFileLine`、`sqlColumnName`、`sqlOrdinalInReport`、`sqlText`、`parseTaskId`、结构解析状态、Access 状态、`failureReason`、问题场景、逻辑对象和时间戳
- 前端可基于上述字段让成功、部分成功和失败 SQL 都可点击查看详情；失败记录不得因状态为 `FAILED` 而禁用详情入口

## 6. Parse Statistics Contracts

- `GET /api/sql-optimization/parse-statistics/overview`
- `GET /api/sql-optimization/parse-statistics/by-issue-scene`
- `GET /api/sql-optimization/parse-statistics/by-sql`
- `GET /api/sql-optimization/parse-statistics/by-report`
- `GET /api/sql-optimization/parse-statistics/priority-matrix`
- `GET /api/sql-optimization/parse-statistics/important-urgent`

当前 repo-side 基线：

- `overview`: `totalSqlCount`,`issueSqlCount`,`totalIssueCount`,`issueSceneCount`,`importantSqlCount`,`urgentSqlCount`,`priorityDistribution`
- `by-issue-scene`: `issueScene`,`issueDomain`,`severity`,`priorityLevel`,`priorityScore`,`affectedSqlCount`,`affectedIssueCount`,`sqlRatio`,`important`,`urgent`
- `by-sql`: `itemId`,`batchId`,`parseTaskId`,`reportCode`,`datasourceCode`,`stage`,`sqlDigest`,`issueCount`,`highestPriorityLevel`,`highestPriorityScore`,`important`,`urgent`,`issueScenes`
- `by-report`: `reportCode`,`sqlCount`,`issueSqlCount`,`issueCount`,`issueSqlRatio`,`highestPriorityLevel`,`highestPriorityScore`,`important`,`urgent`,`issueScenes`
- `priority-matrix`: `priorityLevel`,`urgencyBucket`,`sqlCount`,`issueCount`,`reportCount`
- `important-urgent`: returns `by-sql` records where `important=true` or `urgent=true`
- 当前按 `RequestContext.tenantId` 过滤批量解析记录，避免跨租户聚合

## 7. Recommendation and Dispatch Contracts

- `GET /api/sql-optimization/recommendations`
- `GET /api/sql-optimization/recommendations/{recommendationId}`
- `GET /api/sql-optimization/recommendations/{recommendationId}/trace`
- `POST /api/sql-optimization/recommendations/{recommendationId}/dispatch`
- `GET /api/sql-optimization/dispatch-events`
- `GET /api/sql-optimization/dispatch-events/{dispatchEventId}`
- `POST /api/sql-optimization/dispatch-events/{dispatchEventId}/pull`
- `POST /api/sql-optimization/dispatch-events/{dispatchEventId}/ack`
- `POST /api/sql-optimization/dispatch-events/{dispatchEventId}/fail`
- `GET /api/sql-optimization/dispatch-contract`

`Recommendation` 查询对象至少返回：

- `recommendationId`
- `tenantId`
- `recommendationType`: `REWRITE`,`ACCELERATION`,`CREATE_TABLE`,`PREWARM`,`MAINTENANCE`
- `sourceSqlId`
- `historyId`
- `parseTaskId`
- `batchId`
- `routeDecisionId`
- `alertId`
- `sqlFingerprint`
- `sourceSqlText`
- `recommendedSqlText`
- `targetEngine`
- `targetDatasource`
- `reportCode`
- `logicalObjectKey`
- `summary`
- `reason`
- `expectedGain`
- `benefitLevel`: `UNKNOWN`,`LOW`,`MEDIUM`,`HIGH`
- `riskLevel`: `UNKNOWN`,`LOW`,`MEDIUM`,`HIGH`,`CRITICAL`
- `riskSummary`
- `requiresDispatch`
- `status`: `RECOMMENDED`,`REVIEWING`,`DISPATCH_READY`,`CANCELLED`
- `createdBy`
- `createdAt`
- `updatedAt`

边界：

- 推荐对象只表达建议、收益、风险、目标 SQL 与是否需要装数协同。
- 解析命中 `OR_PREDICATE_INDEX_RISK`、`SELECT_STAR`、`NESTED_SUBQUERY_RISK`、`LEADING_WILDCARD_LIKE_RISK` 时，可由解析链路自动生成 `REWRITE` 推荐；无安全改写规则时 `recommendedSqlText` 保守等于原始 SQL，并在 `reason` / `riskSummary` 标记人工处理要求。
- SQLForge 不在 recommendation status 中提供 `EXECUTED` 状态；执行与装数回执由后续 `DispatchEvent` 承载。
- `requiresDispatch=true` 只表示需要外部装数/预热协同，不表示本项目已执行装数。
- trace 查询只返回同租户 `historyId/parseTaskId/batchId/routeDecisionId/alertId/sqlFingerprint/reportCode/logicalObjectKey` 等引用键，以及同租户 dispatch event；跨服务详情由各自受权接口查询。

`HARN-127` 之后，推荐与加速治理目标契约扩展为；`HARN-143` / `HARN-144` 的复核修正是后续实现的最新基线：

- `POST /api/sql-optimization/acceleration-candidates`
- `GET /api/sql-optimization/acceleration-candidates/{candidateId}`
- `GET /api/sql-optimization/recommendations/{recommendationId}/diff`
- `POST /api/sql-optimization/rewrite-records`
- `GET /api/sql-optimization/rewrite-records`
- `GET /api/sql-optimization/rewrite-records/{rewriteRecordId}`
- `POST /api/sql-optimization/rewrite-records/{rewriteRecordId}/validation-runs`
- `GET /api/sql-optimization/rewrite-records/{rewriteRecordId}/validation-runs`
- `GET /api/governance/query-history/{historyId}/rewrite-records`

新增字段至少覆盖：

- `sourceType`: `PARSE`,`QUERY`
- `sourceKind`: `STRUCTURE_PARSE`,`COMBINED_PARSE`,`PARSE_BATCH`,`REPORT_BATCH`,`END_OF_DAY_SLOW_SQL`,`QUERY_HISTORY`,`SLOW_SQL`,`HIGH_P99`,`HIGH_SCAN`,`BENCHMARK_REGRESSION`,`MANUAL`
- `sourceId`
- `evidenceLevel`: `STATIC_PARSE`,`ACCESS_PARSE`,`EXPLAIN_PLAN`,`RUNTIME_HISTORY`,`BENCHMARK`,`MIXED`
- `schemaVersion`
- `parseHistoryId`
- `historyId`
- `ruleChain[]`
- `unappliedRules[]`
- `preconditions[]`
- `semanticRisks[]`
- `diffSummary`
- `validationMethod`
- `validationStatus`
- `autoApplyAllowed`
- `manualReviewRequired`
- `manualReviewRequired`

推荐 SQL diff 契约必须提供文本 diff、规则级 diff 与 AST 摘要差异。SQL 历史详情不得只依赖 `recommendationRefs` 弱引用展示改写记录，应通过 `query-history/{historyId}/rewrite-records` 聚合同租户可见的改写记录、验证状态和告警引用。

改写结果周期比对契约目标：

- `validationStatus`: `NOT_VALIDATED`,`VALIDATING`,`EQUIVALENT`,`DIVERGED`,`FAILED`,`EXPIRED`
- 差异类型至少包括 `SCHEMA_DIFF`,`ROW_COUNT_DIFF`,`KEY_SET_DIFF`,`ORDER_DIFF`,`VALUE_DIFF`,`CHECKSUM_DIFF`,`TIMEZONE_OR_PRECISION_DIFF`
- 比对不得把大结果集全量拉回前端；后端返回 schema digest、row count、key/hash/checksum digest 和有限差异样本。
- 发现 `DIVERGED` 时必须暂停自动应用，并触发 `SQL_REWRITE_RESULT_DIVERGENCE` 告警事件。
- 默认运行时优先应用门槛为 `planStatus=VERIFIED|ACTIVE`、`validationStatus=EQUIVALENT`、`benefitStatus=POSITIVE`、`schemaVersion` 未过期；`APPLIED` 仅表示已写入配置或绑定，不得等同于生效。

治理事件载荷至少包括：

- `recommendationId`
- `dispatchType`
- `sqlText`
- `targetEngine`
- `targetDatasource`
- `relatedReportCode`
- `relatedLogicalObject`
- `expectedEffect`

`DispatchEvent` 状态机：

- `CREATED -> PUBLISHED -> PULLED -> ACKED`
- `CREATED -> PUBLISHED -> PULLED -> FAILED`
- `PUBLISHED` 表示可被外部装数/预热模块拉取，不表示已主动推送或已执行。
- `PULLED` 表示外部模块已拉取事件，但还未回执成功或失败。
- `ACKED` / `FAILED` 仅表达外部回执状态，本项目不直接装数、不直接执行推荐 SQL。

`dispatch-contract` 固定边界：

- `coordinationMode=PULL_ONLY`
- `sqlExecutionAllowed=false`
- `dataLoadingAllowed=false`
- `activeExternalPushAllowed=false`
- `externalPullRequired=true`
- 允许状态仅为 `CREATED`,`PUBLISHED`,`PULLED`,`ACKED`,`FAILED`
- 允许类型仅为 `REWRITE_SQL`,`ACCELERATION_SQL`,`CREATE_TABLE_SQL`,`PREWARM_SQL`,`MAINTENANCE_SQL`
- 外部模块负责真实装数、预热执行、底层存储变更和执行失败处置；SQLForge 只管理推荐、事件、回执和审计证据。

`DispatchEvent` 查询对象至少返回：

- `dispatchEventId`
- `tenantId`
- `recommendationId`
- `dispatchType`
- `dispatchPayloadJson`
- `targetEngine`
- `targetDatasource`
- `reportCode`
- `logicalObjectKey`
- `status`
- `pulledBy`
- `pulledAt`
- `ackedBy`
- `ackedAt`
- `failedBy`
- `failedAt`
- `resultMessage`
- `statusHistory`
- `createdBy`
- `createdAt`
- `updatedAt`

## 8. Routing Contracts

- `GET /api/governance/routing-rules`
- `POST /api/governance/routing-rules`
- `GET /api/governance/routing-decisions`
- `GET /api/governance/routing-decisions/{decisionId}`

## 9. Data Asset Contracts

- `GET /api/governance/datasources`
- `GET /api/governance/datasources/{datasourceId}`
- `POST /api/governance/datasources`
- `PUT /api/governance/datasources/{datasourceId}`
- `POST /api/governance/datasources/{datasourceId}/test-connection`
- `GET /api/governance/metadata/schemas`
- `GET /api/governance/metadata/tables`
- `GET /api/governance/metadata/snapshots`
- `GET /api/governance/logical-views`
- `GET /api/governance/db-views`

数据源与元数据管理至少要覆盖：

- JDBC / API / Client / Gateway / Proxy 连接方式
- 凭证与安全配置
- 健康状态与最近失败原因
- `freshness / SLA / upstream / downstream / queryability` 快照查询

## 10. Benchmark Contracts

- `POST /api/benchmark-engine/tasks`
- `GET /api/benchmark-engine/tasks/{taskId}`
- `POST /api/benchmark-engine/templates`
- `POST /api/benchmark-engine/test-sets`
- `GET /api/benchmark-engine/reports/{reportId}`

## 11. Open Access Contracts

- `GET /api/governance/access-channels`
- `POST /api/governance/access-policies`
- `GET /api/governance/access-audit`

`JDBC Agent` 策略字段至少包括：

- `agentMode`
- `redisEndpoints`
- `redisNamespace`
- `apiBaseUrl`
- `routeEnabled`
- `rewriteEnabled`
- `lightParseTimeoutMs`
- `fallbackStrategy`
- `historyReportEnabled`

## 12. Alert Contracts

- `GET /api/governance/alerts`
- `GET /api/governance/alerts/{alertId}`
- `POST /api/governance/alerts/{alertId}/ack`
- `POST /api/governance/alerts/policies`

`HARN-143` / `HARN-144` 之后，改写与加速治理相关告警类型至少包括：

- `SQL_REWRITE_RESULT_DIVERGENCE`
- `SQL_REWRITE_VALIDATION_FAILED`
- `ACCELERATION_PLAN_REGRESSED`
- `ACCELERATION_ARTIFACT_INVALIDATED`

告警 payload 至少保留以下可检索或可下钻字段：

- `tenantId`
- `sourceType`
- `sourceKind`
- `sourceId`
- `evidenceLevel`
- `historyId`
- `parseHistoryId`
- `recommendationId`
- `rewriteRecordId`
- `validationRunId`
- `planId`
- `sqlFingerprint`
- `differenceType`
- `sampleEvidence`
- `autoApplyPaused`

边界：

- 差异告警不等于自动回滚生产配置；默认动作是暂停自动应用、记录审计证据并等待人工复核。
- 模拟邮件仍只能标记为 `notify simulated`，不得写成真实通知成功。
- 告警查询必须遵守同租户可见性，不能通过 `historyId`、`recommendationId` 或 `rewriteRecordId` 越权钻取。

## 13. System Management Contracts

- `GET /api/governance/report-interfaces`
- `POST /api/governance/report-interfaces`
- `PUT /api/governance/report-interfaces/{interfaceId}`
- `GET /api/governance/redis-rule-sources`
- `POST /api/governance/redis-rule-sources`
- `PUT /api/governance/redis-rule-sources/{sourceId}`
- `GET /api/governance/dispatch-policies`
- `POST /api/governance/dispatch-policies`

系统管理配置至少要支持：

- 报表接口配置
- Redis 规则源配置
- 装数协同策略配置
- 数据源健康检查入口

## Related Documents

- `docs/product/sql-governance-platform-implementation-spec.md`
- `docs/architecture/service-interface-contract-baseline.md`
- `docs/architecture/sql-governance-data-model-extension.md`
补充规则：

- 跨服务写治理审计时必须透传规范化 `accessChannel`
- 推荐通过 `X-Access-Channel` 头在入口链路显式传递
- 历史兼容调用若未显式声明，则治理侧按 `API` 回填，但未知值必须拒绝

对外 HTTP API 入口基线：

- 受保护入口必须携带 `X-Tenant-Id`
- 受保护入口必须携带 `X-User-Id`
- 受保护入口必须携带 `X-Role-Codes`
- 受保护入口必须携带 `X-Request-Id`
- 受保护入口必须携带 `X-Trace-Id`
- 受保护入口必须携带 `X-Auth-Source`
- 受保护入口建议显式携带 `X-Access-Channel`
- `X-Access-Channel` 支持 `page/api/jdbc/jdbc_agent/sdk/client` 大小写兼容输入，治理侧统一标准化
