package com.company.queryexecution.application.service;

import com.company.queryexecution.config.QueryExecutionHetuProperties;
import com.company.queryexecution.infrastructure.governance.GovernanceCapabilityClient;
import com.company.queryexecution.infrastructure.governance.QueryExecutionAuditRecord;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveResponse;
import com.company.sqlforge.common.jdbc.ManagedJdbcConnectionFactory;
import com.company.sqlforge.common.queryexecution.QueryExecutionMaterializedViewCreateRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionMaterializedViewCreateResponse;
import com.company.sqlforge.common.utils.JsonUtils;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class QueryExecutionMaterializedViewCreateService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "MATERIALIZED_VIEW_CREATE_JDBC_BASELINE";
    private static final String OPERATION = "MATERIALIZED_VIEW_CREATE";
    private static final String RESOURCE_TYPE = "MATERIALIZED_VIEW";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_PARTIAL_SUCCESS = "PARTIAL_SUCCESS";

    private final GovernanceCapabilityClient governanceCapabilityClient;
    private final QueryExecutionHetuProperties hetuProperties;
    private final MaterializedViewJdbcExecutor jdbcExecutor;

    @Autowired
    public QueryExecutionMaterializedViewCreateService(
        GovernanceCapabilityClient governanceCapabilityClient,
        QueryExecutionHetuProperties hetuProperties
    ) {
        this(governanceCapabilityClient, hetuProperties, new DefaultMaterializedViewJdbcExecutor());
    }

    QueryExecutionMaterializedViewCreateService(GovernanceCapabilityClient governanceCapabilityClient,
                                                QueryExecutionHetuProperties hetuProperties,
                                                MaterializedViewJdbcExecutor jdbcExecutor) {
        this.governanceCapabilityClient = governanceCapabilityClient;
        this.hetuProperties = hetuProperties == null ? new QueryExecutionHetuProperties() : hetuProperties;
        this.jdbcExecutor = jdbcExecutor;
    }

    public QueryExecutionMaterializedViewCreateResponse create(QueryExecutionMaterializedViewCreateRequest request) {
        long startedAt = System.currentTimeMillis();
        String tenantId = requireText(request == null ? null : request.getTenantId(), "tenantId");
        requireProtectedTenant(tenantId);
        String recommendationId = requireText(request.getRecommendationId(), "recommendationId");
        String mvName = requireText(request.getMvName(), "mvName");
        String targetDatasource = requireText(request.getTargetDatasource(), "targetDatasource");
        DataSourceTypeEnum targetEngine = requireTargetEngine(request.getTargetEngine());
        String ddlSql = requireText(request.getDdlSql(), "ddlSql");
        String refreshSql = requireText(request.getRefreshSql(), "refreshSql");
        governanceCapabilityClient.assertDatasourceAccess(
            tenantId,
            targetEngine,
            RESOURCE_TYPE,
            mvName,
            OPERATION
        );
        ResolvedJdbcDatasource datasource = resolveJdbcDatasource(tenantId, targetEngine, targetDatasource);
        QueryExecutionMaterializedViewCreateResponse response;
        try {
            ExecutionEvidence ddlEvidence = jdbcExecutor.execute(datasource, ddlSql);
            ExecutionEvidence refreshEvidence;
            try {
                refreshEvidence = jdbcExecutor.execute(datasource, refreshSql);
            } catch (RuntimeException ex) {
                response = buildResponse(
                    request,
                    STATUS_PARTIAL_SUCCESS,
                    STATUS_SUCCESS,
                    STATUS_FAILED,
                    "物化视图 DDL 已执行成功，但 refresh 失败；SQLForge 不自动 rollback。",
                    runtimeDetails(request, datasource, ddlEvidence, failedEvidence(ex), System.currentTimeMillis() - startedAt)
                );
                writeAudit(request, response, startedAt);
                return response;
            }
            response = buildResponse(
                request,
                STATUS_SUCCESS,
                STATUS_SUCCESS,
                STATUS_SUCCESS,
                "物化视图 DDL 与 refresh 已执行成功。",
                runtimeDetails(request, datasource, ddlEvidence, refreshEvidence, System.currentTimeMillis() - startedAt)
            );
            writeAudit(request, response, startedAt);
            return response;
        } catch (RuntimeException ex) {
            response = buildResponse(
                request,
                STATUS_FAILED,
                STATUS_FAILED,
                "NOT_STARTED",
                "物化视图 DDL 执行失败，refresh 未执行。",
                runtimeDetails(request, datasource, failedEvidence(ex), notStartedEvidence(), System.currentTimeMillis() - startedAt)
            );
            writeAudit(request, response, startedAt);
            throw new BizException(
                ErrorCodeConstants.QUERY_EXECUTION_SYSTEM_ROUTE_UNAVAILABLE,
                HttpStatus.SERVICE_UNAVAILABLE,
                "物化视图 DDL 执行失败：" + ex.getMessage(),
                ex
            );
        }
    }

    private QueryExecutionMaterializedViewCreateResponse buildResponse(
        QueryExecutionMaterializedViewCreateRequest request,
        String status,
        String ddlStatus,
        String refreshStatus,
        String runtimeSummary,
        Map<String, Object> runtimeDetails
    ) {
        QueryExecutionMaterializedViewCreateResponse response = new QueryExecutionMaterializedViewCreateResponse();
        response.setRecommendationId(request.getRecommendationId());
        response.setRewriteRecordId(trimToNull(request.getRewriteRecordId()));
        response.setMvName(request.getMvName());
        response.setTargetEngine(request.getTargetEngine());
        response.setTargetDatasource(request.getTargetDatasource());
        response.setStatus(status);
        response.setDdlStatus(ddlStatus);
        response.setRefreshStatus(refreshStatus);
        response.setRuntimeSummary(runtimeSummary);
        response.setRuntimeDetailsJson(JsonUtils.toJson(runtimeDetails));
        return response;
    }

    private Map<String, Object> runtimeDetails(QueryExecutionMaterializedViewCreateRequest request,
                                               ResolvedJdbcDatasource datasource,
                                               ExecutionEvidence ddlEvidence,
                                               ExecutionEvidence refreshEvidence,
                                               long elapsedMs) {
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("contractStage", CONTRACT_STAGE);
        details.put("implementationStage", IMPLEMENTATION_STAGE);
        details.put("tenantId", request.getTenantId());
        details.put("recommendationId", request.getRecommendationId());
        details.put("rewriteRecordId", trimToNull(request.getRewriteRecordId()));
        details.put("mvName", request.getMvName());
        details.put("targetEngine", request.getTargetEngine());
        details.put("targetDatasource", request.getTargetDatasource());
        details.put("jdbcResolutionSource", datasource.getResolutionSource());
        details.put("jdbcUrlConfigured", Boolean.valueOf(StringUtils.hasText(datasource.getJdbcUrl())));
        details.put("ddl", ddlEvidence.toMap());
        details.put("refresh", refreshEvidence.toMap());
        details.put("automaticRollback", Boolean.FALSE);
        details.put("elapsedMs", Long.valueOf(elapsedMs));
        String reason = trimToNull(request.getReason());
        if (reason != null) {
            details.put("reason", reason);
        }
        String requestId = trimToNull(RequestContext.getRequestId());
        if (requestId != null) {
            details.put("requestId", requestId);
        }
        String traceId = trimToNull(RequestContext.getTraceId());
        if (traceId != null) {
            details.put("traceId", traceId);
        }
        details.put("occurredAt", Instant.now().toString());
        return details;
    }

    private void writeAudit(QueryExecutionMaterializedViewCreateRequest request,
                            QueryExecutionMaterializedViewCreateResponse response,
                            long startedAt) {
        Map<String, Object> requestPayload = new LinkedHashMap<String, Object>();
        requestPayload.put("tenantId", request.getTenantId());
        requestPayload.put("recommendationId", request.getRecommendationId());
        requestPayload.put("rewriteRecordId", trimToNull(request.getRewriteRecordId()));
        requestPayload.put("mvName", request.getMvName());
        requestPayload.put("targetEngine", request.getTargetEngine());
        requestPayload.put("targetDatasource", request.getTargetDatasource());
        requestPayload.put("reason", trimToNull(request.getReason()));

        Map<String, Object> responsePayload = new LinkedHashMap<String, Object>();
        responsePayload.put("status", response.getStatus());
        responsePayload.put("ddlStatus", response.getDdlStatus());
        responsePayload.put("refreshStatus", response.getRefreshStatus());
        responsePayload.put("runtimeSummary", response.getRuntimeSummary());
        governanceCapabilityClient.writeAudit(
            new QueryExecutionAuditRecord(
                OPERATION,
                RESOURCE_TYPE,
                request.getMvName(),
                response.getStatus(),
                System.currentTimeMillis() - startedAt,
                JsonUtils.toJson(requestPayload),
                JsonUtils.toJson(responsePayload)
            )
        );
    }

    private ResolvedJdbcDatasource resolveJdbcDatasource(String tenantId,
                                                         DataSourceTypeEnum targetEngine,
                                                         String targetDatasource) {
        GovernanceJdbcDatasourceResolveResponse response = resolveGovernanceDatasource(
            tenantId,
            targetEngine,
            targetDatasource
        );
        if (response != null && response.isResolved() && StringUtils.hasText(response.getJdbcUrl())) {
            return ResolvedJdbcDatasource.governance(response, queryTimeoutSeconds(response));
        }
        if (targetEngine == DataSourceTypeEnum.HETU && StringUtils.hasText(hetuProperties.getJdbc().getUrl())) {
            return ResolvedJdbcDatasource.local(hetuProperties.getJdbc());
        }
        String failureReason = response == null ? "GOVERNANCE_JDBC_DATASOURCE_UNRESOLVED" : response.getFailureReason();
        throw new BizException(
            ErrorCodeConstants.QUERY_EXECUTION_SYSTEM_ROUTE_UNAVAILABLE,
            HttpStatus.SERVICE_UNAVAILABLE,
            "治理 JDBC 数据源不可用：" + (StringUtils.hasText(failureReason) ? failureReason : targetDatasource)
        );
    }

    private GovernanceJdbcDatasourceResolveResponse resolveGovernanceDatasource(String tenantId,
                                                                                DataSourceTypeEnum targetEngine,
                                                                                String targetDatasource) {
        GovernanceJdbcDatasourceResolveRequest resolveRequest = new GovernanceJdbcDatasourceResolveRequest();
        resolveRequest.setTenantId(tenantId);
        resolveRequest.setDatasourceCode(targetDatasource);
        resolveRequest.setEngineType(targetEngine.name());
        return governanceCapabilityClient.resolveJdbcDatasource(resolveRequest);
    }

    private int queryTimeoutSeconds(GovernanceJdbcDatasourceResolveResponse response) {
        if (response != null && response.getTimeoutMs() != null && response.getTimeoutMs().intValue() > 0) {
            return Math.max(1, response.getTimeoutMs().intValue() / 1000);
        }
        return hetuProperties.getJdbc().getQueryTimeoutSeconds();
    }

    private DataSourceTypeEnum requireTargetEngine(String value) {
        String text = requireText(value, "targetEngine");
        try {
            return DataSourceTypeEnum.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "targetEngine 不支持：" + text
            );
        }
    }

    private void requireProtectedTenant(String tenantId) {
        if (!StringUtils.hasText(RequestContext.getTenantId())) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "已认证请求上下文缺少 tenantId"
            );
        }
        if (!RequestContext.getTenantId().equals(tenantId == null ? null : tenantId.trim())) {
            throw new AccessDeniedException("请求 tenantId 与已认证租户上下文不一致");
        }
    }

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                fieldName + " 不能为空"
            );
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private ExecutionEvidence failedEvidence(RuntimeException ex) {
        return new ExecutionEvidence(STATUS_FAILED, 0L, ex.getClass().getSimpleName(), ex.getMessage());
    }

    private ExecutionEvidence notStartedEvidence() {
        return new ExecutionEvidence("NOT_STARTED", 0L, null, null);
    }

    interface MaterializedViewJdbcExecutor {

        ExecutionEvidence execute(ResolvedJdbcDatasource datasource, String sql);
    }

    static final class DefaultMaterializedViewJdbcExecutor implements MaterializedViewJdbcExecutor {

        private final ManagedJdbcConnectionFactory connectionFactory = new ManagedJdbcConnectionFactory();

        @Override
        public ExecutionEvidence execute(ResolvedJdbcDatasource datasource, String sql) {
            long start = System.currentTimeMillis();
            try (Connection connection = connectionFactory.openConnection(
                datasource.getJdbcUrl(),
                datasource.toConnectionProperties(),
                datasource.getDriverClassName(),
                datasource.getDriverSourceType(),
                StringUtils.hasText(datasource.getDriverRelativePath())
                    ? Paths.get(datasource.getDriverRelativePath())
                    : null,
                datasource.getDriverArtifactId(),
                datasource.getDriverSha256()
            );
                 Statement statement = connection.createStatement()) {
                if (datasource.getQueryTimeoutSeconds() > 0) {
                    statement.setQueryTimeout(datasource.getQueryTimeoutSeconds());
                }
                statement.execute(sql);
                return new ExecutionEvidence(STATUS_SUCCESS, System.currentTimeMillis() - start, null, null);
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("JDBC 驱动加载失败", ex);
            } catch (SQLException ex) {
                throw new IllegalStateException("JDBC 语句执行失败", ex);
            }
        }
    }

    static final class ResolvedJdbcDatasource {

        private final String jdbcUrl;
        private final String driverClassName;
        private final String driverSourceType;
        private final String driverArtifactId;
        private final String driverSha256;
        private final String driverRelativePath;
        private final String username;
        private final String password;
        private final int queryTimeoutSeconds;
        private final String resolutionSource;

        private ResolvedJdbcDatasource(String jdbcUrl,
                                       String driverClassName,
                                       String driverSourceType,
                                       String driverArtifactId,
                                       String driverSha256,
                                       String driverRelativePath,
                                       String username,
                                       String password,
                                       int queryTimeoutSeconds,
                                       String resolutionSource) {
            this.jdbcUrl = jdbcUrl;
            this.driverClassName = driverClassName;
            this.driverSourceType = driverSourceType;
            this.driverArtifactId = driverArtifactId;
            this.driverSha256 = driverSha256;
            this.driverRelativePath = driverRelativePath;
            this.username = username;
            this.password = password;
            this.queryTimeoutSeconds = queryTimeoutSeconds;
            this.resolutionSource = resolutionSource;
        }

        static ResolvedJdbcDatasource governance(GovernanceJdbcDatasourceResolveResponse response,
                                                 int queryTimeoutSeconds) {
            return new ResolvedJdbcDatasource(
                response.getJdbcUrl(),
                response.getDriverClassName(),
                response.getDriverSourceType(),
                response.getDriverArtifactId(),
                response.getDriverSha256(),
                response.getDriverRelativePath(),
                response.getUsername(),
                response.getPassword(),
                queryTimeoutSeconds,
                "GOVERNANCE_JDBC_DATASOURCE"
            );
        }

        static ResolvedJdbcDatasource local(QueryExecutionHetuProperties.Jdbc jdbc) {
            return new ResolvedJdbcDatasource(
                jdbc.getUrl(),
                null,
                null,
                null,
                null,
                null,
                jdbc.getUsername(),
                jdbc.getPassword(),
                jdbc.getQueryTimeoutSeconds(),
                "LOCAL_HETU_JDBC_FALLBACK"
            );
        }

        Properties toConnectionProperties() {
            Properties properties = new Properties();
            if (StringUtils.hasText(username)) {
                properties.setProperty("user", username.trim());
            }
            if (StringUtils.hasText(password)) {
                properties.setProperty("password", password);
            }
            return properties;
        }

        String getJdbcUrl() { return jdbcUrl; }
        String getDriverClassName() { return driverClassName; }
        String getDriverSourceType() { return driverSourceType; }
        String getDriverArtifactId() { return driverArtifactId; }
        String getDriverSha256() { return driverSha256; }
        String getDriverRelativePath() { return driverRelativePath; }
        int getQueryTimeoutSeconds() { return queryTimeoutSeconds; }
        String getResolutionSource() { return resolutionSource; }
    }

    static final class ExecutionEvidence {

        private final String status;
        private final long elapsedMs;
        private final String failureType;
        private final String failureReason;

        ExecutionEvidence(String status, long elapsedMs, String failureType, String failureReason) {
            this.status = status;
            this.elapsedMs = elapsedMs;
            this.failureType = failureType;
            this.failureReason = failureReason;
        }

        Map<String, Object> toMap() {
            Map<String, Object> evidence = new LinkedHashMap<String, Object>();
            evidence.put("status", status);
            evidence.put("elapsedMs", Long.valueOf(elapsedMs));
            if (StringUtils.hasText(failureType)) {
                evidence.put("failureType", failureType);
            }
            if (StringUtils.hasText(failureReason)) {
                evidence.put("failureReason", failureReason);
            }
            return evidence;
        }
    }
}
