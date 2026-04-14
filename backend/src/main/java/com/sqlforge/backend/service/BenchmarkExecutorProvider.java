package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.BiReleaseRequest;
import java.util.Map;

public interface BenchmarkExecutorProvider {

    String getCode();

    Map<String, Object> buildExecutionPlan(
        BiReleaseRequest request,
        Map<String, Object> assessmentResult,
        Map<String, Object> benchmarkPlan,
        Map<String, Object> benchmarkAnalysis
    );
}
