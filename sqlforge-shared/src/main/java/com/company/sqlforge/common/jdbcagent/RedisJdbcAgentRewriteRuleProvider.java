package com.company.sqlforge.common.jdbcagent;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqlforge.common.rewrite.RuntimeSqlRewriteTemplateEngine;
import com.company.sqlforge.common.rewrite.RuntimeSqlRewriteTemplateResult;
import java.time.Instant;
import java.util.List;
import org.springframework.util.StringUtils;

public class RedisJdbcAgentRewriteRuleProvider implements JdbcAgentRewriteRuleProvider {

    private final RedisStringReader redisStringReader;

    public RedisJdbcAgentRewriteRuleProvider() {
        this(new LettuceRedisStringReader());
    }

    RedisJdbcAgentRewriteRuleProvider(RedisStringReader redisStringReader) {
        this.redisStringReader = redisStringReader;
    }

    @Override
    public JdbcAgentRewriteDecision resolve(JdbcAgentObservation observation, JdbcAgentProperties properties) {
        List<String> redisEndpoints = properties == null ? null : properties.getRedisEndpoints();
        if (redisEndpoints == null || redisEndpoints.isEmpty()) {
            return JdbcAgentRewriteDecision.passthrough("REDIS_ENDPOINTS_UNCONFIGURED");
        }
        String fingerprint = observation == null ? null : observation.getSqlFingerprint();
        if (!StringUtils.hasText(fingerprint)) {
            return JdbcAgentRewriteDecision.passthrough("SQL_FINGERPRINT_UNAVAILABLE");
        }
        String namespace = JdbcAgentRedisRuleKeys.namespace(properties.getRedisNamespace());
        String endpoint = redisEndpoints.get(0);
        if (properties.isTenantScopedRedisRules()) {
            String tenantId = observation == null ? null : observation.getTenantId();
            if (!StringUtils.hasText(tenantId)) {
                return legacyFallback(endpoint, namespace, fingerprint, properties, "TENANT_UNAVAILABLE");
            }
            return resolveTenantScopedRule(endpoint, namespace, tenantId.trim(), fingerprint, observation, properties);
        }
        return resolveLegacyRule(endpoint, namespace, fingerprint, properties, "REDIS_LEGACY_RULE_HIT");
    }

    private JdbcAgentRewriteDecision resolveTenantScopedRule(String endpoint,
                                                             String namespace,
                                                             String tenantId,
                                                             String fingerprint,
                                                             JdbcAgentObservation observation,
                                                             JdbcAgentProperties properties) {
        String metadataJson = redisStringReader.get(
            endpoint,
            JdbcAgentRedisRuleKeys.tenantMetadataKey(namespace, tenantId, fingerprint)
        );
        if (!StringUtils.hasText(metadataJson)) {
            return legacyFallback(endpoint, namespace, fingerprint, properties, "REDIS_METADATA_MISS");
        }
        JdbcAgentRedisRuleMetadata metadata;
        try {
            metadata = JsonUtils.fromJson(metadataJson, JdbcAgentRedisRuleMetadata.class);
        } catch (RuntimeException ex) {
            return legacyFallback(endpoint, namespace, fingerprint, properties, "REDIS_METADATA_INVALID");
        }
        Instant now = Instant.now();
        if (metadata.isExpiredAt(now)) {
            return JdbcAgentRewriteDecision.passthrough("REDIS_RULE_EXPIRED");
        }
        if (!metadata.isActiveAt(now)) {
            return JdbcAgentRewriteDecision.passthrough("REDIS_RULE_" + safeEvidenceStatus(metadata.getStatus()));
        }
        if (StringUtils.hasText(observation == null ? null : observation.getDatasourceCode())
            && StringUtils.hasText(metadata.getDatasourceCode())
            && !observation.getDatasourceCode().trim().equalsIgnoreCase(metadata.getDatasourceCode().trim())) {
            return JdbcAgentRewriteDecision.passthrough("REDIS_RULE_DATASOURCE_MISMATCH");
        }
        String rewriteSql = properties.isRewriteEnabled()
            ? redisStringReader.get(endpoint, JdbcAgentRedisRuleKeys.tenantRewriteKey(namespace, tenantId, fingerprint))
            : null;
        String routeHint = properties.isRouteEnabled()
            ? redisStringReader.get(endpoint, JdbcAgentRedisRuleKeys.tenantRouteKey(namespace, tenantId, fingerprint))
            : null;
        if (!StringUtils.hasText(rewriteSql) && !StringUtils.hasText(routeHint)) {
            return JdbcAgentRewriteDecision.passthrough("REDIS_RULE_MISS");
        }
        if (StringUtils.hasText(rewriteSql) && StringUtils.hasText(metadata.getOriginalSqlText())) {
            RuntimeSqlRewriteTemplateResult rewriteResult = RuntimeSqlRewriteTemplateEngine.rewrite(
                metadata.getOriginalSqlText(),
                rewriteSql,
                observation == null ? null : observation.getBoundSqlText()
            );
            if (!rewriteResult.isApplied()) {
                return JdbcAgentRewriteDecision.passthrough(
                    "REDIS_RULE_TEMPLATE_MISMATCH_" + safeEvidenceStatus(rewriteResult.getFailureReason())
                );
            }
            rewriteSql = rewriteResult.getRewrittenSql();
        }
        return new JdbcAgentRewriteDecision(
            StringUtils.hasText(rewriteSql),
            StringUtils.hasText(rewriteSql) ? rewriteSql : null,
            StringUtils.hasText(routeHint) ? routeHint : null,
            "REDIS_RULE_HIT"
        );
    }

