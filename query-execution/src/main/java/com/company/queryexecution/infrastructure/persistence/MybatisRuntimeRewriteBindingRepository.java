package com.company.queryexecution.infrastructure.persistence;

import com.company.queryexecution.domain.rewrite.RuntimeRewriteBinding;
import com.company.queryexecution.domain.rewrite.RuntimeRewriteBindingStatus;
import com.company.queryexecution.domain.rewrite.repository.RuntimeRewriteBindingRepository;
import com.company.queryexecution.infrastructure.persistence.entity.RuntimeRewriteBindingRecord;
import com.company.queryexecution.infrastructure.persistence.mapper.RuntimeRewriteBindingMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisRuntimeRewriteBindingRepository implements RuntimeRewriteBindingRepository {

    private static final ZoneOffset DATABASE_ZONE_OFFSET = ZoneOffset.UTC;

    private final RuntimeRewriteBindingMapper runtimeRewriteBindingMapper;

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
        record.setRecommendedSqlText(binding.getRecommendedSqlText());
        record.setDatasourceCode(binding.getDatasourceCode());
        record.setStatus(binding.getStatus().name());
        record.setRuleVersion(Long.valueOf(binding.getRuleVersion()));
        record.setRuntimeRuleVersion(binding.getRuntimeRuleVersion());
        record.setPublishedBy(binding.getPublishedBy());
        record.setPublishedAt(toLocalDateTime(binding.getPublishedAt()));
        record.setPausedBy(binding.getPausedBy());
        record.setPausedAt(toLocalDateTime(binding.getPausedAt()));
        record.setPauseReason(binding.getPauseReason());
        record.setUnpublishedBy(binding.getUnpublishedBy());
        record.setUnpublishedAt(toLocalDateTime(binding.getUnpublishedAt()));
        record.setUnpublishReason(binding.getUnpublishReason());
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
            .recommendedSqlText(record.getRecommendedSqlText())
            .datasourceCode(record.getDatasourceCode())
            .status(record.getStatus() == null ? null : RuntimeRewriteBindingStatus.valueOf(record.getStatus()))
            .ruleVersion(record.getRuleVersion() == null ? 1L : record.getRuleVersion().longValue())
            .runtimeRuleVersion(record.getRuntimeRuleVersion())
            .publishedBy(record.getPublishedBy())
            .publishedAt(toInstant(record.getPublishedAt()))
            .pausedBy(record.getPausedBy())
            .pausedAt(toInstant(record.getPausedAt()))
            .pauseReason(record.getPauseReason())
            .unpublishedBy(record.getUnpublishedBy())
            .unpublishedAt(toInstant(record.getUnpublishedAt()))
            .unpublishReason(record.getUnpublishReason())
            .createdAt(toInstant(record.getCreatedAt()))
            .updatedAt(toInstant(record.getUpdatedAt()))
            .build();
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, DATABASE_ZONE_OFFSET);
    }

    private Instant toInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(DATABASE_ZONE_OFFSET);
    }
}
