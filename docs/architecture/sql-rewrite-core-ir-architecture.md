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

## Phase 2.3 Semantic Equivalence Verification

第三阶段已在后端新增候选级语义等价验证报告，用于在不执行 SQL、不访问数据的前提下，对第二阶段生成的关系代数改写候选输出证明状态、差表达式、NULL 语义、bag 语义和聚合统计等价证据。实现入口：

- 领域模型包：`com.company.sqloptimization.domain.rewrite.semantic`
- 验证器：`SemanticEquivalenceVerifier`
- 应用入口：`SqlOptimizationPipelineService.verifySemanticEquivalence(...)`
- IR 汇总入口：`RewriteCoreIrSnapshot.getSemanticEquivalenceReport()`

### Verification Contract

| 子能力 | 当前实现 | 边界 |
|:---|:---|:---|
| 基于约束的等价性检验 | 为每个 `RelationalRewriteCandidate` 生成 `CONSTRAINT_BASED_EQUIVALENCE` 检查，固化 `Q1 - Q2` 与 `Q2 - Q1` 的差表达式、双向空结果义务、输出 schema 等价义务和补偿谓词义务。 | 当前不接入真实 schema 约束、不执行差查询、不调用 SMT Solver；无 NOT NULL / CHECK / 主外键证据时状态保持 `NEEDS_CONSTRAINTS` 或 `CONDITIONALLY_PROVED`。 |
| NULL 语义 | 每个约束检查显式记录三值逻辑保持、可空列比较需使用 `IS NOT DISTINCT FROM`、可空输出列需要 schema 约束或补偿。 | 静态解析无法证明所有列的可空性；不会把未证明的 NULL 风险写成已证明等价。 |
| bag / set 语义 | 默认按 bag equivalence 记录重复值保持义务；CSE 要求引用替换不改变 multiplicity，横向展开要求 LEFT JOIN 行数不变。 | 当前不做真实基数估计，不声称扫描或行数收益已经发生。 |
| 聚合统计等价 | 对纵向折叠和横向展开候选生成 `STATISTICAL_AGGREGATION_EQUIVALENCE` 检查；固化 `COUNT(DISTINCT CASE WHEN p THEN x END) == COUNT(DISTINCT x) FILTER (WHERE p)` 等价律、CASE false 分支为 NULL、DISTINCT 参数等价和 AVG 拆解义务。 | `COUNT DISTINCT` / `AVG` / LEFT JOIN 空扩展仍需要后续约束、规则或执行验证闭环；本阶段不自动升级为可生产改写。 |

### Report Schema

`SemanticEquivalenceReport` 固化：

- `schemaVersion = semantic-equivalence-report/v1`
- `sourceSchemaVersion = relational-rewrite-plan/v1`
- `status`：`PROVED`、`CONDITIONALLY_PROVED`、`NEEDS_CONSTRAINTS`、`UNSUPPORTED` 或 `NO_CANDIDATE`
- `checks`：包含候选 id、规则类型、验证类型、原表达式、改写表达式、双向差表达式、前置条件、证明义务、NULL 语义、bag 语义、风险和证据。
- `unverifiedCandidateIds`：记录尚未达到 `PROVED` 的候选，避免误触发自动改写。
- `attributes`：固定包含 `runtimeBoundary=NO_SQL_EXECUTION`、`pageImpact=NO_FRONTEND_PAGE_CHANGE`、`autoApplyAllowed=false`、`smtSolverStatus=NOT_INTEGRATED`。

### Conflict / Choice

本阶段没有需要暂停实现的产品冲突；存在两个保守实现选择：

