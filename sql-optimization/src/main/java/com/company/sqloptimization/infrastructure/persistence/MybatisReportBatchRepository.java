package com.company.sqloptimization.infrastructure.persistence;

import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.domain.parse.SqlParserMode;
import com.company.sqloptimization.domain.reportbatch.ReportBatch;
import com.company.sqloptimization.domain.reportbatch.ReportBatch.ParseStatus;
import com.company.sqloptimization.domain.reportbatch.ReportBatchStatusTransition;
import com.company.sqloptimization.domain.reportbatch.repository.ReportBatchRepository;
import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchRecord;
import com.company.sqloptimization.infrastructure.persistence.mapper.ReportBatchMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.company.sqlforge.common.utils.DateUtils;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "sql-optimization.report-batch", name = "repository", havingValue = "database")
public class MybatisReportBatchRepository implements ReportBatchRepository {
    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS = new TypeReference<List<Map<String, Object>>>() {
    };

    private final ReportBatchMapper reportBatchMapper;

    public MybatisReportBatchRepository(ReportBatchMapper reportBatchMapper) {
        this.reportBatchMapper = reportBatchMapper;
    }

    @Override
    public ReportBatch save(ReportBatch batch) {
        ReportBatchRecord record = toRecord(batch);
        if (reportBatchMapper.selectByBatchId(batch.getBatchId()) == null) {
            reportBatchMapper.insert(record);
        } else {
            reportBatchMapper.update(record);
        }
        return batch;
    }

    @Override
    public ReportBatch findByBatchId(String batchId) {
        ReportBatchRecord record = reportBatchMapper.selectByBatchId(batchId);
        return record == null ? null : toDomain(record);
    }

    @Override
    public List<ReportBatch> findAll() {
        List<ReportBatchRecord> records = reportBatchMapper.selectAll();
        List<ReportBatch> result = new ArrayList<ReportBatch>(records.size());
        for (ReportBatchRecord record : records) {
            result.add(toDomain(record));
        }
        result.sort(Comparator.comparing(ReportBatch::getCreatedAt).reversed().thenComparing(ReportBatch::getBatchId));
        return result;
    }

    @Override
    public List<ReportBatch> findPageByTenantId(String tenantId, int offset, int limit) {
        List<ReportBatchRecord> records = reportBatchMapper.selectPageByTenantId(
            tenantId,
            Integer.valueOf(Math.max(0, offset)),
            Integer.valueOf(Math.max(0, limit))
        );
        List<ReportBatch> result = new ArrayList<ReportBatch>(records.size());
        for (ReportBatchRecord record : records) {
            result.add(toDomain(record));
        }
        return result;
    }

    @Override
    public int countByTenantId(String tenantId) {
        Integer count = reportBatchMapper.countByTenantId(tenantId);
        return count == null ? 0 : count.intValue();
    }

    private ReportBatchRecord toRecord(ReportBatch batch) {
        ReportBatchRecord record = new ReportBatchRecord();
        record.setBatchId(batch.getBatchId());
        record.setTenantId(batch.getTenantId());
        record.setBatchName(batch.getBatchName());
        record.setFileType(batch.getFileType());
        record.setReportCodeField(batch.getReportCodeField());
        record.setDatasourceCode(batch.getDatasourceCode());
        record.setStage(batch.getStage());
        record.setPriority(batch.getPriority());
        record.setParserMode(batch.getParserMode());
        record.setSourceType(batch.getSourceType());
        record.setStatus(batch.getStatus().name());
        record.setTotalReports(Integer.valueOf(batch.getTotalReports()));
        record.setResolvedReports(Integer.valueOf(batch.getResolvedReports()));
        record.setFailedReports(Integer.valueOf(batch.getFailedReports()));
        record.setStatusHistoryJson(JsonUtils.toJson(batch.getStatusHistory()));
        record.setCreatedBy(batch.getCreatedBy());
        record.setCreatedAt(toLocalDateTime(batch.getCreatedAt()));
        record.setUpdatedAt(toLocalDateTime(batch.getUpdatedAt()));
        return record;
    }

    private ReportBatch toDomain(ReportBatchRecord record) {
        return ReportBatch.restore(
            record.getBatchId(),
            record.getTenantId(),
            record.getBatchName(),
            record.getFileType(),
            record.getReportCodeField(),
            record.getDatasourceCode(),
            record.getStage(),
            record.getPriority(),
            SqlParserMode.resolveDefault(record.getParserMode()).name(),
            record.getSourceType(),
            record.getStatus() == null ? ParseStatus.FAILED : ParseStatus.valueOf(record.getStatus()),
            record.getTotalReports() == null ? 0 : record.getTotalReports().intValue(),
            record.getResolvedReports() == null ? 0 : record.getResolvedReports().intValue(),
            record.getFailedReports() == null ? 0 : record.getFailedReports().intValue(),
            record.getCreatedBy(),
            toInstant(record.getCreatedAt()),
            toInstant(record.getUpdatedAt()),
            readStatusHistory(record.getStatusHistoryJson())
        );
    }

    private List<ReportBatchStatusTransition> readStatusHistory(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> items = JsonUtils.objectMapper().readValue(json, LIST_OF_MAPS);
            List<ReportBatchStatusTransition> history = new ArrayList<ReportBatchStatusTransition>(items.size());
            for (Map<String, Object> item : items) {
                history.add(new ReportBatchStatusTransition(
                    item.get("previousStatus") == null ? null : ParseStatus.valueOf(String.valueOf(item.get("previousStatus"))),
                    item.get("currentStatus") == null ? null : ParseStatus.valueOf(String.valueOf(item.get("currentStatus"))),
                    item.get("occurredAt") == null ? null : Instant.parse(String.valueOf(item.get("occurredAt"))),
                    item.get("note") == null ? null : String.valueOf(item.get("note"))
                ));
            }
            return history;
        } catch (Exception ex) {
            throw new IllegalArgumentException("报表批次状态历史反序列化失败", ex);
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return DateUtils.toBeijingDateTime(instant);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return DateUtils.toInstant(localDateTime);
    }
}
