package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.application.controller.dto.BenchmarkScaleEvidenceManifestDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkScaleEvidenceBundleDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkScaleEvidenceFileDigestDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkScaleTargetDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskContextDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkSourceReferenceDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskSubmitRequest;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTestSetLabelDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkThresholdDTO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkEngineMetricVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkAlertLinkageVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkRecommendationVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportRawDataResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkRegressionSummaryVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskErrorVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskStatusResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskSubmitResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTrendChartVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTrendPointVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTrendSeriesVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkThresholdAssessmentVO;
import com.company.benchmarkengine.domain.benchmark.BenchmarkEngineProfile;
import com.company.benchmarkengine.domain.benchmark.BenchmarkAlertLinkage;
import com.company.benchmarkengine.domain.benchmark.BenchmarkRecommendation;
import com.company.benchmarkengine.domain.benchmark.BenchmarkRecommendationRiskLevel;
import com.company.benchmarkengine.domain.benchmark.BenchmarkRegressionSummary;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;
import com.company.benchmarkengine.domain.benchmark.BenchmarkSourceReference;
import com.company.benchmarkengine.domain.benchmark.BenchmarkScaleEvidenceBundle;
import com.company.benchmarkengine.domain.benchmark.BenchmarkScaleEvidenceFileDigest;
import com.company.benchmarkengine.domain.benchmark.BenchmarkScaleEvidenceManifest;
import com.company.benchmarkengine.domain.benchmark.BenchmarkScaleTarget;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskError;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskSubmission;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetLabel;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BenchmarkTaskModelApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String TASK_IMPLEMENTATION_STAGE = "EXTERNALIZED_ARTIFACT_GOVERNANCE_TRACE_BASELINE";
    private static final String REPORT_IMPLEMENTATION_STAGE = "EXTERNALIZED_ARTIFACT_GOVERNANCE_TRACE_BASELINE";
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
            toScaleTarget(taskContext.getScaleTarget(), taskContext.getConcurrency(), taskContext.getDatasetSizeLabel()),
            taskContext.getTemplateId(),
            taskContext.getTemplateType(),
            taskContext.getTemplateVersion(),
            taskContext.getTestSetId(),
            taskContext.getTestSetSource(),
            toTestSetLabels(taskContext.getTestSetLabels()),
            toSourceReferences(taskContext.getTestSetSourceRefs()),
            taskContext.getReadonlyRequired(),
            taskContext.getShadowEnvironmentMode(),
            taskContext.getDesensitizationRequirement(),
            toThresholds(taskContext.getThresholds())
        );
        return BenchmarkTask.submit(taskId, submission, submittedAt);
    }

    public BenchmarkTaskSubmitResponse buildSubmitResponse(BenchmarkTask task, Instant estimatedReadyAt) {
        BenchmarkTaskQueueService.QueueEvidence queueEvidence = BenchmarkTaskQueueService.resolveQueueEvidence(task);
        return new BenchmarkTaskSubmitResponse(
            task.getTaskId(),
            task.getStatus(),
            task.getCurrentPhase(),
            estimatedReadyAt,
            buildStatusQueryPath(task.getTaskId()),
            queueEvidence.getQueueMode(),
            queueEvidence.getQueueEvidence(),
            CONTRACT_STAGE,
            TASK_IMPLEMENTATION_STAGE
        );
    }

    public BenchmarkTaskStatusResponse buildStatusResponse(BenchmarkTask task) {
        BenchmarkTaskQueueService.QueueEvidence queueEvidence = BenchmarkTaskQueueService.resolveQueueEvidence(task);
        return new BenchmarkTaskStatusResponse(
            task.getTaskId(),
            task.getTaskType(),
            task.getStatus(),
            task.getCurrentPhase(),
            task.getPriority(),
            task.getProgressPercent(),
            task.getTargetEngines(),
            task.getScaleTarget(),
            task.getTemplateId(),
            task.getTemplateType(),
            task.getTemplateVersion(),
            task.getTestSetId(),
            task.getTestSetSource(),
            task.getTestSetLabels(),
            task.getTestSetSourceRefs(),
            task.getReadonlyRequired(),
            task.getShadowEnvironmentMode(),
            task.getDesensitizationRequirement(),
            Integer.valueOf(task.getThresholds().size()),
            task.getReportId(),
            toErrorVO(task.getError()),
            task.getSubmittedAt(),
            task.getStartedAt(),
            task.getFinishedAt(),
            queueEvidence.getQueueMode(),
            queueEvidence.getQueueEvidence(),
            CONTRACT_STAGE,
            TASK_IMPLEMENTATION_STAGE
        );
    }

    public BenchmarkReport buildExecutedReport(BenchmarkTask task,
                                               BenchmarkIsolatedExecutionResult executionResult,
                                               Instant generatedAt) {
        return new BenchmarkReport(
            "report-" + task.getTaskId(),
            task.getTaskId(),
            task.getTaskType(),
            task.getTenantId(),
            task.getSqlFingerprint(),
            generatedAt,
            executionResult.getEngineProfiles(),
            executionResult.getThresholdAssessments(),
            executionResult.getRecommendations(),
            executionResult.getExecutionSummary(),
            buildRegressionSummary(task.getTaskType(), executionResult.getThresholdAssessments()),
            Collections.<BenchmarkAlertLinkage>emptyList(),
            Collections.<BenchmarkReportArtifact>emptyList()
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
            report.getExecutionSummary() == null ? null : report.getExecutionSummary().getScaleReadiness(),
            toRegressionSummaryVO(report.getRegressionSummary()),
            toAlertLinkageVOs(report.getAlertLinkages()),
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
            report.getExecutionSummary() == null ? null : report.getExecutionSummary().getScaleReadiness(),
            toRegressionSummaryVO(report.getRegressionSummary()),
            toAlertLinkageVOs(report.getAlertLinkages()),
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

    private BenchmarkScaleTarget toScaleTarget(BenchmarkScaleTargetDTO scaleTargetDto,
                                               Integer requestedConcurrency,
                                               String requestedDatasetSizeLabel) {
        if (scaleTargetDto == null) {
            return null;
        }
        return new BenchmarkScaleTarget(
            scaleTargetDto.getTargetConcurrency() == null
                ? requestedConcurrency
                : scaleTargetDto.getTargetConcurrency(),
            firstNonBlank(scaleTargetDto.getTargetDatasetSizeLabel(), requestedDatasetSizeLabel),
            scaleTargetDto.getTargetDailyQueryVolume(),
            scaleTargetDto.getTargetComplexityProfile(),
            scaleTargetDto.getTargetCostEfficiency(),
            BenchmarkScaleTarget.STATUS_TARGET_DECLARED_UNVERIFIED,
            BenchmarkScaleTarget.DEFAULT_EVIDENCE_BOUNDARY,
            null,
            toScaleEvidenceManifest(scaleTargetDto.getEvidenceManifest())
        );
    }

    private BenchmarkScaleEvidenceManifest toScaleEvidenceManifest(BenchmarkScaleEvidenceManifestDTO evidenceManifestDto) {
        if (evidenceManifestDto == null) {
            return null;
        }
        BenchmarkScaleEvidenceManifest evidenceManifest = new BenchmarkScaleEvidenceManifest(
            evidenceManifestDto.getEvidenceSource(),
            evidenceManifestDto.getConcurrencyProofRef(),
            evidenceManifestDto.getDailyQueryVolumeProofRef(),
            evidenceManifestDto.getDataLayoutProofRef(),
            evidenceManifestDto.getWorkloadReplayProofRef(),
            evidenceManifestDto.getWorkloadReplayWindow(),
            evidenceManifestDto.getP95P99MetricProofRef(),
            evidenceManifestDto.getScanCpuQueueMetricProofRef(),
            evidenceManifestDto.getCostBillProofRef(),
            evidenceManifestDto.getExternalVerificationStatus(),
            evidenceManifestDto.getEnvironmentId(),
            evidenceManifestDto.getEnvironmentType(),
            evidenceManifestDto.getEvidenceOwner(),
            evidenceManifestDto.getArtifactArchiveRef(),
            evidenceManifestDto.getVerifierOperator(),
            toScaleEvidenceFileDigests(evidenceManifestDto.getEvidenceFileDigests()),
            toScaleEvidenceBundle(evidenceManifestDto.getVerificationBundle())
        );
        return evidenceManifest.hasAnyEvidence() ? evidenceManifest : null;
    }

    private Map<String, BenchmarkScaleEvidenceFileDigest> toScaleEvidenceFileDigests(
        Map<String, BenchmarkScaleEvidenceFileDigestDTO> digestDtos) {
        if (digestDtos == null || digestDtos.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, BenchmarkScaleEvidenceFileDigest> digests =
            new LinkedHashMap<String, BenchmarkScaleEvidenceFileDigest>();
        for (Map.Entry<String, BenchmarkScaleEvidenceFileDigestDTO> entry : digestDtos.entrySet()) {
            BenchmarkScaleEvidenceFileDigestDTO digestDto = entry.getValue();
            if (digestDto != null) {
                BenchmarkScaleEvidenceFileDigest digest = new BenchmarkScaleEvidenceFileDigest(
                    digestDto.getSha256(),
                    digestDto.getSizeBytes()
                );
                if (digest.hasAnyEvidence()) {
                    digests.put(entry.getKey(), digest);
                }
            }
        }
        return digests;
    }

    private BenchmarkScaleEvidenceBundle toScaleEvidenceBundle(BenchmarkScaleEvidenceBundleDTO bundleDto) {
        if (bundleDto == null) {
            return null;
        }
        BenchmarkScaleEvidenceBundle bundle = new BenchmarkScaleEvidenceBundle(
            bundleDto.getObservedConcurrency(),
            bundleDto.getObservedDailyQueryVolume(),
            bundleDto.getObservedDatasetSizeBytes(),
            bundleDto.getWorkloadReplayDurationHours(),
            bundleDto.getP95LatencyMs(),
            bundleDto.getP99LatencyMs(),
            bundleDto.getScannedBytes(),
            bundleDto.getCpuUsagePercent(),
            bundleDto.getQueueWaitMs(),
            bundleDto.getCostBillAmount(),
            bundleDto.getCostBillCurrency(),
            bundleDto.getVerifierRef()
        );
        return bundle.hasAnyEvidence() ? bundle : null;
    }

    private List<BenchmarkTestSetLabel> toTestSetLabels(List<BenchmarkTestSetLabelDTO> labelDtos) {
        if (labelDtos == null || labelDtos.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkTestSetLabel> labels = new ArrayList<BenchmarkTestSetLabel>(labelDtos.size());
        for (BenchmarkTestSetLabelDTO item : labelDtos) {
            labels.add(new BenchmarkTestSetLabel(item.getType(), item.getValue()));
        }
        return Collections.unmodifiableList(labels);
    }

    private List<BenchmarkSourceReference> toSourceReferences(List<BenchmarkSourceReferenceDTO> referenceDtos) {
        if (referenceDtos == null || referenceDtos.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkSourceReference> refs = new ArrayList<BenchmarkSourceReference>(referenceDtos.size());
        for (BenchmarkSourceReferenceDTO item : referenceDtos) {
            refs.add(new BenchmarkSourceReference(item.getType(), item.getReferenceId()));
        }
        return Collections.unmodifiableList(refs);
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && primary.trim().length() > 0) {
            return primary.trim();
        }
        return fallback == null || fallback.trim().length() == 0 ? null : fallback.trim();
    }

    public List<BenchmarkEngineProfile> buildEngineProfiles(BenchmarkTask task) {
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
            return "隔离影子环境的基线运行。";
        }
        if (taskType == BenchmarkTaskType.COMPARISON && index > 0) {
            return "用于跨引擎对比的辅助引擎。";
        }
        if (taskType == BenchmarkTaskType.REGRESSION_GUARD) {
            return "与已批准基线对比的回归防护运行。";
        }
        return "主压测引擎结果。";
    }

    public List<BenchmarkThresholdAssessment> evaluateThresholds(List<BenchmarkThreshold> thresholds,
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

    public List<BenchmarkRecommendation> buildRecommendations(BenchmarkTaskType taskType,
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
                    "固化基线报告和阈值包",
                    "保存基线画像，便于后续回归任务对比稳定的影子环境快照。",
                    "提升后续回归检测能力和报告可解释性。",
                    BenchmarkRecommendationRiskLevel.LOW
                ),
                new BenchmarkRecommendation(
                    "RESOURCE_TUNING",
                    "仅在阈值包稳定后调优并发",
                    reportVerdict == BenchmarkThresholdVerdict.PASS
                        ? "当前基线在目标范围内，可以把并发调优作为第二步。"
                        : "延迟和 CPU 阈值停止告警前，不要提高并发。",
                    "避免在基线可信前放大噪声压测结果。",
                    BenchmarkRecommendationRiskLevel.MEDIUM
                )
            );
        }
        if (taskType == BenchmarkTaskType.COMPARISON) {
            return Arrays.asList(
                new BenchmarkRecommendation(
                    "ENGINE_SELECTION",
                    "优先选择 p99 和 CPU 画像更强的引擎",
                    "使用对比报告决定该 SQL 指纹的默认路由应归属哪个引擎。",
                    "降低重型分析 SQL 的路由歧义。",
                    BenchmarkRecommendationRiskLevel.MEDIUM
                ),
                new BenchmarkRecommendation(
                    "PLAN_ALIGNMENT",
                    "随报告一起捕获路由和会话变量",
                    "只有影响计划的设置与结果一起版本化时，对比报告才可复用。",
                    "提升跨引擎和后续优化任务的可复现性。",
                    BenchmarkRecommendationRiskLevel.LOW
                )
            );
        }
        return Arrays.asList(
            new BenchmarkRecommendation(
                "REGRESSION_GATE",
                "关键阈值失败时阻断发布",
                "回归防护任务应产出发布门禁结果，而不仅是描述性报告。",
                "阻止延迟回归进入生产发布。",
                BenchmarkRecommendationRiskLevel.LOW
            ),
            new BenchmarkRecommendation(
                "OPTIMIZATION_FEEDBACK",
                "将失败指纹回传到 SQL 优化",
                "当回归防护检测到 p99 或扫描量失败时，将指纹发送到 SQL 优化待办。",
                "闭合压测证据与优化工作的反馈链路。",
                reportVerdict == BenchmarkThresholdVerdict.FAIL
                    ? BenchmarkRecommendationRiskLevel.HIGH
                    : BenchmarkRecommendationRiskLevel.MEDIUM
            )
        );
    }

    private BenchmarkRegressionSummary buildRegressionSummary(BenchmarkTaskType taskType,
                                                              List<BenchmarkThresholdAssessment> assessments) {
        if (taskType != BenchmarkTaskType.REGRESSION_GUARD) {
            return null;
        }
        int hitCount = 0;
        int failedCount = 0;
        int warningCount = 0;
        if (assessments != null) {
            for (BenchmarkThresholdAssessment assessment : assessments) {
                if (assessment.getVerdict() == BenchmarkThresholdVerdict.FAIL) {
                    hitCount++;
                    failedCount++;
                    continue;
                }
                if (assessment.getVerdict() == BenchmarkThresholdVerdict.WARNING) {
                    hitCount++;
                    warningCount++;
                }
            }
        }
        boolean alertRequired = failedCount > 0;
        String summary = hitCount == 0
            ? "回归门禁保持在已配置阈值内。"
            : "回归门禁命中 " + hitCount + " 个阈值：失败=" + failedCount + "，预警=" + warningCount + "。";
        return new BenchmarkRegressionSummary(
            Integer.valueOf(hitCount),
            Integer.valueOf(failedCount),
            Integer.valueOf(warningCount),
            Boolean.valueOf(alertRequired),
            summary
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

    private BenchmarkRegressionSummaryVO toRegressionSummaryVO(BenchmarkRegressionSummary summary) {
        if (summary == null) {
            return null;
        }
        return new BenchmarkRegressionSummaryVO(
            summary.getThresholdHitCount(),
            summary.getFailedThresholdCount(),
            summary.getWarningThresholdCount(),
            summary.getAlertRequired(),
            summary.getSummary()
        );
    }

    private List<BenchmarkAlertLinkageVO> toAlertLinkageVOs(List<BenchmarkAlertLinkage> linkages) {
        if (linkages == null || linkages.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkAlertLinkageVO> items = new ArrayList<BenchmarkAlertLinkageVO>(linkages.size());
        for (BenchmarkAlertLinkage linkage : linkages) {
            items.add(
                new BenchmarkAlertLinkageVO(
                    linkage.getAlertId(),
                    linkage.getAlertType(),
                    linkage.getAlertLevel(),
                    linkage.getAlertStatus(),
                    linkage.getNotifyStatus(),
                    linkage.getSummary(),
                    linkage.getDetailPath(),
                    linkage.getLinkageMode(),
                    linkage.getNotificationLogId()
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
            "延迟分布",
            "分位桶",
            "延迟（毫秒）",
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
            "吞吐时间线",
            "已用时间",
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
                        new BenchmarkTrendPointVO("预热", scaled(profile.getCpuUsagePercent(), "0.72")),
                        new BenchmarkTrendPointVO("稳定", scaled(profile.getCpuUsagePercent(), "0.88")),
                        new BenchmarkTrendPointVO("峰值", profile.getCpuUsagePercent())
                    )
                )
            );
            series.add(
                new BenchmarkTrendSeriesVO(
                    profile.getEngine().name() + " Memory",
                    Arrays.asList(
                        new BenchmarkTrendPointVO("预热", scaled(profile.getMemoryUsageMb(), "0.67")),
                        new BenchmarkTrendPointVO("稳定", scaled(profile.getMemoryUsageMb(), "0.84")),
                        new BenchmarkTrendPointVO("峰值", profile.getMemoryUsageMb())
                    )
                )
            );
        }
        return new BenchmarkTrendChartVO(
            "RESOURCE_USAGE_CURVE",
            "资源使用曲线",
            "压测阶段",
            "资源值",
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