| 选择点 | 已采用方案 | 备选 |
|:---|:---|:---|
| 用户算法包含 SMT Solver / Z3 高级方法，但仓库当前没有 solver 依赖和真实 schema 约束输入。 | 先实现静态规则验证报告，显式标记 `smtSolverStatus=NOT_INTEGRATED`，并把需要主键、外键、NOT NULL、CHECK 的部分列入 proof obligations。 | 直接引入 Z3；证明能力更强，但会引入新依赖、构建环境和约束抽取任务，超出本阶段“先搭能力”的边界。 |
| 第二阶段只有候选形式，没有完整 rewritten SQL。 | 以候选级验证为边界，生成差表达式和等价义务，不声称完整 SQL 已被证明。 | 强行拼接完整 SQL；短期看更完整，但会在 SQL 生成器和语义验证都不完备时扩大误证明风险。 |

### No Page / Runtime Impact

本阶段仍保持：

- 不改动前端页面、路由、菜单和展示文案。
- 不执行真实 SQL，不读取生产数据。
- 不创建、激活或暂停 runtime rewrite binding。
- 不调用外部 SMT Solver，不新增数据库 schema 依赖。
- 不把 `CONDITIONALLY_PROVED` 或 `NEEDS_CONSTRAINTS` 候选升级为自动生产改写。

## Phase 2.4 Cost-Based Rewriting Selection

第四阶段已在后端新增候选级代价模型与改写排序能力，用于在第二阶段关系代数候选和第三阶段语义等价验证报告基础上，对多个静态等价改写方案生成可审计的抽象代价向量、帕累托前沿和 SLA 策略选择结果。实现入口：

- 领域模型包：`com.company.sqloptimization.domain.rewrite.cost`
- 选择器：`CostBasedRewriteSelector`
- 应用入口：`SqlOptimizationPipelineService.selectCostBasedRewrite(...)`
- IR 汇总入口：`RewriteCoreIrSnapshot.getCostBasedRewriteSelectionReport()`

### Cost Model Contract

| 子能力 | 当前实现 | 边界 |
|:---|:---|:---|
| 四维代价向量 | 为每个 `RelationalRewriteCandidate` 计算 `ScanCost`、`ShuffleCost`、`ComputeCost`、`MemoryCost` 和 `weightedCost`；输入来自 `QBDAG` 的查询块字段、谓词、聚合、JOIN、UNION 与候选补偿谓词。 | 代价值是 `ABSTRACT_STATIC_UNITS`，不是 Hetu EXPLAIN 成本、真实扫描字节或真实集群资源消耗。 |
| Hetu / Presto 解耦调整 | 对横向展开候选优先标记 `HETU_SHUFFLE_REDUCTION_PRIORITY`；对 CSE / 纵向折叠标记 `CTE_MATERIALIZATION_RECOMMENDED_FOR_REUSE`；有补偿谓词或多谓词时标记 `HETU_DYNAMIC_FILTER_PUSHDOWN_PREFERRED`。 | 调整只改变静态排序证据，不调用 Hetu optimizer，不生成 engine-native plan，不自动下推生产谓词。 |
| 帕累托前沿 | 按 `(Scan, Shuffle, Compute, Memory)` 四维坐标剔除被支配候选，并记录 `paretoFrontierCandidateIds`。 | 非前沿候选仍保留在报告中，便于人工看到被支配原因和候选证据。 |
| SLA 策略排序 | 支持 `DEFAULT_WEIGHTED`、`TIMEOUT_SENSITIVE`、`MEMORY_CONSTRAINED` 三种策略；默认按加权线性组合，超时敏感按最低扫描代价，内存紧张按最低内存代价。 | 策略只影响候选排序和 `selectedCandidateId`，不改变候选语义验证状态，不越过人工复核和生产改写治理链。 |

### Report Schema

`CostBasedRewriteSelectionReport` 固化：

