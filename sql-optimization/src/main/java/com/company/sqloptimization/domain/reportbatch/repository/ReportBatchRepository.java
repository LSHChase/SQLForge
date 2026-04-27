package com.company.sqloptimization.domain.reportbatch.repository;

import com.company.sqloptimization.domain.reportbatch.ReportBatch;

public interface ReportBatchRepository {

    ReportBatch save(ReportBatch batch);

    ReportBatch findByBatchId(String batchId);
}
