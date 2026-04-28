package com.company.governance.application.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.governance.application.controller.vo.GovernanceAlertDetailVO;
import com.company.governance.application.controller.vo.GovernanceAlertPageVO;
import com.company.governance.application.controller.vo.GovernanceAlertSummaryVO;
import com.company.governance.application.service.GovernanceAlertApplicationService;
import com.company.sqlforge.common.exception.GlobalExceptionHandler;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GovernanceAlertControllerTest {

    @Test
    void shouldExposeAlertPageEndpoint() throws Exception {
        GovernanceAlertApplicationService service = org.mockito.Mockito.mock(GovernanceAlertApplicationService.class);
        GovernanceAlertSummaryVO item = new GovernanceAlertSummaryVO();
        item.setAlertId("alert-001");
        item.setAlertStatus("OPEN");
        when(service.findAlertPage("tenant-a", "OPEN", null, null, Integer.valueOf(1), Integer.valueOf(10)))
            .thenReturn(new GovernanceAlertPageVO(
                Collections.singletonList(item),
                Integer.valueOf(1),
                Integer.valueOf(10),
                Integer.valueOf(1),
                Boolean.FALSE
            ));
        GovernanceAlertController controller = new GovernanceAlertController(service);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        mockMvc.perform(get("/api/governance/alerts")
                .param("tenantId", "tenant-a")
                .param("alertStatus", "OPEN")
                .param("pageNo", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].alertId").value("alert-001"))
            .andExpect(jsonPath("$.items[0].alertStatus").value("OPEN"));

        verify(service).findAlertPage("tenant-a", "OPEN", null, null, Integer.valueOf(1), Integer.valueOf(10));
    }

    @Test
    void shouldExposeAlertAckEndpoint() throws Exception {
        GovernanceAlertApplicationService service = org.mockito.Mockito.mock(GovernanceAlertApplicationService.class);
        GovernanceAlertDetailVO detail = new GovernanceAlertDetailVO();
        detail.setAlertId("alert-001");
        detail.setAlertStatus("ACKED");
        detail.setAckedBy("operator-001");
        when(service.ackAlert(
            org.mockito.Mockito.eq("tenant-a"),
            org.mockito.Mockito.eq("alert-001"),
            org.mockito.Mockito.isNull(),
            org.mockito.ArgumentMatchers.any(Instant.class)
        )).thenReturn(detail);
        GovernanceAlertController controller = new GovernanceAlertController(service);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        mockMvc.perform(post("/api/governance/alerts/alert-001/ack")
                .param("tenantId", "tenant-a"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.alertId").value("alert-001"))
            .andExpect(jsonPath("$.alertStatus").value("ACKED"));

        verify(service).ackAlert(
            org.mockito.Mockito.eq("tenant-a"),
            org.mockito.Mockito.eq("alert-001"),
            org.mockito.Mockito.isNull(),
            org.mockito.ArgumentMatchers.any(Instant.class)
        );
    }
}
