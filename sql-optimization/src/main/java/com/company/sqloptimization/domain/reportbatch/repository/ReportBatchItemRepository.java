package com.company.sqloptimization.domain.reportbatch.repository;

import com.company.sqloptimization.domain.reportbatch.ReportBatchItem;
import java.util.List;

public interface ReportBatchItemRepository {

    ReportBatchItem save(ReportBatchItem item);

    List<ReportBatchItem> findByBatchId(String batchId);

    ReportBatchItem findByItemId(String itemId);
}
