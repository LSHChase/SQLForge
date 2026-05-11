# PRW-008 补齐 SQL 执行历史的改写审计链

## Source

- Primary plan: `docs/plans/production-rewrite-auto-apply-task-plan.md`
- Formal story: `E-STORY-015` 加速与改写治理工作台闭环
- Formal task matrix row: `docs/plans/task-spec-matrix.md#PRW-008`
- Governance extension row: `docs/plans/task-governance-extension-matrix.md#PRW-008`
- Ledger ref: `tasks.md#PRW-008`

## Objective

扩展 SQL 执行历史写入、查询和详情 DTO，使历史能记录原始 SQL、实际执行 SQL、是否改写、改写记录 ID、运行时绑定 ID、规则版本和发布状态快照。

## Contract

历史接口必须以后端真实审计字段证明自动改写是否发生，前端不得自行推断 rewriteApplied。

## Dependencies

`PRW-007`, `HARN-134`

## Write Boundary

- Tech: `JAVA-BE`, `SQL`, `DOCS`
- Layer: `governance`, `query-execution`, `persistence`, `application(controller/service)`, `tests`
- Environment: `dev/test/prod-doc`

## Human Confirmation Boundary

若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。

## Data Impact

新增或扩展执行历史追溯字段；旧历史记录必须有兼容默认值。

## Validation

- `mvn -pl governance,query-execution,sqlforge-shared -am test`
- `python3 scripts/foreman.py validate PRW-008`
- `python3 scripts/foreman.py validate PRW-008`
- `python3 scripts/task_audit.py --check --phase pre-closeout`
- `git diff --check`

## Rollback / Recovery

保留旧历史展示路径，停止写入新增改写审计字段或将其置为默认未改写状态。

## Residual Risk

- 历史表字段、聚合接口和详情 DTO 需要保持向后兼容。

## Non-Goals

- 不实现投产前本地或测试环境核验闭环线。
- 不把 `manualReviewRequired` 当作审批通过。
- 不把加速计划审批当作 SQL 改写审批入口。
- 不绕过 SQLForge `preflight` / `validate` / `task_audit` / `closeout` 审计链。
