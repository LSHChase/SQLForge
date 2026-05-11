# PRW-002 扩展改写记录审批与发布数据模型

## Source

- Primary plan: `docs/plans/production-rewrite-auto-apply-task-plan.md`
- Formal story: `E-STORY-015` 加速与改写治理工作台闭环
- Formal task matrix row: `docs/plans/task-spec-matrix.md#PRW-002`
- Governance extension row: `docs/plans/task-governance-extension-matrix.md#PRW-002`
- Ledger ref: `tasks.md#PRW-002`

## Objective

为 sql_rewrite_record 或等价改写记录模型新增审批、发布和运行时绑定追踪字段，并补齐 migration、entity、DTO、MyBatis 映射和仓储测试。

## Contract

改写记录必须独立持有 reviewStatus、publishStatus 和 runtime binding 追踪字段，manualReviewRequired 不得自动代表审批通过。

## Dependencies

`PRW-001`

## Write Boundary

- Tech: `JAVA-BE`, `SQL`, `DOCS`
- Layer: `sql-optimization`, `persistence`, `application(controller/service)/domain/infrastructure`, `docs`
- Environment: `dev/test/prod-doc`

## Human Confirmation Boundary

若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。

## Data Impact

新增兼容性数据库字段、映射和 DTO 字段；旧数据必须有保守默认状态，不迁移为已审批或已发布。

## Validation

- `mvn -pl sql-optimization test`
- `node scripts/lint-repository-knowledge.js`
- `python3 scripts/foreman.py validate PRW-002`
- `python3 scripts/foreman.py validate PRW-002`
- `python3 scripts/task_audit.py --check --phase pre-closeout`
- `git diff --check`

## Rollback / Recovery

通过兼容 DDL 或停用新增读写路径回退；保留旧改写记录查询能力，不删除历史记录。

## Residual Risk

- DDL 默认值和旧数据状态需要在实现时与现有 migration 体系核对。

## Non-Goals

- 不实现投产前本地或测试环境核验闭环线。
- 不把 `manualReviewRequired` 当作审批通过。
- 不把加速计划审批当作 SQL 改写审批入口。
- 不绕过 SQLForge `preflight` / `validate` / `task_audit` / `closeout` 审计链。
