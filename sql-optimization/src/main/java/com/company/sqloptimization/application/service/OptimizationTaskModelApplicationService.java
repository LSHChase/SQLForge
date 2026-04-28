package com.company.sqloptimization.application.service;

import com.company.sqloptimization.application.controller.dto.OptimizationTaskContextDTO;
import com.company.sqloptimization.application.controller.vo.OptimizationArtifactVO;
import com.company.sqloptimization.application.controller.vo.OptimizationBenefitVO;
import com.company.sqloptimization.application.controller.vo.OptimizationCostVO;
import com.company.sqloptimization.application.controller.vo.OptimizationFailureVO;
import com.company.sqloptimization.application.controller.vo.OptimizationRiskVO;
import com.company.sqloptimization.application.controller.vo.OptimizationSuggestionVO;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskSubmitRequest;
import com.company.sqloptimization.application.controller.vo.OptimizationTaskStatusHistoryVO;
import com.company.sqloptimization.application.controller.vo.OptimizationTaskStatusResponse;
import com.company.sqloptimization.application.controller.vo.OptimizationTaskSubmitResponse;
import com.company.sqloptimization.domain.task.OptimizationTask;
import com.company.sqloptimization.domain.task.OptimizationTaskArtifact;
import com.company.sqloptimization.domain.task.OptimizationTaskBenefit;
import com.company.sqloptimization.domain.task.OptimizationTaskCost;
import com.company.sqloptimization.domain.task.OptimizationTaskError;
import com.company.sqloptimization.domain.task.OptimizationTaskRisk;
import com.company.sqloptimization.domain.task.OptimizationTaskSuggestion;
import com.company.sqloptimization.domain.task.OptimizationTaskStatus;
import com.company.sqloptimization.domain.task.OptimizationTaskStatusTransition;
import com.company.sqloptimization.domain.task.OptimizationTaskSubmission;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Builds canonical domain and contract objects for async optimization tasks.
 */
@Service
public class OptimizationTaskModelApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "ACCELERATION_PLAN_GOVERNANCE_BASELINE";
    private static final String STATUS_QUERY_PATH_TEMPLATE = "/api/sql-optimization/tasks/%s";

    public OptimizationTask createQueuedTask(OptimizationTaskSubmitRequest request, String taskId, Instant submittedAt) {
        OptimizationTaskContextDTO taskContext = request.getTaskContext() == null
            ? new OptimizationTaskContextDTO()
            : request.getTaskContext();
        OptimizationTaskSubmission submission = new OptimizationTaskSubmission(
            request.getTenantId(),
            request.getTaskType(),
            request.getSqlText(),
            request.getSqlFingerprint(),
            request.getDatasourceType(),
            taskContext.getPriority(),
            taskContext.getParseDepth(),
            taskContext.getCallbackUrl(),
            taskContext.getRequestedSuggestionTypes()
        );
        return OptimizationTask.submit(taskId, submission, submittedAt);
    }

    public OptimizationTaskSubmitResponse buildSubmitResponse(OptimizationTask task, Instant estimatedReadyAt) {
        return new OptimizationTaskSubmitResponse(
            task.getTaskId(),
            task.getStatus(),
            task.getCurrentPhase(),
            estimatedReadyAt,
            buildStatusQueryPath(task.getTaskId()),
            CONTRACT_STAGE,
            IMPLEMENTATION_STAGE
        );
    }

    public OptimizationTaskStatusResponse buildStatusResponse(OptimizationTask task) {
        return new OptimizationTaskStatusResponse(
            task.getTaskId(),
            task.getTaskType(),
            task.getStatus(),
            task.getCurrentPhase(),
            task.getPriority(),
            task.getProgressPercent(),
            task.getRequestedSuggestionTypes(),
            toSuggestionVO(task),
            toFailureVO(task),
            task.getSubmittedAt(),
            task.getStartedAt(),
            task.getFinishedAt(),
            toHistoryVO(task.getStatusHistory()),
            CONTRACT_STAGE,
            IMPLEMENTATION_STAGE
        );
    }

    private OptimizationSuggestionVO toSuggestionVO(OptimizationTask task) {
        if (task.getStatus() != OptimizationTaskStatus.SUCCEEDED || task.getSuggestion() == null) {
            return null;
        }
        OptimizationTaskSuggestion suggestion = task.getSuggestion();
        return new OptimizationSuggestionVO(
            suggestion.getSummary(),
            suggestion.getPrimaryRecommendation(),
            suggestion.getConfidenceScore(),
            toArtifactVOs(suggestion.getArtifacts()),
            toBenefitVOs(suggestion.getBenefits()),
            toCostVOs(suggestion.getCosts()),
            toRiskVOs(suggestion.getRisks())
        );
    }

    private OptimizationFailureVO toFailureVO(OptimizationTask task) {
        if (task.getError() == null && task.getStatus() != OptimizationTaskStatus.FAILED) {
            return null;
        }
        OptimizationTaskError error = task.getError();
        if (error == null) {
            return new OptimizationFailureVO(
                0,
                "Optimization task was cancelled before suggestion output became available.",
                "Resubmit the task when the optimization window is available.",
                false,
                task.getCurrentPhase().name(),
                Collections.singletonList(
                    new OptimizationRiskVO(
                        "LOW",
                        "TASK_CANCELLED",
                        "No suggestion payload was produced because the task stopped before completion.",
                        "Requeue only after the caller confirms cancellation intent."
                    )
                )
            );
        }
        return new OptimizationFailureVO(
            error.getCode(),
            error.getMessage(),
            error.getSuggestedAction(),
            error.isRetryable(),
            error.getFailedPhase() == null ? task.getCurrentPhase().name() : error.getFailedPhase().name(),
            toRiskVOs(error.getRisks())
        );
    }

    private List<OptimizationTaskStatusHistoryVO> toHistoryVO(List<OptimizationTaskStatusTransition> history) {
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }
        List<OptimizationTaskStatusHistoryVO> items = new ArrayList<OptimizationTaskStatusHistoryVO>(history.size());
        for (OptimizationTaskStatusTransition item : history) {
            items.add(
                new OptimizationTaskStatusHistoryVO(
                    item.getPreviousStatus(),
                    item.getCurrentStatus(),
                    item.getPreviousPhase(),
                    item.getCurrentPhase(),
                    item.getOccurredAt(),
                    item.getNote()
                )
            );
        }
        return Collections.unmodifiableList(items);
    }

    private List<OptimizationArtifactVO> toArtifactVOs(List<OptimizationTaskArtifact> artifacts) {
        if (artifacts == null || artifacts.isEmpty()) {
            return Collections.emptyList();
        }
        List<OptimizationArtifactVO> items = new ArrayList<OptimizationArtifactVO>(artifacts.size());
        for (OptimizationTaskArtifact artifact : artifacts) {
            items.add(new OptimizationArtifactVO(artifact.getCategory(), artifact.getName(), artifact.getContent()));
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

    private String buildStatusQueryPath(String taskId) {
        return String.format(STATUS_QUERY_PATH_TEMPLATE, taskId);
    }
}
