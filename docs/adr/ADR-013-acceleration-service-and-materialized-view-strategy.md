# ADR-013: 加速服务独立部署与物化视图管理策略

- Status: Accepted
- Date: 2026-04-19
- Deciders: Human Architect, Codex Implementation Lead
- Technical Story: `Phase-D / SQL优化服务`
- Tags: acceleration, materialized-view, optimization

## Context

架构文档要求平台提供预计算、分区、分桶、拆分、替换等加速建议和配置管理，并把物化视图管理纳入核心能力。加速逻辑属于计算密集型和策略密集型能力，不适合完全嵌入联机查询主链路。

## Decision Drivers

- 联机查询主链路需要低延迟
- 加速建议和物化视图管理属于异步优化能力
- 需要统一管理加速配置的审批、生效、评估和回滚
- 必须保留与查询执行服务的验证接口

## Considered Options

1. 加速能力完全内嵌查询执行服务
2. 加速能力完全独立且与优化脱离
3. 以 SQL 优化服务为核心管理加速建议和物化视图，查询执行服务只消费已批准配置

## Decision

选择将加速建议和物化视图管理收敛到 SQL 优化服务，作为独立优化域能力部署；查询执行服务只在运行时应用已批准和已生效的加速配置。

## Consequences

### Positive

- 降低联机查询主链路复杂度
- 优化建议、成本估算、物化视图和效果评估集中治理
- 便于审批、回滚和周期性重评估

### Negative

- 需要服务间接口验证加速效果
- 配置同步和失效控制更复杂
- 需要管理物化视图生命周期和存储成本

### Neutral

- 不改变公共管理服务的审批和审计职责
- 不改变压测服务独立隔离策略

## Compliance Impact

- 身份鉴别：创建、审批、撤销加速配置都要鉴权
- 访问控制：租户只能管理本租户范围内的加速策略
- 安全审计：配置变更、审批、生效、回滚和评估全部留痕
- 加密存储：相关数据源敏感配置仍由管理服务加密控制
- 备份恢复：加速配置和物化视图元数据纳入恢复清单

## Implementation Notes

- 影响模块：SQL 优化服务、查询执行服务、公共管理服务
- 需要定义加速配置状态机：`DRAFT`、`PENDING_APPROVAL`、`APPROVED`、`ACTIVE`、`REVOKED`
- 需要提供效果验证与失效回滚机制

## Validation

- 单元测试：建议生成、状态机、生效和回滚逻辑
- 集成测试：优化服务到查询执行服务的配置应用
- 构建验证：相关服务编译和契约测试
- 文档一致性检查：接口契约、部署和审计文档一致
- 合规自检：审批、越权和回滚路径可审计

## Links

- 相关规则：`R-020`, `R-034`, `R-041`, `R-044`, `R-068`
- 相关计划：`docs/plans/master-execution-plan.md`
- 相关文档：`docs/architecture/init.md`
