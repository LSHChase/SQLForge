package com.company.benchmarkengine.application.controller.dto;

import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdMetric;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdOperator;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdSeverity;
import java.math.BigDecimal;

public class BenchmarkThresholdDTO {

    private BenchmarkThresholdMetric metric;
    private BenchmarkThresholdOperator operator;
    private BigDecimal targetValue;
    private BenchmarkThresholdSeverity severity;
    private String description;

    public BenchmarkThresholdMetric getMetric() {
        return metric;
    }

    public void setMetric(BenchmarkThresholdMetric metric) {
        this.metric = metric;
    }

    public BenchmarkThresholdOperator getOperator() {
        return operator;
    }

    public void setOperator(BenchmarkThresholdOperator operator) {
        this.operator = operator;
    }

    public BigDecimal getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(BigDecimal targetValue) {
        this.targetValue = targetValue;
    }

    public BenchmarkThresholdSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(BenchmarkThresholdSeverity severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
