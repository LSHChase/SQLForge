package com.company.sqlforge.common.config;

import java.util.Locale;

/**
 * Shared messaging modes across all services.
 */
public enum MessagingMode {
    DATABASE,
    KAFKA,
    MOCK;

    public static MessagingMode fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Messaging mode must not be blank");
        }
        return MessagingMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
