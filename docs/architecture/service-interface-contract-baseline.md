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
| 查询执行服务 -> 公共管理服务 | HTTP | 公共管理服务 | `TenantScopeCheckRequest/Response`, `DatasourceAccessCheckRequest/Response`, `QuotaCheckRequest/Response`, `AuditWriteRequest/Response` | Partial |
| SQL 优化服务 -> 公共管理服务 | HTTP | 公共管理服务 | `OptimizationApprovalCheckRequest/Response`, `MetadataLookupRequest/Response`, `AuditWriteRequest/Response` | Partial |
| 压测引擎服务 -> 公共管理服务 | HTTP | 公共管理服务 | `BenchmarkAuthorizationRequest/Response`, `ShadowEnvironmentCheckRequest/Response`, `AuditWriteRequest/Response` | Partial |
| 查询执行服务 -> SQL 优化服务 | HTTP / async callback | SQL 优化服务 | `OptimizationTaskSubmitRequest/Response`, `OptimizationTaskStatusResponse`, `AccelerationPlanApplyRequest/Response` | `ASYNC_TASK_API_SKELETON` |
| 压测引擎服务 -> 查询执行服务 | HTTP | 查询执行服务 | `QueryFingerprintLookupRequest/Response`, `RoutingRuleSnapshotRequest/Response` | Planned |

规则：

- 所有 DTO 均为跨服务契约对象，不得复用内部 entity。
- `sqlforge-shared` 仅承载共享契约基类、通用上下文和错误响应，不承载某一服务专属业务 DTO。
- 若跨服务契约变化具有兼容风险，必须先更新本文件和主计划，再进入实现。
- 当前 `governance` 已提供首轮内部契约入口：
  - `/api/governance/internal/tenant-scope/check`
  - `/api/governance/internal/datasource-access/check`
  - `/api/governance/internal/audit/write`
  - `/api/governance/internal/schedule/extensions`
- 当前 `datasource-access` 占位实现已改为治理服务本地显式配置驱动：
  - 治理内置数据源按角色白名单放行
  - 其他数据源按租户绑定表放行
  - 未命中显式规则时默认拒绝

治理内部契约当前收口如下：

| Endpoint | Contract stage | Current implementation stage | Required baseline | Current notes |
|:---|:---|:---|:---|:---|
| `/api/governance/internal/datasource-access/check` | `LONG_TERM_BASELINE` | `TRANSITIONAL_SKELETON` | request: `tenantId`,`datasourceId`; response: `allowed`,`reason`,`errorCode`,`contractStage`,`implementationStage` | 长期保留为跨服务授权检查入口；当前决策仍由治理本地 placeholder 规则驱动 |
| `/api/governance/internal/audit/write` | `LONG_TERM_BASELINE` | `TRANSITIONAL_SKELETON` | request: `serviceCode`,`operationCode`,`resourceType`,`resourceId`,`resultStatus`,`elapsedMs`,`sourceIp`,`userAgent`; response: `status`,`messageTopic`,`deliveryMode`,`contractStage`,`implementationStage` | 长期保留为跨服务审计写入入口；当前仍通过共享消息抽象发送审计事件 |
| `/api/governance/internal/schedule/extensions` | `TRANSITIONAL_SKELETON` | `TRANSITIONAL_SKELETON` | response: `extensionPoint`,`ownerService`,`status`,`currentMode`,`contractStage`,`implementationStage` | 当前只暴露治理调度扩展状态骨架，不代表完整调度域模型已固化 |

## 3.1 Query Execution Public HTTP Baseline

当前 `query-execution` 已固化联机查询 DTO / VO / 错误码契约，并把公共 HTTP 入口接到最小同步执行闭环：

| Endpoint | Request baseline | Response baseline | Current implementation stage |
|:---|:---|:---|:---|
| `/api/query-execution/queries/execute` | `QueryExecuteRequest` with `sqlText`,`tenantId`,`datasourceType`,`queryContext`,`accelerationPreference`,`faultToleranceStrategy` | `QueryExecuteResponse` with `status`,`rows`,`downloadUrl`,`metadata`,`degraded`,`degradeReason`,`retryPath`,`error`,`sqlFingerprint`,`contractStage`,`implementationStage` | `MINIMAL_SYNC_BASELINE` |

