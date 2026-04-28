package com.company.sqlforge.common.governance;

import java.util.List;

public class GovernanceBenchmarkRegressionAlertResponse {

    private String reportId;
    private String taskId;
    private Boolean alertTriggered;
    private List<GovernanceBenchmarkRegressionAlertLinkage> alertLinkages;
    private String contractStage;
    private String implementationStage;

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Boolean getAlertTriggered() {
        return alertTriggered;
    }

    public void setAlertTriggered(Boolean alertTriggered) {
        this.alertTriggered = alertTriggered;
    }

    public List<GovernanceBenchmarkRegressionAlertLinkage> getAlertLinkages() {
        return alertLinkages;
    }

    public void setAlertLinkages(List<GovernanceBenchmarkRegressionAlertLinkage> alertLinkages) {
        this.alertLinkages = alertLinkages;
    }

    public String getContractStage() {
        return contractStage;
    }

    public void setContractStage(String contractStage) {
        this.contractStage = contractStage;
    }

    public String getImplementationStage() {
        return implementationStage;
    }

    public void setImplementationStage(String implementationStage) {
        this.implementationStage = implementationStage;
    }
}
