package com.company.sqloptimization.infrastructure.repository;

import com.company.sqloptimization.domain.batch.ParseBatchItem;
import com.company.sqloptimization.domain.batch.repository.ParseBatchItemRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class InMemoryParseBatchItemRepository implements ParseBatchItemRepository {

    private final Map<String, ParseBatchItem> items = new ConcurrentHashMap<String, ParseBatchItem>();

    @Override
    public ParseBatchItem save(ParseBatchItem item) {
        items.put(item.getItemId(), item);
        return item;
    }

    @Override
    public List<ParseBatchItem> findByBatchId(String batchId) {
        List<ParseBatchItem> result = new ArrayList<ParseBatchItem>();
        for (ParseBatchItem item : items.values()) {
            if (batchId.equals(item.getBatchId())) {
                result.add(item);
            }
        }
        result.sort(Comparator.comparingInt(ParseBatchItem::getSequenceNumber));
        return result;
    }

    @Override
    public ParseBatchItem findByItemId(String itemId) {
        return items.get(itemId);
    }
}
