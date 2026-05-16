ALTER TABLE optimization_task
  ADD COLUMN task_context_json JSON DEFAULT NULL COMMENT '解析触发任务的来源上下文 JSON' AFTER requested_suggestion_types_json;
