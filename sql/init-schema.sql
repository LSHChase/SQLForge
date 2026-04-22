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

CREATE TABLE IF NOT EXISTS kafka_message_queue (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  topic VARCHAR(128) NOT NULL COMMENT 'Simulated Kafka topic',
  partition_key VARCHAR(128) DEFAULT NULL COMMENT 'Partition key',
  message_body TEXT NOT NULL COMMENT 'JSON message body',
  headers TEXT DEFAULT NULL COMMENT 'JSON headers',
  status ENUM('PENDING','SENT','CONSUMED','FAILED') NOT NULL DEFAULT 'PENDING' COMMENT 'Message status',
  retry_count INT NOT NULL DEFAULT 0 COMMENT 'Retry count',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  consumed_at TIMESTAMP NULL DEFAULT NULL COMMENT 'Consumed time',
  error_log TEXT DEFAULT NULL COMMENT 'Error log',
  PRIMARY KEY (id),
  KEY idx_topic_status (topic, status),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='R-144数据库模拟模式消息队列';

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

CREATE TABLE IF NOT EXISTS benchmark_task (
  task_id VARCHAR(64) NOT NULL COMMENT 'Benchmark task identifier',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Tenant identifier',
  task_type VARCHAR(32) NOT NULL COMMENT 'Benchmark task type: BASELINE/COMPARISON/REGRESSION_GUARD',
  sql_text LONGTEXT DEFAULT NULL COMMENT 'Submitted SQL text snapshot',
  sql_fingerprint VARCHAR(128) NOT NULL COMMENT 'Normalized SQL fingerprint',
  priority VARCHAR(16) NOT NULL COMMENT 'Task priority',
  target_engines_json JSON NOT NULL COMMENT 'Requested target engine list JSON',
  concurrency INT DEFAULT NULL COMMENT 'Requested benchmark concurrency',
  duration_seconds INT DEFAULT NULL COMMENT 'Requested benchmark duration in seconds',
  ramp_up_seconds INT DEFAULT NULL COMMENT 'Requested ramp-up duration in seconds',
  dataset_size_label VARCHAR(64) DEFAULT NULL COMMENT 'Dataset size label',
  readonly_required TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Readonly guardrail flag',
  shadow_environment_mode VARCHAR(32) NOT NULL COMMENT 'Shadow environment requirement mode',
  desensitization_requirement VARCHAR(32) NOT NULL COMMENT 'Desensitization requirement mode',
  thresholds_json JSON NOT NULL COMMENT 'Threshold definitions JSON',
  status VARCHAR(16) NOT NULL COMMENT 'Task status: QUEUED/RUNNING/SUCCEEDED/FAILED/CANCELLED',
  current_phase VARCHAR(32) NOT NULL COMMENT 'Current task phase',
  progress_percent INT NOT NULL DEFAULT 0 COMMENT 'Progress percentage',
  report_id VARCHAR(64) DEFAULT NULL COMMENT 'Generated report identifier when available',
  error_code INT DEFAULT NULL COMMENT 'Failure code',
  error_message VARCHAR(512) DEFAULT NULL COMMENT 'Desensitized failure message',
  error_suggested_action VARCHAR(512) DEFAULT NULL COMMENT 'Suggested follow-up action',
  error_retryable TINYINT(1) DEFAULT NULL COMMENT 'Whether failure is retryable',
  status_history_json JSON NOT NULL COMMENT 'Ordered status transition history JSON',
  submitted_at DATETIME(3) NOT NULL COMMENT 'Submit timestamp',
  started_at DATETIME(3) DEFAULT NULL COMMENT 'Worker start timestamp',
  finished_at DATETIME(3) DEFAULT NULL COMMENT 'Finish timestamp',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
  PRIMARY KEY (task_id),
  KEY idx_benchmark_task_status_submitted (status, submitted_at),
  KEY idx_benchmark_task_tenant_time (tenant_id, create_time),
  KEY idx_benchmark_task_fingerprint (sql_fingerprint)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Persistent benchmark task carrier for scheduler and worker flow';

CREATE TABLE IF NOT EXISTS benchmark_task_report (
  report_id VARCHAR(64) NOT NULL COMMENT 'Benchmark report identifier',
  task_id VARCHAR(64) NOT NULL COMMENT 'Related benchmark task identifier',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Tenant identifier',
  task_type VARCHAR(32) NOT NULL COMMENT 'Benchmark task type',
  sql_fingerprint VARCHAR(128) NOT NULL COMMENT 'Normalized SQL fingerprint',
  generated_at DATETIME(3) NOT NULL COMMENT 'Report generation timestamp',
  verdict VARCHAR(32) NOT NULL COMMENT 'Report verdict',
  engine_profiles_json JSON NOT NULL COMMENT 'Per-engine benchmark metrics JSON',
  threshold_assessments_json JSON NOT NULL COMMENT 'Threshold assessment JSON',
  recommendations_json JSON NOT NULL COMMENT 'Recommendation list JSON',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
  PRIMARY KEY (report_id),
  UNIQUE KEY uk_benchmark_task_report_task_id (task_id),
  KEY idx_benchmark_task_report_tenant_time (tenant_id, create_time),
  KEY idx_benchmark_task_report_fingerprint (sql_fingerprint)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Persistent benchmark reports written back by the benchmark worker';

CREATE TABLE IF NOT EXISTS optimization_task (
  task_id VARCHAR(64) NOT NULL COMMENT 'Optimization task identifier',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Tenant identifier',
  task_type VARCHAR(32) NOT NULL COMMENT 'Optimization task type: PARSE/REWRITE/ACCELERATION_SUGGESTION',
  sql_text LONGTEXT DEFAULT NULL COMMENT 'Submitted SQL text snapshot',
  sql_fingerprint VARCHAR(128) NOT NULL COMMENT 'Normalized SQL fingerprint',
  datasource_type VARCHAR(32) NOT NULL COMMENT 'Datasource type',
  priority VARCHAR(16) NOT NULL COMMENT 'Task priority',
  parse_depth VARCHAR(16) NOT NULL COMMENT 'Parse depth',
  callback_url VARCHAR(2048) DEFAULT NULL COMMENT 'Optional callback URL',
  requested_suggestion_types_json JSON DEFAULT NULL COMMENT 'Requested acceleration suggestion types JSON',
  status VARCHAR(16) NOT NULL COMMENT 'Task status: QUEUED/RUNNING/SUCCEEDED/FAILED/CANCELLED',
  current_phase VARCHAR(32) NOT NULL COMMENT 'Current task phase',
  progress_percent INT NOT NULL DEFAULT 0 COMMENT 'Progress percentage',
  summary TEXT DEFAULT NULL COMMENT 'Task summary when completed',
  error_code INT DEFAULT NULL COMMENT 'Failure code',
  error_message VARCHAR(512) DEFAULT NULL COMMENT 'Desensitized failure message',
  error_suggested_action VARCHAR(512) DEFAULT NULL COMMENT 'Suggested follow-up action',
  error_retryable TINYINT(1) DEFAULT NULL COMMENT 'Whether failure is retryable',
  status_history_json JSON NOT NULL COMMENT 'Ordered status transition history JSON',
  submitted_at DATETIME(3) NOT NULL COMMENT 'Submit timestamp',
  started_at DATETIME(3) DEFAULT NULL COMMENT 'Worker start timestamp',
  finished_at DATETIME(3) DEFAULT NULL COMMENT 'Finish timestamp',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
  PRIMARY KEY (task_id),
  KEY idx_optimization_task_status_submitted (status, submitted_at),
  KEY idx_optimization_task_tenant_time (tenant_id, create_time),
  KEY idx_optimization_task_fingerprint (sql_fingerprint)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Persistent SQL optimization task carrier for scheduler and worker flow';

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

CREATE TABLE IF NOT EXISTS config_snapshot (
  config_snapshot_id VARCHAR(64) NOT NULL COMMENT 'Config snapshot identifier',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Tenant identifier',
  service_code VARCHAR(32) NOT NULL COMMENT 'Owner service code',
  source_config_type VARCHAR(32) NOT NULL COMMENT 'Source config type such as TENANT_CONFIG/SYSTEM_CONFIG/ACCELERATION_CONFIG',
  source_config_id VARCHAR(128) NOT NULL COMMENT 'Source config business identifier',
  source_version VARCHAR(64) DEFAULT NULL COMMENT 'Optional source config version',
  snapshot_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Snapshot status: ACTIVE/DEPRECATED/ROLLED_BACK',
  snapshot_reason VARCHAR(128) DEFAULT NULL COMMENT 'Snapshot reason such as TASK_SUBMIT/EXPORT_REQUEST/AUDIT_REPLAY',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT 'Trace identifier',
  request_id VARCHAR(64) DEFAULT NULL COMMENT 'Request identifier',
  saga_id VARCHAR(64) DEFAULT NULL COMMENT 'Saga identifier',
  snapshot_payload JSON NOT NULL COMMENT 'Config snapshot payload with sensitive leaves encrypted',
  created_by VARCHAR(64) DEFAULT NULL COMMENT 'Operator identifier',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  PRIMARY KEY (config_snapshot_id),
  KEY idx_config_snapshot_tenant_time (tenant_id, create_time),
  KEY idx_config_snapshot_source (source_config_type, source_config_id),
  KEY idx_config_snapshot_trace (trace_id, request_id),
  KEY idx_config_snapshot_status (snapshot_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Immutable configuration snapshots used as the traceability anchor';

CREATE TABLE IF NOT EXISTS execution_result (
  result_id VARCHAR(64) NOT NULL COMMENT 'Execution result identifier',
  config_snapshot_id VARCHAR(64) NOT NULL COMMENT 'Referenced config snapshot identifier',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Tenant identifier',
  service_code VARCHAR(32) NOT NULL COMMENT 'Owner service code',
  task_id VARCHAR(64) DEFAULT NULL COMMENT 'Related task identifier',
  task_type VARCHAR(32) NOT NULL COMMENT 'Task type such as QUERY_EXECUTION/OPTIMIZATION/BENCHMARK',
  result_status VARCHAR(16) NOT NULL COMMENT 'Result status: QUEUED/RUNNING/SUCCEEDED/FAILED/CANCELLED',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT 'Trace identifier',
  request_id VARCHAR(64) DEFAULT NULL COMMENT 'Request identifier',
  saga_id VARCHAR(64) DEFAULT NULL COMMENT 'Saga identifier',
  result_summary JSON DEFAULT NULL COMMENT 'Structured summary without sensitive data',
  result_payload JSON DEFAULT NULL COMMENT 'Structured result payload with sensitive leaves encrypted',
  error_code VARCHAR(32) DEFAULT NULL COMMENT 'Error code when failed',
  error_message VARCHAR(512) DEFAULT NULL COMMENT 'Desensitized error message',
  started_at DATETIME DEFAULT NULL COMMENT 'Start time',
  finished_at DATETIME DEFAULT NULL COMMENT 'Finish time',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  PRIMARY KEY (result_id),
  KEY idx_execution_result_tenant_time (tenant_id, create_time),
  KEY idx_execution_result_task (task_id, task_type),
  KEY idx_execution_result_trace (trace_id, request_id),
  KEY idx_execution_result_status (result_status),
  KEY idx_execution_result_config_snapshot_id (config_snapshot_id),
  CONSTRAINT fk_execution_result_config_snapshot FOREIGN KEY (config_snapshot_id)
    REFERENCES config_snapshot (config_snapshot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Immutable execution and analysis results';

CREATE TABLE IF NOT EXISTS query_history (
  history_id VARCHAR(64) NOT NULL COMMENT 'Query history identifier',
  result_id VARCHAR(64) NOT NULL COMMENT 'Referenced execution result identifier',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Tenant identifier',
  history_type VARCHAR(32) NOT NULL COMMENT 'History type such as QUERY_EXECUTION/SQL_OPTIMIZATION/BENCHMARK',
  sql_fingerprint CHAR(32) NOT NULL COMMENT 'Normalized SQL fingerprint',
  sql_text_cipher MEDIUMBLOB DEFAULT NULL COMMENT 'Encrypted SQL text payload, ciphertext only',
  datasource_type VARCHAR(32) DEFAULT NULL COMMENT 'Datasource type',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT 'Trace identifier',
  request_id VARCHAR(64) DEFAULT NULL COMMENT 'Request identifier',
  saga_id VARCHAR(64) DEFAULT NULL COMMENT 'Saga identifier',
  query_context JSON DEFAULT NULL COMMENT 'Structured query context with sensitive leaves encrypted',
  submitted_by VARCHAR(64) DEFAULT NULL COMMENT 'Operator identifier',
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Submitted timestamp',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  PRIMARY KEY (history_id),
  KEY idx_query_history_result_id (result_id),
  KEY idx_query_history_tenant_time (tenant_id, create_time),
  KEY idx_query_history_fingerprint (sql_fingerprint),
  KEY idx_query_history_trace (trace_id, request_id),
  CONSTRAINT fk_query_history_result FOREIGN KEY (result_id)
    REFERENCES execution_result (result_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Historical immutable query snapshots linked to execution results';

CREATE TABLE IF NOT EXISTS export_record (
  export_id VARCHAR(64) NOT NULL COMMENT 'Export record identifier',
  history_id VARCHAR(64) NOT NULL COMMENT 'Referenced query history identifier',
  result_id VARCHAR(64) NOT NULL COMMENT 'Referenced execution result identifier',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Tenant identifier',
  export_format VARCHAR(16) NOT NULL COMMENT 'Export format: JSON/PDF/HTML/CSV',
  export_status VARCHAR(16) NOT NULL COMMENT 'Export status: REQUESTED/GENERATING/READY/FAILED/EXPIRED',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT 'Trace identifier',
  request_id VARCHAR(64) DEFAULT NULL COMMENT 'Request identifier',
  saga_id VARCHAR(64) DEFAULT NULL COMMENT 'Saga identifier',
  storage_type VARCHAR(32) DEFAULT NULL COMMENT 'Storage type such as INLINE/MINIO/OSS',
  storage_uri VARCHAR(512) DEFAULT NULL COMMENT 'Desensitized export storage location',
  checksum VARCHAR(128) DEFAULT NULL COMMENT 'Export checksum',
  export_options JSON DEFAULT NULL COMMENT 'Structured export options with sensitive leaves encrypted',
  error_code VARCHAR(32) DEFAULT NULL COMMENT 'Error code when failed',
  error_message VARCHAR(512) DEFAULT NULL COMMENT 'Desensitized error message',
  created_by VARCHAR(64) DEFAULT NULL COMMENT 'Operator identifier',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  finished_at DATETIME DEFAULT NULL COMMENT 'Finish timestamp',
  PRIMARY KEY (export_id),
  KEY idx_export_record_history_id (history_id),
  KEY idx_export_record_result_id (result_id),
  KEY idx_export_record_tenant_time (tenant_id, create_time),
  KEY idx_export_record_trace (trace_id, request_id),
  KEY idx_export_record_status (export_status),
  CONSTRAINT fk_export_record_history FOREIGN KEY (history_id)
    REFERENCES query_history (history_id),
  CONSTRAINT fk_export_record_result FOREIGN KEY (result_id)
    REFERENCES execution_result (result_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Export metadata and immutable render outputs';

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Audit log primary key',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Tenant identifier',
  service_code VARCHAR(32) DEFAULT NULL COMMENT 'Owner service code for traceability',
  operation_type VARCHAR(64) NOT NULL COMMENT 'Operation type such as LOGIN, QUERY, PERMISSION_CHANGE',
  target_type VARCHAR(64) NOT NULL COMMENT 'Target type such as SQL_JOB, DATASOURCE, USER',
  target_id VARCHAR(128) NOT NULL COMMENT 'Target business identifier',
  request_id VARCHAR(64) DEFAULT NULL COMMENT 'Request identifier',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT 'Trace identifier',
  saga_id VARCHAR(64) DEFAULT NULL COMMENT 'Saga identifier for cross-service compensation chain',
  config_snapshot_id VARCHAR(64) DEFAULT NULL COMMENT 'Referenced config snapshot identifier',
  result_id VARCHAR(64) DEFAULT NULL COMMENT 'Referenced execution result identifier',
  history_id VARCHAR(64) DEFAULT NULL COMMENT 'Referenced query history identifier',
  export_id VARCHAR(64) DEFAULT NULL COMMENT 'Referenced export record identifier',
  request_params JSON NULL COMMENT 'Desensitized request parameters, no raw secret allowed',
  response_summary TEXT NULL COMMENT 'Response summary without sensitive data',
  status VARCHAR(16) NOT NULL COMMENT 'Operation result: SUCCESS/FAILED/PARTIAL',
  cost_ms BIGINT NOT NULL DEFAULT 0 COMMENT 'Processing latency in milliseconds',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Audit creation time, retain for at least 180 days',
  PRIMARY KEY (id),
  KEY idx_audit_log_tenant_time (tenant_id, create_time),
  KEY idx_audit_log_operation_time (operation_type, create_time),
  KEY idx_audit_log_target (target_type, target_id),
  KEY idx_audit_log_trace_request (trace_id, request_id),
  KEY idx_audit_log_config_snapshot_id (config_snapshot_id),
  KEY idx_audit_log_result_id (result_id),
  KEY idx_audit_log_history_id (history_id),
  KEY idx_audit_log_export_id (export_id),
  CONSTRAINT fk_audit_log_config_snapshot FOREIGN KEY (config_snapshot_id)
    REFERENCES config_snapshot (config_snapshot_id),
  CONSTRAINT fk_audit_log_result FOREIGN KEY (result_id)
    REFERENCES execution_result (result_id),
  CONSTRAINT fk_audit_log_history FOREIGN KEY (history_id)
    REFERENCES query_history (history_id),
  CONSTRAINT fk_audit_log_export FOREIGN KEY (export_id)
    REFERENCES export_record (export_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Immutable audit logs, production should isolate storage strategy';

CREATE TABLE IF NOT EXISTS governance_history_lookup_index (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Tenant identifier',
  lookup_type VARCHAR(16) NOT NULL COMMENT 'Lookup category: TASK/REPORT',
  lookup_id VARCHAR(128) NOT NULL COMMENT 'Task or report identifier',
  trace_id VARCHAR(64) NOT NULL COMMENT 'Trace identifier',
  source_target_type VARCHAR(64) NOT NULL COMMENT 'Original audit target type',
  last_seen_at DATETIME NOT NULL COMMENT 'Last audit event time',
  last_audit_id BIGINT UNSIGNED NOT NULL COMMENT 'Last audit log id for cursor stability',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
  PRIMARY KEY (id),
  UNIQUE KEY uk_governance_history_lookup_item (tenant_id, lookup_type, lookup_id, trace_id),
  KEY idx_governance_history_lookup_window (tenant_id, lookup_type, lookup_id, last_seen_at, last_audit_id),
  KEY idx_governance_history_lookup_trace (tenant_id, trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Materialized lookup index for long-window governance history reverse search';

INSERT INTO governance_history_lookup_index (
  tenant_id,
  lookup_type,
  lookup_id,
  trace_id,
  source_target_type,
  last_seen_at,
  last_audit_id,
  create_time,
  update_time
)
SELECT
  audit_log.tenant_id,
  CASE
    WHEN audit_log.target_type IN ('TASK', 'SQL_OPTIMIZATION_TASK', 'BENCHMARK_ENGINE_TASK') THEN 'TASK'
    ELSE 'REPORT'
  END AS lookup_type,
  audit_log.target_id,
  audit_log.trace_id,
  MAX(audit_log.target_type) AS source_target_type,
  MAX(audit_log.create_time) AS last_seen_at,
  MAX(audit_log.id) AS last_audit_id,
  NOW(),
  NOW()
FROM audit_log
WHERE audit_log.trace_id IS NOT NULL
  AND audit_log.target_id IS NOT NULL
  AND audit_log.target_type IN ('TASK', 'SQL_OPTIMIZATION_TASK', 'BENCHMARK_ENGINE_TASK', 'REPORT', 'BENCHMARK_ENGINE_REPORT')
GROUP BY audit_log.tenant_id,
  CASE
    WHEN audit_log.target_type IN ('TASK', 'SQL_OPTIMIZATION_TASK', 'BENCHMARK_ENGINE_TASK') THEN 'TASK'
    ELSE 'REPORT'
  END,
  audit_log.target_id,
  audit_log.trace_id
ON DUPLICATE KEY UPDATE
  source_target_type = VALUES(source_target_type),
  last_seen_at = CASE
    WHEN VALUES(last_seen_at) > last_seen_at THEN VALUES(last_seen_at)
    ELSE last_seen_at
  END,
  last_audit_id = CASE
    WHEN VALUES(last_seen_at) > last_seen_at THEN VALUES(last_audit_id)
    WHEN VALUES(last_seen_at) = last_seen_at AND VALUES(last_audit_id) > last_audit_id THEN VALUES(last_audit_id)
    ELSE last_audit_id
  END,
  update_time = NOW();

DROP TRIGGER IF EXISTS trg_audit_log_lookup_index_ai;

DELIMITER $$
CREATE TRIGGER trg_audit_log_lookup_index_ai
AFTER INSERT ON audit_log
FOR EACH ROW
BEGIN
  IF NEW.trace_id IS NOT NULL
     AND NEW.target_id IS NOT NULL
     AND NEW.target_type IN ('TASK', 'SQL_OPTIMIZATION_TASK', 'BENCHMARK_ENGINE_TASK', 'REPORT', 'BENCHMARK_ENGINE_REPORT') THEN
    INSERT INTO governance_history_lookup_index (
      tenant_id,
      lookup_type,
      lookup_id,
      trace_id,
      source_target_type,
      last_seen_at,
      last_audit_id
    ) VALUES (
      NEW.tenant_id,
      CASE
        WHEN NEW.target_type IN ('TASK', 'SQL_OPTIMIZATION_TASK', 'BENCHMARK_ENGINE_TASK') THEN 'TASK'
        ELSE 'REPORT'
      END,
      NEW.target_id,
      NEW.trace_id,
      NEW.target_type,
      NEW.create_time,
      NEW.id
    )
    ON DUPLICATE KEY UPDATE
      source_target_type = VALUES(source_target_type),
      last_seen_at = CASE
        WHEN VALUES(last_seen_at) > last_seen_at THEN VALUES(last_seen_at)
        ELSE last_seen_at
      END,
      last_audit_id = CASE
        WHEN VALUES(last_seen_at) > last_seen_at THEN VALUES(last_audit_id)
        WHEN VALUES(last_seen_at) = last_seen_at AND VALUES(last_audit_id) > last_audit_id THEN VALUES(last_audit_id)
        ELSE last_audit_id
      END,
      update_time = NOW();
  END IF;
END$$
DELIMITER ;

CREATE TABLE IF NOT EXISTS system_config (
  config_key VARCHAR(128) NOT NULL COMMENT 'System configuration key',
  config_value VARCHAR(512) DEFAULT NULL COMMENT 'Non-sensitive system configuration value only',
  sensitive_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 means value is stored only in ciphertext columns',
  value_ciphertext TEXT DEFAULT NULL COMMENT 'AES-256 ciphertext envelope for sensitive config value',
  value_mask VARCHAR(128) DEFAULT NULL COMMENT 'Masked preview for sensitive config value',
  encryption_algorithm VARCHAR(32) DEFAULT NULL COMMENT 'Sensitive value encryption algorithm',
  encryption_key_id VARCHAR(64) DEFAULT NULL COMMENT 'Sensitive value encryption key identifier',
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
