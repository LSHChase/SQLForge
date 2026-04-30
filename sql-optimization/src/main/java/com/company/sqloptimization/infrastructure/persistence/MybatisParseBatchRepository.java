package com.company.sqloptimization.infrastructure.persistence;

import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.domain.batch.ParseBatch;
import com.company.sqloptimization.domain.batch.ParseBatchFileType;
import com.company.sqloptimization.domain.batch.ParseBatchImportMode;
import com.company.sqloptimization.domain.batch.ParseBatchSourceType;
import com.company.sqloptimization.domain.batch.ParseBatchStatus;
import com.company.sqloptimization.domain.batch.ParseBatchStatusTransition;
import com.company.sqloptimization.domain.batch.repository.ParseBatchRepository;
import com.company.sqloptimization.infrastructure.persistence.entity.ParseBatchRecord;
import com.company.sqloptimization.infrastructure.persistence.mapper.ParseBatchMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "sql-optimization.parse-batch", name = "repository", havingValue = "database")
public class MybatisParseBatchRepository implements ParseBatchRepository {

    private static final ZoneOffset DATABASE_ZONE_OFFSET = ZoneOffset.UTC;
    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS = new TypeReference<List<Map<String, Object>>>() {
    };

    private final ParseBatchMapper parseBatchMapper;

    public MybatisParseBatchRepository(ParseBatchMapper parseBatchMapper) {
        this.parseBatchMapper = parseBatchMapper;
    }

    @Override
    public ParseBatch save(ParseBatch batch) {
        ParseBatchRecord record = toRecord(batch);
        if (parseBatchMapper.selectByBatchId(batch.getBatchId()) == null) {
            parseBatchMapper.insert(record);
        } else {
            parseBatchMapper.update(record);
        }
        return batch;
    }

    @Override
    public ParseBatch findByBatchId(String batchId) {
        ParseBatchRecord record = parseBatchMapper.selectByBatchId(batchId);
        return record == null ? null : toDomain(record);
    }

    @Override
    public List<ParseBatch> findAll() {
        List<ParseBatchRecord> records = parseBatchMapper.selectAll();
        List<ParseBatch> result = new ArrayList<ParseBatch>(records.size());
        for (ParseBatchRecord record : records) {
            result.add(toDomain(record));
        }
        result.sort(Comparator.comparing(ParseBatch::getCreatedAt).reversed().thenComparing(ParseBatch::getBatchId));
        return result;
    }

    private ParseBatchRecord toRecord(ParseBatch batch) {
        ParseBatchRecord record = new ParseBatchRecord();
        record.setBatchId(batch.getBatchId());
        record.setTenantId(batch.getTenantId());
        record.setBatchName(batch.getBatchName());
        record.setImportMode(batch.getImportMode().name());
        record.setSourceType(batch.getSourceType().name());
        record.setFileType(batch.getFileType().name());
        record.setTemplateVersion(batch.getTemplateVersion());
        record.setDatasourceCode(batch.getDatasourceCode());
        record.setStructureParseOnly(Boolean.valueOf(batch.isStructureParseOnly()));
        record.setStatus(batch.getStatus().name());
        record.setTotalRecords(Integer.valueOf(batch.getTotalRecords()));
        record.setSuccessRecords(Integer.valueOf(batch.getSuccessRecords()));
        record.setPartialSuccessRecords(Integer.valueOf(batch.getPartialSuccessRecords()));
        record.setFailedRecords(Integer.valueOf(batch.getFailedRecords()));
        record.setStructureParseSuccessRate(batch.getStructureParseSuccessRate());
        record.setAccessParseSuccessRate(batch.getAccessParseSuccessRate());
        record.setStatusHistoryJson(JsonUtils.toJson(batch.getStatusHistory()));
        record.setCreatedBy(batch.getCreatedBy());
        record.setCreatedAt(toLocalDateTime(batch.getCreatedAt()));
        record.setUpdatedAt(toLocalDateTime(batch.getUpdatedAt()));
        return record;
    }

    private ParseBatch toDomain(ParseBatchRecord record) {
        return ParseBatch.restore(
            record.getBatchId(),
            record.getTenantId(),
            record.getBatchName(),
            ParseBatchImportMode.valueOf(record.getImportMode()),
            ParseBatchSourceType.valueOf(record.getSourceType()),
            ParseBatchFileType.valueOf(record.getFileType()),
            record.getTemplateVersion(),
            record.getDatasourceCode(),
            Boolean.TRUE.equals(record.getStructureParseOnly()),
            ParseBatchStatus.valueOf(record.getStatus()),
            record.getTotalRecords() == null ? 0 : record.getTotalRecords().intValue(),
            record.getSuccessRecords() == null ? 0 : record.getSuccessRecords().intValue(),
            record.getPartialSuccessRecords() == null ? 0 : record.getPartialSuccessRecords().intValue(),
            record.getFailedRecords() == null ? 0 : record.getFailedRecords().intValue(),
            record.getStructureParseSuccessRate(),
            record.getAccessParseSuccessRate(),
            record.getCreatedBy(),
            toInstant(record.getCreatedAt()),
            toInstant(record.getUpdatedAt()),
            readStatusHistory(record.getStatusHistoryJson())
        );
    }

    private List<ParseBatchStatusTransition> readStatusHistory(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> items = JsonUtils.objectMapper().readValue(json, LIST_OF_MAPS);
            List<ParseBatchStatusTransition> history = new ArrayList<ParseBatchStatusTransition>(items.size());
            for (Map<String, Object> item : items) {
                history.add(new ParseBatchStatusTransition(
                    item.get("previousStatus") == null ? null : ParseBatchStatus.valueOf(String.valueOf(item.get("previousStatus"))),
                    item.get("currentStatus") == null ? null : ParseBatchStatus.valueOf(String.valueOf(item.get("currentStatus"))),
                    item.get("occurredAt") == null ? null : Instant.parse(String.valueOf(item.get("occurredAt"))),
                    item.get("note") == null ? null : String.valueOf(item.get("note"))
                ));
            }
            return history;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to deserialize parse batch status history", ex);
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, DATABASE_ZONE_OFFSET);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(DATABASE_ZONE_OFFSET);
    }
}
