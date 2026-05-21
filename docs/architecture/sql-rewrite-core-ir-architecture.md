# SQL 改写核心 IR 架构骨架

## Summary

本文固化 SQLForge 改写核心的五层中间表示骨架。该骨架用于后续把“复杂 SQL 文本改写”从单次规则判断升级为可分层推导的后端核心能力；本文件只声明架构骨架和兼容边界，不声明页面、数据库 schema、生产自动改写或真实 SQL 执行行为已经改变。

当前实现入口位于 `sql-optimization`：

- 领域模型包：`com.company.sqloptimization.domain.rewrite.ir`
- 应用转换入口：`SqlOptimizationPipelineService.buildRewriteCoreIr(...)`

## Layer Contract

| IR 层 | 领域对象 | 模式 | 说明 |
|:---|:---|:---|:---|
| `L1_AST` | `AstNodeReference` | 具体方言节点 | 记录 parser engine、dialect node kind、root kind 与 normalized SQL；当前支持以 JSqlParser / Calcite / Trino 解析结果作为来源，不把 AST 对象跨层暴露为可变状态。 |
| `L2_TABLE_REFERENCE` | `TableReferenceIr` | `{源, 别名, 访问路径, 谓词下推}` | 表示物理表、CTE、派生表、视图或未知来源；访问路径当前为静态解析证据，不代表真实执行计划。 |
| `L3_QUERY_BLOCK` | `QueryBlockIr` | `{输入, 输出, 谓词, 聚合}` | 表示 root query、CTE 与 subquery 单元；root block 复用结构解析的 projection、predicate、aggregation 与 groupBy evidence。 |
| `L4_RELATIONAL_ALGEBRA` | `RelationalAlgebraNode` | `σ, π, γ, ⋈, ∪, ∩, -` | 生成选择、投影、聚合、连接和集合运算的标准形节点，用于后续规则匹配和等价性验证准备。 |
| `L5_BUSINESS_INTENT` | `BusinessIntentIr` | `{时间锚点, 度量, 维度, 筛选}` | 从 time function、时间谓词、聚合、分组和过滤条件中推导业务意图草案；该层是静态启发式，不是人工确认后的业务真值。 |

## Boundary

- 不改动任何前端页面、路由、菜单或展示逻辑。
- 不执行用户 SQL，不访问生产数据，不创建 runtime rewrite binding。
- 不改变推荐记录、改写记录、激活/暂停、真实改写历史的既有产品边界。
- 不把静态 IR 推导写成真实收益、真实扫描量或生产执行事实。
- 不把 `L5_BUSINESS_INTENT` 直接作为业务口径审批结果；后续仍需人工复核、验证运行和审计链。

## Conflict Handling

本次发现一个命名冲突，但不构成实现阻断：

| 冲突 | 影响 | 选项 |
|:---|:---|:---|
| 既有 HARN-130 推荐规则模型已经使用 `L0/L1/L2` 表示规则等级；本次 IR 架构使用 `L1-L5` 表示改写核心层级。 | 如果共用 `level` 字段，推荐规则层级可能被误读为 IR 层级。 | 已选择兼容方案：IR 使用 `irLayerCode` 语义，即 `L1_AST` 至 `L5_BUSINESS_INTENT`；推荐 payload 继续使用 rule level `L0/L1/L2`。备选一：把本次架构改名为 `B1-B5`，但偏离用户给定规范。备选二：立刻重命名 HARN-130 字段，但会破坏现有推荐 payload 兼容。 |

## Next Extension Points

- L1 后续可保留 parser-specific node path，用于精确定位 Calcite `SqlNode` 或 JSqlParser `Expression`。
- L2 后续可接入真实 plan / metadata，补齐访问路径和可下推谓词证明。
- L3 后续可支持 CTE 展开、视图展开和 query block 之间的依赖图。
- L4 后续可成为 rewrite rule 的统一输入，减少直接在 SQL 文本或 parser AST 上散落判断。
- L5 后续可接入业务指标字典、时间口径、客户分层和报表模板，不再只依赖静态启发式。
