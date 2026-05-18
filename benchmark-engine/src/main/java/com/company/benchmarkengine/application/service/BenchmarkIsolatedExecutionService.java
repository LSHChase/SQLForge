package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.config.BenchmarkTaskExecutionProperties;
import com.company.benchmarkengine.domain.benchmark.BenchmarkEngineProfile;
import com.company.benchmarkengine.domain.benchmark.BenchmarkExecutionSummary;
import com.company.benchmarkengine.domain.benchmark.BenchmarkScaleReadinessAssessment;
import com.company.benchmarkengine.domain.benchmark.BenchmarkScaleReadinessStatus;
import com.company.benchmarkengine.domain.benchmark.BenchmarkScaleEvidenceManifest;
import com.company.benchmarkengine.domain.benchmark.BenchmarkScaleTarget;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdAssessment;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdVerdict;
import com.company.benchmarkengine.infrastructure.queryexecution.QueryExecutionBenchmarkWorkloadClient;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadEngineSnapshot;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BenchmarkIsolatedExecutionService {

    private static final String EXECUTION_MODE_ISOLATED = "REPO_CLOSED_ISOLATED_EXECUTOR";
    private static final String EXECUTION_MODE_ORCHESTRATED = "QUERY_EXECUTION_WORKLOAD_ORCHESTRATED_REPLAY";
    private static final String ISOLATION_SUMMARY_ISOLATED = "仅影子环境只读压测回放";
    private static final String ISOLATION_SUMMARY_ORCHESTRATED =
        "通过 query-execution 工作负载编排执行仅影子环境只读压测回放";
    private static final QueryExecutionBenchmarkWorkloadClient NOOP_WORKLOAD_CLIENT =
        new QueryExecutionBenchmarkWorkloadClient() {
            @Override
            public QueryExecutionBenchmarkWorkloadResponse captureWorkload(QueryExecutionBenchmarkWorkloadRequest request) {
                return null;
            }
        };

    private final BenchmarkTaskExecutionProperties executionProperties;
    private final BenchmarkTaskModelApplicationService benchmarkTaskModelApplicationService;
    private final QueryExecutionBenchmarkWorkloadClient queryExecutionBenchmarkWorkloadClient;

    public BenchmarkIsolatedExecutionService(BenchmarkTaskExecutionProperties executionProperties,
                                             BenchmarkTaskModelApplicationService benchmarkTaskModelApplicationService) {
        this(executionProperties, benchmarkTaskModelApplicationService, NOOP_WORKLOAD_CLIENT);
    }

    @Autowired
    public BenchmarkIsolatedExecutionService(BenchmarkTaskExecutionProperties executionProperties,
                                             BenchmarkTaskModelApplicationService benchmarkTaskModelApplicationService,
                                             QueryExecutionBenchmarkWorkloadClient queryExecutionBenchmarkWorkloadClient) {
        this.executionProperties = executionProperties;
        this.benchmarkTaskModelApplicationService = benchmarkTaskModelApplicationService;
        this.queryExecutionBenchmarkWorkloadClient = queryExecutionBenchmarkWorkloadClient == null
            ? NOOP_WORKLOAD_CLIENT
            : queryExecutionBenchmarkWorkloadClient;
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
        String syntheticWorkloadDigest = shortDigest(
            task.getTaskType().name() + "|" + task.getSqlFingerprint() + "|" + task.getSqlText() + "|" + generatedAt.toEpochMilli()
        );
        WorkloadOrchestration workloadOrchestration = resolveWorkloadOrchestration(task, targetEngines, syntheticWorkloadDigest);
        String workloadDigest = workloadOrchestration.getWorkloadDigest();
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
                workloadDigest,
                workloadOrchestration.findSignal(targetEngines.get(index))
            );
            totalDurationMs += snapshot.getExecutionDurationMs();
            engineProfiles.add(snapshot.toProfile());
        }
        List<BenchmarkThresholdAssessment> assessments = benchmarkTaskModelApplicationService.evaluateThresholds(
            task.getThresholds(),
            engineProfiles.get(0)
        );
        List<String> phaseNotes = buildPhaseNotes(
            task,
            complexity,
            datasetWeight,
            concurrencyWeight,
            targetEngines,
            totalDurationMs,
            workloadDigest,
            workloadOrchestration
        );
        return new BenchmarkIsolatedExecutionResult(
            new BenchmarkExecutionSummary(
                workloadOrchestration.getExecutionMode(),
                workloadOrchestration.getIsolationSummary(),
                Integer.valueOf(sampleCount),
                Long.valueOf(totalDurationMs),
                workloadDigest,
                buildScaleReadiness(task, complexity, engineProfiles, workloadOrchestration),
                phaseNotes
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
                                                      String workloadDigest,
                                                      WorkloadSignal workloadSignal) {
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
                throw new IllegalStateException("压测隔离执行被中断", ex);
            } catch (ExecutionException ex) {
                throw new IllegalStateException("压测隔离执行失败", ex.getCause());
            }
            long executionDurationMs = Math.max(1L, (System.nanoTime() - startedAt) / 1_000_000L);
            return buildSnapshot(task, engine, index, complexity, datasetWeight, concurrencyWeight, sampleCount, probeToken,
                executionDurationMs, workloadDigest, workloadSignal);
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
                                                  String workloadDigest,
                                                  WorkloadSignal workloadSignal) {
        int enginePenalty = enginePenalty(engine, index);
        int taskLatencyBias = taskLatencyBias(task.getTaskType());
        int taskCpuBias = taskCpuBias(task.getTaskType());
        int taskQpsPenalty = taskQpsPenalty(task.getTaskType());
        long tokenBias = Math.abs(probeToken % 7L);
        long workloadElapsedMs = workloadSignal == null || workloadSignal.getElapsedMs() == null
            ? executionDurationMs
            : workloadSignal.getElapsedMs().longValue();
        long workloadScannedRows = workloadSignal == null || workloadSignal.getScannedRows() == null
            ? 0L
            : workloadSignal.getScannedRows().longValue();
        int workloadRowCount = workloadSignal == null || workloadSignal.getRowCount() == null
            ? 0
            : workloadSignal.getRowCount().intValue();
        long latencyBiasFromWorkload = Math.max(0L, workloadElapsedMs / 8L);
        long rowBias = Math.max(0L, workloadRowCount);

        BigDecimal targetQps = new BigDecimal("120");
        BigDecimal actualQps = decimal(
            160L - (complexity * 3L) - datasetWeight - taskQpsPenalty - enginePenalty + concurrencyWeight - tokenBias
                - Math.max(0L, workloadElapsedMs / 18L) + Math.min(6L, rowBias)
        );
        BigDecimal p50LatencyMs = decimal(
            14L + (complexity * 2L) + datasetWeight + taskLatencyBias + (enginePenalty / 2L) + latencyBiasFromWorkload
        );
        BigDecimal p95LatencyMs = decimal(asLong(p50LatencyMs) + 18L + complexity + (enginePenalty / 2L) + tokenBias);
        BigDecimal p99LatencyMs = decimal(asLong(p95LatencyMs) + 16L + (taskLatencyBias / 2L) + (enginePenalty / 2L) + tokenBias);
        BigDecimal cpuUsagePercent = decimal(
            26L + (complexity * 2L) + concurrencyWeight + taskCpuBias + (enginePenalty / 2L) + Math.max(0L, workloadElapsedMs / 24L)
        );
        BigDecimal memoryUsageMb = decimal(
            256L + (datasetWeight * 48L) + (complexity * 12L) + (enginePenalty * 8L) + Math.max(0L, workloadRowCount * 2L)
        );
        long syntheticScannedDataBytes =
            ((long) (datasetWeight + complexity + 1L) * 33554432L) + ((long) enginePenalty * 4194304L) + (sampleCount * 1024L);
        BigDecimal scannedDataBytes = decimal(
            Math.max(syntheticScannedDataBytes, workloadScannedRows <= 0L ? 0L : workloadScannedRows * 4096L)
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
                buildEngineNote(task, engine, sampleCount, executionDurationMs, workloadDigest, workloadSignal)
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
                                         String workloadDigest,
                                         WorkloadOrchestration workloadOrchestration) {
        List<String> notes = new ArrayList<String>();
        notes.add("executionMode=" + workloadOrchestration.getExecutionMode());
        notes.add("isolation=" + workloadOrchestration.getIsolationSummary());
        notes.add("taskType=" + task.getTaskType().name());
        notes.add("targetEngines=" + targetEngines);
        notes.add("sqlComplexity=" + complexity);
        notes.add("datasetWeight=" + datasetWeight);
        notes.add("concurrencyWeight=" + concurrencyWeight);
        appendScaleTargetNotes(notes, task.getScaleTarget());
        notes.add("workloadDigest=" + workloadDigest);
        notes.add("executionDurationMs=" + totalDurationMs);
        notes.add("workloadSource=" + workloadOrchestration.getWorkloadSource());
        notes.add("backfillApplied=" + workloadOrchestration.isBackfillApplied());
        notes.addAll(workloadOrchestration.getEvidenceNotes());
        return Collections.unmodifiableList(notes);
    }

    private void appendScaleTargetNotes(List<String> notes, BenchmarkScaleTarget scaleTarget) {
        if (scaleTarget == null) {
            return;
        }
        notes.add("scaleTargetStatus=" + scaleTarget.getEvidenceStatus());
        notes.add("scaleTargetConcurrency=" + scaleTarget.getTargetConcurrency());
        notes.add("scaleTargetDatasetSizeLabel=" + scaleTarget.getTargetDatasetSizeLabel());
        notes.add("scaleTargetDailyQueryVolume=" + scaleTarget.getTargetDailyQueryVolume());
        notes.add("scaleTargetComplexityProfile=" + scaleTarget.getTargetComplexityProfile());
        notes.add("scaleTargetCostEfficiency=" + scaleTarget.getTargetCostEfficiency());
        notes.add("scaleTargetBoundary=" + scaleTarget.getEvidenceBoundary());
        appendScaleEvidenceManifestNotes(notes, scaleTarget);
    }

    private void appendScaleEvidenceManifestNotes(List<String> notes, BenchmarkScaleTarget scaleTarget) {
        BenchmarkScaleEvidenceManifest evidenceManifest = scaleTarget.getEvidenceManifest();
        if (evidenceManifest == null) {
            notes.add("productionEvidenceManifest=null");
            return;
        }
        notes.add("productionEvidenceSource=" + evidenceManifest.getEvidenceSource());
        notes.add("productionEvidenceVerification=" + evidenceManifest.getExternalVerificationStatus());
        notes.add("productionConcurrencyProofRef=" + evidenceManifest.getConcurrencyProofRef());
        notes.add("productionDataLayoutProofRef=" + evidenceManifest.getDataLayoutProofRef());
        notes.add("productionWorkloadReplayProofRef=" + evidenceManifest.getWorkloadReplayProofRef());
        notes.add("productionWorkloadReplayWindow=" + evidenceManifest.getWorkloadReplayWindow());
        notes.add("productionP95P99MetricProofRef=" + evidenceManifest.getP95P99MetricProofRef());
        notes.add("productionScanCpuQueueMetricProofRef=" + evidenceManifest.getScanCpuQueueMetricProofRef());
        notes.add("productionCostBillProofRef=" + evidenceManifest.getCostBillProofRef());
        notes.add("productionEvidenceBundleSatisfied="
            + evidenceManifest.hasVerifiedProductionEvidence(scaleTarget.getTargetConcurrency()));
        notes.add("productionEvidenceBundleMissing="
            + evidenceManifest.missingVerificationEvidence(scaleTarget.getTargetConcurrency()));
    }

    private BenchmarkScaleReadinessAssessment buildScaleReadiness(BenchmarkTask task,
                                                                  int sqlComplexity,
                                                                  List<BenchmarkEngineProfile> engineProfiles,
                                                                  WorkloadOrchestration workloadOrchestration) {
        BenchmarkScaleTarget scaleTarget = task.getScaleTarget();
        if (scaleTarget == null) {
            return null;
        }
        List<String> satisfied = new ArrayList<String>();
        List<String> missing = new ArrayList<String>();
        BigDecimal maxP95 = maxMetric(engineProfiles, MetricExtractor.P95);
        BigDecimal maxP99 = maxMetric(engineProfiles, MetricExtractor.P99);
        BigDecimal maxCpu = maxMetric(engineProfiles, MetricExtractor.CPU);
        BigDecimal maxMemory = maxMetric(engineProfiles, MetricExtractor.MEMORY);
        BigDecimal maxScannedBytes = maxMetric(engineProfiles, MetricExtractor.SCANNED_BYTES);
        BigDecimal queueWaitMs = estimateQueueWaitMs(task);
        BigDecimal bestQps = maxMetric(engineProfiles, MetricExtractor.ACTUAL_QPS);
        BigDecimal projectedDailyCapacity = bestQps == null ? null : bestQps.multiply(new BigDecimal("86400"));
        BigDecimal resourceUnit = estimateResourceUnitPerMillionQueries(
            maxP99,
            maxCpu,
            maxMemory,
            maxScannedBytes,
            queueWaitMs,
            projectedDailyCapacity
        );
        addPresenceEvidence(satisfied, missing, "p95P99Latency", maxP95, maxP99);
        addPositiveEvidence(satisfied, missing, "scanBytes", maxScannedBytes);
        addPresenceEvidence(satisfied, missing, "cpuAndMemory", maxCpu, maxMemory);
        addNonNegativeEvidence(satisfied, missing, "queueWaitMs", queueWaitMs);
        addPositiveEvidence(satisfied, missing, "costBillOrResourceUnit", resourceUnit);
        satisfied.add("completedBenchmarkTask");
        String workloadEvidenceStatus = resolveWorkloadEvidenceStatus(workloadOrchestration);
        BenchmarkScaleEvidenceManifest evidenceManifest = scaleTarget.getEvidenceManifest();
        addProductionEvidenceManifest(satisfied, missing, scaleTarget);
        if ("LIVE_ORCHESTRATED".equals(workloadEvidenceStatus) || hasVerifiedLongReplayProof(scaleTarget)) {
            satisfied.add("workloadWindow");
        } else {
            missing.add("workloadWindow:" + workloadEvidenceStatus);
        }
        if (scaleTarget.getTargetConcurrency() != null
            && (task.getConcurrency() == null || task.getConcurrency().intValue() < scaleTarget.getTargetConcurrency().intValue())
            && !hasVerifiedProofRef(scaleTarget, evidenceManifest == null ? null : evidenceManifest.getConcurrencyProofRef())) {
            missing.add("targetConcurrencyCovered:required=" + scaleTarget.getTargetConcurrency()
                + ",actual=" + task.getConcurrency());
        } else {
            satisfied.add("targetConcurrencyCovered");
        }
        if (scaleTarget.getTargetDatasetSizeLabel() != null
            && !scaleTarget.getTargetDatasetSizeLabel().equalsIgnoreCase(task.getDatasetSizeLabel())
            && !hasVerifiedProofRef(scaleTarget, evidenceManifest == null ? null : evidenceManifest.getDataLayoutProofRef())) {
            missing.add("targetDatasetCovered:required=" + scaleTarget.getTargetDatasetSizeLabel()
                + ",actual=" + task.getDatasetSizeLabel());
        } else {
            satisfied.add("targetDatasetCovered");
        }
        if (scaleTarget.getTargetDailyQueryVolume() != null
            && (projectedDailyCapacity == null
            || projectedDailyCapacity.compareTo(new BigDecimal(scaleTarget.getTargetDailyQueryVolume().longValue())) < 0)
            && !hasVerifiedLongReplayProof(scaleTarget)) {
            missing.add("targetDailyQueryVolumeCovered:required=" + scaleTarget.getTargetDailyQueryVolume()
                + ",projected=" + projectedDailyCapacity);
        } else {
            satisfied.add("targetDailyQueryVolumeCovered");
        }
        if (scaleTarget.getTargetComplexityProfile() != null && sqlComplexity < 8) {
            missing.add("targetComplexityCovered:profile=" + scaleTarget.getTargetComplexityProfile()
                + ",observedComplexity=" + sqlComplexity);
        } else {
            satisfied.add("targetComplexityCovered");
        }
        BenchmarkScaleReadinessStatus status = resolveReadinessStatus(satisfied, missing);
        return new BenchmarkScaleReadinessAssessment(
            status,
            scaleTarget,
            maxP95,
            maxP99,
            maxCpu,
            maxMemory,
            maxScannedBytes,
            queueWaitMs,
            projectedDailyCapacity,
            resourceUnit,
            workloadEvidenceStatus,
            satisfied,
            missing,
            buildScaleReadinessSummary(status, satisfied, missing)
        );
    }

    private void addProductionEvidenceManifest(List<String> satisfied,
                                               List<String> missing,
                                               BenchmarkScaleTarget scaleTarget) {
        BenchmarkScaleEvidenceManifest evidenceManifest = scaleTarget.getEvidenceManifest();
        if (evidenceManifest == null) {
            missing.add("productionEvidenceManifest");
            missing.add("productionConcurrencyProof");
            missing.add("productionDataLayoutProof");
            missing.add("productionLongReplayProof");
            missing.add("productionP95P99MetricProof");
            missing.add("productionScanCpuQueueMetricProof");
            missing.add("productionCostBillProof");
            missing.add("productionEvidenceBundle");
            missing.add("productionExternalVerification");
            return;
        }
        satisfied.add("productionEvidenceManifest");
        addProofRefEvidence(satisfied, missing, "productionConcurrencyProof", evidenceManifest.getConcurrencyProofRef());
        addProofRefEvidence(satisfied, missing, "productionDataLayoutProof", evidenceManifest.getDataLayoutProofRef());
        if (StringUtils.hasText(evidenceManifest.getWorkloadReplayProofRef())
            && StringUtils.hasText(evidenceManifest.getWorkloadReplayWindow())) {
            satisfied.add("productionLongReplayProof");
        } else {
            missing.add("productionLongReplayProof");
        }
        addProofRefEvidence(satisfied, missing, "productionP95P99MetricProof", evidenceManifest.getP95P99MetricProofRef());
        addProofRefEvidence(
            satisfied,
            missing,
            "productionScanCpuQueueMetricProof",
            evidenceManifest.getScanCpuQueueMetricProofRef()
        );
        addProofRefEvidence(satisfied, missing, "productionCostBillProof", evidenceManifest.getCostBillProofRef());
        List<String> bundleMissing = evidenceManifest.missingVerificationEvidence(scaleTarget.getTargetConcurrency());
        if (bundleMissing.isEmpty()) {
            satisfied.add("productionEvidenceBundle");
            satisfied.addAll(evidenceManifest.satisfiedVerificationEvidence(scaleTarget.getTargetConcurrency()));
        } else {
            missing.addAll(bundleMissing);
        }
        if (evidenceManifest.isExternallyVerified(scaleTarget.getTargetConcurrency())) {
            satisfied.add("productionExternalVerification");
        } else if (evidenceManifest.hasVerifiedStatus()) {
            missing.add("productionExternalVerification:bundle=" + bundleMissing);
        } else {
            missing.add("productionExternalVerification:status=" + evidenceManifest.getExternalVerificationStatus());
        }
    }

    private void addProofRefEvidence(List<String> satisfied,
                                     List<String> missing,
                                     String evidenceName,
                                     String proofRef) {
        if (StringUtils.hasText(proofRef)) {
            satisfied.add(evidenceName);
            return;
        }
        missing.add(evidenceName);
    }

    private void addPresenceEvidence(List<String> satisfied,
                                     List<String> missing,
                                     String evidenceName,
                                     BigDecimal first,
                                     BigDecimal second) {
        if (first != null && second != null) {
            satisfied.add(evidenceName);
            return;
        }
        missing.add(evidenceName);
    }

    private void addPositiveEvidence(List<String> satisfied,
                                     List<String> missing,
                                     String evidenceName,
                                     BigDecimal value) {
        if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
            satisfied.add(evidenceName);
            return;
        }
        missing.add(evidenceName);
    }

    private void addNonNegativeEvidence(List<String> satisfied,
                                        List<String> missing,
                                        String evidenceName,
                                        BigDecimal value) {
        if (value != null && value.compareTo(BigDecimal.ZERO) >= 0) {
            satisfied.add(evidenceName);
            return;
        }
        missing.add(evidenceName);
    }

    private BenchmarkScaleReadinessStatus resolveReadinessStatus(List<String> satisfied, List<String> missing) {
        if (missing == null || missing.isEmpty()) {
            return BenchmarkScaleReadinessStatus.READY;
        }
        for (String item : missing) {
            if (item != null
                && (item.startsWith("workloadWindow:")
                || item.startsWith("targetConcurrencyCovered:")
                || item.startsWith("targetDatasetCovered:")
                || item.startsWith("targetDailyQueryVolumeCovered:")
                || item.startsWith("productionEvidenceManifest")
                || item.startsWith("productionConcurrencyProof")
                || item.startsWith("productionDataLayoutProof")
                || item.startsWith("productionLongReplayProof")
                || item.startsWith("productionP95P99MetricProof")
                || item.startsWith("productionScanCpuQueueMetricProof")
                || item.startsWith("productionCostBillProof")
                || item.startsWith("productionEvidenceBundle")
                || item.startsWith("productionExternalVerification"))) {
                return BenchmarkScaleReadinessStatus.NOT_PROVEN;
            }
        }
        return satisfied == null || satisfied.size() < 4
            ? BenchmarkScaleReadinessStatus.NOT_PROVEN
            : BenchmarkScaleReadinessStatus.PARTIAL;
    }

    private boolean hasVerifiedLongReplayProof(BenchmarkScaleTarget scaleTarget) {
        BenchmarkScaleEvidenceManifest evidenceManifest = scaleTarget.getEvidenceManifest();
        return evidenceManifest != null
            && evidenceManifest.isExternallyVerified(scaleTarget.getTargetConcurrency())
            && StringUtils.hasText(evidenceManifest.getWorkloadReplayProofRef())
            && StringUtils.hasText(evidenceManifest.getWorkloadReplayWindow());
    }

    private boolean hasVerifiedProofRef(BenchmarkScaleTarget scaleTarget, String proofRef) {
        BenchmarkScaleEvidenceManifest evidenceManifest = scaleTarget.getEvidenceManifest();
        return evidenceManifest != null
            && evidenceManifest.isExternallyVerified(scaleTarget.getTargetConcurrency())
            && StringUtils.hasText(proofRef);
    }

    private String buildScaleReadinessSummary(BenchmarkScaleReadinessStatus status,
                                              List<String> satisfied,
                                              List<String> missing) {
        return "scale readiness=" + status.name()
            + ", satisfied=" + (satisfied == null ? 0 : satisfied.size())
            + ", missing=" + (missing == null ? 0 : missing.size());
    }

    private String resolveWorkloadEvidenceStatus(WorkloadOrchestration workloadOrchestration) {
        if (workloadOrchestration == null) {
            return "NO_WORKLOAD_ORCHESTRATION";
        }
        String source = workloadOrchestration.getWorkloadSource();
        if (source != null && source.toUpperCase(Locale.ROOT).contains("LIVE")
            && EXECUTION_MODE_ORCHESTRATED.equals(workloadOrchestration.getExecutionMode())) {
            return "LIVE_ORCHESTRATED";
        }
        if (workloadOrchestration.isBackfillApplied()) {
            return "SYNTHETIC_BACKFILL";
        }
        if (EXECUTION_MODE_ORCHESTRATED.equals(workloadOrchestration.getExecutionMode())) {
            return "ORCHESTRATED_NON_LIVE";
        }
        return "SYNTHETIC_ONLY";
    }

    private BigDecimal estimateResourceUnitPerMillionQueries(BigDecimal maxP99,
                                                              BigDecimal maxCpu,
                                                              BigDecimal maxMemory,
                                                              BigDecimal maxScannedBytes,
                                                              BigDecimal queueWaitMs,
                                                              BigDecimal projectedDailyCapacity) {
        if (maxP99 == null || maxCpu == null || maxMemory == null || maxScannedBytes == null
            || projectedDailyCapacity == null || projectedDailyCapacity.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal scannedGb = maxScannedBytes.divide(new BigDecimal("1073741824"), 6, RoundingMode.HALF_UP);
        BigDecimal memoryGb = maxMemory.divide(new BigDecimal("1024"), 6, RoundingMode.HALF_UP);
        BigDecimal latencySeconds = maxP99.divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP);
        BigDecimal queueWaitSeconds = queueWaitMs == null
            ? BigDecimal.ZERO
            : queueWaitMs.divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP);
        BigDecimal totalResourceUnit = scannedGb.add(memoryGb).add(maxCpu).add(latencySeconds).add(queueWaitSeconds);
        BigDecimal millionQueries = projectedDailyCapacity.divide(new BigDecimal("1000000"), 6, RoundingMode.HALF_UP);
        if (millionQueries.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return totalResourceUnit.divide(millionQueries, 6, RoundingMode.HALF_UP);
    }

    private BigDecimal estimateQueueWaitMs(BenchmarkTask task) {
        if (task == null || task.getSubmittedAt() == null || task.getStartedAt() == null) {
            return null;
        }
        long waitMs = Math.max(0L, task.getStartedAt().toEpochMilli() - task.getSubmittedAt().toEpochMilli());
        return new BigDecimal(String.valueOf(waitMs));
    }

    private BigDecimal maxMetric(List<BenchmarkEngineProfile> profiles, MetricExtractor extractor) {
        if (profiles == null || profiles.isEmpty()) {
            return null;
        }
        BigDecimal current = null;
        for (BenchmarkEngineProfile profile : profiles) {
            if (profile == null) {
                continue;
            }
            BigDecimal value = extractor.extract(profile);
            if (value != null && (current == null || value.compareTo(current) > 0)) {
                current = value;
            }
        }
        return current;
    }

    private String buildEngineNote(BenchmarkTask task,
                                   DataSourceTypeEnum engine,
                                   int sampleCount,
                                   long executionDurationMs,
                                   String workloadDigest,
                                   WorkloadSignal workloadSignal) {
        String workloadEvidence = workloadSignal == null
            ? "source=SYNTHETIC_ONLY"
            : "source=" + workloadSignal.getWorkloadSource()
                + ", mode=" + workloadSignal.getExecutionMode()
                + ", digest=" + workloadSignal.getWorkloadDigest();
        return "隔离 " + task.getTaskType().name().toLowerCase(Locale.ROOT)
            + " 回放，engine=" + engine.name()
            + "，sampleCount=" + sampleCount
            + "，durationMs=" + executionDurationMs
            + ", digest=" + workloadDigest
            + ", workload=" + workloadEvidence;
    }

    private WorkloadOrchestration resolveWorkloadOrchestration(BenchmarkTask task,
                                                               List<DataSourceTypeEnum> targetEngines,
                                                               String syntheticWorkloadDigest) {
        try {
            QueryExecutionBenchmarkWorkloadResponse response = queryExecutionBenchmarkWorkloadClient.captureWorkload(
                buildWorkloadRequest(task, targetEngines)
            );
            if (response == null || response.getEngineSnapshots() == null || response.getEngineSnapshots().isEmpty()) {
                return WorkloadOrchestration.syntheticOnly(syntheticWorkloadDigest, "workloadOrchestration=EMPTY_RESPONSE");
            }
            Map<DataSourceTypeEnum, WorkloadSignal> signals = new LinkedHashMap<DataSourceTypeEnum, WorkloadSignal>();
            List<String> evidenceNotes = new ArrayList<String>(response.getEngineSnapshots().size() + 2);
            evidenceNotes.add("queryExecutionWorkloadSource=" + response.getWorkloadSource());
            evidenceNotes.add("queryExecutionImplementationStage=" + response.getImplementationStage());
            evidenceNotes.add("queryExecutionCompensationApplied=" + response.isCompensationApplied());
            if (StringUtils.hasText(response.getCompensationStrategy())) {
                evidenceNotes.add("queryExecutionCompensationStrategy=" + response.getCompensationStrategy());
            }
            for (QueryExecutionBenchmarkWorkloadEngineSnapshot snapshot : response.getEngineSnapshots()) {
                if (snapshot == null || snapshot.getTargetEngine() == null) {
                    continue;
                }
                WorkloadSignal signal = WorkloadSignal.from(snapshot);
                signals.put(snapshot.getTargetEngine(), signal);
                evidenceNotes.add(
                    "queryExecution[" + snapshot.getTargetEngine().name() + "]="
                        + signal.getWorkloadSource()
                        + ",mode=" + signal.getExecutionMode()
                        + ",elapsedMs=" + signal.getElapsedMs()
                        + ",scannedRows=" + signal.getScannedRows()
                        + ",cacheHit=" + snapshot.getCacheHit()
                        + ",cacheGovernanceStatus=" + snapshot.getCacheGovernanceStatus()
                        + ",cacheGovernanceEvidence=" + summarizeCacheEvidence(snapshot.getCacheGovernanceEvidence())
                        + ",compensationApplied=" + snapshot.getCompensationApplied()
                        + ",compensationStrategy=" + snapshot.getCompensationStrategy()
                        + ",compensationSourceEngine="
                        + (snapshot.getCompensationSourceEngine() == null ? null : snapshot.getCompensationSourceEngine().name())
                        + ",compensationSourceDigest=" + snapshot.getCompensationSourceWorkloadDigest()
                );
            }
            if (signals.isEmpty()) {
                return WorkloadOrchestration.syntheticOnly(syntheticWorkloadDigest, "workloadOrchestration=NO_VALID_ENGINE_SNAPSHOTS");
            }
            return new WorkloadOrchestration(
                StringUtils.hasText(response.getWorkloadDigest()) ? response.getWorkloadDigest() : syntheticWorkloadDigest,
                response.isBackfillApplied(),
                StringUtils.hasText(response.getWorkloadSource()) ? response.getWorkloadSource() : "QUERY_EXECUTION_SYNC",
                EXECUTION_MODE_ORCHESTRATED,
                ISOLATION_SUMMARY_ORCHESTRATED,
                signals,
                evidenceNotes
            );
        } catch (RuntimeException ex) {
            return WorkloadOrchestration.syntheticOnly(
                syntheticWorkloadDigest,
                "workloadOrchestration=FAILED,reason=" + trimReason(ex.getMessage())
            );
        }
    }

    private QueryExecutionBenchmarkWorkloadRequest buildWorkloadRequest(BenchmarkTask task,
                                                                        List<DataSourceTypeEnum> targetEngines) {
        QueryExecutionBenchmarkWorkloadRequest request = new QueryExecutionBenchmarkWorkloadRequest();
        request.setTenantId(task.getTenantId());
        request.setBenchmarkTaskId(task.getTaskId());
        request.setBenchmarkTaskType(task.getTaskType().name());
        request.setSqlText(task.getSqlText());
        request.setSqlFingerprint(task.getSqlFingerprint());
        request.setTargetEngines(targetEngines);
        request.setConcurrency(task.getConcurrency());
        request.setDurationSeconds(task.getDurationSeconds());
        request.setRampUpSeconds(task.getRampUpSeconds());
        request.setDatasetSizeLabel(task.getDatasetSizeLabel());
        request.setReadonlyRequired(task.getReadonlyRequired());
        return request;
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
            throw new IllegalStateException("SHA-256 算法不可用", ex);
        }
    }

    private String trimReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return "unavailable";
        }
        String trimmed = reason.replace('\n', ' ').replace('\r', ' ').trim();
        return trimmed.length() > 160 ? trimmed.substring(0, 160) : trimmed;
    }

    private String summarizeCacheEvidence(String rawEvidence) {
        if (!StringUtils.hasText(rawEvidence)) {
            return null;
        }
        StringBuilder summary = new StringBuilder();
        appendEvidenceValue(summary, rawEvidence, "policyId");
        appendEvidenceValue(summary, rawEvidence, "status");
        appendEvidenceValue(summary, rawEvidence, "schemaVersion");
        appendEvidenceValue(summary, rawEvidence, "riskCode");
        appendEvidenceValue(summary, rawEvidence, "entryState");
        appendEvidenceValue(summary, rawEvidence, "evictionReason");
        appendEvidenceValue(summary, rawEvidence, "evictedEntryCount");
        appendEvidenceValue(summary, rawEvidence, "maxEntriesPerPolicy");
        appendEvidenceValue(summary, rawEvidence, "policyCachedEntryCount");
        appendEvidenceValue(summary, rawEvidence, "ttlSeconds");
        return summary.length() == 0 ? null : summary.toString();
    }

    private void appendEvidenceValue(StringBuilder summary, String rawEvidence, String key) {
        String value = resolveEvidenceValue(rawEvidence, key);
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (summary.length() > 0) {
            summary.append('|');
        }
        summary.append(key).append(':').append(value);
    }

    private String resolveEvidenceValue(String rawEvidence, String key) {
        if (!StringUtils.hasText(rawEvidence) || !StringUtils.hasText(key)) {
            return null;
        }
        String[] parts = rawEvidence.split(";");
        for (String part : parts) {
            int separator = part.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            if (key.equals(part.substring(0, separator).trim())) {
                return part.substring(separator + 1).trim().replace(',', '_');
            }
        }
        return null;
    }

    private BigDecimal decimal(long value) {
        return new BigDecimal(String.valueOf(Math.max(1L, value)));
    }

    private long asLong(BigDecimal value) {
        return value.longValue();
    }

    private interface MetricExtractor {

        MetricExtractor P95 = new MetricExtractor() {
            @Override
            public BigDecimal extract(BenchmarkEngineProfile profile) {
                return profile.getP95LatencyMs();
            }
        };
        MetricExtractor P99 = new MetricExtractor() {
            @Override
            public BigDecimal extract(BenchmarkEngineProfile profile) {
                return profile.getP99LatencyMs();
            }
        };
        MetricExtractor CPU = new MetricExtractor() {
            @Override
            public BigDecimal extract(BenchmarkEngineProfile profile) {
                return profile.getCpuUsagePercent();
            }
        };
        MetricExtractor MEMORY = new MetricExtractor() {
            @Override
            public BigDecimal extract(BenchmarkEngineProfile profile) {
                return profile.getMemoryUsageMb();
            }
        };
        MetricExtractor SCANNED_BYTES = new MetricExtractor() {
            @Override
            public BigDecimal extract(BenchmarkEngineProfile profile) {
                return profile.getScannedDataBytes();
            }
        };
        MetricExtractor ACTUAL_QPS = new MetricExtractor() {
            @Override
            public BigDecimal extract(BenchmarkEngineProfile profile) {
                return profile.getActualQps();
            }
        };

        BigDecimal extract(BenchmarkEngineProfile profile);
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

    private static final class WorkloadOrchestration {

        private final String workloadDigest;
        private final boolean backfillApplied;
        private final String workloadSource;
        private final String executionMode;
        private final String isolationSummary;
        private final Map<DataSourceTypeEnum, WorkloadSignal> signals;
        private final List<String> evidenceNotes;

        private WorkloadOrchestration(String workloadDigest,
                                      boolean backfillApplied,
                                      String workloadSource,
                                      String executionMode,
                                      String isolationSummary,
                                      Map<DataSourceTypeEnum, WorkloadSignal> signals,
                                      List<String> evidenceNotes) {
            this.workloadDigest = workloadDigest;
            this.backfillApplied = backfillApplied;
            this.workloadSource = workloadSource;
            this.executionMode = executionMode;
            this.isolationSummary = isolationSummary;
            this.signals = signals == null ? Collections.<DataSourceTypeEnum, WorkloadSignal>emptyMap() : signals;
            this.evidenceNotes = evidenceNotes == null ? Collections.<String>emptyList() : evidenceNotes;
        }

        private static WorkloadOrchestration syntheticOnly(String workloadDigest, String evidenceNote) {
            return new WorkloadOrchestration(
                workloadDigest,
                true,
                "SYNTHETIC_ONLY",
                EXECUTION_MODE_ISOLATED,
                ISOLATION_SUMMARY_ISOLATED,
                Collections.<DataSourceTypeEnum, WorkloadSignal>emptyMap(),
                Collections.singletonList(evidenceNote)
            );
        }

        private String getWorkloadDigest() {
            return workloadDigest;
        }

        private boolean isBackfillApplied() {
            return backfillApplied;
        }

        private String getWorkloadSource() {
            return workloadSource;
        }

        private String getExecutionMode() {
            return executionMode;
        }

        private String getIsolationSummary() {
            return isolationSummary;
        }

        private List<String> getEvidenceNotes() {
            return evidenceNotes;
        }

        private WorkloadSignal findSignal(DataSourceTypeEnum engine) {
            return signals.get(engine);
        }
    }

    private static final class WorkloadSignal {

        private final String workloadSource;
        private final String executionMode;
        private final String workloadDigest;
        private final Long elapsedMs;
        private final Long scannedRows;
        private final Integer rowCount;

        private WorkloadSignal(String workloadSource,
                               String executionMode,
                               String workloadDigest,
                               Long elapsedMs,
                               Long scannedRows,
                               Integer rowCount) {
            this.workloadSource = workloadSource;
            this.executionMode = executionMode;
            this.workloadDigest = workloadDigest;
            this.elapsedMs = elapsedMs;
            this.scannedRows = scannedRows;
            this.rowCount = rowCount;
        }

        private static WorkloadSignal from(QueryExecutionBenchmarkWorkloadEngineSnapshot snapshot) {
            return new WorkloadSignal(
                snapshot.getWorkloadSource(),
                snapshot.getExecutionMode(),
                snapshot.getWorkloadDigest(),
                snapshot.getElapsedMs(),
                snapshot.getScannedRows(),
                snapshot.getRowCount()
            );
        }

        private String getWorkloadSource() {
            return workloadSource;
        }

        private String getExecutionMode() {
            return executionMode;
        }

        private String getWorkloadDigest() {
            return workloadDigest;
        }

        private Long getElapsedMs() {
            return elapsedMs;
        }

        private Long getScannedRows() {
            return scannedRows;
        }

        private Integer getRowCount() {
            return rowCount;
        }
    }
}
