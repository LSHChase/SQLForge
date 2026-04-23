# SQLForge Task Ledger

本文件是当前待办与执行中任务的唯一轻量运行台账。

- 状态只允许：`todo`、`in_progress`、`in_review`、`blocked`
- `done` 任务必须移入 `tasks-done.md`
- `blocked` 任务必须包含：`Next action:`、`Escalation:`、`Human decision:`、`INBOX ref:`
- `in_review` 任务必须包含：`Review reason:`、`Human decision:`、`INBOX ref:`
- `todo` / `in_progress` 任务不得保留未决人工判断、升级链字段或显式等待人类处理的标记
- 任务台账不替代 `docs/plans/master-execution-plan.md`

## Todo

_No tasks._


## In Progress

### OPS-GOV-002: 新增后端四服务一键启动脚本

- Status: in_progress
- Priority: 2
- Depends on: N/A
- Scope: scripts + local backend startup docs
- Validation:
  - `python3 scripts/foreman.py validate OPS-GOV-002`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
