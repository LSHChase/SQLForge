ALTER TABLE acceleration_recommendation
  ADD COLUMN history_id VARCHAR(64) DEFAULT NULL COMMENT 'Related query history identifier' AFTER source_sql_id,
  ADD COLUMN parse_task_id VARCHAR(64) DEFAULT NULL COMMENT 'Related parse task identifier' AFTER history_id,
  ADD COLUMN batch_id VARCHAR(64) DEFAULT NULL COMMENT 'Related parse/report batch identifier' AFTER parse_task_id,
  ADD COLUMN route_decision_id VARCHAR(64) DEFAULT NULL COMMENT 'Related route decision identifier' AFTER batch_id,
  ADD COLUMN alert_id VARCHAR(64) DEFAULT NULL COMMENT 'Related alert identifier' AFTER route_decision_id,
  ADD KEY idx_acc_reco_history (tenant_id, history_id),
  ADD KEY idx_acc_reco_parse_task (tenant_id, parse_task_id),
  ADD KEY idx_acc_reco_batch (tenant_id, batch_id),
  ADD KEY idx_acc_reco_route (tenant_id, route_decision_id),
  ADD KEY idx_acc_reco_alert (tenant_id, alert_id);
