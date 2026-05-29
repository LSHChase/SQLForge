package com.company.queryexecution.application.controller.vo;

import java.util.Collections;
import java.util.List;

public class QueryExecutionMetadataVO extends QueryExecutionMetadataBaseVO {

    public QueryExecutionMetadataVO(String targetEngine,
                                    String actualSql,
                                    long elapsedMs,
                                    long scannedRows,
                                    boolean cacheHit,
                                    boolean accelerationApplied) {
        this(targetEngine, actualSql, elapsedMs, scannedRows, cacheHit, accelerationApplied,
            "SIMULATED", Collections.singletonList("SIMULATED"), 0);
    }

    public QueryExecutionMetadataVO(String targetEngine,
                                    String actualSql,
                                    long elapsedMs,
                                    long scannedRows,
                                    boolean cacheHit,
                                    boolean accelerationApplied,
                                    String executionMode,
                                    List<String> attemptedModes,
                                    int rowCount) {
        this(targetEngine, actualSql, elapsedMs, scannedRows, cacheHit, accelerationApplied, executionMode,
            attemptedModes, rowCount, null, Collections.<String>emptyList(), null, null, null, null);
    }

    public QueryExecutionMetadataVO(String targetEngine,
                                    String actualSql,
                                    long elapsedMs,
                                    long scannedRows,
                                    boolean cacheHit,
                                    boolean accelerationApplied,
                                    String executionMode,
                                    List<String> attemptedModes,
                                    int rowCount,
                                    String routeProfile,
                                    List<String> routeOrder,
                                    String routeEvidenceSource,
                                    String routeVerificationStatus,
                                    String cacheGovernanceStatus,
                                    String cacheGovernanceEvidence) {
        this(targetEngine, actualSql, elapsedMs, scannedRows, cacheHit, accelerationApplied, executionMode,
            attemptedModes, rowCount, routeProfile, routeOrder, routeEvidenceSource, routeVerificationStatus,
            cacheGovernanceStatus, cacheGovernanceEvidence, null, false, null, null, null, null, null);
    }

    public QueryExecutionMetadataVO(String targetEngine,
                                    String actualSql,
                                    long elapsedMs,
                                    long scannedRows,
                                    boolean cacheHit,
                                    boolean accelerationApplied,
                                    String executionMode,
                                    List<String> attemptedModes,
                                    int rowCount,
                                    String routeProfile,
                                    List<String> routeOrder,
                                    String routeEvidenceSource,
                                    String routeVerificationStatus,
                                    String cacheGovernanceStatus,
                                    String cacheGovernanceEvidence,
                                    String originalSql,
                                    boolean rewriteApplied,
                                    String rewriteRecordId,
                                    String runtimeBindingId,
                                    Long ruleVersion,
                                    String runtimeRuleVersion,
                                    String rewriteFallbackReason) {
        this(targetEngine, actualSql, elapsedMs, scannedRows, cacheHit, accelerationApplied, executionMode,
            attemptedModes, rowCount, routeProfile, routeOrder, routeEvidenceSource, routeVerificationStatus,
            cacheGovernanceStatus, cacheGovernanceEvidence, originalSql, rewriteApplied, rewriteRecordId,
            runtimeBindingId, ruleVersion, runtimeRuleVersion, null, null, rewriteFallbackReason);
    }

    public QueryExecutionMetadataVO(String targetEngine,
                                    String actualSql,
                                    long elapsedMs,
                                    long scannedRows,
                                    boolean cacheHit,
                                    boolean accelerationApplied,
                                    String executionMode,
                                    List<String> attemptedModes,
                                    int rowCount,
                                    String routeProfile,
                                    List<String> routeOrder,
                                    String routeEvidenceSource,
                                    String routeVerificationStatus,
                                    String cacheGovernanceStatus,
                                    String cacheGovernanceEvidence,
                                    String originalSql,
                                    boolean rewriteApplied,
                                    String rewriteRecordId,
                                    String runtimeBindingId,
                                    Long ruleVersion,
                                    String runtimeRuleVersion,
                                    String runtimeRewriteStatus,
                                    String rewriteActivationStatusSnapshot,
                                    String rewriteFallbackReason) {
        super(targetEngine, actualSql, elapsedMs, scannedRows, cacheHit, accelerationApplied, executionMode,
            attemptedModes, rowCount, routeProfile, routeOrder, routeEvidenceSource, routeVerificationStatus,
            cacheGovernanceStatus, cacheGovernanceEvidence, originalSql, rewriteApplied, rewriteRecordId,
            runtimeBindingId, ruleVersion, runtimeRuleVersion, runtimeRewriteStatus,
            rewriteActivationStatusSnapshot, rewriteFallbackReason);
    }
}
