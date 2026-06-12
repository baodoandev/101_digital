package com.coffee.platform.shop;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OpeningHoursTest {

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final LocalTime SEVEN = LocalTime.of(7, 0);
    private static final LocalTime NINETEEN = LocalTime.of(19, 0);
    private static final LocalTime TWENTY_TWO = LocalTime.of(22, 0);
    private static final LocalTime SIX = LocalTime.of(6, 0);

    private Instant instantAtUtc(int hour, int minute) {
        return ZonedDateTime.of(2025, 1, 15, hour, minute, 0, 0, UTC).toInstant();
    }

    @Test
    @DisplayName("Normal hours (07:00-19:00): 10:00 is open")
    void normalHours_midday_isOpen() {
        assertThat(OpeningHours.isOpen(SEVEN, NINETEEN, UTC, instantAtUtc(10, 0))).isTrue();
    }

    @Test
    @DisplayName("Normal hours (07:00-19:00): 06:59 is closed")
    void normalHours_beforeOpening_isClosed() {
        assertThat(OpeningHours.isOpen(SEVEN, NINETEEN, UTC, instantAtUtc(6, 59))).isFalse();
    }

    @Test
    @DisplayName("Normal hours (07:00-19:00): 19:00 is closed (closing exclusive)")
    void normalHours_atClosing_isClosed() {
        assertThat(OpeningHours.isOpen(SEVEN, NINETEEN, UTC, instantAtUtc(19, 0))).isFalse();
    }

    @Test
    @DisplayName("Midnight wrap (22:00-06:00): 23:00 is open")
    void midnightWrap_lateNight_isOpen() {
        assertThat(OpeningHours.isOpen(TWENTY_TWO, SIX, UTC, instantAtUtc(23, 0))).isTrue();
    }

    @Test
    @DisplayName("Midnight wrap (22:00-06:00): 03:00 is open")
    void midnightWrap_earlyMorning_isOpen() {
        assertThat(OpeningHours.isOpen(TWENTY_TWO, SIX, UTC, instantAtUtc(3, 0))).isTrue();
    }

    @Test
    @DisplayName("Midnight wrap (22:00-06:00): 07:00 is closed")
    void midnightWrap_afterClosing_isClosed() {
        assertThat(OpeningHours.isOpen(TWENTY_TWO, SIX, UTC, instantAtUtc(7, 0))).isFalse();
    }

    @Test
    @DisplayName("Midnight wrap (22:00-06:00): 22:00 is open (opening inclusive)")
    void midnightWrap_atOpening_isOpen() {
        assertThat(OpeningHours.isOpen(TWENTY_TWO, SIX, UTC, instantAtUtc(22, 0))).isTrue();
    }

    @Test
    @DisplayName("Same open/close time (00:00-00:00) means always open")
    void sameOpenClose_alwaysOpen() {
        LocalTime midnight = LocalTime.MIDNIGHT;
        assertThat(OpeningHours.isOpen(midnight, midnight, UTC, instantAtUtc(0, 0))).isTrue();
        assertThat(OpeningHours.isOpen(midnight, midnight, UTC, instantAtUtc(12, 0))).isTrue();
        assertThat(OpeningHours.isOpen(midnight, midnight, UTC, instantAtUtc(23, 59))).isTrue();
    }
}
