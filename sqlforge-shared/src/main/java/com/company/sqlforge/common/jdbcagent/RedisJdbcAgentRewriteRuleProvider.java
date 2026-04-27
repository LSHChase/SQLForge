package com.company.sqlforge.common.jdbcagent;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.util.List;
import org.springframework.util.StringUtils;

public class RedisJdbcAgentRewriteRuleProvider implements JdbcAgentRewriteRuleProvider {

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
        RedisClient redisClient = RedisClient.create(toRedisUri(redisEndpoints.get(0)));
        try {
            StatefulRedisConnection<String, String> connection = redisClient.connect();
            try {
                RedisCommands<String, String> commands = connection.sync();
                String namespace = StringUtils.hasText(properties.getRedisNamespace())
                    ? properties.getRedisNamespace().trim()
                    : "sqlforge:jdbc-agent";
                String rewriteSql = properties.isRewriteEnabled()
                    ? commands.get(namespace + ":rewrite:" + fingerprint)
                    : null;
                String routeHint = properties.isRouteEnabled()
                    ? commands.get(namespace + ":route:" + fingerprint)
                    : null;
                if (!StringUtils.hasText(rewriteSql) && !StringUtils.hasText(routeHint)) {
                    return JdbcAgentRewriteDecision.passthrough("REDIS_RULE_MISS");
                }
                return new JdbcAgentRewriteDecision(
                    StringUtils.hasText(rewriteSql),
                    StringUtils.hasText(rewriteSql) ? rewriteSql : null,
                    StringUtils.hasText(routeHint) ? routeHint : null,
                    "REDIS_RULE_HIT"
                );
            } finally {
                connection.close();
            }
        } finally {
            redisClient.shutdown();
        }
    }

    private RedisURI toRedisUri(String endpoint) {
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalArgumentException("Redis endpoint is blank");
        }
        String normalized = endpoint.startsWith("redis://") ? endpoint : "redis://" + endpoint.trim();
        return RedisURI.create(normalized);
    }
}
