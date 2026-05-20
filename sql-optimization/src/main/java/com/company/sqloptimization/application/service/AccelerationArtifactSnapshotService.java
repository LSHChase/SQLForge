package com.company.sqloptimization.application.service;

import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import java.util.Collections;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccelerationArtifactSnapshotService {

    private final L2AccelerationArtifactApplicationService accelerationArtifactApplicationService;

    @Autowired
    public AccelerationArtifactSnapshotService(
        L2AccelerationArtifactApplicationService accelerationArtifactApplicationService) {
        this.accelerationArtifactApplicationService = accelerationArtifactApplicationService;
    }

    public AccelerationArtifactSnapshotService() {
        this(null);
    }

    public AccelerationRecommendation captureSnapshot(AccelerationRecommendation recommendation) {
        if (recommendation == null) {
            return null;
        }
        if (AccelerationArtifactSnapshotSanitizer.hasStoredSnapshot(recommendation.getAccelerationArtifact())) {
            Map<String, Object> sanitized =
                AccelerationArtifactSnapshotSanitizer.sanitize(recommendation.getAccelerationArtifact());
            return recommendation.withAccelerationArtifact(emptyIfNull(sanitized));
        }
        return recommendation.withAccelerationArtifact(emptyIfNull(buildLegacySnapshot(recommendation)));
    }

    public Map<String, Object> resolveForResponse(AccelerationRecommendation recommendation) {
        if (recommendation == null) {
            return null;
        }
        if (AccelerationArtifactSnapshotSanitizer.hasStoredSnapshot(recommendation.getAccelerationArtifact())) {
            return AccelerationArtifactSnapshotSanitizer.sanitize(recommendation.getAccelerationArtifact());
        }
        return buildLegacySnapshot(recommendation);
    }

    public Map<String, Object> sanitizeForResponse(Map<String, Object> accelerationArtifact) {
        return AccelerationArtifactSnapshotSanitizer.sanitize(accelerationArtifact);
    }

    private Map<String, Object> buildLegacySnapshot(AccelerationRecommendation recommendation) {
        if (accelerationArtifactApplicationService == null) {
            return null;
        }
        try {
            Map<String, Object> artifact =
                accelerationArtifactApplicationService.buildForRecommendation(recommendation);
            return AccelerationArtifactSnapshotSanitizer.sanitize(artifact);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Map<String, Object> emptyIfNull(Map<String, Object> value) {
        return value == null ? Collections.<String, Object>emptyMap() : value;
    }
}
