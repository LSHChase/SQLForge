package com.company.sqlforge.common.governance;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;

public class GovernanceJdbcRouteResolveRequest {

    private String tenantId;
    private String datasourceCode;
    private DataSourceTypeEnum datasourceType;

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

    public DataSourceTypeEnum getDatasourceType() {
        return datasourceType;
    }

    public void setDatasourceType(DataSourceTypeEnum datasourceType) {
        this.datasourceType = datasourceType;
    }
}
