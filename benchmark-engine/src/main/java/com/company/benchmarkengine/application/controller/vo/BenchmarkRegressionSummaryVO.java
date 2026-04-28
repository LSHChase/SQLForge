package com.company.benchmarkengine.application.controller.vo;

public class BenchmarkRegressionSummaryVO {

    private final Integer thresholdHitCount;
    private final Integer failedThresholdCount;
    private final Integer warningThresholdCount;
    private final Boolean alertRequired;
    private final String summary;

    public BenchmarkRegressionSummaryVO(Integer thresholdHitCount,
                                        Integer failedThresholdCount,
                                        Integer warningThresholdCount,
                                        Boolean alertRequired,
                                        String summary) {
        this.thresholdHitCount = thresholdHitCount;
        this.failedThresholdCount = failedThresholdCount;
        this.warningThresholdCount = warningThresholdCount;
        this.alertRequired = alertRequired;
        this.summary = summary;
    }

    public Integer getThresholdHitCount() {
        return thresholdHitCount;
    }

    public Integer getFailedThresholdCount() {
        return failedThresholdCount;
    }

    public Integer getWarningThresholdCount() {
        return warningThresholdCount;
    }

    public Boolean getAlertRequired() {
        return alertRequired;
    }

    public String getSummary() {
        return summary;
    }
}
