package com.company.sqlforge.common.openaccess;

import com.company.sqlforge.common.access.AccessChannel;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class SqlForgeQueryExecutionClient {

    private static final String QUERY_ROUTE_UNAVAILABLE_MESSAGE = "SQLForge query-execution route is unavailable";

    private final RestTemplate restTemplate;
    private final OpenAccessHttpClientProperties properties;

    public SqlForgeQueryExecutionClient(RestTemplateBuilder restTemplateBuilder, OpenAccessHttpClientProperties properties) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
            .build();
        this.properties = properties;
    }

    public SqlForgeQueryResponse execute(OpenAccessRequestContext requestContext,
                                         SqlForgeQueryRequest request,
                                         AccessChannel accessChannel) {
        RestClientException lastFailure = null;
        int attempts = Math.max(0, properties.getMaxRetries()) + 1;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return restTemplate.postForObject(
                    normalizeBaseUrl() + "/api/query-execution/queries/execute",
                    new HttpEntity<Object>(request, OpenAccessHeaderSupport.buildHeaders(requestContext, accessChannel)),
                    SqlForgeQueryResponse.class
                );
            } catch (RestClientException ex) {
                lastFailure = ex;
                if (attempt >= attempts) {
                    break;
                }
                sleepBackoff();
            }
        }
        throw new BizException(
            ErrorCodeConstants.SYSTEM_OPEN_ACCESS_ROUTE_INVALID,
            HttpStatus.SERVICE_UNAVAILABLE,
            QUERY_ROUTE_UNAVAILABLE_MESSAGE,
            lastFailure
        );
    }

    private void sleepBackoff() {
        if (properties.getRetryBackoffMs() <= 0L) {
            return;
        }
        try {
            Thread.sleep(properties.getRetryBackoffMs());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String normalizeBaseUrl() {
        String baseUrl = properties == null ? null : properties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "open-access baseUrl is not configured"
            );
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
