package com.company.benchmarkengine.domain.benchmark;

public class BenchmarkRecommendation {

    private final String category;
    private final String title;
    private final String summary;
    private final String expectedBenefit;
    private final BenchmarkRecommendationRiskLevel riskLevel;

    public BenchmarkRecommendation(String category,
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
