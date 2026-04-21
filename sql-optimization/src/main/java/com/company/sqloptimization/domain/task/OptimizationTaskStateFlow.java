package com.company.sqloptimization.domain.task;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized transition guard for optimization task statuses and task-type-specific phases.
 */
public final class OptimizationTaskStateFlow {

    private static final Map<OptimizationTaskType, List<OptimizationTaskPhase>> PHASE_FLOW =
        new EnumMap<OptimizationTaskType, List<OptimizationTaskPhase>>(OptimizationTaskType.class);

    static {
        PHASE_FLOW.put(
            OptimizationTaskType.PARSE,
            Collections.unmodifiableList(
                Arrays.asList(
                    OptimizationTaskPhase.SUBMITTED,
                    OptimizationTaskPhase.DEEP_PARSING,
                    OptimizationTaskPhase.RESULT_ASSEMBLING,
                    OptimizationTaskPhase.FINISHED
                )
            )
        );
        PHASE_FLOW.put(
            OptimizationTaskType.REWRITE,
            Collections.unmodifiableList(
                Arrays.asList(
                    OptimizationTaskPhase.SUBMITTED,
                    OptimizationTaskPhase.DEEP_PARSING,
                    OptimizationTaskPhase.SQL_REWRITING,
                    OptimizationTaskPhase.RESULT_ASSEMBLING,
                    OptimizationTaskPhase.FINISHED
                )
            )
        );
        PHASE_FLOW.put(
            OptimizationTaskType.ACCELERATION_SUGGESTION,
            Collections.unmodifiableList(
                Arrays.asList(
                    OptimizationTaskPhase.SUBMITTED,
                    OptimizationTaskPhase.DEEP_PARSING,
                    OptimizationTaskPhase.COST_ESTIMATING,
                    OptimizationTaskPhase.ACCELERATION_PLANNING,
                    OptimizationTaskPhase.RESULT_ASSEMBLING,
                    OptimizationTaskPhase.FINISHED
                )
            )
        );
    }

    private OptimizationTaskStateFlow() {
    }

    public static List<OptimizationTaskPhase> supportedPhaseFlow(OptimizationTaskType taskType) {
        return PHASE_FLOW.get(taskType);
    }

    public static OptimizationTaskPhase firstRunningPhase(OptimizationTaskType taskType) {
        return supportedPhaseFlow(taskType).get(1);
    }

    public static void validateStart(OptimizationTaskType taskType,
                                     OptimizationTaskStatus currentStatus,
                                     OptimizationTaskPhase currentPhase) {
        if (currentStatus != OptimizationTaskStatus.QUEUED || currentPhase != OptimizationTaskPhase.SUBMITTED) {
            throw invalidTransition(
                "Optimization task can only start from QUEUED/SUBMITTED. taskType=" + taskType
                    + ", status=" + currentStatus
                    + ", phase=" + currentPhase
            );
        }
    }

    public static void validatePhaseAdvance(OptimizationTaskType taskType,
                                            OptimizationTaskStatus currentStatus,
                                            OptimizationTaskPhase currentPhase,
                                            OptimizationTaskPhase nextPhase) {
        if (currentStatus != OptimizationTaskStatus.RUNNING) {
            throw invalidTransition(
                "Optimization task can only advance phases while RUNNING. taskType=" + taskType
                    + ", status=" + currentStatus
                    + ", phase=" + currentPhase
            );
        }
        List<OptimizationTaskPhase> phaseFlow = supportedPhaseFlow(taskType);
        int currentIndex = phaseFlow.indexOf(currentPhase);
        int nextIndex = phaseFlow.indexOf(nextPhase);
        if (currentIndex < 0 || nextIndex != currentIndex + 1 || nextPhase == OptimizationTaskPhase.FINISHED) {
            throw invalidTransition(
                "Unsupported phase transition for taskType=" + taskType
                    + ": " + currentPhase + " -> " + nextPhase
            );
        }
    }

    public static void validateCompletion(OptimizationTaskType taskType,
                                          OptimizationTaskStatus currentStatus,
                                          OptimizationTaskPhase currentPhase) {
        if (currentStatus != OptimizationTaskStatus.RUNNING) {
            throw invalidTransition(
                "Optimization task can only complete while RUNNING. taskType=" + taskType
                    + ", status=" + currentStatus
                    + ", phase=" + currentPhase
            );
        }
        List<OptimizationTaskPhase> phaseFlow = supportedPhaseFlow(taskType);
        if (phaseFlow.get(phaseFlow.size() - 2) != currentPhase) {
            throw invalidTransition(
                "Optimization task can only finish from the final processing phase. taskType=" + taskType
                    + ", phase=" + currentPhase
            );
        }
    }

    public static void validateTermination(OptimizationTaskStatus currentStatus) {
        if (currentStatus.isTerminal()) {
            throw invalidTransition("Optimization task is already terminal. status=" + currentStatus);
        }
    }

    private static IllegalStateException invalidTransition(String message) {
        return new IllegalStateException(
            ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_STATE_TRANSITION_INVALID + ": " + message
        );
    }
}
