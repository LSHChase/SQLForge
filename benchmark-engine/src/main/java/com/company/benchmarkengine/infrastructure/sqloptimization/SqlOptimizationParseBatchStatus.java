package com.company.benchmarkengine.infrastructure.sqloptimization;

import java.util.List;

public class SqlOptimizationParseBatchStatus {

    private String batchId;
    private String tenantId;
    private List<SqlOptimizationParseBatchItem> importedRecords;

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public List<SqlOptimizationParseBatchItem> getImportedRecords() {
        return importedRecords;
    }

    public void setImportedRecords(List<SqlOptimizationParseBatchItem> importedRecords) {
        this.importedRecords = importedRecords;
    }
}
