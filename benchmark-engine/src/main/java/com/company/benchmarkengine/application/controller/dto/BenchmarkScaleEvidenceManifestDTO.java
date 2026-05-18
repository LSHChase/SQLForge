package com.company.benchmarkengine.application.controller.dto;

import javax.validation.constraints.Size;
import javax.validation.Valid;

public class BenchmarkScaleEvidenceManifestDTO {

    @Size(max = 64, message = "evidenceSource 超过 64 个字符")
    private String evidenceSource;

    @Size(max = 256, message = "concurrencyProofRef 超过 256 个字符")
    private String concurrencyProofRef;

    @Size(max = 256, message = "dailyQueryVolumeProofRef 超过 256 个字符")
    private String dailyQueryVolumeProofRef;

    @Size(max = 256, message = "dataLayoutProofRef 超过 256 个字符")
    private String dataLayoutProofRef;

    @Size(max = 256, message = "workloadReplayProofRef 超过 256 个字符")
    private String workloadReplayProofRef;

    @Size(max = 64, message = "workloadReplayWindow 超过 64 个字符")
    private String workloadReplayWindow;

    @Size(max = 256, message = "p95P99MetricProofRef 超过 256 个字符")
    private String p95P99MetricProofRef;

    @Size(max = 256, message = "scanCpuQueueMetricProofRef 超过 256 个字符")
    private String scanCpuQueueMetricProofRef;

    @Size(max = 256, message = "costBillProofRef 超过 256 个字符")
    private String costBillProofRef;

    @Size(max = 64, message = "externalVerificationStatus 超过 64 个字符")
    private String externalVerificationStatus;

    @Valid
    private BenchmarkScaleEvidenceBundleDTO verificationBundle;

    public String getEvidenceSource() {
        return evidenceSource;
    }

    public void setEvidenceSource(String evidenceSource) {
        this.evidenceSource = evidenceSource;
    }

    public String getConcurrencyProofRef() {
        return concurrencyProofRef;
    }

    public void setConcurrencyProofRef(String concurrencyProofRef) {
        this.concurrencyProofRef = concurrencyProofRef;
    }

    public String getDailyQueryVolumeProofRef() {
        return dailyQueryVolumeProofRef;
    }

    public void setDailyQueryVolumeProofRef(String dailyQueryVolumeProofRef) {
        this.dailyQueryVolumeProofRef = dailyQueryVolumeProofRef;
    }

    public String getDataLayoutProofRef() {
        return dataLayoutProofRef;
    }

    public void setDataLayoutProofRef(String dataLayoutProofRef) {
        this.dataLayoutProofRef = dataLayoutProofRef;
    }

    public String getWorkloadReplayProofRef() {
        return workloadReplayProofRef;
    }

    public void setWorkloadReplayProofRef(String workloadReplayProofRef) {
        this.workloadReplayProofRef = workloadReplayProofRef;
    }

    public String getWorkloadReplayWindow() {
        return workloadReplayWindow;
    }

    public void setWorkloadReplayWindow(String workloadReplayWindow) {
        this.workloadReplayWindow = workloadReplayWindow;
    }

    public String getP95P99MetricProofRef() {
        return p95P99MetricProofRef;
    }

    public void setP95P99MetricProofRef(String p95p99MetricProofRef) {
        this.p95P99MetricProofRef = p95p99MetricProofRef;
    }

    public String getScanCpuQueueMetricProofRef() {
        return scanCpuQueueMetricProofRef;
    }

    public void setScanCpuQueueMetricProofRef(String scanCpuQueueMetricProofRef) {
        this.scanCpuQueueMetricProofRef = scanCpuQueueMetricProofRef;
    }

    public String getCostBillProofRef() {
        return costBillProofRef;
    }

    public void setCostBillProofRef(String costBillProofRef) {
        this.costBillProofRef = costBillProofRef;
    }

    public String getExternalVerificationStatus() {
        return externalVerificationStatus;
    }

    public void setExternalVerificationStatus(String externalVerificationStatus) {
        this.externalVerificationStatus = externalVerificationStatus;
    }

    public BenchmarkScaleEvidenceBundleDTO getVerificationBundle() {
        return verificationBundle;
    }

    public void setVerificationBundle(BenchmarkScaleEvidenceBundleDTO verificationBundle) {
        this.verificationBundle = verificationBundle;
    }
}
