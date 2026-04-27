package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.application.controller.dto.DispatchEventActionRequest;
import com.company.sqloptimization.application.controller.dto.DispatchRecommendationRequest;
import com.company.sqloptimization.application.controller.vo.DispatchEventStatusHistoryVO;
import com.company.sqloptimization.application.controller.vo.DispatchEventVO;
import com.company.sqloptimization.domain.dispatch.DispatchEvent;
import com.company.sqloptimization.domain.dispatch.DispatchEventStatus;
import com.company.sqloptimization.domain.dispatch.DispatchEventTransition;
import com.company.sqloptimization.domain.dispatch.DispatchType;
import com.company.sqloptimization.domain.dispatch.repository.DispatchEventRepository;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.recommendation.repository.AccelerationRecommendationRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DispatchEventApplicationService {

    private final AccelerationRecommendationRepository recommendationRepository;
    private final DispatchEventRepository dispatchEventRepository;

    public DispatchEventApplicationService(AccelerationRecommendationRepository recommendationRepository,
                                           DispatchEventRepository dispatchEventRepository) {
        this.recommendationRepository = recommendationRepository;
        this.dispatchEventRepository = dispatchEventRepository;
    }

    public DispatchEventVO dispatchRecommendation(String recommendationId, DispatchRecommendationRequest request) {
        AccelerationRecommendation recommendation = requireRecommendation(recommendationId);
        verifyTenantAccess(recommendation.getTenantId());
        Instant now = Instant.now();
        DispatchType dispatchType = request == null || request.getDispatchType() == null
            ? defaultDispatchType(recommendation)
            : request.getDispatchType();
        DispatchEvent event = DispatchEvent.create(
            UUID.randomUUID().toString(),
            recommendation.getTenantId(),
            recommendation.getRecommendationId(),
            dispatchType,
            buildPayload(recommendation, dispatchType),
            recommendation.getTargetEngine(),
            recommendation.getTargetDatasource(),
            recommendation.getReportCode(),
            recommendation.getLogicalObjectKey(),
            RequestContext.getUserId(),
            now
        );
        event.publish(now, RequestContext.getUserId());
        dispatchEventRepository.save(event);
        return toVo(event);
    }

    public List<DispatchEventVO> listEvents(DispatchEventStatus status) {
        String tenantId = requireContextTenant();
        List<DispatchEvent> events = status == null
            ? dispatchEventRepository.findByTenantId(tenantId)
            : dispatchEventRepository.findByTenantIdAndStatus(tenantId, status);
        List<DispatchEventVO> result = new ArrayList<DispatchEventVO>(events.size());
        for (DispatchEvent event : events) {
            result.add(toVo(event));
        }
        return result;
    }

    public DispatchEventVO getEvent(String dispatchEventId) {
        DispatchEvent event = requireEvent(dispatchEventId);
        verifyTenantAccess(event.getTenantId());
        return toVo(event);
    }

    public DispatchEventVO markPulled(String dispatchEventId) {
        DispatchEvent event = requireEvent(dispatchEventId);
        verifyTenantAccess(event.getTenantId());
        event.markPulled(Instant.now(), RequestContext.getUserId());
        dispatchEventRepository.save(event);
        return toVo(event);
    }

    public DispatchEventVO ack(String dispatchEventId, DispatchEventActionRequest request) {
        DispatchEvent event = requireEvent(dispatchEventId);
        verifyTenantAccess(event.getTenantId());
        event.ack(Instant.now(), RequestContext.getUserId(), trimToNull(request == null ? null : request.getResultMessage()));
        dispatchEventRepository.save(event);
        return toVo(event);
    }

    public DispatchEventVO fail(String dispatchEventId, DispatchEventActionRequest request) {
        DispatchEvent event = requireEvent(dispatchEventId);
        verifyTenantAccess(event.getTenantId());
        event.fail(Instant.now(), RequestContext.getUserId(), trimToNull(request == null ? null : request.getResultMessage()));
        dispatchEventRepository.save(event);
        return toVo(event);
    }

    private AccelerationRecommendation requireRecommendation(String recommendationId) {
        AccelerationRecommendation recommendation = recommendationRepository.findByRecommendationId(recommendationId);
        if (recommendation == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Recommendation not found: " + recommendationId
            );
        }
        return recommendation;
    }

    private DispatchEvent requireEvent(String dispatchEventId) {
        DispatchEvent event = dispatchEventRepository.findByDispatchEventId(dispatchEventId);
        if (event == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Dispatch event not found: " + dispatchEventId
            );
        }
        return event;
    }

    private DispatchType defaultDispatchType(AccelerationRecommendation recommendation) {
        switch (recommendation.getRecommendationType()) {
            case REWRITE:
                return DispatchType.REWRITE_SQL;
            case ACCELERATION:
                return DispatchType.ACCELERATION_SQL;
            case CREATE_TABLE:
                return DispatchType.CREATE_TABLE_SQL;
            case PREWARM:
                return DispatchType.PREWARM_SQL;
            case MAINTENANCE:
                return DispatchType.MAINTENANCE_SQL;
            default:
                return DispatchType.ACCELERATION_SQL;
        }
    }

    private String buildPayload(AccelerationRecommendation recommendation, DispatchType dispatchType) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("recommendationId", recommendation.getRecommendationId());
        payload.put("dispatchType", dispatchType.name());
        payload.put("sqlText", recommendation.getRecommendedSqlText());
        payload.put("targetEngine", recommendation.getTargetEngine());
        payload.put("targetDatasource", recommendation.getTargetDatasource());
        payload.put("relatedReportCode", recommendation.getReportCode());
        payload.put("relatedLogicalObject", recommendation.getLogicalObjectKey());
        payload.put("expectedEffect", recommendation.getExpectedGain());
        payload.put("recommendationType", recommendation.getRecommendationType().name());
        payload.put("riskLevel", recommendation.getRiskLevel().name());
        return JsonUtils.toJson(payload);
    }

    private DispatchEventVO toVo(DispatchEvent event) {
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
        return vo;
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
            throw new AccessDeniedException("Authenticated tenant cannot access this dispatch event");
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
