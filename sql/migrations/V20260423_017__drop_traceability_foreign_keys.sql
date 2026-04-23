DROP PROCEDURE IF EXISTS drop_foreign_key_if_exists;

DELIMITER $$
CREATE PROCEDURE drop_foreign_key_if_exists(IN p_table_name VARCHAR(64), IN p_constraint_name VARCHAR(64))
BEGIN
  DECLARE constraint_count BIGINT DEFAULT 0;

  SELECT COUNT(*)
    INTO constraint_count
  FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE()
    AND table_name = p_table_name
    AND constraint_name = p_constraint_name
    AND constraint_type = 'FOREIGN KEY';

  IF constraint_count > 0 THEN
    SET @ddl = CONCAT(
      'ALTER TABLE `',
      p_table_name,
      '` DROP FOREIGN KEY `',
      p_constraint_name,
      '`'
    );
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL drop_foreign_key_if_exists('execution_result', 'fk_execution_result_config_snapshot');
CALL drop_foreign_key_if_exists('query_history', 'fk_query_history_result');
CALL drop_foreign_key_if_exists('export_record', 'fk_export_record_history');
CALL drop_foreign_key_if_exists('export_record', 'fk_export_record_result');
CALL drop_foreign_key_if_exists('audit_log', 'fk_audit_log_config_snapshot');
CALL drop_foreign_key_if_exists('audit_log', 'fk_audit_log_result');
CALL drop_foreign_key_if_exists('audit_log', 'fk_audit_log_history');
CALL drop_foreign_key_if_exists('audit_log', 'fk_audit_log_export');

DROP PROCEDURE IF EXISTS drop_foreign_key_if_exists;
