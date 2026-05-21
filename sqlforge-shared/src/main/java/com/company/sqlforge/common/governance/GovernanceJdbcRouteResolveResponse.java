package com.company.sqlforge.common.governance;

import java.util.ArrayList;
import java.util.List;

public class GovernanceJdbcRouteResolveResponse {

    private String tenantId;
    private String datasourceCode;
    private String requestedDatasourceType;
    private List<GovernanceJdbcRouteCandidate> candidates = new ArrayList<GovernanceJdbcRouteCandidate>();
    private String contractStage;
    private String implementationStage;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDatasourceCode() {
        return datasourceCode;
    }

    public void setDatasourceCode(String datasourceCode) {
        this.datasourceCode = datasourceCode;
    }

    public String getRequestedDatasourceType() {
        return requestedDatasourceType;
    }

    public void setRequestedDatasourceType(String requestedDatasourceType) {
        this.requestedDatasourceType = requestedDatasourceType;
    }

    public List<GovernanceJdbcRouteCandidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<GovernanceJdbcRouteCandidate> candidates) {
        this.candidates = candidates == null ? new ArrayList<GovernanceJdbcRouteCandidate>() : candidates;
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
