package com.company.governance.infrastructure.sqloptimization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.company.governance.config.GovernanceSqlOptimizationProperties;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class GovernanceSqlOptimizationHttpClientTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldInvokeSqlOptimizationRewriteRecordRoute() {
        GovernanceSqlOptimizationHttpClient client =
            new GovernanceSqlOptimizationHttpClient(new RestTemplateBuilder(), properties());
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        setRequestContext();
        server.expect(requestTo(
                "http://sql-optimization.test/api/sql-optimization/rewrite-records?historyId=history-001"
            ))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(
                "[{\"rewriteRecordId\":\"rewrite-001\",\"tenantId\":\"tenant-a\",\"historyId\":\"history-001\","
                    + "\"validationStatus\":\"DIVERGED\",\"alertStatus\":\"OPEN\"}]",
                MediaType.APPLICATION_JSON
            ));

        List<SqlOptimizationRewriteRecordResponse> records =
            client.listRewriteRecordsByHistoryId("history-001");

        assertEquals(1, records.size());
        assertEquals("rewrite-001", records.get(0).getRewriteRecordId());
        assertEquals("DIVERGED", records.get(0).getValidationStatus());
        server.verify();
    }

    @Test
    void shouldWrapSqlOptimizationRouteFailureAsBizException() {
        GovernanceSqlOptimizationHttpClient client =
            new GovernanceSqlOptimizationHttpClient(new RestTemplateBuilder(), properties());
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        setRequestContext();
        server.expect(requestTo(
                "http://sql-optimization.test/api/sql-optimization/rewrite-records?historyId=history-001"
            ))
            .andRespond(withServerError());

        BizException exception = assertThrows(
            BizException.class,
            () -> client.listRewriteRecordsByHistoryId("history-001")
        );

        assertEquals(ErrorCodeConstants.SYSTEM_AUDIT_CONTRACT_INVALID, exception.getCode());
        assertEquals("SQL 优化改写记录路由不可用", exception.getMessage());
    }

    private void setRequestContext() {
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
    }

    private GovernanceSqlOptimizationProperties properties() {
        GovernanceSqlOptimizationProperties properties = new GovernanceSqlOptimizationProperties();
        properties.setBaseUrl("http://sql-optimization.test/api/sql-optimization");
        return properties;
    }
}
