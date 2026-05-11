# PRW-005 定义 query-execution 运行时改写绑定模型

## Source

- Primary plan: `docs/plans/production-rewrite-auto-apply-task-plan.md`
- Formal story: `E-STORY-015` 加速与改写治理工作台闭环
- Formal task matrix row: `docs/plans/task-spec-matrix.md#PRW-005`
- Governance extension row: `docs/plans/task-governance-extension-matrix.md#PRW-005`
- Ledger ref: `tasks.md#PRW-005`

## Objective

在 query-execution 中建立生产自动改写绑定模型、状态和仓储接口，支持 active/paused/unpublished 状态、租户+SQL 指纹唯一 active 绑定和规则版本追踪；本任务不改真实 SQL 执行逻辑。

## Contract

query-execution 必须成为生产自动改写运行时绑定的主闭环真值，JDBC Agent/Redis 不得作为唯一真值。

## Dependencies

`PRW-001`, `D-TASK-032`

## Write Boundary

- Tech: `JAVA-BE`, `SQL`, `DOCS`
- Layer: `query-execution`, `persistence`, `domain`, `infrastructure`, `tests`
- Environment: `dev/test/prod-doc`

## Human Confirmation Boundary

若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。

## Data Impact

新增运行时改写绑定持久化或等价存储结构；不改变当前 SQL 执行结果。

## Validation

- `mvn -pl query-execution test`
- `python3 scripts/foreman.py validate PRW-005`
- `python3 scripts/foreman.py validate PRW-005`
- `python3 scripts/task_audit.py --check --phase pre-closeout`
- `git diff --check`

## Rollback / Recovery

停用绑定仓储和服务入口，保持原 query-execution 执行路径不读取改写绑定。

## Residual Risk

- 绑定存储与现有 approved acceleration binding 的边界需在实现时避免混淆。

## Non-Goals

- 不实现投产前本地或测试环境核验闭环线。
- 不把 `manualReviewRequired` 当作审批通过。
- 不把加速计划审批当作 SQL 改写审批入口。
- 不绕过 SQLForge `preflight` / `validate` / `task_audit` / `closeout` 审计链。
