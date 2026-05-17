package com.company.sqloptimization.infrastructure.repository;

import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendationFilter;
import com.company.sqloptimization.domain.recommendation.repository.AccelerationRecommendationRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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

    @Override
    public List<AccelerationRecommendation> findPage(AccelerationRecommendationFilter filter) {
        List<AccelerationRecommendation> matches = matchingRecommendations(filter);
        sortMatches(matches, filter == null ? null : filter.getOrderByClause());
        int offset = filter == null ? 0 : Math.max(0, filter.getOffset());
        int limit = filter == null || filter.getLimit() <= 0 ? matches.size() : filter.getLimit();
        int start = Math.min(matches.size(), offset);
        int end = Math.min(matches.size(), start + limit);
        return new ArrayList<AccelerationRecommendation>(matches.subList(start, end));
    }

    @Override
    public int count(AccelerationRecommendationFilter filter) {
        return matchingRecommendations(filter).size();
    }

    private List<AccelerationRecommendation> matchingRecommendations(AccelerationRecommendationFilter filter) {
        List<AccelerationRecommendation> matches = new ArrayList<AccelerationRecommendation>();
        for (AccelerationRecommendation recommendation : recommendations.values()) {
            if (matchesFilter(recommendation, filter)) {
                matches.add(recommendation);
            }
        }
        return matches;
    }

    private boolean matchesFilter(AccelerationRecommendation recommendation, AccelerationRecommendationFilter filter) {
        if (filter == null) {
            return true;
        }
        if (!equalsText(filter.getTenantId(), recommendation.getTenantId())) {
            return false;
        }
        if (!matchesText(filter.getRecommendationType(), recommendation.getRecommendationType().name())) {
            return false;
        }
        if (!matchesText(filter.getStatus(), recommendation.getStatus().name())) {
            return false;
        }
        if (!matchesText(filter.getBenefitLevel(), recommendation.getBenefitLevel().name())) {
            return false;
        }
        if (!matchesText(filter.getRiskLevel(), recommendation.getRiskLevel().name())) {
            return false;
        }
        if (!matchesText(filter.getValidationStatus(), recommendation.getValidationStatus().name())) {
            return false;
        }
        if (filter.getRequiresDispatch() != null
            && filter.getRequiresDispatch().booleanValue() != recommendation.isRequiresDispatch()) {
            return false;
        }
        if (filter.getManualReviewRequired() != null
            && filter.getManualReviewRequired().booleanValue() != recommendation.isManualReviewRequired()) {
            return false;
        }
        if (!matchesText(filter.getSourceType(),
            recommendation.getSourceType() == null ? null : recommendation.getSourceType().name())) {
            return false;
        }
        if (!matchesSourceKind(filter, recommendation.getSourceKind() == null ? null : recommendation.getSourceKind().name())) {
            return false;
        }
        if (!matchesText(filter.getSourceId(), recommendation.getSourceId())) {
            return false;
        }
        if (!matchesText(filter.getHistoryId(), recommendation.getHistoryId())) {
            return false;
        }
        if (!matchesText(filter.getParseTaskId(), recommendation.getParseTaskId())) {
            return false;
        }
        if (!matchesText(filter.getBatchId(), recommendation.getBatchId())) {
            return false;
        }
        if (!matchesText(filter.getReportCode(), recommendation.getReportCode())) {
            return false;
        }
        return true;
    }

    private boolean equalsText(String expected, String actual) {
        return expected == null || expected.equals(actual);
    }

    private boolean matchesText(String expected, String actual) {
        return expected == null || expected.equals(actual);
    }

    private boolean matchesSourceKind(AccelerationRecommendationFilter filter, String actual) {
        if (filter.getSourceKinds() != null && !filter.getSourceKinds().isEmpty()) {
            return filter.getSourceKinds().contains(actual);
        }
        return matchesText(filter.getSourceKind(), actual);
    }

    private void sortMatches(List<AccelerationRecommendation> matches, String orderByClause) {
        Collections.sort(matches, comparatorFor(orderByClause));
    }

    private Comparator<AccelerationRecommendation> comparatorFor(String orderByClause) {
        final boolean ascending = orderByClause != null && orderByClause.toUpperCase().contains(" ASC");
        final String normalized = orderByClause == null ? "created_at" : orderByClause.toLowerCase();
        Comparator<AccelerationRecommendation> comparator = new Comparator<AccelerationRecommendation>() {
            @Override
            public int compare(AccelerationRecommendation left, AccelerationRecommendation right) {
                Comparable leftValue = comparableValue(left, normalized);
                Comparable rightValue = comparableValue(right, normalized);
                int result = compareNullable(leftValue, rightValue);
                if (result == 0) {
                    result = compareNullable(left.getRecommendationId(), right.getRecommendationId());
                }
                return ascending ? result : -result;
            }
        };
        return comparator;
    }

    private Comparable comparableValue(AccelerationRecommendation recommendation, String orderByClause) {
        if (orderByClause.contains("updated_at")) {
            return recommendation.getUpdatedAt() == null ? Instant.EPOCH : recommendation.getUpdatedAt();
        }
        if (orderByClause.contains("recommendation_type")) {
            return recommendation.getRecommendationType().name();
        }
        if (orderByClause.contains("status")) {
            return recommendation.getStatus().name();
        }
        if (orderByClause.contains("benefit_level")) {
            return recommendation.getBenefitLevel().name();
        }
        if (orderByClause.contains("risk_level")) {
            return recommendation.getRiskLevel().name();
        }
        if (orderByClause.contains("validation_status")) {
            return recommendation.getValidationStatus().name();
        }
        if (orderByClause.contains("requires_dispatch")) {
            return Boolean.valueOf(recommendation.isRequiresDispatch());
        }
        if (orderByClause.contains("manual_review_required")) {
            return Boolean.valueOf(recommendation.isManualReviewRequired());
        }
        return recommendation.getCreatedAt() == null ? Instant.EPOCH : recommendation.getCreatedAt();
    }

    private int compareNullable(Comparable left, Comparable right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }
}
