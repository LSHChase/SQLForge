# SQLForge Service Interface Contract Baseline

## Summary

本文件把 4 微服务之间在编码前必须先统一的接口级约束显式成表，补齐“统一错误码归属”和“DTO/事件边界”未成体系的问题。

说明：

- 本文件是契约基线，不代表当前代码已全部实现。
- 契约命名采用基线名，后续代码类名可以调整，但语义边界不得漂移。

## 1. Request Context Contract

所有受保护请求都必须形成以下后端上下文：

| Field | Required | Description | Owner |
|:---|:---|:---|:---|
| `tenantId` | Yes | 当前租户标识 | 公共管理服务统一校验，所有服务消费 |
| `userId` | Yes | 当前操作者标识 | 公共管理服务统一校验，所有服务消费 |
| `roleCodes` | Yes | 当前角色集合 | 公共管理服务统一校验，所有服务消费 |
| `requestId` | Yes | 请求级唯一标识 | 调用方生成，链路透传 |
| `traceId` | Yes | 分布式追踪标识 | 网关或入口服务生成，链路透传 |
| `authSource` | Yes | 鉴权来源，如 gateway/token/header | 公共管理服务定义 |
| `issuedAt` | Yes | 凭证签发时间 | 身份来源提供 |
| `expiresAt` | Yes | 凭证失效时间 | 身份来源提供 |

说明：

- 当前 `governance` 对所有受保护接口统一要求以下请求头：
  - `X-Tenant-Id`
  - `X-User-Id`
  - `X-Role-Codes`
  - `X-Request-Id`
  - `X-Trace-Id`
  - `X-Auth-Source`
  - `X-Issued-At`
  - `X-Expires-At`

## 2. Unified Error Code Ownership

遵循 `R-057`，错误码继续分为系统级 `10000-19999` 与业务级 `20000-29999`，并按服务域固定子区间：

| Range | Domain | Owner service | Notes |
|:---|:---|:---|:---|
| `10000-10999` | 公共系统错误 | `sqlforge-shared` | 跨服务共享的参数、上下文、序列化、审计、消息抽象错误 |
| `11000-11999` | 公共管理系统错误 | 公共管理服务 | 身份、租户、数据源、审计、配额、配置中心相关系统错误 |
| `12000-12999` | 查询执行系统错误 | 查询执行服务 | 路由、缓存、轻量解析、执行控制系统错误 |
| `13000-13999` | SQL 优化系统错误 | SQL 优化服务 | 异步解析、建议生成、物化视图系统错误 |
| `14000-14999` | 压测系统错误 | 压测引擎服务 | 压测任务、调度、隔离、报告系统错误 |
| `20000-20999` | 公共管理业务错误 | 公共管理服务 | 权限不足、租户越权、配额不足、数据源授权失败等 |
| `21000-21999` | 查询执行业务错误 | 查询执行服务 | 查询风险拒绝、路由拒绝、结果集超限等 |
| `22000-22999` | SQL 优化业务错误 | SQL 优化服务 | 任务非法、建议不可用、审批前不可应用等 |
| `23000-23999` | 压测业务错误 | 压测引擎服务 | 非影子环境拒绝、只读限制、阈值不满足等 |

规则：

- `sqlforge-shared` 只定义共享错误码，不拥有单服务业务错误。
- 单服务不能占用其他服务的业务区间。
- 跨服务返回统一 `ErrorResponse` 结构，不直接暴露内部堆栈。

## 3. Cross-Service DTO Baseline

| Interaction | Transport | Contract owner | Required DTO / response baseline | Current status |
|:---|:---|:---|:---|:---|
| 查询执行服务 -> 公共管理服务 | HTTP | 公共管理服务 | `GovernanceTenantScopeCheckRequest/Response`, `GovernanceAuthorizationDecisionRequest/Response`, `GovernanceAuditWriteRequest`, `AuditWriteResponse` | Baseline |
| SQL 优化服务 -> 公共管理服务 | HTTP | 公共管理服务 | `GovernanceAuthorizationDecisionRequest/Response`, `GovernanceAuditWriteRequest`, `AuditWriteResponse` | Baseline |
| 压测引擎服务 -> 公共管理服务 | HTTP | 公共管理服务 | `GovernanceAuthorizationDecisionRequest/Response`, `GovernanceAuditWriteRequest`, `AuditWriteResponse` | Baseline |
| 查询执行服务 -> SQL 优化服务 | HTTP / async callback | SQL 优化服务 | `OptimizationTaskSubmitRequest/Response`, `OptimizationTaskStatusResponse` | `ACCELERATION_PLAN_GOVERNANCE_BASELINE` |
| SQL 优化服务 -> 查询执行服务 | HTTP | 查询执行服务 | `QueryExecutionAccelerationPlanApplyRequest`, `QueryExecutionAccelerationPlanVerifyRequest`, `QueryExecutionAccelerationPlanRollbackRequest`, `QueryExecutionAccelerationPlanResponse` | `ACCELERATION_PLAN_GOVERNANCE_BASELINE` |
| 压测引擎服务 -> 查询执行服务 | HTTP | 查询执行服务 | `QueryFingerprintLookupRequest/Response`, `RoutingRuleSnapshotRequest/Response` | Planned |

规则：

- 所有 DTO 均为跨服务契约对象，不得复用内部 entity。
- `sqlforge-shared` 仅承载共享契约基类、通用上下文和错误响应，不承载某一服务专属业务 DTO。
- 若跨服务契约变化具有兼容风险，必须先更新本文件和主计划，再进入实现。
- 当前 `governance` 已提供内部契约入口：
  - `/api/governance/internal/tenant-scope/check`
  - `/api/governance/internal/authorization/decide`
  - `/api/governance/internal/authorization/datasource/change`
  - `/api/governance/internal/audit/write`
  - `/api/governance/internal/acceleration-plan/trace/write`
  - `/api/governance/internal/benchmark/report-trace/write`
  - `/api/governance/internal/alerts/benchmark-regression/emit`
  - `/api/governance/internal/alerts/sql-rewrite-divergence/emit`
  - `/api/governance/internal/schedule/extensions`
- 当前授权决策已由治理服务本地矩阵配置驱动：
  - 角色矩阵把角色映射到权限集合
  - 资源模型把服务操作映射到所需权限与数据源动作
  - 数据源授权矩阵把租户数据源映射到 `ACTIVE/REVOKED` 与动作集合
  - 未命中显式规则时默认拒绝

治理内部契约当前收口如下：

