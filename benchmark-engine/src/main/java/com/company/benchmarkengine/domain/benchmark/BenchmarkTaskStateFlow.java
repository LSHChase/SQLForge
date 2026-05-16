package com.company.benchmarkengine.domain.benchmark;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class BenchmarkTaskStateFlow {

    private static final Map<BenchmarkTaskType, List<BenchmarkTaskPhase>> PHASE_FLOW =
        new EnumMap<BenchmarkTaskType, List<BenchmarkTaskPhase>>(BenchmarkTaskType.class);

    static {
        PHASE_FLOW.put(
            BenchmarkTaskType.BASELINE,
            Collections.unmodifiableList(
                Arrays.asList(
                    BenchmarkTaskPhase.SUBMITTED,
                    BenchmarkTaskPhase.BASELINE_PREPARING,
                    BenchmarkTaskPhase.WARMING_UP,
                    BenchmarkTaskPhase.EXECUTING,
                    BenchmarkTaskPhase.THRESHOLD_EVALUATING,
                    BenchmarkTaskPhase.REPORTING,
                    BenchmarkTaskPhase.FINISHED
                )
            )
        );
        PHASE_FLOW.put(
            BenchmarkTaskType.COMPARISON,
            Collections.unmodifiableList(
                Arrays.asList(
                    BenchmarkTaskPhase.SUBMITTED,
                    BenchmarkTaskPhase.BASELINE_PREPARING,
                    BenchmarkTaskPhase.SHADOW_VALIDATING,
                    BenchmarkTaskPhase.WARMING_UP,
                    BenchmarkTaskPhase.EXECUTING,
                    BenchmarkTaskPhase.THRESHOLD_EVALUATING,
                    BenchmarkTaskPhase.REPORTING,
                    BenchmarkTaskPhase.FINISHED
                )
            )
        );
        PHASE_FLOW.put(
            BenchmarkTaskType.REGRESSION_GUARD,
            Collections.unmodifiableList(
                Arrays.asList(
                    BenchmarkTaskPhase.SUBMITTED,
                    BenchmarkTaskPhase.SHADOW_VALIDATING,
                    BenchmarkTaskPhase.EXECUTING,
                    BenchmarkTaskPhase.THRESHOLD_EVALUATING,
                    BenchmarkTaskPhase.REPORTING,
                    BenchmarkTaskPhase.FINISHED
                )
            )
        );
    }

    private BenchmarkTaskStateFlow() {
    }

    public static List<BenchmarkTaskPhase> supportedPhaseFlow(BenchmarkTaskType taskType) {
        return PHASE_FLOW.get(taskType);
    }

    public static BenchmarkTaskPhase firstRunningPhase(BenchmarkTaskType taskType) {
        return supportedPhaseFlow(taskType).get(1);
    }

    public static void validateStart(BenchmarkTaskType taskType,
                                     BenchmarkTaskStatus currentStatus,
                                     BenchmarkTaskPhase currentPhase) {
        if (currentStatus != BenchmarkTaskStatus.QUEUED || currentPhase != BenchmarkTaskPhase.SUBMITTED) {
            throw invalidTransition(
                "压测任务只能从 QUEUED/SUBMITTED 状态启动。taskType=" + taskType
                    + "，状态=" + currentStatus
                    + ", phase=" + currentPhase
            );
        }
    }

    public static void validatePhaseAdvance(BenchmarkTaskType taskType,
                                            BenchmarkTaskStatus currentStatus,
                                            BenchmarkTaskPhase currentPhase,
                                            BenchmarkTaskPhase nextPhase) {
        if (currentStatus != BenchmarkTaskStatus.RUNNING) {
            throw invalidTransition(
                "压测任务只能在 RUNNING 状态推进阶段。taskType=" + taskType
                    + "，状态=" + currentStatus
                    + ", phase=" + currentPhase
            );
        }
        List<BenchmarkTaskPhase> phaseFlow = supportedPhaseFlow(taskType);
        int currentIndex = phaseFlow.indexOf(currentPhase);
        int nextIndex = phaseFlow.indexOf(nextPhase);
        if (currentIndex < 0 || nextIndex != currentIndex + 1 || nextPhase == BenchmarkTaskPhase.FINISHED) {
            throw invalidTransition(
                "不支持该 taskType 的压测阶段流转：" + taskType
                    + ": " + currentPhase + " -> " + nextPhase
            );
        }
    }

    public static void validateCompletion(BenchmarkTaskType taskType,
                                          BenchmarkTaskStatus currentStatus,
                                          BenchmarkTaskPhase currentPhase) {
        if (currentStatus != BenchmarkTaskStatus.RUNNING) {
            throw invalidTransition(
                "压测任务只能在 RUNNING 状态完成。taskType=" + taskType
                    + "，状态=" + currentStatus
                    + ", phase=" + currentPhase
            );
        }
        List<BenchmarkTaskPhase> phaseFlow = supportedPhaseFlow(taskType);
        if (phaseFlow.get(phaseFlow.size() - 2) != currentPhase) {
            throw invalidTransition(
                "压测任务只能从报告阶段结束。taskType=" + taskType
                    + ", phase=" + currentPhase
            );
        }
    }

    public static void validateTermination(BenchmarkTaskStatus currentStatus) {
        if (currentStatus.isTerminal()) {
            throw invalidTransition("压测任务已是终态。status=" + currentStatus);
        }
    }

    private static IllegalStateException invalidTransition(String message) {
        return new IllegalStateException(
            ErrorCodeConstants.BENCHMARK_ENGINE_SYSTEM_STATE_TRANSITION_INVALID + ": " + message
        );
    }
}
