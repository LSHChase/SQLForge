# SQLForge Persistence Baseline

本文件是 `R-031`、`R-055`、`R-065`、`R-129` 与 `R-169` 的当前权威落点，统一描述 SQLForge 的 MySQL 主持久化方向、核心追溯链、MyBatis XML 落点与增量脚本约束。

## Scope

- 当前主持久化方向固定为 MySQL。
- 面向 MySQL / TDSQL 时，业务表禁止新增物理外键约束；表间关系通过引用键、索引和应用层完整性校验维护。
- 当前数据库初始化脚本固定为 `sql/init-schema.sql` 与 `sql/init-data.sql`。
- 当前增量脚本目录固定为 `sql/migrations/`。
- 当前 Java 持久化实现固定为 MyBatis XML，禁止在注解中写复杂 SQL。

## Current Carrier

- `governance` 是当前承载事务型治理元数据的实现载体。
- `sql-optimization` 当前已接入 MySQL `optimization_task` 与 `acceleration_plan` 双表、MyBatis XML mapper 与 in-process scheduled worker，用于承载真实任务持久化、状态流转、结构化 suggestion/failure payload，以及 governed acceleration plan lifecycle 的落仓。
- `benchmark-engine` 当前已接入 MySQL `benchmark_task` / `benchmark_task_report` 双表、MyBatis XML mapper 与 in-process scheduled worker，用于承载真实任务持久化、报告回写和状态流转。
- `benchmark-engine` 当前已通过 `governance` 内部 `benchmark/report-trace/write` 受保护入口，把 benchmark report artifact 的 `config_snapshot/execution_result/query_history/export_record` 编排写入接到真实追溯链。
- `sql-optimization` 当前也已通过 `governance` 内部 `acceleration-plan/trace/write` 受保护入口，把 acceleration plan 的 `config_snapshot/execution_result/query_history` 编排写入接到真实追溯链。
- 因此，Phase-D 的核心追溯链当前在 `governance` 内以 schema + entity + mapper XML 形式固化，同时允许 `sql-optimization` 与 `benchmark-engine` 在独立任务/报告表上落真实 carrier，并通过受保护入口把跨服务 trace/export 编排接回治理链，避免异步任务实现继续漂移。

## Core Traceability Chain

当前 Phase-D 固化的核心追溯链如下：

1. `config_snapshot`
2. `execution_result`
3. `query_history`
4. `export_record`
5. `audit_log`

主引用关系：

- `execution_result.config_snapshot_id -> config_snapshot.config_snapshot_id`
- `query_history.result_id -> execution_result.result_id`
- `export_record.history_id -> query_history.history_id`
- `export_record.result_id -> execution_result.result_id`
- `audit_log.config_snapshot_id -> config_snapshot.config_snapshot_id`
- `audit_log.result_id -> execution_result.result_id`
- `audit_log.history_id -> query_history.history_id`
- `audit_log.export_id -> export_record.export_id`

该链路满足 `R-034` 对 config/result/history/export/audit 可追溯关联键的要求，并为 `ADR-012` 的 saga + 本地事务补偿链预留稳定引用点。

当前规则要求这些关系在 MySQL / TDSQL 上以“引用键 + 索引 + 应用层约束”表达，而不是继续依赖物理外键约束。

## Shared Trace Keys

以下字段是当前跨服务追溯的共享最小键集：

- `tenant_id`
- `service_code`
- `trace_id`
- `request_id`
- `saga_id`

使用原则：

- `tenant_id` 负责租户隔离与历史查询过滤。
- `service_code` 标识写入方服务，避免后续多服务共表时失去归属。
- `trace_id` 用于一次链路内的日志、消息、审计串联。
- `request_id` 用于单次请求级别定位。
- `saga_id` 用于跨服务补偿、重试与消息重放关联。

## Table Responsibilities

### `config_snapshot`

- 保存配置快照，不直接把审计、导出、结果链路绑死在 `tenant_config`、`system_config` 或 `acceleration_config` 原表上。
- 通过 `source_config_type + source_config_id + source_version` 保留来源。
- `snapshot_payload` 必须保存可复现的配置快照。
- 当前 `governance` 已通过共享 AES-256 基线把 `snapshot_payload` 中命中的密码 / token / key 类字段转为密文 envelope，再落入 JSON 列。

### `execution_result`

