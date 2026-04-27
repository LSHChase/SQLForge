package com.company.sqloptimization.infrastructure.repository;

import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.recommendation.repository.AccelerationRecommendationRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class InMemoryAccelerationRecommendationRepository implements AccelerationRecommendationRepository {

    private final Map<String, AccelerationRecommendation> recommendations =
        new ConcurrentHashMap<String, AccelerationRecommendation>();

    @Override
    public AccelerationRecommendation save(AccelerationRecommendation recommendation) {
        recommendations.put(recommendation.getRecommendationId(), recommendation);
        return recommendation;
    }

    @Override
    public AccelerationRecommendation findByRecommendationId(String recommendationId) {
        return recommendations.get(recommendationId);
    }

    @Override
    public List<AccelerationRecommendation> findByTenantId(String tenantId) {
        List<AccelerationRecommendation> matches = new ArrayList<AccelerationRecommendation>();
        for (AccelerationRecommendation recommendation : recommendations.values()) {
            if (recommendation.getTenantId().equals(tenantId)) {
                matches.add(recommendation);
            }
        }
        matches.sort(Comparator.comparing(AccelerationRecommendation::getCreatedAt).reversed());
        return matches;
    }
}
