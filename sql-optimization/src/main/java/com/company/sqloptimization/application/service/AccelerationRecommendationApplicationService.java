package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqloptimization.application.controller.dto.AccelerationRecommendationCreateRequest;
import com.company.sqloptimization.application.controller.vo.AccelerationRecommendationVO;
import com.company.sqloptimization.application.controller.vo.RecommendationDiffVO;
import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.recommendation.repository.AccelerationRecommendationRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AccelerationRecommendationApplicationService {

    private final AccelerationRecommendationRepository recommendationRepository;
    private final SqlDiffApplicationService sqlDiffApplicationService;

    @Autowired
    public AccelerationRecommendationApplicationService(AccelerationRecommendationRepository recommendationRepository,
                                                        SqlDiffApplicationService sqlDiffApplicationService) {
        this.recommendationRepository = recommendationRepository;
        this.sqlDiffApplicationService = sqlDiffApplicationService;
    }

    public AccelerationRecommendationApplicationService(AccelerationRecommendationRepository recommendationRepository) {
        this(recommendationRepository, new SqlDiffApplicationService(new SqlOptimizationPipelineService()));
    }

    public AccelerationRecommendationVO createRecommendation(AccelerationRecommendationCreateRequest request) {
        String tenantId = requireAuthorizedTenant(request == null ? null : request.getTenantId());
        if (request == null || request.getRecommendationType() == null) {
            throw invalidArgument("recommendationType", "recommendationType is required");
        }
        if (!StringUtils.hasText(request.getRecommendedSqlText())) {
            throw invalidArgument("recommendedSqlText", "recommendedSqlText is required");
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

    public AccelerationRecommendationVO getRecommendation(String recommendationId) {
        AccelerationRecommendation recommendation = recommendationRepository.findByRecommendationId(recommendationId);
        if (recommendation == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Recommendation not found: " + recommendationId
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
                "Recommendation not found: " + recommendationId
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
        vo.setExpectedBenefit(recommendation.getExpectedBenefit());
        vo.setEstimatedCost(recommendation.getEstimatedCost());
        vo.setConfidence(recommendation.getConfidence());
        vo.setValidationMethod(recommendation.getValidationMethod());
        vo.setValidationStatus(recommendation.getValidationStatus().name());
        vo.setAutoApplyAllowed(Boolean.valueOf(recommendation.isAutoApplyAllowed()));
        vo.setManualReviewRequired(Boolean.valueOf(recommendation.isManualReviewRequired()));
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
            throw new AccessDeniedException("Request tenantId does not match authenticated tenant context");
        }
        return contextTenantId;
    }

    private String requireContextTenant() {
        String contextTenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(contextTenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "tenantId is missing from authenticated request context"
            );
        }
        return contextTenantId;
    }

    private void verifyTenantAccess(String resourceTenantId) {
        String contextTenantId = requireContextTenant();
        if (!contextTenantId.equals(resourceTenantId)) {
            throw new AccessDeniedException("Authenticated tenant cannot access this recommendation");
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
}
