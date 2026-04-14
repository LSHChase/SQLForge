package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.SqlPressurePlanRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlExecutionManifestServiceTest {

    @Test
    void shouldBuildExecutionManifestFromScenarioBlueprint() {
        SqlExecutionManifestService service = new SqlExecutionManifestService(
            new SqlScenarioBlueprintService(
                new SqlPressurePlanService(
                    new SqlIntentBatchAnalysisService(new SqlIntentAnalysisService(new SqlAssessmentService(), new HeuristicSqlParserAdapter())),
                    new SqlIntentAnalysisService(new SqlAssessmentService(), new HeuristicSqlParserAdapter())
                )
            )
        );
        SqlPressurePlanRequest request = new SqlPressurePlanRequest();
        request.setBatchId("manifest-batch");
        request.setSource("daily-sql");
        request.setTargetConcurrency(Integer.valueOf(36));
        request.setRawSqlText(
            "select id from lake.users where id = 1 limit 1;\n\n"
                + "select user_id, sum(amount) from lake.orders where ds >= '2026-04-01' group by 1;\n\n"
                + "select user_id, row_number() over(partition by ds order by amount desc) from lake.orders"
        );

        Map<String, Object> manifest = service.buildManifest(request);

        assertEquals("pure-structure-execution-manifest", manifest.get("analysisMode"));
        assertEquals("2026-04-14", manifest.get("manifestVersion"));
        assertTrue(castList(manifest.get("stages")).size() >= 4);
        assertTrue(castStringList(manifest.get("globalGuardrails")).size() >= 4);
        assertTrue(castMap(manifest.get("artifactSummary")).containsKey("heavySetSize"));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }

    @SuppressWarnings("unchecked")
    private List<String> castStringList(Object value) {
        return (List<String>) value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
