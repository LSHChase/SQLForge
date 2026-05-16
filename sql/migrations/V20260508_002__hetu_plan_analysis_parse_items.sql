DELIMITER //

CREATE PROCEDURE add_hetu_plan_analysis_parse_item_columns()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'parse_batch_item'
      AND COLUMN_NAME = 'plan_analysis_status'
  ) THEN
    ALTER TABLE parse_batch_item
      ADD COLUMN plan_analysis_status VARCHAR(32) DEFAULT NULL COMMENT 'Hetu EXPLAIN 计划分析状态'
      AFTER access_connection_status;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'parse_batch_item'
      AND COLUMN_NAME = 'combined_analysis_status'
  ) THEN
    ALTER TABLE parse_batch_item
      ADD COLUMN combined_analysis_status VARCHAR(32) DEFAULT NULL COMMENT '结构与计划联合分析状态'
      AFTER plan_analysis_status;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'parse_batch_item'
      AND COLUMN_NAME = 'plan_analysis_json'
  ) THEN
    ALTER TABLE parse_batch_item
      ADD COLUMN plan_analysis_json JSON DEFAULT NULL COMMENT 'Hetu EXPLAIN 计划分析摘要载荷'
      AFTER combined_analysis_status;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report_batch_item'
      AND COLUMN_NAME = 'plan_analysis_status'
  ) THEN
    ALTER TABLE report_batch_item
      ADD COLUMN plan_analysis_status VARCHAR(32) DEFAULT NULL COMMENT 'Hetu EXPLAIN 计划分析状态'
      AFTER access_connection_status;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report_batch_item'
      AND COLUMN_NAME = 'combined_analysis_status'
  ) THEN
    ALTER TABLE report_batch_item
      ADD COLUMN combined_analysis_status VARCHAR(32) DEFAULT NULL COMMENT '结构与计划联合分析状态'
      AFTER plan_analysis_status;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report_batch_item'
      AND COLUMN_NAME = 'plan_analysis_json'
  ) THEN
    ALTER TABLE report_batch_item
      ADD COLUMN plan_analysis_json JSON DEFAULT NULL COMMENT 'Hetu EXPLAIN 计划分析摘要载荷'
      AFTER combined_analysis_status;
  END IF;
END//

DELIMITER ;

CALL add_hetu_plan_analysis_parse_item_columns();

DROP PROCEDURE add_hetu_plan_analysis_parse_item_columns;
