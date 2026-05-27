package com.company.sqloptimization.infrastructure.persistence;

import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.domain.trial.RewriteTrialItem;
import com.company.sqloptimization.domain.trial.RewriteTrialRun;
import com.company.sqloptimization.domain.trial.RewriteTrialStatus;
import com.company.sqloptimization.domain.trial.repository.RewriteTrialRepository;
import com.company.sqloptimization.infrastructure.persistence.entity.RewriteTrialItemRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.RewriteTrialRunRecord;
import com.company.sqloptimization.infrastructure.persistence.mapper.RewriteTrialMapper;
import com.fasterxml.jackson.databind.JavaType;
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
@ConditionalOnProperty(prefix = "sql-optimization.rewrite-trial", name = "repository", havingValue = "database")
public class MybatisRewriteTrialRepository implements RewriteTrialRepository {

    private final RewriteTrialMapper rewriteTrialMapper;

    public MybatisRewriteTrialRepository(RewriteTrialMapper rewriteTrialMapper) {
        this.rewriteTrialMapper = rewriteTrialMapper;
    }

    @Override
    public RewriteTrialRun saveRun(RewriteTrialRun run) {
        RewriteTrialRunRecord record = toRunRecord(run);
        if (rewriteTrialMapper.selectRunById(run.getRunId()) == null) {
            rewriteTrialMapper.insertRun(record);
        } else {
            rewriteTrialMapper.updateRun(record);
        }
        return run;
    }

    @Override
    public RewriteTrialItem saveItem(RewriteTrialItem item) {
        RewriteTrialItemRecord record = toItemRecord(item);
        if (rewriteTrialMapper.selectItemById(item.getTrialItemId()) == null) {
            rewriteTrialMapper.insertItem(record);
        } else {
            rewriteTrialMapper.updateItem(record);
        }
        return item;
    }

    @Override
    public RewriteTrialRun findRunById(String runId) {
        RewriteTrialRunRecord record = rewriteTrialMapper.selectRunById(runId);
        return record == null ? null : toRunDomain(record);
    }

    @Override
    public List<RewriteTrialRun> findRunsByTenantId(String tenantId) {
        List<RewriteTrialRunRecord> records = rewriteTrialMapper.selectRunsByTenantId(tenantId);
        List<RewriteTrialRun> result = new ArrayList<RewriteTrialRun>(records.size());
        for (RewriteTrialRunRecord record : records) {
            result.add(toRunDomain(record));
        }
        return result;
    }

    @Override
    public RewriteTrialRun findLatestRunByBatchId(String tenantId, String batchId) {
        RewriteTrialRunRecord record = rewriteTrialMapper.selectLatestRunByBatchId(tenantId, batchId);
        return record == null ? null : toRunDomain(record);
    }

    @Override
    public List<RewriteTrialItem> findItemsByRunId(String runId) {
        return toItemDomains(rewriteTrialMapper.selectItemsByRunId(runId));
    }

    @Override
    public List<RewriteTrialItem> findItemsByTenantId(String tenantId) {
        return toItemDomains(rewriteTrialMapper.selectItemsByTenantId(tenantId));
    }

    private List<RewriteTrialItem> toItemDomains(List<RewriteTrialItemRecord> records) {
        List<RewriteTrialItem> result = new ArrayList<RewriteTrialItem>();
        for (RewriteTrialItemRecord record : records) {
            result.add(toItemDomain(record));
        }
        return result;
    }

    private RewriteTrialRunRecord toRunRecord(RewriteTrialRun run) {
        RewriteTrialRunRecord record = new RewriteTrialRunRecord();
        record.setRunId(run.getRunId());
        record.setTenantId(run.getTenantId());
        record.setSourceKind(run.getSourceKind());
        record.setSourceId(run.getSourceId());
        record.setBatchId(run.getBatchId());
        record.setStatus(run.getStatus().name());
        record.setTotalCount(Integer.valueOf(run.getTotalCount()));
        record.setAcceptedCount(Integer.valueOf(run.getAcceptedCount()));
        record.setSkippedCount(Integer.valueOf(run.getSkippedCount()));
        record.setCreatedBy(run.getCreatedBy());
        record.setCreatedAt(toLocalDateTime(run.getCreatedAt()));
        record.setUpdatedAt(toLocalDateTime(run.getUpdatedAt()));
        return record;
    }

