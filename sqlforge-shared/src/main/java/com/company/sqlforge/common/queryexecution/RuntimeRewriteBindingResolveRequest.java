package com.company.sqlforge.common.queryexecution;

import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import java.util.List;

public class RuntimeRewriteBindingResolveRequest {

    private String tenantId;
    private String sqlFingerprint;
    private String sqlText;
    private String datasourceCode;
    private List<LogicalObjectSurface> runtimeMatchObjectRefs;
    private List<String> runtimeMatchObjectNames;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getSqlText() { return sqlText; }
    public void setSqlText(String sqlText) { this.sqlText = sqlText; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public List<LogicalObjectSurface> getRuntimeMatchObjectRefs() { return runtimeMatchObjectRefs; }
    public void setRuntimeMatchObjectRefs(List<LogicalObjectSurface> runtimeMatchObjectRefs) { this.runtimeMatchObjectRefs = runtimeMatchObjectRefs; }
    public List<String> getRuntimeMatchObjectNames() { return runtimeMatchObjectNames; }
    public void setRuntimeMatchObjectNames(List<String> runtimeMatchObjectNames) { this.runtimeMatchObjectNames = runtimeMatchObjectNames; }
}
