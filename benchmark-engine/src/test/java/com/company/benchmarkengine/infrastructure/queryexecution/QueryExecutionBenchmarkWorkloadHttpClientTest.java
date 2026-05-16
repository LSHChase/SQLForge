package com.company.benchmarkengine.infrastructure.queryexecution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.company.benchmarkengine.config.BenchmarkEngineQueryExecutionProperties;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadResponse;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class QueryExecutionBenchmarkWorkloadHttpClientTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldCaptureWorkloadWithSyntheticProtectedHeadersWhenContextIsMissing() {
        QueryExecutionBenchmarkWorkloadHttpClient client = createClient();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://query-execution.test/api/query-execution/internal/benchmark/workload/capture"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"benchmarkTaskId\":\"benchmark-task-001\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"sqlFingerprint\":\"fp-001\"")))
            .andRespond(withSuccess(
                "{\"tenantId\":\"tenant-a\",\"benchmarkTaskId\":\"benchmark-task-001\","
                    + "\"sqlFingerprint\":\"fp-001\",\"workloadDigest\":\"digest-001\","
                    + "\"workloadSource\":\"QUERY_EXECUTION_SYNC\",\"backfillApplied\":false,"
                    + "\"engineSnapshots\":[]}",
                MediaType.APPLICATION_JSON
            ));

        QueryExecutionBenchmarkWorkloadResponse response = client.captureWorkload(baseRequest());

        assertEquals("digest-001", response.getWorkloadDigest());
        server.verify();
    }

    @Test
    void shouldRequireConfiguredBaseUrl() {
        BenchmarkEngineQueryExecutionProperties properties = new BenchmarkEngineQueryExecutionProperties();
        properties.setBaseUrl(" ");
        QueryExecutionBenchmarkWorkloadHttpClient client =
            new QueryExecutionBenchmarkWorkloadHttpClient(new RestTemplateBuilder(), properties);

        BizException ex = assertThrows(BizException.class, () -> client.captureWorkload(baseRequest()));

        assertEquals(ErrorCodeConstants.SYSTEM_CONFIG_INVALID, ex.getCode());
        assertEquals("benchmark-engine 的 query-execution baseUrl 未配置", ex.getMessage());
    }

    private QueryExecutionBenchmarkWorkloadHttpClient createClient() {
        BenchmarkEngineQueryExecutionProperties properties = new BenchmarkEngineQueryExecutionProperties();
        properties.setBaseUrl("http://query-execution.test/api/query-execution/internal/benchmark");
        return new QueryExecutionBenchmarkWorkloadHttpClient(new RestTemplateBuilder(), properties);
    }

    private QueryExecutionBenchmarkWorkloadRequest baseRequest() {
        QueryExecutionBenchmarkWorkloadRequest request = new QueryExecutionBenchmarkWorkloadRequest();
        request.setTenantId("tenant-a");
        request.setBenchmarkTaskId("benchmark-task-001");
        request.setBenchmarkTaskType("BASELINE");
        request.setSqlText("SELECT * FROM orders");
        request.setSqlFingerprint("fp-001");
        request.setTargetEngines(Collections.singletonList(DataSourceTypeEnum.HETU));
        return request;
    }
}
