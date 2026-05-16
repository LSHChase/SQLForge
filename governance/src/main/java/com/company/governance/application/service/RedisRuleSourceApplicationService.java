package com.company.governance.application.service;

import com.company.governance.application.controller.dto.RedisRuleSourceUpsertRequest;
import com.company.governance.application.controller.vo.RedisRuleSourceVO;
import com.company.governance.domain.redissource.RedisRuleSourceConfig;
import com.company.governance.domain.redissource.repository.RedisRuleSourceRepository;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RedisRuleSourceApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "REDIS_RULE_SOURCE_BASELINE";

    private final RedisRuleSourceRepository redisRuleSourceRepository;

    public RedisRuleSourceApplicationService(RedisRuleSourceRepository redisRuleSourceRepository) {
        this.redisRuleSourceRepository = redisRuleSourceRepository;
    }

    public RedisRuleSourceVO create(RedisRuleSourceUpsertRequest request) {
        return save(null, request);
    }

    public RedisRuleSourceVO update(String sourceId, RedisRuleSourceUpsertRequest request) {
        return save(requireText(sourceId, "sourceId"), request);
    }

    public List<RedisRuleSourceVO> list(String tenantId) {
        String effectiveTenantId = requireTenant(tenantId);
        List<RedisRuleSourceConfig> configs = redisRuleSourceRepository.findByTenantId(effectiveTenantId);
        List<RedisRuleSourceVO> responses = new ArrayList<RedisRuleSourceVO>(configs.size());
        for (RedisRuleSourceConfig config : configs) {
            responses.add(toVo(config));
        }
        return responses;
    }

    private RedisRuleSourceVO save(String sourceId, RedisRuleSourceUpsertRequest request) {
        String tenantId = requireTenant(request == null ? null : request.getTenantId());
        String effectiveSourceId = sourceId;
        if (StringUtils.hasText(sourceId)) {
            redisRuleSourceRepository.findByTenantIdAndSourceId(tenantId, sourceId).orElseThrow(() -> new BizException(
                ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Redis 规则来源不存在：" + sourceId
            ));
        } else {
            effectiveSourceId = UUID.randomUUID().toString();
        }
        RedisRuleSourceConfig config = new RedisRuleSourceConfig(
            effectiveSourceId,
            tenantId,
            firstNonBlank(request == null ? null : request.getSourceName(), "redis-rule-source"),
            trimToNull(request == null ? null : request.getRedisEndpoints()),
            firstNonBlank(request == null ? null : request.getRedisNamespace(), "sqlforge:rules"),
            firstNonBlank(request == null ? null : request.getKeyPattern(), "jdbc-agent:*"),
            firstNonBlank(request == null ? null : request.getAuthMode(), "NONE").toUpperCase(Locale.ROOT),
            trimToNull(request == null ? null : request.getCredentialRef()),
            request == null || request.getBypassOnUnavailable() == null || request.getBypassOnUnavailable().booleanValue(),
            request == null || request.getEnabled() == null || request.getEnabled().booleanValue(),
            Instant.now()
        );
        redisRuleSourceRepository.save(config);
        return toVo(config);
    }

    private RedisRuleSourceVO toVo(RedisRuleSourceConfig config) {
        RedisRuleSourceVO response = new RedisRuleSourceVO();
        response.setSourceId(config.getSourceId());
        response.setTenantId(config.getTenantId());
        response.setSourceName(config.getSourceName());
        response.setRedisEndpoints(config.getRedisEndpoints());
        response.setRedisNamespace(config.getRedisNamespace());
        response.setKeyPattern(config.getKeyPattern());
        response.setAuthMode(config.getAuthMode());
        response.setCredentialRef(config.getCredentialRef());
        response.setBypassOnUnavailable(config.isBypassOnUnavailable());
        response.setEnabled(config.isEnabled());
        response.setActivationMode("CONFIG_ONLY");
        response.setUpdatedAt(config.getUpdatedAt());
        if (!config.isEnabled()) {
            response.setHealthStatus("DISABLED");
            response.setUnavailableReason("RULE_SOURCE_DISABLED");
        } else if (!StringUtils.hasText(config.getRedisEndpoints())) {
            response.setHealthStatus("DEGRADED");
            response.setUnavailableReason("REDIS_ENDPOINTS_MISSING");
        } else {
            response.setHealthStatus("SIMULATED_READY");
        }
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(IMPLEMENTATION_STAGE);
        return response;
    }

    private String requireTenant(String requestTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(contextTenantId)) {
            throw new BizException(ErrorCodeConstants.SYSTEM_CONTEXT_MISSING, HttpStatus.UNAUTHORIZED,
                "已认证请求上下文缺少 tenantId");
        }
        String normalized = trimToNull(requestTenantId);
        if (StringUtils.hasText(normalized) && !contextTenantId.equals(normalized)) {
            throw new AccessDeniedException("请求 tenantId 与已认证租户上下文不一致");
        }
        return contextTenantId;
    }

    private String requireText(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, HttpStatus.BAD_REQUEST,
                fieldName + " 为必填项");
        }
        return normalized;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (StringUtils.hasText(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
