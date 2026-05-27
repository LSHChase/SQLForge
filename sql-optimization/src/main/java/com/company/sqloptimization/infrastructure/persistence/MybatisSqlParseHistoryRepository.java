package com.company.sqloptimization.infrastructure.persistence;

import com.company.sqloptimization.domain.parsehistory.SqlParseHistory;
import com.company.sqloptimization.domain.parsehistory.SqlParseHistoryFilter;
import com.company.sqloptimization.domain.parsehistory.repository.SqlParseHistoryRepository;
import com.company.sqloptimization.infrastructure.persistence.entity.SqlParseHistoryRecord;
import com.company.sqloptimization.infrastructure.persistence.mapper.SqlParseHistoryMapper;
import com.company.sqlforge.common.utils.DateUtils;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "sql-optimization.parse-history", name = "repository", havingValue = "database")
public class MybatisSqlParseHistoryRepository implements SqlParseHistoryRepository {

    private final SqlParseHistoryMapper sqlParseHistoryMapper;

    public MybatisSqlParseHistoryRepository(SqlParseHistoryMapper sqlParseHistoryMapper) {
        this.sqlParseHistoryMapper = sqlParseHistoryMapper;
    }

    @Override
    public SqlParseHistory save(SqlParseHistory history) {
        SqlParseHistoryRecord record = toRecord(history);
        if (sqlParseHistoryMapper.selectByParseHistoryId(history.getParseHistoryId()) == null) {
            sqlParseHistoryMapper.insert(record);
        } else {
            sqlParseHistoryMapper.update(record);
        }
        return history;
    }

    @Override
    public SqlParseHistory findByParseHistoryId(String parseHistoryId) {
        SqlParseHistoryRecord record = sqlParseHistoryMapper.selectByParseHistoryId(parseHistoryId);
        return record == null ? null : toDomain(record);
    }

    @Override
    public SqlParseHistory findByBatchKeyAndSqlFingerprint(String batchKey, String sqlFingerprint) {
        SqlParseHistoryRecord record = sqlParseHistoryMapper.selectByBatchKeyAndSqlFingerprint(batchKey, sqlFingerprint);
        return record == null ? null : toDomain(record);
    }

    @Override
    public List<SqlParseHistory> findPage(SqlParseHistoryFilter filter) {
        List<SqlParseHistoryRecord> records = sqlParseHistoryMapper.selectPage(filter);
        List<SqlParseHistory> result = new ArrayList<SqlParseHistory>(records.size());
        for (SqlParseHistoryRecord record : records) {
            result.add(toDomain(record));
        }
        return result;
    }

    @Override
    public int count(SqlParseHistoryFilter filter) {
        return sqlParseHistoryMapper.count(filter);
    }

