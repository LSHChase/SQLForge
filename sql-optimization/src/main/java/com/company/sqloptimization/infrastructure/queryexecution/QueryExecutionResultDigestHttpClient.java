package com.company.sqloptimization.infrastructure.queryexecution;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.ProtectedGovernanceRequestSupport;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestResponse;
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
public class QueryExecutionResultDigestHttpClient implements QueryExecutionResultDigestClient {

    private static final String RESULT_DIGEST_ROUTE_UNAVAILABLE_MESSAGE =
        "Query-execution readonly result digest route is unavailable";

    private final RestTemplate restTemplate;
    private final OptimizationQueryExecutionProperties optimizationQueryExecutionProperties;

    public QueryExecutionResultDigestHttpClient(RestTemplateBuilder restTemplateBuilder,
                                                OptimizationQueryExecutionProperties optimizationQueryExecutionProperties) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(optimizationQueryExecutionProperties.getConnectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(optimizationQueryExecutionProperties.getReadTimeoutMs()))
            .build();
        this.optimizationQueryExecutionProperties = optimizationQueryExecutionProperties;
    }

    @Override
    public QueryExecutionResultDigestResponse executeDigest(QueryExecutionResultDigestRequest request) {
        try {
            return restTemplate.postForObject(
                normalizeResultDigestBaseUrl() + "/execute",
                new HttpEntity<Object>(request, buildProtectedHeaders()),
                QueryExecutionResultDigestResponse.class
            );
        } catch (RestClientException ex) {
            throw new BizException(
                ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_REWRITE_FAILURE,
                HttpStatus.SERVICE_UNAVAILABLE,
                RESULT_DIGEST_ROUTE_UNAVAILABLE_MESSAGE,
                ex
            );
        }
    }

    private HttpHeaders buildProtectedHeaders() {
        return ProtectedGovernanceRequestSupport.buildProtectedHeaders();
    }

    private String normalizeResultDigestBaseUrl() {
        String baseUrl = optimizationQueryExecutionProperties.getResultDigestBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "sql-optimization query-execution resultDigestBaseUrl is not configured"
            );
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
