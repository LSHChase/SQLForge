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

### F-TASK-030: 提升覆盖率并补齐 Sonar 发布环境

- Status: in_progress
- Priority: 1
- Depends on: `F-TASK-029`
- Scope: 把 phase1plus 聚合覆盖率提升到 85%+，补齐 Sonar 所需 secrets / 发布环境接线，并验证自动 release gate 可稳定放行 Tech: `OPS`,`DOCS`,`JAVA-BE`. Layer: `deployments/ci/scripts`,`docs`,`application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-F / Story `F-STORY-005` 发布门禁自动化与稳定性收口
- Human confirmation point: 若 coverage 提升方案会删除既有测试、放宽 85% 门槛，或 Sonar 发布环境接线涉及敏感 secrets 管理策略调整需人工确认
- Data impact: 覆盖率结果、CI/release 环境变量、Sonar 扫描结果与相关测试资产
- Rollback / recovery: 恢复到当前自动阻断发布路径，保留覆盖率 / Sonar 失败证据，并回退新增测试或 workflow 环境接线
- Validation:
  - `bash scripts/run-coverage.sh --phase phase1plus`、`bash scripts/run-sonar.sh --require-config`、release gate workflow / phase gate 验证
  - `python3 scripts/foreman.py validate F-TASK-030`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
