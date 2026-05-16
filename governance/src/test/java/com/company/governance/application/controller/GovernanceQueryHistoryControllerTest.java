package com.company.governance.application.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.governance.application.controller.vo.GovernanceQueryHistoryPageVO;
import com.company.governance.application.controller.vo.GovernanceQueryHistoryRewriteRecordVO;
import com.company.governance.application.controller.vo.GovernanceQueryHistoryRewriteRecordsVO;
import com.company.governance.application.controller.vo.GovernanceQueryHistorySummaryVO;
import com.company.governance.application.service.GovernanceHistoryApplicationService;
import com.company.sqlforge.common.exception.GlobalExceptionHandler;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GovernanceQueryHistoryControllerTest {

    @Test
    void shouldNormalizeAccessChannelAliasForHistoryFilter() throws Exception {
        GovernanceHistoryApplicationService service = org.mockito.Mockito.mock(GovernanceHistoryApplicationService.class);
        GovernanceQueryHistorySummaryVO item = new GovernanceQueryHistorySummaryVO();
        item.setHistoryId("history-001");
        item.setTenantId("tenant-a");
        item.setAccessChannel("JDBC_AGENT");
        when(service.findQueryHistoryPage(
            "tenant-a",
            "QUERY_EXECUTION",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "JDBC_AGENT",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )).thenReturn(new GovernanceQueryHistoryPageVO(
            Collections.singletonList(item),
            Integer.valueOf(1),
            Integer.valueOf(20),
            Integer.valueOf(1),
            Integer.valueOf(1),
            Boolean.FALSE,
            Collections.<String, Object>emptyMap()
        ));
        GovernanceQueryHistoryController controller = new GovernanceQueryHistoryController(service);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        mockMvc.perform(get("/api/governance/query-history")
                .param("tenantId", "tenant-a")
                .param("historyType", "query_execution")
                .param("accessChannel", "jdbc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].historyId").value("history-001"))
            .andExpect(jsonPath("$.items[0].tenantId").value("tenant-a"))
            .andExpect(jsonPath("$.items[0].accessChannel").value("JDBC_AGENT"))
            .andExpect(jsonPath("$.totalCount").value(1))
            .andExpect(jsonPath("$.pageCount").value(1));

        verify(service).findQueryHistoryPage(
            "tenant-a",
            "QUERY_EXECUTION",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "JDBC_AGENT",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    @Test
    void shouldRejectUnknownHistoryTypeFilter() throws Exception {
        GovernanceHistoryApplicationService service = org.mockito.Mockito.mock(GovernanceHistoryApplicationService.class);
        GovernanceQueryHistoryController controller = new GovernanceQueryHistoryController(service);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        mockMvc.perform(get("/api/governance/query-history")
                .param("tenantId", "tenant-a")
                .param("historyType", "REPORT_IMPORT"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(10001))
            .andExpect(jsonPath("$.message").value(GovernanceHistoryApplicationService.allowedHistoryTypeMessage()));
    }

    @Test
    void shouldRejectUnknownAccessChannelFilter() throws Exception {
        GovernanceHistoryApplicationService service = org.mockito.Mockito.mock(GovernanceHistoryApplicationService.class);
        GovernanceQueryHistoryController controller = new GovernanceQueryHistoryController(service);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        mockMvc.perform(get("/api/governance/query-history")
                .param("tenantId", "tenant-a")
                .param("accessChannel", "batch_job"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(10001))
            .andExpect(jsonPath("$.message").value("accessChannel 必须是 PAGE/API/JDBC_AGENT/SDK/CLIENT 之一"));
    }

    @Test
    void shouldExposeQueryHistoryRewriteRecordsAggregationEndpoint() throws Exception {
        GovernanceHistoryApplicationService service = org.mockito.Mockito.mock(GovernanceHistoryApplicationService.class);
        GovernanceQueryHistoryRewriteRecordVO item = new GovernanceQueryHistoryRewriteRecordVO();
        item.setRewriteRecordId("rewrite-001");
        item.setTenantId("tenant-a");
        item.setHistoryId("history-001");
        item.setValidationStatus("DIVERGED");
        item.setAlertStatus("OPEN");
        GovernanceQueryHistoryRewriteRecordsVO response = new GovernanceQueryHistoryRewriteRecordsVO();
        response.setTenantId("tenant-a");
        response.setHistoryId("history-001");
        response.setRewriteRecordCount(Integer.valueOf(1));
        response.setItems(Collections.singletonList(item));
        response.setContractStage("LONG_TERM_BASELINE");
        response.setImplementationStage("QUERY_HISTORY_REWRITE_RECORD_AGGREGATION");
        when(service.findQueryHistoryRewriteRecords("tenant-a", "history-001")).thenReturn(response);
        GovernanceQueryHistoryController controller = new GovernanceQueryHistoryController(service);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        mockMvc.perform(get("/api/governance/query-history/history-001/rewrite-records")
                .param("tenantId", "tenant-a"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rewriteRecordCount").value(1))
            .andExpect(jsonPath("$.items[0].rewriteRecordId").value("rewrite-001"))
            .andExpect(jsonPath("$.items[0].validationStatus").value("DIVERGED"))
            .andExpect(jsonPath("$.implementationStage").value("QUERY_HISTORY_REWRITE_RECORD_AGGREGATION"));

        verify(service).findQueryHistoryRewriteRecords("tenant-a", "history-001");
    }
}
