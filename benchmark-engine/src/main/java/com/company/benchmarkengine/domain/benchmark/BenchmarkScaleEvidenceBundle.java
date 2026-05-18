package com.company.benchmarkengine.domain.benchmark;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BenchmarkScaleEvidenceBundle {

    public static final int MIN_PRODUCTION_CONCURRENCY = 10000;
    public static final long MIN_PRODUCTION_DATASET_SIZE_BYTES = 30000000000000000L;
    public static final BigDecimal MIN_LONG_REPLAY_HOURS = new BigDecimal("24");

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final Integer observedConcurrency;
    private final Long observedDatasetSizeBytes;
    private final BigDecimal workloadReplayDurationHours;
    private final BigDecimal p95LatencyMs;
    private final BigDecimal p99LatencyMs;
    private final Long scannedBytes;
    private final BigDecimal cpuUsagePercent;
    private final BigDecimal queueWaitMs;
    private final BigDecimal costBillAmount;
    private final String costBillCurrency;
    private final String verifierRef;

    public BenchmarkScaleEvidenceBundle(Integer observedConcurrency,
                                        Long observedDatasetSizeBytes,
                                        BigDecimal workloadReplayDurationHours,
                                        BigDecimal p95LatencyMs,
                                        BigDecimal p99LatencyMs,
                                        Long scannedBytes,
                                        BigDecimal cpuUsagePercent,
                                        BigDecimal queueWaitMs,
                                        BigDecimal costBillAmount,
                                        String costBillCurrency,
                                        String verifierRef) {
        this.observedConcurrency = observedConcurrency;
        this.observedDatasetSizeBytes = observedDatasetSizeBytes;
        this.workloadReplayDurationHours = workloadReplayDurationHours;
        this.p95LatencyMs = p95LatencyMs;
        this.p99LatencyMs = p99LatencyMs;
        this.scannedBytes = scannedBytes;
        this.cpuUsagePercent = cpuUsagePercent;
        this.queueWaitMs = queueWaitMs;
        this.costBillAmount = costBillAmount;
        this.costBillCurrency = normalizeText(costBillCurrency);
        this.verifierRef = normalizeText(verifierRef);
    }

    public boolean hasAnyEvidence() {
        return observedConcurrency != null
            || observedDatasetSizeBytes != null
            || workloadReplayDurationHours != null
            || p95LatencyMs != null
            || p99LatencyMs != null
            || scannedBytes != null
            || cpuUsagePercent != null
            || queueWaitMs != null
            || costBillAmount != null
            || hasText(costBillCurrency)
            || hasText(verifierRef);
    }

    public boolean satisfiesProductionEvidence(Integer targetConcurrency) {
        return missingProductionEvidence(targetConcurrency).isEmpty();
    }

    public List<String> satisfiedProductionEvidence(Integer targetConcurrency) {
        List<String> satisfied = new ArrayList<String>();
        int requiredConcurrency = requiredConcurrency(targetConcurrency);
        if (observedConcurrency != null && observedConcurrency.intValue() >= requiredConcurrency) {
            satisfied.add("productionEvidenceBundle.concurrency");
        }
        if (observedDatasetSizeBytes != null
            && observedDatasetSizeBytes.longValue() >= MIN_PRODUCTION_DATASET_SIZE_BYTES) {
            satisfied.add("productionEvidenceBundle.dataLayout30Pb");
        }
        if (workloadReplayDurationHours != null
            && workloadReplayDurationHours.compareTo(MIN_LONG_REPLAY_HOURS) >= 0) {
            satisfied.add("productionEvidenceBundle.longReplay");
        }
        if (isPositive(p95LatencyMs) && isPositive(p99LatencyMs)) {
            satisfied.add("productionEvidenceBundle.p95P99Latency");
        }
        if (scannedBytes != null && scannedBytes.longValue() > 0L) {
            satisfied.add("productionEvidenceBundle.scanBytes");
        }
        if (isPositive(cpuUsagePercent)) {
            satisfied.add("productionEvidenceBundle.cpu");
        }
        if (queueWaitMs != null && queueWaitMs.compareTo(ZERO) >= 0) {
            satisfied.add("productionEvidenceBundle.queueWait");
        }
        if (isPositive(costBillAmount) && hasText(costBillCurrency)) {
            satisfied.add("productionEvidenceBundle.costBill");
        }
        if (hasText(verifierRef)) {
            satisfied.add("productionEvidenceBundle.verifierRef");
        }
        return Collections.unmodifiableList(satisfied);
    }

    public List<String> missingProductionEvidence(Integer targetConcurrency) {
        List<String> missing = new ArrayList<String>();
        int requiredConcurrency = requiredConcurrency(targetConcurrency);
        if (observedConcurrency == null || observedConcurrency.intValue() < requiredConcurrency) {
            missing.add("productionEvidenceBundle.concurrency:required=" + requiredConcurrency
                + ",actual=" + observedConcurrency);
        }
        if (observedDatasetSizeBytes == null
            || observedDatasetSizeBytes.longValue() < MIN_PRODUCTION_DATASET_SIZE_BYTES) {
            missing.add("productionEvidenceBundle.dataLayout30Pb:requiredBytes="
                + MIN_PRODUCTION_DATASET_SIZE_BYTES + ",actual=" + observedDatasetSizeBytes);
        }
        if (workloadReplayDurationHours == null
            || workloadReplayDurationHours.compareTo(MIN_LONG_REPLAY_HOURS) < 0) {
            missing.add("productionEvidenceBundle.longReplay:requiredHours="
                + MIN_LONG_REPLAY_HOURS + ",actual=" + workloadReplayDurationHours);
        }
        if (!isPositive(p95LatencyMs) || !isPositive(p99LatencyMs)) {
            missing.add("productionEvidenceBundle.p95P99Latency");
        }
        if (scannedBytes == null || scannedBytes.longValue() <= 0L) {
            missing.add("productionEvidenceBundle.scanBytes");
        }
        if (!isPositive(cpuUsagePercent)) {
            missing.add("productionEvidenceBundle.cpu");
        }
        if (queueWaitMs == null || queueWaitMs.compareTo(ZERO) < 0) {
            missing.add("productionEvidenceBundle.queueWait");
        }
        if (!isPositive(costBillAmount) || !hasText(costBillCurrency)) {
            missing.add("productionEvidenceBundle.costBill");
        }
        if (!hasText(verifierRef)) {
            missing.add("productionEvidenceBundle.verifierRef");
        }
        return Collections.unmodifiableList(missing);
    }

    public Integer getObservedConcurrency() {
        return observedConcurrency;
    }

    public Long getObservedDatasetSizeBytes() {
        return observedDatasetSizeBytes;
    }

    public BigDecimal getWorkloadReplayDurationHours() {
        return workloadReplayDurationHours;
    }

    public BigDecimal getP95LatencyMs() {
        return p95LatencyMs;
    }

    public BigDecimal getP99LatencyMs() {
        return p99LatencyMs;
    }

    public Long getScannedBytes() {
        return scannedBytes;
    }

    public BigDecimal getCpuUsagePercent() {
        return cpuUsagePercent;
    }

    public BigDecimal getQueueWaitMs() {
        return queueWaitMs;
    }

    public BigDecimal getCostBillAmount() {
        return costBillAmount;
    }

    public String getCostBillCurrency() {
        return costBillCurrency;
    }

    public String getVerifierRef() {
        return verifierRef;
    }

    private int requiredConcurrency(Integer targetConcurrency) {
        if (targetConcurrency == null || targetConcurrency.intValue() < MIN_PRODUCTION_CONCURRENCY) {
            return MIN_PRODUCTION_CONCURRENCY;
        }
        return targetConcurrency.intValue();
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(ZERO) > 0;
    }

    private String normalizeText(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }
}
