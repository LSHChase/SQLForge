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

## Phase 2.1 Query Block Decomposition

第一阶段已在后端新增 Query Block Decomposition 能力，用于把扁平嵌套 SQL 还原为查询块有向无环图 `QBDAG`。实现入口：

- 领域模型包：`com.company.sqloptimization.domain.rewrite.qbdag`
- 构建器：`QueryBlockDagBuilder`
- 应用入口：`SqlOptimizationPipelineService.buildQueryBlockDag(...)`
- IR 汇总入口：`RewriteCoreIrSnapshot.getQueryBlockDag()`

### Decomposition Contract

| 子能力 | 当前实现 | 边界 |
|:---|:---|:---|
| 查询块边界识别 | 优先用 Calcite `SqlNode` / `SqlBasicCall` 深度优先扫描所有 `SqlSelect`，生成 `ROOT`、`CTE`、`DERIVED_TABLE`、`SUBQUERY` 查询块。 | Calcite 无法解析 BI 方言时，不中断主流程，降级使用 `advancedStructureProfile` 中的 CTE/subquery 证据。 |
| 查询块字段 | 每个 `QueryBlockNode` 固化 `SelectList`、`FromClause`、`WhereClause`、`GroupBy`、`Having`、输出列、本地别名、外部引用。 | 字段来自静态 AST / profile，不代表执行计划或真实血缘。 |
| 引用图 | 生成 `CONTAINS`、`OUTPUT_REFERENCE`、`EXTERNAL_REFERENCE` 边；关联子查询造成 parent-child 环时，边标记为 `LATERAL_JOIN_PROMOTION`。 | `LATERAL_JOIN_PROMOTION` 是后续改写候选信号，不会自动改 SQL。 |
| 拓扑排序 | 对未被打破的边执行拓扑排序，输出 `topologicalOrder`；残留环记录 `QBDAG_UNRESOLVED_CYCLE`。 | 不隐式删除查询块，不伪造无环事实。 |
| 结构哈希 | 对规范化关系代数字符串计算 SHA-256，并产出 `QueryBlockHashGroup`。 | 哈希命中只表示结构等价候选；碰撞或规范形不一致时进入 `SEMANTIC_EQUIVALENCE_REQUIRED_2_3_2`。 |

### Structural Hash Rules

当前规范化覆盖：

- 表别名替换为位置索引，例如 `$1`、`$2`。
- 字面量替换为类型标记，例如 `<STRING>`、`<NUMBER>`、`<DATE>`、`<BOOLEAN>`、`<NULL>`。
- 列引用替换为来源表索引 + 块内列序号，例如 `$1.$3`。
- `AND` 谓词按原子排序；`OR` 谓词经分配展开后以排序后的可选合取项表示。
- 聚合表达式规范化，`COUNT(DISTINCT x)` 归一为 `COUNT_DISTINCT(...)`。

### Conflict / Choice

本阶段新增一个实现选择，但不构成需要暂停的业务冲突：

| 选择点 | 已采用方案 | 备选 |
|:---|:---|:---|
| 用户算法指定 Calcite AST，但现有 BI SQL 样本可能含 Calcite 不兼容方言。 | Calcite 是主路径；失败时用已存在的 JSQLParser `advancedStructureProfile` 做静态降级，保留 QBDAG 与结构哈希信号。 | 强制 Calcite 失败即失败；实现更纯粹，但会削弱复杂 BI SQL 的可用性。 |

### No Page / Runtime Impact

本阶段仍保持：

- 不改动前端页面、路由、菜单和展示文案。
- 不执行真实 SQL，不读取生产数据。
- 不创建、激活或暂停 runtime rewrite binding。
- 不把结构哈希重复块直接写成已完成子查询合并；该信号只进入后续 rewrite rule / 语义等价验证阶段。

## Phase 2.2 Relational Algebra Rewriting

第二阶段已在后端新增 L4 关系代数等价变换候选识别能力，用于在 `QBDAG` 和结构哈希基础上识别低效率模式，并生成静态、可审计的高效形式候选。实现入口：

