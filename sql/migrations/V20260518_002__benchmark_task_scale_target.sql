ALTER TABLE benchmark_task
  ADD COLUMN scale_target_json JSON DEFAULT NULL COMMENT '生产规模目标与未验证证据边界 JSON' AFTER dataset_size_label;