当前 `QueryExecuteRequest` / `QueryExecuteResponse` 约束如下：

- `sqlText`：必填，最大 `10MB`
- `tenantId`：必填
- `datasourceType`：必填，当前使用共享枚举 `HETU` / `HIVE` / `SPARK` / `CLICKHOUSE` / `GAUSSDB` / `AUTO`
- `queryContext.timeoutMs`：若提供则必须大于 `0`
- `accelerationPreference`：`PREFER_ACCELERATED` / `PREFER_FRESH` / `NONE`
- `faultToleranceStrategy`：`RETRY_THEN_FALLBACK` / `FAIL_FAST` / `FALLBACK_IMMEDIATE`

当前 `QueryExecuteResponse` 契约字段与初始化边界对齐如下：

- `metadata.targetEngine`
- `metadata.actualSql`
- `metadata.elapsedMs`
- `metadata.scannedRows`
- `metadata.cacheHit`
- `metadata.accelerationApplied`
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

- 当前实现已提供确定性的最小同步执行闭环：只读单语句 SQL 守卫、`AUTO/HETU -> HETU` 的主路由、`HETU -> HIVE` 的受控 fallback，以及正常/超时/失败/降级四条基础状态路径。
- 当前实现已补齐入口/出口/异常/状态变更日志，并在 timeout/fallback 路径上输出本地回滚/补偿标记：
  - timeout: `LOCAL_TIMEOUT_ROLLBACK_MARKED` + `CLOSE_PRIMARY_ATTEMPT_CONTEXT`
  - fallback: `LOCAL_FALLBACK_COMPENSATION_MARKED` + `RECORD_DEGRADED_RESULT`
- 当前实现仍不代表真实数据库执行已经开放：真实治理调用、真实引擎适配器和跨服务审计补偿仍待后续任务补齐。

## 3.2 SQL Optimization Task Contract Baseline

当前 `sql-optimization` 已将异步优化任务契约接到公共 HTTP skeleton，并通过 in-memory placeholder repository 提供可测的提交、轮询与失败路径。

| Endpoint | Request baseline | Response baseline | Current implementation stage |
|:---|:---|:---|:---|
| `POST /api/sql-optimization/tasks` | `OptimizationTaskSubmitRequest` with `tenantId`,`taskType`,`sqlText/sqlFingerprint`,`datasourceType`,`taskContext` | `OptimizationTaskSubmitResponse` with `taskId`,`status`,`currentPhase`,`estimatedReadyAt`,`statusQueryPath`,`contractStage`,`implementationStage` | `ASYNC_TASK_API_SKELETON` |
| `GET /api/sql-optimization/tasks/{taskId}` | path: `taskId` | `OptimizationTaskStatusResponse` with `taskId`,`taskType`,`status`,`currentPhase`,`priority`,`progressPercent`,`requestedSuggestionTypes`,`suggestion`,`failure`,`submittedAt`,`startedAt`,`finishedAt`,`statusHistory`,`contractStage`,`implementationStage` | `ASYNC_TASK_API_SKELETON` |

当前 `OptimizationTaskSubmitRequest` 基线字段如下：

- `tenantId`：必填
- `taskType`：必填，当前固定为 `PARSE` / `REWRITE` / `ACCELERATION_SUGGESTION`
- `sqlText` / `sqlFingerprint`：二选一至少提供一个
- `datasourceType`：必填，当前沿用共享枚举
- `taskContext.parseDepth`：`LIGHT` / `DEEP`，默认 `DEEP`
- `taskContext.priority`：`HIGH` / `NORMAL` / `LOW`，默认 `NORMAL`
- `taskContext.callbackUrl`：可选
- `taskContext.requestedSuggestionTypes`：仅 `ACCELERATION_SUGGESTION` 任务允许传入；为空时按 `ALL` 归一

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
- `22000` `SQL_OPTIMIZATION_TASK_INVALID`
- `22001` `SQL_OPTIMIZATION_TASK_NOT_FOUND`
- `22002` `SQL_OPTIMIZATION_TASK_ALREADY_FINISHED`
- `22003` `SQL_OPTIMIZATION_SUGGESTION_NOT_READY`

说明：

