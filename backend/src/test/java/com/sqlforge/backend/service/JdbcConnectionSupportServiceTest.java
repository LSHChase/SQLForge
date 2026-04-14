package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.ConnectionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcConnectionSupportServiceTest {

    @Test
    void shouldReportClassAvailability() {
        JdbcConnectionSupportService service = new JdbcConnectionSupportService(new EngineCatalogService());

        assertTrue(service.isDriverAvailable("java.lang.String"));
        assertFalse(service.isDriverAvailable("com.sqlforge.missing.Driver"));
    }

    @Test
    void shouldBuildJdbcUrlFromEngineProfile() {
        JdbcConnectionSupportService service = new JdbcConnectionSupportService(new EngineCatalogService());
        ConnectionRequest request = new ConnectionRequest();
        request.setEngineCode("kyligence");
        request.setHost("127.0.0.1");
        request.setPort(7070);
        request.setCatalog("cube");

        String jdbcUrl = service.buildJdbcUrl(request);

        assertTrue(jdbcUrl.startsWith("jdbc:kylin://127.0.0.1:7070/"));
    }
}
