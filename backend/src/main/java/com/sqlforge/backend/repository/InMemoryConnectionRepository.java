package com.sqlforge.backend.repository;

import com.sqlforge.backend.model.ConnectionDefinition;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryConnectionRepository {

    private final List<ConnectionDefinition> connections = new ArrayList<ConnectionDefinition>();

    public synchronized ConnectionDefinition save(ConnectionDefinition connectionDefinition) {
        connections.add(connectionDefinition);
        return connectionDefinition;
    }

    public synchronized List<ConnectionDefinition> findAll() {
        return new ArrayList<ConnectionDefinition>(connections);
    }
}