- 保存查询执行、SQL 优化、压测等任务的统一结果锚点。
- 通过 `task_id + task_type` 指回原始任务或执行对象。
- `result_summary` 与 `result_payload` 仅保存结构化、脱敏后的结果内容。
- 当前 `result_summary` 走掩码路径，`result_payload` 走敏感叶子节点加密路径，失败 `error_message` 仅允许脱敏后的摘要。

### `query_history`

- 保存可追溯的历史查询快照。
- 明文 SQL 不得落库；如需保留原文，必须进入 `sql_text_cipher` 等密文字段。
- `history_type` 用于区分查询执行、优化、压测等历史来源。
- 当前 `query_context` 进入受保护持久化入口时，会把密码 / token / key 类字段转为密文 envelope。

### `export_record`

- 保存 JSON / PDF / HTML / CSV 等导出行为的元数据与存储位置。
- 导出对象必须基于 `query_history` 或可复现结果，而不是浏览器瞬时状态。
- 当前 `storage_uri` 仅允许保存脱敏后的地址摘要；`export_options` 的敏感叶子节点会在落库前加密。
- 当前 `benchmark-engine` 会为 `JSON/PDF/HTML` 报告导出与 raw-data snapshot 生成稳定 `export_record`，其 `export_id` 采用 `reportId + artifactKey` 的幂等键策略。
- 当前 `benchmark-engine -> governance` trace 编排会把 workloadDigest/workloadSource/backfillApplied/workloadEvidence 中的 compensation-replay 结构证据，以及 artifact `storageEvidence/retention*` 中的 primary/recovery provider contract、recovery order、cleanup scope、provider/external verification 证据，一并下沉到 `config_snapshot.snapshot_payload`、`execution_result.result_summary/result_payload`、`query_history.query_context` 与 `export_record.export_options`。
- 当前 `export_record.export_options.storageEvidence` 已成为治理查询面的结构化来源之一，供 history summary/detail 抽取 `artifactStorageContract` 与 `artifactRecoverySurface`，而不是只保留成不可复用的文本旁证。
- 当前 `audit_log.response_summary` 还会写入 `artifactOperationSurface`，把 governance-triggered cleanup/recovery 的 `operationType/operationStatus/cleanupScope/storageRecoverySource/storageReadStatus/providerHeadStatus/providerRequestId` 直接暴露给治理 detail/query。

### `audit_log`

- 继续作为不可变审计证据表。
- 在既有字段基础上追加 `service_code`、`trace_id`、`request_id`、`saga_id` 与四类追溯引用键。
- 审计记录允许引用 config/result/history/export 任意一层，但不要求每条记录都填满全部引用键。
- 当前已落地两类真实写入入口：
  - `POST /api/governance/internal/audit/write`
  - `governance` 的 header-based stateless auth `LOGIN` / `LOGOUT` 事件
- 当前 `benchmark-engine` 的报告查询、导出查询与 raw-data 下载在 artifact 已具备治理追溯元数据时，会把 `config_snapshot_id/result_id/history_id/export_id` 一并写入对应 `audit_log`。
- 当前 `benchmark-engine` 的报告查询、导出查询与 raw-data 下载还会把 artifact recovery status、storage recovery source、storage read status 与 provider/external recovery 留痕写入 `audit_log.response_summary`，供 governance history query/detail 直接复用。
- 当前 `benchmark-engine` 新增的内部 artifact operation 会继续把 cleanup/recovery operation surface 写入 `audit_log.response_summary.artifactOperationSurface`，并复用既有 `artifactStorageEvidence`/trace keys，而不是另起不可追溯的旁路表；当前该结构还会显式下沉 `orchestrationType/batchId/batchIndex/batchSize/errorCode/errorMessage`，用于 governance batch retention / recovery orchestration 的逐项追溯。
- 当前 audit 真写链会在入库前统一处理 `request_params` 与 `response_summary`：
  - `request_params` 仅保留脱敏 JSON
  - `response_summary` 仅保留脱敏文本

### `system_config`

- `system_config` 继续作为平台级配置表。
- 非敏感配置继续走 `config_value` 明文列。
- 命中密码 / token / key 类键名的配置改为：
  - `config_value = NULL`
  - `sensitive_flag = 1`
  - `value_ciphertext` 保存 AES-256 密文 envelope
  - `value_mask` 保存只读掩码摘要
  - `encryption_algorithm` / `encryption_key_id` 保存解密元数据

## MyBatis XML Mapping Baseline

当前治理侧的表到实体映射基线如下：

