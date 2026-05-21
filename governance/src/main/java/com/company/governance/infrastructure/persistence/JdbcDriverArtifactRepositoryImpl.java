package com.company.governance.infrastructure.persistence;

import com.company.governance.domain.datasource.JdbcDriverArtifact;
import com.company.governance.domain.datasource.repository.JdbcDriverArtifactRepository;
import com.company.governance.infrastructure.persistence.entity.JdbcDriverArtifactRecord;
import com.company.governance.infrastructure.persistence.mapper.JdbcDriverArtifactMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDriverArtifactRepositoryImpl implements JdbcDriverArtifactRepository {

    private final JdbcDriverArtifactMapper mapper;

    public JdbcDriverArtifactRepositoryImpl(JdbcDriverArtifactMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public JdbcDriverArtifact save(JdbcDriverArtifact artifact) {
        mapper.insert(toRecord(artifact));
        return artifact;
    }

    @Override
    public Optional<JdbcDriverArtifact> findByTenantIdAndArtifactId(String tenantId, String artifactId) {
        return Optional.ofNullable(toDomain(mapper.selectByTenantIdAndArtifactId(tenantId, artifactId)));
    }

    @Override
    public List<JdbcDriverArtifact> findByTenantId(String tenantId) {
        List<JdbcDriverArtifactRecord> records = mapper.selectByTenantId(tenantId);
        List<JdbcDriverArtifact> results = new ArrayList<JdbcDriverArtifact>(records == null ? 0 : records.size());
        if (records != null) {
            for (JdbcDriverArtifactRecord record : records) {
                results.add(toDomain(record));
            }
        }
        return results;
    }

    private JdbcDriverArtifactRecord toRecord(JdbcDriverArtifact artifact) {
        JdbcDriverArtifactRecord record = new JdbcDriverArtifactRecord();
        record.setArtifactId(artifact.getArtifactId());
        record.setTenantId(artifact.getTenantId());
        record.setEngineType(artifact.getEngineType());
        record.setDriverClassName(artifact.getDriverClassName());
        record.setVersionLabel(artifact.getVersionLabel());
        record.setOriginalFileName(artifact.getOriginalFileName());
        record.setSizeBytes(Long.valueOf(artifact.getSizeBytes()));
        record.setSha256(artifact.getSha256());
        record.setRelativePath(artifact.getRelativePath());
        record.setStatus(artifact.getStatus());
        record.setUploadedBy(artifact.getUploadedBy());
        Instant now = artifact.getUpdatedAt() == null ? Instant.now() : artifact.getUpdatedAt();
        record.setCreateTime(artifact.getCreatedAt() == null ? now : artifact.getCreatedAt());
        record.setUpdateTime(now);
        return record;
    }

    private JdbcDriverArtifact toDomain(JdbcDriverArtifactRecord record) {
        if (record == null) {
            return null;
        }
        return new JdbcDriverArtifact(
            record.getArtifactId(),
            record.getTenantId(),
            record.getEngineType(),
            record.getDriverClassName(),
            record.getVersionLabel(),
            record.getOriginalFileName(),
            record.getSizeBytes() == null ? 0L : record.getSizeBytes().longValue(),
            record.getSha256(),
            record.getRelativePath(),
            record.getStatus(),
            record.getUploadedBy(),
            record.getCreateTime(),
            record.getUpdateTime()
        );
    }
}
