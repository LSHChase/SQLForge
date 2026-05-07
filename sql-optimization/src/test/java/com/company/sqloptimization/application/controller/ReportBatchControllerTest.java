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
                .content("{\"tenantId\":\"tenant-a\",\"batchName\":\"report-batch-alpha\",\"fileName\":\"report-batch-alpha.txt\","
                    + "\"reportCodeField\":\"report_code\",\"datasourceCode\":\"hetu_main\",\"stage\":\"PROD\","
                    + "\"priority\":\"high\",\"contentBase64\":\"" + encoded + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("READY"))
            .andExpect(jsonPath("$.fileType").value("TXT"))
            .andExpect(jsonPath("$.totalSqls").value(2))
            .andExpect(jsonPath("$.reportItems.length()").value(2))
            .andReturn();

        String batchId = JsonTestUtils.readValue(imported.getResponse().getContentAsString(), "$.batchId");

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/report-batches")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].batchId").value(batchId))
            .andExpect(jsonPath("$[0].fileType").value("TXT"));

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/report-batches/{batchId}/resolve-sqls", batchId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.resolvedReports").value(2))
            .andExpect(jsonPath("$.resolvedSqls").value(2))
            .andExpect(jsonPath("$.parseStatistics.overview.totalSqlCount").value(2))
            .andExpect(jsonPath("$.parseStatistics.sqlStatistics.length()").value(2))
            .andExpect(jsonPath("$.reportItems[0].sqlText").exists())
            .andExpect(jsonPath("$.reportItems[0].structureSyntaxStatus").value("VALID"));

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/report-batches/{batchId}", batchId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.batchId").value(batchId))
            .andExpect(jsonPath("$.parseStatistics.reportStatistics.length()").value(2));

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/report-batches/{batchId}", batchId)
                .param("pageNumber", "1")
                .param("pageSize", "1")
                .param("reportCode", "RPT_B")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itemPageNumber").value(1))
            .andExpect(jsonPath("$.itemPageSize").value(1))
            .andExpect(jsonPath("$.itemTotalCount").value(1))
            .andExpect(jsonPath("$.itemReportCodeFilter").value("RPT_B"))
            .andExpect(jsonPath("$.reportItems.length()").value(1))
            .andExpect(jsonPath("$.reportItems[0].reportCode").value("RPT_B"));

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/report-batches/{batchId}/parse-statistics", batchId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.overview.totalSqlCount").value(2))
            .andExpect(jsonPath("$.sqlStatistics.length()").value(2))
            .andExpect(jsonPath("$.priorityMatrix.length()").value(1));

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/report-batches/{batchId}/parse-statistics", batchId)
                .param("pageNumber", "1")
                .param("pageSize", "1")
                .param("reportCode", "RPT_B")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.overview.totalSqlCount").value(2))
            .andExpect(jsonPath("$.sqlStatisticPageNumber").value(1))
            .andExpect(jsonPath("$.sqlStatisticPageSize").value(1))
            .andExpect(jsonPath("$.sqlStatisticTotalCount").value(1))
            .andExpect(jsonPath("$.sqlStatisticReportCodeFilter").value("RPT_B"))
            .andExpect(jsonPath("$.sqlStatistics.length()").value(1))
            .andExpect(jsonPath("$.sqlStatistics[0].reportCode").value("RPT_B"));
    }

    @Test
    void shouldExposeInvalidReportSqlDiagnosticsInJson() throws Exception {
        String csv = "report_code,sql_1\nRPT_BAD,\"SELECT FROM\"";
        String encoded = Base64.getEncoder().encodeToString(csv.getBytes(StandardCharsets.UTF_8));
        MvcResult imported = mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/report-batches/import"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"batchName\":\"report-batch-invalid\","
                    + "\"fileName\":\"report-batch-invalid.csv\",\"reportCodeField\":\"report_code\","
                    + "\"datasourceCode\":\"hetu_main\",\"stage\":\"PROD\",\"priority\":\"high\","
                    + "\"contentBase64\":\"" + encoded + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
        String batchId = JsonTestUtils.readValue(imported.getResponse().getContentAsString(), "$.batchId");

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/report-batches/{batchId}/resolve-sqls", batchId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PARTIAL_COMPLETED"))
            .andExpect(jsonPath("$.reportItems[0].reportCode").value("RPT_BAD"))
            .andExpect(jsonPath("$.reportItems[0].sqlColumnName").value("sql_1"))
            .andExpect(jsonPath("$.reportItems[0].failureLine").value(1))
            .andExpect(jsonPath("$.reportItems[0].failureColumn").value(8))
            .andExpect(jsonPath("$.reportItems[0].failureToken").value("FROM"))
            .andExpect(jsonPath("$.reportItems[0].failureSnippet").isNotEmpty())
            .andExpect(jsonPath("$.reportItems[0].issueLocations[0].locationSnippet").value("SELECT FROM"))
            .andExpect(jsonPath("$.reportItems[0].diagnosticSummary").isNotEmpty());
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
