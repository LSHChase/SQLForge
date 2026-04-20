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

## 2. Unified Error Code Ownership

遵循 `R-057`，错误码继续分为系统级 `10000-19999` 与业务级 `20000-29999`，并按服务域固定子区间：

| Range | Domain | Owner service | Notes |
|:---|:---|:---|:---|
| `10000-10999` | 公共系统错误 | `sqlforge-common` | 跨服务共享的参数、上下文、序列化、审计、消息抽象错误 |
| `11000-11999` | 公共管理系统错误 | 公共管理服务 | 身份、租户、数据源、审计、配额、配置中心相关系统错误 |
| `12000-12999` | 查询执行系统错误 | 查询执行服务 | 路由、缓存、轻量解析、执行控制系统错误 |
| `13000-13999` | SQL 优化系统错误 | SQL 优化服务 | 异步解析、建议生成、物化视图系统错误 |
| `14000-14999` | 压测系统错误 | 压测引擎服务 | 压测任务、调度、隔离、报告系统错误 |
| `20000-20999` | 公共管理业务错误 | 公共管理服务 | 权限不足、租户越权、配额不足、数据源授权失败等 |
| `21000-21999` | 查询执行业务错误 | 查询执行服务 | 查询风险拒绝、路由拒绝、结果集超限等 |
| `22000-22999` | SQL 优化业务错误 | SQL 优化服务 | 任务非法、建议不可用、审批前不可应用等 |
| `23000-23999` | 压测业务错误 | 压测引擎服务 | 非影子环境拒绝、只读限制、阈值不满足等 |

规则：

- `sqlforge-common` 只定义共享错误码，不拥有单服务业务错误。
- 单服务不能占用其他服务的业务区间。
- 跨服务返回统一 `ErrorResponse` 结构，不直接暴露内部堆栈。

## 3. Cross-Service DTO Baseline

| Interaction | Transport | Contract owner | Required DTO / response baseline | Current status |
|:---|:---|:---|:---|:---|
| 查询执行服务 -> 公共管理服务 | HTTP | 公共管理服务 | `TenantScopeCheckRequest/Response`, `DatasourceAccessCheckRequest/Response`, `QuotaCheckRequest/Response`, `AuditWriteRequest/Response` | Planned |
| SQL 优化服务 -> 公共管理服务 | HTTP | 公共管理服务 | `OptimizationApprovalCheckRequest/Response`, `MetadataLookupRequest/Response`, `AuditWriteRequest/Response` | Planned |
| 压测引擎服务 -> 公共管理服务 | HTTP | 公共管理服务 | `BenchmarkAuthorizationRequest/Response`, `ShadowEnvironmentCheckRequest/Response`, `AuditWriteRequest/Response` | Planned |
| 查询执行服务 -> SQL 优化服务 | HTTP / async callback | SQL 优化服务 | `OptimizationTaskSubmitRequest/Response`, `OptimizationTaskStatusResponse`, `AccelerationPlanApplyRequest/Response` | Planned |
| 压测引擎服务 -> 查询执行服务 | HTTP | 查询执行服务 | `QueryFingerprintLookupRequest/Response`, `RoutingRuleSnapshotRequest/Response` | Planned |

规则：

- 所有 DTO 均为跨服务契约对象，不得复用内部 entity。
- `sqlforge-common` 仅承载共享契约基类、通用上下文和错误响应，不承载某一服务专属业务 DTO。
- 若跨服务契约变化具有兼容风险，必须先更新本文件和主计划，再进入实现。

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

## 6. Related Documents

- `docs/security/access-control-spec.md`
- `docs/architecture/service-capability-map.md`
- `docs/plans/implementation-readiness.md`
- `docs/plans/task-governance-extension-matrix.md`
