# PRW-013 JDBC Agent Redis 改写规则桥接

## Source

- Primary plan: `docs/plans/production-rewrite-auto-apply-task-plan.md`
- Formal story: `E-STORY-015` 加速与改写治理工作台闭环
- Formal task matrix row: `docs/plans/task-spec-matrix.md#PRW-013`
- Governance extension row: `docs/plans/task-governance-extension-matrix.md#PRW-013`
- Ledger ref: `tasks.md#PRW-013`

## Objective

在生产主闭环完成后，按需把已发布的 query-execution 运行时改写绑定同步到 JDBC Agent 现有 Redis 改写规则格式，并处理同步失败、过期、撤销和版本覆盖策略。

## Contract

JDBC Agent/Redis 只能作为已发布运行时绑定的兼容出口，不能替代 query-execution 主闭环真值。

## Dependencies

`PRW-012`

## Write Boundary

- Tech: `JAVA-BE`, `REDIS`, `OPS`, `DOCS`
- Layer: `query-execution`, `jdbc-agent`, `infrastructure`, `tests`, `docs`
- Environment: `dev/test/prod-doc/test-env`

## Human Confirmation Boundary

若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。

## Data Impact

同步 Redis 改写规则 key；主闭环状态仍以 query-execution 绑定为准，同步失败必须告警而非篡改主状态。

## Validation

- `mvn -pl query-execution,sqlforge-shared -am test`
- `python3 scripts/foreman.py validate PRW-013`
- `python3 scripts/foreman.py validate PRW-013`
- `python3 scripts/task_audit.py --check --phase pre-closeout`
- `git diff --check`

## Rollback / Recovery

停用 Redis 同步适配并删除或标记无效的 Redis key，保持 query-execution 主绑定不变。

## Residual Risk

- 真实 Redis 环境证据可能属于 environment-backed follow-up，不能成为 PRW-001 至 PRW-012 的阻断。

## Non-Goals

- 不实现投产前本地或测试环境核验闭环线。
- 不把 `manualReviewRequired` 当作审批通过。
- 不把加速计划审批当作 SQL 改写审批入口。
- 不绕过 SQLForge `preflight` / `validate` / `task_audit` / `closeout` 审计链。
