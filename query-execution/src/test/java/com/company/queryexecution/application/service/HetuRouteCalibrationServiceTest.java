package com.company.queryexecution.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.config.QueryExecutionHetuProperties;
import com.company.queryexecution.domain.query.HetuRouteCalibrationSnapshot;
import com.company.queryexecution.domain.query.QueryExecutionAccessMode;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.queryexecution.infrastructure.adapter.HetuExecutionModeAdapter;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class HetuRouteCalibrationServiceTest {

    @Test
    void shouldBuildCalibratedSnapshotWithReadyAndSkippedModes() {
        QueryExecutionHetuProperties properties = new QueryExecutionHetuProperties();
        properties.setEnabled(true);
        properties.getAllowedModes().clear();
        properties.getAllowedModes().addAll(Arrays.asList(
            QueryExecutionAccessMode.JDBC,
            QueryExecutionAccessMode.REST,
            QueryExecutionAccessMode.CLIENT
        ));
        properties.getCalibration().getRouteOrder().clear();
        properties.getCalibration().getRouteOrder().addAll(Arrays.asList(
            QueryExecutionAccessMode.CLIENT,
            QueryExecutionAccessMode.JDBC,
            QueryExecutionAccessMode.REST
        ));
        properties.getClient().setEnabled(true);
        properties.getClient().setEndpoint("http://localhost:28090/v1/statement");
        properties.getClient().setUser("hetu-user");
        properties.getJdbc().setUrl("jdbc:hetu://localhost:28088/hive/default");

        HetuRouteCalibrationService service = new HetuRouteCalibrationService(
            properties,
            Arrays.<HetuExecutionModeAdapter>asList(
                adapter(QueryExecutionAccessMode.JDBC),
                adapter(QueryExecutionAccessMode.CLIENT)
            )
        );

        HetuRouteCalibrationSnapshot snapshot = service.currentSnapshot();

        assertTrue(snapshot.isEnabled());
        assertEquals("REPO_CLOSED_BASELINE", snapshot.getRouteProfile());
        assertEquals(Arrays.asList("CLIENT", "JDBC", "REST"), snapshot.routeOrderNames());
        assertEquals("READY", snapshot.getModeSnapshot(QueryExecutionAccessMode.CLIENT).getReadinessStatus());
        assertTrue(snapshot.getModeSnapshot(QueryExecutionAccessMode.CLIENT).isWillAttemptInCurrentPolicy());
        assertEquals("READY", snapshot.getModeSnapshot(QueryExecutionAccessMode.JDBC).getReadinessStatus());
        assertTrue(snapshot.getModeSnapshot(QueryExecutionAccessMode.JDBC).isWillAttemptInCurrentPolicy());
        assertEquals("ADAPTER_UNAVAILABLE", snapshot.getModeSnapshot(QueryExecutionAccessMode.REST).getReadinessStatus());
        assertFalse(snapshot.getModeSnapshot(QueryExecutionAccessMode.REST).isWillAttemptInCurrentPolicy());
        assertEquals("REPO_CLOSED_CONFIGURATION", snapshot.getEvidenceSource());
        assertEquals("PENDING_ENV_WINDOW", snapshot.getLiveVerificationStatus());
    }

    @Test
    void shouldClassifyCommonFailureLayers() {
        QueryExecutionHetuProperties properties = new QueryExecutionHetuProperties();
        HetuRouteCalibrationService service = new HetuRouteCalibrationService(
            properties,
            Collections.<HetuExecutionModeAdapter>emptyList()
        );

        assertEquals("FAILED_TIMEOUT", service.classifyFailure(new IllegalStateException("query timeout exceeded")));
        assertEquals("FAILED_AUTH", service.classifyFailure(new IllegalStateException("unauthorized")));
        assertEquals("FAILED_CONFIGURATION", service.classifyFailure(new IllegalStateException("endpoint is not configured")));
        assertEquals("FAILED_CONNECTIVITY", service.classifyFailure(new IllegalStateException("connection refused")));
        assertEquals("FAILED_EXECUTION", service.classifyFailure(new IllegalStateException("unexpected engine failure")));
    }

    @Test
    void shouldTreatJdbcAsReadyWhenGovernanceResolutionIsEnabled() {
        QueryExecutionHetuProperties properties = new QueryExecutionHetuProperties();
        properties.setEnabled(true);
        properties.getAllowedModes().clear();
        properties.getAllowedModes().add(QueryExecutionAccessMode.JDBC);
        properties.getCalibration().getRouteOrder().clear();
        properties.getCalibration().getRouteOrder().add(QueryExecutionAccessMode.JDBC);
        HetuRouteCalibrationService service = new HetuRouteCalibrationService(
            properties,
            Collections.<HetuExecutionModeAdapter>singletonList(adapter(QueryExecutionAccessMode.JDBC))
        );

        HetuRouteCalibrationSnapshot snapshot = service.currentSnapshot();

        assertEquals("READY", snapshot.getModeSnapshot(QueryExecutionAccessMode.JDBC).getReadinessStatus());
        assertTrue(snapshot.getModeSnapshot(QueryExecutionAccessMode.JDBC).isWillAttemptInCurrentPolicy());
    }

    @Test
    void shouldMarkJdbcUnconfiguredWhenLocalAndGovernanceResolutionAreDisabled() {
        QueryExecutionHetuProperties properties = new QueryExecutionHetuProperties();
        properties.setEnabled(true);
        properties.getJdbc().setGovernanceResolutionEnabled(false);
        properties.getAllowedModes().clear();
        properties.getAllowedModes().add(QueryExecutionAccessMode.JDBC);
        properties.getCalibration().getRouteOrder().clear();
        properties.getCalibration().getRouteOrder().add(QueryExecutionAccessMode.JDBC);
        HetuRouteCalibrationService service = new HetuRouteCalibrationService(
            properties,
            Collections.<HetuExecutionModeAdapter>singletonList(adapter(QueryExecutionAccessMode.JDBC))
        );

        HetuRouteCalibrationSnapshot snapshot = service.currentSnapshot();

        assertEquals("UNCONFIGURED", snapshot.getModeSnapshot(QueryExecutionAccessMode.JDBC).getReadinessStatus());
        assertFalse(snapshot.getModeSnapshot(QueryExecutionAccessMode.JDBC).isWillAttemptInCurrentPolicy());
    }

    private HetuExecutionModeAdapter adapter(QueryExecutionAccessMode mode) {
        return new HetuExecutionModeAdapter() {
            @Override
            public QueryExecutionAccessMode getMode() {
                return mode;
            }

            @Override
            public QueryExecutionStep execute(String actualSql, QueryExecuteRequest request, boolean degradedPath) {
                return new QueryExecutionStep(
                    DataSourceTypeEnum.HETU,
                    Collections.<java.util.Map<String, Object>>emptyList(),
                    1L,
                    1L,
                    false,
                    false,
                    mode.name(),
                    Collections.singletonList(mode.name())
                );
            }
        };
    }
}
