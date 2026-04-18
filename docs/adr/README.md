# ADR Index

本目录采用 MADR 风格维护架构决策记录，所有重要技术决策都应有独立 ADR，并标记状态、上下文、决策、后果与合规影响。

## 当前 ADR 清单

| ADR | 标题 | 状态 |
|:---|:---|:---|
| `ADR-001` | 订单服务数据库选型示例 | Accepted |
| `ADR-002` | 微服务拆分与边界上下文划分 | Accepted |
| `ADR-003` | Hudi Copy on Write 表类型与写入策略 | Accepted |
| `ADR-004` | 华为云 MRS Hetu 与自研路由引擎集成边界（JDBC/REST/客户端三模式） | Accepted |
| `ADR-005` | SQL 解析引擎自研范围与开源组件引用边界 | Accepted |
| `ADR-006` | 数据血缘与元数据管理方案 | Accepted |
| `ADR-007` | 压测引擎与生产环境安全隔离策略 | Accepted |
| `ADR-008` | 永洪 BI 对接协议与 JDBC 驱动自研范围 | Accepted |
| `ADR-009` | 数据保留与销毁策略（等保合规） | Accepted |
| `ADR-010` | 跨境数据流动与多区域扩展预留 | Proposed |
| `ADR-011` | 缓存一致性策略（Hudi 时间戳校验） | Accepted |
| `ADR-012` | 分布式事务暂不引入，采用 Saga + 本地事务 | Accepted |
| `ADR-013` | 加速服务独立部署与物化视图管理策略 | Accepted |

## 记录要求

- 决策前必须记录上下文、约束、备选方案和推荐方案。
- 决策后必须写明对服务边界、分层、合规、测试和运维的影响。
- 状态至少使用 `Proposed`、`Accepted`、`Superseded`、`Deprecated`。
- 与等保、审计、备份、加密相关的变更必须在 ADR 中明确合规影响。
- Codex 执行任务时只能在 ADR 已确认边界内实现代码。
