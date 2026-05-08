package com.company.sqloptimization.application.controller.vo;

import java.util.List;

public class PlanAnalysisVO {

    private String status;
    private String planText;
    private String datasourceCode;
    private Long costMs;
    private String failureReason;
    private List<String> evidence;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPlanText() { return planText; }
    public void setPlanText(String planText) { this.planText = planText; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public Long getCostMs() { return costMs; }
    public void setCostMs(Long costMs) { this.costMs = costMs; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }
}
