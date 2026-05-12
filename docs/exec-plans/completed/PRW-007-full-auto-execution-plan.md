# PRW-007 在 query-execution 执行路径应用自动改写

## Source

- Primary plan: `docs/plans/production-rewrite-auto-apply-task-plan.md`
- Formal story: `E-STORY-015` 加速与改写治理工作台闭环
- Formal task matrix row: `docs/plans/task-spec-matrix.md#PRW-007`
- Governance extension row: `docs/plans/task-governance-extension-matrix.md#PRW-007`
- Ledger ref: `tasks.md#PRW-007`

## Objective

在 query-execution 执行入口按租户和 SQL 指纹查找 active 改写绑定，命中后使用已批准推荐 SQL 作为实际执行 SQL，并保留原始 SQL、实际执行 SQL、绑定 ID 和规则版本。

## Contract

只有 active 运行时绑定可触发自动改写；未命中、跨租户、指纹不匹配或暂停状态必须保持原 SQL 执行路径。

## Dependencies

`PRW-006`

## Write Boundary

- Tech: `JAVA-BE`, `SQL`, `DOCS`
- Layer: `query-execution`, `application service`, `domain`, `infrastructure`, `tests`
- Environment: `dev/test/prod-doc`

## Human Confirmation Boundary

若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。

## Data Impact

会改变命中绑定后的实际执行 SQL；必须保留原始 SQL 与绑定追踪，不改变未命中路径。

## Validation

- `mvn -pl query-execution,governance,sqlforge-shared -am test`
- `python3 scripts/foreman.py validate PRW-007`
- `python3 scripts/foreman.py validate PRW-007`
- `python3 scripts/task_audit.py --check --phase pre-closeout`
- `git diff --check`

## Rollback / Recovery

关闭绑定读取或将绑定置为 paused/unpublished，恢复原 SQL 执行路径。

## Residual Risk

- SQL 指纹一致性和异常降级策略需要与现有历史写入链核对。

## Non-Goals

- 不实现投产前本地或测试环境核验闭环线。
- 不把 `manualReviewRequired` 当作审批通过。
- 不把加速计划审批当作 SQL 改写审批入口。
- 不绕过 SQLForge `preflight` / `validate` / `task_audit` / `closeout` 审计链。
