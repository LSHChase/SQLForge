ALTER TABLE runtime_rewrite_binding
  ADD COLUMN runtime_match_object_refs_json JSON DEFAULT NULL COMMENT '激活时原 SQL 表面对象引用快照，仅用于运行时匹配边界' AFTER template_family_fingerprint,
  ADD COLUMN runtime_match_object_names_json JSON DEFAULT NULL COMMENT '激活时原 SQL 表面对象名集合，禁止写入展开后的底层表作为匹配键' AFTER runtime_match_object_refs_json,
  ADD COLUMN analysis_physical_object_refs_json JSON DEFAULT NULL COMMENT '解析推荐阶段展开的底层物理对象证据，仅用于分析验证' AFTER runtime_match_object_names_json,
  ADD COLUMN metadata_snapshot_version VARCHAR(128) DEFAULT NULL COMMENT '激活校验使用的元数据快照版本' AFTER analysis_physical_object_refs_json,
  ADD COLUMN view_definition_hash VARCHAR(128) DEFAULT NULL COMMENT '激活校验使用的 view definition hash' AFTER metadata_snapshot_version,
  ADD COLUMN metadata_degradation_reason VARCHAR(512) DEFAULT NULL COMMENT '元数据不可用、过期或降级的原因快照' AFTER view_definition_hash;
