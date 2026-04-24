package com.company.sqlforge.common.governance;

public class GovernanceTenantArtifactPolicyRequest {

    private String tenantId;
    private String policyScope;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPolicyScope() {
        return policyScope;
    }

    public void setPolicyScope(String policyScope) {
        this.policyScope = policyScope;
    }
}
