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
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteCandidate;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveResponse;
import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import com.company.sqlforge.common.logicalobject.SqlSurfaceObjectRefExtractor;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResolveRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResponse;
import com.company.sqlforge.common.utils.SqlCatalogQualifierRewriteUtils;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 在保留公共 HTTP 契约的同时执行最小同步查询闭环。
 */
@Service
public class QueryExecutionApplicationService extends QueryExecutionApplicationServiceSupport {

    @Autowired
    public QueryExecutionApplicationService(QueryExecutionAdapter queryExecutionAdapter,
                                            GovernanceCapabilityClient governanceCapabilityClient,
                                            QueryExecutionMetricsRecorder metricsRecorder,
                                            QueryExecutionAccelerationRuntimeService queryExecutionAccelerationRuntimeService,
                                            QueryExecutionCacheGovernanceRuntimeService queryExecutionCacheGovernanceRuntimeService,
                                            QueryExecutionRuntimeRewriteBindingService queryExecutionRuntimeRewriteBindingService,
                                            QueryExecutionRewriteProperties rewriteProperties,
                                            @Qualifier("tenantAwareTaskExecutor") Executor queryHistoryWriteExecutor) {
        super(
            queryExecutionAdapter,
            governanceCapabilityClient,
            metricsRecorder,
            queryExecutionAccelerationRuntimeService,
            queryExecutionCacheGovernanceRuntimeService,
            queryExecutionRuntimeRewriteBindingService,
            rewriteProperties,
            queryHistoryWriteExecutor
        );
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
                runtimeRewriteResolution
            );
            QueryExecutionStep primaryStep;
            try {
                primaryStep = queryExecutionAdapter.execute(primaryEngine, actualSql, executionRequest, false);
            } catch (HetuExecutionUnavailableException ex) {
                if (runtimeRewriteResolution.isRewriteApplied()
                    && shouldRetryOriginalSqlAfterRewriteFailure(ex)) {
                    return handleRuntimeRewriteExecutionFailure(
                        request,
                        primaryEngine,
                        sqlFingerprint,
                        effectiveSql,
                        runtimeRewriteResolution,
                        ex,
                        start
                    );
                }
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
            QueryExecuteResponse timeoutResponse = handlePrimaryTimeout(
                request,
                primaryEngine,
                sqlFingerprint,
                runtimeRewriteResolution,
                timeoutMs,
                primaryStep,
                start
            );
            if (timeoutResponse != null) {
                return timeoutResponse;
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


    private QueryExecuteResponse handlePrimaryTimeout(QueryExecuteRequest request,
                                                      DataSourceTypeEnum primaryEngine,
                                                      String sqlFingerprint,
                                                      RuntimeRewriteResolution runtimeRewriteResolution,
                                                      Long timeoutMs,
                                                      QueryExecutionStep primaryStep,
                                                      long start) {
        if (timeoutMs == null || primaryStep.getElapsedMs() <= timeoutMs.longValue()) {
            return null;
        }
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

    private boolean shouldRetryOriginalSqlAfterRewriteFailure(HetuExecutionUnavailableException exception) {
        if (exception == null || exception.getAttemptedModes() == null || exception.getAttemptedModes().isEmpty()) {
            return false;
        }
        boolean sawExecutionFailure = false;
        for (String attemptedMode : exception.getAttemptedModes()) {
            if (!StringUtils.hasText(attemptedMode)) {
                continue;
            }
            String normalized = attemptedMode.toUpperCase(Locale.ROOT);
            if (normalized.contains("FAILED_CONFIGURATION")
                || normalized.contains("FAILED_CONNECTIVITY")
                || normalized.contains("FAILED_AUTH")
                || normalized.contains("CHAIN_DISABLED")
                || normalized.contains("CHAIN_UNCONFIGURED")
                || normalized.contains("SKIPPED_UNCONFIGURED")) {
                return false;
            }
            if (normalized.contains("FAILED_EXECUTION") || normalized.contains("FAILED_TIMEOUT")) {
                sawExecutionFailure = true;
            }
        }
        return sawExecutionFailure;
    }

    private QueryExecuteResponse handleRuntimeRewriteExecutionFailure(QueryExecuteRequest request,
                                                                      DataSourceTypeEnum primaryEngine,
                                                                      String sqlFingerprint,
                                                                      String originalExecutionSql,
                                                                      RuntimeRewriteResolution runtimeRewriteResolution,
                                                                      HetuExecutionUnavailableException exception,
                                                                      long start) {
        logStateChange(
            sqlFingerprint,
            request,
            STATE_PRIMARY_ROUTE_SELECTED,
            STATE_RUNTIME_REWRITE_EXECUTION_FAILED,
            primaryEngine.name(),
            0L,
            QueryExecutionStatus.FAILED.name(),
            MARKER_RUNTIME_REWRITE_ORIGINAL_RETRY
        );
        QueryRetryStepVO rewriteFailureStep = buildRecoveryStep(
            primaryEngine.name(),
            0L,
            QueryExecutionStatus.FAILED.name(),
            MARKER_RUNTIME_REWRITE_ORIGINAL_RETRY,
            ACTION_RETRY_ORIGINAL_SQL
        );
        RuntimeRewriteResolution fallbackRewriteResolution =
            runtimeRewriteResolution.fallbackAfterRewriteExecutionFailure(
                originalExecutionSql,
                "RUNTIME_REWRITE_EXECUTION_FAILED"
            );
        QueryExecuteRequest fallbackExecutionRequest = normalizeAccelerationRequest(
            request,
            false,
            primaryEngine,
            fallbackRewriteResolution
        );
        try {
            logStateChange(
                sqlFingerprint,
                request,
                STATE_RUNTIME_REWRITE_EXECUTION_FAILED,
                STATE_RUNTIME_REWRITE_ORIGINAL_RETRY,
                primaryEngine.name(),
                0L,
                "RETRYING_ORIGINAL_SQL",
                MARKER_RUNTIME_REWRITE_ORIGINAL_RETRY
            );
            QueryExecutionStep fallbackStep =
                queryExecutionAdapter.execute(primaryEngine, fallbackRewriteResolution.getActualSql(), fallbackExecutionRequest, false);
            fallbackStep = queryExecutionCacheGovernanceRuntimeService.finalizeSuccessfulExecution(
                QueryExecutionCacheGovernanceRuntimeService.CacheResolution.ungoverned(),
                fallbackStep
            );
            return logAndReturn(
                buildSuccessResponse(
                    QueryExecutionStatus.PARTIAL,
                    fallbackStep,
                    fallbackRewriteResolution,
                    true,
                    "运行时改写 SQL 执行失败，已回退原 SQL。原因：" + exception.getMessage(),
                    Collections.singletonList(rewriteFailureStep)
                ),
                request,
                start
            );
        } catch (HetuExecutionUnavailableException retryException) {
            return handleUnavailableHetuRoute(
                request,
                primaryEngine,
                sqlFingerprint,
                fallbackRewriteResolution,
                new HetuExecutionUnavailableException(
                    "运行时改写 SQL 执行失败，原 SQL 回退也未成功。rewriteFailure="
                        + exception.getMessage() + "; originalRetryFailure=" + retryException.getMessage(),
                    retryException.getAttemptedModes(),
                    retryException,
                    retryException.getRouteProfile(),
                    retryException.getRouteOrder(),
                    retryException.getRouteEvidenceSource(),
                    retryException.getRouteVerificationStatus()
                ),
                start,
                QueryExecutionCacheGovernanceRuntimeService.CacheResolution.ungoverned()
            );
        }
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
            request == null ? null : request.getDatasourceCode(),
            commentContext == null ? null : commentContext.get("datasource"),
            commentContext == null ? null : commentContext.get("datasource_code"),
            request == null || request.getQueryContext() == null ? null : request.getQueryContext().getDatabaseName()
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

}
