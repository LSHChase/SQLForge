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
import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import com.company.sqloptimization.domain.task.OptimizationTask;
import com.company.sqloptimization.domain.task.OptimizationTaskError;
import com.company.sqloptimization.domain.task.OptimizationTaskStatus;
import com.company.sqloptimization.domain.task.OptimizationTaskStatusTransition;
import com.company.sqloptimization.domain.task.OptimizationTaskSubmission;
import com.company.sqloptimization.domain.task.OptimizationTaskType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Builds canonical domain and contract objects for async optimization tasks.
 */
@Service
public class OptimizationTaskModelApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "DATABASE_SCHEDULED_WORKER_BASELINE";
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
        if (task.getStatus() != OptimizationTaskStatus.SUCCEEDED) {
            return null;
        }
        if (task.getTaskType() == OptimizationTaskType.PARSE) {
            return new OptimizationSuggestionVO(
                task.getSummary(),
                "Use the deep-parse output to confirm source tables, expression hotspots, and rewrite readiness.",
                Integer.valueOf(78),
                Arrays.asList(
                    new OptimizationArtifactVO("AST_SUMMARY", "logicalPlan", "Projected columns, joins, and filters were normalized."),
                    new OptimizationArtifactVO("LINEAGE_HINT", "tableCoverage", "orders -> customer_profile dependency path detected.")
                ),
                Arrays.asList(
                    new OptimizationBenefitVO("ANALYSIS_CONFIDENCE", Integer.valueOf(65), "Deep parse improves downstream rewrite and acceleration accuracy."),
                    new OptimizationBenefitVO("RISK_VISIBILITY", Integer.valueOf(40), "Potential full-scan and wide-join hotspots were surfaced early.")
                ),
                Collections.singletonList(
                    new OptimizationCostVO("CPU_TIME", "LOW", "Deep parsing adds isolated offline CPU time but does not block online query execution.")
                ),
                Collections.singletonList(
                    new OptimizationRiskVO(
                        "LOW",
                        "PARSER_ABSTRACTION",
                        "Cross-dialect edge syntax may still require later parser adaptation.",
                        "Fallback to parser-specific diagnostics before applying automated rewrites."
                    )
                )
            );
        }
        if (task.getTaskType() == OptimizationTaskType.REWRITE) {
            return new OptimizationSuggestionVO(
                task.getSummary(),
                "Prefer the rewritten statement after validating semantics on a representative sample dataset.",
                Integer.valueOf(84),
                Arrays.asList(
                    new OptimizationArtifactVO("REWRITTEN_SQL", "candidateSql", "Replace SELECT * with projected columns and push filters before join."),
                    new OptimizationArtifactVO("RULE_TRACE", "appliedRules", "COLUMN_PRUNING, FILTER_PUSHDOWN, JOIN_ORDER_HINT")
                ),
                Arrays.asList(
                    new OptimizationBenefitVO("LATENCY", Integer.valueOf(32), "Expected end-to-end latency reduction from narrower scan and earlier filter execution."),
                    new OptimizationBenefitVO("SCANNED_ROWS", Integer.valueOf(41), "Projected scan volume decreases once unused columns are removed.")
                ),
                Arrays.asList(
                    new OptimizationCostVO("VALIDATION", "MEDIUM", "Requires semantic diff verification against the original statement."),
                    new OptimizationCostVO("MAINTENANCE", "LOW", "Rule trace should be preserved for future regression comparison.")
                ),
                Collections.singletonList(
                    new OptimizationRiskVO(
                        "MEDIUM",
                        "SEMANTIC_DRIFT",
                        "Predicate pushdown can change null-handling behavior on some engines if the original SQL relied on implicit evaluation order.",
                        "Run result-set diff checks before approval."
                    )
                )
            );
        }
        return new OptimizationSuggestionVO(
            task.getSummary(),
            "Prioritize partition and precompute options, then evaluate whether materialized-view management is justified.",
            Integer.valueOf(81),
            Arrays.asList(
                new OptimizationArtifactVO("ACCELERATION_PLAN", "recommendedTypes", joinSuggestionTypes(task.getRequestedSuggestionTypes())),
                new OptimizationArtifactVO("MATERIALIZED_VIEW", "refreshStrategy", "Hourly refresh with partition pruning on order_date."),
                new OptimizationArtifactVO("PARTITION_STRATEGY", "partitionKey", "Use order_date by day and archive partitions older than 180 days.")
            ),
            Arrays.asList(
                new OptimizationBenefitVO("LATENCY", Integer.valueOf(55), "Precomputation and partition pruning reduce repeated heavy scans."),
                new OptimizationBenefitVO("SCANNED_ROWS", Integer.valueOf(68), "Recommended acceleration plan narrows active partition reads.")
            ),
            Arrays.asList(
                new OptimizationCostVO("STORAGE", "HIGH", "Materialized-view footprint increases because of duplicated hot aggregates."),
                new OptimizationCostVO("REFRESH_LATENCY", "MEDIUM", "Scheduled refresh introduces bounded staleness for derived data.")
            ),
            Arrays.asList(
                new OptimizationRiskVO(
                    "MEDIUM",
                    "DATA_FRESHNESS",
                    "Refresh cadence can temporarily expose stale aggregates to downstream consumers.",
                    "Require approval with explicit freshness SLA before activation."
                ),
                new OptimizationRiskVO(
                    "LOW",
                    "ROLLBACK_COMPLEXITY",
                    "Acceleration plans need coordinated disable/cleanup when benefit falls below threshold.",
                    "Keep approval, activation, and revoke state transitions auditable."
                )
            )
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
            task.getCurrentPhase().name(),
            Collections.singletonList(
                new OptimizationRiskVO(
                    error.isRetryable() ? "MEDIUM" : "HIGH",
                    "PIPELINE_READINESS",
                    "Suggestion output is unavailable because the database-backed worker could not finish the task.",
                    "Inspect the failed phase and retry only after the task carrier is healthy."
                )
            )
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

    private String joinSuggestionTypes(List<AccelerationSuggestionType> suggestionTypes) {
        if (suggestionTypes == null || suggestionTypes.isEmpty()) {
            return AccelerationSuggestionType.ALL.name();
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < suggestionTypes.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(suggestionTypes.get(index).name());
        }
        return builder.toString();
    }

    private String buildStatusQueryPath(String taskId) {
        return String.format(STATUS_QUERY_PATH_TEMPLATE, taskId);
    }
}
