package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.SqlPressurePlanRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlPressurePlanServiceTest {

    @Test
    void shouldBuildPressurePlanFromDailySqlBatch() {
        SqlPressurePlanService service = new SqlPressurePlanService(
            new SqlIntentBatchAnalysisService(new SqlIntentAnalysisService(new SqlAssessmentService())),
            new SqlIntentAnalysisService(new SqlAssessmentService())
        );
        SqlPressurePlanRequest request = new SqlPressurePlanRequest();
        request.setBatchId("batch-20260414");
        request.setSource("daily-sql");
        request.setTargetConcurrency(Integer.valueOf(48));
        request.setRawSqlText(
            "select id, user_name from lake.users where id = 42 limit 1;\n\n"
                + "select o.user_id, sum(o.amount) from lake.orders o join lake.dim_users u on o.user_id = u.user_id "
                + "where o.ds between '2026-04-01' and '2026-04-14' group by 1;\n\n"
                + "select user_id, row_number() over(partition by ds order by amount desc) from lake.orders"
        );

        Map<String, Object> result = service.buildPlan(request);

        assertEquals("pure-structure-pressure-plan", result.get("analysisMode"));
        assertEquals(Integer.valueOf(3), result.get("parsedStatementCount"));
        assertTrue(castMap(result.get("candidatePack")).containsKey("heavySet"));
        assertTrue(castMap(result.get("executionMatrix")).containsKey("concurrencyLadder"));
        assertTrue(castMap(result.get("samplingPlan")).containsKey("rules"));
    }

    @Test
    void shouldCreateDiverseSmokeSetAcrossLoadClasses() {
        SqlPressurePlanService service = new SqlPressurePlanService(
            new SqlIntentBatchAnalysisService(new SqlIntentAnalysisService(new SqlAssessmentService())),
            new SqlIntentAnalysisService(new SqlAssessmentService())
        );
        SqlPressurePlanRequest request = new SqlPressurePlanRequest();
        request.setBatchId("batch-diverse");
        request.setSource("daily-sql");
        request.setTargetConcurrency(Integer.valueOf(20));
        request.setRawSqlText(
            "select id from lake.users where id = 1 limit 1;\n"
                + "select user_id, sum(amount) from lake.orders where ds >= '2026-04-01' group by 1;\n"
                + "select user_id, row_number() over(partition by ds order by amount desc) from lake.orders"
        );

        Map<String, Object> result = service.buildPlan(request);
        Map<String, Object> candidatePack = castMap(result.get("candidatePack"));
        List<Map<String, Object>> smokeSet = castList(candidatePack.get("smokeSet"));

        assertTrue(smokeSet.size() >= 2);
        assertTrue(hasLoadClass(smokeSet, "point-read") || hasLoadClass(smokeSet, "range-read") || hasLoadClass(smokeSet, "wide-read"));
    }

    private boolean hasLoadClass(List<Map<String, Object>> items, String expected) {
        for (Map<String, Object> item : items) {
            if (expected.equals(String.valueOf(item.get("loadClass")))) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }
}
