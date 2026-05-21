package com.company.governance.domain.datasource.repository;

import com.company.governance.domain.datasource.JdbcDriverArtifact;
import java.util.List;
import java.util.Optional;

public interface JdbcDriverArtifactRepository {

    JdbcDriverArtifact save(JdbcDriverArtifact artifact);

    Optional<JdbcDriverArtifact> findByTenantIdAndArtifactId(String tenantId, String artifactId);

    List<JdbcDriverArtifact> findByTenantId(String tenantId);
}
