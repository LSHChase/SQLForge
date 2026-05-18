package com.company.benchmarkengine.application.controller.vo;

import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkScaleReadinessAssessment;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdVerdict;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.time.Instant;
import java.util.List;

public class BenchmarkReportResponse {

    private final String reportId;
    private final String taskId;
    private final BenchmarkTaskType taskType;
    private final String sqlFingerprint;
    private final BenchmarkThresholdVerdict verdict;
    private final Instant generatedAt;
    private final List<DataSourceTypeEnum> targetEngines;
    private final List<BenchmarkEngineMetricVO> engineResults;
    private final List<BenchmarkThresholdAssessmentVO> thresholdAssessments;
    private final BenchmarkScaleReadinessAssessment scaleReadiness;
    private final BenchmarkRegressionSummaryVO regressionSummary;
    private final List<BenchmarkAlertLinkageVO> alertLinkages;
    private final List<BenchmarkTrendChartVO> trendCharts;
    private final List<BenchmarkRecommendationVO> recommendations;
    private final String requestedFormat;
    private final List<String> availableFormats;
    private final String reportQueryPath;
    private final String rawDataDownloadPath;
    private final String contractStage;
    private final String implementationStage;

    public BenchmarkReportResponse(String reportId,
                                   String taskId,
                                   BenchmarkTaskType taskType,
                                   String sqlFingerprint,
                                   BenchmarkThresholdVerdict verdict,
                                   Instant generatedAt,
                                   List<DataSourceTypeEnum> targetEngines,
                                   List<BenchmarkEngineMetricVO> engineResults,
                                   List<BenchmarkThresholdAssessmentVO> thresholdAssessments,
                                   List<BenchmarkTrendChartVO> trendCharts,
                                   List<BenchmarkRecommendationVO> recommendations,
                                   String requestedFormat,
                                   List<String> availableFormats,
                                   String reportQueryPath,
                                   String rawDataDownloadPath,
                                   String contractStage,
                                   String implementationStage) {
        this(
            reportId,
            taskId,
            taskType,
            sqlFingerprint,
            verdict,
            generatedAt,
            targetEngines,
            engineResults,
            thresholdAssessments,
            null,
            null,
            java.util.Collections.<BenchmarkAlertLinkageVO>emptyList(),
            trendCharts,
            recommendations,
            requestedFormat,
            availableFormats,
            reportQueryPath,
            rawDataDownloadPath,
            contractStage,
            implementationStage
        );
    }

    public BenchmarkReportResponse(String reportId,
                                   String taskId,
                                   BenchmarkTaskType taskType,
                                   String sqlFingerprint,
                                   BenchmarkThresholdVerdict verdict,
                                   Instant generatedAt,
                                   List<DataSourceTypeEnum> targetEngines,
                                   List<BenchmarkEngineMetricVO> engineResults,
                                   List<BenchmarkThresholdAssessmentVO> thresholdAssessments,
                                   BenchmarkScaleReadinessAssessment scaleReadiness,
                                   BenchmarkRegressionSummaryVO regressionSummary,
                                   List<BenchmarkAlertLinkageVO> alertLinkages,
                                   List<BenchmarkTrendChartVO> trendCharts,
                                   List<BenchmarkRecommendationVO> recommendations,
                                   String requestedFormat,
                                   List<String> availableFormats,
                                   String reportQueryPath,
                                   String rawDataDownloadPath,
                                   String contractStage,
                                   String implementationStage) {
        this.reportId = reportId;
        this.taskId = taskId;
        this.taskType = taskType;
        this.sqlFingerprint = sqlFingerprint;
        this.verdict = verdict;
        this.generatedAt = generatedAt;
        this.targetEngines = targetEngines;
        this.engineResults = engineResults;
        this.thresholdAssessments = thresholdAssessments;
        this.scaleReadiness = scaleReadiness;
        this.regressionSummary = regressionSummary;
        this.alertLinkages = alertLinkages;
        this.trendCharts = trendCharts;
        this.recommendations = recommendations;
        this.requestedFormat = requestedFormat;
        this.availableFormats = availableFormats;
        this.reportQueryPath = reportQueryPath;
        this.rawDataDownloadPath = rawDataDownloadPath;
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

    public List<DataSourceTypeEnum> getTargetEngines() {
        return targetEngines;
    }

    public List<BenchmarkEngineMetricVO> getEngineResults() {
        return engineResults;
    }

    public List<BenchmarkThresholdAssessmentVO> getThresholdAssessments() {
        return thresholdAssessments;
    }

    public BenchmarkScaleReadinessAssessment getScaleReadiness() {
        return scaleReadiness;
    }

    public BenchmarkRegressionSummaryVO getRegressionSummary() {
        return regressionSummary;
    }

    public List<BenchmarkAlertLinkageVO> getAlertLinkages() {
        return alertLinkages;
    }

    public List<BenchmarkTrendChartVO> getTrendCharts() {
        return trendCharts;
    }

    public List<BenchmarkRecommendationVO> getRecommendations() {
        return recommendations;
    }

    public String getRequestedFormat() {
        return requestedFormat;
    }

    public List<String> getAvailableFormats() {
        return availableFormats;
    }

    public String getReportQueryPath() {
        return reportQueryPath;
    }

    public String getRawDataDownloadPath() {
        return rawDataDownloadPath;
    }

    public String getContractStage() {
        return contractStage;
    }

    public String getImplementationStage() {
        return implementationStage;
    }
}
