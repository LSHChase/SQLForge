package com.company.sqloptimization.application.controller.dto;

import java.util.List;

public class RewriteTrialBatchRequest {

    private String tenantId;
    private List<String> issueSceneFilter;
    private Boolean forceRecalculate;
    private Integer maxItems;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public List<String> getIssueSceneFilter() { return issueSceneFilter; }
    public void setIssueSceneFilter(List<String> issueSceneFilter) { this.issueSceneFilter = issueSceneFilter; }
    public Boolean getForceRecalculate() { return forceRecalculate; }
    public void setForceRecalculate(Boolean forceRecalculate) { this.forceRecalculate = forceRecalculate; }
    public Integer getMaxItems() { return maxItems; }
    public void setMaxItems(Integer maxItems) { this.maxItems = maxItems; }
}
