# USER-CN-COST-BASED-REWRITE-PHASE4-20260521 Raw Requirement

用户要求：继续实现这个阶段内容，提升改写，不懂页面，补充细节，实现功能。

阶段内容：2.4 第四阶段：代价模型与改写排序（Cost-Based Rewriting Selection）

目标：当存在多种等价改写时，选择执行代价最低的方案。

算法 2.4.1 抽象代价模型（Abstract Cost Model for Hetu/Presto）

代价维度（与具体执行引擎解耦）：

```plain
C = w1 * ScanCost + w2 * ShuffleCost + w3 * ComputeCost + w4 * MemoryCost
```

各维度估算：

```plain
ScanCost    = sum(table_cardinality_i * column_selectivity_i)
ShuffleCost = sum(intermediate_row_count_j * row_width_j * network_factor)
ComputeCost = sum(operation_complexity_k * input_cardinality_k)
MemoryCost  = sum(hash_table_size_l + sort_buffer_size_l)
```

针对 Hetu 的特殊调整：

- 优先减少跨节点 Shuffle（Hetu 的分布式执行特性）。
- CTE 物化 vs 内联展开：基于引用次数和结果大小决策。
- 利用 Hetu 的 Dynamic Filter 下推能力，优先生成带谓词下推的计划。

算法 2.4.2 多目标帕累托优化（Pareto-Optimal Rewrite Selection）

场景：同一 SQL 存在 3-5 种等价改写方案，各有优劣：

- `R1`：扫描最少，但 Shuffle 多。
- `R2`：Shuffle 少，但内存占用大。
- `R3`：平衡方案。

决策算法：

1. 对每个候选改写 `Ri`，计算其在代价空间 `(Scan, Shuffle, Compute, Memory)` 的坐标。
2. 剔除被支配的改写：存在 `Rj` 在所有维度 `<= Ri` 且至少一维 `<`。
3. 对剩余帕累托前沿，结合业务 SLA 选择：
   - 查询超时敏感：选 `ScanCost` 最低的。
   - 集群内存紧张：选 `MemoryCost` 最低的。
   - 默认：使用加权线性组合 `C = sum(wi * cost_i)`。

实现边界：

- 本阶段不得改动页面。
- 本阶段生成静态、抽象、引擎解耦的代价选择报告，不执行真实 SQL。
- 代价值只表示静态估算单位，不得写成真实 Hetu 执行计划成本、真实扫描量或真实集群资源消耗。
- 选择结果只用于候选排序和人工复核，不自动创建、激活或暂停生产 runtime rewrite binding。
