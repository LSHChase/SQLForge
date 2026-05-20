package com.company.sqloptimization.domain.plan;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import com.company.sqloptimization.domain.task.OptimizationTaskBenefit;
import com.company.sqloptimization.domain.task.OptimizationTaskCost;
import com.company.sqloptimization.domain.task.OptimizationTaskRisk;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AccelerationPlan {

    private final String planId;
    private final String tenantId;
    private final String sourceTaskId;
    private final String sqlText;
    private final String sqlFingerprint;
    private final DataSourceTypeEnum datasourceType;
    private final List<AccelerationSuggestionType> selectedSuggestionTypes;
    private final String planSummary;
    private final String primaryRecommendation;
    private final String planPayloadJson;
    private final List<OptimizationTaskBenefit> benefits;
    private final List<OptimizationTaskCost> costs;
    private final List<OptimizationTaskRisk> risks;
    private final Instant createdAt;
    private final List<AccelerationPlanStatusTransition> statusHistory;

    private String configSnapshotId;
    private String resultId;
    private String historyId;
    private AccelerationPlanStatus status;
    private Integer lastErrorCode;
    private String lastErrorMessage;
    private String activationEvidenceJson;
    private Instant activatedAt;
    private String activatedBy;
    private String pauseEvidenceJson;
    private Instant pausedAt;
    private String pausedBy;
    private Instant updatedAt;

    private AccelerationPlan(String planId,
                             String tenantId,
                             String sourceTaskId,
                             String sqlText,
                             String sqlFingerprint,
                             DataSourceTypeEnum datasourceType,
                             List<AccelerationSuggestionType> selectedSuggestionTypes,
                             String planSummary,
                             String primaryRecommendation,
                             String planPayloadJson,
                             List<OptimizationTaskBenefit> benefits,
                             List<OptimizationTaskCost> costs,
                             List<OptimizationTaskRisk> risks,
                             Instant createdAt,
                             List<AccelerationPlanStatusTransition> statusHistory,
                             AccelerationPlanStatus status) {
        this.planId = planId;
        this.tenantId = tenantId;
        this.sourceTaskId = sourceTaskId;
        this.sqlText = sqlText;
        this.sqlFingerprint = sqlFingerprint;
        this.datasourceType = datasourceType;
        this.selectedSuggestionTypes = immutable(selectedSuggestionTypes);
        this.planSummary = planSummary;
        this.primaryRecommendation = primaryRecommendation;
        this.planPayloadJson = planPayloadJson;
        this.benefits = immutable(benefits);
        this.costs = immutable(costs);
        this.risks = immutable(risks);
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.statusHistory = history(statusHistory);
        this.status = status == null ? AccelerationPlanStatus.READY : status;
    }

    public static AccelerationPlan submit(String planId,
                                          String tenantId,
                                          String sourceTaskId,
                                          String sqlText,
                                          String sqlFingerprint,
                                          DataSourceTypeEnum datasourceType,
                                          List<AccelerationSuggestionType> selectedSuggestionTypes,
                                          String planSummary,
                                          String primaryRecommendation,
                                          String planPayloadJson,
                                          List<OptimizationTaskBenefit> benefits,
                                          List<OptimizationTaskCost> costs,
                                          List<OptimizationTaskRisk> risks,
                                          Instant createdAt) {
        List<AccelerationPlanStatusTransition> history = new ArrayList<AccelerationPlanStatusTransition>();
        history.add(new AccelerationPlanStatusTransition(null, AccelerationPlanStatus.READY, createdAt, "PLAN_SUBMITTED"));
        return new AccelerationPlan(
            planId,
            tenantId,
            sourceTaskId,
            sqlText,
            sqlFingerprint,
            datasourceType,
            selectedSuggestionTypes,
            planSummary,
            primaryRecommendation,
            planPayloadJson,
            benefits,
            costs,
            risks,
            createdAt,
            history,
            AccelerationPlanStatus.READY
        );
    }

    public static AccelerationPlan restore(String planId,
                                           String tenantId,
                                           String sourceTaskId,
                                           String sqlText,
                                           String sqlFingerprint,
                                           DataSourceTypeEnum datasourceType,
                                           List<AccelerationSuggestionType> selectedSuggestionTypes,
                                           String planSummary,
                                           String primaryRecommendation,
                                           String planPayloadJson,
                                           List<OptimizationTaskBenefit> benefits,
                                           List<OptimizationTaskCost> costs,
                                           List<OptimizationTaskRisk> risks,
                                           Instant createdAt,
                                           List<AccelerationPlanStatusTransition> statusHistory,
                                           String configSnapshotId,
                                           String resultId,
                                           String historyId,
                                           AccelerationPlanStatus status,
                                           Integer lastErrorCode,
                                           String lastErrorMessage,
                                           String activationEvidenceJson,
                                           Instant activatedAt,
                                           String activatedBy,
                                           String pauseEvidenceJson,
                                           Instant pausedAt,
                                           String pausedBy,
                                           Instant updatedAt) {
        AccelerationPlan plan = new AccelerationPlan(
            planId,
            tenantId,
            sourceTaskId,
            sqlText,
            sqlFingerprint,
            datasourceType,
            selectedSuggestionTypes,
            planSummary,
            primaryRecommendation,
            planPayloadJson,
            benefits,
            costs,
            risks,
            createdAt,
            statusHistory,
            status
        );
        plan.configSnapshotId = configSnapshotId;
        plan.resultId = resultId;
        plan.historyId = historyId;
        plan.lastErrorCode = lastErrorCode;
        plan.lastErrorMessage = lastErrorMessage;
        plan.activationEvidenceJson = activationEvidenceJson;
        plan.activatedAt = activatedAt;
        plan.activatedBy = activatedBy;
        plan.pauseEvidenceJson = pauseEvidenceJson;
        plan.pausedAt = pausedAt;
        plan.pausedBy = pausedBy;
        plan.updatedAt = updatedAt == null ? createdAt : updatedAt;
        return plan;
    }

    public void attachGovernanceTrace(String configSnapshotId, String resultId, String historyId) {
        this.configSnapshotId = configSnapshotId;
        this.resultId = resultId;
        this.historyId = historyId;
    }

    public void markActivated(String activationEvidenceJson, String operator, Instant occurredAt) {
        requireActivateEligible();
        transitionTo(AccelerationPlanStatus.ACTIVE, occurredAt, "PLAN_ACTIVATED");
        this.activationEvidenceJson = activationEvidenceJson;
        this.activatedBy = operator;
        this.activatedAt = occurredAt;
        this.lastErrorCode = null;
        this.lastErrorMessage = null;
    }

    public void markActivateFailed(Integer errorCode, String errorMessage, String activationEvidenceJson, String operator, Instant occurredAt) {
        requireActivateEligible();
        transitionTo(AccelerationPlanStatus.ACTIVATE_FAILED, occurredAt, "PLAN_ACTIVATE_FAILED");
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = errorMessage;
        this.activationEvidenceJson = activationEvidenceJson;
        this.activatedBy = operator;
        this.activatedAt = occurredAt;
    }

    public void markPaused(String pauseEvidenceJson, String operator, Instant occurredAt) {
        requirePauseEligible();
        transitionTo(AccelerationPlanStatus.PAUSED, occurredAt, "PLAN_PAUSED");
        this.pauseEvidenceJson = pauseEvidenceJson;
        this.pausedBy = operator;
        this.pausedAt = occurredAt;
        this.lastErrorCode = null;
        this.lastErrorMessage = null;
    }

    public void markPauseFailed(Integer errorCode, String errorMessage, String pauseEvidenceJson, String operator, Instant occurredAt) {
        requirePauseEligible();
        transitionTo(AccelerationPlanStatus.PAUSE_FAILED, occurredAt, "PLAN_PAUSE_FAILED");
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = errorMessage;
        this.pauseEvidenceJson = pauseEvidenceJson;
        this.pausedBy = operator;
        this.pausedAt = occurredAt;
    }

    private void requireActivateEligible() {
        if (status == AccelerationPlanStatus.READY
            || status == AccelerationPlanStatus.ACTIVATE_FAILED
            || status == AccelerationPlanStatus.PAUSED) {
            return;
        }
        throw new IllegalStateException("加速方案激活前必须处于 READY、ACTIVATE_FAILED 或 PAUSED 状态。");
    }

    private void requirePauseEligible() {
        if (status == AccelerationPlanStatus.ACTIVE
            || status == AccelerationPlanStatus.PAUSED
            || status == AccelerationPlanStatus.PAUSE_FAILED) {
            return;
        }
        throw new IllegalStateException("加速方案暂停前必须处于 ACTIVE、PAUSED 或 PAUSE_FAILED 状态。");
    }

    private void transitionTo(AccelerationPlanStatus nextStatus, Instant occurredAt, String note) {
        statusHistory.add(new AccelerationPlanStatusTransition(status, nextStatus, occurredAt, note));
        status = nextStatus;
        updatedAt = occurredAt;
    }

    private static <T> List<T> immutable(List<T> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<T>(values));
    }

    private static List<AccelerationPlanStatusTransition> history(List<AccelerationPlanStatusTransition> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<AccelerationPlanStatusTransition>();
        }
        return new ArrayList<AccelerationPlanStatusTransition>(values);
    }

    public String getPlanId() { return planId; }
    public String getTenantId() { return tenantId; }
    public String getSourceTaskId() { return sourceTaskId; }
    public String getSqlText() { return sqlText; }
    public String getSqlFingerprint() { return sqlFingerprint; }
    public DataSourceTypeEnum getDatasourceType() { return datasourceType; }
    public List<AccelerationSuggestionType> getSelectedSuggestionTypes() { return selectedSuggestionTypes; }
    public String getPlanSummary() { return planSummary; }
    public String getPrimaryRecommendation() { return primaryRecommendation; }
    public String getPlanPayloadJson() { return planPayloadJson; }
    public List<OptimizationTaskBenefit> getBenefits() { return benefits; }
    public List<OptimizationTaskCost> getCosts() { return costs; }
    public List<OptimizationTaskRisk> getRisks() { return risks; }
    public Instant getCreatedAt() { return createdAt; }
    public String getConfigSnapshotId() { return configSnapshotId; }
    public String getResultId() { return resultId; }
    public String getHistoryId() { return historyId; }
    public AccelerationPlanStatus getStatus() { return status; }
    public Integer getLastErrorCode() { return lastErrorCode; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public String getActivationEvidenceJson() { return activationEvidenceJson; }
    public Instant getActivatedAt() { return activatedAt; }
    public String getActivatedBy() { return activatedBy; }
    public String getPauseEvidenceJson() { return pauseEvidenceJson; }
    public Instant getPausedAt() { return pausedAt; }
    public String getPausedBy() { return pausedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<AccelerationPlanStatusTransition> getStatusHistory() { return Collections.unmodifiableList(statusHistory); }
}
