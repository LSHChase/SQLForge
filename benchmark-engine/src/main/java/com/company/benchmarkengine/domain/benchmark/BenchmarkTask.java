package com.company.benchmarkengine.domain.benchmark;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BenchmarkTask {

    private final String taskId;
    private final String tenantId;
    private final BenchmarkTaskType taskType;
    private final String sqlText;
    private final String sqlFingerprint;
    private final BenchmarkTaskPriority priority;
    private final List<DataSourceTypeEnum> targetEngines;
    private final Integer concurrency;
    private final Integer durationSeconds;
    private final Integer rampUpSeconds;
    private final String datasetSizeLabel;
    private final Boolean readonlyRequired;
    private final ShadowEnvironmentMode shadowEnvironmentMode;
    private final DesensitizationRequirement desensitizationRequirement;
    private final List<BenchmarkThreshold> thresholds;
    private final Instant submittedAt;
    private final List<BenchmarkTaskStatusTransition> statusHistory;

    private BenchmarkTaskStatus status;
    private BenchmarkTaskPhase currentPhase;
    private Integer progressPercent;
    private Instant startedAt;
    private Instant finishedAt;
    private String reportId;
    private BenchmarkTaskError error;

    private BenchmarkTask(String taskId,
                          BenchmarkTaskSubmission submission,
                          Instant submittedAt,
                          List<BenchmarkTaskStatusTransition> statusHistory) {
        this.taskId = taskId;
        this.tenantId = submission.getTenantId();
        this.taskType = submission.getTaskType();
        this.sqlText = submission.getSqlText();
        this.sqlFingerprint = submission.getSqlFingerprint();
        this.priority = submission.getPriority();
        this.targetEngines = submission.getTargetEngines();
        this.concurrency = submission.getConcurrency();
        this.durationSeconds = submission.getDurationSeconds();
        this.rampUpSeconds = submission.getRampUpSeconds();
        this.datasetSizeLabel = submission.getDatasetSizeLabel();
        this.readonlyRequired = submission.getReadonlyRequired();
        this.shadowEnvironmentMode = submission.getShadowEnvironmentMode();
        this.desensitizationRequirement = submission.getDesensitizationRequirement();
        this.thresholds = submission.getThresholds();
        this.submittedAt = submittedAt;
        this.statusHistory = statusHistory;
        this.status = BenchmarkTaskStatus.QUEUED;
        this.currentPhase = BenchmarkTaskPhase.SUBMITTED;
        this.progressPercent = Integer.valueOf(0);
    }

    private BenchmarkTask(String taskId,
                          BenchmarkTaskSubmission submission,
                          Instant submittedAt,
                          List<BenchmarkTaskStatusTransition> statusHistory,
                          BenchmarkTaskStatus status,
                          BenchmarkTaskPhase currentPhase,
                          Integer progressPercent,
                          Instant startedAt,
                          Instant finishedAt,
                          String reportId,
                          BenchmarkTaskError error) {
        this.taskId = taskId;
        this.tenantId = submission.getTenantId();
        this.taskType = submission.getTaskType();
        this.sqlText = submission.getSqlText();
        this.sqlFingerprint = submission.getSqlFingerprint();
        this.priority = submission.getPriority();
        this.targetEngines = submission.getTargetEngines();
        this.concurrency = submission.getConcurrency();
        this.durationSeconds = submission.getDurationSeconds();
        this.rampUpSeconds = submission.getRampUpSeconds();
        this.datasetSizeLabel = submission.getDatasetSizeLabel();
        this.readonlyRequired = submission.getReadonlyRequired();
        this.shadowEnvironmentMode = submission.getShadowEnvironmentMode();
        this.desensitizationRequirement = submission.getDesensitizationRequirement();
        this.thresholds = submission.getThresholds();
        this.submittedAt = submittedAt;
        this.statusHistory = statusHistory == null
            ? new ArrayList<BenchmarkTaskStatusTransition>()
            : new ArrayList<BenchmarkTaskStatusTransition>(statusHistory);
        this.status = status == null ? BenchmarkTaskStatus.QUEUED : status;
        this.currentPhase = currentPhase == null ? BenchmarkTaskPhase.SUBMITTED : currentPhase;
        this.progressPercent = progressPercent == null ? Integer.valueOf(0) : progressPercent;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.reportId = reportId;
        this.error = error;
    }

    public static BenchmarkTask submit(String taskId, BenchmarkTaskSubmission submission, Instant submittedAt) {
        List<BenchmarkTaskStatusTransition> history = new ArrayList<BenchmarkTaskStatusTransition>();
        history.add(
            new BenchmarkTaskStatusTransition(
                null,
                BenchmarkTaskStatus.QUEUED,
                null,
                BenchmarkTaskPhase.SUBMITTED,
                submittedAt,
                "TASK_SUBMITTED"
            )
        );
        return new BenchmarkTask(taskId, submission, submittedAt, history);
    }

    public static BenchmarkTask restore(String taskId,
                                        BenchmarkTaskSubmission submission,
                                        Instant submittedAt,
                                        List<BenchmarkTaskStatusTransition> statusHistory,
                                        BenchmarkTaskStatus status,
                                        BenchmarkTaskPhase currentPhase,
                                        Integer progressPercent,
                                        Instant startedAt,
                                        Instant finishedAt,
                                        String reportId,
                                        BenchmarkTaskError error) {
        return new BenchmarkTask(
            taskId,
            submission,
            submittedAt,
            statusHistory,
            status,
            currentPhase,
            progressPercent,
            startedAt,
            finishedAt,
            reportId,
            error
        );
    }

    public void markRunning(Instant actualStartedAt) {
        BenchmarkTaskStateFlow.validateStart(taskType, status, currentPhase);
        BenchmarkTaskPhase nextPhase = BenchmarkTaskStateFlow.firstRunningPhase(taskType);
        recordTransition(BenchmarkTaskStatus.RUNNING, nextPhase, actualStartedAt, "TASK_STARTED");
        this.status = BenchmarkTaskStatus.RUNNING;
        this.currentPhase = nextPhase;
        this.startedAt = actualStartedAt;
        this.progressPercent = Integer.valueOf(10);
    }

    public void advancePhase(BenchmarkTaskPhase nextPhase, int nextProgressPercent, String note) {
        BenchmarkTaskStateFlow.validatePhaseAdvance(taskType, status, currentPhase, nextPhase);
        recordTransition(status, nextPhase, Instant.now(), note);
        this.currentPhase = nextPhase;
        this.progressPercent = Integer.valueOf(normalizeProgress(nextProgressPercent));
    }

    public void markSucceeded(String actualReportId, Instant actualFinishedAt) {
        BenchmarkTaskStateFlow.validateCompletion(taskType, status, currentPhase);
        recordTransition(BenchmarkTaskStatus.SUCCEEDED, BenchmarkTaskPhase.FINISHED, actualFinishedAt, "TASK_SUCCEEDED");
        this.status = BenchmarkTaskStatus.SUCCEEDED;
        this.currentPhase = BenchmarkTaskPhase.FINISHED;
        this.finishedAt = actualFinishedAt;
        this.reportId = actualReportId;
        this.progressPercent = Integer.valueOf(100);
        this.error = null;
    }

    public void markFailed(BenchmarkTaskError actualError, Instant actualFinishedAt) {
        BenchmarkTaskStateFlow.validateTermination(status);
        recordTransition(BenchmarkTaskStatus.FAILED, BenchmarkTaskPhase.FINISHED, actualFinishedAt, "TASK_FAILED");
        this.status = BenchmarkTaskStatus.FAILED;
        this.currentPhase = BenchmarkTaskPhase.FINISHED;
        this.finishedAt = actualFinishedAt;
        this.error = actualError;
    }

    public void cancel(String reason, Instant actualFinishedAt) {
        BenchmarkTaskStateFlow.validateTermination(status);
        recordTransition(BenchmarkTaskStatus.CANCELLED, BenchmarkTaskPhase.FINISHED, actualFinishedAt, reason);
        this.status = BenchmarkTaskStatus.CANCELLED;
        this.currentPhase = BenchmarkTaskPhase.FINISHED;
        this.finishedAt = actualFinishedAt;
    }

    public void appendOperationalNote(String note) {
        if (note == null || note.trim().isEmpty()) {
            return;
        }
        statusHistory.add(
            new BenchmarkTaskStatusTransition(
                status,
                status,
                currentPhase,
                currentPhase,
                Instant.now(),
                note
            )
        );
    }

    private void recordTransition(BenchmarkTaskStatus nextStatus,
                                  BenchmarkTaskPhase nextPhase,
                                  Instant occurredAt,
                                  String note) {
        statusHistory.add(
            new BenchmarkTaskStatusTransition(
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

    public BenchmarkTaskType getTaskType() {
        return taskType;
    }

    public String getSqlText() {
        return sqlText;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public BenchmarkTaskPriority getPriority() {
        return priority;
    }

    public List<DataSourceTypeEnum> getTargetEngines() {
        return targetEngines;
    }

    public Integer getConcurrency() {
        return concurrency;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public Integer getRampUpSeconds() {
        return rampUpSeconds;
    }

    public String getDatasetSizeLabel() {
        return datasetSizeLabel;
    }

    public Boolean getReadonlyRequired() {
        return readonlyRequired;
    }

    public ShadowEnvironmentMode getShadowEnvironmentMode() {
        return shadowEnvironmentMode;
    }

    public DesensitizationRequirement getDesensitizationRequirement() {
        return desensitizationRequirement;
    }

    public List<BenchmarkThreshold> getThresholds() {
        return thresholds;
    }

    public BenchmarkTaskStatus getStatus() {
        return status;
    }

    public BenchmarkTaskPhase getCurrentPhase() {
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

    public String getReportId() {
        return reportId;
    }

    public BenchmarkTaskError getError() {
        return error;
    }

    public List<BenchmarkTaskStatusTransition> getStatusHistory() {
        return Collections.unmodifiableList(statusHistory);
    }
}
