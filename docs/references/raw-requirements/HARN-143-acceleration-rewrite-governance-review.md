# HARN-143 Raw Requirement Snapshot

## Source

- Date: 2026-05-10
- Task: HARN-143
- Type: user-provided strict architecture review and documentation correction requirement

## User Requirement Excerpt

```text
作为一名资深的架构设计师，严格并全面复核下输入、输出、历史和文档，确认整个流程的完整性、合理性等，找出问题（丢失、遗漏、错误、偏离、不对等），然后给出优化方案，并优化下文档。如有需要人类确认的，优先让人类确认
```

## Normalized Scope

- 复核 `HARN-127` 加速与改写治理方案、历史输入、页面流程、接口基线、数据模型基线、主计划、任务矩阵和任务台账。
- 找出丢失、遗漏、错误、偏离和不对等问题，并将修正落到本地文档。
- 不实现业务代码、不改数据库 schema、不启动真实加速装数或物化视图流程。
- 若修正会改变业务边界、生产数据操作、权限隔离、历史保留策略或外部环境默认依赖，必须优先让人类确认。

## Review Findings To Preserve

- `APPLIED` 只能表示配置或绑定已写入，不能等同于产品态“生效”；默认运行时优先应用必须等待 `VERIFIED` / `ACTIVE`、结果等价、收益有效和 schema 未过期。
- 解析驱动与查询驱动不能只用粗粒度 `sourceType`；必须保留 `sourceKind`、`sourceId` 和 `evidenceLevel`，避免把解析证据、查询历史、EXPLAIN、benchmark 和人工输入混为一类。
- 静态解析、access parse、EXPLAIN、运行时历史和 benchmark 的证据等级不同，页面和接口不得把缺失证据显示为真实收益或 0 成本。
- `rewrite_validation_run` 的数据主责应在 `sql-optimization`；`query-execution` 与 `benchmark-engine` 只提供只读执行或压测证据。
- 加速治理工作台的真实接口 smoke 必须包含基线查询、加速后显式 `PREFER_ACCELERATED` 查询和回滚后查询，不能只验证 plan 生命周期。
- 改写结果差异、验证失败、加速收益回退和 artifact 失效必须进入告警契约。
- `HARN-128` 及后续实现任务必须依赖本复核修正版方案，而不是直接依赖未修正的 `HARN-127`。
