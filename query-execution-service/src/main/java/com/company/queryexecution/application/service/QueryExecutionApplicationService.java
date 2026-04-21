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
import org.springframework.stereotype.Service;

/**
 * Executes the minimal synchronous query loop while preserving the public HTTP contract.
 */
@Service
public class QueryExecutionApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "MINIMAL_SYNC_BASELINE";
    private static final String READONLY_SQL_REJECTION_MESSAGE = "当前同步查询路径仅允许只读单语句 SQL";
    private static final String ROUTE_UNAVAILABLE_MESSAGE = "当前同步查询路径尚未为目标数据源开放执行路由";
    private static final String QUERY_TIMEOUT_MESSAGE = "联机查询在当前超时阈值内未完成";
    private static final String FALLBACK_IMMEDIATE_REASON = "Current fault-tolerance strategy requested immediate fallback.";
    private static final String TIMEOUT_FALLBACK_REASON = "Primary engine exceeded timeout threshold and fallback was applied.";

    private final QueryExecutionAdapter queryExecutionAdapter;

    public QueryExecutionApplicationService(QueryExecutionAdapter queryExecutionAdapter) {
        this.queryExecutionAdapter = queryExecutionAdapter;
    }

    public QueryExecuteResponse executeSynchronously(QueryExecuteRequest request) {
        String actualSql = normalizeSql(request.getSqlText());
        String sqlFingerprint = SqlFingerprintUtils.fingerprint(actualSql);
        DataSourceTypeEnum primaryEngine = resolvePrimaryEngine(request.getDatasourceType());

        ReadonlyQueryAssessment readonlyQueryAssessment = ReadonlyQueryGuard.assess(actualSql);
        if (!readonlyQueryAssessment.isReadonly()) {
            return buildFailureResponse(
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
            );
        }

        if (primaryEngine == null) {
            return buildFailureResponse(
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
            );
        }

        Long timeoutMs = resolveTimeoutMs(request.getQueryContext());
        if (FaultToleranceStrategy.FALLBACK_IMMEDIATE == request.getFaultToleranceStrategy()) {
            return buildFallbackResponse(primaryEngine, actualSql, sqlFingerprint, FALLBACK_IMMEDIATE_REASON);
        }

        QueryExecutionStep primaryStep = queryExecutionAdapter.execute(primaryEngine, actualSql, request, false);
        if (timeoutMs != null && primaryStep.getElapsedMs() > timeoutMs.longValue()) {
            if (FaultToleranceStrategy.RETRY_THEN_FALLBACK == request.getFaultToleranceStrategy()
                && resolveFallbackEngine(primaryEngine) != null) {
                QueryExecuteResponse fallbackResponse =
                    buildFallbackResponse(primaryEngine, actualSql, sqlFingerprint, TIMEOUT_FALLBACK_REASON);
                List<QueryRetryStepVO> retryPath = new ArrayList<QueryRetryStepVO>();
                retryPath.add(new QueryRetryStepVO(primaryEngine.name(), timeoutMs.longValue()));
                retryPath.addAll(fallbackResponse.getRetryPath());
                return new QueryExecuteResponse(
                    fallbackResponse.getStatus(),
                    fallbackResponse.getRows(),
                    fallbackResponse.getDownloadUrl(),
                    fallbackResponse.getMetadata(),
                    fallbackResponse.isDegraded(),
                    fallbackResponse.getDegradeReason(),
                    retryPath,
                    fallbackResponse.getError(),
                    fallbackResponse.getSqlFingerprint(),
                    fallbackResponse.getContractStage(),
                    fallbackResponse.getImplementationStage()
                );
            }

            return buildFailureResponse(
                QueryExecutionStatus.TIMEOUT,
                primaryEngine.name(),
                actualSql,
                timeoutMs.longValue(),
                0L,
                Collections.singletonList(new QueryRetryStepVO(primaryEngine.name(), timeoutMs.longValue())),
                new QueryErrorDetailVO(
                    ErrorCodeConstants.QUERY_EXECUTION_SYSTEM_ENGINE_TIMEOUT,
                    QUERY_TIMEOUT_MESSAGE,
                    "Increase timeoutMs or use RETRY_THEN_FALLBACK for the current synchronous baseline.",
                    true
                ),
                sqlFingerprint
            );
        }

        return buildSuccessResponse(
            QueryExecutionStatus.SUCCESS,
            primaryStep,
            actualSql,
            sqlFingerprint,
            false,
            null,
            Collections.<QueryRetryStepVO>emptyList()
        );
    }

    private QueryExecuteResponse buildFallbackResponse(DataSourceTypeEnum primaryEngine,
                                                       String actualSql,
                                                       String sqlFingerprint,
                                                       String degradeReason) {
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
        return buildSuccessResponse(
            QueryExecutionStatus.PARTIAL,
            fallbackStep,
            actualSql,
            sqlFingerprint,
            true,
            degradeReason,
            Collections.singletonList(new QueryRetryStepVO(fallbackEngine.name(), fallbackStep.getElapsedMs()))
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
