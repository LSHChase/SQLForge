# USER-CN-QBDAG-DECOMPOSITION-PHASE1-20260521 Raw Requirement

## Original User Input

接下来继续实现改写能力提升的实现，先实现第一阶段，别动页面，设计如下，请补充细节后实现功能：

```text
2.1 第一阶段：查询块分解与规范化（Query Block Decomposition）
目标：将扁平嵌套 SQL 还原为「查询块有向无环图」（QBDAG）
算法 2.1.1 子查询边界识别（基于 Calcite 的 SqlBasicCall 模式）
plain
复制

输入: Calcite SqlNode AST
输出: 查询块集合 QB = {qb₁, qb₂, ..., qbₙ}，每个 qbᵢ = (SelectList, FromClause, WhereClause, GroupBy, Having)

过程:
1. 深度优先遍历 AST，标记所有 SqlSelect 节点
2. 对每个 SqlSelect，提取其「外部引用」= 自由变量（非本作用域定义的列/表）
3. 构建引用关系图: qbᵢ → qbⱼ 当且仅当 qbᵢ 的 SelectList/WhereClause 中包含 qbⱼ 的输出列
4. 拓扑排序得到 QBDAG，检测并打破环（将关联子查询提升为 Lateral Join）

关键难点：BI 工具生成的 SQL 中，同一物理查询块被复制为多个别名（如 Sub3_分组和汇总_BASE 出现 N 次）。需要通过 结构哈希（Structural Hashing） 识别等价块。
算法 2.1.2 结构哈希（Structural Hashing for Query Blocks）
plain
复制

Hash(qb) = SHA256(规范化后的关系代数字符串)

规范化规则:
- 表别名替换为位置索引（$1, $2）
- 字面量常量替换为类型标记（<<STRING>, <NUMBER>, <DATE>）
- 列名替换为来源表索引+位置（$1.$3）
- 谓词按合取范式排序，消除 OR 通过分配律展开
- 聚合函数参数规范化（COUNT(DISTINCT x) → COUNT_DISTINCT($1.$2)）

碰撞处理: 若 Hash 冲突，启动语义等价验证（见 2.3.2）

应用：在你的截图 SQL 中，Sub3_分组和汇总_BASE、Sub21_分组和汇总_BASE 等 7 个不同别名的子查询，经结构哈希后发现是同一查询块——这是触发「子查询合并」改写的关键信号。
```

## Implementation Boundary Captured

- 不改动页面、路由、菜单或前端展示。
- 本阶段只做静态查询块分解、QBDAG、结构哈希和合并信号，不执行真实 SQL。
- 结构哈希命中只表示可进入后续子查询合并候选；真实合并、语义等价证明和自动生效仍属于后续阶段。
