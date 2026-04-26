ALTER TABLE optimization_task
  ADD COLUMN suggestion_payload_json JSON DEFAULT NULL COMMENT 'Structured suggestion payload JSON' AFTER summary,
  ADD COLUMN failed_phase VARCHAR(32) DEFAULT NULL COMMENT 'Execution phase that produced the failure' AFTER error_retryable,
  ADD COLUMN error_risks_json JSON DEFAULT NULL COMMENT 'Structured failure risks JSON' AFTER failed_phase;
