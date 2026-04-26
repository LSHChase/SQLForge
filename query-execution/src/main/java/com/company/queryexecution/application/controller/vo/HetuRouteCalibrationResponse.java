package com.company.queryexecution.application.controller.vo;

import com.company.queryexecution.domain.query.HetuClusterEvidenceSnapshot;
import com.company.queryexecution.domain.query.HetuRouteCalibrationModeSnapshot;
import java.util.Collections;
import java.util.List;

public class HetuRouteCalibrationResponse {

    private final boolean hetuEnabled;
    private final String routeProfile;
    private final List<String> declaredAllowedModes;
    private final List<String> effectiveRouteOrder;
    private final boolean skipUnreadyModes;
    private final String evidenceSource;
    private final String liveVerificationStatus;
    private final String readonlyBoundary;
    private final String summary;
    private final HetuClusterEvidenceSnapshot clusterEvidence;
    private final List<HetuRouteCalibrationModeSnapshot> modeCalibrations;
    private final String contractStage;
    private final String implementationStage;

    public HetuRouteCalibrationResponse(boolean hetuEnabled,
                                        String routeProfile,
                                        List<String> declaredAllowedModes,
                                        List<String> effectiveRouteOrder,
                                        boolean skipUnreadyModes,
                                        String evidenceSource,
                                        String liveVerificationStatus,
                                        String readonlyBoundary,
                                        String summary,
                                        HetuClusterEvidenceSnapshot clusterEvidence,
                                        List<HetuRouteCalibrationModeSnapshot> modeCalibrations,
                                        String contractStage,
                                        String implementationStage) {
        this.hetuEnabled = hetuEnabled;
        this.routeProfile = routeProfile;
        this.declaredAllowedModes = declaredAllowedModes == null
            ? Collections.<String>emptyList()
            : declaredAllowedModes;
        this.effectiveRouteOrder = effectiveRouteOrder == null
            ? Collections.<String>emptyList()
            : effectiveRouteOrder;
        this.skipUnreadyModes = skipUnreadyModes;
        this.evidenceSource = evidenceSource;
        this.liveVerificationStatus = liveVerificationStatus;
        this.readonlyBoundary = readonlyBoundary;
        this.summary = summary;
        this.clusterEvidence = clusterEvidence;
        this.modeCalibrations = modeCalibrations == null
            ? Collections.<HetuRouteCalibrationModeSnapshot>emptyList()
            : modeCalibrations;
        this.contractStage = contractStage;
        this.implementationStage = implementationStage;
    }

    public boolean isHetuEnabled() {
        return hetuEnabled;
    }

    public String getRouteProfile() {
        return routeProfile;
    }

    public List<String> getDeclaredAllowedModes() {
        return declaredAllowedModes;
    }

    public List<String> getEffectiveRouteOrder() {
        return effectiveRouteOrder;
    }

    public boolean isSkipUnreadyModes() {
        return skipUnreadyModes;
    }

    public String getEvidenceSource() {
        return evidenceSource;
    }

    public String getLiveVerificationStatus() {
        return liveVerificationStatus;
    }

    public String getReadonlyBoundary() {
        return readonlyBoundary;
    }

    public String getSummary() {
        return summary;
    }

    public HetuClusterEvidenceSnapshot getClusterEvidence() {
        return clusterEvidence;
    }

    public List<HetuRouteCalibrationModeSnapshot> getModeCalibrations() {
        return modeCalibrations;
    }

    public String getContractStage() {
        return contractStage;
    }

    public String getImplementationStage() {
        return implementationStage;
    }
}
