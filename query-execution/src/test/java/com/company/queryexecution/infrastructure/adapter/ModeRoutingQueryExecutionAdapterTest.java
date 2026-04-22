package com.company.queryexecution.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.config.QueryExecutionHetuProperties;
import com.company.queryexecution.domain.query.QueryExecutionAccessMode;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModeRoutingQueryExecutionAdapterTest {

    @Test
    void shouldFallbackToDeterministicExecutionWhenHetuRealChainIsDisabled() {
        QueryExecutionHetuProperties properties = new QueryExecutionHetuProperties();
        ModeRoutingQueryExecutionAdapter adapter =
            new ModeRoutingQueryExecutionAdapter(properties, Collections.<HetuExecutionModeAdapter>emptyList());

        QueryExecutionStep step = adapter.execute(DataSourceTypeEnum.HETU, "SELECT 1", baseRequest(), false);

        assertEquals("SIMULATED", step.getExecutionMode());
        assertEquals(Collections.singletonList("SIMULATED"), step.getAttemptedModes());
        assertFalse(step.getRows().isEmpty());
    }

    @Test
    void shouldRouteToFirstSuccessfulConfiguredMode() {
        QueryExecutionHetuProperties properties = enabledProperties(
            QueryExecutionAccessMode.JDBC,
            QueryExecutionAccessMode.REST
        );
        ModeRoutingQueryExecutionAdapter adapter = new ModeRoutingQueryExecutionAdapter(
            properties,
            Arrays.<HetuExecutionModeAdapter>asList(
                failingAdapter(QueryExecutionAccessMode.JDBC, "jdbc failed"),
                successfulAdapter(QueryExecutionAccessMode.REST, sampleStep("REST"))
            )
        );

        QueryExecutionStep step = adapter.execute(DataSourceTypeEnum.HETU, "SELECT 1", baseRequest(), false);

        assertEquals("REST", step.getExecutionMode());
        assertEquals(Arrays.asList("JDBC", "JDBC:FAILED", "REST"), step.getAttemptedModes());
        assertEquals("REST", step.getRows().get(0).get("executionMode"));
    }

    @Test
    void shouldSkipUnavailableModesBeforeUsingAvailableAdapter() {
        QueryExecutionHetuProperties properties = enabledProperties(
            QueryExecutionAccessMode.JDBC,
            QueryExecutionAccessMode.CLIENT
        );
        ModeRoutingQueryExecutionAdapter adapter = new ModeRoutingQueryExecutionAdapter(
            properties,
            Collections.<HetuExecutionModeAdapter>singletonList(
                successfulAdapter(QueryExecutionAccessMode.CLIENT, sampleStep("CLIENT"))
            )
        );

        QueryExecutionStep step = adapter.execute(DataSourceTypeEnum.HETU, "SELECT 1", baseRequest(), false);

        assertEquals("CLIENT", step.getExecutionMode());
        assertEquals(Arrays.asList("JDBC:UNAVAILABLE", "CLIENT"), step.getAttemptedModes());
    }

    @Test
    void shouldExposeAttemptedModesWhenAllRealModesFail() {
        QueryExecutionHetuProperties properties = enabledProperties(
            QueryExecutionAccessMode.JDBC,
            QueryExecutionAccessMode.REST
        );
        ModeRoutingQueryExecutionAdapter adapter = new ModeRoutingQueryExecutionAdapter(
            properties,
            Arrays.<HetuExecutionModeAdapter>asList(
                failingAdapter(QueryExecutionAccessMode.JDBC, "jdbc failed"),
                failingAdapter(QueryExecutionAccessMode.REST, "rest failed")
            )
        );

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> adapter.execute(DataSourceTypeEnum.HETU, "SELECT 1", baseRequest(), false)
        );

        assertTrue(ex.getMessage().contains("attemptedModes=[JDBC, JDBC:FAILED, REST, REST:FAILED]"));
    }

    private QueryExecutionHetuProperties enabledProperties(QueryExecutionAccessMode... modes) {
        QueryExecutionHetuProperties properties = new QueryExecutionHetuProperties();
        properties.setEnabled(true);
        properties.getAllowedModes().clear();
        properties.getAllowedModes().addAll(Arrays.asList(modes));
        return properties;
    }

    private QueryExecuteRequest baseRequest() {
        QueryExecuteRequest request = new QueryExecuteRequest();
        request.setTenantId("tenant-a");
        request.setDatasourceType(DataSourceTypeEnum.HETU);
        request.setSqlText("SELECT 1");
        return request;
    }

    private HetuExecutionModeAdapter failingAdapter(QueryExecutionAccessMode mode, String message) {
        return new HetuExecutionModeAdapter() {
            @Override
            public QueryExecutionAccessMode getMode() {
                return mode;
            }

            @Override
            public QueryExecutionStep execute(String actualSql, QueryExecuteRequest request, boolean degradedPath) {
                throw new IllegalStateException(message);
            }
        };
    }

    private HetuExecutionModeAdapter successfulAdapter(QueryExecutionAccessMode mode, QueryExecutionStep step) {
        return new HetuExecutionModeAdapter() {
            @Override
            public QueryExecutionAccessMode getMode() {
                return mode;
            }

            @Override
            public QueryExecutionStep execute(String actualSql, QueryExecuteRequest request, boolean degradedPath) {
                return step;
            }
        };
    }

    private QueryExecutionStep sampleStep(String executionMode) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("engine", "HETU");
        row.put("executionMode", executionMode);
        return new QueryExecutionStep(
            DataSourceTypeEnum.HETU,
            Collections.<Map<String, Object>>singletonList(row),
            15L,
            3L,
            false,
            true,
            executionMode,
            Collections.singletonList(executionMode)
        );
    }
}
