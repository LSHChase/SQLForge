package com.company.sqlforge.common.config;

import java.util.Locale;

/**
 * 所有服务共享的消息模式。
 */
public enum MessagingMode {
    DATABASE,
    KAFKA,
    MOCK;

    public static MessagingMode fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("消息模式不能为空");
        }
        return MessagingMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
