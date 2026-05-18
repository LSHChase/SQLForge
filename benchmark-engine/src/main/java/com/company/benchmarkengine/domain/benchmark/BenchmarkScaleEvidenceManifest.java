package com.company.benchmarkengine.domain.benchmark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BenchmarkScaleEvidenceManifest {

    public static final String STATUS_VERIFIED = "VERIFIED";
    public static final String STATUS_UNVERIFIED = "UNVERIFIED";
    public static final List<String> REQUIRED_EVIDENCE_FILES = Collections.unmodifiableList(Arrays.asList(
        "concurrency.json",
        "daily-query-volume.json",
        "data-layout.json",
        "workload-replay.json",
        "metrics.csv",
        "cost-bill.json"
    ));

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
    private final Map<String, BenchmarkScaleEvidenceFileDigest> evidenceFileDigests;
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
            null,
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
            null,
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
        this(
            evidenceSource,
            concurrencyProofRef,
            dailyQueryVolumeProofRef,
            dataLayoutProofRef,
            workloadReplayProofRef,
            workloadReplayWindow,
            p95P99MetricProofRef,
            scanCpuQueueMetricProofRef,
            costBillProofRef,
            externalVerificationStatus,
            null,
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
                                          Map<String, BenchmarkScaleEvidenceFileDigest> evidenceFileDigests,
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
        this.evidenceFileDigests = normalizeEvidenceFileDigests(evidenceFileDigests);
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
            || !evidenceFileDigests.isEmpty()
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
        return verificationBundle != null
            && verificationBundle.satisfiesProductionEvidence(targetConcurrency)
            && hasCompleteEvidenceFileDigests();
    }

    public List<String> satisfiedVerificationEvidence(Integer targetConcurrency) {
        List<String> satisfied = new ArrayList<String>();
        if (verificationBundle == null) {
            if (hasCompleteEvidenceFileDigests()) {
                satisfied.add("productionEvidenceManifest.evidenceFileDigests");
            }
            return Collections.unmodifiableList(satisfied);
        }
        satisfied.addAll(verificationBundle.satisfiedProductionEvidence(targetConcurrency));
        if (hasCompleteEvidenceFileDigests()) {
            satisfied.add("productionEvidenceManifest.evidenceFileDigests");
        }
        return Collections.unmodifiableList(satisfied);
    }

    public List<String> missingVerificationEvidence(Integer targetConcurrency) {
        List<String> missing = new ArrayList<String>();
        if (verificationBundle == null) {
            missing.add("productionEvidenceBundle");
        } else {
            missing.addAll(verificationBundle.missingProductionEvidence(targetConcurrency));
        }
        missing.addAll(missingEvidenceFileDigests());
        return Collections.unmodifiableList(missing);
    }

    public boolean hasCompleteEvidenceFileDigests() {
        return missingEvidenceFileDigests().isEmpty();
    }

    public List<String> missingEvidenceFileDigests() {
        List<String> missing = new ArrayList<String>();
        for (String fileName : REQUIRED_EVIDENCE_FILES) {
            BenchmarkScaleEvidenceFileDigest digest = evidenceFileDigests.get(fileName);
            if (digest == null || !digest.isComplete()) {
                missing.add("productionEvidenceManifest.evidenceFileDigests." + fileName);
            }
        }
        return Collections.unmodifiableList(missing);
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

    public Map<String, BenchmarkScaleEvidenceFileDigest> getEvidenceFileDigests() {
        return evidenceFileDigests;
    }

    public BenchmarkScaleEvidenceBundle getVerificationBundle() {
        return verificationBundle;
    }

    private Map<String, BenchmarkScaleEvidenceFileDigest> normalizeEvidenceFileDigests(
        Map<String, BenchmarkScaleEvidenceFileDigest> value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, BenchmarkScaleEvidenceFileDigest> normalized =
            new LinkedHashMap<String, BenchmarkScaleEvidenceFileDigest>();
        for (Map.Entry<String, BenchmarkScaleEvidenceFileDigest> entry : value.entrySet()) {
            if (hasText(entry.getKey()) && entry.getValue() != null && entry.getValue().hasAnyEvidence()) {
                normalized.put(entry.getKey().trim(), entry.getValue());
            }
        }
        return normalized.isEmpty()
            ? Collections.<String, BenchmarkScaleEvidenceFileDigest>emptyMap()
            : Collections.unmodifiableMap(normalized);
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
