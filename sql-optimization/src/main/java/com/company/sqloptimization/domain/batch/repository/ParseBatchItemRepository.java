package com.company.sqloptimization.domain.batch.repository;

import com.company.sqloptimization.domain.batch.ParseBatchItem;
import java.util.List;

public interface ParseBatchItemRepository {

    ParseBatchItem save(ParseBatchItem item);

    List<ParseBatchItem> findByBatchId(String batchId);

    ParseBatchItem findByItemId(String itemId);

    List<ParseBatchItem> findAll();
}
