DELIMITER //

CREATE PROCEDURE add_parse_batch_item_history_trace_if_missing()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'parse_batch_item'
      AND COLUMN_NAME = 'history_id'
  ) THEN
    ALTER TABLE parse_batch_item
      ADD COLUMN history_id VARCHAR(128) DEFAULT NULL COMMENT 'Governance query history id for this parsed SQL' AFTER failure_reason;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'parse_batch_item'
      AND COLUMN_NAME = 'history_persisted'
  ) THEN
    ALTER TABLE parse_batch_item
      ADD COLUMN history_persisted TINYINT(1) DEFAULT NULL COMMENT 'Whether governance parse history was persisted for this SQL' AFTER history_id;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'parse_batch_item'
      AND COLUMN_NAME = 'history_persistence_status'
  ) THEN
    ALTER TABLE parse_batch_item
      ADD COLUMN history_persistence_status VARCHAR(32) DEFAULT NULL COMMENT 'History write status such as SAVED, NO_RESPONSE, WRITE_FAILED, or WRITE_SKIPPED' AFTER history_persisted;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'parse_batch_item'
      AND INDEX_NAME = 'idx_parse_batch_item_history'
  ) THEN
    ALTER TABLE parse_batch_item
      ADD KEY idx_parse_batch_item_history (history_id);
  END IF;
END//

DELIMITER ;

CALL add_parse_batch_item_history_trace_if_missing();

DROP PROCEDURE add_parse_batch_item_history_trace_if_missing;
