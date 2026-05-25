package com.company.queryexecution.application.service;

import com.company.queryexecution.application.controller.dto.QueryContextDTO;
import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.application.controller.vo.QueryErrorDetailVO;
import com.company.queryexecution.application.controller.vo.QueryExecuteResponse;
import com.company.queryexecution.application.controller.vo.QueryExecutionMetadataVO;
import com.company.queryexecution.application.controller.vo.QueryRetryStepVO;
import com.company.queryexecution.config.QueryExecutionRewriteProperties;
import com.company.queryexecution.domain.query.ActivatedAccelerationBinding;
import com.company.queryexecution.domain.query.FaultToleranceStrategy;
import com.company.queryexecution.domain.query.QueryExecutionStatus;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.queryexecution.domain.query.ReadonlyQueryAssessment;
import com.company.queryexecution.domain.query.ReadonlyQueryGuard;
import com.company.queryexecution.infrastructure.adapter.HetuExecutionUnavailableException;
import com.company.queryexecution.infrastructure.adapter.QueryExecutionAdapter;
import com.company.queryexecution.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqlforge.common.access.AccessAuditContract;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteCandidate;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveResponse;
import com.company.sqlforge.common.jdbcagent.JdbcAgentSqlCommentParser;
import com.company.sqlforge.common.logicalobject.LogicalObjectRef;
import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import com.company.sqlforge.common.logicalobject.LogicalObjectType;
import com.company.sqlforge.common.logicalobject.SqlSurfaceObjectRefExtractor;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResolveRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResponse;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqlforge.common.utils.SqlCatalogQualifierRewriteUtils;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 在保留公共 HTTP 契约的同时执行最小同步查询闭环。
 */
