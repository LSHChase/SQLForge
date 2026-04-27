package com.company.sqlforge.common.access;

import java.util.Locale;
import org.springframework.util.StringUtils;

public enum AccessChannel {

    PAGE,
    API,
    JDBC_AGENT,
    SDK,
    CLIENT;

    public static AccessChannel fromWireValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if ("JDBC".equals(normalized)) {
            normalized = JDBC_AGENT.name();
        }
        for (AccessChannel channel : values()) {
            if (channel.name().equals(normalized)) {
                return channel;
            }
        }
        return null;
    }
}
