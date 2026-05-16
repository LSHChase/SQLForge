CREATE TABLE IF NOT EXISTS acceleration_plan (
  plan_id VARCHAR(64) NOT NULL COMMENT '加速计划标识符',
  tenant_id VARCHAR(64) NOT NULL COMMENT '租户标识符',
  source_task_id VARCHAR(64) NOT NULL COMMENT '来源优化任务标识符',
  sql_text LONGTEXT DEFAULT NULL COMMENT '已提交 SQL 文本快照',
  sql_fingerprint VARCHAR(128) NOT NULL COMMENT '归一化 SQL 指纹',
  datasource_type VARCHAR(32) NOT NULL COMMENT '数据源类型',
  selected_suggestion_types_json JSON NOT NULL COMMENT '已选择的加速建议类型 JSON',
  plan_status VARCHAR(32) NOT NULL COMMENT '计划状态：PENDING_APPROVAL/APPROVED/REJECTED/APPLY_FAILED/APPLIED/VERIFY_FAILED/VERIFIED/ROLLBACK_FAILED/ROLLED_BACK',
  plan_summary TEXT DEFAULT NULL COMMENT '计划摘要',
  primary_recommendation TEXT DEFAULT NULL COMMENT '主要推荐',
  plan_payload_json JSON NOT NULL COMMENT '已选择的加速计划载荷 JSON',
  benefits_json JSON DEFAULT NULL COMMENT '结构化收益 JSON',
  costs_json JSON DEFAULT NULL COMMENT '结构化成本 JSON',
  risks_json JSON DEFAULT NULL COMMENT '结构化风险 JSON',
  config_snapshot_id VARCHAR(64) DEFAULT NULL COMMENT '治理配置快照标识符',
  result_id VARCHAR(64) DEFAULT NULL COMMENT '治理执行结果标识符',
  history_id VARCHAR(64) DEFAULT NULL COMMENT '治理查询历史标识符',
  review_note VARCHAR(512) DEFAULT NULL COMMENT '批准或驳回评审备注',
  approved_by VARCHAR(64) DEFAULT NULL COMMENT '批准人用户标识符',
  approved_at DATETIME(3) DEFAULT NULL COMMENT '批准时间戳',
  rejected_by VARCHAR(64) DEFAULT NULL COMMENT '驳回人用户标识符',
  rejected_at DATETIME(3) DEFAULT NULL COMMENT '驳回时间戳',
  last_error_code INT DEFAULT NULL COMMENT '最近一次生命周期失败码',
  last_error_message VARCHAR(512) DEFAULT NULL COMMENT '最近一次生命周期失败消息',
  runtime_binding_json JSON DEFAULT NULL COMMENT '应用/运行时绑定证据 JSON',
  runtime_binding_at DATETIME(3) DEFAULT NULL COMMENT '应用时间戳',
  runtime_binding_by VARCHAR(64) DEFAULT NULL COMMENT '应用操作人标识符',
  verification_evidence_json JSON DEFAULT NULL COMMENT '验证证据 JSON',
  verified_at DATETIME(3) DEFAULT NULL COMMENT '验证时间戳',
  verified_by VARCHAR(64) DEFAULT NULL COMMENT '验证操作人标识符',
  rollback_evidence_json JSON DEFAULT NULL COMMENT '回滚证据 JSON',
  rolled_back_at DATETIME(3) DEFAULT NULL COMMENT '回滚时间戳',
  rolled_back_by VARCHAR(64) DEFAULT NULL COMMENT '回滚操作人标识符',
  status_history_json JSON NOT NULL COMMENT '有序计划状态流转历史 JSON',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间戳',
  updated_at DATETIME(3) NOT NULL COMMENT '最后更新时间戳',
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
