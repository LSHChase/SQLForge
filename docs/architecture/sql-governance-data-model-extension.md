# SQL Governance Data Model Extension

## Summary

本文件定义 SQL 治理产品线的新增对象、推荐落表位置、扩表现状、JSON 与结构化字段取舍，以及跨服务追溯键策略。目标是在不新增微服务的前提下，为后续 D/E/F 新 Story 提供统一的数据建模输入。

## 1. Modeling Principles

- 只扩展现有四服务边界：
  - `governance`
  - `query-execution`
  - `sql-optimization`
  - `benchmark-engine`
- 统一优先：
  - 结构化主键
  - 显式追溯键
  - 审计可关联
  - append-only 修正
- 若字段满足以下条件，优先结构化：
  - 需要检索
  - 需要筛选
  - 需要排序
  - 需要聚合统计
- 若字段满足以下条件，可 JSON：
  - 低频展示
  - 嵌套证据
  - provider-specific 扩展
  - 兼容未来字段扩展

## 2. New / Extended Domain Objects

- `Datasource`
- `QueryExecution`
- `QueryHistoryView`
- `SqlParseHistory`
- `CommentContext`
- `SqlBindingSnapshot`
- `MetadataSnapshot`
- `BusinessLogicalView`
- `DatabaseViewRef`
- `LogicalObjectRef`
- `LogicalObjectMapping`
- `ParseTask`
- `ParseIssue`
- `ParseBatch`
- `ParseStatistic`
- `ReportResolveRecord`
- `RoutingRule`
- `RoutingDecision`
- `AccelerationRecommendation`
- `AccelerationCandidate`
- `SqlRewriteRecord`
- `RewriteValidationRun`
- `DispatchEvent`
- `BenchmarkTask`
- `BenchmarkTemplate`
- `BenchmarkTestSet`
- `BenchmarkReport`
- `AccessChannel`
- `AccessPolicy`
- `AccessAudit`
- `AlertEvent`
- `AlertPolicy`

## 3. Table Placement Strategy

### 3.1 Governance

优先落在 `governance`：

- SQL 执行历史宽表 / 查询面
- 路由规则与历史决策
- 数据源
- 逻辑对象目录与映射
- 装数协同事件
- 接入策略
- 告警

### 3.2 Query Execution

优先落在 `query-execution`：

- 单次执行运行态对象
- 绑定 SQL 快照
- 轻量解析摘要
- 本地执行结果摘要

### 3.3 SQL Optimization

优先落在 `sql-optimization`：

- 解析任务
- SQL 解析记录
- 解析问题
- 批量解析
- 报表解析记录
- 推荐对象

### 3.4 Benchmark Engine

优先落在 `benchmark-engine`：

- 模板
- 测试集
- 压测任务
- 压测报告

## 4. Existing Table Extension Guidance

建议优先扩展现有：

- `datasource_config`
- `query_history`
- `execution_result`
- `metadata_snapshot`
- `config_snapshot`
- `export_record`
- `audit_log`
- `optimization_task`
- `acceleration_plan`
- `benchmark_task`
- `benchmark_task_report`
- `tenant_config`
- `system_config`
- `report_interface_config`
  - repo-side 当前实现为 governance 内存配置对象，后续可落到 `system_config` 或独立表

建议新增：

