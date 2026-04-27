package com.company.sqloptimization.application.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.sqloptimization.SqlOptimizationApplication;
import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
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

    @Test
    void shouldIngestCsvBatchAndRetryUnavailableAccess() throws Exception {
        MvcResult createResult = mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse-batches"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"batchName\":\"batch-csv\",\"importMode\":\"TABULAR_FILE\","
                    + "\"fileType\":\"CSV\",\"templateVersion\":\"v1\",\"datasourceCode\":\"hetu_main\","
                    + "\"structureParseOnly\":false}"))
            .andExpect(status().isOk())
            .andReturn();
        String batchId = JsonTestUtils.readValue(createResult.getResponse().getContentAsString(), "$.batchId");
        String csv = "report_code,datasource,sql_text\n"
            + "RPT_A,hetu_main,SELECT * FROM orders WHERE dt = '2026-04-01'\n"
            + "RPT_B,unavailable_ds,SELECT * FROM vw_orders WHERE dt = '2026-04-02'\n";
        String encoded = Base64.getEncoder().encodeToString(csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse-batches/{batchId}/ingest", batchId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contentBase64\":\"" + encoded + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PARTIAL_COMPLETED"))
            .andExpect(jsonPath("$.importedRecords.length()").value(2))
            .andExpect(jsonPath("$.accessParseStatistics.partialSuccessRecords").value(1))
            .andExpect(jsonPath("$.reportStatistics[0].reportCode").value("RPT_A"));

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse-batches/{batchId}/retry-access", batchId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"failureFilter\":\"UNAVAILABLE\",\"datasourceCode\":\"hetu_main\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.accessParseStatistics.successRecords").value(2));
    }

    @Test
    void shouldIngestXlsxBatch() throws Exception {
        MvcResult createResult = mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse-batches"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"batchName\":\"batch-xlsx\",\"importMode\":\"TABULAR_FILE\","
                    + "\"fileType\":\"XLSX\",\"templateVersion\":\"v1\",\"datasourceCode\":\"hetu_main\","
                    + "\"structureParseOnly\":true}"))
            .andExpect(status().isOk())
            .andReturn();
        String batchId = JsonTestUtils.readValue(createResult.getResponse().getContentAsString(), "$.batchId");
        String encoded = Base64.getEncoder().encodeToString(buildXlsxPayload());

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/parse-batches/{batchId}/ingest", batchId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contentBase64\":\"" + encoded + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.structureParseStatistics.successRecords").value(1))
            .andExpect(jsonPath("$.importedRecords[0].reportCode").value("RPT_XLSX"));
    }

    private byte[] buildXlsxPayload() throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("batch");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("report_code");
        header.createCell(1).setCellValue("sql_text");
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("RPT_XLSX");
        row.createCell(1).setCellValue("SELECT * FROM orders WHERE dt = '2026-04-03'");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
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
