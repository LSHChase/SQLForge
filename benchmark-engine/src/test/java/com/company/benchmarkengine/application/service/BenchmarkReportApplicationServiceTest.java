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
import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportFormat;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.benchmarkengine.infrastructure.repository.InMemoryBenchmarkTaskRepository;
import com.company.sqlforge.common.exception.BizException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BenchmarkReportApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldRenderPdfAndHtmlPlaceholderContent() {
        BenchmarkTaskModelApplicationService modelService = new BenchmarkTaskModelApplicationService();
        InMemoryBenchmarkTaskRepository repository = new InMemoryBenchmarkTaskRepository();
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        BenchmarkReportApplicationService service =
            new BenchmarkReportApplicationService(
                modelService,
                repository,
                governanceCapabilityClient,
                new BenchmarkMetricsRecorder(meterRegistry)
            );
        BenchmarkReport report = storeReport(modelService, repository, "benchmark-report-001");
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
    void shouldRejectUnknownFormatAndMissingReport() {
        BenchmarkTaskModelApplicationService modelService = new BenchmarkTaskModelApplicationService();
        InMemoryBenchmarkTaskRepository repository = new InMemoryBenchmarkTaskRepository();
        BenchmarkReportApplicationService service =
            new BenchmarkReportApplicationService(
                modelService,
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

    private BenchmarkReport storeReport(BenchmarkTaskModelApplicationService modelService,
                                        InMemoryBenchmarkTaskRepository repository,
                                        String taskId) {
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
        BenchmarkReport report = modelService.buildPlaceholderReport(task, Instant.parse("2026-04-21T00:00:11Z"));
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
