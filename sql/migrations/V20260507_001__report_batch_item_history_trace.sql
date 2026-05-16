DELIMITER //

CREATE PROCEDURE add_report_batch_item_history_trace_if_missing()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report_batch_item'
      AND COLUMN_NAME = 'history_id'
  ) THEN
    ALTER TABLE report_batch_item
      ADD COLUMN history_id VARCHAR(128) DEFAULT NULL COMMENT '该已解析报表 SQL 的治理查询历史 ID' AFTER failure_reason;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report_batch_item'
      AND COLUMN_NAME = 'history_persisted'
  ) THEN
    ALTER TABLE report_batch_item
      ADD COLUMN history_persisted TINYINT(1) DEFAULT NULL COMMENT '该 SQL 是否已持久化治理解析历史' AFTER history_id;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report_batch_item'
      AND COLUMN_NAME = 'history_persistence_status'
  ) THEN
    ALTER TABLE report_batch_item
      ADD COLUMN history_persistence_status VARCHAR(32) DEFAULT NULL COMMENT '历史写入状态，例如 SAVED、NO_RESPONSE、WRITE_FAILED 或 WRITE_SKIPPED' AFTER history_persisted;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report_batch_item'
      AND INDEX_NAME = 'idx_report_batch_item_history'
  ) THEN
    ALTER TABLE report_batch_item
      ADD KEY idx_report_batch_item_history (history_id);
  END IF;
END//

DELIMITER ;

CALL add_report_batch_item_history_trace_if_missing();

DROP PROCEDURE add_report_batch_item_history_trace_if_missing;