- `sql_parse_history`
  - 所属服务：`sql-optimization`
  - 主键：`parse_history_id`
  - 结构化字段：`tenant_id`,`source_type`,`source_id`,`batch_key`,`parse_task_id`,`sql_fingerprint`,`datasource_code`,`datasource_type`,`report_code`,`stage_code`,`biz_date`,`query_date_start`,`query_date_end`,`query_date_status`,`access_channel`,`parser_mode`,`binding_mode`,`parameterized_sql_flag`,`result_status`,`target_engine`,`trace_id`,`request_id`,`saga_id`,`submitted_by`,`submitted_at`,`created_at`,`updated_at`
  - 大文本字段：`sql_text`,`sql_template_text`
  - JSON 字段：`structure_parse_summary_json`,`access_parse_summary_json`,`result_summary_json`,`result_payload_json`,`query_context_json`,`comment_context_json`,`binding_summary_json`,`logical_object_hits_json`,`issue_scenes_json`,`logical_object_keys_json`
  - 追溯键：`tenant_id`,`parse_history_id`,`parse_task_id`,`sql_fingerprint`,`source_type`,`source_id`,`batch_key`,`report_code`,`datasource_code`,`trace_id`,`request_id`,`saga_id`
  - 来源类型：`STRUCTURE_PARSE`,`COMBINED_PARSE`,`PARSE_BATCH`,`REPORT_BATCH`,`END_OF_DAY_SLOW_SQL`
  - 边界：仅承载 SQL 解析记录；不得再把解析记录写入 governance `query_history` 或使用 `historyType=SQL_PARSE` 做逻辑隔离
- `parse_batch`
  - 所属服务：`sql-optimization`
  - 主键：`batch_id`
  - 结构化字段：`tenant_id`,`batch_name`,`import_mode`,`source_type`,`file_type`,`template_version`,`datasource_code`,`structure_parse_only`,`status`,`total_records`,`success_records`,`partial_success_records`,`failed_records`,`structure_parse_success_rate`,`access_parse_success_rate`,`created_by`,`created_at`,`updated_at`
  - JSON 字段：`status_history_json`
  - 追溯键：`tenant_id`,`batch_id`,`report_code`,`datasource_code`
- `parse_batch_item`
  - 所属服务：`sql-optimization`
  - 主键：`item_id`
  - 外键语义：`batch_id -> parse_batch.batch_id`
  - 结构化字段：`batch_id`,`sequence_number`,`report_code`,`report_name`,`datasource_code`,`stage`,`biz_date`,`priority`,`owner`,`tags`,`status`,`parse_task_id`,`structure_syntax_status`,`access_service_status`,`access_connection_status`,`failure_reason`,`binding_mode`,`created_at`,`updated_at`
  - 大文本字段：`sql_text`,`sql_template_text`
  - JSON 字段：`bind_parameters_json`,`issue_scenes_json`,`logical_object_keys_json`
  - 追溯键：`tenant_id(经 batch 间接关联)`,`batch_id`,`item_id`,`report_code`,`parse_task_id`,`datasource_code`,`history_id(解析记录 ID)`
- `report_batch`
  - 所属服务：`sql-optimization`
  - 主键：`batch_id`
  - 结构化字段：`tenant_id`,`batch_name`,`file_type`,`report_code_field`,`datasource_code`,`stage`,`priority`,`source_type`,`status`,`total_reports`,`resolved_reports`,`failed_reports`,`created_by`,`created_at`,`updated_at`
  - JSON 字段：`status_history_json`
  - 追溯键：`tenant_id`,`batch_id`,`report_code_field`,`datasource_code`
- `report_batch_item`
  - 所属服务：`sql-optimization`
  - 主键：`item_id`
  - 外键语义：`batch_id -> report_batch.batch_id`
  - 结构化字段：`batch_id`,`sequence_number`,`report_code`,`report_name`,`datasource_code`,`stage`,`priority`,`source_file_line`,`sql_text`,`parse_task_id`,`structure_syntax_status`,`access_service_status`,`access_connection_status`,`failure_reason`,`status`,`created_at`,`updated_at`
  - JSON 字段：`issue_scenes_json`,`logical_object_keys_json`
  - 追溯键：`tenant_id(经 batch 间接关联)`,`batch_id`,`item_id`,`report_code`,`parse_task_id`,`datasource_code`,`history_id(解析记录 ID)`
