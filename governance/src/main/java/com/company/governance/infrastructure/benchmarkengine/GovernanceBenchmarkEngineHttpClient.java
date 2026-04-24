package com.company.governance.infrastructure.benchmarkengine;

import com.company.governance.config.GovernanceBenchmarkEngineProperties;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactOperationRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactOperationResponse;
import com.company.sqlforge.common.governance.ProtectedGovernanceRequestSupport;
import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class GovernanceBenchmarkEngineHttpClient implements GovernanceBenchmarkEngineClient {

    private static final String BENCHMARK_ENGINE_ROUTE_UNAVAILABLE_MESSAGE =
        "Benchmark-engine artifact operation route is unavailable";

    private final RestTemplate restTemplate;
    private final GovernanceBenchmarkEngineProperties governanceBenchmarkEngineProperties;

    public GovernanceBenchmarkEngineHttpClient(RestTemplateBuilder restTemplateBuilder,
                                               GovernanceBenchmarkEngineProperties governanceBenchmarkEngineProperties) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(governanceBenchmarkEngineProperties.getConnectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(governanceBenchmarkEngineProperties.getReadTimeoutMs()))
            .build();
        this.governanceBenchmarkEngineProperties = governanceBenchmarkEngineProperties;
    }

    @Override
    public GovernanceBenchmarkArtifactOperationResponse operateArtifact(GovernanceBenchmarkArtifactOperationRequest request) {
        try {
            return restTemplate.postForObject(
                normalizeBaseUrl() + "/artifact-operations",
                new HttpEntity<Object>(request, ProtectedGovernanceRequestSupport.buildProtectedHeaders()),
                GovernanceBenchmarkArtifactOperationResponse.class
            );
        } catch (RestClientException ex) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID,
                HttpStatus.SERVICE_UNAVAILABLE,
                BENCHMARK_ENGINE_ROUTE_UNAVAILABLE_MESSAGE,
                ex
            );
        }
    }

    private String normalizeBaseUrl() {
        String baseUrl = governanceBenchmarkEngineProperties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "governance benchmark-engine baseUrl is not configured"
            );
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
