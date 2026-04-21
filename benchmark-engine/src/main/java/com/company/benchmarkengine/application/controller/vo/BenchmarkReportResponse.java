package com.company.benchmarkengine.application.controller.vo;

import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdVerdict;
import java.time.Instant;
import java.util.List;

public class BenchmarkReportResponse {

    private final String reportId;
    private final String taskId;
    private final BenchmarkTaskType taskType;
    private final String sqlFingerprint;
    private final BenchmarkThresholdVerdict verdict;
    private final Instant generatedAt;
    private final List<BenchmarkEngineMetricVO> engineResults;
    private final List<BenchmarkThresholdAssessmentVO> thresholdAssessments;
    private final List<BenchmarkRecommendationVO> recommendations;
    private final String contractStage;
    private final String implementationStage;

    public BenchmarkReportResponse(String reportId,
                                   String taskId,
                                   BenchmarkTaskType taskType,
                                   String sqlFingerprint,
                                   BenchmarkThresholdVerdict verdict,
                                   Instant generatedAt,
                                   List<BenchmarkEngineMetricVO> engineResults,
                                   List<BenchmarkThresholdAssessmentVO> thresholdAssessments,
                                   List<BenchmarkRecommendationVO> recommendations,
                                   String contractStage,
                                   String implementationStage) {
        this.reportId = reportId;
        this.taskId = taskId;
        this.taskType = taskType;
        this.sqlFingerprint = sqlFingerprint;
        this.verdict = verdict;
        this.generatedAt = generatedAt;
        this.engineResults = engineResults;
        this.thresholdAssessments = thresholdAssessments;
        this.recommendations = recommendations;
        this.contractStage = contractStage;
        this.implementationStage = implementationStage;
    }

    public String getReportId() {
        return reportId;
    }

    public String getTaskId() {
        return taskId;
    }

    public BenchmarkTaskType getTaskType() {
        return taskType;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public BenchmarkThresholdVerdict getVerdict() {
        return verdict;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public List<BenchmarkEngineMetricVO> getEngineResults() {
        return engineResults;
    }

    public List<BenchmarkThresholdAssessmentVO> getThresholdAssessments() {
        return thresholdAssessments;
    }

    public List<BenchmarkRecommendationVO> getRecommendations() {
        return recommendations;
    }

    public String getContractStage() {
        return contractStage;
    }

    public String getImplementationStage() {
        return implementationStage;
    }
}
