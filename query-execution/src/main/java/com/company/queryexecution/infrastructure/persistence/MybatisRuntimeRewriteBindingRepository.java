package com.company.queryexecution.infrastructure.persistence;

import com.company.queryexecution.domain.rewrite.RuntimeRewriteBinding;
import com.company.queryexecution.domain.rewrite.RuntimeRewriteBindingStatus;
import com.company.queryexecution.domain.rewrite.repository.RuntimeRewriteBindingRepository;
import com.company.queryexecution.infrastructure.persistence.entity.RuntimeRewriteBindingRecord;
import com.company.queryexecution.infrastructure.persistence.mapper.RuntimeRewriteBindingMapper;
import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import com.company.sqlforge.common.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.company.sqlforge.common.utils.DateUtils;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class MybatisRuntimeRewriteBindingRepository implements RuntimeRewriteBindingRepository {
    private static final TypeReference<List<LogicalObjectSurface>> SURFACE_LIST_TYPE =
        new TypeReference<List<LogicalObjectSurface>>() {
        };
    private static final TypeReference<List<String>> STRING_LIST_TYPE =
        new TypeReference<List<String>>() {
        };

    private final RuntimeRewriteBindingMapper runtimeRewriteBindingMapper;
    private final ObjectMapper objectMapper = JsonUtils.objectMapper();

    public MybatisRuntimeRewriteBindingRepository(RuntimeRewriteBindingMapper runtimeRewriteBindingMapper) {
        this.runtimeRewriteBindingMapper = runtimeRewriteBindingMapper;
    }

    @Override
    public RuntimeRewriteBinding save(RuntimeRewriteBinding binding) {
        RuntimeRewriteBindingRecord record = toRecord(binding);
        if (runtimeRewriteBindingMapper.selectByRuntimeBindingId(binding.getRuntimeBindingId()) == null) {
            runtimeRewriteBindingMapper.insert(record);
        } else {
            runtimeRewriteBindingMapper.update(record);
        }
        return binding;
    }

    @Override
    public RuntimeRewriteBinding findByRuntimeBindingId(String runtimeBindingId) {
        RuntimeRewriteBindingRecord record = runtimeRewriteBindingMapper.selectByRuntimeBindingId(runtimeBindingId);
        return record == null ? null : toDomain(record);
    }

    @Override
    public RuntimeRewriteBinding findActiveByTenantIdAndSqlFingerprint(String tenantId, String sqlFingerprint) {
        RuntimeRewriteBindingRecord record =
            runtimeRewriteBindingMapper.selectActiveByTenantIdAndSqlFingerprint(tenantId, sqlFingerprint);
        return record == null ? null : toDomain(record);
    }

    @Override
    public RuntimeRewriteBinding findLatestByTenantIdAndSqlFingerprint(String tenantId, String sqlFingerprint) {
        RuntimeRewriteBindingRecord record =
            runtimeRewriteBindingMapper.selectLatestByTenantIdAndSqlFingerprint(tenantId, sqlFingerprint);
        return record == null ? null : toDomain(record);
    }

    @Override
    public List<RuntimeRewriteBinding> findByTenantIdAndSqlFingerprint(String tenantId, String sqlFingerprint) {
        List<RuntimeRewriteBindingRecord> records =
            runtimeRewriteBindingMapper.selectByTenantIdAndSqlFingerprint(tenantId, sqlFingerprint);
        List<RuntimeRewriteBinding> bindings = new ArrayList<RuntimeRewriteBinding>(records.size());
        for (RuntimeRewriteBindingRecord record : records) {
            bindings.add(toDomain(record));
        }
        return bindings;
    }

    @Override
    public List<RuntimeRewriteBinding> findActiveByTenantId(String tenantId) {
        List<RuntimeRewriteBindingRecord> records = runtimeRewriteBindingMapper.selectActiveByTenantId(tenantId);
        List<RuntimeRewriteBinding> bindings = new ArrayList<RuntimeRewriteBinding>(records.size());
        for (RuntimeRewriteBindingRecord record : records) {
            bindings.add(toDomain(record));
        }
        return bindings;
    }

    private RuntimeRewriteBindingRecord toRecord(RuntimeRewriteBinding binding) {
        RuntimeRewriteBindingRecord record = new RuntimeRewriteBindingRecord();
        record.setRuntimeBindingId(binding.getRuntimeBindingId());
        record.setTenantId(binding.getTenantId());
        record.setRewriteRecordId(binding.getRewriteRecordId());
        record.setRecommendationId(binding.getRecommendationId());
        record.setSourceType(binding.getSourceType());
        record.setSourceKind(binding.getSourceKind());
        record.setSourceId(binding.getSourceId());
        record.setSqlFingerprint(binding.getSqlFingerprint());
        record.setOriginalSqlDigest(binding.getOriginalSqlDigest());
        record.setOriginalSqlText(binding.getOriginalSqlText());
        record.setRecommendedSqlText(binding.getRecommendedSqlText());
        record.setRewriteMatchMode(binding.getRewriteMatchMode());
        record.setRewriteProgramJson(binding.getRewriteProgramJson());
        record.setTemplateFamilyFingerprint(binding.getTemplateFamilyFingerprint());
        record.setRuntimeMatchObjectRefsJson(toJson(binding.getRuntimeMatchObjectRefs()));
        record.setRuntimeMatchObjectNamesJson(toJson(binding.getRuntimeMatchObjectNames()));
        record.setAnalysisPhysicalObjectRefsJson(toJson(binding.getAnalysisPhysicalObjectRefs()));
        record.setMetadataSnapshotVersion(binding.getMetadataSnapshotVersion());
        record.setViewDefinitionHash(binding.getViewDefinitionHash());
        record.setMetadataDegradationReason(binding.getMetadataDegradationReason());
        record.setDatasourceCode(binding.getDatasourceCode());
        record.setStatus(binding.getStatus().name());
        record.setRuleVersion(Long.valueOf(binding.getRuleVersion()));
        record.setRuntimeRuleVersion(binding.getRuntimeRuleVersion());
        record.setActivatedBy(binding.getActivatedBy());
        record.setActivatedAt(toLocalDateTime(binding.getActivatedAt()));
        record.setPausedBy(binding.getPausedBy());
        record.setPausedAt(toLocalDateTime(binding.getPausedAt()));
        record.setPauseReason(binding.getPauseReason());
        record.setCreatedAt(toLocalDateTime(binding.getCreatedAt()));
        record.setUpdatedAt(toLocalDateTime(binding.getUpdatedAt()));
        return record;
    }

    private RuntimeRewriteBinding toDomain(RuntimeRewriteBindingRecord record) {
        return RuntimeRewriteBinding.builder()
            .runtimeBindingId(record.getRuntimeBindingId())
            .tenantId(record.getTenantId())
            .rewriteRecordId(record.getRewriteRecordId())
            .recommendationId(record.getRecommendationId())
            .sourceType(record.getSourceType())
            .sourceKind(record.getSourceKind())
            .sourceId(record.getSourceId())
            .sqlFingerprint(record.getSqlFingerprint())
            .originalSqlDigest(record.getOriginalSqlDigest())
            .originalSqlText(record.getOriginalSqlText())
            .recommendedSqlText(record.getRecommendedSqlText())
            .rewriteMatchMode(record.getRewriteMatchMode())
            .rewriteProgramJson(record.getRewriteProgramJson())
            .templateFamilyFingerprint(record.getTemplateFamilyFingerprint())
            .runtimeMatchObjectRefs(readSurfaces(record.getRuntimeMatchObjectRefsJson()))
            .runtimeMatchObjectNames(readStrings(record.getRuntimeMatchObjectNamesJson()))
            .analysisPhysicalObjectRefs(readSurfaces(record.getAnalysisPhysicalObjectRefsJson()))
            .metadataSnapshotVersion(record.getMetadataSnapshotVersion())
            .viewDefinitionHash(record.getViewDefinitionHash())
            .metadataDegradationReason(record.getMetadataDegradationReason())
            .datasourceCode(record.getDatasourceCode())
            .status(record.getStatus() == null ? null : RuntimeRewriteBindingStatus.valueOf(record.getStatus()))
            .ruleVersion(record.getRuleVersion() == null ? 1L : record.getRuleVersion().longValue())
            .runtimeRuleVersion(record.getRuntimeRuleVersion())
            .activatedBy(record.getActivatedBy())
            .activatedAt(toInstant(record.getActivatedAt()))
            .pausedBy(record.getPausedBy())
            .pausedAt(toInstant(record.getPausedAt()))
            .pauseReason(record.getPauseReason())
            .createdAt(toInstant(record.getCreatedAt()))
            .updatedAt(toInstant(record.getUpdatedAt()))
            .build();
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return DateUtils.toBeijingDateTime(instant);
    }

    private Instant toInstant(LocalDateTime dateTime) {
        return DateUtils.toInstant(dateTime);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List && ((List<?>) value).isEmpty()) {
            return null;
        }
        return JsonUtils.toJson(value);
    }

    private List<LogicalObjectSurface> readSurfaces(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, SURFACE_LIST_TYPE);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private List<String> readStrings(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }
}
