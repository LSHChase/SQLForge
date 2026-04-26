package com.company.governance.application.controller.vo;

import java.util.List;

public class BusinessLogicalViewVO {

    private String viewId;
    private String tenantId;
    private String viewCode;
    private String viewName;
    private String datasourceCode;
    private String subjectArea;
    private String ownerUser;
    private String freshnessStatus;
    private String slaStatus;
    private Boolean queryable;
    private String latestRefreshTime;
    private String description;
    private List<LogicalObjectMappingVO> physicalTargets;

    public String getViewId() {
        return viewId;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getViewCode() {
        return viewCode;
    }

    public void setViewCode(String viewCode) {
        this.viewCode = viewCode;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public String getDatasourceCode() {
        return datasourceCode;
    }

    public void setDatasourceCode(String datasourceCode) {
        this.datasourceCode = datasourceCode;
    }

    public String getSubjectArea() {
        return subjectArea;
    }

    public void setSubjectArea(String subjectArea) {
        this.subjectArea = subjectArea;
    }

    public String getOwnerUser() {
        return ownerUser;
    }

    public void setOwnerUser(String ownerUser) {
        this.ownerUser = ownerUser;
    }

    public String getFreshnessStatus() {
        return freshnessStatus;
    }

    public void setFreshnessStatus(String freshnessStatus) {
        this.freshnessStatus = freshnessStatus;
    }

    public String getSlaStatus() {
        return slaStatus;
    }

    public void setSlaStatus(String slaStatus) {
        this.slaStatus = slaStatus;
    }

    public Boolean getQueryable() {
        return queryable;
    }

    public void setQueryable(Boolean queryable) {
        this.queryable = queryable;
    }

    public String getLatestRefreshTime() {
        return latestRefreshTime;
    }

    public void setLatestRefreshTime(String latestRefreshTime) {
        this.latestRefreshTime = latestRefreshTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<LogicalObjectMappingVO> getPhysicalTargets() {
        return physicalTargets;
    }

    public void setPhysicalTargets(List<LogicalObjectMappingVO> physicalTargets) {
        this.physicalTargets = physicalTargets;
    }
}
