package com.company.sqloptimization.infrastructure.persistence;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import com.company.sqloptimization.domain.task.OptimizationParseDepth;
import com.company.sqloptimization.domain.task.OptimizationTask;
import com.company.sqloptimization.domain.task.OptimizationTaskError;
import com.company.sqloptimization.domain.task.OptimizationTaskPhase;
import com.company.sqloptimization.domain.task.OptimizationTaskPriority;
import com.company.sqloptimization.domain.task.OptimizationTaskRisk;
import com.company.sqloptimization.domain.task.OptimizationTaskSourceContext;
import com.company.sqloptimization.domain.task.OptimizationTaskStatus;
import com.company.sqloptimization.domain.task.OptimizationTaskStatusTransition;
import com.company.sqloptimization.domain.task.OptimizationTaskSubmission;
import com.company.sqloptimization.domain.task.OptimizationTaskSuggestion;
import com.company.sqloptimization.domain.task.OptimizationTaskType;
import com.company.sqloptimization.domain.task.repository.OptimizationTaskRepository;
import com.company.sqloptimization.infrastructure.persistence.entity.OptimizationTaskRecord;
import com.company.sqloptimization.infrastructure.persistence.mapper.OptimizationTaskMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class MybatisOptimizationTaskRepository implements OptimizationTaskRepository {

    private static final ZoneOffset DATABASE_ZONE_OFFSET = ZoneOffset.UTC;

    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS = new TypeReference<List<Map<String, Object>>>() {
    };
    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<List<String>>() {
    };
    private static final TypeReference<List<OptimizationTaskRisk>> LIST_OF_RISKS = new TypeReference<List<OptimizationTaskRisk>>() {
    };

    private final OptimizationTaskMapper optimizationTaskMapper;
    private final ObjectMapper objectMapper;

    public MybatisOptimizationTaskRepository(OptimizationTaskMapper optimizationTaskMapper) {
        this.optimizationTaskMapper = optimizationTaskMapper;
        this.objectMapper = JsonUtils.objectMapper();
    }

    @Override
    public OptimizationTask save(OptimizationTask task) {
        OptimizationTaskRecord record = toRecord(task);
        if (optimizationTaskMapper.selectByTaskId(task.getTaskId()) == null) {
            optimizationTaskMapper.insert(record);
        } else {
            optimizationTaskMapper.update(record);
        }
        return task;
    }

    @Override
    public OptimizationTask findByTaskId(String taskId) {
        OptimizationTaskRecord record = optimizationTaskMapper.selectByTaskId(taskId);
        return record == null ? null : toDomain(record);
    }

    @Override
    public List<OptimizationTask> findQueuedTasksSubmittedBefore(Instant cutoff) {
        if (cutoff == null) {
            return Collections.emptyList();
        }
        List<OptimizationTaskRecord> records =
            optimizationTaskMapper.selectQueuedTasksSubmittedBefore(toLocalDateTime(cutoff));
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<OptimizationTask> tasks = new ArrayList<OptimizationTask>(records.size());
        for (OptimizationTaskRecord record : records) {
            tasks.add(toDomain(record));
        }
        return tasks;
    }

    private OptimizationTaskRecord toRecord(OptimizationTask task) {
        OptimizationTaskRecord record = new OptimizationTaskRecord();
        record.setTaskId(task.getTaskId());
        record.setTenantId(task.getTenantId());
        record.setTaskType(task.getTaskType().name());
        record.setSqlText(task.getSqlText());
        record.setSqlFingerprint(task.getSqlFingerprint());
        record.setDatasourceType(task.getDatasourceType().name());
        record.setPriority(task.getPriority().name());
        record.setParseDepth(task.getParseDepth().name());
        record.setCallbackUrl(task.getCallbackUrl());
        record.setRequestedSuggestionTypesJson(writeJson(task.getRequestedSuggestionTypes()));
        record.setTaskContextJson(writeSourceContextJson(task.getSourceContext()));
        record.setStatus(task.getStatus().name());
        record.setCurrentPhase(task.getCurrentPhase().name());
        record.setProgressPercent(task.getProgressPercent());
        record.setSummary(task.getSummary());
        record.setSuggestionPayloadJson(writeJson(task.getSuggestion()));
        if (task.getError() != null) {
            record.setErrorCode(Integer.valueOf(task.getError().getCode()));
            record.setErrorMessage(task.getError().getMessage());
            record.setErrorSuggestedAction(task.getError().getSuggestedAction());
            record.setErrorRetryable(Boolean.valueOf(task.getError().isRetryable()));
            record.setFailedPhase(task.getError().getFailedPhase() == null ? null : task.getError().getFailedPhase().name());
            record.setErrorRisksJson(writeJson(task.getError().getRisks()));
        }
        record.setStatusHistoryJson(writeJson(task.getStatusHistory()));
        record.setSubmittedAt(toLocalDateTime(task.getSubmittedAt()));
        record.setStartedAt(toLocalDateTime(task.getStartedAt()));
        record.setFinishedAt(toLocalDateTime(task.getFinishedAt()));
        return record;
    }

    private OptimizationTask toDomain(OptimizationTaskRecord record) {
        OptimizationTaskSubmission submission = new OptimizationTaskSubmission(
            record.getTenantId(),
            OptimizationTaskType.valueOf(record.getTaskType()),
            record.getSqlText(),
            record.getSqlFingerprint(),
            DataSourceTypeEnum.valueOf(record.getDatasourceType()),
            OptimizationTaskPriority.valueOf(record.getPriority()),
            OptimizationParseDepth.valueOf(record.getParseDepth()),
            record.getCallbackUrl(),
            readSuggestionTypes(record.getRequestedSuggestionTypesJson()),
            readSourceContext(record.getTaskContextJson())
        );
        return OptimizationTask.restore(
            record.getTaskId(),
            submission,
            toInstant(record.getSubmittedAt()),
            readStatusHistory(record.getStatusHistoryJson()),
            OptimizationTaskStatus.valueOf(record.getStatus()),
            OptimizationTaskPhase.valueOf(record.getCurrentPhase()),
            record.getProgressPercent(),
            toInstant(record.getStartedAt()),
            toInstant(record.getFinishedAt()),
            record.getSummary(),
            readSuggestion(record.getSuggestionPayloadJson()),
            toError(record)
        );
    }

    private OptimizationTaskError toError(OptimizationTaskRecord record) {
        if (record.getErrorCode() == null) {
            return null;
        }
        return new OptimizationTaskError(
            record.getErrorCode().intValue(),
            record.getErrorMessage(),
            record.getErrorSuggestedAction(),
            Boolean.TRUE.equals(record.getErrorRetryable()),
            readFailedPhase(record.getFailedPhase()),
            readRisks(record.getErrorRisksJson())
        );
    }

    private OptimizationTaskSuggestion readSuggestion(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, OptimizationTaskSuggestion.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("优化建议载荷反序列化失败", ex);
        }
    }

    private List<AccelerationSuggestionType> readSuggestionTypes(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<String> values = objectMapper.readValue(json, LIST_OF_STRINGS);
            List<AccelerationSuggestionType> types = new ArrayList<AccelerationSuggestionType>(values.size());
            for (String value : values) {
                types.add(AccelerationSuggestionType.valueOf(value));
            }
            return types;
        } catch (Exception ex) {
            throw new IllegalArgumentException("优化建议类型反序列化失败", ex);
        }
    }

    private OptimizationTaskSourceContext readSourceContext(String json) {
        if (json == null || json.trim().isEmpty()) {
            return OptimizationTaskSourceContext.empty();
        }
        try {
            return objectMapper.readValue(json, OptimizationTaskSourceContext.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("优化任务上下文载荷反序列化失败", ex);
        }
    }

    private String writeSourceContextJson(OptimizationTaskSourceContext sourceContext) {
        if (sourceContext == null || sourceContext.isEmpty()) {
            return null;
        }
        return writeJson(sourceContext);
    }

    private List<OptimizationTaskStatusTransition> readStatusHistory(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> items = objectMapper.readValue(json, LIST_OF_MAPS);
            List<OptimizationTaskStatusTransition> history = new ArrayList<OptimizationTaskStatusTransition>(items.size());
            for (Map<String, Object> item : items) {
                history.add(
                    new OptimizationTaskStatusTransition(
                        readEnum(item.get("previousStatus"), OptimizationTaskStatus.class),
                        readEnum(item.get("currentStatus"), OptimizationTaskStatus.class),
                        readEnum(item.get("previousPhase"), OptimizationTaskPhase.class),
                        readEnum(item.get("currentPhase"), OptimizationTaskPhase.class),
                        item.get("occurredAt") == null ? null : Instant.parse(String.valueOf(item.get("occurredAt"))),
                        item.get("note") == null ? null : String.valueOf(item.get("note"))
                    )
                );
            }
            return history;
        } catch (Exception ex) {
            throw new IllegalArgumentException("优化任务状态历史反序列化失败", ex);
        }
    }

    private OptimizationTaskPhase readFailedPhase(String failedPhase) {
        if (failedPhase == null || failedPhase.trim().isEmpty()) {
            return null;
        }
        return OptimizationTaskPhase.valueOf(failedPhase);
    }

    private List<OptimizationTaskRisk> readRisks(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, LIST_OF_RISKS);
        } catch (Exception ex) {
            throw new IllegalArgumentException("优化任务风险反序列化失败", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("优化任务持久化载荷序列化失败", ex);
        }
    }

    private <T extends Enum<T>> T readEnum(Object value, Class<T> enumType) {
        if (value == null) {
            return null;
        }
        return Enum.valueOf(enumType, String.valueOf(value));
    }

    private LocalDateTime toLocalDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, DATABASE_ZONE_OFFSET);
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(DATABASE_ZONE_OFFSET);
    }
}
