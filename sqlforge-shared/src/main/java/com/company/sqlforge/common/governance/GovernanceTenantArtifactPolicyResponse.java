package com.company.sqlforge.common.governance;

public class GovernanceTenantArtifactPolicyResponse {

    private String tenantId;
    private Integer retentionDays;
    private String retentionPolicySource;
    private String retentionPolicyStatus;
    private String policyScope;
    private String contractStage;
    private String implementationStage;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(Integer retentionDays) {
        this.retentionDays = retentionDays;
    }

    public String getRetentionPolicySource() {
        return retentionPolicySource;
    }

    public void setRetentionPolicySource(String retentionPolicySource) {
        this.retentionPolicySource = retentionPolicySource;
    }

    public String getRetentionPolicyStatus() {
        return retentionPolicyStatus;
    }

    public void setRetentionPolicyStatus(String retentionPolicyStatus) {
        this.retentionPolicyStatus = retentionPolicyStatus;
    }

    public String getPolicyScope() {
        return policyScope;
    }

    public void setPolicyScope(String policyScope) {
        this.policyScope = policyScope;
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
