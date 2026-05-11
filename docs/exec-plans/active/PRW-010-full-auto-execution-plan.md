# PRW-010 推荐中心与改写记录详情页面接入审批动作

## Source

- Primary plan: `docs/plans/production-rewrite-auto-apply-task-plan.md`
- Formal story: `E-STORY-015` 加速与改写治理工作台闭环
- Formal task matrix row: `docs/plans/task-spec-matrix.md#PRW-010`
- Governance extension row: `docs/plans/task-governance-extension-matrix.md#PRW-010`
- Ledger ref: `tasks.md#PRW-010`

## Objective

在推荐中心详情或改写记录详情增加审批、发布、暂停、撤销动作区，展示审批状态、发布状态、验证结果、发布资格和拒绝原因；不得把加速计划审批标注为改写审批入口。

## Contract

页面必须明确“审批通过但未发布”与“运行时已生效”的差异，并只调用后端真实接口。

## Dependencies

`PRW-003`, `PRW-004`, `PRW-006`, `HARN-145`

## Write Boundary

- Tech: `VUE-FE`, `DOCS`
- Layer: `frontend/router/views/styles`, `api client`, `tests`
- Environment: `dev/test/prod-doc`

## Human Confirmation Boundary

若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。

## Data Impact

仅改变前端交互和 API 调用，不直接修改持久化数据；审批/发布状态由后端接口写入。

## Validation

- `npm run lint`
- `npm run build`
- `node scripts/check-recommendation-page-contract.mjs`
- `python3 scripts/foreman.py validate PRW-010`
- `python3 scripts/foreman.py validate PRW-010`
- `python3 scripts/task_audit.py --check --phase pre-closeout`
- `git diff --check`

## Rollback / Recovery

回退页面动作区和 API client 变更，恢复推荐中心只读或既有工作流显示。

## Residual Risk

- 前端动作必须防止把 acceleration plan approval 误导为 rewrite review。

## Non-Goals

- 不实现投产前本地或测试环境核验闭环线。
- 不把 `manualReviewRequired` 当作审批通过。
- 不把加速计划审批当作 SQL 改写审批入口。
- 不绕过 SQLForge `preflight` / `validate` / `task_audit` / `closeout` 审计链。
