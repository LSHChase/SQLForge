package com.company.benchmarkengine.domain.benchmark;

import java.math.BigDecimal;

public class BenchmarkThresholdAssessment {

    private final BenchmarkThresholdMetric metric;
    private final BenchmarkThresholdVerdict verdict;
    private final BigDecimal actualValue;
    private final BigDecimal targetValue;
    private final String summary;

    public BenchmarkThresholdAssessment(BenchmarkThresholdMetric metric,
                                        BenchmarkThresholdVerdict verdict,
                                        BigDecimal actualValue,
                                        BigDecimal targetValue,
                                        String summary) {
        this.metric = metric;
        this.verdict = verdict;
        this.actualValue = actualValue;
        this.targetValue = targetValue;
        this.summary = summary;
    }

    public BenchmarkThresholdMetric getMetric() {
        return metric;
    }

    public BenchmarkThresholdVerdict getVerdict() {
        return verdict;
    }

    public BigDecimal getActualValue() {
        return actualValue;
    }

    public BigDecimal getTargetValue() {
        return targetValue;
    }

    public String getSummary() {
        return summary;
    }
}
