package com.company.sqlforge.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DateUtilsTest {

    @Test
    void shouldConvertInstantToBeijingDateTime() {
        assertEquals(
            LocalDateTime.of(2026, 5, 27, 8, 0, 0),
            DateUtils.toBeijingDateTime(Instant.parse("2026-05-27T00:00:00Z"))
        );
    }

    @Test
    void shouldConvertBeijingDateTimeToInstant() {
        assertEquals(
            Instant.parse("2026-05-27T00:00:00Z"),
            DateUtils.toInstant(LocalDateTime.of(2026, 5, 27, 8, 0, 0))
        );
    }

    @Test
    void shouldFormatWithTwentyFourHourClock() {
        assertEquals(
            "2026-05-27 23:05:06",
            DateUtils.format(LocalDateTime.of(2026, 5, 27, 23, 5, 6))
        );
    }
}
