package com.company.sqloptimization.domain.task;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Async optimization task aggregate used to freeze task metadata and validated status transitions.
 */
public class OptimizationTask {

    private final String taskId;
    private final String tenantId;
    private final OptimizationTaskType taskType;
    private final String sqlText;
    private final String sqlFingerprint;
    private final DataSourceTypeEnum datasourceType;
    private final OptimizationTaskPriority priority;
    private final OptimizationParseDepth parseDepth;
    private final String callbackUrl;
    private final List<AccelerationSuggestionType> requestedSuggestionTypes;
    private final Instant submittedAt;
    private final List<OptimizationTaskStatusTransition> statusHistory;

    private OptimizationTaskStatus status;
    private OptimizationTaskPhase currentPhase;
    private Integer progressPercent;
    private Instant startedAt;
    private Instant finishedAt;
    private String summary;
    private OptimizationTaskSuggestion suggestion;
    private OptimizationTaskError error;

    private OptimizationTask(String taskId,
                             OptimizationTaskSubmission submission,
                             Instant submittedAt,
                             List<OptimizationTaskStatusTransition> statusHistory) {
        this.taskId = taskId;
        this.tenantId = submission.getTenantId();
        this.taskType = submission.getTaskType();
        this.sqlText = submission.getSqlText();
        this.sqlFingerprint = submission.getSqlFingerprint();
        this.datasourceType = submission.getDatasourceType();
        this.priority = submission.getPriority();
        this.parseDepth = submission.getParseDepth();
        this.callbackUrl = submission.getCallbackUrl();
        this.requestedSuggestionTypes = submission.getRequestedSuggestionTypes();
        this.submittedAt = submittedAt;
        this.status = OptimizationTaskStatus.QUEUED;
        this.currentPhase = OptimizationTaskPhase.SUBMITTED;
        this.progressPercent = Integer.valueOf(0);
        this.statusHistory = statusHistory;
    }

    private OptimizationTask(String taskId,
                             OptimizationTaskSubmission submission,
                             Instant submittedAt,
                             List<OptimizationTaskStatusTransition> statusHistory,
                             OptimizationTaskStatus status,
                             OptimizationTaskPhase currentPhase,
                             Integer progressPercent,
                             Instant startedAt,
                             Instant finishedAt,
                             String summary,
                             OptimizationTaskSuggestion suggestion,
                             OptimizationTaskError error) {
        this.taskId = taskId;
        this.tenantId = submission.getTenantId();
        this.taskType = submission.getTaskType();
        this.sqlText = submission.getSqlText();
        this.sqlFingerprint = submission.getSqlFingerprint();
        this.datasourceType = submission.getDatasourceType();
        this.priority = submission.getPriority();
        this.parseDepth = submission.getParseDepth();
        this.callbackUrl = submission.getCallbackUrl();
        this.requestedSuggestionTypes = submission.getRequestedSuggestionTypes();
        this.submittedAt = submittedAt;
        this.statusHistory = statusHistory == null
            ? new ArrayList<OptimizationTaskStatusTransition>()
            : new ArrayList<OptimizationTaskStatusTransition>(statusHistory);
        this.status = status == null ? OptimizationTaskStatus.QUEUED : status;
        this.currentPhase = currentPhase == null ? OptimizationTaskPhase.SUBMITTED : currentPhase;
        this.progressPercent = progressPercent == null ? Integer.valueOf(0) : progressPercent;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.summary = summary;
        this.suggestion = suggestion;
        this.error = error;
    }

    public static OptimizationTask submit(String taskId, OptimizationTaskSubmission submission, Instant submittedAt) {
        List<OptimizationTaskStatusTransition> history = new ArrayList<OptimizationTaskStatusTransition>();
        history.add(
            new OptimizationTaskStatusTransition(
                null,
                OptimizationTaskStatus.QUEUED,
                null,
                OptimizationTaskPhase.SUBMITTED,
                submittedAt,
                "TASK_SUBMITTED"
            )
        );
        return new OptimizationTask(taskId, submission, submittedAt, history);
    }

    public static OptimizationTask restore(String taskId,
                                           OptimizationTaskSubmission submission,
                                           Instant submittedAt,
                                           List<OptimizationTaskStatusTransition> statusHistory,
                                           OptimizationTaskStatus status,
                                           OptimizationTaskPhase currentPhase,
                                           Integer progressPercent,
                                           Instant startedAt,
                                           Instant finishedAt,
                                           String summary,
                                           OptimizationTaskSuggestion suggestion,
                                           OptimizationTaskError error) {
        return new OptimizationTask(
            taskId,
            submission,
            submittedAt,
            statusHistory,
            status,
            currentPhase,
            progressPercent,
            startedAt,
            finishedAt,
            summary,
            suggestion,
            error
        );
    }

