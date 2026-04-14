package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.SqlPressurePlanRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlScenarioBlueprintServiceTest {

    @Test
    void shouldBuildScenarioBlueprintFromDailySqlBatch() {
        SqlScenarioBlueprintService service = new SqlScenarioBlueprintService(
            new SqlPressurePlanService(
                new SqlIntentBatchAnalysisService(new SqlIntentAnalysisService(new SqlAssessmentService(), new HeuristicSqlParserAdapter())),
                new SqlIntentAnalysisService(new SqlAssessmentService(), new HeuristicSqlParserAdapter())
            )
        );
        SqlPressurePlanRequest request = new SqlPressurePlanRequest();
        request.setBatchId("scenario-batch");
        request.setSource("daily-sql");
        request.setTargetConcurrency(Integer.valueOf(40));
        request.setRawSqlText(
            "select id from lake.users where id = 1 limit 1;\n\n"
                + "select user_id, sum(amount) from lake.orders where ds >= '2026-04-01' group by 1;\n\n"
                + "select user_id, row_number() over(partition by ds order by amount desc) from lake.orders"
        );

        Map<String, Object> blueprint = service.buildBlueprint(request);

        assertEquals("pure-structure-scenario-blueprint", blueprint.get("analysisMode"));
        assertEquals(Integer.valueOf(3), blueprint.get("parsedStatementCount"));
        assertTrue(castList(blueprint.get("stages")).size() >= 4);
        assertTrue(castList(blueprint.get("workloadMix")).size() >= 2);
        assertTrue(castStringList(blueprint.get("operatorChecklist")).size() >= 4);
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
