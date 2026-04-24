package com.company.benchmarkengine.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.company.sqlforge.common.context.RequestContext;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskSubmitRequest;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportRawDataResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportResponse;
import com.company.benchmarkengine.config.BenchmarkArtifactStorageProperties;
import com.company.benchmarkengine.config.BenchmarkTaskExecutionProperties;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportFormat;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.benchmarkengine.infrastructure.repository.InMemoryBenchmarkTaskRepository;
import com.company.sqlforge.common.exception.BizException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class BenchmarkReportApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldRenderPersistedPdfAndHtmlContentAndWriteTraceLinkedAudit(@TempDir Path tempDir) {
        BenchmarkTaskModelApplicationService modelService = new BenchmarkTaskModelApplicationService();
        InMemoryBenchmarkTaskRepository repository = new InMemoryBenchmarkTaskRepository();
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        BenchmarkArtifactStorageProperties properties = new BenchmarkArtifactStorageProperties();
        properties.setBaseDir(tempDir.resolve("artifacts").toString());
        BenchmarkArtifactStorageService storageService = new BenchmarkArtifactStorageService(properties);
        BenchmarkReportApplicationService service =
            new BenchmarkReportApplicationService(
                modelService,
                new BenchmarkReportExportService(),
                storageService,
                new BenchmarkGovernanceTraceService(governanceCapabilityClient),
                repository,
                governanceCapabilityClient,
                new BenchmarkMetricsRecorder(meterRegistry)
            );
        BenchmarkReport report = storeReport(modelService, repository, "benchmark-report-001", storageService);
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        BenchmarkRenderedReport pdf = service.renderReport(report.getReportId(), BenchmarkReportFormat.PDF);
        BenchmarkRenderedReport html = service.renderReport(report.getReportId(), BenchmarkReportFormat.HTML);

        assertEquals("application/pdf", pdf.getMediaType().toString());
        assertTrue(new String(pdf.getContent()).startsWith("%PDF-1.4"));
        assertTrue(new String(pdf.getContent()).contains(report.getReportId()));

        assertEquals("text/html", html.getMediaType().toString());
        assertTrue(new String(html.getContent()).contains("SQLForge Benchmark Report"));
        assertTrue(new String(html.getContent()).contains(report.getReportId()));
        verify(governanceCapabilityClient, org.mockito.Mockito.atLeast(2)).assertAuthorization(
            org.mockito.Mockito.eq("tenant-a"),
            org.mockito.Mockito.any(),
            org.mockito.Mockito.eq("BENCHMARK_ENGINE_REPORT"),
            org.mockito.Mockito.eq(report.getReportId()),
            org.mockito.Mockito.eq("BENCHMARK_REPORT_QUERY")
        );
        verify(governanceCapabilityClient, org.mockito.Mockito.atLeast(2)).writeAudit(any());
        ArgumentCaptor<com.company.benchmarkengine.infrastructure.governance.BenchmarkAuditRecord> captor =
            ArgumentCaptor.forClass(com.company.benchmarkengine.infrastructure.governance.BenchmarkAuditRecord.class);
        verify(governanceCapabilityClient, org.mockito.Mockito.atLeast(2)).writeAudit(captor.capture());
        List<com.company.benchmarkengine.infrastructure.governance.BenchmarkAuditRecord> records = captor.getAllValues();
        assertEquals("cfg-benchmark-" + report.getReportId(), records.get(0).getConfigSnapshotId());
        assertEquals("result-benchmark-" + report.getReportId(), records.get(0).getResultId());
        assertEquals("history-benchmark-" + report.getReportId(), records.get(0).getHistoryId());
        assertEquals("export-benchmark-" + report.getReportId() + "-pdf-export", records.get(0).getExportId());
        assertEquals("benchmark-report-" + report.getTaskId(), records.get(0).getSagaId());
        assertEquals("export-benchmark-" + report.getReportId() + "-html-export", records.get(1).getExportId());
        assertEquals(1.0D, meterRegistry.get("sqlforge.benchmark.engine.report.requests").tags(
            "format", "PDF",
            "result_status", "SUCCESS"
        ).counter().count());
        assertEquals(1.0D, meterRegistry.get("sqlforge.benchmark.engine.report.requests").tags(
            "format", "HTML",
            "result_status", "SUCCESS"
        ).counter().count());
    }

    @Test
    void shouldDownloadRawDataArtifact(@TempDir Path tempDir) {
        BenchmarkTaskModelApplicationService modelService = new BenchmarkTaskModelApplicationService();
        InMemoryBenchmarkTaskRepository repository = new InMemoryBenchmarkTaskRepository();
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        BenchmarkArtifactStorageProperties properties = new BenchmarkArtifactStorageProperties();
        properties.setBaseDir(tempDir.resolve("artifacts").toString());
        BenchmarkArtifactStorageService storageService = new BenchmarkArtifactStorageService(properties);
        BenchmarkReportApplicationService service =
            new BenchmarkReportApplicationService(
                modelService,
                new BenchmarkReportExportService(),
                storageService,
                new BenchmarkGovernanceTraceService(governanceCapabilityClient),
                repository,
                governanceCapabilityClient,
                new BenchmarkMetricsRecorder(new SimpleMeterRegistry())
            );
        BenchmarkReport report = storeReport(modelService, repository, "benchmark-report-raw-001", storageService);
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        BenchmarkRenderedReport rawData = service.downloadRawDataReport(report.getReportId());

        assertEquals("application/json", rawData.getMediaType().toString());
        assertTrue(new String(rawData.getContent()).contains("\"reportId\":\"" + report.getReportId() + "\""));
        ArgumentCaptor<com.company.benchmarkengine.infrastructure.governance.BenchmarkAuditRecord> captor =
            ArgumentCaptor.forClass(com.company.benchmarkengine.infrastructure.governance.BenchmarkAuditRecord.class);
        verify(governanceCapabilityClient).writeAudit(captor.capture());
        assertEquals("export-benchmark-" + report.getReportId() + "-raw-data", captor.getValue().getExportId());
    }

    @Test
    void shouldRejectUnknownFormatAndMissingReport() {
        BenchmarkTaskModelApplicationService modelService = new BenchmarkTaskModelApplicationService();
        InMemoryBenchmarkTaskRepository repository = new InMemoryBenchmarkTaskRepository();
        BenchmarkReportApplicationService service =
            new BenchmarkReportApplicationService(
                modelService,
                new BenchmarkReportExportService(),
                new BenchmarkArtifactStorageService(new BenchmarkArtifactStorageProperties()),
                new BenchmarkGovernanceTraceService(mockGovernanceClient()),
                repository,
                mockGovernanceClient(),
                new BenchmarkMetricsRecorder(new SimpleMeterRegistry())
            );
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        BizException invalidFormat = assertThrows(BizException.class, () -> service.parseFormat("CSV"));
        BizException missingReport = assertThrows(BizException.class, () -> service.getJsonReport("missing-report"));

        assertEquals(Integer.valueOf(10001), Integer.valueOf(invalidFormat.getCode()));
        assertEquals(Integer.valueOf(23002), Integer.valueOf(missingReport.getCode()));
    }

    @Test
    void shouldRecoverMissingArtifactFromPersistedReportSnapshot(@TempDir Path tempDir) throws Exception {
        BenchmarkTaskModelApplicationService modelService = new BenchmarkTaskModelApplicationService();
        InMemoryBenchmarkTaskRepository repository = new InMemoryBenchmarkTaskRepository();
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        BenchmarkArtifactStorageProperties properties = new BenchmarkArtifactStorageProperties();
        properties.setBaseDir(tempDir.resolve("artifacts").toString());
        BenchmarkArtifactStorageService storageService = new BenchmarkArtifactStorageService(properties);
        BenchmarkReportApplicationService service =
            new BenchmarkReportApplicationService(
                modelService,
                new BenchmarkReportExportService(),
                storageService,
                new BenchmarkGovernanceTraceService(governanceCapabilityClient),
                repository,
                governanceCapabilityClient,
                new BenchmarkMetricsRecorder(new SimpleMeterRegistry())
            );
        BenchmarkReport report = storeReport(modelService, repository, "benchmark-report-recover-001", storageService);
        BenchmarkReportArtifact pdfArtifact = report.findArtifact(BenchmarkReportFormat.PDF);
        Files.delete(Paths.get(URI.create(pdfArtifact.getStorageUri())));
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        BenchmarkRenderedReport recovered = service.renderReport(report.getReportId(), BenchmarkReportFormat.PDF);

        assertTrue(new String(recovered.getContent()).contains(report.getReportId()));
        BenchmarkReport persisted = repository.findReportByReportId(report.getReportId());
        BenchmarkReportArtifact recoveredArtifact = persisted.findArtifact(BenchmarkReportFormat.PDF);
        assertTrue(Files.exists(Paths.get(URI.create(recoveredArtifact.getStorageUri()))));
        ArgumentCaptor<com.company.benchmarkengine.infrastructure.governance.BenchmarkAuditRecord> captor =
            ArgumentCaptor.forClass(com.company.benchmarkengine.infrastructure.governance.BenchmarkAuditRecord.class);
        verify(governanceCapabilityClient).writeAudit(captor.capture());
        assertTrue(captor.getValue().getResponseSummary().contains("RECOVERED_FROM_REPORT_SNAPSHOT"));
    }

    private BenchmarkReport storeReport(BenchmarkTaskModelApplicationService modelService,
                                        InMemoryBenchmarkTaskRepository repository,
                                        String taskId,
                                        BenchmarkArtifactStorageService storageService) {
        BenchmarkTask task = modelService.createQueuedTask(
            baseRequest(),
            taskId,
            Instant.parse("2026-04-21T00:00:00Z")
        );
        task.markRunning(Instant.parse("2026-04-21T00:00:01Z"));
        task.advancePhase(com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPhase.WARMING_UP, 35, "WARMUP");
        task.advancePhase(com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPhase.EXECUTING, 60, "RUNNING");
        task.advancePhase(
            com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPhase.THRESHOLD_EVALUATING,
            82,
            "THRESHOLDS"
        );
        task.advancePhase(com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPhase.REPORTING, 96, "REPORTING");
        task.markSucceeded("report-" + taskId, Instant.parse("2026-04-21T00:00:10Z"));
        BenchmarkTaskExecutionProperties properties = new BenchmarkTaskExecutionProperties();
        properties.setIsolationSampleCount(4);
        properties.setIsolationWorkIterations(24);
        BenchmarkIsolatedExecutionResult executionResult =
            new BenchmarkIsolatedExecutionService(properties, modelService).execute(task, Instant.parse("2026-04-21T00:00:11Z"));
        BenchmarkReport report = modelService.buildExecutedReport(task, executionResult, Instant.parse("2026-04-21T00:00:11Z"));
        BenchmarkReportResponse reportResponse = modelService.buildReportResponse(report);
        BenchmarkReportRawDataResponse rawDataResponse = modelService.buildRawDataResponse(report);
        java.util.List<com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact> artifacts =
            new BenchmarkReportExportService().buildArtifacts(reportResponse);
        artifacts.add(new BenchmarkReportExportService().buildRawDataArtifact(rawDataResponse));
        java.util.List<com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact> externalized =
            storageService.externalize(report.getReportId(), artifacts);
        java.util.List<com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact> traced =
            new java.util.ArrayList<com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact>(externalized.size());
        for (com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact artifact : externalized) {
            traced.add(artifact.withExportId("export-benchmark-" + report.getReportId() + "-" + artifact.getArtifactKey()));
        }
        report = report.withExportArtifacts(traced);
        repository.saveReport(report);
        return report;
    }

    private BenchmarkTaskSubmitRequest baseRequest() {
        BenchmarkTaskSubmitRequest request = new BenchmarkTaskSubmitRequest();
        request.setTenantId("tenant-a");
        request.setTaskType(BenchmarkTaskType.BASELINE);
        request.setSqlText("SELECT * FROM orders");
        request.setSqlFingerprint("fp-report-query");
        return request;
    }

    private GovernanceCapabilityClient mockGovernanceClient() {
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        doNothing().when(governanceCapabilityClient).assertAuthorization(any(), any(), any(), any(), any());
        doNothing().when(governanceCapabilityClient).writeAudit(any());
        return governanceCapabilityClient;
    }
}
