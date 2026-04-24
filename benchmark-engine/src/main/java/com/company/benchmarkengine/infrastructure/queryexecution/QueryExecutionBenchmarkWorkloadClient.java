package com.company.benchmarkengine.infrastructure.queryexecution;

import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadResponse;

public interface QueryExecutionBenchmarkWorkloadClient {

    QueryExecutionBenchmarkWorkloadResponse captureWorkload(QueryExecutionBenchmarkWorkloadRequest request);
}
