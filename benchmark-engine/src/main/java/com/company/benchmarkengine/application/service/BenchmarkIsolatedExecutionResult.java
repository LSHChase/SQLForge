package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.domain.benchmark.BenchmarkEngineProfile;
import com.company.benchmarkengine.domain.benchmark.BenchmarkExecutionSummary;
import com.company.benchmarkengine.domain.benchmark.BenchmarkRecommendation;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdAssessment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BenchmarkIsolatedExecutionResult {

    private final BenchmarkExecutionSummary executionSummary;
    private final List<BenchmarkEngineProfile> engineProfiles;
    private final List<BenchmarkThresholdAssessment> thresholdAssessments;
    private final List<BenchmarkRecommendation> recommendations;

    public BenchmarkIsolatedExecutionResult(BenchmarkExecutionSummary executionSummary,
                                            List<BenchmarkEngineProfile> engineProfiles,
                                            List<BenchmarkThresholdAssessment> thresholdAssessments,
                                            List<BenchmarkRecommendation> recommendations) {
        this.executionSummary = executionSummary;
        this.engineProfiles = immutableCopy(engineProfiles);
        this.thresholdAssessments = immutableCopy(thresholdAssessments);
        this.recommendations = immutableCopy(recommendations);
    }

    private <T> List<T> immutableCopy(List<T> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<T>(items));
    }

    public BenchmarkExecutionSummary getExecutionSummary() {
        return executionSummary;
    }

    public List<BenchmarkEngineProfile> getEngineProfiles() {
        return engineProfiles;
    }

    public List<BenchmarkThresholdAssessment> getThresholdAssessments() {
        return thresholdAssessments;
    }

    public List<BenchmarkRecommendation> getRecommendations() {
        return recommendations;
    }
}
