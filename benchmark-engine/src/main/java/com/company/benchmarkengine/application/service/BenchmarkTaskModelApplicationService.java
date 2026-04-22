package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskContextDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskSubmitRequest;
import com.company.benchmarkengine.application.controller.dto.BenchmarkThresholdDTO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkEngineMetricVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkRecommendationVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportRawDataResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskErrorVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskStatusResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskSubmitResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTrendChartVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTrendPointVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTrendSeriesVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkThresholdAssessmentVO;
import com.company.benchmarkengine.domain.benchmark.BenchmarkEngineProfile;
import com.company.benchmarkengine.domain.benchmark.BenchmarkRecommendation;
import com.company.benchmarkengine.domain.benchmark.BenchmarkRecommendationRiskLevel;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskError;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskSubmission;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThreshold;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdAssessment;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdMetric;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdVerdict;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BenchmarkTaskModelApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String TASK_IMPLEMENTATION_STAGE = "DATABASE_SCHEDULED_WORKER_BASELINE";
    private static final String REPORT_IMPLEMENTATION_STAGE = "DATABASE_PERSISTED_REPORT_BASELINE";
    private static final String STATUS_QUERY_PATH_TEMPLATE = "/api/benchmark-engine/tasks/%s";
    private static final String REPORT_QUERY_PATH_TEMPLATE = "/api/benchmark-engine/reports/%s";
    private static final String RAW_DATA_PATH_TEMPLATE = "/api/benchmark-engine/reports/%s/raw-data";

    public BenchmarkTask createQueuedTask(BenchmarkTaskSubmitRequest request, String taskId, Instant submittedAt) {
        BenchmarkTaskContextDTO taskContext = request.getTaskContext() == null
            ? new BenchmarkTaskContextDTO()
            : request.getTaskContext();
        BenchmarkTaskSubmission submission = new BenchmarkTaskSubmission(
            request.getTenantId(),
            request.getTaskType(),
            request.getSqlText(),
            request.getSqlFingerprint(),
            taskContext.getPriority(),
            taskContext.getTargetEngines(),
            taskContext.getConcurrency(),
            taskContext.getDurationSeconds(),
            taskContext.getRampUpSeconds(),
            taskContext.getDatasetSizeLabel(),
            taskContext.getReadonlyRequired(),
            taskContext.getShadowEnvironmentMode(),
            taskContext.getDesensitizationRequirement(),
            toThresholds(taskContext.getThresholds())
        );
        return BenchmarkTask.submit(taskId, submission, submittedAt);
    }

    public BenchmarkTaskSubmitResponse buildSubmitResponse(BenchmarkTask task, Instant estimatedReadyAt) {
        return new BenchmarkTaskSubmitResponse(
            task.getTaskId(),
            task.getStatus(),
            task.getCurrentPhase(),
            estimatedReadyAt,
            buildStatusQueryPath(task.getTaskId()),
            CONTRACT_STAGE,
            TASK_IMPLEMENTATION_STAGE
        );
    }

    public BenchmarkTaskStatusResponse buildStatusResponse(BenchmarkTask task) {
        return new BenchmarkTaskStatusResponse(
            task.getTaskId(),
            task.getTaskType(),
            task.getStatus(),
            task.getCurrentPhase(),
            task.getPriority(),
            task.getProgressPercent(),
            task.getTargetEngines(),
            task.getReadonlyRequired(),
            task.getShadowEnvironmentMode(),
            task.getDesensitizationRequirement(),
            Integer.valueOf(task.getThresholds().size()),
            task.getReportId(),
            toErrorVO(task.getError()),
            task.getSubmittedAt(),
            task.getStartedAt(),
            task.getFinishedAt(),
            CONTRACT_STAGE,
            TASK_IMPLEMENTATION_STAGE
        );
    }

    public BenchmarkReport buildPlaceholderReport(BenchmarkTask task, Instant generatedAt) {
        List<BenchmarkEngineProfile> engineProfiles = buildEngineProfiles(task);
        List<BenchmarkThresholdAssessment> assessments = evaluateThresholds(task.getThresholds(), engineProfiles.get(0));
        return new BenchmarkReport(
            "report-" + task.getTaskId(),
            task.getTaskId(),
            task.getTaskType(),
            task.getTenantId(),
            task.getSqlFingerprint(),
            generatedAt,
            engineProfiles,
            assessments,
            buildRecommendations(task.getTaskType(), assessments)
        );
    }

    public BenchmarkReportResponse buildReportResponse(BenchmarkReport report) {
        return new BenchmarkReportResponse(
            report.getReportId(),
            report.getTaskId(),
            report.getTaskType(),
            report.getSqlFingerprint(),
            report.getVerdict(),
            report.getGeneratedAt(),
            toTargetEngines(report.getEngineProfiles()),
            toEngineMetricVOs(report.getEngineProfiles()),
            toThresholdAssessmentVOs(report.getThresholdAssessments()),
            buildTrendCharts(report.getEngineProfiles()),
            toRecommendationVOs(report.getRecommendations()),
            "JSON",
            Arrays.asList("JSON", "PDF", "HTML"),
            buildReportQueryPath(report.getReportId()),
            buildRawDataDownloadPath(report.getReportId()),
            CONTRACT_STAGE,
            REPORT_IMPLEMENTATION_STAGE
        );
    }

    public BenchmarkReportRawDataResponse buildRawDataResponse(BenchmarkReport report) {
        return new BenchmarkReportRawDataResponse(
            report.getReportId(),
            report.getTaskId(),
            report.getTaskType(),
            report.getTenantId(),
            report.getSqlFingerprint(),
            report.getVerdict(),
            report.getGeneratedAt(),
            toTargetEngines(report.getEngineProfiles()),
            toEngineMetricVOs(report.getEngineProfiles()),
            toThresholdAssessmentVOs(report.getThresholdAssessments()),
            buildTrendCharts(report.getEngineProfiles()),
            toRecommendationVOs(report.getRecommendations()),
            buildReportQueryPath(report.getReportId()),
            CONTRACT_STAGE,
            REPORT_IMPLEMENTATION_STAGE
        );
    }

    private String buildStatusQueryPath(String taskId) {
        return String.format(STATUS_QUERY_PATH_TEMPLATE, taskId);
    }

    private String buildReportQueryPath(String reportId) {
        return String.format(REPORT_QUERY_PATH_TEMPLATE, reportId);
    }

    private String buildRawDataDownloadPath(String reportId) {
        return String.format(RAW_DATA_PATH_TEMPLATE, reportId);
    }

    private BenchmarkTaskErrorVO toErrorVO(BenchmarkTaskError error) {
        if (error == null) {
            return null;
        }
        return new BenchmarkTaskErrorVO(
            error.getCode(),
            error.getMessage(),
            error.getSuggestedAction(),
            error.isRetryable()
        );
    }

    private List<BenchmarkThreshold> toThresholds(List<BenchmarkThresholdDTO> thresholdDtos) {
        if (thresholdDtos == null || thresholdDtos.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkThreshold> thresholds = new ArrayList<BenchmarkThreshold>(thresholdDtos.size());
        for (BenchmarkThresholdDTO item : thresholdDtos) {
            thresholds.add(
                new BenchmarkThreshold(
                    item.getMetric(),
                    item.getOperator(),
                    item.getTargetValue(),
                    item.getSeverity(),
                    item.getDescription()
                )
            );
        }
        return Collections.unmodifiableList(thresholds);
    }

    private List<BenchmarkEngineProfile> buildEngineProfiles(BenchmarkTask task) {
        List<DataSourceTypeEnum> engines = task.getTargetEngines();
        List<BenchmarkEngineProfile> profiles = new ArrayList<BenchmarkEngineProfile>(engines.size());
        for (int index = 0; index < engines.size(); index++) {
            DataSourceTypeEnum engine = engines.get(index);
            BigDecimal actualQps = task.getTaskType() == BenchmarkTaskType.COMPARISON && index > 0
                ? new BigDecimal("119")
                : new BigDecimal("138");
            BigDecimal p99Latency = task.getTaskType() == BenchmarkTaskType.REGRESSION_GUARD
                ? new BigDecimal("80")
                : index > 0
                ? new BigDecimal("96")
                : new BigDecimal("74");
            profiles.add(
                new BenchmarkEngineProfile(
                    engine,
                    new BigDecimal("120"),
                    actualQps,
                    new BigDecimal(index > 0 ? "41" : "35"),
                    new BigDecimal(index > 0 ? "70" : "58"),
                    p99Latency,
                    new BigDecimal(index > 0 ? "67" : "61"),
                    new BigDecimal(index > 0 ? "640" : "512"),
                    new BigDecimal(index > 0 ? "402653184" : "268435456"),
                    index > 0 ? BenchmarkThresholdVerdict.WARNING : BenchmarkThresholdVerdict.PASS,
                    buildEngineNote(task.getTaskType(), index)
                )
            );
        }
        return Collections.unmodifiableList(profiles);
    }

    private List<DataSourceTypeEnum> toTargetEngines(List<BenchmarkEngineProfile> engineProfiles) {
        if (engineProfiles == null || engineProfiles.isEmpty()) {
            return Collections.emptyList();
        }
        List<DataSourceTypeEnum> targetEngines = new ArrayList<DataSourceTypeEnum>(engineProfiles.size());
        for (BenchmarkEngineProfile engineProfile : engineProfiles) {
            targetEngines.add(engineProfile.getEngine());
        }
        return Collections.unmodifiableList(targetEngines);
    }

    private String buildEngineNote(BenchmarkTaskType taskType, int index) {
        if (taskType == BenchmarkTaskType.BASELINE) {
            return "Baseline run for isolated shadow environment.";
        }
        if (taskType == BenchmarkTaskType.COMPARISON && index > 0) {
            return "Secondary engine used for cross-engine comparison.";
        }
        if (taskType == BenchmarkTaskType.REGRESSION_GUARD) {
            return "Regression guard run compared against approved baseline.";
        }
        return "Primary benchmark engine result.";
    }

    private List<BenchmarkThresholdAssessment> evaluateThresholds(List<BenchmarkThreshold> thresholds,
                                                                  BenchmarkEngineProfile primaryProfile) {
        if (thresholds == null || thresholds.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkThresholdAssessment> assessments = new ArrayList<BenchmarkThresholdAssessment>(thresholds.size());
        for (BenchmarkThreshold threshold : thresholds) {
            assessments.add(threshold.evaluate(metricValue(primaryProfile, threshold.getMetric())));
        }
        return Collections.unmodifiableList(assessments);
    }

    private BigDecimal metricValue(BenchmarkEngineProfile profile, BenchmarkThresholdMetric metric) {
        switch (metric) {
            case QPS:
                return profile.getActualQps();
            case P50_LATENCY_MS:
                return profile.getP50LatencyMs();
            case P95_LATENCY_MS:
                return profile.getP95LatencyMs();
            case P99_LATENCY_MS:
                return profile.getP99LatencyMs();
            case CPU_USAGE_PERCENT:
                return profile.getCpuUsagePercent();
            case MEMORY_USAGE_MB:
                return profile.getMemoryUsageMb();
            case SCANNED_DATA_BYTES:
                return profile.getScannedDataBytes();
            default:
                return BigDecimal.ZERO;
        }
    }

    private List<BenchmarkRecommendation> buildRecommendations(BenchmarkTaskType taskType,
                                                               List<BenchmarkThresholdAssessment> assessments) {
        BenchmarkThresholdVerdict reportVerdict = BenchmarkThresholdVerdict.PASS;
        for (BenchmarkThresholdAssessment assessment : assessments) {
            if (assessment.getVerdict() == BenchmarkThresholdVerdict.FAIL) {
                reportVerdict = BenchmarkThresholdVerdict.FAIL;
                break;
            }
            if (assessment.getVerdict() == BenchmarkThresholdVerdict.WARNING) {
                reportVerdict = BenchmarkThresholdVerdict.WARNING;
            }
        }
        if (taskType == BenchmarkTaskType.BASELINE) {
            return Arrays.asList(
                new BenchmarkRecommendation(
                    "BASELINE_GOVERNANCE",
                    "Freeze baseline report and threshold pack",
                    "Store the baseline profile so later regression tasks can compare against a stable shadow-environment snapshot.",
                    "Improves future regression detection and report explainability.",
                    BenchmarkRecommendationRiskLevel.LOW
                ),
                new BenchmarkRecommendation(
                    "RESOURCE_TUNING",
                    "Tune concurrency only after threshold pack is stable",
                    reportVerdict == BenchmarkThresholdVerdict.PASS
                        ? "Current baseline is within target, so concurrency tuning can be considered as a second step."
                        : "Do not raise concurrency until latency and CPU thresholds stop warning.",
                    "Avoids amplifying noisy benchmark results before the baseline is trustworthy.",
                    BenchmarkRecommendationRiskLevel.MEDIUM
                )
            );
        }
        if (taskType == BenchmarkTaskType.COMPARISON) {
            return Arrays.asList(
                new BenchmarkRecommendation(
                    "ENGINE_SELECTION",
                    "Prefer the engine with the stronger p99 and CPU profile",
                    "Use the comparison report to decide which engine should own the default route for this SQL fingerprint.",
                    "Reduces routing ambiguity for heavy analytical SQL.",
                    BenchmarkRecommendationRiskLevel.MEDIUM
                ),
                new BenchmarkRecommendation(
                    "PLAN_ALIGNMENT",
                    "Capture routing and session variables alongside the report",
                    "Comparison reports are only reusable when plan-affecting settings are versioned with the result.",
                    "Improves reproducibility across engines and later optimization tasks.",
                    BenchmarkRecommendationRiskLevel.LOW
                )
            );
        }
        return Arrays.asList(
            new BenchmarkRecommendation(
                "REGRESSION_GATE",
                "Block release when critical threshold fails",
                "Regression guard tasks should produce a release gate outcome, not only a descriptive report.",
                "Stops latency regressions from leaking into production rollout.",
                BenchmarkRecommendationRiskLevel.LOW
            ),
            new BenchmarkRecommendation(
                "OPTIMIZATION_FEEDBACK",
                "Route the failed fingerprint back to SQL optimization",
                "When regression guard detects a p99 or scan-volume failure, emit the fingerprint to the SQL optimization backlog.",
                "Closes the loop between benchmark evidence and optimization work.",
                reportVerdict == BenchmarkThresholdVerdict.FAIL
                    ? BenchmarkRecommendationRiskLevel.HIGH
                    : BenchmarkRecommendationRiskLevel.MEDIUM
            )
        );
    }

    private List<BenchmarkEngineMetricVO> toEngineMetricVOs(List<BenchmarkEngineProfile> engineProfiles) {
        if (engineProfiles == null || engineProfiles.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkEngineMetricVO> items = new ArrayList<BenchmarkEngineMetricVO>(engineProfiles.size());
        for (BenchmarkEngineProfile profile : engineProfiles) {
            items.add(
                new BenchmarkEngineMetricVO(
                    profile.getEngine(),
                    profile.getTargetQps(),
                    profile.getActualQps(),
                    profile.getP50LatencyMs(),
                    profile.getP95LatencyMs(),
                    profile.getP99LatencyMs(),
                    profile.getCpuUsagePercent(),
                    profile.getMemoryUsageMb(),
                    profile.getScannedDataBytes(),
                    profile.getVerdict(),
                    profile.getNotes()
                )
            );
        }
        return Collections.unmodifiableList(items);
    }

    private List<BenchmarkThresholdAssessmentVO> toThresholdAssessmentVOs(
        List<BenchmarkThresholdAssessment> assessments) {
        if (assessments == null || assessments.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkThresholdAssessmentVO> items =
            new ArrayList<BenchmarkThresholdAssessmentVO>(assessments.size());
        for (BenchmarkThresholdAssessment assessment : assessments) {
            items.add(
                new BenchmarkThresholdAssessmentVO(
                    assessment.getMetric(),
                    assessment.getVerdict(),
                    assessment.getActualValue(),
                    assessment.getTargetValue(),
                    assessment.getSummary()
                )
            );
        }
        return Collections.unmodifiableList(items);
    }

    private List<BenchmarkTrendChartVO> buildTrendCharts(List<BenchmarkEngineProfile> engineProfiles) {
        return Arrays.asList(
            buildLatencyDistributionChart(engineProfiles),
            buildThroughputTimelineChart(engineProfiles),
            buildResourceUsageChart(engineProfiles)
        );
    }

    private BenchmarkTrendChartVO buildLatencyDistributionChart(List<BenchmarkEngineProfile> engineProfiles) {
        List<BenchmarkTrendSeriesVO> series = new ArrayList<BenchmarkTrendSeriesVO>(engineProfiles.size());
        for (BenchmarkEngineProfile profile : engineProfiles) {
            series.add(
                new BenchmarkTrendSeriesVO(
                    profile.getEngine().name(),
                    Arrays.asList(
                        new BenchmarkTrendPointVO("P50", profile.getP50LatencyMs()),
                        new BenchmarkTrendPointVO("P95", profile.getP95LatencyMs()),
                        new BenchmarkTrendPointVO("P99", profile.getP99LatencyMs())
                    )
                )
            );
        }
        return new BenchmarkTrendChartVO(
            "LATENCY_DISTRIBUTION_HISTOGRAM",
            "Latency distribution",
            "Percentile bucket",
            "Latency (ms)",
            Collections.unmodifiableList(series)
        );
    }

    private BenchmarkTrendChartVO buildThroughputTimelineChart(List<BenchmarkEngineProfile> engineProfiles) {
        List<BenchmarkTrendSeriesVO> series = new ArrayList<BenchmarkTrendSeriesVO>(engineProfiles.size());
        for (BenchmarkEngineProfile profile : engineProfiles) {
            series.add(
                new BenchmarkTrendSeriesVO(
                    profile.getEngine().name(),
                    Arrays.asList(
                        new BenchmarkTrendPointVO("T+0", scaled(profile.getActualQps(), "0.78")),
                        new BenchmarkTrendPointVO("T+60", scaled(profile.getActualQps(), "0.92")),
                        new BenchmarkTrendPointVO("T+120", profile.getActualQps())
                    )
                )
            );
        }
        return new BenchmarkTrendChartVO(
            "THROUGHPUT_TIME_SERIES",
            "Throughput timeline",
            "Elapsed time",
            "QPS",
            Collections.unmodifiableList(series)
        );
    }

    private BenchmarkTrendChartVO buildResourceUsageChart(List<BenchmarkEngineProfile> engineProfiles) {
        List<BenchmarkTrendSeriesVO> series = new ArrayList<BenchmarkTrendSeriesVO>(engineProfiles.size() * 2);
        for (BenchmarkEngineProfile profile : engineProfiles) {
            series.add(
                new BenchmarkTrendSeriesVO(
                    profile.getEngine().name() + " CPU",
                    Arrays.asList(
                        new BenchmarkTrendPointVO("Warmup", scaled(profile.getCpuUsagePercent(), "0.72")),
                        new BenchmarkTrendPointVO("Steady", scaled(profile.getCpuUsagePercent(), "0.88")),
                        new BenchmarkTrendPointVO("Peak", profile.getCpuUsagePercent())
                    )
                )
            );
            series.add(
                new BenchmarkTrendSeriesVO(
                    profile.getEngine().name() + " Memory",
                    Arrays.asList(
                        new BenchmarkTrendPointVO("Warmup", scaled(profile.getMemoryUsageMb(), "0.67")),
                        new BenchmarkTrendPointVO("Steady", scaled(profile.getMemoryUsageMb(), "0.84")),
                        new BenchmarkTrendPointVO("Peak", profile.getMemoryUsageMb())
                    )
                )
            );
        }
        return new BenchmarkTrendChartVO(
            "RESOURCE_USAGE_CURVE",
            "Resource usage curve",
            "Benchmark stage",
            "Resource value",
            Collections.unmodifiableList(series)
        );
    }

    private List<BenchmarkRecommendationVO> toRecommendationVOs(List<BenchmarkRecommendation> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkRecommendationVO> items = new ArrayList<BenchmarkRecommendationVO>(recommendations.size());
        for (BenchmarkRecommendation recommendation : recommendations) {
            items.add(
                new BenchmarkRecommendationVO(
                    recommendation.getCategory(),
                    recommendation.getTitle(),
                    recommendation.getSummary(),
                    recommendation.getExpectedBenefit(),
                    recommendation.getRiskLevel()
                )
            );
        }
        return Collections.unmodifiableList(items);
    }

    private BigDecimal scaled(BigDecimal value, String factor) {
        return value.multiply(new BigDecimal(factor)).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}
