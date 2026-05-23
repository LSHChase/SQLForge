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

class SqlForgeJdbcAgentObserveTest {

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
            requestContext(),
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
    void shouldIgnoreMalformedLeadingCommentsAndKeepObserveOnlyFlow() {
        JdbcAgentProperties properties = baseProperties();
        properties.setAgentMode(JdbcAgentMode.OBSERVE);
        SqlForgeJdbcAgent agent = new SqlForgeJdbcAgent(
            properties,
            queryExecutionClient(),
            auditClient(),
            new NoopJdbcAgentRewriteRuleProvider()
        );

        JdbcAgentExecutionResult<String> result = agent.execute(
            requestContext(),
            sampleRequest(
                "-- operator note without kv\n"
                    + "--report_code=RPT_SALES_DAILY\n"
                    + "--stage=PROD\n"
                    + "SELECT * FROM sales WHERE query_date = '2026-04-26'"
            ),
            new JdbcAgentDirectExecutor<String>() {
                @Override
                public JdbcAgentDirectResult<String> execute(JdbcAgentDirectExecution execution) {
                    return JdbcAgentDirectResult.success("observe-comment-ok", Integer.valueOf(1));
                }
            }
        );

        assertEquals("observe-comment-ok", result.getDirectResult().getPayload());
        assertEquals("RPT_SALES_DAILY", result.getMetadata().getObservation().getCommentContext().get("report_code"));
        assertEquals("PROD", result.getMetadata().getObservation().getCommentContext().get("stage"));
    }

    @Test
    void shouldFailOpenWhenAuditWriteFailsInObserveMode() {
        SqlForgeAccessAuditClient auditClient = auditClient();
        RestTemplate auditRestTemplate = (RestTemplate) ReflectionTestUtils.getField(auditClient, "restTemplate");
        MockRestServiceServer auditServer = MockRestServiceServer.bindTo(auditRestTemplate).build();
        auditServer.expect(requestTo("http://sqlforge.test/api/governance/internal/audit/write"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withServerError());

        JdbcAgentProperties properties = baseProperties();
        properties.setAgentMode(JdbcAgentMode.OBSERVE);
        SqlForgeJdbcAgent agent = new SqlForgeJdbcAgent(
            properties,
            queryExecutionClient(),
            auditClient,
            new NoopJdbcAgentRewriteRuleProvider()
        );

        JdbcAgentExecutionResult<String> result = agent.execute(
            requestContext(),
            sampleRequest(),
            new JdbcAgentDirectExecutor<String>() {
                @Override
                public JdbcAgentDirectResult<String> execute(JdbcAgentDirectExecution execution) {
                    return JdbcAgentDirectResult.success("audit-fail-open", Integer.valueOf(1));
                }
            }
        );

        assertEquals("audit-fail-open", result.getDirectResult().getPayload());
        assertFalse(result.getMetadata().isAuditReported());
        assertTrue(result.getMetadata().getAuditFailureReason().contains("SQLForge 治理审计路由不可用"));
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
            "request-001",
            "trace-001",
            "header",
            1713700000000L,
            2713700000000L,
            "10.0.0.7",
            "SQLForge-JDBC-Agent-Observe-Test",
            AccessChannel.JDBC_AGENT
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
