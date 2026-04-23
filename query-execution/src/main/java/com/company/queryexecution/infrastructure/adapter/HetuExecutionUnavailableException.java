package com.company.queryexecution.infrastructure.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HetuExecutionUnavailableException extends RuntimeException {

    private final List<String> attemptedModes;

    public HetuExecutionUnavailableException(String message, List<String> attemptedModes) {
        super(message);
        this.attemptedModes = attemptedModes == null
            ? Collections.<String>emptyList()
            : Collections.unmodifiableList(new ArrayList<String>(attemptedModes));
    }

    public HetuExecutionUnavailableException(String message, List<String> attemptedModes, Throwable cause) {
        super(message, cause);
        this.attemptedModes = attemptedModes == null
            ? Collections.<String>emptyList()
            : Collections.unmodifiableList(new ArrayList<String>(attemptedModes));
    }

    public List<String> getAttemptedModes() {
        return attemptedModes;
    }
}