| Endpoint | Contract stage | Current implementation stage | Required baseline | Current notes |
|:---|:---|:---|:---|:---|
| `/api/governance/internal/tenant-scope/check` | `LONG_TERM_BASELINE` | `AUTHORIZATION_MATRIX_BASELINE` | request: `tenantId`,`targetTenantId`; response: `tenantId`,`targetTenantId`,`allowed`,`reason` | 保留租户隔离显式检查能力，供治理和扩展链路单独复用 |
| `/api/governance/internal/authorization/decide` | `LONG_TERM_BASELINE` | `AUTHORIZATION_MATRIX_BASELINE` | request: `serviceCode`,`tenantId`,`resourceType`,`resourceId`,`operationCode`,`datasourceId`; response: `tenantId`,`resourceType`,`resourceId`,`operationCode`,`datasourceId`,`allowed`,`reason`,`errorCode`,`contractStage`,`implementationStage` | 三个业务服务统一复用的授权决策入口；当前执行角色矩阵、资源模型和数据源动作授权 |
| `/api/governance/internal/authorization/datasource/change` | `LONG_TERM_BASELINE` | `AUTHORIZATION_MATRIX_BASELINE` | request: `tenantId`,`datasourceId`,`state`,`actions[]`,`changeReason`; response: `tenantId`,`datasourceId`,`state`,`actions[]`,`status`,`contractStage`,`implementationStage` | 运行态更新数据源授权矩阵，并写入权限变更审计 |
| `/api/governance/internal/audit/write` | `LONG_TERM_BASELINE` | `DATABASE_AUDIT_WRITE_BASELINE` | request: required `serviceCode`,`operationCode`,`resourceType`,`resourceId`,`resultStatus`,`elapsedMs`,`sourceIp`,`userAgent`; optional `sagaId`,`configSnapshotId`,`resultId`,`historyId`,`exportId`,`requestParams`,`responseSummary`; response: `auditId`,`status`,`messageTopic`,`deliveryMode`,`contractStage`,`implementationStage` | 长期保留为跨服务审计写入入口；当前已同步写入 `audit_log`、统一脱敏 `requestParams/responseSummary` 并保留共享消息抽象扩散 |
| `/api/governance/internal/query-execution-history/write` | `LONG_TERM_BASELINE` | `QUERY_EXECUTION_HISTORY_PERSISTENCE_BASELINE` | request: `tenantId`,`sqlText`,`sqlTemplate`,`boundSql`,`sqlFingerprint`,`datasourceCode`,`datasourceType`,`historyType=QUERY_EXECUTION`,`resultStatus`,`targetEngine`,`returnedRowCount`,`cacheHit`,`rewriteApplied`,`rewriteRecordId`,`runtimeBindingId`,`rewriteRuleVersion`,`runtimeRuleVersion`,`runtimeRewriteStatus`,`rewritePublishStatusSnapshot`,`rewriteFallbackReason`,`accelerationApplied`,`accessChannel`,`commentContext`,`queryDateSummary`,`bindingSummary`,`logicalObjectHits`,`routeSummary`,`cacheSummary`,`queryContext`,`traceId`,`requestId`,`sagaId`,`submittedBy`,`startedAt`,`finishedAt`,`elapsedMs`,`errorCode`,`errorMessage`; response: `configSnapshotId`,`resultId`,`historyId`,`auditId`,`traceId`,`requestId`,`sagaId`,`contractStage`,`implementationStage` | query-execution 只提交执行证据；governance 在自身事务边界写入 `execution_result`、`query_history` 与审计引用，并通过受保护持久化服务加密 SQL 与敏感 query context；SQL 历史详情/导出通过后端 `rewriteAudit` 面暴露改写审计链，前端不得由 SQL 文本自行推断 |
| `/api/governance/internal/acceleration-plan/trace/write` | `LONG_TERM_BASELINE` | `ACCELERATION_PLAN_TRACEABILITY_BASELINE` | request: `planId`,`sourceTaskId`,`sqlFingerprint`,`datasourceType`,`sqlText`,`planStatus`,`snapshotPayloadJson`,`resultSummaryJson`,`resultPayloadJson`,`queryContextJson`,`createdAt`,`updatedAt`,`errorCode`,`errorMessage`; response: `configSnapshotId`,`resultId`,`historyId`,`traceId`,`requestId`,`sagaId`,`contractStage`,`implementationStage` | acceleration plan 生命周期通过治理受保护入口落 `config/result/history` 追溯链；当前 `config_snapshot` 与 `query_history` 幂等创建，`execution_result` 随 apply/verify/rollback 状态更新 |
| `/api/governance/internal/benchmark/report-trace/write` | `LONG_TERM_BASELINE` | `DATABASE_TRACE_EXPORT_ORCHESTRATION_BASELINE` | request: `reportId`,`taskId`,`taskType`,`sqlFingerprint`,`resultStatus`,`generatedAt`,`startedAt`,`finishedAt`,`readonlyRequired`,`shadowEnvironmentMode`,`desensitizationRequirement`,`targetEngines[]`,`sqlText`,`workloadDigest`,`workloadSource`,`backfillApplied`,`workloadEvidenceJson`,`executionSummaryJson`,`reportQueryPath`,`rawDataDownloadPath`,`artifacts[].artifactKey/artifactKind/exportFormat/mediaType/fileName/contentLength/checksumSha256/storageType/storageUri/storageEvidence/retentionDays/retentionPolicySource/retentionDeleteAfter`; response: `configSnapshotId`,`resultId`,`historyId`,`traceId`,`requestId`,`sagaId`,`artifacts[].artifactKey`,`artifacts[].exportId`,`artifacts[].exportStatus`,`contractStage`,`implementationStage` | benchmark report/raw-data artifact 通过治理受保护编排落 `config/result/history/export` 追溯链；当前同时持久化 workload/backfill/compensation 结构证据，以及带 primary/recovery provider、recovery order、cleanup scope、provider/external verification + retention 的 artifact storage evidence，导出记录按 `reportId + artifactKey` 幂等生成 |
| `/api/governance/internal/alerts/benchmark-regression/emit` | `LONG_TERM_BASELINE` | `REGRESSION_ALERT_LINKAGE_BASELINE` | request: `tenantId`,`reportId`,`taskId`,`historyId`,`sqlFingerprint`,`verdict`,`thresholdHitCount`,`failedThresholdCount`,`warningThresholdCount`,`summary`,`reportQueryPath`,`rawDataDownloadPath`,`thresholdAssessmentsJson`,`executionSummaryJson`; response: `reportId`,`taskId`,`alertTriggered`,`alertLinkages[].alertId/alertType/alertLevel/alertStatus/notifyStatus/summary/detailPath/linkageMode/notificationLogId`,`contractStage`,`implementationStage` | 为 `REGRESSION_GUARD` 报告生成治理告警 linkage；当前仅在 failed threshold count 大于 0 时发出 `BENCHMARK_REGRESSION_FAILED`，dedupe suppressed 时回链既有 alert，而不会把所有 benchmark 结果默认升级为生产风险判定 |
| `/api/governance/internal/alerts/sql-rewrite-divergence/emit` | `LONG_TERM_BASELINE` | `SQL_REWRITE_DIVERGENCE_ALERT_LINKAGE_BASELINE` | request: `tenantId`,`sourceType`,`sourceKind`,`sourceId`,`evidenceLevel`,`historyId`,`parseHistoryId`,`recommendationId`,`rewriteRecordId`,`validationRunId`,`planId`,`sqlFingerprint`,`comparisonStatus`,`differenceType`,`sampleEvidenceJson`,`autoApplyPaused`,`summary`; response: `rewriteRecordId`,`validationRunId`,`alertTriggered`,`alertLinkages[].alertId/alertType/alertLevel/alertStatus/notifyStatus/summary/detailPath/linkageMode/notificationLogId`,`contractStage`,`implementationStage` | 为只读改写结果比对的 `DIVERGED` 结论生成 `SQL_REWRITE_RESULT_DIVERGENCE` 告警 linkage；`FAILED/EXPIRED` 不在本入口默认扩大为 divergence 告警，dedupe suppressed 时回链既有 alert |
| `/api/governance/internal/tenant-artifact-policy/resolve` | `LONG_TERM_BASELINE` | `TENANT_ARTIFACT_POLICY_BASELINE` | request: `tenantId`,`policyScope`; response: `tenantId`,`retentionDays`,`retentionPolicySource`,`retentionPolicyStatus`,`policyScope`,`contractStage`,`implementationStage` | 为业务服务解析 tenant-specific artifact retention policy；当前 benchmark-engine 使用 `tenant_config.retention_days` 回填 retention/backfill metadata，但不把真实外部对象存储写成仓库默认事实 |
| `/api/governance/internal/schedule/extensions` | `TRANSITIONAL_SKELETON` | `TRANSITIONAL_SKELETON` | response: `extensionPoint`,`ownerService`,`status`,`currentMode`,`contractStage`,`implementationStage` | 当前只暴露治理调度扩展状态骨架，不代表完整调度域模型已固化 |

敏感字段处理补充基线：

- `AuditWriteRequest.requestParams`：允许调用方上传结构化请求摘要，但当前真实写入路径只保留脱敏 JSON，不允许密码 / token / key 明文进入 `audit_log.request_params`
- `AuditWriteRequest.responseSummary`：只允许传入脱敏文本摘要；当前真实写入路径会再次执行敏感模式掩码
- `system_config`：命中密码 / token / key 类键名时，当前治理持久化基线只允许写入 `value_ciphertext/value_mask/encryption_*`，不得把原值留在 `config_value`
- `config_snapshot.snapshotPayload`、`execution_result.resultPayload`、`query_history.queryContext`、`export_record.exportOptions`：当前治理持久化基线对命中的敏感叶子节点执行 AES-256 envelope 加密

## 3.1 Query Execution Public HTTP Baseline

当前 `query-execution` 已固化联机查询 DTO / VO / 错误码契约，并把公共 HTTP 入口接到真实 Hetu 多模式执行链；默认仓库 dev/test profile 仍通过显式配置控制外部 Hetu 接入，但 `HETU` 请求在链路未启用时会按严格路由失败，而不是回落到 `SIMULATED` 冒充成功：

| Endpoint | Request baseline | Response baseline | Current implementation stage |
|:---|:---|:---|:---|
| `/api/query-execution/queries/execute` | `QueryExecuteRequest` with `sqlText`,`tenantId`,`datasourceType`,`queryContext`,`accelerationPreference`,`faultToleranceStrategy` | `QueryExecuteResponse` with `status`,`rows`,`downloadUrl`,`metadata`,`degraded`,`degradeReason`,`retryPath`,`error`,`sqlFingerprint`,`contractStage`,`implementationStage` | `HETU_REAL_INTEGRATION` |

当前 `QueryExecuteRequest` / `QueryExecuteResponse` 约束如下：

- `sqlText`：必填，最大 `10MB`
- `tenantId`：必填
- `datasourceType`：必填，当前使用共享枚举 `HETU` / `HIVE` / `SPARK` / `CLICKHOUSE` / `GAUSSDB` / `AUTO`
- `queryContext.timeoutMs`：若提供则必须大于 `0`
- `queryContext.schemaVersion`：当命中受治理 result-cache policy 时作为一致性 token；缺失时只允许执行但会标记 cache bypass 风险，不允许伪造 cache hit
- `accelerationPreference`：`PREFER_ACCELERATED` / `PREFER_FRESH` / `NONE`
- `faultToleranceStrategy`：`RETRY_THEN_FALLBACK` / `FAIL_FAST` / `FALLBACK_IMMEDIATE`

当前 `QueryExecuteResponse` 契约字段与初始化边界对齐如下：

- `metadata.targetEngine`
- `metadata.actualSql`
- `metadata.elapsedMs`
- `metadata.scannedRows`
- `metadata.cacheHit`
- `metadata.cacheGovernanceStatus`
- `metadata.cacheGovernanceEvidence`
- `metadata.accelerationApplied`
- `metadata.executionMode`
- `metadata.attemptedModes[]`
- `metadata.rowCount`
- `metadata.routeProfile`
- `metadata.routeOrder[]`
- `metadata.routeEvidenceSource`
- `metadata.routeVerificationStatus`
- `retryPath[].engine`
- `retryPath[].elapsedMs`
- `retryPath[].resultStatus`
- `retryPath[].localRecoveryMarker`
- `retryPath[].localRecoveryAction`
- `error.code`
- `error.message`
- `error.suggestedAction`
- `error.retryable`

当前固定的查询执行错误码首轮落点：

- `12000` `QUERY_EXECUTION_SYSTEM_ROUTE_UNAVAILABLE`
- `12001` `QUERY_EXECUTION_SYSTEM_ENGINE_TIMEOUT`
- `12002` `QUERY_EXECUTION_SYSTEM_PARSER_FAILURE`
- `12003` `QUERY_EXECUTION_SYSTEM_PIPELINE_NOT_READY`
- `21000` `QUERY_EXECUTION_RISK_REJECTED`
- `21001` `QUERY_EXECUTION_ROUTE_REJECTED`
- `21002` `QUERY_EXECUTION_RESULT_LIMIT_EXCEEDED`

说明：

- 当前实现已提供真实 Hetu 多模式执行链：只读单语句 SQL 守卫、`AUTO/HETU -> HETU` 的主路由、`HETU -> HIVE` 的受控 fallback，以及 `JDBC` / `REST` / `CLIENT` 三种 Hetu 接入模式的顺序选择、route calibration 元数据与 attempted-modes 结果收口。
- 当 `query-execution.hetu.enabled=false` 或 Hetu 模式链全部失败时，`HETU` 请求会返回结构化 `QUERY_EXECUTION_SYSTEM_ROUTE_UNAVAILABLE`，或在 `RETRY_THEN_FALLBACK` 下受控降级到 `HIVE`；确定性 `SIMULATED` 结果只保留给非 Hetu 路由与测试桩。
- 当前 `attemptedModes[]` 会显式区分 `CHAIN_DISABLED`、`CHAIN_UNCONFIGURED`、`<MODE>:SKIPPED_*` 与 `<MODE>:FAILED_*`，用于保留 mode priority、ready/unready 判定和失败分层证据。
- 当前实现已补齐入口/出口/异常/状态变更日志，并在 timeout/fallback 路径上输出本地回滚/补偿标记：
  - timeout: `LOCAL_TIMEOUT_ROLLBACK_MARKED` + `CLOSE_PRIMARY_ATTEMPT_CONTEXT`
  - fallback: `LOCAL_FALLBACK_COMPENSATION_MARKED` + `RECORD_DEGRADED_RESULT`
