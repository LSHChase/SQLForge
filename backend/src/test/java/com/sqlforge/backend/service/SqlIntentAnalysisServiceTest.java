package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.SqlIntentAnalysisRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlIntentAnalysisServiceTest {

    @Test
    void shouldAnalyzeSqlStructureWithoutExecution() {
        SqlIntentAnalysisService service = new SqlIntentAnalysisService(new SqlAssessmentService(), new HeuristicSqlParserAdapter());
        SqlIntentAnalysisRequest request = request(
            statement(
                "daily-report",
                "ods-daily",
                "with recent_orders as (select user_id, amount from lake.orders where ds >= '2026-04-01') "
                    + "select user_id, sum(amount) from recent_orders group by 1 order by sum(amount) desc"
            )
        );

        Map<String, Object> result = service.analyze(request);
        List<Map<String, Object>> analyses = castList(result.get("analyses"));
        Map<String, Object> analysis = analyses.get(0);
        Map<String, Object> structure = castMap(analysis.get("structure"));
        Map<String, Object> pressureProfile = castMap(analysis.get("pressureProfile"));
        List<String> intentTags = castStringList(analysis.get("intentTags"));

        assertEquals("pure-structure", result.get("analysisMode"));
        assertEquals(Integer.valueOf(1), castMap(result.get("summary")).get("statementCount"));
        assertEquals("select", structure.get("statementType"));
        assertEquals(Integer.valueOf(1), structure.get("cteCount"));
        assertTrue(intentTags.contains("aggregation"));
        assertEquals("aggregation-heavy", pressureProfile.get("loadClass"));
    }

    @Test
    void shouldSummarizeBatchIntentAndLoadClasses() {
        SqlIntentAnalysisService service = new SqlIntentAnalysisService(new SqlAssessmentService(), new HeuristicSqlParserAdapter());
        SqlIntentAnalysisRequest request = request(
            statement("point", "daily-sample", "select id, user_name from lake.users where id = 42 limit 1"),
            statement(
                "join-heavy",
                "daily-sample",
                "select o.user_id, sum(o.amount) from lake.orders o "
                    + "join lake.dim_users u on o.user_id = u.user_id "
                    + "join lake.dim_region r on u.region_id = r.region_id "
                    + "where o.ds between '2026-04-01' and '2026-04-14' group by 1"
            )
        );

        Map<String, Object> result = service.analyze(request);
        Map<String, Object> summary = castMap(result.get("summary"));
        Map<String, Object> byLoadClass = castMap(summary.get("byLoadClass"));
        Map<String, Object> byIntentTag = castMap(summary.get("byIntentTag"));

        assertEquals(Integer.valueOf(2), summary.get("statementCount"));
        assertEquals(Integer.valueOf(1), byLoadClass.get("point-read"));
        assertEquals(Integer.valueOf(1), byLoadClass.get("aggregation-heavy"));
        assertTrue(byIntentTag.containsKey("point-check"));
        assertTrue(byIntentTag.containsKey("join-analysis"));
    }

    private SqlIntentAnalysisRequest request(SqlIntentAnalysisRequest.StatementInput... statements) {
        SqlIntentAnalysisRequest request = new SqlIntentAnalysisRequest();
        request.setStatements(Arrays.asList(statements));
        return request;
    }

    private SqlIntentAnalysisRequest.StatementInput statement(String id, String source, String sql) {
        SqlIntentAnalysisRequest.StatementInput item = new SqlIntentAnalysisRequest.StatementInput();
        item.setId(id);
        item.setSource(source);
        item.setSql(sql);
        return item;
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
