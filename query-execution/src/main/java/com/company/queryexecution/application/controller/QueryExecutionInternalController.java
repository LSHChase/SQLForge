package com.company.queryexecution.application.controller;

import com.company.queryexecution.application.service.QueryExecutionBenchmarkWorkloadService;
import com.company.queryexecution.application.service.QueryExecutionAccelerationRuntimeService;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanApplyRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanRollbackRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanVerifyRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/query-execution/internal")
public class QueryExecutionInternalController {

    private final QueryExecutionBenchmarkWorkloadService queryExecutionBenchmarkWorkloadService;
    private final QueryExecutionAccelerationRuntimeService queryExecutionAccelerationRuntimeService;

    public QueryExecutionInternalController(QueryExecutionBenchmarkWorkloadService queryExecutionBenchmarkWorkloadService,
                                            QueryExecutionAccelerationRuntimeService queryExecutionAccelerationRuntimeService) {
        this.queryExecutionBenchmarkWorkloadService = queryExecutionBenchmarkWorkloadService;
        this.queryExecutionAccelerationRuntimeService = queryExecutionAccelerationRuntimeService;
    }

    @PostMapping("/benchmark/workload/capture")
    public QueryExecutionBenchmarkWorkloadResponse captureWorkload(@RequestBody QueryExecutionBenchmarkWorkloadRequest request) {
        return queryExecutionBenchmarkWorkloadService.capture(request);
    }

    @PostMapping("/acceleration-plans/apply")
    public QueryExecutionAccelerationPlanResponse applyAccelerationPlan(
        @RequestBody QueryExecutionAccelerationPlanApplyRequest request
    ) {
        return queryExecutionAccelerationRuntimeService.apply(request);
    }

    @PostMapping("/acceleration-plans/verify")
    public QueryExecutionAccelerationPlanResponse verifyAccelerationPlan(
        @RequestBody QueryExecutionAccelerationPlanVerifyRequest request
    ) {
        return queryExecutionAccelerationRuntimeService.verify(request);
    }

    @PostMapping("/acceleration-plans/rollback")
    public QueryExecutionAccelerationPlanResponse rollbackAccelerationPlan(
        @RequestBody QueryExecutionAccelerationPlanRollbackRequest request
    ) {
        return queryExecutionAccelerationRuntimeService.rollback(request);
    }
}
