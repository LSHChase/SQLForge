package com.company.sqloptimization.application.controller.vo;

import java.util.List;

public class DispatchCollaborationContractVO {

    private String coordinationMode;
    private Boolean sqlExecutionAllowed;
    private Boolean dataLoadingAllowed;
    private Boolean activeExternalPushAllowed;
    private Boolean externalPullRequired;
    private List<String> allowedEventStatuses;
    private List<String> allowedDispatchTypes;
    private String auditBoundary;
    private String residualOwner;

    public String getCoordinationMode() { return coordinationMode; }
    public void setCoordinationMode(String coordinationMode) { this.coordinationMode = coordinationMode; }
    public Boolean getSqlExecutionAllowed() { return sqlExecutionAllowed; }
    public void setSqlExecutionAllowed(Boolean sqlExecutionAllowed) { this.sqlExecutionAllowed = sqlExecutionAllowed; }
    public Boolean getDataLoadingAllowed() { return dataLoadingAllowed; }
    public void setDataLoadingAllowed(Boolean dataLoadingAllowed) { this.dataLoadingAllowed = dataLoadingAllowed; }
    public Boolean getActiveExternalPushAllowed() { return activeExternalPushAllowed; }
    public void setActiveExternalPushAllowed(Boolean activeExternalPushAllowed) { this.activeExternalPushAllowed = activeExternalPushAllowed; }
    public Boolean getExternalPullRequired() { return externalPullRequired; }
    public void setExternalPullRequired(Boolean externalPullRequired) { this.externalPullRequired = externalPullRequired; }
    public List<String> getAllowedEventStatuses() { return allowedEventStatuses; }
    public void setAllowedEventStatuses(List<String> allowedEventStatuses) { this.allowedEventStatuses = allowedEventStatuses; }
    public List<String> getAllowedDispatchTypes() { return allowedDispatchTypes; }
    public void setAllowedDispatchTypes(List<String> allowedDispatchTypes) { this.allowedDispatchTypes = allowedDispatchTypes; }
    public String getAuditBoundary() { return auditBoundary; }
    public void setAuditBoundary(String auditBoundary) { this.auditBoundary = auditBoundary; }
    public String getResidualOwner() { return residualOwner; }
    public void setResidualOwner(String residualOwner) { this.residualOwner = residualOwner; }
}
