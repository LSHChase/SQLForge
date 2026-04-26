# SQLForge Service Capability Map

## Summary

本文件把 SQLForge 已确认的 4 微服务目标边界映射到当前仓库现状，明确“最终由谁负责”“当前由谁承载”“哪些能力不能继续堆进现有模块”。

## Current-To-Target Mapping

| Final service | Core responsibilities | Current carrier | Current state |
|:---|:---|:---|:---|
| 查询执行服务 | 查询提交、路由、缓存、轻量解析、轻量改写、执行控制、结果聚合 | `query-execution` | Partial |
| SQL 优化服务 | 异步深度解析、改写建议、加速建议、成本估算、物化视图策略 | `sql-optimization` | Partial |
| 压测引擎服务 | 压测任务、调度、并行执行、报告生成、隔离控制 | `benchmark-engine` | Partial |
| 公共管理服务 | 租户、配额、数据源、审计、合规、元数据、调度、平台治理 | `governance` | Partial |

## Service Allocation

### 1. 查询执行服务

负责：

- 查询提交与执行生命周期
- SQL 指纹、缓存命中、路由决策
- 轻量解析、轻量改写和加速配置应用
- 与公共管理服务协同完成租户、数据源、审计校验

不负责：

- 平台级租户管理
- 权限模型配置
- 异步深度优化建议计算
- 加速建议审批与物化视图治理
- 压测执行与压测报告

当前由 `query-execution` 承载的最小实现包括：

- Spring Boot 应用入口和独立 Maven 模块
- `application` / `domain` / `infrastructure` / `config` 分层骨架
- 查询执行服务的不可变边界定义，显式收口到路由、执行控制、轻量解析、轻量改写和已批准加速配置应用
- `JDBC` / `REST` / `CLIENT` 三种 Hetu 访问模式的边界声明，以及真实模式选择、route calibration、严格路由失败语义与结果聚合实现
- 只读优先、开源 parser 复用、已批准加速配置运行时应用的策略声明
- 受保护内部 Hetu route calibration / cluster evidence 快照入口，以及 ready/unready / priority / failure-layer 证据模型
- 受保护内部 acceleration-plan apply / verify / rollback runtime gating 入口，以及按 `tenantId + sqlFingerprint + datasourceType` 收口的 approved binding registry
- 受保护内部 cache policy apply / verify / invalidate runtime surface，以及按 `tenantId + sqlFingerprint + datasourceType + schemaVersion` 收口的 result-cache hit / bypass / invalidate / backfill 证据模型；当前还具备 provider-neutral cache backend contract、默认 in-memory backend、显式配置的 Redis RESP provider adapter、per-tenant/per-policy capacity limit、TTL/manual/capacity/schema eviction reason evidence、policy verify capacity/backend health summary，以及低基数 cache governance metrics
- 与 `governance` 的租户范围检查、数据源访问检查和审计写入 HTTP 调用基线

当前还未完整承载：

- 外部 Win10 + Hetu/MRS live smoke 的长期归档留证与环境 owner 执行窗口
- 分布式 cache provider 的真实环境长期运行证据与跨节点恢复演练；真实 Redis 集群长跑/恢复演练仍是 environment-backed follow-up
- 更完整的跨服务审计补偿与持久化追溯收口

## 2. SQL 优化服务

负责：

- 异步深度解析
- 改写建议、收益评估和成本估算
- 加速建议与物化视图相关策略
- 结果反馈给查询执行服务或管理面

不负责：

- 同步联机查询主链路
- 租户、数据源和审计主数据管理
- 压测调度

当前由 `sql-optimization` 承载的最小实现包括：

