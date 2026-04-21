package com.company.benchmarkengine.domain.benchmark;

import java.math.BigDecimal;

public class BenchmarkThreshold {

    private final BenchmarkThresholdMetric metric;
    private final BenchmarkThresholdOperator operator;
    private final BigDecimal targetValue;
    private final BenchmarkThresholdSeverity severity;
    private final String description;

    public BenchmarkThreshold(BenchmarkThresholdMetric metric,
                              BenchmarkThresholdOperator operator,
                              BigDecimal targetValue,
                              BenchmarkThresholdSeverity severity,
                              String description) {
        this.metric = metric;
        this.operator = operator;
        this.targetValue = targetValue;
        this.severity = severity;
        this.description = description;
    }

    public BenchmarkThresholdAssessment evaluate(BigDecimal actualValue) {
        boolean meetsTarget = compare(actualValue, targetValue);
        BenchmarkThresholdVerdict verdict = meetsTarget
            ? BenchmarkThresholdVerdict.PASS
            : severity == BenchmarkThresholdSeverity.WARNING
            ? BenchmarkThresholdVerdict.WARNING
            : BenchmarkThresholdVerdict.FAIL;
        StringBuilder summaryBuilder = new StringBuilder();
        summaryBuilder.append(metric.name())
            .append(' ')
            .append(operator.name())
            .append(' ')
            .append(targetValue.toPlainString())
            .append(", actual=")
            .append(actualValue.toPlainString());
        if (!meetsTarget) {
            summaryBuilder.append(", severity=").append(severity.name());
        }
        if (description != null && !description.isEmpty()) {
            summaryBuilder.append(", ").append(description);
        }
        return new BenchmarkThresholdAssessment(metric, verdict, actualValue, targetValue, summaryBuilder.toString());
    }

    private boolean compare(BigDecimal actualValue, BigDecimal thresholdValue) {
        int compared = actualValue.compareTo(thresholdValue);
        if (operator == BenchmarkThresholdOperator.LESS_THAN_OR_EQUAL) {
            return compared <= 0;
        }
        return compared >= 0;
    }

    public BenchmarkThresholdMetric getMetric() {
        return metric;
    }

    public BenchmarkThresholdOperator getOperator() {
        return operator;
    }

    public BigDecimal getTargetValue() {
        return targetValue;
    }

    public BenchmarkThresholdSeverity getSeverity() {
        return severity;
    }

    public String getDescription() {
        return description;
    }
}
