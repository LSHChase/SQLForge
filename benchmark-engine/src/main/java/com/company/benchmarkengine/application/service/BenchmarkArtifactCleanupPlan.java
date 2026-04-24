package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.util.StringUtils;

public class BenchmarkArtifactCleanupPlan {

    private final Set<String> retainedFileNames;
    private final Set<String> retainedArtifactKeys;

    public BenchmarkArtifactCleanupPlan(List<BenchmarkReportArtifact> artifacts) {
        LinkedHashSet<String> fileNames = new LinkedHashSet<String>();
        LinkedHashSet<String> artifactKeys = new LinkedHashSet<String>();
        if (artifacts != null) {
            for (BenchmarkReportArtifact artifact : artifacts) {
                if (artifact == null) {
                    continue;
                }
                if (StringUtils.hasText(artifact.getFileName())) {
                    fileNames.add(artifact.getFileName().trim());
                }
                if (StringUtils.hasText(artifact.getArtifactKey())) {
                    artifactKeys.add(artifact.getArtifactKey().trim());
                }
            }
        }
        this.retainedFileNames = Collections.unmodifiableSet(fileNames);
        this.retainedArtifactKeys = Collections.unmodifiableSet(artifactKeys);
    }

    public Set<String> getRetainedFileNames() {
        return retainedFileNames;
    }

    public Set<String> getRetainedArtifactKeys() {
        return retainedArtifactKeys;
    }
}
