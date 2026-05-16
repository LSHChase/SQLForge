ALTER TABLE benchmark_task_report
  ADD COLUMN execution_summary_json JSON DEFAULT NULL COMMENT '隔离执行摘要 JSON' AFTER recommendations_json,
  ADD COLUMN export_artifacts_json JSON DEFAULT NULL COMMENT '已持久化导出制品元数据与内容 JSON' AFTER execution_summary_json;
