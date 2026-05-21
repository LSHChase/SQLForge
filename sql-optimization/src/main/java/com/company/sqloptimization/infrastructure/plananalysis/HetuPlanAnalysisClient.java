package com.company.sqloptimization.infrastructure.plananalysis;

import com.company.sqloptimization.domain.parse.HetuPlanAnalysisResult;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.Collections;

public interface HetuPlanAnalysisClient {

    HetuPlanAnalysisResult explain(String sqlText, String tenantId, String datasourceCode);

    default HetuPlanAnalysisResult explain(String sqlText,
                                           String tenantId,
                                           String datasourceCode,
                                           DataSourceTypeEnum datasourceType) {
        return explain(sqlText, tenantId, datasourceCode);
    }

    static HetuPlanAnalysisClient unavailable() {
        return new HetuPlanAnalysisClient() {
            @Override
            public HetuPlanAnalysisResult explain(String sqlText, String tenantId, String datasourceCode) {
                return HetuPlanAnalysisResult.failed(
                    datasourceCode,
                    "HETU_PLAN_CLIENT_UNAVAILABLE",
                    0L,
                    Collections.singletonList("client=不可用")
                );
            }
        };
    }
}
