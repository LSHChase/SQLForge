package com.company.queryexecution.infrastructure.adapter;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.domain.query.AccelerationPreference;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Produces deterministic synchronous execution results without opening a real SQL runtime.
 */
@Component
public class DeterministicQueryExecutionAdapter implements QueryExecutionAdapter {

    private static final long HETU_BASE_ELAPSED_MS = 80L;
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
            buildRows(targetEngine, actualSql, degradedPath, accelerationApplied),
            elapsedMs,
            scannedRows,
            false,
            accelerationApplied
        );
    }

    private List<Map<String, Object>> buildRows(DataSourceTypeEnum targetEngine,
                                                String actualSql,
                                                boolean degradedPath,
                                                boolean accelerationApplied) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("engine", targetEngine.name());
        row.put("mode", degradedPath ? "FALLBACK" : "PRIMARY");
        row.put("sqlFingerprint", SqlFingerprintUtils.fingerprint(actualSql));
        row.put("accelerationApplied", Boolean.valueOf(accelerationApplied));
        rows.add(row);
        return rows;
    }

    private long resolveElapsedMs(DataSourceTypeEnum targetEngine, String actualSql, boolean accelerationApplied) {
        long baseElapsedMs = DataSourceTypeEnum.HIVE == targetEngine ? HIVE_BASE_ELAPSED_MS : HETU_BASE_ELAPSED_MS;
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
        return DataSourceTypeEnum.HETU == targetEngine
            && request.getAccelerationPreference() == AccelerationPreference.PREFER_ACCELERATED;
    }
}
