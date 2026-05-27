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

    public static final String BEIJING_ZONE_ID = "Asia/Shanghai";
    public static final ZoneId BEIJING_ZONE = ZoneId.of(BEIJING_ZONE_ID);
    public static final ZoneOffset BEIJING_ZONE_OFFSET = ZoneOffset.ofHours(8);

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateUtils() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(BEIJING_ZONE);
    }

    public static Instant nowInstant() {
        return Instant.now();
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? null : DATE_TIME_FORMATTER.format(dateTime);
    }

    public static String format(Instant instant) {
        return instant == null ? null : DATE_TIME_FORMATTER.format(toBeijingDateTime(instant));
    }

    public static LocalDateTime parse(String value) {
        return value == null || value.trim().isEmpty()
            ? null
            : LocalDateTime.parse(value, DATE_TIME_FORMATTER);
    }

    public static LocalDateTime fromEpochMilli(long epochMilli) {
        return toBeijingDateTime(Instant.ofEpochMilli(epochMilli));
    }

    public static long toEpochMilli(LocalDateTime dateTime) {
        return dateTime == null ? 0L : toInstant(dateTime).toEpochMilli();
    }

    public static LocalDateTime toBeijingDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, BEIJING_ZONE);
    }

    public static Instant toInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(BEIJING_ZONE_OFFSET);
    }

    public static Date toDate(LocalDateTime dateTime) {
        return dateTime == null ? null : Date.from(dateTime.atZone(BEIJING_ZONE).toInstant());
    }
}
