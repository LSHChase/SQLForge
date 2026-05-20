ALTER TABLE acceleration_recommendation
  ADD COLUMN acceleration_artifact_json JSON DEFAULT NULL COMMENT '高级 MV accelerationArtifact 持久快照，响应仅返回允许的高级 MV 类型' AFTER semantic_risks_json;
