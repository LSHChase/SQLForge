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
- `structureParseSummary`
- `accessParseSummary`
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

## 6. Parse Statistics Contracts

- `GET /api/sql-optimization/parse-statistics/overview`
- `GET /api/sql-optimization/parse-statistics/by-issue-scene`
- `GET /api/sql-optimization/parse-statistics/by-sql`
- `GET /api/sql-optimization/parse-statistics/by-report`
- `GET /api/sql-optimization/parse-statistics/priority-matrix`

当前 repo-side 基线：

- `overview`: `totalSqlCount`,`issueSqlCount`,`totalIssueCount`,`issueSceneCount`,`importantSqlCount`,`urgentSqlCount`,`priorityDistribution`
- `by-issue-scene`: `issueScene`,`issueDomain`,`severity`,`priorityLevel`,`priorityScore`,`affectedSqlCount`,`affectedIssueCount`,`sqlRatio`,`important`,`urgent`
- `by-sql`: `itemId`,`batchId`,`parseTaskId`,`reportCode`,`datasourceCode`,`stage`,`sqlDigest`,`issueCount`,`highestPriorityLevel`,`highestPriorityScore`,`important`,`urgent`,`issueScenes`
- `by-report`: `reportCode`,`sqlCount`,`issueSqlCount`,`issueCount`,`issueSqlRatio`,`highestPriorityLevel`,`highestPriorityScore`,`important`,`urgent`,`issueScenes`
- 当前按 `RequestContext.tenantId` 过滤批量解析记录，避免跨租户聚合
- `priority-matrix` 在后续任务扩展

## 7. Recommendation and Dispatch Contracts

- `GET /api/sql-optimization/recommendations`
- `GET /api/sql-optimization/recommendations/{recommendationId}`
- `POST /api/sql-optimization/recommendations/{recommendationId}/dispatch`

治理事件载荷至少包括：

- `recommendationId`
- `dispatchType`
- `sqlText`
- `targetEngine`
- `targetDatasource`
- `relatedReportCode`
- `relatedLogicalObject`
- `expectedEffect`

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
