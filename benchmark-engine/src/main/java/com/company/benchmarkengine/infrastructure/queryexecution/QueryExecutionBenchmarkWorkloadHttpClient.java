package com.company.benchmarkengine.infrastructure.queryexecution;

import com.company.benchmarkengine.config.BenchmarkEngineQueryExecutionProperties;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.ProtectedGovernanceRequestSupport;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadResponse;
import java.time.Duration;
import java.util.UUID;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class QueryExecutionBenchmarkWorkloadHttpClient implements QueryExecutionBenchmarkWorkloadClient {

    private static final String QUERY_EXECUTION_ROUTE_UNAVAILABLE_MESSAGE = "Query-execution workload route is unavailable";

    private final RestTemplate restTemplate;
    private final BenchmarkEngineQueryExecutionProperties queryExecutionProperties;

    public QueryExecutionBenchmarkWorkloadHttpClient(RestTemplateBuilder restTemplateBuilder,
                                                     BenchmarkEngineQueryExecutionProperties queryExecutionProperties) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(queryExecutionProperties.getConnectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(queryExecutionProperties.getReadTimeoutMs()))
            .build();
        this.queryExecutionProperties = queryExecutionProperties;
    }

    @Override
    public QueryExecutionBenchmarkWorkloadResponse captureWorkload(QueryExecutionBenchmarkWorkloadRequest request) {
        try {
            return restTemplate.postForObject(
                normalizeBaseUrl() + "/workload/capture",
                new HttpEntity<Object>(request, buildProtectedHeaders(request == null ? null : request.getTenantId())),
                QueryExecutionBenchmarkWorkloadResponse.class
            );
        } catch (RestClientException ex) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID,
                HttpStatus.SERVICE_UNAVAILABLE,
                QUERY_EXECUTION_ROUTE_UNAVAILABLE_MESSAGE,
                ex
            );
        }
    }

    private HttpHeaders buildProtectedHeaders(String fallbackTenantId) {
        if (hasProtectedRequestContext()) {
            return ProtectedGovernanceRequestSupport.buildProtectedHeaders();
        }
        if (!StringUtils.hasText(fallbackTenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "Missing protected request context field: tenantId"
            );
        }
        long issuedAt = System.currentTimeMillis();
        long expiresAt = issuedAt + 60000L;
        String syntheticTraceId = "benchmark-engine-query-execution-" + UUID.randomUUID().toString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-Id", fallbackTenantId);
        headers.set("X-User-Id", "benchmark-engine-service");
        headers.set("X-Role-Codes", "SERVICE");
        headers.set("X-Request-Id", syntheticTraceId);
        headers.set("X-Trace-Id", syntheticTraceId);
        headers.set("X-Auth-Source", "header");
        headers.set("X-Issued-At", String.valueOf(issuedAt));
        headers.set("X-Expires-At", String.valueOf(expiresAt));
        return headers;
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
        String baseUrl = queryExecutionProperties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "benchmark-engine query-execution baseUrl is not configured"
            );
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
