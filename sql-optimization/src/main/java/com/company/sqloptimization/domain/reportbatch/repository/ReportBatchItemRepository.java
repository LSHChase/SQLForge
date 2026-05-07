package com.company.sqloptimization.domain.reportbatch.repository;

import com.company.sqloptimization.domain.reportbatch.ReportBatchItem;
import java.util.List;

public interface ReportBatchItemRepository {

    ReportBatchItem save(ReportBatchItem item);

    default List<ReportBatchItem> saveAll(List<ReportBatchItem> items) {
        if (items == null) {
            return items;
        }
        for (ReportBatchItem item : items) {
            save(item);
        }
        return items;
    }

    List<ReportBatchItem> findByBatchId(String batchId);

    ReportBatchItem findByItemId(String itemId);
}