- 当前实现已具备治理检查与审计写入的跨服务 HTTP 基线、Hetu JDBC driver 接线、Hetu client 协议执行、本地 mock-Hetu runtime smoke，以及受保护 `GET /api/query-execution/internal/hetu/route-calibration` 快照与外部环境 `bash scripts/run-hetu-env-smoke.sh` 结构化证据入口；真实 Win10 环境窗口下的 live smoke 日志归档仍由 `HARN-016` / `INBOX-002` 继续跟踪。
- 当前实现已补齐受治理 result-cache runtime：只有内部 cache policy 明确 apply 后，才会基于 `tenantId + sqlFingerprint + datasourceType + queryContext.schemaVersion` 尝试命中；命中、旁路、失效、回填和风险原因会进入 `metadata.cacheGovernanceStatus/cacheGovernanceEvidence` 与审计 `responseSummary`。

## 3.1.1 Query Execution Internal Hetu Route Calibration Baseline

当前 `query-execution` 已新增受保护内部只读契约，供运维/测试环境在不改写 repo-closed 默认事实的前提下读取当前 Hetu route calibration、cluster evidence 与 ready/unready 分层快照：

| Endpoint | Request baseline | Response baseline | Current implementation stage |
|:---|:---|:---|:---|
| `/api/query-execution/internal/hetu/route-calibration` | `GET`, no request body, protected headers required | `HetuRouteCalibrationResponse` with `hetuEnabled`,`routeProfile`,`declaredAllowedModes[]`,`effectiveRouteOrder[]`,`skipUnreadyModes`,`evidenceSource`,`liveVerificationStatus`,`readonlyBoundary`,`summary`,`clusterEvidence`,`modeCalibrations[]`,`contractStage`,`implementationStage`; `clusterEvidence` carries `evidenceSource`,`environmentLabel`,`clusterName`,`coordinatorEndpoint`,`runbookRef`,`evidenceRef`,`readonlyBoundary`,`liveVerificationStatus`,`operatorNotes`; `modeCalibrations[]` carries `mode`,`priority`,`allowed`,`calibrationPreferred`,`adapterAvailable`,`configured`,`ready`,`willAttemptInCurrentPolicy`,`readinessStatus`,`readinessReason`,`routeParameters` | `HETU_ROUTE_CALIBRATION_BASELINE` |

说明：

- internal snapshot 仍通过 header-based protected request context 受控访问，不绕过统一授权入口或只读/影子环境边界。
- `effectiveRouteOrder[]` 先遵循 `query-execution.hetu.calibration.route-order`，再回补 `allowed-modes` 中未显式排序的模式；calibration 只能重排已允许模式，不能借此启用未允许模式。
- `skipUnreadyModes=true` 时，未 ready 的模式不会被实际调用，而会在 `attemptedModes[]` 中落成 `SKIPPED_*` 证据；`clusterEvidence.liveVerificationStatus` 默认仍是 `PENDING_ENV_WINDOW`，只表示仓库侧校准状态，不把外部 Win10/Hetu live 结果写成当前仓库默认事实。

## 3.1.2 Query Execution Internal Result Digest Baseline

当前 `query-execution` 已新增受保护内部只读摘要契约，供 `sql-optimization` 在改写验证中执行原 SQL / 推荐 SQL 的摘要化结果比对，而不把大结果集跨服务传递或返回前端：

| Endpoint | Request baseline | Response baseline | Current implementation stage |
|:---|:---|:---|:---|
| `/api/query-execution/internal/result-digests/execute` | `QueryExecutionResultDigestRequest` with `tenantId`,`validationRunId`,`rewriteRecordId`,`sqlFingerprint`,`sqlText`,`datasourceType`,`datasourceCode`,`comparisonPolicy`; protected headers required | `QueryExecutionResultDigestResponse` with `tenantId`,`validationRunId`,`rewriteRecordId`,`sqlFingerprint`,`status`,`targetEngine`,`resultDigest`,`limitedSample`,`executionEvidence`,`errorCode`,`errorMessage`,`contractStage`,`implementationStage`; `resultDigest` carries schema digest, row count/sample row count, order digest, key-set digest and checksum digest; response never returns the full result set | `READONLY_RESULT_DIGEST_BASELINE` |

说明：

- 内部入口复用同步查询路径的只读 SQL guard、租户上下文、路由、失败 JSON 与治理历史写入；非只读 SQL、多语句或未授权访问不得进入摘要比对。
- `limitedSample` 仅用于差异证据下钻，受 `comparisonPolicy.sampleLimit` 上限控制；完整 rows 不进入接口响应、`rewrite_validation_run` 或前端展示。
- `resultDigest` 是后端比较输入，不是第二套验证真值；最终验证状态仍以 `sql-optimization.rewrite_validation_run` 为准。

## 3.1.3 Query Execution Internal Benchmark Workload Baseline

当前 `query-execution` 已新增受保护内部契约，供 `benchmark-engine` 在 worker 场景下抓取 workload/backfill evidence，而不绕过既有查询执行与治理审计边界：

| Endpoint | Request baseline | Response baseline | Current implementation stage |
|:---|:---|:---|:---|
| `/api/query-execution/internal/benchmark/workload/capture` | `QueryExecutionBenchmarkWorkloadRequest` with `tenantId`,`benchmarkTaskId`,`benchmarkTaskType`,`sqlText`,`sqlFingerprint`,`targetEngines[]`,`concurrency`,`durationSeconds`,`rampUpSeconds`,`datasetSizeLabel`,`readonlyRequired` | `QueryExecutionBenchmarkWorkloadResponse` with `tenantId`,`benchmarkTaskId`,`sqlFingerprint`,`workloadDigest`,`workloadSource`,`backfillApplied`,`compensationApplied`,`compensationStrategy`,`engineSnapshots[]`,`contractStage`,`implementationStage`; `engineSnapshots[]` carries `targetEngine`,`resultStatus`,`workloadSource`,`backfillSource`,`backfillReason`,`executionMode`,`attemptedModes[]`,`elapsedMs`,`scannedRows`,`rowCount`,`cacheHit`,`cacheGovernanceStatus`,`cacheGovernanceEvidence`,`accelerationApplied`,`workloadDigest`,`evidence`,`compensationApplied`,`compensationStrategy`,`compensationSourceEngine`,`compensationSourceWorkloadDigest` | `BENCHMARK_WORKLOAD_ORCHESTRATION_BASELINE` |

说明：

- 内部入口仍通过 header-based protected request context 受控访问；benchmark worker 在无前台请求上下文时，允许以 service identity 合成受保护请求头继续执行 repo-side 编排。
- 当前实现会逐个目标引擎复用 `QueryExecutionApplicationService.executeSynchronously`；成功时返回 live workload snapshot，失败时返回显式 `SYNTHETIC_BACKFILL` 证据；若同批次至少有一个 live snapshot，则会把失败快照进一步收口为 `COMPENSATED_REPLAY`，显式保留 source engine / source workload digest / strategy，而不是把失败静默折叠为 benchmark 本地无来源的 synthetic replay。live workload evidence 现在也会带出 `routeProfile` / `routeOrder` 片段，便于和 Hetu route calibration 快照做交叉审计。
- live workload snapshot 也会带出 cache governance status/evidence，benchmark execution summary 中的 `queryExecution[...]` note 与 governance `workloadEvidenceJson` 会继续保留这些字段，便于治理历史从 workload 侧还原 cache hit / bypass / invalidation / backfill 证据。
- 当前内部入口会额外写入 `QUERY_BENCHMARK_WORKLOAD_CAPTURE` 审计摘要，用于记录 workloadSource / backfillApplied / compensationApplied / compensationStrategy / workloadDigest 的跨服务编排结果。

## 3.1.3 Query Execution Internal Acceleration Plan Runtime Baseline

当前 `query-execution` 已新增受保护内部契约，供 `sql-optimization` 在审批通过后把 acceleration plan 收口到 runtime gating 闭环，而不是让 `PREFER_ACCELERATED` 默认 fail-open：

| Endpoint | Request baseline | Response baseline | Current implementation stage |
|:---|:---|:---|:---|
| `/api/query-execution/internal/acceleration-plans/apply` | `QueryExecutionAccelerationPlanApplyRequest` with `tenantId`,`planId`,`sqlFingerprint`,`datasourceType`,`selectedSuggestionTypes[]`,`planSummary`,`primaryRecommendation` | `QueryExecutionAccelerationPlanResponse` with `tenantId`,`planId`,`sqlFingerprint`,`targetEngine`,`active`,`status`,`runtimeSummary`,`runtimeDetailsJson`,`contractStage`,`implementationStage` | `APPROVED_ACCELERATION_RUNTIME_BASELINE` |
| `/api/query-execution/internal/acceleration-plans/verify` | `QueryExecutionAccelerationPlanVerifyRequest` with `tenantId`,`planId`,`sqlFingerprint` | `QueryExecutionAccelerationPlanResponse` | `APPROVED_ACCELERATION_RUNTIME_BASELINE` |
| `/api/query-execution/internal/acceleration-plans/rollback` | `QueryExecutionAccelerationPlanRollbackRequest` with `tenantId`,`planId`,`sqlFingerprint` | `QueryExecutionAccelerationPlanResponse` | `APPROVED_ACCELERATION_RUNTIME_BASELINE` |

说明：

- 当前 internal runtime surface 仍通过 header-based protected request context 受控访问，保持统一授权入口与 tenant 隔离不变。
- 当前实现把 approved binding 收口为 `tenantId + sqlFingerprint + datasourceType` 维度的内存注册表，并对同一 fingerprint 的并发 plan 激活做冲突拒绝。
- 当前 `QueryExecutionApplicationService` 不再因为调用方声明 `PREFER_ACCELERATED` 就直接标记 `accelerationApplied=true`；只有存在 approved binding 时才允许 runtime gating 命中。
- 当前该闭环只解决“受治理激活/验证/回滚”和 no-fail-open runtime gating，不代表物理物化视图、provider-native cache 或引擎侧加速已自动编排完成。

## 3.1.4 Query Execution Internal Cache Governance Runtime Baseline

当前 `query-execution` 已新增受保护内部契约，供治理或已审批流程把 result-cache policy 从元数据占位收口到可审计 runtime 行为：

