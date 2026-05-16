package com.company.queryexecution.application.controller.dto;

import com.company.queryexecution.domain.query.AccelerationPreference;
import com.company.queryexecution.domain.query.FaultToleranceStrategy;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class QueryExecuteRequest {

    @NotBlank(message = "sqlText 为必填项")
    @Size(max = 10485760, message = "sqlText 超过 10MB 限制")
    private String sqlText;

    @NotBlank(message = "tenantId 为必填项")
    private String tenantId;

    @NotNull(message = "datasourceType 为必填项")
    private DataSourceTypeEnum datasourceType;

    @Valid
    private QueryContextDTO queryContext;

    private AccelerationPreference accelerationPreference = AccelerationPreference.NONE;
    private FaultToleranceStrategy faultToleranceStrategy = FaultToleranceStrategy.FAIL_FAST;

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

    public QueryContextDTO getQueryContext() {
        return queryContext;
    }

    public void setQueryContext(QueryContextDTO queryContext) {
        this.queryContext = queryContext;
    }

    public AccelerationPreference getAccelerationPreference() {
        return accelerationPreference;
    }

    public void setAccelerationPreference(AccelerationPreference accelerationPreference) {
        this.accelerationPreference = accelerationPreference;
    }

    public FaultToleranceStrategy getFaultToleranceStrategy() {
        return faultToleranceStrategy;
    }

    public void setFaultToleranceStrategy(FaultToleranceStrategy faultToleranceStrategy) {
        this.faultToleranceStrategy = faultToleranceStrategy;
    }
}
