package com.company.benchmarkengine.application.controller.vo;

import com.company.benchmarkengine.domain.benchmark.BenchmarkRecommendationSqlRole;

public class BenchmarkRecommendationComparisonResponse {

    private String recommendationId;
    private BenchmarkRecommendationSqlRole benchmarkSqlRole;
    private BenchmarkTestSetResponse testSet;
    private BenchmarkTaskSubmitResponse benchmarkTask;
    private String contractStage;
    private String implementationStage;

    public String getRecommendationId() {
        return recommendationId;
    }

    public void setRecommendationId(String recommendationId) {
        this.recommendationId = recommendationId;
    }

    public BenchmarkRecommendationSqlRole getBenchmarkSqlRole() {
        return benchmarkSqlRole;
    }

    public void setBenchmarkSqlRole(BenchmarkRecommendationSqlRole benchmarkSqlRole) {
        this.benchmarkSqlRole = benchmarkSqlRole;
    }

    public BenchmarkTestSetResponse getTestSet() {
        return testSet;
    }

    public void setTestSet(BenchmarkTestSetResponse testSet) {
        this.testSet = testSet;
    }

    public BenchmarkTaskSubmitResponse getBenchmarkTask() {
        return benchmarkTask;
    }

    public void setBenchmarkTask(BenchmarkTaskSubmitResponse benchmarkTask) {
        this.benchmarkTask = benchmarkTask;
    }

    public String getContractStage() {
        return contractStage;
    }

    public void setContractStage(String contractStage) {
        this.contractStage = contractStage;
    }

    public String getImplementationStage() {
        return implementationStage;
    }

    public void setImplementationStage(String implementationStage) {
        this.implementationStage = implementationStage;
    }
}
