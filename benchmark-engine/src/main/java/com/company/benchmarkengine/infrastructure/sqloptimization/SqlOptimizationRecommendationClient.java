package com.company.benchmarkengine.infrastructure.sqloptimization;

public interface SqlOptimizationRecommendationClient {

    SqlOptimizationAccelerationRecommendation getRecommendation(String recommendationId);
}
