package com.sqlforge.backend.service;

import com.sqlforge.backend.model.ConnectionDefinition;
import com.sqlforge.backend.model.ConnectionValidationResult;
import com.sqlforge.backend.model.EngineDescriptor;
import com.sqlforge.backend.repository.FileConnectionRepository;
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
    private final FileConnectionRepository connectionRepository;
    private final ConnectionActivityService connectionActivityService;

    public ConnectionService(
        EngineCatalogService engineCatalogService,
        FileConnectionRepository connectionRepository,
        ConnectionActivityService connectionActivityService
    ) {
        this.engineCatalogService = engineCatalogService;
        this.connectionRepository = connectionRepository;
        this.connectionActivityService = connectionActivityService;
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

        ConnectionDefinition saved = connectionRepository.save(connectionDefinition);
        connectionActivityService.record(saved.getId(), saved.getName(), "create", "registered", "connection created");
        return saved;
    }

    public ConnectionDefinition update(String id, ConnectionRequest request) {
        ConnectionValidationResult validation = validate(request);

        if (!validation.isValid()) {
            throw new IllegalArgumentException(String.join("; ", validation.getMessages()));
        }

        ConnectionDefinition existing = connectionRepository.findById(id);

        if (existing == null) {
            throw new IllegalArgumentException("connection does not exist");
        }

        ConnectionDefinition updated = new ConnectionDefinition(
            existing.getId(),
            request.getName().trim(),
            request.getEngineCode().trim(),
            request.getHost().trim(),
            request.getPort(),
            request.getCatalog().trim(),
            request.getUsername().trim(),
            request.isSslEnabled(),
            existing.getStatus(),
            existing.getCreatedAt()
        );

        ConnectionDefinition saved = connectionRepository.update(updated);
        connectionActivityService.record(saved.getId(), saved.getName(), "update", "updated", "connection updated");
        return saved;
    }

    public List<ConnectionDefinition> listConnections() {
        return connectionRepository.findAll();
    }

    public ConnectionDefinition recordProbe(String id, String probeStatus) {
        ConnectionDefinition existing = connectionRepository.findById(id);

        if (existing == null) {
            throw new IllegalArgumentException("connection does not exist");
        }

        ConnectionDefinition updated = new ConnectionDefinition(
            existing.getId(),
            existing.getName(),
            existing.getEngineCode(),
            existing.getHost(),
            existing.getPort(),
            existing.getCatalog(),
            existing.getUsername(),
            existing.isSslEnabled(),
            existing.getStatus(),
            existing.getCreatedAt(),
            probeStatus,
            Instant.now(),
            existing.getLastPreviewStatus(),
            existing.getLastPreviewAt()
        );

        ConnectionDefinition saved = connectionRepository.update(updated);
        connectionActivityService.record(saved.getId(), saved.getName(), "probe", probeStatus, "probe result recorded");
        return saved;
    }

    public ConnectionDefinition recordPreview(String id, String previewStatus) {
        ConnectionDefinition existing = connectionRepository.findById(id);

        if (existing == null) {
            throw new IllegalArgumentException("connection does not exist");
        }

        ConnectionDefinition updated = new ConnectionDefinition(
            existing.getId(),
            existing.getName(),
            existing.getEngineCode(),
            existing.getHost(),
            existing.getPort(),
            existing.getCatalog(),
            existing.getUsername(),
            existing.isSslEnabled(),
            existing.getStatus(),
            existing.getCreatedAt(),
            existing.getLastProbeStatus(),
            existing.getLastProbeAt(),
            previewStatus,
            Instant.now()
        );

        ConnectionDefinition saved = connectionRepository.update(updated);
        connectionActivityService.record(saved.getId(), saved.getName(), "preview", previewStatus, "preview result recorded");
        return saved;
    }

    public void deleteConnection(String id) {
        ConnectionDefinition existing = connectionRepository.findById(id);

        if (existing == null || !connectionRepository.deleteById(id)) {
            throw new IllegalArgumentException("connection does not exist");
        }

        connectionActivityService.record(existing.getId(), existing.getName(), "delete", "deleted", "connection deleted");
    }

    public List<com.sqlforge.backend.model.ConnectionActivity> listActivity(String id) {
        return connectionActivityService.listForConnection(id);
    }
}
