package com.company.sqloptimization.domain.recommendation.repository;

import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendationFilter;
import java.util.List;

public interface AccelerationRecommendationRepository {

    AccelerationRecommendation save(AccelerationRecommendation recommendation);

    AccelerationRecommendation findByRecommendationId(String recommendationId);

    List<AccelerationRecommendation> findByTenantId(String tenantId);

    List<AccelerationRecommendation> findPage(AccelerationRecommendationFilter filter);

    int count(AccelerationRecommendationFilter filter);
}
