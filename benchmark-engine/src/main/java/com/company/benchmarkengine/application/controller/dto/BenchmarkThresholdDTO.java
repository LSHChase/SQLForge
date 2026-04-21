package com.company.benchmarkengine.application.controller.dto;

import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdMetric;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdOperator;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdSeverity;
import java.math.BigDecimal;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class BenchmarkThresholdDTO {

    @NotNull(message = "threshold metric is required")
    private BenchmarkThresholdMetric metric;

    @NotNull(message = "threshold operator is required")
    private BenchmarkThresholdOperator operator;

    @NotNull(message = "threshold targetValue is required")
    private BigDecimal targetValue;

    @NotNull(message = "threshold severity is required")
    private BenchmarkThresholdSeverity severity;

    @Size(max = 255, message = "threshold description exceeds 255 characters")
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
