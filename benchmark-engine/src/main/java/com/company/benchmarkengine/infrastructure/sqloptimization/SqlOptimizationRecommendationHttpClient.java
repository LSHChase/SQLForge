package com.company.benchmarkengine.infrastructure.sqloptimization;

import com.company.benchmarkengine.config.BenchmarkEngineSqlOptimizationProperties;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.ProtectedGovernanceRequestSupport;
import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class SqlOptimizationRecommendationHttpClient implements SqlOptimizationRecommendationClient {

    private static final String SQL_OPTIMIZATION_ROUTE_UNAVAILABLE_MESSAGE = "SQL-optimization recommendation route is unavailable";

    private final RestTemplate restTemplate;
    private final BenchmarkEngineSqlOptimizationProperties properties;

    public SqlOptimizationRecommendationHttpClient(RestTemplateBuilder restTemplateBuilder,
                                                   BenchmarkEngineSqlOptimizationProperties properties) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
            .build();
        this.properties = properties;
    }

    @Override
    public SqlOptimizationAccelerationRecommendation getRecommendation(String recommendationId) {
        try {
            ResponseEntity<SqlOptimizationAccelerationRecommendation> response = restTemplate.exchange(
                normalizeBaseUrl() + "/recommendations/" + recommendationId,
                HttpMethod.GET,
                new HttpEntity<Object>(buildProtectedHeaders()),
                SqlOptimizationAccelerationRecommendation.class
            );
            return response.getBody();
        } catch (RestClientException ex) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID,
                HttpStatus.SERVICE_UNAVAILABLE,
                SQL_OPTIMIZATION_ROUTE_UNAVAILABLE_MESSAGE,
                ex
            );
        }
    }

    private HttpHeaders buildProtectedHeaders() {
        if (hasProtectedRequestContext()) {
            return ProtectedGovernanceRequestSupport.buildProtectedHeaders();
        }
        throw new BizException(
            ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
            HttpStatus.UNAUTHORIZED,
            "Missing protected request context field: tenantId"
        );
    }

    private boolean hasProtectedRequestContext() {
        return StringUtils.hasText(RequestContext.getTenantId())
            && StringUtils.hasText(RequestContext.getUserId())
            && RequestContext.getRoleCodes() != null
            && !RequestContext.getRoleCodes().isEmpty()
            && StringUtils.hasText(RequestContext.getRequestId())
            && StringUtils.hasText(RequestContext.getTraceId())
            && StringUtils.hasText(RequestContext.getAuthSource())
            && RequestContext.getIssuedAt() > 0L
            && RequestContext.getExpiresAt() > 0L;
    }

    private String normalizeBaseUrl() {
        String baseUrl = properties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "benchmark-engine sql-optimization baseUrl is not configured"
            );
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
