# PRW-009 比对差异触发自动暂停与告警闭环

## Source

- Primary plan: `docs/plans/production-rewrite-auto-apply-task-plan.md`
- Formal story: `E-STORY-015` 加速与改写治理工作台闭环
- Formal task matrix row: `docs/plans/task-spec-matrix.md#PRW-009`
- Governance extension row: `docs/plans/task-governance-extension-matrix.md#PRW-009`
- Ledger ref: `tasks.md#PRW-009`

## Objective

扩展周期验证服务，在改写结果不等价或超过容忍阈值时自动暂停运行时绑定、更新改写记录发布状态，并写入告警、trace 和审计事件。

## Contract

周期比对发现差异后必须阻断后续自动改写，不得只停留在告警展示。

## Dependencies

`PRW-006`, `PRW-008`, `HARN-136`

## Write Boundary

- Tech: `JAVA-BE`, `SQL`, `DOCS`
- Layer: `sql-optimization`, `query-execution client`, `governance alert`, `scheduler`, `tests`
- Environment: `dev/test/prod-doc`

## Human Confirmation Boundary

若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。

## Data Impact

差异验证会暂停 active 绑定并改变发布状态；必须保留告警和验证运行证据。

## Validation

- `mvn -pl sql-optimization,query-execution,governance,sqlforge-shared -am test`
- `python3 scripts/foreman.py validate PRW-009`
- `python3 scripts/foreman.py validate PRW-009`
- `python3 scripts/task_audit.py --check --phase pre-closeout`
- `git diff --check`

## Rollback / Recovery

关闭自动暂停调度或回退暂停调用，恢复手动暂停路径；保留历史告警记录。

## Residual Risk

- 暂停调用失败时的补偿状态和告警语义需要重点验证。

## Non-Goals

- 不实现投产前本地或测试环境核验闭环线。
- 不把 `manualReviewRequired` 当作审批通过。
- 不把加速计划审批当作 SQL 改写审批入口。
- 不绕过 SQLForge `preflight` / `validate` / `task_audit` / `closeout` 审计链。
