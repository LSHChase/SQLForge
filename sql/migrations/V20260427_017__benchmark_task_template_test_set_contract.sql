ALTER TABLE benchmark_task
  ADD COLUMN template_id VARCHAR(64) DEFAULT NULL COMMENT '压测模板标识符' AFTER dataset_size_label,
  ADD COLUMN template_type VARCHAR(32) DEFAULT NULL COMMENT '压测模板类型' AFTER template_id,
  ADD COLUMN template_version VARCHAR(32) DEFAULT NULL COMMENT '压测模板契约版本' AFTER template_type,
  ADD COLUMN test_set_id VARCHAR(64) DEFAULT NULL COMMENT '压测测试集标识符' AFTER template_version,
  ADD COLUMN test_set_source VARCHAR(32) DEFAULT NULL COMMENT '压测测试集来源类型' AFTER test_set_id,
  ADD COLUMN test_set_labels_json JSON DEFAULT NULL COMMENT '压测测试集标签模型 JSON' AFTER test_set_source,
  ADD COLUMN test_set_source_refs_json JSON DEFAULT NULL COMMENT '压测测试集来源引用 JSON' AFTER test_set_labels_json;

CREATE INDEX idx_benchmark_task_template_type ON benchmark_task (template_type);
CREATE INDEX idx_benchmark_task_test_set_source ON benchmark_task (test_set_source);
