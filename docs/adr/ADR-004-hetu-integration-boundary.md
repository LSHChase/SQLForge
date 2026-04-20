# ADR-004: 华为云 MRS Hetu 与自研路由引擎集成边界（JDBC/REST/客户端三模式）

- Status: Accepted
- Date: 2026-04-19
- Deciders: Human Architect, Codex Implementation Lead
- Technical Story: `Phase-D / 查询执行服务`
- Tags: hetu, huawei-cloud, integration, routing

## Context

项目目标环境为华为云私有云与 MRS，核心查询引擎包含 Hetu。架构文档要求查询执行服务具备多模式集成能力，并在不同部署约束和故障条件下做路由、降级和回滚。

## Decision Drivers

- 华为云私有云是目标部署环境
- 查询执行需要兼顾兼容性、稳定性和故障降级
- 不同场景下的接入能力和可运维性不同
- 必须保证执行边界明确，不能让业务层直接耦合具体客户端

## Considered Options

1. 仅保留 JDBC
2. 仅保留 REST
3. 统一抽象后支持 JDBC / REST / 客户端三模式

## Decision

选择统一抽象后支持 JDBC、REST、客户端三模式。业务与路由层只依赖统一的执行抽象接口，由基础设施层根据目标环境、权限和故障状态切换具体模式。

## Consequences

### Positive

- 提高对 Hetu 部署形态和网络环境的适配性
- 为降级、回滚和影子验证提供更大空间
- 避免业务层被某一种接入方式锁死

### Negative

- 执行适配层复杂度上升
- 需要更完整的超时、重试、错误码和日志治理
- 测试矩阵扩大

### Neutral

- 不改变其他查询引擎的存在
- 不直接决定 SQL 优化服务的异步建议逻辑

## Compliance Impact

- 身份鉴别：执行前统一校验身份上下文
- 访问控制：目标数据源与模式选择都必须经过授权检查
- 安全审计：实际执行模式、降级路径和结果状态进入审计日志
- 加密存储：连接信息由公共管理服务加密保管
- 备份恢复：模式配置和连接元数据纳入恢复清单

## Implementation Notes

- 影响模块：查询执行服务、公共管理服务
- 对外暴露统一执行契约和错误码，不直接暴露 Hetu 客户端细节
- 需明确超时、重试、降级和回滚日志字段

## Validation

- 单元测试：模式选择、超时、错误映射
- 集成测试：三模式连通性和降级路径
- 构建验证：执行抽象编译与契约测试
- 文档一致性检查：部署与接口契约一致
- 合规自检：租户、权限、审计与敏感数据不泄漏

## Links

- 相关规则：`R-018`, `R-020`, `R-041`, `R-044`, `R-047`
- 相关计划：`docs/plans/master-execution-plan.md`
- 相关文档：`docs/architecture/init.md`, `docs/deployments/huawei-cloud-setup.md`