    private SqlParseHistoryRecord toRecord(SqlParseHistory history) {
        SqlParseHistoryRecord record = new SqlParseHistoryRecord();
        record.setParseHistoryId(history.getParseHistoryId());
        record.setTenantId(history.getTenantId());
        record.setSourceType(history.getSourceType());
        record.setSourceId(history.getSourceId());
        record.setBatchKey(history.getBatchKey());
        record.setParseTaskId(history.getParseTaskId());
        record.setSqlFingerprint(history.getSqlFingerprint());
        record.setDatasourceCode(history.getDatasourceCode());
        record.setDatasourceType(history.getDatasourceType());
        record.setReportCode(history.getReportCode());
        record.setStageCode(history.getStageCode());
        record.setBizDate(history.getBizDate());
        record.setQueryDateStart(history.getQueryDateStart());
        record.setQueryDateEnd(history.getQueryDateEnd());
        record.setQueryDateStatus(history.getQueryDateStatus());
        record.setAccessChannel(history.getAccessChannel());
        record.setParserMode(history.getParserMode());
        record.setSqlText(history.getSqlText());
        record.setSqlTemplateText(history.getSqlTemplateText());
        record.setBindingMode(history.getBindingMode());
        record.setParameterizedSqlFlag(history.getParameterizedSqlFlag());
        record.setResultStatus(history.getResultStatus());
        record.setTargetEngine(history.getTargetEngine());
        record.setStructureParseSummaryJson(history.getStructureParseSummaryJson());
        record.setAccessParseSummaryJson(history.getAccessParseSummaryJson());
        record.setResultSummaryJson(history.getResultSummaryJson());
        record.setResultPayloadJson(history.getResultPayloadJson());
        record.setQueryContextJson(history.getQueryContextJson());
        record.setCommentContextJson(history.getCommentContextJson());
        record.setBindingSummaryJson(history.getBindingSummaryJson());
        record.setLogicalObjectHitsJson(history.getLogicalObjectHitsJson());
        record.setIssueScenesJson(history.getIssueScenesJson());
        record.setLogicalObjectKeysJson(history.getLogicalObjectKeysJson());
        record.setTraceId(history.getTraceId());
        record.setRequestId(history.getRequestId());
        record.setSagaId(history.getSagaId());
        record.setSubmittedBy(history.getSubmittedBy());
        record.setSubmittedAt(toLocalDateTime(history.getSubmittedAt()));
        record.setCreatedAt(toLocalDateTime(history.getCreatedAt()));
        record.setUpdatedAt(toLocalDateTime(history.getUpdatedAt()));
        return record;
    }

    private SqlParseHistory toDomain(SqlParseHistoryRecord record) {
        SqlParseHistory history = new SqlParseHistory();
        history.setParseHistoryId(record.getParseHistoryId());
        history.setTenantId(record.getTenantId());
        history.setSourceType(record.getSourceType());
        history.setSourceId(record.getSourceId());
        history.setBatchKey(record.getBatchKey());
        history.setParseTaskId(record.getParseTaskId());
        history.setSqlFingerprint(record.getSqlFingerprint());
        history.setDatasourceCode(record.getDatasourceCode());
        history.setDatasourceType(record.getDatasourceType());
        history.setReportCode(record.getReportCode());
        history.setStageCode(record.getStageCode());
        history.setBizDate(record.getBizDate());
        history.setQueryDateStart(record.getQueryDateStart());
        history.setQueryDateEnd(record.getQueryDateEnd());
        history.setQueryDateStatus(record.getQueryDateStatus());
        history.setAccessChannel(record.getAccessChannel());
        history.setParserMode(record.getParserMode());
        history.setSqlText(record.getSqlText());
        history.setSqlTemplateText(record.getSqlTemplateText());
        history.setBindingMode(record.getBindingMode());
        history.setParameterizedSqlFlag(record.getParameterizedSqlFlag());
        history.setResultStatus(record.getResultStatus());
        history.setTargetEngine(record.getTargetEngine());
        history.setStructureParseSummaryJson(record.getStructureParseSummaryJson());
        history.setAccessParseSummaryJson(record.getAccessParseSummaryJson());
        history.setResultSummaryJson(record.getResultSummaryJson());
        history.setResultPayloadJson(record.getResultPayloadJson());
        history.setQueryContextJson(record.getQueryContextJson());
        history.setCommentContextJson(record.getCommentContextJson());
        history.setBindingSummaryJson(record.getBindingSummaryJson());
        history.setLogicalObjectHitsJson(record.getLogicalObjectHitsJson());
        history.setIssueScenesJson(record.getIssueScenesJson());
        history.setLogicalObjectKeysJson(record.getLogicalObjectKeysJson());
        history.setTraceId(record.getTraceId());
        history.setRequestId(record.getRequestId());
        history.setSagaId(record.getSagaId());
        history.setSubmittedBy(record.getSubmittedBy());
        history.setSubmittedAt(toInstant(record.getSubmittedAt()));
        history.setCreatedAt(toInstant(record.getCreatedAt()));
        history.setUpdatedAt(toInstant(record.getUpdatedAt()));
        return history;
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return DateUtils.toBeijingDateTime(instant);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return DateUtils.toInstant(localDateTime);
    }
}
