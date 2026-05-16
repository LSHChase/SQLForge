package com.company.governance.infrastructure.benchmarkengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.company.governance.config.GovernanceBenchmarkEngineProperties;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactOperationRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkArtifactOperationResponse;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class GovernanceBenchmarkEngineHttpClientTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldInvokeBenchmarkEngineArtifactOperationRoute() {
        GovernanceBenchmarkEngineHttpClient client =
            new GovernanceBenchmarkEngineHttpClient(new RestTemplateBuilder(), properties());
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        RequestContext.set(
            "tenant-a",
            "tenant-admin-001",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-001",
            "header",
            1L,
            System.currentTimeMillis() + 60000L
        );
        server.expect(requestTo("http://benchmark-engine.test/api/benchmark-engine/internal/artifact-operations"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(
                "{\"tenantId\":\"tenant-a\",\"reportId\":\"report-001\",\"artifactKey\":\"json-export\","
                    + "\"operationType\":\"RECOVER_ARTIFACT\",\"operationStatus\":\"RECOVERY_COMPLETED\"}",
                MediaType.APPLICATION_JSON
            ));

        GovernanceBenchmarkArtifactOperationRequest request = new GovernanceBenchmarkArtifactOperationRequest();
        request.setTenantId("tenant-a");
        request.setReportId("report-001");
        request.setArtifactKey("json-export");
        request.setOperationType("RECOVER_ARTIFACT");
        GovernanceBenchmarkArtifactOperationResponse response = client.operateArtifact(request);

        assertEquals("RECOVERY_COMPLETED", response.getOperationStatus());
        server.verify();
    }

    @Test
    void shouldWrapRouteFailureAsBizException() {
        GovernanceBenchmarkEngineHttpClient client =
            new GovernanceBenchmarkEngineHttpClient(new RestTemplateBuilder(), properties());
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        RequestContext.set(
            "tenant-a",
            "tenant-admin-001",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-001",
            "header",
            1L,
            System.currentTimeMillis() + 60000L
        );
        server.expect(requestTo("http://benchmark-engine.test/api/benchmark-engine/internal/artifact-operations"))
            .andRespond(withServerError());

        GovernanceBenchmarkArtifactOperationRequest request = new GovernanceBenchmarkArtifactOperationRequest();
        request.setTenantId("tenant-a");
        request.setReportId("report-001");
        request.setArtifactKey("json-export");
        request.setOperationType("RECOVER_ARTIFACT");
        BizException exception = assertThrows(BizException.class, () -> client.operateArtifact(request));

        assertEquals(ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID, exception.getCode());
        assertEquals("压测引擎产物操作路由不可用", exception.getMessage());
    }

    private GovernanceBenchmarkEngineProperties properties() {
        GovernanceBenchmarkEngineProperties properties = new GovernanceBenchmarkEngineProperties();
        properties.setBaseUrl("http://benchmark-engine.test/api/benchmark-engine/internal");
        return properties;
    }
}
