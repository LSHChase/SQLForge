package com.company.sqloptimization.infrastructure.repository;

import com.company.sqloptimization.domain.batch.ParseBatch;
import com.company.sqloptimization.domain.batch.repository.ParseBatchRepository;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class InMemoryParseBatchRepository implements ParseBatchRepository {

    private final Map<String, ParseBatch> batches = new ConcurrentHashMap<String, ParseBatch>();

    @Override
    public ParseBatch save(ParseBatch batch) {
        batches.put(batch.getBatchId(), batch);
        return batch;
    }

    @Override
    public ParseBatch findByBatchId(String batchId) {
        return batches.get(batchId);
    }
}
