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
- `JDBC` / `REST` / `CLIENT` 三种 Hetu 访问模式的边界声明
- 只读优先、开源 parser 复用、已批准加速配置运行时应用的策略声明

当前还未完整承载：

- 与公共管理服务的实际租户、数据源、审计调用
- 真实引擎适配器与生产结果聚合实现

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
- `POST /api/sql-optimization/tasks` 和 `GET /api/sql-optimization/tasks/{taskId}` 的过渡骨架
- 基于 in-memory placeholder repository 的提交、轮询、失败路径与流程日志
- 基础 DTO / VO 与错误码区间固化
- 结构化 `suggestion / failure` 输出，覆盖收益、成本、风险与任务类型差异

当前还未完整承载：

- MySQL 持久化、队列调度和回调通知
- 建议结果明细、审批协同与物化视图治理

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
- 基于 in-memory placeholder repository 的提交、轮询、失败路径、占位报告落库与流程日志
- 只读要求、影子环境模式、脱敏要求、并发/时长/预热/数据规模等任务元数据固化
- 阈值模型、阈值判定结果、引擎指标快照、优化建议和报告契约对象
- 基础 DTO / VO、错误码区间和模型装配 service

当前还未完整承载：

- 真正的调度、隔离执行、队列和报告导出
- 报告查询公共 HTTP 接口
- 与公共管理服务、查询执行服务的真实跨服务调用

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
- MyBatis XML 与多环境配置基础

当前还未完整承载：

- 角色矩阵
- 数据源授权矩阵
- 审计全链路
- 敏感配置加密落库
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
  1. 在 `benchmark-engine` 上补报告查询与导出骨架
  2. 在 `query-execution` 上继续补真实治理调用与真实引擎适配器
  3. 在 `sql-optimization` 上补持久化、队列和回调通知
  4. 在 `governance` 上继续补完整授权矩阵与审计链路
  5. 在 `benchmark-engine` 上补真实调度与隔离执行链路

## Related Documents

- `docs/architecture/init.md`
- `docs/adr/ADR-002-microservice-splitting-and-bounded-contexts.md`
- `docs/plans/document-truth-baseline.md`
- `docs/plans/implementation-readiness.md`
