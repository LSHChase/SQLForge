ALTER TABLE query_history
  ADD COLUMN rewrite_record_id VARCHAR(64) DEFAULT NULL COMMENT '本次执行已应用或评估的 SQL 改写记录标识符' AFTER binding_summary,
  ADD COLUMN runtime_binding_id VARCHAR(64) DEFAULT NULL COMMENT 'query-execution 使用的运行时改写绑定标识符' AFTER rewrite_record_id,
  ADD COLUMN rewrite_rule_version BIGINT DEFAULT NULL COMMENT '数值型运行时改写规则版本快照' AFTER runtime_binding_id,
  ADD COLUMN runtime_rule_version VARCHAR(64) DEFAULT NULL COMMENT '运行时改写规则版本标签快照' AFTER rewrite_rule_version,
  ADD COLUMN runtime_rewrite_status VARCHAR(32) DEFAULT NULL COMMENT '运行时改写绑定解析状态快照' AFTER runtime_rule_version,
  ADD COLUMN rewrite_publish_status_snapshot VARCHAR(32) DEFAULT NULL COMMENT '从后端运行时证据派生的发布状态快照' AFTER runtime_rewrite_status,
  ADD COLUMN rewrite_fallback_reason VARCHAR(128) DEFAULT NULL COMMENT '自动改写回退到原始 SQL 的原因' AFTER rewrite_publish_status_snapshot,
  ADD KEY idx_query_history_rewrite_record (tenant_id, rewrite_record_id, create_time),
  ADD KEY idx_query_history_runtime_binding (tenant_id, runtime_binding_id, create_time);
