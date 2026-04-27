package com.company.sqlforge.common.jdbcagent;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.openaccess.SqlForgeQueryContext;
import java.util.Map;

public class JdbcAgentSqlRequest {

    private String tenantId;
    private DataSourceTypeEnum datasourceType;
    private String datasourceCode;
    private String sqlText;
    private String templateSql;
    private String boundSqlText;
    private Map<String, Object> parameterSnapshot;
    private SqlForgeQueryContext queryContext;

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

    public String getDatasourceCode() {
        return datasourceCode;
    }

    public void setDatasourceCode(String datasourceCode) {
        this.datasourceCode = datasourceCode;
    }

    public String getSqlText() {
        return sqlText;
    }

    public void setSqlText(String sqlText) {
        this.sqlText = sqlText;
    }

    public String getTemplateSql() {
        return templateSql;
    }

    public void setTemplateSql(String templateSql) {
        this.templateSql = templateSql;
    }

    public String getBoundSqlText() {
        return boundSqlText;
    }

    public void setBoundSqlText(String boundSqlText) {
        this.boundSqlText = boundSqlText;
    }

    public Map<String, Object> getParameterSnapshot() {
        return parameterSnapshot;
    }

    public void setParameterSnapshot(Map<String, Object> parameterSnapshot) {
        this.parameterSnapshot = parameterSnapshot;
    }

    public SqlForgeQueryContext getQueryContext() {
        return queryContext;
    }

    public void setQueryContext(SqlForgeQueryContext queryContext) {
        this.queryContext = queryContext;
    }
}
