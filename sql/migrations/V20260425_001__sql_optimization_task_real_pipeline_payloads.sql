ALTER TABLE optimization_task
  ADD COLUMN suggestion_payload_json JSON DEFAULT NULL COMMENT '结构化建议载荷 JSON' AFTER summary,
  ADD COLUMN failed_phase VARCHAR(32) DEFAULT NULL COMMENT '产生失败的执行阶段' AFTER error_retryable,
  ADD COLUMN error_risks_json JSON DEFAULT NULL COMMENT '结构化失败风险 JSON' AFTER failed_phase;
