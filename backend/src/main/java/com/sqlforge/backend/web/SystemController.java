package com.sqlforge.backend.web;

import com.sqlforge.backend.service.EngineCatalogService;
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

    public SystemController(EngineCatalogService engineCatalogService) {
        this.engineCatalogService = engineCatalogService;
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
}
