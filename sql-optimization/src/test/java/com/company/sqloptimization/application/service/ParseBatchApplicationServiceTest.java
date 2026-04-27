package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqloptimization.application.controller.dto.ParseBatchCreateRequest;
import com.company.sqloptimization.application.controller.dto.ParseBatchIngestRequest;
import com.company.sqloptimization.application.controller.dto.ParseBatchRetryAccessRequest;
import com.company.sqloptimization.application.controller.vo.ParseBatchStatusResponse;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqloptimization.infrastructure.repository.InMemoryParseBatchItemRepository;
import com.company.sqloptimization.infrastructure.repository.InMemoryParseBatchRepository;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ParseBatchApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldCreateReadyParseBatchWithTemplateContract() {
        ParseBatchApplicationService service = buildService();
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        ParseBatchStatusResponse response = service.createBatch(baseRequest("TABULAR_FILE", "XLSX"));

        assertEquals("READY", response.getStatus());
        assertEquals("FILE_UPLOAD", response.getSourceType());
        assertEquals(Integer.valueOf(3), Integer.valueOf(response.getStatusHistory().size()));
        assertEquals("sql_text", response.getTemplateColumns().get(8).getColumnKey());
    }

    @Test
    void shouldRejectUnsupportedFileTypeForSqlFileMode() {
        ParseBatchApplicationService service = buildService();
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        BizException exception = assertThrows(BizException.class, () -> service.createBatch(baseRequest("SQL_FILE", "XLSX")));

        assertEquals(10001, exception.getCode());
    }

    @Test
    void shouldIngestSqlFileAndRetryPartialAccessRows() {
        ParseBatchApplicationService service = buildService();
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        ParseBatchStatusResponse created = service.createBatch(baseRequest("SQL_FILE", "SQL"));

        ParseBatchIngestRequest ingestRequest = new ParseBatchIngestRequest();
        ingestRequest.setContentBase64(Base64.getEncoder().encodeToString((
            "SELECT * FROM orders WHERE dt = '2026-04-01';\n"
                + "SELECT * FROM vw_orders WHERE dt = '2026-04-02';"
        ).getBytes(StandardCharsets.UTF_8)));
        ParseBatchStatusResponse ingested = service.ingestBatch(created.getBatchId(), ingestRequest);

        assertEquals(Integer.valueOf(2), ingested.getTotalRecords());
        assertEquals("COMPLETED", ingested.getStatus());
        assertEquals(Integer.valueOf(2), ingested.getImportedRecords().size());
        assertEquals("VALID", ingested.getImportedRecords().get(0).getStructureSyntaxStatus());
        assertEquals("AVAILABLE", ingested.getImportedRecords().get(0).getAccessServiceStatus());

        ParseBatchRetryAccessRequest retryRequest = new ParseBatchRetryAccessRequest();
        retryRequest.setFailureFilter("ALL");
        ParseBatchStatusResponse retried = service.retryAccess(created.getBatchId(), retryRequest);

        assertEquals("COMPLETED", retried.getStatus());
        assertEquals(Integer.valueOf(2), retried.getSuccessRecords());
    }

    private ParseBatchApplicationService buildService() {
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        StructureParseApplicationService structureService = new StructureParseApplicationService(
            new SqlOptimizationPipelineService(),
            governanceCapabilityClient
        );
        AccessParseApplicationService accessService = new AccessParseApplicationService();
        return new ParseBatchApplicationService(
            new InMemoryParseBatchRepository(),
            new InMemoryParseBatchItemRepository(),
            structureService,
            accessService
        );
    }

    private ParseBatchCreateRequest baseRequest(String importMode, String fileType) {
        ParseBatchCreateRequest request = new ParseBatchCreateRequest();
        request.setTenantId("tenant-a");
        request.setBatchName("batch-alpha");
        request.setImportMode(importMode);
        request.setFileType(fileType);
        request.setTemplateVersion("v1");
        request.setDatasourceCode("hetu_main");
        request.setStructureParseOnly(Boolean.FALSE);
        return request;
    }
}
