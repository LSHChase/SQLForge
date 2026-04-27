package com.company.sqloptimization.application.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqloptimization.SqlOptimizationApplication;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
class ReportBatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldImportAndResolveMockReportCatalog() throws Exception {
        String encoded = Base64.getEncoder().encodeToString((
            "RPT_A|Revenue Report|hetu_main|PROD|high\n"
                + "RPT_B|Ops Report|hetu_main|PROD|medium\n"
        ).getBytes(StandardCharsets.UTF_8));

        MvcResult imported = mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/report-batches/import"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"batchName\":\"report-batch-alpha\",\"fileType\":\"TXT\","
                    + "\"reportCodeField\":\"report_code\",\"datasourceCode\":\"hetu_main\",\"stage\":\"PROD\","
                    + "\"priority\":\"high\",\"contentBase64\":\"" + encoded + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("READY"))
            .andExpect(jsonPath("$.reportItems.length()").value(2))
            .andReturn();

        String batchId = JsonTestUtils.readValue(imported.getResponse().getContentAsString(), "$.batchId");

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/report-batches/{batchId}/resolve-sqls", batchId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.resolvedReports").value(2))
            .andExpect(jsonPath("$.reportItems[0].sqlText").exists())
            .andExpect(jsonPath("$.reportItems[0].structureSyntaxStatus").value("VALID"));

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/report-batches/{batchId}", batchId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.batchId").value(batchId));
    }

    private MockHttpServletRequestBuilder addProtectedHeaders(MockHttpServletRequestBuilder builder) {
        long now = System.currentTimeMillis();
        return builder
            .header(RequestHeaderConstants.TENANT_ID, "tenant-a")
            .header(RequestHeaderConstants.USER_ID, "operator-001")
            .header(RequestHeaderConstants.ROLE_CODES, "TENANT_ADMIN,OPERATOR")
            .header(RequestHeaderConstants.REQUEST_ID, "request-report-001")
            .header(RequestHeaderConstants.TRACE_ID, "trace-report-001")
            .header(RequestHeaderConstants.AUTH_SOURCE, AuthSourceConstants.HEADER)
            .header(RequestHeaderConstants.ISSUED_AT, String.valueOf(now - 1000L))
            .header(RequestHeaderConstants.EXPIRES_AT, String.valueOf(now + 60000L));
    }
}
