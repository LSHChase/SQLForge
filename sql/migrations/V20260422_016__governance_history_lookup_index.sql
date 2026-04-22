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
