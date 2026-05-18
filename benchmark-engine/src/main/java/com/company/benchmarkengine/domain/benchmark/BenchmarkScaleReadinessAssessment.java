package com.company.benchmarkengine.domain.benchmark;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BenchmarkScaleReadinessAssessment {

    private final BenchmarkScaleReadinessStatus readinessStatus;
    private final BenchmarkScaleTarget scaleTarget;
    private final BigDecimal observedMaxP95LatencyMs;
    private final BigDecimal observedMaxP99LatencyMs;
    private final BigDecimal observedMaxCpuUsagePercent;
    private final BigDecimal observedMaxMemoryUsageMb;
    private final BigDecimal observedMaxScannedDataBytes;
    private final BigDecimal observedQueueWaitMs;
    private final BigDecimal projectedDailyQueryCapacity;
    private final BigDecimal estimatedResourceUnitPerMillionQueries;
    private final String workloadEvidenceStatus;
    private final List<String> satisfiedEvidence;
    private final List<String> missingEvidence;
    private final String summary;

    public BenchmarkScaleReadinessAssessment(BenchmarkScaleReadinessStatus readinessStatus,
                                             BenchmarkScaleTarget scaleTarget,
                                             BigDecimal observedMaxP95LatencyMs,
                                             BigDecimal observedMaxP99LatencyMs,
                                             BigDecimal observedMaxCpuUsagePercent,
                                             BigDecimal observedMaxMemoryUsageMb,
                                             BigDecimal observedMaxScannedDataBytes,
                                             BigDecimal observedQueueWaitMs,
                                             BigDecimal projectedDailyQueryCapacity,
                                             BigDecimal estimatedResourceUnitPerMillionQueries,
                                             String workloadEvidenceStatus,
                                             List<String> satisfiedEvidence,
                                             List<String> missingEvidence,
                                             String summary) {
        this.readinessStatus = readinessStatus == null ? BenchmarkScaleReadinessStatus.NOT_PROVEN : readinessStatus;
        this.scaleTarget = scaleTarget;
        this.observedMaxP95LatencyMs = observedMaxP95LatencyMs;
        this.observedMaxP99LatencyMs = observedMaxP99LatencyMs;
        this.observedMaxCpuUsagePercent = observedMaxCpuUsagePercent;
        this.observedMaxMemoryUsageMb = observedMaxMemoryUsageMb;
        this.observedMaxScannedDataBytes = observedMaxScannedDataBytes;
        this.observedQueueWaitMs = observedQueueWaitMs;
        this.projectedDailyQueryCapacity = projectedDailyQueryCapacity;
        this.estimatedResourceUnitPerMillionQueries = estimatedResourceUnitPerMillionQueries;
        this.workloadEvidenceStatus = workloadEvidenceStatus;
        this.satisfiedEvidence = immutableCopy(satisfiedEvidence);
        this.missingEvidence = immutableCopy(missingEvidence);
        this.summary = summary;
    }

    private <T> List<T> immutableCopy(List<T> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<T>(items));
    }

    public BenchmarkScaleReadinessStatus getReadinessStatus() {
        return readinessStatus;
    }

    public BenchmarkScaleTarget getScaleTarget() {
        return scaleTarget;
    }

    public BigDecimal getObservedMaxP95LatencyMs() {
        return observedMaxP95LatencyMs;
    }

    public BigDecimal getObservedMaxP99LatencyMs() {
        return observedMaxP99LatencyMs;
    }

    public BigDecimal getObservedMaxCpuUsagePercent() {
        return observedMaxCpuUsagePercent;
    }

    public BigDecimal getObservedMaxMemoryUsageMb() {
        return observedMaxMemoryUsageMb;
    }

    public BigDecimal getObservedMaxScannedDataBytes() {
        return observedMaxScannedDataBytes;
    }

    public BigDecimal getObservedQueueWaitMs() {
        return observedQueueWaitMs;
    }

    public BigDecimal getProjectedDailyQueryCapacity() {
        return projectedDailyQueryCapacity;
    }

    public BigDecimal getEstimatedResourceUnitPerMillionQueries() {
        return estimatedResourceUnitPerMillionQueries;
    }

    public String getWorkloadEvidenceStatus() {
        return workloadEvidenceStatus;
    }

    public List<String> getSatisfiedEvidence() {
        return satisfiedEvidence;
    }

    public List<String> getMissingEvidence() {
        return missingEvidence;
    }

    public String getSummary() {
        return summary;
    }
}
