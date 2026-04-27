package com.company.sqlforge.common.openaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.company.sqlforge.common.access.AccessChannel;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class SqlForgeJavaSdkClientTest {

    @Test
    void shouldRetryQueryExecutionAndWriteSdkAudit() {
        OpenAccessHttpClientProperties properties = new OpenAccessHttpClientProperties();
        properties.setBaseUrl("http://sqlforge.test");
        properties.setMaxRetries(1);
        properties.setRetryBackoffMs(0L);

        SqlForgeQueryExecutionClient queryExecutionClient =
            new SqlForgeQueryExecutionClient(new RestTemplateBuilder(), properties);
        RestTemplate queryRestTemplate = (RestTemplate) ReflectionTestUtils.getField(queryExecutionClient, "restTemplate");
        MockRestServiceServer queryServer = MockRestServiceServer.bindTo(queryRestTemplate).build();
        queryServer.expect(requestTo("http://sqlforge.test/api/query-execution/queries/execute"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withServerError());
        queryServer.expect(requestTo("http://sqlforge.test/api/query-execution/queries/execute"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("X-Access-Channel", "SDK"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"datasourceType\":\"HETU\"")))
            .andRespond(withSuccess(
                "{\"status\":\"SUCCESS\",\"rows\":[{\"engine\":\"HETU\"}],"
                    + "\"metadata\":{\"targetEngine\":\"HETU\",\"executionMode\":\"CLIENT\",\"elapsedMs\":18,"
                    + "\"attemptedModes\":[\"CLIENT\"],\"rowCount\":1},"
                    + "\"degraded\":false,\"sqlFingerprint\":\"fp-001\",\"contractStage\":\"LONG_TERM_BASELINE\","
                    + "\"implementationStage\":\"HETU_REAL_INTEGRATION\"}",
                MediaType.APPLICATION_JSON
            ));

        SqlForgeAccessAuditClient accessAuditClient = new SqlForgeAccessAuditClient(new RestTemplateBuilder(), properties);
        RestTemplate auditRestTemplate = (RestTemplate) ReflectionTestUtils.getField(accessAuditClient, "restTemplate");
        MockRestServiceServer auditServer = MockRestServiceServer.bindTo(auditRestTemplate).build();
        auditServer.expect(requestTo("http://sqlforge.test/api/governance/internal/audit/write"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("X-Access-Channel", "SDK"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"serviceCode\":\"JAVA_SDK\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"resultStatus\":\"SUCCESS\"")))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        SqlForgeJavaSdkClient sdkClient = new SqlForgeJavaSdkClient(queryExecutionClient, accessAuditClient);
        SqlForgeQueryResponse response = sdkClient.execute(requestContext(), sampleRequest());

        assertEquals(SqlForgeQueryStatus.SUCCESS, response.getStatus());
        assertEquals("CLIENT", response.getMetadata().getExecutionMode());
        queryServer.verify();
        auditServer.verify();
    }

    private OpenAccessRequestContext requestContext() {
        return new OpenAccessRequestContext(
            "tenant-a",
            "sdk-user",
            Arrays.asList("TENANT_ADMIN", "ANALYST"),
            "request-002",
            "trace-002",
            "header",
            1713700000000L,
            2713700000000L,
            "10.0.0.8",
            "SQLForge-Java-SDK-Test",
            AccessChannel.SDK
        );
    }

    private SqlForgeQueryRequest sampleRequest() {
        SqlForgeQueryRequest request = new SqlForgeQueryRequest();
        request.setTenantId("tenant-a");
        request.setDatasourceType(DataSourceTypeEnum.HETU);
        request.setSqlText("SELECT 1");
        request.setAccelerationPreference(SqlForgeAccelerationPreference.NONE);
        request.setFaultToleranceStrategy(SqlForgeFaultToleranceStrategy.FAIL_FAST);
        return request;
    }
}
