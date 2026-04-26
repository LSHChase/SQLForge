ALTER TABLE execution_result
  ADD COLUMN access_channel VARCHAR(32) DEFAULT NULL COMMENT 'Access channel such as PAGE/API/JDBC_AGENT/SDK/CLIENT' AFTER saga_id,
  ADD COLUMN target_engine VARCHAR(64) DEFAULT NULL COMMENT 'Selected execution engine or routed engine' AFTER access_channel,
  ADD COLUMN returned_row_count BIGINT DEFAULT NULL COMMENT 'Returned row count when known' AFTER target_engine,
  ADD COLUMN cache_hit TINYINT(1) DEFAULT NULL COMMENT 'Whether cache was hit' AFTER returned_row_count,
  ADD COLUMN rewrite_applied TINYINT(1) DEFAULT NULL COMMENT 'Whether lightweight rewrite was applied' AFTER cache_hit,
  ADD COLUMN acceleration_applied TINYINT(1) DEFAULT NULL COMMENT 'Whether acceleration path was applied' AFTER rewrite_applied,
  ADD COLUMN hit_table_summary JSON DEFAULT NULL COMMENT 'Structured hit table summary JSON' AFTER acceleration_applied,
  ADD COLUMN route_summary JSON DEFAULT NULL COMMENT 'Structured route summary JSON' AFTER hit_table_summary,
  ADD COLUMN cache_summary JSON DEFAULT NULL COMMENT 'Structured cache summary JSON' AFTER route_summary;

ALTER TABLE execution_result
  ADD KEY idx_execution_result_access_channel (tenant_id, access_channel, create_time),
  ADD KEY idx_execution_result_target_engine (tenant_id, target_engine, create_time),
  ADD KEY idx_execution_result_cache_hit (tenant_id, cache_hit, create_time);

ALTER TABLE query_history
  ADD COLUMN sql_template_cipher MEDIUMBLOB DEFAULT NULL COMMENT 'Encrypted template SQL payload for parameterized queries' AFTER sql_text_cipher,
  ADD COLUMN bound_sql_text_cipher MEDIUMBLOB DEFAULT NULL COMMENT 'Encrypted bound SQL payload after parameter binding' AFTER sql_template_cipher,
  ADD COLUMN datasource_code VARCHAR(128) DEFAULT NULL COMMENT 'Datasource business identifier or alias' AFTER bound_sql_text_cipher,
  ADD COLUMN report_code VARCHAR(128) DEFAULT NULL COMMENT 'Report code parsed from SQL comment context' AFTER datasource_type,
  ADD COLUMN stage_code VARCHAR(32) DEFAULT NULL COMMENT 'Execution stage parsed from SQL comment context' AFTER report_code,
  ADD COLUMN biz_date DATE DEFAULT NULL COMMENT 'Execution date parsed from SQL comment context' AFTER stage_code,
  ADD COLUMN query_date_start DATE DEFAULT NULL COMMENT 'Query date lower bound parsed from SQL body' AFTER biz_date,
  ADD COLUMN query_date_end DATE DEFAULT NULL COMMENT 'Query date upper bound parsed from SQL body' AFTER query_date_start,
  ADD COLUMN query_date_status VARCHAR(32) DEFAULT NULL COMMENT 'Query date extraction status such as RESOLVED/UNRESOLVED/PARTIAL' AFTER query_date_end,
  ADD COLUMN access_channel VARCHAR(32) DEFAULT NULL COMMENT 'Access channel such as PAGE/API/JDBC_AGENT/SDK/CLIENT' AFTER query_date_status,
  ADD COLUMN parameterized_sql_flag TINYINT(1) DEFAULT NULL COMMENT 'Whether the SQL was parameterized before binding' AFTER access_channel,
  ADD COLUMN binding_mode VARCHAR(16) DEFAULT NULL COMMENT 'Binding mode such as POSITIONAL/NAMED' AFTER parameterized_sql_flag,
  ADD COLUMN binding_render_status VARCHAR(16) DEFAULT NULL COMMENT 'Binding render status such as SUCCESS/PARTIAL/FAILED/MASKED' AFTER binding_mode,
  ADD COLUMN sql_template_fingerprint CHAR(32) DEFAULT NULL COMMENT 'Template SQL fingerprint before binding' AFTER binding_render_status,
  ADD COLUMN bound_sql_fingerprint CHAR(32) DEFAULT NULL COMMENT 'Bound SQL fingerprint after binding' AFTER sql_template_fingerprint,
  ADD COLUMN comment_context JSON DEFAULT NULL COMMENT 'Structured SQL comment context JSON' AFTER saga_id,
  ADD COLUMN binding_summary JSON DEFAULT NULL COMMENT 'Structured parameter binding summary JSON' AFTER comment_context,
  ADD COLUMN logical_object_hits JSON DEFAULT NULL COMMENT 'Structured logical object and table hit summary JSON' AFTER binding_summary,
  ADD COLUMN route_summary JSON DEFAULT NULL COMMENT 'Structured route decision summary JSON' AFTER logical_object_hits,
  ADD COLUMN cache_summary JSON DEFAULT NULL COMMENT 'Structured cache decision summary JSON' AFTER route_summary;

ALTER TABLE query_history
  ADD KEY idx_query_history_report_stage_date (tenant_id, report_code, stage_code, biz_date),
  ADD KEY idx_query_history_query_date (tenant_id, query_date_start, query_date_end),
  ADD KEY idx_query_history_datasource_code (tenant_id, datasource_code, create_time),
  ADD KEY idx_query_history_access_channel (tenant_id, access_channel, create_time),
  ADD KEY idx_query_history_binding_mode (tenant_id, binding_mode, create_time);
