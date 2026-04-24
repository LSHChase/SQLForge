package com.company.governance.infrastructure.benchmarkengine;

import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactOperationRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactOperationResponse;

public interface GovernanceBenchmarkEngineClient {

    GovernanceBenchmarkArtifactOperationResponse operateArtifact(GovernanceBenchmarkArtifactOperationRequest request);
}
