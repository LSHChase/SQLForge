package com.company.sqloptimization.infrastructure.persistence;

import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.BenefitLevel;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RecommendationStatus;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RecommendationType;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RiskLevel;
import com.company.sqloptimization.domain.recommendation.repository.AccelerationRecommendationRepository;
import com.company.sqloptimization.infrastructure.persistence.entity.AccelerationRecommendationRecord;
import com.company.sqloptimization.infrastructure.persistence.mapper.AccelerationRecommendationMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "sql-optimization.recommendation", name = "repository", havingValue = "database")
public class MybatisAccelerationRecommendationRepository implements AccelerationRecommendationRepository {

    private static final ZoneOffset DATABASE_ZONE_OFFSET = ZoneOffset.UTC;

    private final AccelerationRecommendationMapper recommendationMapper;

    public MybatisAccelerationRecommendationRepository(AccelerationRecommendationMapper recommendationMapper) {
        this.recommendationMapper = recommendationMapper;
    }

    @Override
    public AccelerationRecommendation save(AccelerationRecommendation recommendation) {
        AccelerationRecommendationRecord record = toRecord(recommendation);
        if (recommendationMapper.selectByRecommendationId(recommendation.getRecommendationId()) == null) {
            recommendationMapper.insert(record);
        } else {
            recommendationMapper.update(record);
        }
        return recommendation;
    }

    @Override
    public AccelerationRecommendation findByRecommendationId(String recommendationId) {
        AccelerationRecommendationRecord record = recommendationMapper.selectByRecommendationId(recommendationId);
        return record == null ? null : toDomain(record);
    }

    @Override
    public List<AccelerationRecommendation> findByTenantId(String tenantId) {
        List<AccelerationRecommendationRecord> records = recommendationMapper.selectByTenantId(tenantId);
        List<AccelerationRecommendation> result = new ArrayList<AccelerationRecommendation>();
        for (AccelerationRecommendationRecord record : records) {
            result.add(toDomain(record));
        }
        return result;
    }

    private AccelerationRecommendationRecord toRecord(AccelerationRecommendation recommendation) {
        AccelerationRecommendationRecord record = new AccelerationRecommendationRecord();
        record.setRecommendationId(recommendation.getRecommendationId());
        record.setTenantId(recommendation.getTenantId());
        record.setRecommendationType(recommendation.getRecommendationType().name());
        record.setSourceSqlId(recommendation.getSourceSqlId());
        record.setHistoryId(recommendation.getHistoryId());
        record.setParseTaskId(recommendation.getParseTaskId());
        record.setBatchId(recommendation.getBatchId());
        record.setRouteDecisionId(recommendation.getRouteDecisionId());
        record.setAlertId(recommendation.getAlertId());
        record.setSqlFingerprint(recommendation.getSqlFingerprint());
        record.setSourceSqlText(recommendation.getSourceSqlText());
        record.setRecommendedSqlText(recommendation.getRecommendedSqlText());
        record.setTargetEngine(recommendation.getTargetEngine());
        record.setTargetDatasource(recommendation.getTargetDatasource());
        record.setReportCode(recommendation.getReportCode());
        record.setLogicalObjectKey(recommendation.getLogicalObjectKey());
        record.setSummary(recommendation.getSummary());
        record.setReason(recommendation.getReason());
        record.setExpectedGain(recommendation.getExpectedGain());
        record.setBenefitLevel(recommendation.getBenefitLevel().name());
        record.setRiskLevel(recommendation.getRiskLevel().name());
        record.setRiskSummary(recommendation.getRiskSummary());
        record.setRequiresDispatch(Boolean.valueOf(recommendation.isRequiresDispatch()));
        record.setStatus(recommendation.getStatus().name());
        record.setCreatedBy(recommendation.getCreatedBy());
        record.setCreatedAt(toLocalDateTime(recommendation.getCreatedAt()));
        record.setUpdatedAt(toLocalDateTime(recommendation.getUpdatedAt()));
        return record;
    }

    private AccelerationRecommendation toDomain(AccelerationRecommendationRecord record) {
        return AccelerationRecommendation.builder()
            .recommendationId(record.getRecommendationId())
            .tenantId(record.getTenantId())
            .recommendationType(RecommendationType.valueOf(record.getRecommendationType()))
            .sourceSqlId(record.getSourceSqlId())
            .historyId(record.getHistoryId())
            .parseTaskId(record.getParseTaskId())
            .batchId(record.getBatchId())
            .routeDecisionId(record.getRouteDecisionId())
            .alertId(record.getAlertId())
            .sqlFingerprint(record.getSqlFingerprint())
            .sourceSqlText(record.getSourceSqlText())
            .recommendedSqlText(record.getRecommendedSqlText())
            .targetEngine(record.getTargetEngine())
            .targetDatasource(record.getTargetDatasource())
            .reportCode(record.getReportCode())
            .logicalObjectKey(record.getLogicalObjectKey())
            .summary(record.getSummary())
            .reason(record.getReason())
            .expectedGain(record.getExpectedGain())
            .benefitLevel(record.getBenefitLevel() == null ? null : BenefitLevel.valueOf(record.getBenefitLevel()))
            .riskLevel(record.getRiskLevel() == null ? null : RiskLevel.valueOf(record.getRiskLevel()))
            .riskSummary(record.getRiskSummary())
            .requiresDispatch(Boolean.TRUE.equals(record.getRequiresDispatch()))
            .status(record.getStatus() == null ? null : RecommendationStatus.valueOf(record.getStatus()))
            .createdBy(record.getCreatedBy())
            .createdAt(toInstant(record.getCreatedAt()))
            .updatedAt(toInstant(record.getUpdatedAt()))
            .build();
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, DATABASE_ZONE_OFFSET);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(DATABASE_ZONE_OFFSET);
    }
}