- `acceleration_recommendation`
  - 所属服务：`sql-optimization`
  - 主键：`recommendation_id`
  - 结构化字段：`tenant_id`,`recommendation_type`,`source_sql_id`,`history_id`,`parse_task_id`,`batch_id`,`route_decision_id`,`alert_id`,`sql_fingerprint`,`target_engine`,`target_datasource`,`report_code`,`logical_object_key`,`summary`,`expected_gain`,`benefit_level`,`risk_level`,`requires_dispatch`,`status`,`created_by`,`created_at`,`updated_at`
  - 大文本字段：`source_sql_text`,`recommended_sql_text`,`reason`,`risk_summary`
  - 追溯键：`tenant_id`,`recommendation_id`,`source_sql_id`,`history_id`,`parse_task_id`,`batch_id`,`route_decision_id`,`alert_id`,`sql_fingerprint`,`report_code`,`logical_object_key`
  - 自动生成来源：当单条结构解析、普通批量解析或报表批量解析在有效结构解析结果中命中 `OR_PREDICATE_INDEX_RISK`、`SELECT_STAR`、`NESTED_SUBQUERY_RISK`、`LEADING_WILDCARD_LIKE_RISK` 任一问题场景时，`sql-optimization` 会提交解析来源标记的异步 `REWRITE` 任务；worker 成功后以 `REWRITE` 类型写入本表，并保留原始 SQL、推荐 SQL、`history_id`、`parse_task_id`、`batch_id`、`report_code`、`sql_fingerprint` 与目标数据源
  - 状态边界：仅允许 `RECOMMENDED`、`REVIEWING`、`DISPATCH_READY`、`CANCELLED`；不得在本对象内表达 `EXECUTED`，避免把推荐误写成真实装数或执行结果
- `acceleration_candidate`
  - 所属服务：`sql-optimization`
  - 主键：`candidate_id`
  - 结构化字段：`tenant_id`,`source_type`,`source_kind`,`source_id`,`history_id`,`parse_history_id`,`parse_task_id`,`batch_id`,`batch_item_id`,`benchmark_task_id`,`optimization_task_id`,`sql_fingerprint`,`datasource_code`,`stage`,`report_code`,`candidate_type`,`status`,`confidence`,`priority`,`evidence_level`,`schema_version`,`created_by`,`created_at`,`updated_at`
  - JSON 字段：`source_evidence_json`,`issue_evidence_json`,`runtime_evidence_json`,`benefit_estimate_json`,`cost_estimate_json`,`risk_json`
  - 追溯键：`tenant_id`,`candidate_id`,`source_type`,`source_kind`,`source_id`,`history_id`,`parse_history_id`,`parse_task_id`,`batch_id`,`batch_item_id`,`benchmark_task_id`,`optimization_task_id`,`sql_fingerprint`,`report_code`,`datasource_code`
  - 边界：统一承接解析驱动与查询驱动进入加速/改写治理工作台的候选对象；不得表示真实物理加速已执行
- `sql_rewrite_record`
  - 所属服务：`sql-optimization`，由 `governance` 查询面聚合到 SQL 历史详情
  - 主键：`rewrite_record_id`
  - 结构化字段：`tenant_id`,`recommendation_id`,`optimization_task_id`,`source_type`,`source_kind`,`source_id`,`history_id`,`parse_history_id`,`sql_fingerprint`,`datasource_code`,`status`,`validation_status`,`auto_apply_allowed`,`manual_review_required`,`validation_policy_id`,`last_validation_run_id`,`last_compared_at`,`alert_status`,`created_by`,`created_at`,`updated_at`
  - 大文本字段：`original_sql_text`,`recommended_sql_text`,`executed_sql_text`
  - JSON 字段：`rule_chain_json`,`diff_summary_json`,`risk_json`,`trace_refs_json`
  - 追溯键：`tenant_id`,`rewrite_record_id`,`recommendation_id`,`optimization_task_id`,`source_type`,`source_kind`,`source_id`,`history_id`,`parse_history_id`,`sql_fingerprint`
  - 边界：承载改写建议、diff、验证状态和实际执行 SQL 追溯；不得把推荐状态误写成装数或真实物理加速完成
