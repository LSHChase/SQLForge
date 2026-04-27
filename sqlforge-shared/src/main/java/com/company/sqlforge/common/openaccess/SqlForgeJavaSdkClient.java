package com.company.sqlforge.common.openaccess;

import com.company.sqlforge.common.access.AccessChannel;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import java.util.LinkedHashMap;
import java.util.Map;

public class SqlForgeJavaSdkClient {

    private static final String OPERATION_CODE = "SDK_QUERY_EXECUTE";
    private static final String RESOURCE_TYPE = "SDK_QUERY";

    private final SqlForgeQueryExecutionClient queryExecutionClient;
    private final SqlForgeAccessAuditClient accessAuditClient;

    public SqlForgeJavaSdkClient(SqlForgeQueryExecutionClient queryExecutionClient,
                                 SqlForgeAccessAuditClient accessAuditClient) {
        this.queryExecutionClient = queryExecutionClient;
        this.accessAuditClient = accessAuditClient;
    }

    public SqlForgeQueryResponse execute(OpenAccessRequestContext requestContext, SqlForgeQueryRequest request) {
        OpenAccessRequestContext sdkContext = requestContext.withAccessChannel(AccessChannel.SDK);
        long start = System.currentTimeMillis();
        String sqlFingerprint = SqlFingerprintUtils.fingerprint(request == null ? null : request.getSqlText());
        try {
            SqlForgeQueryResponse response = queryExecutionClient.execute(sdkContext, request, AccessChannel.SDK);
            if (accessAuditClient != null) {
                accessAuditClient.writeAudit(
                    sdkContext,
                    ServiceCodeConstants.JAVA_SDK,
                    OPERATION_CODE,
                    RESOURCE_TYPE,
                    sqlFingerprint,
                    response == null || response.getStatus() == null ? "UNKNOWN" : response.getStatus().name(),
                    System.currentTimeMillis() - start,
                    buildRequestSummary(request, sqlFingerprint),
                    buildSuccessSummary(response)
                );
            }
            return response;
        } catch (RuntimeException ex) {
            if (accessAuditClient != null) {
                accessAuditClient.writeAudit(
                    sdkContext,
                    ServiceCodeConstants.JAVA_SDK,
                    OPERATION_CODE,
                    RESOURCE_TYPE,
                    sqlFingerprint,
                    "FAILED",
                    System.currentTimeMillis() - start,
                    buildRequestSummary(request, sqlFingerprint),
                    buildFailureSummary(ex)
                );
            }
            throw ex;
        }
    }

    private Map<String, Object> buildRequestSummary(SqlForgeQueryRequest request, String sqlFingerprint) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("sqlFingerprint", sqlFingerprint);
        summary.put("tenantId", request == null ? null : request.getTenantId());
        summary.put("datasourceType", request == null || request.getDatasourceType() == null
            ? null
            : request.getDatasourceType().name());
        summary.put("faultToleranceStrategy", request == null || request.getFaultToleranceStrategy() == null
            ? null
            : request.getFaultToleranceStrategy().name());
        summary.put("accelerationPreference", request == null || request.getAccelerationPreference() == null
            ? null
            : request.getAccelerationPreference().name());
        return summary;
    }

    private Map<String, Object> buildSuccessSummary(SqlForgeQueryResponse response) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("resultStatus", response == null || response.getStatus() == null ? null : response.getStatus().name());
        summary.put("degraded", response != null && response.isDegraded());
        summary.put("routeProfile", response == null || response.getMetadata() == null ? null : response.getMetadata().getRouteProfile());
        summary.put("executionMode", response == null || response.getMetadata() == null ? null : response.getMetadata().getExecutionMode());
        return summary;
    }

    private Map<String, Object> buildFailureSummary(RuntimeException ex) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("resultStatus", "FAILED");
        summary.put("errorType", ex.getClass().getSimpleName());
        summary.put("message", ex.getMessage());
        return summary;
    }
}
