# HARN-084 Raw Requirement

Source: user-provided previous-agent plan on 2026-05-08.

## SQL 执行历史与 SQL 解析记录解耦计划

将 SQL 查询、SQL 历史限定为“SQL 执行管理”；将解析、加速限定为“SQL 解析管理”。两条线在后端代码、前端 API client、页面数据源、持久化模型上完全独立。唯一允许的交互是日终批处理：解析侧批量读取慢 SQL 执行历史，生成并落库独立的 SQL 解析记录。

## Key Changes

- 后端新增独立解析历史模型，由 `sql-optimization` 拥有：
  - 新增 `sql_parse_history` 表、MyBatis mapper、domain/entity、repository、application service。
  - `StructureParseApplicationService`、`CombinedParseApplicationService`、`ParseBatchApplicationService`、`ReportBatchApplicationService` 不再通过 `GovernanceParseHistoryTraceabilityApplicationService` 写 `query_history`。
  - 解析记录 ID、解析状态、parseTaskId、SQL 指纹、SQL 文本、结构解析摘要、访问解析摘要、问题场景、逻辑对象命中、来源类型等全部写入 `sql_parse_history`。

- API 边界拆分：
  - SQL 执行历史继续使用执行历史接口，只返回 `QUERY_EXECUTION` 语义数据。
  - 新增解析历史接口，例如 `GET /api/sql-optimization/parse-history`、`GET /api/sql-optimization/parse-history/{parseHistoryId}`、`POST /api/sql-optimization/parse-history/export`。
  - 停止让解析页调用 `/api/governance/query-history`；治理侧 `query_history` 不再承载 `SQL_PARSE` 记录。

- 日终慢 SQL 解析骨架：
  - 在 `sql-optimization` 新增 `SlowSqlExecutionHistorySource` 端口和 `EndOfDaySlowSqlParseApplicationService`。
  - 批处理只读拉取慢 SQL 执行历史候选，按阈值和时间窗口生成解析任务，并写入 `sql_parse_history`。
  - 本轮只落契约、服务骨架、幂等批次键和测试，不接真实 cron 调度。

- 前端拆分：
  - `SqlHistoryView.vue` 只使用 SQL 执行历史 client，移除解析证据展示和 `SQL_PARSE` 相关概念。
  - `ParseRecordView.vue` 改用解析历史 client，页面文案、筛选项、详情抽屉都围绕解析记录，不再出现 query-history 语义。
  - `runtimeGateApi.js` 拆出执行历史 API 与解析历史 API，避免共用 `getGovernanceQueryHistoryPage/detail/export`。

- 文档与治理：
  - 更新接口基线、服务能力图、数据模型文档，明确 `query_history` 等于执行历史，`sql_parse_history` 等于解析记录。
  - 正式实现时按仓库治理执行：`preflight`、`instantiate <TASK_ID>`、实现、`validate <TASK_ID>`、`task_audit --phase pre-closeout`、`closeout`、`task_audit --phase post-closeout`，单任务单 commit。

## Test Plan

- 后端单测/契约测试：
  - 解析入口写入 `sql_parse_history`，不写 `query_history`。
  - SQL 历史列表不会返回解析记录。
  - 解析历史列表/detail/export 只读 `sql_parse_history`。
  - 日终服务从慢 SQL 候选生成解析记录，重复执行保持幂等。

- 前端检查：
  - SQL 历史页只调用执行历史接口。
  - 解析记录页只调用解析历史接口。
  - 更新现有 history/parse contract 脚本，禁止解析页请求 `/api/governance/query-history`。

- 数据库检查：
  - `sql/init-schema.sql` 与新 migration 一致。
  - mapper schema mapping 测试覆盖 `sql_parse_history` 关键字段。

## Assumptions

- 采用独立表和独立 API，不再用 `historyType=SQL_PARSE` 在 `query_history` 中做逻辑隔离。
- 日终慢 SQL 拉取本轮只做契约和服务骨架，不实现真实调度。
- 旧 `query_history` 中既有 `SQL_PARSE` 数据不做在线迁移；如需迁移，另立数据迁移任务。
