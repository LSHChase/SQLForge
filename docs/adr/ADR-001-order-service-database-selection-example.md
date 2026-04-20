# ADR-001: 订单服务数据库选型示例

- Status: Accepted
- Date: 2026-04-19
- Deciders: Human Architect, Codex Implementation Lead
- Technical Story: `Phase-B / B-TASK-004`
- Tags: metadata, transactional-store, mysql, reference

## Context

该 ADR 以“订单服务数据库选型示例”为历史标题保留，用于沉淀 SQLForge 在事务型元数据域上的数据库选型原则。项目当前的核心事务数据包括租户配置、数据源、审计日志、调度元数据、导出记录与消息补偿状态，均要求强一致、结构化查询、分页筛选、可追溯关联键和成熟运维能力。

`docs/architecture/init.md` 已锁定 MySQL/TDSQL 为主持久化方向，且 `R-031`、`R-033`、`R-034`、`R-036`、`R-037`、`R-038` 要求历史、结果、导出、审计能够长期保留、可筛选、可导出、可追溯。

## Decision Drivers

- 事务型元数据需要 ACID 与结构化查询
- 审计、历史、导出和配置管理高度依赖 SQL 查询与索引
- 团队熟悉 Java + Spring Boot + MyBatis + MySQL 生态
- 运维环境已规划 MySQL/TDSQL 主从、高可用与备份
- 规则要求主持久化方向明确，不能让浏览器状态成为唯一事实来源

## Considered Options

1. MySQL 8.0 / TDSQL 作为事务主库
2. 文档型 NoSQL 作为主库
3. 图数据库或对象存储兼任事务主库

## Decision

选择 MySQL 8.0（或华为云 TDSQL 等效兼容方案）作为 SQLForge 事务型元数据和管理面数据的主存储。

本决策适用于以下域：

- 租户与权限配置
- 数据源与连接元数据
- 审计日志索引与检索元数据
- 查询历史、导出记录、调度元数据
- `R-144` 数据库模式消息表

图数据库、对象存储、Kafka 和 Hudi 只承担各自专门职责，不替代事务主库。

## Consequences

### Positive

- 满足事务、一致性、关联查询、分页筛选和导出需求
- 与 MyBatis XML、审计检索和多环境配置天然兼容
- 便于实现租户隔离、历史永久保留和可追溯关联键

### Negative

- 需要严格控制索引、冷热分层和归档策略
- 大体量审计与历史数据需要配套归档和分区策略
- 高吞吐写入场景需要通过异步链路和批量策略减压

### Neutral

- 不影响 Hudi、Kafka、Neo4j/TuGraph 等专门型存储的引入
- 不改变前后端分离和微服务边界本身

## Compliance Impact

- 身份鉴别：认证后的用户上下文进入事务写库前仍需后端校验
- 访问控制：租户 ID 需作为主查询与关联键的一部分
- 安全审计：审计日志索引元数据保存在事务库，正文可分层归档
- 加密存储：密码、Token、密钥类字段加密后入库
- 备份恢复：主从复制、全量备份、binlog/增量恢复围绕 MySQL 建立

## Implementation Notes

- 影响模块：`governance-service`、后续 4 个微服务的管理面与事务域
- 数据访问方式：MyBatis XML
- 关键表：租户、数据源、审计、导出、消息队列、调度元数据
- 后续需建立冷热分层、审计归档和备份恢复文档

## Validation

- 单元测试：Repository 和转换逻辑测试
- 集成测试：MyBatis 映射、分页筛选、事务回滚
- 构建验证：`mvn -B test`
- 文档一致性检查：`docs/architecture/init.md`、`docs/security/compliance.md`
- 合规自检：`R-111` 至 `R-115`

## Links

- 相关规则：`R-031`, `R-033`, `R-034`, `R-036`, `R-037`, `R-038`, `R-065`
- 相关计划：`docs/plans/master-execution-plan.md`
- 相关文档：`docs/architecture/init.md`, `docs/security/compliance.md`