    private JdbcAgentRewriteDecision legacyFallback(String endpoint,
                                                    String namespace,
                                                    String fingerprint,
                                                    JdbcAgentProperties properties,
                                                    String missEvidence) {
        if (!properties.isLegacyRedisRuleFallbackEnabled()) {
            return JdbcAgentRewriteDecision.passthrough(missEvidence);
        }
        return resolveLegacyRule(endpoint, namespace, fingerprint, properties, "REDIS_LEGACY_RULE_HIT");
    }

    private JdbcAgentRewriteDecision resolveLegacyRule(String endpoint,
                                                       String namespace,
                                                       String fingerprint,
                                                       JdbcAgentProperties properties,
                                                       String hitEvidence) {
        String rewriteSql = properties.isRewriteEnabled()
            ? redisStringReader.get(endpoint, JdbcAgentRedisRuleKeys.legacyRewriteKey(namespace, fingerprint))
            : null;
        String routeHint = properties.isRouteEnabled()
            ? redisStringReader.get(endpoint, JdbcAgentRedisRuleKeys.legacyRouteKey(namespace, fingerprint))
            : null;
        if (!StringUtils.hasText(rewriteSql) && !StringUtils.hasText(routeHint)) {
            return JdbcAgentRewriteDecision.passthrough("REDIS_RULE_MISS");
        }
        return new JdbcAgentRewriteDecision(
            StringUtils.hasText(rewriteSql),
            StringUtils.hasText(rewriteSql) ? rewriteSql : null,
            StringUtils.hasText(routeHint) ? routeHint : null,
            hitEvidence
        );
    }

    private String safeEvidenceStatus(String status) {
        return StringUtils.hasText(status) ? status.trim() : "UNAVAILABLE";
    }

    interface RedisStringReader {
        String get(String endpoint, String key);
    }

    static final class LettuceRedisStringReader implements RedisStringReader {
        @Override
        public String get(String endpoint, String key) {
            RedisClient redisClient = RedisClient.create(toRedisUri(endpoint));
            try {
                StatefulRedisConnection<String, String> connection = redisClient.connect();
                try {
                    RedisCommands<String, String> commands = connection.sync();
                    return commands.get(key);
                } finally {
                    connection.close();
                }
            } finally {
                redisClient.shutdown();
            }
        }

        private RedisURI toRedisUri(String endpoint) {
            if (!StringUtils.hasText(endpoint)) {
                throw new IllegalArgumentException("Redis 端点为空");
            }
            String normalized = endpoint.startsWith("redis://") ? endpoint : "redis://" + endpoint.trim();
            return RedisURI.create(normalized);
        }
    }
}
