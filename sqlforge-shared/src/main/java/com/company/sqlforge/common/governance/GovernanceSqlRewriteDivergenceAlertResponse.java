package com.company.sqlforge.common.governance;

import java.util.List;

public class GovernanceSqlRewriteDivergenceAlertResponse {

    private String rewriteRecordId;
    private String validationRunId;
    private Boolean alertTriggered;
    private List<GovernanceSqlRewriteDivergenceAlertLinkage> alertLinkages;
    private String contractStage;
    private String implementationStage;

    public String getRewriteRecordId() {
        return rewriteRecordId;
    }

    public void setRewriteRecordId(String rewriteRecordId) {
        this.rewriteRecordId = rewriteRecordId;
    }

    public String getValidationRunId() {
        return validationRunId;
    }

    public void setValidationRunId(String validationRunId) {
        this.validationRunId = validationRunId;
    }

    public Boolean getAlertTriggered() {
        return alertTriggered;
    }

    public void setAlertTriggered(Boolean alertTriggered) {
        this.alertTriggered = alertTriggered;
    }

    public List<GovernanceSqlRewriteDivergenceAlertLinkage> getAlertLinkages() {
        return alertLinkages;
    }

    public void setAlertLinkages(List<GovernanceSqlRewriteDivergenceAlertLinkage> alertLinkages) {
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
