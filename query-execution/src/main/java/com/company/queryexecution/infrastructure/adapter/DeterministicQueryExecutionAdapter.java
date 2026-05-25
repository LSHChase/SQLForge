package com.company.queryexecution.infrastructure.adapter;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.domain.query.AccelerationPreference;
import com.company.queryexecution.domain.query.QueryExecutionAccessMode;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.Collections;
import java.util.Map;

/**
 * 在不打开真实 SQL 运行时的情况下生成确定性同步执行结果。
 */
public class DeterministicQueryExecutionAdapter implements QueryExecutionAdapter {

    private static final long HETU_BASE_ELAPSED_MS = 80L;
    private static final long TRINO_BASE_ELAPSED_MS = 36L;
    private static final long HIVE_BASE_ELAPSED_MS = 24L;

    @Override
    public QueryExecutionStep execute(DataSourceTypeEnum targetEngine,
                                      String actualSql,
                                      QueryExecuteRequest request,
                                      boolean degradedPath) {
        boolean accelerationApplied = shouldApplyAcceleration(targetEngine, request, degradedPath);
        long elapsedMs = resolveElapsedMs(targetEngine, actualSql, accelerationApplied);
        long scannedRows = resolveScannedRows(actualSql);
        return new QueryExecutionStep(
            targetEngine,
            Collections.<Map<String, Object>>emptyList(),
            elapsedMs,
            scannedRows,
            false,
            accelerationApplied,
            resolveExecutionMode(targetEngine, degradedPath).name(),
            Collections.singletonList(resolveExecutionMode(targetEngine, degradedPath).name())
        );
    }

    private long resolveElapsedMs(DataSourceTypeEnum targetEngine, String actualSql, boolean accelerationApplied) {
        long baseElapsedMs = HETU_BASE_ELAPSED_MS;
        if (DataSourceTypeEnum.TRINO == targetEngine) {
            baseElapsedMs = TRINO_BASE_ELAPSED_MS;
        } else if (DataSourceTypeEnum.HIVE == targetEngine) {
            baseElapsedMs = HIVE_BASE_ELAPSED_MS;
        }
        long shapeOffsetMs = Math.abs(actualSql.hashCode() % 7);
        if (accelerationApplied && baseElapsedMs > 10L) {
            return baseElapsedMs - 10L + shapeOffsetMs;
        }
        return baseElapsedMs + shapeOffsetMs;
    }

    private long resolveScannedRows(String actualSql) {
        return 32L + Math.abs(actualSql.hashCode() % 17);
    }

    private boolean shouldApplyAcceleration(DataSourceTypeEnum targetEngine,
                                            QueryExecuteRequest request,
                                            boolean degradedPath) {
        if (degradedPath || request == null) {
            return false;
        }
        return (DataSourceTypeEnum.HETU == targetEngine || DataSourceTypeEnum.TRINO == targetEngine)
            && request.getAccelerationPreference() == AccelerationPreference.PREFER_ACCELERATED;
    }

    private QueryExecutionAccessMode resolveExecutionMode(DataSourceTypeEnum targetEngine, boolean degradedPath) {
        if (degradedPath || DataSourceTypeEnum.HIVE == targetEngine) {
            return QueryExecutionAccessMode.HIVE_FALLBACK;
        }
        return QueryExecutionAccessMode.SIMULATED;
    }
}
