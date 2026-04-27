CREATE TABLE IF NOT EXISTS database_view_ref (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    datasource_code VARCHAR(128) NOT NULL,
    view_name VARCHAR(255) NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    schema_name VARCHAR(128) DEFAULT NULL,
    catalog_name VARCHAR(128) DEFAULT NULL,
    owner_user VARCHAR(128) DEFAULT NULL,
    queryable TINYINT(1) NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_database_view_ref_lookup (tenant_id, datasource_code, view_name),
    KEY idx_database_view_ref_key (tenant_id, object_key)
);

CREATE TABLE IF NOT EXISTS database_view_dependency (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    db_view_id VARCHAR(64) NOT NULL,
    dependency_object_type VARCHAR(32) NOT NULL,
    dependency_object_key VARCHAR(255) NOT NULL,
    dependency_object_name VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_database_view_dependency_view (tenant_id, db_view_id),
    KEY idx_database_view_dependency_key (tenant_id, dependency_object_key)
);
