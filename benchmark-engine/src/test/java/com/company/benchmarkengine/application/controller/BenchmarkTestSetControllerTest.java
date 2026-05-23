package com.company.benchmarkengine.application.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.benchmarkengine.BenchmarkEngineApplication;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationCombinedParseStatus;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationParseResultClient;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationStructureParseIssue;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationStructureParseStatus;
import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Base64;
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
class BenchmarkTestSetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GovernanceCapabilityClient governanceCapabilityClient;

    @MockBean
    private SqlOptimizationParseResultClient sqlOptimizationParseResultClient;

    @Test
    void shouldImportBenchmarkTestSetAndSupportLookup() throws Exception {
        String contentBase64 = Base64.getEncoder().encodeToString((
            "case_name,sql_text,report_code,tags\n"
                + "safe,SELECT * FROM orders,report-001,comparison;route\n"
                + "unsafe,DELETE FROM orders,report-002,unsafe\n"
        ).getBytes(StandardCharsets.UTF_8));

        MvcResult createResult = mockMvc.perform(addProtectedHeaders(post("/api/benchmark-engine/test-sets"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"testSetName\":\"route-governance-import\","
                    + "\"templateId\":\"comparison-dual-engine\",\"templateType\":\"CROSS_ENGINE_COMPARISON\","
                    + "\"templateVersion\":\"v2026.04\",\"testSetSource\":\"BATCH_IMPORT\","
                    + "\"testSetLabels\":[{\"type\":\"SCENARIO\",\"value\":\"COMPARISON\"}],"
                    + "\"importRequest\":{\"fileType\":\"CSV\",\"fileName\":\"comparison.csv\","
                    + "\"contentBase64\":\"" + contentBase64 + "\","
                    + "\"fieldMappings\":["
                    + "{\"field\":\"CASE_NAME\",\"columnName\":\"case_name\"},"
                    + "{\"field\":\"SQL_TEXT\",\"columnName\":\"sql_text\"},"
                    + "{\"field\":\"REPORT_CODE\",\"columnName\":\"report_code\"},"
                    + "{\"field\":\"TAGS\",\"columnName\":\"tags\"}]}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PARTIAL_READY"))
            .andExpect(jsonPath("$.acceptedCases").value(1))
            .andExpect(jsonPath("$.rejectedCases").value(1))
            .andExpect(jsonPath("$.importBatchId", startsWith("import-")))
            .andExpect(jsonPath("$.testSetSourceRefs[0].type").value("IMPORT_BATCH"))
            .andExpect(jsonPath("$.cases[1].status").value("REJECTED"))
            .andExpect(header().exists(RequestHeaderConstants.TRACE_ID))
            .andReturn();

        String testSetId = JsonTestUtils.readValue(createResult.getResponse().getContentAsString(), "$.testSetId");

        mockMvc.perform(addProtectedHeaders(get("/api/benchmark-engine/test-sets/{testSetId}", testSetId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.testSetId").value(testSetId))
            .andExpect(jsonPath("$.status").value("PARTIAL_READY"))
            .andExpect(jsonPath("$.cases[0].caseName").value("safe"))
            .andExpect(jsonPath("$.cases[1].rejectionReason").value(org.hamcrest.Matchers.containsString("只读压测边界")));
    }

    @Test
    void shouldRejectMissingProtectedHeaders() throws Exception {
        mockMvc.perform(post("/api/benchmark-engine/test-sets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"testSetName\":\"import\","
                    + "\"testSetSource\":\"BATCH_IMPORT\","
                    + "\"importRequest\":{\"fileType\":\"CSV\",\"fileName\":\"x.csv\","
                    + "\"contentBase64\":\"Y2FzZV9uYW1lLHNxbF90ZXh0CnNhZmUsU0VMRUNUIDEK\","
                    + "\"fieldMappings\":[{\"field\":\"CASE_NAME\",\"columnName\":\"case_name\"},"
                    + "{\"field\":\"SQL_TEXT\",\"columnName\":\"sql_text\"}]}}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(10002));
    }

    @Test
    void shouldRejectUnknownTestSetId() throws Exception {
        mockMvc.perform(addProtectedHeaders(get("/api/benchmark-engine/test-sets/{testSetId}", "missing-set")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(23001));
    }

    @Test
    void shouldGenerateBenchmarkTestSetFromSingleParseTask() throws Exception {
        org.mockito.Mockito.when(sqlOptimizationParseResultClient.getCombinedParseStatus("parse-001"))
            .thenReturn(combinedParseStatus());

        mockMvc.perform(addProtectedHeaders(post("/api/benchmark-engine/test-sets/parse-results"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"testSetName\":\"parse-generated\","
                    + "\"templateId\":\"comparison-dual-engine\",\"templateType\":\"CROSS_ENGINE_COMPARISON\","
                    + "\"templateVersion\":\"v2026.04\",\"parseTaskId\":\"parse-001\","
                    + "\"sqlText\":\"SELECT * FROM orders\",\"includeIssueScenes\":[\"comparison\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.testSetSource").value("PARSE_RESULT_GENERATION"))
            .andExpect(jsonPath("$.status").value("READY"))
            .andExpect(jsonPath("$.acceptedCases").value(1))
            .andExpect(jsonPath("$.testSetSourceRefs[0].type").value("PARSE_TASK"))
            .andExpect(jsonPath("$.cases[0].caseName").value("PARSE-TASK-parse-001"));
    }

    @Test
    void shouldRejectInvalidParseResultSelector() throws Exception {
        mockMvc.perform(addProtectedHeaders(post("/api/benchmark-engine/test-sets/parse-results"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"testSetName\":\"parse-generated\","
                    + "\"parseTaskId\":\"parse-001\",\"parseBatchId\":\"batch-001\","
                    + "\"sqlText\":\"SELECT 1\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(10001));
    }

    private MockHttpServletRequestBuilder addProtectedHeaders(MockHttpServletRequestBuilder builder) {
        long now = System.currentTimeMillis();
        return builder
            .header(RequestHeaderConstants.TENANT_ID, "tenant-a")
            .header(RequestHeaderConstants.USER_ID, "user-001")
            .header(RequestHeaderConstants.REQUEST_ID, "request-001")
            .header(RequestHeaderConstants.TRACE_ID, "trace-001")
            .header(RequestHeaderConstants.AUTH_SOURCE, AuthSourceConstants.HEADER)
            .header(RequestHeaderConstants.ISSUED_AT, String.valueOf(now - 1000L))
            .header(RequestHeaderConstants.EXPIRES_AT, String.valueOf(now + 60000L));
    }

    private SqlOptimizationCombinedParseStatus combinedParseStatus() {
        SqlOptimizationCombinedParseStatus status = new SqlOptimizationCombinedParseStatus();
        status.setParseTaskId("parse-001");
        status.setStatus("SUCCEEDED");
        SqlOptimizationStructureParseStatus structure = new SqlOptimizationStructureParseStatus();
        structure.setSyntaxStatus("VALID");
        structure.setImportant(Boolean.TRUE);
        structure.setUrgent(Boolean.FALSE);
        structure.setPriorityLevel("P1");
        SqlOptimizationStructureParseIssue issue = new SqlOptimizationStructureParseIssue();
        issue.setIssueScene("comparison");
        structure.setIssues(Collections.singletonList(issue));
        status.setStructureParse(structure);
        return status;
    }
}
