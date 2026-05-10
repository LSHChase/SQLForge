package com.company.sqloptimization.infrastructure.repository;

import com.company.sqloptimization.domain.rewrite.RewriteValidationRun;
import com.company.sqloptimization.domain.rewrite.SqlRewriteRecord;
import com.company.sqloptimization.domain.rewrite.repository.SqlRewriteRecordRepository;
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
}
