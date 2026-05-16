package com.company.benchmarkengine.application.controller.dto;

import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdMetric;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdOperator;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdSeverity;
import java.math.BigDecimal;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class BenchmarkThresholdDTO {

    @NotNull(message = "threshold 指标为必填项")
    private BenchmarkThresholdMetric metric;

    @NotNull(message = "threshold 操作符为必填项")
    private BenchmarkThresholdOperator operator;

    @NotNull(message = "threshold 目标值为必填项")
    private BigDecimal targetValue;

    @NotNull(message = "threshold 严重级别为必填项")
    private BenchmarkThresholdSeverity severity;

    @Size(max = 255, message = "threshold 描述超过 255 个字符")
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