- Spring Boot 应用入口和独立 Maven 模块
- `application` / `domain` / `infrastructure` / `config` 分层骨架
- `PARSE` / `REWRITE` / `ACCELERATION_SUGGESTION` 三类异步优化任务模型
- `QUEUED` / `RUNNING` / `SUCCEEDED` / `FAILED` / `CANCELLED` 生命周期状态与类型感知的处理阶段流转
- `POST /api/sql-optimization/tasks` 和 `GET /api/sql-optimization/tasks/{taskId}` 的受保护异步入口
- `POST /api/sql-optimization/acceleration-plans`、`GET /api/sql-optimization/acceleration-plans/{planId}`、`approval/apply/verify/rollback` 的受保护治理入口
- 基于 MySQL `optimization_task` / `acceleration_plan` 双表、MyBatis XML repository 和 in-process scheduled worker 的提交、轮询、失败路径与流程日志
- 基础 DTO / VO 与错误码区间固化
- 真实 SQL parser / AST analysis / conservative rewrite rule / acceleration suggestion pipeline
- 结构化 `suggestion / failure` 输出，覆盖收益、成本、风险、失败阶段与任务类型差异
- acceleration plan 通过 `governance` 受保护 trace 入口回写 `config/result/history` 追溯链，并通过 `query-execution` internal runtime surface 收口 apply/verify/rollback 闭环
- 与 `governance` 的租户/数据源检查、审计写入、失败恢复与补偿 queue smoke

当前还未完整承载：

- 外部队列调度、回调通知
- 更接近 engine-native / materialized-view 的物理加速编排与长期运行证据

## 3. 压测引擎服务

负责：

- 压测任务模型与状态机
- 影子环境或隔离执行路径
- 基线生成、阈值判断、报告输出
- 与公共管理服务协同完成授权和审计

不负责：

- 生产查询入口
- 平台治理配置中心
- 通用元数据治理

当前由 `benchmark-engine` 承载的最小实现包括：

- Spring Boot 应用入口和独立 Maven 模块
- `application` / `domain` / `infrastructure` / `config` 分层骨架
- `BASELINE` / `COMPARISON` / `REGRESSION_GUARD` 三类压测任务模型
- `QUEUED` / `RUNNING` / `SUCCEEDED` / `FAILED` / `CANCELLED` 生命周期状态与任务类型感知的阶段流转
- `POST /api/benchmark-engine/tasks` 和 `GET /api/benchmark-engine/tasks/{taskId}` 的过渡骨架
- `GET /api/benchmark-engine/reports/{reportId}` 的 JSON / PDF / HTML 报告查询骨架
- 基于 MySQL `benchmark_task` / `benchmark_task_report`、MyBatis XML repository 和 in-process scheduled worker 的提交、轮询、失败路径、报告回写、报告查询与流程日志；当前还支持显式配置的 `external-file-queue` carrier，并把 `queueMode/queueEvidence` 回写到任务状态与审计载荷
- repo-closed 隔离执行 service、执行摘要，以及优先复用 `query-execution` 内部 workload capture、失败时显式 synthetic backfill、同批次 mixed live/fallback 时的 compensation-replay 可复现 orchestration
- benchmark workload 与 execution summary 已保留 query-execution cache governance status/evidence，可把 cache hit/bypass/backfill/invalidation 以及 eviction/capacity 证据继续写入 governance trace payload
- 持久化 `JSON/PDF/HTML` 导出产物 bundle、raw-data snapshot download，以及从已 externalize artifact 直接返回报告导出/下载的查询路径
- repo-local artifact storage 基线，以及面向 `governance` 内部受保护入口的 benchmark report trace/export orchestration；当前还会把 workload/backfill/compensation evidence 提升为治理长期追溯链中的显式结构载荷
- 报告查询/下载审计补齐 `config/result/history/export` 链接键，以及 repo-local artifact 的 stale-file cleanup / snapshot recovery 语义
- tenant-specific artifact retention/backfill policy：通过治理侧 `tenant_config.retention_days` 解析 retention days，并在历史 artifact 查询/恢复时回填 policy metadata
- 显式配置的 `ENVIRONMENT_OBJECT_STORAGE` adapter：保留 repo-local lifecycle 为默认路径，同时为 object URI / repo-local mirror / env var 依赖生成 evidence，并可在配置 primary/recovery provider endpoint、bucket、credentials、provider contract 与 cleanup scope 时执行真实 provider-backed write/readback recovery verification；如同时配置 external write dir，则会叠加 external write/readback verification
- provider-specific / multi-provider contract、cleanup/recovery order 与 failure-replay 语义：当前可按 `REPO_LOCAL_MIRROR -> PRIMARY_PROVIDER -> RECOVERY_PROVIDER -> EXTERNAL_WRITE -> REPORT_SNAPSHOT` 的证据顺序恢复 artifact，并把实际 recovery source/read status 写回 benchmark 审计与治理追溯查询面
- provider-native 运行语义：当前 live-evidence manifest 与 storage evidence 会显式沉淀 `providerContract/providerDialect/providerHeadStatus/providerContent*/providerEtag/providerRequestId`，并保留 provider-authenticated cleanup/recovery 边界，而不把环境级对象存储改写成仓库默认主路径
- 只读要求、影子环境模式、脱敏要求、并发/时长/预热/数据规模等任务元数据固化
- 阈值模型、阈值判定结果、引擎指标快照、趋势图表、优化建议和报告契约对象
- 基础 DTO / VO、错误码区间和模型装配 service

