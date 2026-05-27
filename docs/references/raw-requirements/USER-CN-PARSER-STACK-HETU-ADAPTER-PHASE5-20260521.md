# USER-CN-PARSER-STACK-HETU-ADAPTER-PHASE5-20260521 Raw Requirement Snapshot

## Source

User request on 2026-05-21.

## Requirement

继续实现下一阶段内容，提升改写，不懂页面，补充细节，实现功能。内容如下：

四、与 Calcite / historical legacy parser 的集成策略

### 4.1 双解析栈分工

| 解析器 | 职责 | 原因 |
|:---|:---|:---|
| Calcite | L1->L3 解析、L4 关系代数构建、优化器集成 | 强大的 SqlNode -> RelNode 转换、内置 HepPlanner/VolcanoPlanner |
| historical legacy parser | L1 语法细节提取、方言特定模式识别、原始 SQL 文本映射 | 对 MySQL/Oracle 方言支持更灵活，便于保留原始注释/格式 |

### 4.2 协同工作流

```plain
输入 SQL
  │
  ├─→ historical legacy parser ──→ 原始 AST (保留注释、格式、方言特征)
  │       │
  │       └─→ 模式扫描: 识别 BI 工具特定生成模式（如帆软的 "SubXX_分组和汇总" 别名）
  │           输出: 元数据标签 {tool: FANRUAN, version_hint, nested_depth}
  │
  └─→ Calcite ────→ SqlNode → RelNode (L4 关系代数)
          │
          ├─→ HepPlanner: 应用启发式规则（子查询去关联、谓词下推）
          │
          └─→ 输出: RelNode Tree + 代价估算

  融合层: 将 historical legacy parser 的元数据标签注入 Calcite RelNode 作为「改写约束」
          例如: 标记 "此子查询为 BI 工具生成的重复块，允许激进合并"
```

### 4.3 Hetu 执行计划适配

```plain
Hetu 特定优化点:
  1. CTE 物化策略: Hetu 支持 MATERIALIZED CTE，改写引擎应优先生成 WITH 语句
  2. Dynamic Filter: 在 JOIN 条件下推时，生成 Hetu 可识别的 dynamic_filter 提示
  3. 分区裁剪: 识别时间分区列（如 DTE），将范围条件转换为分区裁剪提示
  4. 分布式聚合: 避免单节点聚合瓶颈，改写为「预聚合 + 最终聚合」两阶段模式
```

## Execution Boundary

- 不改动前端页面。
- 不执行真实 SQL，不访问生产数据。
- 不调用真实 Hetu EXPLAIN、SqlToRelConverter、HepPlanner 或 VolcanoPlanner。
- 本阶段输出静态双解析栈融合报告和 Hetu 适配建议，作为后续真实 planner / engine validation 输入。
