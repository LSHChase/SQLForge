package com.company.benchmarkengine.domain.benchmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BenchmarkExecutionSummary {

    private final String executionMode;
    private final String isolationSummary;
    private final Integer sampleCount;
    private final Long executionDurationMs;
    private final String workloadDigest;
    private final BenchmarkScaleReadinessAssessment scaleReadiness;
    private final List<String> phaseNotes;

    public BenchmarkExecutionSummary(String executionMode,
                                     String isolationSummary,
                                     Integer sampleCount,
                                     Long executionDurationMs,
                                     String workloadDigest,
                                     List<String> phaseNotes) {
        this(executionMode, isolationSummary, sampleCount, executionDurationMs, workloadDigest, null, phaseNotes);
    }

    public BenchmarkExecutionSummary(String executionMode,
                                     String isolationSummary,
                                     Integer sampleCount,
                                     Long executionDurationMs,
                                     String workloadDigest,
                                     BenchmarkScaleReadinessAssessment scaleReadiness,
                                     List<String> phaseNotes) {
        this.executionMode = executionMode;
        this.isolationSummary = isolationSummary;
        this.sampleCount = sampleCount;
        this.executionDurationMs = executionDurationMs;
        this.workloadDigest = workloadDigest;
        this.scaleReadiness = scaleReadiness;
        this.phaseNotes = immutableCopy(phaseNotes);
    }

    private <T> List<T> immutableCopy(List<T> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<T>(items));
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public String getIsolationSummary() {
        return isolationSummary;
    }

    public Integer getSampleCount() {
        return sampleCount;
    }

    public Long getExecutionDurationMs() {
        return executionDurationMs;
    }

    public String getWorkloadDigest() {
        return workloadDigest;
    }

    public BenchmarkScaleReadinessAssessment getScaleReadiness() {
        return scaleReadiness;
    }

    public List<String> getPhaseNotes() {
        return phaseNotes;
    }
}
