SET NAMES utf8mb4;

USE sqlforge;

INSERT INTO tenant_config (
  id,
  tenant_id,
  quota_concurrent,
  quota_storage,
  default_engine,
  backup_engine,
  audit_level,
  retention_days,
  acceleration_quota
) VALUES (
  1,
  'system',
  20,
  2048,
  'HETU',
  'HIVE',
  'NORMAL',
  180,
  50
)
ON DUPLICATE KEY UPDATE
  quota_concurrent = VALUES(quota_concurrent),
  quota_storage = VALUES(quota_storage),
  default_engine = VALUES(default_engine),
  backup_engine = VALUES(backup_engine),
  audit_level = VALUES(audit_level),
  retention_days = VALUES(retention_days),
  acceleration_quota = VALUES(acceleration_quota),
  update_time = CURRENT_TIMESTAMP;

INSERT INTO system_config (config_key, config_value, config_type, description) VALUES
  ('system.defaultEngine', 'HETU', 'STRING', 'Platform default execution engine'),
  ('system.backupEngine', 'HIVE', 'STRING', 'Platform fallback execution engine'),
  ('system.defaultAuditLevel', 'NORMAL', 'STRING', 'Platform default audit level'),
  ('system.auditRetentionDays', '180', 'INTEGER', 'Minimum audit log retention days'),
  ('system.backupPolicy', 'RPO<1h,RTO<4h,encrypted-backup-required', 'STRING', 'Backup and recovery baseline')
ON DUPLICATE KEY UPDATE
  config_value = VALUES(config_value),
  config_type = VALUES(config_type),
  description = VALUES(description),
  update_time = CURRENT_TIMESTAMP;

INSERT INTO system_dictionary (
  dictionary_type,
  dictionary_code,
  dictionary_name,
  sort_order,
  enabled_flag,
  remark
) VALUES
  ('dataSourceType', 'HETU', 'Hetu', 1, 1, 'Default distributed SQL engine'),
  ('dataSourceType', 'HIVE', 'Hive', 2, 1, 'Hive SQL engine'),
  ('dataSourceType', 'SPARK', 'Spark', 3, 1, 'Spark SQL engine'),
  ('dataSourceType', 'CLICKHOUSE', 'ClickHouse', 4, 1, 'ClickHouse analytical engine'),
  ('dataSourceType', 'GAUSSDB', 'GaussDB', 5, 1, 'GaussDB engine'),
  ('dataSourceType', 'AUTO', 'Auto Route', 6, 1, 'Route automatically by governance rules'),
  ('accelerationType', 'PRECOMPUTE', 'Precompute', 1, 1, 'Precompute acceleration'),
  ('accelerationType', 'PARTITION', 'Partition', 2, 1, 'Partition acceleration'),
  ('accelerationType', 'BUCKET', 'Bucket', 3, 1, 'Bucket acceleration'),
  ('accelerationType', 'SPLIT', 'Split', 4, 1, 'Split acceleration'),
  ('accelerationType', 'REPLACE', 'Replace', 5, 1, 'Replace acceleration'),
  ('messagingStatus', 'PENDING', 'Pending', 1, 1, 'R-144 database queue pending message'),
  ('messagingStatus', 'SENT', 'Sent', 2, 1, 'R-144 database queue sent message'),
  ('messagingStatus', 'CONSUMED', 'Consumed', 3, 1, 'R-144 database queue consumed message'),
  ('messagingStatus', 'FAILED', 'Failed', 4, 1, 'R-144 database queue failed message'),
  ('auditLevel', 'STRICT', 'Strict', 1, 1, 'High risk statements require strict review'),
  ('auditLevel', 'NORMAL', 'Normal', 2, 1, 'Default audit review level'),
  ('auditLevel', 'LOOSE', 'Loose', 3, 1, 'Record only with low intervention')
ON DUPLICATE KEY UPDATE
  dictionary_name = VALUES(dictionary_name),
  sort_order = VALUES(sort_order),
  enabled_flag = VALUES(enabled_flag),
  remark = VALUES(remark),
  update_time = CURRENT_TIMESTAMP;
