package com.company.sqloptimization.domain.reportbatch.repository;

import com.company.sqloptimization.domain.reportbatch.ReportBatch;
import java.util.List;

public interface ReportBatchRepository {

    ReportBatch save(ReportBatch batch);

    ReportBatch findByBatchId(String batchId);

    List<ReportBatch> findAll();

    default List<ReportBatch> findPageByTenantId(String tenantId, int offset, int limit) {
        List<ReportBatch> batches = findAll();
        java.util.ArrayList<ReportBatch> filtered = new java.util.ArrayList<ReportBatch>();
        for (ReportBatch batch : batches) {
            if (batch != null && tenantId != null && tenantId.equals(batch.getTenantId())) {
                filtered.add(batch);
            }
        }
        int start = Math.min(filtered.size(), Math.max(0, offset));
        int end = Math.min(filtered.size(), start + Math.max(0, limit));
        return new java.util.ArrayList<ReportBatch>(filtered.subList(start, end));
    }

    default int countByTenantId(String tenantId) {
        int count = 0;
        for (ReportBatch batch : findAll()) {
            if (batch != null && tenantId != null && tenantId.equals(batch.getTenantId())) {
                count++;
            }
        }
        return count;
    }
}