| Endpoint | Request baseline | Response baseline | Current implementation stage |
|:---|:---|:---|:---|
| `/api/query-execution/internal/cache-policies/apply` | `QueryExecutionCachePolicyApplyRequest` with `tenantId`,`policyId`,`sqlFingerprint`,`datasourceType`,`schemaVersion`,`sourcePlanId`,`policyReason`,`maxEntries`,`ttlSeconds` | `QueryExecutionCachePolicyResponse` with `tenantId`,`policyId`,`sqlFingerprint`,`targetEngine`,`schemaVersion`,`sourcePlanId`,`active`,`status`,`policySummary`,`runtimeDetailsJson`,`contractStage`,`implementationStage`; `runtimeDetailsJson` carries `cacheBackendType/cacheBackendProvider/cacheBackendCarrier/cacheBackendDistributed/cacheBackendEnvironment/providerEvidence`, policy/tenant capacity counters, TTL, eviction summary and backend health evidence | `CACHE_GOVERNANCE_RUNTIME_BASELINE` |
| `/api/query-execution/internal/cache-policies/verify` | `QueryExecutionCachePolicyVerifyRequest` with `tenantId`,`policyId`,`sqlFingerprint`,`datasourceType`,`schemaVersion` | `QueryExecutionCachePolicyResponse` | `CACHE_GOVERNANCE_RUNTIME_BASELINE` |
| `/api/query-execution/internal/cache-policies/invalidate` | `QueryExecutionCachePolicyInvalidateRequest` with `tenantId`,`policyId`,`sqlFingerprint`,`datasourceType`,`schemaVersion`,`invalidateReason` | `QueryExecutionCachePolicyResponse` | `CACHE_GOVERNANCE_RUNTIME_BASELINE` |

说明：

- cache policy runtime surface 仍通过 header-based protected request context 受控访问，默认无 policy 时返回 `UNGOVERNED`，不把普通执行结果伪装成受治理缓存。
- result-cache key 采用 `tenantId + datasourceType + sqlFingerprint + schemaVersion`；`schemaVersion` 来自 `queryContext.schemaVersion`，对齐 ADR-011 的 Hudi timestamp / schema consistency token 口径。
- `schemaVersion` 缺失会标记 `BYPASSED / SCHEMA_VERSION_MISSING`；session variable `sqlforge.cache.bypass=true` 会标记 `BYPASSED / SESSION_VARIABLE_BYPASS`；版本变更会先失效旧 entry，再以新版本回填，并在 evidence 中保留 `SCHEMA_VERSION_MISMATCH` 与 invalidated count。
- 当前 cache policy 已支持 per-policy `maxEntries` 与 `ttlSeconds`，并由 runtime 默认值收口 per-tenant capacity 上限；超过 policy/tenant 容量时按最旧访问 entry 驱逐，过期 entry 在读取/计数/回填前失效。
- cache governance evidence 固定保留 `TTL_EXPIRED`、`CAPACITY_EVICTED`、`MANUAL_INVALIDATED`、`SCHEMA_VERSION_MISMATCH` 四类 eviction reason，以及 `evictedEntryCount`、`maxEntriesPerPolicy`、`maxEntriesPerTenant`、`policyCachedEntryCount`、`tenantCachedEntryCount`、`ttlSeconds` 等容量证据。
- 当前 runtime 已抽象为 provider-neutral backend contract。默认 backend 仍是 repo-closed `IN_MEMORY / LOCAL_PROCESS`；只有显式设置 `query-execution.cache.backend.type=REDIS` 或 `PROVIDER_NATIVE_REDIS` 且提供 Redis endpoint 时，才走低层 RESP provider adapter。
- distributed backend 不可用或写入失败时会 fail-closed 为 `BYPASSED`，并在 `cacheGovernanceEvidence` 中保留 `DISTRIBUTED_BACKEND_UNAVAILABLE` 或 `DISTRIBUTED_BACKEND_WRITE_FAILED`、backend descriptor 与 provider command evidence；不允许 provider 故障时伪造 cache hit。
- policy verify 会返回 capacity remaining、tenant/policy cached count、eviction summary 与 backend descriptor/provider verify evidence；查询执行指标新增低基数 `sqlforge.query.execution.cache.governance`，按 `target_engine/status/event/risk_code/eviction_reason` 统计 hit、miss、bypass、backfill、invalidate 与 backend-unavailable，不把 tenant/policy/cache key 写进指标标签。
- 该能力代表 query-execution 已具备 provider-native distributed cache backend baseline 与可审计读写/失效/验证证据，以及 repo-side capacity/eviction/metrics governance baseline；不代表仓库默认启用 Redis/provider cache，也不代表真实 Redis 集群长跑、跨节点恢复演练或引擎侧缓存已投产。真实 Redis 长跑和恢复演练仍是 environment-backed follow-up。

## 3.2 SQL Optimization Task Contract Baseline

当前 `sql-optimization` 已将异步优化任务契约接到公共 HTTP 入口，并通过 MySQL 持久化任务表与 scheduled worker 提供可测的提交、轮询、真实 parse/rewrite/acceleration suggestion 与失败路径。

| Endpoint | Request baseline | Response baseline | Current implementation stage |
|:---|:---|:---|:---|
| `POST /api/sql-optimization/tasks` | `OptimizationTaskSubmitRequest` with `tenantId`,`taskType`,`sqlText/sqlFingerprint`,`datasourceType`,`taskContext` | `OptimizationTaskSubmitResponse` with `taskId`,`status`,`currentPhase`,`estimatedReadyAt`,`statusQueryPath`,`contractStage`,`implementationStage` | `ACCELERATION_PLAN_GOVERNANCE_BASELINE` |
| `GET /api/sql-optimization/tasks/{taskId}` | path: `taskId` | `OptimizationTaskStatusResponse` with `taskId`,`taskType`,`status`,`currentPhase`,`priority`,`progressPercent`,`requestedSuggestionTypes`,`suggestion`,`failure`,`submittedAt`,`startedAt`,`finishedAt`,`statusHistory`,`contractStage`,`implementationStage` | `ACCELERATION_PLAN_GOVERNANCE_BASELINE` |

当前 `OptimizationTaskSubmitRequest` 基线字段如下：

- `tenantId`：必填
- `taskType`：必填，当前固定为 `PARSE` / `REWRITE` / `ACCELERATION_SUGGESTION`
- `sqlText` / `sqlFingerprint`：二选一至少提供一个
- `datasourceType`：必填，当前沿用共享枚举
- `taskContext.parseDepth`：`LIGHT` / `DEEP`，默认 `DEEP`
- `taskContext.priority`：`HIGH` / `NORMAL` / `LOW`，默认 `NORMAL`
- `taskContext.callbackUrl`：可选
- `taskContext.requestedSuggestionTypes`：仅 `ACCELERATION_SUGGESTION` 任务允许传入；为空时按 `ALL` 归一
- `taskContext.sourceType` / `sourceId` / `batchId` / `reportCode` / `historyId` / `parseTaskId` / `datasourceCode` / `issueScenes`：解析命中问题场景后自动提交 `REWRITE` 任务时写入的来源上下文；普通外部提交可不传，旧请求保持兼容

当前 `OptimizationTaskSubmitResponse` / `OptimizationTaskStatusResponse` 基线字段如下：

- `taskId`
- `status`
- `currentPhase`
- `estimatedReadyAt`
- `statusQueryPath`
- `progressPercent`
- `requestedSuggestionTypes`
- `suggestion.summary`
- `suggestion.primaryRecommendation`
- `suggestion.confidenceScore`
- `suggestion.artifacts[].category`
- `suggestion.artifacts[].name`
- `suggestion.artifacts[].content`
- `suggestion.benefits[].category`
- `suggestion.benefits[].estimatedImprovementPercent`
- `suggestion.benefits[].summary`
- `suggestion.costs[].category`
- `suggestion.costs[].level`
- `suggestion.costs[].summary`
- `suggestion.risks[].level`
- `suggestion.risks[].category`
- `suggestion.risks[].summary`
- `suggestion.risks[].mitigation`
- `failure.code`
- `failure.message`
- `failure.suggestedAction`
- `failure.retryable`
- `failure.failedPhase`
- `failure.risks[].category`
- `failure.risks[].summary`
- `failure.risks[].mitigation`
- `submittedAt`
- `startedAt`
- `finishedAt`
- `statusHistory[].previousStatus`
- `statusHistory[].currentStatus`
- `statusHistory[].previousPhase`
- `statusHistory[].currentPhase`
- `statusHistory[].occurredAt`
- `statusHistory[].note`
- `contractStage`
- `implementationStage`

当前异步优化任务的公共状态与阶段基线如下：

- 生命周期状态：`QUEUED` / `RUNNING` / `SUCCEEDED` / `FAILED` / `CANCELLED`
- 通用起止阶段：`SUBMITTED` -> `FINISHED`
- `PARSE`：`SUBMITTED` -> `DEEP_PARSING` -> `RESULT_ASSEMBLING` -> `FINISHED`
- `REWRITE`：`SUBMITTED` -> `DEEP_PARSING` -> `SQL_REWRITING` -> `RESULT_ASSEMBLING` -> `FINISHED`
- `ACCELERATION_SUGGESTION`：`SUBMITTED` -> `DEEP_PARSING` -> `COST_ESTIMATING` -> `ACCELERATION_PLANNING` -> `RESULT_ASSEMBLING` -> `FINISHED`

当前固定的 SQL 优化错误码首轮落点：

- `13000` `SQL_OPTIMIZATION_SYSTEM_PIPELINE_NOT_READY`
- `13001` `SQL_OPTIMIZATION_SYSTEM_STATE_TRANSITION_INVALID`
- `13002` `SQL_OPTIMIZATION_SYSTEM_CALLBACK_CONTRACT_INVALID`
- `13003` `SQL_OPTIMIZATION_SYSTEM_PARSER_FAILURE`
- `13004` `SQL_OPTIMIZATION_SYSTEM_REWRITE_FAILURE`
- `13005` `SQL_OPTIMIZATION_SYSTEM_ACCELERATION_PLANNING_FAILURE`
- `13006` `SQL_OPTIMIZATION_SYSTEM_ACCELERATION_PLAN_TRACE_FAILURE`
- `13007` `SQL_OPTIMIZATION_SYSTEM_ACCELERATION_PLAN_APPLY_FAILURE`
- `13008` `SQL_OPTIMIZATION_SYSTEM_ACCELERATION_PLAN_VERIFY_FAILURE`
- `13009` `SQL_OPTIMIZATION_SYSTEM_ACCELERATION_PLAN_ROLLBACK_FAILURE`
- `22000` `SQL_OPTIMIZATION_TASK_INVALID`
- `22001` `SQL_OPTIMIZATION_TASK_NOT_FOUND`
- `22002` `SQL_OPTIMIZATION_TASK_ALREADY_FINISHED`
- `22003` `SQL_OPTIMIZATION_SUGGESTION_NOT_READY`
- `22004` `SQL_OPTIMIZATION_ACCELERATION_PLAN_INVALID`
- `22005` `SQL_OPTIMIZATION_ACCELERATION_PLAN_NOT_FOUND`
- `22006` `SQL_OPTIMIZATION_ACCELERATION_PLAN_STATE_INVALID`

