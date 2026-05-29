package com.company.sqloptimization.application.service;

import com.company.sqloptimization.application.controller.vo.DispatchEventStatusHistoryVO;
import com.company.sqloptimization.application.controller.vo.DispatchEventVO;
import com.company.sqloptimization.domain.dispatch.DispatchEvent;
import com.company.sqloptimization.domain.dispatch.DispatchEventTransition;
import java.util.ArrayList;
import java.util.List;

final class DispatchEventAssembler {

    DispatchEventVO toVo(DispatchEvent event) {
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
}
