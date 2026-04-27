package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqloptimization.application.controller.dto.AccelerationRecommendationCreateRequest;
import com.company.sqloptimization.application.controller.vo.AccelerationRecommendationVO;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.BenefitLevel;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RecommendationStatus;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RecommendationType;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation.RiskLevel;
import com.company.sqloptimization.infrastructure.repository.InMemoryAccelerationRecommendationRepository;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AccelerationRecommendationApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldCreateReadonlyRecommendationWithBenefitAndRiskModel() {
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);
        AccelerationRecommendationApplicationService service =
            new AccelerationRecommendationApplicationService(new InMemoryAccelerationRecommendationRepository());

        AccelerationRecommendationCreateRequest request = new AccelerationRecommendationCreateRequest();
        request.setTenantId("tenant-a");
        request.setRecommendationType(RecommendationType.CREATE_TABLE);
        request.setSourceSqlId("parse-item-001");
        request.setHistoryId("history-001");
        request.setParseTaskId("parse-task-001");
        request.setBatchId("batch-001");
        request.setRouteDecisionId("route-001");
        request.setAlertId("alert-001");
        request.setSqlFingerprint("fp-001");
        request.setRecommendedSqlText("CREATE TABLE agg_sales AS SELECT * FROM sales");
        request.setTargetEngine("trino");
        request.setTargetDatasource("hetu_main");
        request.setReportCode("RPT_SALES_DAILY");
        request.setLogicalObjectKey("BUSINESS_VIEW:sales_daily");
        request.setExpectedGain("Reduce repeated report runtime by precomputing daily sales");
        request.setBenefitLevel(BenefitLevel.HIGH);
        request.setRiskLevel(RiskLevel.MEDIUM);
        request.setRequiresDispatch(Boolean.TRUE);

        AccelerationRecommendationVO created = service.createRecommendation(request);
        List<AccelerationRecommendationVO> list = service.listRecommendations();
        AccelerationRecommendationVO detail = service.getRecommendation(created.getRecommendationId());

        assertEquals("CREATE_TABLE", created.getRecommendationType());
        assertEquals("HIGH", created.getBenefitLevel());
        assertEquals("MEDIUM", created.getRiskLevel());
        assertEquals("history-001", created.getHistoryId());
        assertEquals("parse-task-001", created.getParseTaskId());
        assertEquals("batch-001", created.getBatchId());
        assertEquals("route-001", created.getRouteDecisionId());
        assertEquals("alert-001", created.getAlertId());
        assertEquals(Boolean.TRUE, created.getRequiresDispatch());
        assertEquals("RECOMMENDED", created.getStatus());
        assertEquals(1, list.size());
        assertEquals(created.getRecommendationId(), detail.getRecommendationId());
        assertFalse(enumContainsExecuted(), "RecommendationStatus must not expose an executed state");
    }

    @Test
    void shouldRejectCrossTenantRecommendationCreation() {
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);
        AccelerationRecommendationApplicationService service =
            new AccelerationRecommendationApplicationService(new InMemoryAccelerationRecommendationRepository());
        AccelerationRecommendationCreateRequest request = new AccelerationRecommendationCreateRequest();
        request.setTenantId("tenant-b");
        request.setRecommendationType(RecommendationType.REWRITE);
        request.setRecommendedSqlText("SELECT 1");

        assertThrows(AccessDeniedException.class, () -> service.createRecommendation(request));
    }

    private boolean enumContainsExecuted() {
        for (RecommendationStatus status : RecommendationStatus.values()) {
            if ("EXECUTED".equals(status.name())) {
                return true;
            }
        }
        return false;
    }
}
