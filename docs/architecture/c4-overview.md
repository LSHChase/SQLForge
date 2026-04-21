# SQLForge C4 Overview

本文档是 SQLForge 当前权威的文字版 C4 工件，用于满足 `R-133` 的“架构图（C4）描述同步更新”要求。

- 当前仓库阶段不强制要求独立图片工件。
- 当前以本文件作为 C4 Level 1-4 的统一更新落点。
- 若后续补充图片版 C4，本文件仍作为文字权威说明与索引入口保留。

## 1. Update Rules

- 发生以下变化时，必须同步更新本文件：
  - 服务边界、系统上下游、部署拓扑变化
  - 新增或移除微服务、前端容器、共享模块
  - 容器内分层、关键组件职责或跨模块调用关系变化
  - 影响 `docs/architecture/service-capability-map.md` 或 `docs/architecture/service-interface-contract-baseline.md` 解释前提的架构变化
- 若变更同时影响初始化总览、文档索引或专项架构文档，还需同步：
  - `docs/architecture/init.md`
  - `docs/README.md`
  - 对应专项文档，例如 `messaging-abstraction.md`

## 2. Current Truth Snapshot

- 当前仓库已有根级前端工程，负责驾驶舱与业务页面。
- 当前 Maven 聚合工程包含 `sqlforge-shared/`、`governance/`、`query-execution/` 与 `sql-optimization/`。
- 目标架构固定为 4 个微服务：
  - 查询执行服务
  - SQL 优化服务
  - 压测引擎服务
  - 公共管理服务
- 当前已有公共管理服务方向的 `governance` 基线、查询执行服务方向的 `query-execution` 边界骨架，以及 SQL 优化服务方向的 `sql-optimization` 任务模型骨架；压测引擎服务仍处于目标边界和计划阶段。

## 3. C4 Level 1: System Context

### 3.1 Primary actors

| Actor / External System | Role | Current status |
|:---|:---|:---|
| 平台管理员 / 租户管理员 / 运营人员 | 使用管理能力、查看状态、维护租户与消息治理 | 已有文档与部分治理接口基线 |
| 研发与运维人员 | 按主计划、规则与门禁推进实现、验证与部署 | 已落地文档与脚本流程 |
| Hetu / Hive / Spark / ClickHouse 等执行引擎 | 查询执行、加速能力的外部依赖 | 当前以目标边界和初始化设计存在 |
| MySQL | 配置、治理数据、消息表、审计主存储的核心关系型存储 | 当前已形成治理服务侧基线 |
| Kafka | 异步消息总线，生产环境目标模式 | 当前 `DATABASE` / `MOCK` 可运行，`KAFKA` 客户端已接入但真实集群验证仍待补齐 |

### 3.2 System purpose

SQLForge 面向多租户数据平台治理、查询执行、SQL 优化与压测治理场景，目标是在等保、审计、分层与服务边界约束下，提供可演进的 4 微服务体系。

## 4. C4 Level 2: Container Overview

| Container | Responsibility | Current / Target |
|:---|:---|:---|
| Root frontend (`src/`, `package.json`) | 驾驶舱、业务页面、前端信息架构 | Current |
| `governance` | 公共管理服务阶段性基线，承载治理接口、租户配置、消息治理、请求上下文与基础能力 | Current |
| `query-execution` | 查询执行服务边界骨架，固化路由、执行控制、轻量解析、轻量改写和已批准加速配置应用的承载位置 | Current baseline |
| `sql-optimization` | SQL 优化服务任务模型骨架，固化异步任务类型、状态流转和提交/轮询契约对象 | Current baseline |
| `sqlforge-shared` | 共享错误码、上下文、异常、审计契约、配置常量、日志与工具能力 | Current |
| 压测引擎服务 | 负责压测任务编排、隔离执行、阈值判定与报告 | Target |
| MySQL / 存储表结构 | 配置、治理、消息、审计等数据持久化 | Current |
| Kafka / 消息抽象通道 | 生产消息总线，支持治理与异步事件 | Current baseline + Target production |

## 5. C4 Level 3: Current Container Internals

### 5.1 `governance`

- `application/`
  - 作为入站与编排代码包域，承载 Controller、VO/DTO、以 `*ApplicationService` 命名的 service、Interceptor
  - 实际分层仍以 `controller`、`service` 为准，不把 `application` 单独视为一层
- `domain/`
  - 租户配置、消息、访问校验等领域实体、仓储接口、领域逻辑
- `infrastructure/`
  - MyBatis 持久化、消息实现、调度与适配层
- `config/`
  - `messaging`、鉴权、Web MVC 等配置基线

### 5.2 `sqlforge-shared`

- `config/`
  - 统一请求头、服务编码、消息模式等公共配置
- `context/`
  - `RequestContext`、`TenantContext`
- `exception/`
  - `BizException`、统一异常响应与处理器
- `audit/`
  - 审计上下文与审计事件契约
- `constants/`
  - 错误码、缓存键、数据源类型等
- `log/` / `utils/` / `async/`
  - 公共日志切面、工具类与异步执行能力

### 5.3 `query-execution`

- `application/`
  - 作为入站与编排代码包域，承载公共查询 controller/DTO/VO、边界快照 service、最小同步执行 service 和流程日志入口
  - 实际分层仍以 `controller`、`service` 为准，不把 `application` 单独视为一层
- `domain/`
  - 查询执行边界定义、只读优先策略、最小同步状态流转、本地 timeout/fallback 恢复标记、Hetu 三模式接入边界、已批准加速配置应用边界
- `infrastructure/`
  - 当前包含受控的确定性同步执行适配器，并继续为 JDBC / REST / client 真实适配器预留独立目录
- `config/`
  - 独立服务名、端口、多环境和日志配置骨架

### 5.4 `sql-optimization`

- `application/`
  - 作为入站与编排代码包域，承载异步优化任务提交 DTO、状态响应 VO 和模型转换 service
  - 实际分层仍以 `controller`、`service` 为准，不把 `application` 单独视为一层
- `domain/`
  - 异步优化任务实体、任务类型、生命周期状态、处理阶段流转、优先级和加速建议类型
- `infrastructure/`
  - 当前仅预留队列、持久化和回调适配位置，尚未接入真实实现
- `config/`
  - 独立服务名、端口、多环境和日志配置骨架

## 6. C4 Level 4: Code-Level Baseline

- 当前 Level 4 不以逐类绘图表达，而以“分层结构 + 接口契约 + 专项架构文档”的文字组合描述为准。
- 代码级细化说明主要由以下文档共同承担：
  - `docs/architecture/service-interface-contract-baseline.md`
  - `docs/architecture/messaging-abstraction.md`
  - `docs/architecture/service-capability-map.md`
  - `docs/architecture/init.md` 中保留的初始化总览和历史结构索引
- 若某服务后续进入独立实现阶段，应先在本文件更新 Level 2/3 边界，再在对应专项文档细化 Level 4 说明。

## 7. Sync Checklist

在执行 `R-133` 时，涉及架构变化的提交至少检查以下项目：

1. 本文件的 Level 1-4 说明是否仍与当前仓库事实和目标边界一致。
2. `docs/README.md` 是否已收录本文件并保持索引可达。
3. `docs/architecture/init.md` 是否仍与本文件的 C4 更新落点说明一致。
4. 如变化涉及服务边界、接口或消息模式，对应专项文档是否已同步。
