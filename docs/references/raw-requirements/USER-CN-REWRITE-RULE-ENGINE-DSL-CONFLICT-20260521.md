# USER-CN-REWRITE-RULE-ENGINE-DSL-CONFLICT-20260521 Raw Requirement Snapshot

## Source

User request on 2026-05-21.

## Requirement

继续实现下一阶段内容，提升改写，不懂页面，补充细节，实现功能。内容如下：

三、关键数据结构：改写规则引擎

### 3.1 规则描述 DSL（领域特定语言）

规则模板结构:

```plain
Rule {
  id: "CSE-DEDUP-001"
  name: "CommonSubexpressionElimination"
  category: STRUCTURAL_REWRITE
  severity: HIGH

  pattern: {
    type: MULTIPLE_QUERY_BLOCKS
    relation: EQUIVALENT_HASH
    min_count: 2
    context: SAME_PARENT_BLOCK
  }

  preconditions: [
    { check: "output_columns_compatible", params: [IGNORE_ALIAS] },
    { check: "predicates_composable", params: [DISJOINT_OR_OVERLAPPING] },
    { check: "aggregation_decomposable", params: [COUNT, SUM, MIN, MAX] }
  ]

  actions: [
    { type: MERGE_BLOCKS, strategy: WIDEN_PREDICATE },
    { type: PUSH_DOWN, target: CASE_EXPRESSION, location: SELECT_LIST },
    { type: DEDUPLICATE, scope: WITHIN_PARENT }
  ]

  verification: {
    method: STRUCTURAL_HASH
    fallback: SMT_SOLVER
    null_semantics: IS_NOT_DISTINCT_FROM
  }

  cost_impact: {
    scan_reduction: MULTIPLICATIVE,
    memory_increase: ADDITIVE,
    risk: LOW
  }
}
```

### 3.2 规则冲突消解（Rule Conflict Resolution）

```plain
冲突类型:
  1. 互斥改写: R1 要求合并子查询 A+B，R2 要求拆分 A 为 A1+A2
  2. 顺序依赖: R3 必须在 R1 之后应用（R1 产生 R3 所需的前置模式）
  3. 代价矛盾: R4 减少扫描但增加内存，R5 减少内存但增加扫描

消解策略:
  - 构建规则依赖图（RDG），检测环（冲突信号）
  - 对无环 RDG，按拓扑序应用规则
  - 对有环/互斥场景，启动「局部搜索」：
      状态空间 = {已应用规则子集}
      转移 = 应用/撤销一条规则
      目标函数 = 预估代价 C
      算法 = 模拟退火或束搜索（Beam Search），宽度设为 3-5
```

## Execution Boundary

- 不改动前端页面。
- 不执行真实 SQL，不访问生产数据。
- 不自动应用生产改写。
- 本阶段输出静态规则 DSL、依赖图、冲突和排序报告，作为后续人工复核与验证链输入。
