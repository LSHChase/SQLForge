package com.company.benchmarkengine.application.controller;

import com.company.benchmarkengine.application.service.BenchmarkArtifactGovernanceOperationService;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactOperationRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactOperationResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/benchmark-engine/internal")
public class BenchmarkInternalController {

    private final BenchmarkArtifactGovernanceOperationService benchmarkArtifactGovernanceOperationService;

    public BenchmarkInternalController(
        BenchmarkArtifactGovernanceOperationService benchmarkArtifactGovernanceOperationService
    ) {
        this.benchmarkArtifactGovernanceOperationService = benchmarkArtifactGovernanceOperationService;
    }

    @PostMapping("/artifact-operations")
    public GovernanceBenchmarkArtifactOperationResponse operateArtifact(
        @RequestBody GovernanceBenchmarkArtifactOperationRequest request
    ) {
        return benchmarkArtifactGovernanceOperationService.operate(request);
    }
}
