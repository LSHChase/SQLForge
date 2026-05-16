package com.company.sqloptimization.domain.dispatch;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DispatchEvent {

    private final String dispatchEventId;
    private final String tenantId;
    private final String recommendationId;
    private final DispatchType dispatchType;
    private final String dispatchPayloadJson;
    private final String targetEngine;
    private final String targetDatasource;
    private final String reportCode;
    private final String logicalObjectKey;
    private final String createdBy;
    private final Instant createdAt;
    private final List<DispatchEventTransition> statusHistory;

    private DispatchEventStatus status;
    private String pulledBy;
    private Instant pulledAt;
    private String ackedBy;
    private Instant ackedAt;
    private String failedBy;
    private Instant failedAt;
    private String resultMessage;
    private Instant updatedAt;

    private DispatchEvent(String dispatchEventId,
                          String tenantId,
                          String recommendationId,
                          DispatchType dispatchType,
                          String dispatchPayloadJson,
                          String targetEngine,
                          String targetDatasource,
                          String reportCode,
                          String logicalObjectKey,
                          String createdBy,
                          Instant createdAt,
                          List<DispatchEventTransition> statusHistory) {
        this.dispatchEventId = dispatchEventId;
        this.tenantId = tenantId;
        this.recommendationId = recommendationId;
        this.dispatchType = dispatchType;
        this.dispatchPayloadJson = dispatchPayloadJson;
        this.targetEngine = targetEngine;
        this.targetDatasource = targetDatasource;
        this.reportCode = reportCode;
        this.logicalObjectKey = logicalObjectKey;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.statusHistory = new ArrayList<DispatchEventTransition>();
        if (statusHistory != null) {
            this.statusHistory.addAll(statusHistory);
        }
        validateRequired();
    }

    public static DispatchEvent create(String dispatchEventId,
                                       String tenantId,
                                       String recommendationId,
                                       DispatchType dispatchType,
                                       String dispatchPayloadJson,
                                       String targetEngine,
                                       String targetDatasource,
                                       String reportCode,
                                       String logicalObjectKey,
                                       String createdBy,
                                       Instant createdAt) {
        DispatchEvent event = new DispatchEvent(
            dispatchEventId,
            tenantId,
            recommendationId,
            dispatchType,
            dispatchPayloadJson,
            targetEngine,
            targetDatasource,
            reportCode,
            logicalObjectKey,
            createdBy,
            createdAt,
            Collections.<DispatchEventTransition>emptyList()
        );
        event.transitionTo(DispatchEventStatus.CREATED, createdAt, "DISPATCH_EVENT_CREATED", createdBy);
        return event;
    }

    public static DispatchEvent restore(String dispatchEventId,
                                        String tenantId,
                                        String recommendationId,
                                        DispatchType dispatchType,
                                        String dispatchPayloadJson,
                                        String targetEngine,
                                        String targetDatasource,
                                        String reportCode,
                                        String logicalObjectKey,
                                        DispatchEventStatus status,
                                        String pulledBy,
                                        Instant pulledAt,
                                        String ackedBy,
                                        Instant ackedAt,
                                        String failedBy,
                                        Instant failedAt,
                                        String resultMessage,
                                        String createdBy,
                                        Instant createdAt,
                                        Instant updatedAt,
                                        List<DispatchEventTransition> statusHistory) {
        DispatchEvent event = new DispatchEvent(
            dispatchEventId,
            tenantId,
            recommendationId,
            dispatchType,
            dispatchPayloadJson,
            targetEngine,
            targetDatasource,
            reportCode,
            logicalObjectKey,
            createdBy,
            createdAt,
            statusHistory
        );
        event.status = status;
        event.pulledBy = pulledBy;
        event.pulledAt = pulledAt;
        event.ackedBy = ackedBy;
        event.ackedAt = ackedAt;
        event.failedBy = failedBy;
        event.failedAt = failedAt;
        event.resultMessage = resultMessage;
        event.updatedAt = updatedAt == null ? createdAt : updatedAt;
        return event;
    }

    public void publish(Instant occurredAt, String operator) {
        requireStatus(DispatchEventStatus.CREATED, "只有 CREATED 状态的分发事件可以发布");
        transitionTo(DispatchEventStatus.PUBLISHED, occurredAt, "DISPATCH_EVENT_PUBLISHED_FOR_PULL", operator);
    }

    public void markPulled(Instant occurredAt, String operator) {
        requireStatus(DispatchEventStatus.PUBLISHED, "只有 PUBLISHED 状态的分发事件可以拉取");
        this.pulledBy = operator;
        this.pulledAt = occurredAt;
        transitionTo(DispatchEventStatus.PULLED, occurredAt, "DISPATCH_EVENT_PULLED_BY_EXTERNAL_MODULE", operator);
    }

    public void ack(Instant occurredAt, String operator, String resultMessage) {
        requireStatus(DispatchEventStatus.PULLED, "只有 PULLED 状态的分发事件可以确认");
        this.ackedBy = operator;
        this.ackedAt = occurredAt;
        this.resultMessage = resultMessage;
        transitionTo(DispatchEventStatus.ACKED, occurredAt, "DISPATCH_EVENT_ACKED", operator);
    }

    public void fail(Instant occurredAt, String operator, String resultMessage) {
        requireStatus(DispatchEventStatus.PULLED, "只有 PULLED 状态的分发事件可以标记失败");
        this.failedBy = operator;
        this.failedAt = occurredAt;
        this.resultMessage = resultMessage;
        transitionTo(DispatchEventStatus.FAILED, occurredAt, "DISPATCH_EVENT_FAILED", operator);
    }

    private void transitionTo(DispatchEventStatus nextStatus, Instant occurredAt, String note, String operator) {
        DispatchEventStatus previous = this.status;
        this.status = nextStatus;
        this.updatedAt = occurredAt;
        this.statusHistory.add(new DispatchEventTransition(previous, nextStatus, occurredAt, note, operator));
    }

    private void requireStatus(DispatchEventStatus expected, String message) {
        if (status != expected) {
            throw new IllegalStateException(message + "，当前状态=" + status);
        }
    }

    private void validateRequired() {
        requireText(dispatchEventId, "dispatchEventId");
        requireText(tenantId, "tenantId");
        requireText(recommendationId, "recommendationId");
        if (dispatchType == null) {
            throw new IllegalArgumentException("dispatchType 为必填项");
        }
        requireText(dispatchPayloadJson, "dispatchPayloadJson");
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt 为必填项");
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " 为必填项");
        }
    }

    public String getDispatchEventId() { return dispatchEventId; }
    public String getTenantId() { return tenantId; }
    public String getRecommendationId() { return recommendationId; }
    public DispatchType getDispatchType() { return dispatchType; }
    public String getDispatchPayloadJson() { return dispatchPayloadJson; }
    public String getTargetEngine() { return targetEngine; }
    public String getTargetDatasource() { return targetDatasource; }
    public String getReportCode() { return reportCode; }
    public String getLogicalObjectKey() { return logicalObjectKey; }
    public DispatchEventStatus getStatus() { return status; }
    public String getPulledBy() { return pulledBy; }
    public Instant getPulledAt() { return pulledAt; }
    public String getAckedBy() { return ackedBy; }
    public Instant getAckedAt() { return ackedAt; }
    public String getFailedBy() { return failedBy; }
    public Instant getFailedAt() { return failedAt; }
    public String getResultMessage() { return resultMessage; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<DispatchEventTransition> getStatusHistory() { return Collections.unmodifiableList(statusHistory); }
}
