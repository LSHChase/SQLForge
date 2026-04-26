package com.company.sqloptimization.application.service;

import com.company.sqloptimization.application.controller.vo.AccelerationPlanStatusHistoryVO;
import com.company.sqloptimization.application.controller.vo.AccelerationPlanStatusResponse;
import com.company.sqloptimization.application.controller.vo.AccelerationPlanSubmitResponse;
import com.company.sqloptimization.application.controller.vo.OptimizationBenefitVO;
import com.company.sqloptimization.application.controller.vo.OptimizationCostVO;
import com.company.sqloptimization.application.controller.vo.OptimizationRiskVO;
import com.company.sqloptimization.domain.plan.AccelerationPlan;
import com.company.sqloptimization.domain.plan.AccelerationPlanStatusTransition;
import com.company.sqloptimization.domain.task.OptimizationTaskBenefit;
import com.company.sqloptimization.domain.task.OptimizationTaskCost;
import com.company.sqloptimization.domain.task.OptimizationTaskRisk;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AccelerationPlanModelApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "ACCELERATION_PLAN_GOVERNANCE_BASELINE";
    private static final String STATUS_QUERY_PATH_TEMPLATE = "/api/sql-optimization/acceleration-plans/%s";

    public AccelerationPlanSubmitResponse buildSubmitResponse(AccelerationPlan accelerationPlan) {
        return new AccelerationPlanSubmitResponse(
            accelerationPlan.getPlanId(),
            accelerationPlan.getStatus(),
            String.format(STATUS_QUERY_PATH_TEMPLATE, accelerationPlan.getPlanId()),
            CONTRACT_STAGE,
            IMPLEMENTATION_STAGE
        );
    }

    public AccelerationPlanStatusResponse buildStatusResponse(AccelerationPlan accelerationPlan) {
        return new AccelerationPlanStatusResponse(
            accelerationPlan.getPlanId(),
            accelerationPlan.getSourceTaskId(),
            accelerationPlan.getStatus(),
            accelerationPlan.getDatasourceType(),
            accelerationPlan.getSqlFingerprint(),
            accelerationPlan.getSelectedSuggestionTypes(),
            accelerationPlan.getPlanSummary(),
            accelerationPlan.getPrimaryRecommendation(),
            accelerationPlan.getPlanPayloadJson(),
            toBenefitVOs(accelerationPlan.getBenefits()),
            toCostVOs(accelerationPlan.getCosts()),
            toRiskVOs(accelerationPlan.getRisks()),
            accelerationPlan.getReviewNote(),
            accelerationPlan.getApprovedBy(),
            accelerationPlan.getApprovedAt(),
            accelerationPlan.getRejectedBy(),
            accelerationPlan.getRejectedAt(),
            accelerationPlan.getLastErrorCode(),
            accelerationPlan.getLastErrorMessage(),
            accelerationPlan.getRuntimeBindingJson(),
            accelerationPlan.getRuntimeBindingAt(),
            accelerationPlan.getRuntimeBindingBy(),
            accelerationPlan.getVerificationEvidenceJson(),
            accelerationPlan.getVerifiedAt(),
            accelerationPlan.getVerifiedBy(),
            accelerationPlan.getRollbackEvidenceJson(),
            accelerationPlan.getRolledBackAt(),
            accelerationPlan.getRolledBackBy(),
            accelerationPlan.getConfigSnapshotId(),
            accelerationPlan.getResultId(),
            accelerationPlan.getHistoryId(),
            accelerationPlan.getCreatedAt(),
            accelerationPlan.getUpdatedAt(),
            toHistoryVOs(accelerationPlan.getStatusHistory()),
            CONTRACT_STAGE,
            IMPLEMENTATION_STAGE
        );
    }

    private List<AccelerationPlanStatusHistoryVO> toHistoryVOs(List<AccelerationPlanStatusTransition> history) {
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }
        List<AccelerationPlanStatusHistoryVO> items = new ArrayList<AccelerationPlanStatusHistoryVO>(history.size());
        for (AccelerationPlanStatusTransition item : history) {
            items.add(new AccelerationPlanStatusHistoryVO(
                item.getPreviousStatus(),
                item.getCurrentStatus(),
                item.getOccurredAt(),
                item.getNote()
            ));
        }
        return Collections.unmodifiableList(items);
    }

    private List<OptimizationBenefitVO> toBenefitVOs(List<OptimizationTaskBenefit> benefits) {
        if (benefits == null || benefits.isEmpty()) {
            return Collections.emptyList();
        }
        List<OptimizationBenefitVO> items = new ArrayList<OptimizationBenefitVO>(benefits.size());
        for (OptimizationTaskBenefit benefit : benefits) {
            items.add(new OptimizationBenefitVO(
                benefit.getCategory(),
                benefit.getEstimatedImprovementPercent(),
                benefit.getSummary()
            ));
        }
        return Collections.unmodifiableList(items);
    }

    private List<OptimizationCostVO> toCostVOs(List<OptimizationTaskCost> costs) {
        if (costs == null || costs.isEmpty()) {
            return Collections.emptyList();
        }
        List<OptimizationCostVO> items = new ArrayList<OptimizationCostVO>(costs.size());
        for (OptimizationTaskCost cost : costs) {
            items.add(new OptimizationCostVO(cost.getCategory(), cost.getLevel(), cost.getSummary()));
        }
        return Collections.unmodifiableList(items);
    }

    private List<OptimizationRiskVO> toRiskVOs(List<OptimizationTaskRisk> risks) {
        if (risks == null || risks.isEmpty()) {
            return Collections.emptyList();
        }
        List<OptimizationRiskVO> items = new ArrayList<OptimizationRiskVO>(risks.size());
        for (OptimizationTaskRisk risk : risks) {
            items.add(new OptimizationRiskVO(
                risk.getLevel(),
                risk.getCategory(),
                risk.getSummary(),
                risk.getMitigation()
            ));
        }
        return Collections.unmodifiableList(items);
    }
}
