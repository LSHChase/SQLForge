package com.company.queryexecution.application.service;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.application.controller.vo.QueryExecuteResponse;
import com.company.queryexecution.application.controller.vo.QueryExecutionMetadataVO;
import com.company.queryexecution.domain.query.AccelerationPreference;
import com.company.queryexecution.domain.query.FaultToleranceStrategy;
import com.company.queryexecution.domain.query.QueryExecutionStatus;
import com.company.queryexecution.infrastructure.governance.GovernanceCapabilityClient;
import com.company.queryexecution.infrastructure.governance.QueryExecutionAuditRecord;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadEngineSnapshot;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadResponse;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class QueryExecutionBenchmarkWorkloadService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "BENCHMARK_WORKLOAD_ORCHESTRATION_BASELINE";
    private static final String WORKLOAD_SOURCE_LIVE = "QUERY_EXECUTION_SYNC";
    private static final String WORKLOAD_SOURCE_BACKFILL = "SYNTHETIC_BACKFILL";
    private static final String WORKLOAD_SOURCE_COMPENSATED_REPLAY = "COMPENSATED_REPLAY";
    private static final String COMPENSATION_STRATEGY = "PRIMARY_LIVE_ENGINE_REPLAY";
    private static final String OPERATION = "QUERY_BENCHMARK_WORKLOAD_CAPTURE";
    private static final String RESOURCE_TYPE = "QUERY_EXECUTION_BENCHMARK_WORKLOAD";

    private final QueryExecutionApplicationService queryExecutionApplicationService;
    private final GovernanceCapabilityClient governanceCapabilityClient;

    public QueryExecutionBenchmarkWorkloadService(QueryExecutionApplicationService queryExecutionApplicationService,
                                                  GovernanceCapabilityClient governanceCapabilityClient) {
        this.queryExecutionApplicationService = queryExecutionApplicationService;
        this.governanceCapabilityClient = governanceCapabilityClient;
    }

    public QueryExecutionBenchmarkWorkloadResponse capture(QueryExecutionBenchmarkWorkloadRequest request) {
        long startedAt = System.currentTimeMillis();
        String normalizedSql = normalizeSql(request == null ? null : request.getSqlText());
        String sqlFingerprint = StringUtils.hasText(request == null ? null : request.getSqlFingerprint())
            ? request.getSqlFingerprint()
            : SqlFingerprintUtils.fingerprint(normalizedSql);
        List<DataSourceTypeEnum> targetEngines = normalizeTargetEngines(request == null ? null : request.getTargetEngines());
        List<QueryExecutionBenchmarkWorkloadEngineSnapshot> engineSnapshots =
            new ArrayList<QueryExecutionBenchmarkWorkloadEngineSnapshot>(targetEngines.size());
        for (DataSourceTypeEnum targetEngine : targetEngines) {
            QueryExecutionBenchmarkWorkloadEngineSnapshot snapshot;
            try {
                QueryExecuteResponse response = queryExecutionApplicationService.executeSynchronously(
                    buildExecuteRequest(request, normalizedSql, sqlFingerprint, targetEngine)
                );
                if (response != null && response.getStatus() == QueryExecutionStatus.SUCCESS && response.getMetadata() != null) {
                    snapshot = toLiveSnapshot(sqlFingerprint, targetEngine, response);
                } else {
                    snapshot = buildBackfillSnapshot(
                        request,
                        normalizedSql,
                        sqlFingerprint,
                        targetEngine,
                        response == null ? "empty response" : summarizeFailure(response)
                    );
                }
            } catch (RuntimeException ex) {
                snapshot = buildBackfillSnapshot(request, normalizedSql, sqlFingerprint, targetEngine, ex.getMessage());
            }
            engineSnapshots.add(snapshot);
        }
        boolean backfillApplied = containsBackfill(engineSnapshots);
        CompensationPlan compensationPlan = applyCompensationReplay(engineSnapshots, sqlFingerprint);
        QueryExecutionBenchmarkWorkloadResponse response =
            new QueryExecutionBenchmarkWorkloadResponse();
        response.setTenantId(request == null ? null : request.getTenantId());
        response.setBenchmarkTaskId(request == null ? null : request.getBenchmarkTaskId());
        response.setSqlFingerprint(sqlFingerprint);
        response.setWorkloadDigest(buildAggregateDigest(sqlFingerprint, engineSnapshots));
        response.setWorkloadSource(resolveWorkloadSource(backfillApplied, compensationPlan.isCompensationApplied()));
        response.setBackfillApplied(backfillApplied);
        response.setCompensationApplied(compensationPlan.isCompensationApplied());
        response.setCompensationStrategy(compensationPlan.getCompensationStrategy());
        response.setEngineSnapshots(Collections.unmodifiableList(engineSnapshots));
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(IMPLEMENTATION_STAGE);
        writeAuditRecord(request, response, System.currentTimeMillis() - startedAt);
        return response;
    }

    private QueryExecuteRequest buildExecuteRequest(QueryExecutionBenchmarkWorkloadRequest request,
                                                    String normalizedSql,
                                                    String sqlFingerprint,
                                                    DataSourceTypeEnum targetEngine) {
        QueryExecuteRequest executeRequest = new QueryExecuteRequest();
        executeRequest.setTenantId(request == null ? null : request.getTenantId());
        executeRequest.setSqlText(normalizedSql);
        executeRequest.setDatasourceType(targetEngine);
        executeRequest.setAccelerationPreference(AccelerationPreference.NONE);
        executeRequest.setFaultToleranceStrategy(FaultToleranceStrategy.FAIL_FAST);
        return executeRequest;
    }

    private QueryExecutionBenchmarkWorkloadEngineSnapshot toLiveSnapshot(String sqlFingerprint,
                                                                         DataSourceTypeEnum targetEngine,
                                                                         QueryExecuteResponse response) {
        QueryExecutionMetadataVO metadata = response.getMetadata();
        QueryExecutionBenchmarkWorkloadEngineSnapshot snapshot = new QueryExecutionBenchmarkWorkloadEngineSnapshot();
        snapshot.setTargetEngine(targetEngine);
        snapshot.setResultStatus(response.getStatus().name());
        snapshot.setWorkloadSource(WORKLOAD_SOURCE_LIVE);
        snapshot.setBackfillSource(response.isDegraded() ? "QUERY_EXECUTION_FALLBACK" : "NONE");
        snapshot.setBackfillReason(response.isDegraded() ? response.getDegradeReason() : null);
        snapshot.setExecutionMode(metadata.getExecutionMode());
        snapshot.setAttemptedModes(metadata.getAttemptedModes());
        snapshot.setElapsedMs(Long.valueOf(metadata.getElapsedMs()));
        snapshot.setScannedRows(Long.valueOf(metadata.getScannedRows()));
        snapshot.setRowCount(Integer.valueOf(metadata.getRowCount()));
        snapshot.setCacheHit(Boolean.valueOf(metadata.isCacheHit()));
        snapshot.setAccelerationApplied(Boolean.valueOf(metadata.isAccelerationApplied()));
        snapshot.setCompensationApplied(Boolean.FALSE);
        snapshot.setWorkloadDigest(shortDigest(
            sqlFingerprint + "|" + targetEngine.name() + "|" + metadata.getExecutionMode() + "|" + metadata.getElapsedMs()
        ));
        snapshot.setEvidence(buildLiveEvidence(targetEngine, response));
        return snapshot;
    }

    private QueryExecutionBenchmarkWorkloadEngineSnapshot buildBackfillSnapshot(QueryExecutionBenchmarkWorkloadRequest request,
                                                                                String normalizedSql,
                                                                                String sqlFingerprint,
                                                                                DataSourceTypeEnum targetEngine,
                                                                                String reason) {
        int complexity = sqlComplexity(normalizedSql);
        int datasetWeight = datasetWeight(request == null ? null : request.getDatasetSizeLabel());
        int concurrencyWeight = concurrencyWeight(request == null ? null : request.getConcurrency());
        long elapsedMs = Math.max(12L, (complexity * 7L) + datasetWeight + concurrencyWeight);
        long scannedRows = Math.max(32L, (complexity * 96L) + (datasetWeight * 128L));
        QueryExecutionBenchmarkWorkloadEngineSnapshot snapshot = new QueryExecutionBenchmarkWorkloadEngineSnapshot();
        snapshot.setTargetEngine(targetEngine);
        snapshot.setResultStatus(QueryExecutionStatus.PARTIAL.name());
        snapshot.setWorkloadSource(WORKLOAD_SOURCE_BACKFILL);
        snapshot.setBackfillSource("QUERY_EXECUTION_UNAVAILABLE");
        snapshot.setBackfillReason(trimReason(reason));
        snapshot.setExecutionMode("SYNTHETIC_BACKFILL");
        snapshot.setAttemptedModes(Collections.singletonList("SYNTHETIC_BACKFILL"));
        snapshot.setElapsedMs(Long.valueOf(elapsedMs));
        snapshot.setScannedRows(Long.valueOf(scannedRows));
        snapshot.setRowCount(Integer.valueOf(Math.max(1, complexity % 5)));
        snapshot.setCacheHit(Boolean.FALSE);
        snapshot.setAccelerationApplied(Boolean.FALSE);
        snapshot.setCompensationApplied(Boolean.FALSE);
        snapshot.setWorkloadDigest(shortDigest(
            sqlFingerprint + "|" + targetEngine.name() + "|SYNTHETIC_BACKFILL|" + elapsedMs + "|" + scannedRows
        ));
        snapshot.setEvidence(
            "source=SYNTHETIC_BACKFILL"
                + ";targetEngine=" + targetEngine.name()
                + ";reason=" + trimReason(reason)
                + ";elapsedMs=" + elapsedMs
                + ";scannedRows=" + scannedRows
        );
        return snapshot;
    }

    private CompensationPlan applyCompensationReplay(List<QueryExecutionBenchmarkWorkloadEngineSnapshot> engineSnapshots,
                                                     String sqlFingerprint) {
        if (engineSnapshots == null || engineSnapshots.isEmpty()) {
            return CompensationPlan.none();
        }
        QueryExecutionBenchmarkWorkloadEngineSnapshot sourceSnapshot = firstLiveSnapshot(engineSnapshots);
        if (sourceSnapshot == null) {
            return CompensationPlan.none();
        }
        boolean compensationApplied = false;
        for (QueryExecutionBenchmarkWorkloadEngineSnapshot snapshot : engineSnapshots) {
            if (snapshot == null || !WORKLOAD_SOURCE_BACKFILL.equals(snapshot.getWorkloadSource())) {
                continue;
            }
            applyCompensatedReplaySnapshot(snapshot, sourceSnapshot, sqlFingerprint);
            compensationApplied = true;
        }
        return compensationApplied ? CompensationPlan.compensated(COMPENSATION_STRATEGY) : CompensationPlan.none();
    }

    private QueryExecutionBenchmarkWorkloadEngineSnapshot firstLiveSnapshot(
        List<QueryExecutionBenchmarkWorkloadEngineSnapshot> engineSnapshots) {
        for (QueryExecutionBenchmarkWorkloadEngineSnapshot snapshot : engineSnapshots) {
            if (snapshot != null && WORKLOAD_SOURCE_LIVE.equals(snapshot.getWorkloadSource())) {
                return snapshot;
            }
        }
        return null;
    }

    private void applyCompensatedReplaySnapshot(QueryExecutionBenchmarkWorkloadEngineSnapshot snapshot,
                                                QueryExecutionBenchmarkWorkloadEngineSnapshot sourceSnapshot,
                                                String sqlFingerprint) {
        long sourceElapsedMs = sourceSnapshot.getElapsedMs() == null ? 24L : sourceSnapshot.getElapsedMs().longValue();
        long sourceScannedRows = sourceSnapshot.getScannedRows() == null ? 128L : sourceSnapshot.getScannedRows().longValue();
        int sourceRowCount = sourceSnapshot.getRowCount() == null ? 1 : sourceSnapshot.getRowCount().intValue();
        long engineBias = snapshot.getTargetEngine() == null ? 3L : (snapshot.getTargetEngine().ordinal() + 1L) * 3L;
        long replayElapsedMs = Math.max(10L, sourceElapsedMs + engineBias);
        long replayScannedRows = Math.max(32L, sourceScannedRows + (engineBias * 32L));
        String originalReason = snapshot.getBackfillReason();
        snapshot.setResultStatus(QueryExecutionStatus.PARTIAL.name());
        snapshot.setWorkloadSource(WORKLOAD_SOURCE_COMPENSATED_REPLAY);
        snapshot.setBackfillSource("LIVE_WORKLOAD_COMPENSATED_REPLAY");
        snapshot.setBackfillReason(trimReason(originalReason));
        snapshot.setExecutionMode("COMPENSATED_REPLAY");
        snapshot.setAttemptedModes(buildCompensatedAttemptedModes(sourceSnapshot));
        snapshot.setElapsedMs(Long.valueOf(replayElapsedMs));
        snapshot.setScannedRows(Long.valueOf(replayScannedRows));
        snapshot.setRowCount(Integer.valueOf(Math.max(1, sourceRowCount)));
        snapshot.setCacheHit(Boolean.FALSE);
        snapshot.setAccelerationApplied(Boolean.FALSE);
        snapshot.setCompensationApplied(Boolean.TRUE);
        snapshot.setCompensationStrategy(COMPENSATION_STRATEGY);
        snapshot.setCompensationSourceEngine(sourceSnapshot.getTargetEngine());
        snapshot.setCompensationSourceWorkloadDigest(sourceSnapshot.getWorkloadDigest());
        snapshot.setWorkloadDigest(shortDigest(
            sqlFingerprint
                + "|"
                + (snapshot.getTargetEngine() == null ? "AUTO" : snapshot.getTargetEngine().name())
                + "|COMPENSATED_REPLAY|"
                + sourceSnapshot.getWorkloadDigest()
        ));
        snapshot.setEvidence(buildCompensationEvidence(snapshot, sourceSnapshot, originalReason));
    }

    private List<String> buildCompensatedAttemptedModes(QueryExecutionBenchmarkWorkloadEngineSnapshot sourceSnapshot) {
        List<String> attemptedModes = new ArrayList<String>();
        if (sourceSnapshot != null && sourceSnapshot.getAttemptedModes() != null) {
            attemptedModes.addAll(sourceSnapshot.getAttemptedModes());
        }
        attemptedModes.add("COMPENSATED_REPLAY");
        return Collections.unmodifiableList(attemptedModes);
    }

    private String buildCompensationEvidence(QueryExecutionBenchmarkWorkloadEngineSnapshot snapshot,
                                             QueryExecutionBenchmarkWorkloadEngineSnapshot sourceSnapshot,
                                             String originalReason) {
        return "source=" + WORKLOAD_SOURCE_COMPENSATED_REPLAY
            + ";targetEngine=" + (snapshot.getTargetEngine() == null ? "AUTO" : snapshot.getTargetEngine().name())
            + ";strategy=" + COMPENSATION_STRATEGY
            + ";sourceEngine=" + (sourceSnapshot.getTargetEngine() == null ? "AUTO" : sourceSnapshot.getTargetEngine().name())
            + ";sourceWorkloadDigest=" + sourceSnapshot.getWorkloadDigest()
            + ";originalReason=" + trimReason(originalReason);
    }

    private String buildLiveEvidence(DataSourceTypeEnum targetEngine, QueryExecuteResponse response) {
        QueryExecutionMetadataVO metadata = response.getMetadata();
        return "source=" + WORKLOAD_SOURCE_LIVE
            + ";targetEngine=" + targetEngine.name()
            + ";executionMode=" + metadata.getExecutionMode()
            + ";attemptedModes=" + metadata.getAttemptedModes()
            + ";routeProfile=" + metadata.getRouteProfile()
            + ";routeOrder=" + metadata.getRouteOrder()
            + ";elapsedMs=" + metadata.getElapsedMs()
            + ";scannedRows=" + metadata.getScannedRows()
            + ";rowCount=" + metadata.getRowCount()
            + ";degraded=" + response.isDegraded();
    }

    private String buildAggregateDigest(String sqlFingerprint,
                                        List<QueryExecutionBenchmarkWorkloadEngineSnapshot> engineSnapshots) {
        StringBuilder builder = new StringBuilder();
        builder.append(sqlFingerprint).append('|');
        for (QueryExecutionBenchmarkWorkloadEngineSnapshot snapshot : engineSnapshots) {
            builder.append(snapshot.getTargetEngine() == null ? "AUTO" : snapshot.getTargetEngine().name())
                .append(':')
                .append(snapshot.getWorkloadDigest())
                .append('|');
        }
        return shortDigest(builder.toString());
    }

    private List<DataSourceTypeEnum> normalizeTargetEngines(List<DataSourceTypeEnum> targetEngines) {
        if (targetEngines == null || targetEngines.isEmpty()) {
            return Collections.singletonList(DataSourceTypeEnum.HETU);
        }
        return targetEngines;
    }

    private boolean containsBackfill(List<QueryExecutionBenchmarkWorkloadEngineSnapshot> engineSnapshots) {
        for (QueryExecutionBenchmarkWorkloadEngineSnapshot snapshot : engineSnapshots) {
            if (snapshot != null && WORKLOAD_SOURCE_BACKFILL.equals(snapshot.getWorkloadSource())) {
                return true;
            }
        }
        return false;
    }

    private String resolveWorkloadSource(boolean backfillApplied, boolean compensationApplied) {
        if (compensationApplied) {
            return "LIVE_WITH_COMPENSATED_REPLAY";
        }
        return backfillApplied ? "LIVE_WITH_BACKFILL" : WORKLOAD_SOURCE_LIVE;
    }

    private void writeAuditRecord(QueryExecutionBenchmarkWorkloadRequest request,
                                  QueryExecutionBenchmarkWorkloadResponse response,
                                  long elapsedMs) {
        Map<String, Object> requestPayload = new LinkedHashMap<String, Object>();
        requestPayload.put("benchmarkTaskId", request == null ? null : request.getBenchmarkTaskId());
        requestPayload.put("benchmarkTaskType", request == null ? null : request.getBenchmarkTaskType());
        requestPayload.put("tenantId", request == null ? null : request.getTenantId());
        requestPayload.put("sqlFingerprint", response == null ? null : response.getSqlFingerprint());
        requestPayload.put("targetEngineCount", response == null || response.getEngineSnapshots() == null
            ? Integer.valueOf(0)
            : Integer.valueOf(response.getEngineSnapshots().size()));
        Map<String, Object> responsePayload = new LinkedHashMap<String, Object>();
        responsePayload.put("workloadSource", response == null ? null : response.getWorkloadSource());
        responsePayload.put("backfillApplied", response == null ? Boolean.FALSE : Boolean.valueOf(response.isBackfillApplied()));
        responsePayload.put("compensationApplied", response == null ? Boolean.FALSE : Boolean.valueOf(response.isCompensationApplied()));
        responsePayload.put("compensationStrategy", response == null ? null : response.getCompensationStrategy());
        responsePayload.put("workloadDigest", response == null ? null : response.getWorkloadDigest());
        governanceCapabilityClient.writeAudit(
            new QueryExecutionAuditRecord(
                OPERATION,
                RESOURCE_TYPE,
                response == null ? null : response.getSqlFingerprint(),
                response != null && response.isBackfillApplied() ? "PARTIAL" : "SUCCESS",
                elapsedMs,
                JsonUtils.toJson(requestPayload),
                JsonUtils.toJson(responsePayload)
            )
        );
    }

    private String summarizeFailure(QueryExecuteResponse response) {
        if (response == null) {
            return "query-execution returned null response";
        }
        if (response.getError() != null && StringUtils.hasText(response.getError().getMessage())) {
            return response.getError().getMessage();
        }
        if (StringUtils.hasText(response.getDegradeReason())) {
            return response.getDegradeReason();
        }
        return "query-execution status=" + response.getStatus().name();
    }

    private String normalizeSql(String sqlText) {
        String normalized = sqlText == null ? "" : sqlText.trim();
        if (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private int sqlComplexity(String sql) {
        String normalized = sql == null ? "" : sql.toUpperCase(Locale.ROOT);
        int complexity = 3;
        complexity += countKeyword(normalized, "JOIN") * 2;
        complexity += countKeyword(normalized, "WHERE");
        complexity += countKeyword(normalized, "GROUP BY") * 2;
        complexity += countKeyword(normalized, "ORDER BY");
        complexity += Math.min(4, normalized.length() / 48);
        return complexity;
    }

    private int datasetWeight(String datasetSizeLabel) {
        if (!StringUtils.hasText(datasetSizeLabel)) {
            return 3;
        }
        String normalized = datasetSizeLabel.toUpperCase(Locale.ROOT);
        if (normalized.contains("HUNDRED") || normalized.contains("100")) {
            return 8;
        }
        if (normalized.contains("TEN")) {
            return 4;
        }
        return 2;
    }

    private int concurrencyWeight(Integer concurrency) {
        if (concurrency == null || concurrency.intValue() <= 1) {
            return 1;
        }
        return Math.min(8, concurrency.intValue());
    }

    private int countKeyword(String sql, String keyword) {
        int count = 0;
        int start = 0;
        while (true) {
            int index = sql.indexOf(keyword, start);
            if (index < 0) {
                return count;
            }
            count++;
            start = index + keyword.length();
        }
    }

    private String trimReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return "query-execution unavailable";
        }
        String trimmed = reason.replace('\n', ' ').replace('\r', ' ').trim();
        return trimmed.length() > 160 ? trimmed.substring(0, 160) : trimmed;
    }

    private String shortDigest(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < 8 && index < hashed.length; index++) {
                builder.append(String.format("%02x", Byte.valueOf(hashed[index])));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private static final class CompensationPlan {

        private final boolean compensationApplied;
        private final String compensationStrategy;

        private CompensationPlan(boolean compensationApplied, String compensationStrategy) {
            this.compensationApplied = compensationApplied;
            this.compensationStrategy = compensationStrategy;
        }

        private static CompensationPlan none() {
            return new CompensationPlan(false, null);
        }

        private static CompensationPlan compensated(String compensationStrategy) {
            return new CompensationPlan(true, compensationStrategy);
        }

        private boolean isCompensationApplied() {
            return compensationApplied;
        }

        private String getCompensationStrategy() {
            return compensationStrategy;
        }
    }
}