- 领域模型包：`com.company.sqloptimization.domain.rewrite.ra`
- 构建器：`RelationalRewritePlanBuilder`
- 应用入口：`SqlOptimizationPipelineService.buildRelationalRewritePlan(...)`
- IR 汇总入口：`RewriteCoreIrSnapshot.getRelationalRewritePlan()`

### Rewriting Contract

| 子能力 | 当前实现 | 边界 |
|:---|:---|:---|
| 公共子表达式消除 | 对 `QBDAG.duplicateStructuralGroups` 中结构哈希一致、输出列数量等价的查询块生成 `CSE_ELIMINATION` 候选；选择谓词原子更少的查询块作为主副本，并在引用点列出补偿谓词。 | 谓词包含关系当前为静态合取原子比较；碰撞、范围语义和列别名语义仍需 2.3.2 语义等价验证后才能自动合并。 |
| 纵向折叠 | 对同源、同 `GROUP BY`、输出列数量等价的聚合查询块生成 `VERTICAL_FOLDING` 候选，候选形式使用 `CASE_AGGREGATION` 表达多指标聚合合并。 | 当前只生成候选和前置条件，不直接重写 SQL；`AVG` 拆解、`COUNT DISTINCT` 参数等价和重叠谓词策略必须在后续验证阶段确认。 |
| 横向展开消除 | 对同一父查询下多个 `LEFT JOIN` 聚合子查询，若 join key 静态等价、`GROUP BY` 等价且子查询之间无外部依赖，生成 `HORIZONTAL_UNNESTING` 候选。 | 源结构差异过大时仍应保留 Lateral Join / 原结构；当前候选只说明可下推为扩展 `GROUP BY` 的方向。 |

### Candidate Schema

`RelationalRewritePlan` 固化：

- `schemaVersion = relational-rewrite-plan/v1`
- `sourceSchemaVersion = query-block-dag/v1`
- `candidates`：包含规则类型、主查询块、源查询块、替代形式、补偿谓词、前置条件、语义风险、证据和静态收益估计。
- `unappliedRules`：记录未命中的规则，便于后续排查为什么某条 SQL 未触发候选。
- `attributes`：固定包含 `runtimeBoundary=NO_SQL_EXECUTION`、`pageImpact=NO_FRONTEND_PAGE_CHANGE`、`autoApplyAllowed=false`。

### Rule Semantics

| 规则 | 触发信号 | 输出 |
|:---|:---|:---|
| `CSE_ELIMINATION` | 结构哈希重复、输出列位置等价、谓词差异可列为补偿谓词。 | `CSE(master=..., replace=..., pushCompensationPredicatesAtReference=true)` |
| `VERTICAL_FOLDING` | 重复聚合块具有等价来源和等价 `GROUP BY`，指标或谓词不同。 | `VERTICAL_FOLD(source=..., groupBy=..., metrics=...)` |
| `HORIZONTAL_UNNESTING` | 父查询存在多个同级 `LEFT JOIN` 聚合右表，join key 和 `GROUP BY` 等价，子查询互不依赖。 | `HORIZONTAL_UNNEST(parent=..., joinKey=..., groupBy=..., metrics=...)` |

### Conflict / Choice

本阶段没有需要暂停实现的产品冲突；存在一个必须保守处理的工程选择：

| 选择点 | 已采用方案 | 备选 |
|:---|:---|:---|
| CSE、纵向折叠和横向展开都能推导出等价高效形式，但谓词包含、聚合可分解性和 `COUNT DISTINCT` 参数等价仍可能依赖真实语义。 | 只生成 `manualReviewRequired=true`、`autoApplyAllowed=false` 的静态候选，并保留补偿谓词、前置条件和语义风险。 | 直接改写 SQL；实现速度更快，但会在 2.3.2 语义等价验证未完成前扩大误改风险。 |

### No Page / Runtime Impact

本阶段仍保持：

- 不改动前端页面、路由、菜单和展示文案。
- 不执行真实 SQL，不读取生产数据。
- 不创建、激活或暂停 runtime rewrite binding。
- 不把候选收益写成真实扫描量或真实运行收益。
- 不自动合并子查询、不自动下推补偿谓词、不自动替换生产 SQL。