当前还未完整承载：

- 更广的 environment-backed 执行证据、真实对象存储长期留证与跨服务恢复编排的进一步扩展
- 更广的跨服务运行时留证、恢复编排与环境级操作证据

## 4. 公共管理服务

负责：

- 租户、角色、配额、数据源、配置中心
- 审计、合规、元数据、血缘、通用调度
- 统一身份上下文和资源范围校验基线
- 为其他服务提供受控查询、授权和审计入口

当前由 `governance` 承载的最小实现包括：

- 健康检查
- 最小租户上下文建立
- 租户配置查询
- 消息重试与管理入口骨架
- 核心追溯链表结构基线：`config_snapshot`、`execution_result`、`query_history`、`export_record` 与扩展后的 `audit_log`
- `POST /api/governance/internal/audit/write` 的真实落库基线，支持把 `config/result/history/export` 追溯键接入 `audit_log`
- `POST /api/governance/internal/acceleration-plan/trace/write` 的真实落库基线，支持把 acceleration plan 生命周期回写到 `config_snapshot/execution_result/query_history`
- header-based stateless auth 的 `LOGIN` / `LOGOUT` 审计落库基线
- 共享 AES-256 敏感字段保护基线，以及 `GovernanceProtectedPersistenceService` 对 config/result/history/export/audit/system-config 的受保护写入入口
- governance history summaries/lookups/detail：可把 cache governance surface（含 eviction/capacity evidence）、compensation-replay evidence、artifact storage contract、artifact recovery surface 与 artifact operation surface 作为显式结构字段提供给治理检索、恢复判断与受控 cleanup/recovery 触发链路
- MyBatis XML 与多环境配置基础

当前还未完整承载：

- 角色矩阵
- 数据源授权矩阵
- 跨服务主动上报与审计全链路
- 更完整的平台治理配置与外部密钥管理接入
- 平台治理完整实体模型

## Governance Service Transition Rules

- 当前 `governance` 只能继续向“公共管理服务”边界收敛。
- 不允许把查询执行、异步优化或压测主流程长期堆进 `governance`。
- 若为快速验证需要在 `governance` 暂放跨域能力，必须：
  - 明确标注是过渡实现
  - 给出迁移目标服务
  - 不固化为长期契约

## sqlforge-shared Boundary

`sqlforge-shared` 仅允许承载以下公共能力：

- 错误码常量
- 基础异常模型
- 请求上下文与租户上下文
- 审计事件契约
- 公共配置契约
- 无业务语义的工具类

`sqlforge-shared` 不允许承载：

- 业务实体
- 服务专有 repository
- 领域逻辑
- 面向单一服务的 controller 或位于 `application` 包域内的 service

## Cross-Service Rules

- 所有跨服务交互只能通过接口契约、DTO 或消息契约完成。
- 禁止跨服务直接共享 entity。
- 公共管理服务可以作为共享治理能力提供方，但不应成为其他服务的逻辑兜底层。
- 审计、租户和数据源权限必须在调用链中可追踪。

## Immediate Implementation Implications

- 下一轮后端实现优先级应是：
  1. 在 `query-execution` 上继续补真实治理调用与真实引擎适配器
  2. 在 `sql-optimization` 上补外部队列、回调通知和更细粒度结果持久化
  3. 在 `benchmark-engine` 上补真实调度、隔离执行与真实导出链路
  4. 在 `governance` 上继续补完整授权矩阵与审计链路
  5. 在跨服务共享表与关联键层补齐 Phase-D 核心数据基线

## Related Documents

- `docs/architecture/init.md`
- `docs/adr/ADR-002-microservice-splitting-and-bounded-contexts.md`
- `docs/plans/document-truth-baseline.md`
- `docs/plans/implementation-readiness.md`
