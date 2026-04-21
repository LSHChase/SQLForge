package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskContextDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskSubmitRequest;
import com.company.benchmarkengine.application.controller.dto.BenchmarkThresholdDTO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkEngineMetricVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkRecommendationVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskErrorVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskStatusResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskSubmitResponse;
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
    private static final String IMPLEMENTATION_STAGE = "MODEL_BASELINE";

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

    public BenchmarkTaskSubmitResponse buildSubmitResponse(BenchmarkTask task) {
        return new BenchmarkTaskSubmitResponse(
            task.getTaskId(),
            task.getStatus(),
            task.getCurrentPhase(),
            CONTRACT_STAGE,
            IMPLEMENTATION_STAGE
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
            IMPLEMENTATION_STAGE
        );
    }

    public BenchmarkReport buildPlaceholderReport(BenchmarkTask task, Instant generatedAt) {
        List<BenchmarkEngineProfile> engineProfiles = buildEngineProfiles(task);
        List<BenchmarkThresholdAssessment> assessments = evaluateThresholds(task.getThresholds(), engineProfiles.get(0));
        return new BenchmarkReport(
            "report-" + task.getTaskId(),
            task.getTaskId(),
            task.getTaskType(),
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
            toEngineMetricVOs(report.getEngineProfiles()),
            toThresholdAssessmentVOs(report.getThresholdAssessments()),
            toRecommendationVOs(report.getRecommendations()),
            CONTRACT_STAGE,
            IMPLEMENTATION_STAGE
        );
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
}
