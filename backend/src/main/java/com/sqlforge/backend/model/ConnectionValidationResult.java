package com.sqlforge.backend.model;

import java.util.List;

public class ConnectionValidationResult {

    private final boolean valid;
    private final String status;
    private final List<String> messages;

    public ConnectionValidationResult(boolean valid, String status, List<String> messages) {
        this.valid = valid;
        this.status = status;
        this.messages = messages;
    }

    public boolean isValid() {
        return valid;
    }

    public String getStatus() {
        return status;
    }

    public List<String> getMessages() {
        return messages;
    }
}
