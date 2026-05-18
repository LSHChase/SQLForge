package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.application.controller.dto.BenchmarkRecommendationComparisonCreateRequest;
import com.company.benchmarkengine.application.controller.dto.BenchmarkScaleTargetDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkSourceReferenceDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskContextDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskSubmitRequest;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTestSetLabelDTO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkRecommendationComparisonResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskSubmitResponse;
import com.company.benchmarkengine.domain.benchmark.BenchmarkRecommendationSqlRole;
import com.company.benchmarkengine.domain.benchmark.BenchmarkScaleTarget;
import com.company.benchmarkengine.domain.benchmark.BenchmarkSourceReference;
import com.company.benchmarkengine.domain.benchmark.BenchmarkSourceReferenceType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTemplateType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSet;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetCase;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetCaseStatus;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetLabel;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetLabelType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetSource;
import com.company.benchmarkengine.domain.benchmark.repository.BenchmarkTestSetRepository;
import com.company.benchmarkengine.infrastructure.governance.BenchmarkAuditRecord;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationAccelerationRecommendation;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationRecommendationClient;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BenchmarkRecommendationComparisonApplicationService {

    private static final String ORCHESTRATION_OPERATION = "BENCHMARK_RECOMMENDATION_COMPARISON_SUBMIT";
    private static final String RESOURCE_TYPE_TASK = "BENCHMARK_ENGINE_TASK";
    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "RECOMMENDATION_COMPARISON_ORCHESTRATION_BASELINE";
    private static final String DEFAULT_TEMPLATE_ID = "comparison-dual-engine";
    private static final String DEFAULT_TEMPLATE_VERSION = "v2026.04";

    private final BenchmarkTestSetModelApplicationService benchmarkTestSetModelApplicationService;
    private final BenchmarkTestSetRepository benchmarkTestSetRepository;
    private final BenchmarkTaskApplicationService benchmarkTaskApplicationService;
    private final GovernanceCapabilityClient governanceCapabilityClient;
    private final SqlOptimizationRecommendationClient sqlOptimizationRecommendationClient;

    public BenchmarkRecommendationComparisonApplicationService(
        BenchmarkTestSetModelApplicationService benchmarkTestSetModelApplicationService,
        BenchmarkTestSetRepository benchmarkTestSetRepository,
        BenchmarkTaskApplicationService benchmarkTaskApplicationService,
        GovernanceCapabilityClient governanceCapabilityClient,
        SqlOptimizationRecommendationClient sqlOptimizationRecommendationClient
    ) {
        this.benchmarkTestSetModelApplicationService = benchmarkTestSetModelApplicationService;
        this.benchmarkTestSetRepository = benchmarkTestSetRepository;
        this.benchmarkTaskApplicationService = benchmarkTaskApplicationService;
        this.governanceCapabilityClient = governanceCapabilityClient;
        this.sqlOptimizationRecommendationClient = sqlOptimizationRecommendationClient;
    }

    public BenchmarkRecommendationComparisonResponse submitComparisonBenchmark(String recommendationId,
                                                                              BenchmarkRecommendationComparisonCreateRequest request) {
        long start = System.currentTimeMillis();
        request.setTenantId(requireAuthorizedTenant(request.getTenantId()));
        BenchmarkScaleTargetDTO scaleTargetDto = resolveScaleTargetDto(request);
        BenchmarkScaleTarget scaleTarget = toScaleTarget(scaleTargetDto);
        try {
            SqlOptimizationAccelerationRecommendation recommendation =
                requireRecommendation(recommendationId, request.getTenantId());
            validateRecommendationForBenchmark(recommendation);
            validateComparisonSqlBoundary(recommendation);

            Instant now = Instant.now();
            String testSetId = UUID.randomUUID().toString();
            List<BenchmarkSourceReference> refs = buildSourceReferences(recommendationId, recommendation);
            List<BenchmarkTestSetLabel> labels = buildLabels(recommendation, request.getTestSetLabels());
            BenchmarkTestSet testSet = benchmarkTestSetModelApplicationService.buildGeneratedTestSet(
                request.getTenantId(),
                testSetId,
                resolveTestSetName(request, recommendationId),
                firstNonBlank(request.getTemplateId(), DEFAULT_TEMPLATE_ID),
                BenchmarkTemplateType.CROSS_ENGINE_COMPARISON,
                firstNonBlank(request.getTemplateVersion(), DEFAULT_TEMPLATE_VERSION),
                BenchmarkTestSetSource.RECOMMENDATION_GENERATION,
                labels,
                refs,
                buildCases(testSetId, recommendationId, recommendation, scaleTarget),
                now,
                RequestContext.getUserId()
            );
            benchmarkTestSetRepository.saveTestSet(testSet);

            BenchmarkTaskSubmitResponse taskResponse = benchmarkTaskApplicationService.submitTask(
                buildTaskSubmitRequest(request, recommendationId, recommendation, testSet, scaleTargetDto)
            );
            BenchmarkRecommendationComparisonResponse response = new BenchmarkRecommendationComparisonResponse();
            response.setRecommendationId(recommendationId);
            response.setBenchmarkSqlRole(request.getBenchmarkSqlRole());
            response.setTestSet(benchmarkTestSetModelApplicationService.buildResponse(testSet));
            response.setBenchmarkTask(taskResponse);
            response.setContractStage(CONTRACT_STAGE);
            response.setImplementationStage(IMPLEMENTATION_STAGE);
            writeAudit(
                recommendationId,
                response.getBenchmarkTask().getTaskId(),
                System.currentTimeMillis() - start,
                buildRequestSummary(recommendationId, request, scaleTarget),
                buildResponseSummary(response, scaleTarget, null)
            );
            return response;
        } catch (RuntimeException ex) {
            writeAudit(
                recommendationId,
                recommendationId,
                System.currentTimeMillis() - start,
                buildRequestSummary(recommendationId, request, scaleTarget),
                buildResponseSummary(null, scaleTarget, ex.getMessage())
            );
            throw ex;
        }
    }

    private SqlOptimizationAccelerationRecommendation requireRecommendation(String recommendationId, String tenantId) {
        SqlOptimizationAccelerationRecommendation recommendation =
            sqlOptimizationRecommendationClient.getRecommendation(trimToNull(recommendationId));
        if (recommendation == null) {
            throw invalidArgument("recommendationId", "推荐记录不存在");
        }
        if (!tenantId.equals(recommendation.getTenantId())) {
            throw new AccessDeniedException("当前认证租户无权访问该推荐");
        }
        return recommendation;
    }

    private void validateRecommendationForBenchmark(SqlOptimizationAccelerationRecommendation recommendation) {
        if ("CANCELLED".equals(trimToNull(recommendation.getStatus()))) {
            throw invalidArgument("recommendationStatus", "已取消的推荐不能提升为对比压测");
        }
        if (!hasText(recommendation.getSourceSqlText()) || !hasText(recommendation.getRecommendedSqlText())) {
            throw invalidArgument("recommendationSql", "对比压测必须同时提供 sourceSqlText 和 recommendedSqlText");
        }
    }

    private void validateComparisonSqlBoundary(SqlOptimizationAccelerationRecommendation recommendation) {
        String sourceFailure = BenchmarkReadonlySqlSupport.validateReadonlySql(recommendation.getSourceSqlText());
        if (sourceFailure != null) {
            throw invalidArgument("sourceSqlText", sourceFailure);
        }
        String recommendedFailure = BenchmarkReadonlySqlSupport.validateReadonlySql(recommendation.getRecommendedSqlText());
        if (recommendedFailure != null) {
            throw invalidArgument("recommendedSqlText", recommendedFailure);
        }
    }

    private List<BenchmarkSourceReference> buildSourceReferences(String recommendationId,
                                                                 SqlOptimizationAccelerationRecommendation recommendation) {
        List<BenchmarkSourceReference> refs = new ArrayList<BenchmarkSourceReference>();
        refs.add(new BenchmarkSourceReference(BenchmarkSourceReferenceType.RECOMMENDATION, recommendationId));
        appendUniqueRef(refs, BenchmarkSourceReferenceType.PARSE_TASK, recommendation.getParseTaskId());
        appendUniqueRef(refs, BenchmarkSourceReferenceType.QUERY_HISTORY, recommendation.getHistoryId());
        appendUniqueRef(refs, BenchmarkSourceReferenceType.REPORT, recommendation.getReportCode());
        appendUniqueRef(
            refs,
            BenchmarkSourceReferenceType.SQL_FINGERPRINT,
            firstNonBlank(recommendation.getSqlFingerprint(), SqlFingerprintUtils.fingerprint(recommendation.getSourceSqlText()))
        );
        return refs;
    }

    private List<BenchmarkTestSetLabel> buildLabels(SqlOptimizationAccelerationRecommendation recommendation,
                                                    List<BenchmarkTestSetLabelDTO> requestLabels) {
        List<BenchmarkTestSetLabel> labels = new ArrayList<BenchmarkTestSetLabel>();
        appendUniqueLabel(labels, BenchmarkTestSetLabelType.SCENARIO, "COMPARISON");
        appendUniqueLabel(labels, BenchmarkTestSetLabelType.DOMAIN, firstNonBlank(recommendation.getRecommendationType(), "RECOMMENDATION"));
        appendUniqueLabel(labels, BenchmarkTestSetLabelType.SOURCE, "RECOMMENDATION_ORCHESTRATION");
        appendUniqueLabel(labels, BenchmarkTestSetLabelType.RISK, firstNonBlank(recommendation.getRiskLevel(), "UNKNOWN"));
        if (requestLabels != null) {
            for (BenchmarkTestSetLabelDTO label : requestLabels) {
                if (label != null) {
                    appendUniqueLabel(labels, label.getType(), label.getValue());
                }
            }
        }
        return labels;
    }

    private List<BenchmarkTestSetCase> buildCases(String testSetId,
                                                  String recommendationId,
                                                  SqlOptimizationAccelerationRecommendation recommendation,
                                                  BenchmarkScaleTarget scaleTarget) {
        List<BenchmarkTestSetCase> cases = new ArrayList<BenchmarkTestSetCase>(2);
        cases.add(buildCase(testSetId, 1, "SOURCE_SQL", recommendationId, recommendation, recommendation.getSourceSqlText(), scaleTarget));
        cases.add(buildCase(testSetId, 2, "RECOMMENDED_SQL", recommendationId, recommendation, recommendation.getRecommendedSqlText(), scaleTarget));
        return cases;
    }

    private BenchmarkTestSetCase buildCase(String testSetId,
                                           int sequence,
                                           String role,
                                           String recommendationId,
                                           SqlOptimizationAccelerationRecommendation recommendation,
                                           String sqlText,
                                           BenchmarkScaleTarget scaleTarget) {
        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("recommendationId", recommendationId);
        evidence.put("role", role);
        evidence.put("recommendationType", recommendation.getRecommendationType());
        evidence.put("benefitLevel", recommendation.getBenefitLevel());
        evidence.put("riskLevel", recommendation.getRiskLevel());
        evidence.put("expectedGain", recommendation.getExpectedGain());
        evidence.put("reportCode", recommendation.getReportCode());
        evidence.put("logicalObjectKey", recommendation.getLogicalObjectKey());
        evidence.put("targetEngine", recommendation.getTargetEngine());
        evidence.put("targetDatasource", recommendation.getTargetDatasource());
        evidence.put("requiresDispatch", recommendation.getRequiresDispatch());
        evidence.put("scaleTarget", scaleTarget);
        return new BenchmarkTestSetCase(
            UUID.randomUUID().toString(),
            testSetId,
            Integer.valueOf(sequence),
            Integer.valueOf(sequence),
            role,
            sqlText,
            SqlFingerprintUtils.fingerprint(sqlText),
            trimToNull(recommendation.getTargetDatasource()),
            trimToNull(recommendation.getReportCode()),
            buildCaseTags(role, recommendation),
            null,
            BenchmarkTestSetCaseStatus.ACCEPTED,
            null,
            JsonUtils.toJson(evidence)
        );
    }

    private List<String> buildCaseTags(String role, SqlOptimizationAccelerationRecommendation recommendation) {
        Set<String> tags = new LinkedHashSet<String>();
        tags.add(role);
        addIfPresent(tags, recommendation.getRecommendationType());
        addIfPresent(tags, recommendation.getBenefitLevel());
        addIfPresent(tags, recommendation.getRiskLevel());
        return new ArrayList<String>(tags);
    }

    private BenchmarkTaskSubmitRequest buildTaskSubmitRequest(BenchmarkRecommendationComparisonCreateRequest request,
                                                              String recommendationId,
                                                              SqlOptimizationAccelerationRecommendation recommendation,
                                                              BenchmarkTestSet testSet,
                                                              BenchmarkScaleTargetDTO scaleTarget) {
        BenchmarkTaskSubmitRequest taskRequest = new BenchmarkTaskSubmitRequest();
        taskRequest.setTenantId(request.getTenantId());
        taskRequest.setTaskType(BenchmarkTaskType.COMPARISON);
        taskRequest.setSqlText(resolveBenchmarkSql(request.getBenchmarkSqlRole(), recommendation));

        BenchmarkTaskContextDTO context = new BenchmarkTaskContextDTO();
        context.setTargetEngines(request.getTargetEngines());
        context.setConcurrency(resolveBenchmarkConcurrency(request, scaleTarget));
        context.setDurationSeconds(request.getDurationSeconds());
        context.setRampUpSeconds(request.getRampUpSeconds());
        context.setDatasetSizeLabel(resolveBenchmarkDatasetSizeLabel(request, scaleTarget));
        context.setScaleTarget(scaleTarget);
        context.setTemplateId(testSet.getTemplateId());
        context.setTemplateType(BenchmarkTemplateType.CROSS_ENGINE_COMPARISON);
        context.setTemplateVersion(testSet.getTemplateVersion());
        context.setTestSetId(testSet.getTestSetId());
        context.setTestSetSource(BenchmarkTestSetSource.RECOMMENDATION_GENERATION);
        context.setTestSetLabels(toLabelDtos(testSet.getTestSetLabels()));
        context.setTestSetSourceRefs(toSourceReferenceDtos(testSet.getTestSetSourceRefs()));
        context.setReadonlyRequired(Boolean.TRUE);
        context.setShadowEnvironmentMode(com.company.benchmarkengine.domain.benchmark.ShadowEnvironmentMode.REQUIRED);
        context.setDesensitizationRequirement(com.company.benchmarkengine.domain.benchmark.DesensitizationRequirement.REQUIRED);
        context.setThresholds(request.getThresholds());
        taskRequest.setTaskContext(context);
        taskRequest.setSqlFingerprint(SqlFingerprintUtils.fingerprint(taskRequest.getSqlText()));
        return taskRequest;
    }

    private List<BenchmarkTestSetLabelDTO> toLabelDtos(List<BenchmarkTestSetLabel> labels) {
        if (labels == null || labels.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkTestSetLabelDTO> items = new ArrayList<BenchmarkTestSetLabelDTO>(labels.size());
        for (BenchmarkTestSetLabel label : labels) {
            BenchmarkTestSetLabelDTO item = new BenchmarkTestSetLabelDTO();
            item.setType(label.getType());
            item.setValue(label.getValue());
            items.add(item);
        }
        return items;
    }

    private List<BenchmarkSourceReferenceDTO> toSourceReferenceDtos(List<BenchmarkSourceReference> refs) {
        if (refs == null || refs.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkSourceReferenceDTO> items = new ArrayList<BenchmarkSourceReferenceDTO>(refs.size());
        for (BenchmarkSourceReference ref : refs) {
            BenchmarkSourceReferenceDTO item = new BenchmarkSourceReferenceDTO();
            item.setType(ref.getType());
            item.setReferenceId(ref.getReferenceId());
            items.add(item);
        }
        return items;
    }

    private String resolveBenchmarkSql(BenchmarkRecommendationSqlRole benchmarkSqlRole,
                                       SqlOptimizationAccelerationRecommendation recommendation) {
        if (benchmarkSqlRole == BenchmarkRecommendationSqlRole.SOURCE_SQL) {
            return recommendation.getSourceSqlText();
        }
        return recommendation.getRecommendedSqlText();
    }

    private BenchmarkScaleTargetDTO resolveScaleTargetDto(BenchmarkRecommendationComparisonCreateRequest request) {
        BenchmarkScaleTargetDTO requested = request.getScaleTarget();
        if (requested == null && request.getConcurrency() == null && !hasText(request.getDatasetSizeLabel())) {
            return null;
        }
        BenchmarkScaleTargetDTO resolved = new BenchmarkScaleTargetDTO();
        resolved.setTargetConcurrency(requested == null || requested.getTargetConcurrency() == null
            ? request.getConcurrency()
            : requested.getTargetConcurrency());
        resolved.setTargetDatasetSizeLabel(requested == null
            ? trimToNull(request.getDatasetSizeLabel())
            : firstNonBlank(requested.getTargetDatasetSizeLabel(), trimToNull(request.getDatasetSizeLabel())));
        resolved.setTargetDailyQueryVolume(requested == null ? null : requested.getTargetDailyQueryVolume());
        resolved.setTargetComplexityProfile(requested == null ? null : trimToNull(requested.getTargetComplexityProfile()));
        resolved.setTargetCostEfficiency(requested == null ? null : trimToNull(requested.getTargetCostEfficiency()));
        return hasAnyScaleTarget(resolved) ? resolved : null;
    }

    private BenchmarkScaleTarget toScaleTarget(BenchmarkScaleTargetDTO scaleTarget) {
        if (scaleTarget == null) {
            return null;
        }
        return new BenchmarkScaleTarget(
            scaleTarget.getTargetConcurrency(),
            scaleTarget.getTargetDatasetSizeLabel(),
            scaleTarget.getTargetDailyQueryVolume(),
            scaleTarget.getTargetComplexityProfile(),
            scaleTarget.getTargetCostEfficiency(),
            BenchmarkScaleTarget.STATUS_TARGET_DECLARED_UNVERIFIED,
            BenchmarkScaleTarget.DEFAULT_EVIDENCE_BOUNDARY,
            null
        );
    }

    private Integer resolveBenchmarkConcurrency(BenchmarkRecommendationComparisonCreateRequest request,
                                                BenchmarkScaleTargetDTO scaleTarget) {
        if (request.getConcurrency() != null) {
            return request.getConcurrency();
        }
        return scaleTarget == null ? null : scaleTarget.getTargetConcurrency();
    }

    private String resolveBenchmarkDatasetSizeLabel(BenchmarkRecommendationComparisonCreateRequest request,
                                                    BenchmarkScaleTargetDTO scaleTarget) {
        String requestedLabel = trimToNull(request.getDatasetSizeLabel());
        if (requestedLabel != null) {
            return requestedLabel;
        }
        return scaleTarget == null ? null : trimToNull(scaleTarget.getTargetDatasetSizeLabel());
    }

    private String resolveTestSetName(BenchmarkRecommendationComparisonCreateRequest request, String recommendationId) {
        return hasText(request.getTestSetName())
            ? request.getTestSetName().trim()
            : "recommendation-" + recommendationId + "-comparison";
    }

    private void appendUniqueRef(List<BenchmarkSourceReference> refs,
                                 BenchmarkSourceReferenceType type,
                                 String referenceId) {
        if (!hasText(referenceId)) {
            return;
        }
        for (BenchmarkSourceReference ref : refs) {
            if (ref != null && ref.getType() == type && referenceId.equals(ref.getReferenceId())) {
                return;
            }
        }
        refs.add(new BenchmarkSourceReference(type, referenceId));
    }

    private void appendUniqueLabel(List<BenchmarkTestSetLabel> labels,
                                   BenchmarkTestSetLabelType type,
                                   String value) {
        if (!hasText(value)) {
            return;
        }
        for (BenchmarkTestSetLabel label : labels) {
            if (label != null && label.getType() == type && value.equals(label.getValue())) {
                return;
            }
        }
        labels.add(new BenchmarkTestSetLabel(type, value));
    }

    private void addIfPresent(Set<String> target, String value) {
        if (hasText(value)) {
            target.add(value);
        }
    }

    private void writeAudit(String recommendationId,
                            String resourceId,
                            long elapsedMs,
                            String requestParams,
                            String responseSummary) {
        governanceCapabilityClient.writeAudit(
            new BenchmarkAuditRecord(
                ORCHESTRATION_OPERATION,
                RESOURCE_TYPE_TASK,
                resourceId,
                responseSummary.contains("\"resultStatus\":\"FAILED\"") ? "FAILED" : "QUEUED",
                elapsedMs,
                null,
                null,
                null,
                null,
                null,
                requestParams,
                responseSummary
            )
        );
    }

    private String buildRequestSummary(String recommendationId,
                                       BenchmarkRecommendationComparisonCreateRequest request,
                                       BenchmarkScaleTarget scaleTarget) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("serviceCode", ServiceCodeConstants.BENCHMARK_ENGINE);
        payload.put("recommendationId", recommendationId);
        payload.put("tenantId", request.getTenantId());
        payload.put("benchmarkSqlRole", request.getBenchmarkSqlRole() == null ? null : request.getBenchmarkSqlRole().name());
        payload.put("targetEngines", request.getTargetEngines());
        payload.put("concurrency", request.getConcurrency());
        payload.put("durationSeconds", request.getDurationSeconds());
        payload.put("rampUpSeconds", request.getRampUpSeconds());
        payload.put("datasetSizeLabel", trimToNull(request.getDatasetSizeLabel()));
        payload.put("scaleTarget", scaleTarget);
        payload.put("templateId", trimToNull(request.getTemplateId()));
        payload.put("templateVersion", trimToNull(request.getTemplateVersion()));
        return JsonUtils.toJson(payload);
    }

    private String buildResponseSummary(BenchmarkRecommendationComparisonResponse response,
                                        BenchmarkScaleTarget scaleTarget,
                                        String failureReason) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("resultStatus", response == null ? "FAILED" : response.getBenchmarkTask().getStatus().name());
        payload.put("testSetId", response == null || response.getTestSet() == null ? null : response.getTestSet().getTestSetId());
        payload.put("taskId", response == null || response.getBenchmarkTask() == null ? null : response.getBenchmarkTask().getTaskId());
        payload.put("scaleTarget", scaleTarget);
        payload.put("failureReason", failureReason);
        return JsonUtils.toJson(payload);
    }

    private String requireAuthorizedTenant(String requestTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (!hasText(contextTenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "已认证请求上下文缺少 tenantId"
            );
        }
        if (hasText(requestTenantId) && !contextTenantId.equals(requestTenantId.trim())) {
            throw new AccessDeniedException("请求 tenantId 与已认证租户上下文不一致");
        }
        return contextTenantId;
    }

    private BizException invalidArgument(String fieldName, String message) {
        return new BizException(
            ErrorCodeConstants.BENCHMARK_TASK_INVALID,
            HttpStatus.BAD_REQUEST,
            "推荐转压测请求无效：" + fieldName + ": " + message
        );
    }

    private String firstNonBlank(String primary, String fallback) {
        return hasText(primary) ? primary.trim() : fallback;
    }

    private boolean hasAnyScaleTarget(BenchmarkScaleTargetDTO scaleTarget) {
        return scaleTarget != null
            && (scaleTarget.getTargetConcurrency() != null
            || hasText(scaleTarget.getTargetDatasetSizeLabel())
            || scaleTarget.getTargetDailyQueryVolume() != null
            || hasText(scaleTarget.getTargetComplexityProfile())
            || hasText(scaleTarget.getTargetCostEfficiency()));
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }
}
