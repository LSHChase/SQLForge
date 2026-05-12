ALTER TABLE query_history
  ADD COLUMN rewrite_record_id VARCHAR(64) DEFAULT NULL COMMENT 'SQL rewrite record identifier applied or evaluated for this execution' AFTER binding_summary,
  ADD COLUMN runtime_binding_id VARCHAR(64) DEFAULT NULL COMMENT 'Runtime rewrite binding identifier used by query-execution' AFTER rewrite_record_id,
  ADD COLUMN rewrite_rule_version BIGINT DEFAULT NULL COMMENT 'Numeric runtime rewrite rule version snapshot' AFTER runtime_binding_id,
  ADD COLUMN runtime_rule_version VARCHAR(64) DEFAULT NULL COMMENT 'Runtime rewrite rule version label snapshot' AFTER rewrite_rule_version,
  ADD COLUMN runtime_rewrite_status VARCHAR(32) DEFAULT NULL COMMENT 'Runtime rewrite binding resolution status snapshot' AFTER runtime_rule_version,
  ADD COLUMN rewrite_publish_status_snapshot VARCHAR(32) DEFAULT NULL COMMENT 'Publish status snapshot derived from backend runtime evidence' AFTER runtime_rewrite_status,
  ADD COLUMN rewrite_fallback_reason VARCHAR(128) DEFAULT NULL COMMENT 'Reason automatic rewrite fell back to original SQL' AFTER rewrite_publish_status_snapshot,
  ADD KEY idx_query_history_rewrite_record (tenant_id, rewrite_record_id, create_time),
  ADD KEY idx_query_history_runtime_binding (tenant_id, runtime_binding_id, create_time);
