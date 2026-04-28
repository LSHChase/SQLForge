ALTER TABLE benchmark_task_report
    ADD COLUMN regression_summary_json JSON DEFAULT NULL COMMENT 'Regression summary JSON' AFTER execution_summary_json,
    ADD COLUMN alert_linkages_json JSON DEFAULT NULL COMMENT 'Governance alert linkage JSON' AFTER regression_summary_json;
