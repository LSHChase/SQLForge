package com.company.sqlforge.common.jdbcagent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.company.sqlforge.common.access.AccessChannel;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.openaccess.OpenAccessHttpClientProperties;
import com.company.sqlforge.common.openaccess.OpenAccessRequestContext;
import com.company.sqlforge.common.openaccess.SqlForgeAccessAuditClient;
import com.company.sqlforge.common.openaccess.SqlForgeQueryExecutionClient;
import com.company.sqlforge.common.openaccess.SqlForgeQueryStatus;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class SqlForgeJdbcAgentGovernedExecuteTest {

    @Test
    void shouldExecuteThroughPlatformWithoutDirectTakeover() {
        SqlForgeQueryExecutionClient queryClient = queryExecutionClient();
        RestTemplate queryRestTemplate = (RestTemplate) ReflectionTestUtils.getField(queryClient, "restTemplate");
        MockRestServiceServer queryServer = MockRestServiceServer.bindTo(queryRestTemplate).build();
        queryServer.expect(requestTo("http://sqlforge.test/api/query-execution/queries/execute"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("X-Access-Channel", "JDBC_AGENT"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"tenantId\":\"tenant-a\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"sqlText\":\"SELECT * FROM sales WHERE query_date = '2026-04-26'\"")))
            .andRespond(withSuccess(
                "{\"status\":\"SUCCESS\",\"rows\":[{\"engine\":\"HETU\"}],"
                    + "\"metadata\":{\"targetEngine\":\"HETU\",\"executionMode\":\"GOVERNED\",\"elapsedMs\":21,"
                    + "\"attemptedModes\":[\"API\"],\"rowCount\":1},"
                    + "\"degraded\":false,\"sqlFingerprint\":\"fp-governed-001\","
                    + "\"contractStage\":\"LONG_TERM_BASELINE\",\"implementationStage\":\"GOVERNED_EXECUTE_BASELINE\"}",
                MediaType.APPLICATION_JSON
            ));

        SqlForgeAccessAuditClient auditClient = auditClient();
        RestTemplate auditRestTemplate = (RestTemplate) ReflectionTestUtils.getField(auditClient, "restTemplate");
        MockRestServiceServer auditServer = MockRestServiceServer.bindTo(auditRestTemplate).build();
        auditServer.expect(requestTo("http://sqlforge.test/api/governance/internal/audit/write"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"operationCode\":\"JDBC_AGENT_GOVERNED_EXECUTE\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\\\"resultStatus\\\":\\\"SUCCESS\\\"")))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        JdbcAgentProperties properties = baseProperties();
        properties.setAgentMode(JdbcAgentMode.GOVERNED_EXECUTE);
        SqlForgeJdbcAgent agent = new SqlForgeJdbcAgent(
            properties,
            queryClient,
            auditClient,
            new NoopJdbcAgentRewriteRuleProvider()
        );

        JdbcAgentExecutionResult<String> result = agent.execute(requestContext(), sampleRequest(), null);

        assertNotNull(result.getGovernedResponse());
        assertEquals(SqlForgeQueryStatus.SUCCESS, result.getGovernedResponse().getStatus());
        assertEquals("GOVERNED", result.getGovernedResponse().getMetadata().getExecutionMode());
        assertEquals("GOVERNED_EXECUTE", result.getMetadata().getEffectiveMode());
        assertTrue(result.getMetadata().isAuditReported());
        queryServer.verify();
        auditServer.verify();
    }

    @Test
    void shouldFallbackToDirectJdbcWhenGovernedExecuteFails() {
        SqlForgeQueryExecutionClient queryClient = queryExecutionClient();
        RestTemplate queryRestTemplate = (RestTemplate) ReflectionTestUtils.getField(queryClient, "restTemplate");
        MockRestServiceServer queryServer = MockRestServiceServer.bindTo(queryRestTemplate).build();
        queryServer.expect(requestTo("http://sqlforge.test/api/query-execution/queries/execute"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"tenantId\":\"tenant-a\"")))
            .andRespond(withServerError());

        SqlForgeAccessAuditClient auditClient = auditClient();
        RestTemplate auditRestTemplate = (RestTemplate) ReflectionTestUtils.getField(auditClient, "restTemplate");
        MockRestServiceServer auditServer = MockRestServiceServer.bindTo(auditRestTemplate).build();
        auditServer.expect(requestTo("http://sqlforge.test/api/governance/internal/audit/write"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\\\"fallbackApplied\\\":true")))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        JdbcAgentProperties properties = baseProperties();
        properties.setAgentMode(JdbcAgentMode.GOVERNED_EXECUTE);
        properties.setFallbackStrategy(JdbcAgentFallbackStrategy.DIRECT_JDBC);
        SqlForgeJdbcAgent agent = new SqlForgeJdbcAgent(
            properties,
            queryClient,
            auditClient,
            new NoopJdbcAgentRewriteRuleProvider()
        );

        JdbcAgentExecutionResult<String> result = agent.execute(
            requestContext(),
            sampleRequest(),
            new JdbcAgentDirectExecutor<String>() {
                @Override
                public JdbcAgentDirectResult<String> execute(JdbcAgentDirectExecution execution) {
                    return JdbcAgentDirectResult.success("direct-fallback", Integer.valueOf(1));
                }
            }
        );

        assertEquals("direct-fallback", result.getDirectResult().getPayload());
        assertTrue(result.getMetadata().isFallbackApplied());
        assertTrue(result.getMetadata().getPlatformFailureReason().contains("SQLForge query-execution route is unavailable"));
        queryServer.verify();
        auditServer.verify();
    }

    @Test
    void shouldFailClosedWhenGovernedExecuteFallbackIsDisabled() {
        SqlForgeQueryExecutionClient queryClient = queryExecutionClient();
        RestTemplate queryRestTemplate = (RestTemplate) ReflectionTestUtils.getField(queryClient, "restTemplate");
        MockRestServiceServer queryServer = MockRestServiceServer.bindTo(queryRestTemplate).build();
        queryServer.expect(requestTo("http://sqlforge.test/api/query-execution/queries/execute"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withServerError());

        SqlForgeAccessAuditClient auditClient = auditClient();
        RestTemplate auditRestTemplate = (RestTemplate) ReflectionTestUtils.getField(auditClient, "restTemplate");
        MockRestServiceServer auditServer = MockRestServiceServer.bindTo(auditRestTemplate).build();
        auditServer.expect(requestTo("http://sqlforge.test/api/governance/internal/audit/write"))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        JdbcAgentProperties properties = baseProperties();
        properties.setAgentMode(JdbcAgentMode.GOVERNED_EXECUTE);
        properties.setFallbackStrategy(JdbcAgentFallbackStrategy.FAIL_CLOSED);
        SqlForgeJdbcAgent agent = new SqlForgeJdbcAgent(
            properties,
            queryClient,
            auditClient,
            new NoopJdbcAgentRewriteRuleProvider()
        );

        BizException ex = assertThrows(
            BizException.class,
            () -> agent.execute(requestContext(), sampleRequest(), null)
        );

        assertTrue(ex.getMessage().contains("SQLForge query-execution route is unavailable"));
        queryServer.verify();
        auditServer.verify();
    }

    private JdbcAgentProperties baseProperties() {
        JdbcAgentProperties properties = new JdbcAgentProperties();
        properties.setApiBaseUrl("http://sqlforge.test");
        return properties;
    }

    private SqlForgeQueryExecutionClient queryExecutionClient() {
        return new SqlForgeQueryExecutionClient(new RestTemplateBuilder(), baseHttpProperties());
    }

    private SqlForgeAccessAuditClient auditClient() {
        return new SqlForgeAccessAuditClient(new RestTemplateBuilder(), baseHttpProperties());
    }

    private OpenAccessHttpClientProperties baseHttpProperties() {
        OpenAccessHttpClientProperties properties = new OpenAccessHttpClientProperties();
        properties.setBaseUrl("http://sqlforge.test");
        properties.setMaxRetries(0);
        return properties;
    }

    private OpenAccessRequestContext requestContext() {
        return new OpenAccessRequestContext(
            "tenant-a",
            "agent-user",
            Arrays.asList("TENANT_ADMIN", "ANALYST"),
            "request-002",
            "trace-002",
            "header",
            1713700000000L,
            2713700000000L,
            "10.0.0.8",
            "SQLForge-JDBC-Agent-Governed-Test",
            AccessChannel.JDBC_AGENT
        );
    }

    private JdbcAgentSqlRequest sampleRequest() {
        JdbcAgentSqlRequest request = new JdbcAgentSqlRequest();
        request.setTenantId("tenant-a");
        request.setDatasourceType(DataSourceTypeEnum.HETU);
        request.setDatasourceCode("hetu_main");
        request.setSqlText(
            "--report_code=RPT_SALES_DAILY\n"
                + "--stage=PROD\n"
                + "SELECT * FROM sales WHERE query_date = '2026-04-26'"
        );
        request.setTemplateSql("SELECT * FROM sales WHERE query_date = ?");
        request.setBoundSqlText("SELECT * FROM sales WHERE query_date = '2026-04-26'");
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("query_date", "2026-04-26");
        request.setParameterSnapshot(params);
        return request;
    }
}
