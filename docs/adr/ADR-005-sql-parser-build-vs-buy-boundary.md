# ADR-005: SQL 解析引擎自研范围与开源组件引用边界

- Status: Accepted
- Date: 2026-04-19
- Deciders: Human Architect, Codex Implementation Lead
- Technical Story: `Phase-D / 查询执行服务 + SQL优化服务`
- Tags: parser, calcite, druid, boundary

## Context

SQLForge 需要支持 SQL 解析、改写、风险识别、血缘抽取和优化建议。架构文档已列出 Apache Calcite、Druid SQL Parser 和 Presto/Trino Parser 作为参考生态，但未要求从零自研完整 parser。

## Decision Drivers

- 项目需要跨引擎、多方言的解析能力
- 自研完整 parser 成本高、维护风险大
- 业务价值在治理规则、改写策略、风险判断和编排，而非重复造轮子
- 需要保留可替换和可组合的解析能力

## Considered Options

1. 全量自研 parser
2. 只使用单一开源 parser
3. 复用开源 parser，围绕治理语义和编排做自研

## Decision

选择“开源 parser 复用 + 治理语义自研”的边界。底层语法树解析优先复用成熟组件；SQLForge 自研的部分集中在：

- 跨方言统一抽象
- 风险规则与审核语义
- 改写规则编排
- 成本估算与优化建议绑定
- 与路由、压测、加速、审计联动的治理逻辑

## Consequences

### Positive

- 降低基础 parser 的研发与维护成本
- 聚焦真正差异化的治理能力
- 有利于多引擎和多方言扩展

### Negative

- 需要处理不同 parser 之间的抽象差异
- 某些边角语法仍需定制适配
- 升级开源组件时需要回归验证

### Neutral

- 不改变前端和部署架构
- 不直接决定执行引擎选型

## Compliance Impact

- 身份鉴别：解析请求仍必须带身份上下文
- 访问控制：风险校验和资源授权独立于 parser 实现
- 安全审计：解析、改写与拦截决策需记录
- 加密存储：不涉及新增敏感配置存储模式
- 备份恢复：解析规则和元数据需入配置恢复清单

## Implementation Notes

- 影响模块：查询执行服务、SQL 优化服务
- 需定义统一 AST/语义对象，避免业务层直接依赖某个 parser API
- 解析异常要映射到统一错误码

## Validation

- 单元测试：方言解析、统一抽象转换、异常映射
- 集成测试：解析-改写-审核链路
- 构建验证：后端模块编译与测试
- 文档一致性检查：接口契约、风险规则、优化建议一致
- 合规自检：解析失败与高风险 SQL 拦截可审计

## Links

- 相关规则：`R-018`, `R-019`, `R-041`, `R-045`, `R-057`
- 相关计划：`docs/plans/master-execution-plan.md`
- 相关文档：`docs/architecture/init.md`
