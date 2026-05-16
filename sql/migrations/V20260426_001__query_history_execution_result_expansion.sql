ALTER TABLE execution_result
  ADD COLUMN access_channel VARCHAR(32) DEFAULT NULL COMMENT '访问渠道，例如 PAGE/API/JDBC_AGENT/SDK/CLIENT' AFTER saga_id,
  ADD COLUMN target_engine VARCHAR(64) DEFAULT NULL COMMENT '已选择执行引擎或路由引擎' AFTER access_channel,
  ADD COLUMN returned_row_count BIGINT DEFAULT NULL COMMENT '已知时的返回行数' AFTER target_engine,
  ADD COLUMN cache_hit TINYINT(1) DEFAULT NULL COMMENT '是否命中缓存' AFTER returned_row_count,
  ADD COLUMN rewrite_applied TINYINT(1) DEFAULT NULL COMMENT '是否应用轻量改写' AFTER cache_hit,
  ADD COLUMN acceleration_applied TINYINT(1) DEFAULT NULL COMMENT '是否应用加速路径' AFTER rewrite_applied,
  ADD COLUMN hit_table_summary JSON DEFAULT NULL COMMENT '结构化命中表摘要 JSON' AFTER acceleration_applied,
  ADD COLUMN route_summary JSON DEFAULT NULL COMMENT '结构化路由摘要 JSON' AFTER hit_table_summary,
  ADD COLUMN cache_summary JSON DEFAULT NULL COMMENT '结构化缓存摘要 JSON' AFTER route_summary;

ALTER TABLE execution_result
  ADD KEY idx_execution_result_access_channel (tenant_id, access_channel, create_time),
  ADD KEY idx_execution_result_target_engine (tenant_id, target_engine, create_time),
  ADD KEY idx_execution_result_cache_hit (tenant_id, cache_hit, create_time);

ALTER TABLE query_history
  ADD COLUMN sql_template_cipher MEDIUMBLOB DEFAULT NULL COMMENT '参数化查询的加密模板 SQL 载荷' AFTER sql_text_cipher,
  ADD COLUMN bound_sql_text_cipher MEDIUMBLOB DEFAULT NULL COMMENT '参数绑定后的加密绑定 SQL 载荷' AFTER sql_template_cipher,
  ADD COLUMN datasource_code VARCHAR(128) DEFAULT NULL COMMENT '数据源业务标识符或别名' AFTER bound_sql_text_cipher,
  ADD COLUMN report_code VARCHAR(128) DEFAULT NULL COMMENT '从 SQL 注释上下文解析出的报表编码' AFTER datasource_type,
  ADD COLUMN stage_code VARCHAR(32) DEFAULT NULL COMMENT '从 SQL 注释上下文解析出的执行阶段' AFTER report_code,
  ADD COLUMN biz_date DATE DEFAULT NULL COMMENT '从 SQL 注释上下文解析出的执行日期' AFTER stage_code,
  ADD COLUMN query_date_start DATE DEFAULT NULL COMMENT '从 SQL 正文解析出的查询日期下界' AFTER biz_date,
  ADD COLUMN query_date_end DATE DEFAULT NULL COMMENT '从 SQL 正文解析出的查询日期上界' AFTER query_date_start,
  ADD COLUMN query_date_status VARCHAR(32) DEFAULT NULL COMMENT '查询日期提取状态，例如 RESOLVED/UNRESOLVED/PARTIAL' AFTER query_date_end,
  ADD COLUMN access_channel VARCHAR(32) DEFAULT NULL COMMENT '访问渠道，例如 PAGE/API/JDBC_AGENT/SDK/CLIENT' AFTER query_date_status,
  ADD COLUMN parameterized_sql_flag TINYINT(1) DEFAULT NULL COMMENT '绑定前 SQL 是否已参数化' AFTER access_channel,
  ADD COLUMN binding_mode VARCHAR(16) DEFAULT NULL COMMENT 'Binding 模式，例如 POSITIONAL/NAMED' AFTER parameterized_sql_flag,
  ADD COLUMN binding_render_status VARCHAR(16) DEFAULT NULL COMMENT '绑定渲染状态，例如 SUCCESS/PARTIAL/FAILED/MASKED' AFTER binding_mode,
  ADD COLUMN sql_template_fingerprint CHAR(32) DEFAULT NULL COMMENT '绑定前模板 SQL 指纹' AFTER binding_render_status,
  ADD COLUMN bound_sql_fingerprint CHAR(32) DEFAULT NULL COMMENT '绑定后 SQL 指纹' AFTER sql_template_fingerprint,
  ADD COLUMN comment_context JSON DEFAULT NULL COMMENT '结构化 SQL 注释上下文 JSON' AFTER saga_id,
  ADD COLUMN binding_summary JSON DEFAULT NULL COMMENT '结构化参数绑定摘要 JSON' AFTER comment_context,
  ADD COLUMN logical_object_hits JSON DEFAULT NULL COMMENT '结构化逻辑对象与表命中摘要 JSON' AFTER binding_summary,
  ADD COLUMN route_summary JSON DEFAULT NULL COMMENT '结构化路由决策摘要 JSON' AFTER logical_object_hits,
  ADD COLUMN cache_summary JSON DEFAULT NULL COMMENT '结构化缓存决策摘要 JSON' AFTER route_summary;

ALTER TABLE query_history
  ADD KEY idx_query_history_report_stage_date (tenant_id, report_code, stage_code, biz_date),
  ADD KEY idx_query_history_query_date (tenant_id, query_date_start, query_date_end),
  ADD KEY idx_query_history_datasource_code (tenant_id, datasource_code, create_time),
  ADD KEY idx_query_history_access_channel (tenant_id, access_channel, create_time),
  ADD KEY idx_query_history_binding_mode (tenant_id, binding_mode, create_time);
