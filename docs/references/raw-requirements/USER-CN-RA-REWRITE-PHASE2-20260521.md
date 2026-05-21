# USER-CN-RA-REWRITE-PHASE2-20260521 Raw Requirement

用户要求：继续按照仓库治理要求，实现下一个阶段，不要动页面，提升改写核心功能能力。

阶段内容：2.2 第二阶段：关系代数等价变换（Relational Algebra Rewriting）

目标：在 L4 层识别“低效率模式”并生成等价高效形式。

算法 2.2.1 公共子表达式消除（CSE Elimination / Subquery Deduplication）

模式识别：

- 若 QBDAG 中存在 `qb_i` 和 `qb_j` 满足 `Hash(qb_i) == Hash(qb_j)`。
- `qb_i.output_columns` 与 `qb_j.output_columns` 列名映射等价。
- `qb_i.predicate` 包含于 `qb_j.predicate` 或反之。

触发 CSE 合并：

1. 保留谓词更宽松的查询块作为主副本。
2. 对被替换查询块的引用者，将其谓词下推至引用点。
3. 若谓词不相同，在引用处增加补偿谓词。

应用示例：`Sub34(基期100万)` 和 `Sub37(当期100万)` 共享同一基础结构，仅日期谓词不同，可合并为 `single_scan`，日期条件通过 `CASE` 或 `GROUP BY` 区分。

算法 2.2.2 纵向折叠（Vertical Folding）：多指标聚合合并

模式识别：

- 多个聚合子查询 `qb_1...qb_n` 的 `FromClause` 相同或等价。
- `GroupBy` 列相同或等价。
- 仅 `SelectList` 中的聚合函数或筛选谓词不同。

变换规则：

```sql
SELECT g,
       COUNT(DISTINCT CASE WHEN p1 THEN x END) AS m1,
       COUNT(DISTINCT CASE WHEN p2 THEN x END) AS m2
FROM T
GROUP BY g
```

关键约束：

- 聚合函数必须满足可分解性；`COUNT` / `SUM` 可合并，`AVG` 需拆为 `SUM` / `COUNT`。
- `DISTINCT` 需保证参数 `x` 等价。
- 谓词可以互不重叠或允许重叠，但必须通过 `CASE` 内嵌条件控制。

应用示例：原 SQL 的 `Sub34` 至 `Sub39` 六个子查询各自包含完整的 `Sub6 UNION ALL Sub8` 链路；纵向折叠后，通过单次扫描和两层 `UNION ALL`，把所有日期 / AUM 维度的 `COUNT DISTINCT` 通过 `CASE` 内嵌条件一次计算。

算法 2.2.3 横向展开消除（Horizontal Unnesting）

模式识别：

- 父查询包含多个同级子查询作为 `LEFT JOIN` 右表。
- 所有子查询 join key 相同。
- 子查询输出为单一聚合值，或是每个 key 单行的 grouped aggregation。
- 子查询之间没有数据依赖。

变换规则：

- 将 `LEFT JOIN` 序列转换为来自合并源的 `GROUP BY key`，输出 `agg_1, agg_2, ...`。
- 若源结构差异过大，则保留 Lateral Join。

应用示例：`Sub34` 至 `Sub41` 的 8 个 `LEFT JOIN` 满足横向展开条件时，可合并为单次 `GROUP BY`。

实现边界：

- 本阶段不得改动页面。
- 本阶段生成静态 rewrite candidate，不自动执行真实 SQL。
- 生产自动改写仍需后续语义等价验证和审计链确认。
