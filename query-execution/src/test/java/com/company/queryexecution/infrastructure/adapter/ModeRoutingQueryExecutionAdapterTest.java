package com.company.queryexecution.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.application.service.HetuRouteCalibrationService;
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
    void shouldRejectHetuRequestWhenRealChainIsDisabled() {
        QueryExecutionHetuProperties properties = new QueryExecutionHetuProperties();
        ModeRoutingQueryExecutionAdapter adapter = newAdapter(properties, Collections.<HetuExecutionModeAdapter>emptyList());

        HetuExecutionUnavailableException ex = assertThrows(
            HetuExecutionUnavailableException.class,
            () -> adapter.execute(DataSourceTypeEnum.HETU, "SELECT 1", baseRequest(), false)
        );

        assertEquals("Hetu execution chain is disabled for the current environment", ex.getMessage());
        assertEquals(Collections.singletonList("CHAIN_DISABLED"), ex.getAttemptedModes());
        assertEquals("REPO_CLOSED_BASELINE", ex.getRouteProfile());
    }

    @Test
    void shouldKeepDeterministicExecutionForNonHetuTargets() {
        QueryExecutionHetuProperties properties = new QueryExecutionHetuProperties();
        ModeRoutingQueryExecutionAdapter adapter = newAdapter(properties, Collections.<HetuExecutionModeAdapter>emptyList());

        QueryExecutionStep step = adapter.execute(DataSourceTypeEnum.HIVE, "SELECT 1", baseRequest(), true);

        assertEquals("HIVE_FALLBACK", step.getExecutionMode());
        assertEquals(Collections.singletonList("HIVE_FALLBACK"), step.getAttemptedModes());
        assertFalse(step.getRows().isEmpty());
    }

    @Test
    void shouldRejectHetuRequestWhenNoModeIsConfigured() {
        QueryExecutionHetuProperties properties = enabledProperties();
        ModeRoutingQueryExecutionAdapter adapter = newAdapter(properties, Collections.<HetuExecutionModeAdapter>emptyList());

        HetuExecutionUnavailableException ex = assertThrows(
            HetuExecutionUnavailableException.class,
            () -> adapter.execute(DataSourceTypeEnum.HETU, "SELECT 1", baseRequest(), false)
        );

        assertEquals("No Hetu execution mode is configured for the current environment", ex.getMessage());
        assertEquals(Collections.singletonList("CHAIN_UNCONFIGURED"), ex.getAttemptedModes());
    }

    @Test
    void shouldRejectWhenConfiguredModesResolveToNoAvailableAdapter() {
        QueryExecutionHetuProperties properties = enabledProperties(QueryExecutionAccessMode.JDBC);
        ModeRoutingQueryExecutionAdapter adapter = newAdapter(properties, Collections.<HetuExecutionModeAdapter>emptyList());

        HetuExecutionUnavailableException ex = assertThrows(
            HetuExecutionUnavailableException.class,
            () -> adapter.execute(DataSourceTypeEnum.HETU, "SELECT 1", baseRequest(), false)
        );

        assertEquals("No calibrated Hetu execution mode is currently routable", ex.getMessage());
        assertEquals(Collections.singletonList("JDBC:SKIPPED_ADAPTER_UNAVAILABLE"), ex.getAttemptedModes());
    }

    @Test
    void shouldRouteToFirstSuccessfulConfiguredMode() {
        QueryExecutionHetuProperties properties = enabledProperties(
            QueryExecutionAccessMode.JDBC,
            QueryExecutionAccessMode.REST
        );
        configureJdbc(properties);
        configureRest(properties);
        ModeRoutingQueryExecutionAdapter adapter = newAdapter(
            properties,
            Arrays.<HetuExecutionModeAdapter>asList(
                failingAdapter(QueryExecutionAccessMode.JDBC, "jdbc failed"),
                successfulAdapter(QueryExecutionAccessMode.REST, sampleStep("REST"))
            )
        );

        QueryExecutionStep step = adapter.execute(DataSourceTypeEnum.HETU, "SELECT 1", baseRequest(), false);

        assertEquals("REST", step.getExecutionMode());
        assertEquals(Arrays.asList("JDBC", "JDBC:FAILED_EXECUTION", "REST"), step.getAttemptedModes());
        assertEquals("REST", step.getRows().get(0).get("executionMode"));
        assertEquals("REPO_CLOSED_BASELINE", step.getRouteProfile());
        assertEquals(Arrays.asList("JDBC", "REST"), step.getRouteOrder());
    }

    @Test
    void shouldSkipUnavailableModesBeforeUsingAvailableAdapter() {
        QueryExecutionHetuProperties properties = enabledProperties(
            QueryExecutionAccessMode.JDBC,
            QueryExecutionAccessMode.CLIENT
        );
        configureClient(properties);
        ModeRoutingQueryExecutionAdapter adapter = newAdapter(
            properties,
            Collections.<HetuExecutionModeAdapter>singletonList(
                successfulAdapter(QueryExecutionAccessMode.CLIENT, sampleStep("CLIENT"))
            )
        );

        QueryExecutionStep step = adapter.execute(DataSourceTypeEnum.HETU, "SELECT 1", baseRequest(), false);

        assertEquals("CLIENT", step.getExecutionMode());
        assertEquals(Arrays.asList("JDBC:SKIPPED_ADAPTER_UNAVAILABLE", "CLIENT"), step.getAttemptedModes());
    }

    @Test
    void shouldExposeAttemptedModesWhenAllRealModesFail() {
        QueryExecutionHetuProperties properties = enabledProperties(
            QueryExecutionAccessMode.JDBC,
            QueryExecutionAccessMode.REST
        );
        configureJdbc(properties);
        configureRest(properties);
        ModeRoutingQueryExecutionAdapter adapter = newAdapter(
            properties,
            Arrays.<HetuExecutionModeAdapter>asList(
                failingAdapter(QueryExecutionAccessMode.JDBC, "jdbc failed"),
                failingAdapter(QueryExecutionAccessMode.REST, "rest failed")
            )
        );

        HetuExecutionUnavailableException ex = assertThrows(
            HetuExecutionUnavailableException.class,
            () -> adapter.execute(DataSourceTypeEnum.HETU, "SELECT 1", baseRequest(), false)
        );

        assertTrue(ex.getMessage().contains("attemptedModes=[JDBC, JDBC:FAILED_EXECUTION, REST, REST:FAILED_EXECUTION]"));
        assertEquals(Arrays.asList("JDBC", "JDBC:FAILED_EXECUTION", "REST", "REST:FAILED_EXECUTION"), ex.getAttemptedModes());
        assertEquals(Arrays.asList("JDBC", "REST"), ex.getRouteOrder());
    }

    private QueryExecutionHetuProperties enabledProperties(QueryExecutionAccessMode... modes) {
        QueryExecutionHetuProperties properties = new QueryExecutionHetuProperties();
        properties.setEnabled(true);
        properties.getAllowedModes().clear();
        properties.getAllowedModes().addAll(Arrays.asList(modes));
        properties.getCalibration().getRouteOrder().clear();
        properties.getCalibration().getRouteOrder().addAll(Arrays.asList(modes));
        return properties;
    }

    private ModeRoutingQueryExecutionAdapter newAdapter(QueryExecutionHetuProperties properties,
                                                        java.util.List<HetuExecutionModeAdapter> adapters) {
        return new ModeRoutingQueryExecutionAdapter(
            properties,
            new HetuRouteCalibrationService(properties, adapters),
            adapters
        );
    }

    private void configureJdbc(QueryExecutionHetuProperties properties) {
        properties.getJdbc().setUrl("jdbc:hetu://localhost:28088/hive/default");
    }

    private void configureRest(QueryExecutionHetuProperties properties) {
        properties.getRest().setEndpoint("http://localhost:28089/v1/query");
    }

    private void configureClient(QueryExecutionHetuProperties properties) {
        properties.getClient().setEnabled(true);
        properties.getClient().setEndpoint("http://localhost:28090/v1/statement");
        properties.getClient().setUser("hetu-user");
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
