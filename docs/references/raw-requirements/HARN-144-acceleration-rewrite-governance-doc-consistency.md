# HARN-144 Raw Requirement Snapshot

## Source

- Date: 2026-05-10
- Task: HARN-144
- Type: user-provided strict architecture review and documentation correction requirement

## User Requirement Excerpt

```text
作为一名资深的架构设计师，严格并全面复核下输入、输出、历史和文档，确认整个流程的完整性、合理性等，找出问题（丢失、遗漏、错误、偏离、不对等），然后给出优化方案，并优化下文档。如有需要人类确认的，优先让人类确认；如果没有，不要硬加内容。
```

## Normalized Scope

- 基于 `HARN-143` closeout 后的仓库真值，复核加速与改写治理方案、输入输出、历史记录、产品规格、接口基线、数据模型、主计划、任务矩阵、覆盖矩阵和任务台账。
- 只修正文档中的真实遗留歧义、遗漏和任务表达偏差。
- 不实现业务代码、不修改数据库 schema、不改变生产装数、物化视图、权限隔离、历史保留或外部环境默认依赖。
- 若复核发现需要改变业务边界或生产侧行为，必须先让人类确认；本轮未发现这类确认项。

## Findings To Preserve

- `HARN-143` 的核心流程完整、边界合理，无需新增业务范围。
- 推荐输出、SQL 历史改写记录和告警 payload 应与 `sourceKind` / `evidenceLevel` 证据分层一致。
- 产品方案的告警 payload 需要与接口基线对齐，保留 `sourceKind` 和 `planId`。
- 目标数据对象中的 `task_id` 命名不够明确，应拆分或明确为 `parse_task_id`、`optimization_task_id`、`benchmark_task_id` 等可追溯字段。
- `rewrite_validation_run` 应显式保留 `auto_apply_paused`，支撑周期比对暂停自动应用的审计。
- 后续实现依赖应从 `HARN-128 depends on HARN-143` 更新为 `HARN-128 depends on HARN-144`。
