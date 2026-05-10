package com.company.sqloptimization.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.sqloptimization.domain.candidate.AccelerationCandidate;
import com.company.sqloptimization.domain.governance.CandidateStatus;
import com.company.sqloptimization.domain.governance.CandidateType;
import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.infrastructure.persistence.entity.AccelerationCandidateRecord;
import com.company.sqloptimization.infrastructure.persistence.mapper.AccelerationCandidateMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MybatisAccelerationCandidateRepositoryTest {

    @Test
    void shouldPersistCandidateAsDatabaseRecord() {
        AccelerationCandidateMapper mapper = org.mockito.Mockito.mock(AccelerationCandidateMapper.class);
        when(mapper.selectByCandidateId(anyString())).thenReturn(null);
        MybatisAccelerationCandidateRepository repository = new MybatisAccelerationCandidateRepository(mapper);

        repository.save(sampleCandidate());

        ArgumentCaptor<AccelerationCandidateRecord> captor =
            ArgumentCaptor.forClass(AccelerationCandidateRecord.class);
        verify(mapper).insert(captor.capture());
        AccelerationCandidateRecord record = captor.getValue();
        assertEquals("candidate-001", record.getCandidateId());
        assertEquals("tenant-a", record.getTenantId());
        assertEquals("PARSE", record.getSourceType());
        assertEquals("STRUCTURE_PARSE", record.getSourceKind());
        assertEquals("ACCELERATION_AND_REWRITE", record.getCandidateType());
        assertEquals("STATIC_PARSE", record.getEvidenceLevel());
        assertEquals(LocalDateTime.of(2026, 5, 10, 9, 0), record.getCreatedAt());
        assertEquals("{\"object\":\"orders\"}", record.getSourceEvidenceJson());
    }

    @Test
    void shouldRestoreCandidateFromDatabaseRecord() {
        AccelerationCandidateMapper mapper = org.mockito.Mockito.mock(AccelerationCandidateMapper.class);
        AccelerationCandidateRecord record = new AccelerationCandidateRecord();
        record.setCandidateId("candidate-002");
        record.setTenantId("tenant-a");
        record.setSourceType("QUERY");
        record.setSourceKind("QUERY_HISTORY");
        record.setSourceId("history-002");
        record.setHistoryId("history-002");
        record.setParseHistoryId("parse-history-002");
        record.setParseTaskId("parse-task-002");
        record.setBatchId("batch-002");
        record.setBatchItemId("batch-item-002");
        record.setBenchmarkTaskId("benchmark-002");
        record.setOptimizationTaskId("task-002");
        record.setSqlFingerprint("fp-002");
        record.setDatasourceCode("hetu_main");
        record.setStage("PROD");
        record.setReportCode("RPT_002");
        record.setCandidateType("REWRITE");
        record.setStatus("RECOMMENDED");
        record.setConfidence(new BigDecimal("0.8200"));
        record.setPriority(Integer.valueOf(8));
        record.setEvidenceLevel("RUNTIME_HISTORY");
        record.setSchemaVersion("v1");
        record.setCreatedBy("operator-001");
        record.setCreatedAt(LocalDateTime.of(2026, 5, 10, 9, 1));
        record.setUpdatedAt(LocalDateTime.of(2026, 5, 10, 9, 2));
        record.setRuntimeEvidenceJson("{\"p99Ms\":1200}");
        when(mapper.selectByCandidateId("candidate-002")).thenReturn(record);

        MybatisAccelerationCandidateRepository repository = new MybatisAccelerationCandidateRepository(mapper);
        AccelerationCandidate restored = repository.findByCandidateId("candidate-002");

        assertNotNull(restored);
        assertEquals(GovernanceSourceType.QUERY, restored.getSourceType());
        assertEquals(GovernanceSourceKind.QUERY_HISTORY, restored.getSourceKind());
        assertEquals(CandidateType.REWRITE, restored.getCandidateType());
        assertEquals(CandidateStatus.RECOMMENDED, restored.getStatus());
        assertEquals(EvidenceLevel.RUNTIME_HISTORY, restored.getEvidenceLevel());
        assertEquals(Instant.parse("2026-05-10T09:01:00Z"), restored.getCreatedAt());
        assertEquals(Integer.valueOf(1200), restored.getRuntimeEvidence().get("p99Ms"));
    }

    private AccelerationCandidate sampleCandidate() {
        Map<String, Object> sourceEvidence = new LinkedHashMap<String, Object>();
        sourceEvidence.put("object", "orders");
        return AccelerationCandidate.builder()
            .candidateId("candidate-001")
            .tenantId("tenant-a")
            .sourceType(GovernanceSourceType.PARSE)
            .sourceKind(GovernanceSourceKind.STRUCTURE_PARSE)
            .sourceId("parse-task-001")
            .historyId("history-001")
            .parseHistoryId("parse-history-001")
            .parseTaskId("parse-task-001")
            .sqlFingerprint("fp-001")
            .datasourceCode("hetu_main")
            .stage("DEV")
            .reportCode("RPT_001")
            .candidateType(CandidateType.ACCELERATION_AND_REWRITE)
            .status(CandidateStatus.DRAFT)
            .confidence(new BigDecimal("0.7500"))
            .priority(Integer.valueOf(10))
            .evidenceLevel(EvidenceLevel.STATIC_PARSE)
            .schemaVersion("v1")
            .createdBy("operator-001")
            .createdAt(Instant.parse("2026-05-10T09:00:00Z"))
            .updatedAt(Instant.parse("2026-05-10T09:00:00Z"))
            .sourceEvidence(sourceEvidence)
            .issueEvidence(Collections.<String, Object>emptyMap())
            .runtimeEvidence(Collections.<String, Object>emptyMap())
            .benefitEstimate(Collections.<String, Object>emptyMap())
            .costEstimate(Collections.<String, Object>emptyMap())
            .risk(Collections.<String, Object>emptyMap())
            .build();
    }
}
