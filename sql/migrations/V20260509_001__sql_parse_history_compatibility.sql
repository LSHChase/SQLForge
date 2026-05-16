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
      ALTER TABLE sql_parse_history ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT 'STRUCTURE_PARSE' COMMENT '解析来源，例如 STRUCTURE_PARSE/COMBINED_PARSE/PARSE_BATCH/REPORT_BATCH/END_OF_DAY_SLOW_SQL';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'source_id'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN source_id VARCHAR(128) DEFAULT NULL COMMENT '来源条目或执行历史标识符';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'batch_key'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN batch_key VARCHAR(255) DEFAULT NULL COMMENT '定时解析作业的幂等批次键';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'datasource_code'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN datasource_code VARCHAR(128) DEFAULT NULL COMMENT '数据源业务标识符或别名';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'datasource_type'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN datasource_type VARCHAR(32) DEFAULT NULL COMMENT '数据源类型';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'report_code'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN report_code VARCHAR(128) DEFAULT NULL COMMENT '从 SQL 注释上下文解析出的报表编码';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'stage_code'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN stage_code VARCHAR(32) DEFAULT NULL COMMENT '从 SQL 注释上下文解析出的执行阶段';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'biz_date'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN biz_date VARCHAR(32) DEFAULT NULL COMMENT '从 SQL 注释上下文解析出的业务日期';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'query_date_start'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN query_date_start VARCHAR(32) DEFAULT NULL COMMENT '从 SQL 正文解析出的查询日期下界';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'query_date_end'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN query_date_end VARCHAR(32) DEFAULT NULL COMMENT '从 SQL 正文解析出的查询日期上界';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'query_date_status'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN query_date_status VARCHAR(32) DEFAULT NULL COMMENT '查询日期提取状态，例如 RESOLVED/UNRESOLVED/PARTIAL';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'access_channel'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN access_channel VARCHAR(32) DEFAULT NULL COMMENT '解析访问渠道或批次来源';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'parser_mode'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN parser_mode VARCHAR(32) DEFAULT NULL COMMENT 'SQL 解析器模式';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'sql_template_text'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN sql_template_text MEDIUMTEXT DEFAULT NULL COMMENT '预备 SQL 模板文本';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'binding_mode'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN binding_mode VARCHAR(32) DEFAULT NULL COMMENT 'Binding 模式，例如 POSITIONAL/NAMED';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'parameterized_sql_flag'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN parameterized_sql_flag TINYINT(1) DEFAULT NULL COMMENT '绑定前 SQL 是否已参数化';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'target_engine'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN target_engine VARCHAR(32) DEFAULT NULL COMMENT '解析流程确定的目标引擎';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'structure_parse_summary_json'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN structure_parse_summary_json JSON DEFAULT NULL COMMENT '结构解析摘要载荷';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'access_parse_summary_json'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN access_parse_summary_json JSON DEFAULT NULL COMMENT '访问解析摘要载荷';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'result_summary_json'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN result_summary_json JSON DEFAULT NULL COMMENT '组合解析结果摘要载荷';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'result_payload_json'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN result_payload_json JSON DEFAULT NULL COMMENT '完整解析结果载荷';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'query_context_json'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN query_context_json JSON DEFAULT NULL COMMENT '用于解析管理的结构化查询上下文';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'comment_context_json'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN comment_context_json JSON DEFAULT NULL COMMENT '结构化 SQL 注释上下文 JSON';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'binding_summary_json'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN binding_summary_json JSON DEFAULT NULL COMMENT '结构化参数绑定摘要 JSON';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'logical_object_hits_json'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN logical_object_hits_json JSON DEFAULT NULL COMMENT '结构化逻辑对象命中摘要 JSON';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'issue_scenes_json'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN issue_scenes_json JSON DEFAULT NULL COMMENT '问题场景摘要载荷';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'logical_object_keys_json'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN logical_object_keys_json JSON DEFAULT NULL COMMENT '逻辑对象键摘要载荷';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'trace_id'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN trace_id VARCHAR(64) DEFAULT NULL COMMENT '追踪标识符';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'request_id'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN request_id VARCHAR(64) DEFAULT NULL COMMENT '请求标识符';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'saga_id'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN saga_id VARCHAR(128) DEFAULT NULL COMMENT '跨服务补偿链 Saga 标识符';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'submitted_by'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN submitted_by VARCHAR(64) DEFAULT NULL COMMENT '操作人标识符';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'created_at'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间戳';
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sql_parse_history' AND COLUMN_NAME = 'updated_at'
    ) THEN
      ALTER TABLE sql_parse_history ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间戳';
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
