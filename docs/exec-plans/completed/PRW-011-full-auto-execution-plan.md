# PRW-011 SQL 历史页面展示改写前后链路

## Source

- Primary plan: `docs/plans/production-rewrite-auto-apply-task-plan.md`
- Formal story: `E-STORY-015` 加速与改写治理工作台闭环
- Formal task matrix row: `docs/plans/task-spec-matrix.md#PRW-011`
- Governance extension row: `docs/plans/task-governance-extension-matrix.md#PRW-011`
- Ledger ref: `tasks.md#PRW-011`

## Objective

在 SQL 历史列表和详情中展示自动改写状态、原始 SQL、实际执行 SQL、diff、改写记录、运行时绑定和规则版本，并提供跳转到改写记录详情的入口。

## Contract

SQL 历史页面展示必须来自 PRW-008 的后端审计字段，不得由前端推断改写是否发生。

## Dependencies

`PRW-008`, `PRW-010`, `HARN-145`

## Write Boundary

- Tech: `VUE-FE`, `DOCS`
- Layer: `frontend/router/views/styles`, `api client`, `tests`
- Environment: `dev/test/prod-doc`

## Human Confirmation Boundary

若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。

## Data Impact

仅改变历史页面展示和 API 消费，不改写历史持久化数据。

## Validation

- `npm run lint`
- `npm run build`
- `node scripts/check-history-page-contract.mjs`
- `node scripts/check-history-detail-contract.mjs`
- `python3 scripts/foreman.py validate PRW-011`
- `python3 scripts/foreman.py validate PRW-011`
- `python3 scripts/task_audit.py --check --phase pre-closeout`
- `git diff --check`

## Rollback / Recovery

回退历史页面新增列、详情 tab 和跳转入口，恢复既有 SQL 历史视图。

## Residual Risk

- 长 SQL diff 和移动端布局需要通过页面治理与截图自检验证。

## Non-Goals

- 不实现投产前本地或测试环境核验闭环线。
- 不把 `manualReviewRequired` 当作审批通过。
- 不把加速计划审批当作 SQL 改写审批入口。
- 不绕过 SQLForge `preflight` / `validate` / `task_audit` / `closeout` 审计链。
