package com.company.sqlforge.common.openaccess;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;

public class SqlForgeQueryRequest {

    private String sqlText;
    private String tenantId;
    private DataSourceTypeEnum datasourceType;
    private SqlForgeQueryContext queryContext;
    private SqlForgeAccelerationPreference accelerationPreference = SqlForgeAccelerationPreference.NONE;
    private SqlForgeFaultToleranceStrategy faultToleranceStrategy = SqlForgeFaultToleranceStrategy.FAIL_FAST;

    public String getSqlText() {
        return sqlText;
    }

    public void setSqlText(String sqlText) {
        this.sqlText = sqlText;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public DataSourceTypeEnum getDatasourceType() {
        return datasourceType;
    }

    public void setDatasourceType(DataSourceTypeEnum datasourceType) {
        this.datasourceType = datasourceType;
    }

    public SqlForgeQueryContext getQueryContext() {
        return queryContext;
    }

    public void setQueryContext(SqlForgeQueryContext queryContext) {
        this.queryContext = queryContext;
    }

    public SqlForgeAccelerationPreference getAccelerationPreference() {
        return accelerationPreference;
    }

    public void setAccelerationPreference(SqlForgeAccelerationPreference accelerationPreference) {
        this.accelerationPreference = accelerationPreference;
    }

    public SqlForgeFaultToleranceStrategy getFaultToleranceStrategy() {
        return faultToleranceStrategy;
    }

    public void setFaultToleranceStrategy(SqlForgeFaultToleranceStrategy faultToleranceStrategy) {
        this.faultToleranceStrategy = faultToleranceStrategy;
    }
}
