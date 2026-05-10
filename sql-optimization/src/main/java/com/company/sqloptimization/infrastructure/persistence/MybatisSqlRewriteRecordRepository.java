package com.company.sqloptimization.infrastructure.persistence;

import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.domain.governance.ComparisonStatus;
import com.company.sqloptimization.domain.governance.DifferenceType;
import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.domain.governance.RewriteAlertStatus;
import com.company.sqloptimization.domain.governance.RewriteRecordStatus;
import com.company.sqloptimization.domain.governance.RewriteValidationStatus;
import com.company.sqloptimization.domain.governance.ValidationRunStatus;
import com.company.sqloptimization.domain.rewrite.RewriteValidationRun;
import com.company.sqloptimization.domain.rewrite.SqlRewriteRecord;
import com.company.sqloptimization.domain.rewrite.repository.SqlRewriteRecordRepository;
import com.company.sqloptimization.infrastructure.persistence.entity.RewriteValidationRunRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.SqlRewriteRecordRecord;
import com.company.sqloptimization.infrastructure.persistence.mapper.RewriteValidationRunMapper;
import com.company.sqloptimization.infrastructure.persistence.mapper.SqlRewriteRecordMapper;
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
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(prefix = "sql-optimization.queues", name = "mode", havingValue = "database-worker")
public class MybatisSqlRewriteRecordRepository implements SqlRewriteRecordRepository {

    private static final ZoneOffset DATABASE_ZONE_OFFSET = ZoneOffset.UTC;

    private final SqlRewriteRecordMapper sqlRewriteRecordMapper;
    private final RewriteValidationRunMapper rewriteValidationRunMapper;

    public MybatisSqlRewriteRecordRepository(SqlRewriteRecordMapper sqlRewriteRecordMapper,
                                             RewriteValidationRunMapper rewriteValidationRunMapper) {
        this.sqlRewriteRecordMapper = sqlRewriteRecordMapper;
        this.rewriteValidationRunMapper = rewriteValidationRunMapper;
    }

    @Override
    public SqlRewriteRecord saveRecord(SqlRewriteRecord rewriteRecord) {
        SqlRewriteRecordRecord record = toRecord(rewriteRecord);
        if (sqlRewriteRecordMapper.selectByRewriteRecordId(rewriteRecord.getRewriteRecordId()) == null) {
            sqlRewriteRecordMapper.insert(record);
        } else {
            sqlRewriteRecordMapper.update(record);
        }
        return rewriteRecord;
    }

    @Override
    public SqlRewriteRecord findRecordById(String rewriteRecordId) {
        SqlRewriteRecordRecord record = sqlRewriteRecordMapper.selectByRewriteRecordId(rewriteRecordId);
        return record == null ? null : toDomain(record);
    }

    @Override
    public List<SqlRewriteRecord> findRecordsByTenantId(String tenantId) {
        List<SqlRewriteRecordRecord> records = sqlRewriteRecordMapper.selectByTenantId(tenantId);
        List<SqlRewriteRecord> result = new ArrayList<SqlRewriteRecord>(records.size());
        for (SqlRewriteRecordRecord record : records) {
            result.add(toDomain(record));
        }
        return result;
    }

    @Override
    public List<SqlRewriteRecord> findRecordsByTenantIdAndHistoryId(String tenantId, String historyId) {
        List<SqlRewriteRecordRecord> records =
            sqlRewriteRecordMapper.selectByTenantIdAndHistoryId(tenantId, historyId);
        List<SqlRewriteRecord> result = new ArrayList<SqlRewriteRecord>(records.size());
        for (SqlRewriteRecordRecord record : records) {
            result.add(toDomain(record));
        }
        return result;
    }

    @Override
    @Transactional
    public RewriteValidationRun saveValidationRun(RewriteValidationRun validationRun) {
        RewriteValidationRunRecord record = toRecord(validationRun);
        if (rewriteValidationRunMapper.selectByValidationRunId(validationRun.getValidationRunId()) == null) {
            rewriteValidationRunMapper.insert(record);
        } else {
            rewriteValidationRunMapper.update(record);
        }
        SqlRewriteRecord existing = findRecordById(validationRun.getRewriteRecordId());
        if (existing != null) {
            saveRecord(existing.withValidationSummary(validationRun, Instant.now()));
        }
        return validationRun;
    }

