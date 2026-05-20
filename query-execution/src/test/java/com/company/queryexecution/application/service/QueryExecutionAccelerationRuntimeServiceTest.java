package com.company.queryexecution.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanActivationRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionAccelerationPlanPauseRequest;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class QueryExecutionAccelerationRuntimeServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldActivateAndPauseRuntimeBinding() {
        setRequestContext();
        QueryExecutionAccelerationRuntimeService service = new QueryExecutionAccelerationRuntimeService();
        QueryExecutionAccelerationPlanActivationRequest activationRequest = new QueryExecutionAccelerationPlanActivationRequest();
        activationRequest.setTenantId("tenant-a");
        activationRequest.setPlanId("plan-001");
        activationRequest.setSqlFingerprint("fp-001");
        activationRequest.setDatasourceType("HETU");
        activationRequest.setSelectedSuggestionTypes(Arrays.asList("PRECOMPUTE", "PARTITION"));
        activationRequest.setPlanSummary("activated plan");
        activationRequest.setPrimaryRecommendation("use activated runtime config");

        QueryExecutionAccelerationPlanResponse activationResponse = service.activate(activationRequest);

        assertEquals("ACTIVE", activationResponse.getStatus());
        assertTrue(activationResponse.isActive());
        assertNotNull(service.resolveActiveBinding("tenant-a", "fp-001", "HETU"));

        QueryExecutionAccelerationPlanPauseRequest pauseRequest = new QueryExecutionAccelerationPlanPauseRequest();
        pauseRequest.setTenantId("tenant-a");
        pauseRequest.setPlanId("plan-001");
        pauseRequest.setSqlFingerprint("fp-001");
        QueryExecutionAccelerationPlanResponse pauseResponse = service.pause(pauseRequest);
        assertEquals("PAUSED", pauseResponse.getStatus());
        assertFalse(pauseResponse.isActive());
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
