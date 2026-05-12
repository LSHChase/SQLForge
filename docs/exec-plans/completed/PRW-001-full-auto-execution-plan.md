# PRW-001 固化生产改写闭环接口与状态契约

## Source

- Primary plan: `docs/plans/production-rewrite-auto-apply-task-plan.md`
- Formal story: `E-STORY-015` 加速与改写治理工作台闭环
- Formal task matrix row: `docs/plans/task-spec-matrix.md#PRW-001`
- Governance extension row: `docs/plans/task-governance-extension-matrix.md#PRW-001`
- Ledger ref: `tasks.md#PRW-001`

## Objective

更新生产自动改写闭环的产品与服务接口契约，明确推荐、人工复核标记、审批状态、发布状态和运行时生效状态的边界；不修改业务代码。

## Contract

文档必须明确 rewrite review / publish / runtime binding 状态机，以及审批入口不属于 acceleration plan 审批页。只覆盖生产自动改写闭环；不得把投产前核验闭环混入本任务。

## Dependencies

`HARN-142`, `HARN-145`

## Write Boundary

- Tech: `DOCS`, `OPS`
- Layer: `docs`, `architecture`, `product`
- Environment: `docs/dev`

## Human Confirmation Boundary

若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。

## Data Impact

仅影响文档、主计划、接口契约说明和任务治理记录；不修改运行时数据、数据库 schema 或接口实现。

## Validation

- `node scripts/lint-repository-knowledge.js`
- `python3 scripts/task_audit.py --check --phase pre-closeout`
- `git diff --check`
- `python3 scripts/foreman.py validate PRW-001`
- `python3 scripts/task_audit.py --check --phase pre-closeout`
- `git diff --check`

## Rollback / Recovery

回退本任务文档增量并保留后续任务不执行；若状态命名不合适，用追加文档修正替代覆盖历史。

## Residual Risk

- 后续实现仍需在代码阅读后确认最终字段命名和接口路径。

## Non-Goals

- 不实现投产前本地或测试环境核验闭环线。
- 不把 `manualReviewRequired` 当作审批通过。
- 不把加速计划审批当作 SQL 改写审批入口。
- 不绕过 SQLForge `preflight` / `validate` / `task_audit` / `closeout` 审计链。
