# ADR-012: 分布式事务暂不引入，采用 Saga + 本地事务

- Status: Accepted
- Date: 2026-04-19
- Deciders: Human Architect, Codex Implementation Lead
- Technical Story: `Phase-D / 跨服务事务编排`
- Tags: saga, transaction, distributed-systems

## Context

SQLForge 最终采用 4 个微服务，需要跨服务处理查询执行、优化建议、压测报告、审计、导出和配置变更。项目当前更关注清晰边界、可观测性和可恢复性，而不是在早期引入复杂分布式事务框架。

## Decision Drivers

- 4 个微服务之间存在跨服务业务流程
- 团队成熟度与当前阶段不适合引入复杂分布式事务框架
- 审计和补偿链路比强一致 2PC 更契合当前架构
- 消息抽象和状态机已经为补偿提供基础

## Considered Options

1. 引入 2PC/XA 分布式事务
2. 不处理跨服务一致性
3. 采用 Saga + 本地事务 + 审计补偿

## Decision

选择 Saga + 本地事务。每个服务只保证自己的本地事务边界，跨服务一致性通过状态机、补偿操作、审计日志和消息驱动协调完成。当前阶段不引入 XA/Seata 一类强分布式事务框架。

## Consequences

### Positive

- 保持服务边界清晰
- 与消息抽象、审计链路和状态流转更匹配
- 降低早期系统复杂度和耦合

### Negative

- 需要设计补偿、幂等和状态回放
- 短时间内可能存在最终一致性窗口
- 业务建模与错误处理要求更高

### Neutral

- 不改变 MySQL 作为事务主库
- 不改变压测、查询或前端边界本身

## Compliance Impact

- 身份鉴别：跨服务流程中的身份上下文必须显式传递
- 访问控制：补偿和重试同样受权限控制
- 安全审计：每步事务和补偿都必须可审计
- 加密存储：消息和持久化中的敏感字段仍需脱敏/加密
- 备份恢复：状态机和补偿记录纳入恢复清单

## Implementation Notes

- 影响模块：4 个微服务的跨域流程
- 需要统一幂等键、关联键、状态机模型和补偿日志
- 与 `R-144` 消息抽象、`R-036` 结果不可变和审计链路联动

## Validation

- 单元测试：状态流转、补偿、幂等
- 集成测试：跨服务异常回滚和最终一致性路径
- 构建验证：服务编译与消息契约测试
- 文档一致性检查：异常回滚流程和接口契约一致
- 合规自检：补偿流程可审计且不越权

## Links

- 相关规则：`R-034`, `R-036`, `R-041`, `R-042`, `R-044`, `R-144`
- 相关计划：`docs/plans/master-execution-plan.md`
- 相关文档：`docs/architecture/init.md`, `docs/architecture/messaging-abstraction.md`
