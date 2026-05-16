ALTER TABLE benchmark_task_report
    ADD COLUMN regression_summary_json JSON DEFAULT NULL COMMENT '回归摘要 JSON' AFTER execution_summary_json,
    ADD COLUMN alert_linkages_json JSON DEFAULT NULL COMMENT '治理告警关联 JSON' AFTER regression_summary_json;
