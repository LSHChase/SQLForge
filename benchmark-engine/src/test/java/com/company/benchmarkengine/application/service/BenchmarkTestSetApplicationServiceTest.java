package com.company.benchmarkengine.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.company.benchmarkengine.application.controller.dto.BenchmarkTestSetCreateRequest;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTestSetFieldMappingDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTestSetImportRequest;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTestSetLabelDTO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTestSetResponse;
import com.company.benchmarkengine.domain.benchmark.BenchmarkSourceReferenceType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTemplateType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetField;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetFileType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetLabelType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetSource;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.benchmarkengine.infrastructure.repository.InMemoryBenchmarkTaskRepository;
import com.company.sqlforge.common.context.RequestContext;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BenchmarkTestSetApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldImportBenchmarkTestSetAndPreserveRejectedEvidence() {
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        InMemoryBenchmarkTaskRepository repository = new InMemoryBenchmarkTaskRepository();
        BenchmarkTestSetApplicationService service = new BenchmarkTestSetApplicationService(
            new BenchmarkTestSetModelApplicationService(),
            repository,
            governanceCapabilityClient
        );
        RequestContext.set("tenant-a", "user-001", "request-001", "trace-001", "header", 1L, 2L);

        BenchmarkTestSetResponse created = service.createTestSet(baseRequest(
            "case_name,sql_text,report_code,tags\n"
                + "safe,SELECT * FROM orders,report-001,comparison;route\n"
                + "unsafe,DELETE FROM orders,report-002,unsafe\n"
        ));

        assertEquals(BenchmarkTestSetSource.BATCH_IMPORT, created.getTestSetSource());
        assertEquals("PARTIAL_READY", created.getStatus().name());
        assertEquals(Integer.valueOf(2), created.getTotalCases());
        assertEquals(Integer.valueOf(1), created.getAcceptedCases());
        assertEquals(Integer.valueOf(1), created.getRejectedCases());
        assertEquals(BenchmarkSourceReferenceType.IMPORT_BATCH, created.getTestSetSourceRefs().get(0).getType());
        assertEquals("safe", created.getCases().get(0).getCaseName());
        assertEquals("unsafe", created.getCases().get(1).getCaseName());
        assertTrue(created.getCases().get(1).getRejectionReason().contains("只读压测边界"));

        BenchmarkTestSetResponse loaded = service.getTestSet(created.getTestSetId());

        assertEquals(created.getTestSetId(), loaded.getTestSetId());
        assertEquals(2, loaded.getCases().size());
        assertEquals("comparison", loaded.getCases().get(0).getTags().get(0));
        verify(governanceCapabilityClient, org.mockito.Mockito.atLeast(2)).assertDatasourceAccess(
            org.mockito.Mockito.eq("tenant-a"),
            org.mockito.Mockito.any(),
            org.mockito.Mockito.eq("BENCHMARK_ENGINE_TEST_SET"),
            org.mockito.Mockito.anyString(),
            org.mockito.Mockito.anyString()
        );
        verify(governanceCapabilityClient, org.mockito.Mockito.atLeast(2)).writeAudit(any());
    }

    @Test
    void shouldMarkTestSetFailedWhenAllImportedRowsBreakReadonlyBoundary() {
        BenchmarkTestSetApplicationService service = new BenchmarkTestSetApplicationService(
            new BenchmarkTestSetModelApplicationService(),
            new InMemoryBenchmarkTaskRepository(),
            mockGovernanceClient()
        );
        RequestContext.set("tenant-a", "user-001", "request-001", "trace-001", "header", 1L, 2L);

        BenchmarkTestSetResponse created = service.createTestSet(baseRequest(
            "case_name,sql_text,report_code,tags\nunsafe,DELETE FROM orders,report-002,unsafe\n"
        ));

        assertEquals("FAILED", created.getStatus().name());
        assertEquals(Integer.valueOf(0), created.getAcceptedCases());
        assertEquals(Integer.valueOf(1), created.getRejectedCases());
        assertTrue(created.getCases().get(0).getRejectionReason().contains("只读压测边界"));
    }

    private BenchmarkTestSetCreateRequest baseRequest(String csvContent) {
        BenchmarkTestSetCreateRequest request = new BenchmarkTestSetCreateRequest();
        request.setTenantId("tenant-a");
        request.setTestSetName("route-governance-import");
        request.setTemplateId("comparison-dual-engine");
        request.setTemplateType(BenchmarkTemplateType.CROSS_ENGINE_COMPARISON);
        request.setTemplateVersion("v2026.04");
        request.setTestSetSource(BenchmarkTestSetSource.BATCH_IMPORT);
        request.setTestSetLabels(Arrays.asList(label(BenchmarkTestSetLabelType.SCENARIO, "COMPARISON")));
        BenchmarkTestSetImportRequest importRequest = new BenchmarkTestSetImportRequest();
        importRequest.setFileType(BenchmarkTestSetFileType.CSV);
        importRequest.setFileName("comparison.csv");
        importRequest.setContentBase64(Base64.getEncoder().encodeToString(csvContent.getBytes(StandardCharsets.UTF_8)));
        importRequest.setFieldMappings(
            Arrays.asList(
                mapping(BenchmarkTestSetField.CASE_NAME, "case_name"),
                mapping(BenchmarkTestSetField.SQL_TEXT, "sql_text"),
                mapping(BenchmarkTestSetField.REPORT_CODE, "report_code"),
                mapping(BenchmarkTestSetField.TAGS, "tags")
            )
        );
        request.setImportRequest(importRequest);
        return request;
    }

    private BenchmarkTestSetFieldMappingDTO mapping(BenchmarkTestSetField field, String columnName) {
        BenchmarkTestSetFieldMappingDTO item = new BenchmarkTestSetFieldMappingDTO();
        item.setField(field);
        item.setColumnName(columnName);
        return item;
    }

    private BenchmarkTestSetLabelDTO label(BenchmarkTestSetLabelType type, String value) {
        BenchmarkTestSetLabelDTO item = new BenchmarkTestSetLabelDTO();
        item.setType(type);
        item.setValue(value);
        return item;
    }

    private GovernanceCapabilityClient mockGovernanceClient() {
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        doNothing().when(governanceCapabilityClient).assertDatasourceAccess(any(), any(), any(), any(), any());
        doNothing().when(governanceCapabilityClient).writeAudit(any());
        return governanceCapabilityClient;
    }
}
