package com.company.benchmarkengine.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.benchmarkengine.application.controller.dto.BenchmarkScaleEvidenceManifestDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkScaleEvidenceBundleDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkScaleEvidenceFileDigestDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskContextDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkSourceReferenceDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkScaleTargetDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskSubmitRequest;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTestSetLabelDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkThresholdDTO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportRawDataResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskStatusResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskSubmitResponse;
import com.company.benchmarkengine.domain.benchmark.BenchmarkSourceReferenceType;
import com.company.benchmarkengine.config.BenchmarkTaskExecutionProperties;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportFormat;
import com.company.benchmarkengine.domain.benchmark.BenchmarkScaleReadinessStatus;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskError;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPhase;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPriority;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskStatus;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTemplateType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetLabelType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetSource;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdMetric;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdOperator;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdSeverity;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdVerdict;
import com.company.benchmarkengine.domain.benchmark.DesensitizationRequirement;
import com.company.benchmarkengine.domain.benchmark.ShadowEnvironmentMode;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BenchmarkTaskModelApplicationServiceTest {

    @Test
    void shouldCreateQueuedTaskWithIsolationDefaults() {
        BenchmarkTaskModelApplicationService service = new BenchmarkTaskModelApplicationService();
        BenchmarkTaskSubmitRequest request = new BenchmarkTaskSubmitRequest();
        request.setTenantId("tenant-a");
        request.setTaskType(BenchmarkTaskType.BASELINE);
        request.setSqlText("SELECT * FROM orders");
        request.setTaskContext(new BenchmarkTaskContextDTO());

        BenchmarkTask task = service.createQueuedTask(
            request,
            "benchmark-task-001",
            Instant.parse("2026-04-20T00:00:00Z")
        );

        assertEquals(BenchmarkTaskStatus.QUEUED, task.getStatus());
        assertEquals(ShadowEnvironmentMode.REQUIRED, task.getShadowEnvironmentMode());
        assertEquals(DesensitizationRequirement.REQUIRED, task.getDesensitizationRequirement());
        assertTrue(task.getReadonlyRequired().booleanValue());
        assertEquals(1, task.getTargetEngines().size());
        assertEquals(DataSourceTypeEnum.HETU, task.getTargetEngines().get(0));
        assertNull(task.getScaleTarget());
        assertNull(task.getTemplateId());
        assertNull(task.getTestSetSource());
    }

    @Test
    void shouldBuildSubmitAndStatusResponseFromRunningTask() {
        BenchmarkTaskModelApplicationService service = new BenchmarkTaskModelApplicationService();
        BenchmarkTask task = service.createQueuedTask(
            baseRequest(BenchmarkTaskType.COMPARISON),
            "benchmark-task-002",
            Instant.parse("2026-04-20T00:05:00Z")
        );
        task.markRunning(Instant.parse("2026-04-20T00:05:05Z"));

        BenchmarkTaskSubmitResponse submitResponse = service.buildSubmitResponse(
            task,
            Instant.parse("2026-04-20T00:05:50Z")
        );
        BenchmarkTaskStatusResponse statusResponse = service.buildStatusResponse(task);

        assertEquals("benchmark-task-002", submitResponse.getTaskId());
        assertEquals(BenchmarkTaskStatus.RUNNING, submitResponse.getStatus());
        assertEquals(BenchmarkTaskPhase.BASELINE_PREPARING, submitResponse.getCurrentPhase());
        assertEquals("/api/benchmark-engine/tasks/benchmark-task-002", submitResponse.getStatusQueryPath());
        assertEquals("LONG_TERM_BASELINE", submitResponse.getContractStage());
        assertEquals("EXTERNALIZED_ARTIFACT_GOVERNANCE_TRACE_BASELINE", submitResponse.getImplementationStage());

        assertEquals(BenchmarkTaskType.COMPARISON, statusResponse.getTaskType());
        assertEquals(BenchmarkTaskPriority.NORMAL, statusResponse.getPriority());
        assertEquals(Integer.valueOf(2), Integer.valueOf(statusResponse.getTargetEngines().size()));
        assertNotNull(statusResponse.getScaleTarget());
        assertEquals(Integer.valueOf(10000), statusResponse.getScaleTarget().getTargetConcurrency());
        assertEquals("THIRTY_PB", statusResponse.getScaleTarget().getTargetDatasetSizeLabel());
        assertEquals(Long.valueOf(10000000L), statusResponse.getScaleTarget().getTargetDailyQueryVolume());
        assertEquals("TARGET_DECLARED_UNVERIFIED", statusResponse.getScaleTarget().getEvidenceStatus());
        assertNotNull(statusResponse.getScaleTarget().getEvidenceManifest());
        assertEquals("prod-run-20260518/concurrency.log",
            statusResponse.getScaleTarget().getEvidenceManifest().getConcurrencyProofRef());
        assertEquals("prod-run-20260518/daily-query-volume.json",
            statusResponse.getScaleTarget().getEvidenceManifest().getDailyQueryVolumeProofRef());
        assertEquals("prod-bi-cn-01",
            statusResponse.getScaleTarget().getEvidenceManifest().getEnvironmentId());
        assertEquals(Long.valueOf(128L),
            statusResponse.getScaleTarget().getEvidenceManifest().getEvidenceFileDigests().get("metrics.csv").getSizeBytes());
        assertNotNull(statusResponse.getScaleTarget().getEvidenceManifest().getVerificationBundle());
        assertEquals(Integer.valueOf(10000),
            statusResponse.getScaleTarget().getEvidenceManifest().getVerificationBundle().getObservedConcurrency());
        assertEquals(Long.valueOf(10000000L),
            statusResponse.getScaleTarget().getEvidenceManifest().getVerificationBundle().getObservedDailyQueryVolume());
        assertEquals("comparison-dual-engine", statusResponse.getTemplateId());
        assertEquals(BenchmarkTemplateType.CROSS_ENGINE_COMPARISON, statusResponse.getTemplateType());
        assertEquals("set-route-comparison", statusResponse.getTestSetId());
        assertEquals(BenchmarkTestSetSource.RECOMMENDATION_GENERATION, statusResponse.getTestSetSource());
        assertEquals(2, statusResponse.getTestSetLabels().size());
        assertEquals(2, statusResponse.getTestSetSourceRefs().size());
        assertEquals(Integer.valueOf(3), statusResponse.getThresholdCount());
        assertNull(statusResponse.getError());
        assertNotNull(statusResponse.getStartedAt());
    }

    @Test
    void shouldBuildComparisonReportWithWarningThreshold() {
        BenchmarkTaskModelApplicationService service = new BenchmarkTaskModelApplicationService();
        BenchmarkTask task = service.createQueuedTask(
            baseRequest(BenchmarkTaskType.COMPARISON),
            "benchmark-task-003",
            Instant.parse("2026-04-20T00:10:00Z")
        );
        task.markRunning(Instant.parse("2026-04-20T00:10:01Z"));
        task.advancePhase(BenchmarkTaskPhase.SHADOW_VALIDATING, 30, "PREPARED");
        task.advancePhase(BenchmarkTaskPhase.WARMING_UP, 45, "SHADOW_VALIDATED");
        task.advancePhase(BenchmarkTaskPhase.EXECUTING, 60, "WARMUP_FINISHED");
        task.advancePhase(BenchmarkTaskPhase.THRESHOLD_EVALUATING, 80, "RUN_FINISHED");
        task.advancePhase(BenchmarkTaskPhase.REPORTING, 95, "THRESHOLDS_EVALUATED");
        task.markSucceeded("report-benchmark-task-003", Instant.parse("2026-04-20T00:10:10Z"));

        BenchmarkReport report = buildReport(service, task, Instant.parse("2026-04-20T00:10:11Z"));
        BenchmarkReportResponse response = service.buildReportResponse(report);

        assertEquals(BenchmarkThresholdVerdict.WARNING, response.getVerdict());
        assertEquals(2, response.getEngineResults().size());
        assertEquals(2, response.getTargetEngines().size());
        assertEquals(3, response.getThresholdAssessments().size());
        assertEquals(3, response.getTrendCharts().size());
        assertEquals(BenchmarkThresholdVerdict.WARNING, response.getThresholdAssessments().get(2).getVerdict());
        assertEquals("JSON", response.getRequestedFormat());
        assertEquals("/api/benchmark-engine/reports/report-benchmark-task-003", response.getReportQueryPath());
        assertEquals("/api/benchmark-engine/reports/report-benchmark-task-003/raw-data", response.getRawDataDownloadPath());
        assertEquals("ENGINE_SELECTION", response.getRecommendations().get(0).getCategory());
        assertEquals("EXTERNALIZED_ARTIFACT_GOVERNANCE_TRACE_BASELINE", response.getImplementationStage());
        assertNotNull(response.getScaleReadiness());
        assertEquals(BenchmarkScaleReadinessStatus.NOT_PROVEN, response.getScaleReadiness().getReadinessStatus());
        assertEquals(new BigDecimal("1000"), response.getScaleReadiness().getObservedQueueWaitMs());
        assertTrue(response.getScaleReadiness().getSatisfiedEvidence().contains("queueWaitMs"));
        assertTrue(response.getScaleReadiness().getSatisfiedEvidence().contains("productionConcurrencyProof"));
        assertTrue(response.getScaleReadiness().getSatisfiedEvidence().contains("productionEvidenceBundle"));
        assertTrue(response.getScaleReadiness().getMissingEvidence().toString().contains("productionExternalVerification"));
        assertTrue(response.getScaleReadiness().getMissingEvidence().toString().contains("targetConcurrencyCovered"));
        assertEquals("tenant-a", report.getTenantId());
        assertNotNull(report.getExecutionSummary());
        assertNotNull(report.getExecutionSummary().getScaleReadiness());
        assertTrue(report.getExecutionSummary().getPhaseNotes().contains("scaleTargetStatus=TARGET_DECLARED_UNVERIFIED"));
        assertTrue(report.getExecutionSummary().getPhaseNotes().contains("scaleTargetDailyQueryVolume=10000000"));
        assertTrue(report.getExecutionSummary().getPhaseNotes().contains("productionEvidenceVerification=UNVERIFIED"));
        assertTrue(report.getExecutionSummary().getPhaseNotes().contains("productionEvidenceBundleSatisfied=true"));
        BenchmarkReportRawDataResponse rawData = service.buildRawDataResponse(report);
        assertNotNull(rawData.getScaleReadiness());
        BenchmarkReportExportService exportService = new BenchmarkReportExportService();
        BenchmarkReportArtifact jsonArtifact = exportService.buildReportArtifact(response, BenchmarkReportFormat.JSON);
        BenchmarkReportArtifact pdfArtifact = exportService.buildReportArtifact(response, BenchmarkReportFormat.PDF);
        BenchmarkReportArtifact htmlArtifact = exportService.buildReportArtifact(response, BenchmarkReportFormat.HTML);
        BenchmarkReportArtifact rawDataArtifact = exportService.buildRawDataArtifact(rawData);
        assertTrue(jsonArtifact.getContent().contains("\"scaleReadiness\""));
        assertTrue(jsonArtifact.getContent().contains("\"evidenceFileDigests\""));
        assertTrue(pdfArtifact.getContent().contains("scaleReadiness=NOT_PROVEN"));
        assertTrue(pdfArtifact.getContent().contains("productionEvidence=source=PROD_REPLAY"));
        assertTrue(pdfArtifact.getContent().contains("dailyQueryVolumeRef=prod-run-20260518/daily-query-volume.json"));
        assertTrue(pdfArtifact.getContent().contains("verificationBundleSatisfied=true"));
        assertTrue(pdfArtifact.getContent().contains("evidenceFileDigestCount=7"));
        assertTrue(pdfArtifact.getContent().contains("provenance=environmentId=prod-bi-cn-01"));
        assertTrue(htmlArtifact.getContent().contains("规模就绪"));
        assertTrue(htmlArtifact.getContent().contains("prod-run-20260518/cost-bill.csv"));
        assertTrue(rawDataArtifact.getContent().contains("\"scaleReadiness\""));
        assertEquals(Integer.valueOf(4), Integer.valueOf(report.getExportArtifacts().size()));
    }

    @Test
    void shouldNotAcceptVerifiedStatusWithoutStructuredProductionBundle() {
        BenchmarkTaskModelApplicationService service = new BenchmarkTaskModelApplicationService();
        BenchmarkTaskSubmitRequest request = baseRequest(BenchmarkTaskType.COMPARISON);
        request.getTaskContext().getScaleTarget().getEvidenceManifest().setExternalVerificationStatus("VERIFIED");
        request.getTaskContext().getScaleTarget().getEvidenceManifest().setVerificationBundle(null);
        BenchmarkTask task = service.createQueuedTask(
            request,
            "benchmark-task-verified-without-bundle",
            Instant.parse("2026-04-20T00:12:00Z")
        );
        task.markRunning(Instant.parse("2026-04-20T00:12:01Z"));
        task.advancePhase(BenchmarkTaskPhase.SHADOW_VALIDATING, 30, "PREPARED");
        task.advancePhase(BenchmarkTaskPhase.WARMING_UP, 45, "SHADOW_VALIDATED");
        task.advancePhase(BenchmarkTaskPhase.EXECUTING, 60, "WARMUP_FINISHED");
        task.advancePhase(BenchmarkTaskPhase.THRESHOLD_EVALUATING, 80, "RUN_FINISHED");
        task.advancePhase(BenchmarkTaskPhase.REPORTING, 95, "THRESHOLDS_EVALUATED");
        task.markSucceeded("report-benchmark-task-verified-without-bundle", Instant.parse("2026-04-20T00:12:10Z"));

        BenchmarkReportResponse response = service.buildReportResponse(
            buildReport(service, task, Instant.parse("2026-04-20T00:12:11Z"))
        );

        assertEquals(BenchmarkScaleReadinessStatus.NOT_PROVEN, response.getScaleReadiness().getReadinessStatus());
        assertTrue(response.getScaleReadiness().getMissingEvidence().contains("productionEvidenceBundle"));
        assertTrue(response.getScaleReadiness().getMissingEvidence().toString()
            .contains("productionExternalVerification:bundle"));
        assertTrue(response.getScaleReadiness().getMissingEvidence().toString().contains("targetConcurrencyCovered"));
    }

    @Test
    void shouldBuildRegressionGuardReportWithFailVerdict() {
        BenchmarkTaskModelApplicationService service = new BenchmarkTaskModelApplicationService();
        BenchmarkTaskSubmitRequest request = baseRequest(BenchmarkTaskType.REGRESSION_GUARD);
        request.getTaskContext().setTargetEngines(Arrays.asList(DataSourceTypeEnum.HETU));
        request.getTaskContext().setThresholds(
            Arrays.asList(
                threshold(
                    BenchmarkThresholdMetric.P99_LATENCY_MS,
                    BenchmarkThresholdOperator.LESS_THAN_OR_EQUAL,
                    "70",
                    BenchmarkThresholdSeverity.CRITICAL,
                    "P99 regression gate"
                )
            )
        );
        BenchmarkTask task = service.createQueuedTask(
            request,
            "benchmark-task-004",
            Instant.parse("2026-04-20T00:15:00Z")
        );
        task.markRunning(Instant.parse("2026-04-20T00:15:01Z"));
        task.advancePhase(BenchmarkTaskPhase.EXECUTING, 60, "SHADOW_VALIDATED");
        task.advancePhase(BenchmarkTaskPhase.THRESHOLD_EVALUATING, 80, "RUN_FINISHED");
        task.advancePhase(BenchmarkTaskPhase.REPORTING, 95, "THRESHOLDS_EVALUATED");
        task.markSucceeded("report-benchmark-task-004", Instant.parse("2026-04-20T00:15:10Z"));

        BenchmarkReportResponse response = service.buildReportResponse(
            buildReport(service, task, Instant.parse("2026-04-20T00:15:11Z"))
        );

        assertEquals(BenchmarkThresholdVerdict.FAIL, response.getVerdict());
        assertEquals(BenchmarkThresholdVerdict.FAIL, response.getThresholdAssessments().get(0).getVerdict());
        assertNotNull(response.getRegressionSummary());
        assertEquals(Integer.valueOf(1), response.getRegressionSummary().getThresholdHitCount());
        assertEquals(Integer.valueOf(1), response.getRegressionSummary().getFailedThresholdCount());
        assertEquals(Boolean.TRUE, response.getRegressionSummary().getAlertRequired());
        assertEquals("RESOURCE_USAGE_CURVE", response.getTrendCharts().get(2).getChartType());
        assertEquals("REGRESSION_GATE", response.getRecommendations().get(0).getCategory());
    }

    @Test
    void shouldExposeStructuredErrorForFailedTask() {
        BenchmarkTaskModelApplicationService service = new BenchmarkTaskModelApplicationService();
        BenchmarkTask task = service.createQueuedTask(
            baseRequest(BenchmarkTaskType.BASELINE),
            "benchmark-task-005",
            Instant.parse("2026-04-20T00:20:00Z")
        );
        task.markRunning(Instant.parse("2026-04-20T00:20:01Z"));
        task.markFailed(
            new BenchmarkTaskError(
                14000,
                "压测 worker 在报告写回完成前失败",
                "重试前请检查 benchmark_task 表、benchmark_task_report 表和 worker 流水线。",
                true
            ),
            Instant.parse("2026-04-20T00:20:05Z")
        );

        BenchmarkTaskStatusResponse response = service.buildStatusResponse(task);

        assertNotNull(response.getError());
        assertEquals(Integer.valueOf(14000), Integer.valueOf(response.getError().getCode()));
        assertEquals(BenchmarkTaskStatus.FAILED, response.getStatus());
        assertEquals(BenchmarkTaskPhase.FINISHED, response.getCurrentPhase());
    }

    private BenchmarkReport buildReport(BenchmarkTaskModelApplicationService service,
                                        BenchmarkTask task,
                                        Instant generatedAt) {
        BenchmarkTaskExecutionProperties properties = new BenchmarkTaskExecutionProperties();
        properties.setIsolationSampleCount(4);
        properties.setIsolationWorkIterations(24);
        BenchmarkIsolatedExecutionResult executionResult =
            new BenchmarkIsolatedExecutionService(properties, service).execute(task, generatedAt);
        BenchmarkReport report = service.buildExecutedReport(task, executionResult, generatedAt);
        BenchmarkReportResponse reportResponse = service.buildReportResponse(report);
        BenchmarkReportRawDataResponse rawDataResponse = service.buildRawDataResponse(report);
        java.util.List<com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact> artifacts =
            new BenchmarkReportExportService().buildArtifacts(reportResponse);
        artifacts.add(new BenchmarkReportExportService().buildRawDataArtifact(rawDataResponse));
        return report.withExportArtifacts(artifacts);
    }

    private BenchmarkTaskSubmitRequest baseRequest(BenchmarkTaskType taskType) {
        BenchmarkTaskSubmitRequest request = new BenchmarkTaskSubmitRequest();
        request.setTenantId("tenant-a");
        request.setTaskType(taskType);
        request.setSqlText("SELECT * FROM orders");
        request.setSqlFingerprint("fp-benchmark-base");
        request.setTaskContext(new BenchmarkTaskContextDTO());
        request.getTaskContext().setPriority(BenchmarkTaskPriority.NORMAL);
        request.getTaskContext().setTargetEngines(Arrays.asList(DataSourceTypeEnum.HETU, DataSourceTypeEnum.HIVE));
        request.getTaskContext().setConcurrency(Integer.valueOf(16));
        request.getTaskContext().setDurationSeconds(Integer.valueOf(300));
        request.getTaskContext().setRampUpSeconds(Integer.valueOf(30));
        request.getTaskContext().setDatasetSizeLabel("TEN_GB");
        request.getTaskContext().setScaleTarget(productionScaleTarget());
        request.getTaskContext().setTemplateId(templateId(taskType));
        request.getTaskContext().setTemplateType(templateType(taskType));
        request.getTaskContext().setTemplateVersion("v2026.04");
        request.getTaskContext().setTestSetId(testSetId(taskType));
        request.getTaskContext().setTestSetSource(testSetSource(taskType));
        request.getTaskContext().setTestSetLabels(
            Arrays.asList(
                label(BenchmarkTestSetLabelType.SCENARIO, taskType.name()),
                label(BenchmarkTestSetLabelType.DOMAIN, taskType == BenchmarkTaskType.COMPARISON ? "ROUTE_GOVERNANCE" : "REGRESSION")
            )
        );
        request.getTaskContext().setTestSetSourceRefs(testSetSourceRefs(taskType));
        request.getTaskContext().setReadonlyRequired(Boolean.TRUE);
        request.getTaskContext().setShadowEnvironmentMode(ShadowEnvironmentMode.REQUIRED);
        request.getTaskContext().setDesensitizationRequirement(DesensitizationRequirement.REQUIRED);
        request.getTaskContext().setThresholds(
            Arrays.asList(
                threshold(
                    BenchmarkThresholdMetric.QPS,
                    BenchmarkThresholdOperator.GREATER_THAN_OR_EQUAL,
                    "120",
                    BenchmarkThresholdSeverity.CRITICAL,
                    "Throughput baseline"
                ),
                threshold(
                    BenchmarkThresholdMetric.P99_LATENCY_MS,
                    BenchmarkThresholdOperator.LESS_THAN_OR_EQUAL,
                    "90",
                    BenchmarkThresholdSeverity.CRITICAL,
                    "Tail latency"
                ),
                threshold(
                    BenchmarkThresholdMetric.CPU_USAGE_PERCENT,
                    BenchmarkThresholdOperator.LESS_THAN_OR_EQUAL,
                    "50",
                    BenchmarkThresholdSeverity.WARNING,
                    "CPU pressure"
                )
            )
        );
        return request;
    }

    private BenchmarkScaleTargetDTO productionScaleTarget() {
        BenchmarkScaleTargetDTO target = new BenchmarkScaleTargetDTO();
        target.setTargetConcurrency(Integer.valueOf(10000));
        target.setTargetDatasetSizeLabel("THIRTY_PB");
        target.setTargetDailyQueryVolume(Long.valueOf(10000000L));
        target.setTargetComplexityProfile("HIGH_COMPLEXITY_SELECT");
        target.setTargetCostEfficiency("minimize-scan-cpu-and-cost-per-query");
        target.setEvidenceManifest(productionEvidenceManifest());
        return target;
    }

    private BenchmarkScaleEvidenceManifestDTO productionEvidenceManifest() {
        BenchmarkScaleEvidenceManifestDTO manifest = new BenchmarkScaleEvidenceManifestDTO();
        manifest.setEvidenceSource("PROD_REPLAY");
        manifest.setConcurrencyProofRef("prod-run-20260518/concurrency.log");
        manifest.setDailyQueryVolumeProofRef("prod-run-20260518/daily-query-volume.json");
        manifest.setDataLayoutProofRef("prod-run-20260518/data-layout-30pb.json");
        manifest.setWorkloadReplayProofRef("prod-run-20260518/replay-window.log");
        manifest.setWorkloadReplayWindow("2026-05-17T00:00Z/2026-05-18T00:00Z");
        manifest.setP95P99MetricProofRef("prod-run-20260518/p95-p99.csv");
        manifest.setScanCpuQueueMetricProofRef("prod-run-20260518/scan-cpu-queue.csv");
        manifest.setCostBillProofRef("prod-run-20260518/cost-bill.csv");
        manifest.setExternalVerificationStatus("UNVERIFIED");
        manifest.setEnvironmentId("prod-bi-cn-01");
        manifest.setEnvironmentType("PRODUCTION");
        manifest.setEvidenceOwner("bi-platform-owner");
        manifest.setArtifactArchiveRef("s3://audit-prod/sqlforge/prod-run-20260518/");
        manifest.setVerifierOperator("benchmark-sre");
        manifest.setEvidenceFileDigests(productionDigests());
        manifest.setVerificationBundle(productionEvidenceBundle());
        return manifest;
    }

    private Map<String, BenchmarkScaleEvidenceFileDigestDTO> productionDigests() {
        Map<String, BenchmarkScaleEvidenceFileDigestDTO> digests =
            new LinkedHashMap<String, BenchmarkScaleEvidenceFileDigestDTO>();
        addDigest(digests, "provenance.json");
        addDigest(digests, "concurrency.json");
        addDigest(digests, "daily-query-volume.json");
        addDigest(digests, "data-layout.json");
        addDigest(digests, "workload-replay.json");
        addDigest(digests, "metrics.csv");
        addDigest(digests, "cost-bill.json");
        return digests;
    }

    private void addDigest(Map<String, BenchmarkScaleEvidenceFileDigestDTO> digests, String fileName) {
        BenchmarkScaleEvidenceFileDigestDTO digest = new BenchmarkScaleEvidenceFileDigestDTO();
        digest.setSha256(repeat("a", 64));
        digest.setSizeBytes(Long.valueOf(128L));
        digests.put(fileName, digest);
    }

    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder(value.length() * count);
        for (int index = 0; index < count; index++) {
            builder.append(value);
        }
        return builder.toString();
    }

    private BenchmarkScaleEvidenceBundleDTO productionEvidenceBundle() {
        BenchmarkScaleEvidenceBundleDTO bundle = new BenchmarkScaleEvidenceBundleDTO();
        bundle.setObservedConcurrency(Integer.valueOf(10000));
        bundle.setObservedDailyQueryVolume(Long.valueOf(10000000L));
        bundle.setObservedDatasetSizeBytes(Long.valueOf(30000000000000000L));
        bundle.setWorkloadReplayDurationHours(new BigDecimal("24"));
        bundle.setP95LatencyMs(new BigDecimal("120"));
        bundle.setP99LatencyMs(new BigDecimal("240"));
        bundle.setScannedBytes(Long.valueOf(9876543210L));
        bundle.setCpuUsagePercent(new BigDecimal("72.5"));
        bundle.setQueueWaitMs(new BigDecimal("8"));
        bundle.setCostBillAmount(new BigDecimal("12345.67"));
        bundle.setCostBillCurrency("USD");
        bundle.setVerifierRef("prod-run-20260518/verifier.json");
        return bundle;
    }

    private BenchmarkTestSetLabelDTO label(BenchmarkTestSetLabelType type, String value) {
        BenchmarkTestSetLabelDTO item = new BenchmarkTestSetLabelDTO();
        item.setType(type);
        item.setValue(value);
        return item;
    }

    private java.util.List<BenchmarkSourceReferenceDTO> testSetSourceRefs(BenchmarkTaskType taskType) {
        if (taskType == BenchmarkTaskType.COMPARISON) {
            return Arrays.asList(
                sourceRef(BenchmarkSourceReferenceType.RECOMMENDATION, "rec-001"),
                sourceRef(BenchmarkSourceReferenceType.SQL_FINGERPRINT, "fp-benchmark-base")
            );
        }
        if (taskType == BenchmarkTaskType.BASELINE) {
            return Arrays.asList(sourceRef(BenchmarkSourceReferenceType.IMPORT_BATCH, "batch-001"));
        }
        return Arrays.asList(
            sourceRef(BenchmarkSourceReferenceType.QUERY_HISTORY, "history-001"),
            sourceRef(BenchmarkSourceReferenceType.REPORT, "report-guard-001")
        );
    }

    private BenchmarkSourceReferenceDTO sourceRef(BenchmarkSourceReferenceType type, String referenceId) {
        BenchmarkSourceReferenceDTO item = new BenchmarkSourceReferenceDTO();
        item.setType(type);
        item.setReferenceId(referenceId);
        return item;
    }

    private String templateId(BenchmarkTaskType taskType) {
        switch (taskType) {
            case BASELINE:
                return "baseline-snapshot";
            case COMPARISON:
                return "comparison-dual-engine";
            case REGRESSION_GUARD:
                return "regression-guard";
            default:
                return "baseline-snapshot";
        }
    }

    private BenchmarkTemplateType templateType(BenchmarkTaskType taskType) {
        switch (taskType) {
            case BASELINE:
                return BenchmarkTemplateType.BASELINE_SNAPSHOT;
            case COMPARISON:
                return BenchmarkTemplateType.CROSS_ENGINE_COMPARISON;
            case REGRESSION_GUARD:
                return BenchmarkTemplateType.REGRESSION_GUARD;
            default:
                return BenchmarkTemplateType.BASELINE_SNAPSHOT;
        }
    }

    private String testSetId(BenchmarkTaskType taskType) {
        switch (taskType) {
            case BASELINE:
                return "set-baseline-capture";
            case COMPARISON:
                return "set-route-comparison";
            case REGRESSION_GUARD:
                return "set-regression-gate";
            default:
                return "set-baseline-capture";
        }
    }

    private BenchmarkTestSetSource testSetSource(BenchmarkTaskType taskType) {
        switch (taskType) {
            case BASELINE:
                return BenchmarkTestSetSource.BATCH_IMPORT;
            case COMPARISON:
                return BenchmarkTestSetSource.RECOMMENDATION_GENERATION;
            case REGRESSION_GUARD:
                return BenchmarkTestSetSource.PARSE_RESULT_GENERATION;
            default:
                return BenchmarkTestSetSource.MANUAL_CURATION;
        }
    }

    private BenchmarkThresholdDTO threshold(BenchmarkThresholdMetric metric,
                                            BenchmarkThresholdOperator operator,
                                            String targetValue,
                                            BenchmarkThresholdSeverity severity,
                                            String description) {
        BenchmarkThresholdDTO item = new BenchmarkThresholdDTO();
        item.setMetric(metric);
        item.setOperator(operator);
        item.setTargetValue(new BigDecimal(targetValue));
        item.setSeverity(severity);
        item.setDescription(description);
        return item;
    }
}
