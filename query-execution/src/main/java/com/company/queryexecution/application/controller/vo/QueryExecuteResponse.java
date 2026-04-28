package com.company.queryexecution.application.controller.vo;

import com.company.queryexecution.domain.query.QueryExecutionStatus;
import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
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
    private final Map<String, String> commentContext;
    private final Map<String, Object> queryDateSummary;
    private final Map<String, Object> bindingSummary;
    private final List<LogicalObjectSurface> logicalObjectHits;
    private final Map<String, Object> routeSummary;
    private final Map<String, Object> cacheSummary;
    private final Map<String, Object> lightweightParseSummary;
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
        this(
            status,
            rows,
            downloadUrl,
            metadata,
            degraded,
            degradeReason,
            retryPath,
            error,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            sqlFingerprint,
            contractStage,
            implementationStage
        );
    }

    public QueryExecuteResponse(QueryExecutionStatus status,
                                List<Map<String, Object>> rows,
                                String downloadUrl,
                                QueryExecutionMetadataVO metadata,
                                boolean degraded,
                                String degradeReason,
                                List<QueryRetryStepVO> retryPath,
                                QueryErrorDetailVO error,
                                Map<String, String> commentContext,
                                Map<String, Object> queryDateSummary,
                                Map<String, Object> bindingSummary,
                                List<LogicalObjectSurface> logicalObjectHits,
                                Map<String, Object> routeSummary,
                                Map<String, Object> cacheSummary,
                                Map<String, Object> lightweightParseSummary,
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
        this.commentContext = commentContext;
        this.queryDateSummary = queryDateSummary;
        this.bindingSummary = bindingSummary;
        this.logicalObjectHits = logicalObjectHits;
        this.routeSummary = routeSummary;
        this.cacheSummary = cacheSummary;
        this.lightweightParseSummary = lightweightParseSummary;
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

    public Map<String, String> getCommentContext() {
        return commentContext;
    }

    public Map<String, Object> getQueryDateSummary() {
        return queryDateSummary;
    }

    public Map<String, Object> getBindingSummary() {
        return bindingSummary;
    }

    public List<LogicalObjectSurface> getLogicalObjectHits() {
        return logicalObjectHits;
    }

    public Map<String, Object> getRouteSummary() {
        return routeSummary;
    }

    public Map<String, Object> getCacheSummary() {
        return cacheSummary;
    }

    public Map<String, Object> getLightweightParseSummary() {
        return lightweightParseSummary;
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
