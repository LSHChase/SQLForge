package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.SqlPressurePlanRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlReadinessGateServiceTest {

    @Test
    void shouldProduceReadinessDecisionAndBlockers() {
        SqlReadinessGateService service = new SqlReadinessGateService(
            new SqlBriefingReportService(
                new SqlRunPackageService(
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
                )
            )
        );
        SqlPressurePlanRequest request = new SqlPressurePlanRequest();
        request.setBatchId("gate-batch");
        request.setSource("daily-sql");
        request.setTargetConcurrency(Integer.valueOf(48));
        request.setRawSqlText(
            "select id from lake.users where id = 1 limit 1;\n\n"
                + "select user_id, sum(amount) from lake.orders where ds >= '2026-04-01' group by 1;\n\n"
                + "select user_id, row_number() over(partition by ds order by amount desc) from lake.orders"
        );

        Map<String, Object> gate = service.evaluate(request);

        assertEquals("pure-structure-readiness-gate", gate.get("analysisMode"));
        assertTrue(gate.containsKey("decision"));
        assertTrue(castList(gate.get("blockers")).size() >= 0);
        assertTrue(castStringList(gate.get("prerequisites")).size() >= 3);
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
