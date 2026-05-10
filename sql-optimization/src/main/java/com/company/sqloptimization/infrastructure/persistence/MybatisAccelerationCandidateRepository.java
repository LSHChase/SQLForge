package com.company.sqloptimization.infrastructure.persistence;

import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.domain.candidate.AccelerationCandidate;
import com.company.sqloptimization.domain.candidate.repository.AccelerationCandidateRepository;
import com.company.sqloptimization.domain.governance.CandidateStatus;
import com.company.sqloptimization.domain.governance.CandidateType;
import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.infrastructure.persistence.entity.AccelerationCandidateRecord;
import com.company.sqloptimization.infrastructure.persistence.mapper.AccelerationCandidateMapper;
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
@ConditionalOnProperty(prefix = "sql-optimization.queues", name = "mode", havingValue = "database-worker")
public class MybatisAccelerationCandidateRepository implements AccelerationCandidateRepository {

    private static final ZoneOffset DATABASE_ZONE_OFFSET = ZoneOffset.UTC;

    private final AccelerationCandidateMapper accelerationCandidateMapper;

    public MybatisAccelerationCandidateRepository(AccelerationCandidateMapper accelerationCandidateMapper) {
        this.accelerationCandidateMapper = accelerationCandidateMapper;
    }

    @Override
    public AccelerationCandidate save(AccelerationCandidate candidate) {
        AccelerationCandidateRecord record = toRecord(candidate);
        if (accelerationCandidateMapper.selectByCandidateId(candidate.getCandidateId()) == null) {
            accelerationCandidateMapper.insert(record);
        } else {
            accelerationCandidateMapper.update(record);
        }
        return candidate;
    }

    @Override
    public AccelerationCandidate findByCandidateId(String candidateId) {
        AccelerationCandidateRecord record = accelerationCandidateMapper.selectByCandidateId(candidateId);
        return record == null ? null : toDomain(record);
    }

    @Override
    public List<AccelerationCandidate> findByTenantId(String tenantId) {
        List<AccelerationCandidateRecord> records = accelerationCandidateMapper.selectByTenantId(tenantId);
        List<AccelerationCandidate> result = new ArrayList<AccelerationCandidate>(records.size());
        for (AccelerationCandidateRecord record : records) {
            result.add(toDomain(record));
        }
        return result;
    }

    private AccelerationCandidateRecord toRecord(AccelerationCandidate candidate) {
        AccelerationCandidateRecord record = new AccelerationCandidateRecord();
        record.setCandidateId(candidate.getCandidateId());
        record.setTenantId(candidate.getTenantId());
        record.setSourceType(candidate.getSourceType().name());
        record.setSourceKind(candidate.getSourceKind().name());
        record.setSourceId(candidate.getSourceId());
        record.setHistoryId(candidate.getHistoryId());
        record.setParseHistoryId(candidate.getParseHistoryId());
        record.setParseTaskId(candidate.getParseTaskId());
        record.setBatchId(candidate.getBatchId());
        record.setBatchItemId(candidate.getBatchItemId());
        record.setBenchmarkTaskId(candidate.getBenchmarkTaskId());
        record.setOptimizationTaskId(candidate.getOptimizationTaskId());
        record.setSqlFingerprint(candidate.getSqlFingerprint());
        record.setDatasourceCode(candidate.getDatasourceCode());
        record.setStage(candidate.getStage());
        record.setReportCode(candidate.getReportCode());
        record.setCandidateType(candidate.getCandidateType().name());
        record.setStatus(candidate.getStatus().name());
        record.setConfidence(candidate.getConfidence());
        record.setPriority(candidate.getPriority());
        record.setEvidenceLevel(candidate.getEvidenceLevel().name());
        record.setSchemaVersion(candidate.getSchemaVersion());
        record.setCreatedBy(candidate.getCreatedBy());
        record.setCreatedAt(toLocalDateTime(candidate.getCreatedAt()));
        record.setUpdatedAt(toLocalDateTime(candidate.getUpdatedAt()));
        record.setSourceEvidenceJson(writeMap(candidate.getSourceEvidence()));
        record.setIssueEvidenceJson(writeMap(candidate.getIssueEvidence()));
        record.setRuntimeEvidenceJson(writeMap(candidate.getRuntimeEvidence()));
        record.setBenefitEstimateJson(writeMap(candidate.getBenefitEstimate()));
        record.setCostEstimateJson(writeMap(candidate.getCostEstimate()));
        record.setRiskJson(writeMap(candidate.getRisk()));
        return record;
    }

    private AccelerationCandidate toDomain(AccelerationCandidateRecord record) {
        return AccelerationCandidate.builder()
            .candidateId(record.getCandidateId())
            .tenantId(record.getTenantId())
            .sourceType(GovernanceSourceType.valueOf(record.getSourceType()))
            .sourceKind(GovernanceSourceKind.valueOf(record.getSourceKind()))
            .sourceId(record.getSourceId())
            .historyId(record.getHistoryId())
            .parseHistoryId(record.getParseHistoryId())
            .parseTaskId(record.getParseTaskId())
            .batchId(record.getBatchId())
            .batchItemId(record.getBatchItemId())
            .benchmarkTaskId(record.getBenchmarkTaskId())
            .optimizationTaskId(record.getOptimizationTaskId())
            .sqlFingerprint(record.getSqlFingerprint())
            .datasourceCode(record.getDatasourceCode())
            .stage(record.getStage())
            .reportCode(record.getReportCode())
            .candidateType(record.getCandidateType() == null ? null : CandidateType.valueOf(record.getCandidateType()))
            .status(record.getStatus() == null ? null : CandidateStatus.valueOf(record.getStatus()))
            .confidence(record.getConfidence())
            .priority(record.getPriority())
            .evidenceLevel(EvidenceLevel.valueOf(record.getEvidenceLevel()))
            .schemaVersion(record.getSchemaVersion())
            .createdBy(record.getCreatedBy())
            .createdAt(toInstant(record.getCreatedAt()))
            .updatedAt(toInstant(record.getUpdatedAt()))
            .sourceEvidence(readMap(record.getSourceEvidenceJson()))
            .issueEvidence(readMap(record.getIssueEvidenceJson()))
            .runtimeEvidence(readMap(record.getRuntimeEvidenceJson()))
            .benefitEstimate(readMap(record.getBenefitEstimateJson()))
            .costEstimate(readMap(record.getCostEstimateJson()))
            .risk(readMap(record.getRiskJson()))
            .build();
    }

    private String writeMap(Map<String, Object> value) {
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
            throw new IllegalArgumentException("Failed to deserialize acceleration candidate JSON", ex);
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, DATABASE_ZONE_OFFSET);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(DATABASE_ZONE_OFFSET);
    }
}