- `rewrite_validation_run`
  - 所属服务：`sql-optimization`，只读执行可通过 `query-execution` 协同，压测或回归证据可通过 `benchmark-engine` 协同；`benchmark-engine` 不拥有本对象的长期真值
  - 主键：`validation_run_id`
  - 结构化字段：`tenant_id`,`rewrite_record_id`,`recommendation_id`,`history_id`,`sql_fingerprint`,`status`,`comparison_status`,`difference_type`,`auto_apply_paused`,`started_at`,`finished_at`
  - JSON 字段：`comparison_policy_json`,`original_result_digest_json`,`recommended_result_digest_json`,`difference_sample_json`,`execution_evidence_json`
  - 追溯键：`tenant_id`,`validation_run_id`,`rewrite_record_id`,`recommendation_id`,`history_id`,`sql_fingerprint`
  - 边界：只保存结果比对摘要、digest、有限差异样本和执行证据；大结果集不得全量拉回前端或落入本表
- `dispatch_event`
  - 所属服务：`sql-optimization`
  - 主键：`dispatch_event_id`
  - 结构化字段：`tenant_id`,`recommendation_id`,`dispatch_type`,`target_engine`,`target_datasource`,`report_code`,`logical_object_key`,`status`,`pulled_by`,`pulled_at`,`acked_by`,`acked_at`,`failed_by`,`failed_at`,`result_message`,`created_by`,`created_at`,`updated_at`
  - JSON 字段：`dispatch_payload_json`,`status_history_json`
  - 追溯键：`tenant_id`,`dispatch_event_id`,`recommendation_id`,`report_code`,`logical_object_key`
  - 状态边界：`CREATED -> PUBLISHED -> PULLED -> ACKED|FAILED`；`PUBLISHED` 仅表示可拉取，不表示主动推送或已装数
- `benchmark_test_set`
  - 所属服务：`benchmark-engine`
  - 主键：`test_set_id`
  - 结构化字段：`tenant_id`,`test_set_name`,`template_id`,`template_type`,`template_version`,`test_set_source`,`status`,`total_cases`,`accepted_cases`,`rejected_cases`,`file_type`,`file_name`,`import_batch_id`,`created_by`,`created_at`,`updated_at`
  - JSON 字段：`field_mappings_json`,`test_set_labels_json`,`test_set_source_refs_json`
  - 追溯键：`tenant_id`,`test_set_id`,`import_batch_id`,`template_id`
  - 状态边界：`READY` / `PARTIAL_READY` / `FAILED`；`FAILED` 仍保留 rejected-row evidence，不把导入失败静默丢弃
- `benchmark_test_set_case`
  - 所属服务：`benchmark-engine`
  - 主键：`case_id`
  - 外键语义：`test_set_id -> benchmark_test_set.test_set_id`
  - 结构化字段：`test_set_id`,`sequence_number`,`source_line_number`,`case_name`,`sql_fingerprint`,`datasource_code`,`report_code`,`status`,`rejection_reason`
  - 大文本字段：`sql_text`
  - JSON 字段：`tags_json`,`bind_parameters_json`,`raw_case_data_json`
  - 追溯键：`tenant_id(经 test set 间接关联)`,`test_set_id`,`case_id`,`report_code`,`sql_fingerprint`,`import_batch_id(经 test set 间接关联)`

配置对象：

- `ReportInterfaceConfig`
  - 所属服务：`governance`
  - 配置键：`tenant_id`,`datasource_code`,`stage`,`endpoint_code`
  - 结构化字段：`source_type`,`endpoint_name`,`base_url`,`path_template`,`http_method`,`report_code_param_name`,`sql_json_path`,`auth_mode`,`timeout_ms`,`enabled`,`updated_at`
  - 安全边界：不在 repo 固化真实 endpoint secret；真实密钥后续进入受保护配置源

领域值对象：

- `StructureParseIssueScenario`
  - 所属服务：`sql-optimization`
  - 用途：统一 `issue_scene -> issue_domain/default_severity/default_important/default_urgent/scene_weight`
  - 消费方：结构解析响应、批量解析统计、后续按 SQL/报表/场景统计