说明：

- 当前 `POST /api/sql-optimization/tasks` 会先返回 `QUEUED / SUBMITTED` 快照，再由 MySQL carrier + scheduled worker 异步推进真实 parse/rewrite/acceleration suggestion 链。
- 当前 `GET /api/sql-optimization/tasks/{taskId}` 已可查询 MySQL repository 中的最新任务状态；未知任务返回 `22001`。
- 当前模型已显式区分“外部生命周期状态”和“内部处理阶段”，避免把 parse / rewrite / acceleration suggestion 三类任务混成单一线性状态。
- 当前成功结果统一输出到 `suggestion`，并按三类任务给出结构化 `artifacts / benefits / costs / risks`：
  - `PARSE`：偏向 AST profile、表/谓词/聚合信号、热点风险
  - `REWRITE`：偏向候选 SQL、规则轨迹、计划简化收益与语义校验风险
  - `ACCELERATION_SUGGESTION`：偏向 acceleration plan、shape signal、延迟/扫描收益、存储与治理成本
- 当前失败结果统一输出到 `failure`，保留错误码、重试语义、失败阶段与风险说明。
- 当前 placeholder 失败路径通过 SQL 或指纹中的显式 `FAIL_OPTIMIZATION` 标记触发，用于稳定验证轮询失败场景。
- 当前真实结果会以 `summary` + `suggestion_payload_json` 落仓，失败链路会额外沉淀 `failed_phase` 与 `error_risks_json`；acceleration suggestion 仍保持“给出建议”边界，而后续 apply/verify/rollback 需要经 governed acceleration plan 对象显式审批后才能进入 runtime。

## 3.2.1 SQL Optimization Parse History Baseline

当前 `sql-optimization` 拥有独立 SQL 解析记录面。结构解析、综合解析、批量解析、报表解析和日终慢 SQL 解析只写 `sql_parse_history`，不再通过治理侧 `query_history` 存放 `SQL_PARSE` 语义记录。`query_history` 保持 SQL 执行历史边界。

| Endpoint | Request baseline | Response baseline | Current implementation stage |
|:---|:---|:---|:---|
| `GET /api/sql-optimization/parse-history` | query: `tenantId`,`sourceType`,`reportCode`,`datasourceCode`,`stage`,`bizDate`,`queryDateStart`,`queryDateEnd`,`status`,`logicalObjectType`,`accessChannel`,`engine`,`submittedBy`,`traceId`,`parseTaskId`,`submittedStart`,`submittedEnd`,`sortBy`,`sortOrder`,`pageNo`,`pageSize` | `SqlParseHistoryPageVO` with `items[]`,`pageNo`,`pageSize`,`totalCount`,`pageCount`,`hasMore`,`classificationSummary` | `SQL_PARSE_HISTORY_DECOUPLED_BASELINE` |
| `GET /api/sql-optimization/parse-history/{parseHistoryId}` | path: `parseHistoryId`, optional `tenantId` | `SqlParseHistoryDetailVO` with summary fields plus `sqlText`,`sqlTemplateText`,`sqlState`,`structureParseSummary`,`accessParseSummary`,`executionSummary`,`queryContext`,`commentContext`,`bindingSummary`,`logicalObjectHits`,`issueScenes`,`logicalObjectKeys`,`recommendationRefs`,`benchmarkRefs`,`alertRefs`,`auditRefs` | `SQL_PARSE_HISTORY_DECOUPLED_BASELINE` |
| `POST /api/sql-optimization/parse-history/export` | query: optional `tenantId`; body: `parseHistoryId` or legacy alias `historyId`, `exportFormat` | `SqlParseHistoryExportVO` with `exportId`,`parseHistoryId`,`historyId`,`exportFormat`,`exportStatus`,`fileName`,`contentType`,`storageType`,`storageUri`,`exportedAt`,`auditReference`,`payload` | `SQL_PARSE_HISTORY_DECOUPLED_BASELINE` |

当前解析记录边界如下：

- 主键使用 `parseHistoryId`，兼容前端历史字段时可回填同值 `historyId`，但语义仍是解析记录 ID。
- `historyType` 固定返回 `SQL_PARSE_RECORD`，不得再在治理 `query_history` 中使用 `historyType=SQL_PARSE` 做逻辑隔离。
- `sourceType` 固定覆盖 `STRUCTURE_PARSE`、`COMBINED_PARSE`、`PARSE_BATCH`、`REPORT_BATCH`、`END_OF_DAY_SLOW_SQL`。
- 关键结构字段包括 `parseTaskId`、`sqlFingerprint`、`datasourceCode`、`reportCode`、`stageCode`、`bizDate`、`queryDate*`、`accessChannel`、`parserMode`、`bindingMode`、`resultStatus`、`targetEngine`、`traceId`、`requestId`、`sagaId`、`submittedBy`、`submittedAt`。
- 解析证据字段包括 `structureParseSummary`、`accessParseSummary`、`resultSummary/resultPayload`、`queryContext`、`commentContext`、`bindingSummary`、`logicalObjectHits`、`issueScenes` 与 `logicalObjectKeys`；若 SQL 命中底层 DB View，`logicalObjectHits` 保留 View hit 并追加实时 View definition 展开的叶子 `TABLE` hit，`logicalObjectKeys` 固化最终唯一 `TABLE:*` keys。
- 导出当前只承诺 `JSON` / `CSV` inline response；不写治理侧 `export_record`，后续如需跨服务取证归档必须另行扩展契约。

日终慢 SQL 解析当前只固化服务骨架：`SlowSqlExecutionHistorySource` 只读拉取执行历史候选，`EndOfDaySlowSqlParseApplicationService` 按时间窗口、慢 SQL 阈值、limit 与幂等 `batchKey + sqlFingerprint` 生成解析记录。本轮不固化真实 cron 调度和执行历史 source 实现。

## 3.2.2 SQL Optimization Acceleration Plan Governance Baseline

当前 `sql-optimization` 已把 acceleration plan 从 suggestion artifact 收口为受治理正式对象，并通过独立 MySQL carrier、governance traceability 与 query-execution runtime gating 形成 submit/approve/apply/verify/rollback 闭环：

| Endpoint | Request baseline | Response baseline | Current implementation stage |
|:---|:---|:---|:---|
| `POST /api/sql-optimization/acceleration-plans` | `AccelerationPlanSubmitRequest` with `tenantId`,`sourceTaskId`,`selectedSuggestionTypes[]` | `AccelerationPlanSubmitResponse` with `planId`,`status`,`statusQueryPath`,`contractStage`,`implementationStage` | `ACCELERATION_PLAN_GOVERNANCE_BASELINE` |
| `GET /api/sql-optimization/acceleration-plans/{planId}` | path: `planId` | `AccelerationPlanStatusResponse` with `planId`,`sourceTaskId`,`status`,`datasourceType`,`sqlFingerprint`,`selectedSuggestionTypes`,`planSummary`,`primaryRecommendation`,`planPayloadJson`,`benefits`,`costs`,`risks`,`reviewNote`,`approvedBy`,`approvedAt`,`rejectedBy`,`rejectedAt`,`lastErrorCode`,`lastErrorMessage`,`runtimeBindingJson`,`runtimeBindingAt`,`runtimeBindingBy`,`verificationEvidenceJson`,`verifiedAt`,`verifiedBy`,`rollbackEvidenceJson`,`rolledBackAt`,`rolledBackBy`,`configSnapshotId`,`resultId`,`historyId`,`createdAt`,`updatedAt`,`statusHistory`,`contractStage`,`implementationStage` | `ACCELERATION_PLAN_GOVERNANCE_BASELINE` |
| `POST /api/sql-optimization/acceleration-plans/{planId}/approval` | `AccelerationPlanApprovalRequest` with `approve`,`reviewNote` | `AccelerationPlanStatusResponse` | `ACCELERATION_PLAN_GOVERNANCE_BASELINE` |
| `POST /api/sql-optimization/acceleration-plans/{planId}/apply` | optional `AccelerationPlanActionRequest` with `reason` | `AccelerationPlanStatusResponse` | `ACCELERATION_PLAN_GOVERNANCE_BASELINE` |
| `POST /api/sql-optimization/acceleration-plans/{planId}/verify` | optional `AccelerationPlanActionRequest` with `reason` | `AccelerationPlanStatusResponse` | `ACCELERATION_PLAN_GOVERNANCE_BASELINE` |
| `POST /api/sql-optimization/acceleration-plans/{planId}/rollback` | optional `AccelerationPlanActionRequest` with `reason` | `AccelerationPlanStatusResponse` | `ACCELERATION_PLAN_GOVERNANCE_BASELINE` |

当前 acceleration plan 生命周期与状态基线如下：

- `PENDING_APPROVAL`
- `APPROVED`
- `REJECTED`
- `APPLY_FAILED`
- `APPLIED`
- `VERIFY_FAILED`
- `VERIFIED`
- `ROLLBACK_FAILED`
- `ROLLED_BACK`

说明：

- acceleration plan 只能从 `SUCCEEDED` 的 `ACCELERATION_SUGGESTION` 任务派生；若请求了原建议中不存在的 suggestion type，返回 `22004`。
- `apply` / `verify` / `rollback` 都要求 request tenant 与 authenticated tenant 一致，且先经过治理授权，再做 plan 状态校验；非法状态返回 `22006`，不会先去调用 runtime。
- 当前治理 trace 会为每个 acceleration plan 生成稳定的 `configSnapshotId/resultId/historyId/sagaId`，并在后续 lifecycle 变更时更新 `execution_result`，保持长期追溯链一致。
- 当前 runtime 闭环仍聚焦“已批准计划的激活/验证/回滚证据”和 `PREFER_ACCELERATED` gating；它不等价于自动创建物化视图、provider-native cache 或引擎侧真实加速对象。

## 3.2.3 Production SQL Rewrite Auto-Apply Contract

本节固化 PRW-001 的生产自动改写闭环目标契约，不代表当前代码已全部实现。该闭环只覆盖“正常解析或执行历史产生推荐 SQL，经人类审批和等价验证后发布到运行时，后续同租户、同 SQL 指纹执行时自动替换为已批准 SQL”的生产路径；投产前本地或测试环境核验闭环不纳入本契约。

生产 SQL 改写闭环不得复用 acceleration plan 的 `approval/apply/verify/rollback` 页面或接口作为改写审批入口。acceleration plan 仍负责物理加速或 runtime gating 计划治理；SQL 文本改写必须由改写推荐或改写记录自己的 review / publish / runtime binding 状态机证明。

状态维度必须保持独立：

