package com.company.governance.infrastructure.sqloptimization;

import com.company.governance.config.GovernanceSqlOptimizationProperties;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.ProtectedGovernanceRequestSupport;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GovernanceSqlOptimizationHttpClient implements GovernanceSqlOptimizationClient {

    private static final String SQL_OPTIMIZATION_ROUTE_UNAVAILABLE_MESSAGE =
        "SQL 优化改写记录路由不可用";

    private final RestTemplate restTemplate;
    private final GovernanceSqlOptimizationProperties governanceSqlOptimizationProperties;

    public GovernanceSqlOptimizationHttpClient(RestTemplateBuilder restTemplateBuilder,
                                               GovernanceSqlOptimizationProperties governanceSqlOptimizationProperties) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(governanceSqlOptimizationProperties.getConnectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(governanceSqlOptimizationProperties.getReadTimeoutMs()))
            .build();
        this.governanceSqlOptimizationProperties = governanceSqlOptimizationProperties;
    }

    @Override
    public List<SqlOptimizationRewriteRecordResponse> listRewriteRecordsByHistoryId(String historyId) {
        return listRewriteRecords(historyId, null, null, null);
    }

    @Override
    public List<SqlOptimizationRewriteRecordResponse> listRewriteRecords(String historyId,
                                                                         String recommendationId,
                                                                         String validationStatus,
                                                                         String sourceType) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(normalizeBaseUrl())
                .path("/rewrite-records");
            appendQueryParam(builder, "historyId", historyId);
            appendQueryParam(builder, "recommendationId", recommendationId);
            appendQueryParam(builder, "validationStatus", validationStatus);
            appendQueryParam(builder, "sourceType", sourceType);
            ResponseEntity<List<SqlOptimizationRewriteRecordResponse>> response = restTemplate.exchange(
                builder.build(false)
                    .encode()
                    .toUri(),
                HttpMethod.GET,
                new HttpEntity<Object>(ProtectedGovernanceRequestSupport.buildProtectedHeaders()),
                new ParameterizedTypeReference<List<SqlOptimizationRewriteRecordResponse>>() {
                }
            );
            return response.getBody() == null
                ? Collections.<SqlOptimizationRewriteRecordResponse>emptyList()
                : response.getBody();
        } catch (RestClientException ex) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID,
                HttpStatus.SERVICE_UNAVAILABLE,
                SQL_OPTIMIZATION_ROUTE_UNAVAILABLE_MESSAGE,
                ex
            );
        }
    }

    private void appendQueryParam(UriComponentsBuilder builder, String name, String value) {
        if (StringUtils.hasText(value)) {
            builder.queryParam(name, value.trim());
        }
    }

    private String normalizeBaseUrl() {
        String baseUrl = governanceSqlOptimizationProperties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "governance sql-optimization baseUrl 未配置"
            );
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
