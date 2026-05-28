DELIMITER //

CREATE PROCEDURE expand_report_batch_source_file_line_locator()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report_batch_item'
      AND COLUMN_NAME = 'source_file_line'
  ) THEN
    ALTER TABLE report_batch_item
      MODIFY COLUMN source_file_line VARCHAR(4096) DEFAULT NULL COMMENT '源行、源列和报表编码定位摘要';
  END IF;
END//

DELIMITER ;

CALL expand_report_batch_source_file_line_locator();

DROP PROCEDURE expand_report_batch_source_file_line_locator;
