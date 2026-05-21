package com.company.sqlforge.common.governance;

public class GovernanceJdbcRouteCandidate {

    private String engineType;
    private String datasourceCode;
    private String connectionMode;
    private String jdbcUrl;
    private String driverClassName;
    private String driverArtifactId;
    private String driverSha256;
    private Integer timeoutMs;
    private String healthStatus;
    private boolean enabled;
    private boolean readonly;

    public String getEngineType() {
        return engineType;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }

    public String getDatasourceCode() {
        return datasourceCode;
    }

    public void setDatasourceCode(String datasourceCode) {
        this.datasourceCode = datasourceCode;
    }

    public String getConnectionMode() {
        return connectionMode;
    }

    public void setConnectionMode(String connectionMode) {
        this.connectionMode = connectionMode;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getDriverArtifactId() {
        return driverArtifactId;
    }

    public void setDriverArtifactId(String driverArtifactId) {
        this.driverArtifactId = driverArtifactId;
    }

    public String getDriverSha256() {
        return driverSha256;
    }

    public void setDriverSha256(String driverSha256) {
        this.driverSha256 = driverSha256;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }
}
