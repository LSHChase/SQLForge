ALTER TABLE datasource_config
  ADD COLUMN driver_source_type VARCHAR(32) NOT NULL DEFAULT 'CLASSPATH' COMMENT '驱动来源类型：CLASSPATH/UPLOADED' AFTER jdbc_driver_class_name,
  ADD COLUMN jdbc_driver_artifact_id VARCHAR(64) DEFAULT NULL COMMENT '已绑定的上传驱动制品标识符' AFTER driver_source_type,
  ADD COLUMN driver_version_label VARCHAR(128) DEFAULT NULL COMMENT '已绑定驱动版本标签快照' AFTER jdbc_driver_artifact_id,
  ADD COLUMN driver_sha256 VARCHAR(64) DEFAULT NULL COMMENT '已绑定驱动 sha256 快照' AFTER driver_version_label,
  ADD COLUMN driver_load_status VARCHAR(32) DEFAULT NULL COMMENT '已绑定驱动加载状态快照' AFTER driver_sha256;

CREATE TABLE IF NOT EXISTS jdbc_driver_artifact (
  artifact_id VARCHAR(64) NOT NULL COMMENT 'JDBC 驱动制品标识符',
  tenant_id VARCHAR(64) NOT NULL COMMENT '所属租户标识符',
  engine_type VARCHAR(32) NOT NULL COMMENT '引擎类型：TRINO/HETU/HIVE',
  driver_class_name VARCHAR(255) NOT NULL COMMENT '驱动主类名',
  version_label VARCHAR(128) NOT NULL COMMENT '版本标签',
  original_file_name VARCHAR(255) NOT NULL COMMENT '原始上传文件名',
  size_bytes BIGINT NOT NULL COMMENT '文件大小，单位字节',
  sha256 VARCHAR(64) NOT NULL COMMENT '文件 sha256 校验和',
  relative_path VARCHAR(512) NOT NULL COMMENT '共享卷相对路径',
  status VARCHAR(32) NOT NULL COMMENT '制品状态：READY/FAILED',
  uploaded_by VARCHAR(64) DEFAULT NULL COMMENT '上传操作者标识符',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间戳',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间戳',
  PRIMARY KEY (artifact_id),
  KEY idx_jdbc_driver_artifact_tenant_time (tenant_id, create_time),
  KEY idx_jdbc_driver_artifact_tenant_engine (tenant_id, engine_type, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Append-only uploaded JDBC driver artifacts';