| Table | Entity | Mapper XML |
|:---|:---|:---|
| `config_snapshot` | `ConfigSnapshotRecord` | `governance/src/main/resources/mapper/ConfigSnapshotMapper.xml` |
| `execution_result` | `ExecutionResultRecord` | `governance/src/main/resources/mapper/ExecutionResultMapper.xml` |
| `query_history` | `QueryHistoryRecord` | `governance/src/main/resources/mapper/QueryHistoryMapper.xml` |
| `export_record` | `ExportRecord` | `governance/src/main/resources/mapper/ExportRecordMapper.xml` |
| `audit_log` | `AuditLogRecord` | `governance/src/main/resources/mapper/AuditLogMapper.xml` |
| `system_config` | `SystemConfigRecord` | `governance/src/main/resources/mapper/SystemConfigMapper.xml` |
| `optimization_task` | `OptimizationTaskRecord` | `sql-optimization/src/main/resources/mapper/OptimizationTaskMapper.xml` |
| `acceleration_plan` | `AccelerationPlanRecord` | `sql-optimization/src/main/resources/mapper/AccelerationPlanMapper.xml` |
| `benchmark_task` | `BenchmarkTaskRecord` | `benchmark-engine/src/main/resources/mapper/BenchmarkTaskMapper.xml` |
| `benchmark_task_report` | `BenchmarkReportRecord` | `benchmark-engine/src/main/resources/mapper/BenchmarkReportMapper.xml` |

当前 mapper 只固化 `insert/selectById` 或等价最小骨架，目的是先把表结构、主引用键和字段命名稳定下来，再在后续任务中接入真实 repository、事务编排和业务写入路径。当前 `governance` 已额外提供 `GovernanceProtectedPersistenceService` 作为 config/result/history/export/audit/system-config 的敏感字段保护写入入口，并由 benchmark report trace/export orchestration 走真实写入路径验证 `config/result/history/export` 编排。

在 `R-169` 生效后，`GovernanceProtectedPersistenceService` 同时承担核心追溯链的应用层引用完整性校验，负责在无物理外键约束前提下检查：

- 被引用记录存在
- 引用链上的 `tenant_id` 一致
- `history -> result`、`export -> history/result`、`audit -> history/result/export` 等关系不自相矛盾

## Migration Policy

- 当前不引入 Flyway。
- MySQL / TDSQL 的初始化脚本和增量脚本不得新增物理外键约束。
- 变更表结构时必须同时更新：
  - `sql/init-schema.sql`
  - `sql/migrations/V{date_or_version}__*.sql`
  - 对应 Entity / Mapper XML
  - 本文档中的映射与职责描述
- 已发布环境必须优先执行增量脚本，不允许仅依赖重跑初始化脚本。

当前仓库的历史 schema 仍存在已落库的外键约束，这是新规则生效前的遗留实现漂移。后续需要专门的 schema 治理任务移除，但在该任务开始前不得继续新增或扩散外键约束。

当前 D-TASK-011 的增量脚本：

- `sql/migrations/V20260421_011__core_traceability_chain.sql`

当前 D-TASK-013 追加的增量脚本：

- `sql/migrations/V20260421_013__sensitive_data_encryption_baseline.sql`

当前 F-TASK-015 / F-TASK-016 追加的增量脚本：

- `sql/migrations/V20260422_014__sql_optimization_task_persistence.sql`
- `sql/migrations/V20260422_015__benchmark_engine_task_report_persistence.sql`

当前 D-TASK-031 追加的增量脚本：

- `sql/migrations/V20260425_001__sql_optimization_task_real_pipeline_payloads.sql`

当前 D-TASK-032 追加的增量脚本：

- `sql/migrations/V20260425_002__acceleration_plan_governance.sql`

当前 D-TASK-018 追加的增量脚本：

- `sql/migrations/V20260423_017__drop_traceability_foreign_keys.sql`

## Validation Baseline

触发 `R-129` 时至少完成以下四项：

1. 更新 `sql/init-schema.sql`
2. 提供增量脚本
3. 在本地 MySQL 执行脚本成功
4. 校验本次 DDL 未新增外键约束
5. 更新 Entity 与脚本映射文档

当前仓库内的最小自动化校验为：

- `governance` 模块中的 `TraceabilitySchemaMappingTest`

该测试只检查 schema / migration / mapper XML 的命名一致性，不替代真实 MySQL 执行验证。
