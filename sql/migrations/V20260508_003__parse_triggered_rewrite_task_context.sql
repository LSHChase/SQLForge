ALTER TABLE optimization_task
  ADD COLUMN task_context_json JSON DEFAULT NULL COMMENT 'Parse-triggered task source context JSON' AFTER requested_suggestion_types_json;
