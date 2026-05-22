ALTER TABLE runtime_rewrite_binding
  ADD COLUMN original_sql_text MEDIUMTEXT DEFAULT NULL COMMENT '激活时生成改写规则程序的原始 SQL 模板' AFTER original_sql_digest,
  ADD COLUMN rewrite_match_mode VARCHAR(64) NOT NULL DEFAULT 'EXACT_FINGERPRINT' COMMENT '运行时改写匹配模式：EXACT_FINGERPRINT/TEMPLATE_CONDITION_REPLAY' AFTER recommended_sql_text,
  ADD COLUMN rewrite_program_json MEDIUMTEXT DEFAULT NULL COMMENT '参数化改写模板、谓词来源、适用前置条件与风险边界证据' AFTER rewrite_match_mode,
  ADD COLUMN template_family_fingerprint VARCHAR(128) DEFAULT NULL COMMENT '忽略可重放 WHERE 条件后的模板族指纹' AFTER rewrite_program_json,
  ADD KEY idx_runtime_rewrite_tenant_status (tenant_id, status),
  ADD KEY idx_runtime_rewrite_template_family (tenant_id, template_family_fingerprint, status);
