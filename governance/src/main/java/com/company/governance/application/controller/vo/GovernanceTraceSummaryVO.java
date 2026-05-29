package com.company.governance.application.controller.vo;

import java.time.LocalDateTime;
import java.util.Map;

public class GovernanceTraceSummaryVO extends GovernanceTraceSummaryBaseVO {

    public GovernanceTraceSummaryVO() {
    }

    public GovernanceTraceSummaryVO(String traceId,
                                    String requestId,
                                    String serviceCode,
                                    String operationType,
                                    String resourceType,
                                    String resourceId,
                                    String latestStatus,
                                    LocalDateTime lastSeenAt,
                                    Integer auditEventCount,
                                    Integer nonSuccessEventCount,
                                    Integer queryHistoryCount,
                                    Integer exportRecordCount,
                                    String taskId,
                                    String reportId,
                                    String sqlFingerprint,
                                    String errorCode,
                                    String targetEngine,
                                    Boolean degraded) {
        super(traceId, requestId, serviceCode, operationType, resourceType, resourceId, latestStatus, lastSeenAt,
            auditEventCount, nonSuccessEventCount, queryHistoryCount, exportRecordCount, taskId, reportId,
            sqlFingerprint, errorCode, targetEngine, degraded);
    }

    public GovernanceTraceSummaryVO(String traceId,
                                    String requestId,
                                    String serviceCode,
                                    String operationType,
                                    String resourceType,
                                    String resourceId,
                                    String latestStatus,
                                    LocalDateTime lastSeenAt,
                                    Integer auditEventCount,
                                    Integer nonSuccessEventCount,
                                    Integer queryHistoryCount,
                                    Integer exportRecordCount,
                                    String taskId,
                                    String reportId,
                                    String sqlFingerprint,
                                    String errorCode,
                                    String targetEngine,
                                    Boolean degraded,
                                    Map<String, Object> compensationReplayEvidence,
                                    Map<String, Object> cacheGovernanceSurface,
                                    Map<String, Object> artifactStorageContract,
                                    Map<String, Object> artifactRecoverySurface,
                                    Map<String, Object> artifactOperationSurface) {
        super(traceId, requestId, serviceCode, operationType, resourceType, resourceId, latestStatus, lastSeenAt,
            auditEventCount, nonSuccessEventCount, queryHistoryCount, exportRecordCount, taskId, reportId,
            sqlFingerprint, errorCode, targetEngine, degraded, compensationReplayEvidence, cacheGovernanceSurface,
            artifactStorageContract, artifactRecoverySurface, artifactOperationSurface);
    }
}
