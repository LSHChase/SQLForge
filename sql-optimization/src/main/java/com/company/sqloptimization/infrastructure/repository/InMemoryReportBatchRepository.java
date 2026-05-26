package com.company.sqloptimization.infrastructure.repository;

import com.company.sqloptimization.domain.reportbatch.ReportBatch;
import com.company.sqloptimization.domain.reportbatch.repository.ReportBatchRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "sql-optimization.report-batch", name = "repository", havingValue = "test")
public class InMemoryReportBatchRepository implements ReportBatchRepository {

    private final Map<String, ReportBatch> batches = new ConcurrentHashMap<String, ReportBatch>();

    @Override
    public ReportBatch save(ReportBatch batch) {
        batches.put(batch.getBatchId(), batch);
        return batch;
    }

    @Override
    public ReportBatch findByBatchId(String batchId) {
        return batches.get(batchId);
    }

    @Override
    public List<ReportBatch> findAll() {
        List<ReportBatch> result = new ArrayList<ReportBatch>(batches.values());
        result.sort(Comparator.comparing(ReportBatch::getCreatedAt).reversed().thenComparing(ReportBatch::getBatchId));
        return result;
    }
}