- `schemaVersion = cost-based-rewrite-selection/v1`
- `sourceSchemaVersion = relational-rewrite-plan/v1`
- `strategy`：`DEFAULT_WEIGHTED`、`TIMEOUT_SENSITIVE` 或 `MEMORY_CONSTRAINED`
- `selectionStatus`：`NO_CANDIDATE`、`SELECTED_PROVED_CANDIDATE` 或 `RANKED_WITH_SEMANTIC_GATES`
- `selectedCandidateId`：当前策略下从帕累托前沿选出的候选；空值表示没有候选
- `paretoFrontierCandidateIds`：四维代价空间中未被支配的候选集合
- `estimates`：包含候选 id、规则类型、四维代价、是否帕累托最优、是否选中、排名、语义门禁、Hetu 调整、选择原因、证据和属性
- `weights`：当前策略的四维权重
- `attributes`：固定包含 `runtimeBoundary=NO_SQL_EXECUTION`、`pageImpact=NO_FRONTEND_PAGE_CHANGE`、`autoApplyAllowed=false`、`costModel=ABSTRACT_HETU_PRESTO_DECOUPLED`

### Selection Semantics

| 策略 | 权重 / 指标 | 适用场景 |
|:---|:---|:---|
| `DEFAULT_WEIGHTED` | `Scan=0.35`、`Shuffle=0.30`、`Compute=0.20`、`Memory=0.15` | 默认平衡扫描、网络、计算和内存。 |
| `TIMEOUT_SENSITIVE` | 从帕累托前沿选择最低 `ScanCost`，同时保留加权成本。 | 报表超时敏感，优先降低扫描压力。 |
| `MEMORY_CONSTRAINED` | 从帕累托前沿选择最低 `MemoryCost`，同时保留加权成本。 | 集群内存紧张，优先避开高内存 HASH / SORT 方案。 |

### Conflict / Choice

本阶段没有需要暂停实现的产品冲突；存在两个保守实现选择：

| 选择点 | 已采用方案 | 备选 |
|:---|:---|:---|
| 用户算法要求代价模型与具体执行引擎解耦，但 Hetu / Presto 的真实统计信息和 EXPLAIN 成本不在当前仓库闭环内。 | 使用查询块静态特征推导抽象代价单位，并显式写入 `STATIC_ABSTRACT_COST_NOT_REAL_EXECUTION_PLAN` 边界。 | 接入真实 Hetu EXPLAIN 或表统计；排序更贴近运行时，但会引入环境依赖并违反本阶段不执行 SQL 的边界。 |
| 多候选可能只有条件证明或需要约束证明。 | 代价报告保留 `semanticGate`，选择状态为 `RANKED_WITH_SEMANTIC_GATES` 时只表示候选排序，不表示可自动生产改写。 | 只允许 `PROVED` 候选进入排序；更保守，但当前静态验证阶段可能导致所有候选不可比较，削弱本阶段排序能力。 |

### No Page / Runtime Impact

本阶段仍保持：

- 不改动前端页面、路由、菜单和展示文案。
- 不执行真实 SQL，不读取生产数据。
- 不调用 Hetu EXPLAIN，不访问表统计元数据，不新增数据库 schema 依赖。
- 不创建、激活或暂停 runtime rewrite binding。
- 不把抽象代价值写成真实收益、真实扫描量、真实 Shuffle 字节或真实内存占用。
- 不把 `selectedCandidateId` 自动升级为生产改写；后续仍需完整语义验证、人工复核、压测和治理链。

## Phase 3.1 / 3.2 Rewrite Rule Engine DSL and Conflict Resolution

当前已在后端新增改写规则引擎关键数据结构，用于把第二阶段候选、第三阶段语义门禁和第四阶段代价排序继续组织为可审计的规则 DSL、规则依赖图和冲突消解报告。实现入口：

- 领域模型包：`com.company.sqloptimization.domain.rewrite.rule`
- 规则目录：`RewriteRuleCatalog`
- 冲突消解器：`RuleConflictResolver`
- 应用入口：`SqlOptimizationPipelineService.resolveRewriteRuleConflicts(...)`
- IR 汇总入口：`RewriteCoreIrSnapshot.getRuleConflictResolutionReport()`

### Rule DSL Contract

