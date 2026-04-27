package com.company.sqloptimization.application.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.sqloptimization.SqlOptimizationApplication;
import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(classes = SqlOptimizationApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ParseBatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateAndFetchParseBatchContract() throws Exception {
        MvcResult createResult = mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse-batches"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"batchName\":\"batch-alpha\",\"importMode\":\"TABULAR_FILE\","
                    + "\"fileType\":\"XLSX\",\"templateVersion\":\"v1\",\"datasourceCode\":\"hetu_main\","
                    + "\"structureParseOnly\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("READY"))
            .andExpect(jsonPath("$.templateColumns[8].columnKey").value("sql_text"))
            .andExpect(header().exists(RequestHeaderConstants.TRACE_ID))
            .andReturn();

        String batchId = JsonTestUtils.readValue(createResult.getResponse().getContentAsString(), "$.batchId");

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/parse-batches/{batchId}", batchId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.batchId").value(batchId))
            .andExpect(jsonPath("$.sourceType").value("FILE_UPLOAD"))
            .andExpect(jsonPath("$.statusHistory[0].currentStatus").value("UPLOADED"))
            .andExpect(jsonPath("$.supportedFileTypes[0]").value("XLSX"));
    }

    private MockHttpServletRequestBuilder addProtectedHeaders(MockHttpServletRequestBuilder builder) {
        long now = System.currentTimeMillis();
        return builder
            .header(RequestHeaderConstants.TENANT_ID, "tenant-a")
            .header(RequestHeaderConstants.USER_ID, "operator-001")
            .header(RequestHeaderConstants.ROLE_CODES, "TENANT_ADMIN,OPERATOR")
            .header(RequestHeaderConstants.REQUEST_ID, "request-001")
            .header(RequestHeaderConstants.TRACE_ID, "trace-001")
            .header(RequestHeaderConstants.AUTH_SOURCE, AuthSourceConstants.HEADER)
            .header(RequestHeaderConstants.ISSUED_AT, String.valueOf(now - 1000L))
            .header(RequestHeaderConstants.EXPIRES_AT, String.valueOf(now + 60000L));
    }
}