    private RewriteTrialRun toRunDomain(RewriteTrialRunRecord record) {
        return RewriteTrialRun.builder()
            .runId(record.getRunId())
            .tenantId(record.getTenantId())
            .sourceKind(record.getSourceKind())
            .sourceId(record.getSourceId())
            .batchId(record.getBatchId())
            .status(record.getStatus() == null ? null : RewriteTrialStatus.valueOf(record.getStatus()))
            .totalCount(record.getTotalCount() == null ? 0 : record.getTotalCount().intValue())
            .acceptedCount(record.getAcceptedCount() == null ? 0 : record.getAcceptedCount().intValue())
            .skippedCount(record.getSkippedCount() == null ? 0 : record.getSkippedCount().intValue())
            .createdBy(record.getCreatedBy())
            .createdAt(toInstant(record.getCreatedAt()))
            .updatedAt(toInstant(record.getUpdatedAt()))
            .build();
    }

    private RewriteTrialItemRecord toItemRecord(RewriteTrialItem item) {
        RewriteTrialItemRecord record = new RewriteTrialItemRecord();
        record.setTrialItemId(item.getTrialItemId());
        record.setRunId(item.getRunId());
        record.setBatchItemId(item.getBatchItemId());
        record.setParseTaskId(item.getParseTaskId());
        record.setParseHistoryId(item.getParseHistoryId());
        record.setHistoryId(item.getHistoryId());
        record.setSqlFingerprint(item.getSqlFingerprint());
        record.setDatasourceCode(item.getDatasourceCode());
        record.setSourceSqlText(item.getSourceSqlText());
        record.setSourceProblemsJson(writeList(item.getSourceProblems()));
        record.setTaskId(item.getTaskId());
        record.setRecommendationId(item.getRecommendationId());
        record.setRewriteRecordId(item.getRewriteRecordId());
        record.setTrialStatus(item.getTrialStatus().name());
        record.setFailureReason(item.getFailureReason());
        record.setCandidateSql(item.getCandidateSql());
        record.setValidationStatus(item.getValidationStatus());
        record.setIssueRuleLinksJson(writeList(item.getIssueRuleLinks()));
        record.setCreatedAt(toLocalDateTime(item.getCreatedAt()));
        record.setUpdatedAt(toLocalDateTime(item.getUpdatedAt()));
        return record;
    }

    private RewriteTrialItem toItemDomain(RewriteTrialItemRecord record) {
        return RewriteTrialItem.builder()
            .trialItemId(record.getTrialItemId())
            .runId(record.getRunId())
            .batchItemId(record.getBatchItemId())
            .parseTaskId(record.getParseTaskId())
            .parseHistoryId(record.getParseHistoryId())
            .historyId(record.getHistoryId())
            .sqlFingerprint(record.getSqlFingerprint())
            .datasourceCode(record.getDatasourceCode())
            .sourceSqlText(record.getSourceSqlText())
            .sourceProblems(readList(record.getSourceProblemsJson()))
            .taskId(record.getTaskId())
            .recommendationId(record.getRecommendationId())
            .rewriteRecordId(record.getRewriteRecordId())
            .trialStatus(record.getTrialStatus() == null ? null : RewriteTrialStatus.valueOf(record.getTrialStatus()))
            .failureReason(record.getFailureReason())
            .candidateSql(record.getCandidateSql())
            .validationStatus(record.getValidationStatus())
            .issueRuleLinks(readList(record.getIssueRuleLinksJson()))
            .createdAt(toInstant(record.getCreatedAt()))
            .updatedAt(toInstant(record.getUpdatedAt()))
            .build();
    }

    private String writeList(List<Map<String, Object>> value) {
        return value == null || value.isEmpty() ? null : JsonUtils.toJson(value);
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
            throw new IllegalArgumentException("改写试算 JSON 反序列化失败", ex);
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return DateUtils.toBeijingDateTime(instant);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return DateUtils.toInstant(localDateTime);
    }
}
