package com.company.sqloptimization.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.domain.governance.RewriteValidationStatus;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendationFilter;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RecommendationType;
import com.company.sqloptimization.infrastructure.persistence.entity.AccelerationRecommendationRecord;
import com.company.sqloptimization.infrastructure.persistence.mapper.AccelerationRecommendationMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MybatisAccelerationRecommendationRepositoryTest {

    @Test
    void shouldPersistRecommendationRuleOutputModelEvidence() {
        AccelerationRecommendationMapper mapper = org.mockito.Mockito.mock(AccelerationRecommendationMapper.class);
        when(mapper.selectByRecommendationId(anyString())).thenReturn(null);
        MybatisAccelerationRecommendationRepository repository =
            new MybatisAccelerationRecommendationRepository(mapper);

        repository.save(sampleRecommendation());

        ArgumentCaptor<AccelerationRecommendationRecord> captor =
            ArgumentCaptor.forClass(AccelerationRecommendationRecord.class);
        verify(mapper).insert(captor.capture());
        AccelerationRecommendationRecord record = captor.getValue();
        assertEquals("recommendation-001", record.getRecommendationId());
        assertEquals("PARSE", record.getSourceType());
        assertEquals("STRUCTURE_PARSE", record.getSourceKind());
        assertEquals("STATIC_PARSE", record.getEvidenceLevel());
        assertEquals("SQL_RECOMMENDATION_RULE_MODEL_V1", record.getSchemaVersion());
        assertEquals("[{\"rule\":\"COUNT_ONE_TO_COUNT_STAR\"}]", record.getRuleChainJson());
        assertEquals("[{\"rule\":\"SELECT_STAR_EXPANSION\"}]", record.getUnappliedRulesJson());
        assertEquals("{\"mvType\":\"PARAMETERIZED_AGG_MV\"}", record.getAccelerationArtifactJson());
        assertEquals("{\"claimBoundary\":\"NOT_REAL_EXECUTION_GAIN\"}", record.getExpectedBenefitJson());
        assertEquals(Boolean.FALSE, record.getAutoApplyAllowed());
        assertEquals(Boolean.TRUE, record.getManualReviewRequired());
    }

    @Test
    void shouldRestoreRecommendationRuleOutputModelEvidence() {
        AccelerationRecommendationMapper mapper = org.mockito.Mockito.mock(AccelerationRecommendationMapper.class);
        when(mapper.selectByRecommendationId("recommendation-002")).thenReturn(sampleRecord());
        MybatisAccelerationRecommendationRepository repository =
            new MybatisAccelerationRecommendationRepository(mapper);

        AccelerationRecommendation restored = repository.findByRecommendationId("recommendation-002");

        assertNotNull(restored);
        assertEquals(GovernanceSourceType.PARSE, restored.getSourceType());
        assertEquals(GovernanceSourceKind.STRUCTURE_PARSE, restored.getSourceKind());
        assertEquals(EvidenceLevel.STATIC_PARSE, restored.getEvidenceLevel());
        assertEquals(RewriteValidationStatus.NOT_VALIDATED, restored.getValidationStatus());
        assertEquals("COUNT_ONE_TO_COUNT_STAR", restored.getRuleChain().get(0).get("rule"));
        assertEquals("SELECT_STAR_EXPANSION", restored.getUnappliedRules().get(0).get("rule"));
        assertEquals("PARAMETERIZED_AGG_MV", restored.getAccelerationArtifact().get("mvType"));
        assertEquals("NOT_REAL_EXECUTION_GAIN", restored.getExpectedBenefit().get("claimBoundary"));
        assertFalse(restored.isAutoApplyAllowed());
        assertEquals(true, restored.isManualReviewRequired());
    }

    @Test
    void shouldDelegatePagedRecommendationFilterAndCount() {
        AccelerationRecommendationMapper mapper = org.mockito.Mockito.mock(AccelerationRecommendationMapper.class);
        AccelerationRecommendationFilter filter = new AccelerationRecommendationFilter();
        filter.setTenantId("tenant-a");
        filter.setRecommendationType("REWRITE");
        filter.setBenefitLevel("HIGH");
        filter.setRequiresDispatch(Boolean.TRUE);
        filter.setManualReviewRequired(Boolean.TRUE);
        filter.setOrderByClause("created_at DESC, recommendation_id DESC");
        filter.setOffset(8);
        filter.setLimit(9);
        when(mapper.selectPage(filter)).thenReturn(Collections.singletonList(sampleRecord()));
        when(mapper.count(filter)).thenReturn(Integer.valueOf(11));
        MybatisAccelerationRecommendationRepository repository =
            new MybatisAccelerationRecommendationRepository(mapper);

        List<AccelerationRecommendation> rows = repository.findPage(filter);
        int totalCount = repository.count(filter);

        assertEquals(1, rows.size());
        assertEquals("recommendation-002", rows.get(0).getRecommendationId());
        assertEquals(11, totalCount);
        verify(mapper).selectPage(filter);
        verify(mapper).count(filter);
    }

    private AccelerationRecommendation sampleRecommendation() {
        return AccelerationRecommendation.builder()
            .recommendationId("recommendation-001")
            .tenantId("tenant-a")
            .recommendationType(RecommendationType.REWRITE)
            .sourceSqlText("SELECT COUNT(1) FROM orders")
            .recommendedSqlText("SELECT COUNT(*) FROM orders")
            .sourceType(GovernanceSourceType.PARSE)
            .sourceKind(GovernanceSourceKind.STRUCTURE_PARSE)
            .sourceId("parse-task-001")
            .evidenceLevel(EvidenceLevel.STATIC_PARSE)
            .ruleChain(Collections.singletonList(rule("COUNT_ONE_TO_COUNT_STAR")))
            .unappliedRules(Collections.singletonList(rule("SELECT_STAR_EXPANSION")))
            .accelerationArtifact(map("mvType", "PARAMETERIZED_AGG_MV"))
            .expectedBenefit(map("claimBoundary", "NOT_REAL_EXECUTION_GAIN"))
            .confidence(Integer.valueOf(70))
            .validationMethod("RESULT_DIFF_THEN_MANUAL_REVIEW")
            .validationStatus(RewriteValidationStatus.NOT_VALIDATED)
            .autoApplyAllowed(Boolean.FALSE)
            .manualReviewRequired(Boolean.TRUE)
            .createdBy("user-001")
            .createdAt(Instant.parse("2026-05-10T10:00:00Z"))
            .updatedAt(Instant.parse("2026-05-10T10:00:00Z"))
            .build();
    }

    private AccelerationRecommendationRecord sampleRecord() {
        AccelerationRecommendationRecord record = new AccelerationRecommendationRecord();
        record.setRecommendationId("recommendation-002");
        record.setTenantId("tenant-a");
        record.setRecommendationType("REWRITE");
        record.setRecommendedSqlText("SELECT COUNT(*) FROM orders");
        record.setSourceType("PARSE");
        record.setSourceKind("STRUCTURE_PARSE");
        record.setSourceId("parse-task-001");
        record.setEvidenceLevel("STATIC_PARSE");
        record.setSchemaVersion("SQL_RECOMMENDATION_RULE_MODEL_V1");
        record.setRuleChainJson("[{\"rule\":\"COUNT_ONE_TO_COUNT_STAR\"}]");
        record.setUnappliedRulesJson("[{\"rule\":\"SELECT_STAR_EXPANSION\"}]");
        record.setAccelerationArtifactJson("{\"mvType\":\"PARAMETERIZED_AGG_MV\"}");
        record.setExpectedBenefitJson("{\"claimBoundary\":\"NOT_REAL_EXECUTION_GAIN\"}");
        record.setValidationStatus("NOT_VALIDATED");
        record.setAutoApplyAllowed(Boolean.FALSE);
        record.setManualReviewRequired(Boolean.TRUE);
        record.setCreatedAt(LocalDateTime.of(2026, 5, 10, 10, 0));
        record.setUpdatedAt(LocalDateTime.of(2026, 5, 10, 10, 0));
        return record;
    }

    private Map<String, Object> rule(String rule) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("rule", rule);
        return entry;
    }

    private Map<String, Object> map(String key, Object value) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put(key, value);
        return entry;
    }
}
