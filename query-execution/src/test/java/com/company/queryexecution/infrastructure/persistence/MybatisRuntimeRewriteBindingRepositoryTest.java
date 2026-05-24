package com.company.queryexecution.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.queryexecution.domain.rewrite.RuntimeRewriteBinding;
import com.company.queryexecution.domain.rewrite.RuntimeRewriteBindingStatus;
import com.company.queryexecution.infrastructure.persistence.entity.RuntimeRewriteBindingRecord;
import com.company.queryexecution.infrastructure.persistence.mapper.RuntimeRewriteBindingMapper;
import com.company.sqlforge.common.logicalobject.SqlSurfaceObjectRefExtractor;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MybatisRuntimeRewriteBindingRepositoryTest {

    @Test
    void shouldPersistRuntimeRewriteBindingWithVersionAndLifecycleFields() {
        RuntimeRewriteBindingMapper mapper = org.mockito.Mockito.mock(RuntimeRewriteBindingMapper.class);
        when(mapper.selectByRuntimeBindingId(anyString())).thenReturn(null);
        MybatisRuntimeRewriteBindingRepository repository = new MybatisRuntimeRewriteBindingRepository(mapper);

        repository.save(sampleBinding());

        ArgumentCaptor<RuntimeRewriteBindingRecord> captor =
            ArgumentCaptor.forClass(RuntimeRewriteBindingRecord.class);
        verify(mapper).insert(captor.capture());
        RuntimeRewriteBindingRecord record = captor.getValue();
        assertEquals("rwb-001", record.getRuntimeBindingId());
        assertEquals("tenant-a", record.getTenantId());
        assertEquals("rewrite-001", record.getRewriteRecordId());
        assertEquals("QUERY_HISTORY", record.getSourceKind());
        assertEquals("digest-original-001", record.getOriginalSqlDigest());
        assertEquals("SELECT * FROM orders WHERE tenant_id = 1", record.getOriginalSqlText());
        assertEquals("SELECT id FROM orders", record.getRecommendedSqlText());
        assertEquals("TEMPLATE_CONDITION_REPLAY", record.getRewriteMatchMode());
        assertEquals("{\"programVersion\":\"template-replay-v1\"}", record.getRewriteProgramJson());
        assertEquals("family-001", record.getTemplateFamilyFingerprint());
        assertNotNull(record.getRuntimeMatchObjectNamesJson());
        assertTrue(record.getRuntimeMatchObjectNamesJson().contains("orders"));
        assertNotNull(record.getAnalysisPhysicalObjectRefsJson());
        assertEquals("metadata-v1", record.getMetadataSnapshotVersion());
        assertEquals("view-hash-001", record.getViewDefinitionHash());
        assertEquals("ACTIVE", record.getStatus());
        assertEquals(Long.valueOf(3), record.getRuleVersion());
        assertEquals("runtime-rewrite-v3", record.getRuntimeRuleVersion());
        assertEquals(LocalDateTime.of(2026, 5, 11, 14, 0), record.getActivatedAt());
    }

    @Test
    void shouldRestorePausedRuntimeRewriteBindingFromRecord() {
        RuntimeRewriteBindingMapper mapper = org.mockito.Mockito.mock(RuntimeRewriteBindingMapper.class);
        when(mapper.selectByRuntimeBindingId("rwb-002")).thenReturn(sampleRecord());
        MybatisRuntimeRewriteBindingRepository repository = new MybatisRuntimeRewriteBindingRepository(mapper);

        RuntimeRewriteBinding binding = repository.findByRuntimeBindingId("rwb-002");

        assertNotNull(binding);
        assertEquals(RuntimeRewriteBindingStatus.PAUSED, binding.getStatus());
        assertEquals(4L, binding.getRuleVersion());
        assertEquals("runtime-rewrite-v4", binding.getRuntimeRuleVersion());
        assertEquals("TEMPLATE_CONDITION_REPLAY", binding.getRewriteMatchMode());
        assertEquals("family-002", binding.getTemplateFamilyFingerprint());
        assertEquals("orders", binding.getRuntimeMatchObjectNames().get(0));
        assertEquals("metadata-v2", binding.getMetadataSnapshotVersion());
        assertEquals("view-hash-002", binding.getViewDefinitionHash());
        assertEquals("validation divergence", binding.getPauseReason());
        assertEquals(Instant.parse("2026-05-11T14:05:00Z"), binding.getPausedAt());
    }

    private RuntimeRewriteBinding sampleBinding() {
        return RuntimeRewriteBinding.builder()
            .runtimeBindingId("rwb-001")
            .tenantId("tenant-a")
            .rewriteRecordId("rewrite-001")
            .recommendationId("recommendation-001")
            .sourceType("QUERY")
            .sourceKind("QUERY_HISTORY")
            .sourceId("history-001")
            .sqlFingerprint("fp-001")
            .originalSqlDigest("digest-original-001")
            .originalSqlText("SELECT * FROM orders WHERE tenant_id = 1")
            .recommendedSqlText("SELECT id FROM orders")
            .rewriteMatchMode("TEMPLATE_CONDITION_REPLAY")
            .rewriteProgramJson("{\"programVersion\":\"template-replay-v1\"}")
            .templateFamilyFingerprint("family-001")
            .runtimeMatchObjectRefs(SqlSurfaceObjectRefExtractor.extractSurfaceRefs("SELECT * FROM orders"))
            .runtimeMatchObjectNames(SqlSurfaceObjectRefExtractor.extractSurfaceObjectNames("SELECT * FROM orders"))
            .analysisPhysicalObjectRefs(SqlSurfaceObjectRefExtractor.extractSurfaceRefs("SELECT * FROM orders_base"))
            .metadataSnapshotVersion("metadata-v1")
            .viewDefinitionHash("view-hash-001")
            .datasourceCode("hetu_main")
            .ruleVersion(3L)
            .runtimeRuleVersion("runtime-rewrite-v3")
            .activatedBy("publisher-001")
            .activatedAt(Instant.parse("2026-05-11T14:00:00Z"))
            .createdAt(Instant.parse("2026-05-11T14:00:00Z"))
            .updatedAt(Instant.parse("2026-05-11T14:00:00Z"))
            .build();
    }

    private RuntimeRewriteBindingRecord sampleRecord() {
        RuntimeRewriteBindingRecord record = new RuntimeRewriteBindingRecord();
        record.setRuntimeBindingId("rwb-002");
        record.setTenantId("tenant-a");
        record.setRewriteRecordId("rewrite-002");
        record.setRecommendationId("recommendation-002");
        record.setSourceType("QUERY");
        record.setSourceKind("QUERY_HISTORY");
        record.setSourceId("history-002");
        record.setSqlFingerprint("fp-002");
        record.setOriginalSqlDigest("digest-original-002");
        record.setOriginalSqlText("SELECT * FROM orders WHERE query_date = ?");
        record.setRecommendedSqlText("SELECT id FROM orders WHERE query_date = ?");
        record.setRewriteMatchMode("TEMPLATE_CONDITION_REPLAY");
        record.setRewriteProgramJson("{\"programVersion\":\"template-replay-v1\"}");
        record.setTemplateFamilyFingerprint("family-002");
        record.setRuntimeMatchObjectRefsJson("[{\"objectType\":\"TABLE\",\"objectName\":\"orders\",\"objectKey\":\"TABLE:orders\"}]");
        record.setRuntimeMatchObjectNamesJson("[\"orders\"]");
        record.setAnalysisPhysicalObjectRefsJson("[{\"objectType\":\"TABLE\",\"objectName\":\"orders_base\",\"objectKey\":\"TABLE:orders_base\"}]");
        record.setMetadataSnapshotVersion("metadata-v2");
        record.setViewDefinitionHash("view-hash-002");
        record.setDatasourceCode("hetu_main");
        record.setStatus("PAUSED");
        record.setRuleVersion(Long.valueOf(4));
        record.setRuntimeRuleVersion("runtime-rewrite-v4");
        record.setActivatedBy("publisher-002");
        record.setActivatedAt(LocalDateTime.of(2026, 5, 11, 14, 0));
        record.setPausedBy("validator");
        record.setPausedAt(LocalDateTime.of(2026, 5, 11, 14, 5));
        record.setPauseReason("validation divergence");
        record.setCreatedAt(LocalDateTime.of(2026, 5, 11, 14, 0));
        record.setUpdatedAt(LocalDateTime.of(2026, 5, 11, 14, 5));
        return record;
    }
}
