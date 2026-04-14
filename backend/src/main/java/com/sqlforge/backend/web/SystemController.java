package com.sqlforge.backend.web;

import com.sqlforge.backend.service.EngineCatalogService;
import com.sqlforge.backend.service.JdbcConnectionSupportService;
import com.sqlforge.backend.service.ConnectionOverviewService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    private final EngineCatalogService engineCatalogService;
    private final JdbcConnectionSupportService jdbcConnectionSupportService;
    private final ConnectionOverviewService connectionOverviewService;

    public SystemController(
        EngineCatalogService engineCatalogService,
        JdbcConnectionSupportService jdbcConnectionSupportService,
        ConnectionOverviewService connectionOverviewService
    ) {
        this.engineCatalogService = engineCatalogService;
        this.jdbcConnectionSupportService = jdbcConnectionSupportService;
        this.connectionOverviewService = connectionOverviewService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("status", "ok");
        payload.put("service", "sqlforge-backend");
        payload.put("timestamp", Instant.now().toString());
        return payload;
    }

    @GetMapping("/engines")
    public Map<String, Object> engines() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("engines", engineCatalogService.listSupportedEngines());
        return payload;
    }

    @GetMapping("/driver-audit")
    public Map<String, Object> driverAudit() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("drivers", jdbcConnectionSupportService.auditDrivers());
        return payload;
    }

    @GetMapping("/connection-overview")
    public Map<String, Object> connectionOverview() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("overview", connectionOverviewService.buildOverview());
        return payload;
    }
}
