package com.moguyn.deepdesk.tools;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link DateTimeTools} class.
 */
class DateTimeToolsTest {

    private DateTimeTools dateTimeTools;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @BeforeEach
    public void setUp() {
        dateTimeTools = new DateTimeTools();
    }

    @Test
    void getCurrentDateTime_shouldReturnCurrentDateTimeInCorrectFormat() {
        // When
        String result = dateTimeTools.getCurrentDateTime();

        // Then
        // Verify the format is correct (yyyy-MM-dd HH:mm:ss)
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"),
                "Date format should match 'yyyy-MM-dd HH:mm:ss'");

        // Verify the date is close to now
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime resultDateTime = LocalDateTime.parse(result, FORMATTER);

        long diffInSeconds = Math.abs(
                now.toEpochSecond(ZoneOffset.UTC) - resultDateTime.toEpochSecond(ZoneOffset.UTC));

        // The result should be within 5 seconds of the current time
        assertTrue(diffInSeconds < 5, "Date should be close to current time");
    }

    @Test
    void translateUnixTimestamp_shouldCorrectlyFormatTimestamp() {
        // Given
        String unixTimestamp = "1609459200"; // 2021-01-01 00:00:00 UTC
        String expected = "2021-01-01 00:00:00";

        // When
        String result = dateTimeTools.translateUnixTimestamp(unixTimestamp);

        // Then
        assertEquals(expected, result, "Unix timestamp should be correctly translated");
    }

    @Test
    void translateUnixTimestamp_shouldHandleRecentTimestamp() {
        // Given
        long currentUnixTime = System.currentTimeMillis() / 1000;
        String unixTimestamp = String.valueOf(currentUnixTime);

        // When
        String result = dateTimeTools.translateUnixTimestamp(unixTimestamp);

        // Then
        LocalDateTime expectedDateTime = LocalDateTime.ofEpochSecond(currentUnixTime, 0, ZoneOffset.UTC);
        String expected = expectedDateTime.format(FORMATTER);

        assertEquals(expected, result, "Recent unix timestamp should be correctly translated");
    }
}
