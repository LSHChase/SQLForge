# PRW-003 实现改写记录审批状态机

## Source

- Primary plan: `docs/plans/production-rewrite-auto-apply-task-plan.md`
- Formal story: `E-STORY-015` 加速与改写治理工作台闭环
- Formal task matrix row: `docs/plans/task-spec-matrix.md#PRW-003`
- Governance extension row: `docs/plans/task-governance-extension-matrix.md#PRW-003`
- Ledger ref: `tasks.md#PRW-003`

## Objective

在 sql-optimization 中新增改写记录审批应用服务和 review API，支持批准、驳回或要求修改等状态迁移，并写入审批人、时间、意见和审计 trace。

## Contract

未审批或已驳回的改写记录不得发布运行时绑定，所有非法状态迁移必须由后端拒绝。

## Dependencies

`PRW-002`

## Write Boundary

- Tech: `JAVA-BE`, `SQL`, `DOCS`
- Layer: `sql-optimization`, `application(controller/service)/domain/infrastructure`, `tests`
- Environment: `dev/test/prod-doc`

## Human Confirmation Boundary

若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。

## Data Impact

更新改写记录审批状态和审计字段；不直接创建运行时绑定，不执行推荐 SQL。

## Validation

- `mvn -pl sql-optimization test`
- `python3 scripts/foreman.py validate PRW-003`
- `python3 scripts/foreman.py validate PRW-003`
- `python3 scripts/task_audit.py --check --phase pre-closeout`
- `git diff --check`

## Rollback / Recovery

停用审批接口并回退状态机服务；保留新增字段的保守默认状态，避免历史记录丢失。

## Residual Risk

- 最终权限与用户上下文字段需复用现有统一授权入口。

## Non-Goals

- 不实现投产前本地或测试环境核验闭环线。
- 不把 `manualReviewRequired` 当作审批通过。
- 不把加速计划审批当作 SQL 改写审批入口。
- 不绕过 SQLForge `preflight` / `validate` / `task_audit` / `closeout` 审计链。