@Service
public class QueryExecutionApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryExecutionApplicationService.class);

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "HETU_REAL_INTEGRATION";
    private static final String OPERATION = "QUERY_EXECUTE_SYNC";
    private static final String READONLY_SQL_REJECTION_MESSAGE = "当前同步查询路径仅允许只读单语句 SQL";
    private static final String ROUTE_UNAVAILABLE_MESSAGE = "当前同步查询路径尚未为目标数据源开放执行路由";
    private static final String QUERY_TIMEOUT_MESSAGE = "联机查询在当前超时阈值内未完成";
    private static final String FALLBACK_IMMEDIATE_REASON = "当前容错策略要求立即走兜底路径。";
    private static final String TIMEOUT_FALLBACK_REASON = "主引擎超过超时阈值，已应用兜底策略。";
    private static final String STATE_REQUEST_ACCEPTED = "REQUEST_ACCEPTED";
    private static final String STATE_RISK_REJECTED = "RISK_REJECTED";
    private static final String STATE_ROUTE_UNAVAILABLE = "ROUTE_UNAVAILABLE";
    private static final String STATE_PRIMARY_ROUTE_SELECTED = "PRIMARY_ROUTE_SELECTED";
    private static final String STATE_PRIMARY_MODE_CHAIN_FAILED = "PRIMARY_MODE_CHAIN_FAILED";
    private static final String STATE_PRIMARY_TIMEOUT = "PRIMARY_TIMEOUT";
    private static final String STATE_DEV_REWRITE_DIRECT_SUCCESS = "DEV_REWRITE_DIRECT_SUCCESS";
    private static final String STATE_LOCAL_ROLLBACK_MARKED = "LOCAL_ROLLBACK_MARKED";
    private static final String STATE_FALLBACK_REQUESTED = "FALLBACK_REQUESTED";
    private static final String STATE_LOCAL_COMPENSATION_MARKED = "LOCAL_COMPENSATION_MARKED";
    private static final String STATE_COMPLETED = "COMPLETED";
    private static final String MARKER_TIMEOUT_ROLLBACK = "LOCAL_TIMEOUT_ROLLBACK_MARKED";
    private static final String MARKER_PRIMARY_ROUTE_FAILURE = "LOCAL_PRIMARY_ROUTE_FAILURE_MARKED";
    private static final String MARKER_FALLBACK_COMPENSATION = "LOCAL_FALLBACK_COMPENSATION_MARKED";
    private static final String ACTION_CLOSE_PRIMARY_ATTEMPT_CONTEXT = "CLOSE_PRIMARY_ATTEMPT_CONTEXT";
    private static final String ACTION_RECORD_DEGRADED_RESULT = "RECORD_DEGRADED_RESULT";
    private static final String RESOURCE_TYPE_QUERY = "QUERY_EXECUTION_QUERY";
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern NAMED_BINDING_PATTERN = Pattern.compile(":[A-Za-z][A-Za-z0-9_]*");
    private static final Pattern POSITIONAL_BINDING_PATTERN = Pattern.compile("\\?");
    private static final Pattern LOGICAL_OBJECT_PATTERN = Pattern.compile("(?i)\\b(?:from|join|into|update)\\s+([A-Za-z0-9_$.]+)");

    private final QueryExecutionAdapter queryExecutionAdapter;
    private final GovernanceCapabilityClient governanceCapabilityClient;
    private final QueryExecutionMetricsRecorder metricsRecorder;
    private final QueryExecutionAccelerationRuntimeService queryExecutionAccelerationRuntimeService;
    private final QueryExecutionCacheGovernanceRuntimeService queryExecutionCacheGovernanceRuntimeService;
    private final QueryExecutionRuntimeRewriteBindingService queryExecutionRuntimeRewriteBindingService;
    private final QueryExecutionRewriteProperties rewriteProperties;
    private final Executor queryHistoryWriteExecutor;

    @Autowired
    public QueryExecutionApplicationService(QueryExecutionAdapter queryExecutionAdapter,
                                            GovernanceCapabilityClient governanceCapabilityClient,
                                            QueryExecutionMetricsRecorder metricsRecorder,
                                            QueryExecutionAccelerationRuntimeService queryExecutionAccelerationRuntimeService,
                                            QueryExecutionCacheGovernanceRuntimeService queryExecutionCacheGovernanceRuntimeService,
                                            QueryExecutionRuntimeRewriteBindingService queryExecutionRuntimeRewriteBindingService,
                                            QueryExecutionRewriteProperties rewriteProperties,
                                            @Qualifier("tenantAwareTaskExecutor") Executor queryHistoryWriteExecutor) {
        this.queryExecutionAdapter = queryExecutionAdapter;
        this.governanceCapabilityClient = governanceCapabilityClient;
        this.metricsRecorder = metricsRecorder;
        this.queryExecutionAccelerationRuntimeService = queryExecutionAccelerationRuntimeService;
        this.queryExecutionCacheGovernanceRuntimeService = queryExecutionCacheGovernanceRuntimeService;
        this.queryExecutionRuntimeRewriteBindingService = queryExecutionRuntimeRewriteBindingService;
        this.rewriteProperties = rewriteProperties == null
            ? new QueryExecutionRewriteProperties()
            : rewriteProperties;
        this.queryHistoryWriteExecutor = queryHistoryWriteExecutor == null
            ? directExecutor()
            : queryHistoryWriteExecutor;
    }

    public QueryExecutionApplicationService(QueryExecutionAdapter queryExecutionAdapter,
                                            GovernanceCapabilityClient governanceCapabilityClient,
                                            QueryExecutionMetricsRecorder metricsRecorder,
                                            QueryExecutionAccelerationRuntimeService queryExecutionAccelerationRuntimeService,
                                            QueryExecutionCacheGovernanceRuntimeService queryExecutionCacheGovernanceRuntimeService,
                                            QueryExecutionRuntimeRewriteBindingService queryExecutionRuntimeRewriteBindingService,
                                            QueryExecutionRewriteProperties rewriteProperties) {
        this(
            queryExecutionAdapter,
            governanceCapabilityClient,
            metricsRecorder,
            queryExecutionAccelerationRuntimeService,
            queryExecutionCacheGovernanceRuntimeService,
            queryExecutionRuntimeRewriteBindingService,
            rewriteProperties,
            directExecutor()
        );
    }

    QueryExecutionApplicationService(QueryExecutionAdapter queryExecutionAdapter,
                                     GovernanceCapabilityClient governanceCapabilityClient) {
        this(
            queryExecutionAdapter,
            governanceCapabilityClient,
            QueryExecutionMetricsRecorder.noop(),
            new QueryExecutionAccelerationRuntimeService(),
            new QueryExecutionCacheGovernanceRuntimeService(),
            null,
            new QueryExecutionRewriteProperties()
        );
    }

    QueryExecutionApplicationService(QueryExecutionAdapter queryExecutionAdapter,
                                     GovernanceCapabilityClient governanceCapabilityClient,
                                     QueryExecutionMetricsRecorder metricsRecorder) {
        this(
            queryExecutionAdapter,
            governanceCapabilityClient,
            metricsRecorder,
            new QueryExecutionAccelerationRuntimeService(),
            new QueryExecutionCacheGovernanceRuntimeService(),
            null,
            new QueryExecutionRewriteProperties()
        );
    }

    QueryExecutionApplicationService(QueryExecutionAdapter queryExecutionAdapter,
                                     GovernanceCapabilityClient governanceCapabilityClient,
                                     QueryExecutionMetricsRecorder metricsRecorder,
                                     QueryExecutionAccelerationRuntimeService queryExecutionAccelerationRuntimeService) {
        this(
            queryExecutionAdapter,
            governanceCapabilityClient,
            metricsRecorder,
            queryExecutionAccelerationRuntimeService,
            new QueryExecutionCacheGovernanceRuntimeService(),
            null,
            new QueryExecutionRewriteProperties()
        );
    }

    QueryExecutionApplicationService(QueryExecutionAdapter queryExecutionAdapter,
                                     GovernanceCapabilityClient governanceCapabilityClient,
                                     QueryExecutionMetricsRecorder metricsRecorder,
                                     QueryExecutionAccelerationRuntimeService queryExecutionAccelerationRuntimeService,
                                     QueryExecutionCacheGovernanceRuntimeService queryExecutionCacheGovernanceRuntimeService) {
        this(
            queryExecutionAdapter,
            governanceCapabilityClient,
            metricsRecorder,
            queryExecutionAccelerationRuntimeService,
            queryExecutionCacheGovernanceRuntimeService,
            null,
            new QueryExecutionRewriteProperties()
        );
    }

    public QueryExecutionApplicationService(QueryExecutionAdapter queryExecutionAdapter,
                                            GovernanceCapabilityClient governanceCapabilityClient,
                                            QueryExecutionMetricsRecorder metricsRecorder,
                                            QueryExecutionAccelerationRuntimeService queryExecutionAccelerationRuntimeService,
                                            QueryExecutionCacheGovernanceRuntimeService queryExecutionCacheGovernanceRuntimeService,
                                            QueryExecutionRuntimeRewriteBindingService queryExecutionRuntimeRewriteBindingService) {
        this(
            queryExecutionAdapter,
            governanceCapabilityClient,
            metricsRecorder,
            queryExecutionAccelerationRuntimeService,
            queryExecutionCacheGovernanceRuntimeService,
            queryExecutionRuntimeRewriteBindingService,
            new QueryExecutionRewriteProperties()
        );
    }

    public QueryExecuteResponse executeSynchronously(QueryExecuteRequest request) {
        long start = System.currentTimeMillis();
        request.setTenantId(requireAuthorizedTenant(request.getTenantId()));
        String rawSql = normalizeSql(request.getSqlText());
        String effectiveSql = SqlCatalogQualifierRewriteUtils.rewriteBiViewCatalogQualifier(rawSql);
        String sqlFingerprint = SqlFingerprintUtils.fingerprint(effectiveSql);
        RuntimeRewriteResolution runtimeRewriteResolution =
            RuntimeRewriteResolution.noRewrite(rawSql, effectiveSql, sqlFingerprint);
        logStart(request, sqlFingerprint);
        try {
            DataSourceTypeEnum primaryEngine = resolvePrimaryEngine(request);
            governanceCapabilityClient.assertDatasourceAccess(
                request.getTenantId(),
                request.getDatasourceType(),
                RESOURCE_TYPE_QUERY,
                sqlFingerprint,
                OPERATION
            );

            ReadonlyQueryAssessment readonlyQueryAssessment = ReadonlyQueryGuard.assess(effectiveSql);
            if (!readonlyQueryAssessment.isReadonly()) {
                logStateChange(
                    sqlFingerprint,
                    request,
                    STATE_REQUEST_ACCEPTED,
                    STATE_RISK_REJECTED,
                    resolveTargetEngine(primaryEngine, request.getDatasourceType()),
                    0L,
                    QueryExecutionStatus.FAILED.name(),
                    null
                );
                return logAndReturn(
                    buildFailureResponse(
                        QueryExecutionStatus.FAILED,
                        resolveTargetEngine(primaryEngine, request.getDatasourceType()),
                        runtimeRewriteResolution,
                        0L,
                        0L,
                        Collections.<QueryRetryStepVO>emptyList(),
                        new QueryErrorDetailVO(
                            ErrorCodeConstants.QUERY_EXECUTION_RISK_REJECTED,
                            READONLY_SQL_REJECTION_MESSAGE,
                            readonlyQueryAssessment.getSuggestedAction(),
                            false
                        ),
                        sqlFingerprint
                    ),
                    request,
                    start
                );
            }

            if (primaryEngine == null) {
                logStateChange(
                    sqlFingerprint,
                    request,
                    STATE_REQUEST_ACCEPTED,
                    STATE_ROUTE_UNAVAILABLE,
                    request.getDatasourceType().name(),
                    0L,
                    QueryExecutionStatus.FAILED.name(),
                    null
                );
                return logAndReturn(
                    buildFailureResponse(
                        QueryExecutionStatus.FAILED,
                        request.getDatasourceType().name(),
                        runtimeRewriteResolution,
                        0L,
                        0L,
                        Collections.<QueryRetryStepVO>emptyList(),
                        new QueryErrorDetailVO(
                            ErrorCodeConstants.QUERY_EXECUTION_SYSTEM_ROUTE_UNAVAILABLE,
                            ROUTE_UNAVAILABLE_MESSAGE,
                            "当前同步基线请使用 TRINO、HETU、HIVE 或 AUTO。",
                            true
                        ),
                        sqlFingerprint
                    ),
                    request,
                    start
                );
            }

            runtimeRewriteResolution = resolveRuntimeRewrite(
                request,
                rawSql,
                effectiveSql,
                sqlFingerprint
            );
            String actualSql = runtimeRewriteResolution.getActualSql();
            String actualSqlFingerprint = runtimeRewriteResolution.getActualSqlFingerprint();
            Long timeoutMs = resolveTimeoutMs(request.getQueryContext());
            logStateChange(
                sqlFingerprint,
                request,
                STATE_REQUEST_ACCEPTED,
                STATE_PRIMARY_ROUTE_SELECTED,
                primaryEngine.name(),
                0L,
                "ROUTED",
                null
            );
            if (runtimeRewriteResolution.isRewriteApplied()
                && rewriteProperties.isDevelopmentDirectSuccessEnabled()) {
                logStateChange(
                    sqlFingerprint,
                    request,
                    STATE_PRIMARY_ROUTE_SELECTED,
                    STATE_DEV_REWRITE_DIRECT_SUCCESS,
                    primaryEngine.name(),
                    0L,
                    QueryExecutionStatus.SUCCESS.name(),
                    "DEV_RUNTIME_REWRITE_SHORT_CIRCUIT"
                );
                return logAndReturn(
                    buildSuccessResponse(
                        QueryExecutionStatus.SUCCESS,
                        buildDevelopmentRewriteStep(primaryEngine, runtimeRewriteResolution),
                        runtimeRewriteResolution,
                        false,
                        null,
                        Collections.<QueryRetryStepVO>emptyList()
                    ),
                    request,
                    start
                );
            }
            if (FaultToleranceStrategy.FALLBACK_IMMEDIATE == request.getFaultToleranceStrategy()) {
                logStateChange(
                    sqlFingerprint,
                    request,
                    STATE_PRIMARY_ROUTE_SELECTED,
                    STATE_FALLBACK_REQUESTED,
                    primaryEngine.name(),
                    0L,
                    QueryExecutionStatus.PARTIAL.name(),
                    null
                );
                return logAndReturn(
                    buildFallbackResponse(
                        primaryEngine,
                        runtimeRewriteResolution,
                        FALLBACK_IMMEDIATE_REASON,
                        Collections.<QueryRetryStepVO>emptyList(),
                        request
                    ),
                    request,
                    start
                );
            }

            ActivatedAccelerationBinding activatedAccelerationBinding = resolveActivatedAccelerationBinding(
                request,
                actualSqlFingerprint,
                primaryEngine
            );
            QueryExecutionCacheGovernanceRuntimeService.CacheResolution cacheResolution =
                queryExecutionCacheGovernanceRuntimeService.resolve(
                    request.getTenantId(),
                    actualSqlFingerprint,
                    primaryEngine.name(),
                    request.getQueryContext()
                );
            QueryExecutionStep cachedStep = queryExecutionCacheGovernanceRuntimeService.buildCacheHitStep(cacheResolution);
            if (cachedStep != null) {
                return logAndReturn(
                    buildSuccessResponse(
                        QueryExecutionStatus.SUCCESS,
                        cachedStep,
                        runtimeRewriteResolution,
                        false,
                        null,
                        Collections.<QueryRetryStepVO>emptyList()
                    ),
                    request,
                    start
                );
            }
            QueryExecuteRequest executionRequest = normalizeAccelerationRequest(
                request,
                activatedAccelerationBinding != null,
                primaryEngine,
                actualSql
            );
            QueryExecutionStep primaryStep;
            try {
                primaryStep = queryExecutionAdapter.execute(primaryEngine, actualSql, executionRequest, false);
            } catch (HetuExecutionUnavailableException ex) {
                return handleUnavailableHetuRoute(
                    request,
                    primaryEngine,
                    sqlFingerprint,
                    runtimeRewriteResolution,
                    ex,
                    start,
                    cacheResolution
                );
            }
            primaryStep = queryExecutionCacheGovernanceRuntimeService.finalizeSuccessfulExecution(cacheResolution, primaryStep);
            if (timeoutMs != null && primaryStep.getElapsedMs() > timeoutMs.longValue()) {
                logStateChange(
                    sqlFingerprint,
                    request,
                    STATE_PRIMARY_ROUTE_SELECTED,
                    STATE_PRIMARY_TIMEOUT,
                    primaryEngine.name(),
                    timeoutMs.longValue(),
                    QueryExecutionStatus.TIMEOUT.name(),
                    null
                );
                QueryRetryStepVO timeoutRecoveryStep = buildRecoveryStep(
                    primaryEngine.name(),
                    timeoutMs.longValue(),
                    QueryExecutionStatus.TIMEOUT.name(),
                    MARKER_TIMEOUT_ROLLBACK,
                    ACTION_CLOSE_PRIMARY_ATTEMPT_CONTEXT
                );
                logStateChange(
                    sqlFingerprint,
                    request,
                    STATE_PRIMARY_TIMEOUT,
                    STATE_LOCAL_ROLLBACK_MARKED,
                    primaryEngine.name(),
                    timeoutMs.longValue(),
                    QueryExecutionStatus.TIMEOUT.name(),
                    MARKER_TIMEOUT_ROLLBACK
                );
                if (FaultToleranceStrategy.RETRY_THEN_FALLBACK == request.getFaultToleranceStrategy()
                    && resolveFallbackEngine(primaryEngine) != null) {
                    logStateChange(
                        sqlFingerprint,
                        request,
                        STATE_LOCAL_ROLLBACK_MARKED,
                        STATE_FALLBACK_REQUESTED,
                        resolveFallbackEngine(primaryEngine).name(),
                        timeoutMs.longValue(),
                        QueryExecutionStatus.PARTIAL.name(),
                        timeoutRecoveryStep.getLocalRecoveryMarker()
                    );
                    return logAndReturn(
                        buildFallbackResponse(
                            primaryEngine,
                            runtimeRewriteResolution,
                            TIMEOUT_FALLBACK_REASON,
                            Collections.singletonList(timeoutRecoveryStep),
                            request
                        ),
                        request,
                        start
                    );
                }

                return logAndReturn(
                    buildFailureResponse(
                        QueryExecutionStatus.TIMEOUT,
                        primaryEngine.name(),
                        runtimeRewriteResolution,
                        timeoutMs.longValue(),
                        0L,
                        Collections.singletonList(timeoutRecoveryStep),
                        new QueryErrorDetailVO(
                            ErrorCodeConstants.QUERY_EXECUTION_SYSTEM_ENGINE_TIMEOUT,
                            QUERY_TIMEOUT_MESSAGE,
                            "请增加 timeoutMs，或在当前同步基线下使用 RETRY_THEN_FALLBACK。",
                            true
                        ),
                        sqlFingerprint,
                        primaryStep.getExecutionMode(),
                        primaryStep.getAttemptedModes(),
                        primaryStep.getRouteProfile(),
                        primaryStep.getRouteOrder(),
                        primaryStep.getRouteEvidenceSource(),
                        primaryStep.getRouteVerificationStatus(),
                        primaryStep.getCacheGovernanceStatus(),
                        primaryStep.getCacheGovernanceEvidence()
                    ),
                    request,
                    start
                );
            }

            return logAndReturn(
                buildSuccessResponse(
                    QueryExecutionStatus.SUCCESS,
                    primaryStep,
                    runtimeRewriteResolution,
                    false,
                    null,
                    Collections.<QueryRetryStepVO>emptyList()
                ),
                request,
                start
            );
        } catch (RuntimeException ex) {
            LOGGER.error(
                "操作日志 operation={} entity={} tenantId={} requestedDatasource={} faultTolerance={} costMs={} status=FAILED phase=EXCEPTION reason={}",
                OPERATION,
                sqlFingerprint,
                request.getTenantId(),
                request.getDatasourceType(),
                request.getFaultToleranceStrategy(),
                System.currentTimeMillis() - start,
                ex.getMessage(),
                ex
            );
            metricsRecorder.recordException(request, System.currentTimeMillis() - start);
            writeExecutionHistoryRecord(
                request,
                sqlFingerprint,
                null,
                QueryExecutionStatus.FAILED.name(),
                System.currentTimeMillis() - start,
                ex.getMessage(),
                runtimeRewriteResolution
            );
            throw ex;
        }
    }

    private QueryExecuteResponse buildFallbackResponse(DataSourceTypeEnum primaryEngine,
                                                       RuntimeRewriteResolution runtimeRewriteResolution,
                                                       String degradeReason,
                                                       List<QueryRetryStepVO> recoverySteps,
                                                       QueryExecuteRequest request) {
        DataSourceTypeEnum fallbackEngine = resolveFallbackEngine(primaryEngine);
        if (fallbackEngine == null) {
            return buildFailureResponse(
                QueryExecutionStatus.FAILED,
                primaryEngine.name(),
                runtimeRewriteResolution,
                0L,
                0L,
                Collections.<QueryRetryStepVO>emptyList(),
                new QueryErrorDetailVO(
                    ErrorCodeConstants.QUERY_EXECUTION_SYSTEM_ROUTE_UNAVAILABLE,
                    ROUTE_UNAVAILABLE_MESSAGE,
                    "当前基线下所选数据源没有可用的兜底路由。",
                    true
                ),
                runtimeRewriteResolution.getOriginalSqlFingerprint()
            );
        }

        QueryExecutionStep fallbackStep =
            queryExecutionAdapter.execute(fallbackEngine, runtimeRewriteResolution.getActualSql(), null, true);
        fallbackStep = queryExecutionCacheGovernanceRuntimeService.finalizeSuccessfulExecution(
            QueryExecutionCacheGovernanceRuntimeService.CacheResolution.ungoverned(),
            fallbackStep
        );
        List<QueryRetryStepVO> retryPath = new ArrayList<QueryRetryStepVO>(recoverySteps);
        QueryRetryStepVO compensationStep = buildRecoveryStep(
            fallbackEngine.name(),
            fallbackStep.getElapsedMs(),
            QueryExecutionStatus.SUCCESS.name(),
            MARKER_FALLBACK_COMPENSATION,
            ACTION_RECORD_DEGRADED_RESULT
        );
        retryPath.add(compensationStep);
        logStateChange(
            runtimeRewriteResolution.getOriginalSqlFingerprint(),
            request,
            STATE_FALLBACK_REQUESTED,
            STATE_LOCAL_COMPENSATION_MARKED,
            fallbackEngine.name(),
            fallbackStep.getElapsedMs(),
            QueryExecutionStatus.PARTIAL.name(),
            MARKER_FALLBACK_COMPENSATION
        );
        return buildSuccessResponse(
            QueryExecutionStatus.PARTIAL,
            fallbackStep,
            runtimeRewriteResolution,
            true,
            degradeReason,
            retryPath
        );
    }

    private QueryExecutionStep buildDevelopmentRewriteStep(DataSourceTypeEnum primaryEngine,
                                                           RuntimeRewriteResolution runtimeRewriteResolution) {
        return new QueryExecutionStep(
            primaryEngine,
            Collections.<Map<String, Object>>emptyList(),
            1L,
            0L,
            false,
            false,
            STATE_DEV_REWRITE_DIRECT_SUCCESS,
            Collections.singletonList(STATE_DEV_REWRITE_DIRECT_SUCCESS),
            "DEV_RUNTIME_REWRITE_SHORT_CIRCUIT",
            Arrays.asList("RUNTIME_REWRITE_BINDING", "DIRECT_SUCCESS"),
            "ACTIVE_RUNTIME_REWRITE_BINDING",
            "DEV_BYPASS_NO_ENGINE_EXECUTION",
            "BYPASSED",
            "DEV_RUNTIME_REWRITE_DIRECT_SUCCESS"
        );
    }

    private QueryExecuteResponse handleUnavailableHetuRoute(QueryExecuteRequest request,
                                                            DataSourceTypeEnum primaryEngine,
                                                            String sqlFingerprint,
                                                            RuntimeRewriteResolution runtimeRewriteResolution,
                                                            HetuExecutionUnavailableException exception,
                                                            long start,
                                                            QueryExecutionCacheGovernanceRuntimeService.CacheResolution cacheResolution) {
        logStateChange(
            sqlFingerprint,
            request,
            STATE_PRIMARY_ROUTE_SELECTED,
            STATE_PRIMARY_MODE_CHAIN_FAILED,
            primaryEngine.name(),
            0L,
            QueryExecutionStatus.FAILED.name(),
            null
        );
        QueryRetryStepVO routeFailureStep = buildRecoveryStep(
            primaryEngine.name(),
            0L,
            QueryExecutionStatus.FAILED.name(),
            MARKER_PRIMARY_ROUTE_FAILURE,
            ACTION_CLOSE_PRIMARY_ATTEMPT_CONTEXT
        );
        if (FaultToleranceStrategy.RETRY_THEN_FALLBACK == request.getFaultToleranceStrategy()
            && resolveFallbackEngine(primaryEngine) != null) {
            logStateChange(
                sqlFingerprint,
                request,
                STATE_PRIMARY_MODE_CHAIN_FAILED,
                STATE_FALLBACK_REQUESTED,
                resolveFallbackEngine(primaryEngine).name(),
                0L,
                QueryExecutionStatus.PARTIAL.name(),
                MARKER_PRIMARY_ROUTE_FAILURE
            );
            return logAndReturn(
                buildFallbackResponse(
                    primaryEngine,
                    runtimeRewriteResolution,
                    exception.getMessage(),
                    Collections.singletonList(routeFailureStep),
                    request
                ),
                request,
                start
            );
        }
        logStateChange(
            sqlFingerprint,
            request,
            STATE_PRIMARY_MODE_CHAIN_FAILED,
            STATE_ROUTE_UNAVAILABLE,
            primaryEngine.name(),
            0L,
            QueryExecutionStatus.FAILED.name(),
            MARKER_PRIMARY_ROUTE_FAILURE
        );
        return logAndReturn(
            buildFailureResponse(
                QueryExecutionStatus.FAILED,
                primaryEngine.name(),
                runtimeRewriteResolution,
                0L,
                0L,
                Collections.singletonList(routeFailureStep),
                new QueryErrorDetailVO(
                    ErrorCodeConstants.QUERY_EXECUTION_SYSTEM_ROUTE_UNAVAILABLE,
                    ROUTE_UNAVAILABLE_MESSAGE,
                    exception.getMessage(),
                    true
                ),
                sqlFingerprint,
                "NONE",
                exception.getAttemptedModes(),
                exception.getRouteProfile(),
                exception.getRouteOrder(),
                exception.getRouteEvidenceSource(),
                exception.getRouteVerificationStatus(),
                cacheResolution == null ? null : cacheResolution.getStatus(),
                cacheResolution == null ? null : cacheResolution.buildEvidence(false, null)
            ),
            request,
            start
        );
    }

    private QueryExecuteResponse buildSuccessResponse(QueryExecutionStatus status,
                                                      QueryExecutionStep executionStep,
                                                      RuntimeRewriteResolution runtimeRewriteResolution,
                                                      boolean degraded,
                                                      String degradeReason,
                                                      List<QueryRetryStepVO> retryPath) {
        String actualSql = runtimeRewriteResolution.getActualSql();
        return new QueryExecuteResponse(
            status,
            executionStep.getRows(),
            null,
            new QueryExecutionMetadataVO(
                executionStep.getTargetEngine().name(),
                actualSql,
                executionStep.getElapsedMs(),
                executionStep.getScannedRows(),
                executionStep.isCacheHit(),
                executionStep.isAccelerationApplied(),
                executionStep.getExecutionMode(),
                executionStep.getAttemptedModes(),
                executionStep.getRows() == null ? 0 : executionStep.getRows().size(),
                executionStep.getRouteProfile(),
                executionStep.getRouteOrder(),
                executionStep.getRouteEvidenceSource(),
                executionStep.getRouteVerificationStatus(),
                executionStep.getCacheGovernanceStatus(),
                executionStep.getCacheGovernanceEvidence(),
                runtimeRewriteResolution.getOriginalSql(),
                runtimeRewriteResolution.isRewriteApplied(),
                runtimeRewriteResolution.getRewriteRecordId(),
                runtimeRewriteResolution.getRuntimeBindingId(),
                runtimeRewriteResolution.getRuleVersion(),
                runtimeRewriteResolution.getRuntimeRuleVersion(),
                runtimeRewriteResolution.getRuntimeStatus(),
                runtimeRewriteResolution.getRewriteActivationStatusSnapshot(),
                runtimeRewriteResolution.getRewriteFallbackReason()
            ),
            degraded,
            degradeReason,
            retryPath,
            null,
            buildCommentContext(actualSql),
            buildQueryDateSummary(actualSql),
            buildBindingSummary(actualSql, runtimeRewriteResolution),
            buildLogicalObjectHits(actualSql),
            buildRouteSummary(
                executionStep.getTargetEngine() == null ? null : executionStep.getTargetEngine().name(),
                executionStep.getExecutionMode(),
                executionStep.getRouteProfile(),
                executionStep.getRouteOrder(),
                executionStep.getRouteEvidenceSource(),
                executionStep.getRouteVerificationStatus(),
                degraded,
                degradeReason
            ),
            buildCacheSummary(
                executionStep.isCacheHit(),
                executionStep.getCacheGovernanceStatus(),
                executionStep.getCacheGovernanceEvidence()
            ),
            buildLightweightParseSummary(actualSql, status, null),
            runtimeRewriteResolution.getOriginalSqlFingerprint(),
            CONTRACT_STAGE,
            IMPLEMENTATION_STAGE
        );
    }

    private QueryExecuteResponse buildFailureResponse(QueryExecutionStatus status,
                                                      String targetEngine,
                                                      String actualSql,
                                                      long elapsedMs,
                                                      long scannedRows,
                                                      List<QueryRetryStepVO> retryPath,
                                                      QueryErrorDetailVO errorDetail,
                                                      String sqlFingerprint) {
        return buildFailureResponse(
            status,
            targetEngine,
            RuntimeRewriteResolution.noRewrite(actualSql, sqlFingerprint),
            elapsedMs,
            scannedRows,
            retryPath,
            errorDetail,
            sqlFingerprint,
            "NONE",
            Collections.<String>emptyList(),
            null,
            Collections.<String>emptyList(),
            null,
            null,
            null,
            null
        );
    }

    private QueryExecuteResponse buildFailureResponse(QueryExecutionStatus status,
                                                      String targetEngine,
                                                      String actualSql,
                                                      long elapsedMs,
                                                      long scannedRows,
                                                      List<QueryRetryStepVO> retryPath,
                                                      QueryErrorDetailVO errorDetail,
                                                      String sqlFingerprint,
                                                      RuntimeRewriteResolution runtimeRewriteResolution) {
        return buildFailureResponse(
            status,
            targetEngine,
            runtimeRewriteResolution,
            elapsedMs,
            scannedRows,
            retryPath,
            errorDetail,
            sqlFingerprint,
            "NONE",
            Collections.<String>emptyList(),
            null,
            Collections.<String>emptyList(),
            null,
            null,
            null,
            null
        );
    }

    private QueryExecuteResponse buildFailureResponse(QueryExecutionStatus status,
                                                      String targetEngine,
                                                      RuntimeRewriteResolution runtimeRewriteResolution,
                                                      long elapsedMs,
                                                      long scannedRows,
                                                      List<QueryRetryStepVO> retryPath,
                                                      QueryErrorDetailVO errorDetail,
                                                      String sqlFingerprint) {
        return buildFailureResponse(
            status,
            targetEngine,
            runtimeRewriteResolution,
            elapsedMs,
            scannedRows,
            retryPath,
            errorDetail,
            sqlFingerprint,
            "NONE",
            Collections.<String>emptyList(),
            null,
            Collections.<String>emptyList(),
            null,
            null,
            null,
            null
        );
    }

    private QueryExecuteResponse buildFailureResponse(QueryExecutionStatus status,
                                                      String targetEngine,
                                                      String actualSql,
                                                      long elapsedMs,
                                                      long scannedRows,
                                                      List<QueryRetryStepVO> retryPath,
                                                      QueryErrorDetailVO errorDetail,
                                                      String sqlFingerprint,
                                                      String executionMode,
                                                      List<String> attemptedModes) {
        return buildFailureResponse(
            status,
            targetEngine,
            RuntimeRewriteResolution.noRewrite(actualSql, sqlFingerprint),
            elapsedMs,
            scannedRows,
            retryPath,
            errorDetail,
            sqlFingerprint,
            executionMode,
            attemptedModes,
            null,
            Collections.<String>emptyList(),
            null,
            null,
            null,
            null
        );
    }

    private QueryExecuteResponse buildFailureResponse(QueryExecutionStatus status,
                                                      String targetEngine,
                                                      RuntimeRewriteResolution runtimeRewriteResolution,
                                                      long elapsedMs,
                                                      long scannedRows,
                                                      List<QueryRetryStepVO> retryPath,
                                                      QueryErrorDetailVO errorDetail,
                                                      String sqlFingerprint,
                                                      String executionMode,
                                                      List<String> attemptedModes,
                                                      String routeProfile,
                                                      List<String> routeOrder,
                                                      String routeEvidenceSource,
                                                      String routeVerificationStatus,
                                                      String cacheGovernanceStatus,
                                                      String cacheGovernanceEvidence) {
        String actualSql = runtimeRewriteResolution.getActualSql();
        return new QueryExecuteResponse(
            status,
            Collections.<java.util.Map<String, Object>>emptyList(),
            null,
            new QueryExecutionMetadataVO(
                targetEngine,
                actualSql,
                elapsedMs,
                scannedRows,
                false,
                false,
                executionMode,
                attemptedModes,
                0,
                routeProfile,
                routeOrder,
                routeEvidenceSource,
                routeVerificationStatus,
                cacheGovernanceStatus,
                cacheGovernanceEvidence,
                runtimeRewriteResolution.getOriginalSql(),
                runtimeRewriteResolution.isRewriteApplied(),
                runtimeRewriteResolution.getRewriteRecordId(),
                runtimeRewriteResolution.getRuntimeBindingId(),
                runtimeRewriteResolution.getRuleVersion(),
                runtimeRewriteResolution.getRuntimeRuleVersion(),
                runtimeRewriteResolution.getRuntimeStatus(),
                runtimeRewriteResolution.getRewriteActivationStatusSnapshot(),
                runtimeRewriteResolution.getRewriteFallbackReason()
            ),
            false,
            null,
            retryPath,
            errorDetail,
            buildCommentContext(actualSql),
            buildQueryDateSummary(actualSql),
            buildBindingSummary(actualSql, runtimeRewriteResolution),
            buildLogicalObjectHits(actualSql),
            buildRouteSummary(
                targetEngine,
                executionMode,
                routeProfile,
                routeOrder,
                routeEvidenceSource,
                routeVerificationStatus,
                false,
                null
            ),
            buildCacheSummary(false, cacheGovernanceStatus, cacheGovernanceEvidence),
            buildLightweightParseSummary(actualSql, status, errorDetail),
            runtimeRewriteResolution.getOriginalSqlFingerprint(),
            CONTRACT_STAGE,
            IMPLEMENTATION_STAGE
        );
    }

    private QueryRetryStepVO buildRecoveryStep(String engine,
                                               long elapsedMs,
                                               String resultStatus,
                                               String localRecoveryMarker,
                                               String localRecoveryAction) {
        return new QueryRetryStepVO(
            engine,
            elapsedMs,
            resultStatus,
            localRecoveryMarker,
            localRecoveryAction
        );
    }

    private QueryExecuteResponse logAndReturn(QueryExecuteResponse response,
                                              QueryExecuteRequest request,
                                              long start) {
        long costMs = System.currentTimeMillis() - start;
        LOGGER.info(
            "操作日志 operation={} entity={} tenantId={} requestedDatasource={} faultTolerance={} costMs={} status=END resultStatus={} degraded={}",
            OPERATION,
            response.getSqlFingerprint(),
            request.getTenantId(),
            request.getDatasourceType(),
            request.getFaultToleranceStrategy(),
            costMs,
            response.getStatus(),
            response.isDegraded()
        );
        metricsRecorder.recordResponse(request, response, costMs);
        writeExecutionHistoryRecord(
            request,
            response.getSqlFingerprint(),
            response,
            response.getStatus().name(),
            costMs,
            null
        );
        return response;
    }

    private String requireAuthorizedTenant(String requestTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (contextTenantId == null || contextTenantId.trim().isEmpty()) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "已认证请求上下文缺少 tenantId"
            );
        }
        if (requestTenantId != null && requestTenantId.trim().length() > 0
            && !contextTenantId.equals(requestTenantId.trim())) {
            throw new AccessDeniedException("请求 tenantId 与已认证租户上下文不一致");
        }
        return contextTenantId;
    }

    private void logStart(QueryExecuteRequest request, String sqlFingerprint) {
        LOGGER.info(
            "操作日志 operation={} entity={} tenantId={} requestedDatasource={} faultTolerance={} status=START",
            OPERATION,
            sqlFingerprint,
            request.getTenantId(),
            request.getDatasourceType(),
            request.getFaultToleranceStrategy()
        );
    }

    private void logStateChange(String sqlFingerprint,
                                QueryExecuteRequest request,
                                String fromState,
                                String toState,
                                String engine,
                                long elapsedMs,
                                String resultStatus,
                                String localRecoveryMarker) {
        LOGGER.info(
            "操作日志 operation={} entity={} tenantId={} requestedDatasource={} faultTolerance={} status=STATE_CHANGE from={} to={} engine={} elapsedMs={} resultStatus={} localRecoveryMarker={}",
            OPERATION,
            sqlFingerprint,
            request.getTenantId(),
            request.getDatasourceType(),
            request.getFaultToleranceStrategy(),
            fromState,
            toState,
            engine,
            elapsedMs,
            resultStatus,
            localRecoveryMarker
        );
    }

    private String normalizeSql(String sqlText) {
        String normalized = sqlText == null ? "" : sqlText.trim();
        if (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private DataSourceTypeEnum resolvePrimaryEngine(QueryExecuteRequest request) {
        if (request == null || request.getDatasourceType() == null) {
            return null;
        }
        String datasourceCode = trimToNull(request.getDatasourceCode());
        if (StringUtils.hasText(datasourceCode)) {
            GovernanceJdbcRouteResolveRequest routeRequest = new GovernanceJdbcRouteResolveRequest();
            routeRequest.setTenantId(request.getTenantId());
            routeRequest.setDatasourceCode(datasourceCode);
            routeRequest.setDatasourceType(request.getDatasourceType());
            try {
                GovernanceJdbcRouteResolveResponse response = governanceCapabilityClient.resolveJdbcRoute(routeRequest);
                DataSourceTypeEnum candidate = firstHealthyCandidate(response == null ? null : response.getCandidates());
                if (candidate != null) {
                    return candidate;
                }
            } catch (RuntimeException ignored) {
                // 当前同步基线在治理路由缺失时保留本地默认回退。
            }
        }
        if (DataSourceTypeEnum.AUTO == request.getDatasourceType()) {
            return DataSourceTypeEnum.TRINO;
        }
        if (DataSourceTypeEnum.TRINO == request.getDatasourceType()
            || DataSourceTypeEnum.HETU == request.getDatasourceType()
            || DataSourceTypeEnum.HIVE == request.getDatasourceType()) {
            return request.getDatasourceType();
        }
        return null;
    }

    private DataSourceTypeEnum resolveFallbackEngine(DataSourceTypeEnum primaryEngine) {
        if (DataSourceTypeEnum.TRINO == primaryEngine || DataSourceTypeEnum.HETU == primaryEngine) {
            return DataSourceTypeEnum.HIVE;
        }
        return null;
    }

    private DataSourceTypeEnum firstHealthyCandidate(List<GovernanceJdbcRouteCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        for (GovernanceJdbcRouteCandidate candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            if (candidate.isEnabled() && "HEALTHY".equalsIgnoreCase(trimToNull(candidate.getHealthStatus()))) {
                return parseEngine(candidate.getEngineType());
            }
        }
        for (GovernanceJdbcRouteCandidate candidate : candidates) {
            if (candidate != null && candidate.isEnabled()) {
                return parseEngine(candidate.getEngineType());
            }
        }
        return parseEngine(candidates.get(0).getEngineType());
    }

    private DataSourceTypeEnum parseEngine(String engineType) {
        String normalized = trimToNull(engineType);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        try {
            return DataSourceTypeEnum.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Long resolveTimeoutMs(QueryContextDTO queryContext) {
        if (queryContext == null) {
            return null;
        }
        return queryContext.getTimeoutMs();
    }

    private String resolveTargetEngine(DataSourceTypeEnum primaryEngine, DataSourceTypeEnum requestedEngine) {
        if (primaryEngine != null) {
            return primaryEngine.name();
        }
        return requestedEngine == null ? DataSourceTypeEnum.AUTO.name() : requestedEngine.name();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private RuntimeRewriteResolution resolveRuntimeRewrite(QueryExecuteRequest request,
                                                           String rawSql,
                                                           String effectiveSql,
                                                           String effectiveSqlFingerprint) {
        if (queryExecutionRuntimeRewriteBindingService == null) {
            return RuntimeRewriteResolution.noRewrite(rawSql, effectiveSql, effectiveSqlFingerprint);
        }
        try {
            RuntimeRewriteBindingResolveRequest resolveRequest = new RuntimeRewriteBindingResolveRequest();
            List<LogicalObjectSurface> runtimeMatchObjectRefs =
                SqlSurfaceObjectRefExtractor.extractSurfaceRefs(effectiveSql);
            resolveRequest.setTenantId(request.getTenantId());
            resolveRequest.setSqlFingerprint(effectiveSqlFingerprint);
            resolveRequest.setSqlText(effectiveSql);
            resolveRequest.setDatasourceCode(resolveRuntimeRewriteDatasourceEvidence(request, effectiveSql));
            resolveRequest.setRuntimeMatchObjectRefs(runtimeMatchObjectRefs);
            resolveRequest.setRuntimeMatchObjectNames(
                SqlSurfaceObjectRefExtractor.surfaceObjectNames(runtimeMatchObjectRefs)
            );
            RuntimeRewriteBindingResponse response =
                queryExecutionRuntimeRewriteBindingService.resolveActive(resolveRequest);
            if (response == null || !response.isActive()) {
                return RuntimeRewriteResolution.inactive(
                    rawSql,
                    effectiveSql,
                    effectiveSqlFingerprint,
                    response == null ? "MISSING" : response.getStatus(),
                    response == null ? null : response.getRuntimeSummary()
                );
            }
            if (!request.getTenantId().equals(response.getTenantId())) {
                return RuntimeRewriteResolution.fallback(
                    rawSql,
                    effectiveSql,
                    effectiveSqlFingerprint,
                    response,
                    "TENANT_MISMATCH"
                );
            }
            if (!effectiveSqlFingerprint.equals(response.getSqlFingerprint())) {
                return RuntimeRewriteResolution.fallback(
                    rawSql,
                    effectiveSql,
                    effectiveSqlFingerprint,
                    response,
                    "SQL_FINGERPRINT_MISMATCH"
                );
            }
            String recommendedSql = SqlCatalogQualifierRewriteUtils.rewriteBiViewCatalogQualifier(
                normalizeSql(response.getRecommendedSqlText())
            );
            if (!StringUtils.hasText(recommendedSql)) {
                return RuntimeRewriteResolution.fallback(
                    rawSql,
                    effectiveSql,
                    effectiveSqlFingerprint,
                    response,
                    "RECOMMENDED_SQL_EMPTY"
                );
            }
            ReadonlyQueryAssessment recommendedAssessment = ReadonlyQueryGuard.assess(recommendedSql);
            if (!recommendedAssessment.isReadonly()) {
                return RuntimeRewriteResolution.fallback(
                    rawSql,
                    effectiveSql,
                    effectiveSqlFingerprint,
                    response,
                    "RECOMMENDED_SQL_NOT_READONLY"
                );
            }
            return RuntimeRewriteResolution.applied(
                rawSql,
                effectiveSqlFingerprint,
                recommendedSql,
                response
            );
        } catch (RuntimeException ex) {
            LOGGER.warn(
                "操作日志 operation={} entity={} tenantId={} status=RUNTIME_REWRITE_FALLBACK reason={}",
                OPERATION,
                effectiveSqlFingerprint,
                request.getTenantId(),
                ex.getMessage()
            );
            return RuntimeRewriteResolution.fallback(
                rawSql,
                effectiveSql,
                effectiveSqlFingerprint,
                null,
                "RUNTIME_REWRITE_RESOLVE_FAILED"
            );
        }
    }

    private String resolveRuntimeRewriteDatasourceEvidence(QueryExecuteRequest request, String originalSql) {
        Map<String, String> commentContext = buildCommentContext(originalSql);
        return firstText(
            commentContext == null ? null : commentContext.get("datasource"),
            commentContext == null ? null : commentContext.get("datasource_code"),
            request.getQueryContext() == null ? null : request.getQueryContext().getDatabaseName()
        );
    }

    private void writeExecutionHistoryRecord(QueryExecuteRequest request,
                                             String sqlFingerprint,
                                             QueryExecuteResponse response,
                                             String resultStatus,
                                             long elapsedMs,
                                             String failureReason) {
        writeExecutionHistoryRecord(
            request,
            sqlFingerprint,
            response,
            resultStatus,
            elapsedMs,
            failureReason,
            null
        );
    }

    private void writeExecutionHistoryRecord(QueryExecuteRequest request,
                                             String sqlFingerprint,
                                             QueryExecuteResponse response,
                                             String resultStatus,
                                             long elapsedMs,
                                             String failureReason,
                                             RuntimeRewriteResolution fallbackRewriteResolution) {
        AccessAuditContract accessAuditContract = AccessAuditContract.capture();
        QueryExecutionMetadataVO metadata = response == null ? null : response.getMetadata();
        String originalSql = metadata == null
            ? fallbackRewriteResolution == null
                ? normalizeSql(request.getSqlText())
                : fallbackRewriteResolution.getOriginalSql()
            : metadata.getOriginalSql();
        String actualSql = metadata == null
            ? fallbackRewriteResolution == null ? originalSql : fallbackRewriteResolution.getActualSql()
            : metadata.getActualSql();
        String effectiveFingerprint = StringUtils.hasText(sqlFingerprint)
            ? sqlFingerprint
            : fallbackRewriteResolution == null
                ? SqlFingerprintUtils.fingerprint(originalSql)
                : fallbackRewriteResolution.getOriginalSqlFingerprint();
        Object bindingSummary = response == null
            ? fallbackRewriteResolution == null ? null : buildBindingSummary(actualSql, fallbackRewriteResolution)
            : response.getBindingSummary();
        String stableKey = stableHash(
            RequestContext.getRequestId(),
            RequestContext.getTraceId(),
            request.getTenantId(),
            effectiveFingerprint
        );
        Instant finishedAt = Instant.now();
        Instant startedAt = finishedAt.minusMillis(Math.max(0L, elapsedMs));

        GovernanceQueryExecutionHistoryWriteRequest historyRequest =
            new GovernanceQueryExecutionHistoryWriteRequest();
        historyRequest.setConfigSnapshotId("cfg-qe-" + stableKey);
        historyRequest.setResultId("result-qe-" + stableKey);
        historyRequest.setHistoryId("history-qe-" + stableKey);
        historyRequest.setTenantId(request.getTenantId());
        historyRequest.setSqlText(request.getSqlText());
        historyRequest.setSqlTemplate(originalSql);
        historyRequest.setBoundSql(actualSql);
        historyRequest.setSqlFingerprint(effectiveFingerprint);
        historyRequest.setDatasourceCode(resolveDatasourceCode(request, response));
        historyRequest.setDatasourceType(request.getDatasourceType() == null ? null : request.getDatasourceType().name());
        historyRequest.setHistoryType("QUERY_EXECUTION");
        historyRequest.setResultStatus(resultStatus);
        historyRequest.setTargetEngine(response == null || response.getMetadata() == null
            ? null
            : response.getMetadata().getTargetEngine());
        historyRequest.setReturnedRowCount(response == null || response.getMetadata() == null
            ? Long.valueOf(0L)
            : Long.valueOf(response.getMetadata().getRowCount()));
        historyRequest.setCacheHit(response == null || response.getMetadata() == null
            ? Boolean.FALSE
            : Boolean.valueOf(response.getMetadata().isCacheHit()));
        historyRequest.setRewriteApplied(metadata == null
            ? Boolean.valueOf(fallbackRewriteResolution != null && fallbackRewriteResolution.isRewriteApplied())
            : Boolean.valueOf(metadata.isRewriteApplied()));
        historyRequest.setAccelerationApplied(response == null || response.getMetadata() == null
            ? Boolean.FALSE
            : Boolean.valueOf(response.getMetadata().isAccelerationApplied()));
        historyRequest.setAccessChannel(accessAuditContract.getAccessChannel().name());
        historyRequest.setCommentContext(toJson(response == null ? null : response.getCommentContext()));
        historyRequest.setQueryDateSummary(toJson(response == null ? null : response.getQueryDateSummary()));
        historyRequest.setBindingSummary(toJson(bindingSummary));
        historyRequest.setRewriteRecordId(metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getRewriteRecordId()
            : metadata.getRewriteRecordId());
        historyRequest.setRuntimeBindingId(metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getRuntimeBindingId()
            : metadata.getRuntimeBindingId());
        historyRequest.setRewriteRuleVersion(metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getRuleVersion()
            : metadata.getRuleVersion());
        historyRequest.setRuntimeRuleVersion(metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getRuntimeRuleVersion()
            : metadata.getRuntimeRuleVersion());
        historyRequest.setRuntimeRewriteStatus(metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getRuntimeStatus()
            : metadata.getRuntimeRewriteStatus());
        historyRequest.setRewriteActivationStatusSnapshot(metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getRewriteActivationStatusSnapshot()
            : metadata.getRewriteActivationStatusSnapshot());
        historyRequest.setRewriteFallbackReason(metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getRewriteFallbackReason()
            : metadata.getRewriteFallbackReason());
        historyRequest.setLogicalObjectHits(toJson(response == null ? null : response.getLogicalObjectHits()));
        historyRequest.setRouteSummary(toJson(response == null ? null : response.getRouteSummary()));
        historyRequest.setCacheSummary(toJson(response == null ? null : response.getCacheSummary()));
        historyRequest.setQueryContext(toJson(buildHistoryQueryContext(
            request,
            response,
            resultStatus,
            failureReason,
            accessAuditContract,
            fallbackRewriteResolution
        )));
        historyRequest.setTraceId(RequestContext.getTraceId());
        historyRequest.setRequestId(RequestContext.getRequestId());
        historyRequest.setSagaId("query-execution-" + stableKey);
        historyRequest.setSubmittedBy(RequestContext.getUserId());
        historyRequest.setStartedAt(startedAt.toString());
        historyRequest.setFinishedAt(finishedAt.toString());
        historyRequest.setElapsedMs(Long.valueOf(elapsedMs));
        historyRequest.setErrorCode(response == null || response.getError() == null
            ? null
            : Integer.valueOf(response.getError().getCode()));
        historyRequest.setErrorMessage(firstText(
            failureReason,
            response == null || response.getError() == null ? null : response.getError().getMessage()
        ));
        submitQueryExecutionHistoryWrite(historyRequest, effectiveFingerprint);
    }

    private void submitQueryExecutionHistoryWrite(final GovernanceQueryExecutionHistoryWriteRequest historyRequest,
                                                  final String sqlFingerprint) {
        Runnable historyWriteTask = new Runnable() {
            @Override
            public void run() {
                try {
                    governanceCapabilityClient.writeQueryExecutionHistory(historyRequest);
                } catch (RuntimeException ex) {
                    LOGGER.warn(
                        "操作日志 operation={} entity={} tenantId={} status=HISTORY_WRITE_ASYNC_FAILED reason={}",
                        OPERATION,
                        sqlFingerprint,
                        historyRequest == null ? null : historyRequest.getTenantId(),
                        ex.getMessage(),
                        ex
                    );
                }
            }
        };
        try {
            queryHistoryWriteExecutor.execute(historyWriteTask);
        } catch (RuntimeException ex) {
            LOGGER.warn(
                "操作日志 operation={} entity={} tenantId={} status=HISTORY_WRITE_SUBMIT_FAILED reason={}",
                OPERATION,
                sqlFingerprint,
                historyRequest == null ? null : historyRequest.getTenantId(),
                ex.getMessage(),
                ex
            );
        }
    }

    private static Executor directExecutor() {
        return new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
    }

    private Map<String, Object> buildHistoryQueryContext(QueryExecuteRequest request,
                                                         QueryExecuteResponse response,
                                                         String resultStatus,
                                                         String failureReason,
                                                         AccessAuditContract accessAuditContract,
                                                         RuntimeRewriteResolution fallbackRewriteResolution) {
        QueryExecutionMetadataVO metadata = response == null ? null : response.getMetadata();
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("serviceCode", ServiceCodeConstants.QUERY_EXECUTION);
        payload.put("operationCode", OPERATION);
        payload.put("resourceType", RESOURCE_TYPE_QUERY);
        payload.put("accessChannel", accessAuditContract.getAccessChannel().name());
        payload.put("authSource", accessAuditContract.getAuthSource());
        payload.put("tenantId", request.getTenantId());
        payload.put("datasourceType", request.getDatasourceType() == null ? null : request.getDatasourceType().name());
        payload.put("faultToleranceStrategy", request.getFaultToleranceStrategy() == null
            ? null
            : request.getFaultToleranceStrategy().name());
        payload.put("accelerationPreference", request.getAccelerationPreference() == null
            ? null
            : request.getAccelerationPreference().name());
        payload.put("requestContext", buildRequestContextPayload(request.getQueryContext()));
        payload.put("resultStatus", resultStatus);
        payload.put("targetEngine", response == null || response.getMetadata() == null
            ? null
            : response.getMetadata().getTargetEngine());
        payload.put("returnedRowCount", response == null || response.getMetadata() == null
            ? Long.valueOf(0L)
            : Long.valueOf(response.getMetadata().getRowCount()));
        payload.put("scannedRows", response == null || response.getMetadata() == null
            ? Long.valueOf(0L)
            : Long.valueOf(response.getMetadata().getScannedRows()));
        payload.put("elapsedMs", response == null || response.getMetadata() == null
            ? null
            : Long.valueOf(response.getMetadata().getElapsedMs()));
        payload.put("degraded", Boolean.valueOf(response != null && response.isDegraded()));
        payload.put("degradeReason", response == null ? null : response.getDegradeReason());
        payload.put("retryPathSize", response == null || response.getRetryPath() == null
            ? Integer.valueOf(0)
            : Integer.valueOf(response.getRetryPath().size()));
        payload.put("commentContext", response == null ? null : response.getCommentContext());
        payload.put("queryDateSummary", response == null ? null : response.getQueryDateSummary());
        payload.put("bindingSummary", response == null ? null : response.getBindingSummary());
        payload.put("logicalObjectHits", response == null ? null : response.getLogicalObjectHits());
        payload.put("routeSummary", response == null ? null : response.getRouteSummary());
        payload.put("cacheSummary", response == null ? null : response.getCacheSummary());
        payload.put("rewriteApplied", metadata == null
            ? Boolean.valueOf(fallbackRewriteResolution != null && fallbackRewriteResolution.isRewriteApplied())
            : Boolean.valueOf(metadata.isRewriteApplied()));
        payload.put("rewriteRecordId", metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getRewriteRecordId()
            : metadata.getRewriteRecordId());
        payload.put("runtimeBindingId", metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getRuntimeBindingId()
            : metadata.getRuntimeBindingId());
        payload.put("ruleVersion", metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getRuleVersion()
            : metadata.getRuleVersion());
        payload.put("runtimeRuleVersion", metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getRuntimeRuleVersion()
            : metadata.getRuntimeRuleVersion());
        payload.put("runtimeRewriteStatus", metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getRuntimeStatus()
            : metadata.getRuntimeRewriteStatus());
        payload.put("rewriteActivationStatusSnapshot", metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getRewriteActivationStatusSnapshot()
            : metadata.getRewriteActivationStatusSnapshot());
        payload.put("rewriteFallbackReason", metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getRewriteFallbackReason()
            : metadata.getRewriteFallbackReason());
        payload.put("originalSqlFingerprint", response == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getOriginalSqlFingerprint()
            : response.getSqlFingerprint());
        payload.put("actualSqlFingerprint", metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getActualSqlFingerprint()
            : SqlFingerprintUtils.fingerprint(metadata.getActualSql()));
        if (response != null && response.getQueryDateSummary() != null) {
            payload.put("queryDateStart", response.getQueryDateSummary().get("queryDateStart"));
            payload.put("queryDateEnd", response.getQueryDateSummary().get("queryDateEnd"));
            payload.put("queryDateStatus", response.getQueryDateSummary().get("queryDateStatus"));
        }
        payload.put("failureReason", failureReason);
        return payload;
    }

    private Map<String, Object> buildRequestContextPayload(QueryContextDTO queryContext) {
        if (queryContext == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("databaseName", queryContext.getDatabaseName());
        payload.put("schemaVersion", queryContext.getSchemaVersion());
        payload.put("timeoutMs", queryContext.getTimeoutMs());
        payload.put("sessionVariables", queryContext.getSessionVariables());
        return payload;
    }

    private String resolveDatasourceCode(QueryExecuteRequest request, QueryExecuteResponse response) {
        Map<String, String> commentContext = response == null ? null : response.getCommentContext();
        return firstText(
            commentContext == null ? null : commentContext.get("datasource"),
            commentContext == null ? null : commentContext.get("datasource_code"),
            request.getQueryContext() == null ? null : request.getQueryContext().getDatabaseName(),
            request.getDatasourceType() == null ? null : request.getDatasourceType().name()
        );
    }

    private String toJson(Object payload) {
        return payload == null ? null : JsonUtils.toJson(payload);
    }

    private Map<String, String> buildCommentContext(String sqlText) {
        return JdbcAgentSqlCommentParser.parseLeadingComments(sqlText);
    }

    private Map<String, Object> buildQueryDateSummary(String sqlText) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        if (!StringUtils.hasText(sqlText)) {
            summary.put("queryDateFields", Collections.<String>emptyList());
            summary.put("queryDateStatus", "UNRESOLVED");
            return summary;
        }
        List<LocalDate> dates = new ArrayList<LocalDate>();
        Matcher matcher = ISO_DATE_PATTERN.matcher(sqlText);
        while (matcher.find()) {
            LocalDate parsed = tryParseDate(matcher.group(1));
            if (parsed != null) {
                dates.add(parsed);
            }
        }
        List<String> fields = detectQueryDateFields(sqlText);
        summary.put("queryDateFields", fields);
        if (!dates.isEmpty()) {
            dates.sort(Comparator.naturalOrder());
            summary.put("queryDateStart", dates.get(0).format(ISO_DATE));
            summary.put("queryDateEnd", dates.get(dates.size() - 1).format(ISO_DATE));
            summary.put("queryDateStatus", "RESOLVED");
            return summary;
        }
        summary.put("queryDateStatus", fields.isEmpty() ? "UNRESOLVED" : "PARTIAL");
        return summary;
    }

    private Map<String, Object> buildBindingSummary(String sqlText, String sqlFingerprint) {
        return buildBindingSummary(
            sqlText,
            RuntimeRewriteResolution.noRewrite(sqlText, sqlFingerprint)
        );
    }

    private Map<String, Object> buildBindingSummary(String sqlText,
                                                    RuntimeRewriteResolution runtimeRewriteResolution) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        int namedBindings = countMatches(NAMED_BINDING_PATTERN, sqlText);
        int positionalBindings = countMatches(POSITIONAL_BINDING_PATTERN, sqlText);
        boolean parameterized = namedBindings > 0 || positionalBindings > 0;
        summary.put("parameterizedSqlFlag", Boolean.valueOf(parameterized));
        if (namedBindings > 0) {
            summary.put("bindingMode", "NAMED");
        } else if (positionalBindings > 0) {
            summary.put("bindingMode", "POSITIONAL");
        } else {
            summary.put("bindingMode", "NONE");
        }
        summary.put("bindingRenderStatus", parameterized ? "PARTIAL" : "SUCCESS");
        summary.put("bindingParameterCount", Integer.valueOf(namedBindings + positionalBindings));
        summary.put("sqlTemplateFingerprint", runtimeRewriteResolution.getOriginalSqlFingerprint());
        summary.put("boundSqlFingerprint", parameterized ? null : runtimeRewriteResolution.getActualSqlFingerprint());
        summary.put("actualSqlFingerprint", runtimeRewriteResolution.getActualSqlFingerprint());
        summary.put("rewriteApplied", Boolean.valueOf(runtimeRewriteResolution.isRewriteApplied()));
        summary.put("runtimeRewriteStatus", runtimeRewriteResolution.getRuntimeStatus());
        summary.put("rewriteRecordId", runtimeRewriteResolution.getRewriteRecordId());
        summary.put("runtimeBindingId", runtimeRewriteResolution.getRuntimeBindingId());
        summary.put("ruleVersion", runtimeRewriteResolution.getRuleVersion());
        summary.put("runtimeRuleVersion", runtimeRewriteResolution.getRuntimeRuleVersion());
        summary.put("rewriteActivationStatusSnapshot", runtimeRewriteResolution.getRewriteActivationStatusSnapshot());
        if (StringUtils.hasText(runtimeRewriteResolution.getRewriteFallbackReason())) {
            summary.put("rewriteFallbackReason", runtimeRewriteResolution.getRewriteFallbackReason());
        }
        return summary;
    }

    private List<LogicalObjectSurface> buildLogicalObjectHits(String sqlText) {
        if (!StringUtils.hasText(sqlText)) {
            return Collections.emptyList();
        }
        List<LogicalObjectSurface> hits = new ArrayList<LogicalObjectSurface>();
        List<String> seenKeys = new ArrayList<String>();
        Matcher matcher = LOGICAL_OBJECT_PATTERN.matcher(sqlText);
        while (matcher.find()) {
            String rawReference = sanitizeObjectReference(matcher.group(1));
            if (!StringUtils.hasText(rawReference)) {
                continue;
            }
            LogicalObjectSurface hit = toLogicalObjectSurface(rawReference);
            if (hit.getObjectKey() == null || seenKeys.contains(hit.getObjectKey())) {
                continue;
            }
            seenKeys.add(hit.getObjectKey());
            hits.add(hit);
        }
        return hits;
    }

    private Map<String, Object> buildRouteSummary(String selectedEngine,
                                                  String executionMode,
                                                  String routeProfile,
                                                  List<String> routeOrder,
                                                  String routeEvidenceSource,
                                                  String routeVerificationStatus,
                                                  boolean degraded,
                                                  String degradeReason) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("selectedEngine", selectedEngine);
        summary.put("executionMode", executionMode);
        summary.put("routeProfile", routeProfile);
        summary.put("routeOrder", routeOrder == null ? Collections.<String>emptyList() : routeOrder);
        summary.put("routeEvidenceSource", routeEvidenceSource);
        summary.put("routeVerificationStatus", routeVerificationStatus);
        summary.put("degraded", Boolean.valueOf(degraded));
        if (StringUtils.hasText(degradeReason)) {
            summary.put("degradeReason", degradeReason);
        }
        return summary;
    }

    private Map<String, Object> buildCacheSummary(boolean cacheHit,
                                                  String cacheGovernanceStatus,
                                                  String cacheGovernanceEvidence) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("cacheHit", Boolean.valueOf(cacheHit));
        summary.put("cacheGovernanceStatus", cacheGovernanceStatus);
        summary.put("cacheGovernanceEvidence", cacheGovernanceEvidence);
        return summary;
    }

    private Map<String, Object> buildLightweightParseSummary(String sqlText,
                                                             QueryExecutionStatus status,
                                                             QueryErrorDetailVO errorDetail) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        String normalized = sqlText == null ? "" : sqlText.trim();
        ReadonlyQueryAssessment assessment = ReadonlyQueryGuard.assess(normalized);
        String sqlType = resolveSqlType(normalized);
        List<String> riskTags = detectRiskTags(normalized);
        List<String> rewriteCandidates = detectRewriteCandidates(riskTags, buildQueryDateSummary(normalized));
        List<String> issueCodes = new ArrayList<String>();
        if (!assessment.isReadonly()) {
            issueCodes.add("NON_READONLY_STATEMENT");
        }
        if (errorDetail != null && errorDetail.getCode() == ErrorCodeConstants.QUERY_EXECUTION_RISK_REJECTED
            && !issueCodes.contains("NON_READONLY_STATEMENT")) {
            issueCodes.add("NON_READONLY_STATEMENT");
        }
        summary.put("syntaxStatus", issueCodes.isEmpty() ? "VALID" : "INVALID");
        summary.put("sqlType", sqlType);
        summary.put("readonly", Boolean.valueOf(assessment.isReadonly()));
        summary.put("complexityLevel", resolveComplexityLevel(riskTags));
        summary.put("riskTags", riskTags);
        summary.put("rewriteCandidates", rewriteCandidates);
        summary.put("issueCodes", issueCodes);
        summary.put("issueCount", Integer.valueOf(issueCodes.size()));
        summary.put("resultStatus", status == null ? null : status.name());
        return summary;
    }

    private List<String> detectQueryDateFields(String sqlText) {
        if (!StringUtils.hasText(sqlText)) {
            return Collections.emptyList();
        }
        String lower = sqlText.toLowerCase(Locale.ROOT);
        List<String> hits = new ArrayList<String>();
        if (lower.contains("query_date")) {
            hits.add("query_date");
        }
        if (lower.contains("biz_date")) {
            hits.add("biz_date");
        }
        if (lower.contains(" dt ") || lower.contains(".dt") || lower.contains("dt=")) {
            hits.add("dt");
        }
        if (hits.isEmpty() && (lower.contains(" date ") || lower.contains(".date") || lower.contains("date="))) {
            hits.add("date");
        }
        return hits;
    }

    private LocalDate tryParseDate(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return null;
        }
        try {
            return LocalDate.parse(candidate, ISO_DATE);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private int countMatches(Pattern pattern, String sqlText) {
        if (pattern == null || !StringUtils.hasText(sqlText)) {
            return 0;
        }
        int count = 0;
        Matcher matcher = pattern.matcher(sqlText);
        while (matcher.find()) {
            count += 1;
        }
        return count;
    }

    private String sanitizeObjectReference(String rawReference) {
        if (!StringUtils.hasText(rawReference)) {
            return null;
        }
        String sanitized = rawReference.trim();
        while (sanitized.endsWith(",") || sanitized.endsWith(")") || sanitized.endsWith(";")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1).trim();
        }
        return sanitized;
    }

    private LogicalObjectSurface toLogicalObjectSurface(String objectReference) {
        LogicalObjectType objectType = resolveLogicalObjectType(objectReference);
        String[] parts = objectReference.split("\\.");
        String objectName = parts.length == 0 ? objectReference : parts[parts.length - 1];
        LogicalObjectSurface surface = new LogicalObjectSurface();
        surface.setObjectType(objectType.name());
        surface.setObjectName(objectName);
        surface.setObjectKey(LogicalObjectRef.buildObjectKey(objectType, objectName));
        if (parts.length >= 3) {
            surface.setCatalogName(parts[0]);
            surface.setSchemaName(parts[1]);
        } else if (parts.length == 2) {
            surface.setSchemaName(parts[0]);
        }
        surface.setMatchSource("SQL_TOKEN");
        surface.setResolved(Boolean.TRUE);
        surface.setMappedPhysicalTargets(Collections.<String>emptyList());
        return surface;
    }

    private LogicalObjectType resolveLogicalObjectType(String objectReference) {
        String lower = objectReference == null ? "" : objectReference.toLowerCase(Locale.ROOT);
        if (lower.startsWith("business_view")
            || lower.contains(".business_view.")
            || lower.endsWith("_logic")
            || lower.contains("customer_360")) {
            return LogicalObjectType.BUSINESS_VIEW;
        }
        if (lower.contains("vw_") || lower.endsWith("_view") || lower.contains(".view.")) {
            return LogicalObjectType.DB_VIEW;
        }
        return LogicalObjectType.TABLE;
    }

    private String resolveSqlType(String sqlText) {
        if (!StringUtils.hasText(sqlText)) {
            return "UNKNOWN";
        }
        String upper = stripLeadingComments(sqlText).toUpperCase(Locale.ROOT);
        if (upper.startsWith("EXPLAIN")) {
            return "EXPLAIN";
        }
        if (upper.startsWith("WITH")) {
            return "WITH";
        }
        if (upper.startsWith("SELECT")) {
            return "SELECT";
        }
        if (upper.startsWith("INSERT")) {
            return "INSERT";
        }
        if (upper.startsWith("UPDATE")) {
            return "UPDATE";
        }
        if (upper.startsWith("DELETE")) {
            return "DELETE";
        }
        return "UNKNOWN";
    }

    private String stripLeadingComments(String sqlText) {
        if (!StringUtils.hasText(sqlText)) {
            return "";
        }
        String[] lines = sqlText.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        StringBuilder builder = new StringBuilder();
        boolean copying = false;
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (!copying && trimmed.startsWith("--")) {
                continue;
            }
            copying = true;
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(trimmed);
        }
        return builder.toString().trim();
    }

    private List<String> detectRiskTags(String sqlText) {
        if (!StringUtils.hasText(sqlText)) {
            return Collections.emptyList();
        }
        String lower = sqlText.toLowerCase(Locale.ROOT);
        List<String> riskTags = new ArrayList<String>();
        if (lower.contains("select *")) {
            riskTags.add("SELECT_STAR");
        }
        if (lower.contains(" join ")) {
            riskTags.add("JOIN");
        }
        if (lower.contains(" group by ")) {
            riskTags.add("AGGREGATION");
        }
        if (lower.contains(" over ")) {
            riskTags.add("WINDOW");
        }
        if (lower.contains(" limit ")) {
            riskTags.add("LIMIT");
        }
        return riskTags;
    }

    private List<String> detectRewriteCandidates(List<String> riskTags, Map<String, Object> queryDateSummary) {
        List<String> rewriteCandidates = new ArrayList<String>();
        if (riskTags.contains("SELECT_STAR")) {
            rewriteCandidates.add("NARROW_SELECT_COLUMNS");
        }
        if (riskTags.contains("JOIN")) {
            rewriteCandidates.add("VALIDATE_JOIN_FILTERS");
        }
        Object queryDateStatus = queryDateSummary == null ? null : queryDateSummary.get("queryDateStatus");
        if ("PARTIAL".equals(queryDateStatus)) {
            rewriteCandidates.add("RESOLVE_QUERY_DATE_BINDINGS");
        }
        return rewriteCandidates;
    }

    private String resolveComplexityLevel(List<String> riskTags) {
        if (riskTags == null || riskTags.isEmpty()) {
            return "SIMPLE";
        }
        if (riskTags.size() >= 3 || riskTags.contains("WINDOW")) {
            return "COMPLEX";
        }
        return "MODERATE";
    }

    private ActivatedAccelerationBinding resolveActivatedAccelerationBinding(QueryExecuteRequest request,
                                                                           String sqlFingerprint,
                                                                           DataSourceTypeEnum primaryEngine) {
        if (request.getAccelerationPreference() != com.company.queryexecution.domain.query.AccelerationPreference.PREFER_ACCELERATED) {
            return null;
        }
        if (primaryEngine == null) {
            return null;
        }
        return queryExecutionAccelerationRuntimeService.resolveActiveBinding(
            request.getTenantId(),
            sqlFingerprint,
            primaryEngine.name()
        );
    }

    private QueryExecuteRequest normalizeAccelerationRequest(QueryExecuteRequest request,
                                                            boolean accelerationAllowed,
                                                            DataSourceTypeEnum primaryEngine,
                                                            String actualSql) {
        QueryExecuteRequest normalized = new QueryExecuteRequest();
        normalized.setSqlText(actualSql);
        normalized.setTenantId(request.getTenantId());
        normalized.setDatasourceType(primaryEngine == null ? request.getDatasourceType() : primaryEngine);
        normalized.setDatasourceCode(request.getDatasourceCode());
        normalized.setQueryContext(request.getQueryContext());
        normalized.setFaultToleranceStrategy(request.getFaultToleranceStrategy());
        normalized.setAccelerationPreference(accelerationAllowed
            ? request.getAccelerationPreference()
            : com.company.queryexecution.domain.query.AccelerationPreference.NONE);
        return normalized;
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String stableHash(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (parts != null) {
                for (String part : parts) {
                    digest.update((part == null ? "" : part).getBytes(StandardCharsets.UTF_8));
                    digest.update((byte) '|');
                }
            }
            byte[] hash = digest.digest();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 12 && i < hash.length; i += 1) {
                builder.append(String.format("%02x", Integer.valueOf(hash[i] & 0xff)));
            }
            return builder.toString();
        } catch (Exception ex) {
            return String.valueOf(Math.abs(Arrays.hashCode(parts)));
        }
    }

    private static final class RuntimeRewriteResolution {
        private final String originalSql;
        private final String originalSqlFingerprint;
        private final String actualSql;
        private final String actualSqlFingerprint;
        private final boolean rewriteApplied;
        private final String runtimeStatus;
        private final String rewriteRecordId;
        private final String runtimeBindingId;
        private final Long ruleVersion;
        private final String runtimeRuleVersion;
        private final String rewriteActivationStatusSnapshot;
        private final String rewriteFallbackReason;

        private RuntimeRewriteResolution(String originalSql,
                                         String originalSqlFingerprint,
                                         String actualSql,
                                         String actualSqlFingerprint,
                                         boolean rewriteApplied,
                                         String runtimeStatus,
                                         String rewriteRecordId,
                                         String runtimeBindingId,
                                         Long ruleVersion,
                                         String runtimeRuleVersion,
                                         String rewriteActivationStatusSnapshot,
                                         String rewriteFallbackReason) {
            this.originalSql = originalSql;
            this.originalSqlFingerprint = originalSqlFingerprint;
            this.actualSql = actualSql;
            this.actualSqlFingerprint = actualSqlFingerprint;
            this.rewriteApplied = rewriteApplied;
            this.runtimeStatus = runtimeStatus;
            this.rewriteRecordId = rewriteRecordId;
            this.runtimeBindingId = runtimeBindingId;
            this.ruleVersion = ruleVersion;
            this.runtimeRuleVersion = runtimeRuleVersion;
            this.rewriteActivationStatusSnapshot = rewriteActivationStatusSnapshot;
            this.rewriteFallbackReason = rewriteFallbackReason;
        }

        static RuntimeRewriteResolution noRewrite(String originalSql, String originalSqlFingerprint) {
            return noRewrite(originalSql, originalSql, originalSqlFingerprint);
        }

        static RuntimeRewriteResolution noRewrite(String originalSql, String actualSql, String originalSqlFingerprint) {
            return new RuntimeRewriteResolution(
                originalSql,
                originalSqlFingerprint,
                actualSql,
                SqlFingerprintUtils.fingerprint(actualSql),
                false,
                "NOT_LOOKED_UP",
                null,
                null,
                null,
                null,
                "INACTIVE",
                null
            );
        }

        static RuntimeRewriteResolution inactive(String originalSql,
                                                 String actualSql,
                                                 String originalSqlFingerprint,
                                                 String runtimeStatus,
                                                 String summary) {
            return new RuntimeRewriteResolution(
                originalSql,
                originalSqlFingerprint,
                actualSql,
                SqlFingerprintUtils.fingerprint(actualSql),
                false,
                StringUtils.hasText(runtimeStatus) ? runtimeStatus : "MISSING",
                null,
                null,
                null,
                null,
                toRewriteActivationStatusSnapshot(runtimeStatus, null),
                summary
            );
        }

        static RuntimeRewriteResolution applied(String originalSql,
                                                String originalSqlFingerprint,
                                                String recommendedSql,
                                                RuntimeRewriteBindingResponse response) {
            return new RuntimeRewriteResolution(
                originalSql,
                originalSqlFingerprint,
                recommendedSql,
                SqlFingerprintUtils.fingerprint(recommendedSql),
                true,
                response.getStatus(),
                response.getRewriteRecordId(),
                response.getRuntimeBindingId(),
                response.getRuleVersion(),
                response.getRuntimeRuleVersion(),
                toRewriteActivationStatusSnapshot(response.getStatus(), response.getRewriteRecordId()),
                null
            );
        }

        static RuntimeRewriteResolution fallback(String originalSql,
                                                 String originalSqlFingerprint,
                                                 RuntimeRewriteBindingResponse response,
                                                 String reason) {
            return fallback(originalSql, originalSql, originalSqlFingerprint, response, reason);
        }

        static RuntimeRewriteResolution fallback(String originalSql,
                                                 String actualSql,
                                                 String originalSqlFingerprint,
                                                 RuntimeRewriteBindingResponse response,
                                                 String reason) {
            return new RuntimeRewriteResolution(
                originalSql,
                originalSqlFingerprint,
                actualSql,
                SqlFingerprintUtils.fingerprint(actualSql),
                false,
                response == null ? "LOOKUP_FAILED" : response.getStatus(),
                response == null ? null : response.getRewriteRecordId(),
                response == null ? null : response.getRuntimeBindingId(),
                response == null ? null : response.getRuleVersion(),
                response == null ? null : response.getRuntimeRuleVersion(),
                response == null ? "UNKNOWN" : toRewriteActivationStatusSnapshot(response.getStatus(), response.getRewriteRecordId()),
                reason
            );
        }

        private static String toRewriteActivationStatusSnapshot(String runtimeStatus, String rewriteRecordId) {
            if ("ACTIVE".equals(runtimeStatus)) {
                return "ACTIVE";
            }
            if ("PAUSED".equals(runtimeStatus)) {
                return runtimeStatus;
            }
            if (StringUtils.hasText(rewriteRecordId)) {
                return "UNKNOWN";
            }
            return "INACTIVE";
        }

        String getOriginalSql() { return originalSql; }
        String getOriginalSqlFingerprint() { return originalSqlFingerprint; }
        String getActualSql() { return actualSql; }
        String getActualSqlFingerprint() { return actualSqlFingerprint; }
        boolean isRewriteApplied() { return rewriteApplied; }
        String getRuntimeStatus() { return runtimeStatus; }
        String getRewriteRecordId() { return rewriteRecordId; }
        String getRuntimeBindingId() { return runtimeBindingId; }
        Long getRuleVersion() { return ruleVersion; }
        String getRuntimeRuleVersion() { return runtimeRuleVersion; }
        String getRewriteActivationStatusSnapshot() { return rewriteActivationStatusSnapshot; }
        String getRewriteFallbackReason() { return rewriteFallbackReason; }
    }
}
