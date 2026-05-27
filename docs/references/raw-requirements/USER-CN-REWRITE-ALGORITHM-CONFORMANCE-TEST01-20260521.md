# USER-CN-REWRITE-ALGORITHM-CONFORMANCE-TEST01-20260521 Raw Requirement

用户要求作为资深架构设计师及软件开发程序员，基于以下核心算法脉络确认当前改写逻辑是否符合，如不符合则继续补充细节、实现功能，并最后使用 `docs/test01.sql` 复测。

## Core Algorithm Path

```plain
输入: 病态 SQL（BI 工具生成 / 手写复杂查询）
  │
  ▼
[解析] Calcite SqlNode + historical legacy parser AST → 双栈融合
  │
  ▼
[分解] 查询块 DAG + 结构哈希 → 识别重复/冗余模式
  │
  ▼
[识别] 模式匹配引擎 → 命中规则库 {CSE, VerticalFold, HorizontalUnnest, ...}
  │
  ▼
[变换] 关系代数等价重写 → 生成候选改写集合
  │
  ▼
[验证] 结构哈希 + SMT 求解 + 统计等价性 → 过滤不安全改写
  │
  ▼
[择优] 帕累托代价模型 → 选择最优改写
  │
  ▼
[生成] RelToSqlConverter → 可执行 SQL + 推荐报告
  │
  ▼
输出: {改写 SQL, 等价性证明, 性能预估, 风险评级}
```

## Implementation Boundary

- 本阶段不改动页面。
- 本阶段不执行真实 SQL，不读取生产数据。
- 本阶段不创建、激活或暂停 runtime rewrite binding。
- 本阶段新增算法链路一致性报告，用于逐段说明当前实现与理想算法的符合度，并显式暴露真实 Calcite RelNode、外部 SMT Solver 与真实 Calcite `RelToSqlConverter` 尚未接入的边界。
- 本阶段必须使用 `docs/test01.sql` 完成复测。
