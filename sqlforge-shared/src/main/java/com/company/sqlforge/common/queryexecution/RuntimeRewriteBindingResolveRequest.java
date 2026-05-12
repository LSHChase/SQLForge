package com.company.sqlforge.common.queryexecution;

public class RuntimeRewriteBindingResolveRequest {

    private String tenantId;
    private String sqlFingerprint;
    private String datasourceCode;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
}
