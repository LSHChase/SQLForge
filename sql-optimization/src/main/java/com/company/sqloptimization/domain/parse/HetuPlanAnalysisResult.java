package com.company.sqloptimization.domain.parse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HetuPlanAnalysisResult {

    private PlanAnalysisStatus status;
    private String planText;
    private String datasourceCode;
    private Long costMs;
    private String failureReason;
    private List<String> evidence;

    public static HetuPlanAnalysisResult success(String datasourceCode,
                                                 String planText,
                                                 long costMs,
                                                 List<String> evidence) {
        HetuPlanAnalysisResult result = new HetuPlanAnalysisResult();
        result.setStatus(PlanAnalysisStatus.SUCCESS);
        result.setDatasourceCode(datasourceCode);
        result.setPlanText(planText);
        result.setCostMs(Long.valueOf(costMs));
        result.setEvidence(evidence);
        return result;
    }

    public static HetuPlanAnalysisResult failed(String datasourceCode,
                                                String failureReason,
                                                long costMs,
                                                List<String> evidence) {
        HetuPlanAnalysisResult result = new HetuPlanAnalysisResult();
        result.setStatus(PlanAnalysisStatus.FAILED);
        result.setDatasourceCode(datasourceCode);
        result.setFailureReason(failureReason);
        result.setCostMs(Long.valueOf(costMs));
        result.setEvidence(evidence);
        return result;
    }

    public static HetuPlanAnalysisResult skipped(String datasourceCode,
                                                 String failureReason,
                                                 List<String> evidence) {
        HetuPlanAnalysisResult result = new HetuPlanAnalysisResult();
        result.setStatus(PlanAnalysisStatus.SKIPPED);
        result.setDatasourceCode(datasourceCode);
        result.setFailureReason(failureReason);
        result.setCostMs(Long.valueOf(0L));
        result.setEvidence(evidence);
        return result;
    }

    public PlanAnalysisStatus getStatus() {
        return status;
    }

    public void setStatus(PlanAnalysisStatus status) {
        this.status = status;
    }

    public String getPlanText() {
        return planText;
    }

    public void setPlanText(String planText) {
        this.planText = planText;
    }

    public String getDatasourceCode() {
        return datasourceCode;
    }

    public void setDatasourceCode(String datasourceCode) {
        this.datasourceCode = datasourceCode;
    }

    public Long getCostMs() {
        return costMs;
    }

    public void setCostMs(Long costMs) {
        this.costMs = costMs;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public List<String> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<String> evidence) {
        if (evidence == null) {
            this.evidence = Collections.emptyList();
            return;
        }
        this.evidence = new ArrayList<String>(evidence);
    }
}
