package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqloptimization.application.controller.vo.AccelerationRecommendationVO;
import com.company.sqloptimization.application.controller.vo.DispatchEventStatusHistoryVO;
import com.company.sqloptimization.application.controller.vo.DispatchEventVO;
import com.company.sqloptimization.application.controller.vo.RecommendationTraceVO;
import com.company.sqloptimization.domain.dispatch.DispatchEvent;
import com.company.sqloptimization.domain.dispatch.DispatchEventTransition;
import com.company.sqloptimization.domain.dispatch.repository.DispatchEventRepository;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.recommendation.repository.AccelerationRecommendationRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RecommendationTraceApplicationService {

    private final AccelerationRecommendationRepository recommendationRepository;
    private final DispatchEventRepository dispatchEventRepository;

    public RecommendationTraceApplicationService(AccelerationRecommendationRepository recommendationRepository,
                                                 DispatchEventRepository dispatchEventRepository) {
        this.recommendationRepository = recommendationRepository;
        this.dispatchEventRepository = dispatchEventRepository;
    }

    public RecommendationTraceVO trace(String recommendationId) {
        AccelerationRecommendation recommendation = recommendationRepository.findByRecommendationId(recommendationId);
        if (recommendation == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Recommendation not found: " + recommendationId
            );
        }
        verifyTenantAccess(recommendation.getTenantId());
        RecommendationTraceVO trace = new RecommendationTraceVO();
        trace.setRecommendationId(recommendation.getRecommendationId());
        trace.setTenantId(recommendation.getTenantId());
        trace.setHistoryId(recommendation.getHistoryId());
        trace.setParseTaskId(recommendation.getParseTaskId());
        trace.setBatchId(recommendation.getBatchId());
        trace.setRouteDecisionId(recommendation.getRouteDecisionId());
        trace.setAlertId(recommendation.getAlertId());
        trace.setSqlFingerprint(recommendation.getSqlFingerprint());
        trace.setReportCode(recommendation.getReportCode());
        trace.setLogicalObjectKey(recommendation.getLogicalObjectKey());
        trace.setRecommendation(toRecommendationVo(recommendation));
        trace.setDispatchEvents(toDispatchVos(dispatchEventRepository.findByTenantIdAndRecommendationId(
            recommendation.getTenantId(),
            recommendation.getRecommendationId()
        )));
        trace.setTraceRefs(buildTraceRefs(recommendation));
        return trace;
    }

    private Map<String, Object> buildTraceRefs(AccelerationRecommendation recommendation) {
        Map<String, Object> refs = new LinkedHashMap<String, Object>();
        putIfPresent(refs, "historyId", recommendation.getHistoryId());
        putIfPresent(refs, "parseTaskId", recommendation.getParseTaskId());
        putIfPresent(refs, "batchId", recommendation.getBatchId());
        putIfPresent(refs, "routeDecisionId", recommendation.getRouteDecisionId());
        putIfPresent(refs, "alertId", recommendation.getAlertId());
        putIfPresent(refs, "sqlFingerprint", recommendation.getSqlFingerprint());
        putIfPresent(refs, "reportCode", recommendation.getReportCode());
        putIfPresent(refs, "logicalObjectKey", recommendation.getLogicalObjectKey());
        return refs;
    }

    private void putIfPresent(Map<String, Object> target, String key, String value) {
        if (StringUtils.hasText(value)) {
            target.put(key, value);
        }
    }

    private AccelerationRecommendationVO toRecommendationVo(AccelerationRecommendation recommendation) {
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

    private List<DispatchEventVO> toDispatchVos(List<DispatchEvent> events) {
        List<DispatchEventVO> result = new ArrayList<DispatchEventVO>(events.size());
        for (DispatchEvent event : events) {
            DispatchEventVO vo = new DispatchEventVO();
            vo.setDispatchEventId(event.getDispatchEventId());
            vo.setTenantId(event.getTenantId());
            vo.setRecommendationId(event.getRecommendationId());
            vo.setDispatchType(event.getDispatchType().name());
            vo.setDispatchPayloadJson(event.getDispatchPayloadJson());
            vo.setTargetEngine(event.getTargetEngine());
            vo.setTargetDatasource(event.getTargetDatasource());
            vo.setReportCode(event.getReportCode());
            vo.setLogicalObjectKey(event.getLogicalObjectKey());
            vo.setStatus(event.getStatus().name());
            vo.setPulledBy(event.getPulledBy());
            vo.setPulledAt(event.getPulledAt());
            vo.setAckedBy(event.getAckedBy());
            vo.setAckedAt(event.getAckedAt());
            vo.setFailedBy(event.getFailedBy());
            vo.setFailedAt(event.getFailedAt());
            vo.setResultMessage(event.getResultMessage());
            vo.setCreatedBy(event.getCreatedBy());
            vo.setCreatedAt(event.getCreatedAt());
            vo.setUpdatedAt(event.getUpdatedAt());
            vo.setStatusHistory(toStatusHistory(event.getStatusHistory()));
            result.add(vo);
        }
        return result;
    }

    private List<DispatchEventStatusHistoryVO> toStatusHistory(List<DispatchEventTransition> transitions) {
        List<DispatchEventStatusHistoryVO> result = new ArrayList<DispatchEventStatusHistoryVO>(transitions.size());
        for (DispatchEventTransition transition : transitions) {
            DispatchEventStatusHistoryVO vo = new DispatchEventStatusHistoryVO();
            vo.setPreviousStatus(transition.getPreviousStatus() == null ? null : transition.getPreviousStatus().name());
            vo.setCurrentStatus(transition.getCurrentStatus() == null ? null : transition.getCurrentStatus().name());
            vo.setOccurredAt(transition.getOccurredAt());
            vo.setNote(transition.getNote());
            vo.setOperator(transition.getOperator());
            result.add(vo);
        }
        return result;
    }

    private void verifyTenantAccess(String resourceTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(contextTenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "tenantId is missing from authenticated request context"
            );
        }
        if (!contextTenantId.equals(resourceTenantId)) {
            throw new AccessDeniedException("Authenticated tenant cannot access this recommendation trace");
        }
    }
}
