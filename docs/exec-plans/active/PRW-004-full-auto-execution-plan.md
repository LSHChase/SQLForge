# PRW-004 实现发布资格策略与验证门禁

## Source

- Primary plan: `docs/plans/production-rewrite-auto-apply-task-plan.md`
- Formal story: `E-STORY-015` 加速与改写治理工作台闭环
- Formal task matrix row: `docs/plans/task-spec-matrix.md#PRW-004`
- Governance extension row: `docs/plans/task-governance-extension-matrix.md#PRW-004`
- Ledger ref: `tasks.md#PRW-004`

## Objective

新增集中式发布资格策略服务，校验审批通过、等价验证通过、来源证据完整、自动应用允许、无未关闭差异告警或暂停标记、目标运行时校验通过，并返回结构化拒绝原因。

## Contract

所有发布接口必须调用同一个后端策略服务，页面只能展示策略结果，不能成为核心门禁。

## Dependencies

`PRW-003`, `HARN-135`

## Write Boundary

- Tech: `JAVA-BE`, `SQL`, `DOCS`
- Layer: `sql-optimization`, `application service`, `domain policy`, `tests`
- Environment: `dev/test/prod-doc`

## Human Confirmation Boundary

若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。

## Data Impact

读取验证、告警和改写记录状态并产生资格判断；不修改运行时绑定。

## Validation

- `mvn -pl sql-optimization test`
- `python3 scripts/foreman.py validate PRW-004`
- `python3 scripts/foreman.py validate PRW-004`
- `python3 scripts/task_audit.py --check --phase pre-closeout`
- `git diff --check`

## Rollback / Recovery

回退策略服务和发布入口调用，恢复为不可发布或只读状态。

## Residual Risk

- 部分验证证据来源可能需要适配现有 HARN-135/HARN-136 字段。

## Non-Goals

- 不实现投产前本地或测试环境核验闭环线。
- 不把 `manualReviewRequired` 当作审批通过。
- 不把加速计划审批当作 SQL 改写审批入口。
- 不绕过 SQLForge `preflight` / `validate` / `task_audit` / `closeout` 审计链。