| Dimension | Owner | Baseline values | Contract meaning |
|:---|:---|:---|:---|
| `manualReviewRequired` | `sql-optimization` recommendation / rewrite record | `true`, `false` | 风险或复核提示，只说明是否需要人工看过；不得自动代表审批通过。 |
| `reviewStatus` | `sql-optimization` rewrite record | `PENDING_REVIEW`, `APPROVED`, `REJECTED`, `CHANGES_REQUESTED` | 人类审批状态。只有 `APPROVED` 才允许进入发布资格判断。 |
| `publishStatus` | `sql-optimization` rewrite record | `UNPUBLISHED`, `PUBLISHING`, `PUBLISHED`, `PAUSED`, `UNPUBLISHING`, `UNPUBLISH_FAILED`, `PUBLISH_FAILED` | 改写记录发布状态。审批通过不等于运行时已生效，只有发布成功并绑定 active 才能触发自动改写。 |
| runtime binding status | `query-execution` runtime rewrite binding | `ACTIVE`, `PAUSED`, `UNPUBLISHED` | 运行时生效状态。`ACTIVE` 是后续 SQL 执行可以自动替换 SQL 文本的唯一状态。 |

目标服务协作契约如下，后续 PRW-002 至 PRW-009 实现时可调整具体 Java 类名，但不得改变语义边界：

| Surface | Contract owner | Required behavior |
|:---|:---|:---|
| `GET /api/sql-optimization/rewrite-records/{rewriteRecordId}` | `sql-optimization` | 返回 `manualReviewRequired`, `reviewStatus`, `publishStatus`, `runtimeBindingId`, `runtimeRuleVersion`, `validationStatus`, source evidence 和审计 trace refs。 |
| `POST /api/sql-optimization/rewrite-records/{rewriteRecordId}/review` | `sql-optimization` | 接收审批结论与意见，写入审批人、时间和审计 trace；非法状态迁移由后端拒绝。 |
| `GET /api/sql-optimization/rewrite-records/{rewriteRecordId}/publish-eligibility` | `sql-optimization` | 返回集中式发布资格判断与结构化拒绝原因；前端只能展示，不得自行实现核心门禁。 |
| `POST /api/sql-optimization/rewrite-records/{rewriteRecordId}/publish` | `sql-optimization` | 在审批通过、等价验证通过、租户/指纹/来源证据完整且无未关闭差异暂停时，调用 `query-execution` 创建或更新运行时改写绑定。 |
| `POST /api/sql-optimization/rewrite-records/{rewriteRecordId}/pause` | `sql-optimization` | 暂停已发布改写记录并同步暂停对应 runtime binding，保留告警、原因和 trace。 |
| `POST /api/sql-optimization/rewrite-records/{rewriteRecordId}/unpublish` | `sql-optimization` | 撤销运行时绑定并回写发布状态，不删除历史改写记录。 |
| `POST /api/query-execution/internal/rewrite-bindings/publish` | `query-execution` | 创建生产 runtime rewrite binding，返回 `runtimeBindingId` 与 `runtimeRuleVersion`；同租户同 SQL 指纹最多只能存在一个 `ACTIVE` binding。 |
| `POST /api/query-execution/internal/rewrite-bindings/resolve-active` | `query-execution` | 以 tenant + SQL fingerprint 查询 `ACTIVE` runtime rewrite binding；可用 datasource evidence 收窄匹配。 |
| `POST /api/query-execution/internal/rewrite-bindings/pause` | `query-execution` | 将 runtime rewrite binding 置为 `PAUSED`，保留原因、操作人、时间和版本追踪。 |
| `POST /api/query-execution/internal/rewrite-bindings/unpublish` | `query-execution` | 将 runtime rewrite binding 置为 `UNPUBLISHED`，保留历史绑定与规则版本，不物理删除。 |

运行时执行契约如下：

- `query-execution` 的 `runtime_rewrite_binding` 持久化表是生产自动改写运行时绑定真值；JDBC Agent / Redis 只能作为后续兼容出口，不能替代主闭环。
- 只有同租户、同 SQL 指纹且 runtime binding status 为 `ACTIVE` 时，查询执行入口才能把原 SQL 替换为已批准推荐 SQL。
- 执行历史必须记录原始 SQL、实际执行 SQL、是否改写、改写记录 ID、runtime binding ID、规则版本和发布状态快照，前端不得自行推断 `rewriteApplied`。
- 周期比对发现结果不等价或超过容忍阈值时，必须暂停或撤销 runtime binding 并更新 `publishStatus`，不能只写告警展示。

## 3.3 Benchmark Engine Task Contract Baseline

当前 `benchmark-engine` 已将压测任务、测试集导入与报告查询契约接到公共 HTTP + MySQL 基线，并通过 `benchmark_task` / `benchmark_task_report` / `benchmark_test_set` / `benchmark_test_set_case`、MyBatis XML repository、in-process scheduled worker、repo-closed 隔离执行服务和持久化导出产物提供可测的提交、轮询、测试集导入、报告查询与失败路径；当前任务队列 carrier 支持默认 `database-worker` 与显式启用的 `external-file-queue`，并把 carrier evidence 暴露到任务响应与审计载荷。

| Endpoint | Request baseline | Response baseline | Current implementation stage |
|:---|:---|:---|:---|
| `POST /api/benchmark-engine/tasks` | `BenchmarkTaskSubmitRequest` with `tenantId`,`taskType`,`sqlText/sqlFingerprint`,`taskContext` | `BenchmarkTaskSubmitResponse` with `taskId`,`status`,`currentPhase`,`estimatedReadyAt`,`statusQueryPath`,`queueMode`,`queueEvidence`,`contractStage`,`implementationStage` | `EXTERNALIZED_ARTIFACT_GOVERNANCE_TRACE_BASELINE` |
| `GET /api/benchmark-engine/tasks/{taskId}` | path: `taskId` | `BenchmarkTaskStatusResponse` with `taskId`,`taskType`,`status`,`currentPhase`,`priority`,`progressPercent`,`targetEngines`,`templateId`,`templateType`,`templateVersion`,`testSetId`,`testSetSource`,`testSetLabels[]`,`testSetSourceRefs[]`,`readonlyRequired`,`shadowEnvironmentMode`,`desensitizationRequirement`,`thresholdCount`,`reportId`,`error`,`submittedAt`,`startedAt`,`finishedAt`,`queueMode`,`queueEvidence`,`contractStage`,`implementationStage` | `EXTERNALIZED_ARTIFACT_GOVERNANCE_TRACE_BASELINE` |
| `POST /api/benchmark-engine/tasks/recommendations/{recommendationId}/comparison` | path: `recommendationId`; `BenchmarkRecommendationComparisonCreateRequest` with `tenantId`,`benchmarkSqlRole`,`targetEngines[]`, optional `testSetName`,`templateId`,`templateVersion`,`concurrency`,`durationSeconds`,`rampUpSeconds`,`datasetSizeLabel`,`thresholds[]`,`testSetLabels[]` | `BenchmarkRecommendationComparisonResponse` with `recommendationId`,`benchmarkSqlRole`,`testSet`,`benchmarkTask`,`contractStage`,`implementationStage` | `RECOMMENDATION_COMPARISON_ORCHESTRATION_BASELINE` |
| `POST /api/benchmark-engine/test-sets` | `BenchmarkTestSetCreateRequest` with `tenantId`,`testSetName`,`templateId`,`templateType`,`templateVersion`,`testSetSource`,`testSetLabels[]`,`testSetSourceRefs[]`,`importRequest` | `BenchmarkTestSetResponse` with `testSetId`,`tenantId`,`testSetName`,`templateId`,`templateType`,`templateVersion`,`testSetSource`,`status`,`totalCases`,`acceptedCases`,`rejectedCases`,`fileType`,`fileName`,`importBatchId`,`fieldMappings[]`,`testSetLabels[]`,`testSetSourceRefs[]`,`cases[]`,`createdAt`,`updatedAt`,`contractStage`,`implementationStage` | `BATCH_IMPORT_BASELINE` |
| `POST /api/benchmark-engine/test-sets/parse-results` | `BenchmarkParseResultTestSetCreateRequest` with `tenantId`,`testSetName`,`templateId`,`templateType`,`templateVersion`, exactly one of `parseTaskId` or `parseBatchId`, optional `sqlText`,`includeIssueScenes[]`,`includeReportCodes[]`,`testSetLabels[]`,`testSetSourceRefs[]` | `BenchmarkTestSetResponse` with `testSetId`,`tenantId`,`testSetName`,`templateId`,`templateType`,`templateVersion`,`testSetSource`,`status`,`totalCases`,`acceptedCases`,`rejectedCases`,`testSetLabels[]`,`testSetSourceRefs[]`,`cases[]`,`createdAt`,`updatedAt`,`contractStage`,`implementationStage` | `PARSE_RESULT_GENERATION_BASELINE` |
| `GET /api/benchmark-engine/test-sets/{testSetId}` | path: `testSetId` | `BenchmarkTestSetResponse` | `BATCH_IMPORT_BASELINE` |
| `GET /api/benchmark-engine/reports/{reportId}` | path: `reportId`, query: `format=JSON|PDF|HTML` (default `JSON`) | JSON: `BenchmarkReportResponse`; PDF/HTML: externalized persisted export snapshot with stable `Content-Type` and `Content-Disposition` | `EXTERNALIZED_ARTIFACT_GOVERNANCE_TRACE_BASELINE` |
| `GET /api/benchmark-engine/reports/{reportId}/raw-data` | path: `reportId` | attachment download backed by persisted raw-data snapshot with stable `Content-Type` and `Content-Disposition` | `EXTERNAL_QUEUE_PROVIDER_NATIVE_STORAGE_BASELINE` |

当前模型基线涉及以下契约对象：

- `BenchmarkTaskSubmitRequest`
- `BenchmarkTaskSubmitResponse`
- `BenchmarkTaskStatusResponse`
- `BenchmarkTestSetCreateRequest`
- `BenchmarkTestSetResponse`
- `BenchmarkReportResponse`

当前 `BenchmarkTaskSubmitRequest` 基线字段如下：

