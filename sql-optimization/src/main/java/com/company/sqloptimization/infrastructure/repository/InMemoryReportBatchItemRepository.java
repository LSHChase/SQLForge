package com.company.sqloptimization.infrastructure.repository;

import com.company.sqloptimization.domain.reportbatch.ReportBatchItem;
import com.company.sqloptimization.domain.reportbatch.repository.ReportBatchItemRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class InMemoryReportBatchItemRepository implements ReportBatchItemRepository {

    private final Map<String, ReportBatchItem> items = new ConcurrentHashMap<String, ReportBatchItem>();

    @Override
    public ReportBatchItem save(ReportBatchItem item) {
        items.put(item.getItemId(), item);
        return item;
    }

    @Override
    public List<ReportBatchItem> saveAll(List<ReportBatchItem> nextItems) {
        if (nextItems == null) {
            return nextItems;
        }
        for (ReportBatchItem item : nextItems) {
            save(item);
        }
        return nextItems;
    }

    @Override
    public List<ReportBatchItem> findByBatchId(String batchId) {
        List<ReportBatchItem> result = new ArrayList<ReportBatchItem>();
        for (ReportBatchItem item : items.values()) {
            if (batchId.equals(item.getBatchId())) {
                result.add(item);
            }
        }
        result.sort(Comparator.comparingInt(ReportBatchItem::getSequenceNumber));
        return result;
    }

    @Override
    public ReportBatchItem findByItemId(String itemId) {
        return items.get(itemId);
    }
}
