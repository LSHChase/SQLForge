DELIMITER //

CREATE PROCEDURE add_report_batch_wide_sql_columns_if_missing()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report_batch_item'
      AND COLUMN_NAME = 'sql_column_name'
  ) THEN
    ALTER TABLE report_batch_item
      ADD COLUMN sql_column_name VARCHAR(128) DEFAULT NULL COMMENT 'Source spreadsheet column that supplied this SQL' AFTER source_file_line;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report_batch_item'
      AND COLUMN_NAME = 'sql_ordinal_in_report'
  ) THEN
    ALTER TABLE report_batch_item
      ADD COLUMN sql_ordinal_in_report INT DEFAULT NULL COMMENT '1-based SQL ordinal inside the same report_code' AFTER sql_column_name;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report_batch_item'
      AND INDEX_NAME = 'idx_report_batch_item_report_sql'
  ) THEN
    ALTER TABLE report_batch_item
      ADD KEY idx_report_batch_item_report_sql (batch_id, report_code, sql_ordinal_in_report);
  END IF;
END//

DELIMITER ;

CALL add_report_batch_wide_sql_columns_if_missing();

DROP PROCEDURE add_report_batch_wide_sql_columns_if_missing;
