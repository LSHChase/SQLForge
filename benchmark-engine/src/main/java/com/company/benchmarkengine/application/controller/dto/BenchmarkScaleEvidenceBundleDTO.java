package com.company.benchmarkengine.application.controller.dto;

import java.math.BigDecimal;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

public class BenchmarkScaleEvidenceBundleDTO {

    @Min(value = 1L, message = "observedConcurrency 必须大于 0")
    private Integer observedConcurrency;

    @Min(value = 1L, message = "observedDailyQueryVolume 必须大于 0")
    private Long observedDailyQueryVolume;

    @Min(value = 1L, message = "observedDatasetSizeBytes 必须大于 0")
    private Long observedDatasetSizeBytes;

    @DecimalMin(value = "0.000001", message = "workloadReplayDurationHours 必须大于 0")
    private BigDecimal workloadReplayDurationHours;

    @DecimalMin(value = "0.000001", message = "p95LatencyMs 必须大于 0")
    private BigDecimal p95LatencyMs;

    @DecimalMin(value = "0.000001", message = "p99LatencyMs 必须大于 0")
    private BigDecimal p99LatencyMs;

    @Min(value = 1L, message = "scannedBytes 必须大于 0")
    private Long scannedBytes;

    @DecimalMin(value = "0.000001", message = "cpuUsagePercent 必须大于 0")
    private BigDecimal cpuUsagePercent;

    @DecimalMin(value = "0.0", message = "queueWaitMs 必须大于等于 0")
    private BigDecimal queueWaitMs;

    @DecimalMin(value = "0.000001", message = "costBillAmount 必须大于 0")
    private BigDecimal costBillAmount;

    @Size(max = 16, message = "costBillCurrency 超过 16 个字符")
    private String costBillCurrency;

    @Size(max = 128, message = "verifierRef 超过 128 个字符")
    private String verifierRef;

    public Integer getObservedConcurrency() {
        return observedConcurrency;
    }

    public void setObservedConcurrency(Integer observedConcurrency) {
        this.observedConcurrency = observedConcurrency;
    }

    public Long getObservedDailyQueryVolume() {
        return observedDailyQueryVolume;
    }

    public void setObservedDailyQueryVolume(Long observedDailyQueryVolume) {
        this.observedDailyQueryVolume = observedDailyQueryVolume;
    }

    public Long getObservedDatasetSizeBytes() {
        return observedDatasetSizeBytes;
    }

    public void setObservedDatasetSizeBytes(Long observedDatasetSizeBytes) {
        this.observedDatasetSizeBytes = observedDatasetSizeBytes;
    }

    public BigDecimal getWorkloadReplayDurationHours() {
        return workloadReplayDurationHours;
    }

    public void setWorkloadReplayDurationHours(BigDecimal workloadReplayDurationHours) {
        this.workloadReplayDurationHours = workloadReplayDurationHours;
    }

    public BigDecimal getP95LatencyMs() {
        return p95LatencyMs;
    }

    public void setP95LatencyMs(BigDecimal p95LatencyMs) {
        this.p95LatencyMs = p95LatencyMs;
    }

    public BigDecimal getP99LatencyMs() {
        return p99LatencyMs;
    }

    public void setP99LatencyMs(BigDecimal p99LatencyMs) {
        this.p99LatencyMs = p99LatencyMs;
    }

    public Long getScannedBytes() {
        return scannedBytes;
    }

    public void setScannedBytes(Long scannedBytes) {
        this.scannedBytes = scannedBytes;
    }

    public BigDecimal getCpuUsagePercent() {
        return cpuUsagePercent;
    }

    public void setCpuUsagePercent(BigDecimal cpuUsagePercent) {
        this.cpuUsagePercent = cpuUsagePercent;
    }

    public BigDecimal getQueueWaitMs() {
        return queueWaitMs;
    }

    public void setQueueWaitMs(BigDecimal queueWaitMs) {
        this.queueWaitMs = queueWaitMs;
    }

    public BigDecimal getCostBillAmount() {
        return costBillAmount;
    }

    public void setCostBillAmount(BigDecimal costBillAmount) {
        this.costBillAmount = costBillAmount;
    }

    public String getCostBillCurrency() {
        return costBillCurrency;
    }

    public void setCostBillCurrency(String costBillCurrency) {
        this.costBillCurrency = costBillCurrency;
    }

    public String getVerifierRef() {
        return verifierRef;
    }

    public void setVerifierRef(String verifierRef) {
        this.verifierRef = verifierRef;
    }
}
