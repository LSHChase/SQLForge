package com.company.sqloptimization.infrastructure.persistence;

import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.domain.batch.ParseBatchItem;
import com.company.sqloptimization.domain.batch.ParseBatchItemStatus;
import com.company.sqloptimization.domain.batch.repository.ParseBatchItemRepository;
import com.company.sqloptimization.infrastructure.persistence.entity.ParseBatchItemRecord;
import com.company.sqloptimization.infrastructure.persistence.mapper.ParseBatchItemMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "sql-optimization.parse-batch", name = "repository", havingValue = "database")
public class MybatisParseBatchItemRepository implements ParseBatchItemRepository {

    private static final ZoneOffset DATABASE_ZONE_OFFSET = ZoneOffset.UTC;
    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<List<String>>() {
    };

    private final ParseBatchItemMapper parseBatchItemMapper;

    public MybatisParseBatchItemRepository(ParseBatchItemMapper parseBatchItemMapper) {
        this.parseBatchItemMapper = parseBatchItemMapper;
    }

    @Override
    public ParseBatchItem save(ParseBatchItem item) {
        ParseBatchItemRecord record = toRecord(item);
        if (parseBatchItemMapper.selectByItemId(item.getItemId()) == null) {
            parseBatchItemMapper.insert(record);
        } else {
            parseBatchItemMapper.update(record);
        }
        return item;
    }

    @Override
    public List<ParseBatchItem> findByBatchId(String batchId) {
        List<ParseBatchItemRecord> records = parseBatchItemMapper.selectByBatchId(batchId);
        java.util.ArrayList<ParseBatchItem> items = new java.util.ArrayList<ParseBatchItem>(records.size());
        for (ParseBatchItemRecord record : records) {
            items.add(toDomain(record));
        }
        return items;
    }

    @Override
    public ParseBatchItem findByItemId(String itemId) {
        ParseBatchItemRecord record = parseBatchItemMapper.selectByItemId(itemId);
        return record == null ? null : toDomain(record);
    }

    @Override
    public List<ParseBatchItem> findAll() {
        List<ParseBatchItemRecord> records = parseBatchItemMapper.selectAll();
        java.util.ArrayList<ParseBatchItem> items = new java.util.ArrayList<ParseBatchItem>(records.size());
        for (ParseBatchItemRecord record : records) {
            items.add(toDomain(record));
        }
        return items;
    }

    private ParseBatchItemRecord toRecord(ParseBatchItem item) {
        ParseBatchItemRecord record = new ParseBatchItemRecord();
        record.setItemId(item.getItemId());
        record.setBatchId(item.getBatchId());
        record.setSequenceNumber(Integer.valueOf(item.getSequenceNumber()));
        record.setReportCode(item.getReportCode());
        record.setReportName(item.getReportName());
        record.setDatasourceCode(item.getDatasourceCode());
        record.setStage(item.getStage());
        record.setBizDate(item.getBizDate());
        record.setPriority(item.getPriority());
        record.setOwner(item.getOwner());
        record.setTags(item.getTags());
        record.setSqlText(item.getSqlText());
        record.setSqlTemplateText(item.getSqlTemplateText());
        record.setBindParametersJson(item.getBindParametersJson());
        record.setBindingMode(item.getBindingMode());
        record.setStatus(item.getStatus() == null ? null : item.getStatus().name());
        record.setParseTaskId(item.getParseTaskId());
        record.setStructureSyntaxStatus(item.getStructureSyntaxStatus());
        record.setAccessServiceStatus(item.getAccessServiceStatus());
        record.setAccessConnectionStatus(item.getAccessConnectionStatus());
        record.setFailureReason(item.getFailureReason());
        record.setHistoryId(item.getHistoryId());
        record.setHistoryPersisted(item.getHistoryPersisted());
        record.setHistoryPersistenceStatus(item.getHistoryPersistenceStatus());
        record.setIssueScenesJson(JsonUtils.toJson(item.getIssueScenes()));
        record.setLogicalObjectKeysJson(JsonUtils.toJson(item.getLogicalObjectKeys()));
        record.setCreatedAt(toLocalDateTime(item.getCreatedAt()));
        record.setUpdatedAt(toLocalDateTime(item.getUpdatedAt()));
        return record;
    }

    private ParseBatchItem toDomain(ParseBatchItemRecord record) {
        return ParseBatchItem.restore(
            record.getItemId(),
            record.getBatchId(),
            record.getSequenceNumber() == null ? 0 : record.getSequenceNumber().intValue(),
            record.getReportCode(),
            record.getReportName(),
            record.getDatasourceCode(),
            record.getStage(),
            record.getBizDate(),
            record.getPriority(),
            record.getOwner(),
            record.getTags(),
            record.getSqlText(),
            record.getSqlTemplateText(),
            record.getBindParametersJson(),
            record.getBindingMode(),
            record.getStatus() == null ? null : ParseBatchItemStatus.valueOf(record.getStatus()),
            record.getParseTaskId(),
            record.getStructureSyntaxStatus(),
            record.getAccessServiceStatus(),
            record.getAccessConnectionStatus(),
            record.getFailureReason(),
            record.getHistoryId(),
            record.getHistoryPersisted(),
            record.getHistoryPersistenceStatus(),
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
            throw new IllegalArgumentException("Failed to deserialize parse batch item list payload", ex);
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, DATABASE_ZONE_OFFSET);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(DATABASE_ZONE_OFFSET);
    }
}
