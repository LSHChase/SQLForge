package com.company.sqloptimization.application.controller.dto;

public class ParseBatchRetryAccessRequest {

    private String failureFilter;
    private String datasourceCode;
    private Boolean forceRecheckAvailability = Boolean.FALSE;

    public String getFailureFilter() { return failureFilter; }
    public void setFailureFilter(String failureFilter) { this.failureFilter = failureFilter; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public Boolean getForceRecheckAvailability() { return forceRecheckAvailability; }
    public void setForceRecheckAvailability(Boolean forceRecheckAvailability) { this.forceRecheckAvailability = forceRecheckAvailability; }
}
