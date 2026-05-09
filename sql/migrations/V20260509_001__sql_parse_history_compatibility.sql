DELIMITER //

CREATE PROCEDURE ensure_sql_parse_history_compatibility()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sql_parse_history'
  ) THEN
    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'source_type'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT 'STRUCTURE_PARSE' COMMENT 'Parse source such as STRUCTURE_PARSE/COMBINED_PARSE/PARSE_BATCH/REPORT_BATCH/END_OF_DAY_SLOW_SQL';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'source_id'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN source_id VARCHAR(128) DEFAULT NULL COMMENT 'Source item or execution history identifier';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'batch_key'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN batch_key VARCHAR(255) DEFAULT NULL COMMENT 'Idempotent batch key for scheduled parse jobs';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'datasource_code'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN datasource_code VARCHAR(128) DEFAULT NULL COMMENT 'Datasource business identifier or alias';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'datasource_type'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN datasource_type VARCHAR(32) DEFAULT NULL COMMENT 'Datasource type';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'report_code'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN report_code VARCHAR(128) DEFAULT NULL COMMENT 'Report code parsed from SQL comment context';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'stage_code'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN stage_code VARCHAR(32) DEFAULT NULL COMMENT 'Execution stage parsed from SQL comment context';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'biz_date'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN biz_date VARCHAR(32) DEFAULT NULL COMMENT 'Business date parsed from SQL comment context';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'query_date_start'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN query_date_start VARCHAR(32) DEFAULT NULL COMMENT 'Query date lower bound parsed from SQL body';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'query_date_end'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN query_date_end VARCHAR(32) DEFAULT NULL COMMENT 'Query date upper bound parsed from SQL body';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'query_date_status'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN query_date_status VARCHAR(32) DEFAULT NULL COMMENT 'Query date extraction status such as RESOLVED/UNRESOLVED/PARTIAL';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'access_channel'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN access_channel VARCHAR(32) DEFAULT NULL COMMENT 'Parse access channel or batch source';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'parser_mode'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN parser_mode VARCHAR(32) DEFAULT NULL COMMENT 'SQL parser mode';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'sql_template_text'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN sql_template_text MEDIUMTEXT DEFAULT NULL COMMENT 'Prepared SQL template text';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'binding_mode'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN binding_mode VARCHAR(32) DEFAULT NULL COMMENT 'Binding mode such as POSITIONAL/NAMED';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'parameterized_sql_flag'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN parameterized_sql_flag TINYINT(1) DEFAULT NULL COMMENT 'Whether the SQL was parameterized before binding';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'target_engine'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN target_engine VARCHAR(32) DEFAULT NULL COMMENT 'Target engine resolved by parse flow';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'structure_parse_summary_json'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN structure_parse_summary_json JSON DEFAULT NULL COMMENT 'Structure parse summary payload';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'access_parse_summary_json'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN access_parse_summary_json JSON DEFAULT NULL COMMENT 'Access parse summary payload';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'result_summary_json'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN result_summary_json JSON DEFAULT NULL COMMENT 'Combined parse result summary payload';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'result_payload_json'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN result_payload_json JSON DEFAULT NULL COMMENT 'Full parse result payload';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'query_context_json'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN query_context_json JSON DEFAULT NULL COMMENT 'Structured query context for parse management';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'comment_context_json'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN comment_context_json JSON DEFAULT NULL COMMENT 'Structured SQL comment context JSON';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'binding_summary_json'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN binding_summary_json JSON DEFAULT NULL COMMENT 'Structured parameter binding summary JSON';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'logical_object_hits_json'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN logical_object_hits_json JSON DEFAULT NULL COMMENT 'Structured logical object hit summary JSON';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'issue_scenes_json'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN issue_scenes_json JSON DEFAULT NULL COMMENT 'Issue scene summary payload';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'logical_object_keys_json'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN logical_object_keys_json JSON DEFAULT NULL COMMENT 'Logical object key summary payload';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'trace_id'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN trace_id VARCHAR(64) DEFAULT NULL COMMENT 'Trace identifier';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'request_id'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN request_id VARCHAR(64) DEFAULT NULL COMMENT 'Request identifier';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'saga_id'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN saga_id VARCHAR(128) DEFAULT NULL COMMENT 'Saga identifier';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'submitted_by'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN submitted_by VARCHAR(64) DEFAULT NULL COMMENT 'Operator identifier';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'created_at'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'updated_at'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND INDEX_NAME = 'idx_sql_parse_history_batch'
    ) THEN
      ALTER TABLE sql_parse_history ADD KEY idx_sql_parse_history_batch (tenant_id, batch_key, sql_fingerprint);
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND INDEX_NAME = 'idx_sql_parse_history_report_stage_date'
    ) THEN
      ALTER TABLE sql_parse_history ADD KEY idx_sql_parse_history_report_stage_date (tenant_id, report_code, stage_code, biz_date);
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND INDEX_NAME = 'idx_sql_parse_history_trace'
    ) THEN
      ALTER TABLE sql_parse_history ADD KEY idx_sql_parse_history_trace (trace_id, request_id);
    END IF;
  END IF;
END//

DELIMITER ;

CALL ensure_sql_parse_history_compatibility();

DROP PROCEDURE ensure_sql_parse_history_compatibility;
