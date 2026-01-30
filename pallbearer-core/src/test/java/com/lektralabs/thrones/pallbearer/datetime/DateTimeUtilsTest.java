package com.lektralabs.thrones.pallbearer.datetime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Provides unit tests to verify that DateTimeUtils methods are working as
 * expected
 */
public class DateTimeUtilsTest {

    @Test
    void testFormatDurationWithSeconds() {
        assertEquals(DateTimeUtils.formatDurationWithSeconds(6),
                "00:00:06.000");
        assertEquals(DateTimeUtils.formatDurationWithSeconds(3600),
                "01:00:00.000");
        assertEquals(DateTimeUtils.formatDurationWithSeconds(
                (184.0 / 30.0)),
                "00:00:06.133");
    }
}
