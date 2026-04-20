# SQLForge Service Capability Map

## Summary

本文件把 SQLForge 已确认的 4 微服务目标边界映射到当前仓库现状，明确“最终由谁负责”“当前由谁承载”“哪些能力不能继续堆进现有模块”。

## Current-To-Target Mapping

| Final service | Core responsibilities | Current carrier | Current state |
|:---|:---|:---|:---|
| 查询执行服务 | 查询提交、路由、缓存、轻量解析、轻量改写、执行控制、结果聚合 | 尚未建立独立模块 | Pending |
| SQL 优化服务 | 异步深度解析、改写建议、加速建议、成本估算、物化视图策略 | 尚未建立独立模块 | Pending |
| 压测引擎服务 | 压测任务、调度、并行执行、报告生成、隔离控制 | 尚未建立独立模块 | Pending |
| 公共管理服务 | 租户、配额、数据源、审计、合规、元数据、调度、平台治理 | `governance-service` | Partial |

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
- 压测执行与压测报告

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

## 4. 公共管理服务

负责：

- 租户、角色、配额、数据源、配置中心
- 审计、合规、元数据、血缘、通用调度
- 统一身份上下文和资源范围校验基线
- 为其他服务提供受控查询、授权和审计入口

当前由 `governance-service` 承载的最小实现包括：

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

- 当前 `governance-service` 只能继续向“公共管理服务”边界收敛。
- 不允许把查询执行、异步优化或压测主流程长期堆进 `governance-service`。
- 若为快速验证需要在 `governance-service` 暂放跨域能力，必须：
  - 明确标注是过渡实现
  - 给出迁移目标服务
  - 不固化为长期契约

## sqlforge-common Boundary

`sqlforge-common` 仅允许承载以下公共能力：

- 错误码常量
- 基础异常模型
- 请求上下文与租户上下文
- 审计事件契约
- 公共配置契约
- 无业务语义的工具类

`sqlforge-common` 不允许承载：

- 业务实体
- 服务专有 repository
- 领域逻辑
- 面向单一服务的 controller 或 application service

## Cross-Service Rules

- 所有跨服务交互只能通过接口契约、DTO 或消息契约完成。
- 禁止跨服务直接共享 entity。
- 公共管理服务可以作为共享治理能力提供方，但不应成为其他服务的逻辑兜底层。
- 审计、租户和数据源权限必须在调用链中可追踪。

## Immediate Implementation Implications

- 下一轮后端实现优先级应是：
  1. 把 `sqlforge-common` 做实
  2. 把 `governance-service` 强化为公共管理服务基线
  3. 新建查询执行服务骨架
  4. 新建 SQL 优化服务骨架
  5. 新建压测引擎服务骨架

## Related Documents

- `docs/architecture/init.md`
- `docs/adr/ADR-002-microservice-splitting-and-bounded-contexts.md`
- `docs/plans/document-truth-baseline.md`
- `docs/plans/implementation-readiness.md`
