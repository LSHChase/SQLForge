package com.company.sqloptimization.infrastructure.persistence;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.domain.task.OptimizationParseDepth;
import com.company.sqloptimization.domain.task.OptimizationTask;
import com.company.sqloptimization.domain.task.OptimizationTaskError;
import com.company.sqloptimization.domain.task.OptimizationTaskPhase;
import com.company.sqloptimization.domain.task.OptimizationTaskPriority;
import com.company.sqloptimization.domain.task.OptimizationTaskRisk;
import com.company.sqloptimization.domain.task.OptimizationTaskSourceContext;
import com.company.sqloptimization.domain.task.OptimizationTaskStatus;
import com.company.sqloptimization.domain.task.OptimizationTaskSubmission;
import com.company.sqloptimization.domain.task.OptimizationTaskType;
import com.company.sqloptimization.domain.task.repository.OptimizationTaskRepository;
import com.company.sqloptimization.infrastructure.persistence.entity.OptimizationTaskRecord;
import com.company.sqloptimization.infrastructure.persistence.mapper.OptimizationTaskMapper;
import com.company.sqlforge.common.utils.DateUtils;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "sql-optimization.queues", name = "mode", havingValue = "database-worker")
public class MybatisOptimizationTaskRepository implements OptimizationTaskRepository {

    private final OptimizationTaskMapper optimizationTaskMapper;
    private final OptimizationTaskJsonCodec jsonCodec;

    public MybatisOptimizationTaskRepository(OptimizationTaskMapper optimizationTaskMapper) {
        this.optimizationTaskMapper = optimizationTaskMapper;
        this.jsonCodec = new OptimizationTaskJsonCodec();
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
        record.setRequestedSuggestionTypesJson(jsonCodec.writeJson(task.getRequestedSuggestionTypes()));
        record.setTaskContextJson(jsonCodec.writeSourceContextJson(task.getSourceContext()));
        record.setStatus(task.getStatus().name());
        record.setCurrentPhase(task.getCurrentPhase().name());
        record.setProgressPercent(task.getProgressPercent());
        record.setSummary(task.getSummary());
        record.setSuggestionPayloadJson(jsonCodec.writeJson(task.getSuggestion()));
        if (task.getError() != null) {
            record.setErrorCode(Integer.valueOf(task.getError().getCode()));
            record.setErrorMessage(task.getError().getMessage());
            record.setErrorSuggestedAction(task.getError().getSuggestedAction());
            record.setErrorRetryable(Boolean.valueOf(task.getError().isRetryable()));
            record.setFailedPhase(task.getError().getFailedPhase() == null ? null : task.getError().getFailedPhase().name());
            record.setErrorRisksJson(jsonCodec.writeJson(task.getError().getRisks()));
        }
        record.setStatusHistoryJson(jsonCodec.writeJson(task.getStatusHistory()));
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
            jsonCodec.readSuggestionTypes(record.getRequestedSuggestionTypesJson()),
            jsonCodec.readSourceContext(record.getTaskContextJson())
        );
        return OptimizationTask.restore(
            record.getTaskId(),
            submission,
            toInstant(record.getSubmittedAt()),
            jsonCodec.readStatusHistory(record.getStatusHistoryJson()),
            OptimizationTaskStatus.valueOf(record.getStatus()),
            OptimizationTaskPhase.valueOf(record.getCurrentPhase()),
            record.getProgressPercent(),
            toInstant(record.getStartedAt()),
            toInstant(record.getFinishedAt()),
            record.getSummary(),
            jsonCodec.readSuggestion(record.getSuggestionPayloadJson()),
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
            jsonCodec.readFailedPhase(record.getFailedPhase()),
            jsonCodec.readRisks(record.getErrorRisksJson())
        );
    }

    private LocalDateTime toLocalDateTime(Instant value) {
        return DateUtils.toBeijingDateTime(value);
    }

    private Instant toInstant(LocalDateTime value) {
        return DateUtils.toInstant(value);
    }
}
