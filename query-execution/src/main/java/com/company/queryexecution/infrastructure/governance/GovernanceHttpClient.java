package com.company.queryexecution.infrastructure.governance;

import com.company.queryexecution.config.QueryExecutionGovernanceProperties;
import com.company.sqlforge.common.access.AccessAuditContract;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.governance.GovernanceAuditWriteRequest;
import com.company.sqlforge.common.governance.GovernanceDatasourceAccessCheckRequest;
import com.company.sqlforge.common.governance.GovernanceDatasourceAccessCheckResponse;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveResponse;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteRequest;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteResponse;
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

    private static final String GOVERNANCE_ROUTE_UNAVAILABLE_MESSAGE = "治理能力路由不可用";

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
    public void assertDatasourceAccess(String tenantId,
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
                "缺少数据源映射：" + datasourceType
            );
        }

        GovernanceDatasourceAccessCheckRequest request = new GovernanceDatasourceAccessCheckRequest();
        request.setServiceCode(ServiceCodeConstants.QUERY_EXECUTION);
        request.setTenantId(tenantId);
        request.setResourceType(resourceType);
        request.setResourceId(resourceId);
        request.setOperationCode(operationCode);
        request.setDatasourceId(datasourceId);
        request.setAction(resolveDatasourceAction(operationCode));
        GovernanceDatasourceAccessCheckResponse response =
            post("/datasource-access/check", request, GovernanceDatasourceAccessCheckResponse.class);
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
        AccessAuditContract accessAuditContract = AccessAuditContract.capture();
        GovernanceAuditWriteRequest request = new GovernanceAuditWriteRequest();
        request.setAccessChannel(accessAuditContract.getAccessChannel().name());
        request.setAuthSource(accessAuditContract.getAuthSource());
        request.setServiceCode(ServiceCodeConstants.QUERY_EXECUTION);
        request.setOperationCode(auditRecord.getOperationCode());
        request.setResourceType(auditRecord.getResourceType());
        request.setResourceId(auditRecord.getResourceId());
        request.setResultStatus(auditRecord.getResultStatus());
        request.setElapsedMs(Long.valueOf(auditRecord.getElapsedMs()));
        request.setSourceIp(ProtectedGovernanceRequestSupport.resolveSourceIp("127.0.0.1"));
        request.setUserAgent(ProtectedGovernanceRequestSupport.resolveUserAgent("SQLForge-QueryExecution"));
        request.setRequestParams(auditRecord.getRequestParams());
        request.setResponseSummary(auditRecord.getResponseSummary());
        post("/audit/write", request, Object.class);
    }

    @Override
    public GovernanceJdbcRouteResolveResponse resolveJdbcRoute(GovernanceJdbcRouteResolveRequest request) {
        return post("/datasources/jdbc/resolve-route", request, GovernanceJdbcRouteResolveResponse.class);
    }

    @Override
    public GovernanceQueryExecutionHistoryWriteResponse writeQueryExecutionHistory(
        GovernanceQueryExecutionHistoryWriteRequest request
    ) {
        return post(
            "/query-execution-history/write",
            request,
            GovernanceQueryExecutionHistoryWriteResponse.class
        );
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

    private String resolveDatasourceAction(String operationCode) {
        return "USE";
    }

    private String normalizeBaseUrl() {
        String baseUrl = governanceProperties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "query-execution governance baseUrl 未配置"
            );
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

}
