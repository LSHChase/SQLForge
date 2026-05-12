package com.company.queryexecution.application.controller;

import com.company.queryexecution.application.controller.dto.RuntimeRewriteBindingPublishRequest;
import com.company.queryexecution.application.controller.dto.RuntimeRewriteBindingResolveRequest;
import com.company.queryexecution.application.controller.dto.RuntimeRewriteBindingResponse;
import com.company.queryexecution.application.controller.dto.RuntimeRewriteBindingStateChangeRequest;
import com.company.queryexecution.application.controller.vo.HetuRouteCalibrationResponse;
import com.company.queryexecution.application.service.HetuRouteCalibrationService;
import com.company.queryexecution.application.service.QueryExecutionAccelerationRuntimeService;
import com.company.queryexecution.application.service.QueryExecutionBenchmarkWorkloadService;
import com.company.queryexecution.application.service.QueryExecutionCacheGovernanceRuntimeService;
import com.company.queryexecution.application.service.QueryExecutionResultDigestService;
import com.company.queryexecution.application.service.QueryExecutionRuntimeRewriteBindingService;
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
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestResponse;
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
    private final QueryExecutionResultDigestService queryExecutionResultDigestService;
    private final QueryExecutionRuntimeRewriteBindingService queryExecutionRuntimeRewriteBindingService;

    public QueryExecutionInternalController(QueryExecutionBenchmarkWorkloadService queryExecutionBenchmarkWorkloadService,
                                            QueryExecutionAccelerationRuntimeService queryExecutionAccelerationRuntimeService,
                                            QueryExecutionCacheGovernanceRuntimeService queryExecutionCacheGovernanceRuntimeService,
                                            HetuRouteCalibrationService hetuRouteCalibrationService,
                                            QueryExecutionResultDigestService queryExecutionResultDigestService,
                                            QueryExecutionRuntimeRewriteBindingService queryExecutionRuntimeRewriteBindingService) {
        this.queryExecutionBenchmarkWorkloadService = queryExecutionBenchmarkWorkloadService;
        this.queryExecutionAccelerationRuntimeService = queryExecutionAccelerationRuntimeService;
        this.queryExecutionCacheGovernanceRuntimeService = queryExecutionCacheGovernanceRuntimeService;
        this.hetuRouteCalibrationService = hetuRouteCalibrationService;
        this.queryExecutionResultDigestService = queryExecutionResultDigestService;
        this.queryExecutionRuntimeRewriteBindingService = queryExecutionRuntimeRewriteBindingService;
    }

    @PostMapping("/benchmark/workload/capture")
    public QueryExecutionBenchmarkWorkloadResponse captureWorkload(@RequestBody QueryExecutionBenchmarkWorkloadRequest request) {
        return queryExecutionBenchmarkWorkloadService.capture(request);
    }

    @PostMapping("/result-digests/execute")
    public QueryExecutionResultDigestResponse executeResultDigest(@RequestBody QueryExecutionResultDigestRequest request) {
        return queryExecutionResultDigestService.executeDigest(request);
    }

    @PostMapping("/rewrite-bindings/publish")
    public RuntimeRewriteBindingResponse publishRuntimeRewriteBinding(
        @RequestBody RuntimeRewriteBindingPublishRequest request
    ) {
        return queryExecutionRuntimeRewriteBindingService.publish(request);
    }

    @PostMapping("/rewrite-bindings/resolve-active")
    public RuntimeRewriteBindingResponse resolveActiveRuntimeRewriteBinding(
        @RequestBody RuntimeRewriteBindingResolveRequest request
    ) {
        return queryExecutionRuntimeRewriteBindingService.resolveActive(request);
    }

    @PostMapping("/rewrite-bindings/pause")
    public RuntimeRewriteBindingResponse pauseRuntimeRewriteBinding(
        @RequestBody RuntimeRewriteBindingStateChangeRequest request
    ) {
        return queryExecutionRuntimeRewriteBindingService.pause(request);
    }

    @PostMapping("/rewrite-bindings/unpublish")
    public RuntimeRewriteBindingResponse unpublishRuntimeRewriteBinding(
        @RequestBody RuntimeRewriteBindingStateChangeRequest request
    ) {
        return queryExecutionRuntimeRewriteBindingService.unpublish(request);
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
