ALTER TABLE benchmark_task_report
  ADD COLUMN execution_summary_json JSON DEFAULT NULL COMMENT 'Isolated execution summary JSON' AFTER recommendations_json,
  ADD COLUMN export_artifacts_json JSON DEFAULT NULL COMMENT 'Persisted export artifact metadata and content JSON' AFTER execution_summary_json;
