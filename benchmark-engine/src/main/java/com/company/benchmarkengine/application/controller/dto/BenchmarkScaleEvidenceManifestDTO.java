package com.company.benchmarkengine.application.controller.dto;

import javax.validation.constraints.Size;
import javax.validation.Valid;
import java.util.Map;

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

    @Size(max = 128, message = "environmentId 超过 128 个字符")
    private String environmentId;

    @Size(max = 64, message = "environmentType 超过 64 个字符")
    private String environmentType;

    @Size(max = 128, message = "evidenceOwner 超过 128 个字符")
    private String evidenceOwner;

    @Size(max = 256, message = "artifactArchiveRef 超过 256 个字符")
    private String artifactArchiveRef;

    @Size(max = 128, message = "verifierOperator 超过 128 个字符")
    private String verifierOperator;

    @Valid
    private Map<String, BenchmarkScaleEvidenceFileDigestDTO> evidenceFileDigests;

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

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    public String getEnvironmentType() {
        return environmentType;
    }

    public void setEnvironmentType(String environmentType) {
        this.environmentType = environmentType;
    }

    public String getEvidenceOwner() {
        return evidenceOwner;
    }

    public void setEvidenceOwner(String evidenceOwner) {
        this.evidenceOwner = evidenceOwner;
    }

    public String getArtifactArchiveRef() {
        return artifactArchiveRef;
    }

    public void setArtifactArchiveRef(String artifactArchiveRef) {
        this.artifactArchiveRef = artifactArchiveRef;
    }

    public String getVerifierOperator() {
        return verifierOperator;
    }

    public void setVerifierOperator(String verifierOperator) {
        this.verifierOperator = verifierOperator;
    }

    public Map<String, BenchmarkScaleEvidenceFileDigestDTO> getEvidenceFileDigests() {
        return evidenceFileDigests;
    }

    public void setEvidenceFileDigests(Map<String, BenchmarkScaleEvidenceFileDigestDTO> evidenceFileDigests) {
        this.evidenceFileDigests = evidenceFileDigests;
    }

    public BenchmarkScaleEvidenceBundleDTO getVerificationBundle() {
        return verificationBundle;
    }

    public void setVerificationBundle(BenchmarkScaleEvidenceBundleDTO verificationBundle) {
        this.verificationBundle = verificationBundle;
    }
}
