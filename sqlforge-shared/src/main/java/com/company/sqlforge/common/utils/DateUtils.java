package com.company.sqlforge.common.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 所有服务共享的无状态日期工具。
 */
public final class DateUtils {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateUtils() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(DEFAULT_ZONE);
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? null : DATE_TIME_FORMATTER.format(dateTime);
    }

    public static LocalDateTime parse(String value) {
        return value == null || value.trim().isEmpty()
            ? null
            : LocalDateTime.parse(value, DATE_TIME_FORMATTER);
    }

    public static LocalDateTime fromEpochMilli(long epochMilli) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), DEFAULT_ZONE);
    }

    public static long toEpochMilli(LocalDateTime dateTime) {
        return dateTime == null ? 0L : dateTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
    }

    public static Date toDate(LocalDateTime dateTime) {
        return dateTime == null ? null : Date.from(dateTime.atZone(DEFAULT_ZONE).toInstant());
    }
}
