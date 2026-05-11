package com.company.sqloptimization.infrastructure.repository;

import com.company.sqloptimization.domain.rewrite.RewriteValidationRun;
import com.company.sqloptimization.domain.rewrite.SqlRewriteRecord;
import com.company.sqloptimization.domain.rewrite.repository.SqlRewriteRecordRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "sql-optimization.queues", name = "mode", havingValue = "local-placeholder")
public class InMemorySqlRewriteRecordRepository implements SqlRewriteRecordRepository {

    private final Map<String, SqlRewriteRecord> records = new ConcurrentHashMap<String, SqlRewriteRecord>();
    private final Map<String, RewriteValidationRun> validationRuns =
        new ConcurrentHashMap<String, RewriteValidationRun>();

    @Override
    public SqlRewriteRecord saveRecord(SqlRewriteRecord rewriteRecord) {
        records.put(rewriteRecord.getRewriteRecordId(), rewriteRecord);
        return rewriteRecord;
    }

    @Override
    public SqlRewriteRecord findRecordById(String rewriteRecordId) {
        return records.get(rewriteRecordId);
    }

    @Override
    public List<SqlRewriteRecord> findRecordsByTenantId(String tenantId) {
        List<SqlRewriteRecord> matches = new ArrayList<SqlRewriteRecord>();
        for (SqlRewriteRecord record : records.values()) {
            if (record.getTenantId().equals(tenantId)) {
                matches.add(record);
            }
        }
        matches.sort(Comparator.comparing(SqlRewriteRecord::getCreatedAt).reversed());
        return matches;
    }

    @Override
    public List<SqlRewriteRecord> findRecordsByTenantIdAndHistoryId(String tenantId, String historyId) {
        List<SqlRewriteRecord> matches = new ArrayList<SqlRewriteRecord>();
        for (SqlRewriteRecord record : records.values()) {
            if (record.getTenantId().equals(tenantId) && historyId.equals(record.getHistoryId())) {
                matches.add(record);
            }
        }
        matches.sort(Comparator.comparing(SqlRewriteRecord::getCreatedAt).reversed());
        return matches;
    }

    @Override
    public List<SqlRewriteRecord> findScheduledValidationCandidates(int limit, Instant dueBefore) {
        List<SqlRewriteRecord> matches = new ArrayList<SqlRewriteRecord>();
        for (SqlRewriteRecord record : records.values()) {
            if (isScheduledValidationCandidate(record, dueBefore)) {
                matches.add(record);
            }
        }
        matches.sort(Comparator.comparing(this::lastComparedOrCreatedAt));
        int safeLimit = limit <= 0 ? 20 : limit;
        return matches.size() <= safeLimit ? matches : new ArrayList<SqlRewriteRecord>(matches.subList(0, safeLimit));
    }

    @Override
    public RewriteValidationRun saveValidationRun(RewriteValidationRun validationRun) {
        validationRuns.put(validationRun.getValidationRunId(), validationRun);
        return validationRun;
    }

    @Override
    public List<RewriteValidationRun> findValidationRunsByRewriteRecordId(String rewriteRecordId) {
        List<RewriteValidationRun> matches = new ArrayList<RewriteValidationRun>();
        for (RewriteValidationRun run : validationRuns.values()) {
            if (run.getRewriteRecordId().equals(rewriteRecordId)) {
                matches.add(run);
            }
        }
        matches.sort(Comparator.comparing(RewriteValidationRun::getStartedAt).reversed());
        return matches;
    }

    private boolean isScheduledValidationCandidate(SqlRewriteRecord record, Instant dueBefore) {
        if (record == null || !record.isAutoApplyAllowed()) {
            return false;
        }
        String status = record.getStatus().name();
        String validationStatus = record.getValidationStatus().name();
        if (!("APPROVED".equals(status) || "APPLIED".equals(status))) {
            return false;
        }
        if (!("NOT_VALIDATED".equals(validationStatus) || "EXPIRED".equals(validationStatus))) {
            return false;
        }
        Instant lastComparedAt = record.getLastComparedAt();
        return lastComparedAt == null || dueBefore == null || !lastComparedAt.isAfter(dueBefore);
    }

    private Instant lastComparedOrCreatedAt(SqlRewriteRecord record) {
        return record.getLastComparedAt() == null ? record.getCreatedAt() : record.getLastComparedAt();
    }
}
