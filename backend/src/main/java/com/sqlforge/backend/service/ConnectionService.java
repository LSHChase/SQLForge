package com.sqlforge.backend.service;

import com.sqlforge.backend.model.ConnectionDefinition;
import com.sqlforge.backend.model.ConnectionValidationResult;
import com.sqlforge.backend.model.EngineDescriptor;
import com.sqlforge.backend.repository.InMemoryConnectionRepository;
import com.sqlforge.backend.web.dto.ConnectionRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ConnectionService {

    private final EngineCatalogService engineCatalogService;
    private final InMemoryConnectionRepository connectionRepository;

    public ConnectionService(
        EngineCatalogService engineCatalogService,
        InMemoryConnectionRepository connectionRepository
    ) {
        this.engineCatalogService = engineCatalogService;
        this.connectionRepository = connectionRepository;
    }

    public ConnectionValidationResult validate(ConnectionRequest request) {
        List<String> messages = new ArrayList<String>();
        EngineDescriptor engine = engineCatalogService.findByCode(request.getEngineCode());

        if (engine == null) {
            messages.add("engine is not supported");
        }

        if (request.getHost() != null && request.getHost().trim().length() < 2) {
            messages.add("host is too short");
        }

        if (request.getCatalog() != null && request.getCatalog().trim().length() < 2) {
            messages.add("catalog is too short");
        }

        if (request.getPassword() != null && request.getPassword().trim().length() < 6) {
            messages.add("password length must be at least 6");
        }

        if (engine != null) {
            String normalizedEngine = engine.getCode().toLowerCase(Locale.ROOT);

            if (("trino".equals(normalizedEngine) || "presto".equals(normalizedEngine) || "mrs-hetu".equals(normalizedEngine))
                && request.getPort() != null
                && request.getPort().intValue() == 3306) {
                messages.add("query engines should not use the MySQL default port 3306");
            }

            if ("mysql".equals(normalizedEngine) && request.getPort() != null && request.getPort().intValue() == 8080) {
                messages.add("MySQL should not use an HTTP port");
            }
        }

        if (messages.isEmpty()) {
            messages.add("connection definition passed offline validation");
            return new ConnectionValidationResult(true, "validated", messages);
        }

        return new ConnectionValidationResult(false, "invalid", messages);
    }

    public ConnectionDefinition create(ConnectionRequest request) {
        ConnectionValidationResult validation = validate(request);

        if (!validation.isValid()) {
            throw new IllegalArgumentException(String.join("; ", validation.getMessages()));
        }

        ConnectionDefinition connectionDefinition = new ConnectionDefinition(
            UUID.randomUUID().toString(),
            request.getName().trim(),
            request.getEngineCode().trim(),
            request.getHost().trim(),
            request.getPort(),
            request.getCatalog().trim(),
            request.getUsername().trim(),
            request.isSslEnabled(),
            "registered",
            Instant.now()
        );

        return connectionRepository.save(connectionDefinition);
    }

    public List<ConnectionDefinition> listConnections() {
        return connectionRepository.findAll();
    }
}
