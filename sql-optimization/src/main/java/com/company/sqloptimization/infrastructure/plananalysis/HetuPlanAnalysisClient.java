package com.company.sqloptimization.infrastructure.plananalysis;

import com.company.sqloptimization.domain.parse.HetuPlanAnalysisResult;
import java.util.Collections;

public interface HetuPlanAnalysisClient {

    HetuPlanAnalysisResult explain(String sqlText, String tenantId, String datasourceCode);

    static HetuPlanAnalysisClient unavailable() {
        return new HetuPlanAnalysisClient() {
            @Override
            public HetuPlanAnalysisResult explain(String sqlText, String tenantId, String datasourceCode) {
                return HetuPlanAnalysisResult.failed(
                    datasourceCode,
                    "HETU_PLAN_CLIENT_UNAVAILABLE",
                    0L,
                    Collections.singletonList("client=unavailable")
                );
            }
        };
    }
}
