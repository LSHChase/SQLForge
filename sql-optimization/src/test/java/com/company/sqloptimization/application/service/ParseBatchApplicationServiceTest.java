package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqloptimization.application.controller.dto.ParseBatchCreateRequest;
import com.company.sqloptimization.application.controller.vo.ParseBatchStatusResponse;
import com.company.sqloptimization.infrastructure.repository.InMemoryParseBatchRepository;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ParseBatchApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldCreateReadyParseBatchWithTemplateContract() {
        ParseBatchApplicationService service = new ParseBatchApplicationService(new InMemoryParseBatchRepository());
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        ParseBatchStatusResponse response = service.createBatch(baseRequest("TABULAR_FILE", "XLSX"));

        assertEquals("READY", response.getStatus());
        assertEquals("FILE_UPLOAD", response.getSourceType());
        assertEquals(Integer.valueOf(3), Integer.valueOf(response.getStatusHistory().size()));
        assertEquals("sql_text", response.getTemplateColumns().get(8).getColumnKey());
    }

    @Test
    void shouldRejectUnsupportedFileTypeForSqlFileMode() {
        ParseBatchApplicationService service = new ParseBatchApplicationService(new InMemoryParseBatchRepository());
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        BizException exception = assertThrows(BizException.class, () -> service.createBatch(baseRequest("SQL_FILE", "XLSX")));

        assertEquals(10001, exception.getCode());
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
