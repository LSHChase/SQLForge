package com.company.sqloptimization.infrastructure.persistence;

import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.domain.plan.AccelerationPlan;
import com.company.sqloptimization.domain.plan.AccelerationPlanStatus;
import com.company.sqloptimization.domain.plan.AccelerationPlanStatusTransition;
import com.company.sqloptimization.domain.plan.repository.AccelerationPlanRepository;
import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import com.company.sqloptimization.domain.task.OptimizationTaskBenefit;
import com.company.sqloptimization.domain.task.OptimizationTaskCost;
import com.company.sqloptimization.domain.task.OptimizationTaskRisk;
import com.company.sqloptimization.infrastructure.persistence.entity.AccelerationPlanRecord;
import com.company.sqloptimization.infrastructure.persistence.mapper.AccelerationPlanMapper;
import com.fasterxml.jackson.databind.JavaType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;

@Repository
@ConditionalOnProperty(prefix = "sql-optimization.queues", name = "mode", havingValue = "database-worker")
public class MybatisAccelerationPlanRepository implements AccelerationPlanRepository {

    private static final ZoneOffset DATABASE_ZONE_OFFSET = ZoneOffset.UTC;

    private final AccelerationPlanMapper accelerationPlanMapper;

    public MybatisAccelerationPlanRepository(AccelerationPlanMapper accelerationPlanMapper) {
        this.accelerationPlanMapper = accelerationPlanMapper;
    }

    @Override
    public AccelerationPlan save(AccelerationPlan accelerationPlan) {
        AccelerationPlanRecord record = toRecord(accelerationPlan);
        if (accelerationPlanMapper.selectByPlanId(accelerationPlan.getPlanId()) == null) {
            accelerationPlanMapper.insert(record);
        } else {
            accelerationPlanMapper.update(record);
        }
        return accelerationPlan;
    }

    @Override
    public AccelerationPlan findByPlanId(String planId) {
        AccelerationPlanRecord record = accelerationPlanMapper.selectByPlanId(planId);
        if (record == null) {
            return null;
        }
        return toDomain(record);
    }

    private AccelerationPlanRecord toRecord(AccelerationPlan plan) {
        AccelerationPlanRecord record = new AccelerationPlanRecord();
        record.setPlanId(plan.getPlanId());
        record.setTenantId(plan.getTenantId());
        record.setSourceTaskId(plan.getSourceTaskId());
        record.setSqlText(plan.getSqlText());
        record.setSqlFingerprint(plan.getSqlFingerprint());
        record.setDatasourceType(plan.getDatasourceType().name());
        record.setSelectedSuggestionTypesJson(JsonUtils.toJson(plan.getSelectedSuggestionTypes()));
        record.setPlanStatus(plan.getStatus().name());
        record.setPlanSummary(plan.getPlanSummary());
        record.setPrimaryRecommendation(plan.getPrimaryRecommendation());
        record.setPlanPayloadJson(plan.getPlanPayloadJson());
        record.setBenefitsJson(JsonUtils.toJson(plan.getBenefits()));
        record.setCostsJson(JsonUtils.toJson(plan.getCosts()));
        record.setRisksJson(JsonUtils.toJson(plan.getRisks()));
        record.setConfigSnapshotId(plan.getConfigSnapshotId());
        record.setResultId(plan.getResultId());
        record.setHistoryId(plan.getHistoryId());
        record.setLastErrorCode(plan.getLastErrorCode());
        record.setLastErrorMessage(plan.getLastErrorMessage());
        record.setActivationEvidenceJson(plan.getActivationEvidenceJson());
        record.setActivatedAt(toLocalDateTime(plan.getActivatedAt()));
        record.setActivatedBy(plan.getActivatedBy());
        record.setPauseEvidenceJson(plan.getPauseEvidenceJson());
        record.setPausedAt(toLocalDateTime(plan.getPausedAt()));
        record.setPausedBy(plan.getPausedBy());
        record.setStatusHistoryJson(JsonUtils.toJson(plan.getStatusHistory()));
        record.setCreatedAt(toLocalDateTime(plan.getCreatedAt()));
        record.setUpdatedAt(toLocalDateTime(plan.getUpdatedAt()));
        return record;
    }

    private AccelerationPlan toDomain(AccelerationPlanRecord record) {
        return AccelerationPlan.restore(
            record.getPlanId(),
            record.getTenantId(),
            record.getSourceTaskId(),
            record.getSqlText(),
            record.getSqlFingerprint(),
            DataSourceTypeEnum.valueOf(record.getDatasourceType()),
            readList(record.getSelectedSuggestionTypesJson(), AccelerationSuggestionType.class),
            record.getPlanSummary(),
            record.getPrimaryRecommendation(),
            record.getPlanPayloadJson(),
            readList(record.getBenefitsJson(), OptimizationTaskBenefit.class),
            readList(record.getCostsJson(), OptimizationTaskCost.class),
            readList(record.getRisksJson(), OptimizationTaskRisk.class),
            toInstant(record.getCreatedAt()),
            readList(record.getStatusHistoryJson(), AccelerationPlanStatusTransition.class),
            record.getConfigSnapshotId(),
            record.getResultId(),
            record.getHistoryId(),
            record.getPlanStatus() == null ? null : AccelerationPlanStatus.valueOf(record.getPlanStatus()),
            record.getLastErrorCode(),
            record.getLastErrorMessage(),
            record.getActivationEvidenceJson(),
            toInstant(record.getActivatedAt()),
            record.getActivatedBy(),
            record.getPauseEvidenceJson(),
            toInstant(record.getPausedAt()),
            record.getPausedBy(),
            toInstant(record.getUpdatedAt())
        );
    }

    private <T> List<T> readList(String json, Class<T> itemType) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            JavaType type = JsonUtils.objectMapper().getTypeFactory().constructCollectionType(List.class, itemType);
            return JsonUtils.objectMapper().readValue(json, type);
        } catch (Exception ex) {
            throw new IllegalArgumentException("加速方案 JSON 反序列化失败", ex);
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, DATABASE_ZONE_OFFSET);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(DATABASE_ZONE_OFFSET);
    }
}
