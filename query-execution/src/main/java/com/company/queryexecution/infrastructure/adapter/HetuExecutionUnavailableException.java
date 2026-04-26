package com.company.queryexecution.infrastructure.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HetuExecutionUnavailableException extends RuntimeException {

    private final List<String> attemptedModes;
    private final String routeProfile;
    private final List<String> routeOrder;
    private final String routeEvidenceSource;
    private final String routeVerificationStatus;

    public HetuExecutionUnavailableException(String message, List<String> attemptedModes) {
        this(message, attemptedModes, null, null, Collections.<String>emptyList(), null, null);
    }

    public HetuExecutionUnavailableException(String message, List<String> attemptedModes, Throwable cause) {
        this(message, attemptedModes, cause, null, Collections.<String>emptyList(), null, null);
    }

    public HetuExecutionUnavailableException(String message,
                                             List<String> attemptedModes,
                                             String routeProfile,
                                             List<String> routeOrder,
                                             String routeEvidenceSource,
                                             String routeVerificationStatus) {
        this(message, attemptedModes, null, routeProfile, routeOrder, routeEvidenceSource, routeVerificationStatus);
    }

    public HetuExecutionUnavailableException(String message,
                                             List<String> attemptedModes,
                                             Throwable cause,
                                             String routeProfile,
                                             List<String> routeOrder,
                                             String routeEvidenceSource,
                                             String routeVerificationStatus) {
        super(message, cause);
        this.attemptedModes = attemptedModes == null
            ? Collections.<String>emptyList()
            : Collections.unmodifiableList(new ArrayList<String>(attemptedModes));
        this.routeProfile = routeProfile;
        this.routeOrder = routeOrder == null
            ? Collections.<String>emptyList()
            : Collections.unmodifiableList(new ArrayList<String>(routeOrder));
        this.routeEvidenceSource = routeEvidenceSource;
        this.routeVerificationStatus = routeVerificationStatus;
    }

    public List<String> getAttemptedModes() {
        return attemptedModes;
    }

    public String getRouteProfile() {
        return routeProfile;
    }

    public List<String> getRouteOrder() {
        return routeOrder;
    }

    public String getRouteEvidenceSource() {
        return routeEvidenceSource;
    }

    public String getRouteVerificationStatus() {
        return routeVerificationStatus;
    }
}
