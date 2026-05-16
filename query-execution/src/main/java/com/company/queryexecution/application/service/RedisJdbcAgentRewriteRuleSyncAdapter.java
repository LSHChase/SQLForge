package com.company.queryexecution.application.service;

import com.company.queryexecution.config.QueryExecutionJdbcAgentRedisProperties;
import com.company.queryexecution.domain.rewrite.RuntimeRewriteBinding;
import com.company.sqlforge.common.jdbcagent.JdbcAgentRedisRuleKeys;
import com.company.sqlforge.common.jdbcagent.JdbcAgentRedisRuleMetadata;
import com.company.sqlforge.common.utils.JsonUtils;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RedisJdbcAgentRewriteRuleSyncAdapter implements JdbcAgentRewriteRuleSyncPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisJdbcAgentRewriteRuleSyncAdapter.class);

    private final QueryExecutionJdbcAgentRedisProperties properties;
    private final RedisRewriteRuleClient redisRewriteRuleClient;

    @Autowired
    public RedisJdbcAgentRewriteRuleSyncAdapter(QueryExecutionJdbcAgentRedisProperties properties) {
        this(properties, new RedisRewriteRuleClient(properties));
    }

    RedisJdbcAgentRewriteRuleSyncAdapter(QueryExecutionJdbcAgentRedisProperties properties,
                                         RedisRewriteRuleClient redisRewriteRuleClient) {
        this.properties = properties;
        this.redisRewriteRuleClient = redisRewriteRuleClient;
    }

    @Override
    public JdbcAgentRewriteRuleSyncResult publish(RuntimeRewriteBinding binding) {
        if (!isEnabled()) {
            return JdbcAgentRewriteRuleSyncResult.skipped("PUBLISH", "JDBC Agent Redis 同步已禁用");
        }
        String rewriteKey = rewriteKey(binding);
        String metadataKey = metadataKey(binding);
        try {
            redisRewriteRuleClient.set(rewriteKey, binding.getRecommendedSqlText(), properties.getTtlSeconds());
            redisRewriteRuleClient.set(metadataKey, metadataJson(binding, "ACTIVE"), properties.getTtlSeconds());
            return JdbcAgentRewriteRuleSyncResult.builder()
                .syncStatus("SYNCED")
                .syncAction("PUBLISH")
                .redisRewriteKey(rewriteKey)
                .redisMetadataKey(metadataKey)
                .retryable(false)
                .alertRequired(false)
                .build();
        } catch (RuntimeException ex) {
            return failed("PUBLISH", rewriteKey, metadataKey, ex);
        }
    }

    @Override
    public JdbcAgentRewriteRuleSyncResult disable(RuntimeRewriteBinding binding) {
        if (!isEnabled()) {
            return JdbcAgentRewriteRuleSyncResult.skipped("DISABLE", "JDBC Agent Redis 同步已禁用");
        }
        String rewriteKey = rewriteKey(binding);
        String metadataKey = metadataKey(binding);
        try {
            redisRewriteRuleClient.delete(rewriteKey);
            redisRewriteRuleClient.delete(JdbcAgentRedisRuleKeys.tenantRouteKey(
                properties.getNamespace(),
                binding.getTenantId(),
                binding.getSqlFingerprint()
            ));
            redisRewriteRuleClient.set(metadataKey, metadataJson(binding, binding.getStatus().name()), properties.getTtlSeconds());
            return JdbcAgentRewriteRuleSyncResult.builder()
                .syncStatus("SYNCED")
                .syncAction("DISABLE")
                .redisRewriteKey(rewriteKey)
                .redisMetadataKey(metadataKey)
                .retryable(false)
                .alertRequired(false)
                .build();
        } catch (RuntimeException ex) {
            return failed("DISABLE", rewriteKey, metadataKey, ex);
        }
    }

    private boolean isEnabled() {
        return properties != null && properties.isEnabled() && StringUtils.hasText(properties.getHost());
    }

    private JdbcAgentRewriteRuleSyncResult failed(String action, String rewriteKey, String metadataKey, RuntimeException ex) {
        String reason = sanitize(ex.getMessage());
        LOGGER.warn(
            "JDBC Agent Redis 改写规则同步失败，action={}, rewriteKey={}, metadataKey={}, reason={}",
            action,
            rewriteKey,
            metadataKey,
            reason
        );
        return JdbcAgentRewriteRuleSyncResult.builder()
            .syncStatus("FAILED")
            .syncAction(action)
            .redisRewriteKey(rewriteKey)
            .redisMetadataKey(metadataKey)
            .failureReason(reason)
            .retryable(true)
            .alertRequired(true)
            .build();
    }

    private String rewriteKey(RuntimeRewriteBinding binding) {
        return JdbcAgentRedisRuleKeys.tenantRewriteKey(
            properties.getNamespace(),
            binding.getTenantId(),
            binding.getSqlFingerprint()
        );
    }

    private String metadataKey(RuntimeRewriteBinding binding) {
        return JdbcAgentRedisRuleKeys.tenantMetadataKey(
            properties.getNamespace(),
            binding.getTenantId(),
            binding.getSqlFingerprint()
        );
    }

    private String metadataJson(RuntimeRewriteBinding binding, String status) {
        JdbcAgentRedisRuleMetadata metadata = new JdbcAgentRedisRuleMetadata();
        metadata.setTenantId(binding.getTenantId());
        metadata.setSqlFingerprint(binding.getSqlFingerprint());
        metadata.setRuntimeBindingId(binding.getRuntimeBindingId());
        metadata.setDatasourceCode(binding.getDatasourceCode());
        metadata.setStatus(status);
        metadata.setRuleVersion(Long.valueOf(binding.getRuleVersion()));
        metadata.setRuntimeRuleVersion(binding.getRuntimeRuleVersion());
        metadata.setUpdatedAt(Instant.now().toString());
        metadata.setSyncStatus("SYNCED");
        if (properties.getTtlSeconds() > 0L) {
            metadata.setExpiresAt(Instant.now().plusSeconds(properties.getTtlSeconds()).toString());
        }
        return JsonUtils.toJson(metadata);
    }

    private String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return "Redis 同步后端不可用";
        }
        return value.replace(';', ',').replace('\n', ' ').replace('\r', ' ');
    }
}
