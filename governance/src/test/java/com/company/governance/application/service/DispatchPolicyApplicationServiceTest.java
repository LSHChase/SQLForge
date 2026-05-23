package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.company.governance.application.controller.dto.DispatchPolicyUpsertRequest;
import com.company.governance.application.controller.vo.DispatchPolicyVO;
import com.company.governance.infrastructure.repository.InMemoryDispatchPolicyRepository;
import com.company.sqlforge.common.context.RequestContext;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DispatchPolicyApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldCreateAndListDispatchPolicies() {
        RequestContext.set("tenant-a", "admin-011", "request-041", "trace-041", "header", 1L, 2L);
        DispatchPolicyApplicationService service = new DispatchPolicyApplicationService(new InMemoryDispatchPolicyRepository());

        DispatchPolicyUpsertRequest request = new DispatchPolicyUpsertRequest();
        request.setTenantId("tenant-a");
        request.setPolicyName("dispatch-main");
        request.setTargetEngine("hetu");
        request.setTargetDatasource("hetu_main");
        request.setEnabled(Boolean.TRUE);
        DispatchPolicyVO created = service.create(request);
        List<DispatchPolicyVO> listed = service.list("tenant-a");

        assertEquals("PULL_ONLY", created.getDispatchType());
        assertEquals("SIMULATED_ACTIVE", created.getEnforcementStatus());
        assertEquals(1, listed.size());
        assertEquals("EXTERNAL_MODULE_REQUIRED", listed.get(0).getExecutionBoundary());
    }
}
