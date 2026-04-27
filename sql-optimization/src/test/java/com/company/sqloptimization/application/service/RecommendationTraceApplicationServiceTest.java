package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqloptimization.application.controller.vo.RecommendationTraceVO;
import com.company.sqloptimization.domain.dispatch.DispatchEvent;
import com.company.sqloptimization.domain.dispatch.DispatchType;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RecommendationType;
import com.company.sqloptimization.infrastructure.repository.InMemoryAccelerationRecommendationRepository;
import com.company.sqloptimization.infrastructure.repository.InMemoryDispatchEventRepository;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RecommendationTraceApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldReturnTenantScopedRecommendationTraceReferencesAndDispatchEvents() {
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);
        InMemoryAccelerationRecommendationRepository recommendationRepository = new InMemoryAccelerationRecommendationRepository();
        InMemoryDispatchEventRepository dispatchEventRepository = new InMemoryDispatchEventRepository();
        recommendationRepository.save(recommendation("rec-001", "tenant-a"));
        dispatchEventRepository.save(dispatchEvent("dispatch-001", "tenant-a", "rec-001"));
        dispatchEventRepository.save(dispatchEvent("dispatch-other", "tenant-b", "rec-001"));
        RecommendationTraceApplicationService service = new RecommendationTraceApplicationService(
            recommendationRepository,
            dispatchEventRepository
        );

        RecommendationTraceVO trace = service.trace("rec-001");

        assertEquals("history-001", trace.getHistoryId());
        assertEquals("parse-task-001", trace.getParseTaskId());
        assertEquals("batch-001", trace.getBatchId());
        assertEquals("route-001", trace.getRouteDecisionId());
        assertEquals("alert-001", trace.getAlertId());
        assertEquals(1, trace.getDispatchEvents().size());
        assertEquals("dispatch-001", trace.getDispatchEvents().get(0).getDispatchEventId());
        assertEquals("history-001", trace.getTraceRefs().get("historyId"));
    }

    @Test
    void shouldRejectCrossTenantTraceAccess() {
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);
        InMemoryAccelerationRecommendationRepository recommendationRepository = new InMemoryAccelerationRecommendationRepository();
        InMemoryDispatchEventRepository dispatchEventRepository = new InMemoryDispatchEventRepository();
        recommendationRepository.save(recommendation("rec-002", "tenant-b"));
        RecommendationTraceApplicationService service = new RecommendationTraceApplicationService(
            recommendationRepository,
            dispatchEventRepository
        );

        assertThrows(AccessDeniedException.class, () -> service.trace("rec-002"));
    }

    private AccelerationRecommendation recommendation(String recommendationId, String tenantId) {
        return AccelerationRecommendation.builder()
            .recommendationId(recommendationId)
            .tenantId(tenantId)
            .recommendationType(RecommendationType.ACCELERATION)
            .sourceSqlId("source-sql-001")
            .historyId("history-001")
            .parseTaskId("parse-task-001")
            .batchId("batch-001")
            .routeDecisionId("route-001")
            .alertId("alert-001")
            .sqlFingerprint("fp-001")
            .recommendedSqlText("SELECT * FROM agg_sales")
            .reportCode("RPT_SALES_DAILY")
            .logicalObjectKey("BUSINESS_VIEW:sales_daily")
            .createdAt(Instant.now())
            .build();
    }

    private DispatchEvent dispatchEvent(String eventId, String tenantId, String recommendationId) {
        DispatchEvent event = DispatchEvent.create(
            eventId,
            tenantId,
            recommendationId,
            DispatchType.ACCELERATION_SQL,
            "{\"recommendationId\":\"" + recommendationId + "\"}",
            "trino",
            "hetu_main",
            "RPT_SALES_DAILY",
            "BUSINESS_VIEW:sales_daily",
            "operator-001",
            Instant.now()
        );
        event.publish(Instant.now(), "operator-001");
        return event;
    }
}
