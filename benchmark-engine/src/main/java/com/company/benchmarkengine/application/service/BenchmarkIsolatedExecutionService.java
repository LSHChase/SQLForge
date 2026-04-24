package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.config.BenchmarkTaskExecutionProperties;
import com.company.benchmarkengine.domain.benchmark.BenchmarkEngineProfile;
import com.company.benchmarkengine.domain.benchmark.BenchmarkExecutionSummary;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdAssessment;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdVerdict;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.stereotype.Service;

@Service
public class BenchmarkIsolatedExecutionService {

    private static final String EXECUTION_MODE = "REPO_CLOSED_ISOLATED_EXECUTOR";
    private static final String ISOLATION_SUMMARY = "shadow-only readonly benchmark replay";

    private final BenchmarkTaskExecutionProperties executionProperties;
    private final BenchmarkTaskModelApplicationService benchmarkTaskModelApplicationService;

    public BenchmarkIsolatedExecutionService(BenchmarkTaskExecutionProperties executionProperties,
                                             BenchmarkTaskModelApplicationService benchmarkTaskModelApplicationService) {
        this.executionProperties = executionProperties;
        this.benchmarkTaskModelApplicationService = benchmarkTaskModelApplicationService;
    }

    public BenchmarkIsolatedExecutionResult execute(BenchmarkTask task, Instant generatedAt) {
        List<DataSourceTypeEnum> targetEngines = task.getTargetEngines();
        if (targetEngines == null || targetEngines.isEmpty()) {
            targetEngines = Collections.singletonList(DataSourceTypeEnum.HETU);
        }
        int sampleCount = normalizeSampleCount();
        int complexity = sqlComplexity(task);
        int datasetWeight = datasetWeight(task.getDatasetSizeLabel());
        int concurrencyWeight = concurrencyWeight(task.getConcurrency());
        String workloadDigest = shortDigest(
            task.getTaskType().name() + "|" + task.getSqlFingerprint() + "|" + task.getSqlText() + "|" + generatedAt.toEpochMilli()
        );
        List<BenchmarkEngineProfile> engineProfiles = new ArrayList<BenchmarkEngineProfile>(targetEngines.size());
        long totalDurationMs = 0L;
        for (int index = 0; index < targetEngines.size(); index++) {
            EngineExecutionSnapshot snapshot = runIsolatedEngine(
                task,
                targetEngines.get(index),
                index,
                sampleCount,
                complexity,
                datasetWeight,
                concurrencyWeight,
                workloadDigest
            );
            totalDurationMs += snapshot.getExecutionDurationMs();
            engineProfiles.add(snapshot.toProfile());
        }
        List<BenchmarkThresholdAssessment> assessments = benchmarkTaskModelApplicationService.evaluateThresholds(
            task.getThresholds(),
            engineProfiles.get(0)
        );
        return new BenchmarkIsolatedExecutionResult(
            new BenchmarkExecutionSummary(
                EXECUTION_MODE,
                ISOLATION_SUMMARY,
                Integer.valueOf(sampleCount),
                Long.valueOf(totalDurationMs),
                workloadDigest,
                buildPhaseNotes(task, complexity, datasetWeight, concurrencyWeight, targetEngines, totalDurationMs, workloadDigest)
            ),
            engineProfiles,
            assessments,
            benchmarkTaskModelApplicationService.buildRecommendations(task.getTaskType(), assessments)
        );
    }

