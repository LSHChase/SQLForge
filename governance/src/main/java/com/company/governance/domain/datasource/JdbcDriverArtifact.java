package com.company.governance.domain.datasource;

import java.time.Instant;

public class JdbcDriverArtifact {

    private final String artifactId;
    private final String tenantId;
    private final String engineType;
    private final String driverClassName;
    private final String versionLabel;
    private final String originalFileName;
    private final long sizeBytes;
    private final String sha256;
    private final String relativePath;
    private final String status;
    private final String uploadedBy;
    private final Instant createdAt;
    private final Instant updatedAt;

    public JdbcDriverArtifact(String artifactId,
                              String tenantId,
                              String engineType,
                              String driverClassName,
                              String versionLabel,
                              String originalFileName,
                              long sizeBytes,
                              String sha256,
                              String relativePath,
                              String status,
                              String uploadedBy,
                              Instant createdAt,
                              Instant updatedAt) {
        this.artifactId = artifactId;
        this.tenantId = tenantId;
        this.engineType = engineType;
        this.driverClassName = driverClassName;
        this.versionLabel = versionLabel;
        this.originalFileName = originalFileName;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.relativePath = relativePath;
        this.status = status;
        this.uploadedBy = uploadedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getEngineType() {
        return engineType;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getVersionLabel() {
        return versionLabel;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getStatus() {
        return status;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
