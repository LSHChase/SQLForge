CREATE TABLE IF NOT EXISTS sql_parse_history (
  parse_history_id VARCHAR(128) NOT NULL COMMENT 'SQL parse history identifier owned by sql-optimization',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Owning tenant identifier',
  source_type VARCHAR(32) NOT NULL COMMENT 'Parse source such as STRUCTURE_PARSE/COMBINED_PARSE/PARSE_BATCH/REPORT_BATCH/END_OF_DAY_SLOW_SQL',
  source_id VARCHAR(128) DEFAULT NULL COMMENT 'Source item or execution history identifier',
  batch_key VARCHAR(255) DEFAULT NULL COMMENT 'Idempotent batch key for scheduled parse jobs',
  parse_task_id VARCHAR(64) NOT NULL COMMENT 'Structure or combined parse task identifier',
  sql_fingerprint CHAR(32) NOT NULL COMMENT 'Normalized SQL fingerprint',
  datasource_code VARCHAR(128) DEFAULT NULL COMMENT 'Datasource business identifier or alias',
  datasource_type VARCHAR(32) DEFAULT NULL COMMENT 'Datasource type',
  report_code VARCHAR(128) DEFAULT NULL COMMENT 'Report code parsed from SQL comment context',
  stage_code VARCHAR(32) DEFAULT NULL COMMENT 'Execution stage parsed from SQL comment context',
  biz_date VARCHAR(32) DEFAULT NULL COMMENT 'Business date parsed from SQL comment context',
  query_date_start VARCHAR(32) DEFAULT NULL COMMENT 'Query date lower bound parsed from SQL body',
  query_date_end VARCHAR(32) DEFAULT NULL COMMENT 'Query date upper bound parsed from SQL body',
  query_date_status VARCHAR(32) DEFAULT NULL COMMENT 'Query date extraction status such as RESOLVED/UNRESOLVED/PARTIAL',
  access_channel VARCHAR(32) DEFAULT NULL COMMENT 'Parse access channel or batch source',
  parser_mode VARCHAR(32) DEFAULT NULL COMMENT 'SQL parser mode: JSQLPARSER or APACHE_CALCITE',
  sql_text MEDIUMTEXT NOT NULL COMMENT 'SQL text captured for parse management',
  sql_template_text MEDIUMTEXT DEFAULT NULL COMMENT 'Prepared SQL template text',
  binding_mode VARCHAR(32) DEFAULT NULL COMMENT 'Binding mode such as POSITIONAL/NAMED',
  parameterized_sql_flag TINYINT(1) DEFAULT NULL COMMENT 'Whether the SQL was parameterized before binding',
  result_status VARCHAR(32) NOT NULL COMMENT 'Parse result status such as SUCCESS/PARTIAL/FAILED',
  target_engine VARCHAR(32) DEFAULT NULL COMMENT 'Target engine resolved by parse flow',
  structure_parse_summary_json JSON DEFAULT NULL COMMENT 'Structure parse summary payload',
  access_parse_summary_json JSON DEFAULT NULL COMMENT 'Access parse summary payload',
  result_summary_json JSON DEFAULT NULL COMMENT 'Combined parse result summary payload',
  result_payload_json JSON DEFAULT NULL COMMENT 'Full parse result payload',
  query_context_json JSON DEFAULT NULL COMMENT 'Structured query context for parse management',
  comment_context_json JSON DEFAULT NULL COMMENT 'Structured SQL comment context JSON',
  binding_summary_json JSON DEFAULT NULL COMMENT 'Structured parameter binding summary JSON',
  logical_object_hits_json JSON DEFAULT NULL COMMENT 'Structured logical object hit summary JSON',
  issue_scenes_json JSON DEFAULT NULL COMMENT 'Issue scene summary payload',
  logical_object_keys_json JSON DEFAULT NULL COMMENT 'Logical object key summary payload',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT 'Trace identifier',
  request_id VARCHAR(64) DEFAULT NULL COMMENT 'Request identifier',
  saga_id VARCHAR(128) DEFAULT NULL COMMENT 'Saga identifier',
  submitted_by VARCHAR(64) DEFAULT NULL COMMENT 'Operator identifier',
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Submitted timestamp',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
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
      MODIFY COLUMN history_id VARCHAR(128) DEFAULT NULL COMMENT 'SQL parse history id for this parsed SQL';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'parse_batch_item'
      AND COLUMN_NAME = 'history_persisted'
  ) THEN
    ALTER TABLE parse_batch_item
      MODIFY COLUMN history_persisted TINYINT(1) DEFAULT NULL COMMENT 'Whether SQL parse history was persisted for this SQL';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report_batch_item'
      AND COLUMN_NAME = 'history_id'
  ) THEN
    ALTER TABLE report_batch_item
      MODIFY COLUMN history_id VARCHAR(128) DEFAULT NULL COMMENT 'SQL parse history id for this parsed report SQL';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report_batch_item'
      AND COLUMN_NAME = 'history_persisted'
  ) THEN
    ALTER TABLE report_batch_item
      MODIFY COLUMN history_persisted TINYINT(1) DEFAULT NULL COMMENT 'Whether SQL parse history was persisted for this SQL';
  END IF;
END//

DELIMITER ;

CALL update_parse_batch_item_history_comment_for_sql_parse_history();

DROP PROCEDURE update_parse_batch_item_history_comment_for_sql_parse_history;
