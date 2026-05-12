package com.company.sqlforge.common.jdbcagent;

import org.springframework.util.StringUtils;

public final class JdbcAgentRedisRuleKeys {

    public static final String DEFAULT_NAMESPACE = "sqlforge:jdbc-agent";

    private JdbcAgentRedisRuleKeys() {
    }

    public static String namespace(String namespace) {
        return StringUtils.hasText(namespace) ? namespace.trim() : DEFAULT_NAMESPACE;
    }

    public static String tenantRewriteKey(String namespace, String tenantId, String sqlFingerprint) {
        return tenantScopedPrefix(namespace, tenantId) + ":rewrite:" + requireText(sqlFingerprint, "sqlFingerprint");
    }

    public static String tenantRouteKey(String namespace, String tenantId, String sqlFingerprint) {
        return tenantScopedPrefix(namespace, tenantId) + ":route:" + requireText(sqlFingerprint, "sqlFingerprint");
    }

    public static String tenantMetadataKey(String namespace, String tenantId, String sqlFingerprint) {
        return tenantScopedPrefix(namespace, tenantId) + ":meta:" + requireText(sqlFingerprint, "sqlFingerprint");
    }

    public static String legacyRewriteKey(String namespace, String sqlFingerprint) {
        return namespace(namespace) + ":rewrite:" + requireText(sqlFingerprint, "sqlFingerprint");
    }

    public static String legacyRouteKey(String namespace, String sqlFingerprint) {
        return namespace(namespace) + ":route:" + requireText(sqlFingerprint, "sqlFingerprint");
    }

    private static String tenantScopedPrefix(String namespace, String tenantId) {
        return namespace(namespace) + ":tenant:" + requireText(tenantId, "tenantId");
    }

    private static String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        return value.trim();
    }
}
