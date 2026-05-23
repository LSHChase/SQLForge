package com.company.sqloptimization.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.sqloptimization.domain.governance.ComparisonStatus;
import com.company.sqloptimization.domain.governance.DifferenceType;
import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.domain.governance.RewriteAlertStatus;
import com.company.sqloptimization.domain.governance.RewriteActivationStatus;
import com.company.sqloptimization.domain.governance.RewriteRecordStatus;
import com.company.sqloptimization.domain.governance.RewriteReviewStatus;
import com.company.sqloptimization.domain.governance.RewriteValidationStatus;
import com.company.sqloptimization.domain.governance.ValidationRunStatus;
import com.company.sqloptimization.domain.rewrite.RewriteValidationRun;
import com.company.sqloptimization.domain.rewrite.SqlRewriteRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.RewriteValidationRunRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.SqlRewriteRecordRecord;
import com.company.sqloptimization.infrastructure.persistence.mapper.RewriteValidationRunMapper;
import com.company.sqloptimization.infrastructure.persistence.mapper.SqlRewriteRecordMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MybatisSqlRewriteRecordRepositoryTest {

    @Test
    void shouldPersistRewriteRecordWithDiffAndRiskEvidence() {
        SqlRewriteRecordMapper recordMapper = org.mockito.Mockito.mock(SqlRewriteRecordMapper.class);
        RewriteValidationRunMapper runMapper = org.mockito.Mockito.mock(RewriteValidationRunMapper.class);
        when(recordMapper.selectByRewriteRecordId(anyString())).thenReturn(null);
        MybatisSqlRewriteRecordRepository repository =
            new MybatisSqlRewriteRecordRepository(recordMapper, runMapper);

        repository.saveRecord(sampleRewriteRecord());

        ArgumentCaptor<SqlRewriteRecordRecord> captor =
            ArgumentCaptor.forClass(SqlRewriteRecordRecord.class);
        verify(recordMapper).insert(captor.capture());
        SqlRewriteRecordRecord record = captor.getValue();
        assertEquals("rewrite-001", record.getRewriteRecordId());
        assertEquals("tenant-a", record.getTenantId());
        assertEquals("PARSE", record.getSourceType());
        assertEquals("NOT_VALIDATED", record.getValidationStatus());
        assertEquals(Boolean.TRUE, record.getAutoApplyAllowed());
        assertEquals("CHANGES_REQUESTED", record.getReviewStatus());
        assertEquals("needs safer predicate", record.getReviewNote());
        assertEquals("reviewer-001", record.getReviewedBy());
        assertEquals(LocalDateTime.of(2026, 5, 10, 10, 5), record.getReviewedAt());
        assertEquals("ACTIVATE_FAILED", record.getActivationStatus());
        assertEquals("binding-001", record.getRuntimeBindingId());
        assertEquals(LocalDateTime.of(2026, 5, 10, 10, 6), record.getRuntimeBindingAt());
        assertEquals("operator-002", record.getRuntimeBindingBy());
        assertEquals("tenant-a:fp-001", record.getRuntimeBindingScope());
        assertEquals("fp-published-001", record.getActivatedSqlFingerprint());
        assertEquals("rule-v1", record.getRuntimeRuleVersion());
        assertEquals(LocalDateTime.of(2026, 5, 10, 10, 0), record.getCreatedAt());
        assertEquals("[{\"rule\":\"COUNT_STAR\"}]", record.getRuleChainJson());
        assertEquals("{\"risk\":\"LOW\"}", record.getRiskJson());
    }

    @Test
    void shouldRestoreRewriteRecordAndValidationRunFromDatabaseRecords() {
        SqlRewriteRecordMapper recordMapper = org.mockito.Mockito.mock(SqlRewriteRecordMapper.class);
        RewriteValidationRunMapper runMapper = org.mockito.Mockito.mock(RewriteValidationRunMapper.class);
        when(recordMapper.selectByRewriteRecordId("rewrite-002")).thenReturn(sampleRewriteRecordRecord());
        when(runMapper.selectByRewriteRecordId("rewrite-002")).thenReturn(
            Collections.singletonList(sampleValidationRunRecord())
        );
        MybatisSqlRewriteRecordRepository repository =
            new MybatisSqlRewriteRecordRepository(recordMapper, runMapper);

        SqlRewriteRecord restored = repository.findRecordById("rewrite-002");
        RewriteValidationRun run = repository.findValidationRunsByRewriteRecordId("rewrite-002").get(0);

        assertNotNull(restored);
        assertEquals(GovernanceSourceKind.QUERY_HISTORY, restored.getSourceKind());
        assertEquals(RewriteRecordStatus.PAUSED, restored.getStatus());
        assertEquals(RewriteValidationStatus.DIVERGED, restored.getValidationStatus());
        assertEquals(RewriteReviewStatus.APPROVED, restored.getReviewStatus());
        assertEquals("review accepted", restored.getReviewNote());
        assertEquals("reviewer-002", restored.getReviewedBy());
        assertEquals(Instant.parse("2026-05-10T10:32:00Z"), restored.getReviewedAt());
        assertEquals(RewriteActivationStatus.ACTIVE, restored.getActivationStatus());
        assertEquals("binding-002", restored.getRuntimeBindingId());
        assertEquals(Instant.parse("2026-05-10T10:33:00Z"), restored.getRuntimeBindingAt());
        assertEquals("operator-002", restored.getRuntimeBindingBy());
        assertEquals("tenant-a:fp-002", restored.getRuntimeBindingScope());
        assertEquals("fp-published-002", restored.getActivatedSqlFingerprint());
        assertEquals("rule-v2", restored.getRuntimeRuleVersion());
        assertFalse(restored.isAutoApplyAllowed());
        assertEquals("LOW", restored.getRisk().get("risk"));
        assertEquals(ValidationRunStatus.SUCCEEDED, run.getStatus());
        assertEquals(ComparisonStatus.DIVERGED, run.getComparisonStatus());
        assertEquals(DifferenceType.VALUE_DIFF, run.getDifferenceType());
        assertEquals(Integer.valueOf(2), run.getDifferenceSample().get("rows"));
    }

    @Test
    void shouldPersistValidationRunAndPauseRewriteRecordSummary() {
        SqlRewriteRecordMapper recordMapper = org.mockito.Mockito.mock(SqlRewriteRecordMapper.class);
        RewriteValidationRunMapper runMapper = org.mockito.Mockito.mock(RewriteValidationRunMapper.class);
        SqlRewriteRecordRecord existing = sampleRewriteRecordRecord();
        existing.setStatus("ACTIVE");
        existing.setValidationStatus("EQUIVALENT");
        existing.setAutoApplyAllowed(Boolean.TRUE);
        existing.setAlertStatus("NONE");
        when(runMapper.selectByValidationRunId("validation-003")).thenReturn(null);
        when(recordMapper.selectByRewriteRecordId("rewrite-002")).thenReturn(existing);
        MybatisSqlRewriteRecordRepository repository =
            new MybatisSqlRewriteRecordRepository(recordMapper, runMapper);

        repository.saveValidationRun(sampleValidationRun());

        ArgumentCaptor<RewriteValidationRunRecord> runCaptor =
            ArgumentCaptor.forClass(RewriteValidationRunRecord.class);
        verify(runMapper).insert(runCaptor.capture());
        assertEquals("validation-003", runCaptor.getValue().getValidationRunId());
        assertEquals(Boolean.TRUE, runCaptor.getValue().getAutoApplyPaused());

        ArgumentCaptor<SqlRewriteRecordRecord> recordCaptor =
            ArgumentCaptor.forClass(SqlRewriteRecordRecord.class);
        verify(recordMapper, atLeastOnce()).update(recordCaptor.capture());
        SqlRewriteRecordRecord updated = recordCaptor.getValue();
        assertEquals("validation-003", updated.getLastValidationRunId());
        assertEquals("PAUSED", updated.getStatus());
        assertEquals("DIVERGED", updated.getValidationStatus());
        assertEquals("OPEN", updated.getAlertStatus());
        assertEquals(Boolean.FALSE, updated.getAutoApplyAllowed());
        assertEquals("APPROVED", updated.getReviewStatus());
        assertEquals("ACTIVE", updated.getActivationStatus());
        assertEquals("binding-002", updated.getRuntimeBindingId());
        assertEquals("rule-v2", updated.getRuntimeRuleVersion());
    }

    private SqlRewriteRecord sampleRewriteRecord() {
        Map<String, Object> rule = new LinkedHashMap<String, Object>();
        rule.put("rule", "COUNT_STAR");
        Map<String, Object> risk = new LinkedHashMap<String, Object>();
        risk.put("risk", "LOW");
        return SqlRewriteRecord.builder()
            .rewriteRecordId("rewrite-001")
            .tenantId("tenant-a")
            .recommendationId("recommendation-001")
            .optimizationTaskId("task-001")
            .sourceType(GovernanceSourceType.PARSE)
            .sourceKind(GovernanceSourceKind.STRUCTURE_PARSE)
            .sourceId("parse-task-001")
            .evidenceLevel(EvidenceLevel.STATIC_PARSE)
            .historyId("history-001")
            .parseHistoryId("parse-history-001")
            .sqlFingerprint("fp-001")
            .datasourceCode("hetu_main")
            .status(RewriteRecordStatus.DRAFT)
            .validationStatus(RewriteValidationStatus.NOT_VALIDATED)
            .autoApplyAllowed(true)
            .manualReviewRequired(false)
            .reviewStatus(RewriteReviewStatus.CHANGES_REQUESTED)
            .reviewNote("needs safer predicate")
            .reviewedBy("reviewer-001")
            .reviewedAt(Instant.parse("2026-05-10T10:05:00Z"))
            .activationStatus(RewriteActivationStatus.ACTIVATE_FAILED)
            .runtimeBindingId("binding-001")
            .runtimeBindingAt(Instant.parse("2026-05-10T10:06:00Z"))
            .runtimeBindingBy("operator-002")
            .runtimeBindingScope("tenant-a:fp-001")
            .activatedSqlFingerprint("fp-published-001")
            .runtimeRuleVersion("rule-v1")
            .alertStatus(RewriteAlertStatus.NONE)
            .originalSqlText("SELECT COUNT(1) FROM orders")
            .recommendedSqlText("SELECT COUNT(*) FROM orders")
            .createdBy("user-001")
            .createdAt(Instant.parse("2026-05-10T10:00:00Z"))
            .updatedAt(Instant.parse("2026-05-10T10:00:00Z"))
            .ruleChain(Collections.singletonList(rule))
            .risk(risk)
            .build();
    }

    private RewriteValidationRun sampleValidationRun() {
        Map<String, Object> sample = new LinkedHashMap<String, Object>();
        sample.put("rows", Integer.valueOf(2));
        return RewriteValidationRun.builder()
            .validationRunId("validation-003")
            .tenantId("tenant-a")
            .rewriteRecordId("rewrite-002")
            .recommendationId("recommendation-002")
            .historyId("history-002")
            .sqlFingerprint("fp-002")
            .status(ValidationRunStatus.SUCCEEDED)
            .comparisonStatus(ComparisonStatus.DIVERGED)
            .differenceType(DifferenceType.VALUE_DIFF)
            .autoApplyPaused(true)
            .startedAt(Instant.parse("2026-05-10T11:00:00Z"))
            .finishedAt(Instant.parse("2026-05-10T11:01:00Z"))
            .differenceSample(sample)
            .build();
    }

    private SqlRewriteRecordRecord sampleRewriteRecordRecord() {
        SqlRewriteRecordRecord record = new SqlRewriteRecordRecord();
        record.setRewriteRecordId("rewrite-002");
        record.setTenantId("tenant-a");
        record.setRecommendationId("recommendation-002");
        record.setOptimizationTaskId("task-002");
        record.setSourceType("QUERY");
        record.setSourceKind("QUERY_HISTORY");
        record.setSourceId("history-002");
        record.setEvidenceLevel("RUNTIME_HISTORY");
        record.setHistoryId("history-002");
        record.setParseHistoryId("parse-history-002");
        record.setSqlFingerprint("fp-002");
        record.setDatasourceCode("hetu_main");
        record.setStatus("PAUSED");
        record.setValidationStatus("DIVERGED");
        record.setAutoApplyAllowed(Boolean.FALSE);
        record.setManualReviewRequired(Boolean.TRUE);
        record.setReviewStatus("APPROVED");
        record.setReviewNote("review accepted");
        record.setReviewedBy("reviewer-002");
        record.setReviewedAt(LocalDateTime.of(2026, 5, 10, 10, 32));
        record.setActivationStatus("ACTIVE");
        record.setRuntimeBindingId("binding-002");
        record.setRuntimeBindingAt(LocalDateTime.of(2026, 5, 10, 10, 33));
        record.setRuntimeBindingBy("operator-002");
        record.setRuntimeBindingScope("tenant-a:fp-002");
        record.setActivatedSqlFingerprint("fp-published-002");
        record.setRuntimeRuleVersion("rule-v2");
        record.setLastValidationRunId("validation-002");
        record.setLastComparedAt(LocalDateTime.of(2026, 5, 10, 10, 31));
        record.setAlertStatus("OPEN");
        record.setOriginalSqlText("SELECT * FROM orders");
        record.setRecommendedSqlText("SELECT id FROM orders");
        record.setCreatedBy("user-001");
        record.setCreatedAt(LocalDateTime.of(2026, 5, 10, 10, 30));
        record.setUpdatedAt(LocalDateTime.of(2026, 5, 10, 10, 31));
        record.setRiskJson("{\"risk\":\"LOW\"}");
        return record;
    }

    private RewriteValidationRunRecord sampleValidationRunRecord() {
        RewriteValidationRunRecord record = new RewriteValidationRunRecord();
        record.setValidationRunId("validation-002");
        record.setTenantId("tenant-a");
        record.setRewriteRecordId("rewrite-002");
        record.setRecommendationId("recommendation-002");
        record.setHistoryId("history-002");
        record.setSqlFingerprint("fp-002");
        record.setStatus("SUCCEEDED");
        record.setComparisonStatus("DIVERGED");
        record.setDifferenceType("VALUE_DIFF");
        record.setAutoApplyPaused(Boolean.TRUE);
        record.setStartedAt(LocalDateTime.of(2026, 5, 10, 10, 30));
        record.setFinishedAt(LocalDateTime.of(2026, 5, 10, 10, 31));
        record.setDifferenceSampleJson("{\"rows\":2}");
        return record;
    }
}
