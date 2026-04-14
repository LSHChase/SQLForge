package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.SqlPressurePlanRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlCampaignScheduleServiceTest {

    @Test
    void shouldBuildCampaignScheduleFromExecutionManifest() {
        SqlCampaignScheduleService service = new SqlCampaignScheduleService(
            new SqlExecutionManifestService(
                new SqlScenarioBlueprintService(
                    new SqlPressurePlanService(
                        new SqlIntentBatchAnalysisService(new SqlIntentAnalysisService(new SqlAssessmentService(), new HeuristicSqlParserAdapter())),
                        new SqlIntentAnalysisService(new SqlAssessmentService(), new HeuristicSqlParserAdapter())
                    )
                )
            )
        );
        SqlPressurePlanRequest request = new SqlPressurePlanRequest();
        request.setBatchId("campaign-batch");
        request.setSource("daily-sql");
        request.setTargetConcurrency(Integer.valueOf(40));
        request.setRawSqlText(
            "select id from lake.users where id = 1 limit 1;\n\n"
                + "select user_id, sum(amount) from lake.orders where ds >= '2026-04-01' group by 1;\n\n"
                + "select user_id, row_number() over(partition by ds order by amount desc) from lake.orders"
        );

        Map<String, Object> schedule = service.buildSchedule(request);

        assertEquals("pure-structure-campaign-schedule", schedule.get("analysisMode"));
        assertEquals("2026-04-14", schedule.get("scheduleVersion"));
        assertTrue(castList(schedule.get("stages")).size() >= 4);
        assertTrue(castMap(schedule.get("campaignSummary")).containsKey("totalDurationMinutes"));
        assertTrue(castStringList(schedule.get("handoffNotes")).size() >= 4);
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
