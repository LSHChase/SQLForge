# USER-CN-REWRITE-RECOMMENDATION-FINAL-OUTPUT-PHASE6-20260521 Raw Requirement

用户要求继续实现下一阶段内容，提升改写，不动页面，补充细节，实现功能。阶段主题为“改写推荐生成算法（最终输出）”。

## 5.1 推荐项结构

```plain
RewriteRecommendation {
  rewrite_id: UUID
  confidence: 0.95          // 基于等价验证严格程度
  category: STRUCTURAL_OPTIMIZATION
  severity: HIGH

  // 变更描述（面向开发者的 diff 视图）
  before_summary: "14次基础表扫描，12层嵌套子查询"
  after_summary: "1次基础表扫描，3层 CTE"

  // 详细变更（基于 RelNode 的规范化表示）
  transformations: [
    { type: MERGE, source: ["Sub34", "Sub35", "Sub36", "Sub37", "Sub38", "Sub39"], target: "metric_by_org" },
    { type: UNNEST, source: ["LEFT JOIN Sub40", "LEFT JOIN Sub41"], target: "growth_by_org" },
    { type: INLINE, source: "Sub3_分组和汇总_BASE", target: "raw_customer_snapshot" }
  ]

  // 等价性保证
  equivalence_proof: {
    method: STRUCTURAL_HASH + PREDICATE_SUBSUMPTION
    verified_dimensions: [ROW_COUNT, COLUMN_VALUES, AGGREGATION_RESULTS, NULL_HANDLING]
    edge_cases: ["branch_org_no = org_level2_no 时客户数跨层去重差异 — 已通过 UNION ALL 消除"]
  }

  // 性能预估
  performance: {
    scan_reduction: "14x → 1x"
    estimated_speedup: "5-10x (取决于数据量和集群规模)"
    memory_impact: "+20% (CASE 表达式和中间状态)"
    risk_level: LOW
  }

  // 可执行 SQL（通过 Calcite RelToSqlConverter 生成）
  executable_sql: "WITH ..."
}
```

## 5.2 推荐排序算法

```plain
排序因子:
  Score = α·performance_gain + β·confidence + γ·(1 - risk_level) + δ·readability_improvement

权重建议（金融级生产环境）:
  α = 0.4  // 性能优先
  β = 0.3  // 等价性确信度优先（避免数据错误）
  γ = 0.2  // 风险厌恶
  δ = 0.1  // 可维护性

过滤条件:
  - confidence < 0.9 的改写不自动应用，仅作提示
  - 涉及 COUNT DISTINCT 语义变更的，必须人工审核
  - 时间窗口类报表（如你的净增报表），改写前后需抽样比对 1-2 个机构的数据
```

## Implementation Boundary

- 本阶段不改动页面。
- 本阶段不执行真实 SQL，不读取生产数据。
- 本阶段不创建、激活或暂停 runtime rewrite binding。
- 当前 SQL 输出是基于 repo 内 RelNode surrogate 的静态模板，并显式标记 `STATIC_RELNODE_SURROGATE_NOT_REAL_CALCITE_RELTOSQL`；真实 Calcite `RelToSqlConverter` 接入需后续独立任务完成 schema、类型系统和 Hetu 方言验证。