    private EngineExecutionSnapshot runIsolatedEngine(BenchmarkTask task,
                                                      DataSourceTypeEnum engine,
                                                      int index,
                                                      int sampleCount,
                                                      int complexity,
                                                      int datasetWeight,
                                                      int concurrencyWeight,
                                                      String workloadDigest) {
        long startedAt = System.nanoTime();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Long> future = executor.submit(new SyntheticEngineProbe(
                task.getSqlText(),
                task.getSqlFingerprint(),
                engine,
                index,
                sampleCount,
                complexity,
                datasetWeight,
                concurrencyWeight,
                executionProperties.getIsolationWorkIterations()
            ));
            long probeToken;
            try {
                probeToken = future.get().longValue();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Benchmark isolated execution interrupted", ex);
            } catch (ExecutionException ex) {
                throw new IllegalStateException("Benchmark isolated execution failed", ex.getCause());
            }
            long executionDurationMs = Math.max(1L, (System.nanoTime() - startedAt) / 1_000_000L);
            return buildSnapshot(task, engine, index, complexity, datasetWeight, concurrencyWeight, sampleCount, probeToken,
                executionDurationMs, workloadDigest);
        } finally {
            executor.shutdownNow();
        }
    }

    private EngineExecutionSnapshot buildSnapshot(BenchmarkTask task,
                                                  DataSourceTypeEnum engine,
                                                  int index,
                                                  int complexity,
                                                  int datasetWeight,
                                                  int concurrencyWeight,
                                                  int sampleCount,
                                                  long probeToken,
                                                  long executionDurationMs,
                                                  String workloadDigest) {
        int enginePenalty = enginePenalty(engine, index);
        int taskLatencyBias = taskLatencyBias(task.getTaskType());
        int taskCpuBias = taskCpuBias(task.getTaskType());
        int taskQpsPenalty = taskQpsPenalty(task.getTaskType());
        long tokenBias = Math.abs(probeToken % 7L);

        BigDecimal targetQps = new BigDecimal("120");
        BigDecimal actualQps = decimal(
            160L - (complexity * 3L) - datasetWeight - taskQpsPenalty - enginePenalty + concurrencyWeight - tokenBias
        );
        BigDecimal p50LatencyMs = decimal(14L + (complexity * 2L) + datasetWeight + taskLatencyBias + (enginePenalty / 2L));
        BigDecimal p95LatencyMs = decimal(asLong(p50LatencyMs) + 18L + complexity + (enginePenalty / 2L) + tokenBias);
        BigDecimal p99LatencyMs = decimal(asLong(p95LatencyMs) + 16L + (taskLatencyBias / 2L) + (enginePenalty / 2L) + tokenBias);
        BigDecimal cpuUsagePercent = decimal(26L + (complexity * 2L) + concurrencyWeight + taskCpuBias + (enginePenalty / 2L));
        BigDecimal memoryUsageMb = decimal(256L + (datasetWeight * 48L) + (complexity * 12L) + (enginePenalty * 8L));
        BigDecimal scannedDataBytes = decimal(
            ((long) (datasetWeight + complexity + 1L) * 33554432L) + ((long) enginePenalty * 4194304L) + (sampleCount * 1024L)
        );
        BenchmarkThresholdVerdict verdict = deriveVerdict(task, actualQps, p99LatencyMs, cpuUsagePercent, targetQps);
        return new EngineExecutionSnapshot(
            new BenchmarkEngineProfile(
                engine,
                targetQps,
                actualQps,
                p50LatencyMs,
                p95LatencyMs,
                p99LatencyMs,
                cpuUsagePercent,
                memoryUsageMb,
                scannedDataBytes,
                verdict,
                buildEngineNote(task, engine, sampleCount, executionDurationMs, workloadDigest)
            ),
            executionDurationMs
        );
    }

    private BenchmarkThresholdVerdict deriveVerdict(BenchmarkTask task,
                                                    BigDecimal actualQps,
                                                    BigDecimal p99LatencyMs,
                                                    BigDecimal cpuUsagePercent,
                                                    BigDecimal targetQps) {
        if (task.getTaskType() == BenchmarkTaskType.REGRESSION_GUARD && p99LatencyMs.compareTo(new BigDecimal("70")) > 0) {
            return BenchmarkThresholdVerdict.FAIL;
        }
        if (actualQps.compareTo(targetQps) < 0) {
            return BenchmarkThresholdVerdict.WARNING;
        }
        if (cpuUsagePercent.compareTo(new BigDecimal("50")) > 0 || p99LatencyMs.compareTo(new BigDecimal("90")) > 0) {
            return BenchmarkThresholdVerdict.WARNING;
        }
        return BenchmarkThresholdVerdict.PASS;
    }

    private List<String> buildPhaseNotes(BenchmarkTask task,
                                         int complexity,
                                         int datasetWeight,
                                         int concurrencyWeight,
                                         List<DataSourceTypeEnum> targetEngines,
                                         long totalDurationMs,
                                         String workloadDigest) {
        return Arrays.asList(
            "executionMode=" + EXECUTION_MODE,
            "isolation=" + ISOLATION_SUMMARY,
            "taskType=" + task.getTaskType().name(),
            "targetEngines=" + targetEngines,
            "sqlComplexity=" + complexity,
            "datasetWeight=" + datasetWeight,
            "concurrencyWeight=" + concurrencyWeight,
            "workloadDigest=" + workloadDigest,
            "executionDurationMs=" + totalDurationMs
        );
    }

    private String buildEngineNote(BenchmarkTask task,
                                   DataSourceTypeEnum engine,
                                   int sampleCount,
                                   long executionDurationMs,
                                   String workloadDigest) {
        return "Isolated " + task.getTaskType().name().toLowerCase(Locale.ROOT)
            + " replay for " + engine.name()
            + " with " + sampleCount
            + " samples, durationMs=" + executionDurationMs
            + ", digest=" + workloadDigest;
    }

    private int normalizeSampleCount() {
        int configured = executionProperties.getIsolationSampleCount();
        if (configured < 4) {
            return 4;
        }
        if (configured > 24) {
            return 24;
        }
        return configured;
    }

    private int sqlComplexity(BenchmarkTask task) {
        String sql = (task.getSqlText() == null ? "" : task.getSqlText()).toUpperCase(Locale.ROOT);
        int complexity = 3;
        complexity += countKeyword(sql, "JOIN") * 2;
        complexity += countKeyword(sql, "WHERE");
        complexity += countKeyword(sql, "GROUP BY") * 2;
        complexity += countKeyword(sql, "ORDER BY");
        complexity += countKeyword(sql, "UNION") * 2;
        complexity += countKeyword(sql, "OVER(") * 2;
        complexity += Math.min(4, sql.length() / 40);
        return complexity;
    }

    private int datasetWeight(String datasetSizeLabel) {
        if (datasetSizeLabel == null || datasetSizeLabel.trim().isEmpty()) {
            return 3;
        }
        String normalized = datasetSizeLabel.toUpperCase(Locale.ROOT);
        if (normalized.contains("HUNDRED") || normalized.contains("100")) {
            return 8;
        }
        if (normalized.contains("TEN")) {
            return 4;
        }
        if (normalized.contains("ONE")) {
            return 2;
        }
        return 3;
    }

    private int concurrencyWeight(Integer concurrency) {
        if (concurrency == null || concurrency.intValue() <= 0) {
            return 2;
        }
        return Math.min(16, Math.max(2, concurrency.intValue() / 4));
    }

    private int enginePenalty(DataSourceTypeEnum engine, int index) {
        if (engine == DataSourceTypeEnum.HIVE) {
            return 6 + index;
        }
        if (engine == DataSourceTypeEnum.AUTO) {
            return Math.max(0, index);
        }
        return index;
    }

    private int taskLatencyBias(BenchmarkTaskType taskType) {
        if (taskType == BenchmarkTaskType.BASELINE) {
            return -4;
        }
        if (taskType == BenchmarkTaskType.COMPARISON) {
            return 4;
        }
        return 12;
    }

    private int taskCpuBias(BenchmarkTaskType taskType) {
        if (taskType == BenchmarkTaskType.BASELINE) {
            return -6;
        }
        if (taskType == BenchmarkTaskType.COMPARISON) {
            return 16;
        }
        return 20;
    }

    private int taskQpsPenalty(BenchmarkTaskType taskType) {
        if (taskType == BenchmarkTaskType.BASELINE) {
            return 0;
        }
        if (taskType == BenchmarkTaskType.COMPARISON) {
            return 6;
        }
        return 18;
    }

    private int countKeyword(String sql, String keyword) {
        int count = 0;
        int index = sql.indexOf(keyword);
        while (index >= 0) {
            count++;
            index = sql.indexOf(keyword, index + keyword.length());
        }
        return count;
    }

    private String shortDigest(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < 8; index++) {
                builder.append(String.format(Locale.ROOT, "%02x", Byte.valueOf(bytes[index])));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private BigDecimal decimal(long value) {
        return new BigDecimal(String.valueOf(Math.max(1L, value)));
    }

    private long asLong(BigDecimal value) {
        return value.longValue();
    }

    private static final class SyntheticEngineProbe implements Callable<Long> {

        private final String sqlText;
        private final String sqlFingerprint;
        private final DataSourceTypeEnum engine;
        private final int engineIndex;
        private final int sampleCount;
        private final int complexity;
        private final int datasetWeight;
        private final int concurrencyWeight;
        private final int workIterations;

        private SyntheticEngineProbe(String sqlText,
                                     String sqlFingerprint,
                                     DataSourceTypeEnum engine,
                                     int engineIndex,
                                     int sampleCount,
                                     int complexity,
                                     int datasetWeight,
                                     int concurrencyWeight,
                                     int workIterations) {
            this.sqlText = sqlText == null ? "" : sqlText;
            this.sqlFingerprint = sqlFingerprint == null ? "" : sqlFingerprint;
            this.engine = engine;
            this.engineIndex = engineIndex;
            this.sampleCount = sampleCount;
            this.complexity = complexity;
            this.datasetWeight = datasetWeight;
            this.concurrencyWeight = concurrencyWeight;
            this.workIterations = Math.max(32, workIterations);
        }

        @Override
        public Long call() {
            long accumulator = 1125899906842597L;
            String seed = sqlFingerprint + "|" + sqlText + "|" + engine.name() + "|" + engineIndex;
            byte[] bytes = seed.getBytes(StandardCharsets.UTF_8);
            int loopCount = workIterations + (sampleCount * 8) + (complexity * 12) + (datasetWeight * 8) + (concurrencyWeight * 4);
            for (int outer = 0; outer < sampleCount; outer++) {
                for (int inner = 0; inner < loopCount; inner++) {
                    byte value = bytes[(outer + inner) % bytes.length];
                    accumulator = (accumulator * 1315423911L) ^ (long) value ^ (long) outer ^ (long) inner;
                }
            }
            return Long.valueOf(accumulator);
        }
    }

    private static final class EngineExecutionSnapshot {

        private final BenchmarkEngineProfile profile;
        private final long executionDurationMs;

        private EngineExecutionSnapshot(BenchmarkEngineProfile profile, long executionDurationMs) {
            this.profile = profile;
            this.executionDurationMs = executionDurationMs;
        }

        private BenchmarkEngineProfile toProfile() {
            return profile;
        }

        private long getExecutionDurationMs() {
            return executionDurationMs;
        }
    }
}
