package com.company.sqloptimization.application.controller.vo;

public class ParseBatchStageStatisticsVO {

    private Integer successRecords;
    private Integer partialSuccessRecords;
    private Integer failedRecords;
    private Double successRate;

    public Integer getSuccessRecords() { return successRecords; }
    public void setSuccessRecords(Integer successRecords) { this.successRecords = successRecords; }
    public Integer getPartialSuccessRecords() { return partialSuccessRecords; }
    public void setPartialSuccessRecords(Integer partialSuccessRecords) { this.partialSuccessRecords = partialSuccessRecords; }
    public Integer getFailedRecords() { return failedRecords; }
    public void setFailedRecords(Integer failedRecords) { this.failedRecords = failedRecords; }
    public Double getSuccessRate() { return successRate; }
    public void setSuccessRate(Double successRate) { this.successRate = successRate; }
}
