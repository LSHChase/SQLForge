-- Repair legacy local schemas that were created while rewrite runtime fields
-- were still named publish_status / published_sql_fingerprint.
-- Current application code reads and writes activation_status /
-- activated_sql_fingerprint.

SET @has_activation_status := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sql_rewrite_record'
    AND COLUMN_NAME = 'activation_status'
);

SET @add_activation_status := IF(
  @has_activation_status = 0,
  'ALTER TABLE sql_rewrite_record ADD COLUMN activation_status VARCHAR(32) NOT NULL DEFAULT ''INACTIVE'' COMMENT ''激活状态：INACTIVE/ACTIVATING/ACTIVE/PAUSED/PAUSING/PAUSE_FAILED/ACTIVATE_FAILED'' AFTER reviewed_at',
  'SELECT 1'
);
PREPARE add_activation_status_stmt FROM @add_activation_status;
EXECUTE add_activation_status_stmt;
DEALLOCATE PREPARE add_activation_status_stmt;

SET @has_publish_status := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sql_rewrite_record'
    AND COLUMN_NAME = 'publish_status'
);

SET @backfill_activation_status := IF(
  @has_publish_status > 0,
  'UPDATE sql_rewrite_record SET activation_status = CASE publish_status WHEN ''PUBLISHED'' THEN ''ACTIVE'' WHEN ''PUBLISH_FAILED'' THEN ''ACTIVATE_FAILED'' WHEN ''PAUSED'' THEN ''PAUSED'' ELSE ''INACTIVE'' END WHERE activation_status IS NULL OR activation_status IN (''INACTIVE'', ''UNPUBLISHED'')',
  'SELECT 1'
);
PREPARE backfill_activation_status_stmt FROM @backfill_activation_status;
EXECUTE backfill_activation_status_stmt;
DEALLOCATE PREPARE backfill_activation_status_stmt;

SET @has_activated_sql_fingerprint := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sql_rewrite_record'
    AND COLUMN_NAME = 'activated_sql_fingerprint'
);

SET @add_activated_sql_fingerprint := IF(
  @has_activated_sql_fingerprint = 0,
  'ALTER TABLE sql_rewrite_record ADD COLUMN activated_sql_fingerprint VARCHAR(128) DEFAULT NULL COMMENT ''激活到运行时绑定的 SQL 指纹'' AFTER runtime_binding_scope',
  'SELECT 1'
);
PREPARE add_activated_sql_fingerprint_stmt FROM @add_activated_sql_fingerprint;
EXECUTE add_activated_sql_fingerprint_stmt;
DEALLOCATE PREPARE add_activated_sql_fingerprint_stmt;

SET @has_published_sql_fingerprint := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sql_rewrite_record'
    AND COLUMN_NAME = 'published_sql_fingerprint'
);

SET @backfill_activated_sql_fingerprint := IF(
  @has_published_sql_fingerprint > 0,
  'UPDATE sql_rewrite_record SET activated_sql_fingerprint = COALESCE(activated_sql_fingerprint, published_sql_fingerprint) WHERE published_sql_fingerprint IS NOT NULL',
  'SELECT 1'
);
PREPARE backfill_activated_sql_fingerprint_stmt FROM @backfill_activated_sql_fingerprint;
EXECUTE backfill_activated_sql_fingerprint_stmt;
DEALLOCATE PREPARE backfill_activated_sql_fingerprint_stmt;

SET @has_activation_index := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sql_rewrite_record'
    AND INDEX_NAME = 'idx_rewrite_record_activation'
);

SET @add_activation_index := IF(
  @has_activation_index = 0,
  'ALTER TABLE sql_rewrite_record ADD KEY idx_rewrite_record_activation (tenant_id, activation_status, updated_at)',
  'SELECT 1'
);
PREPARE add_activation_index_stmt FROM @add_activation_index;
EXECUTE add_activation_index_stmt;
DEALLOCATE PREPARE add_activation_index_stmt;
