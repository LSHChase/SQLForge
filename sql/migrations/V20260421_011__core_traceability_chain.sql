ALTER TABLE audit_log
  ADD COLUMN service_code VARCHAR(32) DEFAULT NULL COMMENT '用于可追踪性的归属服务编码' AFTER tenant_id,
  ADD COLUMN request_id VARCHAR(64) DEFAULT NULL COMMENT '请求标识符' AFTER target_id,
  ADD COLUMN trace_id VARCHAR(64) DEFAULT NULL COMMENT '追踪标识符' AFTER request_id,
  ADD COLUMN saga_id VARCHAR(64) DEFAULT NULL COMMENT '用于跨服务补偿链的 Saga 标识符' AFTER trace_id,
  ADD COLUMN config_snapshot_id VARCHAR(64) DEFAULT NULL COMMENT '引用的配置快照标识符' AFTER saga_id,
  ADD COLUMN result_id VARCHAR(64) DEFAULT NULL COMMENT '引用的执行结果标识符' AFTER config_snapshot_id,
  ADD COLUMN history_id VARCHAR(64) DEFAULT NULL COMMENT '引用的查询历史标识符' AFTER result_id,
  ADD COLUMN export_id VARCHAR(64) DEFAULT NULL COMMENT '引用的导出记录标识符' AFTER history_id,
  ADD KEY idx_audit_log_trace_request (trace_id, request_id),
  ADD KEY idx_audit_log_config_snapshot_id (config_snapshot_id),
  ADD KEY idx_audit_log_result_id (result_id),
  ADD KEY idx_audit_log_history_id (history_id),
  ADD KEY idx_audit_log_export_id (export_id);

