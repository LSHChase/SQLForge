package com.company.sqloptimization.infrastructure.queryexecution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingPublishRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResponse;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingStateChangeRequest;
import com.company.sqloptimization.config.OptimizationQueryExecutionProperties;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class QueryExecutionRuntimeRewriteBindingHttpClientTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldPostPublishPauseAndUnpublishWithProtectedHeaders() {
        QueryExecutionRuntimeRewriteBindingHttpClient client = createClient();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        setRequestContext();
        server.expect(requestTo("http://query-execution.test/api/query-execution/internal/rewrite-bindings/publish"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(RequestHeaderConstants.TENANT_ID, "tenant-a"))
            .andExpect(header(RequestHeaderConstants.USER_ID, "operator-001"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"rewriteRecordId\":\"rewrite-001\"")))
            .andRespond(withSuccess(activeResponse(), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://query-execution.test/api/query-execution/internal/rewrite-bindings/pause"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"runtimeBindingId\":\"rwb-001\"")))
            .andRespond(withSuccess(pausedResponse(), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://query-execution.test/api/query-execution/internal/rewrite-bindings/unpublish"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"reason\":\"operator rollback\"")))
            .andRespond(withSuccess(unpublishedResponse(), MediaType.APPLICATION_JSON));

        RuntimeRewriteBindingResponse published = client.publish(publishRequest());
        RuntimeRewriteBindingResponse paused = client.pause(stateChangeRequest("validation divergence"));
        RuntimeRewriteBindingResponse unpublished = client.unpublish(stateChangeRequest("operator rollback"));

        assertEquals("ACTIVE", published.getStatus());
        assertEquals("PAUSED", paused.getStatus());
        assertEquals("UNPUBLISHED", unpublished.getStatus());
        server.verify();
    }

    @Test
    void shouldConvertRuntimeRewriteRouteFailureToBizException() {
        QueryExecutionRuntimeRewriteBindingHttpClient client = createClient();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        setRequestContext();
        server.expect(requestTo("http://query-execution.test/api/query-execution/internal/rewrite-bindings/publish"))
            .andRespond(withServerError());

        BizException ex = assertThrows(BizException.class, () -> client.publish(publishRequest()));

        assertEquals(ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_REWRITE_FAILURE, ex.getCode());
        server.verify();
    }

    private QueryExecutionRuntimeRewriteBindingHttpClient createClient() {
        OptimizationQueryExecutionProperties properties = new OptimizationQueryExecutionProperties();
        properties.setRewriteBindingBaseUrl("http://query-execution.test/api/query-execution/internal/rewrite-bindings/");
        return new QueryExecutionRuntimeRewriteBindingHttpClient(new RestTemplateBuilder(), properties);
    }

    private RuntimeRewriteBindingPublishRequest publishRequest() {
        RuntimeRewriteBindingPublishRequest request = new RuntimeRewriteBindingPublishRequest();
        request.setTenantId("tenant-a");
        request.setRewriteRecordId("rewrite-001");
        request.setRecommendationId("recommendation-001");
        request.setSourceType("QUERY");
        request.setSourceKind("QUERY_HISTORY");
        request.setSourceId("history-001");
        request.setSqlFingerprint("fp-001");
        request.setOriginalSqlDigest("fp-001");
        request.setRecommendedSqlText("SELECT id FROM orders");
        request.setDatasourceCode("hetu_main");
        request.setPublishedBy("operator-001");
        return request;
    }

    private RuntimeRewriteBindingStateChangeRequest stateChangeRequest(String reason) {
        RuntimeRewriteBindingStateChangeRequest request = new RuntimeRewriteBindingStateChangeRequest();
        request.setTenantId("tenant-a");
        request.setRuntimeBindingId("rwb-001");
        request.setSqlFingerprint("fp-001");
        request.setOperatorId("operator-001");
        request.setReason(reason);
        return request;
    }

    private String activeResponse() {
        return "{\"tenantId\":\"tenant-a\",\"runtimeBindingId\":\"rwb-001\","
            + "\"rewriteRecordId\":\"rewrite-001\",\"sqlFingerprint\":\"fp-001\","
            + "\"status\":\"ACTIVE\",\"active\":true,\"ruleVersion\":1,"
            + "\"runtimeRuleVersion\":\"runtime-rewrite-v1\"}";
    }

    private String pausedResponse() {
        return "{\"tenantId\":\"tenant-a\",\"runtimeBindingId\":\"rwb-001\","
            + "\"rewriteRecordId\":\"rewrite-001\",\"sqlFingerprint\":\"fp-001\","
            + "\"status\":\"PAUSED\",\"active\":false,\"ruleVersion\":1,"
            + "\"runtimeRuleVersion\":\"runtime-rewrite-v1\"}";
    }

    private String unpublishedResponse() {
        return "{\"tenantId\":\"tenant-a\",\"runtimeBindingId\":\"rwb-001\","
            + "\"rewriteRecordId\":\"rewrite-001\",\"sqlFingerprint\":\"fp-001\","
            + "\"status\":\"UNPUBLISHED\",\"active\":false,\"ruleVersion\":1,"
            + "\"runtimeRuleVersion\":\"runtime-rewrite-v1\"}";
    }

    private void setRequestContext() {
        RequestContext.set(
            "tenant-a",
            "operator-001",
            Arrays.asList("TENANT_ADMIN", "OPERATOR"),
            "request-001",
            "trace-001",
            "header",
            1L,
            2L
        );
    }
}
