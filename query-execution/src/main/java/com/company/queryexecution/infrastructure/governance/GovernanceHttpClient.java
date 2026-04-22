package com.company.queryexecution.infrastructure.governance;

import com.company.queryexecution.application.context.RequestMetadataContext;
import com.company.queryexecution.config.QueryExecutionGovernanceProperties;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import java.time.Duration;
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
public class GovernanceHttpClient implements GovernanceCapabilityClient {

    private static final String GOVERNANCE_ROUTE_UNAVAILABLE_MESSAGE = "Governance capability route is unavailable";

    private final RestTemplate restTemplate;
    private final QueryExecutionGovernanceProperties governanceProperties;

    public GovernanceHttpClient(RestTemplateBuilder restTemplateBuilder,
                                QueryExecutionGovernanceProperties governanceProperties) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(governanceProperties.getConnectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(governanceProperties.getReadTimeoutMs()))
            .build();
        this.governanceProperties = governanceProperties;
    }

    @Override
    public void assertTenantScope(String tenantId) {
        TenantScopeCheckRequest request = new TenantScopeCheckRequest();
        request.setTenantId(tenantId);
        request.setTargetTenantId(tenantId);
        TenantScopeCheckResponse response = post("/tenant-scope/check", request, TenantScopeCheckResponse.class);
        if (response == null || !response.isAllowed()) {
            throw new AccessDeniedException(
                response == null || !StringUtils.hasText(response.getReason())
                    ? "Governance tenant-scope check denied the request"
                    : response.getReason()
            );
        }
    }

    @Override
    public void assertDatasourceAccess(String tenantId, DataSourceTypeEnum datasourceType) {
        String datasourceId = governanceProperties.getDatasourceIdMap().get(
            datasourceType == null ? DataSourceTypeEnum.AUTO.name() : datasourceType.name()
        );
        if (!StringUtils.hasText(datasourceId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Datasource mapping is missing for " + datasourceType
            );
        }

        DatasourceAccessCheckRequest request = new DatasourceAccessCheckRequest();
        request.setTenantId(tenantId);
        request.setDatasourceId(datasourceId);
        DatasourceAccessCheckResponse response =
            post("/datasource-access/check", request, DatasourceAccessCheckResponse.class);
        if (response == null || !response.isAllowed()) {
            throw new AccessDeniedException(
                response == null || !StringUtils.hasText(response.getReason())
                    ? ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED_MESSAGE
                    : response.getReason()
            );
        }
    }

    @Override
    public void writeAudit(QueryExecutionAuditRecord auditRecord) {
        AuditWriteRequest request = new AuditWriteRequest();
        request.setServiceCode(ServiceCodeConstants.QUERY_EXECUTION);
        request.setOperationCode(auditRecord.getOperationCode());
        request.setResourceType(auditRecord.getResourceType());
        request.setResourceId(auditRecord.getResourceId());
        request.setResultStatus(auditRecord.getResultStatus());
        request.setElapsedMs(Long.valueOf(auditRecord.getElapsedMs()));
        request.setSourceIp(resolveMetadata(RequestMetadataContext.getSourceIp(), "127.0.0.1"));
        request.setUserAgent(resolveMetadata(RequestMetadataContext.getUserAgent(), "SQLForge-QueryExecution"));
        request.setRequestParams(auditRecord.getRequestParams());
        request.setResponseSummary(auditRecord.getResponseSummary());
        post("/audit/write", request, Object.class);
    }

    private <T> T post(String path, Object request, Class<T> responseType) {
        try {
            return restTemplate.postForObject(
                normalizeBaseUrl() + path,
                new HttpEntity<Object>(request, buildProtectedHeaders()),
                responseType
            );
        } catch (RestClientException ex) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID,
                HttpStatus.SERVICE_UNAVAILABLE,
                GOVERNANCE_ROUTE_UNAVAILABLE_MESSAGE,
                ex
            );
        }
    }

    private HttpHeaders buildProtectedHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(RequestHeaderConstants.TENANT_ID, requiredContextValue(RequestContext.getTenantId(), "tenantId"));
        headers.set(RequestHeaderConstants.USER_ID, requiredContextValue(RequestContext.getUserId(), "userId"));
        headers.set(RequestHeaderConstants.ROLE_CODES, String.join(",", RequestContext.getRoleCodes()));
        headers.set(RequestHeaderConstants.REQUEST_ID, requiredContextValue(RequestContext.getRequestId(), "requestId"));
        headers.set(RequestHeaderConstants.TRACE_ID, requiredContextValue(RequestContext.getTraceId(), "traceId"));
        headers.set(RequestHeaderConstants.AUTH_SOURCE, requiredContextValue(RequestContext.getAuthSource(), "authSource"));
        headers.set(RequestHeaderConstants.ISSUED_AT, String.valueOf(RequestContext.getIssuedAt()));
        headers.set(RequestHeaderConstants.EXPIRES_AT, String.valueOf(RequestContext.getExpiresAt()));
        return headers;
    }

    private String requiredContextValue(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "Missing protected request context field: " + fieldName
            );
        }
        return value;
    }

    private String resolveMetadata(String currentValue, String defaultValue) {
        return StringUtils.hasText(currentValue) ? currentValue : defaultValue;
    }

    private String normalizeBaseUrl() {
        String baseUrl = governanceProperties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "query-execution governance baseUrl is not configured"
            );
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public static class TenantScopeCheckRequest {

        private String tenantId;
        private String targetTenantId;

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getTargetTenantId() {
            return targetTenantId;
        }

        public void setTargetTenantId(String targetTenantId) {
            this.targetTenantId = targetTenantId;
        }
    }

    public static class TenantScopeCheckResponse {

        private String tenantId;
        private String targetTenantId;
        private boolean allowed;
        private String reason;

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getTargetTenantId() {
            return targetTenantId;
        }

        public void setTargetTenantId(String targetTenantId) {
            this.targetTenantId = targetTenantId;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public void setAllowed(boolean allowed) {
            this.allowed = allowed;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class DatasourceAccessCheckRequest {

        private String tenantId;
        private String datasourceId;

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getDatasourceId() {
            return datasourceId;
        }

        public void setDatasourceId(String datasourceId) {
            this.datasourceId = datasourceId;
        }
    }

    public static class DatasourceAccessCheckResponse {

        private String tenantId;
        private String datasourceId;
        private boolean allowed;
        private String reason;
        private Integer errorCode;
        private String contractStage;
        private String implementationStage;

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getDatasourceId() {
            return datasourceId;
        }

        public void setDatasourceId(String datasourceId) {
            this.datasourceId = datasourceId;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public void setAllowed(boolean allowed) {
            this.allowed = allowed;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public Integer getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(Integer errorCode) {
            this.errorCode = errorCode;
        }

        public String getContractStage() {
            return contractStage;
        }

        public void setContractStage(String contractStage) {
            this.contractStage = contractStage;
        }

        public String getImplementationStage() {
            return implementationStage;
        }

        public void setImplementationStage(String implementationStage) {
            this.implementationStage = implementationStage;
        }
    }

    public static class AuditWriteRequest {

        private String serviceCode;
        private String operationCode;
        private String resourceType;
        private String resourceId;
        private String resultStatus;
        private Long elapsedMs;
        private String sourceIp;
        private String userAgent;
        private String requestParams;
        private String responseSummary;

        public String getServiceCode() {
            return serviceCode;
        }

        public void setServiceCode(String serviceCode) {
            this.serviceCode = serviceCode;
        }

        public String getOperationCode() {
            return operationCode;
        }

        public void setOperationCode(String operationCode) {
            this.operationCode = operationCode;
        }

        public String getResourceType() {
            return resourceType;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }

        public String getResourceId() {
            return resourceId;
        }

        public void setResourceId(String resourceId) {
            this.resourceId = resourceId;
        }

        public String getResultStatus() {
            return resultStatus;
        }

        public void setResultStatus(String resultStatus) {
            this.resultStatus = resultStatus;
        }

        public Long getElapsedMs() {
            return elapsedMs;
        }

        public void setElapsedMs(Long elapsedMs) {
            this.elapsedMs = elapsedMs;
        }

        public String getSourceIp() {
            return sourceIp;
        }

        public void setSourceIp(String sourceIp) {
            this.sourceIp = sourceIp;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public String getRequestParams() {
            return requestParams;
        }

        public void setRequestParams(String requestParams) {
            this.requestParams = requestParams;
        }

        public String getResponseSummary() {
            return responseSummary;
        }

        public void setResponseSummary(String responseSummary) {
            this.responseSummary = responseSummary;
        }
    }
}
