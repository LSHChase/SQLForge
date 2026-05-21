package com.company.sqlforge.common.governance;

public class GovernanceJdbcDatasourceResolveResponse {

    private String tenantId;
    private String datasourceId;
    private String datasourceCode;
    private String engineType;
    private String connectionMode;
    private boolean resolved;
    private boolean enabled;
    private boolean readonly;
    private String jdbcUrl;
    private String driverClassName;
    private String driverSourceType;
    private String driverArtifactId;
    private String driverVersionLabel;
    private String driverSha256;
    private String driverRelativePath;
    private String username;
    private String password;
    private Integer timeoutMs;
    private String credentialMask;
    private String failureReason;
    private String contractStage;
    private String implementationStage;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDatasourceId() {
        return datasourceId;
    }

    public void setDatasourceId(String datasourceId) {
        this.datasourceId = datasourceId;
    }

    public String getDatasourceCode() {
        return datasourceCode;
    }

    public void setDatasourceCode(String datasourceCode) {
        this.datasourceCode = datasourceCode;
    }

    public String getEngineType() {
        return engineType;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }

    public String getConnectionMode() {
        return connectionMode;
    }

    public void setConnectionMode(String connectionMode) {
        this.connectionMode = connectionMode;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
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

    public String getDriverSourceType() {
        return driverSourceType;
    }

    public void setDriverSourceType(String driverSourceType) {
        this.driverSourceType = driverSourceType;
    }

    public String getDriverArtifactId() {
        return driverArtifactId;
    }

    public void setDriverArtifactId(String driverArtifactId) {
        this.driverArtifactId = driverArtifactId;
    }

    public String getDriverVersionLabel() {
        return driverVersionLabel;
    }

    public void setDriverVersionLabel(String driverVersionLabel) {
        this.driverVersionLabel = driverVersionLabel;
    }

    public String getDriverSha256() {
        return driverSha256;
    }

    public void setDriverSha256(String driverSha256) {
        this.driverSha256 = driverSha256;
    }

    public String getDriverRelativePath() {
        return driverRelativePath;
    }

    public void setDriverRelativePath(String driverRelativePath) {
        this.driverRelativePath = driverRelativePath;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public String getCredentialMask() {
        return credentialMask;
    }

    public void setCredentialMask(String credentialMask) {
        this.credentialMask = credentialMask;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
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
