# PRW-006 实现改写记录发布、暂停和撤销接口

## Source

- Primary plan: `docs/plans/production-rewrite-auto-apply-task-plan.md`
- Formal story: `E-STORY-015` 加速与改写治理工作台闭环
- Formal task matrix row: `docs/plans/task-spec-matrix.md#PRW-006`
- Governance extension row: `docs/plans/task-governance-extension-matrix.md#PRW-006`
- Ledger ref: `tasks.md#PRW-006`

## Objective

在 sql-optimization 中新增 publish/pause/unpublish 接口，调用发布资格策略和 query-execution 运行时绑定接口，并回写改写记录 publishStatus、runtimeBindingId、runtimeRuleVersion 等字段。

## Contract

发布、暂停、撤销必须保持改写记录状态与 query-execution 运行时绑定状态一致；失败不得留下半成功状态。

## Dependencies

`PRW-004`, `PRW-005`

## Write Boundary

- Tech: `JAVA-BE`, `SQL`, `DOCS`
- Layer: `sql-optimization`, `query-execution client`, `application(controller/service)`, `tests`
- Environment: `dev/test/prod-doc`

## Human Confirmation Boundary

若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。

## Data Impact

发布时创建或更新运行时绑定并回写改写记录发布字段；暂停/撤销会改变自动改写生效状态。

## Validation

- `mvn -pl sql-optimization,query-execution,sqlforge-shared -am test`
- `python3 scripts/foreman.py validate PRW-006`
- `python3 scripts/foreman.py validate PRW-006`
- `python3 scripts/task_audit.py --check --phase pre-closeout`
- `git diff --check`

## Rollback / Recovery

暂停或撤销已发布绑定；回退发布接口后保持改写记录为未发布或暂停状态。

## Residual Risk

- 跨服务调用失败补偿和重试边界需要严格测试。

## Non-Goals

- 不实现投产前本地或测试环境核验闭环线。
- 不把 `manualReviewRequired` 当作审批通过。
- 不把加速计划审批当作 SQL 改写审批入口。
- 不绕过 SQLForge `preflight` / `validate` / `task_audit` / `closeout` 审计链。
