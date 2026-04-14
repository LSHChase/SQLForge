package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.SqlPressurePlanRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlBriefingReportServiceTest {

    @Test
    void shouldBuildOperatorBriefingFromRunPackage() {
        SqlBriefingReportService service = new SqlBriefingReportService(
            new SqlRunPackageService(
                new SqlCampaignScheduleService(
                    new SqlExecutionManifestService(
                        new SqlScenarioBlueprintService(
                            new SqlPressurePlanService(
                                new SqlIntentBatchAnalysisService(new SqlIntentAnalysisService(new SqlAssessmentService())),
                                new SqlIntentAnalysisService(new SqlAssessmentService())
                            )
                        )
                    )
                )
            )
        );
        SqlPressurePlanRequest request = new SqlPressurePlanRequest();
        request.setBatchId("briefing-batch");
        request.setSource("daily-sql");
        request.setTargetConcurrency(Integer.valueOf(48));
        request.setRawSqlText(
            "select id from lake.users where id = 1 limit 1;\n\n"
                + "select user_id, sum(amount) from lake.orders where ds >= '2026-04-01' group by 1;\n\n"
                + "select user_id, row_number() over(partition by ds order by amount desc) from lake.orders"
        );

        Map<String, Object> report = service.buildReport(request);

        assertEquals("pure-structure-briefing-report", report.get("analysisMode"));
        assertEquals("2026-04-14", report.get("reportVersion"));
        assertTrue(castMap(report.get("executiveSummary")).containsKey("headline"));
        assertTrue(castList(report.get("riskFocus")).size() >= 2);
        assertTrue(castList(report.get("reviewAgenda")).size() >= 3);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
