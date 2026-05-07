DELIMITER //

CREATE PROCEDURE add_sql_parser_mode_if_missing()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'parse_batch'
      AND COLUMN_NAME = 'parser_mode'
  ) THEN
    ALTER TABLE parse_batch
      ADD COLUMN parser_mode VARCHAR(32) NOT NULL DEFAULT 'JSQLPARSER' COMMENT 'SQL parser mode: JSQLPARSER or APACHE_CALCITE' AFTER datasource_code;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report_batch'
      AND COLUMN_NAME = 'parser_mode'
  ) THEN
    ALTER TABLE report_batch
      ADD COLUMN parser_mode VARCHAR(32) NOT NULL DEFAULT 'JSQLPARSER' COMMENT 'SQL parser mode: JSQLPARSER or APACHE_CALCITE' AFTER priority;
  END IF;
END//

DELIMITER ;

CALL add_sql_parser_mode_if_missing();

DROP PROCEDURE add_sql_parser_mode_if_missing;
