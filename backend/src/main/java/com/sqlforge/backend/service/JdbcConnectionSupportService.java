package com.sqlforge.backend.service;

import com.sqlforge.backend.model.EngineDriverAudit;
import com.sqlforge.backend.model.EngineDescriptor;
import com.sqlforge.backend.web.dto.ConnectionRequest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.springframework.stereotype.Service;

@Service
public class JdbcConnectionSupportService {

    private final EngineCatalogService engineCatalogService;

    public JdbcConnectionSupportService(EngineCatalogService engineCatalogService) {
        this.engineCatalogService = engineCatalogService;
    }

    public EngineDescriptor findEngine(String engineCode) {
        return engineCatalogService.findByCode(engineCode);
    }

    public String buildJdbcUrl(ConnectionRequest request) {
        EngineDescriptor engine = findEngine(request.getEngineCode());

        if (engine == null) {
            return "unsupported://" + request.getHost() + ":" + request.getPort();
        }

        return "jdbc:" + engine.getJdbcScheme() + "://" + request.getHost().trim() + ":" + request.getPort()
            + "/" + request.getCatalog().trim();
    }

    public boolean isDriverAvailable(String driverClassName) {
        if (driverClassName == null || driverClassName.trim().isEmpty()) {
            return false;
        }

        try {
            Class.forName(driverClassName);
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    public Connection openConnection(String jdbcUrl, ConnectionRequest request, int timeoutMs) throws SQLException {
        DriverManager.setLoginTimeout(Math.max(1, timeoutMs / 1000));

        Properties properties = new Properties();
        properties.setProperty("user", request.getUsername().trim());
        properties.setProperty("password", request.getPassword());
        properties.setProperty("ssl", String.valueOf(request.isSslEnabled()));

        return DriverManager.getConnection(jdbcUrl, properties);
    }

    public List<EngineDriverAudit> auditDrivers() {
        List<EngineDriverAudit> audits = new ArrayList<EngineDriverAudit>();

        for (EngineDescriptor engine : engineCatalogService.listSupportedEngines()) {
            audits.add(
                new EngineDriverAudit(
                    engine.getCode(),
                    engine.getName(),
                    engine.getDriverClassName(),
                    isDriverAvailable(engine.getDriverClassName())
                )
            );
        }

        return audits;
    }
}