| 子能力 | 当前实现 | 边界 |
|:---|:---|:---|
| 规则描述 DSL | 固化 `RewriteRuleDefinition`、`RewriteRulePattern`、`RewriteRulePrecondition`、`RewriteRuleAction`、`RewriteRuleVerification` 与 `RewriteRuleCostImpact`；默认目录包含 `CSE-DEDUP-001`、`AGG-VFOLD-001` 和 `JOIN-HUNNEST-001`。 | DSL 是后端静态规则描述，不是外部可编辑配置中心；当前不新增数据库 schema、不开放页面编辑。 |
| `CSE-DEDUP-001` 模板 | 按用户规范落地 `CommonSubexpressionElimination`：`STRUCTURAL_REWRITE`、`HIGH`、`MULTIPLE_QUERY_BLOCKS`、`EQUIVALENT_HASH`、`SAME_PARENT_BLOCK`、输出列兼容、谓词可组合、聚合可分解、`MERGE_BLOCKS / PUSH_DOWN / DEDUPLICATE`、`STRUCTURAL_HASH`、`SMT_SOLVER` fallback 和 `IS_NOT_DISTINCT_FROM` NULL 语义。 | `SMT_SOLVER` 仍是声明式 fallback，当前不引入 solver 依赖、不执行证明器。 |
| 候选绑定 | 规则目录通过 `RelationalRewriteRuleType` 绑定已有 `RelationalRewriteCandidate`，使 DSL 报告能引用 CSE、纵向折叠和横向展开候选。 | 没有关系代数候选时不伪造规则命中；未命中规则保持在目录中，不写成已应用。 |

### Conflict Resolution Contract

| 子能力 | 当前实现 | 边界 |
|:---|:---|:---|
| 规则依赖图 RDG | 根据 DSL 中的 `mustRunAfterRuleIds` 与共享 QBDAG scope 构建 `RuleDependencyEdge`；使用 Kahn 拓扑排序输出 `topologicalOrder`，并把环检测结果写入报告。 | RDG 只组织静态规则顺序，不实际改写 SQL 文本。 |
| 冲突识别 | 记录三类冲突：`MUTUALLY_EXCLUSIVE_REWRITE`、`ORDER_DEPENDENCY` 和 `COST_CONTRADICTION`；有环时额外记录 `RDG_CYCLE`。 | 冲突是规则规划信号，不代表候选语义已经失败；最终仍需语义验证、人工复核和治理链。 |
| 局部搜索 | 对有环或互斥场景启动 `BEAM_SEARCH`，`beamWidth=4`，状态空间为已应用规则子集，转移为 `APPLY` / `SKIP`，目标函数为抽象预估代价 `C` 加依赖、冲突和语义门禁惩罚。 | Beam Search 只选择规则子集和排序建议，不自动应用生产改写。 |
| 无冲突顺序 | 无环且无互斥冲突时按拓扑序输出 `selectedRuleIds`。 | `selectedRuleIds` 是排序建议，不绕过 `autoApplyAllowed=false`。 |

### Report Schema

`RuleConflictResolutionReport` 固化：

- `schemaVersion = rewrite-rule-engine/v1`
- `sourceSchemaVersion`：优先引用 `cost-based-rewrite-selection/v1`
- `resolutionStatus`：`NO_RULE`、`TOPOLOGICAL_ORDER_READY`、`LOCAL_SEARCH_SELECTED_FOR_CONFLICT` 或 `LOCAL_SEARCH_SELECTED_FOR_RDG_CYCLE`
- `matchedRules`：命中的规则 DSL 定义
- `dependencyEdges`：RDG 边，包含 source / target / edge type / reason / evidence
- `conflicts`：冲突类型、规则集合、信号、消解策略和证据
- `topologicalOrder`：无环 RDG 的拓扑序
- `searchStates`：Beam Search 候选状态、已应用/跳过规则、目标成本、排名和是否选中
- `selectedRuleIds`：当前报告建议的规则子集或拓扑序
- `attributes`：固定包含 `runtimeBoundary=NO_SQL_EXECUTION`、`pageImpact=NO_FRONTEND_PAGE_CHANGE`、`autoApplyAllowed=false`、`dslStatus=STATIC_RULE_DSL_V1`

### Conflict / Choice

本阶段没有需要暂停实现的产品冲突；存在两个保守实现选择：

