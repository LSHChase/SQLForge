package com.company.queryexecution.application.controller;

import com.company.queryexecution.application.service.QueryExecutionBenchmarkWorkloadService;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/query-execution/internal/benchmark")
public class QueryExecutionInternalController {

    private final QueryExecutionBenchmarkWorkloadService queryExecutionBenchmarkWorkloadService;

    public QueryExecutionInternalController(QueryExecutionBenchmarkWorkloadService queryExecutionBenchmarkWorkloadService) {
        this.queryExecutionBenchmarkWorkloadService = queryExecutionBenchmarkWorkloadService;
    }

    @PostMapping("/workload/capture")
    public QueryExecutionBenchmarkWorkloadResponse captureWorkload(@RequestBody QueryExecutionBenchmarkWorkloadRequest request) {
        return queryExecutionBenchmarkWorkloadService.capture(request);
    }
}
