# SQLForge Task Ledger

本文件是当前待办与执行中任务的唯一轻量运行台账。

- 状态只允许：`todo`、`in_progress`、`in_review`、`blocked`
- `done` 任务必须移入 `tasks-done.md`
- `blocked` 任务必须包含：`Next action:`、`Escalation:`、`Human decision:`、`INBOX ref:`
- `in_review` 任务必须包含：`Review reason:`、`Human decision:`、`INBOX ref:`
- `todo` / `in_progress` 任务不得保留未决人工判断、升级链字段或显式等待人类处理的标记
- 任务台账不替代 `docs/plans/master-execution-plan.md`

## Todo

_No tasks._


## In Progress

### PRW-013: JDBC Agent Redis 改写规则桥接

- Status: in_progress
- Priority: 1
- Depends on: `PRW-012`
- Scope: JDBC Agent/Redis 只能作为已发布运行时绑定的兼容出口，不能替代 query-execution 主闭环真值。 Tech: `JAVA-BE`,`REDIS`,`OPS`,`DOCS`. Layer: `query-execution`,`jdbc-agent`,`infrastructure`,`tests`,`docs`.
- Plan ref: docs/exec-plans/active/PRW-013-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-015` 加速与改写治理工作台闭环
- Human confirmation point: 若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。
- Data impact: 同步 Redis 改写规则 key；主闭环状态仍以 query-execution 绑定为准，同步失败必须告警而非篡改主状态。
- Rollback / recovery: 停用 Redis 同步适配并删除或标记无效的 Redis key，保持 query-execution 主绑定不变。
- Validation:
  - `mvn -pl query-execution,sqlforge-shared -am test、python3 scripts/foreman.py validate PRW-013`
  - `python3 scripts/foreman.py validate PRW-013`
- Progress log:
  - 2026-05-11: instantiated from foreman CLI using repository truth and task matrices.

### PRW-012: 生产闭环端到端测试与 smoke

- Status: in_progress
- Priority: 1
- Depends on: `PRW-009`,`PRW-010`,`PRW-011`
- Scope: 测试必须证明生产路径真的闭合，且不依赖投产前本地/测试环境核验闭环线。 Tech: `JAVA-BE`,`VUE-FE`,`OPS`,`DOCS`. Layer: `tests`,`deployments/ci/scripts`,`frontend`,`backend`,`docs`.
- Plan ref: docs/exec-plans/active/PRW-012-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-015` 加速与改写治理工作台闭环
- Human confirmation point: 若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。
- Data impact: 新增测试、smoke、runbook 或契约检查；不改变业务生产数据。
- Rollback / recovery: 回退新增测试和 smoke 接线，不影响 PRW-001 至 PRW-011 的已实现业务能力。
- Validation:
  - `mvn test、npm run lint、npm run build、npm run smoke:acceleration-governance、python3 scripts/foreman.py validate PRW-012`
  - `python3 scripts/foreman.py validate PRW-012`
- Progress log:
  - 2026-05-11: instantiated from foreman CLI using repository truth and task matrices.

### PRW-001: 固化生产改写闭环接口与状态契约

- Status: in_progress
- Priority: 1
- Depends on: `HARN-142`,`HARN-145`
- Scope: 文档必须明确 rewrite review / publish / runtime binding 状态机，以及审批入口不属于 acceleration plan 审批页。只覆盖生产自动改写闭环；不得把投产前核验闭环混入本任务。 Tech: `DOCS`,`OPS`. Layer: `docs`,`architecture`,`product`.
- Plan ref: docs/exec-plans/active/PRW-001-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-015` 加速与改写治理工作台闭环
- Human confirmation point: 若实现需要绕过人类审批、等价验证、租户隔离、审计留痕、发布资格策略，或把投产前核验闭环混入生产闭环验收，必须暂停并回到人工确认。
- Data impact: 仅影响文档、主计划、接口契约说明和任务治理记录；不修改运行时数据、数据库 schema 或接口实现。
- Rollback / recovery: 回退本任务文档增量并保留后续任务不执行；若状态命名不合适，用追加文档修正替代覆盖历史。
- Validation:
  - `node scripts/lint-repository-knowledge.js、python3 scripts/task_audit.py --check --phase pre-closeout、git diff --check`
  - `python3 scripts/foreman.py validate PRW-001`
- Progress log:
  - 2026-05-11: instantiated from foreman CLI using repository truth and task matrices.


## Blocked

### HARN-016: Track deferred external Hetu/MRS validation

- Status: blocked
- Priority: 1
- Depends on: `D-TASK-017`, `D-TASK-018`, `HARN-013`, `HARN-014`
- Scope: Record that external Win10 test-environment Hetu/MRS validation is deferred while repository-side implementation proceeds, and wire the pending follow-up into tasks/INBOX/plan audit chain without changing business code.
- Validation:
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: converted the environment-backed Hetu/MRS verification wait into an explicit blocked governance follow-up so repository-side implementation can continue without treating external test-environment latency as an active coding blocker.
  - 2026-05-08: HARN-088 added repository-side Hetu EXPLAIN plan-analysis support; live JDBC evidence for real Hetu/MRS credentials remains part of this blocked environment-backed validation chain.
- Next action: When the Win10 test environment is ready, deploy the yml-based governance/query-execution configuration from the runbook, run `bash scripts/run-hetu-env-smoke.sh` for one of `JDBC` / `REST` / `CLIENT`, and archive the returned log/response proof outside the repository.
- Escalation: If the external environment remains unavailable or credentials/connectivity are still uncertain after the deployment window opens, keep repository implementation moving and ask the environment owner to provide the executable window, reachable Hetu/MRS endpoint, and evidence retention location.
- Human decision: Confirm the deployment window, final Hetu mode, target datasource credentials, and who will archive the live smoke evidence in the real test environment.
- INBOX ref: INBOX-002
