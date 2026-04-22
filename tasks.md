# SQLForge Task Ledger

本文件是当前待办与执行中任务的唯一轻量运行台账。

- 状态只允许：`todo`、`in_progress`、`in_review`、`blocked`
- `done` 任务必须移入 `tasks-done.md`
- `blocked` 任务必须包含：`Next action:`、`Escalation:`、`Human decision:`、`INBOX ref:`
- `in_review` 任务必须包含：`Review reason:`、`Human decision:`、`INBOX ref:`
- `todo` / `in_progress` 任务不得保留未决人工判断、升级链字段或显式等待人类处理的标记
- 任务台账不替代 `docs/plans/master-execution-plan.md`

## Todo

### F-TASK-027: 收口 Phase-F 退出门禁缺口

- Status: in_progress
- Priority: 1
- Depends on: F-TASK-026
- Scope: Close the remaining Phase-F exit-gate blockers by wiring dedicated database-script executability checks, coverage-threshold enforcement, Sonar-required delivery mode, and stronger R-118 compliance evidence into the phase-gate workflow and closeout path.
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-027`


## In Progress

_No tasks._
