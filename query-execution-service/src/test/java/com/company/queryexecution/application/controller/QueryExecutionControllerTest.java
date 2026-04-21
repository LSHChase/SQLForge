package com.company.queryexecution.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.queryexecution.application.controller.vo.QueryExecuteResponse;
import com.company.queryexecution.application.controller.vo.QueryExecutionMetadataVO;
import com.company.queryexecution.application.service.QueryExecutionApplicationService;
import com.company.queryexecution.domain.query.QueryExecutionStatus;
import com.company.sqlforge.common.exception.GlobalExceptionHandler;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(QueryExecutionController.class)
@Import(GlobalExceptionHandler.class)
class QueryExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryExecutionApplicationService queryExecutionApplicationService;

    @Test
    void shouldReturnSynchronousQueryResponse() throws Exception {
        when(queryExecutionApplicationService.executeSynchronously(any()))
            .thenReturn(new QueryExecuteResponse(
                QueryExecutionStatus.SUCCESS,
                Collections.singletonList(Collections.<String, Object>singletonMap("engine", "HETU")),
                null,
                new QueryExecutionMetadataVO("HETU", "SELECT 1", 74L, 32L, false, true),
                false,
                null,
                Collections.emptyList(),
                null,
                "fingerprint-001",
                "LONG_TERM_BASELINE",
                "MINIMAL_SYNC_BASELINE"
            ));

        mockMvc.perform(post("/api/query-execution/queries/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT 1\",\"tenantId\":\"tenant-a\",\"datasourceType\":\"HETU\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.metadata.targetEngine").value("HETU"))
            .andExpect(jsonPath("$.rows[0].engine").value("HETU"))
            .andExpect(jsonPath("$.contractStage").value("LONG_TERM_BASELINE"))
            .andExpect(jsonPath("$.implementationStage").value("MINIMAL_SYNC_BASELINE"));

        verify(queryExecutionApplicationService).executeSynchronously(any());
    }

    @Test
    void shouldRejectInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/api/query-execution/queries/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"\",\"tenantId\":\"tenant-a\",\"datasourceType\":\"HETU\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(10001))
            .andExpect(jsonPath("$.message").value("sqlText is required"));
    }
}