- `tenantId`：必填
- `taskType`：必填，当前固定为 `BASELINE` / `COMPARISON` / `REGRESSION_GUARD`
- `sqlText` / `sqlFingerprint`：至少应有一个可追踪 SQL 标识
- `taskContext.priority`：`HIGH` / `NORMAL` / `LOW`，默认 `NORMAL`
- `taskContext.targetEngines`：目标引擎列表，默认 `HETU`
- `taskContext.concurrency`：并发度，默认 `8`
- `taskContext.durationSeconds`：压测持续时长，默认 `300`
- `taskContext.rampUpSeconds`：预热时长，默认 `30`
- `taskContext.datasetSizeLabel`：数据规模标签，默认 `UNSPECIFIED`
- `taskContext.templateId`：可选模板标识；当前 repo-side baseline 用它固化前端 catalog / SDK / future import flow 的稳定主键
- `taskContext.templateType`：`BASELINE_SNAPSHOT` / `CROSS_ENGINE_COMPARISON` / `REGRESSION_GUARD`，并与 `taskType` 一一对应
- `taskContext.templateVersion`：模板契约版本，当前默认 `v1`
- `taskContext.testSetId`：可选测试集标识
- `taskContext.testSetSource`：`MANUAL_CURATION` / `BATCH_IMPORT` / `PARSE_RESULT_GENERATION` / `RECOMMENDATION_GENERATION`
- `taskContext.testSetLabels[]`：测试集标签模型，元素字段为 `type`、`value`；当前 `type` 支持 `SCENARIO` / `DOMAIN` / `SOURCE` / `RISK`
- `taskContext.testSetSourceRefs[]`：测试集来源引用，元素字段为 `type`、`referenceId`；当前 `type` 支持 `IMPORT_BATCH` / `PARSE_TASK` / `QUERY_HISTORY` / `REPORT` / `RECOMMENDATION` / `SQL_FINGERPRINT`
- `taskContext.readonlyRequired`：默认 `true`
- `taskContext.shadowEnvironmentMode`：`REQUIRED` / `PREFERRED` / `DISABLED`，默认 `REQUIRED`
- `taskContext.desensitizationRequirement`：`REQUIRED` / `OPTIONAL`，默认 `REQUIRED`
- `taskContext.thresholds[]`：阈值列表，元素字段为 `metric`、`operator`、`targetValue`、`severity`、`description`

当前 `BenchmarkTaskSubmitResponse` 基线字段如下：

- `taskId`
- `status`
- `currentPhase`
- `estimatedReadyAt`
- `statusQueryPath`
- `contractStage`
- `implementationStage`

当前 `BenchmarkTaskStatusResponse` 基线字段如下：

- `taskId`
- `taskType`
- `status`
- `currentPhase`
- `priority`
- `progressPercent`
- `targetEngines`
- `templateId`
- `templateType`
- `templateVersion`
- `testSetId`
- `testSetSource`
- `testSetLabels[].type`
- `testSetLabels[].value`
- `testSetSourceRefs[].type`
- `testSetSourceRefs[].referenceId`
- `readonlyRequired`
- `shadowEnvironmentMode`
- `desensitizationRequirement`
- `thresholdCount`
- `reportId`
- `error.code`
- `error.message`
- `error.suggestedAction`
- `error.retryable`
- `submittedAt`
- `startedAt`
- `finishedAt`
- `contractStage`
- `implementationStage`

当前 `BenchmarkTestSetCreateRequest` / `BenchmarkTestSetResponse` 基线字段如下：

- `tenantId`：必填
- `testSetName`：必填
- `templateId` / `templateType` / `templateVersion`：可选；当前用于把导入测试集与 template contract 显式关联
- `testSetSource`：当前只开放 `BATCH_IMPORT`
- `testSetLabels[]`：标签模型，沿用 `type` / `value`
- `testSetSourceRefs[]`：额外来源引用；当前导入链路会补充 `IMPORT_BATCH`
- `importRequest.fileType`：当前支持 `CSV` / `TXT` / `XLSX` / `XLS`
- `importRequest.fileName`
- `importRequest.contentBase64`
- `importRequest.charset`：可选，默认 `UTF-8`
- `importRequest.delimiter`：可选，`TXT` 默认 `TAB`，其余默认 `,`
- `importRequest.fieldMappings[].field`：当前支持 `CASE_NAME` / `SQL_TEXT` / `SQL_FINGERPRINT` / `DATASOURCE_CODE` / `REPORT_CODE` / `TAGS` / `BIND_PARAMETERS_JSON`
- `importRequest.fieldMappings[].columnName`
- `response.status`：`READY` / `PARTIAL_READY` / `FAILED`
- `response.totalCases` / `acceptedCases` / `rejectedCases`
- `response.importBatchId`
- `response.fieldMappings[]`
- `response.testSetLabels[]`
- `response.testSetSourceRefs[]`
- `response.cases[].caseId`
- `response.cases[].sequenceNumber`
- `response.cases[].sourceLineNumber`
- `response.cases[].caseName`
- `response.cases[].sqlText`
- `response.cases[].sqlFingerprint`
- `response.cases[].datasourceCode`
- `response.cases[].reportCode`
- `response.cases[].tags[]`
- `response.cases[].bindParametersJson`
- `response.cases[].status`
- `response.cases[].rejectionReason`
- `response.cases[].rawCaseDataJson`
- `response.createdAt`
- `response.updatedAt`
- `response.contractStage`
- `response.implementationStage`

当前 `BenchmarkReportResponse` 基线字段如下：

- `reportId`
- `taskId`
- `taskType`
- `sqlFingerprint`
- `verdict`
- `generatedAt`
- `targetEngines[]`
- `engineResults[].engine`
- `engineResults[].targetQps`
- `engineResults[].actualQps`
- `engineResults[].p50LatencyMs`
- `engineResults[].p95LatencyMs`
- `engineResults[].p99LatencyMs`
- `engineResults[].cpuUsagePercent`
- `engineResults[].memoryUsageMb`
- `engineResults[].scannedDataBytes`
- `engineResults[].verdict`
- `engineResults[].notes`
- `thresholdAssessments[].metric`
- `thresholdAssessments[].verdict`
- `thresholdAssessments[].actualValue`
- `thresholdAssessments[].targetValue`
- `thresholdAssessments[].summary`
- `regressionSummary.thresholdHitCount`
- `regressionSummary.failedThresholdCount`
- `regressionSummary.warningThresholdCount`
- `regressionSummary.alertRequired`
- `regressionSummary.summary`
- `alertLinkages[].alertId`
- `alertLinkages[].alertType`
- `alertLinkages[].alertLevel`
- `alertLinkages[].alertStatus`
- `alertLinkages[].notifyStatus`
- `alertLinkages[].summary`
- `alertLinkages[].detailPath`
- `alertLinkages[].linkageMode`
- `alertLinkages[].notificationLogId`
- `trendCharts[].chartType`
- `trendCharts[].title`
- `trendCharts[].xAxisLabel`
- `trendCharts[].yAxisLabel`
- `trendCharts[].series[].seriesName`
- `trendCharts[].series[].points[].label`
- `trendCharts[].series[].points[].value`
- `recommendations[].category`
- `recommendations[].title`
- `recommendations[].summary`
- `recommendations[].expectedBenefit`
- `recommendations[].riskLevel`
- `requestedFormat`
- `availableFormats[]`
- `reportQueryPath`
- `rawDataDownloadPath`
- `contractStage`
- `implementationStage`

当前压测任务的公共状态与阶段基线如下：

- 生命周期状态：`QUEUED` / `RUNNING` / `SUCCEEDED` / `FAILED` / `CANCELLED`
- 通用起止阶段：`SUBMITTED` -> `FINISHED`
- `BASELINE`：`SUBMITTED` -> `BASELINE_PREPARING` -> `WARMING_UP` -> `EXECUTING` -> `THRESHOLD_EVALUATING` -> `REPORTING` -> `FINISHED`
- `COMPARISON`：`SUBMITTED` -> `BASELINE_PREPARING` -> `SHADOW_VALIDATING` -> `WARMING_UP` -> `EXECUTING` -> `THRESHOLD_EVALUATING` -> `REPORTING` -> `FINISHED`
- `REGRESSION_GUARD`：`SUBMITTED` -> `SHADOW_VALIDATING` -> `EXECUTING` -> `THRESHOLD_EVALUATING` -> `REPORTING` -> `FINISHED`

当前固定的压测引擎错误码首轮落点：

- `14000` `BENCHMARK_ENGINE_SYSTEM_PIPELINE_NOT_READY`
- `14001` `BENCHMARK_ENGINE_SYSTEM_STATE_TRANSITION_INVALID`
- `14002` `BENCHMARK_ENGINE_SYSTEM_REPORT_MODEL_INVALID`
- `23000` `BENCHMARK_TASK_INVALID`
- `23001` `BENCHMARK_TASK_NOT_FOUND`
- `23002` `BENCHMARK_REPORT_NOT_FOUND`
- `23003` `BENCHMARK_ISOLATION_POLICY_REJECTED`

说明：

