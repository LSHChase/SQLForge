package com.company.queryexecution.domain.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HetuRouteCalibrationSnapshot {

    private final boolean enabled;
    private final String routeProfile;
    private final List<QueryExecutionAccessMode> declaredAllowedModes;
    private final List<QueryExecutionAccessMode> effectiveRouteOrder;
    private final boolean skipUnreadyModes;
    private final String evidenceSource;
    private final String liveVerificationStatus;
    private final String readonlyBoundary;
    private final String summary;
    private final HetuClusterEvidenceSnapshot clusterEvidence;
    private final List<HetuRouteCalibrationModeSnapshot> modeCalibrations;
    private final Map<QueryExecutionAccessMode, HetuRouteCalibrationModeSnapshot> modesByName;

    public HetuRouteCalibrationSnapshot(boolean enabled,
                                        String routeProfile,
                                        List<QueryExecutionAccessMode> declaredAllowedModes,
                                        List<QueryExecutionAccessMode> effectiveRouteOrder,
                                        boolean skipUnreadyModes,
                                        String evidenceSource,
                                        String liveVerificationStatus,
                                        String readonlyBoundary,
                                        String summary,
                                        HetuClusterEvidenceSnapshot clusterEvidence,
                                        List<HetuRouteCalibrationModeSnapshot> modeCalibrations) {
        this.enabled = enabled;
        this.routeProfile = routeProfile;
        this.declaredAllowedModes = immutableCopy(declaredAllowedModes);
        this.effectiveRouteOrder = immutableCopy(effectiveRouteOrder);
        this.skipUnreadyModes = skipUnreadyModes;
        this.evidenceSource = evidenceSource;
        this.liveVerificationStatus = liveVerificationStatus;
        this.readonlyBoundary = readonlyBoundary;
        this.summary = summary;
        this.clusterEvidence = clusterEvidence;
        this.modeCalibrations = modeCalibrations == null
            ? Collections.<HetuRouteCalibrationModeSnapshot>emptyList()
            : Collections.unmodifiableList(new ArrayList<HetuRouteCalibrationModeSnapshot>(modeCalibrations));
        Map<QueryExecutionAccessMode, HetuRouteCalibrationModeSnapshot> index =
            new LinkedHashMap<QueryExecutionAccessMode, HetuRouteCalibrationModeSnapshot>();
        for (HetuRouteCalibrationModeSnapshot snapshot : this.modeCalibrations) {
            if (snapshot != null && snapshot.getMode() != null) {
                index.put(snapshot.getMode(), snapshot);
            }
        }
        this.modesByName = Collections.unmodifiableMap(index);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getRouteProfile() {
        return routeProfile;
    }

    public List<QueryExecutionAccessMode> getDeclaredAllowedModes() {
        return declaredAllowedModes;
    }

    public List<QueryExecutionAccessMode> getEffectiveRouteOrder() {
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

    public HetuRouteCalibrationModeSnapshot getModeSnapshot(QueryExecutionAccessMode mode) {
        return mode == null ? null : modesByName.get(mode);
    }

    public List<String> routeOrderNames() {
        List<String> names = new ArrayList<String>(effectiveRouteOrder.size());
        for (QueryExecutionAccessMode mode : effectiveRouteOrder) {
            if (mode != null) {
                names.add(mode.name());
            }
        }
        return Collections.unmodifiableList(names);
    }

    private List<QueryExecutionAccessMode> immutableCopy(List<QueryExecutionAccessMode> source) {
        return source == null
            ? Collections.<QueryExecutionAccessMode>emptyList()
            : Collections.unmodifiableList(new ArrayList<QueryExecutionAccessMode>(source));
    }
}
