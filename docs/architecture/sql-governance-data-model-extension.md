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

建议新增：

- `parse_batch`
  - 所属服务：`sql-optimization`
  - 主键：`batch_id`
  - 结构化字段：`tenant_id`,`batch_name`,`import_mode`,`source_type`,`file_type`,`template_version`,`datasource_code`,`structure_parse_only`,`status`,`total_records`,`success_records`,`partial_success_records`,`failed_records`,`structure_parse_success_rate`,`access_parse_success_rate`,`created_by`,`created_at`,`updated_at`
  - JSON 字段：`status_history_json`
  - 追溯键：`tenant_id`,`batch_id`,`report_code`,`datasource_code`

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
