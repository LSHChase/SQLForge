ALTER TABLE audit_log
  ADD COLUMN service_code VARCHAR(32) DEFAULT NULL COMMENT 'Owner service code for traceability' AFTER tenant_id,
  ADD COLUMN request_id VARCHAR(64) DEFAULT NULL COMMENT 'Request identifier' AFTER target_id,
  ADD COLUMN trace_id VARCHAR(64) DEFAULT NULL COMMENT 'Trace identifier' AFTER request_id,
  ADD COLUMN saga_id VARCHAR(64) DEFAULT NULL COMMENT 'Saga identifier for cross-service compensation chain' AFTER trace_id,
  ADD COLUMN config_snapshot_id VARCHAR(64) DEFAULT NULL COMMENT 'Referenced config snapshot identifier' AFTER saga_id,
  ADD COLUMN result_id VARCHAR(64) DEFAULT NULL COMMENT 'Referenced execution result identifier' AFTER config_snapshot_id,
  ADD COLUMN history_id VARCHAR(64) DEFAULT NULL COMMENT 'Referenced query history identifier' AFTER result_id,
  ADD COLUMN export_id VARCHAR(64) DEFAULT NULL COMMENT 'Referenced export record identifier' AFTER history_id,
  ADD KEY idx_audit_log_trace_request (trace_id, request_id),
  ADD KEY idx_audit_log_config_snapshot_id (config_snapshot_id),
  ADD KEY idx_audit_log_result_id (result_id),
  ADD KEY idx_audit_log_history_id (history_id),
  ADD KEY idx_audit_log_export_id (export_id);

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
  snapshot_payload JSON NOT NULL COMMENT 'Config snapshot payload',
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
  result_payload JSON DEFAULT NULL COMMENT 'Structured result payload',
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
  query_context JSON DEFAULT NULL COMMENT 'Structured query context',
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
  storage_uri VARCHAR(512) DEFAULT NULL COMMENT 'Export storage location',
  checksum VARCHAR(128) DEFAULT NULL COMMENT 'Export checksum',
  export_options JSON DEFAULT NULL COMMENT 'Structured export options',
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

ALTER TABLE audit_log
  ADD CONSTRAINT fk_audit_log_config_snapshot FOREIGN KEY (config_snapshot_id)
    REFERENCES config_snapshot (config_snapshot_id),
  ADD CONSTRAINT fk_audit_log_result FOREIGN KEY (result_id)
    REFERENCES execution_result (result_id),
  ADD CONSTRAINT fk_audit_log_history FOREIGN KEY (history_id)
    REFERENCES query_history (history_id),
  ADD CONSTRAINT fk_audit_log_export FOREIGN KEY (export_id)
    REFERENCES export_record (export_id);
