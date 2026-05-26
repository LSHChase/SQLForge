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

    default List<ReportBatchItem> findPageByBatchId(String batchId, int offset, int limit, String reportCode) {
        List<ReportBatchItem> items = findByBatchId(batchId);
        java.util.ArrayList<ReportBatchItem> filtered = new java.util.ArrayList<ReportBatchItem>();
        for (ReportBatchItem item : items) {
            if (item == null) {
                continue;
            }
            if (reportCode == null || reportCode.equals(item.getReportCode())) {
                filtered.add(item);
            }
        }
        int start = Math.min(filtered.size(), Math.max(0, offset));
        int end = Math.min(filtered.size(), start + Math.max(0, limit));
        return new java.util.ArrayList<ReportBatchItem>(filtered.subList(start, end));
    }

    default int countByBatchId(String batchId, String reportCode) {
        int count = 0;
        for (ReportBatchItem item : findByBatchId(batchId)) {
            if (item != null && (reportCode == null || reportCode.equals(item.getReportCode()))) {
                count++;
            }
        }
        return count;
    }
}
