package com.company.sqloptimization.infrastructure.queryexecution;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.ProtectedGovernanceRequestSupport;
import com.company.sqlforge.common.queryexecution.QueryExecutionMaterializedViewCreateRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionMaterializedViewCreateResponse;
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
public class QueryExecutionMaterializedViewHttpClient implements QueryExecutionMaterializedViewClient {

    private static final String MATERIALIZED_VIEW_ROUTE_UNAVAILABLE_MESSAGE = "查询执行物化视图创建路由不可用";

    private final RestTemplate restTemplate;
    private final OptimizationQueryExecutionProperties optimizationQueryExecutionProperties;

    public QueryExecutionMaterializedViewHttpClient(
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
    public QueryExecutionMaterializedViewCreateResponse create(QueryExecutionMaterializedViewCreateRequest request) {
        try {
            return restTemplate.postForObject(
                normalizeMaterializedViewBaseUrl() + "/create",
                new HttpEntity<Object>(request, buildProtectedHeaders()),
                QueryExecutionMaterializedViewCreateResponse.class
            );
        } catch (RestClientException ex) {
            throw new BizException(
                ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_ACCELERATION_PLAN_APPLY_FAILURE,
                HttpStatus.SERVICE_UNAVAILABLE,
                MATERIALIZED_VIEW_ROUTE_UNAVAILABLE_MESSAGE,
                ex
            );
        }
    }

    private HttpHeaders buildProtectedHeaders() {
        return ProtectedGovernanceRequestSupport.buildProtectedHeaders();
    }

    private String normalizeMaterializedViewBaseUrl() {
        String baseUrl = optimizationQueryExecutionProperties.getMaterializedViewBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "sql-optimization query-execution materializedViewBaseUrl 未配置"
            );
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
