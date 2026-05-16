package com.company.sqlforge.common.constants;

/**
 * 共享 Redis 键前缀。
 */
public final class CacheKeyConstants {

    public static final String TENANT_CONFIG_PREFIX = "sqlforge:tenant:config:";
    public static final String DATASOURCE_HEALTH_PREFIX = "sqlforge:datasource:health:";
    public static final String SQL_FINGERPRINT_PREFIX = "sqlforge:sql:fingerprint:";
    public static final String AUDIT_ARCHIVE_PREFIX = "sqlforge:audit:archive:";
    public static final String QUERY_RESULT_CACHE_POLICY_PREFIX = "sqlforge:query:cache:policy:";
    public static final String QUERY_RESULT_CACHE_ENTRY_PREFIX = "sqlforge:query:cache:entry:";

    private CacheKeyConstants() {
    }
}