- 当前 `POST /api/sql-optimization/tasks` 会先返回 `QUEUED / SUBMITTED` 快照，再由占位处理链立即推进到成功或失败，以保持异步接口语义和稳定测试行为。
- 当前 `GET /api/sql-optimization/tasks/{taskId}` 已可查询占位 repository 中的最新任务状态；未知任务返回 `22001`。
- 当前模型已显式区分“外部生命周期状态”和“内部处理阶段”，避免把 parse / rewrite / acceleration suggestion 三类任务混成单一线性状态。
- 当前成功结果统一输出到 `suggestion`，并按三类任务给出结构化 `artifacts / benefits / costs / risks`：
  - `PARSE`：偏向 AST 摘要、血缘提示、分析收益和解析适配风险
  - `REWRITE`：偏向候选 SQL、规则轨迹、延迟/扫描收益、语义漂移风险
  - `ACCELERATION_SUGGESTION`：偏向加速计划、物化视图/分区策略、延迟/扫描收益、存储与新鲜度成本
- 当前失败结果统一输出到 `failure`，保留错误码和重试语义，并附失败阶段与风险说明。
- 当前 placeholder 失败路径通过 SQL 或指纹中的显式 `FAIL_OPTIMIZATION` 标记触发，用于稳定验证轮询失败场景。
- 当前实现仍未接入真实 MySQL 持久化、队列调度、事件回调和建议结果明细输出，这些能力继续由后续 `Phase-D` 任务补齐。

## 3.3 Benchmark Engine Model Contract Baseline

当前 `benchmark-engine` 已建立独立模块，并将压测任务/阈值/报告模型固化为后续接口复用的公共契约对象；真实 HTTP 入口继续由 `D-TASK-009` / `D-TASK-010` 补齐。

当前模型基线涉及以下契约对象：

- `BenchmarkTaskSubmitRequest`
- `BenchmarkTaskSubmitResponse`
- `BenchmarkTaskStatusResponse`
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
- `taskContext.readonlyRequired`：默认 `true`
- `taskContext.shadowEnvironmentMode`：`REQUIRED` / `PREFERRED` / `DISABLED`，默认 `REQUIRED`
- `taskContext.desensitizationRequirement`：`REQUIRED` / `OPTIONAL`，默认 `REQUIRED`
- `taskContext.thresholds[]`：阈值列表，元素字段为 `metric`、`operator`、`targetValue`、`severity`、`description`

当前 `BenchmarkTaskStatusResponse` 基线字段如下：

- `taskId`
- `taskType`
- `status`
- `currentPhase`
- `priority`
- `progressPercent`
- `targetEngines`
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

当前 `BenchmarkReportResponse` 基线字段如下：

- `reportId`
- `taskId`
- `taskType`
- `sqlFingerprint`
- `verdict`
- `generatedAt`
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
- `recommendations[].category`
- `recommendations[].title`
- `recommendations[].summary`
- `recommendations[].expectedBenefit`
- `recommendations[].riskLevel`
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

- 当前 `benchmark-engine` 只固化了模型与契约，不代表压测执行、调度、导出和报告查询接口已经开放。
- 当前模型已经把 `ADR-007` 要求的只读标记、影子环境标记和脱敏要求显式入模，避免后续执行链路绕过安全基线。
- 当前报告模型已覆盖引擎指标快照、阈值判定和建议输出，供后续提交流程与报告查询接口直接复用。

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
- 当前 `audit/write` 失败错误码已固定：
  - `10005` `SYSTEM_CONTEXT_MISSING`
  - `10008` `SYSTEM_MESSAGE_MODE_INVALID`
  - `10009` `SYSTEM_AUDIT_CONTRACT_INVALID`
  - `11002` `GOVERNANCE_SYSTEM_MESSAGE_ROUTE_INVALID`

## 6. Datasource Access Contract Baseline

当前治理内部 `datasource-access/check` 契约返回规则：

- `allowed=true` 时必须返回 `reason`，并显式返回 `contractStage` 与 `implementationStage`
- `allowed=false` 时必须返回显式 `errorCode`
- 当前失败错误码已固定：
  - `20001` `GOVERNANCE_TENANT_ACCESS_DENIED`
  - `20002` `GOVERNANCE_DATASOURCE_ACCESS_DENIED`

## 7. Related Documents

- `docs/security/access-control-spec.md`
- `docs/architecture/service-capability-map.md`
- `docs/plans/implementation-readiness.md`
- `docs/plans/task-governance-extension-matrix.md`
