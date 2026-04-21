# ADR-002: 微服务拆分与边界上下文划分

- Status: Accepted
- Date: 2026-04-19
- Deciders: Human Architect, Codex Implementation Lead
- Technical Story: `Phase-B / HC-001`
- Tags: microservices, bounded-context, architecture

## Context

`docs/architecture/init.md` 同时保留了“11 个原始服务来源”和“合并后的 4 个微服务目标”。人类已确认项目最终形态采用 4 个微服务，而不是保留 11 个独立交付服务。当前仓库已有前端工程、`sqlforge-shared` 和 `governance`，仍处于从初始化脚手架向目标服务拓扑过渡阶段。

## Decision Drivers

- 人类明确确认最终目标为 4 个微服务
- 需要减少联机查询主链路的 RPC 抖动和边界重复
- 需要让异步优化、压测隔离、管理面能力形成清晰边界
- 团队规模与“2 Pizza Team”更适合 4 个清晰域
- 必须保持高内聚低耦合和前后端分离

## Considered Options

1. 保留 11 个原始服务长期独立演进
2. 合并为 4 个微服务作为最终目标
3. 继续单体化推进，后续再拆分

## Decision

选择 4 个微服务作为 SQLForge 的最终服务域：

1. 查询执行服务
2. SQL 优化服务
3. 压测引擎服务
4. 公共管理服务

11 个原始服务仅作为来源和职责映射参考，不再作为最终交付口径。仓库中间态允许以较少模块逐步演进，但所有新增能力必须向这 4 个最终边界对齐。

## Consequences

### Positive

- 联机查询链路聚合，降低跨服务调用成本
- 异步优化与压测隔离，避免干扰主查询
- 管理面、审计、数据源、租户、血缘等共享能力内聚

### Negative

- 需要在过渡期间管理“当前实现模块”和“目标服务域”的双重视图
- 合并边界后，单个服务的职责密度更高，需要强制分层
- 需要更严格的契约治理与 Service DTO 边界

### Neutral

- 不改变前端技术栈和消息抽象模式
- 不直接决定每个服务的具体代码目录，只决定最终边界

## Compliance Impact

- 身份鉴别：所有 4 个服务均必须在后端身份链路下运行
- 访问控制：租户与资源范围校验由公共管理服务统一约束，并被其他服务消费
- 安全审计：查询、优化、压测、管理操作均需落审计
- 加密存储：敏感连接信息统一在管理域控制
- 备份恢复：围绕管理域和事务主库设计服务级恢复策略

## Implementation Notes

- 当前 `governance` 视为公共管理服务的现阶段实现基线
- 后续新增模块和接口必须标注映射到 4 个最终服务之一
- 跨域调用只能通过 Service DTO 和接口契约

## Validation

- 单元测试：跨域转换和服务边界测试
- 集成测试：服务间接口契约测试
- 构建验证：多模块编译与分离检查
- 文档一致性检查：服务边界与计划、ADR、接口契约一致
- 合规自检：角色、租户、审计链路不被合并边界削弱

## Links

- 相关规则：`R-015`, `R-018`, `R-020`, `R-021`, `R-022`, `R-068`
- 相关计划：`docs/plans/master-execution-plan.md`
- 相关文档：`docs/architecture/init.md`, `docs/quality/frontend-backend-separation-baseline.md`
