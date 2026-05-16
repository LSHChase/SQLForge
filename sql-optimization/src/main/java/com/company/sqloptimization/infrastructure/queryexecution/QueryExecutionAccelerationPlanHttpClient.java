package com.company.sqloptimization.infrastructure.queryexecution;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.ProtectedGovernanceRequestSupport;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanApplyRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanRollbackRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanVerifyRequest;
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
public class QueryExecutionAccelerationPlanHttpClient implements QueryExecutionAccelerationPlanClient {

    private static final String QUERY_EXECUTION_ROUTE_UNAVAILABLE_MESSAGE = "查询执行加速运行时路由不可用";

    private final RestTemplate restTemplate;
    private final OptimizationQueryExecutionProperties optimizationQueryExecutionProperties;

    public QueryExecutionAccelerationPlanHttpClient(RestTemplateBuilder restTemplateBuilder,
                                                    OptimizationQueryExecutionProperties optimizationQueryExecutionProperties) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(optimizationQueryExecutionProperties.getConnectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(optimizationQueryExecutionProperties.getReadTimeoutMs()))
            .build();
        this.optimizationQueryExecutionProperties = optimizationQueryExecutionProperties;
    }

    @Override
    public QueryExecutionAccelerationPlanResponse apply(QueryExecutionAccelerationPlanApplyRequest request) {
        return post(
            "/apply",
            request,
            QueryExecutionAccelerationPlanResponse.class,
            ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_ACCELERATION_PLAN_APPLY_FAILURE
        );
    }

    @Override
    public QueryExecutionAccelerationPlanResponse verify(QueryExecutionAccelerationPlanVerifyRequest request) {
        return post(
            "/verify",
            request,
            QueryExecutionAccelerationPlanResponse.class,
            ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_ACCELERATION_PLAN_VERIFY_FAILURE
        );
    }

    @Override
    public QueryExecutionAccelerationPlanResponse rollback(QueryExecutionAccelerationPlanRollbackRequest request) {
        return post(
            "/rollback",
            request,
            QueryExecutionAccelerationPlanResponse.class,
            ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_ACCELERATION_PLAN_ROLLBACK_FAILURE
        );
    }

    private <T> T post(String path, Object request, Class<T> responseType, int failureCode) {
        try {
            return restTemplate.postForObject(
                normalizeBaseUrl() + path,
                new HttpEntity<Object>(request, buildProtectedHeaders()),
                responseType
            );
        } catch (RestClientException ex) {
            throw new BizException(
                failureCode,
                HttpStatus.SERVICE_UNAVAILABLE,
                QUERY_EXECUTION_ROUTE_UNAVAILABLE_MESSAGE,
                ex
            );
        }
    }

    private HttpHeaders buildProtectedHeaders() {
        return ProtectedGovernanceRequestSupport.buildProtectedHeaders();
    }

    private String normalizeBaseUrl() {
        String baseUrl = optimizationQueryExecutionProperties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "sql-optimization query-execution baseUrl 未配置"
            );
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
