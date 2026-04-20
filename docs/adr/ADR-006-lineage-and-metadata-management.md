# ADR-006: 数据血缘与元数据管理方案

- Status: Accepted
- Date: 2026-04-19
- Deciders: Human Architect, Codex Implementation Lead
- Technical Story: `Phase-D / 公共管理服务`
- Tags: lineage, metadata, graph

## Context

架构文档要求公共管理服务承载数据源管理、Schema 同步、血缘采集和影响分析。技术栈中已经预留 Neo4j/TuGraph 作为图数据库选项，而事务配置与查询检索仍以 MySQL 为主。

## Decision Drivers

- 元数据检索和血缘遍历的访问模式不同
- 需要支持表级、列级、SQL 指纹级的双向血缘查询
- 需要保留租户隔离和审计能力
- 必须避免把事务主库和图关系库职责混在一起

## Considered Options

1. 全部元数据和血缘都放 MySQL
2. 全部都放图数据库
3. MySQL 管理事务元数据，图数据库承载血缘关系

## Decision

选择“双存储职责分离”：

- MySQL 负责事务型元数据、配置、权限和检索索引
- 图数据库负责血缘关系、影响路径和多跳遍历

公共管理服务对外提供统一接口，不让上层直接依赖底层图数据库细节。

## Consequences

### Positive

- 元数据事务管理与血缘遍历各用最合适的存储
- 支持深度影响分析和可视化扩展
- 保持租户、审计和权限校验在统一管理域收口

### Negative

- 需要处理双写或异步同步复杂度
- 血缘重算与事务元数据之间存在一致性窗口
- 部署和运维组件增加

### Neutral

- 不改变查询执行和压测的主链路边界
- 前端只通过统一接口获取血缘结果

## Compliance Impact

- 身份鉴别：血缘查询仍需后端鉴权
- 访问控制：租户只能查看自己授权范围内的血缘图
- 安全审计：血缘重算、元数据同步和影响查询需审计
- 加密存储：数据源凭据仍只在事务元数据层加密保存
- 备份恢复：事务元数据和图数据库都需纳入恢复方案

## Implementation Notes

- 影响模块：公共管理服务、后续血缘子模块
- Schema 同步、血缘计算和影响分析可通过调度任务异步执行
- 需要统一节点/边抽象与租户标识

## Validation

- 单元测试：节点边模型、权限过滤、影响分析
- 集成测试：元数据同步到图关系、查询方向遍历
- 构建验证：服务编译与接口测试
- 文档一致性检查：接口契约、部署和运维文档一致
- 合规自检：跨租户血缘查询被拒绝并留痕

## Links

- 相关规则：`R-033`, `R-034`, `R-038`, `R-047`, `R-068`
- 相关计划：`docs/plans/master-execution-plan.md`
- 相关文档：`docs/architecture/init.md`, `docs/security/access-control-spec.md`
