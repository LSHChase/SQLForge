package com.sqlforge.backend.service;

import com.sqlforge.backend.model.EngineDescriptor;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineCatalogServiceTest {

    @Test
    void shouldExposeRichProfilesForAllSupportedEngines() {
        EngineCatalogService service = new EngineCatalogService();

        List<EngineDescriptor> engines = service.listSupportedEngines();
        EngineDescriptor trino = service.findByCode("trino");
        EngineDescriptor kyligence = service.findByCode("kyligence");

        assertEquals(6, engines.size());
        assertEquals(8080, trino.getDefaultPort());
        assertEquals("http", trino.getTransport());
        assertEquals("trino", trino.getJdbcScheme());
        assertEquals(7070, kyligence.getDefaultPort());
        assertEquals("kylin", kyligence.getJdbcScheme());
        assertTrue(kyligence.isArmReady());
    }
}
