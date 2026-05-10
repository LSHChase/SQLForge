package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqloptimization.application.controller.dto.AccelerationRecommendationCreateRequest;
import com.company.sqloptimization.application.controller.vo.AccelerationRecommendationVO;
import com.company.sqloptimization.application.controller.vo.RecommendationDiffVO;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.recommendation.repository.AccelerationRecommendationRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AccelerationRecommendationApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String DIFF_IMPLEMENTATION_STAGE = "RECOMMENDATION_DIFF_CONTRACT_BASELINE";

    private final AccelerationRecommendationRepository recommendationRepository;

    public AccelerationRecommendationApplicationService(AccelerationRecommendationRepository recommendationRepository) {
        this.recommendationRepository = recommendationRepository;
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
        RecommendationDiffVO vo = new RecommendationDiffVO();
        vo.setRecommendationId(recommendation.getRecommendationId());
        vo.setTenantId(recommendation.getTenantId());
        vo.setSourceType(inferSourceType(recommendation));
        vo.setSourceKind(inferSourceKind(recommendation));
        vo.setSourceId(inferSourceId(recommendation));
        vo.setEvidenceLevel(inferEvidenceLevel(recommendation));
        vo.setSqlFingerprint(recommendation.getSqlFingerprint());
        vo.setOriginalSql(recommendation.getSourceSqlText());
        vo.setRecommendedSql(recommendation.getRecommendedSqlText());
        vo.setTextDiff(Collections.<Map<String, Object>>emptyList());
        vo.setRuleDiff(Collections.<Map<String, Object>>emptyList());
        vo.setAstSummaryDiff(Collections.<String, Object>emptyMap());
        vo.setDiffSummary(buildDiffSummary());
        vo.setDiffStatus("CONTRACT_ONLY");
        vo.setContractStage(CONTRACT_STAGE);
        vo.setImplementationStage(DIFF_IMPLEMENTATION_STAGE);
        return vo;
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
        vo.setCreatedBy(recommendation.getCreatedBy());
        vo.setCreatedAt(recommendation.getCreatedAt());
        vo.setUpdatedAt(recommendation.getUpdatedAt());
        return vo;
    }

    private Map<String, Object> buildDiffSummary() {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("textDiffReady", Boolean.FALSE);
        summary.put("ruleDiffReady", Boolean.FALSE);
        summary.put("astSummaryReady", Boolean.FALSE);
        summary.put("followUpTask", "HARN-132");
        return summary;
    }

    private String inferSourceType(AccelerationRecommendation recommendation) {
        if (StringUtils.hasText(recommendation.getParseTaskId()) || StringUtils.hasText(recommendation.getBatchId())) {
            return "PARSE";
        }
        if (StringUtils.hasText(recommendation.getHistoryId())) {
            return "QUERY";
        }
        return null;
    }

    private String inferSourceKind(AccelerationRecommendation recommendation) {
        if (StringUtils.hasText(recommendation.getBatchId())) {
            return "PARSE_BATCH";
        }
        if (StringUtils.hasText(recommendation.getParseTaskId())) {
            return "STRUCTURE_PARSE";
        }
        if (StringUtils.hasText(recommendation.getHistoryId())) {
            return "QUERY_HISTORY";
        }
        return null;
    }

    private String inferSourceId(AccelerationRecommendation recommendation) {
        if (StringUtils.hasText(recommendation.getSourceSqlId())) {
            return recommendation.getSourceSqlId();
        }
        if (StringUtils.hasText(recommendation.getHistoryId())) {
            return recommendation.getHistoryId();
        }
        if (StringUtils.hasText(recommendation.getParseTaskId())) {
            return recommendation.getParseTaskId();
        }
        if (StringUtils.hasText(recommendation.getBatchId())) {
            return recommendation.getBatchId();
        }
        return null;
    }

    private String inferEvidenceLevel(AccelerationRecommendation recommendation) {
        if (StringUtils.hasText(recommendation.getHistoryId())) {
            return "RUNTIME_HISTORY";
        }
        if (StringUtils.hasText(recommendation.getParseTaskId()) || StringUtils.hasText(recommendation.getBatchId())) {
            return "STATIC_PARSE";
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
