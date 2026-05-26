CREATE TABLE IF NOT EXISTS report_batch_stat_summary (
  batch_id VARCHAR(64) NOT NULL COMMENT '报表批次标识符',
  tenant_id VARCHAR(64) NOT NULL COMMENT '所属租户标识符',
  total_sql_count INT NOT NULL DEFAULT 0 COMMENT '批次 SQL 总数',
  resolved_sql_count INT NOT NULL DEFAULT 0 COMMENT '完全解析 SQL 数',
  partial_resolved_sql_count INT NOT NULL DEFAULT 0 COMMENT '部分解析 SQL 数',
  failed_sql_count INT NOT NULL DEFAULT 0 COMMENT '解析失败 SQL 数',
  issue_sql_count INT NOT NULL DEFAULT 0 COMMENT '存在问题场景的 SQL 数',
  total_issue_count INT NOT NULL DEFAULT 0 COMMENT '问题场景出现总次数',
  issue_scene_count INT NOT NULL DEFAULT 0 COMMENT '不同问题场景数',
  important_sql_count INT NOT NULL DEFAULT 0 COMMENT '重要问题 SQL 数',
  urgent_sql_count INT NOT NULL DEFAULT 0 COMMENT '紧急问题 SQL 数',
  merge_candidate_report_count INT NOT NULL DEFAULT 0 COMMENT '可合并候选报表数',
  plan_analysis_success_count INT NOT NULL DEFAULT 0 COMMENT '计划分析成功 SQL 数',
  plan_analysis_partial_count INT NOT NULL DEFAULT 0 COMMENT '计划分析部分或跳过 SQL 数',
  plan_analysis_failed_count INT NOT NULL DEFAULT 0 COMMENT '计划分析失败 SQL 数',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间戳',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间戳',
  PRIMARY KEY (batch_id),
  KEY idx_report_batch_stat_summary_batch (batch_id, updated_at),
  KEY idx_report_batch_stat_summary_tenant (tenant_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Report batch normalized statistics summary';

CREATE TABLE IF NOT EXISTS report_batch_issue_scene_stat (
  batch_id VARCHAR(64) NOT NULL COMMENT '报表批次标识符',
  tenant_id VARCHAR(64) NOT NULL COMMENT '所属租户标识符',
  issue_scene VARCHAR(128) NOT NULL COMMENT '问题场景编码',
  issue_domain VARCHAR(64) DEFAULT NULL COMMENT '问题领域',
  severity VARCHAR(32) DEFAULT NULL COMMENT '严重级别',
  priority_level VARCHAR(16) DEFAULT NULL COMMENT '优先级等级',
  priority_score INT NOT NULL DEFAULT 0 COMMENT '优先级分值',
  affected_sql_count INT NOT NULL DEFAULT 0 COMMENT '受影响 SQL 数',
  affected_issue_count INT NOT NULL DEFAULT 0 COMMENT '受影响问题次数',
  sql_ratio DECIMAL(12,8) NOT NULL DEFAULT 0 COMMENT '受影响 SQL 占比',
  important TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否重要',
  urgent TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否紧急',
  report_count INT NOT NULL DEFAULT 0 COMMENT '受影响报表数',
  logical_object_count INT NOT NULL DEFAULT 0 COMMENT '受影响逻辑对象数',
  sample_report_codes_json JSON DEFAULT NULL COMMENT '样例报表编码列表',
  sample_logical_object_keys_json JSON DEFAULT NULL COMMENT '样例逻辑对象键列表',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间戳',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间戳',
  PRIMARY KEY (batch_id, issue_scene),
  KEY idx_report_batch_issue_scene_batch (batch_id, affected_sql_count),
  KEY idx_report_batch_issue_scene_tenant (tenant_id, issue_scene)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Report batch issue scene statistics';

CREATE TABLE IF NOT EXISTS report_batch_severity_stat (
  batch_id VARCHAR(64) NOT NULL COMMENT '报表批次标识符',
  tenant_id VARCHAR(64) NOT NULL COMMENT '所属租户标识符',
  severity VARCHAR(32) NOT NULL COMMENT '严重级别',
  issue_count INT NOT NULL DEFAULT 0 COMMENT '问题次数',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间戳',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间戳',
  PRIMARY KEY (batch_id, severity),
  KEY idx_report_batch_severity_batch (batch_id, severity),
  KEY idx_report_batch_severity_tenant (tenant_id, severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Report batch severity distribution';

CREATE TABLE IF NOT EXISTS report_batch_report_stat (
  batch_id VARCHAR(64) NOT NULL COMMENT '报表批次标识符',
  tenant_id VARCHAR(64) NOT NULL COMMENT '所属租户标识符',
  report_code VARCHAR(128) NOT NULL COMMENT '报表编码',
  sql_count INT NOT NULL DEFAULT 0 COMMENT '报表 SQL 数',
  issue_sql_count INT NOT NULL DEFAULT 0 COMMENT '存在问题 SQL 数',
  issue_count INT NOT NULL DEFAULT 0 COMMENT '问题次数',
  issue_sql_ratio DECIMAL(12,8) NOT NULL DEFAULT 0 COMMENT '问题 SQL 占比',
  highest_priority_level VARCHAR(16) DEFAULT NULL COMMENT '最高优先级等级',
  highest_priority_score INT NOT NULL DEFAULT 0 COMMENT '最高优先级分值',
  important TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否重要',
  urgent TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否紧急',
  issue_scenes_json JSON DEFAULT NULL COMMENT '问题场景列表',
  merge_candidate TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否合并候选',
  merge_candidate_sql_count INT NOT NULL DEFAULT 0 COMMENT '合并候选 SQL 数',
  merge_candidate_reason VARCHAR(512) DEFAULT NULL COMMENT '合并候选原因',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间戳',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间戳',
  PRIMARY KEY (batch_id, report_code),
  KEY idx_report_batch_report_batch (batch_id, issue_sql_count),
  KEY idx_report_batch_report_tenant (tenant_id, report_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Report batch report-level statistics';

CREATE TABLE IF NOT EXISTS report_batch_sql_stat (
  batch_id VARCHAR(64) NOT NULL COMMENT '报表批次标识符',
  tenant_id VARCHAR(64) NOT NULL COMMENT '所属租户标识符',
  item_id VARCHAR(64) NOT NULL COMMENT '报表批次条目标识符',
  parse_task_id VARCHAR(64) DEFAULT NULL COMMENT '解析任务标识符',
  report_code VARCHAR(128) DEFAULT NULL COMMENT '报表编码',
  report_name VARCHAR(255) DEFAULT NULL COMMENT '报表名称',
  datasource_code VARCHAR(128) DEFAULT NULL COMMENT '数据源编码',
  stage VARCHAR(32) DEFAULT NULL COMMENT '阶段元数据',
  sql_column_name VARCHAR(128) DEFAULT NULL COMMENT 'SQL 来源列名',
  sql_ordinal_in_report INT DEFAULT NULL COMMENT '同一报表内 SQL 序号',
  status VARCHAR(32) DEFAULT NULL COMMENT '条目解析状态',
  sql_digest VARCHAR(255) DEFAULT NULL COMMENT 'SQL 摘要',
  issue_count INT NOT NULL DEFAULT 0 COMMENT '问题次数',
  highest_priority_level VARCHAR(16) DEFAULT NULL COMMENT '最高优先级等级',
  highest_priority_score INT NOT NULL DEFAULT 0 COMMENT '最高优先级分值',
  important TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否重要',
  urgent TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否紧急',
  issue_locations_json JSON DEFAULT NULL COMMENT '问题定位列表',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间戳',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间戳',
  PRIMARY KEY (batch_id, item_id),
  KEY idx_report_batch_sql_batch_priority (batch_id, highest_priority_score, issue_count),
  KEY idx_report_batch_sql_batch_report (batch_id, report_code),
  KEY idx_report_batch_sql_tenant (tenant_id, report_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Report batch SQL-level statistics';

CREATE TABLE IF NOT EXISTS report_batch_sql_issue_scene_stat (
  batch_id VARCHAR(64) NOT NULL COMMENT '报表批次标识符',
  tenant_id VARCHAR(64) NOT NULL COMMENT '所属租户标识符',
  item_id VARCHAR(64) NOT NULL COMMENT '报表批次条目标识符',
  issue_scene VARCHAR(128) NOT NULL COMMENT '问题场景编码',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间戳',
  PRIMARY KEY (batch_id, item_id, issue_scene),
  KEY idx_report_batch_sql_issue_scene_batch (batch_id, issue_scene),
  KEY idx_report_batch_sql_issue_scene_tenant (tenant_id, issue_scene)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Report batch SQL issue scene membership';

CREATE TABLE IF NOT EXISTS report_batch_sql_logical_object_stat (
  batch_id VARCHAR(64) NOT NULL COMMENT '报表批次标识符',
  tenant_id VARCHAR(64) NOT NULL COMMENT '所属租户标识符',
  item_id VARCHAR(64) NOT NULL COMMENT '报表批次条目标识符',
  logical_object_key VARCHAR(255) NOT NULL COMMENT '逻辑对象键',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间戳',
  PRIMARY KEY (batch_id, item_id, logical_object_key),
  KEY idx_report_batch_sql_logical_object_batch (batch_id, logical_object_key),
  KEY idx_report_batch_sql_logical_object_tenant (tenant_id, logical_object_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Report batch SQL logical object membership';

CREATE TABLE IF NOT EXISTS report_batch_priority_stat (
  batch_id VARCHAR(64) NOT NULL COMMENT '报表批次标识符',
  tenant_id VARCHAR(64) NOT NULL COMMENT '所属租户标识符',
  priority_level VARCHAR(16) NOT NULL COMMENT '优先级等级',
  urgency_bucket VARCHAR(32) NOT NULL COMMENT '重要紧急桶',
  sql_count INT NOT NULL DEFAULT 0 COMMENT 'SQL 数',
  issue_count INT NOT NULL DEFAULT 0 COMMENT '问题次数',
  report_count INT NOT NULL DEFAULT 0 COMMENT '报表数',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间戳',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间戳',
  PRIMARY KEY (batch_id, priority_level, urgency_bucket),
  KEY idx_report_batch_priority_batch (batch_id, priority_level),
  KEY idx_report_batch_priority_tenant (tenant_id, priority_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Report batch priority matrix statistics';

CREATE TABLE IF NOT EXISTS report_batch_importance_stat (
  batch_id VARCHAR(64) NOT NULL COMMENT '报表批次标识符',
  tenant_id VARCHAR(64) NOT NULL COMMENT '所属租户标识符',
  importance_bucket VARCHAR(32) NOT NULL COMMENT '重要紧急桶',
  sql_count INT NOT NULL DEFAULT 0 COMMENT 'SQL 数',
  issue_count INT NOT NULL DEFAULT 0 COMMENT '问题次数',
  report_count INT NOT NULL DEFAULT 0 COMMENT '报表数',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间戳',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间戳',
  PRIMARY KEY (batch_id, importance_bucket),
  KEY idx_report_batch_importance_batch (batch_id, sql_count),
  KEY idx_report_batch_importance_tenant (tenant_id, importance_bucket)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Report batch importance bucket statistics';

CREATE TABLE IF NOT EXISTS report_batch_logical_object_stat (
  batch_id VARCHAR(64) NOT NULL COMMENT '报表批次标识符',
  tenant_id VARCHAR(64) NOT NULL COMMENT '所属租户标识符',
  logical_object_key VARCHAR(255) NOT NULL COMMENT '逻辑对象键',
  sql_count INT NOT NULL DEFAULT 0 COMMENT 'SQL 数',
  issue_count INT NOT NULL DEFAULT 0 COMMENT '问题次数',
  report_count INT NOT NULL DEFAULT 0 COMMENT '报表数',
  report_codes_json JSON DEFAULT NULL COMMENT '关联报表编码列表',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间戳',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间戳',
  PRIMARY KEY (batch_id, logical_object_key),
  KEY idx_report_batch_logical_object_batch (batch_id, sql_count),
  KEY idx_report_batch_logical_object_tenant (tenant_id, logical_object_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Report batch logical object statistics';
