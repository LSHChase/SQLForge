package com.company.sqloptimization.domain.batch.repository;

import com.company.sqloptimization.domain.batch.ParseBatch;
import java.util.List;

public interface ParseBatchRepository {

    ParseBatch save(ParseBatch batch);

    ParseBatch findByBatchId(String batchId);

    List<ParseBatch> findAll();
}
