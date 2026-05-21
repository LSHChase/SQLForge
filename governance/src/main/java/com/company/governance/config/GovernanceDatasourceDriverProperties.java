package com.company.governance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "governance.datasource-driver")
public class GovernanceDatasourceDriverProperties {

    private String storagePath = "./runtime/governance-drivers";
    private DataSize maxFileSize = DataSize.ofMegabytes(50);

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSize.toBytes();
    }

    public DataSize getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(DataSize maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
}
