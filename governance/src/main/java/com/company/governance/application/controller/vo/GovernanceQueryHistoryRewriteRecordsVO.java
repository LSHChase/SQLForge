package com.company.governance.application.controller.vo;

import java.util.List;

public class GovernanceQueryHistoryRewriteRecordsVO {

    private String tenantId;
    private String historyId;
    private Integer rewriteRecordCount;
    private List<GovernanceQueryHistoryRewriteRecordVO> items;
    private String contractStage;
    private String implementationStage;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getHistoryId() {
        return historyId;
    }

    public void setHistoryId(String historyId) {
        this.historyId = historyId;
    }

    public Integer getRewriteRecordCount() {
        return rewriteRecordCount;
    }

    public void setRewriteRecordCount(Integer rewriteRecordCount) {
        this.rewriteRecordCount = rewriteRecordCount;
    }

    public List<GovernanceQueryHistoryRewriteRecordVO> getItems() {
        return items;
    }

    public void setItems(List<GovernanceQueryHistoryRewriteRecordVO> items) {
        this.items = items;
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
