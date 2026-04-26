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

DELETE FROM governance_history_lookup_index
WHERE source_target_type = 'SQL_ACCELERATION_PLAN';

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
