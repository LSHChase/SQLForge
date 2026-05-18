package com.company.benchmarkengine.application.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.benchmarkengine.BenchmarkEngineApplication;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(classes = BenchmarkEngineApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BenchmarkReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GovernanceCapabilityClient governanceCapabilityClient;

    @Test
    void shouldQueryJsonReportByReportId() throws Exception {
        String reportId = submitSucceededTaskAndReadReportId();

        mockMvc.perform(addProtectedHeaders(get("/api/benchmark-engine/reports/{reportId}", reportId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reportId").value(reportId))
            .andExpect(jsonPath("$.requestedFormat").value("JSON"))
            .andExpect(jsonPath("$.availableFormats[0]").value("JSON"))
            .andExpect(jsonPath("$.availableFormats[1]").value("PDF"))
            .andExpect(jsonPath("$.availableFormats[2]").value("HTML"))
            .andExpect(jsonPath("$.targetEngines[0]").value("HETU"))
            .andExpect(jsonPath("$.scaleReadiness.readinessStatus").value("NOT_PROVEN"))
            .andExpect(jsonPath("$.scaleReadiness.scaleTarget.targetConcurrency").value(10000))
            .andExpect(jsonPath("$.scaleReadiness.observedQueueWaitMs").exists())
            .andExpect(jsonPath("$.trendCharts[0].chartType").value("LATENCY_DISTRIBUTION_HISTOGRAM"))
            .andExpect(jsonPath("$.reportQueryPath").value("/api/benchmark-engine/reports/" + reportId))
            .andExpect(jsonPath("$.rawDataDownloadPath").value("/api/benchmark-engine/reports/" + reportId + "/raw-data"))
            .andExpect(jsonPath("$.implementationStage").value("EXTERNALIZED_ARTIFACT_GOVERNANCE_TRACE_BASELINE"));
    }

    @Test
    void shouldRenderPdfAndHtmlReportFormats() throws Exception {
        String reportId = submitSucceededTaskAndReadReportId();

        mockMvc.perform(addProtectedHeaders(get("/api/benchmark-engine/reports/{reportId}", reportId).queryParam("format", "PDF")))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition", containsString(".pdf")))
            .andExpect(content().string(startsWith("%PDF-1.4")));

        mockMvc.perform(addProtectedHeaders(get("/api/benchmark-engine/reports/{reportId}", reportId).queryParam("format", "HTML")))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(header().string("Content-Disposition", endsWith(".html\"")))
            .andExpect(content().string(containsString("SQLForge 压测报告")));
    }

    @Test
    void shouldRejectUnknownFormatAndMissingReport() throws Exception {
        mockMvc.perform(addProtectedHeaders(get("/api/benchmark-engine/reports/{reportId}", "missing-report")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(23002));

        mockMvc.perform(addProtectedHeaders(get("/api/benchmark-engine/reports/{reportId}", "missing-report").queryParam("format", "CSV")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(10001));
    }

    @Test
    void shouldQueryRawDataAndRejectCrossTenantAccess() throws Exception {
        String reportId = submitSucceededTaskAndReadReportId();

        mockMvc.perform(addProtectedHeaders(get("/api/benchmark-engine/reports/{reportId}/raw-data", reportId)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(header().string("Content-Disposition", containsString("benchmark-raw-data-")))
            .andExpect(content().string(containsString(reportId)))
            .andExpect(content().string(containsString("\"scaleReadiness\"")));

        mockMvc.perform(addProtectedHeaders(get("/api/benchmark-engine/reports/{reportId}", reportId), "tenant-b"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(10003));
    }

    private String submitSucceededTaskAndReadReportId() throws Exception {
        MvcResult submitResult = mockMvc.perform(addProtectedHeaders(post("/api/benchmark-engine/tasks"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"taskType\":\"COMPARISON\",\"sqlText\":\"SELECT * FROM orders\","
                    + "\"taskContext\":{\"priority\":\"HIGH\",\"targetEngines\":[\"HETU\",\"HIVE\"],"
                    + "\"concurrency\":16,\"durationSeconds\":300,\"rampUpSeconds\":30,"
                    + "\"datasetSizeLabel\":\"TEN_GB\","
                    + "\"scaleTarget\":{\"targetConcurrency\":10000,\"targetDatasetSizeLabel\":\"THIRTY_PB\","
                    + "\"targetDailyQueryVolume\":10000000,\"targetComplexityProfile\":\"HIGH_COMPLEXITY_SELECT\","
                    + "\"targetCostEfficiency\":\"minimize-scan-cpu-and-cost-per-query\"},"
                    + "\"readonlyRequired\":true,\"shadowEnvironmentMode\":\"REQUIRED\"}}"))
            .andExpect(status().isOk())
            .andReturn();

        String taskId = JsonTestUtils.readValue(submitResult.getResponse().getContentAsString(), "$.taskId");
        for (int attempt = 0; attempt < 20; attempt++) {
            MvcResult statusResult = mockMvc.perform(addProtectedHeaders(get("/api/benchmark-engine/tasks/{taskId}", taskId)))
                .andExpect(status().isOk())
                .andReturn();
            String status = JsonTestUtils.readValue(statusResult.getResponse().getContentAsString(), "$.status");
            if ("SUCCEEDED".equals(status)) {
                return JsonTestUtils.readValue(statusResult.getResponse().getContentAsString(), "$.reportId");
            }
            Thread.sleep(40L);
        }
        throw new AssertionError("Benchmark report was not generated in time");
    }

    private MockHttpServletRequestBuilder addProtectedHeaders(MockHttpServletRequestBuilder builder) {
        return addProtectedHeaders(builder, "tenant-a");
    }

    private MockHttpServletRequestBuilder addProtectedHeaders(MockHttpServletRequestBuilder builder, String tenantId) {
        long now = System.currentTimeMillis();
        return builder
            .header(RequestHeaderConstants.TENANT_ID, tenantId)
            .header(RequestHeaderConstants.USER_ID, "operator-001")
            .header(RequestHeaderConstants.ROLE_CODES, "TENANT_ADMIN,OPERATOR")
            .header(RequestHeaderConstants.REQUEST_ID, "request-001")
            .header(RequestHeaderConstants.TRACE_ID, "trace-001")
            .header(RequestHeaderConstants.AUTH_SOURCE, AuthSourceConstants.HEADER)
            .header(RequestHeaderConstants.ISSUED_AT, String.valueOf(now - 1000L))
            .header(RequestHeaderConstants.EXPIRES_AT, String.valueOf(now + 60000L));
    }
}
