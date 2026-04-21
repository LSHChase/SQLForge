package com.company.queryexecution.application.controller.vo;

import com.company.queryexecution.domain.query.QueryExecutionStatus;
import java.util.List;
import java.util.Map;

public class QueryExecuteResponse {

    private final QueryExecutionStatus status;
    private final List<Map<String, Object>> rows;
    private final String downloadUrl;
    private final QueryExecutionMetadataVO metadata;
    private final boolean degraded;
    private final String degradeReason;
    private final List<QueryRetryStepVO> retryPath;
    private final QueryErrorDetailVO error;
    private final String sqlFingerprint;
    private final String contractStage;
    private final String implementationStage;

    public QueryExecuteResponse(QueryExecutionStatus status,
                                List<Map<String, Object>> rows,
                                String downloadUrl,
                                QueryExecutionMetadataVO metadata,
                                boolean degraded,
                                String degradeReason,
                                List<QueryRetryStepVO> retryPath,
                                QueryErrorDetailVO error,
                                String sqlFingerprint,
                                String contractStage,
                                String implementationStage) {
        this.status = status;
        this.rows = rows;
        this.downloadUrl = downloadUrl;
        this.metadata = metadata;
        this.degraded = degraded;
        this.degradeReason = degradeReason;
        this.retryPath = retryPath;
        this.error = error;
        this.sqlFingerprint = sqlFingerprint;
        this.contractStage = contractStage;
        this.implementationStage = implementationStage;
    }

    public QueryExecutionStatus getStatus() {
        return status;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public QueryExecutionMetadataVO getMetadata() {
        return metadata;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public String getDegradeReason() {
        return degradeReason;
    }

    public List<QueryRetryStepVO> getRetryPath() {
        return retryPath;
    }

    public QueryErrorDetailVO getError() {
        return error;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public String getContractStage() {
        return contractStage;
    }

    public String getImplementationStage() {
        return implementationStage;
    }
}
