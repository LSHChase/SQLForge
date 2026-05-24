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
import com.company.sqlforge.common.queryexecution.QueryExecutionMaterializedViewCreateRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionMaterializedViewCreateResponse;
import com.company.sqloptimization.config.OptimizationQueryExecutionProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class QueryExecutionMaterializedViewHttpClientTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldPostMaterializedViewCreateWithProtectedHeaders() {
        QueryExecutionMaterializedViewHttpClient client = createClient();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        setRequestContext();
        server.expect(requestTo("http://query-execution.test/api/query-execution/internal/materialized-views/create"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(RequestHeaderConstants.TENANT_ID, "tenant-a"))
            .andExpect(header(RequestHeaderConstants.USER_ID, "user-001"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"recommendationId\":\"rec-001\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"mvName\":\"mv_orders_customer\"")))
            .andRespond(withSuccess(successResponse(), MediaType.APPLICATION_JSON));

        QueryExecutionMaterializedViewCreateResponse response = client.create(createRequest());

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("mv_orders_customer", response.getMvName());
        server.verify();
    }

    @Test
    void shouldConvertMaterializedViewRouteFailureToBizException() {
        QueryExecutionMaterializedViewHttpClient client = createClient();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        setRequestContext();
        server.expect(requestTo("http://query-execution.test/api/query-execution/internal/materialized-views/create"))
            .andRespond(withServerError());

        BizException ex = assertThrows(BizException.class, () -> client.create(createRequest()));

        assertEquals(ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_ACCELERATION_PLAN_APPLY_FAILURE, ex.getCode());
        server.verify();
    }

    private QueryExecutionMaterializedViewHttpClient createClient() {
        OptimizationQueryExecutionProperties properties = new OptimizationQueryExecutionProperties();
        properties.setMaterializedViewBaseUrl(
            "http://query-execution.test/api/query-execution/internal/materialized-views/"
        );
        return new QueryExecutionMaterializedViewHttpClient(new RestTemplateBuilder(), properties);
    }

    private QueryExecutionMaterializedViewCreateRequest createRequest() {
        QueryExecutionMaterializedViewCreateRequest request = new QueryExecutionMaterializedViewCreateRequest();
        request.setTenantId("tenant-a");
        request.setRecommendationId("rec-001");
        request.setRewriteRecordId("rewrite-001");
        request.setMvName("mv_orders_customer");
        request.setTargetEngine("HETU");
        request.setTargetDatasource("hetu_main");
        request.setDdlSql("CREATE MATERIALIZED VIEW mv_orders_customer AS SELECT 1");
        request.setRefreshSql("REFRESH MATERIALIZED VIEW mv_orders_customer");
        request.setReason("manual consent");
        return request;
    }

    private String successResponse() {
        return "{\"recommendationId\":\"rec-001\",\"rewriteRecordId\":\"rewrite-001\","
            + "\"mvName\":\"mv_orders_customer\",\"targetEngine\":\"HETU\","
            + "\"targetDatasource\":\"hetu_main\",\"status\":\"SUCCESS\","
            + "\"ddlStatus\":\"SUCCESS\",\"refreshStatus\":\"SUCCESS\"}";
    }

    private void setRequestContext() {
        RequestContext.set(
            "tenant-a",
            "user-001",
            "request-001",
            "trace-001",
            "header",
            1L,
            2L
        );
    }
}
