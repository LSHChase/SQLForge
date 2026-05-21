package com.company.governance.application.service;

import com.company.governance.config.GovernanceDatasourceDriverProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.stereotype.Service;

@Service
public class JdbcDriverArtifactStorageService {

    private final GovernanceDatasourceDriverProperties properties;

    public JdbcDriverArtifactStorageService(GovernanceDatasourceDriverProperties properties) {
        this.properties = properties;
    }

    public Path store(String tenantId, String engineType, String artifactId, String originalFileName, InputStream inputStream)
        throws IOException {
        Path basePath = Paths.get(properties.getStoragePath()).toAbsolutePath().normalize();
        Path tenantPath = basePath.resolve(tenantId).resolve(engineType);
        Files.createDirectories(tenantPath);
        String fileName = artifactId + "-" + sanitizeFileName(originalFileName);
        Path target = tenantPath.resolve(fileName);
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    public Path resolve(String relativePath) {
        return Paths.get(properties.getStoragePath()).toAbsolutePath().normalize().resolve(relativePath).normalize();
    }

    private String sanitizeFileName(String originalFileName) {
        return String.valueOf(originalFileName == null ? "driver.jar" : originalFileName).replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
