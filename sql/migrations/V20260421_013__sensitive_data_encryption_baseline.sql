ALTER TABLE system_config
  MODIFY COLUMN config_value VARCHAR(512) NULL COMMENT 'Non-sensitive system configuration value only',
  ADD COLUMN sensitive_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 means value is stored only in ciphertext columns' AFTER config_value,
  ADD COLUMN value_ciphertext TEXT DEFAULT NULL COMMENT 'AES-256 ciphertext envelope for sensitive config value' AFTER sensitive_flag,
  ADD COLUMN value_mask VARCHAR(128) DEFAULT NULL COMMENT 'Masked preview for sensitive config value' AFTER value_ciphertext,
  ADD COLUMN encryption_algorithm VARCHAR(32) DEFAULT NULL COMMENT 'Sensitive value encryption algorithm' AFTER value_mask,
  ADD COLUMN encryption_key_id VARCHAR(64) DEFAULT NULL COMMENT 'Sensitive value encryption key identifier' AFTER encryption_algorithm;