- 当前 `POST /api/benchmark-engine/tasks` 会先返回 `QUEUED / SUBMITTED` 快照，再由 `BenchmarkTaskWorker` 基于 `benchmark_task` 表推进到成功或失败，并在成功路径上先向 `query-execution` 内部受保护入口抓取 workload/backfill snapshot，再执行 repo-closed 隔离 replay、组装报告快照、生成导出产物、externalize 到 artifact storage、调用治理 trace/export orchestration，最后把 artifact metadata 回写到 `benchmark_task_report`；当 `benchmark-engine.queues.mode=external-file-queue` 时，提交流程会先把最小 envelope 写入外部文件队列，并把 `queueMessagePath` 等 evidence 回写到任务状态历史与审计面。
- 当前 `GET /api/benchmark-engine/tasks/{taskId}` 已可查询最新任务状态，并返回 `queueMode/queueEvidence` 以标记当前任务使用的 queue carrier；未知任务返回 `23001`。
- 当前 `POST /api/benchmark-engine/tasks/recommendations/{recommendationId}/comparison` 会显式读取 sql-optimization recommendation，校验 source/recommended SQL 均仍处于 benchmark 只读边界后，先生成 `RECOMMENDATION_GENERATION` test set（保留 source/recommended 双 case、recommendation/report/history/parse/sqlFingerprint 来源引用和风险标签），再提交 `COMPARISON` benchmark task；该路径不会自动触发 recommendation 执行，也不会放行越界 SQL 进入 benchmark task。
- 当前 `POST /api/benchmark-engine/test-sets` 已可接收 base64 文件、表头字段映射和标签元数据，把通过只读 SQL 边界校验的行导入为 `ACCEPTED` case，把越界 SQL / 非法 JSON / 缺失必填字段的行保留为 `REJECTED` evidence，并自动补充 `IMPORT_BATCH` 来源引用；导入结果按 `READY` / `PARTIAL_READY` / `FAILED` 收口到 `benchmark_test_set` / `benchmark_test_set_case`。
- 当前 `POST /api/benchmark-engine/test-sets/parse-results` 已可从单个 `parseTaskId` 或 batch 级 `important/urgent` 解析结果一键生成 benchmark test set；batch 路径默认只吸纳 `GET /api/sql-optimization/parse-statistics/important-urgent` 命中的项，可再按 `includeIssueScenes[]` / `includeReportCodes[]` 收窄，且会对解析结果中的 SQL 重新执行 benchmark 只读边界校验，把不可直接压测的条目保留为 `REJECTED` evidence，而不是静默放行。
- 当前 `GET /api/benchmark-engine/test-sets/{testSetId}` 可返回测试集元数据、成员列表和 rejected-row evidence；未知测试集当前沿用 `23001`。
- 当前 `GET /api/benchmark-engine/reports/{reportId}` 默认返回结构化 JSON；当 `format=PDF|HTML` 时返回 externalized 持久化导出产物内容，并保留稳定的 content-type / filename 契约。
- 当前 `GET /api/benchmark-engine/reports/{reportId}/raw-data` 返回 attachment download，并从 externalized raw-data snapshot 直接读取响应体。
- 当前失败路径通过 SQL 或指纹中的显式 `FAIL_BENCHMARK` 标记触发，用于稳定验证 worker 失败与轮询失败场景。
- 当前 `readonlyRequired=false` 或 `shadowEnvironmentMode=DISABLED` 会被当前骨架拒绝，并返回 `23003`，以保持 `ADR-007` 的隔离约束不被绕过。
- 当前模型已经把 `ADR-007` 要求的只读标记、影子环境标记和脱敏要求显式入模，避免后续执行链路绕过安全基线。
- 当前报告模型已覆盖引擎指标快照、阈值判定、回归守护摘要、治理告警 linkage、趋势图表、建议输出、执行摘要以及导出产物元数据；成功路径会生成 `JSON/PDF/HTML` 与 raw-data artifact，记录 `artifactKey/artifactKind/storageType/storageUri/storageEvidence/exportId/retentionDays/retentionPolicySource/retentionDeleteAfter`，并把 workload/backfill/compensation 结构证据、provider-specific / multi-provider object-storage contract、cleanup/recovery order 与 object-storage provider/external verification 证据同步写入 governance trace payload，供后续查询直接复用。
- 当前 `REGRESSION_GUARD` 在治理 trace 完成后，会把 failed threshold count 汇总为 `regressionSummary`，并通过治理内部 `alerts/benchmark-regression/emit` 入口生成或回链 `BENCHMARK_REGRESSION_FAILED`；warning-only 场景保留在报告里，但不会默认扩大到治理告警面。
- 当前报告查询/下载审计会在 artifact 已具备治理追溯元数据时补齐 `configSnapshotId/resultId/historyId/exportId`；其中 `JSON` 查询绑定 `json-export`，`PDF/HTML` 查询绑定对应导出 artifact，`raw-data` 下载绑定 `raw-data` artifact。
- 当前 repo-local artifact lifecycle 已固化为保留当前 report-set、重写时清理陈旧 sibling 文件，以及在 `PDF/HTML/raw-data` 文件缺失时从持久化报告快照恢复后再继续返回响应。
- 当前仓库已补齐 repo-closed 隔离执行、benchmark/query-execution workload/backfill/compensation orchestration、artifact externalization、查询 audit-link enrichment、tenant-specific retention/backfill policy、外部文件队列 carrier，以及治理 trace/export orchestration 链路；`LOCAL_FILE` 仍是默认主路径，而 `ENVIRONMENT_OBJECT_STORAGE` 只在显式配置时启用，并通过 repo-local mirror + object URI + live-evidence manifest 保留环境级对象存储接线证据，在提供 primary/recovery provider endpoint 时还会执行真实 provider-backed write/readback/head verification，并把实际 recovery source/read status 与 provider-native live evidence 作为治理查询面的显式字段暴露。
- 当前 tenant-specific artifact policy 通过 `governance tenant_config.retention_days` 解析；若历史 artifact 缺少该元数据，则会在后续查询/恢复时回填 retention evidence，而不是把旧 artifact 直接写成永久缺省无策略。
- 当前 governance history summaries/lookups/detail 已把 `compensationReplayEvidence`、`artifactStorageContract`、`artifactRecoverySurface`、`artifactOperationSurface` 作为显式结构字段返回，其中 `artifactStorageContract` 至少覆盖 `storageType/providerMode/primaryProvider/recoveryProvider/recoveryOrder/cleanupScope/retention*`，`artifactRecoverySurface` 至少覆盖 `artifactRecoveryStatus/storageRecoverySource/storageReadStatus/provider*/externalWrite*`，而 `artifactOperationSurface` 至少覆盖 `operationType/operationStatus/cleanupScope/storageRecoverySource/storageReadStatus/providerHeadStatus/providerRequestId/orchestrationType/batchId/batchIndex/batchSize/errorCode/errorMessage`。
- 当前新增 `POST /api/governance/history/artifact-operations` 与 `POST /api/governance/history/artifact-operations/batch` 两个公共受保护入口，并与 `POST /api/benchmark-engine/internal/artifact-operations` 内部受保护入口组成 governance-triggered cleanup/recovery operation contract；当前只开放 tenant/platform admin 触发，batch retention 默认 cleanup scope 为 `MIRROR_LIVE_EVIDENCE_EXTERNAL_WRITE_PROVIDER`，并通过 governance 侧编排复用 benchmark-engine 单条 artifact operation 路由，不把 provider-backed object storage 写成仓库默认主路径。
- 当前环境级 artifact cleanup scope 已显式收口为 `MIRROR_ONLY`、`MIRROR_LIVE_EVIDENCE`、`MIRROR_LIVE_EVIDENCE_EXTERNAL_WRITE`、`MIRROR_LIVE_EVIDENCE_EXTERNAL_WRITE_PROVIDER`；其中 provider cleanup/delete 属于受治理的 provider-authenticated operation，要求显式配置 credentials，且会把 primary/recovery provider delete status 连同 batch/orchestration 元数据一并写入治理审计面。
- 当前 `Phase-D` 剩余关注点已收窄为真正的缓存治理能力，以及更广的 environment-backed 执行证据、跨服务 workload compensation/recovery 结果向长期治理追溯面的进一步沉淀。

## 4. Event Contract Baseline

| Event | Producer | Consumer | Payload minimum fields | Purpose |
|:---|:---|:---|:---|:---|
| `ConfigChangedEvent` | 公共管理服务 | 查询执行 / SQL 优化 / 压测引擎 | `tenantId`,`eventId`,`traceId`,`configType`,`resourceId`,`changedAt` | 配置下发与缓存失效 |
| `QuotaChangedEvent` | 公共管理服务 | 查询执行 / 压测引擎 | `tenantId`,`eventId`,`traceId`,`quotaType`,`effectiveAt` | 配额变更同步 |
| `OptimizationSuggestionReadyEvent` | SQL 优化服务 | 查询执行 / 公共管理服务 | `tenantId`,`taskId`,`traceId`,`suggestionId`,`status`,`generatedAt` | 优化建议结果异步通知 |
| `AccelerationPlanApprovedEvent` | 公共管理服务 | 查询执行 / SQL 优化服务 | `tenantId`,`planId`,`traceId`,`approvalStatus`,`approvedAt` | 审批结果下发 |
| `BenchmarkReportReadyEvent` | 压测引擎服务 | 公共管理服务 | `tenantId`,`taskId`,`traceId`,`reportId`,`resultStatus`,`finishedAt` | 压测报告归档与审计 |

规则：

- 事件契约必须可审计、可追踪、可重放。
- 事件名和字段语义由生产者拥有，但不得绕过本基线定义最低字段。
- 若运行环境暂不启用消息中间件，必须保留等价的数据库队列或 mock 契约语义。
- 当前代码已通过 `governance.audit.event` 发布审计事件契约；`ConfigChangedEvent` 等其他治理事件仍处于基线定义阶段。

## 5. Audit Contract Baseline

跨服务统一审计事件最少字段：

| Field | Required |
|:---|:---|
| `occurredAt` | Yes |
| `tenantId` | Yes |
| `userId` | Yes |
| `serviceCode` | Yes |
| `operationCode` | Yes |
| `resourceType` | Yes |
| `resourceId` | Yes |
| `resultStatus` | Yes |
| `elapsedMs` | Yes |
| `traceId` | Yes |
| `requestId` | Yes |
| `sourceIp` | Yes |
| `userAgent` | Yes |

说明：

- 当前治理内部 `audit/write` 契约中：
  - `tenantId`,`userId`,`traceId`,`requestId` 由受保护请求上下文提供
  - `occurredAt` 由治理服务落审计事件时生成
  - `serviceCode`,`operationCode`,`resourceType`,`resourceId`,`resultStatus`,`elapsedMs`,`sourceIp`,`userAgent` 由调用方显式提供
  - `configSnapshotId`,`resultId`,`historyId`,`exportId` 为可选追溯键；若提供，治理服务必须校验对应记录已存在
  - `requestParams`,`responseSummary` 为可选脱敏审计摘要；不得包含明文凭据或原始敏感 SQL
- 当前实现状态：
  - `audit/write` 已同步写入 `audit_log`
  - 同一请求会保留 `governance.audit.event` 消息扩散语义，供后续异步消费或外部归档复用
  - 当前 `governance` 在 header-based stateless auth 基线下，已把每次受保护请求的鉴权建立/释放审计为 `LOGIN` / `LOGOUT` 类型事件
- 当前 `audit/write` 失败错误码已固定：
  - `10005` `SYSTEM_CONTEXT_MISSING`
  - `10008` `SYSTEM_MESSAGE_MODE_INVALID`
  - `10009` `SYSTEM_AUDIT_CONTRACT_INVALID`
  - `11002` `GOVERNANCE_SYSTEM_MESSAGE_ROUTE_INVALID`

## 6. Authorization Decision Contract Baseline

当前治理内部 `authorization/decide` 契约返回规则：

- `allowed=true` 时必须返回 `reason`，并显式返回 `contractStage` 与 `implementationStage`
- `allowed=false` 时必须返回显式 `errorCode`
- 当前失败错误码已固定：
  - `20000` `GOVERNANCE_ACCESS_DENIED`
  - `20001` `GOVERNANCE_TENANT_ACCESS_DENIED`
  - `20002` `GOVERNANCE_DATASOURCE_ACCESS_DENIED`
- 当前 `authorization/datasource/change` 契约必须返回 `status=UPDATED`，并把权限变更写入 `audit_log`

## 7. Related Documents

- `docs/security/access-control-spec.md`
- `docs/architecture/service-capability-map.md`
- `docs/plans/implementation-readiness.md`
- `docs/plans/task-governance-extension-matrix.md`
