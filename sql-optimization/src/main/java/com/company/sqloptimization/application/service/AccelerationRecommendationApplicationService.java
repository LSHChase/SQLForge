package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqloptimization.application.controller.dto.AccelerationRecommendationCreateRequest;
import com.company.sqloptimization.application.controller.vo.AccelerationRecommendationVO;
import com.company.sqloptimization.application.controller.vo.RecommendationPageVO;
import com.company.sqloptimization.application.controller.vo.RecommendationDiffVO;
import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendationFilter;
import com.company.sqloptimization.domain.recommendation.repository.AccelerationRecommendationRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AccelerationRecommendationApplicationService {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 8;
    private static final int MAX_PAGE_SIZE = 100;

    private final AccelerationRecommendationRepository recommendationRepository;
    private final SqlDiffApplicationService sqlDiffApplicationService;
    private final L2AccelerationArtifactApplicationService accelerationArtifactApplicationService;

    @Autowired
    public AccelerationRecommendationApplicationService(AccelerationRecommendationRepository recommendationRepository,
                                                        SqlDiffApplicationService sqlDiffApplicationService,
                                                        L2AccelerationArtifactApplicationService accelerationArtifactApplicationService) {
        this.recommendationRepository = recommendationRepository;
        this.sqlDiffApplicationService = sqlDiffApplicationService;
        this.accelerationArtifactApplicationService = accelerationArtifactApplicationService;
    }

    public AccelerationRecommendationApplicationService(AccelerationRecommendationRepository recommendationRepository,
                                                        SqlDiffApplicationService sqlDiffApplicationService) {
        this(
            recommendationRepository,
            sqlDiffApplicationService,
            new L2AccelerationArtifactApplicationService(new SqlOptimizationPipelineService())
        );
    }

    public AccelerationRecommendationApplicationService(AccelerationRecommendationRepository recommendationRepository) {
        this(recommendationRepository, new SqlDiffApplicationService(new SqlOptimizationPipelineService()));
    }

    public AccelerationRecommendationVO createRecommendation(AccelerationRecommendationCreateRequest request) {
        String tenantId = requireAuthorizedTenant(request == null ? null : request.getTenantId());
        if (request == null || request.getRecommendationType() == null) {
            throw invalidArgument("recommendationType", "recommendationType 为必填项");
        }
        if (!StringUtils.hasText(request.getRecommendedSqlText())) {
            throw invalidArgument("recommendedSqlText", "recommendedSqlText 为必填项");
        }
        Instant now = Instant.now();
        AccelerationRecommendation recommendation = AccelerationRecommendation.builder()
            .recommendationId(UUID.randomUUID().toString())
            .tenantId(tenantId)
            .recommendationType(request.getRecommendationType())
            .sourceSqlId(trimToNull(request.getSourceSqlId()))
            .historyId(trimToNull(request.getHistoryId()))
            .parseTaskId(trimToNull(request.getParseTaskId()))
            .batchId(trimToNull(request.getBatchId()))
            .routeDecisionId(trimToNull(request.getRouteDecisionId()))
            .alertId(trimToNull(request.getAlertId()))
            .sqlFingerprint(trimToNull(request.getSqlFingerprint()))
            .sourceSqlText(trimToNull(request.getSourceSqlText()))
            .recommendedSqlText(request.getRecommendedSqlText().trim())
            .targetEngine(trimToNull(request.getTargetEngine()))
            .targetDatasource(trimToNull(request.getTargetDatasource()))
            .reportCode(trimToNull(request.getReportCode()))
            .logicalObjectKey(trimToNull(request.getLogicalObjectKey()))
            .summary(trimToNull(request.getSummary()))
            .reason(trimToNull(request.getReason()))
            .expectedGain(trimToNull(request.getExpectedGain()))
            .benefitLevel(request.getBenefitLevel())
            .riskLevel(request.getRiskLevel())
            .riskSummary(trimToNull(request.getRiskSummary()))
            .requiresDispatch(Boolean.TRUE.equals(request.getRequiresDispatch()))
            .status(request.getStatus())
            .sourceType(request.getSourceType() == null ? inferSourceType(request) : request.getSourceType())
            .sourceKind(request.getSourceKind() == null ? inferSourceKind(request) : request.getSourceKind())
            .sourceId(resolveSourceId(request))
            .evidenceLevel(
                request.getEvidenceLevel() == null ? inferEvidenceLevel(request) : request.getEvidenceLevel()
            )
            .schemaVersion(trimToNull(request.getSchemaVersion()))
            .ruleChain(request.getRuleChain())
            .unappliedRules(request.getUnappliedRules())
            .preconditions(request.getPreconditions())
            .semanticRisks(request.getSemanticRisks())
            .expectedBenefit(request.getExpectedBenefit())
            .estimatedCost(request.getEstimatedCost())
            .confidence(request.getConfidence())
            .validationMethod(trimToNull(request.getValidationMethod()))
            .validationStatus(request.getValidationStatus())
            .autoApplyAllowed(Boolean.valueOf(Boolean.TRUE.equals(request.getAutoApplyAllowed())))
            .manualReviewRequired(request.getManualReviewRequired())
            .sourceProblems(request.getSourceProblems())
            .issueRuleLinks(request.getIssueRuleLinks())
            .createdBy(RequestContext.getUserId())
            .createdAt(now)
            .updatedAt(now)
            .build();
        return toVo(recommendationRepository.save(recommendation));
    }

    public List<AccelerationRecommendationVO> listRecommendations() {
        String tenantId = requireContextTenant();
        List<AccelerationRecommendation> recommendations = recommendationRepository.findByTenantId(tenantId);
        List<AccelerationRecommendationVO> result = new ArrayList<AccelerationRecommendationVO>(recommendations.size());
        for (AccelerationRecommendation recommendation : recommendations) {
            result.add(toVo(recommendation));
        }
        return result;
    }

    public RecommendationPageVO listRecommendationPage(String recommendationType,
                                                       String status,
                                                       String benefitLevel,
                                                       String riskLevel,
                                                       String validationStatus,
                                                       Boolean requiresDispatch,
                                                       Boolean manualReviewRequired,
                                                       String sortBy,
                                                       String sortOrder,
                                                       Integer pageNo,
                                                       Integer pageSize) {
        return listRecommendationPage(
            recommendationType,
            status,
            benefitLevel,
            riskLevel,
            validationStatus,
            requiresDispatch,
            manualReviewRequired,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            sortBy,
            sortOrder,
            pageNo,
            pageSize
        );
    }

    public RecommendationPageVO listRecommendationPage(String recommendationType,
                                                       String status,
                                                       String benefitLevel,
                                                       String riskLevel,
                                                       String validationStatus,
                                                       Boolean requiresDispatch,
                                                       Boolean manualReviewRequired,
                                                       String sourceType,
                                                       String sourceKind,
                                                       String sourceId,
                                                       String historyId,
                                                       String parseTaskId,
                                                       String batchId,
                                                       String reportCode,
                                                       String sortBy,
                                                       String sortOrder,
                                                       Integer pageNo,
                                                       Integer pageSize) {
        String tenantId = requireContextTenant();
        int resolvedPageNo = normalizePageNo(pageNo);
        int resolvedPageSize = normalizePageSize(pageSize);
        AccelerationRecommendationFilter filter = new AccelerationRecommendationFilter();
        filter.setTenantId(tenantId);
        filter.setRecommendationType(normalizeFilterValue(recommendationType));
        filter.setStatus(normalizeFilterValue(status));
        filter.setBenefitLevel(normalizeFilterValue(benefitLevel));
        filter.setRiskLevel(normalizeFilterValue(riskLevel));
        filter.setValidationStatus(normalizeFilterValue(validationStatus));
        filter.setRequiresDispatch(requiresDispatch);
        filter.setManualReviewRequired(manualReviewRequired);
        filter.setSourceType(normalizeFilterValue(sourceType));
        filter.setSourceKind(normalizeFilterValue(sourceKind));
        filter.setSourceKinds(normalizeSourceKinds(sourceKind));
        filter.setSourceId(trimToNull(sourceId));
        filter.setHistoryId(trimToNull(historyId));
        filter.setParseTaskId(trimToNull(parseTaskId));
        filter.setBatchId(trimToNull(batchId));
        filter.setReportCode(trimToNull(reportCode));
        filter.setOrderByClause(resolveRecommendationOrderBy(sortBy, sortOrder));
        filter.setOffset((resolvedPageNo - 1) * resolvedPageSize);
        filter.setLimit(resolvedPageSize + 1);

        List<AccelerationRecommendation> rows = recommendationRepository.findPage(filter);
        boolean hasMore = rows.size() > resolvedPageSize;
        if (hasMore) {
            rows = new ArrayList<AccelerationRecommendation>(rows.subList(0, resolvedPageSize));
        }
        filter.setLimit(resolvedPageSize);
        int totalCount = recommendationRepository.count(filter);
        List<AccelerationRecommendationVO> items =
            new ArrayList<AccelerationRecommendationVO>(rows.size());
        for (AccelerationRecommendation recommendation : rows) {
            items.add(toVo(recommendation));
        }
        return new RecommendationPageVO(
            items,
            Integer.valueOf(resolvedPageNo),
            Integer.valueOf(resolvedPageSize),
            Integer.valueOf(totalCount),
            Integer.valueOf(pageCount(totalCount, resolvedPageSize)),
            Boolean.valueOf(hasMore)
        );
    }

    public AccelerationRecommendationVO getRecommendation(String recommendationId) {
        AccelerationRecommendation recommendation = recommendationRepository.findByRecommendationId(recommendationId);
        if (recommendation == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "推荐不存在：" + recommendationId
            );
        }
        verifyTenantAccess(recommendation.getTenantId());
        return toVo(recommendation);
    }

    public RecommendationDiffVO getRecommendationDiff(String recommendationId) {
        AccelerationRecommendation recommendation = recommendationRepository.findByRecommendationId(recommendationId);
        if (recommendation == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "推荐不存在：" + recommendationId
            );
        }
        verifyTenantAccess(recommendation.getTenantId());
        return sqlDiffApplicationService.buildRecommendationDiff(recommendation);
    }

    private AccelerationRecommendationVO toVo(AccelerationRecommendation recommendation) {
        AccelerationRecommendationVO vo = new AccelerationRecommendationVO();
        vo.setRecommendationId(recommendation.getRecommendationId());
        vo.setTenantId(recommendation.getTenantId());
        vo.setRecommendationType(recommendation.getRecommendationType().name());
        vo.setSourceSqlId(recommendation.getSourceSqlId());
        vo.setHistoryId(recommendation.getHistoryId());
        vo.setParseTaskId(recommendation.getParseTaskId());
        vo.setBatchId(recommendation.getBatchId());
        vo.setRouteDecisionId(recommendation.getRouteDecisionId());
        vo.setAlertId(recommendation.getAlertId());
        vo.setSqlFingerprint(recommendation.getSqlFingerprint());
        vo.setSourceSqlText(recommendation.getSourceSqlText());
        vo.setRecommendedSqlText(recommendation.getRecommendedSqlText());
        vo.setTargetEngine(recommendation.getTargetEngine());
        vo.setTargetDatasource(recommendation.getTargetDatasource());
        vo.setReportCode(recommendation.getReportCode());
        vo.setLogicalObjectKey(recommendation.getLogicalObjectKey());
        vo.setSummary(recommendation.getSummary());
        vo.setReason(recommendation.getReason());
        vo.setExpectedGain(recommendation.getExpectedGain());
        vo.setBenefitLevel(recommendation.getBenefitLevel().name());
        vo.setRiskLevel(recommendation.getRiskLevel().name());
        vo.setRiskSummary(recommendation.getRiskSummary());
        vo.setRequiresDispatch(Boolean.valueOf(recommendation.isRequiresDispatch()));
        vo.setStatus(recommendation.getStatus().name());
        vo.setSourceType(recommendation.getSourceType() == null ? null : recommendation.getSourceType().name());
        vo.setSourceKind(recommendation.getSourceKind() == null ? null : recommendation.getSourceKind().name());
        vo.setSourceId(recommendation.getSourceId());
        vo.setEvidenceLevel(
            recommendation.getEvidenceLevel() == null ? null : recommendation.getEvidenceLevel().name()
        );
        vo.setSchemaVersion(recommendation.getSchemaVersion());
        vo.setRuleChain(recommendation.getRuleChain());
        vo.setUnappliedRules(recommendation.getUnappliedRules());
        vo.setPreconditions(recommendation.getPreconditions());
        vo.setSemanticRisks(recommendation.getSemanticRisks());
        vo.setAccelerationArtifact(accelerationArtifactApplicationService.buildForRecommendation(recommendation));
        vo.setExpectedBenefit(recommendation.getExpectedBenefit());
        vo.setEstimatedCost(recommendation.getEstimatedCost());
        vo.setConfidence(recommendation.getConfidence());
        vo.setValidationMethod(recommendation.getValidationMethod());
        vo.setValidationStatus(recommendation.getValidationStatus().name());
        vo.setAutoApplyAllowed(Boolean.valueOf(recommendation.isAutoApplyAllowed()));
        vo.setManualReviewRequired(Boolean.valueOf(recommendation.isManualReviewRequired()));
        vo.setSourceProblems(recommendation.getSourceProblems());
        vo.setIssueRuleLinks(recommendation.getIssueRuleLinks());
        vo.setCreatedBy(recommendation.getCreatedBy());
        vo.setCreatedAt(recommendation.getCreatedAt());
        vo.setUpdatedAt(recommendation.getUpdatedAt());
        return vo;
    }

    private GovernanceSourceType inferSourceType(AccelerationRecommendationCreateRequest request) {
        if (StringUtils.hasText(request.getParseTaskId()) || StringUtils.hasText(request.getBatchId())) {
            return GovernanceSourceType.PARSE;
        }
        if (StringUtils.hasText(request.getHistoryId())) {
            return GovernanceSourceType.QUERY;
        }
        return null;
    }

    private GovernanceSourceKind inferSourceKind(AccelerationRecommendationCreateRequest request) {
        if (StringUtils.hasText(request.getBatchId())) {
            return GovernanceSourceKind.PARSE_BATCH;
        }
        if (StringUtils.hasText(request.getParseTaskId())) {
            return GovernanceSourceKind.STRUCTURE_PARSE;
        }
        if (StringUtils.hasText(request.getHistoryId())) {
            return GovernanceSourceKind.QUERY_HISTORY;
        }
        return null;
    }

    private String resolveSourceId(AccelerationRecommendationCreateRequest request) {
        if (StringUtils.hasText(request.getSourceId())) {
            return request.getSourceId().trim();
        }
        if (StringUtils.hasText(request.getSourceSqlId())) {
            return request.getSourceSqlId().trim();
        }
        if (StringUtils.hasText(request.getHistoryId())) {
            return request.getHistoryId().trim();
        }
        if (StringUtils.hasText(request.getParseTaskId())) {
            return request.getParseTaskId().trim();
        }
        if (StringUtils.hasText(request.getBatchId())) {
            return request.getBatchId().trim();
        }
        return null;
    }

    private EvidenceLevel inferEvidenceLevel(AccelerationRecommendationCreateRequest request) {
        if (StringUtils.hasText(request.getHistoryId())) {
            return EvidenceLevel.RUNTIME_HISTORY;
        }
        if (StringUtils.hasText(request.getParseTaskId()) || StringUtils.hasText(request.getBatchId())) {
            return EvidenceLevel.STATIC_PARSE;
        }
        return null;
    }

    private String requireAuthorizedTenant(String requestTenantId) {
        String contextTenantId = requireContextTenant();
        if (StringUtils.hasText(requestTenantId) && !contextTenantId.equals(requestTenantId.trim())) {
            throw new AccessDeniedException("请求 tenantId 与已认证租户上下文不一致");
        }
        return contextTenantId;
    }

    private String requireContextTenant() {
        String contextTenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(contextTenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "已认证请求上下文缺少 tenantId"
            );
        }
        return contextTenantId;
    }

    private void verifyTenantAccess(String resourceTenantId) {
        String contextTenantId = requireContextTenant();
        if (!contextTenantId.equals(resourceTenantId)) {
            throw new AccessDeniedException("当前认证租户无权访问该推荐");
        }
    }

    private BizException invalidArgument(String field, String message) {
        return new BizException(
            ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
            HttpStatus.BAD_REQUEST,
            field + ": " + message
        );
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeFilterValue(String value) {
        String normalized = trimToNull(value);
        if (normalized == null || "ALL".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private List<String> normalizeSourceKinds(String value) {
        String normalized = normalizeFilterValue(value);
        if (normalized == null) {
            return null;
        }
        List<String> sourceKinds = new ArrayList<String>();
        for (String part : Arrays.asList(normalized.split(","))) {
            String sourceKind = trimToNull(part);
            if (sourceKind != null && !"ALL".equalsIgnoreCase(sourceKind)) {
                sourceKinds.add(sourceKind.toUpperCase(Locale.ROOT));
            }
        }
        return sourceKinds.isEmpty() ? null : sourceKinds;
    }

    private String resolveRecommendationOrderBy(String sortBy, String sortOrder) {
        String normalizedSortBy = trimToNull(sortBy);
        String column = "created_at";
        if ("updatedAt".equals(normalizedSortBy)) {
            column = "updated_at";
        } else if ("recommendationType".equals(normalizedSortBy)) {
            column = "recommendation_type";
        } else if ("status".equals(normalizedSortBy)) {
            column = "status";
        } else if ("benefitLevel".equals(normalizedSortBy)) {
            column = "benefit_level";
        } else if ("riskLevel".equals(normalizedSortBy)) {
            column = "risk_level";
        } else if ("validationStatus".equals(normalizedSortBy)) {
            column = "validation_status";
        } else if ("requiresDispatch".equals(normalizedSortBy)) {
            column = "requires_dispatch";
        } else if ("manualReviewRequired".equals(normalizedSortBy)) {
            column = "manual_review_required";
        } else if ("recommendationId".equals(normalizedSortBy)) {
            column = "recommendation_id";
        }
        String direction = "ASC".equalsIgnoreCase(trimToNull(sortOrder)) ? "ASC" : "DESC";
        return column + " " + direction + ", recommendation_id DESC";
    }

    private int normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo.intValue() <= 0 ? DEFAULT_PAGE_NO : pageNo.intValue();
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize.intValue() <= 0
            ? DEFAULT_PAGE_SIZE
            : Math.min(MAX_PAGE_SIZE, pageSize.intValue());
    }

    private int pageCount(int totalCount, int pageSize) {
        if (totalCount <= 0) {
            return 0;
        }
        return (totalCount + pageSize - 1) / pageSize;
    }
}
