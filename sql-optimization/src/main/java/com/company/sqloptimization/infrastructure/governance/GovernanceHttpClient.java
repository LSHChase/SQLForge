package com.company.sqloptimization.infrastructure.governance;

import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.governance.GovernanceAccelerationPlanTraceRequest;
import com.company.sqlforge.common.governance.GovernanceAccelerationPlanTraceResponse;
import com.company.sqlforge.common.governance.GovernanceAuditWriteRequest;
import com.company.sqlforge.common.governance.GovernanceAuthorizationDecisionRequest;
import com.company.sqlforge.common.governance.GovernanceAuthorizationDecisionResponse;
import com.company.sqlforge.common.governance.GovernanceDbViewResolveRequest;
import com.company.sqlforge.common.governance.GovernanceDbViewResolveResponse;
import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigRequest;
import com.company.sqlforge.common.governance.GovernanceReportInterfaceConfigResponse;
import com.company.sqlforge.common.governance.ProtectedGovernanceRequestSupport;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqloptimization.config.OptimizationGovernanceProperties;
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
    private final OptimizationGovernanceProperties governanceProperties;

    public GovernanceHttpClient(RestTemplateBuilder restTemplateBuilder,
                                OptimizationGovernanceProperties governanceProperties) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(governanceProperties.getConnectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(governanceProperties.getReadTimeoutMs()))
            .build();
        this.governanceProperties = governanceProperties;
    }

    @Override
    public void assertAuthorization(String tenantId,
                                    DataSourceTypeEnum datasourceType,
                                    String resourceType,
                                    String resourceId,
                                    String operationCode) {
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

        GovernanceAuthorizationDecisionRequest request = new GovernanceAuthorizationDecisionRequest();
        request.setServiceCode(ServiceCodeConstants.SQL_OPTIMIZATION);
        request.setTenantId(tenantId);
        request.setResourceType(resourceType);
        request.setResourceId(resourceId);
        request.setOperationCode(operationCode);
        request.setDatasourceId(datasourceId);
        GovernanceAuthorizationDecisionResponse response =
            post("/authorization/decide", request, GovernanceAuthorizationDecisionResponse.class);
        if (response == null || !response.isAllowed()) {
            throw new AccessDeniedException(
                response == null || !StringUtils.hasText(response.getReason())
                    ? ErrorCodeConstants.GOVERNANCE_DATASOURCE_ACCESS_DENIED_MESSAGE
                    : response.getReason()
            );
        }
    }

    @Override
    public GovernanceAccelerationPlanTraceResponse writeAccelerationPlanTrace(GovernanceAccelerationPlanTraceRequest request) {
        return post("/acceleration-plan/trace/write", request, GovernanceAccelerationPlanTraceResponse.class);
    }

    @Override
    public GovernanceDbViewResolveResponse resolveDbView(GovernanceDbViewResolveRequest request) {
        return post("/db-views/resolve", request, GovernanceDbViewResolveResponse.class);
    }

    @Override
    public GovernanceReportInterfaceConfigResponse resolveReportInterfaceConfig(
        GovernanceReportInterfaceConfigRequest request
    ) {
        return post("/report-interface-configs/resolve", request, GovernanceReportInterfaceConfigResponse.class);
    }

    @Override
    public void writeAudit(OptimizationAuditRecord auditRecord) {
        GovernanceAuditWriteRequest request = new GovernanceAuditWriteRequest();
        request.setServiceCode(ServiceCodeConstants.SQL_OPTIMIZATION);
        request.setOperationCode(auditRecord.getOperationCode());
        request.setResourceType(auditRecord.getResourceType());
        request.setResourceId(auditRecord.getResourceId());
        request.setResultStatus(auditRecord.getResultStatus());
        request.setElapsedMs(Long.valueOf(auditRecord.getElapsedMs()));
        request.setSagaId(auditRecord.getSagaId());
        request.setConfigSnapshotId(auditRecord.getConfigSnapshotId());
        request.setResultId(auditRecord.getResultId());
        request.setHistoryId(auditRecord.getHistoryId());
        request.setSourceIp(ProtectedGovernanceRequestSupport.resolveSourceIp("127.0.0.1"));
        request.setUserAgent(ProtectedGovernanceRequestSupport.resolveUserAgent("SQLForge-SqlOptimization"));
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
        return ProtectedGovernanceRequestSupport.buildProtectedHeaders();
    }

    private String normalizeBaseUrl() {
        String baseUrl = governanceProperties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "sql-optimization governance baseUrl is not configured"
            );
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

}
