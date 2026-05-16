package com.company.sqloptimization.infrastructure.persistence;

import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.domain.governance.RewriteValidationStatus;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.BenefitLevel;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RecommendationStatus;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RecommendationType;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RiskLevel;
import com.company.sqloptimization.domain.recommendation.repository.AccelerationRecommendationRepository;
import com.company.sqloptimization.infrastructure.persistence.entity.AccelerationRecommendationRecord;
import com.company.sqloptimization.infrastructure.persistence.mapper.AccelerationRecommendationMapper;
import com.fasterxml.jackson.databind.JavaType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
        record.setSourceType(recommendation.getSourceType() == null ? null : recommendation.getSourceType().name());
        record.setSourceKind(recommendation.getSourceKind() == null ? null : recommendation.getSourceKind().name());
        record.setSourceId(recommendation.getSourceId());
        record.setEvidenceLevel(recommendation.getEvidenceLevel() == null ? null : recommendation.getEvidenceLevel().name());
        record.setSchemaVersion(recommendation.getSchemaVersion());
        record.setRuleChainJson(writeList(recommendation.getRuleChain()));
        record.setUnappliedRulesJson(writeList(recommendation.getUnappliedRules()));
        record.setPreconditionsJson(writeList(recommendation.getPreconditions()));
        record.setSemanticRisksJson(writeList(recommendation.getSemanticRisks()));
        record.setExpectedBenefitJson(writeMap(recommendation.getExpectedBenefit()));
        record.setEstimatedCostJson(writeMap(recommendation.getEstimatedCost()));
        record.setConfidence(recommendation.getConfidence());
        record.setValidationMethod(recommendation.getValidationMethod());
        record.setValidationStatus(recommendation.getValidationStatus().name());
        record.setAutoApplyAllowed(Boolean.valueOf(recommendation.isAutoApplyAllowed()));
        record.setManualReviewRequired(Boolean.valueOf(recommendation.isManualReviewRequired()));
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
            .sourceType(record.getSourceType() == null ? null : GovernanceSourceType.valueOf(record.getSourceType()))
            .sourceKind(record.getSourceKind() == null ? null : GovernanceSourceKind.valueOf(record.getSourceKind()))
            .sourceId(record.getSourceId())
            .evidenceLevel(record.getEvidenceLevel() == null ? null : EvidenceLevel.valueOf(record.getEvidenceLevel()))
            .schemaVersion(record.getSchemaVersion())
            .ruleChain(readList(record.getRuleChainJson()))
            .unappliedRules(readList(record.getUnappliedRulesJson()))
            .preconditions(readList(record.getPreconditionsJson()))
            .semanticRisks(readList(record.getSemanticRisksJson()))
            .expectedBenefit(readMap(record.getExpectedBenefitJson()))
            .estimatedCost(readMap(record.getEstimatedCostJson()))
            .confidence(record.getConfidence())
            .validationMethod(record.getValidationMethod())
            .validationStatus(record.getValidationStatus() == null
                ? null
                : RewriteValidationStatus.valueOf(record.getValidationStatus()))
            .autoApplyAllowed(record.getAutoApplyAllowed())
            .manualReviewRequired(record.getManualReviewRequired())
            .createdBy(record.getCreatedBy())
            .createdAt(toInstant(record.getCreatedAt()))
            .updatedAt(toInstant(record.getUpdatedAt()))
            .build();
    }

    private String writeMap(Map<String, Object> value) {
        return value == null || value.isEmpty() ? null : JsonUtils.toJson(value);
    }

    private String writeList(List<Map<String, Object>> value) {
        return value == null || value.isEmpty() ? null : JsonUtils.toJson(value);
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            JavaType type = JsonUtils.objectMapper().getTypeFactory()
                .constructMapType(Map.class, String.class, Object.class);
            return JsonUtils.objectMapper().readValue(json, type);
        } catch (Exception ex) {
            throw new IllegalArgumentException("推荐 JSON 反序列化失败", ex);
        }
    }

    private List<Map<String, Object>> readList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            JavaType type = JsonUtils.objectMapper().getTypeFactory()
                .constructCollectionType(List.class, Map.class);
            return JsonUtils.objectMapper().readValue(json, type);
        } catch (Exception ex) {
            throw new IllegalArgumentException("推荐列表 JSON 反序列化失败", ex);
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, DATABASE_ZONE_OFFSET);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(DATABASE_ZONE_OFFSET);
    }
}
