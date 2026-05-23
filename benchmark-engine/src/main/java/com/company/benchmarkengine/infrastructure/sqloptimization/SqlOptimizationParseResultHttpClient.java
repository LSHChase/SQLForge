package com.company.benchmarkengine.infrastructure.sqloptimization;

import com.company.benchmarkengine.config.BenchmarkEngineSqlOptimizationProperties;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.ProtectedGovernanceRequestSupport;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
public class SqlOptimizationParseResultHttpClient implements SqlOptimizationParseResultClient {

    private static final String SQL_OPTIMIZATION_ROUTE_UNAVAILABLE_MESSAGE = "SQL 优化解析结果路由不可用";

    private final RestTemplate restTemplate;
    private final BenchmarkEngineSqlOptimizationProperties properties;

    public SqlOptimizationParseResultHttpClient(RestTemplateBuilder restTemplateBuilder,
                                                BenchmarkEngineSqlOptimizationProperties properties) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
            .build();
        this.properties = properties;
    }

    @Override
    public SqlOptimizationCombinedParseStatus getCombinedParseStatus(String parseTaskId) {
        return exchange(
            "/parse/" + parseTaskId,
            SqlOptimizationCombinedParseStatus.class
        );
    }

    @Override
    public SqlOptimizationParseBatchStatus getParseBatch(String batchId) {
        return exchange(
            "/parse-batches/" + batchId,
            SqlOptimizationParseBatchStatus.class
        );
    }

    @Override
    public List<SqlOptimizationParseSqlIssueStatistic> getImportantUrgentSqls() {
        ResponseEntity<SqlOptimizationParseSqlIssueStatistic[]> response = exchangeEntity(
            "/parse-statistics/important-urgent",
            SqlOptimizationParseSqlIssueStatistic[].class
        );
        SqlOptimizationParseSqlIssueStatistic[] body = response.getBody();
        if (body == null || body.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(body);
    }

    private <T> T exchange(String path, Class<T> responseType) {
        ResponseEntity<T> response = exchangeEntity(path, responseType);
        return response.getBody();
    }

    private <T> ResponseEntity<T> exchangeEntity(String path, Class<T> responseType) {
        try {
            return restTemplate.exchange(
                normalizeBaseUrl() + path,
                HttpMethod.GET,
                new HttpEntity<Object>(buildProtectedHeaders()),
                responseType
            );
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
            "受保护请求上下文字段缺失：tenantId"
        );
    }

    private boolean hasProtectedRequestContext() {
        return StringUtils.hasText(RequestContext.getTenantId())
            && StringUtils.hasText(RequestContext.getUserId())
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
                "benchmark-engine sql-optimization baseUrl 未配置"
            );
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
