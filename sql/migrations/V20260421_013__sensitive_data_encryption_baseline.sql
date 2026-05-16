ALTER TABLE system_config
  MODIFY COLUMN config_value VARCHAR(512) NULL COMMENT '仅非敏感系统配置值',
  ADD COLUMN sensitive_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT '值为 1 表示该值仅存储在密文字段中' AFTER config_value,
  ADD COLUMN value_ciphertext TEXT DEFAULT NULL COMMENT '敏感配置值的 AES-256 密文信封' AFTER sensitive_flag,
  ADD COLUMN value_mask VARCHAR(128) DEFAULT NULL COMMENT '敏感配置值的脱敏预览' AFTER value_ciphertext,
  ADD COLUMN encryption_algorithm VARCHAR(32) DEFAULT NULL COMMENT '敏感值加密算法' AFTER value_mask,
  ADD COLUMN encryption_key_id VARCHAR(64) DEFAULT NULL COMMENT '敏感值加密密钥标识符' AFTER encryption_algorithm;
