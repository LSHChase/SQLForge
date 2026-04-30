package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.governance.domain.trace.entity.ExecutionResultRecord;
import com.company.governance.domain.trace.entity.ConfigSnapshotRecord;
import com.company.governance.domain.trace.entity.QueryHistoryRecord;
import com.company.governance.infrastructure.persistence.mapper.ConfigSnapshotMapper;
import com.company.governance.infrastructure.persistence.mapper.ExecutionResultMapper;
import com.company.governance.infrastructure.persistence.mapper.QueryHistoryMapper;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.governance.GovernanceParseHistoryWriteRequest;
import com.company.sqlforge.common.governance.GovernanceParseHistoryWriteResponse;
import com.company.sqlforge.common.utils.JsonUtils;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GovernanceParseHistoryTraceabilityApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldNormalizeFlatCommentContextAndParseSummariesWhenInsertingHistory() {
        GovernanceProtectedPersistenceService protectedPersistenceService = org.mockito.Mockito.mock(GovernanceProtectedPersistenceService.class);
        ConfigSnapshotMapper configSnapshotMapper = org.mockito.Mockito.mock(ConfigSnapshotMapper.class);
        ExecutionResultMapper executionResultMapper = org.mockito.Mockito.mock(ExecutionResultMapper.class);
        QueryHistoryMapper queryHistoryMapper = org.mockito.Mockito.mock(QueryHistoryMapper.class);
        GovernanceParseHistoryTraceabilityApplicationService service = new GovernanceParseHistoryTraceabilityApplicationService(
            protectedPersistenceService,
            configSnapshotMapper,
            executionResultMapper,
            queryHistoryMapper
        );
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);
        when(configSnapshotMapper.selectById("cfg-parse-task-001")).thenReturn(null);
        when(executionResultMapper.selectById("result-parse-task-001")).thenReturn(null);
        when(queryHistoryMapper.selectById("history-parse-task-001")).thenReturn(null);

        GovernanceParseHistoryWriteRequest request = new GovernanceParseHistoryWriteRequest();
        request.setParseTaskId("task-001");
        request.setSqlFingerprint("fp-001");
        request.setDatasourceCode("hetu_main");
        request.setDatasourceType("AUTO");
        request.setSqlText("SELECT * FROM vw_sales_daily WHERE dt = '2026-04-01'");
        request.setSqlTemplateText("SELECT * FROM vw_sales_daily WHERE dt = ?");
        request.setBindingMode("POSITIONAL");
        request.setResultStatus("SUCCESS");
        request.setResultSummaryJson("{\"overallStatus\":\"SUCCESS\"}");
        request.setResultPayloadJson(
            "{\"structureParse\":{\"parseType\":\"STRUCTURE\",\"syntaxStatus\":\"VALID\","
                + "\"queryDateSummary\":{\"queryDateStart\":\"2026-04-01\",\"queryDateEnd\":\"2026-04-01\","
                + "\"queryDateStatus\":\"RESOLVED\"},\"logicalObjectHits\":[{\"objectKey\":\"DB_VIEW:vw_sales_daily\"}]},"
                + "\"accessParse\":{\"parseType\":\"ACCESS\",\"serviceStatus\":\"AVAILABLE\",\"connectionStatus\":\"CONNECTED\"}}"
        );
        request.setQueryContextJson("{\"report_code\":\"RPT_SALES_DAILY\",\"stage\":\"PROD\",\"biz_date\":\"2026-04-01\",\"datasource\":\"hetu_main\"}");
        request.setLogicalObjectHitsJson("[{\"objectKey\":\"DB_VIEW:vw_sales_daily\"}]");
        request.setSubmittedAt("2026-04-29T12:00:00Z");

        GovernanceParseHistoryWriteResponse response = service.writeParseHistory(request);

        assertEquals("history-parse-task-001", response.getHistoryId());
        ArgumentCaptor<ConfigSnapshotRecord> configCaptor = ArgumentCaptor.forClass(ConfigSnapshotRecord.class);
        verify(protectedPersistenceService).saveConfigSnapshot(configCaptor.capture());
        assertEquals("cfg-parse-task-001", configCaptor.getValue().getConfigSnapshotId());
        assertEquals("SQL_PARSE_HISTORY", configCaptor.getValue().getSourceConfigType());
        assertEquals("task-001", configCaptor.getValue().getSourceConfigId());
        ArgumentCaptor<QueryHistoryRecord> historyCaptor = ArgumentCaptor.forClass(QueryHistoryRecord.class);
        verify(protectedPersistenceService).saveQueryHistoryWithSqlSurfaces(
            historyCaptor.capture(),
            eq("SELECT * FROM vw_sales_daily WHERE dt = '2026-04-01'"),
            eq("SELECT * FROM vw_sales_daily WHERE dt = ?"),
            eq(null)
        );
        QueryHistoryRecord history = historyCaptor.getValue();
        assertEquals("RPT_SALES_DAILY", JsonUtils.fromJson(history.getCommentContext(), Map.class).get("report_code"));
        Map queryContext = JsonUtils.fromJson(history.getQueryContext(), Map.class);
        assertEquals("PAGE", queryContext.get("accessChannel"));
        assertEquals("2026-04-01", queryContext.get("queryDateStart"));
        assertEquals("RESOLVED", queryContext.get("queryDateStatus"));
        assertTrue(queryContext.containsKey("commentContext"));
        assertTrue(queryContext.containsKey("structureParseSummary"));
        assertTrue(queryContext.containsKey("accessParseSummary"));
        assertTrue(queryContext.containsKey("bindingSummary"));
        assertNotNull(history.getLogicalObjectHits());
    }

    @Test
    void shouldUpdateExistingParseHistoryForCombinedAccessResult() {
        GovernanceProtectedPersistenceService protectedPersistenceService = org.mockito.Mockito.mock(GovernanceProtectedPersistenceService.class);
        ConfigSnapshotMapper configSnapshotMapper = org.mockito.Mockito.mock(ConfigSnapshotMapper.class);
        ExecutionResultMapper executionResultMapper = org.mockito.Mockito.mock(ExecutionResultMapper.class);
        QueryHistoryMapper queryHistoryMapper = org.mockito.Mockito.mock(QueryHistoryMapper.class);
        GovernanceParseHistoryTraceabilityApplicationService service = new GovernanceParseHistoryTraceabilityApplicationService(
            protectedPersistenceService,
            configSnapshotMapper,
            executionResultMapper,
            queryHistoryMapper
        );
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-002", "trace-002", "header", 1L, 2L);
        when(configSnapshotMapper.selectById("cfg-parse-task-002")).thenReturn(new ConfigSnapshotRecord());
        when(executionResultMapper.selectById("result-parse-task-002")).thenReturn(new ExecutionResultRecord());
        QueryHistoryRecord existing = new QueryHistoryRecord();
        existing.setHistoryId("history-parse-task-002");
        when(queryHistoryMapper.selectById("history-parse-task-002")).thenReturn(existing);

        GovernanceParseHistoryWriteRequest request = new GovernanceParseHistoryWriteRequest();
        request.setParseTaskId("task-002");
        request.setSqlFingerprint("fp-002");
        request.setDatasourceCode("hetu_main");
        request.setDatasourceType("AUTO");
        request.setSqlText("SELECT * FROM orders");
        request.setResultStatus("PARTIAL");
        request.setResultSummaryJson("{\"overallStatus\":\"PARTIAL\"}");
        request.setResultPayloadJson(
            "{\"structureParse\":{\"syntaxStatus\":\"VALID\"},"
                + "\"accessParse\":{\"serviceStatus\":\"UNAVAILABLE\",\"connectionStatus\":\"UNAVAILABLE\","
                + "\"degradeReason\":\"ACCESS_PARSE_SERVICE_UNAVAILABLE\"}}"
        );
        request.setQueryContextJson("{\"report_code\":\"RPT_ACCESS_RETRY\",\"stage\":\"PROD\"}");

        service.writeParseHistory(request);

        verify(protectedPersistenceService).updateExecutionResult(any(ExecutionResultRecord.class));
        verify(protectedPersistenceService, never()).saveQueryHistoryWithSqlSurfaces(
            any(QueryHistoryRecord.class),
            any(String.class),
            any(String.class),
            any(String.class)
        );
        ArgumentCaptor<QueryHistoryRecord> historyCaptor = ArgumentCaptor.forClass(QueryHistoryRecord.class);
        verify(protectedPersistenceService).updateQueryHistoryWithSqlSurfaces(
            historyCaptor.capture(),
            eq("SELECT * FROM orders"),
            eq(null),
            eq(null)
        );
        Map queryContext = JsonUtils.fromJson(historyCaptor.getValue().getQueryContext(), Map.class);
        assertTrue(queryContext.containsKey("accessParseSummary"));
        assertEquals("RPT_ACCESS_RETRY", JsonUtils.fromJson(historyCaptor.getValue().getCommentContext(), Map.class).get("report_code"));
    }
}
