-- Issue-driven SQL rewrite trial persistence.
-- These objects hold trial/recommendation evidence only; they do not represent production rewrite execution.

ALTER TABLE acceleration_recommendation
  ADD COLUMN source_problems_json JSON DEFAULT NULL COMMENT '改写试算来源问题证据，不代表生产已改写',
  ADD COLUMN issue_rule_links_json JSON DEFAULT NULL COMMENT '来源问题到改写规则的试算链路证据';

CREATE TABLE IF NOT EXISTS rewrite_trial_run (
  run_id VARCHAR(64) NOT NULL COMMENT '改写试算 run 标识符',
  tenant_id VARCHAR(64) NOT NULL COMMENT '所属租户标识符',
  source_kind VARCHAR(64) DEFAULT NULL COMMENT '试算来源类型，例如 STRUCTURE_PARSE/PARSE_BATCH/QUERY_HISTORY/MANUAL',
  source_id VARCHAR(128) DEFAULT NULL COMMENT '来源对象标识符',
  batch_id VARCHAR(64) DEFAULT NULL COMMENT '批量解析来源批次标识符',
  status VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUESTED' COMMENT '试算状态，不代表生产改写事实',
  total_count INT NOT NULL DEFAULT 0 COMMENT '本次试算总条目数',
  accepted_count INT NOT NULL DEFAULT 0 COMMENT '已接受进入试算的条目数',
  skipped_count INT NOT NULL DEFAULT 0 COMMENT '因无来源问题或解析无效而跳过的条目数',
  created_by VARCHAR(64) DEFAULT NULL COMMENT '发起人',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间戳',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间戳',
  PRIMARY KEY (run_id),
  KEY idx_rewrite_trial_run_tenant_created (tenant_id, created_at),
  KEY idx_rewrite_trial_run_batch_created (tenant_id, batch_id, created_at),
  KEY idx_rewrite_trial_run_source (tenant_id, source_kind, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Issue-driven SQL rewrite trial run, not production rewrite history';

CREATE TABLE IF NOT EXISTS rewrite_trial_item (
  trial_item_id VARCHAR(64) NOT NULL COMMENT '改写试算条目标识符',
  run_id VARCHAR(64) NOT NULL COMMENT '所属 rewrite_trial_run 标识符',
  batch_item_id VARCHAR(64) DEFAULT NULL COMMENT '关联解析批次条目标识符',
  parse_task_id VARCHAR(64) DEFAULT NULL COMMENT '关联结构解析任务标识符',
  parse_history_id VARCHAR(128) DEFAULT NULL COMMENT '关联解析历史标识符',
  history_id VARCHAR(128) DEFAULT NULL COMMENT '关联 SQL 历史或解析历史标识符',
  sql_fingerprint VARCHAR(128) DEFAULT NULL COMMENT '归一化 SQL 指纹',
  datasource_code VARCHAR(128) DEFAULT NULL COMMENT '关联数据源编码',
  source_sql_text MEDIUMTEXT DEFAULT NULL COMMENT '试算来源 SQL 文本',
  source_problems_json JSON DEFAULT NULL COMMENT '来源问题证据，含 issueScene/severity/evidenceRef',
  task_id VARCHAR(64) DEFAULT NULL COMMENT '关联优化任务标识符',
  recommendation_id VARCHAR(64) DEFAULT NULL COMMENT '关联推荐标识符',
  rewrite_record_id VARCHAR(64) DEFAULT NULL COMMENT '用户采纳后关联改写记录标识符',
  trial_status VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUESTED' COMMENT '试算状态，不得混同 rewriteApplied',
  failure_reason VARCHAR(512) DEFAULT NULL COMMENT '失败或跳过原因',
  candidate_sql MEDIUMTEXT DEFAULT NULL COMMENT '试算生成的候选 SQL，未发布',
  validation_status VARCHAR(32) DEFAULT NULL COMMENT '关联验证状态',
  issue_rule_links_json JSON DEFAULT NULL COMMENT '来源问题到改写规则链路证据',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间戳',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间戳',
  PRIMARY KEY (trial_item_id),
  KEY idx_rewrite_trial_item_run (run_id),
  KEY idx_rewrite_trial_item_batch_item (batch_item_id),
  KEY idx_rewrite_trial_item_parse_task (parse_task_id),
  KEY idx_rewrite_trial_item_history (history_id),
  KEY idx_rewrite_trial_item_recommendation (recommendation_id),
  KEY idx_rewrite_trial_item_fingerprint (sql_fingerprint),
  KEY idx_rewrite_trial_item_status (trial_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Issue-driven SQL rewrite trial item, candidate only';
