package com.company.benchmarkengine.domain.benchmark;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BenchmarkReport {

    private final String reportId;
    private final String taskId;
    private final BenchmarkTaskType taskType;
    private final String tenantId;
    private final String sqlFingerprint;
    private final Instant generatedAt;
    private final List<BenchmarkEngineProfile> engineProfiles;
    private final List<BenchmarkThresholdAssessment> thresholdAssessments;
    private final List<BenchmarkRecommendation> recommendations;
    private final BenchmarkThresholdVerdict verdict;

    public BenchmarkReport(String reportId,
                           String taskId,
                           BenchmarkTaskType taskType,
                           String tenantId,
                           String sqlFingerprint,
                           Instant generatedAt,
                           List<BenchmarkEngineProfile> engineProfiles,
                           List<BenchmarkThresholdAssessment> thresholdAssessments,
                           List<BenchmarkRecommendation> recommendations) {
        this.reportId = reportId;
        this.taskId = taskId;
        this.taskType = taskType;
        this.tenantId = tenantId;
        this.sqlFingerprint = sqlFingerprint;
        this.generatedAt = generatedAt;
        this.engineProfiles = immutableCopy(engineProfiles);
        this.thresholdAssessments = immutableCopy(thresholdAssessments);
        this.recommendations = immutableCopy(recommendations);
        this.verdict = calculateVerdict(this.thresholdAssessments);
    }

    private BenchmarkThresholdVerdict calculateVerdict(List<BenchmarkThresholdAssessment> assessments) {
        BenchmarkThresholdVerdict current = BenchmarkThresholdVerdict.PASS;
        for (BenchmarkThresholdAssessment assessment : assessments) {
            if (assessment.getVerdict() == BenchmarkThresholdVerdict.FAIL) {
                return BenchmarkThresholdVerdict.FAIL;
            }
            if (assessment.getVerdict() == BenchmarkThresholdVerdict.WARNING) {
                current = BenchmarkThresholdVerdict.WARNING;
            }
        }
        return current;
    }

    private <T> List<T> immutableCopy(List<T> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<T>(items));
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

    public String getTenantId() {
        return tenantId;
    }

    public String getSqlFingerprint() {
        return sqlFingerprint;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
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

    public BenchmarkThresholdVerdict getVerdict() {
        return verdict;
    }
}
