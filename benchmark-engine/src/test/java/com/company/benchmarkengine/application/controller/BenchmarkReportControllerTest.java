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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = BenchmarkEngineApplication.class)
@AutoConfigureMockMvc
class BenchmarkReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldQueryJsonReportByReportId() throws Exception {
        String reportId = submitSucceededTaskAndReadReportId();

        mockMvc.perform(get("/api/benchmark-engine/reports/{reportId}", reportId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reportId").value(reportId))
            .andExpect(jsonPath("$.requestedFormat").value("JSON"))
            .andExpect(jsonPath("$.availableFormats[0]").value("JSON"))
            .andExpect(jsonPath("$.availableFormats[1]").value("PDF"))
            .andExpect(jsonPath("$.availableFormats[2]").value("HTML"))
            .andExpect(jsonPath("$.targetEngines[0]").value("HETU"))
            .andExpect(jsonPath("$.trendCharts[0].chartType").value("LATENCY_DISTRIBUTION_HISTOGRAM"))
            .andExpect(jsonPath("$.reportQueryPath").value("/api/benchmark-engine/reports/" + reportId))
            .andExpect(jsonPath("$.rawDataDownloadPath").value("/api/benchmark-engine/reports/" + reportId + "/raw-data"))
            .andExpect(jsonPath("$.implementationStage").value("REPORT_QUERY_API_SKELETON"));
    }

    @Test
    void shouldRenderPdfAndHtmlReportFormats() throws Exception {
        String reportId = submitSucceededTaskAndReadReportId();

        mockMvc.perform(get("/api/benchmark-engine/reports/{reportId}", reportId).queryParam("format", "PDF"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition", containsString(".pdf")))
            .andExpect(content().string(startsWith("%PDF-1.4")));

        mockMvc.perform(get("/api/benchmark-engine/reports/{reportId}", reportId).queryParam("format", "HTML"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(header().string("Content-Disposition", endsWith(".html\"")))
            .andExpect(content().string(containsString("SQLForge Benchmark Report")));
    }

    @Test
    void shouldRejectUnknownFormatAndMissingReport() throws Exception {
        mockMvc.perform(get("/api/benchmark-engine/reports/{reportId}", "missing-report"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(23002));

        mockMvc.perform(get("/api/benchmark-engine/reports/{reportId}", "missing-report").queryParam("format", "CSV"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(10001));
    }

    private String submitSucceededTaskAndReadReportId() throws Exception {
        MvcResult submitResult = mockMvc.perform(post("/api/benchmark-engine/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"taskType\":\"COMPARISON\",\"sqlText\":\"SELECT * FROM orders\","
                    + "\"taskContext\":{\"priority\":\"HIGH\",\"targetEngines\":[\"HETU\",\"HIVE\"],"
                    + "\"concurrency\":16,\"durationSeconds\":300,\"rampUpSeconds\":30,"
                    + "\"readonlyRequired\":true,\"shadowEnvironmentMode\":\"REQUIRED\"}}"))
            .andExpect(status().isOk())
            .andReturn();

        String taskId = JsonTestUtils.readValue(submitResult.getResponse().getContentAsString(), "$.taskId");
        MvcResult statusResult = mockMvc.perform(get("/api/benchmark-engine/tasks/{taskId}", taskId))
            .andExpect(status().isOk())
            .andReturn();
        return JsonTestUtils.readValue(statusResult.getResponse().getContentAsString(), "$.reportId");
    }
}
