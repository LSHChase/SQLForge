package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqloptimization.application.controller.dto.DispatchEventActionRequest;
import com.company.sqloptimization.application.controller.vo.DispatchCollaborationContractVO;
import com.company.sqloptimization.application.controller.vo.DispatchEventVO;
import com.company.sqloptimization.domain.dispatch.DispatchEventStatus;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.BenefitLevel;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RecommendationType;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RiskLevel;
import com.company.sqloptimization.infrastructure.repository.InMemoryAccelerationRecommendationRepository;
import com.company.sqloptimization.infrastructure.repository.InMemoryDispatchEventRepository;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DispatchEventApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldCreatePublishPullAndAckDispatchEventWithoutExecutingSql() {
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);
        InMemoryAccelerationRecommendationRepository recommendationRepository = new InMemoryAccelerationRecommendationRepository();
        InMemoryDispatchEventRepository dispatchEventRepository = new InMemoryDispatchEventRepository();
        recommendationRepository.save(recommendation("rec-001", RecommendationType.PREWARM));
        DispatchEventApplicationService service = new DispatchEventApplicationService(recommendationRepository, dispatchEventRepository);

        DispatchEventVO published = service.dispatchRecommendation("rec-001", null);
        DispatchEventVO pulled = service.markPulled(published.getDispatchEventId());
        DispatchEventActionRequest ackRequest = new DispatchEventActionRequest();
        ackRequest.setResultMessage("external loader accepted event");
        DispatchEventVO acked = service.ack(published.getDispatchEventId(), ackRequest);

        assertEquals("PUBLISHED", published.getStatus());
        assertEquals("PREWARM_SQL", published.getDispatchType());
        assertEquals(2, published.getStatusHistory().size());
        assertEquals("PULLED", pulled.getStatus());
        assertEquals("ACKED", acked.getStatus());
        assertEquals("external loader accepted event", acked.getResultMessage());
        assertEquals(1, service.listEvents(DispatchEventStatus.ACKED).size());
    }

    @Test
    void shouldRejectInvalidStateTransition() {
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);
        InMemoryAccelerationRecommendationRepository recommendationRepository = new InMemoryAccelerationRecommendationRepository();
        InMemoryDispatchEventRepository dispatchEventRepository = new InMemoryDispatchEventRepository();
        recommendationRepository.save(recommendation("rec-002", RecommendationType.CREATE_TABLE));
        DispatchEventApplicationService service = new DispatchEventApplicationService(recommendationRepository, dispatchEventRepository);

        DispatchEventVO published = service.dispatchRecommendation("rec-002", null);

        assertThrows(IllegalStateException.class, () -> service.ack(published.getDispatchEventId(), null));
    }

    @Test
    void shouldExposePullOnlyNonExecutingCollaborationContract() {
        DispatchEventApplicationService service = new DispatchEventApplicationService(
            new InMemoryAccelerationRecommendationRepository(),
            new InMemoryDispatchEventRepository()
        );

        DispatchCollaborationContractVO contract = service.getCollaborationContract();

        assertEquals("PULL_ONLY", contract.getCoordinationMode());
        assertEquals(Boolean.FALSE, contract.getSqlExecutionAllowed());
        assertEquals(Boolean.FALSE, contract.getDataLoadingAllowed());
        assertEquals(Boolean.FALSE, contract.getActiveExternalPushAllowed());
        assertEquals(Boolean.TRUE, contract.getExternalPullRequired());
    }

    private AccelerationRecommendation recommendation(String recommendationId, RecommendationType type) {
        return AccelerationRecommendation.builder()
            .recommendationId(recommendationId)
            .tenantId("tenant-a")
            .recommendationType(type)
            .recommendedSqlText("INSERT INTO agg_sales SELECT * FROM sales")
            .targetEngine("trino")
            .targetDatasource("hetu_main")
            .reportCode("RPT_SALES_DAILY")
            .logicalObjectKey("BUSINESS_VIEW:sales_daily")
            .expectedGain("Reduce repeated report runtime")
            .benefitLevel(BenefitLevel.HIGH)
            .riskLevel(RiskLevel.LOW)
            .requiresDispatch(true)
            .createdBy("operator-001")
            .createdAt(Instant.now())
            .build();
    }
}
