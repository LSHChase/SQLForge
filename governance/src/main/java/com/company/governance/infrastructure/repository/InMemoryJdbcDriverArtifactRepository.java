package com.company.governance.infrastructure.repository;

import com.company.governance.domain.datasource.JdbcDriverArtifact;
import com.company.governance.domain.datasource.repository.JdbcDriverArtifactRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryJdbcDriverArtifactRepository implements JdbcDriverArtifactRepository {

    private final Map<String, JdbcDriverArtifact> artifacts = new ConcurrentHashMap<String, JdbcDriverArtifact>();

    @Override
    public JdbcDriverArtifact save(JdbcDriverArtifact artifact) {
        artifacts.put(artifact.getArtifactId(), artifact);
        return artifact;
    }

    @Override
    public Optional<JdbcDriverArtifact> findByTenantIdAndArtifactId(String tenantId, String artifactId) {
        JdbcDriverArtifact artifact = artifacts.get(artifactId);
        if (artifact == null || !tenantId.equals(artifact.getTenantId())) {
            return Optional.empty();
        }
        return Optional.of(artifact);
    }

    @Override
    public List<JdbcDriverArtifact> findByTenantId(String tenantId) {
        List<JdbcDriverArtifact> result = new ArrayList<JdbcDriverArtifact>();
        for (JdbcDriverArtifact artifact : artifacts.values()) {
            if (tenantId.equals(artifact.getTenantId())) {
                result.add(artifact);
            }
        }
        result.sort(Comparator.comparing(JdbcDriverArtifact::getCreatedAt).reversed());
        return result;
    }
}
