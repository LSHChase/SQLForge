package com.company.queryexecution.application.controller;

import com.company.queryexecution.application.controller.vo.HetuRouteCalibrationResponse;
import com.company.queryexecution.application.service.HetuRouteCalibrationService;
import com.company.queryexecution.application.service.QueryExecutionBenchmarkWorkloadService;
import com.company.queryexecution.application.service.QueryExecutionAccelerationRuntimeService;
import com.company.queryexecution.application.service.QueryExecutionCacheGovernanceRuntimeService;
import com.company.queryexecution.domain.query.HetuRouteCalibrationSnapshot;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanApplyRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanRollbackRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanVerifyRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionCachePolicyApplyRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionCachePolicyInvalidateRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionCachePolicyResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionCachePolicyVerifyRequest;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/query-execution/internal")
public class QueryExecutionInternalController {

    private final QueryExecutionBenchmarkWorkloadService queryExecutionBenchmarkWorkloadService;
    private final QueryExecutionAccelerationRuntimeService queryExecutionAccelerationRuntimeService;
    private final QueryExecutionCacheGovernanceRuntimeService queryExecutionCacheGovernanceRuntimeService;
    private final HetuRouteCalibrationService hetuRouteCalibrationService;

    public QueryExecutionInternalController(QueryExecutionBenchmarkWorkloadService queryExecutionBenchmarkWorkloadService,
                                            QueryExecutionAccelerationRuntimeService queryExecutionAccelerationRuntimeService,
                                            QueryExecutionCacheGovernanceRuntimeService queryExecutionCacheGovernanceRuntimeService,
                                            HetuRouteCalibrationService hetuRouteCalibrationService) {
        this.queryExecutionBenchmarkWorkloadService = queryExecutionBenchmarkWorkloadService;
        this.queryExecutionAccelerationRuntimeService = queryExecutionAccelerationRuntimeService;
        this.queryExecutionCacheGovernanceRuntimeService = queryExecutionCacheGovernanceRuntimeService;
        this.hetuRouteCalibrationService = hetuRouteCalibrationService;
    }

    @PostMapping("/benchmark/workload/capture")
    public QueryExecutionBenchmarkWorkloadResponse captureWorkload(@RequestBody QueryExecutionBenchmarkWorkloadRequest request) {
        return queryExecutionBenchmarkWorkloadService.capture(request);
    }

    @GetMapping("/hetu/route-calibration")
    public HetuRouteCalibrationResponse calibrationSnapshot() {
        HetuRouteCalibrationSnapshot snapshot = hetuRouteCalibrationService.currentSnapshot();
        return new HetuRouteCalibrationResponse(
            snapshot.isEnabled(),
            snapshot.getRouteProfile(),
            modeNames(snapshot.getDeclaredAllowedModes()),
            snapshot.routeOrderNames(),
            snapshot.isSkipUnreadyModes(),
            snapshot.getEvidenceSource(),
            snapshot.getLiveVerificationStatus(),
            snapshot.getReadonlyBoundary(),
            snapshot.getSummary(),
            snapshot.getClusterEvidence(),
            snapshot.getModeCalibrations(),
            "LONG_TERM_BASELINE",
            "HETU_ROUTE_CALIBRATION_BASELINE"
        );
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

    @PostMapping("/cache-policies/apply")
    public QueryExecutionCachePolicyResponse applyCachePolicy(
        @RequestBody QueryExecutionCachePolicyApplyRequest request
    ) {
        return queryExecutionCacheGovernanceRuntimeService.apply(request);
    }

    @PostMapping("/cache-policies/verify")
    public QueryExecutionCachePolicyResponse verifyCachePolicy(
        @RequestBody QueryExecutionCachePolicyVerifyRequest request
    ) {
        return queryExecutionCacheGovernanceRuntimeService.verify(request);
    }

    @PostMapping("/cache-policies/invalidate")
    public QueryExecutionCachePolicyResponse invalidateCachePolicy(
        @RequestBody QueryExecutionCachePolicyInvalidateRequest request
    ) {
        return queryExecutionCacheGovernanceRuntimeService.invalidate(request);
    }

    private List<String> modeNames(List<com.company.queryexecution.domain.query.QueryExecutionAccessMode> modes) {
        List<String> names = new ArrayList<String>();
        if (modes == null) {
            return names;
        }
        for (com.company.queryexecution.domain.query.QueryExecutionAccessMode mode : modes) {
            if (mode != null) {
                names.add(mode.name());
            }
        }
        return names;
    }
}
