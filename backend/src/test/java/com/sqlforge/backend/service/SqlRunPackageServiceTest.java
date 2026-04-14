package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.SqlPressurePlanRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlRunPackageServiceTest {

    @Test
    void shouldBuildRunPackageFromCampaignSchedule() {
        SqlRunPackageService service = new SqlRunPackageService(
            new SqlCampaignScheduleService(
                new SqlExecutionManifestService(
                    new SqlScenarioBlueprintService(
                        new SqlPressurePlanService(
                            new SqlIntentBatchAnalysisService(new SqlIntentAnalysisService(new SqlAssessmentService(), new HeuristicSqlParserAdapter())),
                            new SqlIntentAnalysisService(new SqlAssessmentService(), new HeuristicSqlParserAdapter())
                        )
                    )
                )
            )
        );
        SqlPressurePlanRequest request = new SqlPressurePlanRequest();
        request.setBatchId("run-package-batch");
        request.setSource("daily-sql");
        request.setTargetConcurrency(Integer.valueOf(48));
        request.setRawSqlText(
            "select id from lake.users where id = 1 limit 1;\n\n"
                + "select user_id, sum(amount) from lake.orders where ds >= '2026-04-01' group by 1;\n\n"
                + "select user_id, row_number() over(partition by ds order by amount desc) from lake.orders"
        );

        Map<String, Object> runPackage = service.buildPackage(request);

        assertEquals("pure-structure-run-package", runPackage.get("analysisMode"));
        assertEquals("2026-04-14", runPackage.get("packageVersion"));
        assertTrue(castList(runPackage.get("artifacts")).size() >= 5);
        assertTrue(castList(runPackage.get("recommendedFiles")).size() >= 5);
        assertTrue(castStringList(runPackage.get("handoffChecklist")).size() >= 4);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }

    @SuppressWarnings("unchecked")
    private List<String> castStringList(Object value) {
        return (List<String>) value;
    }
}
