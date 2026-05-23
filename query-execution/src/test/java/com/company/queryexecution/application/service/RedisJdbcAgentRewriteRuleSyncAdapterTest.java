package com.company.queryexecution.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.queryexecution.config.QueryExecutionJdbcAgentRedisProperties;
import com.company.queryexecution.domain.rewrite.RuntimeRewriteBinding;
import com.company.sqlforge.common.jdbcagent.JdbcAgentRedisRuleKeys;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RedisJdbcAgentRewriteRuleSyncAdapterTest {

    @Test
    void shouldWriteTenantScopedRewriteAndMetadataOnActivate() {
        QueryExecutionJdbcAgentRedisProperties properties = enabledProperties();
        RecordingRedisRewriteRuleClient client = new RecordingRedisRewriteRuleClient(properties);
        RedisJdbcAgentRewriteRuleSyncAdapter adapter = new RedisJdbcAgentRewriteRuleSyncAdapter(properties, client);

        JdbcAgentRewriteRuleSyncResult result = adapter.activate(binding().build());

        assertEquals("SYNCED", result.getSyncStatus());
        assertEquals("SELECT id FROM orders", client.values.get(
            JdbcAgentRedisRuleKeys.tenantRewriteKey("sqlforge:jdbc-agent", "tenant-a", "fp-001")
        ));
        assertTrue(client.values.get(
            JdbcAgentRedisRuleKeys.tenantMetadataKey("sqlforge:jdbc-agent", "tenant-a", "fp-001")
        ).contains("\"status\":\"ACTIVE\""));
        assertTrue(client.values.get(
            JdbcAgentRedisRuleKeys.tenantMetadataKey("sqlforge:jdbc-agent", "tenant-a", "fp-001")
        ).contains("\"originalSqlText\":\"SELECT * FROM orders WHERE tenant_id = 1\""));
    }

    @Test
    void shouldDeleteRewriteAndMarkMetadataUnavailableOnDisable() {
        QueryExecutionJdbcAgentRedisProperties properties = enabledProperties();
        RecordingRedisRewriteRuleClient client = new RecordingRedisRewriteRuleClient(properties);
        RedisJdbcAgentRewriteRuleSyncAdapter adapter = new RedisJdbcAgentRewriteRuleSyncAdapter(properties, client);
        RuntimeRewriteBinding paused = binding()
            .status(com.company.queryexecution.domain.rewrite.RuntimeRewriteBindingStatus.PAUSED)
            .pausedBy("user-001")
            .pausedAt(Instant.now())
            .build();

        JdbcAgentRewriteRuleSyncResult result = adapter.disable(paused);

        assertEquals("SYNCED", result.getSyncStatus());
        assertTrue(client.deletedKeys.contains(
            JdbcAgentRedisRuleKeys.tenantRewriteKey("sqlforge:jdbc-agent", "tenant-a", "fp-001")
        ));
        assertTrue(client.values.get(
            JdbcAgentRedisRuleKeys.tenantMetadataKey("sqlforge:jdbc-agent", "tenant-a", "fp-001")
        ).contains("\"status\":\"PAUSED\""));
    }

    @Test
    void shouldReturnFailedResultWhenRedisWriteFails() {
        QueryExecutionJdbcAgentRedisProperties properties = enabledProperties();
        RedisRewriteRuleClient client = new RedisRewriteRuleClient(properties) {
            @Override
            void set(String key, String value, long ttlSeconds) {
                throw new IllegalStateException("redis 不可用");
            }
        };
        RedisJdbcAgentRewriteRuleSyncAdapter adapter = new RedisJdbcAgentRewriteRuleSyncAdapter(properties, client);

        JdbcAgentRewriteRuleSyncResult result = adapter.activate(binding().build());

        assertEquals("FAILED", result.getSyncStatus());
        assertTrue(result.isAlertRequired());
        assertTrue(result.isRetryable());
    }

    @Test
    void shouldSkipWhenDisabled() {
        QueryExecutionJdbcAgentRedisProperties properties = new QueryExecutionJdbcAgentRedisProperties();
        RedisJdbcAgentRewriteRuleSyncAdapter adapter = new RedisJdbcAgentRewriteRuleSyncAdapter(
            properties,
            new RecordingRedisRewriteRuleClient(properties)
        );

        JdbcAgentRewriteRuleSyncResult result = adapter.activate(binding().build());

        assertEquals("SKIPPED", result.getSyncStatus());
    }

    private QueryExecutionJdbcAgentRedisProperties enabledProperties() {
        QueryExecutionJdbcAgentRedisProperties properties = new QueryExecutionJdbcAgentRedisProperties();
        properties.setEnabled(true);
        properties.setNamespace("sqlforge:jdbc-agent");
        properties.setHost("redis.test");
        properties.setTtlSeconds(60L);
        return properties;
    }

    private RuntimeRewriteBinding.Builder binding() {
        return RuntimeRewriteBinding.builder()
            .runtimeBindingId("rwb-001")
            .tenantId("tenant-a")
            .rewriteRecordId("rewrite-001")
            .recommendationId("recommendation-001")
            .sourceType("QUERY")
            .sourceKind("QUERY_HISTORY")
            .sourceId("history-001")
            .sqlFingerprint("fp-001")
            .originalSqlDigest("digest-original-001")
            .originalSqlText("SELECT * FROM orders WHERE tenant_id = 1")
            .recommendedSqlText("SELECT id FROM orders")
            .rewriteProgramJson("{\"programVersion\":\"template-replay-v1\"}")
            .templateFamilyFingerprint("family-001")
            .datasourceCode("hetu_main")
            .ruleVersion(1L)
            .runtimeRuleVersion("runtime-rewrite-v1")
            .activatedBy("publisher-001")
            .activatedAt(Instant.now())
            .createdAt(Instant.now())
            .updatedAt(Instant.now());
    }

    private static final class RecordingRedisRewriteRuleClient extends RedisRewriteRuleClient {
        private final Map<String, String> values = new LinkedHashMap<String, String>();
        private final List<String> deletedKeys = new ArrayList<String>();

        RecordingRedisRewriteRuleClient(QueryExecutionJdbcAgentRedisProperties properties) {
            super(properties);
        }

        @Override
        void set(String key, String value, long ttlSeconds) {
            values.put(key, value);
        }

        @Override
        void delete(String key) {
            deletedKeys.add(key);
        }
    }
}
