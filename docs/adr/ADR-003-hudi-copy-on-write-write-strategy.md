# ADR-003: Hudi Copy on Write 表类型与写入策略

- Status: Accepted
- Date: 2026-04-19
- Deciders: Human Architect, Codex Implementation Lead
- Technical Story: `Phase-D / 数据与存储`
- Tags: hudi, lakehouse, write-strategy

## Context

`docs/architecture/init.md` 已锁定 Hudi 0.14.0 且明确采用 Copy on Write（COW）模式。SQLForge 需要为 BI 查询、加速、历史治理和数据一致性提供稳定的读取语义，优先满足查询可预测性和结果一致性，而不是极致写入吞吐。

## Decision Drivers

- BI 场景优先读稳定性和可预测查询性能
- 加速、缓存一致性和审计回放依赖稳定快照
- 团队当前阶段更适合先降低读取复杂度
- 架构文档已明确 Hudi 采用 COW

## Considered Options

1. Hudi Copy on Write
2. Hudi Merge on Read
3. 不使用 Hudi，全部落传统数仓表

## Decision

选择 Hudi Copy on Write 作为当前数据湖表类型与默认写入策略。批量导入、预计算和加速结果优先以 COW 表形态落地，后续仅在有明确收益且 ADR 更新后再评估特定场景的 MOR。

## Consequences

### Positive

- 查询侧看到的是稳定列存快照
- 简化 BI、缓存一致性和加速结果校验
- 更易于围绕时间戳做缓存和增量判断

### Negative

- 写放大更高
- 高频更新场景成本增加
- 需要更谨慎地规划批量写入窗口

### Neutral

- 不影响 MySQL 事务主库定位
- 不替代压测、审计和管理域的数据存储职责

## Compliance Impact

- 身份鉴别：访问数据湖能力仍需经后端鉴权
- 访问控制：租户范围和数据源范围校验不能因 COW 被绕过
- 安全审计：写入与加速变更应进入审计日志
- 加密存储：敏感配置不进入 Hudi 明文
- 备份恢复：Hudi 数据与元数据需纳入恢复清单

## Implementation Notes

- 影响模块：查询执行服务、SQL 优化服务、加速能力
- 与 `ADR-011` 缓存一致性策略联动
- Bulk Insert 与物化视图刷新应优先围绕 COW 设计

## Validation

- 单元测试：时间戳与分区策略判断
- 集成测试：批量写入、查询快照一致性
- 构建验证：相关模块编译与契约测试
- 文档一致性检查：Hudi 选型与缓存策略文档一致
- 合规自检：审计和访问控制链路完整

## Links

- 相关规则：`R-031`, `R-042`, `R-043`, `R-069`
- 相关计划：`docs/plans/master-execution-plan.md`
- 相关文档：`docs/architecture/init.md`
