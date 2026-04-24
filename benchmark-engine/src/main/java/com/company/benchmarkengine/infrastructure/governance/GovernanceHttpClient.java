package com.company.benchmarkengine.infrastructure.governance;

import com.company.benchmarkengine.config.BenchmarkEngineGovernanceProperties;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.governance.GovernanceAuditWriteRequest;
import com.company.sqlforge.common.governance.GovernanceAuthorizationDecisionRequest;
import com.company.sqlforge.common.governance.GovernanceAuthorizationDecisionResponse;
import com.company.sqlforge.common.governance.GovernanceBenchmarkReportTraceRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkReportTraceResponse;
import com.company.sqlforge.common.governance.ProtectedGovernanceRequestSupport;
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
    private final BenchmarkEngineGovernanceProperties governanceProperties;

    public GovernanceHttpClient(RestTemplateBuilder restTemplateBuilder,
                                BenchmarkEngineGovernanceProperties governanceProperties) {
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
        request.setServiceCode(ServiceCodeConstants.BENCHMARK_ENGINE);
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
    public GovernanceBenchmarkReportTraceResponse writeBenchmarkReportTrace(GovernanceBenchmarkReportTraceRequest request) {
        return post("/benchmark/report-trace/write", request, GovernanceBenchmarkReportTraceResponse.class);
    }

    @Override
    public void writeAudit(BenchmarkAuditRecord auditRecord) {
        GovernanceAuditWriteRequest request = new GovernanceAuditWriteRequest();
        request.setServiceCode(ServiceCodeConstants.BENCHMARK_ENGINE);
        request.setOperationCode(auditRecord.getOperationCode());
        request.setResourceType(auditRecord.getResourceType());
        request.setResourceId(auditRecord.getResourceId());
        request.setResultStatus(auditRecord.getResultStatus());
        request.setElapsedMs(Long.valueOf(auditRecord.getElapsedMs()));
        request.setSourceIp(ProtectedGovernanceRequestSupport.resolveSourceIp("127.0.0.1"));
        request.setUserAgent(ProtectedGovernanceRequestSupport.resolveUserAgent("SQLForge-BenchmarkEngine"));
        request.setSagaId(auditRecord.getSagaId());
        request.setConfigSnapshotId(auditRecord.getConfigSnapshotId());
        request.setResultId(auditRecord.getResultId());
        request.setHistoryId(auditRecord.getHistoryId());
        request.setExportId(auditRecord.getExportId());
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
                "benchmark-engine governance baseUrl is not configured"
            );
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

}
