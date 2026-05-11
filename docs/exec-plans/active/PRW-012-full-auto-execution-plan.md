# PRW-012 生产闭环端到端测试与 smoke

## Source

- Primary plan: `docs/plans/production-rewrite-auto-apply-task-plan.md`
- Formal story: `E-STORY-015` 加速与改写治理工作台闭环
- Formal task matrix row: `docs/plans/task-spec-matrix.md#PRW-012`
- Governance extension row: `docs/plans/task-governance-extension-matrix.md#PRW-012`
- Ledger ref: `tasks.md#PRW-012`

## Objective

补齐生产自动改写闭环的后端端到端测试和前端 smoke，覆盖推荐生成、改写记录、审批、验证、发布、命中自动改写、历史留痕以及差异暂停。

## Contract

测试必须证明生产路径真的闭合，且不依赖投产前本地/测试环境核验闭环线。

## Dependencies

`PRW-009`, `PRW-010`, `PRW-011`

## Write Boundary

- Tech: `JAVA-BE`, `VUE-FE`, `OPS`, `DOCS`
- Layer: `tests`, `deployments/ci/scripts`, `frontend`, `backend`, `docs`
- Environment: `dev/test/prod-doc`

## Human Confirmation Boundary

若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。

## Data Impact

新增测试、smoke、runbook 或契约检查；不改变业务生产数据。

## Validation

- `mvn test`
- `npm run lint`
- `npm run build`
- `npm run smoke:acceleration-governance`
- `python3 scripts/foreman.py validate PRW-012`
- `python3 scripts/foreman.py validate PRW-012`
- `python3 scripts/task_audit.py --check --phase pre-closeout`
- `git diff --check`

## Rollback / Recovery

回退新增测试和 smoke 接线，不影响 PRW-001 至 PRW-011 的已实现业务能力。

## Residual Risk

- 全量 mvn test 可能受本地 JDK 或环境耗时影响，需要按仓库 JDK 8u112 规则记录证据。

## Non-Goals

- 不实现投产前本地或测试环境核验闭环线。
- 不把 `manualReviewRequired` 当作审批通过。
- 不把加速计划审批当作 SQL 改写审批入口。
- 不绕过 SQLForge `preflight` / `validate` / `task_audit` / `closeout` 审计链。
