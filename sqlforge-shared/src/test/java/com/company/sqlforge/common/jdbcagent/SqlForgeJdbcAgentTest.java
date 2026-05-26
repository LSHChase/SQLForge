package com.company.sqlforge.common.jdbcagent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.company.sqlforge.common.access.AccessChannel;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.openaccess.OpenAccessHttpClientProperties;
import com.company.sqlforge.common.openaccess.OpenAccessRequestContext;
import com.company.sqlforge.common.openaccess.SqlForgeAccessAuditClient;
import com.company.sqlforge.common.openaccess.SqlForgeQueryExecutionClient;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class SqlForgeJdbcAgentTest {

    @Test
    void shouldObserveWithoutTakingOverExecution() {
        SqlForgeAccessAuditClient auditClient = auditClient();
        RestTemplate auditRestTemplate = (RestTemplate) ReflectionTestUtils.getField(auditClient, "restTemplate");
        MockRestServiceServer auditServer = MockRestServiceServer.bindTo(auditRestTemplate).build();
        auditServer.expect(requestTo("http://sqlforge.test/api/governance/internal/audit/write"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"accessChannel\":\"JDBC_AGENT\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\\\"report_code\\\":\\\"RPT_SALES_DAILY\\\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\\\"queryDateStatus\\\":\\\"RESOLVED\\\"")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SELECT * FROM sales"))))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        JdbcAgentProperties properties = baseProperties();
        properties.setAgentMode(JdbcAgentMode.OBSERVE);
        SqlForgeJdbcAgent agent = new SqlForgeJdbcAgent(
            properties,
            queryExecutionClient(),
            auditClient,
            new NoopJdbcAgentRewriteRuleProvider()
        );

        JdbcAgentExecutionResult<String> result = agent.execute(
            requestContext(AccessChannel.JDBC_AGENT),
            sampleRequest(),
            new JdbcAgentDirectExecutor<String>() {
                @Override
                public JdbcAgentDirectResult<String> execute(JdbcAgentDirectExecution execution) {
                    assertEquals("SELECT * FROM sales WHERE query_date = '2026-04-26'", execution.getSqlText());
                    return JdbcAgentDirectResult.success("local-ok", Integer.valueOf(1));
                }
            }
        );

        assertEquals("local-ok", result.getDirectResult().getPayload());
        assertEquals("OBSERVE", result.getMetadata().getEffectiveMode());
        assertTrue(result.getMetadata().isAuditReported());
        assertEquals("RPT_SALES_DAILY", result.getMetadata().getObservation().getCommentContext().get("report_code"));
        auditServer.verify();
    }

    @Test
    void shouldRewriteBiViewCatalogBeforeObserveDirectExecution() {
        SqlForgeAccessAuditClient auditClient = auditClient();
        RestTemplate auditRestTemplate = (RestTemplate) ReflectionTestUtils.getField(auditClient, "restTemplate");
        MockRestServiceServer auditServer = MockRestServiceServer.bindTo(auditRestTemplate).build();
        auditServer.expect(requestTo("http://sqlforge.test/api/governance/internal/audit/write"))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        JdbcAgentProperties properties = baseProperties();
        properties.setAgentMode(JdbcAgentMode.OBSERVE);
        SqlForgeJdbcAgent agent = new SqlForgeJdbcAgent(
            properties,
            queryExecutionClient(),
            auditClient,
            new NoopJdbcAgentRewriteRuleProvider()
        );
        JdbcAgentSqlRequest request = sampleRequest(
            "SELECT * FROM BI_SALES_V.orders WHERE query_date = '2026-04-26'"
        );
        request.setTemplateSql("SELECT * FROM BI_SALES_V.orders WHERE query_date = ?");
        request.setBoundSqlText("SELECT * FROM BI_SALES_V.orders WHERE query_date = '2026-04-26'");

        JdbcAgentExecutionResult<String> result = agent.execute(
            requestContext(AccessChannel.JDBC_AGENT),
            request,
            new JdbcAgentDirectExecutor<String>() {
                @Override
                public JdbcAgentDirectResult<String> execute(JdbcAgentDirectExecution execution) {
                    assertFalse(execution.isRewritten());
                    assertEquals(
                        "SELECT * FROM BI_SALES_HETU.orders WHERE query_date = '2026-04-26'",
                        execution.getSqlText()
                    );
                    return JdbcAgentDirectResult.success("catalog-rewritten-ok", Integer.valueOf(1));
                }
            }
        );

        assertEquals("catalog-rewritten-ok", result.getDirectResult().getPayload());
        assertEquals(
            "SELECT * FROM BI_SALES_V.orders WHERE query_date = '2026-04-26'",
            result.getMetadata().getObservation().getOriginalSql()
        );
        assertEquals(
            "SELECT * FROM BI_SALES_HETU.orders WHERE query_date = '2026-04-26'",
            result.getMetadata().getObservation().getBoundSqlText()
        );
        auditServer.verify();
    }

    @Test
    void shouldRewriteBiViewCatalogBeforeGovernedExecuteRequest() {
        SqlForgeQueryExecutionClient queryClient = queryExecutionClient();
        RestTemplate queryRestTemplate = (RestTemplate) ReflectionTestUtils.getField(queryClient, "restTemplate");
        MockRestServiceServer queryServer = MockRestServiceServer.bindTo(queryRestTemplate).build();
        queryServer.expect(requestTo("http://sqlforge.test/api/query-execution/queries/execute"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("BI_SALES_HETU.orders")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("BI_SALES_V.orders"))))
            .andRespond(withSuccess(
                "{\"status\":\"SUCCESS\",\"rows\":[],\"metadata\":{\"targetEngine\":\"HETU\","
                    + "\"actualSql\":\"SELECT * FROM BI_SALES_HETU.orders\",\"elapsedMs\":1,"
                    + "\"scannedRows\":0,\"cacheHit\":false,\"accelerationApplied\":false,"
                    + "\"executionMode\":\"JDBC\",\"rowCount\":0},\"degraded\":false,"
                    + "\"sqlFingerprint\":\"fp-001\"}",
                MediaType.APPLICATION_JSON
            ));
        SqlForgeAccessAuditClient auditClient = auditClient();
        RestTemplate auditRestTemplate = (RestTemplate) ReflectionTestUtils.getField(auditClient, "restTemplate");
        MockRestServiceServer auditServer = MockRestServiceServer.bindTo(auditRestTemplate).build();
        auditServer.expect(requestTo("http://sqlforge.test/api/governance/internal/audit/write"))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        JdbcAgentProperties properties = baseProperties();
        properties.setAgentMode(JdbcAgentMode.GOVERNED_EXECUTE);
        SqlForgeJdbcAgent agent = new SqlForgeJdbcAgent(
            properties,
            queryClient,
            auditClient,
            new NoopJdbcAgentRewriteRuleProvider()
        );
        JdbcAgentSqlRequest request = sampleRequest("SELECT * FROM BI_SALES_V.orders");
        request.setBoundSqlText("SELECT * FROM BI_SALES_V.orders");

        JdbcAgentExecutionResult<String> result = agent.execute(
            requestContext(AccessChannel.JDBC_AGENT),
            request,
            null
        );

        assertEquals("GOVERNED_EXECUTE", result.getMetadata().getEffectiveMode());
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
            requestContext(AccessChannel.JDBC_AGENT),
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
        assertTrue(result.getMetadata().getPlatformFailureReason().contains("SQLForge 查询执行路由不可用"));
        queryServer.verify();
        auditServer.verify();
    }

    @Test
    void shouldRewriteSqlBeforeDirectExecution() {
        SqlForgeAccessAuditClient auditClient = auditClient();
        RestTemplate auditRestTemplate = (RestTemplate) ReflectionTestUtils.getField(auditClient, "restTemplate");
        MockRestServiceServer auditServer = MockRestServiceServer.bindTo(auditRestTemplate).build();
        auditServer.expect(requestTo("http://sqlforge.test/api/governance/internal/audit/write"))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        JdbcAgentProperties properties = baseProperties();
        properties.setAgentMode(JdbcAgentMode.LOCAL_REWRITE_DIRECT_JDBC);
        properties.setRewriteEnabled(true);
        properties.setRouteEnabled(true);
        SqlForgeJdbcAgent agent = new SqlForgeJdbcAgent(
            properties,
            queryExecutionClient(),
            auditClient,
            new JdbcAgentRewriteRuleProvider() {
                @Override
                public JdbcAgentRewriteDecision resolve(JdbcAgentObservation observation, JdbcAgentProperties ignored) {
                    return new JdbcAgentRewriteDecision(
                        true,
                        "SELECT id FROM sales_daily WHERE query_date = '2026-04-26'",
                        "hetu-main",
                        "STATIC_RULE_HIT"
                    );
                }
            }
        );

        JdbcAgentExecutionResult<String> result = agent.execute(
            requestContext(AccessChannel.JDBC_AGENT),
            sampleRequest(),
            new JdbcAgentDirectExecutor<String>() {
                @Override
                public JdbcAgentDirectResult<String> execute(JdbcAgentDirectExecution execution) {
                    assertTrue(execution.isRewritten());
                    assertEquals("hetu-main", execution.getRouteHint());
                    assertEquals("SELECT id FROM sales_daily WHERE query_date = '2026-04-26'", execution.getSqlText());
                    return JdbcAgentDirectResult.success("rewritten-ok", Integer.valueOf(1));
                }
            }
        );

        assertEquals("rewritten-ok", result.getDirectResult().getPayload());
        assertTrue(result.getMetadata().isRewriteApplied());
        assertEquals("STATIC_RULE_HIT", result.getMetadata().getRewriteEvidence());
        auditServer.verify();
    }

    @Test
    void shouldBypassOriginalSqlWhenLightParseTimesOut() {
        SqlForgeAccessAuditClient auditClient = auditClient();
        RestTemplate auditRestTemplate = (RestTemplate) ReflectionTestUtils.getField(auditClient, "restTemplate");
        MockRestServiceServer auditServer = MockRestServiceServer.bindTo(auditRestTemplate).build();
        auditServer.expect(requestTo("http://sqlforge.test/api/governance/internal/audit/write"))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        JdbcAgentProperties properties = baseProperties();
        properties.setAgentMode(JdbcAgentMode.LOCAL_REWRITE_DIRECT_JDBC);
        properties.setRewriteEnabled(true);
        properties.setFallbackStrategy(JdbcAgentFallbackStrategy.ORIGINAL_SQL);
        properties.setLightParseTimeoutMs(5L);
        SqlForgeJdbcAgent agent = new SqlForgeJdbcAgent(
            properties,
            queryExecutionClient(),
            auditClient,
            new JdbcAgentRewriteRuleProvider() {
                @Override
                public JdbcAgentRewriteDecision resolve(JdbcAgentObservation observation, JdbcAgentProperties ignored)
                    throws Exception {
                    Thread.sleep(50L);
                    return new JdbcAgentRewriteDecision(true, "SELECT 2", "late-route", "LATE_RULE");
                }
            }
        );

        JdbcAgentExecutionResult<String> result = agent.execute(
            requestContext(AccessChannel.JDBC_AGENT),
            sampleRequest(),
            new JdbcAgentDirectExecutor<String>() {
                @Override
                public JdbcAgentDirectResult<String> execute(JdbcAgentDirectExecution execution) {
                    assertFalse(execution.isRewritten());
                    assertEquals("SELECT * FROM sales WHERE query_date = '2026-04-26'", execution.getSqlText());
                    return JdbcAgentDirectResult.success("timeout-bypass", Integer.valueOf(1));
                }
            }
        );

        assertEquals("timeout-bypass", result.getDirectResult().getPayload());
        assertEquals("BYPASSED_TIMEOUT", result.getMetadata().getLightParseStatus());
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

    private OpenAccessRequestContext requestContext(AccessChannel accessChannel) {
        return new OpenAccessRequestContext(
            "tenant-a",
            "agent-user",
            "request-001",
            "trace-001",
            "header",
            1713700000000L,
            2713700000000L,
            "10.0.0.7",
            "SQLForge-JDBC-Agent-Test",
            accessChannel
        );
    }

    private JdbcAgentSqlRequest sampleRequest() {
        return sampleRequest(
            "--report_code=RPT_SALES_DAILY\n"
                + "--stage=PROD\n"
                + "--biz_date=2026-04-25\n"
                + "SELECT * FROM sales WHERE query_date = '2026-04-26'"
        );
    }

    private JdbcAgentSqlRequest sampleRequest(String sqlText) {
        JdbcAgentSqlRequest request = new JdbcAgentSqlRequest();
        request.setTenantId("tenant-a");
        request.setDatasourceType(DataSourceTypeEnum.HETU);
        request.setDatasourceCode("hetu_main");
        request.setSqlText(sqlText);
        request.setTemplateSql("SELECT * FROM sales WHERE query_date = ?");
        request.setBoundSqlText("SELECT * FROM sales WHERE query_date = '2026-04-26'");
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("query_date", "2026-04-26");
        request.setParameterSnapshot(params);
        return request;
    }
}
