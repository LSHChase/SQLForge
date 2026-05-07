package com.company.governance.application.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.governance.application.controller.vo.GovernanceQueryHistoryPageVO;
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
        item.setAccessChannel("JDBC_AGENT");
        when(service.findQueryHistoryPage(
            "tenant-a",
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
                .param("accessChannel", "jdbc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].historyId").value("history-001"))
            .andExpect(jsonPath("$.items[0].accessChannel").value("JDBC_AGENT"))
            .andExpect(jsonPath("$.totalCount").value(1))
            .andExpect(jsonPath("$.pageCount").value(1));

        verify(service).findQueryHistoryPage(
            "tenant-a",
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
            null
        );
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
            .andExpect(jsonPath("$.message").value("accessChannel must be one of PAGE/API/JDBC_AGENT/SDK/CLIENT"));
    }
}
