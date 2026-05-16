package com.company.sqloptimization.infrastructure.queryexecution;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.ProtectedGovernanceRequestSupport;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingPublishRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResponse;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingStateChangeRequest;
import com.company.sqloptimization.config.OptimizationQueryExecutionProperties;
import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class QueryExecutionRuntimeRewriteBindingHttpClient implements QueryExecutionRuntimeRewriteBindingClient {

    private static final String RUNTIME_REWRITE_ROUTE_UNAVAILABLE_MESSAGE =
        "查询执行运行时改写绑定路由不可用";

    private final RestTemplate restTemplate;
    private final OptimizationQueryExecutionProperties optimizationQueryExecutionProperties;

    public QueryExecutionRuntimeRewriteBindingHttpClient(
        RestTemplateBuilder restTemplateBuilder,
        OptimizationQueryExecutionProperties optimizationQueryExecutionProperties
    ) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(optimizationQueryExecutionProperties.getConnectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(optimizationQueryExecutionProperties.getReadTimeoutMs()))
            .build();
        this.optimizationQueryExecutionProperties = optimizationQueryExecutionProperties;
    }

    @Override
    public RuntimeRewriteBindingResponse publish(RuntimeRewriteBindingPublishRequest request) {
        return post("/publish", request);
    }

    @Override
    public RuntimeRewriteBindingResponse pause(RuntimeRewriteBindingStateChangeRequest request) {
        return post("/pause", request);
    }

    @Override
    public RuntimeRewriteBindingResponse unpublish(RuntimeRewriteBindingStateChangeRequest request) {
        return post("/unpublish", request);
    }

    private RuntimeRewriteBindingResponse post(String path, Object request) {
        try {
            return restTemplate.postForObject(
                normalizeRewriteBindingBaseUrl() + path,
                new HttpEntity<Object>(request, buildProtectedHeaders()),
                RuntimeRewriteBindingResponse.class
            );
        } catch (RestClientException ex) {
            throw new BizException(
                ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_REWRITE_FAILURE,
                HttpStatus.SERVICE_UNAVAILABLE,
                RUNTIME_REWRITE_ROUTE_UNAVAILABLE_MESSAGE,
                ex
            );
        }
    }

    private HttpHeaders buildProtectedHeaders() {
        return ProtectedGovernanceRequestSupport.buildProtectedHeaders();
    }

    private String normalizeRewriteBindingBaseUrl() {
        String baseUrl = optimizationQueryExecutionProperties.getRewriteBindingBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "sql-optimization query-execution rewriteBindingBaseUrl 未配置"
            );
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
