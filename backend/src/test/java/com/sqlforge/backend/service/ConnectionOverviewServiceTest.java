package com.sqlforge.backend.service;

import com.sqlforge.backend.model.ConnectionActivity;
import com.sqlforge.backend.model.ConnectionDefinition;
import com.sqlforge.backend.model.ConnectionModuleOverview;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConnectionOverviewServiceTest {

    @Test
    void shouldBuildConnectionModuleOverview() {
        ConnectionService connectionService = new StubConnectionService();
        JdbcConnectionSupportService jdbcConnectionSupportService = new StubJdbcSupportService();
        ConnectionActivityService connectionActivityService = new StubActivityService();

        ConnectionOverviewService service = new ConnectionOverviewService(
            connectionService,
            jdbcConnectionSupportService,
            connectionActivityService
        );

        ConnectionModuleOverview overview = service.buildOverview();

        assertEquals(2, overview.getTotalConnections());
        assertEquals(1, overview.getDriverReadyCount());
        assertEquals(1, overview.getDriverMissingCount());
        assertEquals(2, overview.getRecentActivityCount());
        assertEquals(1, overview.getLastProbeHealthyCount());
        assertEquals(1, overview.getLastPreviewHealthyCount());
        assertEquals(2, overview.getEngines().size());
    }

    static class StubConnectionService extends ConnectionService {

        StubConnectionService() {
            super(null, null, new StubActivityService());
        }

        @Override
        public List<ConnectionDefinition> listConnections() {
            return Arrays.asList(
                new ConnectionDefinition(
                    "conn-1",
                    "Primary Trino",
                    "trino",
                    "127.0.0.1",
                    8080,
                    "lakehouse",
                    "analyst",
                    true,
                    "registered",
                    Instant.parse("2026-04-14T00:00:00Z"),
                    "connected",
                    Instant.parse("2026-04-14T00:01:00Z"),
                    "result-set",
                    Instant.parse("2026-04-14T00:02:00Z")
                ),
                new ConnectionDefinition(
                    "conn-2",
                    "Primary Kyligence",
                    "kyligence",
                    "127.0.0.1",
                    7070,
                    "cube",
                    "analyst",
                    true,
                    "registered",
                    Instant.parse("2026-04-14T00:00:00Z"),
                    "unreachable",
                    Instant.parse("2026-04-14T00:03:00Z"),
                    "driver-missing",
                    Instant.parse("2026-04-14T00:04:00Z")
                )
            );
        }
    }

    static class StubJdbcSupportService extends JdbcConnectionSupportService {

        StubJdbcSupportService() {
            super(new EngineCatalogService());
        }

        @Override
        public java.util.List<com.sqlforge.backend.model.EngineDriverAudit> auditDrivers() {
            return Arrays.asList(
                new com.sqlforge.backend.model.EngineDriverAudit("trino", "Trino", "io.trino.jdbc.TrinoDriver", true),
                new com.sqlforge.backend.model.EngineDriverAudit("kyligence", "Kyligence", "org.apache.kylin.jdbc.Driver", false)
            );
        }
    }

    static class StubActivityService extends ConnectionActivityService {

        StubActivityService() {
            super(null);
        }

        @Override
        public List<ConnectionActivity> listRecentActivity() {
            return Arrays.asList(
                new ConnectionActivity(
                    "activity-1",
                    "conn-1",
                    "Primary Trino",
                    "probe",
                    "connected",
                    "probe succeeded",
                    Instant.parse("2026-04-14T00:05:00Z")
                ),
                new ConnectionActivity(
                    "activity-2",
                    "conn-1",
                    "Primary Trino",
                    "preview",
                    "result-set",
                    "preview succeeded",
                    Instant.parse("2026-04-14T00:06:00Z")
                )
            );
        }
    }
}
