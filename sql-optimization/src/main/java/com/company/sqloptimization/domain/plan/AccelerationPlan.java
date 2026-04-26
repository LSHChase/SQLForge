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
    private String reviewNote;
    private String approvedBy;
    private Instant approvedAt;
    private String rejectedBy;
    private Instant rejectedAt;
    private Integer lastErrorCode;
    private String lastErrorMessage;
    private String runtimeBindingJson;
    private Instant runtimeBindingAt;
    private String runtimeBindingBy;
    private String verificationEvidenceJson;
    private Instant verifiedAt;
    private String verifiedBy;
    private String rollbackEvidenceJson;
    private Instant rolledBackAt;
    private String rolledBackBy;
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
                             List<AccelerationPlanStatusTransition> statusHistory) {
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
        this.status = AccelerationPlanStatus.PENDING_APPROVAL;
    }

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
                             String configSnapshotId,
                             String resultId,
                             String historyId,
                             AccelerationPlanStatus status,
                             String reviewNote,
                             String approvedBy,
                             Instant approvedAt,
                             String rejectedBy,
                             Instant rejectedAt,
                             Integer lastErrorCode,
                             String lastErrorMessage,
                             String runtimeBindingJson,
                             Instant runtimeBindingAt,
                             String runtimeBindingBy,
                             String verificationEvidenceJson,
                             Instant verifiedAt,
                             String verifiedBy,
                             String rollbackEvidenceJson,
                             Instant rolledBackAt,
                             String rolledBackBy,
                             Instant updatedAt) {
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
        this.statusHistory = history(statusHistory);
        this.configSnapshotId = configSnapshotId;
        this.resultId = resultId;
        this.historyId = historyId;
        this.status = status == null ? AccelerationPlanStatus.PENDING_APPROVAL : status;
        this.reviewNote = reviewNote;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.rejectedBy = rejectedBy;
        this.rejectedAt = rejectedAt;
        this.lastErrorCode = lastErrorCode;
        this.lastErrorMessage = lastErrorMessage;
        this.runtimeBindingJson = runtimeBindingJson;
        this.runtimeBindingAt = runtimeBindingAt;
        this.runtimeBindingBy = runtimeBindingBy;
        this.verificationEvidenceJson = verificationEvidenceJson;
        this.verifiedAt = verifiedAt;
        this.verifiedBy = verifiedBy;
        this.rollbackEvidenceJson = rollbackEvidenceJson;
        this.rolledBackAt = rolledBackAt;
        this.rolledBackBy = rolledBackBy;
        this.updatedAt = updatedAt == null ? createdAt : updatedAt;
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
        history.add(new AccelerationPlanStatusTransition(null, AccelerationPlanStatus.PENDING_APPROVAL, createdAt, "PLAN_SUBMITTED"));
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
            history
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
                                           String reviewNote,
                                           String approvedBy,
                                           Instant approvedAt,
                                           String rejectedBy,
                                           Instant rejectedAt,
                                           Integer lastErrorCode,
                                           String lastErrorMessage,
                                           String runtimeBindingJson,
                                           Instant runtimeBindingAt,
                                           String runtimeBindingBy,
                                           String verificationEvidenceJson,
                                           Instant verifiedAt,
                                           String verifiedBy,
                                           String rollbackEvidenceJson,
                                           Instant rolledBackAt,
                                           String rolledBackBy,
                                           Instant updatedAt) {
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
            statusHistory,
            configSnapshotId,
            resultId,
            historyId,
            status,
            reviewNote,
            approvedBy,
            approvedAt,
            rejectedBy,
            rejectedAt,
            lastErrorCode,
            lastErrorMessage,
            runtimeBindingJson,
            runtimeBindingAt,
            runtimeBindingBy,
            verificationEvidenceJson,
            verifiedAt,
            verifiedBy,
            rollbackEvidenceJson,
            rolledBackAt,
            rolledBackBy,
            updatedAt
        );
    }

    public void attachGovernanceTrace(String configSnapshotId, String resultId, String historyId) {
        this.configSnapshotId = configSnapshotId;
        this.resultId = resultId;
        this.historyId = historyId;
    }

    public void approve(String reviewNote, String operator, Instant occurredAt) {
        requireStatus(AccelerationPlanStatus.PENDING_APPROVAL, "Acceleration plan must be pending approval.");
        transitionTo(AccelerationPlanStatus.APPROVED, occurredAt, "PLAN_APPROVED");
        this.reviewNote = reviewNote;
        this.approvedBy = operator;
        this.approvedAt = occurredAt;
        this.rejectedBy = null;
        this.rejectedAt = null;
        this.lastErrorCode = null;
        this.lastErrorMessage = null;
    }

    public void reject(String reviewNote, String operator, Instant occurredAt) {
        requireStatus(AccelerationPlanStatus.PENDING_APPROVAL, "Acceleration plan must be pending approval.");
        transitionTo(AccelerationPlanStatus.REJECTED, occurredAt, "PLAN_REJECTED");
        this.reviewNote = reviewNote;
        this.rejectedBy = operator;
        this.rejectedAt = occurredAt;
        this.lastErrorCode = null;
        this.lastErrorMessage = null;
    }

    public void markApplied(String runtimeBindingJson, String operator, Instant occurredAt) {
        requireApplyEligible();
        transitionTo(AccelerationPlanStatus.APPLIED, occurredAt, "PLAN_APPLIED");
        this.runtimeBindingJson = runtimeBindingJson;
        this.runtimeBindingBy = operator;
        this.runtimeBindingAt = occurredAt;
        this.lastErrorCode = null;
        this.lastErrorMessage = null;
    }

    public void markApplyFailed(Integer errorCode, String errorMessage, String runtimeBindingJson, String operator, Instant occurredAt) {
        requireApplyEligible();
        transitionTo(AccelerationPlanStatus.APPLY_FAILED, occurredAt, "PLAN_APPLY_FAILED");
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = errorMessage;
        this.runtimeBindingJson = runtimeBindingJson;
        this.runtimeBindingBy = operator;
        this.runtimeBindingAt = occurredAt;
    }

    public void markVerified(String verificationEvidenceJson, String operator, Instant occurredAt) {
        if (!(status == AccelerationPlanStatus.APPLIED || status == AccelerationPlanStatus.VERIFY_FAILED)) {
            throw new IllegalStateException("Acceleration plan must be applied before verification.");
        }
        transitionTo(AccelerationPlanStatus.VERIFIED, occurredAt, "PLAN_VERIFIED");
        this.verificationEvidenceJson = verificationEvidenceJson;
        this.verifiedBy = operator;
        this.verifiedAt = occurredAt;
        this.lastErrorCode = null;
        this.lastErrorMessage = null;
    }

    public void markVerificationFailed(Integer errorCode, String errorMessage, String verificationEvidenceJson, String operator, Instant occurredAt) {
        if (!(status == AccelerationPlanStatus.APPLIED || status == AccelerationPlanStatus.VERIFY_FAILED)) {
            throw new IllegalStateException("Acceleration plan must be applied before verification.");
        }
        transitionTo(AccelerationPlanStatus.VERIFY_FAILED, occurredAt, "PLAN_VERIFY_FAILED");
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = errorMessage;
        this.verificationEvidenceJson = verificationEvidenceJson;
        this.verifiedBy = operator;
        this.verifiedAt = occurredAt;
    }

    public void markRolledBack(String rollbackEvidenceJson, String operator, Instant occurredAt) {
        requireRollbackEligible();
        transitionTo(AccelerationPlanStatus.ROLLED_BACK, occurredAt, "PLAN_ROLLED_BACK");
        this.rollbackEvidenceJson = rollbackEvidenceJson;
        this.rolledBackBy = operator;
        this.rolledBackAt = occurredAt;
        this.lastErrorCode = null;
        this.lastErrorMessage = null;
    }

    public void markRollbackFailed(Integer errorCode, String errorMessage, String rollbackEvidenceJson, String operator, Instant occurredAt) {
        requireRollbackEligible();
        transitionTo(AccelerationPlanStatus.ROLLBACK_FAILED, occurredAt, "PLAN_ROLLBACK_FAILED");
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = errorMessage;
        this.rollbackEvidenceJson = rollbackEvidenceJson;
        this.rolledBackBy = operator;
        this.rolledBackAt = occurredAt;
    }

    private void requireStatus(AccelerationPlanStatus expectedStatus, String message) {
        if (status != expectedStatus) {
            throw new IllegalStateException(message);
        }
    }

    private void requireApplyEligible() {
        if (!(status == AccelerationPlanStatus.APPROVED
            || status == AccelerationPlanStatus.APPLY_FAILED
            || status == AccelerationPlanStatus.ROLLED_BACK)) {
            throw new IllegalStateException("Acceleration plan must be approved before apply.");
        }
    }

    private void requireRollbackEligible() {
        if (!(status == AccelerationPlanStatus.APPLIED
            || status == AccelerationPlanStatus.VERIFIED
            || status == AccelerationPlanStatus.VERIFY_FAILED
            || status == AccelerationPlanStatus.ROLLBACK_FAILED)) {
            throw new IllegalStateException("Acceleration plan must be applied before rollback.");
        }
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

    public String getPlanId() {
        return planId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getSourceTaskId() {
        return sourceTaskId;
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

    public List<AccelerationSuggestionType> getSelectedSuggestionTypes() {
        return selectedSuggestionTypes;
    }

    public String getPlanSummary() {
        return planSummary;
    }

    public String getPrimaryRecommendation() {
        return primaryRecommendation;
    }

    public String getPlanPayloadJson() {
        return planPayloadJson;
    }

    public List<OptimizationTaskBenefit> getBenefits() {
        return benefits;
    }

    public List<OptimizationTaskCost> getCosts() {
        return costs;
    }

    public List<OptimizationTaskRisk> getRisks() {
        return risks;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getConfigSnapshotId() {
        return configSnapshotId;
    }

    public String getResultId() {
        return resultId;
    }

    public String getHistoryId() {
        return historyId;
    }

    public AccelerationPlanStatus getStatus() {
        return status;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public String getRejectedBy() {
        return rejectedBy;
    }

    public Instant getRejectedAt() {
        return rejectedAt;
    }

    public Integer getLastErrorCode() {
        return lastErrorCode;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public String getRuntimeBindingJson() {
        return runtimeBindingJson;
    }

    public Instant getRuntimeBindingAt() {
        return runtimeBindingAt;
    }

    public String getRuntimeBindingBy() {
        return runtimeBindingBy;
    }

    public String getVerificationEvidenceJson() {
        return verificationEvidenceJson;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public String getRollbackEvidenceJson() {
        return rollbackEvidenceJson;
    }

    public Instant getRolledBackAt() {
        return rolledBackAt;
    }

    public String getRolledBackBy() {
        return rolledBackBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<AccelerationPlanStatusTransition> getStatusHistory() {
        return Collections.unmodifiableList(statusHistory);
    }
}
