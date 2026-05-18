package com.company.benchmarkengine.domain.benchmark;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class BenchmarkScaleEvidenceManifest {

    public static final String STATUS_VERIFIED = "VERIFIED";
    public static final String STATUS_UNVERIFIED = "UNVERIFIED";

    private final String evidenceSource;
    private final String concurrencyProofRef;
    private final String dailyQueryVolumeProofRef;
    private final String dataLayoutProofRef;
    private final String workloadReplayProofRef;
    private final String workloadReplayWindow;
    private final String p95P99MetricProofRef;
    private final String scanCpuQueueMetricProofRef;
    private final String costBillProofRef;
    private final String externalVerificationStatus;
    private final BenchmarkScaleEvidenceBundle verificationBundle;

    public BenchmarkScaleEvidenceManifest(String evidenceSource,
                                          String concurrencyProofRef,
                                          String dataLayoutProofRef,
                                          String workloadReplayProofRef,
                                          String workloadReplayWindow,
                                          String p95P99MetricProofRef,
                                          String scanCpuQueueMetricProofRef,
                                          String costBillProofRef,
                                          String externalVerificationStatus) {
        this(
            evidenceSource,
            concurrencyProofRef,
            null,
            dataLayoutProofRef,
            workloadReplayProofRef,
            workloadReplayWindow,
            p95P99MetricProofRef,
            scanCpuQueueMetricProofRef,
            costBillProofRef,
            externalVerificationStatus,
            null
        );
    }

    public BenchmarkScaleEvidenceManifest(String evidenceSource,
                                          String concurrencyProofRef,
                                          String dataLayoutProofRef,
                                          String workloadReplayProofRef,
                                          String workloadReplayWindow,
                                          String p95P99MetricProofRef,
                                          String scanCpuQueueMetricProofRef,
                                          String costBillProofRef,
                                          String externalVerificationStatus,
                                          BenchmarkScaleEvidenceBundle verificationBundle) {
        this(
            evidenceSource,
            concurrencyProofRef,
            null,
            dataLayoutProofRef,
            workloadReplayProofRef,
            workloadReplayWindow,
            p95P99MetricProofRef,
            scanCpuQueueMetricProofRef,
            costBillProofRef,
            externalVerificationStatus,
            verificationBundle
        );
    }

    public BenchmarkScaleEvidenceManifest(String evidenceSource,
                                          String concurrencyProofRef,
                                          String dailyQueryVolumeProofRef,
                                          String dataLayoutProofRef,
                                          String workloadReplayProofRef,
                                          String workloadReplayWindow,
                                          String p95P99MetricProofRef,
                                          String scanCpuQueueMetricProofRef,
                                          String costBillProofRef,
                                          String externalVerificationStatus,
                                          BenchmarkScaleEvidenceBundle verificationBundle) {
        this.evidenceSource = normalizeText(evidenceSource);
        this.concurrencyProofRef = normalizeText(concurrencyProofRef);
        this.dailyQueryVolumeProofRef = normalizeText(dailyQueryVolumeProofRef);
        this.dataLayoutProofRef = normalizeText(dataLayoutProofRef);
        this.workloadReplayProofRef = normalizeText(workloadReplayProofRef);
        this.workloadReplayWindow = normalizeText(workloadReplayWindow);
        this.p95P99MetricProofRef = normalizeText(p95P99MetricProofRef);
        this.scanCpuQueueMetricProofRef = normalizeText(scanCpuQueueMetricProofRef);
        this.costBillProofRef = normalizeText(costBillProofRef);
        this.externalVerificationStatus = normalizeVerificationStatus(externalVerificationStatus);
        this.verificationBundle = verificationBundle == null || !verificationBundle.hasAnyEvidence()
            ? null
            : verificationBundle;
    }

    public boolean hasAnyEvidence() {
        return hasText(evidenceSource)
            || hasText(concurrencyProofRef)
            || hasText(dailyQueryVolumeProofRef)
            || hasText(dataLayoutProofRef)
            || hasText(workloadReplayProofRef)
            || hasText(workloadReplayWindow)
            || hasText(p95P99MetricProofRef)
            || hasText(scanCpuQueueMetricProofRef)
            || hasText(costBillProofRef)
            || STATUS_VERIFIED.equals(externalVerificationStatus)
            || verificationBundle != null;
    }

    public boolean isExternallyVerified() {
        return hasVerifiedStatus() && hasVerifiedProductionEvidence(null);
    }

    public boolean isExternallyVerified(Integer targetConcurrency) {
        return hasVerifiedStatus() && hasVerifiedProductionEvidence(targetConcurrency);
    }

    public boolean hasVerifiedStatus() {
        return STATUS_VERIFIED.equals(externalVerificationStatus);
    }

    public boolean hasVerifiedProductionEvidence(Integer targetConcurrency) {
        return verificationBundle != null && verificationBundle.satisfiesProductionEvidence(targetConcurrency);
    }

    public List<String> satisfiedVerificationEvidence(Integer targetConcurrency) {
        if (verificationBundle == null) {
            return Collections.emptyList();
        }
        return verificationBundle.satisfiedProductionEvidence(targetConcurrency);
    }

    public List<String> missingVerificationEvidence(Integer targetConcurrency) {
        if (verificationBundle == null) {
            return Collections.singletonList("productionEvidenceBundle");
        }
        return verificationBundle.missingProductionEvidence(targetConcurrency);
    }

    public String getEvidenceSource() {
        return evidenceSource;
    }

    public String getConcurrencyProofRef() {
        return concurrencyProofRef;
    }

    public String getDailyQueryVolumeProofRef() {
        return dailyQueryVolumeProofRef;
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

    public BenchmarkScaleEvidenceBundle getVerificationBundle() {
        return verificationBundle;
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
