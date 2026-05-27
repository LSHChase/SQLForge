CREATE TABLE IF NOT EXISTS sql_parse_history (
  parse_history_id VARCHAR(128) NOT NULL COMMENT 'sql-optimization 持有的 SQL 解析历史标识符',
  tenant_id VARCHAR(64) NOT NULL COMMENT '所属租户标识符',
  source_type VARCHAR(32) NOT NULL COMMENT '解析来源，例如 STRUCTURE_PARSE/COMBINED_PARSE/PARSE_BATCH/REPORT_BATCH/END_OF_DAY_SLOW_SQL',
  source_id VARCHAR(128) DEFAULT NULL COMMENT '来源条目或执行历史标识符',
  batch_key VARCHAR(255) DEFAULT NULL COMMENT '定时解析作业的幂等批次键',
  parse_task_id VARCHAR(64) NOT NULL COMMENT '结构或组合解析任务标识符',
  sql_fingerprint CHAR(32) NOT NULL COMMENT '归一化 SQL 指纹',
  datasource_code VARCHAR(128) DEFAULT NULL COMMENT '数据源业务标识符或别名',
  datasource_type VARCHAR(32) DEFAULT NULL COMMENT '数据源类型',
  report_code VARCHAR(128) DEFAULT NULL COMMENT '从 SQL 注释上下文解析出的报表编码',
  stage_code VARCHAR(32) DEFAULT NULL COMMENT '从 SQL 注释上下文解析出的执行阶段',
  biz_date VARCHAR(32) DEFAULT NULL COMMENT '从 SQL 注释上下文解析出的业务日期',
  query_date_start VARCHAR(32) DEFAULT NULL COMMENT '从 SQL 正文解析出的查询日期下界',
  query_date_end VARCHAR(32) DEFAULT NULL COMMENT '从 SQL 正文解析出的查询日期上界',
  query_date_status VARCHAR(32) DEFAULT NULL COMMENT '查询日期提取状态，例如 RESOLVED/UNRESOLVED/PARTIAL',
  access_channel VARCHAR(32) DEFAULT NULL COMMENT '解析访问渠道或批次来源',
  parser_mode VARCHAR(32) DEFAULT NULL COMMENT 'SQL 解析器模式：APACHE_CALCITE 或 APACHE_CALCITE_WITH_PLAN',
  sql_text MEDIUMTEXT NOT NULL COMMENT '解析管理捕获的 SQL 文本',
  sql_template_text MEDIUMTEXT DEFAULT NULL COMMENT '预备 SQL 模板文本',
  binding_mode VARCHAR(32) DEFAULT NULL COMMENT 'Binding 模式，例如 POSITIONAL/NAMED',
  parameterized_sql_flag TINYINT(1) DEFAULT NULL COMMENT '绑定前 SQL 是否已参数化',
  result_status VARCHAR(32) NOT NULL COMMENT '解析结果状态，例如 SUCCESS/PARTIAL/FAILED',
  target_engine VARCHAR(32) DEFAULT NULL COMMENT '解析流程确定的目标引擎',
  structure_parse_summary_json JSON DEFAULT NULL COMMENT '结构解析摘要载荷',
  access_parse_summary_json JSON DEFAULT NULL COMMENT '访问解析摘要载荷',
  result_summary_json JSON DEFAULT NULL COMMENT '组合解析结果摘要载荷',
  result_payload_json JSON DEFAULT NULL COMMENT '完整解析结果载荷',
  query_context_json JSON DEFAULT NULL COMMENT '用于解析管理的结构化查询上下文',
  comment_context_json JSON DEFAULT NULL COMMENT '结构化 SQL 注释上下文 JSON',
  binding_summary_json JSON DEFAULT NULL COMMENT '结构化参数绑定摘要 JSON',
  logical_object_hits_json JSON DEFAULT NULL COMMENT '结构化逻辑对象命中摘要 JSON',
  issue_scenes_json JSON DEFAULT NULL COMMENT '问题场景摘要载荷',
  logical_object_keys_json JSON DEFAULT NULL COMMENT '逻辑对象键摘要载荷',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT '追踪标识符',
  request_id VARCHAR(64) DEFAULT NULL COMMENT '请求标识符',
  saga_id VARCHAR(128) DEFAULT NULL COMMENT '跨服务补偿链 Saga 标识符',
  submitted_by VARCHAR(64) DEFAULT NULL COMMENT '操作人标识符',
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间戳',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间戳',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间戳',
  PRIMARY KEY (parse_history_id),
  KEY idx_sql_parse_history_tenant_time (tenant_id, submitted_at),
  KEY idx_sql_parse_history_fingerprint (tenant_id, sql_fingerprint),
  KEY idx_sql_parse_history_source (tenant_id, source_type, source_id),
  KEY idx_sql_parse_history_batch (tenant_id, batch_key, sql_fingerprint),
  KEY idx_sql_parse_history_report_stage_date (tenant_id, report_code, stage_code, biz_date),
  KEY idx_sql_parse_history_datasource (tenant_id, datasource_code, submitted_at),
  KEY idx_sql_parse_history_trace (trace_id, request_id),
  KEY idx_sql_parse_history_status (tenant_id, result_status, submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Independent SQL parse history records owned by sql-optimization';

DELIMITER //

CREATE PROCEDURE update_parse_batch_item_history_comment_for_sql_parse_history()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'parse_batch_item'
      AND COLUMN_NAME = 'history_id'
  ) THEN
    ALTER TABLE parse_batch_item
      MODIFY COLUMN history_id VARCHAR(128) DEFAULT NULL COMMENT '该已解析 SQL 的 SQL 解析历史 ID';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'parse_batch_item'
      AND COLUMN_NAME = 'history_persisted'
  ) THEN
    ALTER TABLE parse_batch_item
      MODIFY COLUMN history_persisted TINYINT(1) DEFAULT NULL COMMENT '该 SQL 是否已持久化 SQL 解析历史';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report_batch_item'
      AND COLUMN_NAME = 'history_id'
  ) THEN
    ALTER TABLE report_batch_item
      MODIFY COLUMN history_id VARCHAR(128) DEFAULT NULL COMMENT '该已解析报表 SQL 的 SQL 解析历史 ID';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report_batch_item'
      AND COLUMN_NAME = 'history_persisted'
  ) THEN
    ALTER TABLE report_batch_item
      MODIFY COLUMN history_persisted TINYINT(1) DEFAULT NULL COMMENT '该 SQL 是否已持久化 SQL 解析历史';
  END IF;
END//

DELIMITER ;

CALL update_parse_batch_item_history_comment_for_sql_parse_history();

DROP PROCEDURE update_parse_batch_item_history_comment_for_sql_parse_history;
