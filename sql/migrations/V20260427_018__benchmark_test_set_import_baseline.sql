CREATE TABLE IF NOT EXISTS benchmark_test_set (
  test_set_id VARCHAR(64) NOT NULL PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  test_set_name VARCHAR(128) NOT NULL,
  template_id VARCHAR(64) DEFAULT NULL,
  template_type VARCHAR(32) DEFAULT NULL,
  template_version VARCHAR(32) DEFAULT NULL,
  test_set_source VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  total_cases INT NOT NULL DEFAULT 0,
  accepted_cases INT NOT NULL DEFAULT 0,
  rejected_cases INT NOT NULL DEFAULT 0,
  file_type VARCHAR(16) DEFAULT NULL,
  file_name VARCHAR(255) DEFAULT NULL,
  import_batch_id VARCHAR(64) DEFAULT NULL,
  field_mappings_json JSON DEFAULT NULL,
  test_set_labels_json JSON DEFAULT NULL,
  test_set_source_refs_json JSON DEFAULT NULL,
  created_by VARCHAR(64) DEFAULT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='Benchmark test-set import and generation catalog';

CREATE TABLE IF NOT EXISTS benchmark_test_set_case (
  case_id VARCHAR(64) NOT NULL PRIMARY KEY,
  test_set_id VARCHAR(64) NOT NULL,
  sequence_number INT NOT NULL,
  source_line_number INT NOT NULL,
  case_name VARCHAR(255) DEFAULT NULL,
  sql_text LONGTEXT DEFAULT NULL,
  sql_fingerprint VARCHAR(128) DEFAULT NULL,
  datasource_code VARCHAR(64) DEFAULT NULL,
  report_code VARCHAR(64) DEFAULT NULL,
  tags_json JSON DEFAULT NULL,
  bind_parameters_json JSON DEFAULT NULL,
  status VARCHAR(32) NOT NULL,
  rejection_reason VARCHAR(255) DEFAULT NULL,
  raw_case_data_json JSON DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_benchmark_test_set_case_set
    FOREIGN KEY (test_set_id) REFERENCES benchmark_test_set (test_set_id)
    ON DELETE CASCADE
) COMMENT='Benchmark test-set case members and rejected-row evidence';

CREATE INDEX idx_benchmark_test_set_tenant_source ON benchmark_test_set (tenant_id, test_set_source);
CREATE INDEX idx_benchmark_test_set_import_batch ON benchmark_test_set (import_batch_id);
CREATE INDEX idx_benchmark_test_set_case_set_seq ON benchmark_test_set_case (test_set_id, sequence_number);
