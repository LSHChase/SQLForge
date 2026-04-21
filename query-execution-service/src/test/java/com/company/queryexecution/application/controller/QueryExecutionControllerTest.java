package com.company.queryexecution.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.queryexecution.application.controller.vo.QueryErrorDetailVO;
import com.company.queryexecution.application.controller.vo.QueryExecuteResponse;
import com.company.queryexecution.application.controller.vo.QueryExecutionMetadataVO;
import com.company.queryexecution.application.service.QueryExecutionContractApplicationService;
import com.company.queryexecution.domain.query.QueryExecutionStatus;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
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
    private QueryExecutionContractApplicationService queryExecutionContractApplicationService;

    @Test
    void shouldReturnQueryContractResponse() throws Exception {
        when(queryExecutionContractApplicationService.describeExecutionContract(any()))
            .thenReturn(new QueryExecuteResponse(
                QueryExecutionStatus.FAILED,
                Collections.emptyList(),
                null,
                new QueryExecutionMetadataVO("HETU", "SELECT 1", 0L, 0L, false, false),
                false,
                null,
                Collections.emptyList(),
                new QueryErrorDetailVO(
                    ErrorCodeConstants.QUERY_EXECUTION_SYSTEM_PIPELINE_NOT_READY,
                    ErrorCodeConstants.QUERY_EXECUTION_PIPELINE_NOT_READY_MESSAGE,
                    "Continue with D-TASK-003 to implement the synchronous execution pipeline.",
                    false
                ),
                "fingerprint-001",
                "LONG_TERM_BASELINE",
                "TRANSITIONAL_SKELETON"
            ));

        mockMvc.perform(post("/api/query-execution/queries/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sqlText\":\"SELECT 1\",\"tenantId\":\"tenant-a\",\"datasourceType\":\"HETU\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.metadata.targetEngine").value("HETU"))
            .andExpect(jsonPath("$.error.code").value(ErrorCodeConstants.QUERY_EXECUTION_SYSTEM_PIPELINE_NOT_READY))
            .andExpect(jsonPath("$.contractStage").value("LONG_TERM_BASELINE"))
            .andExpect(jsonPath("$.implementationStage").value("TRANSITIONAL_SKELETON"));

        verify(queryExecutionContractApplicationService).describeExecutionContract(any());
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