    @Override
    public List<RewriteValidationRun> findValidationRunsByRewriteRecordId(String rewriteRecordId) {
        List<RewriteValidationRunRecord> records = rewriteValidationRunMapper.selectByRewriteRecordId(rewriteRecordId);
        List<RewriteValidationRun> result = new ArrayList<RewriteValidationRun>(records.size());
        for (RewriteValidationRunRecord record : records) {
            result.add(toDomain(record));
        }
        return result;
    }

    private SqlRewriteRecordRecord toRecord(SqlRewriteRecord rewriteRecord) {
        SqlRewriteRecordRecord record = new SqlRewriteRecordRecord();
        record.setRewriteRecordId(rewriteRecord.getRewriteRecordId());
        record.setTenantId(rewriteRecord.getTenantId());
        record.setRecommendationId(rewriteRecord.getRecommendationId());
        record.setOptimizationTaskId(rewriteRecord.getOptimizationTaskId());
        record.setSourceType(rewriteRecord.getSourceType().name());
        record.setSourceKind(rewriteRecord.getSourceKind().name());
        record.setSourceId(rewriteRecord.getSourceId());
        record.setEvidenceLevel(rewriteRecord.getEvidenceLevel().name());
        record.setHistoryId(rewriteRecord.getHistoryId());
        record.setParseHistoryId(rewriteRecord.getParseHistoryId());
        record.setSqlFingerprint(rewriteRecord.getSqlFingerprint());
        record.setDatasourceCode(rewriteRecord.getDatasourceCode());
        record.setStatus(rewriteRecord.getStatus().name());
        record.setValidationStatus(rewriteRecord.getValidationStatus().name());
        record.setAutoApplyAllowed(Boolean.valueOf(rewriteRecord.isAutoApplyAllowed()));
        record.setManualReviewRequired(Boolean.valueOf(rewriteRecord.isManualReviewRequired()));
        record.setValidationPolicyId(rewriteRecord.getValidationPolicyId());
        record.setLastValidationRunId(rewriteRecord.getLastValidationRunId());
        record.setLastComparedAt(toLocalDateTime(rewriteRecord.getLastComparedAt()));
        record.setAlertStatus(rewriteRecord.getAlertStatus().name());
        record.setOriginalSqlText(rewriteRecord.getOriginalSqlText());
        record.setRecommendedSqlText(rewriteRecord.getRecommendedSqlText());
        record.setExecutedSqlText(rewriteRecord.getExecutedSqlText());
        record.setCreatedBy(rewriteRecord.getCreatedBy());
        record.setCreatedAt(toLocalDateTime(rewriteRecord.getCreatedAt()));
        record.setUpdatedAt(toLocalDateTime(rewriteRecord.getUpdatedAt()));
        record.setRuleChainJson(writeList(rewriteRecord.getRuleChain()));
        record.setDiffSummaryJson(writeMap(rewriteRecord.getDiffSummary()));
        record.setRiskJson(writeMap(rewriteRecord.getRisk()));
        record.setTraceRefsJson(writeMap(rewriteRecord.getTraceRefs()));
        return record;
    }

    private SqlRewriteRecord toDomain(SqlRewriteRecordRecord record) {
        return SqlRewriteRecord.builder()
            .rewriteRecordId(record.getRewriteRecordId())
            .tenantId(record.getTenantId())
            .recommendationId(record.getRecommendationId())
            .optimizationTaskId(record.getOptimizationTaskId())
            .sourceType(GovernanceSourceType.valueOf(record.getSourceType()))
            .sourceKind(GovernanceSourceKind.valueOf(record.getSourceKind()))
            .sourceId(record.getSourceId())
            .evidenceLevel(EvidenceLevel.valueOf(record.getEvidenceLevel()))
            .historyId(record.getHistoryId())
            .parseHistoryId(record.getParseHistoryId())
            .sqlFingerprint(record.getSqlFingerprint())
            .datasourceCode(record.getDatasourceCode())
            .status(record.getStatus() == null ? null : RewriteRecordStatus.valueOf(record.getStatus()))
            .validationStatus(record.getValidationStatus() == null
                ? null
                : RewriteValidationStatus.valueOf(record.getValidationStatus()))
            .autoApplyAllowed(Boolean.TRUE.equals(record.getAutoApplyAllowed()))
            .manualReviewRequired(Boolean.TRUE.equals(record.getManualReviewRequired()))
            .validationPolicyId(record.getValidationPolicyId())
            .lastValidationRunId(record.getLastValidationRunId())
            .lastComparedAt(toInstant(record.getLastComparedAt()))
            .alertStatus(record.getAlertStatus() == null ? null : RewriteAlertStatus.valueOf(record.getAlertStatus()))
            .originalSqlText(record.getOriginalSqlText())
            .recommendedSqlText(record.getRecommendedSqlText())
            .executedSqlText(record.getExecutedSqlText())
            .createdBy(record.getCreatedBy())
            .createdAt(toInstant(record.getCreatedAt()))
            .updatedAt(toInstant(record.getUpdatedAt()))
            .ruleChain(readList(record.getRuleChainJson()))
            .diffSummary(readMap(record.getDiffSummaryJson()))
            .risk(readMap(record.getRiskJson()))
            .traceRefs(readMap(record.getTraceRefsJson()))
            .build();
    }

