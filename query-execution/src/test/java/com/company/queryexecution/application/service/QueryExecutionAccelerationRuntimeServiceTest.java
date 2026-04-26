package com.company.queryexecution.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanApplyRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanRollbackRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanVerifyRequest;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class QueryExecutionAccelerationRuntimeServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldApplyVerifyAndRollbackApprovedBinding() {
        setRequestContext();
        QueryExecutionAccelerationRuntimeService service = new QueryExecutionAccelerationRuntimeService();
        QueryExecutionAccelerationPlanApplyRequest applyRequest = new QueryExecutionAccelerationPlanApplyRequest();
        applyRequest.setTenantId("tenant-a");
        applyRequest.setPlanId("plan-001");
        applyRequest.setSqlFingerprint("fp-001");
        applyRequest.setDatasourceType("HETU");
        applyRequest.setSelectedSuggestionTypes(Arrays.asList("PRECOMPUTE", "PARTITION"));
        applyRequest.setPlanSummary("approved plan");
        applyRequest.setPrimaryRecommendation("use approved runtime config");

        QueryExecutionAccelerationPlanResponse applyResponse = service.apply(applyRequest);

        assertEquals("APPLIED", applyResponse.getStatus());
        assertTrue(applyResponse.isActive());
        assertNotNull(service.resolveActiveBinding("tenant-a", "fp-001", "HETU"));

        QueryExecutionAccelerationPlanVerifyRequest verifyRequest = new QueryExecutionAccelerationPlanVerifyRequest();
        verifyRequest.setTenantId("tenant-a");
        verifyRequest.setPlanId("plan-001");
        verifyRequest.setSqlFingerprint("fp-001");
        QueryExecutionAccelerationPlanResponse verifyResponse = service.verify(verifyRequest);
        assertEquals("VERIFIED", verifyResponse.getStatus());
        assertTrue(verifyResponse.isActive());

        QueryExecutionAccelerationPlanRollbackRequest rollbackRequest = new QueryExecutionAccelerationPlanRollbackRequest();
        rollbackRequest.setTenantId("tenant-a");
        rollbackRequest.setPlanId("plan-001");
        rollbackRequest.setSqlFingerprint("fp-001");
        QueryExecutionAccelerationPlanResponse rollbackResponse = service.rollback(rollbackRequest);
        assertEquals("ROLLED_BACK", rollbackResponse.getStatus());
        assertFalse(rollbackResponse.isActive());
        assertNull(service.resolveActiveBinding("tenant-a", "fp-001", "HETU"));
    }

    private void setRequestContext() {
        RequestContext.set(
            "tenant-a",
            "service-user",
            Arrays.asList("SERVICE"),
            "request-001",
            "trace-001",
            "header",
            1L,
            2L
        );
    }
}
