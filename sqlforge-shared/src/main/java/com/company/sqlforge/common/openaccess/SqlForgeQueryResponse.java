package com.company.sqlforge.common.openaccess;

import java.util.List;
import java.util.Map;

public class SqlForgeQueryResponse {

    private SqlForgeQueryStatus status;
    private List<Map<String, Object>> rows;
    private String downloadUrl;
    private SqlForgeQueryMetadata metadata;
    private boolean degraded;
    private String degradeReason;
    private List<SqlForgeQueryRetryStep> retryPath;
    private SqlForgeQueryError error;
    private String sqlFingerprint;
    private String contractStage;
    private String implementationStage;

    public SqlForgeQueryStatus getStatus() {
        return status;
    }

    public void setStatus(SqlForgeQueryStatus status) {
        this.status = status;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, Object>> rows) {
        this.rows = rows;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public SqlForgeQueryMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(SqlForgeQueryMetadata metadata) {
        this.metadata = metadata;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public void setDegraded(boolean degraded) {
        this.degraded = degraded;
    }

    public String getDegradeReason() {
        return degradeReason;
    }

    public void setDegradeReason(String degradeReason) {
        this.degradeReason = degradeReason;
    }

    public List<SqlForgeQueryRetryStep> getRetryPath() {
        return retryPath;
    }

    public void setRetryPath(List<SqlForgeQueryRetryStep> retryPath) {
        this.retryPath = retryPath;
    }

    public SqlForgeQueryError getError() {
        return error;
    }

    public void setError(SqlForgeQueryError error) {
        this.error = error;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public void setSqlFingerprint(String sqlFingerprint) {
        this.sqlFingerprint = sqlFingerprint;
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
