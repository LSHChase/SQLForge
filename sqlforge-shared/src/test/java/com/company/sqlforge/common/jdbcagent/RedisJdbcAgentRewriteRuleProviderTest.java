package com.company.sqlforge.common.jdbcagent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.utils.JsonUtils;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RedisJdbcAgentRewriteRuleProviderTest {

    @Test
    void shouldResolveTenantScopedActiveRule() {
        MapRedisStringReader reader = new MapRedisStringReader();
        JdbcAgentProperties properties = properties();
        reader.values.put(
            JdbcAgentRedisRuleKeys.tenantMetadataKey(properties.getRedisNamespace(), "tenant-a", "fp-001"),
            metadataJson("tenant-a", "fp-001", "ACTIVE", null, "hetu_main")
        );
        reader.values.put(
            JdbcAgentRedisRuleKeys.tenantRewriteKey(properties.getRedisNamespace(), "tenant-a", "fp-001"),
            "SELECT id FROM orders"
        );

        JdbcAgentRewriteDecision decision =
            new RedisJdbcAgentRewriteRuleProvider(reader).resolve(observation("tenant-a", "hetu_main"), properties);

        assertTrue(decision.isApplied());
        assertEquals("SELECT id FROM orders", decision.getRewrittenSql());
        assertEquals("REDIS_RULE_HIT", decision.getEvidence());
    }

    @Test
    void shouldReplayCurrentPredicatesFromTenantScopedTemplateRule() {
        MapRedisStringReader reader = new MapRedisStringReader();
        JdbcAgentProperties properties = properties();
        JdbcAgentRedisRuleMetadata metadata = metadata("tenant-a", "fp-001", "ACTIVE", null, "hetu_main");
        metadata.setOriginalSqlText("SELECT * FROM orders WHERE tenant_id = 1 AND status = 'PAID'");
        reader.values.put(
            JdbcAgentRedisRuleKeys.tenantMetadataKey(properties.getRedisNamespace(), "tenant-a", "fp-001"),
            JsonUtils.toJson(metadata)
        );
        reader.values.put(
            JdbcAgentRedisRuleKeys.tenantRewriteKey(properties.getRedisNamespace(), "tenant-a", "fp-001"),
            "SELECT id FROM orders WHERE tenant_id = 1 AND status = 'PAID'"
        );

        JdbcAgentRewriteDecision decision =
            new RedisJdbcAgentRewriteRuleProvider(reader).resolve(
                observation(
                    "tenant-a",
                    "hetu_main",
                    "SELECT * FROM orders WHERE tenant_id = 8 AND status = 'CANCELLED' AND dt = '2026-05-22'"
                ),
                properties
            );

        assertTrue(decision.isApplied());
        assertEquals(
            "SELECT id FROM orders WHERE tenant_id = 8 AND status = 'CANCELLED' AND dt = '2026-05-22'",
            decision.getRewrittenSql()
        );
    }


    @Test
    void shouldNotFallbackAcrossTenantsByDefault() {
        MapRedisStringReader reader = new MapRedisStringReader();
        JdbcAgentProperties properties = properties();
        reader.values.put(
            JdbcAgentRedisRuleKeys.tenantMetadataKey(properties.getRedisNamespace(), "tenant-b", "fp-001"),
            metadataJson("tenant-b", "fp-001", "ACTIVE", null, "hetu_main")
        );
        reader.values.put(
            JdbcAgentRedisRuleKeys.tenantRewriteKey(properties.getRedisNamespace(), "tenant-b", "fp-001"),
            "SELECT id FROM other_tenant_orders"
        );
        reader.values.put(
            JdbcAgentRedisRuleKeys.legacyRewriteKey(properties.getRedisNamespace(), "fp-001"),
            "SELECT id FROM legacy_orders"
        );

        JdbcAgentRewriteDecision decision =
            new RedisJdbcAgentRewriteRuleProvider(reader).resolve(observation("tenant-a", "hetu_main"), properties);

        assertFalse(decision.isApplied());
        assertEquals("REDIS_METADATA_MISS", decision.getEvidence());
    }

    @Test
    void shouldIgnorePausedExpiredAndDatasourceMismatchedRules() {
        JdbcAgentProperties properties = properties();

        assertRuleBypassed(properties, metadataJson("tenant-a", "fp-001", "PAUSED", null, "hetu_main"), "REDIS_RULE_PAUSED");
        assertRuleBypassed(
            properties,
            metadataJson("tenant-a", "fp-001", "ACTIVE", Instant.now().minusSeconds(10L).toString(), "hetu_main"),
            "REDIS_RULE_EXPIRED"
        );
        assertRuleBypassed(
            properties,
            metadataJson("tenant-a", "fp-001", "ACTIVE", null, "hetu_archive"),
            "REDIS_RULE_DATASOURCE_MISMATCH"
        );
    }

    @Test
    void shouldUseLegacyFallbackOnlyWhenExplicitlyEnabled() {
        MapRedisStringReader reader = new MapRedisStringReader();
        JdbcAgentProperties properties = properties();
        reader.values.put(
            JdbcAgentRedisRuleKeys.legacyRewriteKey(properties.getRedisNamespace(), "fp-001"),
            "SELECT id FROM legacy_orders"
        );

        JdbcAgentRewriteDecision defaultDecision =
            new RedisJdbcAgentRewriteRuleProvider(reader).resolve(observation("tenant-a", "hetu_main"), properties);
        assertFalse(defaultDecision.isApplied());

        properties.setLegacyRedisRuleFallbackEnabled(true);
        JdbcAgentRewriteDecision fallbackDecision =
            new RedisJdbcAgentRewriteRuleProvider(reader).resolve(observation("tenant-a", "hetu_main"), properties);
        assertTrue(fallbackDecision.isApplied());
        assertEquals("REDIS_LEGACY_RULE_HIT", fallbackDecision.getEvidence());
    }

    private void assertRuleBypassed(JdbcAgentProperties properties, String metadataJson, String evidence) {
        MapRedisStringReader reader = new MapRedisStringReader();
        reader.values.put(
            JdbcAgentRedisRuleKeys.tenantMetadataKey(properties.getRedisNamespace(), "tenant-a", "fp-001"),
            metadataJson
        );
        reader.values.put(
            JdbcAgentRedisRuleKeys.tenantRewriteKey(properties.getRedisNamespace(), "tenant-a", "fp-001"),
            "SELECT id FROM orders"
        );

        JdbcAgentRewriteDecision decision =
            new RedisJdbcAgentRewriteRuleProvider(reader).resolve(observation("tenant-a", "hetu_main"), properties);

        assertFalse(decision.isApplied());
        assertEquals(evidence, decision.getEvidence());
    }

    private JdbcAgentProperties properties() {
        JdbcAgentProperties properties = new JdbcAgentProperties();
        properties.setRedisEndpoints(Collections.singletonList("redis.test:6379"));
        properties.setRewriteEnabled(true);
        properties.setRedisNamespace("sqlforge:jdbc-agent");
        return properties;
    }

    private JdbcAgentObservation observation(String tenantId, String datasourceCode) {
        return observation(tenantId, datasourceCode, "SELECT * FROM orders");
    }

    private JdbcAgentObservation observation(String tenantId, String datasourceCode, String sqlText) {
        return new JdbcAgentObservation(
            sqlText,
            sqlText,
            sqlText,
            "fp-001",
            tenantId,
            datasourceCode,
            Collections.<String, String>emptyMap(),
            new JdbcAgentQueryDateSummary(),
            false,
            0
        );
    }

    private String metadataJson(String tenantId,
                                String sqlFingerprint,
                                String status,
                                String expiresAt,
                                String datasourceCode) {
        return JsonUtils.toJson(metadata(tenantId, sqlFingerprint, status, expiresAt, datasourceCode));
    }

    private JdbcAgentRedisRuleMetadata metadata(String tenantId,
                                                String sqlFingerprint,
                                                String status,
                                                String expiresAt,
                                                String datasourceCode) {
        JdbcAgentRedisRuleMetadata metadata = new JdbcAgentRedisRuleMetadata();
        metadata.setTenantId(tenantId);
        metadata.setSqlFingerprint(sqlFingerprint);
        metadata.setRuntimeBindingId("rwb-001");
        metadata.setDatasourceCode(datasourceCode);
        metadata.setStatus(status);
        metadata.setRuleVersion(Long.valueOf(1));
        metadata.setRuntimeRuleVersion("runtime-rewrite-v1");
        metadata.setUpdatedAt(Instant.now().toString());
        metadata.setExpiresAt(expiresAt);
        metadata.setSyncStatus("SYNCED");
        return metadata;
    }

    private static final class MapRedisStringReader implements RedisJdbcAgentRewriteRuleProvider.RedisStringReader {
        private final Map<String, String> values = new HashMap<String, String>();

        @Override
        public String get(String endpoint, String key) {
            return values.get(key);
        }
    }
}
