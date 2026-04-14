package com.sqlforge.backend.service;

import com.sqlforge.backend.model.ConnectionDefinition;
import com.sqlforge.backend.model.ConnectionEngineOverview;
import com.sqlforge.backend.model.ConnectionModuleOverview;
import com.sqlforge.backend.model.EngineDriverAudit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ConnectionOverviewService {

    private final ConnectionService connectionService;
    private final JdbcConnectionSupportService jdbcConnectionSupportService;
    private final ConnectionActivityService connectionActivityService;

    public ConnectionOverviewService(
        ConnectionService connectionService,
        JdbcConnectionSupportService jdbcConnectionSupportService,
        ConnectionActivityService connectionActivityService
    ) {
        this.connectionService = connectionService;
        this.jdbcConnectionSupportService = jdbcConnectionSupportService;
        this.connectionActivityService = connectionActivityService;
    }

    public ConnectionModuleOverview buildOverview() {
        List<ConnectionDefinition> connections = connectionService.listConnections();
        List<EngineDriverAudit> driverAudit = jdbcConnectionSupportService.auditDrivers();
        Map<String, Integer> engineCounts = new LinkedHashMap<String, Integer>();
        int lastProbeHealthyCount = 0;
        int lastPreviewHealthyCount = 0;

        for (ConnectionDefinition connection : connections) {
            Integer existing = engineCounts.get(connection.getEngineCode());
            engineCounts.put(connection.getEngineCode(), existing == null ? 1 : existing + 1);

            if (isHealthyProbe(connection.getLastProbeStatus())) {
                lastProbeHealthyCount += 1;
            }

            if (isHealthyPreview(connection.getLastPreviewStatus())) {
                lastPreviewHealthyCount += 1;
            }
        }

        List<ConnectionEngineOverview> engines = new ArrayList<ConnectionEngineOverview>();

        for (Map.Entry<String, Integer> entry : engineCounts.entrySet()) {
            engines.add(new ConnectionEngineOverview(entry.getKey(), entry.getValue().intValue()));
        }

        int driverReadyCount = 0;

        for (EngineDriverAudit audit : driverAudit) {
            if (audit.isAvailable()) {
                driverReadyCount += 1;
            }
        }

        return new ConnectionModuleOverview(
            connections.size(),
            driverReadyCount,
            driverAudit.size() - driverReadyCount,
            connectionActivityService.listRecentActivity().size(),
            lastProbeHealthyCount,
            lastPreviewHealthyCount,
            engines
        );
    }

    private boolean isHealthyProbe(String status) {
        return "connected".equals(status) || "driver-missing".equals(status);
    }

    private boolean isHealthyPreview(String status) {
        return "result-set".equals(status) || "statement-executed".equals(status);
    }
}