    private RewriteValidationRunRecord toRecord(RewriteValidationRun validationRun) {
        RewriteValidationRunRecord record = new RewriteValidationRunRecord();
        record.setValidationRunId(validationRun.getValidationRunId());
        record.setTenantId(validationRun.getTenantId());
        record.setRewriteRecordId(validationRun.getRewriteRecordId());
        record.setRecommendationId(validationRun.getRecommendationId());
        record.setHistoryId(validationRun.getHistoryId());
        record.setSqlFingerprint(validationRun.getSqlFingerprint());
        record.setStatus(validationRun.getStatus().name());
        record.setComparisonStatus(validationRun.getComparisonStatus().name());
        record.setDifferenceType(validationRun.getDifferenceType().name());
        record.setAutoApplyPaused(Boolean.valueOf(validationRun.isAutoApplyPaused()));
        record.setStartedAt(toLocalDateTime(validationRun.getStartedAt()));
        record.setFinishedAt(toLocalDateTime(validationRun.getFinishedAt()));
        record.setComparisonPolicyJson(writeMap(validationRun.getComparisonPolicy()));
        record.setOriginalResultDigestJson(writeMap(validationRun.getOriginalResultDigest()));
        record.setRecommendedResultDigestJson(writeMap(validationRun.getRecommendedResultDigest()));
        record.setDifferenceSampleJson(writeMap(validationRun.getDifferenceSample()));
        record.setExecutionEvidenceJson(writeMap(validationRun.getExecutionEvidence()));
        return record;
    }

    private RewriteValidationRun toDomain(RewriteValidationRunRecord record) {
        return RewriteValidationRun.builder()
            .validationRunId(record.getValidationRunId())
            .tenantId(record.getTenantId())
            .rewriteRecordId(record.getRewriteRecordId())
            .recommendationId(record.getRecommendationId())
            .historyId(record.getHistoryId())
            .sqlFingerprint(record.getSqlFingerprint())
            .status(record.getStatus() == null ? null : ValidationRunStatus.valueOf(record.getStatus()))
            .comparisonStatus(record.getComparisonStatus() == null
                ? null
                : ComparisonStatus.valueOf(record.getComparisonStatus()))
            .differenceType(record.getDifferenceType() == null ? null : DifferenceType.valueOf(record.getDifferenceType()))
            .autoApplyPaused(Boolean.TRUE.equals(record.getAutoApplyPaused()))
            .startedAt(toInstant(record.getStartedAt()))
            .finishedAt(toInstant(record.getFinishedAt()))
            .comparisonPolicy(readMap(record.getComparisonPolicyJson()))
            .originalResultDigest(readMap(record.getOriginalResultDigestJson()))
            .recommendedResultDigest(readMap(record.getRecommendedResultDigestJson()))
            .differenceSample(readMap(record.getDifferenceSampleJson()))
            .executionEvidence(readMap(record.getExecutionEvidenceJson()))
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
            throw new IllegalArgumentException("Failed to deserialize SQL rewrite JSON", ex);
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
            throw new IllegalArgumentException("Failed to deserialize SQL rewrite list JSON", ex);
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, DATABASE_ZONE_OFFSET);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(DATABASE_ZONE_OFFSET);
    }
}
