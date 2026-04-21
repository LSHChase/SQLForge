package com.company.benchmarkengine.application.controller.vo;

import com.company.benchmarkengine.domain.benchmark.BenchmarkRecommendationRiskLevel;

public class BenchmarkRecommendationVO {

    private final String category;
    private final String title;
    private final String summary;
    private final String expectedBenefit;
    private final BenchmarkRecommendationRiskLevel riskLevel;

    public BenchmarkRecommendationVO(String category,
                                     String title,
                                     String summary,
                                     String expectedBenefit,
                                     BenchmarkRecommendationRiskLevel riskLevel) {
        this.category = category;
        this.title = title;
        this.summary = summary;
        this.expectedBenefit = expectedBenefit;
        this.riskLevel = riskLevel;
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getExpectedBenefit() {
        return expectedBenefit;
    }

    public BenchmarkRecommendationRiskLevel getRiskLevel() {
        return riskLevel;
    }
}
