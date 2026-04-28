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

- 历史宽表 / 查询面
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
  - 追溯键：`tenant_id(经 batch 间接关联)`,`batch_id`,`item_id`,`report_code`,`parse_task_id`,`datasource_code`
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
  - 追溯键：`tenant_id(经 batch 间接关联)`,`batch_id`,`item_id`,`report_code`,`parse_task_id`,`datasource_code`
- `acceleration_recommendation`
  - 所属服务：`sql-optimization`
  - 主键：`recommendation_id`
  - 结构化字段：`tenant_id`,`recommendation_type`,`source_sql_id`,`history_id`,`parse_task_id`,`batch_id`,`route_decision_id`,`alert_id`,`sql_fingerprint`,`target_engine`,`target_datasource`,`report_code`,`logical_object_key`,`summary`,`expected_gain`,`benefit_level`,`risk_level`,`requires_dispatch`,`status`,`created_by`,`created_at`,`updated_at`
  - 大文本字段：`source_sql_text`,`recommended_sql_text`,`reason`,`risk_summary`
  - 追溯键：`tenant_id`,`recommendation_id`,`source_sql_id`,`history_id`,`parse_task_id`,`batch_id`,`route_decision_id`,`alert_id`,`sql_fingerprint`,`report_code`,`logical_object_key`
  - 状态边界：仅允许 `RECOMMENDED`、`REVIEWING`、`DISPATCH_READY`、`CANCELLED`；不得在本对象内表达 `EXECUTED`，避免把推荐误写成真实装数或执行结果
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

## Related Documents

- `docs/product/sql-governance-platform-implementation-spec.md`
- `docs/architecture/sql-governance-interface-extension-baseline.md`
- `docs/architecture/persistence.md`
