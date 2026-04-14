package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.SqlIntentBatchRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlIntentBatchAnalysisServiceTest {

    @Test
    void shouldSplitSemicolonSeparatedDailySqlSamples() {
        SqlIntentBatchAnalysisService service = new SqlIntentBatchAnalysisService(
            new SqlIntentAnalysisService(new SqlAssessmentService(), new HeuristicSqlParserAdapter())
        );
        SqlIntentBatchRequest request = new SqlIntentBatchRequest();
        request.setBatchId("daily");
        request.setSource("stress-sample");
        request.setRawSqlText(
            "select id from lake.users where id = 1;\n"
                + "select user_id, sum(amount) from lake.orders where ds >= '2026-04-01' group by 1;"
        );

        Map<String, Object> result = service.analyzeBatch(request);

        assertEquals("daily", result.get("batchId"));
        assertEquals(Integer.valueOf(2), result.get("parsedStatementCount"));
        assertEquals("semicolon", result.get("splitMode"));
        assertEquals(Integer.valueOf(2), castMap(result.get("summary")).get("statementCount"));
    }

    @Test
    void shouldFallbackToBlankLineSplittingForDailyPasteBlocks() {
        SqlIntentBatchAnalysisService service = new SqlIntentBatchAnalysisService(
            new SqlIntentAnalysisService(new SqlAssessmentService(), new HeuristicSqlParserAdapter())
        );

        List<String> statements = service.splitStatements(
            "select id from lake.users where id = 1\n\n"
                + "-- next sample\n"
                + "select user_id, count(*) from lake.orders group by 1"
        );

        assertEquals(2, statements.size());
        assertTrue(statements.get(1).startsWith("select user_id"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
