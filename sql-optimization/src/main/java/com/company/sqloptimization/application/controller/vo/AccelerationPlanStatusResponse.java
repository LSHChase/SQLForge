package com.company.sqloptimization.application.controller.vo;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.domain.plan.AccelerationPlanStatus;
import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import java.time.Instant;
import java.util.List;

public class AccelerationPlanStatusResponse {

    private final String planId;
    private final String sourceTaskId;
    private final AccelerationPlanStatus status;
    private final DataSourceTypeEnum datasourceType;
    private final String sqlFingerprint;
    private final List<AccelerationSuggestionType> selectedSuggestionTypes;
    private final String planSummary;
    private final String primaryRecommendation;
    private final String planPayloadJson;
    private final List<OptimizationBenefitVO> benefits;
    private final List<OptimizationCostVO> costs;
    private final List<OptimizationRiskVO> risks;
    private final String reviewNote;
    private final String approvedBy;
    private final Instant approvedAt;
    private final String rejectedBy;
    private final Instant rejectedAt;
    private final Integer lastErrorCode;
    private final String lastErrorMessage;
    private final String runtimeBindingJson;
    private final Instant runtimeBindingAt;
    private final String runtimeBindingBy;
    private final String verificationEvidenceJson;
    private final Instant verifiedAt;
    private final String verifiedBy;
    private final String rollbackEvidenceJson;
    private final Instant rolledBackAt;
    private final String rolledBackBy;
    private final String configSnapshotId;
    private final String resultId;
    private final String historyId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<AccelerationPlanStatusHistoryVO> statusHistory;
    private final String contractStage;
    private final String implementationStage;

    public AccelerationPlanStatusResponse(String planId,
                                          String sourceTaskId,
                                          AccelerationPlanStatus status,
                                          DataSourceTypeEnum datasourceType,
                                          String sqlFingerprint,
                                          List<AccelerationSuggestionType> selectedSuggestionTypes,
                                          String planSummary,
                                          String primaryRecommendation,
                                          String planPayloadJson,
                                          List<OptimizationBenefitVO> benefits,
                                          List<OptimizationCostVO> costs,
                                          List<OptimizationRiskVO> risks,
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
                                          String configSnapshotId,
                                          String resultId,
                                          String historyId,
                                          Instant createdAt,
                                          Instant updatedAt,
                                          List<AccelerationPlanStatusHistoryVO> statusHistory,
                                          String contractStage,
                                          String implementationStage) {
        this.planId = planId;
        this.sourceTaskId = sourceTaskId;
        this.status = status;
        this.datasourceType = datasourceType;
        this.sqlFingerprint = sqlFingerprint;
        this.selectedSuggestionTypes = selectedSuggestionTypes;
        this.planSummary = planSummary;
        this.primaryRecommendation = primaryRecommendation;
        this.planPayloadJson = planPayloadJson;
        this.benefits = benefits;
        this.costs = costs;
        this.risks = risks;
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
        this.configSnapshotId = configSnapshotId;
        this.resultId = resultId;
        this.historyId = historyId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.statusHistory = statusHistory;
        this.contractStage = contractStage;
        this.implementationStage = implementationStage;
    }

    public String getPlanId() {
        return planId;
    }

    public String getSourceTaskId() {
        return sourceTaskId;
    }

    public AccelerationPlanStatus getStatus() {
        return status;
    }

    public DataSourceTypeEnum getDatasourceType() {
        return datasourceType;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
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

    public List<OptimizationBenefitVO> getBenefits() {
        return benefits;
    }

    public List<OptimizationCostVO> getCosts() {
        return costs;
    }

    public List<OptimizationRiskVO> getRisks() {
        return risks;
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

    public String getConfigSnapshotId() {
        return configSnapshotId;
    }

    public String getResultId() {
        return resultId;
    }

    public String getHistoryId() {
        return historyId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<AccelerationPlanStatusHistoryVO> getStatusHistory() {
        return statusHistory;
    }

    public String getContractStage() {
        return contractStage;
    }

    public String getImplementationStage() {
        return implementationStage;
    }
}
