package com.company.benchmarkengine.domain.benchmark;

import java.util.Locale;

public class BenchmarkScaleEvidenceManifest {

    public static final String STATUS_VERIFIED = "VERIFIED";
    public static final String STATUS_UNVERIFIED = "UNVERIFIED";

    private final String evidenceSource;
    private final String concurrencyProofRef;
    private final String dataLayoutProofRef;
    private final String workloadReplayProofRef;
    private final String workloadReplayWindow;
    private final String p95P99MetricProofRef;
    private final String scanCpuQueueMetricProofRef;
    private final String costBillProofRef;
    private final String externalVerificationStatus;

    public BenchmarkScaleEvidenceManifest(String evidenceSource,
                                          String concurrencyProofRef,
                                          String dataLayoutProofRef,
                                          String workloadReplayProofRef,
                                          String workloadReplayWindow,
                                          String p95P99MetricProofRef,
                                          String scanCpuQueueMetricProofRef,
                                          String costBillProofRef,
                                          String externalVerificationStatus) {
        this.evidenceSource = normalizeText(evidenceSource);
        this.concurrencyProofRef = normalizeText(concurrencyProofRef);
        this.dataLayoutProofRef = normalizeText(dataLayoutProofRef);
        this.workloadReplayProofRef = normalizeText(workloadReplayProofRef);
        this.workloadReplayWindow = normalizeText(workloadReplayWindow);
        this.p95P99MetricProofRef = normalizeText(p95P99MetricProofRef);
        this.scanCpuQueueMetricProofRef = normalizeText(scanCpuQueueMetricProofRef);
        this.costBillProofRef = normalizeText(costBillProofRef);
        this.externalVerificationStatus = normalizeVerificationStatus(externalVerificationStatus);
    }

    public boolean hasAnyEvidence() {
        return hasText(evidenceSource)
            || hasText(concurrencyProofRef)
            || hasText(dataLayoutProofRef)
            || hasText(workloadReplayProofRef)
            || hasText(workloadReplayWindow)
            || hasText(p95P99MetricProofRef)
            || hasText(scanCpuQueueMetricProofRef)
            || hasText(costBillProofRef)
            || STATUS_VERIFIED.equals(externalVerificationStatus);
    }

    public boolean isExternallyVerified() {
        return STATUS_VERIFIED.equals(externalVerificationStatus);
    }

    public String getEvidenceSource() {
        return evidenceSource;
    }

    public String getConcurrencyProofRef() {
        return concurrencyProofRef;
    }

    public String getDataLayoutProofRef() {
        return dataLayoutProofRef;
    }

    public String getWorkloadReplayProofRef() {
        return workloadReplayProofRef;
    }

    public String getWorkloadReplayWindow() {
        return workloadReplayWindow;
    }

    public String getP95P99MetricProofRef() {
        return p95P99MetricProofRef;
    }

    public String getScanCpuQueueMetricProofRef() {
        return scanCpuQueueMetricProofRef;
    }

    public String getCostBillProofRef() {
        return costBillProofRef;
    }

    public String getExternalVerificationStatus() {
        return externalVerificationStatus;
    }

    private String normalizeVerificationStatus(String value) {
        if (!hasText(value)) {
            return STATUS_UNVERIFIED;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }
}