| 选择点 | 已采用方案 | 备选 |
|:---|:---|:---|
| 用户算法允许模拟退火或 Beam Search。 | 选择 Beam Search，宽度固定为 4，便于在单元测试和审计报告中保持确定性。 | 模拟退火可探索更多状态，但随机性会降低审计可复现性。 |
| 规则 DSL 是否允许页面或数据库配置。 | 当前只在后端代码目录固化默认规则，保证与已实现候选类型、语义验证和代价报告一致。 | 直接做动态规则配置会牵涉权限、版本、发布、回滚和页面，不属于本阶段“关键数据结构”闭环。 |

### No Page / Runtime Impact

本阶段仍保持：

- 不改动前端页面、路由、菜单和展示文案。
- 不执行真实 SQL，不读取生产数据。
- 不调用 SMT Solver，不新增数据库 schema，不开放动态规则配置。
- 不创建、激活或暂停 runtime rewrite binding。
- 不把 `selectedRuleIds` 或 Beam Search 最优状态自动升级为生产改写。

## Phase 4.1 / 4.2 / 4.3 Dual Parser Stack and Hetu Plan Adapter

当前已在后端新增 Calcite / JSqlParser 双解析栈融合报告，用于把 JSqlParser 的方言与 BI 工具元数据标签注入 Calcite L4 改写规划，并输出 Hetu 专属计划适配建议。实现入口：

- 领域模型包：`com.company.sqloptimization.domain.rewrite.parser`
- 融合分析器：`ParserStackFusionAnalyzer`
- 应用入口：`SqlOptimizationPipelineService.buildParserStackFusionReport(...)`
- IR 汇总入口：`RewriteCoreIrSnapshot.getParserStackFusionReport()`

### Parser Stack Contract

| 解析器 | 当前职责 | 边界 |
|:---|:---|:---|
| Calcite | 继续承担 QBDAG 的 L1-L3 查询块分解主路径，并把当前 L4 `RelationalAlgebraNode / RelationalRewritePlan` 作为 repo-closed 的 RelNode surrogate；报告中记录 `SQL_NODE`、`RELNODE_TREE`、`HEP_PLANNER`、`VOLCANO_PLANNER` 阶段。 | 当前不执行真实 `SqlToRelConverter`、`HepPlanner` 或 `VolcanoPlanner`，不把 L4 surrogate 写成真实 Calcite RelNode tree。 |
| JSqlParser | 继续承担 L1 语法细节、方言模式、别名、谓词、函数、原始 SQL 文本扫描和 advanced structure profile；报告中检测 BI 工具生成模式。 | 当前不改变既有结构解析响应字段，不保留可执行 AST 对象，不开放页面配置。 |

### Fusion Contract

| 子能力 | 当前实现 | 边界 |
|:---|:---|:---|
| 元数据标签 | `ParserMetadataTag` 当前识别帆软样式 `SubXX_分组和汇总` / `SubXX_*` 模式，输出 `{tool=FANRUAN, versionHint=ALIAS_PATTERN_SUBXX_GROUP_SUMMARY, nestedDepth, confidence}`。 | 标签来自静态文本和 advanced profile，不代表已确认的外部 BI 工具来源事实。 |
| 改写约束注入 | `RewriteConstraint` 将 BI 工具重复块标签转成 `BI_GENERATED_REPEATED_BLOCK_AGGRESSIVE_MERGE_ALLOWED`，并在纵向聚合形态上输出 `BI_AGGREGATION_BLOCK_VERTICAL_FOLDING_PREFERRED`。 | 约束只影响后续候选排序和人工复核证据，不跳过语义验证和生产治理链。 |
| Calcite planner 阶段 | 报告保留 `SQL_NODE`、`RELNODE_TREE`、`HEP_PLANNER` 和 `VOLCANO_PLANNER` 四段证据；HepPlanner 阶段声明子查询去关联、谓词下推，VolcanoPlanner 阶段引用抽象代价和规则冲突报告。 | `HEP_PLANNER` / `VOLCANO_PLANNER` 目前为 `PLANNED_NOT_EXECUTED`，用于记录集成策略和后续接入点。 |

