SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS sqlforge
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE sqlforge;

-- SQLForge initialization schema
-- Compliance notes:
-- 1. audit_log must use dedicated storage/tablespace strategy in production operations.
-- 2. Encrypted fields must store AES-256 or SM4 ciphertext only, never plaintext.
-- 3. Backup target: RPO < 1 hour, RTO < 4 hours, encrypted backup media.

CREATE TABLE IF NOT EXISTS tenant_config (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Tenant identifier, unique across platform',
  quota_concurrent INT NOT NULL DEFAULT 10 COMMENT 'Max concurrent queries per tenant',
  quota_storage BIGINT NOT NULL DEFAULT 1024 COMMENT 'Storage quota in GB',
  default_engine VARCHAR(32) NOT NULL COMMENT 'Default execution engine',
  backup_engine VARCHAR(32) NOT NULL COMMENT 'Fallback execution engine',
  audit_level VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT 'Audit review level: STRICT/NORMAL/LOOSE',
  retention_days INT NOT NULL DEFAULT 180 COMMENT 'Retention policy in days',
  acceleration_quota INT NOT NULL DEFAULT 20 COMMENT 'Acceleration config quota',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_config_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tenant governance configuration';

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Audit log primary key',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Tenant identifier',
  operation_type VARCHAR(64) NOT NULL COMMENT 'Operation type such as LOGIN, QUERY, PERMISSION_CHANGE',
  target_type VARCHAR(64) NOT NULL COMMENT 'Target type such as SQL_JOB, DATASOURCE, USER',
  target_id VARCHAR(128) NOT NULL COMMENT 'Target business identifier',
  request_params JSON NULL COMMENT 'Desensitized request parameters, no raw secret allowed',
  response_summary TEXT NULL COMMENT 'Response summary without sensitive data',
  status VARCHAR(16) NOT NULL COMMENT 'Operation result: SUCCESS/FAILED/PARTIAL',
  cost_ms BIGINT NOT NULL DEFAULT 0 COMMENT 'Processing latency in milliseconds',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Audit creation time, retain for at least 180 days',
  PRIMARY KEY (id),
  KEY idx_audit_log_tenant_time (tenant_id, create_time),
  KEY idx_audit_log_operation_time (operation_type, create_time),
  KEY idx_audit_log_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Immutable audit logs, production should isolate storage strategy';

CREATE TABLE IF NOT EXISTS sql_query_job (
  job_id VARCHAR(64) NOT NULL COMMENT 'SQL query job identifier',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Tenant identifier',
  sql_text MEDIUMBLOB NOT NULL COMMENT 'Encrypted SQL text payload, ciphertext only',
  parse_mode VARCHAR(16) NOT NULL DEFAULT 'LIGHT' COMMENT 'Parse mode: LIGHT/DEEP',
  data_source_type VARCHAR(32) NOT NULL COMMENT 'Data source type',
  status VARCHAR(32) NOT NULL COMMENT 'Job status',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  finish_time DATETIME NULL DEFAULT NULL COMMENT 'Finish timestamp',
  PRIMARY KEY (job_id),
  KEY idx_sql_query_job_tenant_time (tenant_id, create_time),
  KEY idx_sql_query_job_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SQL lifecycle query jobs';

CREATE TABLE IF NOT EXISTS execution_plan (
  plan_id VARCHAR(64) NOT NULL COMMENT 'Execution plan identifier',
  job_id VARCHAR(64) NOT NULL COMMENT 'Related query job identifier',
  target_engine VARCHAR(32) NOT NULL COMMENT 'Selected execution engine',
  fallback_engine VARCHAR(32) DEFAULT NULL COMMENT 'Configured fallback engine',
  ast_summary JSON NULL COMMENT 'AST summary payload',
  estimated_cost DECIMAL(18,4) DEFAULT NULL COMMENT 'Estimated execution cost',
  actual_cost DECIMAL(18,4) DEFAULT NULL COMMENT 'Actual execution cost',
  degraded_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 means fallback or degraded mode applied',
  PRIMARY KEY (plan_id),
  KEY idx_execution_plan_job_id (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Execution plans and fallback metadata';

CREATE TABLE IF NOT EXISTS benchmark_report (
  report_id VARCHAR(64) NOT NULL COMMENT 'Benchmark report identifier',
  sql_fingerprint CHAR(32) NOT NULL COMMENT 'Normalized SQL fingerprint',
  engine_type VARCHAR(32) NOT NULL COMMENT 'Benchmarked engine type',
  qps_baseline DECIMAL(18,4) DEFAULT NULL COMMENT 'Baseline QPS',
  p50_latency DECIMAL(18,4) DEFAULT NULL COMMENT 'P50 latency in ms',
  p99_latency DECIMAL(18,4) DEFAULT NULL COMMENT 'P99 latency in ms',
  cpu_usage DECIMAL(8,4) DEFAULT NULL COMMENT 'CPU usage ratio',
  memory_usage DECIMAL(18,4) DEFAULT NULL COMMENT 'Memory usage in MB',
  scan_rows BIGINT DEFAULT NULL COMMENT 'Rows scanned',
  verdict VARCHAR(32) NOT NULL COMMENT 'Benchmark verdict',
  PRIMARY KEY (report_id),
  KEY idx_benchmark_report_fingerprint (sql_fingerprint),
  KEY idx_benchmark_report_engine (engine_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Benchmark reports for engine comparison';

CREATE TABLE IF NOT EXISTS acceleration_config (
  config_id VARCHAR(64) NOT NULL COMMENT 'Acceleration config identifier',
  sql_fingerprint CHAR(32) NOT NULL COMMENT 'SQL fingerprint',
  acceleration_type VARCHAR(32) NOT NULL COMMENT 'Acceleration type: PRECOMPUTE/PARTITION/BUCKET/SPLIT/REPLACE',
  config_detail_json JSON NOT NULL COMMENT 'Acceleration configuration detail',
  status VARCHAR(16) NOT NULL COMMENT 'Config status: DRAFT/ACTIVE/INVALID/DEPRECATED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  invalid_time DATETIME NULL DEFAULT NULL COMMENT 'Invalidation timestamp',
  PRIMARY KEY (config_id),
  KEY idx_acceleration_config_fingerprint (sql_fingerprint),
  KEY idx_acceleration_config_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Acceleration strategy configurations';

CREATE TABLE IF NOT EXISTS system_config (
  config_key VARCHAR(128) NOT NULL COMMENT 'System configuration key',
  config_value VARCHAR(512) NOT NULL COMMENT 'System configuration value',
  config_type VARCHAR(32) NOT NULL DEFAULT 'STRING' COMMENT 'Configuration type',
  description VARCHAR(255) DEFAULT NULL COMMENT 'Configuration description',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
  PRIMARY KEY (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Global platform configuration';

CREATE TABLE IF NOT EXISTS system_dictionary (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Dictionary primary key',
  dictionary_type VARCHAR(64) NOT NULL COMMENT 'Dictionary category',
  dictionary_code VARCHAR(64) NOT NULL COMMENT 'Dictionary code',
  dictionary_name VARCHAR(128) NOT NULL COMMENT 'Display name',
  sort_order INT NOT NULL DEFAULT 0 COMMENT 'Display order',
  enabled_flag TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
  remark VARCHAR(255) DEFAULT NULL COMMENT 'Dictionary remark',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
  PRIMARY KEY (id),
  UNIQUE KEY uk_system_dictionary_type_code (dictionary_type, dictionary_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System dictionary for enums and display mappings';