CREATE TABLE IF NOT EXISTS config_snapshot (
  config_snapshot_id VARCHAR(64) NOT NULL COMMENT '配置快照标识符',
  tenant_id VARCHAR(64) NOT NULL COMMENT '租户标识符',
  service_code VARCHAR(32) NOT NULL COMMENT '归属服务编码',
  source_config_type VARCHAR(32) NOT NULL COMMENT '来源配置类型，例如 TENANT_CONFIG/SYSTEM_CONFIG/ACCELERATION_CONFIG',
  source_config_id VARCHAR(128) NOT NULL COMMENT '来源配置业务标识符',
  source_version VARCHAR(64) DEFAULT NULL COMMENT '可选来源配置版本',
  snapshot_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '快照状态：ACTIVE/DEPRECATED/ROLLED_BACK',
  snapshot_reason VARCHAR(128) DEFAULT NULL COMMENT '快照原因，例如 TASK_SUBMIT/EXPORT_REQUEST/AUDIT_REPLAY',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT '追踪标识符',
  request_id VARCHAR(64) DEFAULT NULL COMMENT '请求标识符',
  saga_id VARCHAR(64) DEFAULT NULL COMMENT '跨服务补偿链 Saga 标识符',
  snapshot_payload JSON NOT NULL COMMENT '配置快照载荷',
  created_by VARCHAR(64) DEFAULT NULL COMMENT '操作人标识符',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间戳',
  PRIMARY KEY (config_snapshot_id),
  KEY idx_config_snapshot_tenant_time (tenant_id, create_time),
  KEY idx_config_snapshot_source (source_config_type, source_config_id),
  KEY idx_config_snapshot_trace (trace_id, request_id),
  KEY idx_config_snapshot_status (snapshot_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Immutable configuration snapshots used as the traceability anchor';

CREATE TABLE IF NOT EXISTS execution_result (
  result_id VARCHAR(64) NOT NULL COMMENT '执行结果标识符',
  config_snapshot_id VARCHAR(64) NOT NULL COMMENT '引用的配置快照标识符',
  tenant_id VARCHAR(64) NOT NULL COMMENT '租户标识符',
  service_code VARCHAR(32) NOT NULL COMMENT '归属服务编码',
  task_id VARCHAR(64) DEFAULT NULL COMMENT '关联任务标识符',
  task_type VARCHAR(32) NOT NULL COMMENT '任务类型，例如 QUERY_EXECUTION/OPTIMIZATION/BENCHMARK',
  result_status VARCHAR(16) NOT NULL COMMENT '结果状态：QUEUED/RUNNING/SUCCEEDED/FAILED/CANCELLED',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT '追踪标识符',
  request_id VARCHAR(64) DEFAULT NULL COMMENT '请求标识符',
  saga_id VARCHAR(64) DEFAULT NULL COMMENT '跨服务补偿链 Saga 标识符',
  result_summary JSON DEFAULT NULL COMMENT '不含敏感数据的结构化摘要',
  result_payload JSON DEFAULT NULL COMMENT '结构化结果载荷',
  error_code VARCHAR(32) DEFAULT NULL COMMENT '失败时的错误码',
  error_message VARCHAR(512) DEFAULT NULL COMMENT '脱敏后的错误消息',
  started_at DATETIME DEFAULT NULL COMMENT '开始时间',
  finished_at DATETIME DEFAULT NULL COMMENT '结束时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间戳',
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
  history_id VARCHAR(64) NOT NULL COMMENT '查询历史标识符',
  result_id VARCHAR(64) NOT NULL COMMENT '引用的执行结果标识符',
  tenant_id VARCHAR(64) NOT NULL COMMENT '租户标识符',
  history_type VARCHAR(32) NOT NULL COMMENT '历史类型，例如 QUERY_EXECUTION/SQL_OPTIMIZATION/BENCHMARK',
  sql_fingerprint CHAR(32) NOT NULL COMMENT '归一化 SQL 指纹',
  sql_text_cipher MEDIUMBLOB DEFAULT NULL COMMENT '加密 SQL 文本载荷，仅保存密文',
  datasource_type VARCHAR(32) DEFAULT NULL COMMENT '数据源类型',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT '追踪标识符',
  request_id VARCHAR(64) DEFAULT NULL COMMENT '请求标识符',
  saga_id VARCHAR(64) DEFAULT NULL COMMENT '跨服务补偿链 Saga 标识符',
  query_context JSON DEFAULT NULL COMMENT '结构化查询上下文',
  submitted_by VARCHAR(64) DEFAULT NULL COMMENT '操作人标识符',
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间戳',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间戳',
  PRIMARY KEY (history_id),
  KEY idx_query_history_result_id (result_id),
  KEY idx_query_history_tenant_time (tenant_id, create_time),
  KEY idx_query_history_fingerprint (sql_fingerprint),
  KEY idx_query_history_trace (trace_id, request_id),
  CONSTRAINT fk_query_history_result FOREIGN KEY (result_id)
    REFERENCES execution_result (result_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Historical immutable query snapshots linked to execution results';

CREATE TABLE IF NOT EXISTS export_record (
  export_id VARCHAR(64) NOT NULL COMMENT '导出记录标识符',
  history_id VARCHAR(64) NOT NULL COMMENT '引用的查询历史标识符',
  result_id VARCHAR(64) NOT NULL COMMENT '引用的执行结果标识符',
  tenant_id VARCHAR(64) NOT NULL COMMENT '租户标识符',
  export_format VARCHAR(16) NOT NULL COMMENT '导出格式：JSON/PDF/HTML/CSV',
  export_status VARCHAR(16) NOT NULL COMMENT '导出状态：REQUESTED/GENERATING/READY/FAILED/EXPIRED',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT '追踪标识符',
  request_id VARCHAR(64) DEFAULT NULL COMMENT '请求标识符',
  saga_id VARCHAR(64) DEFAULT NULL COMMENT '跨服务补偿链 Saga 标识符',
  storage_type VARCHAR(32) DEFAULT NULL COMMENT '存储类型，例如 INLINE/MINIO/OSS',
  storage_uri VARCHAR(512) DEFAULT NULL COMMENT '导出存储位置',
  checksum VARCHAR(128) DEFAULT NULL COMMENT '导出校验和',
  export_options JSON DEFAULT NULL COMMENT '结构化导出选项',
  error_code VARCHAR(32) DEFAULT NULL COMMENT '失败时的错误码',
  error_message VARCHAR(512) DEFAULT NULL COMMENT '脱敏后的错误消息',
  created_by VARCHAR(64) DEFAULT NULL COMMENT '操作人标识符',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间戳',
  finished_at DATETIME DEFAULT NULL COMMENT '结束时间戳',
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