### Hetu Adapter Contract

| 优化点 | 当前实现 | 边界 |
|:---|:---|:---|
| CTE 物化策略 | 对 QBDAG 重复结构或 CSE 候选输出 `HETU_MATERIALIZED_CTE`，建议优先生成 WITH 结构并验证 MATERIALIZED CTE 策略。 | 不生成生产 SQL，不验证具体 Hetu 语法。 |
| Dynamic Filter | 对 JOIN 图或横向展开候选输出 `HETU_DYNAMIC_FILTER_PUSHDOWN`，保留 `dynamic_filter` 提示文本和 join evidence。 | 不调用 Hetu optimizer，不证明提示一定被目标集群采纳。 |
| 分区裁剪 | 识别 `DTE / DT / BIZ_DATE / QUERY_DATE / DATE_DAY / DAY_ID` 等时间分区列及日期比较谓词，输出 `HETU_PARTITION_PRUNING`。 | 分区列识别是静态启发式，仍需表元数据确认。 |
| 分布式聚合 | 对聚合或 GROUP BY 形态输出 `HETU_TWO_PHASE_DISTRIBUTED_AGGREGATION`，建议预聚合 + 最终聚合两阶段模式。 | 不改写为真实两阶段 SQL，不验证单节点瓶颈是否真实存在。 |

### Report Schema

`ParserStackFusionReport` 固化：

- `schemaVersion = parser-stack-fusion/v1`
- `sourceSchemaVersion`：当前引用 `query-block-dag/v1`
- `fusionStatus`：`DUAL_STACK_REPORT_READY`、`HETU_HINTS_READY` 或 `METADATA_CONSTRAINTS_AND_HETU_HINTS_READY`
- `parserRoles`：Calcite 与 JSqlParser 分工说明
- `calcitePlannerStages`：SQL_NODE / RELNODE_TREE / HEP_PLANNER / VOLCANO_PLANNER 阶段证据
- `metadataTags`：JSqlParser 方言与 BI 工具标签
- `rewriteConstraints`：注入 Calcite L4 改写规划的静态约束
- `hetuPlanHints`：Hetu CTE、dynamic filter、分区裁剪和两阶段聚合提示
- `attributes`：固定包含 `runtimeBoundary=NO_SQL_EXECUTION`、`pageImpact=NO_FRONTEND_PAGE_CHANGE`、`autoApplyAllowed=false`、`hetuAdapterStatus=STATIC_PLAN_HINTS_ONLY`

### Conflict / Choice

本阶段没有需要暂停实现的产品冲突；存在两个保守实现选择：

| 选择点 | 已采用方案 | 备选 |
|:---|:---|:---|
| 用户算法要求 Calcite `SqlNode -> RelNode`、HepPlanner 和 VolcanoPlanner。 | 当前先用已有 QBDAG + L4 关系代数候选 + 抽象代价报告作为 repo-closed surrogate，并把真实 planner 状态显式标为 `PLANNED_NOT_EXECUTED`。 | 直接接入真实 Calcite RelNode 需要 schema catalog、类型系统和规则集校准，容易引入环境依赖并扩大阶段边界。 |
| Hetu 提示是否直接生成可执行 SQL。 | 当前只生成结构化 Hetu plan hints 和语法验证边界。 | 直接输出生产 SQL 会绕过完整 SQL 生成、语义验证、压测和治理链。 |

### No Page / Runtime Impact

本阶段仍保持：

- 不改动前端页面、路由、菜单和展示文案。
- 不执行真实 SQL，不读取生产数据。
- 不调用真实 Hetu EXPLAIN、Calcite `SqlToRelConverter`、HepPlanner 或 VolcanoPlanner。
- 不新增数据库 schema，不创建、激活或暂停 runtime rewrite binding。
- 不把静态 Hetu hints 写成真实执行计划或真实性能收益。