    public void markRunning(Instant actualStartedAt) {
        OptimizationTaskStateFlow.validateStart(taskType, status, currentPhase);
        OptimizationTaskPhase nextPhase = OptimizationTaskStateFlow.firstRunningPhase(taskType);
        recordTransition(OptimizationTaskStatus.RUNNING, nextPhase, actualStartedAt, "TASK_STARTED");
        this.status = OptimizationTaskStatus.RUNNING;
        this.currentPhase = nextPhase;
        this.startedAt = actualStartedAt;
        this.progressPercent = Integer.valueOf(10);
    }

    public void advancePhase(OptimizationTaskPhase nextPhase, int nextProgressPercent, String note) {
        OptimizationTaskStateFlow.validatePhaseAdvance(taskType, status, currentPhase, nextPhase);
        recordTransition(status, nextPhase, Instant.now(), note);
        this.currentPhase = nextPhase;
        this.progressPercent = Integer.valueOf(normalizeProgress(nextProgressPercent));
    }

    public void markSucceeded(OptimizationTaskSuggestion actualSuggestion, Instant actualFinishedAt) {
        OptimizationTaskStateFlow.validateCompletion(taskType, status, currentPhase);
        recordTransition(OptimizationTaskStatus.SUCCEEDED, OptimizationTaskPhase.FINISHED, actualFinishedAt, "TASK_SUCCEEDED");
        this.status = OptimizationTaskStatus.SUCCEEDED;
        this.currentPhase = OptimizationTaskPhase.FINISHED;
        this.finishedAt = actualFinishedAt;
        this.progressPercent = Integer.valueOf(100);
        this.summary = actualSuggestion == null ? null : actualSuggestion.getSummary();
        this.suggestion = actualSuggestion;
        this.error = null;
    }

    public void markFailed(OptimizationTaskError actualError, Instant actualFinishedAt) {
        OptimizationTaskStateFlow.validateTermination(status);
        recordTransition(OptimizationTaskStatus.FAILED, OptimizationTaskPhase.FINISHED, actualFinishedAt, "TASK_FAILED");
        this.status = OptimizationTaskStatus.FAILED;
        this.currentPhase = OptimizationTaskPhase.FINISHED;
        this.finishedAt = actualFinishedAt;
        this.suggestion = null;
        this.error = actualError;
    }

    public void cancel(String reason, Instant actualFinishedAt) {
        OptimizationTaskStateFlow.validateTermination(status);
        recordTransition(OptimizationTaskStatus.CANCELLED, OptimizationTaskPhase.FINISHED, actualFinishedAt, reason);
        this.status = OptimizationTaskStatus.CANCELLED;
        this.currentPhase = OptimizationTaskPhase.FINISHED;
        this.finishedAt = actualFinishedAt;
        this.suggestion = null;
    }

    private void recordTransition(OptimizationTaskStatus nextStatus,
                                  OptimizationTaskPhase nextPhase,
                                  Instant occurredAt,
                                  String note) {
        statusHistory.add(
            new OptimizationTaskStatusTransition(
                status,
                nextStatus,
                currentPhase,
                nextPhase,
                occurredAt,
                note
            )
        );
    }

    private int normalizeProgress(int nextProgressPercent) {
        if (nextProgressPercent < 0) {
            return 0;
        }
        if (nextProgressPercent > 99) {
            return 99;
        }
        return nextProgressPercent;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public OptimizationTaskType getTaskType() {
        return taskType;
    }

    public String getSqlText() {
        return sqlText;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public DataSourceTypeEnum getDatasourceType() {
        return datasourceType;
    }

    public OptimizationTaskPriority getPriority() {
        return priority;
    }

    public OptimizationParseDepth getParseDepth() {
        return parseDepth;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public List<AccelerationSuggestionType> getRequestedSuggestionTypes() {
        return requestedSuggestionTypes;
    }

    public OptimizationTaskStatus getStatus() {
        return status;
    }

    public OptimizationTaskPhase getCurrentPhase() {
        return currentPhase;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public String getSummary() {
        return summary;
    }

    public OptimizationTaskSuggestion getSuggestion() {
        return suggestion;
    }

    public OptimizationTaskError getError() {
        return error;
    }

    public List<OptimizationTaskStatusTransition> getStatusHistory() {
        return Collections.unmodifiableList(statusHistory);
    }
}
