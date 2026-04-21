package com.company.benchmarkengine.application.controller.vo;

import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdVerdict;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.math.BigDecimal;

public class BenchmarkEngineMetricVO {

    private final DataSourceTypeEnum engine;
    private final BigDecimal targetQps;
    private final BigDecimal actualQps;
    private final BigDecimal p50LatencyMs;
    private final BigDecimal p95LatencyMs;
    private final BigDecimal p99LatencyMs;
    private final BigDecimal cpuUsagePercent;
    private final BigDecimal memoryUsageMb;
    private final BigDecimal scannedDataBytes;
    private final BenchmarkThresholdVerdict verdict;
    private final String notes;

    public BenchmarkEngineMetricVO(DataSourceTypeEnum engine,
                                   BigDecimal targetQps,
                                   BigDecimal actualQps,
                                   BigDecimal p50LatencyMs,
                                   BigDecimal p95LatencyMs,
                                   BigDecimal p99LatencyMs,
                                   BigDecimal cpuUsagePercent,
                                   BigDecimal memoryUsageMb,
                                   BigDecimal scannedDataBytes,
                                   BenchmarkThresholdVerdict verdict,
                                   String notes) {
        this.engine = engine;
        this.targetQps = targetQps;
        this.actualQps = actualQps;
        this.p50LatencyMs = p50LatencyMs;
        this.p95LatencyMs = p95LatencyMs;
        this.p99LatencyMs = p99LatencyMs;
        this.cpuUsagePercent = cpuUsagePercent;
        this.memoryUsageMb = memoryUsageMb;
        this.scannedDataBytes = scannedDataBytes;
        this.verdict = verdict;
        this.notes = notes;
    }

    public DataSourceTypeEnum getEngine() {
        return engine;
    }

    public BigDecimal getTargetQps() {
        return targetQps;
    }

    public BigDecimal getActualQps() {
        return actualQps;
    }

    public BigDecimal getP50LatencyMs() {
        return p50LatencyMs;
    }

    public BigDecimal getP95LatencyMs() {
        return p95LatencyMs;
    }

    public BigDecimal getP99LatencyMs() {
        return p99LatencyMs;
    }

    public BigDecimal getCpuUsagePercent() {
        return cpuUsagePercent;
    }

    public BigDecimal getMemoryUsageMb() {
        return memoryUsageMb;
    }

    public BigDecimal getScannedDataBytes() {
        return scannedDataBytes;
    }

    public BenchmarkThresholdVerdict getVerdict() {
        return verdict;
    }

    public String getNotes() {
        return notes;
    }
}
