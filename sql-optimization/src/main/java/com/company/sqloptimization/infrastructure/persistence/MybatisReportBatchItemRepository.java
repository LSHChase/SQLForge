package com.company.sqloptimization.infrastructure.persistence;

import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.domain.reportbatch.ReportBatchItem;
import com.company.sqloptimization.domain.reportbatch.ReportBatchItem.Status;
import com.company.sqloptimization.domain.reportbatch.repository.ReportBatchItemRepository;
import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchItemRecord;
import com.company.sqloptimization.infrastructure.persistence.mapper.ReportBatchItemMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "sql-optimization.queues", name = "mode", havingValue = "database-worker")
public class MybatisReportBatchItemRepository implements ReportBatchItemRepository {

    private static final ZoneOffset DATABASE_ZONE_OFFSET = ZoneOffset.UTC;
    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<List<String>>() {
    };

    private final ReportBatchItemMapper reportBatchItemMapper;

    public MybatisReportBatchItemRepository(ReportBatchItemMapper reportBatchItemMapper) {
        this.reportBatchItemMapper = reportBatchItemMapper;
    }

    @Override
    public ReportBatchItem save(ReportBatchItem item) {
        ReportBatchItemRecord record = toRecord(item);
        if (reportBatchItemMapper.selectByItemId(item.getItemId()) == null) {
            reportBatchItemMapper.insert(record);
        } else {
            reportBatchItemMapper.update(record);
        }
        return item;
    }

    @Override
    public List<ReportBatchItem> findByBatchId(String batchId) {
        List<ReportBatchItemRecord> records = reportBatchItemMapper.selectByBatchId(batchId);
        List<ReportBatchItem> items = new ArrayList<ReportBatchItem>(records.size());
        for (ReportBatchItemRecord record : records) {
            items.add(toDomain(record));
        }
        return items;
    }

    @Override
    public ReportBatchItem findByItemId(String itemId) {
        ReportBatchItemRecord record = reportBatchItemMapper.selectByItemId(itemId);
        return record == null ? null : toDomain(record);
    }

    private ReportBatchItemRecord toRecord(ReportBatchItem item) {
        ReportBatchItemRecord record = new ReportBatchItemRecord();
        record.setItemId(item.getItemId());
        record.setBatchId(item.getBatchId());
        record.setSequenceNumber(Integer.valueOf(item.getSequenceNumber()));
        record.setReportCode(item.getReportCode());
        record.setReportName(item.getReportName());
        record.setDatasourceCode(item.getDatasourceCode());
        record.setStage(item.getStage());
        record.setPriority(item.getPriority());
        record.setSourceFileLine(item.getSourceFileLine());
        record.setSqlColumnName(item.getSqlColumnName());
        record.setSqlOrdinalInReport(item.getSqlOrdinalInReport());
        record.setSqlText(item.getSqlText());
        record.setParseTaskId(item.getParseTaskId());
        record.setStructureSyntaxStatus(item.getStructureSyntaxStatus());
        record.setAccessServiceStatus(item.getAccessServiceStatus());
        record.setAccessConnectionStatus(item.getAccessConnectionStatus());
        record.setFailureReason(item.getFailureReason());
        record.setStatus(item.getStatus() == null ? null : item.getStatus().name());
        record.setIssueScenesJson(JsonUtils.toJson(item.getIssueScenes()));
        record.setLogicalObjectKeysJson(JsonUtils.toJson(item.getLogicalObjectKeys()));
        record.setCreatedAt(toLocalDateTime(item.getCreatedAt()));
        record.setUpdatedAt(toLocalDateTime(item.getUpdatedAt()));
        return record;
    }

    private ReportBatchItem toDomain(ReportBatchItemRecord record) {
        return ReportBatchItem.restore(
            record.getItemId(),
            record.getBatchId(),
            record.getSequenceNumber() == null ? 0 : record.getSequenceNumber().intValue(),
            record.getReportCode(),
            record.getReportName(),
            record.getDatasourceCode(),
            record.getStage(),
            record.getPriority(),
            record.getSourceFileLine(),
            record.getSqlColumnName(),
            record.getSqlOrdinalInReport(),
            record.getSqlText(),
            record.getParseTaskId(),
            record.getStructureSyntaxStatus(),
            record.getAccessServiceStatus(),
            record.getAccessConnectionStatus(),
            record.getFailureReason(),
            record.getStatus() == null ? null : Status.valueOf(record.getStatus()),
            readStringList(record.getIssueScenesJson()),
            readStringList(record.getLogicalObjectKeysJson()),
            toInstant(record.getCreatedAt()),
            toInstant(record.getUpdatedAt())
        );
    }

    private List<String> readStringList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JsonUtils.objectMapper().readValue(json, LIST_OF_STRINGS);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to deserialize report batch item list payload", ex);
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, DATABASE_ZONE_OFFSET);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(DATABASE_ZONE_OFFSET);
    }
}
