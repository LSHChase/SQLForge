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

### E-TASK-032: 落地 Redis 规则源、装数协同与系统参数页

- Status: in_progress
- Priority: 1
- Depends on: `E-TASK-031`,`D-TASK-072`
- Scope: Redis rule source、dispatch policy、系统参数与权限审计展示 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-013` 系统管理与数据源治理页
- Human confirmation point: 若 Redis 规则源、装数协同与系统参数页会把 environment-backed 配置写成默认已启用事实，需人工确认
- Data impact: rule-source、dispatch policy、system-param/permission 展示面
- Rollback / recovery: 恢复到查询/模拟状态展示，保留 simulated 或未联通提示
- Validation:
  - `npm run lint`、`npm run build`、system-management config contract 测试
  - `python3 scripts/foreman.py validate E-TASK-032`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.

### E-TASK-031: 落地系统管理中的数据源与报表接口页

- Status: in_progress
- Priority: 1
- Depends on: `E-TASK-024`,`D-TASK-072`
- Scope: datasource 管理、测试连接、报表接口配置与健康状态页 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-013` 系统管理与数据源治理页
- Human confirmation point: 若系统管理数据源/报表接口页会暴露敏感连接信息、误导用户认为真实外部接口已默认联通，需人工确认
- Data impact: 系统管理中的 datasource、health-check、report-interface 展示面
- Rollback / recovery: 恢复脱敏与 mock/config 标识，关闭高风险编辑入口
- Validation:
  - `npm run lint`、`npm run build`、system-management datasource contract 测试
  - `python3 scripts/foreman.py validate E-TASK-031`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.


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
- Next action: When the Win10 test environment is ready, deploy the yml-based governance/query-execution configuration from the runbook, run `bash scripts/run-hetu-env-smoke.sh` for one of `JDBC` / `REST` / `CLIENT`, and archive the returned log/response proof outside the repository.
- Escalation: If the external environment remains unavailable or credentials/connectivity are still uncertain after the deployment window opens, keep repository implementation moving and ask the environment owner to provide the executable window, reachable Hetu/MRS endpoint, and evidence retention location.
- Human decision: Confirm the deployment window, final Hetu mode, target datasource credentials, and who will archive the live smoke evidence in the real test environment.
- INBOX ref: INBOX-002
