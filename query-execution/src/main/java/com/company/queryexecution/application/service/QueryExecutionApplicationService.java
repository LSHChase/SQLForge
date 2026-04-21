package com.company.queryexecution.application.service;

import com.company.queryexecution.application.controller.dto.QueryContextDTO;
import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.application.controller.vo.QueryErrorDetailVO;
import com.company.queryexecution.application.controller.vo.QueryExecuteResponse;
import com.company.queryexecution.application.controller.vo.QueryExecutionMetadataVO;
import com.company.queryexecution.application.controller.vo.QueryRetryStepVO;
import com.company.queryexecution.domain.query.FaultToleranceStrategy;
import com.company.queryexecution.domain.query.QueryExecutionStatus;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.queryexecution.domain.query.ReadonlyQueryAssessment;
import com.company.queryexecution.domain.query.ReadonlyQueryGuard;
import com.company.queryexecution.infrastructure.adapter.QueryExecutionAdapter;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Executes the minimal synchronous query loop while preserving the public HTTP contract.
 */
@Service
public class QueryExecutionApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryExecutionApplicationService.class);

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "MINIMAL_SYNC_BASELINE";
    private static final String OPERATION = "QUERY_EXECUTE_SYNC";
    private static final String READONLY_SQL_REJECTION_MESSAGE = "当前同步查询路径仅允许只读单语句 SQL";
    private static final String ROUTE_UNAVAILABLE_MESSAGE = "当前同步查询路径尚未为目标数据源开放执行路由";
    private static final String QUERY_TIMEOUT_MESSAGE = "联机查询在当前超时阈值内未完成";
    private static final String FALLBACK_IMMEDIATE_REASON = "Current fault-tolerance strategy requested immediate fallback.";
    private static final String TIMEOUT_FALLBACK_REASON = "Primary engine exceeded timeout threshold and fallback was applied.";
    private static final String STATE_REQUEST_ACCEPTED = "REQUEST_ACCEPTED";
    private static final String STATE_RISK_REJECTED = "RISK_REJECTED";
    private static final String STATE_ROUTE_UNAVAILABLE = "ROUTE_UNAVAILABLE";
    private static final String STATE_PRIMARY_ROUTE_SELECTED = "PRIMARY_ROUTE_SELECTED";
    private static final String STATE_PRIMARY_TIMEOUT = "PRIMARY_TIMEOUT";
    private static final String STATE_LOCAL_ROLLBACK_MARKED = "LOCAL_ROLLBACK_MARKED";
    private static final String STATE_FALLBACK_REQUESTED = "FALLBACK_REQUESTED";
    private static final String STATE_LOCAL_COMPENSATION_MARKED = "LOCAL_COMPENSATION_MARKED";
    private static final String STATE_COMPLETED = "COMPLETED";
    private static final String MARKER_TIMEOUT_ROLLBACK = "LOCAL_TIMEOUT_ROLLBACK_MARKED";
    private static final String MARKER_FALLBACK_COMPENSATION = "LOCAL_FALLBACK_COMPENSATION_MARKED";
    private static final String ACTION_CLOSE_PRIMARY_ATTEMPT_CONTEXT = "CLOSE_PRIMARY_ATTEMPT_CONTEXT";
    private static final String ACTION_RECORD_DEGRADED_RESULT = "RECORD_DEGRADED_RESULT";

    private final QueryExecutionAdapter queryExecutionAdapter;

    public QueryExecutionApplicationService(QueryExecutionAdapter queryExecutionAdapter) {
        this.queryExecutionAdapter = queryExecutionAdapter;
    }

    public QueryExecuteResponse executeSynchronously(QueryExecuteRequest request) {
        long start = System.currentTimeMillis();
        String actualSql = normalizeSql(request.getSqlText());
        String sqlFingerprint = SqlFingerprintUtils.fingerprint(actualSql);
        logStart(request, sqlFingerprint);
        try {
            DataSourceTypeEnum primaryEngine = resolvePrimaryEngine(request.getDatasourceType());

            ReadonlyQueryAssessment readonlyQueryAssessment = ReadonlyQueryGuard.assess(actualSql);
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
                        actualSql,
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
                        actualSql,
                        0L,
                        0L,
                        Collections.<QueryRetryStepVO>emptyList(),
                        new QueryErrorDetailVO(
                            ErrorCodeConstants.QUERY_EXECUTION_SYSTEM_ROUTE_UNAVAILABLE,
                            ROUTE_UNAVAILABLE_MESSAGE,
                            "Use HETU, HIVE, or AUTO for the current synchronous baseline.",
                            true
                        ),
                        sqlFingerprint
                    ),
                    request,
                    start
                );
            }

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
                        actualSql,
                        sqlFingerprint,
                        FALLBACK_IMMEDIATE_REASON,
                        Collections.<QueryRetryStepVO>emptyList(),
                        request
                    ),
                    request,
                    start
                );
            }

            QueryExecutionStep primaryStep = queryExecutionAdapter.execute(primaryEngine, actualSql, request, false);
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
                            actualSql,
                            sqlFingerprint,
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
                        actualSql,
                        timeoutMs.longValue(),
                        0L,
                        Collections.singletonList(timeoutRecoveryStep),
                        new QueryErrorDetailVO(
                            ErrorCodeConstants.QUERY_EXECUTION_SYSTEM_ENGINE_TIMEOUT,
                            QUERY_TIMEOUT_MESSAGE,
                            "Increase timeoutMs or use RETRY_THEN_FALLBACK for the current synchronous baseline.",
                            true
                        ),
                        sqlFingerprint
                    ),
                    request,
                    start
                );
            }

            return logAndReturn(
                buildSuccessResponse(
                    QueryExecutionStatus.SUCCESS,
                    primaryStep,
                    actualSql,
                    sqlFingerprint,
                    false,
                    null,
                    Collections.<QueryRetryStepVO>emptyList()
                ),
                request,
                start
            );
        } catch (RuntimeException ex) {
            LOGGER.error(
                "operation={} entity={} tenantId={} requestedDatasource={} faultTolerance={} costMs={} status=FAILED phase=EXCEPTION reason={}",
                OPERATION,
                sqlFingerprint,
                request.getTenantId(),
                request.getDatasourceType(),
                request.getFaultToleranceStrategy(),
                System.currentTimeMillis() - start,
                ex.getMessage(),
                ex
            );
            throw ex;
        }
    }

    private QueryExecuteResponse buildFallbackResponse(DataSourceTypeEnum primaryEngine,
                                                       String actualSql,
                                                       String sqlFingerprint,
                                                       String degradeReason,
                                                       List<QueryRetryStepVO> recoverySteps,
                                                       QueryExecuteRequest request) {
        DataSourceTypeEnum fallbackEngine = resolveFallbackEngine(primaryEngine);
        if (fallbackEngine == null) {
            return buildFailureResponse(
                QueryExecutionStatus.FAILED,
                primaryEngine.name(),
                actualSql,
                0L,
                0L,
                Collections.<QueryRetryStepVO>emptyList(),
                new QueryErrorDetailVO(
                    ErrorCodeConstants.QUERY_EXECUTION_SYSTEM_ROUTE_UNAVAILABLE,
                    ROUTE_UNAVAILABLE_MESSAGE,
                    "No fallback route is available for the selected datasource in the current baseline.",
                    true
                ),
                sqlFingerprint
            );
        }

        QueryExecutionStep fallbackStep =
            queryExecutionAdapter.execute(fallbackEngine, actualSql, null, true);
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
            sqlFingerprint,
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
            actualSql,
            sqlFingerprint,
            true,
            degradeReason,
            retryPath
        );
    }

    private QueryExecuteResponse buildSuccessResponse(QueryExecutionStatus status,
                                                      QueryExecutionStep executionStep,
                                                      String actualSql,
                                                      String sqlFingerprint,
                                                      boolean degraded,
                                                      String degradeReason,
                                                      List<QueryRetryStepVO> retryPath) {
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
                executionStep.isAccelerationApplied()
            ),
            degraded,
            degradeReason,
            retryPath,
            null,
            sqlFingerprint,
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
        return new QueryExecuteResponse(
            status,
            Collections.<java.util.Map<String, Object>>emptyList(),
            null,
            new QueryExecutionMetadataVO(targetEngine, actualSql, elapsedMs, scannedRows, false, false),
            false,
            null,
            retryPath,
            errorDetail,
            sqlFingerprint,
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
        LOGGER.info(
            "operation={} entity={} tenantId={} requestedDatasource={} faultTolerance={} costMs={} status=END resultStatus={} degraded={}",
            OPERATION,
            response.getSqlFingerprint(),
            request.getTenantId(),
            request.getDatasourceType(),
            request.getFaultToleranceStrategy(),
            System.currentTimeMillis() - start,
            response.getStatus(),
            response.isDegraded()
        );
        return response;
    }

    private void logStart(QueryExecuteRequest request, String sqlFingerprint) {
        LOGGER.info(
            "operation={} entity={} tenantId={} requestedDatasource={} faultTolerance={} status=START",
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
            "operation={} entity={} tenantId={} requestedDatasource={} faultTolerance={} status=STATE_CHANGE from={} to={} engine={} elapsedMs={} resultStatus={} localRecoveryMarker={}",
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

    private DataSourceTypeEnum resolvePrimaryEngine(DataSourceTypeEnum datasourceType) {
        if (datasourceType == null) {
            return null;
        }
        if (DataSourceTypeEnum.AUTO == datasourceType) {
            return DataSourceTypeEnum.HETU;
        }
        if (DataSourceTypeEnum.HETU == datasourceType || DataSourceTypeEnum.HIVE == datasourceType) {
            return datasourceType;
        }
        return null;
    }

    private DataSourceTypeEnum resolveFallbackEngine(DataSourceTypeEnum primaryEngine) {
        if (DataSourceTypeEnum.HETU == primaryEngine) {
            return DataSourceTypeEnum.HIVE;
        }
        return null;
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
}
