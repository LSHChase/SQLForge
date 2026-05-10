package com.company.sqlforge.common.queryexecution;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.Map;

public class QueryExecutionResultDigestRequest {

    private String tenantId;
    private String validationRunId;
    private String rewriteRecordId;
    private String sqlFingerprint;
    private String sqlText;
    private DataSourceTypeEnum datasourceType;
    private String datasourceCode;
    private Map<String, Object> comparisonPolicy;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getValidationRunId() { return validationRunId; }
    public void setValidationRunId(String validationRunId) { this.validationRunId = validationRunId; }
    public String getRewriteRecordId() { return rewriteRecordId; }
    public void setRewriteRecordId(String rewriteRecordId) { this.rewriteRecordId = rewriteRecordId; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public void setSqlFingerprint(String sqlFingerprint) { this.sqlFingerprint = sqlFingerprint; }
    public String getSqlText() { return sqlText; }
    public void setSqlText(String sqlText) { this.sqlText = sqlText; }
    public DataSourceTypeEnum getDatasourceType() { return datasourceType; }
    public void setDatasourceType(DataSourceTypeEnum datasourceType) { this.datasourceType = datasourceType; }
    public String getDatasourceCode() { return datasourceCode; }
    public void setDatasourceCode(String datasourceCode) { this.datasourceCode = datasourceCode; }
    public Map<String, Object> getComparisonPolicy() { return comparisonPolicy; }
    public void setComparisonPolicy(Map<String, Object> comparisonPolicy) { this.comparisonPolicy = comparisonPolicy; }
}