- `StructureParseIssueScoringSnapshot`
  - 所属服务：`sql-optimization`
  - 用途：为统计聚合提供稳定快照字段：`issue_scene`,`issue_domain`,`severity`,`priority_level`,`priority_score`,`important`,`urgent`

## 5. Traceability Keys

所有新增对象至少应支持以下关联键中的适用子集：

- `tenant_id`
- `request_id`
- `trace_id`
- `execution_id`
- `history_id`
- `parse_task_id`
- `batch_id`
- `recommendation_id`
- `dispatch_event_id`
- `benchmark_task_id`
- `report_code`
- `sql_fingerprint`

## 6. JSON vs Structured Guidance

建议结构化的字段：

- `report_code`
- `stage`
- `biz_date`
- `datasource_code`
- `status`
- `severity`
- `priority_level`
- `important_flag`
- `urgent_flag`
- `access_channel`
- `recommendation_type`
- `benefit_level`
- `risk_level`
- `requires_dispatch`
- `dispatch_status`
- `binding_mode`
- `binding_render_status`
- `query_date_status`
- `connection_status`
- `service_status`

建议 JSON 的字段：

- `comment_context_json`
- `bind_parameters_masked_json`
- `logical_object_hits_json`
- `route_summary_json`
- `cache_summary_json`
- `issue_detail`
- `dispatch_payload_json`
- `detail_json`
- `threshold_json`

逻辑对象统一引用字段至少包括：

- `object_type`
- `object_key`
- `object_name`
- `catalog_name`
- `schema_name`
- `match_source`
- `resolved`
- `mapped_physical_targets_json`

其中：

- `object_type` 枚举固定为 `BUSINESS_VIEW / DB_VIEW / TABLE`
- `object_key` 固定为 `TYPE:qualified_object_name_lowercase`
- `logical_object_hits_json` 中若对象以 JSON 形式落库，也必须沿用同一字段命名，不再混用 `type/objectType`

## 7. HARN-084 Parse History Storage Boundary

SQL 解析记录写入 `sql_parse_history` 时，`query_context_json` 必须使用可投影的归一化结构。解析记录列表结构化列从 `sql_parse_history` 的结构化字段投影，解析记录详情从同一记录读取解析证据。

必备顶层字段：

- `parseTaskId`
- `sourceType`
- `sourceId`
- `batchKey`
- `datasourceCode`
- `datasourceType`
- `sqlFingerprint`
- `accessChannel`
- `commentContext`: 原 SQL 注释上下文或页面/批次传入上下文，至少承载 `report_code`、`stage`、`biz_date`、`datasource`
- `queryDateSummary`: 结构解析得到的查询日期摘要
- `queryDateStart`
- `queryDateEnd`
- `queryDateStatus`
- `logicalObjectHits`
- `bindingSummary`
- `structureParseSummary`
- `accessParseSummary`
- `logicalObjectKeys`: 解析写入端按最终唯一 `TABLE:*` keys 归档；若原 SQL 命中底层 DB View，应优先保存实时 View definition 递归展开后的叶子表 keys，实时元数据不可用时才允许使用治理 DB View 目录 fallback 证据。

兼容规则：

- 解析写入端允许接收 legacy 扁平 comment context，但持久化前必须归一成 `queryContext.commentContext`，并同步投影 `report_code`、`stage_code`、`biz_date` 等结构化列。
- 同一 `parseTaskId` 的结构解析和 access 解析必须合并到同一 `parseHistoryId`，不得拆成两条互不关联的解析记录。
- `query_history` 只承载 SQL 执行历史和跨服务追溯链的执行语义记录，不再新增 `SQL_PARSE` 解析记录。
- 旧 `query_history` 中既有 `SQL_PARSE` 数据不做在线迁移；若需要历史迁移或兼容查询，应另立数据迁移任务并单独审计。

## Related Documents

- `docs/product/sql-governance-platform-implementation-spec.md`
- `docs/architecture/sql-governance-interface-extension-baseline.md`
- `docs/architecture/persistence.md`
