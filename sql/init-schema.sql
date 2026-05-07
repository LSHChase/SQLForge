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
  execution_summary_json JSON DEFAULT NULL COMMENT 'Isolated execution summary JSON',
  regression_summary_json JSON DEFAULT NULL COMMENT 'Regression summary JSON',
  alert_linkages_json JSON DEFAULT NULL COMMENT 'Governance alert linkage JSON',
  export_artifacts_json JSON DEFAULT NULL COMMENT 'Persisted export artifact metadata and content JSON',
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
  suggestion_payload_json JSON DEFAULT NULL COMMENT 'Structured suggestion payload JSON',
  error_code INT DEFAULT NULL COMMENT 'Failure code',
  error_message VARCHAR(512) DEFAULT NULL COMMENT 'Desensitized failure message',
  error_suggested_action VARCHAR(512) DEFAULT NULL COMMENT 'Suggested follow-up action',
  error_retryable TINYINT(1) DEFAULT NULL COMMENT 'Whether failure is retryable',
  failed_phase VARCHAR(32) DEFAULT NULL COMMENT 'Execution phase that produced the failure',
  error_risks_json JSON DEFAULT NULL COMMENT 'Structured failure risks JSON',
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

CREATE TABLE IF NOT EXISTS acceleration_plan (
  plan_id VARCHAR(64) NOT NULL COMMENT 'Acceleration plan identifier',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Tenant identifier',
  source_task_id VARCHAR(64) NOT NULL COMMENT 'Source optimization task identifier',
  sql_text LONGTEXT DEFAULT NULL COMMENT 'Submitted SQL text snapshot',
  sql_fingerprint VARCHAR(128) NOT NULL COMMENT 'Normalized SQL fingerprint',
  datasource_type VARCHAR(32) NOT NULL COMMENT 'Datasource type',
  selected_suggestion_types_json JSON NOT NULL COMMENT 'Selected acceleration suggestion types JSON',
  plan_status VARCHAR(32) NOT NULL COMMENT 'Plan status: PENDING_APPROVAL/APPROVED/REJECTED/APPLY_FAILED/APPLIED/VERIFY_FAILED/VERIFIED/ROLLBACK_FAILED/ROLLED_BACK',
  plan_summary TEXT DEFAULT NULL COMMENT 'Plan summary',
  primary_recommendation TEXT DEFAULT NULL COMMENT 'Primary recommendation',
  plan_payload_json JSON NOT NULL COMMENT 'Selected acceleration plan payload JSON',
  benefits_json JSON DEFAULT NULL COMMENT 'Structured benefits JSON',
  costs_json JSON DEFAULT NULL COMMENT 'Structured costs JSON',
  risks_json JSON DEFAULT NULL COMMENT 'Structured risks JSON',
  config_snapshot_id VARCHAR(64) DEFAULT NULL COMMENT 'Governance config snapshot identifier',
  result_id VARCHAR(64) DEFAULT NULL COMMENT 'Governance execution result identifier',
  history_id VARCHAR(64) DEFAULT NULL COMMENT 'Governance query history identifier',
  review_note VARCHAR(512) DEFAULT NULL COMMENT 'Approval or rejection review note',
  approved_by VARCHAR(64) DEFAULT NULL COMMENT 'Approver user identifier',
  approved_at DATETIME(3) DEFAULT NULL COMMENT 'Approval timestamp',
  rejected_by VARCHAR(64) DEFAULT NULL COMMENT 'Rejector user identifier',
  rejected_at DATETIME(3) DEFAULT NULL COMMENT 'Rejection timestamp',
  last_error_code INT DEFAULT NULL COMMENT 'Last lifecycle failure code',
  last_error_message VARCHAR(512) DEFAULT NULL COMMENT 'Last lifecycle failure message',
  runtime_binding_json JSON DEFAULT NULL COMMENT 'Apply/runtime binding evidence JSON',
  runtime_binding_at DATETIME(3) DEFAULT NULL COMMENT 'Apply timestamp',
  runtime_binding_by VARCHAR(64) DEFAULT NULL COMMENT 'Apply operator identifier',
  verification_evidence_json JSON DEFAULT NULL COMMENT 'Verification evidence JSON',
  verified_at DATETIME(3) DEFAULT NULL COMMENT 'Verification timestamp',
  verified_by VARCHAR(64) DEFAULT NULL COMMENT 'Verification operator identifier',
  rollback_evidence_json JSON DEFAULT NULL COMMENT 'Rollback evidence JSON',
  rolled_back_at DATETIME(3) DEFAULT NULL COMMENT 'Rollback timestamp',
  rolled_back_by VARCHAR(64) DEFAULT NULL COMMENT 'Rollback operator identifier',
  status_history_json JSON NOT NULL COMMENT 'Ordered plan status transition history JSON',
  created_at DATETIME(3) NOT NULL COMMENT 'Creation timestamp',
  updated_at DATETIME(3) NOT NULL COMMENT 'Last update timestamp',
  PRIMARY KEY (plan_id),
  KEY idx_acceleration_plan_status_created (plan_status, created_at),
  KEY idx_acceleration_plan_tenant_time (tenant_id, created_at),
  KEY idx_acceleration_plan_fingerprint (sql_fingerprint),
  KEY idx_acceleration_plan_source_task (source_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Governed acceleration plan lifecycle carrier';

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
  access_channel VARCHAR(32) DEFAULT NULL COMMENT 'Access channel such as PAGE/API/JDBC_AGENT/SDK/CLIENT',
  target_engine VARCHAR(64) DEFAULT NULL COMMENT 'Selected execution engine or routed engine',
  returned_row_count BIGINT DEFAULT NULL COMMENT 'Returned row count when known',
  cache_hit TINYINT(1) DEFAULT NULL COMMENT 'Whether cache was hit',
  rewrite_applied TINYINT(1) DEFAULT NULL COMMENT 'Whether lightweight rewrite was applied',
  acceleration_applied TINYINT(1) DEFAULT NULL COMMENT 'Whether acceleration path was applied',
  hit_table_summary JSON DEFAULT NULL COMMENT 'Structured hit table summary JSON',
  route_summary JSON DEFAULT NULL COMMENT 'Structured route summary JSON',
  cache_summary JSON DEFAULT NULL COMMENT 'Structured cache summary JSON',
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
  KEY idx_execution_result_access_channel (tenant_id, access_channel, create_time),
  KEY idx_execution_result_target_engine (tenant_id, target_engine, create_time),
  KEY idx_execution_result_cache_hit (tenant_id, cache_hit, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Immutable execution and analysis results';

CREATE TABLE IF NOT EXISTS query_history (
  history_id VARCHAR(64) NOT NULL COMMENT 'Query history identifier',
  result_id VARCHAR(64) NOT NULL COMMENT 'Referenced execution result identifier',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Tenant identifier',
  history_type VARCHAR(32) NOT NULL COMMENT 'History type such as QUERY_EXECUTION/SQL_OPTIMIZATION/BENCHMARK',
  sql_fingerprint CHAR(32) NOT NULL COMMENT 'Normalized SQL fingerprint',
  sql_text_cipher MEDIUMBLOB DEFAULT NULL COMMENT 'Encrypted SQL text payload, ciphertext only',
  sql_template_cipher MEDIUMBLOB DEFAULT NULL COMMENT 'Encrypted template SQL payload for parameterized queries',
  bound_sql_text_cipher MEDIUMBLOB DEFAULT NULL COMMENT 'Encrypted bound SQL payload after parameter binding',
  datasource_code VARCHAR(128) DEFAULT NULL COMMENT 'Datasource business identifier or alias',
  datasource_type VARCHAR(32) DEFAULT NULL COMMENT 'Datasource type',
  report_code VARCHAR(128) DEFAULT NULL COMMENT 'Report code parsed from SQL comment context',
  stage_code VARCHAR(32) DEFAULT NULL COMMENT 'Execution stage parsed from SQL comment context',
  biz_date DATE DEFAULT NULL COMMENT 'Execution date parsed from SQL comment context',
  query_date_start DATE DEFAULT NULL COMMENT 'Query date lower bound parsed from SQL body',
  query_date_end DATE DEFAULT NULL COMMENT 'Query date upper bound parsed from SQL body',
  query_date_status VARCHAR(32) DEFAULT NULL COMMENT 'Query date extraction status such as RESOLVED/UNRESOLVED/PARTIAL',
  access_channel VARCHAR(32) DEFAULT NULL COMMENT 'Access channel such as PAGE/API/JDBC_AGENT/SDK/CLIENT',
  parameterized_sql_flag TINYINT(1) DEFAULT NULL COMMENT 'Whether the SQL was parameterized before binding',
  binding_mode VARCHAR(16) DEFAULT NULL COMMENT 'Binding mode such as POSITIONAL/NAMED',
  binding_render_status VARCHAR(16) DEFAULT NULL COMMENT 'Binding render status such as SUCCESS/PARTIAL/FAILED/MASKED',
  sql_template_fingerprint CHAR(32) DEFAULT NULL COMMENT 'Template SQL fingerprint before binding',
  bound_sql_fingerprint CHAR(32) DEFAULT NULL COMMENT 'Bound SQL fingerprint after binding',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT 'Trace identifier',
  request_id VARCHAR(64) DEFAULT NULL COMMENT 'Request identifier',
  saga_id VARCHAR(64) DEFAULT NULL COMMENT 'Saga identifier',
  comment_context JSON DEFAULT NULL COMMENT 'Structured SQL comment context JSON',
  binding_summary JSON DEFAULT NULL COMMENT 'Structured parameter binding summary JSON',
  logical_object_hits JSON DEFAULT NULL COMMENT 'Structured logical object and table hit summary JSON',
  route_summary JSON DEFAULT NULL COMMENT 'Structured route decision summary JSON',
  cache_summary JSON DEFAULT NULL COMMENT 'Structured cache decision summary JSON',
  query_context JSON DEFAULT NULL COMMENT 'Structured query context with sensitive leaves encrypted',
  submitted_by VARCHAR(64) DEFAULT NULL COMMENT 'Operator identifier',
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Submitted timestamp',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  PRIMARY KEY (history_id),
  KEY idx_query_history_result_id (result_id),
  KEY idx_query_history_tenant_time (tenant_id, create_time),
  KEY idx_query_history_fingerprint (sql_fingerprint),
  KEY idx_query_history_trace (trace_id, request_id),
  KEY idx_query_history_report_stage_date (tenant_id, report_code, stage_code, biz_date),
  KEY idx_query_history_query_date (tenant_id, query_date_start, query_date_end),
  KEY idx_query_history_datasource_code (tenant_id, datasource_code, create_time),
  KEY idx_query_history_access_channel (tenant_id, access_channel, create_time),
  KEY idx_query_history_binding_mode (tenant_id, binding_mode, create_time)
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
  KEY idx_export_record_status (export_status)
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
  KEY idx_audit_log_export_id (export_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Immutable audit logs, production should isolate storage strategy';

CREATE TABLE IF NOT EXISTS alert_policy (
  policy_id VARCHAR(64) NOT NULL COMMENT 'Alert policy identifier',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Tenant identifier',
  policy_name VARCHAR(128) NOT NULL COMMENT 'Readable alert policy name',
  alert_type VARCHAR(64) NOT NULL COMMENT 'Alert type such as DATASOURCE_UNAVAILABLE or AUDIT_WRITE_EXCEPTION',
  default_level VARCHAR(16) NOT NULL DEFAULT 'MEDIUM' COMMENT 'Default alert level: LOW/MEDIUM/HIGH/CRITICAL',
  dedupe_strategy VARCHAR(32) NOT NULL DEFAULT 'TENANT_ALERT_TYPE_TARGET' COMMENT 'Dedupe strategy baseline',
  dedupe_window_seconds INT NOT NULL DEFAULT 900 COMMENT 'Deduplication window in seconds',
  notify_channel VARCHAR(32) NOT NULL DEFAULT 'SIMULATED_EMAIL' COMMENT 'Notify channel baseline',
  initial_notify_status VARCHAR(32) NOT NULL DEFAULT 'SIMULATED_PENDING_NOTIFY' COMMENT 'Initial notification status: SIMULATED_PENDING_NOTIFY/SIMULATED_NOTIFIED/SIMULATED_NOTIFY_FAILED',
  owner_role VARCHAR(64) NOT NULL DEFAULT 'TENANT_ADMIN' COMMENT 'Owning role for the policy',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Whether the policy is enabled',
  rule_config_json JSON DEFAULT NULL COMMENT 'Structured threshold or matcher baseline JSON',
  created_by VARCHAR(64) DEFAULT NULL COMMENT 'Operator identifier',
  created_at DATETIME(3) NOT NULL COMMENT 'Creation timestamp',
  updated_at DATETIME(3) NOT NULL COMMENT 'Last update timestamp',
  PRIMARY KEY (policy_id),
  UNIQUE KEY uk_alert_policy_tenant_type (tenant_id, alert_type),
  KEY idx_alert_policy_tenant_enabled (tenant_id, enabled, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Alert policy baseline for dedupe, severity, and simulated notify defaults';

CREATE TABLE IF NOT EXISTS alert_event (
  alert_id VARCHAR(64) NOT NULL COMMENT 'Alert event identifier',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Tenant identifier',
  alert_type VARCHAR(64) NOT NULL COMMENT 'Alert type such as DATASOURCE_UNAVAILABLE or BENCHMARK_REGRESSION_FAILED',
  alert_level VARCHAR(16) NOT NULL DEFAULT 'MEDIUM' COMMENT 'Alert severity level: LOW/MEDIUM/HIGH/CRITICAL',
  alert_status VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'Alert lifecycle status: OPEN/ACKED',
  notify_status VARCHAR(32) NOT NULL DEFAULT 'SIMULATED_PENDING_NOTIFY' COMMENT 'Notification status: SIMULATED_PENDING_NOTIFY/SIMULATED_NOTIFIED/SIMULATED_NOTIFY_FAILED',
  policy_id VARCHAR(64) DEFAULT NULL COMMENT 'Owning alert policy identifier',
  dedupe_key VARCHAR(255) NOT NULL COMMENT 'Tenant-scoped dedupe key',
  source_service VARCHAR(64) DEFAULT NULL COMMENT 'Origin service code or source surface',
  summary VARCHAR(512) NOT NULL COMMENT 'Alert summary for operators',
  history_id VARCHAR(64) DEFAULT NULL COMMENT 'Related governance query history identifier',
  parse_task_id VARCHAR(64) DEFAULT NULL COMMENT 'Related parse task identifier',
  batch_id VARCHAR(64) DEFAULT NULL COMMENT 'Related batch identifier',
  route_decision_id VARCHAR(64) DEFAULT NULL COMMENT 'Related routing decision identifier',
  recommendation_id VARCHAR(64) DEFAULT NULL COMMENT 'Related recommendation identifier',
  dispatch_event_id VARCHAR(64) DEFAULT NULL COMMENT 'Related dispatch event identifier',
  report_code VARCHAR(128) DEFAULT NULL COMMENT 'Related report code',
  logical_object_key VARCHAR(255) DEFAULT NULL COMMENT 'Related logical object key',
  datasource_id VARCHAR(128) DEFAULT NULL COMMENT 'Related datasource identifier',
  sql_fingerprint VARCHAR(128) DEFAULT NULL COMMENT 'Normalized SQL fingerprint',
  evidence_json JSON DEFAULT NULL COMMENT 'Structured evidence payload',
  notify_message VARCHAR(512) DEFAULT NULL COMMENT 'Latest simulated notification result message',
  notified_at DATETIME(3) DEFAULT NULL COMMENT 'Last simulated notification timestamp',
  acked_by VARCHAR(64) DEFAULT NULL COMMENT 'Acknowledging operator identifier',
  acked_at DATETIME(3) DEFAULT NULL COMMENT 'Acknowledgement timestamp',
  created_by VARCHAR(64) DEFAULT NULL COMMENT 'Operator identifier',
  created_at DATETIME(3) NOT NULL COMMENT 'Creation timestamp',
  updated_at DATETIME(3) NOT NULL COMMENT 'Last update timestamp',
  PRIMARY KEY (alert_id),
  KEY idx_alert_event_tenant_status_created (tenant_id, alert_status, created_at),
  KEY idx_alert_event_tenant_dedupe_created (tenant_id, dedupe_key, created_at),
  KEY idx_alert_event_tenant_type_created (tenant_id, alert_type, created_at),
  KEY idx_alert_event_tenant_dispatch (tenant_id, dispatch_event_id, created_at),
  KEY idx_alert_event_tenant_recommendation (tenant_id, recommendation_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Alert event carrier for simulated notify, dedupe, and future ACK flow';

CREATE TABLE IF NOT EXISTS alert_notification_log (
  notification_log_id VARCHAR(96) NOT NULL COMMENT 'Alert notification log identifier',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Tenant identifier',
  alert_id VARCHAR(64) NOT NULL COMMENT 'Alert event identifier used as the persisted source',
  source_alert_id VARCHAR(64) DEFAULT NULL COMMENT 'Original dedupe source alert identifier when notification is suppressed',
  dedupe_key VARCHAR(255) NOT NULL COMMENT 'Tenant-scoped dedupe key',
  notify_channel VARCHAR(32) NOT NULL DEFAULT 'SIMULATED_EMAIL' COMMENT 'Notification channel, simulated-only in phase one',
  delivery_status VARCHAR(32) NOT NULL COMMENT 'Delivery state: SIMULATED_SENT/DEDUPE_SUPPRESSED/SIMULATED_FAILED',
  template_code VARCHAR(64) NOT NULL COMMENT 'Rendered template identifier',
  message_subject VARCHAR(255) NOT NULL COMMENT 'Rendered simulated message subject',
  message_body TEXT NOT NULL COMMENT 'Rendered simulated message body',
  delivery_summary VARCHAR(512) DEFAULT NULL COMMENT 'Delivery or suppression summary',
  payload_json JSON DEFAULT NULL COMMENT 'Structured payload for notify simulated or dedupe suppressed evidence',
  created_by VARCHAR(64) DEFAULT NULL COMMENT 'Operator identifier',
  created_at DATETIME(3) NOT NULL COMMENT 'Creation timestamp',
  PRIMARY KEY (notification_log_id),
  KEY idx_alert_notification_tenant_alert_created (tenant_id, alert_id, created_at),
  KEY idx_alert_notification_tenant_dedupe_created (tenant_id, dedupe_key, created_at),
  KEY idx_alert_notification_tenant_status_created (tenant_id, delivery_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Simulated alert notification and dedupe suppression log';

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
    WHEN audit_log.target_type IN ('TASK', 'SQL_OPTIMIZATION_TASK', 'BENCHMARK_ENGINE_TASK', 'SQL_ACCELERATION_PLAN') THEN 'TASK'
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
  AND audit_log.target_type IN ('TASK', 'SQL_OPTIMIZATION_TASK', 'BENCHMARK_ENGINE_TASK', 'SQL_ACCELERATION_PLAN', 'REPORT', 'BENCHMARK_ENGINE_REPORT')
GROUP BY audit_log.tenant_id,
  CASE
    WHEN audit_log.target_type IN ('TASK', 'SQL_OPTIMIZATION_TASK', 'BENCHMARK_ENGINE_TASK', 'SQL_ACCELERATION_PLAN') THEN 'TASK'
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
     AND NEW.target_type IN ('TASK', 'SQL_OPTIMIZATION_TASK', 'BENCHMARK_ENGINE_TASK', 'SQL_ACCELERATION_PLAN', 'REPORT', 'BENCHMARK_ENGINE_REPORT') THEN
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
        WHEN NEW.target_type IN ('TASK', 'SQL_OPTIMIZATION_TASK', 'BENCHMARK_ENGINE_TASK', 'SQL_ACCELERATION_PLAN') THEN 'TASK'
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

CREATE TABLE IF NOT EXISTS business_logical_view (
  id VARCHAR(64) NOT NULL COMMENT 'Business logical view identifier',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Owning tenant identifier',
  view_code VARCHAR(128) NOT NULL COMMENT 'Stable logical view code',
  view_name VARCHAR(255) NOT NULL COMMENT 'Logical view display name',
  datasource_code VARCHAR(128) NOT NULL COMMENT 'Default datasource for the logical view',
  subject_area VARCHAR(128) DEFAULT NULL COMMENT 'Business subject area',
  owner_user VARCHAR(128) DEFAULT NULL COMMENT 'Logical view owner',
  freshness_status VARCHAR(32) DEFAULT NULL COMMENT 'Latest freshness status',
  sla_status VARCHAR(32) DEFAULT NULL COMMENT 'Latest SLA status',
  queryable TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Whether the logical view is queryable',
  latest_refresh_time DATETIME DEFAULT NULL COMMENT 'Latest successful refresh timestamp',
  description TEXT DEFAULT NULL COMMENT 'Logical view description',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
  PRIMARY KEY (id),
  UNIQUE KEY uk_business_logical_view_tenant_code (tenant_id, view_code),
  KEY idx_business_logical_view_tenant_datasource (tenant_id, datasource_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Business logical view catalog';

CREATE TABLE IF NOT EXISTS logical_object_mapping (
  id VARCHAR(64) NOT NULL COMMENT 'Logical object mapping identifier',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Owning tenant identifier',
  logical_view_id VARCHAR(64) NOT NULL COMMENT 'Business logical view identifier',
  target_object_type VARCHAR(32) NOT NULL COMMENT 'Mapped object type',
  target_object_key VARCHAR(255) NOT NULL COMMENT 'Canonical mapped object key',
  target_object_name VARCHAR(255) NOT NULL COMMENT 'Mapped object display name',
  mapping_role VARCHAR(64) DEFAULT NULL COMMENT 'Mapping role such as SOURCE or SERVING',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
  PRIMARY KEY (id),
  KEY idx_logical_object_mapping_view (tenant_id, logical_view_id),
  KEY idx_logical_object_mapping_target (tenant_id, target_object_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Business logical view to physical object mapping';

CREATE TABLE IF NOT EXISTS database_view_ref (
  id VARCHAR(64) NOT NULL COMMENT 'Database view identifier',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Owning tenant identifier',
  datasource_code VARCHAR(128) NOT NULL COMMENT 'Datasource code',
  view_name VARCHAR(255) NOT NULL COMMENT 'Database view name',
  object_key VARCHAR(255) NOT NULL COMMENT 'Canonical DB view object key',
  schema_name VARCHAR(128) DEFAULT NULL COMMENT 'Schema name',
  catalog_name VARCHAR(128) DEFAULT NULL COMMENT 'Catalog name',
  owner_user VARCHAR(128) DEFAULT NULL COMMENT 'DB view owner',
  queryable TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Whether the DB view is queryable',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
  PRIMARY KEY (id),
  UNIQUE KEY uk_database_view_ref_lookup (tenant_id, datasource_code, view_name),
  KEY idx_database_view_ref_key (tenant_id, object_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Database view catalog';

CREATE TABLE IF NOT EXISTS database_view_dependency (
  id VARCHAR(64) NOT NULL COMMENT 'Database view dependency identifier',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Owning tenant identifier',
  db_view_id VARCHAR(64) NOT NULL COMMENT 'Database view identifier',
  dependency_object_type VARCHAR(32) NOT NULL COMMENT 'Dependency object type',
  dependency_object_key VARCHAR(255) NOT NULL COMMENT 'Dependency object key',
  dependency_object_name VARCHAR(255) NOT NULL COMMENT 'Dependency object name',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
  PRIMARY KEY (id),
  KEY idx_database_view_dependency_view (tenant_id, db_view_id),
  KEY idx_database_view_dependency_key (tenant_id, dependency_object_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Database view dependency catalog';

CREATE TABLE IF NOT EXISTS parse_batch (
  batch_id VARCHAR(64) NOT NULL COMMENT 'Parse batch identifier',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Owning tenant identifier',
  batch_name VARCHAR(255) NOT NULL COMMENT 'Batch display name',
  import_mode VARCHAR(32) NOT NULL COMMENT 'Import mode such as SQL_FILE/TABULAR_FILE/REPORT_CATALOG',
  source_type VARCHAR(32) NOT NULL COMMENT 'Source type such as FILE_UPLOAD/REPORT_CATALOG_IMPORT',
  file_type VARCHAR(16) NOT NULL COMMENT 'Uploaded file type',
  template_version VARCHAR(32) DEFAULT NULL COMMENT 'Template version',
  datasource_code VARCHAR(128) DEFAULT NULL COMMENT 'Datasource code override',
  parser_mode VARCHAR(32) NOT NULL DEFAULT 'JSQLPARSER' COMMENT 'SQL parser mode: JSQLPARSER or APACHE_CALCITE',
  structure_parse_only TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Whether batch only runs structure parse',
  status VARCHAR(32) NOT NULL COMMENT 'Current batch status',
  total_records INT NOT NULL DEFAULT 0 COMMENT 'Imported record count',
  success_records INT NOT NULL DEFAULT 0 COMMENT 'Succeeded record count',
  partial_success_records INT NOT NULL DEFAULT 0 COMMENT 'Partial success record count',
  failed_records INT NOT NULL DEFAULT 0 COMMENT 'Failed record count',
  structure_parse_success_rate DECIMAL(6,2) DEFAULT NULL COMMENT 'Structure parse success rate percentage',
  access_parse_success_rate DECIMAL(6,2) DEFAULT NULL COMMENT 'Access parse success rate percentage',
  status_history_json JSON NOT NULL COMMENT 'Batch status history payload',
  created_by VARCHAR(64) DEFAULT NULL COMMENT 'Batch creator',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
  PRIMARY KEY (batch_id),
  KEY idx_parse_batch_tenant_status_created (tenant_id, status, created_at),
  KEY idx_parse_batch_tenant_mode (tenant_id, import_mode, file_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bulk parse batch contract baseline';

CREATE TABLE IF NOT EXISTS parse_batch_item (
  item_id VARCHAR(64) NOT NULL COMMENT 'Parse batch item identifier',
  batch_id VARCHAR(64) NOT NULL COMMENT 'Owning parse batch identifier',
  sequence_number INT NOT NULL COMMENT '1-based sequence number inside the uploaded batch',
  report_code VARCHAR(128) DEFAULT NULL COMMENT 'Optional report code carried into governance context',
  report_name VARCHAR(255) DEFAULT NULL COMMENT 'Optional report display name',
  datasource_code VARCHAR(128) DEFAULT NULL COMMENT 'Datasource code resolved for this row',
  stage VARCHAR(32) DEFAULT NULL COMMENT 'Stage metadata such as DEV/UAT/PROD',
  biz_date VARCHAR(32) DEFAULT NULL COMMENT 'Execution date metadata',
  priority VARCHAR(32) DEFAULT NULL COMMENT 'Priority hint',
  owner VARCHAR(128) DEFAULT NULL COMMENT 'Owner metadata',
  tags VARCHAR(255) DEFAULT NULL COMMENT 'Free-form tags',
  sql_text MEDIUMTEXT NOT NULL COMMENT 'Resolved SQL text',
  sql_template_text MEDIUMTEXT DEFAULT NULL COMMENT 'Prepared SQL template text',
  bind_parameters_json JSON DEFAULT NULL COMMENT 'Masked bind parameter payload',
  binding_mode VARCHAR(32) DEFAULT NULL COMMENT 'Binding mode such as POSITIONAL/NAMED',
  status VARCHAR(32) NOT NULL COMMENT 'Terminal item status',
  parse_task_id VARCHAR(64) DEFAULT NULL COMMENT 'Structure/access parse task identifier',
  structure_syntax_status VARCHAR(32) DEFAULT NULL COMMENT 'Structure parse syntax status',
  access_service_status VARCHAR(32) DEFAULT NULL COMMENT 'Access parse provider status',
  access_connection_status VARCHAR(32) DEFAULT NULL COMMENT 'Access parse connection status',
  failure_reason VARCHAR(128) DEFAULT NULL COMMENT 'Failure or degrade reason',
  history_id VARCHAR(128) DEFAULT NULL COMMENT 'Governance query history id for this parsed SQL',
  history_persisted TINYINT(1) DEFAULT NULL COMMENT 'Whether governance parse history was persisted for this SQL',
  history_persistence_status VARCHAR(32) DEFAULT NULL COMMENT 'History write status such as SAVED, NO_RESPONSE, WRITE_FAILED, or WRITE_SKIPPED',
  issue_scenes_json JSON DEFAULT NULL COMMENT 'Issue scene summary payload',
  logical_object_keys_json JSON DEFAULT NULL COMMENT 'Logical object hit summary payload',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
  PRIMARY KEY (item_id),
  KEY idx_parse_batch_item_batch_seq (batch_id, sequence_number),
  KEY idx_parse_batch_item_batch_status (batch_id, status),
  KEY idx_parse_batch_item_report (batch_id, report_code),
  KEY idx_parse_batch_item_history (history_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bulk parse batch imported records and parse evidence';

CREATE TABLE IF NOT EXISTS report_batch (
  batch_id VARCHAR(64) NOT NULL COMMENT 'Report catalog batch identifier',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Owning tenant identifier',
  batch_name VARCHAR(255) NOT NULL COMMENT 'Batch display name',
  file_type VARCHAR(16) NOT NULL COMMENT 'Source file type, currently TXT for mock source',
  report_code_field VARCHAR(128) NOT NULL COMMENT 'Column name that contains report_code',
  datasource_code VARCHAR(128) DEFAULT NULL COMMENT 'Datasource code override',
  stage VARCHAR(32) DEFAULT NULL COMMENT 'Stage metadata such as DEV/UAT/PROD',
  priority VARCHAR(32) DEFAULT NULL COMMENT 'Priority hint',
  parser_mode VARCHAR(32) NOT NULL DEFAULT 'JSQLPARSER' COMMENT 'SQL parser mode: JSQLPARSER or APACHE_CALCITE',
  source_type VARCHAR(32) NOT NULL COMMENT 'Source type such as TXT_MOCK_SOURCE',
  status VARCHAR(32) NOT NULL COMMENT 'Current batch status',
  total_reports INT NOT NULL DEFAULT 0 COMMENT 'Imported report count',
  resolved_reports INT NOT NULL DEFAULT 0 COMMENT 'Resolved report count',
  failed_reports INT NOT NULL DEFAULT 0 COMMENT 'Failed report count',
  status_history_json JSON NOT NULL COMMENT 'Batch status history payload',
  created_by VARCHAR(64) DEFAULT NULL COMMENT 'Batch creator',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
  PRIMARY KEY (batch_id),
  KEY idx_report_batch_tenant_status_created (tenant_id, status, created_at),
  KEY idx_report_batch_tenant_code (tenant_id, report_code_field)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Report catalog batch baseline';

CREATE TABLE IF NOT EXISTS report_batch_item (
  item_id VARCHAR(64) NOT NULL COMMENT 'Report batch item identifier',
  batch_id VARCHAR(64) NOT NULL COMMENT 'Owning report batch identifier',
  sequence_number INT NOT NULL COMMENT '1-based sequence number inside the uploaded batch',
  report_code VARCHAR(128) NOT NULL COMMENT 'Report code',
  report_name VARCHAR(255) DEFAULT NULL COMMENT 'Report name',
  datasource_code VARCHAR(128) DEFAULT NULL COMMENT 'Datasource code resolved for this row',
  stage VARCHAR(32) DEFAULT NULL COMMENT 'Stage metadata',
  priority VARCHAR(32) DEFAULT NULL COMMENT 'Priority hint',
  source_file_line VARCHAR(512) DEFAULT NULL COMMENT 'Original source line or CSV payload summary',
  sql_column_name VARCHAR(128) DEFAULT NULL COMMENT 'Source spreadsheet column that supplied this SQL',
  sql_ordinal_in_report INT DEFAULT NULL COMMENT '1-based SQL ordinal inside the same report_code',
  sql_text MEDIUMTEXT DEFAULT NULL COMMENT 'Resolved SQL text',
  parse_task_id VARCHAR(64) DEFAULT NULL COMMENT 'Parse task identifier',
  structure_syntax_status VARCHAR(32) DEFAULT NULL COMMENT 'Structure parse syntax status',
  access_service_status VARCHAR(32) DEFAULT NULL COMMENT 'Access parse provider status',
  access_connection_status VARCHAR(32) DEFAULT NULL COMMENT 'Access parse connection status',
  failure_reason VARCHAR(128) DEFAULT NULL COMMENT 'Failure or degrade reason',
  history_id VARCHAR(128) DEFAULT NULL COMMENT 'Governance query history id for this parsed report SQL',
  history_persisted TINYINT(1) DEFAULT NULL COMMENT 'Whether governance parse history was persisted for this SQL',
  history_persistence_status VARCHAR(32) DEFAULT NULL COMMENT 'History write status such as SAVED, NO_RESPONSE, WRITE_FAILED, or WRITE_SKIPPED',
  status VARCHAR(32) NOT NULL COMMENT 'Current item status',
  issue_scenes_json JSON DEFAULT NULL COMMENT 'Issue scene summary payload',
  logical_object_keys_json JSON DEFAULT NULL COMMENT 'Logical object hit summary payload',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
  PRIMARY KEY (item_id),
  KEY idx_report_batch_item_batch_seq (batch_id, sequence_number),
  KEY idx_report_batch_item_batch_status (batch_id, status),
  KEY idx_report_batch_item_report (batch_id, report_code),
  KEY idx_report_batch_item_report_sql (batch_id, report_code, sql_ordinal_in_report),
  KEY idx_report_batch_item_history (history_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Report catalog imported records and parse evidence';

CREATE TABLE IF NOT EXISTS acceleration_recommendation (
  recommendation_id VARCHAR(64) NOT NULL COMMENT 'Recommendation identifier',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Owning tenant identifier',
  recommendation_type VARCHAR(32) NOT NULL COMMENT 'REWRITE/ACCELERATION/CREATE_TABLE/PREWARM/MAINTENANCE',
  source_sql_id VARCHAR(64) DEFAULT NULL COMMENT 'Source SQL, parse item, history, or task identifier',
  history_id VARCHAR(64) DEFAULT NULL COMMENT 'Related query history identifier',
  parse_task_id VARCHAR(64) DEFAULT NULL COMMENT 'Related parse task identifier',
  batch_id VARCHAR(64) DEFAULT NULL COMMENT 'Related parse/report batch identifier',
  route_decision_id VARCHAR(64) DEFAULT NULL COMMENT 'Related route decision identifier',
  alert_id VARCHAR(64) DEFAULT NULL COMMENT 'Related alert identifier',
  sql_fingerprint VARCHAR(128) DEFAULT NULL COMMENT 'Normalized SQL fingerprint',
  source_sql_text MEDIUMTEXT DEFAULT NULL COMMENT 'Original SQL text or template',
  recommended_sql_text MEDIUMTEXT NOT NULL COMMENT 'Recommended SQL, create table SQL, prewarm SQL, or maintenance SQL',
  target_engine VARCHAR(64) DEFAULT NULL COMMENT 'Target execution engine',
  target_datasource VARCHAR(128) DEFAULT NULL COMMENT 'Target datasource code',
  report_code VARCHAR(128) DEFAULT NULL COMMENT 'Related report code',
  logical_object_key VARCHAR(255) DEFAULT NULL COMMENT 'Related logical object key',
  summary VARCHAR(512) DEFAULT NULL COMMENT 'Recommendation summary',
  reason TEXT DEFAULT NULL COMMENT 'Recommendation reason',
  expected_gain VARCHAR(512) DEFAULT NULL COMMENT 'Expected benefit description',
  benefit_level VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN' COMMENT 'UNKNOWN/LOW/MEDIUM/HIGH',
  risk_level VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN' COMMENT 'UNKNOWN/LOW/MEDIUM/HIGH/CRITICAL',
  risk_summary TEXT DEFAULT NULL COMMENT 'Risk summary and constraints',
  requires_dispatch TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 if external loading/dispatch coordination is required',
  status VARCHAR(32) NOT NULL DEFAULT 'RECOMMENDED' COMMENT 'RECOMMENDED/REVIEWING/DISPATCH_READY/CANCELLED; no executed state in SQLForge',
  created_by VARCHAR(64) DEFAULT NULL COMMENT 'Recommendation creator',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
  PRIMARY KEY (recommendation_id),
  KEY idx_acc_reco_tenant_type_created (tenant_id, recommendation_type, created_at),
  KEY idx_acc_reco_tenant_status_created (tenant_id, status, created_at),
  KEY idx_acc_reco_sql_fingerprint (tenant_id, sql_fingerprint),
  KEY idx_acc_reco_report_code (tenant_id, report_code),
  KEY idx_acc_reco_history (tenant_id, history_id),
  KEY idx_acc_reco_parse_task (tenant_id, parse_task_id),
  KEY idx_acc_reco_batch (tenant_id, batch_id),
  KEY idx_acc_reco_route (tenant_id, route_decision_id),
  KEY idx_acc_reco_alert (tenant_id, alert_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Read-only SQL acceleration and rewrite recommendation catalog';

CREATE TABLE IF NOT EXISTS dispatch_event (
  dispatch_event_id VARCHAR(64) NOT NULL COMMENT 'Dispatch event identifier',
  tenant_id VARCHAR(64) NOT NULL COMMENT 'Owning tenant identifier',
  recommendation_id VARCHAR(64) NOT NULL COMMENT 'Source recommendation identifier',
  dispatch_type VARCHAR(32) NOT NULL COMMENT 'REWRITE_SQL/ACCELERATION_SQL/CREATE_TABLE_SQL/PREWARM_SQL/MAINTENANCE_SQL',
  dispatch_payload_json JSON NOT NULL COMMENT 'Pull-based dispatch payload for external loading or maintenance module',
  target_engine VARCHAR(64) DEFAULT NULL COMMENT 'Target execution engine',
  target_datasource VARCHAR(128) DEFAULT NULL COMMENT 'Target datasource code',
  report_code VARCHAR(128) DEFAULT NULL COMMENT 'Related report code',
  logical_object_key VARCHAR(255) DEFAULT NULL COMMENT 'Related logical object key',
  status VARCHAR(32) NOT NULL COMMENT 'CREATED/PUBLISHED/PULLED/ACKED/FAILED',
  pulled_by VARCHAR(64) DEFAULT NULL COMMENT 'External module or operator that pulled the event',
  pulled_at DATETIME DEFAULT NULL COMMENT 'Pull timestamp',
  acked_by VARCHAR(64) DEFAULT NULL COMMENT 'External module or operator that acked the event',
  acked_at DATETIME DEFAULT NULL COMMENT 'Ack timestamp',
  failed_by VARCHAR(64) DEFAULT NULL COMMENT 'External module or operator that failed the event',
  failed_at DATETIME DEFAULT NULL COMMENT 'Failure timestamp',
  result_message TEXT DEFAULT NULL COMMENT 'Ack/fail message from external module',
  status_history_json JSON NOT NULL COMMENT 'Append-only state transition evidence',
  created_by VARCHAR(64) DEFAULT NULL COMMENT 'Dispatch creator',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
  PRIMARY KEY (dispatch_event_id),
  KEY idx_dispatch_event_tenant_status_created (tenant_id, status, created_at),
  KEY idx_dispatch_event_recommendation (tenant_id, recommendation_id),
  KEY idx_dispatch_event_report_code (tenant_id, report_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Pull-based recommendation dispatch state machine';

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
