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

### D-TASK-071: 落地数据资产与数据源治理查询/详情接口

- Status: in_progress
- Priority: 1
- Depends on: `D-TASK-070`
- Scope: datasource/schema/table/logical-view/db-view 列表、详情与 metadata snapshot 查询面 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-013` 数据源与数据资产治理增强
- Human confirmation point: 若数据资产接口会扩大跨租户可见范围、暴露未授权对象详情或破坏现有查询性能边界，需人工确认
- Data impact: datasource/schema/table/logical-view/db-view 查询面与详情接口
- Rollback / recovery: 回退高风险详情字段与筛选面，恢复基础受保护查询
- Validation:
  - `data-asset API 与 detail query 测试`
  - `python3 scripts/foreman.py validate D-TASK-071`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.

### D-TASK-070: 建立 `MetadataSnapshot` 与数据到位状态模型

- Status: in_progress
- Priority: 1
- Depends on: `D-TASK-069`
- Scope: metadata snapshot、freshness、SLA、upstream/downstream/queryability 的模型与追溯键 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-013` 数据源与数据资产治理增强
- Human confirmation point: 若 metadata snapshot / freshness / SLA / upstream-downstream 状态会把无证据数据写成确定事实，需人工确认
- Data impact: metadata snapshot、freshness/SLA/queryability/upstream/downstream 追溯面
- Rollback / recovery: 恢复未知/未采集默认语义，保留证据来源与回退字段
- Validation:
  - `metadata model 与 snapshot query 测试`
  - `python3 scripts/foreman.py validate D-TASK-070`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.

### D-TASK-069: 固化数据源连接配置与健康检查契约

- Status: in_progress
- Priority: 1
- Depends on: `D-TASK-049`
- Scope: JDBC/API/Client/Gateway 连接方式、凭证、安全、测试连接、健康状态与失败原因契约 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-013` 数据源与数据资产治理增强
- Human confirmation point: 若数据源连接配置与健康检查会落明文凭据、放宽租户隔离或把环境级 endpoint/secret 写入仓库真值，需人工确认
- Data impact: datasource 配置、测试连接、健康状态与失败原因查询面
- Rollback / recovery: 回退到只读 datasource 查询基线，移除高风险配置字段与敏感信息暴露
- Validation:
  - `datasource contract 与 health-check 测试`
  - `python3 scripts/foreman.py validate D-TASK-069`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.

### D-TASK-068: 落地 Java SDK 首版

- Status: in_progress
- Priority: 1
- Depends on: `D-TASK-067`
- Scope: 提供鉴权、trace/requestId、typed client 与 retry 基线 Tech: `JAVA-BE`,`OPS`. Layer: `common`,`deployments/ci/scripts`.
- Matrix context: Phase-D / Story `D-STORY-012` 开放接入与 JDBC Agent
- Human confirmation point: 若 Java SDK 会把未稳定契约写成强依赖、绕过统一 request/trace 语义或暴露敏感配置，需人工确认
- Data impact: SDK client、配置、请求重试与接入文档
- Rollback / recovery: 回退 SDK 到最小 typed client 基线，并保留 HTTP API 主路径
- Validation:
  - `SDK 测试`
  - `python3 scripts/foreman.py validate D-TASK-068`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.

### D-TASK-066: 扩展 JDBC Agent `Governed Execute`

- Status: in_progress
- Priority: 1
- Depends on: `D-TASK-065`
- Scope: 通过平台 API 执行 SQL，并保留 fallback 语义 Tech: `JAVA-BE`,`OPS`. Layer: `common`,`deployments/ci/scripts`.
- Matrix context: Phase-D / Story `D-STORY-012` 开放接入与 JDBC Agent
- Human confirmation point: 若 JDBC Agent `Governed Execute` 会在平台不可用时无回退策略、或默认强制所有 SQL 走平台，需人工确认
- Data impact: Agent 执行模式、fallback 策略、平台调用链
- Rollback / recovery: 恢复租户/数据源级可切换边界和 fallback 语义
- Validation:
  - `governed-execute 测试`
  - `python3 scripts/foreman.py validate D-TASK-066`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.

### D-TASK-065: 落地 JDBC Agent 首版 `Observe`

- Status: in_progress
- Priority: 1
- Depends on: `D-TASK-064`
- Scope: JAR 采集 SQL、注释解析、上报 access audit，不接管执行 Tech: `JAVA-BE`,`OPS`. Layer: `common`,`deployments/ci/scripts`.
- Matrix context: Phase-D / Story `D-STORY-012` 开放接入与 JDBC Agent
- Human confirmation point: 若 JDBC Agent `Observe` 会接管执行、写入敏感信息或在规则源失败时影响业务查询，需人工确认
- Data impact: Agent JAR、采集上报、access audit 与 Redis 依赖
- Rollback / recovery: 恢复 observe-only 语义，禁用高风险上报或敏感字段透出
- Validation:
  - `JDBC agent sample/integration 测试`
  - `python3 scripts/foreman.py validate D-TASK-065`
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
