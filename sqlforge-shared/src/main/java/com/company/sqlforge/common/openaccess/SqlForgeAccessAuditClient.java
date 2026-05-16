package com.company.sqlforge.common.openaccess;

import com.company.sqlforge.common.access.AccessChannel;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceAuditWriteRequest;
import com.company.sqlforge.common.utils.JsonUtils;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class SqlForgeAccessAuditClient {

    private static final String AUDIT_ROUTE_UNAVAILABLE_MESSAGE = "SQLForge 治理审计路由不可用";

    private final RestTemplate restTemplate;
    private final OpenAccessHttpClientProperties properties;

    public SqlForgeAccessAuditClient(RestTemplateBuilder restTemplateBuilder, OpenAccessHttpClientProperties properties) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
            .build();
        this.properties = properties;
    }

    public void writeAudit(OpenAccessRequestContext requestContext,
                           String serviceCode,
                           String operationCode,
                           String resourceType,
                           String resourceId,
                           String resultStatus,
                           long elapsedMs,
                           Map<String, Object> requestParams,
                           Map<String, Object> responseSummary) {
        GovernanceAuditWriteRequest request = new GovernanceAuditWriteRequest();
        AccessChannel accessChannel = requestContext.getAccessChannel() == null
            ? AccessChannel.API
            : requestContext.getAccessChannel();
        request.setAccessChannel(accessChannel.name());
        request.setAuthSource(requestContext.getAuthSource());
        request.setServiceCode(serviceCode);
        request.setOperationCode(operationCode);
        request.setResourceType(resourceType);
        request.setResourceId(resourceId);
        request.setResultStatus(resultStatus);
        request.setElapsedMs(Long.valueOf(elapsedMs));
        request.setSourceIp(StringUtils.hasText(requestContext.getSourceIp()) ? requestContext.getSourceIp() : "UNKNOWN");
        request.setUserAgent(StringUtils.hasText(requestContext.getUserAgent()) ? requestContext.getUserAgent() : serviceCode);
        request.setRequestParams(requestParams == null || requestParams.isEmpty() ? null : JsonUtils.toJson(requestParams));
        request.setResponseSummary(responseSummary == null || responseSummary.isEmpty() ? null : JsonUtils.toJson(responseSummary));
        try {
            restTemplate.postForObject(
                normalizeBaseUrl() + "/api/governance/internal/audit/write",
                new HttpEntity<Object>(request, OpenAccessHeaderSupport.buildHeaders(requestContext, accessChannel)),
                Object.class
            );
        } catch (RestClientException ex) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID,
                HttpStatus.SERVICE_UNAVAILABLE,
                AUDIT_ROUTE_UNAVAILABLE_MESSAGE,
                ex
            );
        }
    }

    private String normalizeBaseUrl() {
        String baseUrl = properties == null ? null : properties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "开放接入 baseUrl 未配置"
            );
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
