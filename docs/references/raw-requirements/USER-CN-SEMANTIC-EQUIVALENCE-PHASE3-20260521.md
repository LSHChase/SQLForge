# USER-CN-SEMANTIC-EQUIVALENCE-PHASE3-20260521 Raw Requirement

用户要求：继续实现下一阶段内容，提升改写，不动页面，补充细节并实现功能。

阶段内容：2.3 第三阶段：语义等价验证（Semantic Equivalence Verification）

目标：确保改写后的 SQL 与原 SQL 在任何数据实例下返回相同结果，需要考虑 NULL、空集和重复值。

算法 2.3.1 基于约束的等价性检验（Constraint-Based Equivalence）

形式化定义：

原查询 `Q1` 与改写查询 `Q2` 等价，当且仅当：

```plain
∀ 数据库实例 D, ∀ 约束集 C（主键、外键、NOT NULL、CHECK）:
  Q1(D) = Q2(D)
```

等价可以是集合等价或包等价，取决于查询语义。

简化检验方法：

1. 将 `Q1` 和 `Q2` 转换为 L4 关系代数表达式 `E1`、`E2`。
2. 构建差查询 `Δ = E1 - E2` 和 `Δ' = E2 - E1`。
3. 对 `Δ` 和 `Δ'` 进行空结果判定：
   - 若 `Δ` 的输出列均受 NOT NULL 约束，且 `Δ` 的谓词恒假，则等价。
   - 若存在 NULL 可能，需扩展为 `IS NOT DISTINCT FROM` 语义比较。

高级方法：

- 将关系代数谓词编码为 SMT-LIB 公式。
- 使用 Z3 等求解器验证谓词永假性。

算法 2.3.2 统计等价性（Statistical Equivalence for Aggregation）

针对聚合查询的特殊处理：

- `COUNT` / `SUM` 等价性依赖于重复值消除的一致性。
- 若原 SQL 使用 `COUNT(DISTINCT x)` 而改写为 `COUNT(CASE WHEN...)`，需验证 CASE 条件不会引入额外 NULL 或重复计数。

验证规则：

```sql
COUNT(DISTINCT CASE WHEN p THEN x END) == COUNT(DISTINCT x) FILTER (WHERE p)
```

成立条件：

- `p` 为真时 `x` 非 NULL；或
- NULL 值在 DISTINCT 中被正确处理。

实现边界：

- 本阶段不得改动页面。
- 本阶段生成候选级语义等价验证报告，不自动执行真实 SQL。
- 未接入 solver 或真实 schema 约束时，不得把条件证明写成生产自动改写已安全。
