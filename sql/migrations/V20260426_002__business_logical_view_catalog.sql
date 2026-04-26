CREATE TABLE IF NOT EXISTS business_logical_view (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    view_code VARCHAR(128) NOT NULL,
    view_name VARCHAR(255) NOT NULL,
    datasource_code VARCHAR(128) NOT NULL,
    subject_area VARCHAR(128) DEFAULT NULL,
    owner_user VARCHAR(128) DEFAULT NULL,
    freshness_status VARCHAR(32) DEFAULT NULL,
    sla_status VARCHAR(32) DEFAULT NULL,
    queryable TINYINT(1) NOT NULL DEFAULT 1,
    latest_refresh_time DATETIME DEFAULT NULL,
    description TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_business_logical_view_tenant_code (tenant_id, view_code),
    KEY idx_business_logical_view_tenant_datasource (tenant_id, datasource_code)
);

CREATE TABLE IF NOT EXISTS logical_object_mapping (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    logical_view_id VARCHAR(64) NOT NULL,
    target_object_type VARCHAR(32) NOT NULL,
    target_object_key VARCHAR(255) NOT NULL,
    target_object_name VARCHAR(255) NOT NULL,
    mapping_role VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_logical_object_mapping_view (tenant_id, logical_view_id),
    KEY idx_logical_object_mapping_target (tenant_id, target_object_key)
);
