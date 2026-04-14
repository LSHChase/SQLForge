package com.sqlforge.backend.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqlforge.backend.model.ConnectionDefinition;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class FileConnectionRepository {

    private final ObjectMapper objectMapper;
    private final String storageFilePath;
    private final List<ConnectionDefinition> connections = new ArrayList<ConnectionDefinition>();

    public FileConnectionRepository(
        ObjectMapper objectMapper,
        @Value("${sqlforge.storage.connections-file:${java.io.tmpdir}/sqlforge/connections.json}") String storageFilePath
    ) {
        this.objectMapper = objectMapper;
        this.storageFilePath = storageFilePath;
    }

    @PostConstruct
    public synchronized void load() {
        File storageFile = new File(storageFilePath);

        if (!storageFile.exists()) {
            return;
        }

        try {
            List<ConnectionDefinition> persisted = objectMapper.readValue(
                storageFile,
                new TypeReference<List<ConnectionDefinition>>() { }
            );
            connections.clear();
            connections.addAll(persisted);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read persisted connections", exception);
        }
    }

    public synchronized ConnectionDefinition save(ConnectionDefinition connectionDefinition) {
        connections.add(connectionDefinition);
        persist();
        return connectionDefinition;
    }

    public synchronized List<ConnectionDefinition> findAll() {
        return new ArrayList<ConnectionDefinition>(connections);
    }

    private void persist() {
        File storageFile = new File(storageFilePath);
        File parent = storageFile.getParentFile();

        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("failed to create storage directory");
        }

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storageFile, connections);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to persist connections", exception);
        }
    }
}
