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
- `objectName`
- `matchSource`
- `resolved`
- `mappedPhysicalTargets`

`objectType` 枚举：

- `BUSINESS_VIEW`
- `DB_VIEW`
- `TABLE`

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

### 4.3 Combined Parse Status

- `POST /api/sql-optimization/parse/combined`
- `GET /api/sql-optimization/parse/{parseTaskId}`

`POST /parse/combined` 必须表达：

- 先执行结构解析
- 若连接可用则自动触发 access parse
- 返回统一 `parseTaskId` 与初始结构解析结果

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

### 5.2 Parse Batch Detail

- `GET /api/sql-optimization/parse-batches/{batchId}`

返回：

- 批次摘要
- 导入记录
- 结构解析统计
- 数据访问解析统计
- 问题统计
- 报表统计
- 失败记录

### 5.3 Retry Access Parse for Batch

- `POST /api/sql-optimization/parse-batches/{batchId}/retry-access`

字段：

- `failureFilter`
- `datasourceCode`
- `forceRecheckAvailability`

### 5.4 Report Batch Import

- `POST /api/sql-optimization/report-batches/import`

字段：

- `batchName`
- `fileType`
- `reportCodeField`
- `datasourceCode`
- `stage`
- `priority`

### 5.5 Resolve Report SQLs

- `POST /api/sql-optimization/report-batches/{batchId}/resolve-sqls`

一期：

- 从 txt/mock source 读取

后续：

- 由 `governance` 配置接口信息
- `sql-optimization` 调用

## 6. Parse Statistics Contracts

- `GET /api/sql-optimization/parse-statistics/overview`
- `GET /api/sql-optimization/parse-statistics/by-issue-scene`
- `GET /api/sql-optimization/parse-statistics/by-sql`
- `GET /api/sql-optimization/parse-statistics/by-report`
- `GET /api/sql-optimization/parse-statistics/priority-matrix`

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
